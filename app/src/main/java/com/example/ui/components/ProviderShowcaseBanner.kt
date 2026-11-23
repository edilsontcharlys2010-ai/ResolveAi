package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.ProviderShowcaseEntry
import com.example.model.UserRole
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight

@Composable
fun ProviderShowcaseSection(
  entries: List<ProviderShowcaseEntry>,
  currentUserRole: UserRole,
  onRequestServiceClick: () -> Unit,
  onManageShowcaseClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  if (entries.isEmpty()) return

  var selectedEntry by remember { mutableStateOf<ProviderShowcaseEntry?>(null) }

  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
    shape = RoundedCornerShape(16.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("provider_showcase_banner")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f),
              MaterialTheme.colorScheme.surface
            )
          )
        )
        .padding(top = 12.dp, bottom = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              color = MaterialTheme.colorScheme.primary,
              shape = RoundedCornerShape(6.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(11.dp)
                )
                Text(
                  text = "PUBLICIDADE • VITRINE",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp
                  ),
                  color = Color.White
                )
              }
            }
            Text(
              text = "${entries.size} profissionais",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = "Prestadores em Destaque e Serviços Realizados",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        if (currentUserRole == UserRole.PRESTADOR && onManageShowcaseClick != null) {
          Surface(
            color = EmeraldWalletLight,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .heightIn(min = 36.dp)
              .clickable { onManageShowcaseClick() }
              .testTag("btn_manage_showcase")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = "Gerenciar minha vitrine",
                tint = EmeraldWallet,
                modifier = Modifier.size(15.dp)
              )
              Text(
                text = "Minhas fotos",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = EmeraldWallet,
                maxLines = 1
              )
            }
          }
        }
      }

      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("provider_showcase_carousel"),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(entries, key = { it.providerId }) { entry ->
          ProviderShowcasePromoCard(
            entry = entry,
            onClick = { selectedEntry = entry }
          )
        }
      }
    }
  }

  selectedEntry?.let { entry ->
    ProviderShowcaseDetailDialog(
      entry = entry,
      currentUserRole = currentUserRole,
      onDismiss = { selectedEntry = null },
      onRequestServiceClick = {
        selectedEntry = null
        onRequestServiceClick()
      },
      onManageShowcaseClick = onManageShowcaseClick?.let { manage ->
        {
          selectedEntry = null
          manage()
        }
      }
    )
  }
}

@Composable
private fun ProviderShowcasePromoCard(
  entry: ProviderShowcaseEntry,
  onClick: () -> Unit
) {
  var selectedPhotoIndex by remember(entry.providerId, entry.photos.size) {
    mutableIntStateOf(0)
  }
  val safeIndex = selectedPhotoIndex.coerceIn(0, (entry.photos.size - 1).coerceAtLeast(0))
  val activePhoto = entry.photos[safeIndex]

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier
      .width(256.dp)
      .clickable { onClick() }
      .testTag("promo_provider_card_${entry.providerId}")
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(126.dp)
      ) {
        DocumentAttachmentPreviewImage(
          attachmentUri = activePhoto.photoUri,
          contentDescription = "Foto do serviço de ${entry.providerName}: ${activePhoto.title}",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )

        // Subtle gradient scrim for crisp readable overlay labels
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(
                  Color.Black.copy(alpha = 0.28f),
                  Color.Transparent,
                  Color.Black.copy(alpha = 0.68f)
                )
              )
            )
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .padding(8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.90f),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(
              text = activePhoto.category.ifBlank { entry.providerCategory }.ifBlank { "Serviço" },
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              ),
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier
                .widthIn(max = 158.dp)
                .padding(horizontal = 7.dp, vertical = 3.dp)
            )
          }

          if (entry.photos.size > 1) {
            Surface(
              color = Color.Black.copy(alpha = 0.65f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Collections,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(11.dp)
                )
                Text(
                  text = "${safeIndex + 1}/${entry.photos.size}",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                  ),
                  color = Color.White
                )
              }
            }
          }
        }

        if (activePhoto.title.isNotBlank()) {
          Text(
            text = activePhoto.title,
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 11.5.sp
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(horizontal = 9.dp, vertical = 7.dp)
          )
        }
      }

      if (entry.photos.size > 1) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 9.dp, end = 9.dp, top = 7.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          entry.photos.take(5).forEachIndexed { idx, photo ->
            val selected = idx == safeIndex
            DocumentAttachmentPreviewImage(
              attachmentUri = photo.photoUri,
              contentDescription = "Miniatura ${idx + 1} de ${entry.providerName}",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(7.dp))
                .border(
                  width = if (selected) 2.dp else 1.dp,
                  color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                  shape = RoundedCornerShape(7.dp)
                )
                .clickable { selectedPhotoIndex = idx }
            )
          }
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        ProfileAvatar(
          name = entry.providerName,
          photoUrl = entry.providerPhotoUrl,
          avatarColorHex = entry.providerAvatarColorHex,
          size = 40.dp,
          contentDescription = "Foto de perfil de ${entry.providerName}",
          modifier = Modifier.testTag("promo_provider_avatar_${entry.providerId}")
        )

        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Text(
              text = entry.providerName,
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
              ),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false)
            )
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Prestador verificado",
              tint = EmeraldWallet,
              modifier = Modifier.size(13.dp)
            )
          }
          Text(
            text = buildString {
              append("${entry.completedJobs} serviços")
              if (entry.providerCity.isNotBlank()) {
                append(" • ${entry.providerCity}")
              }
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Surface(
          color = WarningAmberLight,
          shape = RoundedCornerShape(9.dp),
          modifier = Modifier.testTag("promo_provider_rating_${entry.providerId}")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = "Nota",
              tint = WarningAmber,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = formatUserRating(entry.providerRating),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.5.sp
              ),
              color = Color(0xFF92400E)
            )
          }
        }
      }
    }
  }
}

