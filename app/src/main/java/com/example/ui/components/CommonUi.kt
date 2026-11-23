package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import android.Manifest
import android.content.Context
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.ContactValidator
import com.example.data.CpfValidator
import com.example.data.CurrentCityLocationHelper
import com.example.data.CurrentCityResult
import com.example.data.DocumentAttachmentHelper
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.FilledTonalButton
import com.example.data.LocationAutocompleteMode
import com.example.data.ProviderShowcaseHelper
import com.example.data.RegionalPriceComparisonLevel
import com.example.data.RegionalPriceIndexEngine
import com.example.data.SupportLauncher
import com.example.model.AppNotification
import com.example.model.NotificationType
import com.example.model.ProviderVerificationStatus
import com.example.model.RegionalPriceEstimate
import com.example.model.ServiceRequest
import com.example.model.ServiceReview
import com.example.ui.ChangePasswordUiState
import com.example.ui.PendingRegistration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.data.InsufficientBalanceException
import com.example.model.ContactFilterResult
import com.example.model.Proposal
import com.example.model.ProposalStatus
import com.example.model.ServiceStatus
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight
import java.util.Locale

fun formatUserRating(rating: Double): String {
  val roundedTwo = Math.round(rating * 100.0) / 100.0
  return if ((roundedTwo * 10.0) % 1.0 == 0.0) {
    String.format(Locale.US, "%.1f", roundedTwo)
  } else {
    String.format(Locale.US, "%.2f", roundedTwo)
  }
}

@Composable
fun ProfileAvatar(
  name: String,
  photoUrl: String? = null,
  avatarColorHex: Long = 0xFF1E3A8A,
  size: Dp = 36.dp,
  contentDescription: String? = "Foto de perfil de $name",
  modifier: Modifier = Modifier
) {
  val initials = remember(name) {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    when {
      parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
      parts.size == 1 -> parts[0].take(2).uppercase()
      else -> "?"
    }
  }
  val fontSize = (size.value * 0.36f).coerceIn(10f, 22f).sp

  Box(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .background(Color(avatarColorHex))
      .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), CircleShape)
      .semantics {
        if (!contentDescription.isNullOrBlank()) {
          this.contentDescription = contentDescription
        }
      },
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = initials,
      color = Color.White,
      fontWeight = FontWeight.Bold,
      fontSize = fontSize
    )
    val bundledDrawableRes = remember(photoUrl) {
      ProviderShowcaseHelper.resolveBundledDrawableRes(photoUrl)
    }
    val isRemoteUrl = !photoUrl.isNullOrBlank() &&
      (photoUrl.startsWith("http://", ignoreCase = true) || photoUrl.startsWith("https://", ignoreCase = true))
    val localContext = LocalContext.current
    val localBitmap by produceState<ImageBitmap?>(initialValue = null, photoUrl) {
      value = if (photoUrl.isNullOrBlank() || bundledDrawableRes != null || isRemoteUrl) {
        null
      } else {
        withContext(Dispatchers.IO) {
          DocumentAttachmentHelper.decodeBitmap(localContext, photoUrl)?.asImageBitmap()
        }
      }
    }
    if (bundledDrawableRes != null) {
      Image(
        painter = painterResource(id = bundledDrawableRes),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
    } else if (isRemoteUrl) {
      AsyncImage(
        model = ImageRequest.Builder(localContext)
          .data(photoUrl)
          .crossfade(true)
          .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
    } else if (localBitmap != null) {
      Image(
        bitmap = localBitmap!!,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
    }
  }
}

@Composable
fun OrderParticipantsSection(
  service: ServiceRequest,
  latestProposal: Proposal? = null,
  modifier: Modifier = Modifier
) {
  val hasAcceptedProvider = !service.acceptedProviderId.isNullOrBlank() ||
    (
      !service.acceptedProviderName.isNullOrBlank() &&
        (service.status == ServiceStatus.ACEITO ||
          service.status == ServiceStatus.EM_EXECUCAO ||
          service.status == ServiceStatus.CONCLUIDO)
    )
  val providerName = if (hasAcceptedProvider) {
    service.acceptedProviderName?.takeIf { it.isNotBlank() } ?: latestProposal?.providerName ?: "Prestador"
  } else {
    "Aguardando escolha"
  }
  val providerRating = if (hasAcceptedProvider) {
    service.acceptedProviderRating ?: latestProposal?.providerRating ?: 5.0
  } else {
    null
  }
  val providerPhoto = if (hasAcceptedProvider) {
    service.acceptedProviderPhotoUrl
  } else {
    null
  }
  val providerColor = if (hasAcceptedProvider) {
    service.acceptedProviderAvatarColorHex ?: 0xFF0D9488
  } else {
    0xFF9CA3AF
  }
  val providerRoleLabel = "Prestador"

  Row(
    modifier = modifier
      .fillMaxWidth()
      .testTag("order_participants_${service.id}"),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    OrderParticipantCard(
      roleLabel = "Cliente",
      name = service.clientName,
      rating = service.clientRating,
      photoUrl = service.clientPhotoUrl,
      avatarColorHex = service.clientAvatarColorHex,
      accentColor = InfoBlue,
      isPlaceholder = false,
      modifier = Modifier
        .weight(1f)
        .testTag("order_client_badge_${service.id}")
    )

    OrderParticipantCard(
      roleLabel = providerRoleLabel,
      name = providerName,
      rating = providerRating,
      photoUrl = providerPhoto,
      avatarColorHex = providerColor,
      accentColor = if (hasAcceptedProvider) EmeraldWallet else MaterialTheme.colorScheme.onSurfaceVariant,
      isPlaceholder = !hasAcceptedProvider,
      modifier = Modifier
        .weight(1f)
        .testTag("order_provider_badge_${service.id}")
    )
  }
}

@Composable
fun OrderParticipantCard(
  roleLabel: String,
  name: String,
  rating: Double?,
  photoUrl: String?,
  avatarColorHex: Long,
  accentColor: Color,
  isPlaceholder: Boolean = false,
  modifier: Modifier = Modifier
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    shape = RoundedCornerShape(10.dp),
    modifier = modifier
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (isPlaceholder) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0xFFE5E7EB)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Sem prestador definido",
            tint = Color(0xFF6B7280),
            modifier = Modifier.size(18.dp)
          )
        }
      } else {
        ProfileAvatar(
          name = name,
          photoUrl = photoUrl,
          avatarColorHex = avatarColorHex,
          size = 32.dp,
          contentDescription = "Foto de perfil de $name"
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = roleLabel,
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
          ),
          color = accentColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = name,
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          ),
          color = if (isPlaceholder) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (!isPlaceholder && rating != null) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = "Nota",
              tint = WarningAmber,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
              text = formatUserRating(rating),
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        } else {
          Text(
            text = if (isPlaceholder) "Definido após aceite" else "Sem nota ainda",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

@Composable
fun AppTopBar(
  currentUser: User,
  unreadNotificationCount: Int = 0,
  onSwitchUserClick: () -> Unit,
  onOpenWalletClick: () -> Unit,
  onNotificationsClick: () -> Unit = {},
  onAdminClick: () -> Unit = {},
  onProfileClick: () -> Unit = {}
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    shadowElevation = 2.dp,
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Brand Logo & Title
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f, fill = false)
      ) {
        Image(
          painter = painterResource(id = R.drawable.resolveai_logo),
          contentDescription = "Logo ResolveAi Serviços",
          modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "ResolveAi",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Serviços & Negociação",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
            color = EmeraldWallet
          )
        }
      }

      // Profile & Role Chip with switch option
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (currentUser.role == UserRole.ADMIN) {
          // Admin panel trigger
          Surface(
            color = Color(0xFF1E3A8A),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .clickable { onAdminClick() }
              .padding(end = 6.dp)
              .testTag("top_bar_admin_chip")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = "Admin",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Painel",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        // Notifications Bell for Client & Provider alerts
        IconButton(
          onClick = onNotificationsClick,
          modifier = Modifier
            .padding(end = 4.dp)
            .testTag("top_bar_notifications_button")
        ) {
          BadgedBox(
            badge = {
              if (unreadNotificationCount > 0) {
                Badge(containerColor = DangerRed) { Text("$unreadNotificationCount") }
              }
            }
          ) {
            Icon(
              imageVector = Icons.Default.Notifications,
              contentDescription = "Notificações",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
          }
        }

        if (currentUser.role == UserRole.PRESTADOR) {
          // Wallet pill for provider
          Surface(
            color = EmeraldWalletLight,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .clickable { onOpenWalletClick() }
              .padding(end = 8.dp)
              .testTag("top_bar_wallet_chip")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = "Carteira",
                tint = EmeraldWallet,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "R$ ${String.format("%.2f", currentUser.walletBalance)}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = EmeraldWallet
              )
            }
          }
        }

        // User Avatar (tocar na foto abre os dados do perfil para edição; Admin mantém troca no ícone)
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable {
              if (currentUser.role == UserRole.ADMIN) {
                onSwitchUserClick()
              } else {
                onProfileClick()
              }
            }
            .testTag(if (currentUser.role == UserRole.ADMIN) "switch_user_top_button" else "btn_topbar_profile_avatar")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .clip(CircleShape)
                .clickable { onProfileClick() }
                .testTag("topbar_profile_photo")
            ) {
              ProfileAvatar(
                name = currentUser.name,
                photoUrl = currentUser.photoUrl,
                avatarColorHex = currentUser.avatarColorHex,
                size = 28.dp,
                contentDescription = stringResource(R.string.profile_avatar_open_cd)
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text(
                text = currentUser.name.split(" ").firstOrNull() ?: "",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = when (currentUser.role) {
                  UserRole.CLIENTE -> "Cliente"
                  UserRole.PRESTADOR -> "Prestador"
                  UserRole.ADMIN -> "Admin"
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = if (currentUser.role == UserRole.CLIENTE) InfoBlue else EmeraldWallet
              )
            }
            if (currentUser.role == UserRole.ADMIN) {
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Trocar usuário",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun StatusBadge(status: ServiceStatus) {
  val (bgColor, textColor, icon) = when (status) {
    ServiceStatus.ABERTO -> Triple(InfoBlueLight, InfoBlue, Icons.Default.Info)
    ServiceStatus.EM_NEGOCIACAO -> Triple(WarningAmberLight, WarningAmber, Icons.Default.Handshake)
    ServiceStatus.ACEITO -> Triple(EmeraldWalletLight, EmeraldWallet, Icons.Default.CheckCircle)
    ServiceStatus.EM_EXECUCAO -> Triple(Color(0xFFE0E7FF), Color(0xFF4338CA), Icons.Default.Handshake)
    ServiceStatus.CONCLUIDO -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), Icons.Default.CheckCircle)
    ServiceStatus.CANCELADO -> Triple(DangerRedLight, DangerRed, Icons.Default.Block)
  }

  Surface(
    color = bgColor,
    shape = RoundedCornerShape(8.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = textColor,
        modifier = Modifier.size(13.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = status.label,
        color = textColor,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
      )
    }
  }
}

