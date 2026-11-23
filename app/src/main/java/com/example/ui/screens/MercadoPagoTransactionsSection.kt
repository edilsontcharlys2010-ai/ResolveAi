package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WalletHistoryHelper
import com.example.model.PixChargeStatus
import com.example.model.PixRechargeCharge
import com.example.ui.components.resolveAdaptiveSpec
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MercadoPagoTxFilter(val label: String) {
  TODAS("Todas"),
  AGUARDANDO("Aguardando"),
  APROVADAS("Aprovadas"),
  EXPIRADAS_OU_CANCELADAS("Expiradas / Canceladas")
}

@Composable
fun MercadoPagoTransactionsPreviewSection(
  charges: List<PixRechargeCharge>,
  isPollingActive: Boolean,
  isRefreshing: Boolean,
  onRefreshClick: () -> Unit,
  onOpenFullList: () -> Unit,
  onReopenCharge: (PixRechargeCharge) -> Unit,
  onNewRechargeClick: () -> Unit
) {
  val sortedCharges = remember(charges) {
    charges.sortedByDescending { it.createdAt }
  }
  val pendingCount = remember(sortedCharges) {
    sortedCharges.count { it.status == PixChargeStatus.AGUARDANDO_PAGAMENTO }
  }
  val approvedTotal = remember(sortedCharges) {
    sortedCharges.filter { it.status == PixChargeStatus.PAGO }.sumOf { it.amount }
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, InfoBlue.copy(alpha = 0.25f)),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("mp_pix_transactions_section")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
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
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(InfoBlueLight),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.VerifiedUser,
              contentDescription = null,
              tint = InfoBlue,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Transações Pix Mercado Pago",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = if (pendingCount > 0) {
                "$pendingCount pendente(s) • Aprovado: ${WalletHistoryHelper.formatBrl(approvedTotal)}"
              } else {
                "${sortedCharges.size} registro(s) • Total aprovado: ${WalletHistoryHelper.formatBrl(approvedTotal)}"
              },
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (isRefreshing || isPollingActive) {
            CircularProgressIndicator(
              strokeWidth = 2.dp,
              color = InfoBlue,
              modifier = Modifier
                .padding(horizontal = 6.dp)
                .size(16.dp)
            )
          } else {
            IconButton(
              onClick = onRefreshClick,
              modifier = Modifier
                .size(48.dp)
                .testTag("btn_refresh_mp_transactions")
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Atualizar transações Mercado Pago",
                tint = InfoBlue
              )
            }
          }

          TextButton(
            onClick = onOpenFullList,
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("btn_open_mp_transactions_screen")
          ) {
            Text("Ver todas", fontWeight = FontWeight.Bold, color = InfoBlue)
          }
        }
      }

      if (sortedCharges.isEmpty()) {
        Surface(
          color = InfoBlueLight.copy(alpha = 0.35f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Nenhuma cobrança Pix via Mercado Pago gerada ainda.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onNewRechargeClick) {
              Text("Recarregar", fontWeight = FontWeight.Bold, color = InfoBlue)
            }
          }
        }
      } else {
        sortedCharges.take(3).forEach { charge ->
          MercadoPagoTransactionItemCard(
            charge = charge,
            onReopenQr = { onReopenCharge(charge) },
            onRefreshStatus = onRefreshClick
          )
        }
      }
    }
  }
}

