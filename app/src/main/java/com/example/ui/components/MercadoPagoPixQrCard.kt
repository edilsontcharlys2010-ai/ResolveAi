package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MercadoPagoPixService
import com.example.ui.theme.CardLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.DividerLight
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueLight
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Componente Compose reutilizável que exibe o QR Code Pix gerado via Mercado Pago
 * (decodificando `qrCodeBase64` oficial ou gerando via EMV `qrCodePayload`),
 * com temporizador regressivo de expiração e botão "Copiar Código".
 */
@Composable
fun MercadoPagoPixQrCard(
  amount: Double,
  qrCodePayload: String,
  expiresAtMillis: Long,
  modifier: Modifier = Modifier,
  qrCodeBase64: String? = null,
  totalDurationMs: Long = MercadoPagoPixService.MERCADO_PAGO_DEFAULT_EXPIRATION_MS,
  txId: String? = null,
  paymentId: Long? = null,
  isMercadoPago: Boolean = true,
  onExpired: () -> Unit = {},
  onCopyCode: ((String) -> Unit)? = null
) {
  @Suppress("DEPRECATION")
  val clipboardManager = LocalClipboardManager.current

  var copiedRecently by remember(qrCodePayload) { mutableStateOf(false) }
  var remainingMillis by remember(expiresAtMillis, qrCodePayload) {
    mutableLongStateOf((expiresAtMillis - System.currentTimeMillis()).coerceAtLeast(0L))
  }
  var notifiedExpired by remember(expiresAtMillis, qrCodePayload) { mutableStateOf(false) }

  LaunchedEffect(expiresAtMillis, qrCodePayload) {
    while (true) {
      val currentRemaining = (expiresAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
      remainingMillis = currentRemaining
      if (currentRemaining <= 0L) {
        if (!notifiedExpired) {
          notifiedExpired = true
          onExpired()
        }
        break
      }
      delay(1000L)
    }
  }

  LaunchedEffect(copiedRecently) {
    if (copiedRecently) {
      delay(2500L)
      copiedRecently = false
    }
  }

  val remainingSeconds = remainingMillis / 1000L
  val isExpired = remainingMillis <= 0L
  val isUrgent = !isExpired && remainingSeconds <= 5 * 60L

  val safeTotalMs = totalDurationMs.coerceAtLeast(1000L)
  val targetProgress = (remainingMillis.toFloat() / safeTotalMs.toFloat()).coerceIn(0f, 1f)
  val animatedProgress by animateFloatAsState(
    targetValue = targetProgress,
    label = "pixExpirationProgress"
  )

  val timerBgColor by animateColorAsState(
    targetValue = when {
      isExpired -> DangerRedLight
      isUrgent -> WarningAmberLight
      isMercadoPago -> InfoBlueLight
      else -> EmeraldWalletLight
    },
    label = "pixTimerBg"
  )
  val timerAccentColor by animateColorAsState(
    targetValue = when {
      isExpired -> DangerRed
      isUrgent -> WarningAmber
      isMercadoPago -> InfoBlue
      else -> EmeraldWallet
    },
    label = "pixTimerAccent"
  )

  val formattedCountdown = remember(remainingSeconds) {
    formatPixExpirationCountdown(remainingSeconds)
  }

  val decodedBase64Bitmap = remember(qrCodeBase64) {
    decodeMercadoPagoBase64Bitmap(qrCodeBase64)
  }

  Surface(
    color = CardLight,
    border = BorderStroke(
      width = 1.dp,
      color = if (isMercadoPago) InfoBlue.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant
    ),
    shape = RoundedCornerShape(18.dp),
    tonalElevation = 2.dp,
    modifier = modifier
      .fillMaxWidth()
      .testTag("recharge_pix_panel")
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Header bar with Gateway badge & Amount
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            Brush.horizontalGradient(
              colors = if (isMercadoPago) {
                listOf(InfoBlueLight.copy(alpha = 0.7f), EmeraldWalletLight.copy(alpha = 0.55f))
              } else {
                listOf(EmeraldWalletLight.copy(alpha = 0.65f), EmeraldWalletLight.copy(alpha = 0.25f))
              }
            )
          )
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              color = if (isMercadoPago) InfoBlue else EmeraldWallet,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.size(32.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = if (isMercadoPago) Icons.Default.VerifiedUser else Icons.Default.QrCode2,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
            Column {
              Text(
                text = if (isMercadoPago) "Pix Mercado Pago" else "Pagamento via Pix",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimaryLight
              )
              if (paymentId != null && paymentId > 0L) {
                Text(
                  text = "Cobrança #$paymentId",
                  style = MaterialTheme.typography.labelSmall,
                  color = TextSecondaryLight
                )
              } else if (!txId.isNullOrBlank()) {
                Text(
                  text = "ID: $txId",
                  style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                  color = TextSecondaryLight
                )
              }
            }
          }

          Text(
            text = "R$ ${String.format(Locale("pt", "BR"), "%.2f", amount)}",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = EmeraldWallet
          )
        }
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Temporizador de Expiração
        Surface(
          color = timerBgColor,
          border = BorderStroke(1.dp, timerAccentColor.copy(alpha = 0.4f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("mp_pix_expiration_timer")
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = if (isExpired) Icons.Default.ErrorOutline else Icons.Default.Timer,
                  contentDescription = "Tempo restante para expiração do Pix",
                  tint = timerAccentColor,
                  modifier = Modifier.size(17.dp)
                )
                Text(
                  text = when {
                    isExpired -> "Pagamento expirado"
                    isUrgent -> "Expira em breve"
                    else -> "Tempo para pagar"
                  },
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = timerAccentColor
                )
              }
              Text(
                text = formattedCountdown,
                style = MaterialTheme.typography.titleSmall.copy(
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.ExtraBold
                ),
                color = timerAccentColor
              )
            }
            LinearProgressIndicator(
              progress = { animatedProgress },
              color = timerAccentColor,
              trackColor = timerAccentColor.copy(alpha = 0.18f),
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
            )
          }
        }

        // QR Code Container (sempre fundo branco para leitura confiável pela câmera do banco)
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(224.dp)
            .alpha(if (isExpired) 0.35f else 1f)
            .testTag("recharge_pix_qr_code")
        ) {
          if (decodedBase64Bitmap != null) {
            Surface(
              color = Color.White,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, DividerLight),
              modifier = Modifier.fillMaxSize()
            ) {
              Image(
                bitmap = decodedBase64Bitmap,
                contentDescription = "QR Code Pix Mercado Pago para leitura bancária",
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
                modifier = Modifier
                  .fillMaxSize()
                  .padding(8.dp)
              )
            }
          } else {
            PixQrCodeGraphic(
              payload = qrCodePayload,
              modifier = Modifier.fillMaxSize()
            )
          }
        }

        Text(
          text = if (isExpired) {
            "Este QR Code expirou. Gere uma nova cobrança Pix para continuar."
          } else {
            "Abra o app do seu banco e escaneie o QR Code ou copie o código abaixo."
          },
          style = MaterialTheme.typography.bodySmall,
          color = if (isExpired) DangerRed else TextSecondaryLight,
          textAlign = TextAlign.Center
        )

        // Código Pix Copia e Cola + Botão "Copiar Código"
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, DividerLight),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("mp_pix_code_preview")
        ) {
          Text(
            text = qrCodePayload,
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp
            ),
            color = TextSecondaryLight,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
          )
        }

        Button(
          onClick = {
            clipboardManager.setText(AnnotatedString(qrCodePayload))
            copiedRecently = true
            onCopyCode?.invoke(qrCodePayload)
          },
          enabled = !isExpired && qrCodePayload.isNotBlank(),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (copiedRecently) EmeraldWallet else InfoBlue,
            contentColor = Color.White
          ),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics { contentDescription = "Copiar Código Pix" }
            .testTag("recharge_copy_pix_btn")
        ) {
          Icon(
            imageVector = if (copiedRecently) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (copiedRecently) "Código copiado!" else "Copiar Código",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
          )
        }
      }
    }
  }
}

internal fun formatPixExpirationCountdown(totalSeconds: Long): String {
  val safeSeconds = totalSeconds.coerceAtLeast(0L)
  val hours = safeSeconds / 3600L
  val minutes = (safeSeconds % 3600L) / 60L
  val seconds = safeSeconds % 60L
  return if (hours > 0L) {
    String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
  } else {
    String.format(Locale.US, "%02d:%02d", minutes, seconds)
  }
}

internal fun decodeMercadoPagoBase64Bitmap(rawBase64: String?): ImageBitmap? {
  val clean = rawBase64
    ?.trim()
    ?.substringAfter("base64,", missingDelimiterValue = rawBase64.trim())
    ?.takeIf { it.isNotBlank() }
    ?: return null
  return runCatching {
    val bytes = Base64.decode(clean, Base64.DEFAULT)
    if (bytes.isEmpty()) return@runCatching null
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
  }.getOrNull()
}