@Composable
fun ProposalStatusBadge(status: ProposalStatus) {
  val (bgColor, textColor) = when (status) {
    ProposalStatus.ENVIADA -> Pair(InfoBlueLight, InfoBlue)
    ProposalStatus.CONTRAPROPOSTA_CLIENTE -> Pair(WarningAmberLight, WarningAmber)
    ProposalStatus.CONTRAPROPOSTA_PRESTADOR -> Pair(Color(0xFFEDE9FE), Color(0xFF6D28D9))
    ProposalStatus.ACEITA -> Pair(EmeraldWalletLight, EmeraldWallet)
    ProposalStatus.RECUSADA -> Pair(DangerRedLight, DangerRed)
  }

  Surface(
    color = bgColor,
    shape = RoundedCornerShape(6.dp)
  ) {
    Text(
      text = status.label,
      color = textColor,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
    )
  }
}

@Composable
fun ContactViolationCard(
  violation: ContactFilterResult,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = DangerRedLight),
    border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
    shape = RoundedCornerShape(12.dp),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.Top
    ) {
      Icon(
        imageVector = Icons.Default.Block,
        contentDescription = "Bloqueado",
        tint = DangerRed,
        modifier = Modifier.size(24.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = "⚠️ Troca de Contato Não Permitida",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = DangerRed
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = violation.reasonMessage ?: "Não é permitida a troca de contatos externos.",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF7F1D1D)
        )
        if (!violation.detectedSnippet.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Detectado: \"${violation.detectedSnippet}\" (${violation.violationType ?: "Dado proibido"})",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = DangerRed
          )
        }
      }
    }
  }
}

