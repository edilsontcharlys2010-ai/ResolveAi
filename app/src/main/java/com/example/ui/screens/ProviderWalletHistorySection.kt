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
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProviderWalletSummary
import com.example.data.WalletHistoryFilter
import com.example.data.WalletHistoryHelper
import com.example.model.ServiceRequest
import com.example.model.TransactionType
import com.example.model.User
import com.example.model.WalletTransaction
import com.example.ui.components.resolveAdaptiveSpec
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight

@Composable
fun ProviderFinancialSummarySection(
  currentBalance: Double,
  summary: ProviderWalletSummary,
  selectedFilter: WalletHistoryFilter,
  onSelectFilter: (WalletHistoryFilter) -> Unit,
  onOpenFullScreen: () -> Unit,
  onRechargeClick: () -> Unit,
  onRefreshClick: () -> Unit = {},
  isRefreshing: Boolean = false
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("provider_financial_history_section"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
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
          imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "Histórico Financeiro Detalhado",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "Recargas Pix e uso do saldo em contratações",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        if (isRefreshing) {
          CircularProgressIndicator(
            modifier = Modifier
              .padding(horizontal = 8.dp)
              .size(18.dp),
            strokeWidth = 2.dp
          )
        } else {
          IconButton(
            onClick = onRefreshClick,
            modifier = Modifier.size(48.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Atualizar extrato financeiro",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }

        TextButton(
          onClick = onOpenFullScreen,
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("btn_open_full_wallet_history")
        ) {
          Icon(
            imageVector = Icons.Default.OpenInFull,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Tela completa", fontWeight = FontWeight.SemiBold)
        }
      }
    }

    ProviderWalletSummaryKpiRow(
      currentBalance = currentBalance,
      summary = summary,
      onPixCardClick = { onSelectFilter(WalletHistoryFilter.RECARGAS_PIX) },
      onContractCardClick = { onSelectFilter(WalletHistoryFilter.USO_CONTRATACOES) }
    )

    WalletHistoryFilterChipsRow(
      summary = summary,
      selectedFilter = selectedFilter,
      onSelectFilter = onSelectFilter,
      onRechargeClick = onRechargeClick
    )
  }
}

@Composable
fun ProviderWalletSummaryKpiRow(
  currentBalance: Double,
  summary: ProviderWalletSummary,
  onPixCardClick: () -> Unit = {},
  onContractCardClick: () -> Unit = {}
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Total Pix Recharges
      Card(
        colors = CardDefaults.cardColors(containerColor = EmeraldWalletLight.copy(alpha = 0.55f)),
        border = BorderStroke(1.dp, EmeraldWallet.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .weight(1f)
          .clickable(onClick = onPixCardClick)
          .testTag("kpi_pix_recharges")
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(EmeraldWallet.copy(alpha = 0.16f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = null,
                tint = EmeraldWallet,
                modifier = Modifier.size(16.dp)
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Recargas Pix",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = EmeraldWallet,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Text(
            text = WalletHistoryHelper.formatBrl(summary.totalPixRecharged),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = EmeraldWallet
          )

          Text(
            text = buildString {
              append("${summary.pixRechargesCount} recarga(s)")
              if (summary.autoDetectedPixCount > 0) {
                append(" • ${summary.autoDetectedPixCount} auto")
              }
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      // Total Used in Contracts
      Card(
        colors = CardDefaults.cardColors(containerColor = DangerRedLight.copy(alpha = 0.45f)),
        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .weight(1f)
          .clickable(onClick = onContractCardClick)
          .testTag("kpi_contract_usage")
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(DangerRed.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Handshake,
                contentDescription = null,
                tint = DangerRed,
                modifier = Modifier.size(16.dp)
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Uso Contratações",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = DangerRed,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Text(
            text = WalletHistoryHelper.formatBrl(summary.totalContractFeesUsed),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = DangerRed
          )

          Text(
            text = "${summary.contractUsageCount} contratação(ões) • 10%",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }

    // Balance & Contract Volume summary strip
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Volume total em contratos fechados",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = WalletHistoryHelper.formatBrl(summary.totalEstimatedContractedValue),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "Saldo atual",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = WalletHistoryHelper.formatBrl(currentBalance),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = EmeraldWallet
          )
        }
      }
    }
  }
}

@Composable
fun WalletHistoryFilterChipsRow(
  summary: ProviderWalletSummary,
  selectedFilter: WalletHistoryFilter,
  onSelectFilter: (WalletHistoryFilter) -> Unit,
  onRechargeClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    val allCount = summary.totalMovementsCount
    val pixCount = summary.pixRechargesCount
    val contractCount = summary.contractUsageCount

    FilterChip(
      selected = selectedFilter == WalletHistoryFilter.TODAS,
      onClick = { onSelectFilter(WalletHistoryFilter.TODAS) },
      label = { Text("Todas ($allCount)") },
      colors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
      ),
      modifier = Modifier
        .heightIn(min = 48.dp)
        .testTag("filter_tx_all")
    )

    FilterChip(
      selected = selectedFilter == WalletHistoryFilter.RECARGAS_PIX,
      onClick = { onSelectFilter(WalletHistoryFilter.RECARGAS_PIX) },
      leadingIcon = {
        Icon(
          imageVector = Icons.Default.QrCode2,
          contentDescription = null,
          modifier = Modifier.size(16.dp)
        )
      },
      label = { Text("Recargas Pix ($pixCount)") },
      colors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = EmeraldWalletLight,
        selectedLabelColor = EmeraldWallet
      ),
      modifier = Modifier
        .heightIn(min = 48.dp)
        .testTag("filter_tx_pix")
    )

    FilterChip(
      selected = selectedFilter == WalletHistoryFilter.USO_CONTRATACOES,
      onClick = { onSelectFilter(WalletHistoryFilter.USO_CONTRATACOES) },
      leadingIcon = {
        Icon(
          imageVector = Icons.Default.Handshake,
          contentDescription = null,
          modifier = Modifier.size(16.dp)
        )
      },
      label = { Text("Uso em Contratações ($contractCount)") },
      colors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = DangerRedLight,
        selectedLabelColor = DangerRed
      ),
      modifier = Modifier
        .heightIn(min = 48.dp)
        .testTag("filter_tx_contracts")
    )

    OutlinedButton(
      onClick = onRechargeClick,
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.heightIn(min = 48.dp)
    ) {
      Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text("Novo Pix", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
fun DetailedWalletTransactionCard(
  transaction: WalletTransaction,
  relatedService: ServiceRequest? = null,
  onClick: () -> Unit = {}
) {
  val isDebit = transaction.type == TransactionType.DEBITO_TAXA_10
  val isRefund = transaction.type == TransactionType.REEMBOLSO
  val isAutoPix = WalletHistoryHelper.isAutoDetectedPix(transaction)
  val isBonus = WalletHistoryHelper.isWelcomeOrAdminBonus(transaction)
  val pixTxId = remember(transaction) { WalletHistoryHelper.extractPixTxId(transaction) }
  val balanceBefore = remember(transaction) { WalletHistoryHelper.computeBalanceBefore(transaction) }
  val contractedValue = remember(transaction, relatedService) {
    WalletHistoryHelper.resolveContractedServiceValue(transaction, relatedService)
  }
  val dateStr = remember(transaction.timestamp) {
    WalletHistoryHelper.formatDateTime(transaction.timestamp)
  }

  val accentColor = when {
    isDebit -> DangerRed
    isRefund -> InfoBlue
    else -> EmeraldWallet
  }
  val badgeBgColor = when {
    isDebit -> DangerRedLight
    isRefund -> InfoBlueLight
    else -> EmeraldWalletLight
  }
  val icon = when (transaction.type) {
    TransactionType.DEBITO_TAXA_10 -> Icons.Default.Handshake
    TransactionType.RECARGA_PIX -> Icons.Default.QrCode2
    TransactionType.RECARGA_CARTAO -> Icons.Default.CreditCard
    TransactionType.REEMBOLSO -> Icons.Default.ArrowUpward
    TransactionType.CREDITO_ADMIN -> Icons.Default.AdminPanelSettings
  }

  val titleText = when (transaction.type) {
    TransactionType.DEBITO_TAXA_10 ->
      transaction.serviceTitle?.takeIf { it.isNotBlank() }
        ?: relatedService?.title
        ?: "Uso em Contratação de Serviço"
    TransactionType.RECARGA_PIX ->
      if (isBonus) "Bônus de Crédito na Carteira" else "Recarga via Pix"
    TransactionType.RECARGA_CARTAO -> "Recarga Histórica"
    TransactionType.REEMBOLSO ->
      transaction.serviceTitle?.takeIf { it.isNotBlank() }?.let { "Estorno: $it" }
        ?: "Estorno de Taxa de Contratação"
    TransactionType.CREDITO_ADMIN -> "Crédito Manual do Administrador"
  }

  val originLabel = when {
    isDebit -> "Taxa 10% Contratação"
    isRefund -> "Estorno"
    transaction.type == TransactionType.CREDITO_ADMIN -> "Crédito Admin"
    isAutoPix -> "Pix • Detecção automática"
    isBonus -> "Crédito Promocional"
    transaction.type == TransactionType.RECARGA_PIX -> "Pix Instantâneo"
    else -> "Recarga"
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("wallet_tx_card_${transaction.id}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(badgeBgColor),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = originLabel,
            tint = accentColor,
            modifier = Modifier.size(21.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = titleText,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )

          Spacer(modifier = Modifier.height(4.dp))

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              color = badgeBgColor,
              shape = RoundedCornerShape(6.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (isAutoPix) {
                  Icon(
                    imageVector = Icons.Default.Autorenew,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                  text = originLabel,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                  ),
                  color = accentColor
                )
              }
            }

            if (pixTxId != null) {
              Text(
                text = "TX: $pixTxId",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontFamily = FontFamily.Monospace,
                  fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            } else if (!transaction.serviceId.isNullOrBlank()) {
              Text(
                text = "#${transaction.serviceId}",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontFamily = FontFamily.Monospace,
                  fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "${if (isDebit) "-" else "+"} ${WalletHistoryHelper.formatBrl(transaction.amount)}",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.ExtraBold,
              color = accentColor
            )
          )
          Text(
            text = if (isDebit) "Débito" else "Crédito",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = accentColor.copy(alpha = 0.85f)
          )
        }
      }

      // Secondary metadata line (contract value / client or Pix description)
      if (isDebit) {
        Surface(
          color = WarningAmberLight.copy(alpha = 0.5f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            val clientLabel = relatedService?.clientName?.takeIf { it.isNotBlank() }?.let {
              "Cliente: $it"
            } ?: "Intermediação ResolveAi"
            Text(
              text = clientLabel,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f)
            )
            if (contractedValue != null) {
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Contrato: ${WalletHistoryHelper.formatBrl(contractedValue)}",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp
                ),
                color = WarningAmber
              )
            }
          }
        }
      } else if (transaction.description.isNotBlank()) {
        Text(
          text = transaction.description,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = dateStr,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "Saldo: ${WalletHistoryHelper.formatBrl(balanceBefore)} → ${WalletHistoryHelper.formatBrl(transaction.balanceAfter)}",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
          ),
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}

@Composable
fun TransactionDetailDialog(
  transaction: WalletTransaction,
  relatedService: ServiceRequest? = null,
  onDismiss: () -> Unit
) {
  val isDebit = transaction.type == TransactionType.DEBITO_TAXA_10
  val isAutoPix = WalletHistoryHelper.isAutoDetectedPix(transaction)
  val pixTxId = remember(transaction) { WalletHistoryHelper.extractPixTxId(transaction) }
  val balanceBefore = remember(transaction) { WalletHistoryHelper.computeBalanceBefore(transaction) }
  val contractedValue = remember(transaction, relatedService) {
    WalletHistoryHelper.resolveContractedServiceValue(transaction, relatedService)
  }
  val fullDate = remember(transaction.timestamp) {
    WalletHistoryHelper.formatDateTime(transaction.timestamp, withSeconds = true)
  }
  val accentColor = if (isDebit) DangerRed else EmeraldWallet

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag("tx_detail_dialog"),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = if (isDebit) Icons.Default.Handshake else Icons.Default.QrCode2,
          contentDescription = null,
          tint = accentColor
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isDebit) "Comprovante de Uso na Contratação" else "Comprovante de Recarga Pix",
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
        Surface(
          color = (if (isDebit) DangerRedLight else EmeraldWalletLight).copy(alpha = 0.6f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = if (isDebit) "Valor Debitado (Taxa 10%)" else "Valor Creditado",
              style = MaterialTheme.typography.labelMedium,
              color = accentColor
            )
            Text(
              text = "${if (isDebit) "-" else "+"} ${WalletHistoryHelper.formatBrl(transaction.amount)}",
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
              color = accentColor
            )
            if (isAutoPix) {
              Spacer(modifier = Modifier.height(4.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = EmeraldWallet,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Confirmado com detecção automática",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = EmeraldWallet
                )
              }
            }
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            TransactionReceiptRow("Tipo", transaction.type.label)
            TransactionReceiptRow("ID do Registro", transaction.id)
            if (pixTxId != null) {
              TransactionReceiptRow("Código Pix (TX ID)", pixTxId)
            }
            if (!transaction.serviceTitle.isNullOrBlank() || relatedService != null) {
              TransactionReceiptRow(
                "Serviço Contratado",
                transaction.serviceTitle ?: relatedService?.title.orEmpty()
              )
            }
            if (!transaction.serviceId.isNullOrBlank()) {
              TransactionReceiptRow("Código do Serviço", "#${transaction.serviceId}")
            }
            relatedService?.clientName?.takeIf { it.isNotBlank() }?.let { clientName ->
              TransactionReceiptRow("Cliente", clientName)
            }
            if (contractedValue != null) {
              TransactionReceiptRow(
                "Valor do Contrato",
                WalletHistoryHelper.formatBrl(contractedValue)
              )
            }
            TransactionReceiptRow("Data e Hora", fullDate)
            TransactionReceiptRow("Saldo Anterior", WalletHistoryHelper.formatBrl(balanceBefore))
            TransactionReceiptRow("Saldo Após", WalletHistoryHelper.formatBrl(transaction.balanceAfter))
          }
        }

        if (transaction.description.isNotBlank()) {
          Column {
            Text(
              text = "Descrição",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = transaction.description,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
      ) {
        Text("Fechar")
      }
    }
  )
}

