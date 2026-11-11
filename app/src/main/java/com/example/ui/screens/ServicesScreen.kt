package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import com.example.model.ClientLocation
import com.example.ui.theme.DangerRed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Proposal
import com.example.model.ServiceRequest
import com.example.model.ServiceStatus
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.components.StatusBadge
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight

@Composable
fun ServicesScreen(
  currentUser: User,
  services: List<ServiceRequest>,
  proposals: List<Proposal>,
  selectedServiceId: String?,
  searchQuery: String,
  categoryFilter: String,
  onSearchChange: (String) -> Unit,
  onCategoryChange: (String) -> Unit,
  onSelectService: (String?) -> Unit,
  onCreateServiceClick: () -> Unit,
  onSubmitProposal: (serviceId: String, price: Double, days: Int, desc: String) -> Unit,
  onOpenChat: (String) -> Unit,
  onUpdateStatus: (String, ServiceStatus) -> Unit,
  onRateService: (String, Float, String) -> Unit,
  onEditBudget: (ServiceRequest) -> Unit = {},
  onAcceptProposal: (String) -> Unit = {}
) {
  val selectedService = services.find { it.id == selectedServiceId }

  if (selectedService != null) {
    BackHandler {
      onSelectService(null)
    }
    ServiceDetailView(
      service = selectedService,
      currentUser = currentUser,
      proposals = proposals.filter { it.serviceId == selectedService.id },
      onBack = { onSelectService(null) },
      onSubmitProposal = onSubmitProposal,
      onOpenChat = { onOpenChat(selectedService.id) },
      onUpdateStatus = onUpdateStatus,
      onRateService = onRateService,
      onEditBudget = onEditBudget,
      onAcceptProposal = onAcceptProposal
    )
  } else {
    ServicesListView(
      currentUser = currentUser,
      services = services,
      proposals = proposals,
      searchQuery = searchQuery,
      categoryFilter = categoryFilter,
      onSearchChange = onSearchChange,
      onCategoryChange = onCategoryChange,
      onSelectService = onSelectService,
      onCreateServiceClick = onCreateServiceClick,
      onOpenChat = onOpenChat,
      onEditBudget = onEditBudget
    )
  }
}

