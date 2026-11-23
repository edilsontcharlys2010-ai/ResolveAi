package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.ContactFilter
import com.example.model.ContactFilterResult
import com.example.model.ServiceRequest
import com.example.model.ServiceStatus
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.components.ContactViolationCard
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.IconButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.example.R
import androidx.compose.material.icons.filled.BugReport
import com.example.data.ChatDiagnosticCategory
import com.example.data.ChatDiagnosticLogEntry
import com.example.ui.components.ChatDiagnosticsDialog
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatUserRating
import com.example.ui.components.resolveAdaptiveSpec
import java.util.Locale

@Composable
fun ChatScreen(
  currentUser: User,
  services: List<ServiceRequest>,
  selectedServiceId: String?,
  chatMessages: List<ChatMessage>,
  onSelectService: (String) -> Unit,
  onSendMessage: (serviceId: String, text: String) -> ContactFilterResult,
  onBack: () -> Unit = {},
  onRateServiceClick: ((ServiceRequest) -> Unit)? = null,
  onOpenMenu: (() -> Unit)? = null,
  chatDiagnosticLogs: List<ChatDiagnosticLogEntry> = emptyList(),
  onClearChatDiagnostics: () -> Unit = {},
  onExportChatDiagnostics: (String?) -> String = { "" }
) {
  BackHandler { onBack() }

  var showProtectionInfoDialog by remember { mutableStateOf(false) }
  var showDiagnosticsDialog by remember { mutableStateOf(false) }
  var adminChatSearchQuery by remember { mutableStateOf("") }
  val isAdminViewer = currentUser.role == UserRole.ADMIN

  // Chat option only appears/unlocks when client accepts proposal/counter-proposal
  // REGRA DE PRIVACIDADE: Cliente só visualiza chats dos seus próprios pedidos;
  // Administrador pode acompanhar (somente leitura) todas as conversas de serviços acordados.
  val acceptedServices = remember(services, currentUser, chatMessages, isAdminViewer) {
    services.filter { srv ->
      val hasAgreedProvider = !srv.acceptedProviderId.isNullOrBlank()
      val isAccepted = hasAgreedProvider && (
        srv.status == ServiceStatus.ACEITO ||
          srv.status == ServiceStatus.EM_EXECUCAO ||
          srv.status == ServiceStatus.CONCLUIDO ||
          (isAdminViewer && srv.status == ServiceStatus.CANCELADO)
        )

      val isUserService = when (currentUser.role) {
        UserRole.CLIENTE -> currentUser.ownsId(srv.clientId)
        UserRole.PRESTADOR -> currentUser.ownsId(srv.acceptedProviderId)
        UserRole.ADMIN -> true
      }

      isAccepted && isUserService
    }.sortedByDescending { srv ->
      chatMessages.filter { it.serviceId == srv.id && !it.isSystemNotice }
        .maxOfOrNull { it.timestamp } ?: srv.createdAt
    }
  }

  val visibleAcceptedServices = remember(acceptedServices, adminChatSearchQuery, isAdminViewer) {
    val q = adminChatSearchQuery.trim().lowercase()
    if (!isAdminViewer || q.isBlank()) {
      acceptedServices
    } else {
      acceptedServices.filter { srv ->
        srv.title.lowercase().contains(q) ||
          srv.clientName.lowercase().contains(q) ||
          (srv.acceptedProviderName ?: "").lowercase().contains(q) ||
          srv.category.lowercase().contains(q)
      }.ifEmpty { acceptedServices }
    }
  }

  if (acceptedServices.isEmpty()) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .statusBarsPadding(),
      contentAlignment = Alignment.TopCenter
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 560.dp)
          .fillMaxSize()
      ) {
        // Top back/menu button
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (onOpenMenu != null) {
            IconButton(
              onClick = onOpenMenu,
              modifier = Modifier.testTag("btn_chat_open_menu")
            ) {
              Icon(Icons.Default.Menu, contentDescription = "Abrir Menu")
            }
          }
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar para Serviços")
          }
          Text(
            text = if (isAdminViewer) stringResource(R.string.admin_chat_monitor_title) else "Chat Privado Seguro",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        }

        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Surface(
            color = if (isAdminViewer) InfoBlueLight else WarningAmberLight,
            shape = CircleShape,
            modifier = Modifier.size(72.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = if (isAdminViewer) Icons.Default.Visibility else Icons.Default.Lock,
                contentDescription = null,
                tint = if (isAdminViewer) InfoBlue else WarningAmber,
                modifier = Modifier.size(36.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = if (isAdminViewer) "Nenhuma Conversa Acordada" else "Chat Privado Bloqueado",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = if (isAdminViewer) {
              "Assim que um cliente aceitar a proposta de um prestador, a conversa entre as partes aparecerá aqui para acompanhamento do Administrador."
            } else {
              "A opção de chat privado entre cliente e prestador só aparece e é liberada quando o cliente aceitar a proposta ou contraproposta."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(12.dp))

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "🛡️ Por que esta trava de segurança existe?",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Para proteger ambas as partes, evitar tentativas de evasão e assegurar que todo o atendimento aconteça dentro do ambiente verificado da plataforma, liberando o canal direto e a garantia de suporte.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          Button(
            onClick = onBack,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth(0.8f)
              .heightIn(min = 48.dp)
          ) {
            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Voltar aos Serviços", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
    return
  }

  val activeServiceId = if (acceptedServices.any { it.id == selectedServiceId }) {
    selectedServiceId!!
  } else {
    visibleAcceptedServices.first().id
  }
  val activeService = acceptedServices.find { it.id == activeServiceId }

  val messagesForActiveService = chatMessages.filter { it.serviceId == activeServiceId }

  var messageText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  // Real-time evaluation of anti-contact sharing rule
  val filterResult by remember(messageText) {
    derivedStateOf {
      if (messageText.isBlank()) {
        ContactFilterResult(isAllowed = true)
      } else {
        ContactFilter.evaluate(messageText)
      }
    }
  }

  // Auto-scroll on new message
  LaunchedEffect(messagesForActiveService.size) {
    if (messagesForActiveService.isNotEmpty()) {
      listState.animateScrollToItem(messagesForActiveService.size - 1)
    }
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
  ) {
    val spec = remember(maxWidth) { resolveAdaptiveSpec(maxWidth) }
    val useSplitPane = maxWidth >= 720.dp && acceptedServices.size > 1
    val bubbleMaxWidth = remember(maxWidth, useSplitPane) {
      val contentWidth = if (useSplitPane) maxWidth - 280.dp else maxWidth
      (contentWidth * 0.78f).coerceIn(220.dp, 500.dp)
    }

    Row(modifier = Modifier.fillMaxSize()) {
      if (useSplitPane) {
        Column(
          modifier = Modifier
            .width(290.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = if (isAdminViewer) "Serviços Acordados (${acceptedServices.size})" else "Conversas (${acceptedServices.size})",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
          )
          if (isAdminViewer && acceptedServices.size > 1) {
            OutlinedTextField(
              value = adminChatSearchQuery,
              onValueChange = { adminChatSearchQuery = it },
              placeholder = { Text("Filtrar serviço ou parte...", fontSize = 12.sp) },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar conversa", modifier = Modifier.size(16.dp)) },
              trailingIcon = {
                if (adminChatSearchQuery.isNotEmpty()) {
                  IconButton(onClick = { adminChatSearchQuery = "" }) {
                    Icon(Icons.Default.Close, contentDescription = "Limpar busca", modifier = Modifier.size(16.dp))
                  }
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_admin_chat_search")
            )
          }
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .testTag(if (isAdminViewer) "admin_chat_services_list" else "chat_services_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(visibleAcceptedServices, key = { it.id }) { srv ->
              val isSelected = srv.id == activeServiceId
              val msgCount = chatMessages.count { it.serviceId == srv.id && !it.isSystemNotice }
              val name = when (currentUser.role) {
                UserRole.CLIENTE -> srv.acceptedProviderName ?: "Prestador"
                UserRole.PRESTADOR -> srv.clientName
                UserRole.ADMIN -> "${srv.clientName} ↔ ${srv.acceptedProviderName ?: "Prestador"}"
              }
              val photo = if (currentUser.role == UserRole.CLIENTE) {
                srv.acceptedProviderPhotoUrl
              } else {
                srv.clientPhotoUrl
              }
              val colorHex = if (currentUser.role == UserRole.CLIENTE) {
                srv.acceptedProviderAvatarColorHex ?: 0xFF0D9488
              } else {
                srv.clientAvatarColorHex
              }
              Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { onSelectService(srv.id) }
                  .testTag(if (isAdminViewer) "admin_chat_service_chip_${srv.id}" else "chat_service_chip_${srv.id}")
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  ProfileAvatar(
                    name = srv.clientName,
                    photoUrl = photo,
                    avatarColorHex = colorHex,
                    size = 34.dp
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = srv.title,
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = name,
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                      color = MaterialTheme.colorScheme.primary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    if (isAdminViewer) {
                      Text(
                        text = "${srv.status.label} • $msgCount msg(s)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                      )
                    }
                  }
                }
              }
            }
          }
        }
        VerticalDivider()
      }

      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
      ) {
    // Top Bar with Menu/Back Button, Service Details & Data Protection Dialog trigger
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 2.dp,
      shadowElevation = 1.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (onOpenMenu != null) {
          IconButton(
            onClick = onOpenMenu,
            modifier = Modifier.testTag("btn_chat_open_menu")
          ) {
            Icon(Icons.Default.Menu, contentDescription = "Abrir Menu")
          }
        }

        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("btn_chat_back_to_services")
        ) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Voltar para Serviços")
        }

        if (isAdminViewer && activeService != null) {
          Surface(
            color = InfoBlueLight,
            shape = CircleShape,
            modifier = Modifier.size(34.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = "Acompanhamento Admin",
                tint = InfoBlue,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = activeService.title,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "Cliente: ${activeService.clientName} ↔ Prestador: ${activeService.acceptedProviderName ?: "Prestador"}",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Spacer(modifier = Modifier.width(6.dp))
          StatusBadge(status = activeService.status)
        } else {
          val counterpartName = if (currentUser.role == UserRole.CLIENTE) {
            activeService?.acceptedProviderName ?: "Prestador"
          } else {
            activeService?.clientName ?: "Cliente"
          }
          val counterpartPhoto = if (currentUser.role == UserRole.CLIENTE) {
            activeService?.acceptedProviderPhotoUrl
          } else {
            activeService?.clientPhotoUrl
          }
          val counterpartColor = if (currentUser.role == UserRole.CLIENTE) {
            activeService?.acceptedProviderAvatarColorHex ?: 0xFF0D9488
          } else {
            activeService?.clientAvatarColorHex ?: 0xFF2563EB
          }
          val counterpartRating = if (currentUser.role == UserRole.CLIENTE) {
            activeService?.acceptedProviderRating ?: 5.0
          } else {
            activeService?.clientRating ?: 5.0
          }

          ProfileAvatar(
            name = counterpartName,
            photoUrl = counterpartPhoto,
            avatarColorHex = counterpartColor,
            size = 34.dp
          )

          Spacer(modifier = Modifier.width(8.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = activeService?.title ?: "Chat Seguro",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = counterpartName,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Nota",
                tint = WarningAmber,
                modifier = Modifier.size(12.dp)
              )
              Text(
                text = formatUserRating(counterpartRating),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "• R$ ${String.format("%.2f", activeService?.finalAgreedPrice ?: activeService?.proposedBudget ?: 0.0)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = EmeraldWallet,
                maxLines = 1
              )
            }
          }

          // Data protection badge trigger
          Surface(
            color = Color(0xFFEFF6FF),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .clickable { showProtectionInfoDialog = true }
              .padding(end = 6.dp)
              .testTag("btn_data_protection_info")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = if (spec.isNarrowPhone) 6.dp else 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (spec.isNarrowPhone) "LGPD" else "Proteção LGPD",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
              )
            }
          }
        }

        // Firestore Chat Diagnostic Logs trigger (visible for Client, Provider, and Admin)
        val activeDiagLogs = remember(chatDiagnosticLogs, activeServiceId) {
          chatDiagnosticLogs.filter { it.serviceId == activeServiceId }
        }
        val hasPermDiagError = activeDiagLogs.any {
          it.category == ChatDiagnosticCategory.SEND_PERMISSION_DENIED ||
            it.category == ChatDiagnosticCategory.LISTENER_PERMISSION_DENIED ||
            it.category == ChatDiagnosticCategory.SEND_AUTH_MISSING
        }
        val hasTimeoutDiagWarning = activeDiagLogs.any {
          it.category == ChatDiagnosticCategory.SEND_NETWORK_TIMEOUT ||
            it.category == ChatDiagnosticCategory.LISTENER_NETWORK_TIMEOUT
        }
        val diagTint = when {
          hasPermDiagError -> DangerRed
          hasTimeoutDiagWarning -> WarningAmber
          else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        IconButton(
          onClick = { showDiagnosticsDialog = true },
          modifier = Modifier.testTag("btn_chat_diagnostics")
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.BugReport,
              contentDescription = "Diagnóstico do Chat Firestore",
              tint = diagTint,
              modifier = Modifier.size(20.dp)
            )
            if (hasPermDiagError || hasTimeoutDiagWarning) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .align(Alignment.TopEnd)
                  .background(if (hasPermDiagError) DangerRed else WarningAmber, CircleShape)
              )
            }
          }
        }
      }
    }

    // Service selector chips bar on compact/phone widths
    if (!useSplitPane && (acceptedServices.size > 1 || isAdminViewer)) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        if (isAdminViewer && acceptedServices.size > 1) {
          OutlinedTextField(
            value = adminChatSearchQuery,
            onValueChange = { adminChatSearchQuery = it },
            placeholder = { Text("Buscar por serviço, cliente ou prestador...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar conversa", modifier = Modifier.size(16.dp)) },
            trailingIcon = {
              if (adminChatSearchQuery.isNotEmpty()) {
                IconButton(onClick = { adminChatSearchQuery = "" }) {
                  Icon(Icons.Default.Close, contentDescription = "Limpar busca", modifier = Modifier.size(16.dp))
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_admin_chat_search")
          )
        }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag(if (isAdminViewer) "admin_chat_services_list" else "chat_services_list"),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          visibleAcceptedServices.forEach { srv ->
            val isSelected = srv.id == activeServiceId
            val msgCount = chatMessages.count { it.serviceId == srv.id && !it.isSystemNotice }
            Surface(
              color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
              border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .clickable { onSelectService(srv.id) }
                .testTag(if (isAdminViewer) "admin_chat_service_chip_${srv.id}" else "chat_service_chip_${srv.id}")
            ) {
              Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = if (isAdminViewer) Icons.Default.Visibility else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = srv.title.take(24) + (if (srv.title.length > 24) "..." else ""),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                  )
                }
                if (isAdminViewer) {
                  Text(
                    text = "${srv.clientName} ↔ ${srv.acceptedProviderName ?: "Prestador"} • $msgCount msg(s)",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = if (isSelected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                  )
                }
              }
            }
          }
        }
      }
    }

    // Security Notice Banner or Admin Monitoring Banner
    if (isAdminViewer) {
      Surface(
        color = InfoBlueLight,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("admin_chat_monitor_banner")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Visibility,
            contentDescription = null,
            tint = InfoBlue,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Acompanhamento do Administrador • ${acceptedServices.size} serviço(s) acordado(s) • Cliente à esquerda (Azul) • Prestador à direita (Verde)",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
            color = InfoBlue
          )
        }
      }
    } else {
      Surface(
        color = Color(0xFFFFFBEB),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = WarningAmber,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Regra de Proteção: Bloqueio automático de telefones, e-mails, WhatsApp e redes sociais. Mantenha todas as tratativas no app.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            color = Color(0xFF92400E)
          )
        }
      }
    }

    if (activeService != null &&
      activeService.status == ServiceStatus.CONCLUIDO &&
      currentUser.role == UserRole.CLIENTE &&
      currentUser.ownsId(activeService.clientId)
    ) {
      if (activeService.ratingGiven == null) {
        Surface(
          color = WarningAmberLight,
          border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.55f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_pending_rating_banner")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = null,
              tint = WarningAmber,
              modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.review_chat_pending_banner),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF78350F)
              )
              Text(
                text = "Avalie ${activeService.acceptedProviderName ?: "o prestador"} após o atendimento.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Color(0xFF92400E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            if (onRateServiceClick != null) {
              Button(
                onClick = { onRateServiceClick(activeService) },
                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                  .heightIn(min = 48.dp)
                  .testTag("btn_chat_rate_service")
              ) {
                Icon(
                  imageVector = Icons.Default.Star,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Avaliar ★", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      } else {
        Surface(
          color = EmeraldWalletLight.copy(alpha = 0.75f),
          border = BorderStroke(1.dp, EmeraldWallet.copy(alpha = 0.45f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_rated_summary_banner")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              val filled = kotlin.math.round(activeService.ratingGiven).toInt().coerceIn(1, 5)
              (1..5).forEach { star ->
                Icon(
                  imageVector = if (star <= filled) Icons.Default.Star else Icons.Default.StarBorder,
                  contentDescription = null,
                  tint = WarningAmber,
                  modifier = Modifier.size(15.dp)
                )
              }
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Atendimento avaliado com ${formatUserRating(activeService.ratingGiven.toDouble())} / 5",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF065F46)
              )
            }
          }
        }
      }
    }

    // Messages Stream
    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .padding(horizontal = spec.horizontalPadding),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = PaddingValues(vertical = 12.dp)
    ) {
      if (isAdminViewer && messagesForActiveService.none { !it.isSystemNotice }) {
        item(key = "admin_empty_messages") {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp)
              .testTag("admin_chat_empty_card")
          ) {
            Text(
              text = stringResource(R.string.admin_chat_empty_messages),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
            )
          }
        }
      }
      items(messagesForActiveService, key = { it.id }) { msg ->
        ChatMessageBubble(
          message = msg,
          isFromMe = currentUser.ownsId(msg.senderId),
          isAdminViewer = isAdminViewer,
          maxBubbleWidth = bubbleMaxWidth
        )
      }
    }

    if (isAdminViewer) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("admin_chat_readonly_bar")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Default.Visibility,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.admin_chat_readonly_notice),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
        }
      }
    } else {
      // Real-Time Contact Violation Alert Card
      AnimatedVisibility(
        visible = !filterResult.isAllowed,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        ContactViolationCard(
          violation = filterResult,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
      }

      // Quick Test Chips (To verify anti-leak behavior conveniently)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = "Testar filtro:",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.align(Alignment.CenterVertically)
        )

        QuickTestChip(
          label = "⚠️ Tel/Whats",
          snippet = "Chama no zap 11 98888-7777",
          onSelect = { messageText = it }
        )
        QuickTestChip(
          label = "⚠️ Email",
          snippet = "Manda email para contato@pro.com",
          onSelect = { messageText = it }
        )
        QuickTestChip(
          label = "⚠️ Insta",
          snippet = "Segue meu insta @reparos_sp",
          onSelect = { messageText = it }
        )
        QuickTestChip(
          label = "⚠️ Chave Pix",
          snippet = "Minha chave pix é 123.456.789-00",
          onSelect = { messageText = it }
        )
        QuickTestChip(
          label = "✅ Válida",
          snippet = "Perfeito! Posso ir amanhã às 14h com as ferramentas.",
          onSelect = { messageText = it }
        )
      }

      // Message Input Area
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = messageText,
            onValueChange = { messageText = it },
            placeholder = { Text("Digite sua mensagem segura...") },
            isError = !filterResult.isAllowed,
            maxLines = 4,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = if (filterResult.isAllowed) MaterialTheme.colorScheme.primary else DangerRed,
              unfocusedBorderColor = if (filterResult.isAllowed) MaterialTheme.colorScheme.outlineVariant else DangerRed
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("chat_input_field")
          )

          Spacer(modifier = Modifier.width(8.dp))

          // Send Button
          FilledIconButton(
            onClick = {
              if (messageText.isNotBlank() && filterResult.isAllowed) {
                onSendMessage(activeServiceId, messageText)
                messageText = ""
              }
            },
            enabled = messageText.isNotBlank() && filterResult.isAllowed,
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = if (filterResult.isAllowed) MaterialTheme.colorScheme.primary else DangerRed,
              disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
              .size(48.dp)
              .testTag("chat_send_button")
          ) {
            Icon(
              imageVector = if (filterResult.isAllowed) Icons.Default.Send else Icons.Default.Block,
              contentDescription = "Enviar",
              tint = if (messageText.isNotBlank() && filterResult.isAllowed) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
      }
    }
  }

  if (showProtectionInfoDialog) {
    DataProtectionInfoDialog(
      onDismiss = { showProtectionInfoDialog = false }
    )
  }

  if (showDiagnosticsDialog) {
    ChatDiagnosticsDialog(
      serviceId = activeServiceId,
      serviceTitle = activeService?.title ?: "Chat",
      logs = chatDiagnosticLogs,
      onClearLogs = onClearChatDiagnostics,
      onExportReport = onExportChatDiagnostics,
      onDismiss = { showDiagnosticsDialog = false }
    )
  }
}

