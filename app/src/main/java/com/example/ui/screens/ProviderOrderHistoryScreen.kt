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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ProviderHistoryCategory
import com.example.data.ProviderHistoryFilter
import com.example.data.ProviderOrderHistoryHelper
import com.example.data.ProviderOrderHistoryItem
import com.example.data.ProviderOrderHistorySummary
import com.example.model.CancellationRefundStatus
import com.example.model.Proposal
import com.example.model.ServiceRequest
import com.example.model.ServiceReview
import com.example.model.User
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.formatUserRating
import com.example.ui.components.resolveAdaptiveSpec
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryVariant
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProviderOrderHistoryScreen(
  currentUser: User,
  services: List<ServiceRequest>,
  proposals: List<Proposal>,
  declinedServiceIds: Set<String> = emptySet(),
  reviews: List<ServiceReview> = emptyList(),
  onOpenMenu: () -> Unit = {},
  onBack: () -> Unit
) {
  BackHandler { onBack() }

  var selectedFilter by remember { mutableStateOf(ProviderHistoryFilter.TODOS) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedHistoryItem by remember { mutableStateOf<ProviderOrderHistoryItem?>(null) }

  val allHistoryItems = remember(currentUser, services, proposals, declinedServiceIds, reviews) {
    ProviderOrderHistoryHelper.buildHistory(
      provider = currentUser,
      services = services,
      proposals = proposals,
      declinedServiceIds = declinedServiceIds,
      reviews = reviews
    )
  }

  val summary = remember(allHistoryItems) {
    ProviderOrderHistoryHelper.summarize(allHistoryItems)
  }

  val filteredItems = remember(allHistoryItems, selectedFilter, searchQuery) {
    ProviderOrderHistoryHelper.filterItems(
      items = allHistoryItems,
      filter = selectedFilter,
      query = searchQuery
    )
  }

  Scaffold { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .statusBarsPadding(),
      contentAlignment = Alignment.TopCenter
    ) {
      val spec = remember(maxWidth) { resolveAdaptiveSpec(maxWidth) }

      Column(
        modifier = Modifier
          .widthIn(max = spec.contentMaxWidth)
          .fillMaxSize()
          .padding(horizontal = spec.horizontalPadding)
      ) {
        // Top Header Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            IconButton(
              onClick = onBack,
              modifier = Modifier.testTag("btn_provider_history_back")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.provider_order_history_back)
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
              Text(
                text = stringResource(R.string.provider_order_history_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = stringResource(R.string.provider_order_history_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          IconButton(
            onClick = onOpenMenu,
            modifier = Modifier.testTag("btn_provider_history_menu")
          ) {
            Icon(
              imageVector = Icons.Default.Menu,
              contentDescription = "Abrir Menu"
            )
          }
        }

        // Filter Search Bar
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = {
            Text(
              text = "Buscar no histórico por serviço, cliente ou cidade...",
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          },
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
            .testTag("provider_history_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          ProviderHistoryFilter.entries.forEach { filter ->
            val count = when (filter) {
              ProviderHistoryFilter.TODOS -> summary.totalCount
              ProviderHistoryFilter.FINALIZADO -> summary.completedCount
              ProviderHistoryFilter.CANCELADO -> summary.canceledCount
              ProviderHistoryFilter.RECUSADO -> summary.declinedCount
            }
            val selected = selectedFilter == filter
            val chipTag = "history_filter_${filter.name.lowercase()}"
            Surface(
              color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(20.dp),
              modifier = Modifier
                .heightIn(min = 40.dp)
                .clickable { selectedFilter = filter }
                .testTag(chipTag)
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
              ) {
                Text(
                  text = "${filter.label} ($count)",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                  ),
                  color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  softWrap = false
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("provider_history_list"),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = PaddingValues(bottom = 28.dp)
        ) {
          item {
            ProviderHistorySummaryBanner(summary = summary)
          }

          if (filteredItems.isEmpty()) {
            item {
              ProviderHistoryEmptyStateCard(
                filter = selectedFilter,
                hasQuery = searchQuery.isNotBlank(),
                onClearFilter = {
                  selectedFilter = ProviderHistoryFilter.TODOS
                  searchQuery = ""
                }
              )
            }
          } else {
            items(filteredItems, key = { "${it.category.name}_${it.service.id}" }) { item ->
              ProviderHistoryOrderCard(
                item = item,
                onClick = { selectedHistoryItem = item }
              )
            }
          }
        }
      }
    }
  }

  selectedHistoryItem?.let { item ->
    ProviderOrderHistoryDetailDialog(
      item = item,
      onDismiss = { selectedHistoryItem = null }
    )
  }
}

@Composable
private fun ProviderHistorySummaryBanner(summary: ProviderOrderHistorySummary) {
  val gradient = Brush.horizontalGradient(
    colors = listOf(BrandPrimaryVariant, BrandPrimary, Color(0xFF0F766E))
  )

  Card(
    shape = RoundedCornerShape(18.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("provider_history_summary_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(gradient)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Resumo de Atendimentos",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
          )
        }
        Surface(
          color = Color.White.copy(alpha = 0.18f),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(
            text = "${summary.totalCount} registro(s)",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        HistoryStatTile(
          label = "Finalizados",
          value = summary.completedCount.toString(),
          accent = Color(0xFF6EE7B7),
          modifier = Modifier.weight(1f)
        )
        HistoryStatTile(
          label = "Cancelados",
          value = summary.canceledCount.toString(),
          accent = Color(0xFFFDE68A),
          modifier = Modifier.weight(1f)
        )
        HistoryStatTile(
          label = "Recusados",
          value = summary.declinedCount.toString(),
          accent = Color(0xFFFCA5A5),
          modifier = Modifier.weight(1f)
        )
      }

      if (summary.totalCompletedValue > 0.0) {
        Surface(
          color = Color.White.copy(alpha = 0.14f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Total em serviços concluídos:",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              color = Color.White.copy(alpha = 0.9f)
            )
            Text(
              text = "R$ ${String.format("%.2f", summary.totalCompletedValue)}",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
              color = Color(0xFF6EE7B7)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HistoryStatTile(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    color = Color.White.copy(alpha = 0.12f),
    shape = RoundedCornerShape(12.dp),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
        color = accent
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        color = Color.White.copy(alpha = 0.92f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun ProviderHistoryOrderCard(
  item: ProviderOrderHistoryItem,
  onClick: () -> Unit
) {
  val service = item.service
  val displayPrice = service.finalAgreedPrice ?: item.providerProposal?.proposedPrice ?: service.proposedBudget

  val (badgeBg, badgeFg, badgeIcon) = when (item.category) {
    ProviderHistoryCategory.FINALIZADO ->
      Triple(EmeraldWalletLight, EmeraldWallet, Icons.Default.CheckCircle)
    ProviderHistoryCategory.CANCELADO ->
      Triple(WarningAmberLight, WarningAmber, Icons.Default.Cancel)
    ProviderHistoryCategory.RECUSADO ->
      Triple(DangerRedLight, DangerRed, Icons.Default.DoNotDisturbOn)
  }

  val formattedDate = remember(item.sortTimestamp) {
    SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")).format(Date(item.sortTimestamp))
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("history_order_card_${service.id}")
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = service.category,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }

        Surface(
          color = badgeBg,
          shape = RoundedCornerShape(20.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = badgeIcon,
              contentDescription = null,
              tint = badgeFg,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = item.outcomeLabel,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = badgeFg,
              maxLines = 1
            )
          }
        }
      }

      Text(
        text = service.title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        ProfileAvatar(
          name = service.clientName,
          photoUrl = service.clientPhotoUrl,
          avatarColorHex = service.clientAvatarColorHex,
          size = 30.dp
        )
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = service.clientName,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = null,
              tint = WarningAmber,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = formatUserRating(service.clientRating),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = service.location,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "R$ ${String.format("%.2f", displayPrice)}",
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.ExtraBold,
              color = if (item.category == ProviderHistoryCategory.FINALIZADO) EmeraldWallet else InfoBlue
            )
          )
          Text(
            text = formattedDate,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (!item.reasonSummary.isNullOrBlank()) {
        Surface(
          color = badgeBg.copy(alpha = 0.55f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = item.reasonSummary,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          )
        }
      }

      val refundStatus = service.cancellationRefundStatus
      if (item.category == ProviderHistoryCategory.CANCELADO &&
        refundStatus != null &&
        refundStatus != CancellationRefundStatus.NENHUM
      ) {
        val refundLabel = when (refundStatus) {
          CancellationRefundStatus.PENDENTE_ANALISE_ADMIN -> "Reembolso de taxa em análise pelo Admin"
          CancellationRefundStatus.ESTORNADO_AUTOMATICO -> "Taxa de 10% estornada automaticamente na carteira"
          CancellationRefundStatus.ESTORNADO_ADMIN -> "Taxa de 10% estornada pelo Admin"
          CancellationRefundStatus.NEGADO_ADMIN -> "Solicitação de reembolso negada pelo Admin"
          CancellationRefundStatus.NENHUM -> ""
        }
        Text(
          text = refundLabel,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
          color = InfoBlue
        )
      }

      item.review?.let { rev ->
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = WarningAmber,
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = "Avaliação recebida: ${rev.rating.toInt()}/5" +
              if (rev.comment.isNotBlank()) " — \"${rev.comment}\"" else "",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
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
private fun ProviderHistoryEmptyStateCard(
  filter: ProviderHistoryFilter,
  hasQuery: Boolean,
  onClearFilter: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("provider_history_empty_state")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Icon(
        imageVector = Icons.Default.History,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(44.dp)
      )
      Text(
        text = when {
          hasQuery -> "Nenhum pedido encontrado para esta busca"
          filter == ProviderHistoryFilter.FINALIZADO -> "Você ainda não concluiu nenhum serviço"
          filter == ProviderHistoryFilter.CANCELADO -> "Nenhum pedido cancelado registrado"
          filter == ProviderHistoryFilter.RECUSADO -> "Nenhum pedido recusado até o momento"
          else -> "Seu histórico de pedidos está vazio"
        },
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center
      )
      Text(
        text = "Pedidos finalizados, cancelados ou recusados ficam salvos aqui para você consultar quando precisar.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
      if (filter != ProviderHistoryFilter.TODOS || hasQuery) {
        OutlinedButton(
          onClick = onClearFilter,
          modifier = Modifier.heightIn(min = 48.dp)
        ) {
          Text("Ver todo o histórico", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun ProviderOrderHistoryDetailDialog(
  item: ProviderOrderHistoryItem,
  onDismiss: () -> Unit
) {
  val service = item.service
  val agreedOrProposed = service.finalAgreedPrice ?: item.providerProposal?.proposedPrice ?: service.proposedBudget
  val formattedDate = remember(item.sortTimestamp) {
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date(item.sortTimestamp))
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = service.title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
          text = "${service.category} • ${item.outcomeLabel}",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.primary
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .testTag("provider_history_detail_dialog"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          ProfileAvatar(
            name = service.clientName,
            photoUrl = service.clientPhotoUrl,
            avatarColorHex = service.clientAvatarColorHex,
            size = 38.dp
          )
          Column {
            Text(
              text = service.clientName,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "Nota do cliente: ${formatUserRating(service.clientRating)} ★",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        HorizontalDivider()

        Text(
          text = service.description,
          style = MaterialTheme.typography.bodySmall
        )

        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = "Local: ${service.location}",
              style = MaterialTheme.typography.bodySmall
            )
            Text(
              text = "Valor: R$ ${String.format("%.2f", agreedOrProposed)}",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
            )
            if (service.platformFee != null && item.category == ProviderHistoryCategory.FINALIZADO) {
              Text(
                text = "Taxa da plataforma (10%): R$ ${String.format("%.2f", service.platformFee)}",
                style = MaterialTheme.typography.bodySmall,
                color = WarningAmber
              )
            }
            Text(
              text = "Data de referência: $formattedDate",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        if (!item.reasonSummary.isNullOrBlank()) {
          Text(
            text = when (item.category) {
              ProviderHistoryCategory.FINALIZADO -> "Observações da conclusão:"
              ProviderHistoryCategory.CANCELADO -> "Motivo do cancelamento:"
              ProviderHistoryCategory.RECUSADO -> "Status da recusa:"
            },
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            text = item.reasonSummary,
            style = MaterialTheme.typography.bodySmall
          )
        }

        item.review?.let { rev ->
          HorizontalDivider()
          Text(
            text = "Avaliação do Cliente: ${rev.rating.toInt()} estrela(s)",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = WarningAmber
          )
          if (rev.comment.isNotBlank()) {
            Text(
              text = "\"${rev.comment}\"",
              style = MaterialTheme.typography.bodySmall
            )
          }
          if (!rev.providerReply.isNullOrBlank()) {
            Text(
              text = "Sua resposta: ${rev.providerReply}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("btn_close_history_detail_dialog")
      ) {
        Text("Fechar", fontWeight = FontWeight.Bold)
      }
    }
  )
}