@Composable
fun ProviderShowcaseDetailDialog(
  entry: ProviderShowcaseEntry,
  currentUserRole: UserRole,
  onDismiss: () -> Unit,
  onRequestServiceClick: () -> Unit,
  onManageShowcaseClick: (() -> Unit)? = null
) {
  var selectedPhotoIndex by remember(entry.providerId, entry.photos.size) {
    mutableIntStateOf(0)
  }
  var showFullScreenPhoto by remember { mutableStateOf(false) }
  val safeIndex = selectedPhotoIndex.coerceIn(0, (entry.photos.size - 1).coerceAtLeast(0))
  val activePhoto = entry.photos[safeIndex]

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp)
      .widthIn(max = 520.dp)
      .testTag("promo_provider_detail_dialog"),
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        ProfileAvatar(
          name = entry.providerName,
          photoUrl = entry.providerPhotoUrl,
          avatarColorHex = entry.providerAvatarColorHex,
          size = 48.dp,
          contentDescription = "Foto de perfil de ${entry.providerName}"
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = entry.providerName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = entry.providerCategory.ifBlank { "Prestador ResolveAi" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Surface(
          color = WarningAmberLight,
          shape = RoundedCornerShape(10.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = null,
              tint = WarningAmber,
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = formatUserRating(entry.providerRating),
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
              color = Color(0xFF92400E)
            )
          }
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { showFullScreenPhoto = true }
            .testTag("promo_detail_hero_photo")
        ) {
          DocumentAttachmentPreviewImage(
            attachmentUri = activePhoto.photoUri,
            contentDescription = activePhoto.title.ifBlank { "Serviço de ${entry.providerName}" },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
          Surface(
            color = Color.Black.copy(alpha = 0.62f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(8.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.ZoomIn,
                contentDescription = "Ampliar foto do serviço",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
              )
              Text(
                text = "Ampliar",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
              )
            }
          }
        }

        if (activePhoto.title.isNotBlank()) {
          Text(
            text = activePhoto.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        if (entry.photos.size > 1) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            entry.photos.forEachIndexed { idx, photo ->
              val selected = idx == safeIndex
              DocumentAttachmentPreviewImage(
                attachmentUri = photo.photoUri,
                contentDescription = "Serviço ${idx + 1}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .size(52.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp)
                  )
                  .clickable { selectedPhotoIndex = idx }
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.WorkHistory,
              contentDescription = null,
              tint = EmeraldWallet,
              modifier = Modifier.size(15.dp)
            )
            Text(
              text = "${entry.completedJobs} serviços realizados",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
          if (entry.providerCity.isNotBlank()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
              Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
              )
              Text(
                text = entry.providerCity,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        if (entry.providerBio.isNotBlank()) {
          Text(
            text = entry.providerBio,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    confirmButton = {
      if (currentUserRole == UserRole.CLIENTE) {
        Button(
          onClick = onRequestServiceClick,
          modifier = Modifier.testTag("btn_promo_request_service")
        ) {
          Text("Solicitar Serviço")
        }
      } else if (currentUserRole == UserRole.PRESTADOR && onManageShowcaseClick != null) {
        Button(onClick = onManageShowcaseClick) {
          Text("Minha Vitrine")
        }
      } else {
        TextButton(onClick = onDismiss) {
          Text("Fechar")
        }
      }
    },
    dismissButton = {
      if (currentUserRole == UserRole.CLIENTE || (currentUserRole == UserRole.PRESTADOR && onManageShowcaseClick != null)) {
        TextButton(onClick = onDismiss) {
          Text("Fechar")
        }
      }
    }
  )

  if (showFullScreenPhoto) {
    ServicePhotoFullScreenDialog(
      title = activePhoto.title.ifBlank { "Serviço de ${entry.providerName}" },
      photoUri = activePhoto.photoUri,
      subtitleNote = "${entry.providerName} • Nota ${formatUserRating(entry.providerRating)}",
      onDismiss = { showFullScreenPhoto = false }
    )
  }
}
