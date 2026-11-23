package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppAdminSettings
import com.example.model.AppTip
import com.example.model.ChatMessage
import com.example.model.ServiceCategoryItem
import com.example.model.ServiceRequest
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.components.AdminCategoryEditorDialog
import com.example.ui.components.AdminConfirmDeleteCategoryDialog
import com.example.ui.components.AdminEditServiceDialog
import com.example.ui.components.AdminTipEditorDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryVariant
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight

private enum class AdminCatalogSectionTab(val label: String) {
  CATEGORIAS("Categorias de Serviços"),
  SERVICOS("Editar Serviços"),
  OUTRAS_EDICOES("Outras Edições")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminCatalogAndEditsScreen(
  currentUser: User,
  categories: List<ServiceCategoryItem>,
  services: List<ServiceRequest>,
  appSettings: AppAdminSettings,
  appTips: List<AppTip>,
  onAddCategory: (name: String, specialtyLabel: String) -> Result<ServiceCategoryItem>,
  onUpdateCategory: (id: String, name: String, specialtyLabel: String, isActive: Boolean) -> Result<ServiceCategoryItem>,
  onToggleCategory: (id: String, isActive: Boolean) -> Result<ServiceCategoryItem>,
  onMoveCategory: (id: String, moveUp: Boolean) -> Result<Unit>,
  onDeleteCategory: (id: String) -> Result<Unit>,
  onResetDefaultCategories: () -> Result<List<ServiceCategoryItem>>,
  onUpdateService: (ServiceRequest) -> Result<ServiceRequest>,
  onUpdateAppSettings: (AppAdminSettings) -> Result<AppAdminSettings>,
  onSaveTip: (AppTip) -> Result<AppTip>,
  onDeleteTip: (String) -> Result<Unit>,
  onOpenManageUsers: () -> Unit,
  onOpenDocumentsReview: () -> Unit,
  onOpenSuggestions: () -> Unit,
  onOpenMenu: () -> Unit,
  onBackToHome: () -> Unit,
  chatMessages: List<ChatMessage> = emptyList(),
  onOpenServiceChat: (String) -> Unit = {}
) {
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
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 520.dp)
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Surface(color = DangerRedLight, shape = CircleShape, modifier = Modifier.size(68.dp)) {
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
            text = "Somente administradores podem editar categorias de serviços, pedidos publicados e configurações do aplicativo.",
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

  BackHandler { onBackToHome() }

  var selectedSection by rememberSaveable { mutableStateOf(AdminCatalogSectionTab.CATEGORIAS) }
  var categorySearch by rememberSaveable { mutableStateOf("") }
  var serviceSearch by rememberSaveable { mutableStateOf("") }

  var showAddCategoryDialog by remember { mutableStateOf(false) }
  var editingCategory by remember { mutableStateOf<ServiceCategoryItem?>(null) }
  var deletingCategory by remember { mutableStateOf<ServiceCategoryItem?>(null) }
  var editingService by remember { mutableStateOf<ServiceRequest?>(null) }
  var showAddTipDialog by remember { mutableStateOf(false) }
  var editingTip by remember { mutableStateOf<AppTip?>(null) }

  var bannerText by remember(appSettings.globalBannerMessage) { mutableStateOf(appSettings.globalBannerMessage) }
  var whatsappDigits by remember(appSettings.supportWhatsappNumberE164) { mutableStateOf(appSettings.supportWhatsappNumberE164) }
  var whatsappDisplay by remember(appSettings.supportWhatsappDisplay) { mutableStateOf(appSettings.supportWhatsappDisplay) }
  var defaultUrgency by remember(appSettings.defaultServiceUrgency) { mutableStateOf(appSettings.defaultServiceUrgency) }

  val sortedCategories = remember(categories) { categories.sortedBy { it.orderIndex } }
  val activeCategoriesCount = remember(sortedCategories) { sortedCategories.count { it.isActive } }
  val filteredCategories = remember(sortedCategories, categorySearch) {
    val q = categorySearch.trim().lowercase()
    if (q.isBlank()) sortedCategories else sortedCategories.filter {
      it.name.lowercase().contains(q) || it.effectiveSpecialtyLabel.lowercase().contains(q)
    }
  }
  val filteredServices = remember(services, serviceSearch) {
    val q = serviceSearch.trim().lowercase()
    if (q.isBlank()) services else services.filter {
      it.title.lowercase().contains(q) ||
        it.category.lowercase().contains(q) ||
        it.clientName.lowercase().contains(q) ||
        it.location.lowercase().contains(q)
    }
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    val isWide = maxWidth >= 720.dp

    Column(modifier = Modifier.fillMaxSize()) {
      AdminCatalogHeader(
        adminUser = currentUser,
        activeCategoriesCount = activeCategoriesCount,
        totalCategoriesCount = sortedCategories.size,
        onMenuClick = onOpenMenu,
        onBackClick = onBackToHome
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        AdminCatalogSectionTab.entries.forEach { tab ->
          val count = when (tab) {
            AdminCatalogSectionTab.CATEGORIAS -> sortedCategories.size
            AdminCatalogSectionTab.SERVICOS -> services.size
            AdminCatalogSectionTab.OUTRAS_EDICOES -> appTips.size
          }
          FilterChip(
            selected = selectedSection == tab,
            onClick = { selectedSection = tab },
            label = {
              Text(
                text = "${tab.label} ($count)",
                fontWeight = FontWeight.Bold
              )
            },
            modifier = Modifier.testTag("admin_catalog_tab_${tab.name.lowercase()}")
          )
        }
      }
      HorizontalDivider()

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = if (isWide) 24.dp else 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 960.dp)
        ) {
          when (selectedSection) {
            AdminCatalogSectionTab.CATEGORIAS -> {
              LazyColumn(
                modifier = Modifier
                  .fillMaxSize()
                  .testTag("admin_categories_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 28.dp)
              ) {
                item(key = "cat_actions_header") {
                  Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(
                      modifier = Modifier.padding(14.dp),
                      verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                      Text(
                        text = "Gerenciar Categorias de Serviços ($activeCategoriesCount ativas de ${sortedCategories.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                      )
                      Text(
                        text = "As categorias ativas aparecem automaticamente nos filtros da tela inicial, na criação de pedidos e no cadastro de prestadores.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                      FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        Button(
                          onClick = { showAddCategoryDialog = true },
                          shape = RoundedCornerShape(10.dp),
                          colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet, contentColor = Color.White),
                          modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_admin_add_category")
                        ) {
                          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                          Spacer(modifier = Modifier.width(6.dp))
                          Text("Nova Categoria", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                          onClick = { onResetDefaultCategories() },
                          shape = RoundedCornerShape(10.dp),
                          modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_admin_reset_categories")
                        ) {
                          Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                          Spacer(modifier = Modifier.width(6.dp))
                          Text("Restaurar Padrão", fontWeight = FontWeight.SemiBold)
                        }
                      }
                    }
                  }
                }

                item(key = "cat_search") {
                  OutlinedTextField(
                    value = categorySearch,
                    onValueChange = { categorySearch = it },
                    placeholder = { Text("Buscar categoria ou especialidade...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar categoria") },
                    trailingIcon = {
                      if (categorySearch.isNotEmpty()) {
                        IconButton(onClick = { categorySearch = "" }) {
                          Icon(Icons.Default.Close, contentDescription = "Limpar busca")
                        }
                      }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("input_admin_category_search")
                  )
                }

                items(filteredCategories, key = { it.id }) { cat ->
                  AdminCategoryItemCard(
                    category = cat,
                    canMoveUp = sortedCategories.firstOrNull()?.id != cat.id,
                    canMoveDown = sortedCategories.lastOrNull()?.id != cat.id,
                    canDelete = sortedCategories.size > 1,
                    onToggleActive = { active -> onToggleCategory(cat.id, active) },
                    onMoveUp = { onMoveCategory(cat.id, true) },
                    onMoveDown = { onMoveCategory(cat.id, false) },
                    onEditClick = { editingCategory = cat },
                    onDeleteClick = { deletingCategory = cat }
                  )
                }
              }
            }

            AdminCatalogSectionTab.SERVICOS -> {
              LazyColumn(
                modifier = Modifier
                  .fillMaxSize()
                  .testTag("admin_edit_services_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 28.dp)
              ) {
                item(key = "srv_search") {
                  OutlinedTextField(
                    value = serviceSearch,
                    onValueChange = { serviceSearch = it },
                    placeholder = { Text("Buscar serviço por título, categoria ou cliente...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar serviço") },
                    trailingIcon = {
                      if (serviceSearch.isNotEmpty()) {
                        IconButton(onClick = { serviceSearch = "" }) {
                          Icon(Icons.Default.Close, contentDescription = "Limpar busca")
                        }
                      }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("input_admin_services_search")
                  )
                }

                items(filteredServices, key = { it.id }) { srv ->
                  Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("admin_service_card_${srv.id}")
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
                            text = srv.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                          )
                        }
                        StatusBadge(status = srv.status)
                      }
                      Text(
                        text = srv.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                      )
                      val hasAgreedProvider = !srv.acceptedProviderId.isNullOrBlank()
                      val srvChatCount = chatMessages.count { it.serviceId == srv.id && !it.isSystemNotice }
                      Text(
                        text = buildString {
                          append("Cliente: ${srv.clientName}")
                          if (hasAgreedProvider) {
                            append(" • Prestador: ${srv.acceptedProviderName ?: "Confirmado"}")
                          }
                          append(" • Orçamento: R$ ${String.format("%.2f", srv.finalAgreedPrice ?: srv.proposedBudget)} • Urgência: ${srv.urgency}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                      FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        if (hasAgreedProvider) {
                          Button(
                            onClick = { onOpenServiceChat(srv.id) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                              .heightIn(min = 48.dp)
                              .testTag("btn_admin_catalog_open_chat_${srv.id}")
                          ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Acompanhar Conversa ($srvChatCount)", fontWeight = FontWeight.Bold)
                          }
                        }
                        OutlinedButton(
                          onClick = { editingService = srv },
                          shape = RoundedCornerShape(10.dp),
                          modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_admin_edit_service_${srv.id}")
                        ) {
                          Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.width(6.dp))
                          Text("Editar Serviço", fontWeight = FontWeight.Bold)
                        }
                      }
                    }
                  }
                }
              }
            }

            AdminCatalogSectionTab.OUTRAS_EDICOES -> {
              LazyColumn(
                modifier = Modifier
                  .fillMaxSize()
                  .testTag("admin_other_edits_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 28.dp)
              ) {
                item(key = "app_settings_card") {
                  Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(
                      modifier = Modifier.padding(14.dp),
                      verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                          text = "Configurações do Aplicativo & Suporte",
                          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                      }

                      OutlinedTextField(
                        value = bannerText,
                        onValueChange = { bannerText = it },
                        label = { Text("Aviso Global na Tela de Serviços (opcional)") },
                        placeholder = { Text("Ex: Atendimento 24h com prestadores verificados!") },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                          .fillMaxWidth()
                          .testTag("input_admin_global_banner")
                      )

                      OutlinedTextField(
                        value = whatsappDigits,
                        onValueChange = { whatsappDigits = it },
                        label = { Text("WhatsApp de Suporte (somente números com DDI)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                          .fillMaxWidth()
                          .testTag("input_admin_whatsapp_number")
                      )

                      OutlinedTextField(
                        value = whatsappDisplay,
                        onValueChange = { whatsappDisplay = it },
                        label = { Text("Texto de Exibição do Suporte") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                      )

                      Text(
                        text = "Urgência padrão ao criar novos pedidos",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                      )
                      FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Baixa", "Média", "Alta", "Urgente").forEach { urg ->
                          FilterChip(
                            selected = defaultUrgency == urg,
                            onClick = { defaultUrgency = urg },
                            label = { Text(urg) }
                          )
                        }
                      }

                      Button(
                        onClick = {
                          onUpdateAppSettings(
                            appSettings.copy(
                              globalBannerMessage = bannerText,
                              supportWhatsappNumberE164 = whatsappDigits,
                              supportWhatsappDisplay = whatsappDisplay,
                              defaultServiceUrgency = defaultUrgency
                            )
                          )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                          .fillMaxWidth()
                          .heightIn(min = 48.dp)
                          .testTag("btn_admin_save_settings")
                      ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salvar Configurações do App", fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }

                item(key = "admin_shortcuts_card") {
                  Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(
                      modifier = Modifier.padding(14.dp),
                      verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                      Text(
                        text = "Outras Ferramentas Administrativas",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                      )
                      FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        OutlinedButton(
                          onClick = onOpenManageUsers,
                          modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_catalog_open_users")
                        ) {
                          Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.width(6.dp))
                          Text("Gerenciar Usuários")
                        }
                        OutlinedButton(
                          onClick = onOpenDocumentsReview,
                          modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_catalog_open_docs")
                        ) {
                          Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.width(6.dp))
                          Text("Documentos & Estornos")
                        }
                        OutlinedButton(
                          onClick = onOpenSuggestions,
                          modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_catalog_open_suggestions")
                        ) {
                          Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.width(6.dp))
                          Text("Sugestões dos Usuários")
                        }
                      }
                    }
                  }
                }

                item(key = "tips_header") {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "Dicas de Uso do Aplicativo (${appTips.size})",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Button(
                      onClick = { showAddTipDialog = true },
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_admin_add_tip")
                    ) {
                      Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Nova Dica", fontWeight = FontWeight.Bold)
                    }
                  }
                }

                items(appTips, key = { it.id }) { tip ->
                  Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(
                      modifier = Modifier.padding(14.dp),
                      verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = "${tip.category} • ${tip.targetRole?.label ?: "Todos"}",
                          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                          color = MaterialTheme.colorScheme.primary
                        )
                        Row {
                          IconButton(
                            onClick = { editingTip = tip },
                            modifier = Modifier.testTag("btn_edit_tip_${tip.id}")
                          ) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar dica")
                          }
                          IconButton(
                            onClick = { onDeleteTip(tip.id) },
                            modifier = Modifier.testTag("btn_delete_tip_${tip.id}")
                          ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Excluir dica", tint = DangerRed)
                          }
                        }
                      }
                      Text(
                        text = tip.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                      )
                      Text(
                        text = tip.description,
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
      }
    }
  }

