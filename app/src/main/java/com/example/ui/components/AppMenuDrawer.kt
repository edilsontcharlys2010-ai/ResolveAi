package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CloudSyncState
import com.example.data.CloudSyncStatus
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.MainTab
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppMenuDrawerContent(
  currentUser: User,
  selectedTab: MainTab,
  pendingProposalsCount: Int,
  unreadNotificationsCount: Int,
  pendingAdminReviewsCount: Int = 0,
  onSelectTab: (MainTab) -> Unit,
  onNotificationsClick: () -> Unit,
  onSupportClick: () -> Unit,
  onAdminClick: () -> Unit,
  onSwitchUserClick: () -> Unit,
  onRegisterClick: () -> Unit,
  onRechargeClick: () -> Unit,
  onChangePasswordClick: () -> Unit = {},
  onLogoutClick: () -> Unit = {},
  onOpenOnboarding: () -> Unit = {},
  onCloseDrawer: () -> Unit,
  categoryFilter: String = "Serviços",
  urgencyFilter: String = "Todas",
  availableCategories: List<String> = emptyList(),
  onSelectCategory: (String) -> Unit = {},
  onSelectUrgency: (String) -> Unit = {},
  onResetFilters: () -> Unit = {},
  agreedChatsCount: Int = 0,
  cloudSyncStatus: CloudSyncStatus? = null,
  onSyncFirebaseClick: () -> Unit = {},
  onOpenProfileClick: () -> Unit = { onSelectTab(MainTab.CARTEIRA_PERFIL) }
) {
  ModalDrawerSheet(
    drawerContainerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier
      .widthIn(max = 320.dp)
      .fillMaxWidth(0.84f)
      .fillMaxHeight()
  ) {
    Column(
      modifier = Modifier
        .fillMaxHeight()
        .statusBarsPadding()
        .verticalScroll(rememberScrollState())
    ) {
      // 1. IN-DRIVE STYLE USER PROFILE HEADER
      Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // App Branding in Drawer
            Row(verticalAlignment = Alignment.CenterVertically) {
              Image(
                painter = painterResource(id = R.drawable.resolveai_logo),
                contentDescription = "Logo ResolveAi Serviços",
                modifier = Modifier
                  .size(36.dp)
                  .clip(RoundedCornerShape(8.dp))
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "ResolveAi",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                  ),
                  color = MaterialTheme.colorScheme.primary
                )
                Text(
                  text = "Serviços & Negociação",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                  color = EmeraldWallet
                )
              }
            }

            IconButton(onClick = onCloseDrawer) {
              Icon(Icons.Default.Close, contentDescription = "Fechar Menu")
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // User Card (clicável na foto ou dados para abrir o perfil e editar)
          Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .clickable {
                onCloseDrawer()
                onOpenProfileClick()
              }
              .testTag("drawer_user_profile_card")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(54.dp)
                  .clip(CircleShape)
                  .clickable {
                    onCloseDrawer()
                    onOpenProfileClick()
                  }
                  .testTag("drawer_profile_avatar")
              ) {
                ProfileAvatar(
                  name = currentUser.name,
                  photoUrl = currentUser.photoUrl,
                  avatarColorHex = currentUser.avatarColorHex,
                  size = 52.dp,
                  contentDescription = stringResource(R.string.profile_avatar_open_cd)
                )
                Surface(
                  color = MaterialTheme.colorScheme.primary,
                  shape = CircleShape,
                  shadowElevation = 2.dp,
                  modifier = Modifier
                    .size(19.dp)
                    .align(Alignment.BottomEnd)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.Edit,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(11.dp)
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = currentUser.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  if (currentUser.role == UserRole.PRESTADOR) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                      imageVector = Icons.Default.Verified,
                      contentDescription = "Verificado",
                      tint = EmeraldWallet,
                      modifier = Modifier.size(15.dp)
                    )
                  }
                }

                // Role chip & Rating
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Surface(
                    color = if (currentUser.role == UserRole.PRESTADOR) EmeraldWalletLight else Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = currentUser.role.label,
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                      color = if (currentUser.role == UserRole.PRESTADOR) EmeraldWallet else MaterialTheme.colorScheme.primary,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }

                  if (currentUser.role == UserRole.PRESTADOR) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(13.dp))
                      Spacer(modifier = Modifier.width(2.dp))
                      Text(
                        text = "${currentUser.rating}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = WarningAmber
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = stringResource(R.string.drawer_profile_edit_hint),
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  color = MaterialTheme.colorScheme.primary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }

              Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.profile_avatar_open_cd),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          // Wallet preview & quick recharge (Exclusivo para Prestadores)
          if (currentUser.role == UserRole.PRESTADOR) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
              color = Color.White,
              shape = RoundedCornerShape(12.dp),
              shadowElevation = 1.dp,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(
                    text = if (currentUser.walletBalance < 0.0) {
                      "Saldo Negativo (desconto na recarga)"
                    } else {
                      "Carteira do Prestador"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = if (currentUser.walletBalance < 0.0) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = if (currentUser.walletBalance < 0.0) {
                      "-R$ ${String.format("%.2f", -currentUser.walletBalance)}"
                    } else {
                      "R$ ${String.format("%.2f", currentUser.walletBalance)}"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = if (currentUser.walletBalance < 0.0) DangerRed else EmeraldWallet
                    )
                  )
                }

                Button(
                  onClick = {
                    onCloseDrawer()
                    onRechargeClick()
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text("+ Recarregar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }

      if (cloudSyncStatus != null) {
        val (syncIcon, syncTint, syncBg, syncTitleRes) = when (cloudSyncStatus.state) {
          CloudSyncState.ONLINE_SYNCED -> listOf(
            Icons.Default.CloudDone,
            EmeraldWallet,
            EmeraldWalletLight,
            R.string.firebase_sync_online_title
          )
          CloudSyncState.SYNCING -> listOf(
            Icons.Default.CloudSync,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            R.string.firebase_sync_syncing_title
          )
          CloudSyncState.ERROR -> listOf(
            Icons.Default.CloudOff,
            DangerRed,
            DangerRed.copy(alpha = 0.1f),
            R.string.firebase_sync_error_title
          )
          CloudSyncState.OFFLINE_LOCAL -> listOf(
            Icons.Default.CloudOff,
            WarningAmber,
            WarningAmber.copy(alpha = 0.12f),
            R.string.firebase_sync_offline_title
          )
        }
        val tintColor = syncTint as Color
        val bgColor = syncBg as Color
        val titleRes = syncTitleRes as Int
        val iconVector = syncIcon as ImageVector

        Surface(
          color = bgColor,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, tintColor.copy(alpha = 0.25f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("drawer_firebase_sync_status")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (cloudSyncStatus.state == CloudSyncState.SYNCING) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = tintColor
              )
            } else {
              Icon(
                imageVector = iconVector,
                contentDescription = stringResource(titleRes),
                tint = tintColor,
                modifier = Modifier.size(18.dp)
              )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = tintColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (cloudSyncStatus.statusMessage.isNotBlank()) {
                Text(
                  text = cloudSyncStatus.statusMessage,
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
            OutlinedButton(
              onClick = onSyncFirebaseClick,
              enabled = cloudSyncStatus.state != CloudSyncState.SYNCING,
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, tintColor.copy(alpha = 0.45f)),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("btn_drawer_sync_firebase")
            ) {
              Text(
                text = stringResource(R.string.firebase_sync_now_cta),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = tintColor
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 2. MAIN NAVIGATION ITEMS (IN-DRIVE STYLE)
      Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        Text(
          text = "NAVEGAÇÃO",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )

        // Tab 1: Serviços / Oportunidades (Home)
        val tab1Label = when (currentUser.role) {
          UserRole.CLIENTE -> "Serviços & Pedidos"
          UserRole.PRESTADOR -> "Oportunidades de Trabalho"
          UserRole.ADMIN -> "Gestão de Serviços"
        }
        MenuDrawerItem(
          icon = if (currentUser.role == UserRole.PRESTADOR) Icons.Default.Build else Icons.Default.Work,
          label = tab1Label,
          selected = selectedTab == MainTab.SERVICOS,
          onClick = {
            onCloseDrawer()
            onSelectTab(MainTab.SERVICOS)
          },
          testTag = "menu_item_servicos"
        )

        // Tab 2: Propostas & Negociações
        val tab2Label = when (currentUser.role) {
          UserRole.CLIENTE -> "Minhas Negociações"
          UserRole.PRESTADOR -> "Minhas Negociações"
          UserRole.ADMIN -> "Todas as Propostas"
        }
        MenuDrawerItem(
          icon = Icons.Default.Handshake,
          label = tab2Label,
          badgeCount = pendingProposalsCount,
          selected = selectedTab == MainTab.NEGOCIACOES,
          onClick = {
            onCloseDrawer()
            onSelectTab(MainTab.NEGOCIACOES)
          },
          testTag = "menu_item_negociacoes"
        )

        // Tab 3: Chat Privado Seguro (ou Acompanhamento Admin de Conversas Acordadas)
        val chatLabel = if (currentUser.role == UserRole.ADMIN) {
          stringResource(R.string.admin_chat_monitor_title)
        } else {
          "Chat Privado Seguro"
        }
        val chatSubLabel = if (currentUser.role == UserRole.ADMIN) {
          stringResource(R.string.admin_chat_monitor_subtitle)
        } else {
          "Proteção anti-vazamento ativa"
        }
        MenuDrawerItem(
          icon = Icons.Default.Chat,
          label = chatLabel,
          subLabel = chatSubLabel,
          badgeCount = if (currentUser.role == UserRole.ADMIN) agreedChatsCount else 0,
          selected = selectedTab == MainTab.CHAT,
          onClick = {
            onCloseDrawer()
            onSelectTab(MainTab.CHAT)
          },
          testTag = "menu_item_chat"
        )

        // Tab 4: Carteira & Perfil
        val tab4Label = when (currentUser.role) {
          UserRole.CLIENTE -> "Meu Perfil"
          UserRole.PRESTADOR -> "Carteira & Extrato (10%)"
          UserRole.ADMIN -> "Painel do Admin"
        }
        MenuDrawerItem(
          icon = if (currentUser.role == UserRole.PRESTADOR) Icons.Default.AccountBalanceWallet else Icons.Default.Person,
          label = tab4Label,
          selected = selectedTab == MainTab.CARTEIRA_PERFIL,
          onClick = {
            onCloseDrawer()
            onSelectTab(MainTab.CARTEIRA_PERFIL)
          },
          testTag = "menu_item_carteira"
        )

        if (currentUser.role == UserRole.PRESTADOR) {
          MenuDrawerItem(
            icon = Icons.Default.History,
            label = "Histórico de Pedidos",
            subLabel = "Finalizados, cancelados e recusados",
            selected = selectedTab == MainTab.HISTORICO_PEDIDOS,
            onClick = {
              onCloseDrawer()
              onSelectTab(MainTab.HISTORICO_PEDIDOS)
            },
            testTag = "menu_item_history"
          )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Divider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(10.dp))

        // 2B. CATEGORIAS E FILTROS (Minimizados por padrão; expandem ao clicar)
        val drawerCategories = (
          listOf("Serviços", "Em Andamento", "Concluído", "Cancelado") +
            (availableCategories.ifEmpty {
              listOf(
                "Cabeleireiro",
                "Manicure",
                "Eletricista",
                "Pintura",
                "TI, Redes & Informática",
                "Hidráulica",
                "Limpeza",
                "Montagem"
              )
            })
          ).filter { it.isNotBlank() }.distinct()
        val urgencies = listOf("Todas", "Normal", "Urgente", "Agendado")
        val hasCategoryFilter = categoryFilter != "Serviços" && categoryFilter != "Todos"
        val hasUrgencyFilter = urgencyFilter != "Todas"
        val hasActiveFilters = hasCategoryFilter || hasUrgencyFilter

        var categoriesExpanded by remember { mutableStateOf(false) }
        var urgencyExpanded by remember { mutableStateOf(false) }

        // Cabeçalho retrátil: CATEGORIAS E FILTROS
        Surface(
          onClick = { categoriesExpanded = !categoriesExpanded },
          shape = RoundedCornerShape(12.dp),
          color = if (categoriesExpanded || hasCategoryFilter) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
          } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
          },
          border = BorderStroke(
            width = 1.dp,
            color = if (categoriesExpanded || hasCategoryFilter) {
              MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
              MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
          ),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .heightIn(min = 48.dp)
            .testTag("btn_toggle_categories_filters")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = if (categoriesExpanded || hasCategoryFilter) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.menu_categories_and_filters_title),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp
                ),
                color = if (categoriesExpanded || hasCategoryFilter) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (!categoriesExpanded && hasCategoryFilter) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  color = MaterialTheme.colorScheme.primary,
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Text(
                    text = categoryFilter,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                  )
                }
              }
            }
            Icon(
              imageVector = if (categoriesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
              contentDescription = stringResource(
                if (categoriesExpanded) R.string.menu_categories_collapse_cd else R.string.menu_categories_expand_cd
              ),
              tint = if (categoriesExpanded || hasCategoryFilter) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              modifier = Modifier.size(20.dp)
            )
          }
        }

        AnimatedVisibility(
          visible = categoriesExpanded,
          enter = expandVertically() + fadeIn(),
          exit = shrinkVertically() + fadeOut()
        ) {
          FlowRow(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("menu_categories_flow"),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            drawerCategories.forEach { cat ->
              val isSelected = categoryFilter == cat || (cat == "Serviços" && categoryFilter == "Todos")
              val chipTag = when (cat) {
                "Serviços" -> "menu_filter_cat_servicos"
                "Em Andamento" -> "menu_filter_cat_em_andamento"
                "Concluído" -> "menu_filter_cat_concluido"
                "Cancelado" -> "menu_filter_cat_cancelado"
                else -> "menu_filter_cat_${cat.lowercase().replace(" ", "_")}"
              }
              Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                  .heightIn(min = 36.dp)
                  .clickable {
                    onSelectCategory(cat)
                    onSelectTab(MainTab.SERVICOS)
                    onCloseDrawer()
                  }
                  .testTag(chipTag)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = cat,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                      fontSize = 11.sp
                    ),
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Cabeçalho retrátil: URGÊNCIA (minimizado por padrão; expande ao clicar)
        Surface(
          onClick = { urgencyExpanded = !urgencyExpanded },
          shape = RoundedCornerShape(12.dp),
          color = if (urgencyExpanded || hasUrgencyFilter) {
            EmeraldWalletLight.copy(alpha = 0.45f)
          } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
          },
          border = BorderStroke(
            width = 1.dp,
            color = if (urgencyExpanded || hasUrgencyFilter) {
              EmeraldWallet.copy(alpha = 0.4f)
            } else {
              MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
          ),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .heightIn(min = 48.dp)
            .testTag("btn_toggle_urgency_filters")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = if (urgencyExpanded || hasUrgencyFilter) {
                  EmeraldWallet
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.menu_urgency_filter_title),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp
                ),
                color = if (urgencyExpanded || hasUrgencyFilter) {
                  EmeraldWallet
                } else {
                  MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (!urgencyExpanded && hasUrgencyFilter) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  color = EmeraldWallet,
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Text(
                    text = urgencyFilter,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 10.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                  )
                }
              }
            }
            Icon(
              imageVector = if (urgencyExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
              contentDescription = stringResource(
                if (urgencyExpanded) R.string.menu_urgency_collapse_cd else R.string.menu_urgency_expand_cd
              ),
              tint = if (urgencyExpanded || hasUrgencyFilter) {
                EmeraldWallet
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              modifier = Modifier.size(20.dp)
            )
          }
        }

        AnimatedVisibility(
          visible = urgencyExpanded,
          enter = expandVertically() + fadeIn(),
          exit = shrinkVertically() + fadeOut()
        ) {
          FlowRow(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("menu_urgency_filter_flow"),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            urgencies.forEach { urg ->
              val isSelected = urgencyFilter.equals(urg, ignoreCase = true)
              Surface(
                color = if (isSelected) EmeraldWallet else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                  .heightIn(min = 36.dp)
                  .clickable {
                    onSelectUrgency(urg)
                    onSelectTab(MainTab.SERVICOS)
                    onCloseDrawer()
                  }
                  .testTag("menu_filter_urgency_${urg.lowercase()}")
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = urg,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                      fontSize = 11.sp
                    ),
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                  )
                }
              }
            }
          }
        }

        if (hasActiveFilters) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.End
          ) {
            TextButton(
              onClick = {
                onResetFilters()
                onSelectTab(MainTab.SERVICOS)
                onCloseDrawer()
              },
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("btn_menu_clear_filters")
            ) {
              Text(
                text = stringResource(R.string.menu_clear_filters_cta),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Divider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(10.dp))

        // 3. UTILITIES & SUPPORT SECTION
        Text(
          text = "CENTRAL & AJUDA",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )

        // Notificações
        MenuDrawerItem(
          icon = Icons.Default.Notifications,
          label = "Notificações",
          badgeCount = unreadNotificationsCount,
          onClick = {
            onCloseDrawer()
            onNotificationsClick()
          },
          testTag = "menu_item_notifications"
        )

        // Sugestões e Dicas do App
        MenuDrawerItem(
          icon = Icons.Default.Lightbulb,
          label = "Sugestões e Dicas",
          subLabel = "Dicas de uso e envio de ideias",
          selected = selectedTab == MainTab.SUGESTOES_DICAS,
          onClick = {
            onCloseDrawer()
            onSelectTab(MainTab.SUGESTOES_DICAS)
          },
          testTag = "menu_item_suggestions_tips"
        )

        // Introdução do App (3 passos)
        MenuDrawerItem(
          icon = Icons.Default.Info,
          label = "Como Funciona (Introdução)",
          subLabel = "Propostas, contrapropostas e chat",
          onClick = {
            onCloseDrawer()
            onOpenOnboarding()
          },
          testTag = "menu_item_onboarding"
        )

        // Falar com Suporte
        MenuDrawerItem(
          icon = Icons.Default.SupportAgent,
          label = "Falar com Suporte",
          subLabel = "Atendimento 24/7 ResolveAi",
          onClick = {
            onCloseDrawer()
            onSupportClick()
          },
          testTag = "menu_item_support"
        )

        // Central Exclusiva do Administrador (Análise de Documentos no Firestore)
        if (currentUser.role == UserRole.ADMIN) {
          MenuDrawerItem(
            icon = Icons.Default.AdminPanelSettings,
            label = "Análise de Documentos (Admin)",
            subLabel = "CNH/RG e Antecedentes • Firestore",
            badgeCount = pendingAdminReviewsCount,
            selected = selectedTab == MainTab.ADMIN_DOCUMENTOS,
            onClick = {
              onCloseDrawer()
              onSelectTab(MainTab.ADMIN_DOCUMENTOS)
            },
            testTag = "menu_item_admin"
          )

          // Gestão de Todos os Usuários (Editar e Excluir)
          MenuDrawerItem(
            icon = Icons.Default.Group,
            label = "Gerenciar Usuários (Admin)",
            subLabel = "Editar e excluir clientes, prestadores e admins",
            selected = selectedTab == MainTab.ADMIN_USUARIOS,
            onClick = {
              onCloseDrawer()
              onSelectTab(MainTab.ADMIN_USUARIOS)
            },
            testTag = "menu_item_admin_users"
          )

          // Categorias de Serviços e Outras Edições (Admin)
          MenuDrawerItem(
            icon = Icons.Default.Tune,
            label = "Categorias e Edições (Admin)",
            subLabel = "Categorias de serviços, avisos, dicas e pedidos",
            selected = selectedTab == MainTab.ADMIN_CATALOG_EDICOES,
            onClick = {
              onCloseDrawer()
              onSelectTab(MainTab.ADMIN_CATALOG_EDICOES)
            },
            testTag = "menu_item_admin_catalog_edits"
          )
        }

        if (currentUser.role == UserRole.ADMIN) {
          // Alternar Usuário / Modo
          MenuDrawerItem(
            icon = Icons.Default.SwapHoriz,
            label = "Alternar Modo / Usuário",
            subLabel = "Cliente • Prestador • Admin",
            onClick = {
              onCloseDrawer()
              onSwitchUserClick()
            },
            testTag = "menu_item_switch_user"
          )

          // Cadastrar Novo Usuário
          MenuDrawerItem(
            icon = Icons.Default.PersonAdd,
            label = "Cadastrar Novo Usuário",
            subLabel = "Cadastro direto no painel admin",
            onClick = {
              onCloseDrawer()
              onRegisterClick()
            },
            testTag = "menu_item_register_user"
          )
        }

        // Alterar Senha (disponível para todos os usuários)
        MenuDrawerItem(
          icon = Icons.Default.LockReset,
          label = stringResource(R.string.change_password_cta),
          subLabel = stringResource(R.string.change_password_menu_subtitle),
          onClick = {
            onCloseDrawer()
            onChangePasswordClick()
          },
          testTag = "menu_item_change_password"
        )

        // Sair da Conta (Retorna à tela inicial de Login)
        MenuDrawerItem(
          icon = Icons.AutoMirrored.Filled.ExitToApp,
          label = "Sair da Conta",
          subLabel = "Encerrar sessão atual",
          onClick = {
            onCloseDrawer()
            onLogoutClick()
          },
          testTag = "menu_item_logout"
        )
      }

      Spacer(modifier = Modifier.weight(1f, fill = false))
      Spacer(modifier = Modifier.height(16.dp))

      // 4. FOOTER WITH LGPD NOTICE
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldWallet, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Proteção de Dados & LGPD",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = EmeraldWallet
            )
          }
          Text(
            text = "ResolveAi Serviços v1.0 • Contatos bloqueados no chat para segurança de ambas as partes.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
private fun MenuDrawerItem(
  icon: ImageVector,
  label: String,
  subLabel: String? = null,
  badgeCount: Int = 0,
  selected: Boolean = false,
  onClick: () -> Unit,
  testTag: String
) {
  NavigationDrawerItem(
    icon = {
      BadgedBox(
        badge = {
          if (badgeCount > 0) {
            Badge(containerColor = DangerRed) {
              Text("$badgeCount")
            }
          }
        }
      ) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    label = {
      Column {
        Text(
          text = label,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
          ),
          color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (!subLabel.isNullOrBlank()) {
          Text(
            text = subLabel,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    },
    selected = selected,
    onClick = onClick,
    colors = NavigationDrawerItemDefaults.colors(
      selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
      unselectedContainerColor = Color.Transparent
    ),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier
      .padding(vertical = 3.dp)
      .testTag(testTag)
  )
}