@Composable
fun DataProtectionInfoDialog(
  onDismiss: () -> Unit
) {
  androidx.compose.material3.AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Security,
        contentDescription = "Proteção de Dados",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(36.dp)
      )
    },
    title = {
      Text(
        text = "Proteção de Dados & Privacidade",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Surface(
          color = Color(0xFFEFF6FF),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "🛡️ Bloqueio Ativo de Contatos",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "O sistema ResolveAi analisa em tempo real e impede o envio de números de telefone, WhatsApp, e-mails, redes sociais e chaves Pix no chat.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Surface(
          color = Color(0xFFF0FDF4),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "🔒 Segurança Financeira & Garantia",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = EmeraldWallet
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Manter as tratativas dentro da plataforma protege ambas as partes contra golpes externos e garante o suporte completo ResolveAi do início à conclusão.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "📋 Conformidade LGPD",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Seus dados pessoais permanecem protegidos, criptografados e restritos ao ambiente seguro do aplicativo.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    },
    confirmButton = {
      androidx.compose.material3.Button(onClick = onDismiss) {
        Text("Entendi e Concordo")
      }
    }
  )
}

@Composable
private fun QuickTestChip(
  label: String,
  snippet: String,
  onSelect: (String) -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.clickable { onSelect(snippet) }
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
}