  if (showAddCategoryDialog) {
    AdminCategoryEditorDialog(
      initialCategory = null,
      onDismiss = { showAddCategoryDialog = false },
      onSave = { name, specialty, _ -> onAddCategory(name, specialty) }
    )
  }

  editingCategory?.let { target ->
    AdminCategoryEditorDialog(
      initialCategory = target,
      onDismiss = { editingCategory = null },
      onSave = { name, specialty, active -> onUpdateCategory(target.id, name, specialty, active) }
    )
  }

  deletingCategory?.let { target ->
    AdminConfirmDeleteCategoryDialog(
      category = target,
      onDismiss = { deletingCategory = null },
      onConfirm = {
        onDeleteCategory(target.id)
        deletingCategory = null
      }
    )
  }

  editingService?.let { target ->
    AdminEditServiceDialog(
      service = target,
      availableCategories = sortedCategories.filter { it.isActive }.map { it.name },
      onDismiss = { editingService = null },
      onSave = { updated -> onUpdateService(updated) }
    )
  }

  if (showAddTipDialog) {
    AdminTipEditorDialog(
      initialTip = null,
      onDismiss = { showAddTipDialog = false },
      onSave = { tip -> onSaveTip(tip) }
    )
  }

  editingTip?.let { target ->
    AdminTipEditorDialog(
      initialTip = target,
      onDismiss = { editingTip = null },
      onSave = { tip -> onSaveTip(tip) }
    )
  }
}

