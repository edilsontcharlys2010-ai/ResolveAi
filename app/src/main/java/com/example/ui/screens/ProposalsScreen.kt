package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CounterOfferEntry
import com.example.model.Proposal
import com.example.model.ProposalStatus
import com.example.model.ServiceRequest
import com.example.model.ServiceReview
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.components.CounterOfferDialog
import com.example.ui.components.OrderParticipantCard
import com.example.ui.components.OrderParticipantsSection
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.ProposalStatusBadge
import com.example.ui.components.ProviderReviewsDialog
import com.example.ui.components.formatUserRating
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.R
import com.example.model.ServiceStatus
import com.example.ui.components.StatusBadge
import com.example.ui.components.resolveAdaptiveSpec
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight

private enum class NegotiationsFilterTab(val label: String, val tag: String) {
  TODOS("Todos", "negotiations_filter_todos"),
  EM_ANDAMENTO("Em Andamento", "negotiations_filter_em_andamento"),
  EM_NEGOCIACAO("Em Negociação", "negotiations_filter_em_negociacao"),
  RECUSADAS("Recusadas", "negotiations_filter_recusadas")
}

@Composable
fun ProposalsScreen(
  currentUser: User,
  proposals: List<Proposal>,
  services: List<ServiceRequest>,
  onAcceptProposal: (String) -> Unit,
  onSendCounterOffer: (proposalId: String, price: Double, note: String) -> Unit,
  onOpenChat: (String) -> Unit,
  onBack: () -> Unit = {},
  onRejectProposal: (String) -> Unit = {},
  reviews: List<ServiceReview> = emptyList(),
  onOpenService: (String) -> Unit = {},
  onOpenMenu: (() -> Unit)? = null
) {
  BackHandler { onBack() }

  var selectedProposalForCounter by remember { mutableStateOf<Proposal?>(null) }
  var providerReviewsModalTarget by remember { mutableStateOf<Proposal?>(null) }
  var selectedFilter by remember { mutableStateOf(NegotiationsFilterTab.TODOS) }

  // Filter proposals relevant to the active user (either authored or received on user's services)
  val relevantProposals = proposals.filter { prop ->
    val srv = services.find { it.id == prop.serviceId }
    if (currentUser.role == UserRole.CLIENTE) {
      srv != null && currentUser.ownsId(srv.clientId)
    } else {
      currentUser.ownsId(prop.providerId)
    }
  }
  val acceptedProposalIdByService = remember(proposals, services) {
    services.associate { srv ->
      val matchingProps = proposals.filter { it.serviceId == srv.id }
      val acceptedId = matchingProps.firstOrNull { it.status == ProposalStatus.ACEITA }?.id
        ?: if (!srv.acceptedProviderId.isNullOrBlank()) {
          matchingProps.firstOrNull {
            it.status != ProposalStatus.RECUSADA && it.providerId == srv.acceptedProviderId
          }?.id
        } else null
      srv.id to acceptedId
    }
  }

  val sortedAndFilteredProposals = remember(relevantProposals, services, acceptedProposalIdByService, selectedFilter) {
    relevantProposals
      .filter { prop ->
        val srv = services.find { it.id == prop.serviceId }
        val chosenId = acceptedProposalIdByService[prop.serviceId]
        val isChosen = prop.status != ProposalStatus.RECUSADA &&
          (if (chosenId != null) prop.id == chosenId else prop.status == ProposalStatus.ACEITA)
        val hasChosen = chosenId != null || !srv?.acceptedProviderId.isNullOrBlank()
        val isRefused = prop.status == ProposalStatus.RECUSADA || (!isChosen && hasChosen)
        val isInProgress = isChosen && (srv?.status == ServiceStatus.EM_EXECUCAO || srv?.status == ServiceStatus.ACEITO)
        val isOpenNegotiation = !isChosen && !isRefused &&
          srv?.status != ServiceStatus.CONCLUIDO && srv?.status != ServiceStatus.CANCELADO

        when (selectedFilter) {
          NegotiationsFilterTab.TODOS -> true
          NegotiationsFilterTab.EM_ANDAMENTO -> isInProgress
          NegotiationsFilterTab.EM_NEGOCIACAO -> isOpenNegotiation
          NegotiationsFilterTab.RECUSADAS -> isRefused
        }
      }
      .sortedWith(
        compareBy<Proposal> { prop ->
          val srv = services.find { it.id == prop.serviceId }
          val chosenId = acceptedProposalIdByService[prop.serviceId]
          val isChosen = prop.status != ProposalStatus.RECUSADA &&
            (if (chosenId != null) prop.id == chosenId else prop.status == ProposalStatus.ACEITA)
          val hasChosen = chosenId != null || !srv?.acceptedProviderId.isNullOrBlank()
          val isRefused = prop.status == ProposalStatus.RECUSADA || (!isChosen && hasChosen)
          val isInProgress = isChosen && (srv?.status == ServiceStatus.EM_EXECUCAO || srv?.status == ServiceStatus.ACEITO)
          when {
            isInProgress -> 0
            !isChosen && !isRefused && srv?.status != ServiceStatus.CONCLUIDO && srv?.status != ServiceStatus.CANCELADO -> 1
            srv?.status == ServiceStatus.CONCLUIDO -> 2
            else -> 3
          }
        }.thenByDescending { it.createdAt }
      )
  }

  val inProgressCount = remember(relevantProposals, services, acceptedProposalIdByService) {
    relevantProposals.count { prop ->
      val srv = services.find { it.id == prop.serviceId }
      val chosenId = acceptedProposalIdByService[prop.serviceId]
      val isChosen = prop.status != ProposalStatus.RECUSADA &&
        (if (chosenId != null) prop.id == chosenId else prop.status == ProposalStatus.ACEITA)
      isChosen && (srv?.status == ServiceStatus.EM_EXECUCAO || srv?.status == ServiceStatus.ACEITO)
    }
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding(),
    contentAlignment = Alignment.TopCenter
  ) {
    val spec = remember(maxWidth) { resolveAdaptiveSpec(maxWidth) }

    Column(
      modifier = Modifier
        .widthIn(max = 1140.dp)
        .fillMaxSize()
        .padding(horizontal = spec.horizontalPadding)
    ) {
      // Back navigation header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (onOpenMenu != null) {
          IconButton(
            onClick = onOpenMenu,
            modifier = Modifier.testTag("btn_proposals_open_menu")
          ) {
            Icon(Icons.Default.Menu, contentDescription = "Abrir Menu")
          }
        }
        IconButton(onClick = onBack) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Voltar para Serviços")
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.negotiations_screen_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = if (currentUser.role == UserRole.CLIENTE) {
              "Propostas recebidas e pedidos em andamento"
            } else {
              "Suas propostas enviadas e pedidos em andamento"
            },
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Header explanation
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Handshake,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Módulo de Negociação & Contrapropostas",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = if (currentUser.role == UserRole.CLIENTE) {
                "Aceite uma proposta para mover o pedido automaticamente para Em Andamento e liberar o chat."
              } else {
                "Negocie valores em tempo real. Ao o cliente aceitar, o pedido fica Em Andamento aqui e em Minhas Propostas."
              },
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        NegotiationsFilterTab.entries.forEach { tab ->
          val isSelected = selectedFilter == tab
          val labelText = if (tab == NegotiationsFilterTab.EM_ANDAMENTO && inProgressCount > 0) {
            "${tab.label} ($inProgressCount)"
          } else {
            tab.label
          }
          Surface(
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
              .heightIn(min = 36.dp)
              .clickable { selectedFilter = tab }
              .testTag(tab.tag)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = labelText,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                  fontSize = 12.sp
                ),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = when (selectedFilter) {
          NegotiationsFilterTab.EM_ANDAMENTO -> "Pedidos Em Andamento (${sortedAndFilteredProposals.size})"
          NegotiationsFilterTab.EM_NEGOCIACAO -> "Propostas em Negociação (${sortedAndFilteredProposals.size})"
          NegotiationsFilterTab.RECUSADAS -> "Propostas Recusadas (${sortedAndFilteredProposals.size})"
          NegotiationsFilterTab.TODOS -> "Suas Negociações Ativas (${sortedAndFilteredProposals.size})"
        },
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )

      Spacer(modifier = Modifier.height(8.dp))

      if (sortedAndFilteredProposals.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.SwapHoriz,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = when (selectedFilter) {
                NegotiationsFilterTab.EM_ANDAMENTO -> "Nenhum pedido em andamento no momento"
                NegotiationsFilterTab.RECUSADAS -> "Nenhuma proposta recusada"
                else -> "Nenhuma negociação em andamento no momento"
              },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else if (spec.isTablet) {
        LazyVerticalGrid(
          columns = GridCells.Adaptive(minSize = 350.dp),
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .testTag("proposals_list_lazy_column"),
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
          contentPadding = PaddingValues(bottom = 32.dp)
        ) {
          items(sortedAndFilteredProposals, key = { it.id }) { proposal ->
            val service = services.find { it.id == proposal.serviceId }
            val chosenId = acceptedProposalIdByService[proposal.serviceId]
            val isChosen = proposal.status != ProposalStatus.RECUSADA &&
              (if (chosenId != null) proposal.id == chosenId else proposal.status == ProposalStatus.ACEITA)
            ProposalNegotiationCard(
              proposal = proposal,
              service = service,
              currentUser = currentUser,
              isChosenProposal = isChosen,
              hasChosenProviderForService = chosenId != null || !service?.acceptedProviderId.isNullOrBlank(),
              providerReviewsCount = reviews.count { it.providerId == proposal.providerId },
              isNarrowPhone = spec.isNarrowPhone,
              onAccept = { onAcceptProposal(proposal.id) },
              onReject = { onRejectProposal(proposal.id) },
              onCounterOfferClick = { selectedProposalForCounter = proposal },
              onOpenChat = { onOpenChat(proposal.serviceId) },
              onOpenService = { onOpenService(proposal.serviceId) },
              onViewProviderReviews = { providerReviewsModalTarget = proposal }
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .testTag("proposals_list_lazy_column"),
          verticalArrangement = Arrangement.spacedBy(14.dp),
          contentPadding = PaddingValues(bottom = 32.dp)
        ) {
          items(sortedAndFilteredProposals, key = { it.id }) { proposal ->
            val service = services.find { it.id == proposal.serviceId }
            val chosenId = acceptedProposalIdByService[proposal.serviceId]
            val isChosen = proposal.status != ProposalStatus.RECUSADA &&
              (if (chosenId != null) proposal.id == chosenId else proposal.status == ProposalStatus.ACEITA)

            ProposalNegotiationCard(
              proposal = proposal,
              service = service,
              currentUser = currentUser,
              isChosenProposal = isChosen,
              hasChosenProviderForService = chosenId != null || !service?.acceptedProviderId.isNullOrBlank(),
              providerReviewsCount = reviews.count { it.providerId == proposal.providerId },
              isNarrowPhone = spec.isNarrowPhone,
              onAccept = { onAcceptProposal(proposal.id) },
              onReject = { onRejectProposal(proposal.id) },
              onCounterOfferClick = { selectedProposalForCounter = proposal },
              onOpenChat = { onOpenChat(proposal.serviceId) },
              onOpenService = { onOpenService(proposal.serviceId) },
              onViewProviderReviews = { providerReviewsModalTarget = proposal }
            )
          }
        }
      }
    }
  }

  // Counter offer dialog
  if (selectedProposalForCounter != null) {
    val prop = selectedProposalForCounter!!
    CounterOfferDialog(
      currentPrice = prop.currentPrice,
      currentUserRole = currentUser.role,
      onDismiss = { selectedProposalForCounter = null },
      onConfirm = { newPrice, note ->
        onSendCounterOffer(prop.id, newPrice, note)
        selectedProposalForCounter = null
      }
    )
  }

  providerReviewsModalTarget?.let { prop ->
    val providerReviews = reviews
      .filter { it.providerId == prop.providerId }
      .sortedByDescending { it.createdAt }
    ProviderReviewsDialog(
      providerName = prop.providerName,
      providerCategory = prop.providerCategory,
      providerPhotoUrl = prop.providerPhotoUrl,
      providerAvatarColorHex = prop.providerAvatarColorHex,
      averageRating = prop.providerRating,
      reviews = providerReviews,
      onDismiss = { providerReviewsModalTarget = null }
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProposalNegotiationCard(
  proposal: Proposal,
  service: ServiceRequest?,
  currentUser: User,
  isChosenProposal: Boolean = proposal.status == ProposalStatus.ACEITA,
  hasChosenProviderForService: Boolean = !service?.acceptedProviderId.isNullOrBlank(),
  providerReviewsCount: Int = 0,
  isNarrowPhone: Boolean = false,
  onAccept: () -> Unit,
  onReject: () -> Unit = {},
  onCounterOfferClick: () -> Unit,
  onOpenChat: () -> Unit,
  onOpenService: () -> Unit = {},
  onViewProviderReviews: () -> Unit = {}
) {
  val currentPrice = proposal.currentPrice
  val platformFee10 = currentPrice * 0.10
  val isAccepted = isChosenProposal && proposal.status != ProposalStatus.RECUSADA
  val isRefused = proposal.status == ProposalStatus.RECUSADA ||
    (!isAccepted && hasChosenProviderForService)
  val canClientSeeProviderProfileWithRating = currentUser.role != UserRole.CLIENTE || isAccepted

  Card(
    colors = CardDefaults.cardColors(
      containerColor = if (isAccepted) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
    ),
    border = if (isAccepted) BorderStroke(1.5.dp, EmeraldWallet) else null,
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("proposal_card_${proposal.id}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Header: Service Title and Badges (Service Status + Proposal Status)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = service?.title ?: "Serviço #${proposal.serviceId}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 2
          )
          if (canClientSeeProviderProfileWithRating && proposal.providerCategory.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = proposal.providerCategory,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          if (service != null) {
            StatusBadge(status = service.status)
          }
          ProposalStatusBadge(
            status = when {
              isAccepted -> ProposalStatus.ACEITA
              isRefused -> ProposalStatus.RECUSADA
              else -> proposal.status
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      if (service != null) {
        OrderParticipantsSection(
          service = service,
          latestProposal = if (isAccepted) proposal else null
        )
      }

      if (canClientSeeProviderProfileWithRating) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewProviderReviews() }
            .testTag("btn_proposal_view_reviews_${proposal.providerId}")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              ProfileAvatar(
                name = proposal.providerName,
                photoUrl = proposal.providerPhotoUrl,
                avatarColorHex = proposal.providerAvatarColorHex,
                size = 32.dp
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = proposal.providerName,
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Star, contentDescription = "Nota", tint = WarningAmber, modifier = Modifier.size(13.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = formatUserRating(proposal.providerRating),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "($providerReviewsCount avaliações)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
            Text(
              text = "Ver avaliações",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      } else {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Prestador interessado: ${proposal.providerName}",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Current Price Box
      Surface(
        color = if (isAccepted) EmeraldWalletLight else WarningAmberLight,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = if (isAccepted) "Valor Final Fechado" else "Valor em Negociação",
              style = MaterialTheme.typography.labelSmall,
              color = if (isAccepted) EmeraldWallet else WarningAmber
            )
            Text(
              text = "R$ ${String.format("%.2f", currentPrice)}",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = if (isAccepted) EmeraldWallet else WarningAmber
              )
            )
          }

          if (currentUser.role != UserRole.CLIENTE) {
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "Taxa da Plataforma (10%)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.AccountBalanceWallet,
                  contentDescription = null,
                  tint = if (isAccepted) EmeraldWallet else WarningAmber,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "R$ ${String.format("%.2f", platformFee10)}",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = if (isAccepted) EmeraldWallet else WarningAmber
                )
              }
            }
          }
        }
      }

      // History of counter-offers if any
      if (proposal.counterOffers.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Histórico de Contrapropostas:",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Initial provider proposal
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Proposta inicial (Prestador):", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
            Text("R$ ${String.format("%.2f", proposal.proposedPrice)}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
          }

          proposal.counterOffers.forEach { entry ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Contraproposta de ${entry.authorName} (${if (entry.authorRole == UserRole.CLIENTE) "Cliente" else "Prestador"}):",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                )
                if (entry.note.isNotBlank()) {
                  Text(
                    text = "\"${entry.note}\"",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
              Text(
                text = "R$ ${String.format("%.2f", entry.price)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Action buttons
      if (!isAccepted && !isRefused) {
        val waitingClientAfterDirectAccept =
          currentUser.role == UserRole.PRESTADOR &&
            proposal.isDirectAcceptance &&
            proposal.status == ProposalStatus.ENVIADA

        if (proposal.isDirectAcceptance) {
          Surface(
            color = EmeraldWalletLight,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 8.dp)
          ) {
            Text(
              text = "✓ Prestador aceitou fazer o serviço no valor proposto",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
              color = Color(0xFF065F46),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
        }

        val acceptButtonLabel = if (proposal.isDirectAcceptance && currentUser.role == UserRole.CLIENTE) {
          "Aceitar Prestador"
        } else {
          "Aceitar Fechamento"
        }

        if (isNarrowPhone) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = onCounterOfferClick,
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_counter_offer_${proposal.id}")
            ) {
              Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Contraproposta", fontSize = 12.sp, maxLines = 1, softWrap = false)
            }

            if (currentUser.role == UserRole.CLIENTE) {
              OutlinedButton(
                onClick = onReject,
                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.65f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("btn_reject_proposal_${proposal.id}")
              ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Recusar", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
              }
            }

            if (!waitingClientAfterDirectAccept) {
              Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("btn_accept_proposal_${proposal.id}")
              ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(acceptButtonLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
              }
            }
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = onCounterOfferClick,
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 8.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("btn_counter_offer_${proposal.id}")
            ) {
              Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Contraproposta", fontSize = 12.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }

            if (currentUser.role == UserRole.CLIENTE) {
              OutlinedButton(
                onClick = onReject,
                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.65f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                  .weight(0.8f)
                  .testTag("btn_reject_proposal_${proposal.id}")
              ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Recusar", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
              }
            }

            if (!waitingClientAfterDirectAccept) {
              Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("btn_accept_proposal_${proposal.id}")
              ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(acceptButtonLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = if (waitingClientAfterDirectAccept) {
              "⏳ Aguardando o cliente escolher e confirmar você para abrir o chat privado."
            } else {
              "🔒 O chat privado só aparece quando o cliente aceitar o prestador/proposta."
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
            color = WarningAmber
          )
        }
        if (currentUser.role != UserRole.CLIENTE) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "ℹ️ Ao o cliente aceitar, a taxa de 10% (R$ ${String.format("%.2f", platformFee10)}) é debitada da sua carteira.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      } else if (isRefused) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Cancel, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = when {
                currentUser.role == UserRole.CLIENTE && hasChosenProviderForService ->
                  "Outro prestador foi escolhido para este pedido."
                currentUser.role == UserRole.CLIENTE ->
                  "Prestador recusado para este atendimento."
                else ->
                  "Outro prestador foi selecionado ou a proposta foi recusada. Serviço indisponível."
              },
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldWallet, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = when {
                service?.status == ServiceStatus.CONCLUIDO -> "Serviço Concluído!"
                currentUser.role == UserRole.CLIENTE -> "Pedido Em Andamento!"
                else -> "Pedido Em Andamento! Taxa de 10% Paga."
              },
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
              color = EmeraldWallet
            )
          }

          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = onOpenService,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("btn_proposal_open_service_${proposal.id}")
            ) {
              Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(stringResource(R.string.negotiations_view_service_cta), maxLines = 1, softWrap = false)
            }

            Button(
              onClick = onOpenChat,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("btn_proposal_open_chat_${proposal.id}")
            ) {
              Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Ver Chat", maxLines = 1, softWrap = false)
            }
          }
        }
      }
    }
  }
}