@Composable
fun InsufficientBalanceDialog(
  data: InsufficientBalanceException,
  viewerRole: UserRole = UserRole.PRESTADOR,
  onDismiss: () -> Unit,
  onRechargeClick: () -> Unit
) {
  val hideFeeForClient = viewerRole == UserRole.CLIENTE
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = null,
        tint = WarningAmber,
        modifier = Modifier.size(36.dp)
      )
    },
    title = {
      Text(
        text = if (hideFeeForClient) "Carteira do Prestador Indisponível" else "Saldo Mínimo Necessário (Acima de R$ 1,00)",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = if (hideFeeForClient) {
            "O prestador selecionado precisa atualizar o saldo da carteira antes de iniciar este atendimento."
          } else {
            "Para aceitar qualquer serviço, seu saldo deve estar acima de R$ 1,00. Caso a taxa (10%) seja maior que o saldo disponível, ele ficará negativo e será descontado automaticamente na próxima recarga."
          },
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center
        )
        if (!hideFeeForClient) {
          Spacer(modifier = Modifier.height(16.dp))

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Saldo mínimo exigido:", style = MaterialTheme.typography.bodySmall)
                Text(
                  "Acima de R$ 1,00",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                  color = EmeraldWallet
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Taxa deste serviço (10%):", style = MaterialTheme.typography.bodySmall)
                Text(
                  "R$ ${String.format("%.2f", data.requiredFee)}",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                  color = WarningAmber
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Saldo atual na carteira:", style = MaterialTheme.typography.bodySmall)
                Text(
                  text = if (data.currentBalance < 0.0) {
                    "-R$ ${String.format("%.2f", -data.currentBalance)}"
                  } else {
                    "R$ ${String.format("%.2f", data.currentBalance)}"
                  },
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                  color = if (data.currentBalance <= 1.0) DangerRed else MaterialTheme.colorScheme.onSurface
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Mínimo para liberar:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                Text(
                  "R$ ${String.format("%.2f", data.missingAmount)}",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                  color = DangerRed
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      if (!hideFeeForClient) {
        Button(
          onClick = onRechargeClick,
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
          modifier = Modifier.testTag("dialog_recharge_button")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Recarregar Carteira via Pix")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(if (hideFeeForClient) "Entendi" else "Cancelar")
      }
    }
  )
}

@Composable
fun UserSwitcherDialog(
  users: List<User>,
  currentUserId: String,
  onDismiss: () -> Unit,
  onSelectUser: (String) -> Unit,
  onOpenRegisterClick: () -> Unit = {}
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.SwapHoriz,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp)
      )
    },
    title = {
      Text(
        text = "Alternar ou Criar Conta",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Prominent button to register new user with SMS
        Button(
          onClick = {
            onDismiss()
            onOpenRegisterClick()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("btn_open_sms_register")
        ) {
          Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Novo Cadastro com SMS", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Ou alterne para uma conta existente (Área do Cliente ou Área do Prestador):",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))

        users.forEach { user ->
          val isSelected = user.id == currentUserId
          Card(
            colors = CardDefaults.cardColors(
              containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            ),
            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelectUser(user.id) }
              .testTag("switch_user_item_${user.id}")
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              ProfileAvatar(
                name = user.name,
                photoUrl = user.photoUrl,
                avatarColorHex = user.avatarColorHex,
                size = 36.dp
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                  )
                  if (user.isPhoneVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                      imageVector = Icons.Default.Verified,
                      contentDescription = "SMS Verificado",
                      tint = InfoBlue,
                      modifier = Modifier.size(14.dp)
                    )
                  }
                }
                Text(
                  text = when (user.role) {
                    UserRole.CLIENTE -> "Cliente • ${user.city}"
                    UserRole.ADMIN -> "Administrador do App • Análise e Aprovação"
                    UserRole.PRESTADOR -> {
                      if (user.verificationStatus == ProviderVerificationStatus.PENDENTE_ANALISE) {
                        "Prestador (Documentos em Análise pelo Administrador)"
                      } else {
                        "${user.category} • Carteira: R$ ${String.format("%.2f", user.walletBalance)}"
                      }
                    }
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = when (user.role) {
                    UserRole.ADMIN -> Color(0xFF1E3A8A)
                    UserRole.PRESTADOR -> if (user.verificationStatus == ProviderVerificationStatus.PENDENTE_ANALISE) WarningAmber else EmeraldWallet
                    UserRole.CLIENTE -> InfoBlue
                  }
                )
              }
              if (isSelected) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = "Ativo",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Fechar")
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateServiceDialog(
  onDismiss: () -> Unit,
  onCreate: (title: String, category: String, description: String, location: String, budget: Double, urgency: String, photoUri: String?, photoName: String?) -> Unit,
  onOpenSupport: (() -> Unit)? = null,
  onCreateWithMedia: ((
    title: String,
    category: String,
    description: String,
    location: String,
    budget: Double,
    urgency: String,
    photoUri: String?,
    photoName: String?,
    videoUri: String?,
    videoName: String?,
    videoThumbnailUri: String?,
    videoDurationMs: Long?
  ) -> Unit)? = null,
  availableCategories: List<String> = emptyList(),
  defaultUrgency: String = "Média",
  defaultCity: String = "",
  initialDetectedLocalCity: String = "",
  onLocalCityDetected: ((String) -> Unit)? = null,
  autoDetectLocalCityForRegionalPrice: Boolean = true,
  autoRequestLocalCityPermission: Boolean = false,
  estimateRegionalPrice: (category: String, locationText: String) -> RegionalPriceEstimate = { cat, loc ->
    RegionalPriceIndexEngine.estimate(category = cat, locationText = loc, fallbackCity = defaultCity)
  },
  syncDailyRegionalPrice: (suspend (category: String, locationText: String) -> RegionalPriceEstimate)? = null,
  detectLocalCityProvider: suspend (Context) -> CurrentCityResult = { ctx ->
    CurrentCityLocationHelper.detectCurrentCity(ctx)
  },
  detectAddressProvider: suspend (Context) -> CurrentCityResult = { ctx ->
    CurrentCityLocationHelper.detectCurrentAddress(ctx)
  }
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val categories = remember(availableCategories) {
    if (availableCategories.isNotEmpty()) {
      availableCategories.filter { it.isNotBlank() }.distinct()
    } else {
      listOf(
        "Cabeleireiro",
        "Manicure",
        "Eletricista",
        "Hidráulica & Encanador",
        "Pintura & Acabamento",
        "TI, Redes & Informática",
        "Limpeza & Diarista",
        "Montagem de Móveis",
        "Reformas & Alvenaria",
        "Fretes & Mudanças"
      )
    }
  }
  var title by remember { mutableStateOf("") }
  var category by remember(categories) {
    mutableStateOf(
      categories.firstOrNull { it.contains("Eletricista", ignoreCase = true) }
        ?: categories.firstOrNull()
        ?: "Eletricista"
    )
  }
  var description by remember { mutableStateOf("") }
  var location by remember { mutableStateOf("") }
  var detectedLocalCity by remember(initialDetectedLocalCity) {
    mutableStateOf(
      initialDetectedLocalCity.trim().ifBlank {
        CurrentCityLocationHelper.getCachedLocalCity(context).orEmpty()
      }
    )
  }
  var isDetectingLocalCity by remember { mutableStateOf(false) }
  var localCityErrorHint by remember { mutableStateOf<String?>(null) }
  var hasAttemptedAutoLocalCity by remember { mutableStateOf(false) }
  var isPendingAutoCityPermission by remember { mutableStateOf(false) }

  val effectiveRegionalLocation = location.trim().ifBlank { detectedLocalCity.trim() }

  var regionalEstimate by remember {
    mutableStateOf(estimateRegionalPrice(category, effectiveRegionalLocation))
  }
  var isSyncingRegionalPrice by remember { mutableStateOf(false) }
  var hasCustomBudget by remember { mutableStateOf(false) }
  var budgetText by remember {
    mutableStateOf(RegionalPriceIndexEngine.formatEditableDecimal(regionalEstimate.averagePrice))
  }
  var urgency by remember(defaultUrgency) {
    mutableStateOf(defaultUrgency.takeIf { it in listOf("Baixa", "Média", "Alta", "Urgente") } ?: "Média")
  }
  var photoUri by remember { mutableStateOf<String?>(null) }
  var photoName by remember { mutableStateOf("") }
  var videoUri by remember { mutableStateOf<String?>(null) }
  var videoName by remember { mutableStateOf("") }
  var videoThumbnailUri by remember { mutableStateOf<String?>(null) }
  var videoDurationMs by remember { mutableStateOf<Long?>(null) }
  var showSupportModal by remember { mutableStateOf(false) }

  val urgencies = listOf("Baixa", "Média", "Alta", "Urgente")

  var catExpanded by remember { mutableStateOf(false) }

  fun executeLocalCityLookup(isAutomatic: Boolean) {
    if (isDetectingLocalCity) return
    scope.launch {
      isDetectingLocalCity = true
      if (!isAutomatic) {
        localCityErrorHint = null
      }
      try {
        when (val res = detectLocalCityProvider(context)) {
          is CurrentCityResult.Success -> {
            val cleanCity = res.cityFormatted.trim()
            if (cleanCity.isNotBlank()) {
              detectedLocalCity = cleanCity
              CurrentCityLocationHelper.cacheLocalCity(context, cleanCity)
              onLocalCityDetected?.invoke(cleanCity)
              localCityErrorHint = null
            }
          }
          is CurrentCityResult.Error -> {
            if (!isAutomatic) {
              localCityErrorHint = res.message
            }
          }
        }
      } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        if (!isAutomatic) {
          localCityErrorHint = "Não foi possível obter sua cidade atual pelo GPS."
        }
      } finally {
        isDetectingLocalCity = false
      }
    }
  }

  val localCityPermissionDeniedMessage = stringResource(R.string.regional_price_location_permission_denied)
  val localCityPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    val wasAuto = isPendingAutoCityPermission
    isPendingAutoCityPermission = false
    if (granted) {
      executeLocalCityLookup(isAutomatic = wasAuto)
    } else if (!wasAuto) {
      localCityErrorHint = localCityPermissionDeniedMessage
    }
  }

  fun onManualDetectLocalCityClick() {
    localCityErrorHint = null
    isPendingAutoCityPermission = false
    if (CurrentCityLocationHelper.hasLocationPermission(context)) {
      executeLocalCityLookup(isAutomatic = false)
    } else {
      localCityPermissionLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_COARSE_LOCATION,
          Manifest.permission.ACCESS_FINE_LOCATION
        )
      )
    }
  }

  // Detecta automaticamente a cidade local do aparelho ao abrir "Solicitar Novo Serviço"
  // para puxar a média regional da cidade real do usuário (em vez de fixar São Paulo).
  LaunchedEffect(autoDetectLocalCityForRegionalPrice, autoRequestLocalCityPermission) {
    if (autoDetectLocalCityForRegionalPrice && !hasAttemptedAutoLocalCity) {
      hasAttemptedAutoLocalCity = true
      if (CurrentCityLocationHelper.hasLocationPermission(context)) {
        executeLocalCityLookup(isAutomatic = true)
      } else if (autoRequestLocalCityPermission && detectedLocalCity.isBlank()) {
        isPendingAutoCityPermission = true
        localCityPermissionLauncher.launch(
          arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
          )
        )
      }
    }
  }

  // Recalcula instantaneamente o valor médio regional sempre que mudar a categoria, o endereço ou a cidade local detectada
  LaunchedEffect(category, effectiveRegionalLocation, defaultCity) {
    val instant = estimateRegionalPrice(category, effectiveRegionalLocation)
    regionalEstimate = instant
    if (!hasCustomBudget) {
      budgetText = RegionalPriceIndexEngine.formatEditableDecimal(instant.averagePrice)
    }

    if (syncDailyRegionalPrice != null) {
      delay(250L)
      isSyncingRegionalPrice = true
      try {
        val synced = syncDailyRegionalPrice(category, effectiveRegionalLocation)
        regionalEstimate = synced
        if (!hasCustomBudget) {
          budgetText = RegionalPriceIndexEngine.formatEditableDecimal(synced.averagePrice)
        }
      } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
      } finally {
        isSyncingRegionalPrice = false
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp)
      .widthIn(max = 520.dp),
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Solicitar Novo Serviço",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Quick Support banner inside Request Service dialog
        Surface(
          color = Color(0xFFEFF6FF),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Default.SupportAgent,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "Dúvidas no pedido?",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary
                )
                Text(
                  text = "Nossa equipe pode te ajudar a estimar o valor.",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            OutlinedButton(
              onClick = {
                if (onOpenSupport != null) {
                  onOpenSupport()
                } else {
                  showSupportModal = true
                }
              },
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
              modifier = Modifier.testTag("btn_dialog_contact_support")
            ) {
              Icon(Icons.Default.Headset, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Falar com Suporte", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Título do Serviço") },
          placeholder = { Text("Ex: Troca de tomada e disjuntor") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_service_title")
        )

        // Category dropdown
        ExposedDropdownMenuBox(
          expanded = catExpanded,
          onExpandedChange = { catExpanded = it }
        ) {
          OutlinedTextField(
            value = category,
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoria") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
            modifier = Modifier
              .menuAnchor()
              .fillMaxWidth()
              .testTag("dropdown_service_category")
          )
          ExposedDropdownMenu(
            expanded = catExpanded,
            onDismissRequest = { catExpanded = false }
          ) {
            categories.forEach { cat ->
              DropdownMenuItem(
                text = { Text(cat) },
                onClick = {
                  category = cat
                  catExpanded = false
                }
              )
            }
          }
        }

        val parsedBudget = budgetText.replace(",", ".").toDoubleOrNull()
        val budgetComparison = remember(parsedBudget, regionalEstimate) {
          if (parsedBudget != null && parsedBudget > 0.0) {
            RegionalPriceIndexEngine.compareProposedToRegional(parsedBudget, regionalEstimate)
          } else {
            null
          }
        }
        val differsFromRegionalAverage = parsedBudget == null ||
          kotlin.math.abs(parsedBudget - regionalEstimate.averagePrice) >= 1.0

        OutlinedTextField(
          value = budgetText,
          onValueChange = { raw ->
            val sanitized = buildString {
              var hasSeparator = false
              for (ch in raw) {
                when {
                  ch.isDigit() -> append(ch)
                  (ch == '.' || ch == ',') && !hasSeparator -> {
                    append(ch)
                    hasSeparator = true
                  }
                }
              }
            }
            budgetText = sanitized
            hasCustomBudget = true
          },
          label = { Text("Valor Proposto Inicial (R$)") },
          placeholder = {
            Text("Média regional: ${RegionalPriceIndexEngine.formatEditableDecimal(regionalEstimate.averagePrice)}")
          },
          trailingIcon = {
            if (budgetText.isNotEmpty()) {
              IconButton(
                onClick = {
                  budgetText = ""
                  hasCustomBudget = true
                },
                modifier = Modifier.testTag("btn_clear_service_budget")
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = stringResource(R.string.regional_price_clear_budget_cd),
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_service_budget")
        )

        // Card de Medição Automática Regional (Atualizado Diariamente)
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("regional_price_benchmark_card")
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = Icons.Default.AutoGraph,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.regional_price_card_title),
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }

              Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("regional_price_daily_badge")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.Update,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = stringResource(
                      R.string.regional_price_daily_updated_format,
                      regionalEstimate.displayDate
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 10.sp,
                      fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = RegionalPriceIndexEngine.formatCurrencyBrl(regionalEstimate.averagePrice),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("regional_price_average_value")
              )

              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("regional_price_region_label")
              ) {
                Icon(
                  imageVector = Icons.Default.LocationOn,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                val regionDisplay = if (
                  regionalEstimate.macroRegion.isNotBlank() &&
                  regionalEstimate.macroRegion != "Nacional"
                ) {
                  "${regionalEstimate.regionLabel} • ${regionalEstimate.macroRegion}"
                } else {
                  regionalEstimate.regionLabel
                }
                Text(
                  text = regionDisplay,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            Text(
              text = stringResource(
                R.string.regional_price_range_format,
                RegionalPriceIndexEngine.formatCurrencyBrl(regionalEstimate.minPrice),
                RegionalPriceIndexEngine.formatCurrencyBrl(regionalEstimate.maxPrice)
              ) + " • ${regionalEstimate.sourceLabel}",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              val statusColor = when {
                !hasCustomBudget || !differsFromRegionalAverage -> MaterialTheme.colorScheme.primary
                budgetComparison?.level == RegionalPriceComparisonLevel.BELOW_AVERAGE -> Color(0xFFB45309)
                budgetComparison?.level == RegionalPriceComparisonLevel.ABOVE_AVERAGE -> Color(0xFF047857)
                else -> MaterialTheme.colorScheme.primary
              }
              val statusText = when {
                isDetectingLocalCity -> stringResource(R.string.regional_price_detecting_city)
                isSyncingRegionalPrice -> stringResource(R.string.regional_price_syncing_hint)
                !hasCustomBudget || !differsFromRegionalAverage -> stringResource(R.string.regional_price_auto_applied)
                budgetComparison?.level == RegionalPriceComparisonLevel.BELOW_AVERAGE ->
                  stringResource(R.string.regional_price_below_average_format, kotlin.math.abs(budgetComparison.percentDiff))
                budgetComparison?.level == RegionalPriceComparisonLevel.ABOVE_AVERAGE ->
                  stringResource(R.string.regional_price_above_average_format, budgetComparison.percentDiff)
                else -> stringResource(R.string.regional_price_within_average)
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .weight(1f)
                  .testTag("regional_price_status_badge")
              ) {
                if (isDetectingLocalCity || isSyncingRegionalPrice) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                  text = statusText,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                  ),
                  color = statusColor
                )
              }

              if (hasCustomBudget && differsFromRegionalAverage) {
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                  onClick = {
                    budgetText = RegionalPriceIndexEngine.formatEditableDecimal(regionalEstimate.averagePrice)
                    hasCustomBudget = false
                  },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier
                    .heightIn(min = 36.dp)
                    .testTag("btn_apply_regional_price")
                ) {
                  Text(
                    text = stringResource(
                      R.string.regional_price_apply_cta_format,
                      RegionalPriceIndexEngine.formatCurrencyBrl(regionalEstimate.averagePrice)
                    ),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            if (location.isBlank() && detectedLocalCity.isBlank() && !isDetectingLocalCity) {
              OutlinedButton(
                onClick = { onManualDetectLocalCityClick() },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 36.dp)
                  .testTag("btn_detect_regional_local_city")
              ) {
                Icon(
                  imageVector = Icons.Default.MyLocation,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.regional_price_use_local_city_cta),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            if (!localCityErrorHint.isNullOrBlank()) {
              Text(
                text = localCityErrorHint.orEmpty(),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("regional_price_local_city_error")
              )
            }
          }
        }

        GooglePlacesAutocompleteField(
          value = location,
          onValueChange = { location = it },
          mode = LocationAutocompleteMode.ADDRESS,
          label = stringResource(R.string.service_address_label),
          placeholder = stringResource(R.string.service_address_placeholder),
          shape = RoundedCornerShape(10.dp),
          fieldTestTag = "input_service_location",
          enableCurrentCityGps = true,
          autoDetectCurrentCity = false,
          detectCityProvider = detectAddressProvider
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Descrição detalhada do problema") },
          minLines = 3,
          maxLines = 5,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_service_description")
        )

        ServicePhotoAttachmentPickerCard(
          title = "Foto do Serviço (Opcional)",
          subtitle = "Anexe uma foto da galeria ou tire na hora com a câmera para ajudar os prestadores.",
          docKey = "service_order_photo",
          fileNamePrefix = "foto_pedido",
          photoUri = photoUri,
          photoName = photoName,
          galleryButtonTag = "btn_service_photo_gallery",
          cameraButtonTag = "btn_service_photo_camera",
          removeButtonTag = "btn_remove_service_photo",
          previewTag = "service_photo_preview",
          onPhotoChanged = { newUri, newName ->
            photoUri = newUri
            photoName = newName
          }
        )

        ServiceVideoAttachmentPickerCard(
          title = "Vídeo do Serviço (Opcional)",
          subtitle = "Anexe um vídeo da galeria ou grave na hora para mostrar melhor o problema.",
          docKey = "service_order_video",
          fileNamePrefix = "video_pedido",
          videoUri = videoUri,
          videoName = videoName,
          videoThumbnailUri = videoThumbnailUri,
          videoDurationMs = videoDurationMs,
          galleryButtonTag = "btn_service_video_gallery",
          cameraButtonTag = "btn_service_video_camera",
          removeButtonTag = "btn_remove_service_video",
          previewTag = "service_video_preview",
          onVideoChanged = { newUri, newName, newThumb, newDuration ->
            videoUri = newUri
            videoName = newName
            videoThumbnailUri = newThumb
            videoDurationMs = newDuration
          }
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val budget = budgetText.replace(",", ".").toDoubleOrNull() ?: regionalEstimate.averagePrice
          val resolvedLocation = location.trim().ifBlank { detectedLocalCity.trim() }
          if (title.isNotBlank()) {
            if (onCreateWithMedia != null) {
              onCreateWithMedia(
                title,
                category,
                description,
                resolvedLocation,
                budget,
                urgency,
                photoUri,
                photoName.takeIf { it.isNotBlank() },
                videoUri,
                videoName.takeIf { it.isNotBlank() },
                videoThumbnailUri,
                videoDurationMs
              )
            } else {
              onCreate(
                title,
                category,
                description,
                resolvedLocation,
                budget,
                urgency,
                photoUri,
                photoName.takeIf { it.isNotBlank() }
              )
            }
          }
        },
        enabled = title.isNotBlank() && (budgetText.replace(",", ".").toDoubleOrNull()?.let { it > 0.0 } == true),
        modifier = Modifier.testTag("btn_submit_create_service")
      ) {
        Text("Publicar Pedido")
      }
    },
    dismissButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(
          onClick = {
            if (onOpenSupport != null) {
              onOpenSupport()
            } else {
              showSupportModal = true
            }
          },
          modifier = Modifier.testTag("btn_dialog_support_footer")
        ) {
          Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Suporte ResolveAi", fontSize = 12.sp)
        }

        TextButton(onClick = onDismiss) {
          Text("Cancelar")
        }
      }
    }
  )

  if (showSupportModal) {
    SupportContactDialog(
      onDismiss = { showSupportModal = false }
    )
  }
}

