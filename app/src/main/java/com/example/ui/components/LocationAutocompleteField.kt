package com.example.ui.components

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CurrentCityLocationHelper
import com.example.data.CurrentCityResult
import com.example.data.GooglePlacesService
import com.example.data.LocationAutocompleteMode
import com.example.data.PlaceSearchResult
import com.example.data.PlaceSuggestion
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldWallet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Campo de texto com autocompletar de cidades e endereços em tempo real via Google Maps Places API
 * (com fallback nativo pelo Google Android Geocoder e opção de detecção da cidade ou endereço atual por GPS).
 */
@Composable
fun GooglePlacesAutocompleteField(
  value: String,
  onValueChange: (String) -> Unit,
  mode: LocationAutocompleteMode,
  label: String,
  placeholder: String = if (mode == LocationAutocompleteMode.CITY) {
    "Digite a cidade ou toque no GPS..."
  } else {
    "Digite a rua, bairro ou toque no GPS..."
  },
  leadingIcon: ImageVector = if (mode == LocationAutocompleteMode.CITY) {
    Icons.Default.LocationCity
  } else {
    Icons.Default.LocationOn
  },
  shape: Shape = RoundedCornerShape(14.dp),
  modifier: Modifier = Modifier,
  fieldTestTag: String? = null,
  enableCurrentCityGps: Boolean = (mode == LocationAutocompleteMode.CITY),
  autoDetectCurrentCity: Boolean = (mode == LocationAutocompleteMode.CITY && enableCurrentCityGps),
  autoRequestLocationPermission: Boolean = autoDetectCurrentCity,
  showInlineCurrentLocationButton: Boolean = (mode == LocationAutocompleteMode.ADDRESS && enableCurrentCityGps),
  detectCityProvider: suspend (Context) -> CurrentCityResult = { ctx ->
    if (mode == LocationAutocompleteMode.CITY) {
      CurrentCityLocationHelper.detectCurrentCity(ctx)
    } else {
      CurrentCityLocationHelper.detectCurrentAddress(ctx)
    }
  },
  onSuggestionSelected: ((PlaceSuggestion) -> Unit)? = null
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val isCityMode = mode == LocationAutocompleteMode.CITY
  var isFocused by remember { mutableStateOf(false) }
  var hasUserTyped by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }
  var isDetectingGps by remember { mutableStateOf(false) }
  var gpsErrorMessage by remember { mutableStateOf<String?>(null) }
  var gpsSuccessBanner by remember { mutableStateOf<String?>(null) }
  var searchResult by remember { mutableStateOf(PlaceSearchResult()) }
  var showDropdown by remember { mutableStateOf(false) }
  var autoDetectAttempted by remember { mutableStateOf(false) }
  var isPendingAutoPermission by remember { mutableStateOf(false) }

  fun executeGpsLookup(isAutomatic: Boolean = false) {
    if (isDetectingGps) return
    scope.launch {
      isDetectingGps = true
      if (!isAutomatic) {
        gpsErrorMessage = null
        gpsSuccessBanner = null
      }
      when (val res = detectCityProvider(context)) {
        is CurrentCityResult.Success -> {
          if (!isAutomatic || (value.isBlank() && !hasUserTyped)) {
            hasUserTyped = false
            showDropdown = false
            gpsErrorMessage = null
            onValueChange(res.cityFormatted)
            gpsSuccessBanner = if (isCityMode) {
              if (isAutomatic) {
                "Cidade local detectada: ${res.cityFormatted}"
              } else {
                "Cidade atual pelo GPS: ${res.cityFormatted}"
              }
            } else {
              "Endereço atual preenchido pelo GPS: ${res.cityFormatted}"
            }
            onSuggestionSelected?.invoke(
              PlaceSuggestion(
                placeId = if (isCityMode) "gps_current_city" else "gps_current_address",
                primaryText = res.cityFormatted.substringBefore(",").trim(),
                secondaryText = res.cityFormatted.substringAfter(",", "").trim(),
                formattedValue = res.cityFormatted,
                isFromGooglePlaces = false
              )
            )
          }
        }
        is CurrentCityResult.Error -> {
          if (!isAutomatic) {
            gpsErrorMessage = res.message
          }
        }
      }
      isDetectingGps = false
    }
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    val wasAuto = isPendingAutoPermission
    isPendingAutoPermission = false
    if (granted) {
      executeGpsLookup(isAutomatic = wasAuto)
    } else {
      isDetectingGps = false
      if (!wasAuto) {
        gpsErrorMessage = if (isCityMode) {
          "Permissão de localização negada. Digite sua cidade manualmente."
        } else {
          "Permissão de localização negada. Digite seu endereço manualmente."
        }
      }
    }
  }

  fun onUseCurrentLocationClick() {
    gpsErrorMessage = null
    isPendingAutoPermission = false
    if (CurrentCityLocationHelper.hasLocationPermission(context)) {
      executeGpsLookup(isAutomatic = false)
    } else {
      val permissionsToRequest = if (isCityMode) {
        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
      } else {
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION
        )
      }
      locationPermissionLauncher.launch(permissionsToRequest)
    }
  }

  LaunchedEffect(autoDetectCurrentCity, autoRequestLocationPermission) {
    if (autoDetectCurrentCity && !autoDetectAttempted && value.isBlank()) {
      autoDetectAttempted = true
      if (CurrentCityLocationHelper.hasLocationPermission(context)) {
        executeGpsLookup(isAutomatic = true)
      } else if (autoRequestLocationPermission) {
        isPendingAutoPermission = true
        locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))
      }
    }
  }

  LaunchedEffect(value, hasUserTyped, isFocused, enableCurrentCityGps) {
    val trimmed = value.trim()
    if (!isFocused) {
      isLoading = false
      showDropdown = false
      return@LaunchedEffect
    }

    if (!hasUserTyped || trimmed.length < 2) {
      isLoading = false
      showDropdown = enableCurrentCityGps
      return@LaunchedEffect
    }

    isLoading = true
    delay(320L)
    val result = GooglePlacesService.searchSuggestions(
      context = context,
      query = trimmed,
      mode = mode
    )
    searchResult = result
    isLoading = false
    showDropdown = isFocused && (
      enableCurrentCityGps ||
        result.suggestions.isNotEmpty() ||
        result.requiresApiKeyConfig ||
        !result.errorMessage.isNullOrBlank()
      )
  }

  Column(modifier = modifier.fillMaxWidth()) {
    OutlinedTextField(
      value = value,
      onValueChange = { updated ->
        hasUserTyped = true
        gpsErrorMessage = null
        gpsSuccessBanner = null
        onValueChange(updated)
      },
      label = { Text(label) },
      placeholder = {
        Text(
          when {
            isDetectingGps && isCityMode -> "Detectando cidade local..."
            isDetectingGps -> stringResource(R.string.address_pulling_current_location)
            else -> placeholder
          }
        )
      },
      leadingIcon = { Icon(leadingIcon, contentDescription = null) },
      trailingIcon = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (isLoading || isDetectingGps) {
            CircularProgressIndicator(
              strokeWidth = 2.dp,
              modifier = Modifier
                .padding(end = 6.dp)
                .size(18.dp)
            )
          } else {
            if (value.isNotEmpty() && isFocused) {
              IconButton(
                onClick = {
                  hasUserTyped = false
                  searchResult = PlaceSearchResult()
                  gpsSuccessBanner = null
                  gpsErrorMessage = null
                  showDropdown = enableCurrentCityGps && isFocused
                  onValueChange("")
                }
              ) {
                Icon(
                  imageVector = Icons.Default.Clear,
                  contentDescription = "Limpar campo",
                  modifier = Modifier.size(18.dp)
                )
              }
            }
            if (enableCurrentCityGps) {
              IconButton(
                onClick = { onUseCurrentLocationClick() },
                modifier = Modifier
                  .size(48.dp)
                  .testTag(if (isCityMode) "btn_gps_current_city" else "btn_gps_current_address")
              ) {
                Icon(
                  imageVector = Icons.Default.MyLocation,
                  contentDescription = if (isCityMode) {
                    "Usar cidade atual pelo GPS"
                  } else {
                    stringResource(R.string.address_use_current_location_cd)
                  },
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }
      },
      singleLine = true,
      shape = shape,
      modifier = Modifier
        .fillMaxWidth()
        .onFocusChanged { focusState ->
          isFocused = focusState.isFocused
          if (focusState.isFocused && enableCurrentCityGps) {
            showDropdown = true
          } else if (!focusState.isFocused) {
            showDropdown = false
          }
        }
        .let { base ->
          if (fieldTestTag != null) base.testTag(fieldTestTag) else base
        }
    )

    if (enableCurrentCityGps && showInlineCurrentLocationButton) {
      OutlinedButton(
        onClick = { onUseCurrentLocationClick() },
        enabled = !isDetectingGps,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 6.dp)
          .heightIn(min = 48.dp)
          .testTag("btn_inline_current_address")
      ) {
        if (isDetectingGps) {
          CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier.size(16.dp)
          )
        } else {
          Icon(
            imageVector = Icons.Default.MyLocation,
            contentDescription = stringResource(R.string.address_use_current_location_cd),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isDetectingGps) {
            stringResource(R.string.address_pulling_current_location)
          } else {
            stringResource(R.string.address_use_current_location)
          },
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    if (gpsSuccessBanner != null) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp, start = 4.dp)
          .testTag("gps_city_success_hint")
      ) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = null,
          tint = EmeraldWallet,
          modifier = Modifier.size(14.dp)
        )
        Text(
          text = gpsSuccessBanner!!,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
          color = EmeraldWallet,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    if (gpsErrorMessage != null) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp, start = 4.dp)
          .testTag("gps_city_error_hint")
      ) {
        Icon(
          imageVector = Icons.Default.ErrorOutline,
          contentDescription = null,
          tint = DangerRed,
          modifier = Modifier.size(14.dp)
        )
        Text(
          text = gpsErrorMessage!!,
          style = MaterialTheme.typography.labelSmall,
          color = DangerRed
        )
      }
    }

    AnimatedVisibility(
      visible = showDropdown,
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically()
    ) {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp)
          .testTag("google_places_suggestions_panel")
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          if (enableCurrentCityGps) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable { onUseCurrentLocationClick() }
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag(if (isCityMode) "gps_current_city_option" else "gps_current_address_option")
            ) {
              Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = if (isCityMode) {
                  "Usar cidade atual pelo GPS"
                } else {
                  stringResource(R.string.address_use_current_location_cd)
                },
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = when {
                    isDetectingGps && isCityMode -> "Detectando cidade atual..."
                    isCityMode -> "Usar cidade atual (GPS)"
                    isDetectingGps -> stringResource(R.string.address_pulling_current_location)
                    else -> "Usar localização atual (GPS)"
                  },
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = if (isCityMode) {
                    "Preencher automaticamente pela localização do aparelho"
                  } else {
                    "Puxar rua, bairro e cidade automaticamente pelo GPS"
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            if (
              searchResult.suggestions.isNotEmpty() ||
              (hasUserTyped && value.trim().length >= 2 && (searchResult.requiresApiKeyConfig || !searchResult.errorMessage.isNullOrBlank()))
            ) {
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
          }

          if (searchResult.suggestions.isNotEmpty()) {
            searchResult.suggestions.take(5).forEachIndexed { index, suggestion ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .clickable {
                    hasUserTyped = false
                    showDropdown = false
                    gpsErrorMessage = null
                    gpsSuccessBanner = null
                    onValueChange(suggestion.formattedValue)
                    onSuggestionSelected?.invoke(suggestion)
                  }
                  .padding(horizontal = 12.dp, vertical = 10.dp)
                  .testTag("google_places_option_$index")
              ) {
                Icon(
                  imageVector = leadingIcon,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = suggestion.primaryText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  if (suggestion.secondaryText.isNotBlank()) {
                    Text(
                      text = suggestion.secondaryText,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
              if (index < searchResult.suggestions.take(5).lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
              }
            }
          } else if (hasUserTyped && value.trim().length >= 2 && (searchResult.requiresApiKeyConfig || !searchResult.errorMessage.isNullOrBlank())) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = searchResult.errorMessage
                  ?: "Configure MAPS_API_KEY no painel Secrets para sugestões do Google Maps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Map,
                contentDescription = null,
                tint = Color(0xFF4285F4),
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (enableCurrentCityGps) "GPS do aparelho & Google Maps" else "Sugestões via Google Maps",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}