@Composable
private fun ChatMessageBubble(
  message: ChatMessage,
  isFromMe: Boolean,
  isAdminViewer: Boolean = false,
  maxBubbleWidth: Dp = 280.dp
) {
  val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
  val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }
  val safeContent = remember(message.content) { ContactFilter.maskSensitiveData(message.content) }

  if (message.isSystemNotice) {
    // System message banner
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
      contentAlignment = Alignment.Center
    ) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.widthIn(max = 420.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = safeContent,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  } else {
    val isProviderMsg = message.senderRole == UserRole.PRESTADOR
    val alignEnd = if (isAdminViewer) isProviderMsg else isFromMe
    val bubbleContainerColor = when {
      isAdminViewer && isProviderMsg -> EmeraldWalletLight
      isAdminViewer -> InfoBlueLight
      isFromMe -> MaterialTheme.colorScheme.primary
      else -> MaterialTheme.colorScheme.surface
    }
    val bubbleTextColor = when {
      isAdminViewer -> MaterialTheme.colorScheme.onSurface
      isFromMe -> Color.White
      else -> MaterialTheme.colorScheme.onSurface
    }
    val metaTextColor = when {
      isAdminViewer -> MaterialTheme.colorScheme.onSurfaceVariant
      isFromMe -> Color.White.copy(alpha = 0.7f)
      else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    // User message bubble
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
      // Sender label
      if (isAdminViewer) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
          Surface(
            color = if (isProviderMsg) EmeraldWallet else InfoBlue,
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = if (isProviderMsg) "Prestador" else "Cliente",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
              color = Color.White,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
            )
          }
          Text(
            text = message.senderName,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      } else {
        Text(
          text = if (isFromMe) "Você" else "${message.senderName} (${if (message.senderRole == UserRole.CLIENTE) "Cliente" else "Prestador"})",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
      }

      Surface(
        color = bubbleContainerColor,
        border = if (isAdminViewer) {
          BorderStroke(1.dp, (if (isProviderMsg) EmeraldWallet else InfoBlue).copy(alpha = 0.35f))
        } else null,
        shape = RoundedCornerShape(
          topStart = 16.dp,
          topEnd = 16.dp,
          bottomStart = if (alignEnd) 16.dp else 4.dp,
          bottomEnd = if (alignEnd) 4.dp else 16.dp
        ),
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.widthIn(max = maxBubbleWidth)
      ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
          Text(
            text = safeContent,
            style = MaterialTheme.typography.bodyMedium,
            color = bubbleTextColor
          )
          Spacer(modifier = Modifier.height(3.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.End)
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "Protegido",
              tint = if (!isAdminViewer && isFromMe) Color.White.copy(alpha = 0.7f) else EmeraldWallet,
              modifier = Modifier.size(9.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "Seguro • $formattedTime",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
              color = metaTextColor
            )
          }
        }
      }
    }
  }
}