@Composable
fun SupportContactDialog(
  onDismiss: () -> Unit,
  isLoginContext: Boolean = false
) {
  val context = LocalContext.current
  var sentMessage by remember { mutableStateOf(false) }
  var messageText by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.SupportAgent,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(36.dp)
      )
    },
    title = {
      Text(
        text = "Central de Suporte ResolveAi",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        if (!sentMessage) {
          Text(
            text = if (isLoginContext) {
              "Com dificuldades para entrar com sua conta Google, recuperar sua senha ou concluir seu cadastro?"
            } else {
              "Precisa de ajuda para cadastrar seu pedido, definir um orçamento justo ou tirar dúvidas da plataforma?"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Surface(
            color = Color(0xFFF3F4F6),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("⏰ Horário de Atendimento:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
              Text("Segunda a Sábado, das 08h às 20h (Horário de Brasília)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
              Text("⚡ Resposta direta pelo WhatsApp oficial", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = EmeraldWallet))
              Text("💬 WhatsApp Suporte: ${SupportLauncher.activeWhatsappDisplay}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
            }
          }

          OutlinedTextField(
            value = messageText,
            onValueChange = { messageText = it },
            label = { Text("Mensagem para enviar no WhatsApp") },
            placeholder = { Text("Ex: Olá, preciso de ajuda no aplicativo ResolveAi.") },
            minLines = 3,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_support_message")
          )
        } else {
          val protocolNumber = remember { (100000..999999).random() }
          Surface(
            color = Color(0xFFF0FDF4),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldWallet, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Chamado Aberto com Sucesso!", fontWeight = FontWeight.Bold, color = EmeraldWallet)
              }
              Text(
                "Protocolo: #SUP-$protocolNumber",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              )
              Text(
                text = "Conversa encaminhada para o WhatsApp oficial (${SupportLauncher.activeWhatsappDisplay}).",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
              )
            }
          }
        }
      }
    },
    confirmButton = {
      if (!sentMessage) {
        Button(
          onClick = {
            SupportLauncher.openWhatsAppSupport(context, messageText.ifBlank { null })
            sentMessage = true
          },
          modifier = Modifier.testTag("btn_send_support_ticket")
        ) {
          Text("Abrir WhatsApp")
        }
      } else {
        Button(onClick = onDismiss) {
          Text("Entendido")
        }
      }
    },
    dismissButton = {
      if (!sentMessage) {
        TextButton(onClick = onDismiss) {
          Text("Voltar")
        }
      }
    }
  )
}

