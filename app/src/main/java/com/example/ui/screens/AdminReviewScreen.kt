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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import com.example.model.CancellationRefundStatus
import com.example.model.DocumentReviewAudit
import com.example.model.ProviderDocuments
import com.example.model.ProviderLocationPoint
import com.example.model.ProviderVerificationStatus
import com.example.model.ServiceRequest
import com.example.model.ServiceStatus
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.components.DocumentAttachmentPreviewImage
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryVariant
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

private enum class AdminStatusFilter(val label: String) {
  PENDENTES("Pendentes"),
  TODOS("Todos"),
  APROVADOS("Aprovados"),
  REPROVADOS("Reprovados")
}

private data class DocumentPreviewPayload(
  val title: String,
  val subtitle: String,
  val fileName: String,
  val attachmentUri: String?,
  val code: String,
  val providerName: String,
  val providerDoc: String,
  val accentColor: Color,
  val icon: ImageVector
)

private enum class AdminSectionTab(val label: String) {
  DOCUMENTOS("Documentos"),
  ESTORNOS("Estornos"),
  LOCALIZACAO("Localização GPS")
}

@Composable
fun AdminReviewScreen(
  currentUser: User,
  users: List<User>,
  auditHistory: List<DocumentReviewAudit>,
  isLoading: Boolean,
  syncStatusMessage: String?,
  onSyncFirestoreClick: () -> Unit,
  onApproveProvider: (providerId: String, idOk: Boolean, criminalOk: Boolean, selfieOk: Boolean) -> Unit,
  onRejectProvider: (providerId: String, reason: String, idOk: Boolean, criminalOk: Boolean, selfieOk: Boolean) -> Unit,
  onOpenMenu: () -> Unit,
  onBackToHome: () -> Unit,
  onOpenManageUsers: () -> Unit = {},
  onOpenCatalogAndEdits: () -> Unit = {},
  services: List<ServiceRequest> = emptyList(),
  serviceLocationHistory: Map<String, List<ProviderLocationPoint>> = emptyMap(),
  onProcessCancellationRefund: (serviceId: String, approve: Boolean, note: String) -> Unit = { _, _, _ -> },
  onSyncServiceLocationHistory: (serviceId: String) -> Unit = {}
) {
  // Segurança de interface: acesso exclusivo ao perfil ADMIN
  if (currentUser.role != UserRole.ADMIN) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Surface(
            color = DangerRedLight,
            shape = CircleShape,
            modifier = Modifier.size(68.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Acesso restrito",
                tint = DangerRed,
                modifier = Modifier.size(34.dp)
              )
            }
          }
          Text(
            text = "Acesso Restrito ao Administrador",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
          )
          Text(
            text = "Esta central exclusiva é destinada somente à análise e aprovação de documentos pessoais (CNH/Identidade e Antecedentes) no Firestore.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
          Button(
            onClick = onBackToHome,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
          ) {
            Text("Voltar para Serviços", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
    return
  }

  val providers = remember(users) {
    users.filter { it.role == UserRole.PRESTADOR }
  }
  val pendingCount = remember(providers) {
    providers.count { it.verificationStatus == ProviderVerificationStatus.PENDENTE_ANALISE }
  }
  val approvedCount = remember(providers) {
    providers.count { it.verificationStatus == ProviderVerificationStatus.APROVADO }
  }
  val rejectedCount = remember(providers) {
    providers.count { it.verificationStatus == ProviderVerificationStatus.REJEITADO }
  }

  var searchQuery by rememberSaveable { mutableStateOf("") }
  var selectedFilter by rememberSaveable { mutableStateOf(AdminStatusFilter.PENDENTES) }
  var selectedProviderId by rememberSaveable { mutableStateOf<String?>(null) }
  var previewPayload by remember { mutableStateOf<DocumentPreviewPayload?>(null) }
  var activeAdminTab by rememberSaveable { mutableStateOf(AdminSectionTab.DOCUMENTOS) }

  val pendingRefundsCount = remember(services) {
    services.count {
      it.status == ServiceStatus.CANCELADO &&
        it.cancellationRefundStatus == CancellationRefundStatus.PENDENTE_ANALISE_ADMIN
    }
  }
  val trackedServices = remember(services, serviceLocationHistory) {
    services.filter { srv ->
      srv.acceptedProviderId != null ||
        srv.locationPointsCount > 0 ||
        !serviceLocationHistory[srv.id].isNullOrEmpty()
    }
  }

  val filteredProviders = remember(providers, searchQuery, selectedFilter) {
    providers.filter { provider ->
      val matchesStatus = when (selectedFilter) {
        AdminStatusFilter.PENDENTES -> provider.verificationStatus == ProviderVerificationStatus.PENDENTE_ANALISE
        AdminStatusFilter.TODOS -> true
        AdminStatusFilter.APROVADOS -> provider.verificationStatus == ProviderVerificationStatus.APROVADO
        AdminStatusFilter.REPROVADOS -> provider.verificationStatus == ProviderVerificationStatus.REJEITADO
      }
      val q = searchQuery.trim().lowercase()
      val matchesQuery = q.isBlank() ||
        provider.name.lowercase().contains(q) ||
        provider.category.lowercase().contains(q) ||
        provider.city.lowercase().contains(q) ||
        provider.document.lowercase().contains(q) ||
        (provider.documents?.rgOrCnhNumber?.lowercase()?.contains(q) == true)
      matchesStatus && matchesQuery
    }
  }

  val selectedProvider = remember(providers, selectedProviderId) {
    providers.find { it.id == selectedProviderId }
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    val isExpanded = maxWidth >= 840.dp

    // Em telas compactas, o botão voltar do sistema fecha o inspetor e retorna para a fila
    if (!isExpanded && selectedProvider != null && activeAdminTab == AdminSectionTab.DOCUMENTOS) {
      BackHandler { selectedProviderId = null }
    }

    Column(modifier = Modifier.fillMaxSize()) {
      AdminConsoleHeader(
        adminUser = currentUser,
        pendingCount = pendingCount,
        approvedCount = approvedCount,
        rejectedCount = rejectedCount,
        isLoading = isLoading,
        syncStatusMessage = syncStatusMessage,
        showBackButton = !isExpanded && selectedProvider != null && activeAdminTab == AdminSectionTab.DOCUMENTOS,
        onBackClick = { selectedProviderId = null },
        onMenuClick = onOpenMenu,
        onSyncClick = onSyncFirestoreClick,
        onManageUsersClick = onOpenManageUsers,
        onOpenCatalogAndEdits = onOpenCatalogAndEdits
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        AdminSectionTab.entries.forEach { tab ->
          val badgeCount = when (tab) {
            AdminSectionTab.DOCUMENTOS -> pendingCount
            AdminSectionTab.ESTORNOS -> pendingRefundsCount
            AdminSectionTab.LOCALIZACAO -> trackedServices.size
          }
          FilterChip(
            selected = activeAdminTab == tab,
            onClick = { activeAdminTab = tab },
            label = {
              Text(
                text = if (badgeCount > 0) "${tab.label} ($badgeCount)" else tab.label,
                fontWeight = FontWeight.Bold
              )
            },
            modifier = Modifier.testTag("admin_tab_${tab.name.lowercase()}")
          )
        }
      }
      HorizontalDivider()

      if (activeAdminTab == AdminSectionTab.ESTORNOS) {
        AdminRefundReviewSection(
          services = services,
          onProcessCancellationRefund = onProcessCancellationRefund
        )
      } else if (activeAdminTab == AdminSectionTab.LOCALIZACAO) {
        AdminLocationHistorySection(
          services = trackedServices,
          locationHistory = serviceLocationHistory,
          onSyncLocationHistory = onSyncServiceLocationHistory
        )
      } else if (isExpanded) {
        // Layout Canônico List-Detail (Tablets / Foldables abertos)
        val effectiveDetailProvider = selectedProvider ?: filteredProviders.firstOrNull() ?: providers.firstOrNull()
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Column(
            modifier = Modifier
              .width(380.dp)
              .fillMaxHeight()
          ) {
            ProviderQueueSection(
              filteredProviders = filteredProviders,
              selectedProviderId = effectiveDetailProvider?.id,
              searchQuery = searchQuery,
              selectedFilter = selectedFilter,
              pendingCount = pendingCount,
              approvedCount = approvedCount,
              rejectedCount = rejectedCount,
              totalCount = providers.size,
              onSearchChange = { searchQuery = it },
              onFilterChange = { selectedFilter = it },
              onSelectProvider = { selectedProviderId = it }
            )
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
          ) {
            if (effectiveDetailProvider != null) {
              ProviderDocumentInspector(
                provider = effectiveDetailProvider,
                auditHistory = auditHistory.filter { it.providerId == effectiveDetailProvider.id },
                isLoading = isLoading,
                onPreviewDocument = { previewPayload = it },
                onApprove = { idOk, crimOk, selfieOk ->
                  onApproveProvider(effectiveDetailProvider.id, idOk, crimOk, selfieOk)
                },
                onReject = { reason, idOk, crimOk, selfieOk ->
                  onRejectProvider(effectiveDetailProvider.id, reason, idOk, crimOk, selfieOk)
                }
              )
            } else {
              EmptyAdminInspectorPlaceholder()
            }
          }
        }
      } else {
        // Layout Compacto (Smartphones)
        if (selectedProvider == null) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 16.dp, vertical = 12.dp)
          ) {
            ProviderQueueSection(
              filteredProviders = filteredProviders,
              selectedProviderId = null,
              searchQuery = searchQuery,
              selectedFilter = selectedFilter,
              pendingCount = pendingCount,
              approvedCount = approvedCount,
              rejectedCount = rejectedCount,
              totalCount = providers.size,
              onSearchChange = { searchQuery = it },
              onFilterChange = { selectedFilter = it },
              onSelectProvider = { selectedProviderId = it }
            )
          }
        } else {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 16.dp, vertical = 12.dp)
          ) {
            ProviderDocumentInspector(
              provider = selectedProvider,
              auditHistory = auditHistory.filter { it.providerId == selectedProvider.id },
              isLoading = isLoading,
              onPreviewDocument = { previewPayload = it },
              onApprove = { idOk, crimOk, selfieOk ->
                onApproveProvider(selectedProvider.id, idOk, crimOk, selfieOk)
              },
              onReject = { reason, idOk, crimOk, selfieOk ->
                onRejectProvider(selectedProvider.id, reason, idOk, crimOk, selfieOk)
              }
            )
          }
        }
      }
    }
  }

  previewPayload?.let { payload ->
    DocumentZoomPreviewDialog(
      payload = payload,
      onDismiss = { previewPayload = null }
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminConsoleHeader(
  adminUser: User,
  pendingCount: Int,
  approvedCount: Int,
  rejectedCount: Int,
  isLoading: Boolean,
  syncStatusMessage: String?,
  showBackButton: Boolean,
  onBackClick: () -> Unit,
  onMenuClick: () -> Unit,
  onSyncClick: () -> Unit,
  onManageUsersClick: () -> Unit,
  onOpenCatalogAndEdits: () -> Unit = {}
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        Brush.linearGradient(
          colors = listOf(Color(0xFF0F172A), BrandPrimaryVariant, BrandPrimary)
        )
      )
      .padding(horizontal = 16.dp, vertical = 14.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          IconButton(
            onClick = if (showBackButton) onBackClick else onMenuClick,
            modifier = Modifier
              .size(48.dp)
              .testTag(if (showBackButton) "btn_admin_back_queue" else "btn_admin_menu")
          ) {
            Icon(
              imageVector = if (showBackButton) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu,
              contentDescription = if (showBackButton) "Voltar para fila" else "Abrir menu",
              tint = Color.White
            )
          }
          Spacer(modifier = Modifier.width(4.dp))
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = Color(0xFF93C5FD),
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "PAINEL EXCLUSIVO ADMIN",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = 0.8.sp
                ),
                color = Color(0xFF93C5FD)
              )
            }
            Text(
              text = "Análise Documental Firestore",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${adminUser.name} • ${adminUser.email}",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = Color.White.copy(alpha = 0.78f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        OutlinedButton(
          onClick = onSyncClick,
          enabled = !isLoading,
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("btn_admin_sync_firestore")
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              color = Color.White,
              strokeWidth = 2.dp,
              modifier = Modifier.size(16.dp)
            )
          } else {
            Icon(Icons.Default.CloudSync, contentDescription = "Sincronizar Firestore", modifier = Modifier.size(17.dp))
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text("Sincronizar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onManageUsersClick,
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("btn_admin_open_users_management")
        ) {
          Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(17.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Gerenciar Usuários", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = onOpenCatalogAndEdits,
          border = BorderStroke(1.dp, Color(0xFF93C5FD).copy(alpha = 0.65f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("btn_admin_open_catalog_edits")
        ) {
          Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(17.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Categorias e Outras Edições", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }

      // KPIs Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        AdminKpiPill(
          count = pendingCount,
          label = "Pendentes",
          containerColor = Color(0xFFFEF3C7).copy(alpha = 0.18f),
          accentColor = Color(0xFFFCD34D),
          modifier = Modifier.weight(1f)
        )
        AdminKpiPill(
          count = approvedCount,
          label = "Aprovados",
          containerColor = Color(0xFFD1FAE5).copy(alpha = 0.18f),
          accentColor = Color(0xFF6EE7B7),
          modifier = Modifier.weight(1f)
        )
        AdminKpiPill(
          count = rejectedCount,
          label = "Reprovados",
          containerColor = Color(0xFFFEE2E2).copy(alpha = 0.18f),
          accentColor = Color(0xFFFCA5A5),
          modifier = Modifier.weight(1f)
        )
      }

      // Firestore Sync Banner
      if (!syncStatusMessage.isNullOrBlank()) {
        Surface(
          color = Color.White.copy(alpha = 0.12f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.CloudDone,
              contentDescription = null,
              tint = Color(0xFF6EE7B7),
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = syncStatusMessage,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = Color.White,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AdminKpiPill(
  count: Int,
  label: String,
  containerColor: Color,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    color = containerColor,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "$count",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
        color = accentColor
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White.copy(alpha = 0.9f)
      )
    }
  }
}

@Composable
private fun ProviderQueueSection(
  filteredProviders: List<User>,
  selectedProviderId: String?,
  searchQuery: String,
  selectedFilter: AdminStatusFilter,
  pendingCount: Int,
  approvedCount: Int,
  rejectedCount: Int,
  totalCount: Int,
  onSearchChange: (String) -> Unit,
  onFilterChange: (AdminStatusFilter) -> Unit,
  onSelectProvider: (String) -> Unit
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchChange,
      placeholder = { Text("Buscar prestador, CNH, especialidade...") },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { onSearchChange("") }) {
            Icon(Icons.Default.Close, contentDescription = "Limpar busca")
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("input_admin_search")
    )

    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      AdminStatusFilter.entries.forEach { filter ->
        val count = when (filter) {
          AdminStatusFilter.PENDENTES -> pendingCount
          AdminStatusFilter.TODOS -> totalCount
          AdminStatusFilter.APROVADOS -> approvedCount
          AdminStatusFilter.REPROVADOS -> rejectedCount
        }
        FilterChip(
          selected = selectedFilter == filter,
          onClick = { onFilterChange(filter) },
          label = { Text("${filter.label} ($count)", fontWeight = FontWeight.SemiBold) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.primary
          ),
          modifier = Modifier.testTag("chip_admin_filter_${filter.name.lowercase()}")
        )
      }
    }

    if (filteredProviders.isEmpty()) {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = EmeraldWallet,
            modifier = Modifier.size(36.dp)
          )
          Text(
            text = "Nenhum prestador neste filtro",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            text = "Alterne entre os filtros acima para consultar cadastros já aprovados ou reprovados.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
      ) {
        items(filteredProviders, key = { it.id }) { provider ->
          ProviderQueueCard(
            provider = provider,
            isSelected = provider.id == selectedProviderId,
            onClick = { onSelectProvider(provider.id) }
          )
        }
      }
    }
  }
}

