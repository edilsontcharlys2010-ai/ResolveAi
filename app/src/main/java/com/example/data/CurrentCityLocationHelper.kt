package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

sealed class CurrentCityResult {
  data class Success(
    val cityFormatted: String,
    val latitude: Double,
    val longitude: Double
  ) : CurrentCityResult()

  data class Error(val message: String) : CurrentCityResult()
}

object CurrentCityLocationHelper {
  private const val TAG = "CurrentCityLocation"
  private const val LOCATION_TIMEOUT_MS = 8_000L
  private const val GEOCODER_TIMEOUT_MS = 4_000L
  private const val PREFS_NAME = "device_local_city_cache"
  private const val KEY_CACHED_CITY = "cached_local_city"
  private const val KEY_CACHED_AT_MS = "cached_local_city_at_ms"
  private const val CACHE_TTL_MS = 24L * 60L * 60L * 1000L

  @Volatile
  private var inMemoryCachedCity: String? = null

  @Volatile
  private var inMemoryCachedAtMs: Long = 0L

  fun getCachedLocalCity(context: Context, nowMillis: Long = System.currentTimeMillis()): String? {
    val memCity = inMemoryCachedCity?.trim().orEmpty()
    if (memCity.isNotBlank() && (nowMillis - inMemoryCachedAtMs) in 0L..CACHE_TTL_MS) {
      return memCity
    }
    return try {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val savedCity = prefs.getString(KEY_CACHED_CITY, null)?.trim().orEmpty()
      val savedAt = prefs.getLong(KEY_CACHED_AT_MS, 0L)
      if (savedCity.isNotBlank() && (nowMillis - savedAt) in 0L..CACHE_TTL_MS) {
        inMemoryCachedCity = savedCity
        inMemoryCachedAtMs = savedAt
        savedCity
      } else {
        null
      }
    } catch (_: Exception) {
      null
    }
  }

