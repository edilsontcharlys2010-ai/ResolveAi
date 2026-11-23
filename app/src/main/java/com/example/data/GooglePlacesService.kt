package com.example.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

enum class LocationAutocompleteMode {
  CITY,
  ADDRESS
}

data class PlaceSuggestion(
  val placeId: String,
  val primaryText: String,
  val secondaryText: String,
  val formattedValue: String,
  val isFromGooglePlaces: Boolean = true
)

data class PlaceSearchResult(
  val suggestions: List<PlaceSuggestion> = emptyList(),
  val requiresApiKeyConfig: Boolean = false,
  val errorMessage: String? = null
)

data class PlacesAutocompleteResponse(
  val status: String = "",
  val predictions: List<PlacePredictionDto> = emptyList(),
  @param:Json(name = "error_message") val errorMessage: String? = null
)

data class PlacePredictionDto(
  @param:Json(name = "place_id") val placeId: String = "",
  val description: String = "",
  @param:Json(name = "structured_formatting") val structuredFormatting: StructuredFormattingDto? = null
)

data class StructuredFormattingDto(
  @param:Json(name = "main_text") val mainText: String = "",
  @param:Json(name = "secondary_text") val secondaryText: String = ""
)

data class ReverseGeocodeResponse(
  val status: String = "",
  val results: List<ReverseGeocodeResultDto> = emptyList(),
  @param:Json(name = "error_message") val errorMessage: String? = null
)

data class ReverseGeocodeResultDto(
  @param:Json(name = "formatted_address") val formattedAddress: String = "",
  @param:Json(name = "address_components") val addressComponents: List<ReverseGeocodeComponentDto> = emptyList()
)

data class ReverseGeocodeComponentDto(
  @param:Json(name = "long_name") val longName: String = "",
  @param:Json(name = "short_name") val shortName: String = "",
  val types: List<String> = emptyList()
)

interface GooglePlacesApi {
  @GET("maps/api/place/autocomplete/json")
  suspend fun autocomplete(
    @Query("input") input: String,
    @Query("types") types: String,
    @Query("language") language: String = "pt-BR",
    @Query("components") components: String = "country:br",
    @Query("key") apiKey: String
  ): PlacesAutocompleteResponse

  @GET("maps/api/geocode/json")
  suspend fun reverseGeocode(
    @Query("latlng") latlng: String,
    @Query("language") language: String = "pt-BR",
    @Query("result_type") resultType: String? = "locality|administrative_area_level_2",
    @Query("key") apiKey: String
  ): ReverseGeocodeResponse
}

object GooglePlacesService {
  private const val TAG = "GooglePlacesService"
  private const val BASE_URL = "https://maps.googleapis.com/"
  private const val PLACEHOLDER_MAPS_KEY = "MY_MAPS_API_KEY"

  private val moshi: Moshi by lazy {
    Moshi.Builder()
      .addLast(KotlinJsonAdapterFactory())
      .build()
  }