@Composable
private fun ProviderQueueCard(
  provider: User,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val statusMeta = when (provider.verificationStatus) {
    ProviderVerificationStatus.PENDENTE_ANALISE -> Triple("Pendente", WarningAmber, WarningAmberLight)
    ProviderVerificationStatus.APROVADO -> Triple("Aprovado", EmeraldWallet, EmeraldWalletLight)
    ProviderVerificationStatus.REJEITADO -> Triple("Reprovado", DangerRed, DangerRedLight)
  }
  val docs = provider.documents
  val hasIdDoc = !docs?.rgOrCnhNumber.isNullOrBlank() || provider.document.isNotBlank()
  val hasCriminal = !docs?.criminalRecordCertNumber.isNullOrBlank()
  val hasLegacySelfie = !docs?.selfieAttachmentUri.isNullOrBlank()

  Card(
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
    ),
    border = BorderStroke(
      width = if (isSelected) 2.dp else 1.dp,
      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    ),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag("admin_provider_card_${provider.id}")
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = Color(provider.avatarColorHex),
          shape = CircleShape,
          modifier = Modifier.size(44.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = provider.name.take(2).uppercase(),
              color = Color.White,
              fontWeight = FontWeight.Bold
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = provider.name,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (provider.isPhoneVerified) {
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "SMS verificado",
                tint = InfoBlue,
                modifier = Modifier.size(14.dp)
              )
            }
          }
          Text(
            text = provider.category.ifBlank { "Prestador de Serviços" },
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${provider.city} • ${provider.phone}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Surface(
          color = statusMeta.third,
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            text = statusMeta.first,
            color = statusMeta.second,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      // Document completeness indicators
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        DocPresenceChip(label = "CNH/RG", present = hasIdDoc, verified = docs?.idDocVerified == true)
        DocPresenceChip(label = "Antecedentes", present = hasCriminal, verified = docs?.criminalRecordVerified == true)
        if (hasLegacySelfie) {
          DocPresenceChip(label = "Selfie", present = true, verified = docs?.selfieVerified == true)
        }
      }

      if (provider.verificationStatus == ProviderVerificationStatus.REJEITADO && !docs?.rejectionReason.isNullOrBlank()) {
        Surface(
          color = DangerRedLight,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Motivo: ${docs?.rejectionReason}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            color = DangerRed,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun DocPresenceChip(label: String, present: Boolean, verified: Boolean) {
  val bg = when {
    verified -> EmeraldWalletLight
    present -> InfoBlueLight
    else -> MaterialTheme.colorScheme.surfaceVariant
  }
  val fg = when {
    verified -> EmeraldWallet
    present -> InfoBlue
    else -> MaterialTheme.colorScheme.onSurfaceVariant
  }
  Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
    Text(
      text = if (verified) "✓ $label" else label,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
      color = fg,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
    )
  }
}

@Composable
private fun ProviderDocumentInspector(
  provider: User,
  auditHistory: List<DocumentReviewAudit>,
  isLoading: Boolean,
  onPreviewDocument: (DocumentPreviewPayload) -> Unit,
  onApprove: (idOk: Boolean, criminalOk: Boolean, selfieOk: Boolean) -> Unit,
  onReject: (reason: String, idOk: Boolean, criminalOk: Boolean, selfieOk: Boolean) -> Unit
) {
  val docs = provider.documents ?: ProviderDocuments(
    rgOrCnhNumber = provider.document.ifBlank { "RG/CNH não informado" },
    criminalRecordCertNumber = "CERT-PF-PENDENTE"
  )

  val hasLegacySelfie = !docs.selfieAttachmentUri.isNullOrBlank()

  var idChecked by remember(provider.id, docs.idDocVerified) {
    mutableStateOf(docs.idDocVerified || provider.verificationStatus == ProviderVerificationStatus.APROVADO)
  }
  var criminalChecked by remember(provider.id, docs.criminalRecordVerified) {
    mutableStateOf(docs.criminalRecordVerified || provider.verificationStatus == ProviderVerificationStatus.APROVADO)
  }
  var selfieChecked by remember(provider.id, docs.selfieVerified, hasLegacySelfie) {
    mutableStateOf(!hasLegacySelfie || docs.selfieVerified || provider.verificationStatus == ProviderVerificationStatus.APROVADO)
  }
  var rejectionReason by remember(provider.id, docs.rejectionReason) {
    mutableStateOf(docs.rejectionReason.orEmpty())
  }

  val quickReasons = listOf(
    "CNH/Identidade ilegível ou cortada",
    "Certidão de antecedentes vencida ou inválida",
    "Foto de perfil incompatível ou ilegível",
    "CPF divergente dos documentos enviados"
  )

  val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }
  val totalSteps = if (hasLegacySelfie) 3 else 2
  val allChecked = idChecked && criminalChecked && (!hasLegacySelfie || selfieChecked)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Dossier Header Card
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = Color(provider.avatarColorHex),
            shape = CircleShape,
            modifier = Modifier.size(54.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = provider.name.take(2).uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
              )
            }
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = provider.name,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = provider.category.ifBlank { "Prestador de Serviços" },
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "${provider.email} • ${provider.city}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(color = InfoBlueLight, shape = RoundedCornerShape(8.dp)) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Phone, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "SMS: ${provider.phone}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = InfoBlue
              )
            }
          }
          if (provider.document.isNotBlank()) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
              Text(
                text = "CPF/CNPJ: ${provider.document}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }

        if (provider.bio.isNotBlank()) {
          Text(
            text = provider.bio,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Text(
          text = "Enviado em ${dateFormat.format(Date(docs.submittedAt))} • Status atual: ${provider.verificationStatus.label}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // 2. Checklist Header + Quick Check-All
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Conferência dos Documentos ($totalSteps etapas)",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
      )
      TextButton(
        onClick = {
          val target = !allChecked
          idChecked = target
          criminalChecked = target
          if (hasLegacySelfie) selfieChecked = target
        },
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("btn_admin_check_all_docs")
      ) {
        Text(
          text = if (allChecked) "Desmarcar todos" else "Conferir todos ($totalSteps/$totalSteps)",
          fontWeight = FontWeight.Bold
        )
      }
    }

    // 3. Card 1: CNH / Identidade
    DocumentInspectionCard(
      title = "1. CNH / Documento de Identidade (RG)",
      documentNumber = docs.rgOrCnhNumber.ifBlank { provider.document.ifBlank { "CNH não informada" } },
      fileName = docs.rgOrCnhFileName,
      attachmentUri = docs.rgOrCnhAttachmentUri,
      description = "Verifique legibilidade da foto, número do registro, data de emissão e correspondência com o CPF cadastrado.",
      icon = Icons.Default.Badge,
      accentColor = InfoBlue,
      checked = idChecked,
      onCheckedChange = { idChecked = it },
      checkTag = "chk_admin_doc_id",
      previewTag = "btn_preview_doc_id",
      onOpenPreview = {
        onPreviewDocument(
          DocumentPreviewPayload(
            title = "CNH Digital / Identidade (Frente e Verso)",
            subtitle = "Validação de Identidade Civil",
            fileName = docs.rgOrCnhFileName,
            attachmentUri = docs.rgOrCnhAttachmentUri,
            code = docs.rgOrCnhNumber.ifBlank { "CNH-NÃO-INFORMADA" },
            providerName = provider.name,
            providerDoc = provider.document,
            accentColor = InfoBlue,
            icon = Icons.Default.Badge
          )
        )
      }
    )

    // 4. Card 2: Certidão de Antecedentes Criminais
    DocumentInspectionCard(
      title = "2. Certidão de Antecedentes Criminais",
      documentNumber = docs.criminalRecordCertNumber.ifBlank { "Certidão não informada" },
      fileName = docs.criminalRecordFileName,
      attachmentUri = docs.criminalRecordAttachmentUri,
      description = "Confira o código de autenticidade da Polícia Federal / SSP e certifique-se de que consta 'Nada Consta'.",
      icon = Icons.Default.Gavel,
      accentColor = BrandPrimary,
      checked = criminalChecked,
      onCheckedChange = { criminalChecked = it },
      checkTag = "chk_admin_doc_criminal",
      previewTag = "btn_preview_doc_criminal",
      onOpenPreview = {
        onPreviewDocument(
          DocumentPreviewPayload(
            title = "Certidão de Antecedentes Criminais",
            subtitle = "Polícia Federal / Secretaria de Segurança Pública",
            fileName = docs.criminalRecordFileName,
            attachmentUri = docs.criminalRecordAttachmentUri,
            code = docs.criminalRecordCertNumber.ifBlank { "CERT-PENDENTE" },
            providerName = provider.name,
            providerDoc = provider.document,
            accentColor = BrandPrimary,
            icon = Icons.Default.Gavel
          )
        )
      }
    )

    // 5. Card 3 (legado): Selfie com Documento, exibido apenas se anexado anteriormente
    if (hasLegacySelfie) {
      DocumentInspectionCard(
        title = "3. Selfie Segurando o Documento (Legado)",
        documentNumber = "Anexo enviado em versão anterior",
        fileName = docs.selfieFileName,
        attachmentUri = docs.selfieAttachmentUri,
        description = "Anexo opcional de cadastros anteriores.",
        icon = Icons.Default.Face,
        accentColor = EmeraldWallet,
        checked = selfieChecked,
        onCheckedChange = { selfieChecked = it },
        checkTag = "chk_admin_doc_selfie",
        previewTag = "btn_preview_doc_selfie",
        onOpenPreview = {
          onPreviewDocument(
            DocumentPreviewPayload(
              title = "Selfie Biométrica com Documento",
              subtitle = "Prova de Vida e Titularidade",
              fileName = docs.selfieFileName,
              attachmentUri = docs.selfieAttachmentUri,
              code = "FACE-MATCH-VERIFICATION",
              providerName = provider.name,
              providerDoc = provider.document,
              accentColor = EmeraldWallet,
              icon = Icons.Default.Face
            )
          )
        }
      )
    }

    // 6. Decision Panel (Approve / Reject in Firestore)
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Decisão Administrativa no Firestore",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        Text(
          text = "Motivo de reprovação (obrigatório caso reprove o cadastro):",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          quickReasons.forEach { preset ->
            FilterChip(
              selected = rejectionReason == preset,
              onClick = { rejectionReason = preset },
              label = { Text(preset, fontSize = 11.sp) }
            )
          }
        }

        OutlinedTextField(
          value = rejectionReason,
          onValueChange = { rejectionReason = it },
          label = { Text("Descreva o motivo para o prestador (se reprovar)") },
          placeholder = { Text("Ex: Foto da CNH com reflexo ilegível, reenvie com boa luz.") },
          minLines = 2,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_admin_rejection_reason")
        )

        if (!allChecked) {
          Surface(
            color = WarningAmberLight,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Marque os 3 documentos acima como conferidos antes de aprovar o prestador.",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF78350F)
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = { onReject(rejectionReason, idChecked, criminalChecked, selfieChecked) },
            enabled = !isLoading && rejectionReason.isNotBlank(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
            border = BorderStroke(1.2.dp, if (rejectionReason.isNotBlank()) DangerRed else MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 50.dp)
              .testTag("btn_admin_reject_provider")
          ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reprovar", fontWeight = FontWeight.Bold)
          }

          Button(
            onClick = { onApprove(idChecked, criminalChecked, selfieChecked) },
            enabled = !isLoading && allChecked,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 50.dp)
              .testTag("btn_admin_approve_provider")
          ) {
            if (isLoading) {
              CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Salvando...", fontWeight = FontWeight.Bold)
            } else {
              Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Aprovar no Firestore", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // 7. Audit Trail Section
    if (auditHistory.isNotEmpty() || docs.reviewedAt != null) {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Histórico Imutável de Auditoria",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
          }

          if (auditHistory.isEmpty() && docs.reviewedAt != null) {
            Text(
              text = "Última análise em ${dateFormat.format(Date(docs.reviewedAt))} por ${docs.reviewedByEmail ?: "Administrador"}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          } else {
            auditHistory.forEachIndexed { idx, entry ->
              if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
              val approved = entry.decision == ProviderVerificationStatus.APROVADO.name
              Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = if (approved) "✅ Aprovado" else "⛔ Reprovado",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (approved) EmeraldWallet else DangerRed
                  )
                  Text(
                    text = dateFormat.format(Date(entry.reviewedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                if (!entry.reason.isNullOrBlank()) {
                  Text(
                    text = "Motivo: ${entry.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DangerRed
                  )
                }
                Text(
                  text = "Analisado por: ${entry.adminEmail.ifBlank { entry.adminUid }}",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
private fun DocumentInspectionCard(
  title: String,
  documentNumber: String,
  fileName: String,
  attachmentUri: String?,
  description: String,
  icon: ImageVector,
  accentColor: Color,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  checkTag: String,
  previewTag: String,
  onOpenPreview: () -> Unit
) {
  val hasAttachment = !attachmentUri.isNullOrBlank()

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (checked) EmeraldWalletLight.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
    ),
    border = BorderStroke(
      width = if (checked) 1.5.dp else 1.dp,
      color = if (checked) EmeraldWallet else MaterialTheme.colorScheme.outlineVariant
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Surface(
            color = accentColor.copy(alpha = 0.14f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(40.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = title,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = documentNumber,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              color = accentColor
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = EmeraldWallet),
            modifier = Modifier.testTag(checkTag)
          )
        }
      }

      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      // Embedded Document Preview Frame
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (hasAttachment) accentColor.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .clickable(onClick = onOpenPreview)
          .testTag(previewTag)
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          if (hasAttachment) {
            DocumentAttachmentPreviewImage(
              attachmentUri = attachmentUri,
              contentDescription = "Anexo enviado de $title",
              fallbackIcon = icon,
              accentColor = accentColor,
              modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(start = 10.dp, end = 10.dp, top = 10.dp)
                .clip(RoundedCornerShape(8.dp))
            )
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.Description, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = fileName,
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = when {
                    checked -> "Conferido pelo Administrador ✓"
                    hasAttachment -> "Anexo enviado pelo prestador • Toque para ampliar"
                    else -> "Sem arquivo anexado • Toque para inspecionar dados"
                  },
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                  color = when {
                    checked -> EmeraldWallet
                    hasAttachment -> accentColor
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                  }
                )
              }
            }
            Icon(
              imageVector = Icons.Default.ZoomIn,
              contentDescription = "Ampliar documento",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DocumentZoomPreviewDialog(
  payload: DocumentPreviewPayload,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Surface(
        color = payload.accentColor.copy(alpha = 0.14f),
        shape = CircleShape,
        modifier = Modifier.size(56.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(payload.icon, contentDescription = null, tint = payload.accentColor, modifier = Modifier.size(28.dp))
        }
      }
    },
    title = {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = payload.title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          textAlign = TextAlign.Center
        )
        Text(
          text = payload.subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
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
        if (!payload.attachmentUri.isNullOrBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, payload.accentColor.copy(alpha = 0.4f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("admin_zoom_attachment_preview")
          ) {
            DocumentAttachmentPreviewImage(
              attachmentUri = payload.attachmentUri,
              contentDescription = "Visualização ampliada de ${payload.fileName}",
              contentScale = ContentScale.Fit,
              fallbackIcon = payload.icon,
              accentColor = payload.accentColor,
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 320.dp)
                .clip(RoundedCornerShape(14.dp))
            )
          }
        } else {
          Surface(
            color = WarningAmberLight,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Nenhum arquivo visual foi anexado a este documento pelo prestador.",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              color = Color(0xFF92400E),
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        Surface(
          color = Color(0xFF0F172A),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "REPÚBLICA FEDERATIVA • RESOLVEAI KYC",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = Color(0xFF93C5FD)
              )
              Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
            Text(
              text = "Titular: ${payload.providerName}",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = Color.White
            )
            if (payload.providerDoc.isNotBlank()) {
              Text(
                text = "CPF / CNPJ: ${payload.providerDoc}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
              )
            }
            Text(
              text = "Registro / Código: ${payload.code}",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              color = Color(0xFFFCD34D)
            )
            Text(
              text = "Arquivo anexado: ${payload.fileName}",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = Color.White.copy(alpha = 0.72f)
            )
          }
        }
        Text(
          text = "Confirme se os dados do documento conferem com o titular cadastrado antes de aprovar.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Concluir Inspeção")
      }
    }
  )
}

@Composable
private fun EmptyAdminInspectorPlaceholder() {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier.fillMaxSize()
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.AdminPanelSettings,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(48.dp)
        )
        Text(
          text = "Selecione um prestador na fila",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Text(
          text = "Clique em qualquer cadastro à esquerda para inspecionar CNH/Identidade e Antecedentes.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

@Composable
private fun AdminRefundReviewSection(
  services: List<ServiceRequest>,
  onProcessCancellationRefund: (serviceId: String, approve: Boolean, note: String) -> Unit
) {
  val providerCancelledServices = remember(services) {
    services.filter {
      it.status == ServiceStatus.CANCELADO &&
        (it.cancelledByRole == UserRole.PRESTADOR ||
          it.cancellationRefundStatus == CancellationRefundStatus.PENDENTE_ANALISE_ADMIN ||
          it.cancellationRefundStatus == CancellationRefundStatus.ESTORNADO_ADMIN ||
          it.cancellationRefundStatus == CancellationRefundStatus.NEGADO_ADMIN)
    }.sortedByDescending { it.cancelledAt ?: it.createdAt }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
      .testTag("admin_refund_review_list"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(vertical = 14.dp)
  ) {
    if (providerCancelledServices.isEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Nenhum cancelamento de prestador pendente ou analisado no momento.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(18.dp)
          )
        }
      }
    } else {
      items(providerCancelledServices, key = { it.id }) { srv ->
        var adminNote by remember(srv.id) { mutableStateOf(srv.cancellationRefundNote.orEmpty()) }
        val isPending = srv.cancellationRefundStatus == CancellationRefundStatus.PENDENTE_ANALISE_ADMIN
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, if (isPending) WarningAmber else MaterialTheme.colorScheme.outlineVariant),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_refund_card_${srv.id}")
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
              Text(
                text = srv.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
              )
              Surface(
                color = when (srv.cancellationRefundStatus) {
                  CancellationRefundStatus.PENDENTE_ANALISE_ADMIN -> WarningAmberLight
                  CancellationRefundStatus.ESTORNADO_ADMIN -> EmeraldWalletLight
                  else -> DangerRedLight
                },
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = srv.cancellationRefundStatus?.label ?: "Analisar",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                  color = when (srv.cancellationRefundStatus) {
                    CancellationRefundStatus.PENDENTE_ANALISE_ADMIN -> WarningAmber
                    CancellationRefundStatus.ESTORNADO_ADMIN -> EmeraldWallet
                    else -> DangerRed
                  },
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }
            Text(
              text = "Prestador: ${srv.cancelledByName ?: srv.acceptedProviderName ?: "Não informado"} • Taxa retida: R$ ${String.format("%.2f", srv.platformFee ?: 0.0)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!srv.cancellationReason.isNullOrBlank()) {
              Text(
                text = "Motivo informado: ${srv.cancellationReason}",
                style = MaterialTheme.typography.bodySmall,
                color = DangerRed
              )
            }
            Text(
              text = "Pontos de localização GPS registrados: ${srv.locationPointsCount}",
              style = MaterialTheme.typography.labelSmall,
              color = InfoBlue
            )

            if (isPending) {
              OutlinedTextField(
                value = adminNote,
                onValueChange = { adminNote = it },
                label = { Text("Parecer do Administrador (opcional)") },
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("input_admin_refund_note_${srv.id}")
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = { onProcessCancellationRefund(srv.id, false, adminNote) },
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                  border = BorderStroke(1.dp, DangerRed),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("btn_admin_reject_refund_${srv.id}")
                ) {
                  Text("Negar Estorno", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Button(
                  onClick = { onProcessCancellationRefund(srv.id, true, adminNote) },
                  colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("btn_admin_approve_refund_${srv.id}")
                ) {
                  Text("Aprovar Estorno", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
              }
            } else if (!srv.cancellationRefundNote.isNullOrBlank()) {
              Text(
                text = "Parecer admin: ${srv.cancellationRefundNote}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AdminLocationHistorySection(
  services: List<ServiceRequest>,
  locationHistory: Map<String, List<ProviderLocationPoint>>,
  onSyncLocationHistory: (serviceId: String) -> Unit
) {
  val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm:ss", Locale("pt", "BR")) }
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
      .testTag("admin_location_history_list"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(vertical = 14.dp)
  ) {
    if (services.isEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Nenhum serviço aceito ou finalizado com histórico de localização registrado ainda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(18.dp)
          )
        }
      }
    } else {
      items(services, key = { it.id }) { srv ->
        val points = locationHistory[srv.id].orEmpty()
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_location_service_card_${srv.id}")
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
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = srv.title,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                  text = "Prestador: ${srv.acceptedProviderName ?: "Não atribuído"} • Status: ${srv.status.label}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Surface(
                color = if (srv.locationTrackingActive) EmeraldWalletLight else InfoBlueLight,
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = if (srv.locationTrackingActive) "GPS Ativo" else "${maxOf(srv.locationPointsCount, points.size)} ponto(s)",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                  color = if (srv.locationTrackingActive) EmeraldWallet else InfoBlue,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            OutlinedButton(
              onClick = { onSyncLocationHistory(srv.id) },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_sync_location_${srv.id}")
            ) {
              Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Sincronizar Histórico GPS (${maxOf(srv.locationPointsCount, points.size)})", fontSize = 12.sp)
            }

            if (points.isNotEmpty()) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                points.takeLast(10).reversed().forEach { pt ->
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = "${dateFormat.format(Date(pt.recordedAt))} • (${String.format("%.5f", pt.latitude)}, ${String.format("%.5f", pt.longitude)}) • ${pt.serviceStatus.label} • ±${pt.accuracyMeters.toInt()}m [${pt.source}]",
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