@Composable
private fun TransactionReceiptRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(end = 12.dp)
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
fun ProviderWalletHistoryFullScreen(
  currentUser: User,
  userTransactions: List<WalletTransaction>,
  servicesById: Map<String, ServiceRequest>,
  initialFilter: WalletHistoryFilter,
  onSelectFilter: (WalletHistoryFilter) -> Unit,
  onRechargeClick: () -> Unit,
  onRefreshClick: () -> Unit,
  isRefreshing: Boolean,
  onBack: () -> Unit
) {
  BackHandler { onBack() }

  var searchQuery by remember { mutableStateOf("") }
  var selectedTx by remember { mutableStateOf<WalletTransaction?>(null) }

  val summary = remember(userTransactions, servicesById) {
    WalletHistoryHelper.summarize(userTransactions, servicesById)
  }
  val filteredTransactions = remember(userTransactions, initialFilter, searchQuery) {
    WalletHistoryHelper.filterTransactions(userTransactions, initialFilter, searchQuery)
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .testTag("provider_wallet_history_fullscreen"),
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
              .testTag("btn_close_full_wallet_history")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Voltar ao Perfil do Prestador"
            )
          }
          Column {
            Text(
              text = "Histórico Pix & Contratações",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${currentUser.name} • ${userTransactions.size} movimentação(ões)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        if (isRefreshing) {
          CircularProgressIndicator(
            modifier = Modifier
              .padding(horizontal = 12.dp)
              .size(20.dp),
            strokeWidth = 2.dp
          )
        } else {
          IconButton(
            onClick = onRefreshClick,
            modifier = Modifier
              .size(48.dp)
              .testTag("btn_refresh_wallet_history")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Sincronizar histórico financeiro",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = spec.horizontalPadding)
          .testTag("wallet_history_fullscreen_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 36.dp)
      ) {
        item {
          ProviderWalletSummaryKpiRow(
            currentBalance = currentUser.walletBalance,
            summary = summary,
            onPixCardClick = { onSelectFilter(WalletHistoryFilter.RECARGAS_PIX) },
            onContractCardClick = { onSelectFilter(WalletHistoryFilter.USO_CONTRATACOES) }
          )
        }

        item {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar por serviço, código Pix TX ou descrição") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
              if (searchQuery.isNotBlank()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(Icons.Default.Close, contentDescription = "Limpar busca")
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_wallet_history_search")
          )
        }

        item {
          WalletHistoryFilterChipsRow(
            summary = summary,
            selectedFilter = initialFilter,
            onSelectFilter = onSelectFilter,
            onRechargeClick = onRechargeClick
          )
        }

        if (filteredTransactions.isEmpty()) {
          item {
            WalletHistoryEmptyCard(
              filter = initialFilter,
              hasQuery = searchQuery.isNotBlank(),
              onRechargeClick = onRechargeClick
            )
          }
        } else {
          items(filteredTransactions, key = { it.id }) { tx ->
            DetailedWalletTransactionCard(
              transaction = tx,
              relatedService = tx.serviceId?.let { servicesById[it] },
              onClick = { selectedTx = tx }
            )
          }
        }
      }
    }
  }

  selectedTx?.let { tx ->
    TransactionDetailDialog(
      transaction = tx,
      relatedService = tx.serviceId?.let { servicesById[it] },
      onDismiss = { selectedTx = null }
    )
  }
}

@Composable
fun WalletHistoryEmptyCard(
  filter: WalletHistoryFilter,
  hasQuery: Boolean,
  onRechargeClick: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
        imageVector = when (filter) {
          WalletHistoryFilter.RECARGAS_PIX -> Icons.Default.QrCode2
          WalletHistoryFilter.USO_CONTRATACOES -> Icons.Default.Handshake
          WalletHistoryFilter.TODAS -> Icons.AutoMirrored.Filled.ReceiptLong
        },
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(32.dp)
      )
      Text(
        text = when {
          hasQuery -> "Nenhuma movimentação encontrada para essa busca."
          filter == WalletHistoryFilter.RECARGAS_PIX -> "Nenhuma recarga Pix registrada ainda."
          filter == WalletHistoryFilter.USO_CONTRATACOES -> "Nenhum uso de saldo em contratações registrado ainda."
          else -> "Nenhuma movimentação registrada na carteira ainda."
        },
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      if (!hasQuery && filter != WalletHistoryFilter.USO_CONTRATACOES) {
        OutlinedButton(
          onClick = onRechargeClick,
          modifier = Modifier.heightIn(min = 48.dp)
        ) {
          Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Fazer recarga Pix")
        }
      }
    }
  }
}