  private val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(6, TimeUnit.SECONDS)
      .readTimeout(6, TimeUnit.SECONDS)
      .build()
  }

  private val api: GooglePlacesApi by lazy {
    Retrofit.Builder()
      .baseUrl(BASE_URL)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(GooglePlacesApi::class.java)
  }

  fun hasConfiguredApiKey(rawKey: String = BuildConfig.MAPS_API_KEY): Boolean {
    val trimmed = rawKey.trim()
    return trimmed.isNotEmpty() && trimmed != PLACEHOLDER_MAPS_KEY
  }

  suspend fun searchSuggestions(
    context: Context,
    query: String,
    mode: LocationAutocompleteMode,
    apiOverride: GooglePlacesApi? = null,
    apiKeyOverride: String? = null
  ): PlaceSearchResult {
    val cleanQuery = query.trim()
    if (cleanQuery.length < 2) {
      return PlaceSearchResult()
    }

    val apiKey = (apiKeyOverride ?: BuildConfig.MAPS_API_KEY).trim()
    val placesType = when (mode) {
      LocationAutocompleteMode.CITY -> "(cities)"
      LocationAutocompleteMode.ADDRESS -> "geocode"
    }

    if (hasConfiguredApiKey(apiKey)) {
      try {
        val response = withContext(Dispatchers.IO) {
          (apiOverride ?: api).autocomplete(
            input = cleanQuery,
            types = placesType,
            apiKey = apiKey
          )
        }
        val mapped = mapPlacesResponse(response, mode)
        if (mapped.suggestions.isNotEmpty() || response.status == "ZERO_RESULTS") {
          return mapped
        }
        if (mapped.errorMessage != null) {
          Log.w(TAG, "Google Places API retornou status=${response.status}: ${mapped.errorMessage}")
        }
      } catch (e: Exception) {
        Log.w(TAG, "Falha ao consultar Google Places API: ${e.message}")
      }
    }

    // Fallback nativo via Google Android Geocoder quando MAPS_API_KEY ainda não foi configurada
    val geocoderSuggestions = searchWithAndroidGeocoder(context, cleanQuery, mode)
    if (geocoderSuggestions.isNotEmpty()) {
      return PlaceSearchResult(suggestions = geocoderSuggestions)
    }

    return if (!hasConfiguredApiKey(apiKey)) {
      PlaceSearchResult(
        suggestions = emptyList(),
        requiresApiKeyConfig = true,
        errorMessage = "Configure MAPS_API_KEY no painel Secrets do AI Studio para autocompletar pelo Google Maps Places."
      )
    } else {
      PlaceSearchResult(suggestions = emptyList())
    }
  }

  internal fun mapPlacesResponse(
    response: PlacesAutocompleteResponse,
    mode: LocationAutocompleteMode
  ): PlaceSearchResult {
    return when (response.status) {
      "OK" -> {
        val suggestions = response.predictions.mapNotNull { prediction ->
          val primary = prediction.structuredFormatting?.mainText?.takeIf { it.isNotBlank() }
            ?: prediction.description.substringBefore(",").trim()
          if (primary.isBlank()) return@mapNotNull null

          val secondary = prediction.structuredFormatting?.secondaryText?.takeIf { it.isNotBlank() }
            ?: prediction.description.substringAfter(",", "").trim()

          val formatted = formatSelectedValue(
            description = prediction.description,
            primaryText = primary,
            secondaryText = secondary,
            mode = mode
          )
          PlaceSuggestion(
            placeId = prediction.placeId.ifBlank { formatted },
            primaryText = primary,
            secondaryText = secondary.removeSuffix(", Brasil").removeSuffix(", Brazil").trim(),
            formattedValue = formatted,
            isFromGooglePlaces = true
          )
        }
        PlaceSearchResult(suggestions = suggestions)
      }
      "ZERO_RESULTS" -> PlaceSearchResult(suggestions = emptyList())
      "REQUEST_DENIED", "INVALID_REQUEST", "OVER_QUERY_LIMIT" -> PlaceSearchResult(
        suggestions = emptyList(),
        requiresApiKeyConfig = response.status == "REQUEST_DENIED",
        errorMessage = response.errorMessage ?: "Google Places status: ${response.status}"
      )
      else -> PlaceSearchResult(errorMessage = response.errorMessage)
    }
  }

  internal fun formatSelectedValue(
    description: String,
    primaryText: String,
    secondaryText: String,
    mode: LocationAutocompleteMode
  ): String {
    val cleanSecondary = secondaryText
      .removeSuffix(", Brasil")
      .removeSuffix(", Brazil")
      .trim()

    return when (mode) {
      LocationAutocompleteMode.CITY -> {
        if (cleanSecondary.isBlank()) {
          primaryText.trim()
        } else {
          // Ex: secondary "SP" -> "São Paulo, SP"; secondary "State of São Paulo" -> "São Paulo, State of São Paulo"
          val stateToken = cleanSecondary.split(",", "-").firstOrNull()?.trim().orEmpty()
          if (stateToken.isNotBlank() && stateToken.length <= 3) {
            "${primaryText.trim()}, $stateToken"
          } else {
            "${primaryText.trim()}, $cleanSecondary"
          }
        }
      }
      LocationAutocompleteMode.ADDRESS -> {
        description
          .removeSuffix(", Brasil")
          .removeSuffix(", Brazil")
          .trim()
          .ifBlank {
            if (cleanSecondary.isNotBlank()) "${primaryText.trim()} - $cleanSecondary" else primaryText.trim()
          }
      }
    }
  }

  private suspend fun searchWithAndroidGeocoder(
    context: Context,
    query: String,
    mode: LocationAutocompleteMode
  ): List<PlaceSuggestion> = withContext(Dispatchers.IO) {
    try {
      if (!Geocoder.isPresent()) return@withContext emptyList()
      val geocoder = Geocoder(context, Locale("pt", "BR"))
      val addresses: List<Address> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { cont ->
          try {
            geocoder.getFromLocationName(query, 6) { results ->
              if (cont.isActive) cont.resume(results)
            }
          } catch (_: Throwable) {
            if (cont.isActive) cont.resume(emptyList())
          }
        }
      } else {
        @Suppress("DEPRECATION")
        geocoder.getFromLocationName(query, 6) ?: emptyList()
      }

      addresses.mapIndexedNotNull { idx, addr ->
        val city = addr.locality ?: addr.subAdminArea ?: ""
        val state = normalizeBrazilianStateCode(addr.adminArea ?: "")
        val street = listOfNotNull(addr.thoroughfare, addr.subThoroughfare).joinToString(", ")
        val neighborhood = addr.subLocality ?: ""

        val formatted = when (mode) {
          LocationAutocompleteMode.CITY -> {
            val cityName = city.ifBlank { addr.featureName ?: "" }.trim()
            if (cityName.isBlank()) return@mapIndexedNotNull null
            if (state.isNotBlank()) "$cityName, $state" else cityName
          }
          LocationAutocompleteMode.ADDRESS -> {
            val baseStreet = street.ifBlank { addr.featureName ?: addr.getAddressLine(0) ?: "" }.trim()
            if (baseStreet.isBlank()) return@mapIndexedNotNull null
            val areaPart = listOfNotNull(
              neighborhood.takeIf { it.isNotBlank() },
              listOfNotNull(city.takeIf { it.isNotBlank() }, state.takeIf { it.isNotBlank() }).joinToString(", ").takeIf { it.isNotBlank() }
            ).joinToString(" - ")
            if (areaPart.isNotBlank() && !baseStreet.contains(city, ignoreCase = true)) {
              "$baseStreet - $areaPart"
            } else {
              baseStreet
            }
          }
        }

        val primary = when (mode) {
          LocationAutocompleteMode.CITY -> city.ifBlank { formatted.substringBefore(",") }
          LocationAutocompleteMode.ADDRESS -> street.ifBlank { formatted.substringBefore(" - ") }
        }
        val secondary = when (mode) {
          LocationAutocompleteMode.CITY -> listOfNotNull(state.takeIf { it.isNotBlank() }, addr.countryName?.takeIf { it.isNotBlank() }).joinToString(", ")
          LocationAutocompleteMode.ADDRESS -> listOfNotNull(
            neighborhood.takeIf { it.isNotBlank() },
            city.takeIf { it.isNotBlank() },
            state.takeIf { it.isNotBlank() }
          ).joinToString(" • ")
        }

        PlaceSuggestion(
          placeId = "geo_${idx}_${formatted.hashCode()}",
          primaryText = primary,
          secondaryText = secondary,
          formattedValue = formatted,
          isFromGooglePlaces = false
        )
      }.distinctBy { it.formattedValue }
    } catch (e: Exception) {
      Log.w(TAG, "Geocoder indisponível: ${e.message}")
      emptyList()
    }
  }

  suspend fun reverseGeocodeCity(
    latitude: Double,
    longitude: Double,
    apiOverride: GooglePlacesApi? = null,
    apiKeyOverride: String? = null
  ): String? {
    val apiKey = (apiKeyOverride ?: BuildConfig.MAPS_API_KEY).trim()
    if (!hasConfiguredApiKey(apiKey)) return null
    return try {
      val response = withContext(Dispatchers.IO) {
        (apiOverride ?: api).reverseGeocode(
          latlng = String.format(Locale.US, "%.6f,%.6f", latitude, longitude),
          apiKey = apiKey
        )
      }
      extractCityFromReverseGeocode(response)
    } catch (e: Exception) {
      Log.w(TAG, "Falha no reverse geocode via Google Maps: ${e.message}")
      null
    }
  }

  suspend fun reverseGeocodeAddress(
    latitude: Double,
    longitude: Double,
    apiOverride: GooglePlacesApi? = null,
    apiKeyOverride: String? = null
  ): String? {
    val apiKey = (apiKeyOverride ?: BuildConfig.MAPS_API_KEY).trim()
    if (!hasConfiguredApiKey(apiKey)) return null
    return try {
      val response = withContext(Dispatchers.IO) {
        (apiOverride ?: api).reverseGeocode(
          latlng = String.format(Locale.US, "%.6f,%.6f", latitude, longitude),
          resultType = null,
          apiKey = apiKey
        )
      }
      extractAddressFromReverseGeocode(response)
    } catch (e: Exception) {
      Log.w(TAG, "Falha no reverse geocode de endereço via Google Maps: ${e.message}")
      null
    }
  }

  internal fun extractCityFromReverseGeocode(response: ReverseGeocodeResponse): String? {
    if (response.status != "OK" || response.results.isEmpty()) return null
    for (result in response.results) {
      val comps = result.addressComponents
      val city = comps.firstOrNull {
        it.types.contains("locality") || it.types.contains("administrative_area_level_2")
      }?.longName.orEmpty()
      val stateComp = comps.firstOrNull {
        it.types.contains("administrative_area_level_1")
      }
      val state = stateComp?.shortName?.takeIf { it.isNotBlank() }
        ?: stateComp?.longName.orEmpty()
      val formatted = formatCityAndState(city, state)
      if (formatted.isNotBlank()) return formatted
    }
    return null
  }

  internal fun extractAddressFromReverseGeocode(response: ReverseGeocodeResponse): String? {
    if (response.status != "OK" || response.results.isEmpty()) return null
    for (result in response.results) {
      val comps = result.addressComponents
      val street = comps.firstOrNull { it.types.contains("route") }?.longName.orEmpty()
      val number = comps.firstOrNull { it.types.contains("street_number") }?.shortName.orEmpty()
      val neighborhood = comps.firstOrNull {
        it.types.contains("sublocality_level_1") ||
          it.types.contains("sublocality") ||
          it.types.contains("neighborhood")
      }?.longName.orEmpty()
      val city = comps.firstOrNull {
        it.types.contains("locality") || it.types.contains("administrative_area_level_2")
      }?.longName.orEmpty()
      val stateComp = comps.firstOrNull {
        it.types.contains("administrative_area_level_1")
      }
      val state = stateComp?.shortName?.takeIf { it.isNotBlank() }
        ?: stateComp?.longName.orEmpty()

      val formatted = formatFullAddress(
        street = street,
        number = number,
        neighborhood = neighborhood,
        city = city,
        adminArea = state,
        fallbackFormatted = result.formattedAddress
      )
      if (formatted.isNotBlank()) return formatted
    }
    return null
  }

  internal fun formatFullAddress(
    street: String,
    number: String = "",
    neighborhood: String = "",
    city: String = "",
    adminArea: String = "",
    fallbackFormatted: String = ""
  ): String {
    val cleanStreet = street.trim()
    val cleanNumber = number.trim()
    val streetPart = when {
      cleanStreet.isNotBlank() && cleanNumber.isNotBlank() -> "$cleanStreet, $cleanNumber"
      cleanStreet.isNotBlank() -> cleanStreet
      else -> ""
    }
    val cityState = formatCityAndState(city, adminArea)
    val areaPart = listOfNotNull(
      neighborhood.trim().takeIf { it.isNotBlank() },
      cityState.takeIf { it.isNotBlank() }
    ).joinToString(" - ")

    return when {
      streetPart.isNotBlank() && areaPart.isNotBlank() -> "$streetPart - $areaPart"
      streetPart.isNotBlank() -> streetPart
      areaPart.isNotBlank() -> areaPart
      else -> fallbackFormatted
        .removeSuffix(", Brasil")
        .removeSuffix(", Brazil")
        .trim()
    }
  }

  internal fun formatCityAndState(city: String, adminArea: String): String {
    val cleanCity = city.trim()
    if (cleanCity.isBlank()) return ""
    val state = normalizeBrazilianStateCode(adminArea)
    return if (state.isNotBlank()) "$cleanCity, $state" else cleanCity
  }

  private fun normalizeBrazilianStateCode(adminArea: String): String {
    var clean = adminArea.trim()
    val prefixes = listOf("state of ", "estado de ", "estado do ", "estado da ")
    for (prefix in prefixes) {
      if (clean.lowercase(Locale("pt", "BR")).startsWith(prefix)) {
        clean = clean.substring(prefix.length).trim()
        break
      }
    }
    if (clean.length == 2) return clean.uppercase(Locale.ROOT)
    return when (clean.lowercase(Locale("pt", "BR"))) {
      "acre" -> "AC"
      "alagoas" -> "AL"
      "amapá", "amapa" -> "AP"
      "amazonas" -> "AM"
      "bahia" -> "BA"
      "ceará", "ceara" -> "CE"
      "distrito federal", "federal district" -> "DF"
      "espírito santo", "espirito santo" -> "ES"
      "goiás", "goias" -> "GO"
      "maranhão", "maranhao" -> "MA"
      "mato grosso" -> "MT"
      "mato grosso do sul" -> "MS"
      "minas gerais" -> "MG"
      "pará", "para" -> "PA"
      "paraíba", "paraiba" -> "PB"
      "paraná", "parana" -> "PR"
      "pernambuco" -> "PE"
      "piauí", "piaui" -> "PI"
      "rio de janeiro" -> "RJ"
      "rio grande do norte" -> "RN"
      "rio grande do sul" -> "RS"
      "rondônia", "rondonia" -> "RO"
      "roraima" -> "RR"
      "santa catarina" -> "SC"
      "são paulo", "sao paulo" -> "SP"
      "sergipe" -> "SE"
      "tocantins" -> "TO"
      else -> clean
    }
  }
}