@Composable
fun CounterOfferDialog(
  currentPrice: Double,
  currentUserRole: UserRole = UserRole.CLIENTE,
  onDismiss: () -> Unit,
  onConfirm: (newPrice: Double, note: String) -> Unit
) {
  var priceText by remember { mutableStateOf(String.format("%.2f", currentPrice)) }
  var noteText by remember { mutableStateOf("") }

  val parsedPrice = priceText.replace(",", ".").toDoubleOrNull() ?: 0.0
  val feeEstimate = parsedPrice * 0.10

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Handshake,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp)
      )
    },
    title = {
      Text(
        text = "Enviar Contraproposta",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Valor atual da negociação: R$ ${String.format("%.2f", currentPrice)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
          value = priceText,
          onValueChange = { priceText = it },
          label = { Text("Novo Valor Proposto (R$)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_counter_offer_price")
        )

        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it },
          label = { Text("Observação / Justificativa") },
          placeholder = { Text("Ex: Consigo fechar se for para começar amanhã.") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth()
        )

        if (currentUserRole != UserRole.CLIENTE) {
          // Fee reminder (apenas Prestador / Admin)
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Ao fechar em R$ ${String.format("%.2f", parsedPrice)}, a taxa de 10% da plataforma (R$ ${String.format("%.2f", feeEstimate)}) será debitada da carteira do prestador na aceitação.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (parsedPrice > 0) {
            onConfirm(parsedPrice, noteText)
          }
        },
        enabled = parsedPrice > 0,
        modifier = Modifier.testTag("btn_confirm_counter_offer")
      ) {
        Text("Enviar Contraproposta")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterUserDialog(
  onDismiss: () -> Unit,
  onConfirmRegister: (PendingRegistration) -> Unit,
  availableSpecialties: List<String> = emptyList()
) {
  val categories = remember(availableSpecialties) {
    if (availableSpecialties.isNotEmpty()) {
      availableSpecialties.filter { it.isNotBlank() }.distinct()
    } else {
      listOf(
        "Cabeleireiro",
        "Manicure",
        "Eletricista Residencial & Predial",
        "Hidráulica & Encanador",
        "Pintura & Acabamento",
        "TI, Redes & Informática",
        "Limpeza & Diarista",
        "Montagem de Móveis",
        "Reformas & Alvenaria",
        "Ar Condicionado & Refrigeração"
      )
    }
  }
  val context = LocalContext.current
  var selectedRole by remember { mutableStateOf(UserRole.CLIENTE) }
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var showPassword by remember { mutableStateOf(false) }
  var city by remember { mutableStateOf("") }
  var document by remember { mutableStateOf("") }
  var category by remember(categories) {
    mutableStateOf(categories.firstOrNull { it.contains("Eletricista", ignoreCase = true) } ?: categories.firstOrNull() ?: "Eletricista Residencial")
  }
  var bio by remember { mutableStateOf("") }
  var rgCnhFileName by remember { mutableStateOf("") }
  var rgCnhAttachmentUri by remember { mutableStateOf<String?>(null) }
  var criminalFileName by remember { mutableStateOf("") }
  var criminalAttachmentUri by remember { mutableStateOf<String?>(null) }
  var clientProfilePhotoUri by remember { mutableStateOf<String?>(null) }
  var providerCameraProfilePhotoUri by remember { mutableStateOf<String?>(null) }

  val rgPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      DocumentAttachmentHelper.processPickedAttachment(context, uri, "dlg_rg_cnh")?.let { processed ->
        rgCnhFileName = processed.fileName
        rgCnhAttachmentUri = processed.localFileUri
      }
    }
  }
  val criminalPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      DocumentAttachmentHelper.processPickedAttachment(context, uri, "dlg_criminal")?.let { processed ->
        criminalFileName = processed.fileName
        criminalAttachmentUri = processed.localFileUri
      }
    }
  }

  var catExpanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .widthIn(max = 520.dp)
      .testTag("register_user_dialog"),
    title = {
      Text(
        text = "Criar Cadastro no ResolveAi",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Role Switcher Cards
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            color = if (selectedRole == UserRole.CLIENTE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            border = if (selectedRole == UserRole.CLIENTE) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier
              .weight(1f)
              .clickable { selectedRole = UserRole.CLIENTE }
              .testTag("register_role_cliente")
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = Icons.Default.Handshake,
                contentDescription = null,
                tint = if (selectedRole == UserRole.CLIENTE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Sou Cliente",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (selectedRole == UserRole.CLIENTE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
              )
              Text("Contratar", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }

          Surface(
            color = if (selectedRole == UserRole.PRESTADOR) EmeraldWalletLight else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            border = if (selectedRole == UserRole.PRESTADOR) androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldWallet) else null,
            modifier = Modifier
              .weight(1f)
              .clickable { selectedRole = UserRole.PRESTADOR }
              .testTag("register_role_prestador")
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = if (selectedRole == UserRole.PRESTADOR) EmeraldWallet else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Sou Prestador",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (selectedRole == UserRole.PRESTADOR) EmeraldWallet else MaterialTheme.colorScheme.onSurface
              )
              Text("Trabalhar", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nome Completo") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_reg_name")
        )

        val dlgPhoneError = if (phone.isNotBlank()) ContactValidator.validatePhone(phone) else null
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Celular com DDD *") },
          placeholder = { Text("(11) 98765-4321") },
          leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          isError = dlgPhoneError != null,
          supportingText = {
            if (dlgPhoneError != null) {
              Text(dlgPhoneError, color = DangerRed)
            } else if (ContactValidator.isValidPhone(phone)) {
              Text("✓ Telefone válido", color = EmeraldWallet, fontWeight = FontWeight.SemiBold)
            }
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_reg_phone")
        )

        val dlgEmailError = if (email.isNotBlank()) ContactValidator.validateEmail(email) else null
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("E-mail obrigatório *") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          isError = dlgEmailError != null,
          supportingText = {
            if (dlgEmailError != null) {
              Text(dlgEmailError, color = DangerRed)
            } else if (ContactValidator.isValidEmail(email)) {
              Text("✓ E-mail válido", color = EmeraldWallet, fontWeight = FontWeight.SemiBold)
            }
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_reg_email")
        )

        val dlgPasswordError = if (password.isNotEmpty() && password.length < 6) {
          "A senha deve ter no mínimo 6 caracteres."
        } else {
          null
        }
        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text("Criar Senha (opcional)") },
          placeholder = { Text(stringResource(R.string.auth_password_min_length_hint)) },
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
          trailingIcon = {
            IconButton(
              onClick = { showPassword = !showPassword },
              modifier = Modifier.testTag("toggle_reg_password")
            ) {
              Icon(
                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (showPassword) "Ocultar senha" else "Mostrar senha"
              )
            }
          },
          visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          isError = dlgPasswordError != null,
          supportingText = {
            when {
              dlgPasswordError != null -> Text(dlgPasswordError, color = DangerRed)
              password.length >= 6 -> Text("✓ Senha definida para acesso", color = EmeraldWallet, fontWeight = FontWeight.SemiBold)
              else -> Text("Defina uma senha para acesso direto com e-mail/telefone.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_reg_password")
        )

        val dlgCpfDigits = document.filter { it.isDigit() }
        val isDlgCpfValid = CpfValidator.isValid(document)
        val dlgCpfError = if (dlgCpfDigits.isNotEmpty() && !isDlgCpfValid) {
          if (selectedRole == UserRole.PRESTADOR) {
            CpfValidator.validateProviderCpf(document)
          } else {
            CpfValidator.validateClientCpf(document)
          }
        } else {
          null
        }

        OutlinedTextField(
          value = document,
          onValueChange = { document = CpfValidator.sanitizeInput(it) },
          label = { Text(if (selectedRole == UserRole.PRESTADOR) "CPF obrigatório *" else "CPF *") },
          placeholder = { Text("000.000.000-00") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          isError = dlgCpfError != null,
          supportingText = {
            when {
              isDlgCpfValid -> Text(
                text = "✓ CPF válido: ${CpfValidator.formatCpf(document)}",
                color = EmeraldWallet,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("dlg_reg_cpf_status")
              )
              dlgCpfError != null -> Text(
                text = dlgCpfError,
                color = DangerRed,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("dlg_reg_cpf_status")
              )
              else -> Text(
                text = if (selectedRole == UserRole.PRESTADOR) {
                  "Obrigatório informar CPF válido (11 dígitos)."
                } else {
                  "Informe CPF válido (11 dígitos)."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("dlg_reg_cpf_status")
              )
            }
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_reg_document")
        )

        GooglePlacesAutocompleteField(
          value = city,
          onValueChange = { city = it },
          mode = LocationAutocompleteMode.CITY,
          label = "Cidade / UF",
          shape = RoundedCornerShape(10.dp),
          fieldTestTag = "input_reg_city",
          autoDetectCurrentCity = false,
          autoRequestLocationPermission = false
        )

        if (selectedRole == UserRole.CLIENTE) {
          AdminAttachProfilePhotoCard(
            photoUri = clientProfilePhotoUri,
            isRequired = false,
            onPhotoAttached = { clientProfilePhotoUri = it },
            onClearPhoto = { clientProfilePhotoUri = null }
          )
        }

        if (selectedRole == UserRole.PRESTADOR) {
          ExposedDropdownMenuBox(
            expanded = catExpanded,
            onExpandedChange = { catExpanded = it }
          ) {
            OutlinedTextField(
              value = category,
              onValueChange = {},
              readOnly = true,
              label = { Text("Especialidade Profissional") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
              modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
            )
            ExposedDropdownMenu(
              expanded = catExpanded,
              onDismissRequest = { catExpanded = false }
            ) {
              categories.forEach { cat ->
                DropdownMenuItem(
                  text = { Text(cat) },
                  onClick = {
                    category = cat
                    catExpanded = false
                  }
                )
              }
            }
          }

          OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio / Apresentação de Serviços") },
            placeholder = { Text("Ex: 5 anos de experiência com instalações...") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
          )

          AdminAttachProfilePhotoCard(
            photoUri = providerCameraProfilePhotoUri,
            isRequired = true,
            onPhotoAttached = { providerCameraProfilePhotoUri = it },
            onClearPhoto = { providerCameraProfilePhotoUri = null }
          )

          // Document Section for Providers
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "📄 Documentação Obrigatória para Aprovação:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
              )

              ProviderDocumentAttachmentCard(
                title = "Anexo 1: RG ou CNH *",
                subtitle = "Foto ou PDF do documento",
                fileName = rgCnhFileName,
                attachmentUri = rgCnhAttachmentUri,
                buttonLabel = "Anexar RG / CNH",
                buttonTag = "dlg_attach_rg_cnh",
                onAttachClick = { rgPicker.launch(arrayOf("image/*", "application/pdf")) },
                onClearClick = {
                  rgCnhFileName = ""
                  rgCnhAttachmentUri = null
                }
              )

              ProviderDocumentAttachmentCard(
                title = "Anexo 2: Certidão de Antecedentes *",
                subtitle = "Foto ou PDF da certidão",
                fileName = criminalFileName,
                attachmentUri = criminalAttachmentUri,
                buttonLabel = "Anexar Certidão",
                buttonTag = "dlg_attach_criminal",
                onAttachClick = { criminalPicker.launch(arrayOf("image/*", "application/pdf")) },
                onClearClick = {
                  criminalFileName = ""
                  criminalAttachmentUri = null
                }
              )

              Text(
                text = "⚠️ Um administrador do ResolveAi analisará seus documentos e anexos antes de liberar seu acesso aos serviços.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = WarningAmber
              )
            }
          }

          // Bonus welcome badge
          Surface(
            color = EmeraldWalletLight,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldWallet, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Bônus de R$ 50,00 na carteira concedido ao concluir o cadastro do prestador!",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = EmeraldWallet
              )
            }
          }
        }

        val isDlgPasswordValid = password.isEmpty() || password.length >= 6
        val dlgMissingItems = remember(
          selectedRole,
          name,
          email,
          phone,
          password,
          city,
          document,
          providerCameraProfilePhotoUri,
          rgCnhAttachmentUri,
          criminalAttachmentUri
        ) {
          val baseMissing = if (selectedRole == UserRole.PRESTADOR) {
            CpfValidator.providerMissingRequirements(
              name = name,
              requireName = true,
              email = email,
              phone = phone,
              requireEmailAndPhone = true,
              city = city,
              document = document,
              profilePhotoUri = providerCameraProfilePhotoUri,
              requireCameraProfilePhoto = false,
              requireAttachedProfilePhoto = true,
              rgOrCnhAttachmentUri = rgCnhAttachmentUri,
              criminalRecordAttachmentUri = criminalAttachmentUri
            )
          } else {
            CpfValidator.clientMissingRequirements(
              name = name,
              requireName = true,
              email = email,
              requireEmail = true,
              phone = phone,
              requirePhone = true,
              city = city,
              document = document
            )
          }
          buildList {
            addAll(baseMissing)
            if (password.isNotEmpty() && password.length < 6) {
              add("Senha com pelo menos 6 caracteres (ou deixe em branco)")
            }
          }
        }

        if (dlgMissingItems.isNotEmpty()) {
          Surface(
            color = WarningAmberLight,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("dlg_reg_requirements_banner")
          ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = "Pendente para liberar o cadastro:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF92400E)
              )
              Text(
                text = dlgMissingItems.joinToString("\n") { "• $it" },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = Color(0xFF78350F)
              )
            }
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Cadastro direto no painel administrativo, sem necessidade de verificação por SMS.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    },
    confirmButton = {
      val canConfirmRegister = (password.isEmpty() || password.length >= 6) && if (selectedRole == UserRole.PRESTADOR) {
        CpfValidator.providerMissingRequirements(
          name = name,
          requireName = true,
          email = email,
          phone = phone,
          requireEmailAndPhone = true,
          city = city,
          document = document,
          profilePhotoUri = providerCameraProfilePhotoUri,
          requireCameraProfilePhoto = false,
          requireAttachedProfilePhoto = true,
          rgOrCnhAttachmentUri = rgCnhAttachmentUri,
          criminalRecordAttachmentUri = criminalAttachmentUri
        ).isEmpty()
      } else {
        CpfValidator.clientMissingRequirements(
          name = name,
          requireName = true,
          email = email,
          requireEmail = true,
          phone = phone,
          requirePhone = true,
          city = city,
          document = document
        ).isEmpty()
      }
      Button(
        onClick = {
          if (canConfirmRegister) {
            val resolvedPhotoUri = if (selectedRole == UserRole.PRESTADOR) {
              DocumentAttachmentHelper.resolveForPersistence(context, providerCameraProfilePhotoUri)
            } else {
              DocumentAttachmentHelper.resolveForPersistence(context, clientProfilePhotoUri)
            }
            onConfirmRegister(
              PendingRegistration(
                name = name.trim(),
                email = email.trim(),
                phone = phone.trim(),
                role = selectedRole,
                category = if (selectedRole == UserRole.PRESTADOR) category else "",
                bio = bio.trim(),
                city = city.trim(),
                document = document.trim(),
                initialWalletBonus = if (selectedRole == UserRole.PRESTADOR) 50.0 else 0.0,
                rgOrCnhNumber = "",
                rgOrCnhFileName = if (selectedRole == UserRole.PRESTADOR) rgCnhFileName else "",
                rgOrCnhAttachmentUri = if (selectedRole == UserRole.PRESTADOR) DocumentAttachmentHelper.resolveForPersistence(context, rgCnhAttachmentUri) else null,
                criminalRecordNumber = "",
                criminalRecordFileName = if (selectedRole == UserRole.PRESTADOR) criminalFileName else "",
                criminalRecordAttachmentUri = if (selectedRole == UserRole.PRESTADOR) DocumentAttachmentHelper.resolveForPersistence(context, criminalAttachmentUri) else null,
                selfieFileName = "",
                selfieAttachmentUri = null,
                profilePhotoUri = resolvedPhotoUri,
                password = password.trim()
              )
            )
          }
        },
        enabled = canConfirmRegister,
        modifier = Modifier.testTag("btn_register_user_confirm")
      ) {
        Text("Cadastrar Usuário")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    }
  )
}