@Composable
private fun AdminCatalogHeader(
  adminUser: User,
  activeCategoriesCount: Int,
  totalCategoriesCount: Int,
  onMenuClick: () -> Unit,
  onBackClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        Brush.linearGradient(
          colors = listOf(Color(0xFF0F172A), BrandPrimaryVariant, BrandPrimary)
        )
      )
      .padding(horizontal = 16.dp, vertical = 12.dp)
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
        IconButton(
          onClick = onMenuClick,
          modifier = Modifier
            .size(48.dp)
            .testTag("btn_admin_catalog_menu")
        ) {
          Icon(imageVector = Icons.Default.Menu, contentDescription = "Abrir menu", tint = Color.White)
        }
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.AdminPanelSettings,
              contentDescription = null,
              tint = Color(0xFF93C5FD),
              modifier = Modifier.size(15.dp)
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
            text = "Categorias e Edições",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${adminUser.name} • $activeCategoriesCount/$totalCategoriesCount categorias ativas",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = Color.White.copy(alpha = 0.78f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      IconButton(
        onClick = onBackClick,
        modifier = Modifier
          .size(48.dp)
          .testTag("btn_admin_catalog_back_home")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Voltar para Serviços",
          tint = Color.White
        )
      }
    }
  }
}

@Composable
private fun AdminCategoryItemCard(
  category: ServiceCategoryItem,
  canMoveUp: Boolean,
  canMoveDown: Boolean,
  canDelete: Boolean,
  onToggleActive: (Boolean) -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onEditClick: () -> Unit,
  onDeleteClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (category.isActive) {
        MaterialTheme.colorScheme.surface
      } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
      }
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("admin_category_card_${category.id}")
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
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
          Surface(
            color = if (category.isActive) EmeraldWalletLight else MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape,
            modifier = Modifier.size(38.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Category,
                contentDescription = null,
                tint = if (category.isActive) EmeraldWallet else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = category.name,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "Especialidade: ${category.effectiveSpecialtyLabel}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
        Switch(
          checked = category.isActive,
          onCheckedChange = onToggleActive,
          modifier = Modifier.testTag("switch_category_${category.id}")
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
          IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(Icons.Default.ArrowUpward, contentDescription = "Subir categoria", modifier = Modifier.size(18.dp))
          }
          IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(Icons.Default.ArrowDownward, contentDescription = "Descer categoria", modifier = Modifier.size(18.dp))
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          OutlinedButton(
            onClick = onEditClick,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("btn_edit_category_${category.id}")
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Editar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }
          IconButton(
            onClick = onDeleteClick,
            enabled = canDelete,
            modifier = Modifier.testTag("btn_delete_category_${category.id}")
          ) {
            Icon(
              Icons.Default.DeleteOutline,
              contentDescription = "Remover categoria",
              tint = if (canDelete) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
          }
        }
      }
    }
  }
}
