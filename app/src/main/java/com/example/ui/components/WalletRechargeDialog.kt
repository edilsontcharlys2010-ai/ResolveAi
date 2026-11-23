package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.MercadoPagoPixService
import com.example.data.WalletRechargeHelper
import com.example.ui.PixRechargeUiState
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.max

@Composable
fun RechargeWalletDialog(
  currentBalance: Double,
  pixRechargeState: PixRechargeUiState = PixRechargeUiState.Idle,
  pixGatewayError: String? = null,
  isMercadoPagoConfigured: Boolean = MercadoPagoPixService.isConfigured(),
  onGenerateMercadoPagoPix: (amount: Double) -> Unit = {},
  onCheckPixPaymentNow: () -> Unit = {},
  onResetPixCharge: () -> Unit = {},
  onDismiss: () -> Unit
) {
  val uriHandler = LocalUriHandler.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var selectedAmount by remember { mutableStateOf(50.0) }
  var customAmountText by remember { mutableStateOf("") }
  var isCustom by remember { mutableStateOf(false) }
  var validationError by remember { mutableStateOf<String?>(null) }

  val predefinedAmounts = listOf(20.0, 50.0, 100.0, 200.0)
  val effectiveAmount = if (isCustom) {
    customAmountText.replace(",", ".").toDoubleOrNull() ?: 0.0
  } else {
    selectedAmount
  }

  // Automatically check Firestore/Mercado Pago whenever the provider returns from their banking app
  if (pixRechargeState is PixRechargeUiState.AwaitingPayment) {
    DisposableEffect(lifecycleOwner, pixRechargeState.charge.txId) {
      val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
          onCheckPixPaymentNow()
        }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
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
    icon = {
      Icon(
        imageVector = when (pixRechargeState) {
          is PixRechargeUiState.Detected -> Icons.Default.CheckCircle
          is PixRechargeUiState.CreatingCharge -> Icons.Default.Autorenew
          is PixRechargeUiState.AwaitingPayment -> Icons.Default.QrCode2
          PixRechargeUiState.Idle -> Icons.Default.AccountBalanceWallet
        },
        contentDescription = null,
        tint = EmeraldWallet,
        modifier = Modifier.size(34.dp)
      )
    },
    title = {
      Text(
        text = when (pixRechargeState) {
          is PixRechargeUiState.Detected -> "Recarga Pix Concluída"
          is PixRechargeUiState.CreatingCharge -> "Gerando cobrança Pix..."
          is PixRechargeUiState.AwaitingPayment -> "Pix Mercado Pago"
          PixRechargeUiState.Idle -> "Recarga via Pix"
        },
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Surface(
          color = EmeraldWalletLight,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (pixRechargeState is PixRechargeUiState.Detected) "Saldo atualizado" else "Saldo atual",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              color = Color(0xFF065F46)
            )
            val balanceDisplay = (pixRechargeState as? PixRechargeUiState.Detected)?.newBalance ?: currentBalance
            Text(
              text = "R$ ${String.format("%.2f", balanceDisplay)}",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
              color = EmeraldWallet
            )
          }
        }

        when (pixRechargeState) {
          PixRechargeUiState.Idle -> {
            Text(
              text = "Gere seu QR Code Pix instantâneo via Mercado Pago para recarregar sua carteira:",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val isSandbox = MercadoPagoPixService.isSandboxToken()
            Surface(
              color = if (isMercadoPagoConfigured) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
              border = BorderStroke(
                1.dp,
                if (isMercadoPagoConfigured) EmeraldWallet.copy(alpha = 0.45f) else Color(0xFFF59E0B).copy(alpha = 0.55f)
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("recharge_mercadopago_status_card")
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    imageVector = if (isMercadoPagoConfigured) Icons.Default.CloudDone else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = if (isMercadoPagoConfigured) EmeraldWallet else Color(0xFFB45309),
                    modifier = Modifier.size(18.dp)
                  )
                  Text(
                    text = when {
                      !isMercadoPagoConfigured -> "Mercado Pago não configurado"
                      isSandbox -> "Mercado Pago conectado (Modo Teste TEST)"
                      else -> "Mercado Pago ativo • Cobrança Real"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isMercadoPagoConfigured) Color(0xFF065F46) else Color(0xFF92400E)
                  )
                }
                Text(
                  text = if (isMercadoPagoConfigured) {
                    if (isSandbox) {
                      "Para cobrar dinheiro real, use um Access Token de Produção (APP_USR-...) no painel Secrets do AI Studio."
                    } else {
                      "O QR Code é gerado direto na sua conta Mercado Pago e o saldo só é liberado após aprovação automática."
                    }
                  } else {
                    "Adicione a variável MERCADO_PAGO_ACCESS_TOKEN (token APP_USR-... de Produção do Mercado Pago) no painel Secrets do AI Studio e recompile para cobrar de verdade."
                  },
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                  color = if (isMercadoPagoConfigured) Color(0xFF047857) else Color(0xFF92400E)
                )
              }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "Valor da recarga",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                predefinedAmounts.forEach { amount ->
                  val isSelected = !isCustom && selectedAmount == amount
                  Surface(
                    color = if (isSelected) EmeraldWallet else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                      .weight(1f)
                      .heightIn(min = 48.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .clickable {
                        selectedAmount = amount
                        isCustom = false
                        validationError = null
                      }
                      .testTag("recharge_amount_${amount.toInt()}")
                  ) {
                    Box(
                      modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = "R$ ${amount.toInt()}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                      )
                    }
                  }
                }
              }

              OutlinedTextField(
                value = customAmountText,
                onValueChange = { raw ->
                  customAmountText = raw.filter { char -> char.isDigit() || char == '.' || char == ',' }
                  isCustom = customAmountText.isNotEmpty()
                  validationError = null
                },
                label = { Text("Outro valor (R$)") },
                placeholder = { Text("Ex: 75.00 (mín. R$ 5,00)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("recharge_custom_amount_input")
              )
            }

            val displayedError = validationError ?: pixGatewayError
            if (!displayedError.isNullOrBlank()) {
              Surface(
                color = DangerRedLight,
                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("recharge_validation_error")
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = displayedError,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF991B1B)
                  )
                }
              }
            }
          }

          is PixRechargeUiState.CreatingCharge -> {
            Surface(
              color = Color(0xFFECFDF5),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("recharge_creating_charge_banner")
            ) {
              Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                CircularProgressIndicator(
                  color = EmeraldWallet,
                  modifier = Modifier.size(36.dp)
                )
                Text(
                  text = "Criando cobrança Pix de R$ ${String.format("%.2f", pixRechargeState.amount)} no ${pixRechargeState.gatewayLabel}...",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = Color(0xFF065F46),
                  textAlign = TextAlign.Center
                )
              }
            }
          }

          is PixRechargeUiState.AwaitingPayment -> {
            val charge = pixRechargeState.charge
            var remainingSeconds by remember(charge.txId) {
              mutableLongStateOf(((charge.expiresAt - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L))
            }

            LaunchedEffect(charge.txId) {
              while (remainingSeconds > 0L) {
                delay(1000L)
                remainingSeconds = ((charge.expiresAt - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
                if (remainingSeconds > 0L && remainingSeconds % 6L == 0L) {
                  onCheckPixPaymentNow()
                }
              }
            }

            val mins = remainingSeconds / 60L
            val secs = remainingSeconds % 60L
            val formattedCountdown = String.format(Locale.US, "%02d:%02d", mins, secs)

            // Live Auto-Detection Banner
            Surface(
              color = Color(0xFFECFDF5),
              border = BorderStroke(1.dp, EmeraldWallet.copy(alpha = 0.45f)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("recharge_pix_auto_detect_banner")
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                CircularProgressIndicator(
                  color = EmeraldWallet,
                  strokeWidth = 2.5.dp,
                  modifier = Modifier.size(18.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Detecção automática ativa ($formattedCountdown)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF065F46)
                  )
                  Text(
                    text = pixRechargeState.statusNotice
                      ?: "Assim que o Pix for compensado, o saldo entra automaticamente.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color(0xFF047857)
                  )
                }
              }
            }

            // Mercado Pago Pix QR Code + Temporizador de Expiração + Botão Copiar Código
            MercadoPagoPixQrCard(
              amount = charge.amount,
              qrCodePayload = charge.brCode,
              qrCodeBase64 = charge.qrCodeBase64,
              expiresAtMillis = charge.expiresAt,
              totalDurationMs = MercadoPagoPixService.MERCADO_PAGO_DEFAULT_EXPIRATION_MS,
              txId = charge.txId,
              paymentId = charge.mercadoPagoPaymentId,
              isMercadoPago = true
            )

            if (!charge.ticketUrl.isNullOrBlank()) {
              OutlinedButton(
                onClick = {
                  runCatching { uriHandler.openUri(charge.ticketUrl) }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("recharge_open_mp_ticket_btn")
              ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Abrir cobrança no Mercado Pago", fontWeight = FontWeight.SemiBold)
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = onResetPixCharge,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("recharge_change_amount_btn")
              ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Alterar valor", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
              }

              OutlinedButton(
                onClick = onCheckPixPaymentNow,
                enabled = !pixRechargeState.isCheckingNow,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("recharge_check_pix_btn")
              ) {
                if (pixRechargeState.isCheckingNow) {
                  CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                } else {
                  Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Verificar", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
              }
            }
          }

          is PixRechargeUiState.Detected -> {
            Surface(
              color = EmeraldWalletLight,
              border = BorderStroke(1.dp, EmeraldWallet.copy(alpha = 0.5f)),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("recharge_pix_detected_banner")
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Surface(
                  color = EmeraldWallet,
                  shape = CircleShape,
                  modifier = Modifier.size(48.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.CheckCircle,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(28.dp)
                    )
                  }
                }

                Text(
                  text = if (pixRechargeState.automatic) {
                    "Pagamento Pix detectado automaticamente!"
                  } else {
                    "Recarga Pix confirmada com sucesso!"
                  },
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = Color(0xFF065F46),
                  textAlign = TextAlign.Center
                )

                Text(
                  text = "Valor creditado: R$ ${String.format("%.2f", pixRechargeState.charge.amount)}\nID: ${pixRechargeState.charge.txId}",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFF047857),
                  textAlign = TextAlign.Center
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      when (pixRechargeState) {
        PixRechargeUiState.Idle -> {
          Button(
            onClick = {
              val amountError = WalletRechargeHelper.validateAmount(
                if (isCustom) customAmountText.replace(",", ".").toDoubleOrNull() else selectedAmount
              )
              if (amountError != null) {
                validationError = amountError
                return@Button
              }
              if (!isMercadoPagoConfigured) {
                validationError = "Configure MERCADO_PAGO_ACCESS_TOKEN nos Secrets do AI Studio para gerar a recarga via Mercado Pago."
                return@Button
              }
              validationError = null
              onGenerateMercadoPagoPix(effectiveAmount)
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
            modifier = Modifier.testTag("recharge_generate_qr_btn")
          ) {
            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Gerar Pix Mercado Pago",
              fontWeight = FontWeight.Bold
            )
          }
        }

        is PixRechargeUiState.CreatingCharge -> {}

        is PixRechargeUiState.AwaitingPayment -> {
          Button(
            onClick = onCheckPixPaymentNow,
            enabled = !pixRechargeState.isCheckingNow,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
            modifier = Modifier.testTag("confirm_recharge_button")
          ) {
            Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Confirmar no Mercado Pago",
              fontWeight = FontWeight.Bold
            )
          }
        }

        is PixRechargeUiState.Detected -> {
          Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
            modifier = Modifier.testTag("recharge_done_button")
          ) {
            Text("Concluir", fontWeight = FontWeight.Bold)
          }
        }
      }
    },
    dismissButton = {
      if (pixRechargeState !is PixRechargeUiState.Detected) {
        TextButton(onClick = onDismiss) {
          Text("Fechar")
        }
      }
    }
  )
}