@Composable
fun OtpCodeInputField(
  code: String,
  onCodeChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  length: Int = 6,
  enabled: Boolean = true,
  isError: Boolean = false,
  autoFocus: Boolean = true,
  testTag: String = "auth_input_sms_code",
  onDone: () -> Unit = {}
) {
  val focusRequester = remember { FocusRequester() }
  var isFocused by remember { mutableStateOf(false) }

  LaunchedEffect(autoFocus) {
    if (autoFocus) {
      focusRequester.requestFocus()
    }
  }

  BasicTextField(
    value = code,
    onValueChange = { raw ->
      val filtered = raw.filter { it.isDigit() }.take(length)
      onCodeChange(filtered)
    },
    enabled = enabled,
    singleLine = true,
    cursorBrush = SolidColor(Color.Transparent),
    keyboardOptions = KeyboardOptions(
      keyboardType = KeyboardType.NumberPassword,
      imeAction = ImeAction.Done
    ),
    keyboardActions = KeyboardActions(
      onDone = { onDone() }
    ),
    modifier = modifier
      .fillMaxWidth()
      .focusRequester(focusRequester)
      .onFocusChanged { isFocused = it.isFocused }
      .semantics { contentDescription = "Campo para código OTP de $length dígitos" }
      .testTag(testTag),
    decorationBox = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(enabled = enabled) { focusRequester.requestFocus() },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        repeat(length) { index ->
          val char = code.getOrNull(index)?.toString().orEmpty()
          val isCurrentSlot = isFocused && index == code.length.coerceAtMost(length - 1)
          val isFilled = char.isNotEmpty()
          val borderColor = when {
            isError -> DangerRed
            isCurrentSlot -> MaterialTheme.colorScheme.primary
            isFilled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            else -> MaterialTheme.colorScheme.outlineVariant
          }
          val bgColor = when {
            isError -> DangerRedLight.copy(alpha = 0.65f)
            isFilled -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
          }

          Surface(
            color = bgColor,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
              width = if (isCurrentSlot || isError) 2.dp else 1.2.dp,
              color = borderColor
            ),
            modifier = Modifier
              .weight(1f)
              .height(54.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = if (isFilled) char else if (isCurrentSlot) "•" else "",
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 22.sp
                ),
                color = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
              )
            }
          }
        }
      }
    }
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServiceRatingDialog(
  service: ServiceRequest,
  onDismiss: () -> Unit,
  onSubmit: (rating: Float, comment: String, complimentTags: List<String>) -> Unit,
  initialRating: Float = 5.0f
) {
  var rating by remember(service.id, initialRating) { mutableStateOf(initialRating.coerceIn(1f, 5f)) }
  var comment by remember(service.id) { mutableStateOf("") }
  var selectedTags by remember(service.id) { mutableStateOf(emptySet<String>()) }

  val availableTags = listOf(
    "Pontual",
    "Preço Justo",
    "Caprichoso",
    "Limpeza Impecável",
    "Profissional Educado",
    "Trabalho Rápido",
    "Super Recomendo"
  )

  val sentimentLabel = when (rating.toInt().coerceIn(1, 5)) {
    1 -> "Muito insatisfeito (1 estrela)"
    2 -> "Insatisfeito (2 estrelas)"
    3 -> "Regular (3 estrelas)"
    4 -> "Muito Bom (4 estrelas)"
    else -> "Excelente atendimento! (5 estrelas)"
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp)
      .widthIn(max = 520.dp)
      .testTag("service_rating_dialog"),
    icon = {
      Icon(
        imageVector = Icons.Default.Star,
        contentDescription = null,
        tint = WarningAmber,
        modifier = Modifier.size(34.dp)
      )
    },
    title = {
      Text(
        text = "Avaliar Atendimento",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            ProfileAvatar(
              name = service.acceptedProviderName ?: "Prestador",
              photoUrl = service.acceptedProviderPhotoUrl,
              avatarColorHex = service.acceptedProviderAvatarColorHex ?: 0xFF0D9488,
              size = 42.dp
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = service.acceptedProviderName ?: "Profissional",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = service.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        // 5 Interactive Stars (1 to 5, >=48dp touch targets)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          (1..5).forEach { star ->
            val isFilled = star <= rating.toInt()
            val isSelectedExact = star == rating.toInt()
            val starScale by animateFloatAsState(
              targetValue = if (isSelectedExact) 1.16f else if (isFilled) 1.04f else 0.94f,
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
              ),
              label = "star_scale_$star"
            )
            IconButton(
              onClick = { rating = star.toFloat() },
              modifier = Modifier
                .size(48.dp)
                .testTag("star_rate_$star")
            ) {
              Icon(
                imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "$star estrelas",
                tint = if (isFilled) WarningAmber else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                  .size(34.dp)
                  .graphicsLayer {
                    scaleX = starScale
                    scaleY = starScale
                  }
              )
            }
          }
        }

        Surface(
          color = WarningAmberLight,
          shape = RoundedCornerShape(20.dp)
        ) {
          Text(
            text = "${rating.toInt()} / 5 • $sentimentLabel",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF92400E),
            modifier = Modifier
              .padding(horizontal = 12.dp, vertical = 5.dp)
              .testTag("rating_sentiment_badge")
          )
        }

        Text(
          text = "Destaques do serviço (opcional):",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.align(Alignment.Start)
        )

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          availableTags.forEach { tag ->
            val isSelected = selectedTags.contains(tag)
            Surface(
              color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
              border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier
                .heightIn(min = 36.dp)
                .clickable {
                  selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag
                }
                .testTag("tag_rating_$tag")
            ) {
              Box(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = if (isSelected) "✓ $tag" else tag,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                  ),
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = comment,
          onValueChange = { if (it.length <= 1000) comment = it },
          label = { Text("Comentário sobre o serviço (opcional)") },
          placeholder = { Text("Conte como foi a execução, pontualidade e resultado...") },
          supportingText = {
            Text(
              text = "${comment.length}/1000",
              style = MaterialTheme.typography.labelSmall
            )
          },
          minLines = 3,
          maxLines = 5,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_rating_comment")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSubmit(rating.coerceIn(1f, 5f), comment.trim(), selectedTags.toList())
        },
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("btn_submit_rating")
      ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Enviar Avaliação", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.heightIn(min = 48.dp)
      ) {
        Text("Cancelar")
      }
    }
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewItemCard(
  review: ServiceReview,
  canReply: Boolean = false,
  onReply: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  var showReplyField by remember(review.id) { mutableStateOf(false) }
  var replyText by remember(review.id) { mutableStateOf("") }
  val formattedDate = remember(review.createdAt) {
    java.text.SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
      .format(java.util.Date(review.createdAt))
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("review_card_${review.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          ProfileAvatar(
            name = review.clientName,
            photoUrl = review.clientPhotoUrl,
            avatarColorHex = 0xFF2563EB,
            size = 34.dp
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = review.clientName,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = buildString {
                if (review.serviceCategory.isNotBlank()) append("${review.serviceCategory} • ")
                append(review.serviceTitle.ifBlank { "Serviço ResolveAi" })
              },
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
          Surface(
            color = Color(0xFFFEF3C7),
            shape = RoundedCornerShape(8.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "${review.rating} estrelas",
                tint = WarningAmber,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = formatUserRating(review.rating.toDouble()),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF78350F)
              )
            }
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = formattedDate,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Visual 1..5 stars row
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        val filledStars = kotlin.math.round(review.rating).toInt().coerceIn(1, 5)
        (1..5).forEach { star ->
          Icon(
            imageVector = if (star <= filledStars) Icons.Default.Star else Icons.Default.StarBorder,
            contentDescription = null,
            tint = if (star <= filledStars) WarningAmber else MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      // Compliment tags
      if (review.complimentTags.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          review.complimentTags.forEach { tag ->
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = "✓ $tag",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      if (review.comment.isNotBlank()) {
        Text(
          text = "\"${review.comment}\"",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface
        )
      } else {
        Text(
          text = "Avaliação registrada com estrelas sem comentário adicional.",
          style = MaterialTheme.typography.bodySmall.copy(
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Provider Reply if present
      if (!review.providerReply.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          color = EmeraldWalletLight.copy(alpha = 0.65f),
          border = BorderStroke(1.dp, EmeraldWallet.copy(alpha = 0.3f)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "Resposta de ${review.providerName.ifBlank { "Prestador" }}:",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = EmeraldWallet
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = review.providerReply,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      } else if (canReply) {
        if (!showReplyField) {
          TextButton(
            onClick = { showReplyField = true },
            modifier = Modifier
              .align(Alignment.End)
              .testTag("btn_reply_review_${review.id}")
          ) {
            Text("Responder Avaliação", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }
        } else {
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = replyText,
              onValueChange = { if (it.length <= 500) replyText = it },
              placeholder = { Text("Agradeça o cliente...") },
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("input_review_reply_${review.id}")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                val trimmed = replyText.trim()
                if (trimmed.isNotBlank()) {
                  onReply(trimmed)
                  showReplyField = false
                }
              },
              enabled = replyText.isNotBlank(),
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("btn_send_review_reply_${review.id}")
            ) {
              Text("Enviar")
            }
          }
        }
      }
    }
  }
}

@Composable
fun ProviderReviewsSummaryCard(
  averageRating: Double,
  reviews: List<ServiceReview>,
  declaredCount: Int = reviews.size,
  modifier: Modifier = Modifier
) {
  val effectiveCount = maxOf(reviews.size, declaredCount)
  val effectiveAverage = if (reviews.isNotEmpty()) {
    Math.round(reviews.map { it.rating.toDouble() }.average() * 100.0) / 100.0
  } else {
    averageRating
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.35f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("provider_reviews_summary_card")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp)
      ) {
        Text(
          text = if (effectiveCount > 0) formatUserRating(effectiveAverage) else "5.0",
          style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
          val fullStars = kotlin.math.round(effectiveAverage).toInt().coerceIn(1, 5)
          (1..5).forEach { s ->
            Icon(
              imageVector = if (s <= fullStars) Icons.Default.Star else Icons.Default.StarBorder,
              contentDescription = null,
              tint = WarningAmber,
              modifier = Modifier.size(14.dp)
            )
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = if (effectiveCount == 1) "1 avaliação" else "$effectiveCount avaliações",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        val total = reviews.size.coerceAtLeast(1)
        (5 downTo 1).forEach { star ->
          val countForStar = reviews.count { kotlin.math.round(it.rating).toInt().coerceIn(1, 5) == star }
          val fraction = if (reviews.isEmpty()) 0f else countForStar.toFloat() / total.toFloat()
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "$star★",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.width(22.dp)
            )
            LinearProgressIndicator(
              progress = { fraction },
              color = WarningAmber,
              trackColor = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
            )
            Text(
              text = "$countForStar",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.width(18.dp),
              textAlign = TextAlign.End
            )
          }
        }
      }
    }
  }
}

@Composable
fun ProviderReviewsDialog(
  providerName: String,
  providerCategory: String = "",
  providerPhotoUrl: String? = null,
  providerAvatarColorHex: Long = 0xFF0D9488,
  averageRating: Double,
  reviews: List<ServiceReview>,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .widthIn(max = 560.dp)
      .testTag("provider_reviews_dialog"),
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          ProfileAvatar(
            name = providerName,
            photoUrl = providerPhotoUrl,
            avatarColorHex = providerAvatarColorHex,
            size = 40.dp
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = providerName,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (providerCategory.isNotBlank()) {
              Text(
                text = providerCategory,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Fechar avaliações")
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        ProviderReviewsSummaryCard(
          averageRating = averageRating,
          reviews = reviews
        )

        if (reviews.isEmpty()) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Este prestador ainda não possui avaliações publicadas.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(14.dp)
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 320.dp)
              .testTag("provider_reviews_dialog_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(reviews, key = { it.id }) { review ->
              ReviewItemCard(review = review)
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.heightIn(min = 48.dp)
      ) {
        Text("Fechar", fontWeight = FontWeight.Bold)
      }
    }
  )
}

@Composable
fun EditServiceBudgetDialog(
  service: ServiceRequest,
  onDismiss: () -> Unit,
  onConfirm: (Double) -> Unit
) {
  var budgetText by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", service.proposedBudget)) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Edit,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp)
      )
    },
    title = {
      Text(
        text = "Mudar Valor Proposto",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = service.title,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = "Defina um novo valor proposto para o serviço. Prestadores verão a alteração ao enviar ou negociar propostas.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
          value = budgetText,
          onValueChange = { budgetText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
          label = { Text("Novo Orçamento Proposto (R$)") },
          placeholder = { Text("Ex: 350.00") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_edit_budget")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val newBudget = budgetText.replace(",", ".").toDoubleOrNull()
          if (newBudget != null && newBudget > 0) {
            onConfirm(newBudget)
          }
        },
        modifier = Modifier.testTag("btn_confirm_edit_budget")
      ) {
        Text("Atualizar Valor")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    }
  )
}

@Composable
fun NotificationsDialog(
  notifications: List<AppNotification>,
  onDismiss: () -> Unit,
  onMarkRead: (String) -> Unit,
  onOpenService: (String) -> Unit,
  onOpenChat: (String) -> Unit = onOpenService,
  onMarkAllRead: () -> Unit = {}
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Notifications,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(36.dp)
      )
    },
    title = {
      Text(
        text = "Notificações ResolveAi",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Acompanhe novos pedidos, propostas, aceitações, mensagens do chat e atualizações em tempo real:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (notifications.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(140.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Nenhuma notificação no momento.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(notifications, key = { it.id }) { notif ->
              val opensChat = notif.type == com.example.model.NotificationType.NOVA_MENSAGEM ||
                notif.type == com.example.model.NotificationType.PROPOSTA_ACEITA
              Card(
                colors = CardDefaults.cardColors(
                  containerColor = if (!notif.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(10.dp),
                border = if (!notif.isRead) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    onMarkRead(notif.id)
                    val sId = notif.serviceId
                    if (sId != null) {
                      if (opensChat) {
                        onOpenChat(sId)
                      } else {
                        onOpenService(sId)
                      }
                      onDismiss()
                    }
                  }
                  .testTag("notification_item_${notif.id}")
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = notif.title,
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.weight(1f)
                    )
                    if (!notif.isRead) {
                      Spacer(modifier = Modifier.width(6.dp))
                      Surface(color = DangerRed, shape = CircleShape) {
                        Box(modifier = Modifier.size(8.dp))
                      }
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = notif.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  if (notif.serviceId != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      text = if (opensChat) "💬 Toque para abrir o chat" else "👉 Toque para abrir este serviço",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = EmeraldWallet
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
    dismissButton = if (notifications.any { !it.isRead }) {
      {
        TextButton(onClick = onMarkAllRead) {
          Text("Marcar lidas")
        }
      }
    } else null,
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Fechar")
      }
    }
  )
}

@Composable
fun AdminReviewDialog(
  users: List<User>,
  onDismiss: () -> Unit,
  onApprove: (String) -> Unit,
  onReject: (String, String) -> Unit
) {
  val pendingProviders = users.filter {
    it.role == UserRole.PRESTADOR && it.verificationStatus == ProviderVerificationStatus.PENDENTE_ANALISE
  }
  val approvedCount = users.count { it.role == UserRole.PRESTADOR && it.verificationStatus == ProviderVerificationStatus.APROVADO }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Security,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(36.dp)
      )
    },
    title = {
      Text(
        text = "Administração ResolveAi",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Análise de Documentos de Prestadores (RG/CNH e Antecedentes):",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Surface(
            color = WarningAmberLight,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Text("${pendingProviders.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = WarningAmber)
              Text("Pendentes", fontSize = 10.sp, color = Color(0xFF78350F))
            }
          }
          Surface(
            color = EmeraldWalletLight,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Text("$approvedCount", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EmeraldWallet)
              Text("Aprovados", fontSize = 10.sp, color = Color(0xFF065F46))
            }
          }
        }

        if (pendingProviders.isEmpty()) {
          Surface(
            color = Color(0xFFDCFCE7),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldWallet, modifier = Modifier.size(28.dp))
              Spacer(modifier = Modifier.height(6.dp))
              Text("Nenhum cadastro pendente!", fontWeight = FontWeight.Bold, color = EmeraldWallet, fontSize = 12.sp)
              Text("Todos os documentos de prestadores foram aprovados com sucesso.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(300.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(pendingProviders, key = { it.id }) { provider ->
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber),
                modifier = Modifier.fillMaxWidth().testTag("admin_pending_provider_${provider.id}")
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(provider.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Surface(color = WarningAmberLight, shape = RoundedCornerShape(6.dp)) {
                      Text("Pendente", color = WarningAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                  }
                  Text("Especialidade: ${provider.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                  Text("Celular SMS: ${provider.phone} • ${provider.city}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                  // Documents submitted
                  val docs = provider.documents
                  Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      Text("📋 Documentação Anexada:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      Text(
                        "• RG/CNH: ${docs?.rgOrCnhNumber?.takeIf { it.isNotBlank() } ?: provider.document.ifBlank { "RG/CNH anexado" }} (${docs?.rgOrCnhFileName?.ifBlank { "cnh.pdf" } ?: "cnh.pdf"})" +
                          if (!docs?.rgOrCnhAttachmentUri.isNullOrBlank()) " • Anexo ✓" else "",
                        fontSize = 10.sp
                      )
                      if (!docs?.rgOrCnhAttachmentUri.isNullOrBlank()) {
                        DocumentAttachmentPreviewImage(
                          attachmentUri = docs?.rgOrCnhAttachmentUri,
                          contentDescription = "Anexo RG ou CNH de ${provider.name}",
                          modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(6.dp))
                        )
                      }
                      Text(
                        "• Antecedentes: ${docs?.criminalRecordCertNumber?.takeIf { it.isNotBlank() } ?: "Certidão anexada"} (${docs?.criminalRecordFileName?.ifBlank { "certidao.pdf" } ?: "certidao.pdf"})" +
                          if (!docs?.criminalRecordAttachmentUri.isNullOrBlank()) " • Anexo ✓" else "",
                        fontSize = 10.sp
                      )
                      if (!docs?.criminalRecordAttachmentUri.isNullOrBlank()) {
                        DocumentAttachmentPreviewImage(
                          attachmentUri = docs?.criminalRecordAttachmentUri,
                          contentDescription = "Anexo Antecedentes de ${provider.name}",
                          modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(6.dp))
                        )
                      }
                      if (!docs?.selfieAttachmentUri.isNullOrBlank()) {
                        Text(
                          "• Selfie (legado): ${docs?.selfieFileName ?: "selfie.jpg"} • Anexo ✓",
                          fontSize = 10.sp,
                          color = EmeraldWallet,
                          fontWeight = FontWeight.SemiBold
                        )
                        DocumentAttachmentPreviewImage(
                          attachmentUri = docs?.selfieAttachmentUri,
                          contentDescription = "Anexo Selfie de ${provider.name}",
                          modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(6.dp))
                        )
                      }
                    }
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    OutlinedButton(
                      onClick = { onReject(provider.id, "Documento com foto ilegível") },
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                      modifier = Modifier.weight(1f)
                    ) {
                      Text("Reprovar", fontSize = 11.sp)
                    }
                    Button(
                      onClick = { onApprove(provider.id) },
                      colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
                      modifier = Modifier.weight(1f).testTag("btn_approve_provider_${provider.id}")
                    ) {
                      Text("Aprovar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Fechar")
      }
    }
  )
}