  fun cacheLocalCity(context: Context, cityFormatted: String, nowMillis: Long = System.currentTimeMillis()) {
    val clean = cityFormatted.trim()
    if (clean.isBlank()) return
    inMemoryCachedCity = clean
    inMemoryCachedAtMs = nowMillis
    try {
      context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_CACHED_CITY, clean)
        .putLong(KEY_CACHED_AT_MS, nowMillis)
        .apply()
    } catch (_: Exception) {
      // Ignora erro de persistência em ambientes restritos
    }
  }

  fun clearCachedLocalCity(context: Context) {
    inMemoryCachedCity = null
    inMemoryCachedAtMs = 0L
    try {
      context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .clear()
        .apply()
    } catch (_: Exception) {
      // Ignora
    }
  }

  fun hasLocationPermission(context: Context): Boolean {
    val hasCoarse = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasFine = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return hasCoarse || hasFine
  }

  fun isLocationEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return try {
      LocationManagerCompat.isLocationEnabled(manager)
    } catch (_: Exception) {
      false
    }
  }

  suspend fun detectCurrentCity(
    context: Context,
    apiOverride: GooglePlacesApi? = null,
    apiKeyOverride: String? = null
  ): CurrentCityResult {
    if (!hasLocationPermission(context)) {
      return CurrentCityResult.Error("Permissão de localização necessária para identificar sua cidade.")
    }

    if (!isLocationEnabled(context)) {
      return CurrentCityResult.Error("Ative a localização (GPS) do aparelho para preencher a cidade atual.")
    }

    val location = fetchBestLocation(context)
      ?: return CurrentCityResult.Error(
        "Não foi possível obter a localização atual do GPS. Tente novamente ou digite sua cidade."
      )

    val formattedCity = resolveCityNameFromCoordinates(
      context = context,
      latitude = location.latitude,
      longitude = location.longitude,
      apiOverride = apiOverride,
      apiKeyOverride = apiKeyOverride
    )

    if (formattedCity.isNullOrBlank()) {
      return CurrentCityResult.Error(
        "Sinal de GPS obtido, mas não foi possível identificar a cidade. Digite manualmente."
      )
    }

    cacheLocalCity(context, formattedCity)
    return CurrentCityResult.Success(
      cityFormatted = formattedCity,
      latitude = location.latitude,
      longitude = location.longitude
    )
  }

  suspend fun detectCurrentAddress(
    context: Context,
    apiOverride: GooglePlacesApi? = null,
    apiKeyOverride: String? = null
  ): CurrentCityResult {
    if (!hasLocationPermission(context)) {
      return CurrentCityResult.Error("Permissão de localização necessária para puxar seu endereço atual.")
    }

    if (!isLocationEnabled(context)) {
      return CurrentCityResult.Error("Ative a localização (GPS) do aparelho para preencher o endereço automaticamente.")
    }

    val location = fetchBestLocation(context, highAccuracy = true)
      ?: return CurrentCityResult.Error(
        "Não foi possível obter sua localização atual do GPS. Tente novamente ou digite o endereço."
      )

    val formattedAddress = resolveAddressFromCoordinates(
      context = context,
      latitude = location.latitude,
      longitude = location.longitude,
      apiOverride = apiOverride,
      apiKeyOverride = apiKeyOverride
    )

    if (formattedAddress.isNullOrBlank()) {
      return CurrentCityResult.Error(
        "Sinal de GPS obtido, mas não foi possível identificar o endereço completo. Digite manualmente."
      )
    }

    return CurrentCityResult.Success(
      cityFormatted = formattedAddress,
      latitude = location.latitude,
      longitude = location.longitude
    )
  }

  @SuppressLint("MissingPermission")
  private suspend fun fetchBestLocation(context: Context, highAccuracy: Boolean = false): Location? {
    val freshnessWindowMs = if (highAccuracy) 2 * 60_000L else 30 * 60_000L
    val priority = if (highAccuracy) {
      Priority.PRIORITY_HIGH_ACCURACY
    } else {
      Priority.PRIORITY_BALANCED_POWER_ACCURACY
    }

    // 1. FusedLocationProviderClient (Google Play Services)
    val fusedLocation = try {
      val fusedClient = LocationServices.getFusedLocationProviderClient(context)
      val lastKnown = withTimeoutOrNull(1_500L) {
        awaitTaskNullable(fusedClient.lastLocation)
      }
      val isFresh = lastKnown != null &&
        (System.currentTimeMillis() - lastKnown.time) in 0L..freshnessWindowMs
      if (isFresh) {
        lastKnown
      } else {
        val cts = CancellationTokenSource()
        val current = withTimeoutOrNull(5_000L) {
          awaitTaskNullable(
            fusedClient.getCurrentLocation(priority, cts.token)
          )
        }
        if (current == null) cts.cancel()
        current ?: lastKnown
      }
    } catch (e: Exception) {
      Log.w(TAG, "FusedLocation indisponível: ${e.message}")
      null
    }
    if (fusedLocation != null) return fusedLocation

    // 2. Fallback nativo via LocationManager (GPS / NETWORK / PASSIVE)
    return fetchFromLocationManager(context, preferGps = highAccuracy)
  }

  @SuppressLint("MissingPermission")
  private suspend fun fetchFromLocationManager(
    context: Context,
    preferGps: Boolean = false
  ): Location? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val hasFine = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val cachedProviders = buildList {
      if (preferGps && hasFine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        add(LocationManager.GPS_PROVIDER)
      }
      if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        add(LocationManager.NETWORK_PROVIDER)
      }
      if (!preferGps && hasFine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        add(LocationManager.GPS_PROVIDER)
      }
      add(LocationManager.PASSIVE_PROVIDER)
    }

    for (provider in cachedProviders) {
      val last = try {
        manager.getLastKnownLocation(provider)
      } catch (_: SecurityException) {
        null
      } catch (_: Exception) {
        null
      }
      if (last != null) return last
    }

    val activeProvider = when {
      preferGps && hasFine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
      manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
      hasFine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
      else -> return null
    }

    return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
      suspendCancellableCoroutine { cont ->
        val signal = CancellationSignal()
        cont.invokeOnCancellation { signal.cancel() }
        try {
          LocationManagerCompat.getCurrentLocation(
            manager,
            activeProvider,
            signal,
            ContextCompat.getMainExecutor(context)
          ) { location ->
            if (cont.isActive) cont.resume(location)
          }
        } catch (_: Exception) {
          if (cont.isActive) cont.resume(null)
        }
      }
    }
  }

  internal suspend fun resolveCityNameFromCoordinates(
    context: Context,
    latitude: Double,
    longitude: Double,
    apiOverride: GooglePlacesApi? = null,
    apiKeyOverride: String? = null
  ): String? {
    val fromAndroidGeocoder = withContext(Dispatchers.IO) {
      try {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context, Locale("pt", "BR"))
        val addresses: List<Address> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          withTimeoutOrNull(GEOCODER_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
              try {
                geocoder.getFromLocation(
                  latitude,
                  longitude,
                  3,
                  object : Geocoder.GeocodeListener {
                    override fun onGeocode(results: MutableList<Address>) {
                      if (cont.isActive) cont.resume(results)
                    }

                    override fun onError(errorMessage: String?) {
                      if (cont.isActive) cont.resume(emptyList())
                    }
                  }
                )
              } catch (_: Throwable) {
                if (cont.isActive) cont.resume(emptyList())
              }
            }
          } ?: emptyList()
        } else {
          @Suppress("DEPRECATION")
          geocoder.getFromLocation(latitude, longitude, 3) ?: emptyList()
        }

        addresses.firstNotNullOfOrNull { addr ->
          val city = listOfNotNull(addr.locality, addr.subAdminArea, addr.featureName)
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
          val state = addr.adminArea.orEmpty()
          GooglePlacesService.formatCityAndState(city, state).takeIf { it.isNotBlank() }
        }
      } catch (e: Exception) {
        Log.w(TAG, "Geocoder reverso falhou: ${e.message}")
        null
      }
    }

    if (!fromAndroidGeocoder.isNullOrBlank()) {
      return fromAndroidGeocoder
    }

    return GooglePlacesService.reverseGeocodeCity(
      latitude = latitude,
      longitude = longitude,
      apiOverride = apiOverride,
      apiKeyOverride = apiKeyOverride
    )
  }

  internal suspend fun resolveAddressFromCoordinates(
    context: Context,
    latitude: Double,
    longitude: Double,
    apiOverride: GooglePlacesApi? = null,
    apiKeyOverride: String? = null
  ): String? {
    val fromAndroidGeocoder = withContext(Dispatchers.IO) {
      try {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context, Locale("pt", "BR"))
        val addresses: List<Address> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          withTimeoutOrNull(GEOCODER_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
              try {
                geocoder.getFromLocation(
                  latitude,
                  longitude,
                  3,
                  object : Geocoder.GeocodeListener {
                    override fun onGeocode(results: MutableList<Address>) {
                      if (cont.isActive) cont.resume(results)
                    }

                    override fun onError(errorMessage: String?) {
                      if (cont.isActive) cont.resume(emptyList())
                    }
                  }
                )
              } catch (_: Throwable) {
                if (cont.isActive) cont.resume(emptyList())
              }
            }
          } ?: emptyList()
        } else {
          @Suppress("DEPRECATION")
          geocoder.getFromLocation(latitude, longitude, 3) ?: emptyList()
        }

        addresses.firstNotNullOfOrNull { addr ->
          val city = listOfNotNull(addr.locality, addr.subAdminArea)
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
          GooglePlacesService.formatFullAddress(
            street = addr.thoroughfare.orEmpty(),
            number = addr.subThoroughfare.orEmpty(),
            neighborhood = addr.subLocality.orEmpty(),
            city = city,
            adminArea = addr.adminArea.orEmpty(),
            fallbackFormatted = addr.getAddressLine(0).orEmpty()
          ).takeIf { it.isNotBlank() }
        }
      } catch (e: Exception) {
        Log.w(TAG, "Geocoder reverso de endereço falhou: ${e.message}")
        null
      }
    }

    if (!fromAndroidGeocoder.isNullOrBlank()) {
      return fromAndroidGeocoder
    }

    val fromGoogleApi = GooglePlacesService.reverseGeocodeAddress(
      latitude = latitude,
      longitude = longitude,
      apiOverride = apiOverride,
      apiKeyOverride = apiKeyOverride
    )
    if (!fromGoogleApi.isNullOrBlank()) {
      return fromGoogleApi
    }

    return resolveCityNameFromCoordinates(
      context = context,
      latitude = latitude,
      longitude = longitude,
      apiOverride = apiOverride,
      apiKeyOverride = apiKeyOverride
    )
  }

  private suspend fun <T> awaitTaskNullable(task: Task<T>): T? {
    return suspendCancellableCoroutine { cont ->
      task.addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
      }.addOnFailureListener {
        if (cont.isActive) cont.resume(null)
      }.addOnCanceledListener {
        if (cont.isActive) cont.resume(null)
      }
    }
  }
}