@Composable
private fun ServicesListView(
  currentUser: User,
  services: List<ServiceRequest>,
  proposals: List<Proposal>,
  searchQuery: String,
  categoryFilter: String,
  onSearchChange: (String) -> Unit,
  onCategoryChange: (String) -> Unit,
  onSelectService: (String) -> Unit,
  onCreateServiceClick: () -> Unit,
  onOpenChat: (String) -> Unit,
  onEditBudget: (ServiceRequest) -> Unit = {}
) {
  var showOnlyMyServices by remember { mutableStateOf(false) }

  val categories = listOf("Todos", "Eletricista", "Pintura", "TI, Redes & Informática", "Hidráulica", "Limpeza", "Montagem")

  val filteredServices = services.filter { srv ->
    val matchesSearch = srv.title.contains(searchQuery, ignoreCase = true) ||
      srv.description.contains(searchQuery, ignoreCase = true) ||
      srv.category.contains(searchQuery, ignoreCase = true)

    val matchesCat = categoryFilter == "Todos" || srv.category.contains(categoryFilter, ignoreCase = true)

    val matchesOwnership = if (showOnlyMyServices) {
      if (currentUser.role == UserRole.CLIENTE) {
        srv.clientId == currentUser.id
      } else {
        srv.acceptedProviderId == currentUser.id || proposals.any { it.serviceId == srv.id && it.providerId == currentUser.id }
      }
    } else true

    matchesSearch && matchesCat && matchesOwnership
  }

  Scaffold(
    floatingActionButton = {
      if (currentUser.role == UserRole.CLIENTE) {
        FloatingActionButton(
          onClick = onCreateServiceClick,
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = Color.White,
          modifier = Modifier.testTag("fab_create_service")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Add, contentDescription = "Solicitar")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Novo Pedido", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(12.dp))

      // Search field
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        placeholder = { Text("Buscar serviços, categorias...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("search_services_input")
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Category horizontal scroll
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        categories.forEach { cat ->
          val isSelected = categoryFilter == cat
          Surface(
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.clickable { onCategoryChange(cat) }
          ) {
            Text(
              text = cat,
              color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Filter tabs: Todos vs Meus
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (currentUser.role == UserRole.CLIENTE) {
            "Solicitações de Serviços (${filteredServices.size})"
          } else {
            "Oportunidades de Trabalho (${filteredServices.size})"
          },
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        Row {
          Surface(
            color = if (!showOnlyMyServices) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.clickable { showOnlyMyServices = false }
          ) {
            Text(
              text = "Todos",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = if (!showOnlyMyServices) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
          Spacer(modifier = Modifier.width(4.dp))
          Surface(
            color = if (showOnlyMyServices) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.clickable { showOnlyMyServices = true }
          ) {
            Text(
              text = if (currentUser.role == UserRole.CLIENTE) "Meus Pedidos" else "Minhas Propostas",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = if (showOnlyMyServices) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      if (filteredServices.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.FilterList,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Nenhum serviço encontrado",
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
          verticalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = PaddingValues(bottom = 80.dp)
        ) {
          items(filteredServices, key = { it.id }) { service ->
            val serviceProposals = proposals.filter { it.serviceId == service.id }
            ServiceItemCard(
              service = service,
              currentUser = currentUser,
              proposalCount = serviceProposals.size,
              onClick = { onSelectService(service.id) },
              onOpenChat = { onOpenChat(service.id) },
              onEditBudget = { onEditBudget(service) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ServiceItemCard(
  service: ServiceRequest,
  currentUser: User,
  proposalCount: Int,
  onClick: () -> Unit,
  onOpenChat: () -> Unit,
  onEditBudget: () -> Unit = {}
) {
  val estimatedFee = (service.finalAgreedPrice ?: service.proposedBudget) * 0.10
  val isChatUnlocked = service.status == ServiceStatus.ACEITO ||
    service.status == ServiceStatus.EM_EXECUCAO ||
    service.status == ServiceStatus.CONCLUIDO
  val canEditBudget = (currentUser.id == service.clientId || currentUser.role == UserRole.CLIENTE) &&
    (service.status == ServiceStatus.ABERTO || service.status == ServiceStatus.EM_NEGOCIACAO)

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("service_card_${service.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Header: Category and Status
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

        StatusBadge(status = service.status)
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Title
      Text(
        text = service.title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(4.dp))

      // Description snippet
      Text(
        text = service.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Location & Client
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.LocationOn,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = service.location,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = "• Solicitado por ${service.clientName}",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Travel time indicator for provider
      if (currentUser.role == UserRole.PRESTADOR) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            color = Color(0xFFEFF6FF),
            shape = RoundedCornerShape(6.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = InfoBlue,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Deslocamento: ~${service.effectiveLocation.drivingTimeMinutes} min (${service.effectiveLocation.distanceKm} km)",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = InfoBlue
              )
            }
          }
          Text(
            text = "• ${service.effectiveLocation.trafficCondition}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Option to change proposed value on the requested service
      if (canEditBudget) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditBudget() }
            .testTag("btn_quick_change_budget_${service.id}")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Mudar valor proposto",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
              )
            }
            Text(
              text = "Alterar Orçamento ✏️",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
      }

      // Footer: Budget, Platform fee info, Action
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = if (service.finalAgreedPrice != null) "Valor Acordado" else "Valor Proposto",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = "R$ ${String.format("%.2f", service.finalAgreedPrice ?: service.proposedBudget)}",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (service.status == ServiceStatus.ACEITO) EmeraldWallet else MaterialTheme.colorScheme.primary
              )
            )
          }
          // Fee explanation badge
          Text(
            text = "Taxa plataforma (10%): R$ ${String.format("%.2f", estimatedFee)}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
            color = WarningAmber
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Chat option: only appears when proposal has been accepted!
          if (isChatUnlocked) {
            IconButton(
              onClick = onOpenChat,
              modifier = Modifier.testTag("btn_quick_chat_${service.id}")
            ) {
              Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "Chat",
                tint = MaterialTheme.colorScheme.primary
              )
            }
          }

          Button(
            onClick = onClick,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("btn_view_service_${service.id}")
          ) {
            Text(
              text = if (proposalCount > 0) "$proposalCount propostas" else "Ver detalhes",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ServiceDetailView(
  service: ServiceRequest,
  currentUser: User,
  proposals: List<Proposal>,
  onBack: () -> Unit,
  onSubmitProposal: (String, Double, Int, String) -> Unit,
  onOpenChat: () -> Unit,
  onUpdateStatus: (String, ServiceStatus) -> Unit,
  onRateService: (String, Float, String) -> Unit,
  onEditBudget: (ServiceRequest) -> Unit = {},
  onAcceptProposal: (String) -> Unit = {}
) {
  var showSendProposalDialog by remember { mutableStateOf(false) }
  var showRateDialog by remember { mutableStateOf(false) }
  var showRouteDialog by remember { mutableStateOf(false) }
  val context = LocalContext.current

  val isChatUnlocked = service.status == ServiceStatus.ACEITO ||
    service.status == ServiceStatus.EM_EXECUCAO ||
    service.status == ServiceStatus.CONCLUIDO
  val canEditBudget = (currentUser.id == service.clientId || currentUser.role == UserRole.CLIENTE) &&
    (service.status == ServiceStatus.ABERTO || service.status == ServiceStatus.EM_NEGOCIACAO)
  val canAcceptProposals = (currentUser.id == service.clientId || currentUser.role == UserRole.CLIENTE) &&
    (service.status == ServiceStatus.ABERTO || service.status == ServiceStatus.EM_NEGOCIACAO)
  val latestPendingProposal = proposals.lastOrNull()

  Scaffold(
    topBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBack) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
        }
        Text(
          text = "Detalhes do Serviço",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      }
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(bottom = 40.dp)
    ) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              StatusBadge(status = service.status)
              Text(
                text = "Urgência: ${service.urgency}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = WarningAmber
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = service.title,
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = service.description,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // LOCALIZAÇÃO DO CLIENTE & TEMPO DE DESLOCAMENTO
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("client_location_card_${service.id}")
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.LocationOn,
                      contentDescription = null,
                      tint = DangerRed,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Localização do Cliente",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                  }

                  // Distance badge
                  Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp)
                  ) {
                    Text(
                      text = "${service.effectiveLocation.distanceKm} km de você",
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                      ),
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Full address & neighborhood
                Text(
                  text = service.effectiveLocation.address,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                  text = "${service.effectiveLocation.neighborhood} • ${service.effectiveLocation.city}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Reference point
                if (service.effectiveLocation.referencePoint.isNotBlank()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Row(verticalAlignment = Alignment.Top) {
                    Icon(
                      imageVector = Icons.Default.Place,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(14.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "Ref: ${service.effectiveLocation.referencePoint}",
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Travel time section header
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "⏱️ Tempo estimado de deslocamento:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = service.effectiveLocation.trafficCondition,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                    color = if (service.effectiveLocation.trafficCondition.contains("moderado", ignoreCase = true)) WarningAmber else EmeraldWallet
                  )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Transport mode transit pills
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  // Car
                  Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Column(
                      modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                      horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                      Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.height(2.dp))
                      Text("Carro", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                      Text(
                        "${service.effectiveLocation.drivingTimeMinutes} min",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = InfoBlue
                      )
                    }
                  }

                  // Moto
                  Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Column(
                      modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                      horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                      Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.height(2.dp))
                      Text("Moto", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                      Text(
                        "${service.effectiveLocation.motoTimeMinutes} min",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = WarningAmber
                      )
                    }
                  }

                  // Public Transit
                  Surface(
                    color = Color(0xFFF3F4F6),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Column(
                      modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                      horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                      Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = Color(0xFF4B5563), modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.height(2.dp))
                      Text("Ônibus/Metrô", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                      Text(
                        "${service.effectiveLocation.transitTimeMinutes} min",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = Color(0xFF374151)
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Best route advice & Action buttons
                Text(
                  text = "Via: ${service.effectiveLocation.bestRoute}",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  OutlinedButton(
                    onClick = { showRouteDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                      .weight(1f)
                      .testTag("btn_view_route_details")
                  ) {
                    Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver Rota e Custos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }

                  Button(
                    onClick = {
                      val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode("${service.effectiveLocation.address}, ${service.effectiveLocation.city}")}")
                      val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                        setPackage("com.google.android.apps.maps")
                      }
                      try {
                        context.startActivity(mapIntent)
                      } catch (e: Exception) {
                        showRouteDialog = true
                      }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = InfoBlue),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                      .weight(1f)
                      .testTag("btn_open_gps")
                  ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Traçar GPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text("Valor Proposto pelo Cliente:", style = MaterialTheme.typography.bodySmall)
                  Text(
                    "R$ ${String.format("%.2f", service.proposedBudget)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                  )
                }

                if (service.finalAgreedPrice != null) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text("Valor Final Acordado:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                      "R$ ${String.format("%.2f", service.finalAgreedPrice)}",
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                      color = EmeraldWallet
                    )
                  }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text("Taxa de 10% (debitada da carteira):", style = MaterialTheme.typography.bodySmall)
                  Text(
                    "R$ ${String.format("%.2f", (service.finalAgreedPrice ?: service.proposedBudget) * 0.10)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = WarningAmber
                  )
                }

                if (service.acceptedProviderName != null) {
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = "Prestador Contratado: ${service.acceptedProviderName}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = EmeraldWallet
                  )
                }

                // Client can edit the proposed value on this service OR accept counter proposal side-by-side
                if (canEditBudget) {
                  Spacer(modifier = Modifier.height(10.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    // MUDAR VALOR BUTTON
                    Button(
                      onClick = { onEditBudget(service) },
                      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier
                        .weight(1f)
                        .testTag("btn_detail_edit_budget")
                    ) {
                      Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Mudar Valor", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // ACEITAR BUTTON (lado a lado de Mudar Valor)
                    if (canAcceptProposals && latestPendingProposal != null) {
                      Button(
                        onClick = { onAcceptProposal(latestPendingProposal.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                          .weight(1f)
                          .testTag("btn_detail_accept_proposal_${latestPendingProposal.id}")
                      ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aceitar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                      }
                    } else if (service.status == ServiceStatus.ABERTO && service.acceptedProviderId == null && proposals.isNotEmpty()) {
                      Button(
                        onClick = { onAcceptProposal(proposals.first().id) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                          .weight(1f)
                          .testTag("btn_detail_accept_first_proposal")
                      ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aceitar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                      }
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            if (isChatUnlocked) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Button(
                  onClick = onOpenChat,
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("btn_detail_open_chat")
                ) {
                  Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Abrir Chat Privado")
                }

                if (currentUser.role == UserRole.PRESTADOR && service.status == ServiceStatus.ABERTO) {
                  Button(
                    onClick = { showSendProposalDialog = true },
                    modifier = Modifier
                      .weight(1f)
                      .testTag("btn_detail_send_proposal")
                  ) {
                    Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Enviar Proposta")
                  }
                }
              }
            } else {
              // Locked Chat notification: Only appears when proposal is accepted
              Surface(
                color = WarningAmberLight,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = "🔒 Chat Privado Bloqueado",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = Color(0xFF78350F)
                    )
                    Text(
                      text = "A opção de chat só aparece e é liberada quando o cliente aceitar a contraproposta.",
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                      color = Color(0xFF92400E)
                    )
                  }
                }
              }

              if (currentUser.role == UserRole.PRESTADOR && service.status == ServiceStatus.ABERTO) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                  onClick = { showSendProposalDialog = true },
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_detail_send_proposal")
                ) {
                  Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Enviar Proposta de Trabalho")
                }
              }
            }

            // Lifecycle transitions
            if (service.status == ServiceStatus.ACEITO) {
              Spacer(modifier = Modifier.height(8.dp))
              Button(
                onClick = { onUpdateStatus(service.id, ServiceStatus.EM_EXECUCAO) },
                colors = ButtonDefaults.buttonColors(containerColor = InfoBlue),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text("Iniciar Execução do Serviço")
              }
            } else if (service.status == ServiceStatus.EM_EXECUCAO) {
              Spacer(modifier = Modifier.height(8.dp))
              Button(
                onClick = { onUpdateStatus(service.id, ServiceStatus.CONCLUIDO) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
                modifier = Modifier.fillMaxWidth()
              ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Finalizar & Concluir Serviço")
              }
            } else if (service.status == ServiceStatus.CONCLUIDO && currentUser.role == UserRole.CLIENTE && service.ratingGiven == null) {
              Spacer(modifier = Modifier.height(8.dp))
              Button(
                onClick = { showRateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                modifier = Modifier.fillMaxWidth()
              ) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Avaliar Prestador")
              }
            }

            if (service.ratingGiven != null) {
              Spacer(modifier = Modifier.height(8.dp))
              Surface(
                color = Color(0xFFFEF3C7),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Avaliação do Cliente: ${service.ratingGiven} estrelas", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                  }
                  if (!service.reviewComment.isNullOrBlank()) {
                    Text(
                      text = "\"${service.reviewComment}\"",
                      style = MaterialTheme.typography.bodySmall,
                      color = Color(0xFF78350F)
                    )
                  }
                }
              }
            }
          }
        }
      }

      item {
        Text(
          text = "Propostas Recebidas (${proposals.size})",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      }

      if (proposals.isEmpty()) {
        item {
          Text(
            text = "Nenhuma proposta enviada ainda. Prestadores da categoria podem visualizar e enviar valores.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      } else {
        items(proposals) { proposal ->
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = proposal.providerName,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                  text = "R$ ${String.format("%.2f", proposal.currentPrice)}",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                )
              }
              Text(
                text = "Previsão: ${proposal.estimatedDays} dia(s) • ${proposal.description}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              if (canAcceptProposals && (service.status == ServiceStatus.ABERTO || service.status == ServiceStatus.EM_NEGOCIACAO)) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                  onClick = { onAcceptProposal(proposal.id) },
                  colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_accept_proposal_card_${proposal.id}")
                ) {
                  Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Aceitar Proposta deste Prestador", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
              }
            }
          }
        }
      }
    }
  }

  // Send Proposal Dialog
  if (showSendProposalDialog) {
    var priceText by remember { mutableStateOf(String.format("%.2f", service.proposedBudget)) }
    var daysText by remember { mutableStateOf("1") }
    var descText by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showSendProposalDialog = false },
      title = { Text("Enviar Proposta de Trabalho") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "O cliente propôs inicialmente R$ ${String.format("%.2f", service.proposedBudget)}. Você pode manter ou propor outro valor.",
            style = MaterialTheme.typography.bodySmall
          )
          OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("Seu Valor Proposto (R$)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = daysText,
            onValueChange = { daysText = it.filter { c -> c.isDigit() } },
            label = { Text("Prazo estimado (dias)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = descText,
            onValueChange = { descText = it },
            label = { Text("Mensagem / O que está incluso") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val price = priceText.replace(",", ".").toDoubleOrNull() ?: service.proposedBudget
            val days = daysText.toIntOrNull() ?: 1
            onSubmitProposal(service.id, price, days, descText)
            showSendProposalDialog = false
          }
        ) {
          Text("Confirmar Proposta")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSendProposalDialog = false }) {
          Text("Cancelar")
        }
      }
    )
  }

  // Rate Service Dialog
  if (showRateDialog) {
    var rating by remember { mutableFloatStateOf(5.0f) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showRateDialog = false },
      title = { Text("Avaliar Serviço Concluído") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Como foi a experiência com o prestador?", style = MaterialTheme.typography.bodySmall)

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
          ) {
            (1..5).forEach { star ->
              IconButton(onClick = { rating = star.toFloat() }) {
                Icon(
                  imageVector = Icons.Default.Star,
                  contentDescription = null,
                  tint = if (star <= rating) WarningAmber else Color.LightGray,
                  modifier = Modifier.size(32.dp)
                )
              }
            }
          }

          OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Deixe seu comentário") },
            placeholder = { Text("Ex: Excelente profissional, rápido e limpo!") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onRateService(service.id, rating, comment)
            showRateDialog = false
          }
        ) {
          Text("Enviar Avaliação")
        }
      },
      dismissButton = {
        TextButton(onClick = { showRateDialog = false }) {
          Text("Cancelar")
        }
      }
    )
  }

  // Client Route Details Dialog
  if (showRouteDialog) {
    ClientRouteDetailsDialog(
      service = service,
      onDismiss = { showRouteDialog = false }
    )
  }
}

@Composable
fun ClientRouteDetailsDialog(
  service: ServiceRequest,
  onDismiss: () -> Unit
) {
  val loc = service.effectiveLocation
  val context = LocalContext.current
  val estimatedFuelCost = loc.distanceKm * 0.75 // Approx R$ 0,75/km in Brazil

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Route, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Deslocamento & Rota", fontWeight = FontWeight.Bold, fontSize = 18.sp)
      }
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        // Destination card
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text("Destino do Cliente:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(loc.address, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("${loc.neighborhood} • ${loc.city}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (loc.referencePoint.isNotBlank()) {
              Spacer(modifier = Modifier.height(2.dp))
              Text("Ref: ${loc.referencePoint}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.primary)
            }
          }
        }

        // Distance & Times breakdown
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text("Distância:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${loc.distanceKm} km", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
          }
          Column(horizontalAlignment = Alignment.End) {
            Text("Trânsito atual:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(loc.trafficCondition, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EmeraldWallet)
          }
        }

        // Travel mode cards
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
            .padding(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("De Carro", fontSize = 12.sp)
            }
            Text("~${loc.drivingTimeMinutes} minutos", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = InfoBlue)
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("De Moto", fontSize = 12.sp)
            }
            Text("~${loc.motoTimeMinutes} minutos", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WarningAmber)
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = Color(0xFF4B5563), modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Transporte Público", fontSize = 12.sp)
            }
            Text("~${loc.transitTimeMinutes} minutos", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF374151))
          }
        }

        // Estimate Fuel / Transport Cost
        Surface(
          color = Color(0xFFF0FDF4),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Custo estimado de ida (combustível):", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
            Text("R$ ${String.format("%.2f", estimatedFuelCost)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EmeraldWallet)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode("${loc.address}, ${loc.city}")}")
          val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
          }
          try {
            context.startActivity(mapIntent)
          } catch (e: Exception) {
            val browserIntent = Intent(
              Intent.ACTION_VIEW,
              Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode("${loc.address}, ${loc.city}")}")
            )
            context.startActivity(browserIntent)
          }
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = InfoBlue)
      ) {
        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Abrir Navegação GPS")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Fechar")
      }
    }
  )
}
