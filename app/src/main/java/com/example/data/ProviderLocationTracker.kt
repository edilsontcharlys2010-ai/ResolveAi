package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class CapturedCoordinates(
  val latitude: Double,
  val longitude: Double,
  val accuracyMeters: Float = 0f,
  val speedMps: Float = 0f,
  val source: String = "GPS"
)

object ProviderLocationTracker {

  fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  @SuppressLint("MissingPermission")
  suspend fun captureCurrentLocation(context: Context): CapturedCoordinates? {
    if (!hasLocationPermission(context)) return null
    val appCtx = context.applicationContext

    // 1. Try FusedLocationProviderClient current location with short timeout
    val fusedResult = withTimeoutOrNull(6_000L) {
      suspendCancellableCoroutine<Location?> { cont ->
        try {
          val client = LocationServices.getFusedLocationProviderClient(appCtx)
          client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
              if (cont.isActive) cont.resume(loc)
            }
            .addOnFailureListener {
              client.lastLocation
                .addOnSuccessListener { last -> if (cont.isActive) cont.resume(last) }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            }
        } catch (_: Throwable) {
          if (cont.isActive) cont.resume(null)
        }
      }
    }
    if (fusedResult != null) {
      return CapturedCoordinates(
        latitude = fusedResult.latitude,
        longitude = fusedResult.longitude,
        accuracyMeters = fusedResult.accuracy,
        speedMps = if (fusedResult.hasSpeed()) fusedResult.speed else 0f,
        source = "FUSED_GPS"
      )
    }

    // 2. Fallback to Android LocationManager last known location
    return try {
      val manager = appCtx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return null
      val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
      )
      val best = providers
        .mapNotNull { provider ->
          runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }
        .maxByOrNull { it.time }
      best?.let { loc ->
        CapturedCoordinates(
          latitude = loc.latitude,
          longitude = loc.longitude,
          accuracyMeters = loc.accuracy,
          speedMps = if (loc.hasSpeed()) loc.speed else 0f,
          source = "LOCATION_MANAGER"
        )
      }
    } catch (_: SecurityException) {
      null
    } catch (_: Throwable) {
      null
    }
  }

  @SuppressLint("MissingPermission")
  fun observeLocationUpdates(
    context: Context,
    intervalMs: Long = 30_000L,
    minDistanceMeters: Float = 25f
  ): Flow<CapturedCoordinates> = callbackFlow {
    if (!hasLocationPermission(context)) {
      close()
      return@callbackFlow
    }
    val appCtx = context.applicationContext
    val client = runCatching { LocationServices.getFusedLocationProviderClient(appCtx) }.getOrNull()
    if (client == null) {
      close()
      return@callbackFlow
    }

    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
      .setMinUpdateIntervalMillis((intervalMs / 2L).coerceAtLeast(10_000L))
      .setMinUpdateDistanceMeters(minDistanceMeters)
      .build()

    val callback = object : LocationCallback() {
      override fun onLocationResult(result: LocationResult) {
        val loc = result.lastLocation ?: return
        trySend(
          CapturedCoordinates(
            latitude = loc.latitude,
            longitude = loc.longitude,
            accuracyMeters = loc.accuracy,
            speedMps = if (loc.hasSpeed()) loc.speed else 0f,
            source = "GPS_TRACKING"
          )
        )
      }
    }

    try {
      client.requestLocationUpdates(request, callback, Looper.getMainLooper())
    } catch (_: Throwable) {
      close()
    }

    awaitClose {
      runCatching { client.removeLocationUpdates(callback) }
    }
  }
}