@Composable
fun ProviderPendingApprovalScreen(
  currentUser: User,
  onSwitchUserClick: () -> Unit = {},
  onFastApproveClick: () -> Unit,
  onLogoutClick: () -> Unit = {}
) {
  val isRejected = currentUser.verificationStatus == ProviderVerificationStatus.REJEITADO
  val rejectionReason = currentUser.documents?.rejectionReason

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Surface(
      color = if (isRejected) DangerRedLight else WarningAmberLight,
      shape = CircleShape,
      modifier = Modifier.size(72.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Default.Security,
          contentDescription = null,
          tint = if (isRejected) DangerRed else WarningAmber,
          modifier = Modifier.size(40.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = if (isRejected) "Cadastro Reprovado pelo Administrador" else "Cadastro em Análise de Segurança",
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      textAlign = TextAlign.Center,
      color = if (isRejected) DangerRed else MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = if (isRejected) {
        "Olá, ${currentUser.name}. Sua análise documental no Firestore foi reprovada pelo administrador."
      } else {
        "Olá, ${currentUser.name}! Seus documentos pessoais (RG/CNH e Certidão de Antecedentes Criminais) estão sob análise da equipe de administração do ResolveAi."
      },
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (isRejected && !rejectionReason.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(12.dp))
      Surface(
        color = DangerRedLight,
        border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Motivo informado pelo Administrador:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DangerRed)
          Text(rejectionReason, fontSize = 12.sp, color = Color(0xFF991B1B))
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Documentos Enviados:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("• Documento: ${currentUser.documents?.rgOrCnhNumber?.takeIf { it.isNotBlank() } ?: currentUser.document.ifBlank { "RG/CNH anexado" }}", fontSize = 11.sp)
        Text("• Certidão: ${currentUser.documents?.criminalRecordCertNumber?.takeIf { it.isNotBlank() } ?: "Certidão anexada"}", fontSize = 11.sp)
        if (!currentUser.documents?.selfieAttachmentUri.isNullOrBlank()) {
          Text("• Foto Selfie com documento anexada ✓", fontSize = 11.sp, color = EmeraldWallet, fontWeight = FontWeight.SemiBold)
        }
        Text("• Telefone verificado por SMS: ${currentUser.phone} ✓", fontSize = 11.sp, color = InfoBlue, fontWeight = FontWeight.SemiBold)
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Button(
      onClick = onFastApproveClick,
      colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.fillMaxWidth().testTag("btn_fast_approve_test")
    ) {
      Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("Aprovar como Administrador (Teste Rápido)", fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(
      onClick = onLogoutClick,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("btn_pending_provider_logout")
    ) {
      Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("Sair da Conta")
    }
  }
}

@Composable
fun ChangeOwnPasswordDialog(
  currentUser: User,
  state: ChangePasswordUiState,
  onDismiss: () -> Unit,
  onClearState: () -> Unit = {},
  onConfirmChangePassword: (currentPassword: String, newPassword: String, confirmNewPassword: String) -> Unit
) {
  var currentPassword by remember { mutableStateOf("") }
  var newPassword by remember { mutableStateOf("") }
  var confirmNewPassword by remember { mutableStateOf("") }
  var showCurrentPassword by remember { mutableStateOf(false) }
  var showNewPassword by remember { mutableStateOf(false) }
  var showConfirmPassword by remember { mutableStateOf(false) }
  var localError by remember { mutableStateOf<String?>(null) }

  val isLoading = state is ChangePasswordUiState.Loading
  val remoteError = (state as? ChangePasswordUiState.Error)?.message
  val activeError = localError ?: remoteError

  val isCurrentValid = currentPassword.isNotBlank()
  val isNewLengthValid = newPassword.length >= 6
  val isNewDifferent = newPassword.isNotEmpty() && newPassword != currentPassword
  val isConfirmMatch = confirmNewPassword.isNotEmpty() && confirmNewPassword == newPassword
  val canSubmit = isCurrentValid && isNewLengthValid && isNewDifferent && isConfirmMatch && !isLoading

  AlertDialog(
    onDismissRequest = { if (!isLoading) onDismiss() },
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .widthIn(max = 520.dp)
      .testTag("change_own_password_dialog"),
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.LockReset,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary
        )
        Text(
          text = stringResource(R.string.change_password_dialog_title),
          fontWeight = FontWeight.Bold
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "${currentUser.name} • ${currentUser.role.label}",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            val contactSummary = currentUser.email.ifBlank { currentUser.phone }
            if (contactSummary.isNotBlank()) {
              Text(
                text = contactSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        OutlinedTextField(
          value = currentPassword,
          onValueChange = {
            currentPassword = it
            localError = null
            if (state is ChangePasswordUiState.Error) onClearState()
          },
          label = { Text(stringResource(R.string.change_password_current_label)) },
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
          trailingIcon = {
            IconButton(
              onClick = { showCurrentPassword = !showCurrentPassword },
              modifier = Modifier.testTag("toggle_current_password")
            ) {
              Icon(
                imageVector = if (showCurrentPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (showCurrentPassword) "Ocultar senha atual" else "Mostrar senha atual"
              )
            }
          },
          visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
          singleLine = true,
          enabled = !isLoading,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_current_password")
        )

        OutlinedTextField(
          value = newPassword,
          onValueChange = {
            newPassword = it
            localError = null
            if (state is ChangePasswordUiState.Error) onClearState()
          },
          label = { Text(stringResource(R.string.change_password_new_label)) },
          leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
          trailingIcon = {
            IconButton(
              onClick = { showNewPassword = !showNewPassword },
              modifier = Modifier.testTag("toggle_new_password")
            ) {
              Icon(
                imageVector = if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (showNewPassword) "Ocultar nova senha" else "Mostrar nova senha"
              )
            }
          },
          visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
          singleLine = true,
          enabled = !isLoading,
          isError = newPassword.isNotEmpty() && (!isNewLengthValid || !isNewDifferent),
          supportingText = {
            when {
              newPassword.isEmpty() -> Text(stringResource(R.string.auth_password_min_length_hint))
              !isNewLengthValid -> Text(
                text = "A nova senha deve ter pelo menos 6 caracteres.",
                color = MaterialTheme.colorScheme.error
              )
              !isNewDifferent -> Text(
                text = stringResource(R.string.change_password_same_error),
                color = MaterialTheme.colorScheme.error
              )
              else -> Text(
                text = "Nova senha válida ✓",
                color = EmeraldWallet,
                fontWeight = FontWeight.SemiBold
              )
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_new_password")
        )

        OutlinedTextField(
          value = confirmNewPassword,
          onValueChange = {
            confirmNewPassword = it
            localError = null
            if (state is ChangePasswordUiState.Error) onClearState()
          },
          label = { Text(stringResource(R.string.change_password_confirm_label)) },
          leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
          trailingIcon = {
            IconButton(
              onClick = { showConfirmPassword = !showConfirmPassword },
              modifier = Modifier.testTag("toggle_confirm_new_password")
            ) {
              Icon(
                imageVector = if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (showConfirmPassword) "Ocultar confirmação de senha" else "Mostrar confirmação de senha"
              )
            }
          },
          visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
          singleLine = true,
          enabled = !isLoading,
          isError = confirmNewPassword.isNotEmpty() && !isConfirmMatch,
          supportingText = {
            when {
              confirmNewPassword.isEmpty() -> Text("Repita exatamente a nova senha")
              isConfirmMatch -> Text(
                text = stringResource(R.string.auth_password_match_ok),
                color = EmeraldWallet,
                fontWeight = FontWeight.SemiBold
              )
              else -> Text(
                text = stringResource(R.string.auth_password_mismatch_error),
                color = MaterialTheme.colorScheme.error
              )
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_confirm_new_password")
        )

        if (!activeError.isNullOrBlank()) {
          Surface(
            color = DangerRedLight,
            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("change_own_password_error")
          ) {
            Text(
              text = activeError,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              color = DangerRed,
              modifier = Modifier.padding(10.dp)
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          when {
            currentPassword.isBlank() -> localError = "Informe sua senha atual."
            newPassword.length < 6 -> localError = "A nova senha deve ter no mínimo 6 caracteres."
            newPassword == currentPassword -> localError = "A nova senha deve ser diferente da senha atual."
            newPassword != confirmNewPassword -> localError = "As novas senhas informadas não coincidem."
            else -> {
              localError = null
              onConfirmChangePassword(currentPassword, newPassword, confirmNewPassword)
            }
          }
        },
        enabled = canSubmit,
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("btn_confirm_change_own_password")
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Salvando...")
        } else {
          Text(
            text = stringResource(R.string.change_password_save_cta),
            fontWeight = FontWeight.Bold
          )
        }
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        enabled = !isLoading,
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("btn_cancel_change_own_password")
      ) {
        Text("Cancelar")
      }
    }
  )
}



