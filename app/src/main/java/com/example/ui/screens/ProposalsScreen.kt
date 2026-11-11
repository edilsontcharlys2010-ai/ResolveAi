package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.components.CounterOfferDialog
import com.example.ui.components.ProposalStatusBadge
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight

@Composable
fun ProposalsScreen(
  currentUser: User,
  proposals: List<Proposal>,
  services: List<ServiceRequest>,
  onAcceptProposal: (String) -> Unit,
  onSendCounterOffer: (proposalId: String, price: Double, note: String) -> Unit,
  onOpenChat: (String) -> Unit
) {
  var selectedProposalForCounter by remember { mutableStateOf<Proposal?>(null) }

  // Filter proposals relevant to the active user (either authored or received on user's services)
  val relevantProposals = proposals.filter { prop ->
    val srv = services.find { it.id == prop.serviceId }
    if (currentUser.role == UserRole.CLIENTE) {
      srv?.clientId == currentUser.id
    } else {
      prop.providerId == currentUser.id
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(16.dp))

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
            text = "Negocie valores em tempo real. Na aceitação da proposta, a taxa de 10% é deduzida da carteira do prestador.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = "Suas Negociações Ativas (${relevantProposals.size})",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
    )

    Spacer(modifier = Modifier.height(8.dp))

    if (relevantProposals.isEmpty()) {
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
            text = "Nenhuma negociação em andamento no momento",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
      ) {
        items(relevantProposals, key = { it.id }) { proposal ->
          val service = services.find { it.id == proposal.serviceId }

          ProposalNegotiationCard(
            proposal = proposal,
            service = service,
            currentUser = currentUser,
            onAccept = { onAcceptProposal(proposal.id) },
            onCounterOfferClick = { selectedProposalForCounter = proposal },
            onOpenChat = { onOpenChat(proposal.serviceId) }
          )
        }
      }
    }
  }

  // Counter offer dialog
  if (selectedProposalForCounter != null) {
    val prop = selectedProposalForCounter!!
    CounterOfferDialog(
      currentPrice = prop.currentPrice,
      onDismiss = { selectedProposalForCounter = null },
      onConfirm = { newPrice, note ->
        onSendCounterOffer(prop.id, newPrice, note)
        selectedProposalForCounter = null
      }
    )
  }
}

@Composable
private fun ProposalNegotiationCard(
  proposal: Proposal,
  service: ServiceRequest?,
  currentUser: User,
  onAccept: () -> Unit,
  onCounterOfferClick: () -> Unit,
  onOpenChat: () -> Unit
) {
  val currentPrice = proposal.currentPrice
  val platformFee10 = currentPrice * 0.10
  val isAccepted = proposal.status == ProposalStatus.ACEITA

  Card(
    colors = CardDefaults.cardColors(
      containerColor = if (isAccepted) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
    ),
    border = if (isAccepted) androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldWallet) else null,
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("proposal_card_${proposal.id}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Header: Service Title and Badge
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
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Prestador: ${proposal.providerName} (${proposal.providerCategory})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        ProposalStatusBadge(status = proposal.status)
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
      if (!isAccepted) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Counter-Offer button
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
            Text("Contraproposta", fontSize = 12.sp)
          }

          // Accept button (Debits 10% fee from provider wallet)
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
            Text("Aceitar Fechamento", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "🔒 O chat privado só aparece quando o cliente aceitar a proposta/contraproposta.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
            color = WarningAmber
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "ℹ️ Ao aceitar, o prestador paga a taxa de 10% (R$ ${String.format("%.2f", platformFee10)}) diretamente da carteira.",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldWallet, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Contrato Fechado! Taxa de 10% Paga.",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
              color = EmeraldWallet
            )
          }

          Button(
            onClick = onOpenChat,
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Ver Chat")
          }
        }
      }
    }
  }
}