@Composable
fun PixQrCodeGraphic(
  payload: String,
  modifier: Modifier = Modifier
) {
  val qrImageBitmap = remember(payload) {
    runCatching {
      val matrix = WalletRechargeHelper.buildQrMatrix(payload)
      if (matrix.isEmpty()) return@runCatching null
      val rows = matrix.size
      val cols = matrix[0].size
      val scale = max(1, 560 / max(rows, cols))
      val width = cols * scale
      val height = rows * scale
      val pixels = IntArray(width * height) { android.graphics.Color.WHITE }
      for (y in 0 until rows) {
        for (x in 0 until cols) {
          if (matrix[y][x]) {
            val startY = y * scale
            val startX = x * scale
            for (dy in 0 until scale) {
              val rowOffset = (startY + dy) * width + startX
              for (dx in 0 until scale) {
                pixels[rowOffset + dx] = android.graphics.Color.BLACK
              }
            }
          }
        }
      }
      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
      bitmap.asImageBitmap()
    }.getOrNull()
  }

  Surface(
    color = Color.White,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
    modifier = modifier
  ) {
    if (qrImageBitmap == null) {
      Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
        Icon(
          imageVector = Icons.Default.QrCode2,
          contentDescription = "QR Code Pix",
          tint = Color(0xFF0F172A),
          modifier = Modifier.size(96.dp)
        )
      }
    } else {
      Image(
        bitmap = qrImageBitmap,
        contentDescription = "QR Code Pix para leitura bancária",
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None,
        modifier = Modifier
          .fillMaxSize()
          .padding(6.dp)
      )
    }
  }
}