@Composable
fun MercadoPagoTransactionsFullScreen(
  charges: List<PixRechargeCharge>,
  isPollingActive: Boolean,
  isRefreshing: Boolean,
  onRefreshClick: () -> Unit,
  onReopenCharge: (PixRechargeCharge) -> Unit,
  onCancelCharge: (PixRechargeCharge) -> Unit,
  onNewRechargeClick: () -> Unit,
  onBack: () -> Unit
) {
  BackHandler { onBack() }

  var selectedFilter by remember { mutableStateOf(MercadoPagoTxFilter.TODAS) }
  val sortedCharges = remember(charges) {
    charges.sortedByDescending { it.createdAt }
  }
  val filteredCharges = remember(sortedCharges, selectedFilter) {
    when (selectedFilter) {
      MercadoPagoTxFilter.TODAS -> sortedCharges
      MercadoPagoTxFilter.AGUARDANDO ->
        sortedCharges.filter { it.status == PixChargeStatus.AGUARDANDO_PAGAMENTO }
      MercadoPagoTxFilter.APROVADAS ->
        sortedCharges.filter { it.status == PixChargeStatus.PAGO }
      MercadoPagoTxFilter.EXPIRADAS_OU_CANCELADAS ->
        sortedCharges.filter {
          it.status == PixChargeStatus.EXPIRADO ||
            it.status == PixChargeStatus.CANCELADO_OU_REJEITADO
        }
    }
  }

  val totalApproved = remember(sortedCharges) {
    sortedCharges.filter { it.status == PixChargeStatus.PAGO }.sumOf { it.amount }
  }
  val pendingCount = remember(sortedCharges) {
    sortedCharges.count { it.status == PixChargeStatus.AGUARDANDO_PAGAMENTO }
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .testTag("mp_pix_transactions_fullscreen"),
    contentAlignment = Alignment.TopCenter
  ) {
    val spec = remember(maxWidth) { resolveAdaptiveSpec(maxWidth) }

    Column(
      modifier = Modifier
        .widthIn(max = 840.dp)
        .fillMaxSize()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .size(48.dp)
              .testTag("btn_back_mp_transactions")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Voltar"
            )
          }
          Column {
            Text(
              text = "Transações Pix Mercado Pago",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = if (isPollingActive) {
                "Monitoramento automático ativo • ${sortedCharges.size} transação(ões)"
              } else {
                "${sortedCharges.size} transação(ões) registrada(s)"
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        if (isRefreshing) {
          CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier
              .padding(horizontal = 12.dp)
              .size(20.dp)
          )
        } else {
          IconButton(
            onClick = onRefreshClick,
            modifier = Modifier
              .size(48.dp)
              .testTag("btn_refresh_mp_transactions_full")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Consultar status no Mercado Pago",
              tint = InfoBlue
            )
          }
        }
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = spec.horizontalPadding)
          .testTag("mp_pix_transactions_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 36.dp)
      ) {
        // KPI summary strip
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Card(
              colors = CardDefaults.cardColors(containerColor = EmeraldWalletLight.copy(alpha = 0.55f)),
              border = BorderStroke(1.dp, EmeraldWallet.copy(alpha = 0.35f)),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = "Total aprovado",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = EmeraldWallet
                )
                Text(
                  text = WalletHistoryHelper.formatBrl(totalApproved),
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
            Card(
              colors = CardDefaults.cardColors(containerColor = InfoBlueLight.copy(alpha = 0.55f)),
              border = BorderStroke(1.dp, InfoBlue.copy(alpha = 0.35f)),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = "Aguardando Pix",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = InfoBlue
                )
                Text(
                  text = "$pendingCount cobrança(s)",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }

        // Filter Chips
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            MercadoPagoTxFilter.entries.forEach { filter ->
              val selected = selectedFilter == filter
              FilterChip(
                selected = selected,
                onClick = { selectedFilter = filter },
                label = { Text(filter.label, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = InfoBlue,
                  selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("mp_filter_${filter.name.lowercase()}")
              )
            }
          }
        }

        if (filteredCharges.isEmpty()) {
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.QrCode2,
                  contentDescription = null,
                  tint = InfoBlue,
                  modifier = Modifier.size(36.dp)
                )
                Text(
                  text = "Nenhuma transação Pix encontrada neste filtro.",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Button(
                  onClick = onNewRechargeClick,
                  colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
                  modifier = Modifier.heightIn(min = 48.dp)
                ) {
                  Text("Gerar Pix Mercado Pago", fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        } else {
          items(filteredCharges, key = { it.txId }) { charge ->
            MercadoPagoTransactionItemCard(
              charge = charge,
              onReopenQr = { onReopenCharge(charge) },
              onRefreshStatus = onRefreshClick,
              onCancelCharge = if (charge.status == PixChargeStatus.AGUARDANDO_PAGAMENTO && charge.mercadoPagoPaymentId != null) {
                { onCancelCharge(charge) }
              } else null
            )
          }
        }
      }
    }
  }
}

@Composable
fun MercadoPagoTransactionItemCard(
  charge: PixRechargeCharge,
  onReopenQr: () -> Unit,
  onRefreshStatus: () -> Unit,
  onCancelCharge: (() -> Unit)? = null
) {
  val uriHandler = LocalUriHandler.current
  val statusColors = remember(charge.status) { resolveMercadoPagoStatusStyle(charge.status) }
  val formattedDate = remember(charge.paidAt, charge.createdAt) {
    formatMpTransactionDate(charge.paidAt ?: charge.createdAt)
  }

  Surface(
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, statusColors.accent.copy(alpha = 0.28f)),
    shape = RoundedCornerShape(14.dp),
    tonalElevation = 1.dp,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(
        enabled = charge.status == PixChargeStatus.AGUARDANDO_PAGAMENTO,
        onClick = onReopenQr
      )
      .testTag("mp_tx_item_${charge.txId}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
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
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(statusColors.background),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = statusColors.icon,
              contentDescription = statusColors.label,
              tint = statusColors.accent,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = charge.mercadoPagoPaymentId?.let { "Pix Mercado Pago #$it" } ?: "Pix ${charge.txId}",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = formattedDate,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = WalletHistoryHelper.formatBrl(charge.amount),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = if (charge.status == PixChargeStatus.PAGO) EmeraldWallet else MaterialTheme.colorScheme.onSurface
          )
          Surface(
            color = statusColors.background,
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(
              text = statusColors.label,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
              ),
              color = statusColors.accent,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
          text = "TX: ${charge.txId}",
          style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (!charge.mpStatusDetail.isNullOrBlank()) {
          Text(
            text = charge.mpStatusDetail,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (charge.status == PixChargeStatus.AGUARDANDO_PAGAMENTO || !charge.ticketUrl.isNullOrBlank()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (charge.status == PixChargeStatus.AGUARDANDO_PAGAMENTO) {
            OutlinedButton(
              onClick = onReopenQr,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
            ) {
              Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("QR Code", fontSize = 12.sp, maxLines = 1)
            }
            OutlinedButton(
              onClick = onRefreshStatus,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
            ) {
              Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Verificar", fontSize = 12.sp, maxLines = 1)
            }
          }
          if (!charge.ticketUrl.isNullOrBlank()) {
            OutlinedButton(
              onClick = { runCatching { uriHandler.openUri(charge.ticketUrl) } },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
            ) {
              Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Comprovante", fontSize = 12.sp, maxLines = 1)
            }
          }
          if (onCancelCharge != null) {
            IconButton(
              onClick = onCancelCharge,
              modifier = Modifier.size(48.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = "Cancelar cobrança Pix",
                tint = DangerRed
              )
            }
          }
        }
      }
    }
  }
}

private data class MercadoPagoStatusVisual(
  val label: String,
  val accent: Color,
  val background: Color,
  val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun resolveMercadoPagoStatusStyle(status: PixChargeStatus): MercadoPagoStatusVisual {
  return when (status) {
    PixChargeStatus.PAGO -> MercadoPagoStatusVisual(
      label = "Aprovado",
      accent = EmeraldWallet,
      background = EmeraldWalletLight,
      icon = Icons.Default.CheckCircle
    )
    PixChargeStatus.AGUARDANDO_PAGAMENTO -> MercadoPagoStatusVisual(
      label = "Aguardando",
      accent = WarningAmber,
      background = WarningAmberLight,
      icon = Icons.Default.Schedule
    )
    PixChargeStatus.EXPIRADO -> MercadoPagoStatusVisual(
      label = "Expirado",
      accent = DangerRed,
      background = DangerRedLight,
      icon = Icons.Default.TimerOff
    )
    PixChargeStatus.CANCELADO_OU_REJEITADO -> MercadoPagoStatusVisual(
      label = "Cancelado",
      accent = DangerRed,
      background = DangerRedLight,
      icon = Icons.Default.Cancel
    )
  }
}

internal fun formatMpTransactionDate(timestampMillis: Long): String {
  val formatter = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
  return formatter.format(Date(timestampMillis))
}
