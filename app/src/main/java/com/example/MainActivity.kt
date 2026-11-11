package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ProviderVerificationStatus
import com.example.model.UserRole
import com.example.ui.MainTab
import com.example.ui.MarketplaceViewModel
import com.example.ui.components.AdminReviewDialog
import com.example.ui.components.AppTopBar
import com.example.ui.components.CreateServiceDialog
import com.example.ui.components.EditServiceBudgetDialog
import com.example.ui.components.InsufficientBalanceDialog
import com.example.ui.components.NotificationsDialog
import com.example.ui.components.ProviderPendingApprovalScreen
import com.example.ui.components.RechargeWalletDialog
import com.example.ui.components.RegisterUserDialog
import com.example.ui.components.ServiceRatingDialog
import com.example.ui.components.SmsVerificationDialog
import com.example.ui.components.UserSwitcherDialog
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ProposalsScreen
import com.example.ui.screens.ServicesScreen
import com.example.ui.screens.WalletProfileScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: MarketplaceViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MainApp(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun MainApp(viewModel: MarketplaceViewModel) {
  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
  val users by viewModel.users.collectAsStateWithLifecycle()
  val services by viewModel.services.collectAsStateWithLifecycle()
  val proposals by viewModel.proposals.collectAsStateWithLifecycle()
  val walletTransactions by viewModel.walletTransactions.collectAsStateWithLifecycle()
  val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
  val notifications by viewModel.notifications.collectAsStateWithLifecycle()

  val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
  val selectedServiceId by viewModel.selectedServiceId.collectAsStateWithLifecycle()
  val selectedChatServiceId by viewModel.selectedChatServiceId.collectAsStateWithLifecycle()

  val showCreateService by viewModel.showCreateServiceDialog.collectAsStateWithLifecycle()
  val showRecharge by viewModel.showRechargeDialog.collectAsStateWithLifecycle()
  val showUserSwitcher by viewModel.showUserSwitcherDialog.collectAsStateWithLifecycle()
  val showRegister by viewModel.showRegisterDialog.collectAsStateWithLifecycle()
  val showSms by viewModel.showSmsDialog.collectAsStateWithLifecycle()
  val pendingRegistration by viewModel.pendingRegistration.collectAsStateWithLifecycle()
  val smsNotificationBanner by viewModel.smsNotificationBanner.collectAsStateWithLifecycle()
  val ratingServiceTarget by viewModel.ratingServiceTarget.collectAsStateWithLifecycle()
  val editingBudgetService by viewModel.editingBudgetService.collectAsStateWithLifecycle()
  val showNotifications by viewModel.showNotificationsDialog.collectAsStateWithLifecycle()
  val showAdminReviewDialog by viewModel.showAdminReviewDialog.collectAsStateWithLifecycle()
  val insufficientBalanceData by viewModel.insufficientBalanceData.collectAsStateWithLifecycle()
  val snackBarMessage by viewModel.snackBarMessage.collectAsStateWithLifecycle()

  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val categoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }

  val unreadNotificationsCount = notifications.count {
    !it.isRead && (it.recipientId == "all_providers" || it.recipientId == currentUser.id)
  }

  LaunchedEffect(snackBarMessage) {
    snackBarMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.dismissSnackbar()
    }
  }

  // Pending approval gate: Provider with unapproved documents cannot access opportunities/wallet until approved
  val isProviderPendingApproval = currentUser.role == UserRole.PRESTADOR &&
    currentUser.verificationStatus == ProviderVerificationStatus.PENDENTE_ANALISE

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      AppTopBar(
        currentUser = currentUser,
        unreadNotificationCount = unreadNotificationsCount,
        onSwitchUserClick = { viewModel.setShowUserSwitcher(true) },
        onOpenWalletClick = { viewModel.setTab(MainTab.CARTEIRA_PERFIL) },
        onNotificationsClick = { viewModel.setShowNotifications(true) },
        onAdminClick = { viewModel.setShowAdminReviewDialog(true) }
      )
    },
    bottomBar = {
      if (!isProviderPendingApproval) {
        NavigationBar(
          tonalElevation = 8.dp,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("bottom_nav_bar")
        ) {
          // Tab 1: Role-isolated primary list
          val tab1Label = when (currentUser.role) {
            UserRole.CLIENTE -> "Meus Pedidos"
            UserRole.PRESTADOR -> "Oportunidades"
            UserRole.ADMIN -> "Serviços"
          }
          NavigationBarItem(
            selected = selectedTab == MainTab.SERVICOS,
            onClick = {
              viewModel.selectService(null)
              viewModel.setTab(MainTab.SERVICOS)
            },
            icon = {
              Icon(
                if (currentUser.role == UserRole.PRESTADOR) Icons.Default.Build else Icons.Default.Work,
                contentDescription = tab1Label
              )
            },
            label = { Text(tab1Label) },
            modifier = Modifier.testTag("nav_tab_servicos")
          )

          // Tab 2: Negociações & Contrapropostas
          val pendingProposalsCount = proposals.count { prop ->
            val srv = services.find { it.id == prop.serviceId }
            if (currentUser.role == UserRole.CLIENTE) {
              srv?.clientId == currentUser.id
            } else {
              prop.providerId == currentUser.id
            }
          }
          val tab2Label = when (currentUser.role) {
            UserRole.CLIENTE -> "Propostas"
            UserRole.PRESTADOR -> "Negociações"
            UserRole.ADMIN -> "Propostas"
          }

          NavigationBarItem(
            selected = selectedTab == MainTab.NEGOCIACOES,
            onClick = { viewModel.setTab(MainTab.NEGOCIACOES) },
            icon = {
              BadgedBox(
                badge = {
                  if (pendingProposalsCount > 0) {
                    Badge { Text("$pendingProposalsCount") }
                  }
                }
              ) {
                Icon(Icons.Default.Handshake, contentDescription = tab2Label)
              }
            },
            label = { Text(tab2Label) },
            modifier = Modifier.testTag("nav_tab_negociacoes")
          )

          // Tab 3: Chat Privado Seguro
          NavigationBarItem(
            selected = selectedTab == MainTab.CHAT,
            onClick = { viewModel.setTab(MainTab.CHAT) },
            icon = {
              Icon(Icons.Default.Chat, contentDescription = "Chat Privado")
            },
            label = { Text("Chat Seguro") },
            modifier = Modifier.testTag("nav_tab_chat")
          )

          // Tab 4: Carteira (Prestador) vs Perfil (Cliente) vs Admin
          val tab4Label = when (currentUser.role) {
            UserRole.CLIENTE -> "Meu Perfil"
            UserRole.PRESTADOR -> "Carteira & Reputação"
            UserRole.ADMIN -> "Gestão Admin"
          }
          NavigationBarItem(
            selected = selectedTab == MainTab.CARTEIRA_PERFIL,
            onClick = { viewModel.setTab(MainTab.CARTEIRA_PERFIL) },
            icon = {
              Icon(
                if (currentUser.role == UserRole.PRESTADOR) Icons.Default.AccountBalanceWallet else Icons.Default.Person,
                contentDescription = tab4Label
              )
            },
            label = { Text(tab4Label) },
            modifier = Modifier.testTag("nav_tab_carteira_perfil")
          )
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      if (isProviderPendingApproval) {
        ProviderPendingApprovalScreen(
          currentUser = currentUser,
          onSwitchUserClick = { viewModel.setShowUserSwitcher(true) },
          onFastApproveClick = { viewModel.approveProvider(currentUser.id) }
        )
      } else {
        Crossfade(targetState = selectedTab, label = "tab_crossfade") { tab ->
          when (tab) {
            MainTab.SERVICOS -> {
              ServicesScreen(
                currentUser = currentUser,
                services = services,
                proposals = proposals,
                selectedServiceId = selectedServiceId,
                searchQuery = searchQuery,
                categoryFilter = categoryFilter,
                onSearchChange = { viewModel.setSearchQuery(it) },
                onCategoryChange = { viewModel.setCategoryFilter(it) },
                onSelectService = { viewModel.selectService(it) },
                onCreateServiceClick = { viewModel.setShowCreateService(true) },
                onSubmitProposal = { sId, price, days, desc ->
                  viewModel.submitProposal(sId, price, days, desc)
                },
                onOpenChat = { serviceId ->
                  viewModel.openChatForService(serviceId)
                },
                onUpdateStatus = { sId, status ->
                  viewModel.updateServiceStatus(sId, status)
                },
                onRateService = { sId, rating, comment ->
                  val target = services.find { it.id == sId }
                  if (target != null) viewModel.openRatingDialog(target)
                },
                onEditBudget = { srv ->
                  viewModel.openEditBudgetDialog(srv)
                },
                onAcceptProposal = { proposalId ->
                  viewModel.acceptProposal(proposalId)
                }
              )
            }

            MainTab.NEGOCIACOES -> {
              ProposalsScreen(
                currentUser = currentUser,
                proposals = proposals,
                services = services,
                onAcceptProposal = { proposalId ->
                  viewModel.acceptProposal(proposalId)
                },
                onSendCounterOffer = { proposalId, newPrice, note ->
                  viewModel.sendCounterOffer(proposalId, newPrice, note)
                },
                onOpenChat = { serviceId ->
                  viewModel.openChatForService(serviceId)
                }
              )
            }

            MainTab.CHAT -> {
              ChatScreen(
                currentUser = currentUser,
                services = services,
                selectedServiceId = selectedChatServiceId,
                chatMessages = chatMessages,
                onSelectService = { srvId ->
                  viewModel.openChatForService(srvId)
                },
                onSendMessage = { srvId, text ->
                  viewModel.sendChatMessage(srvId, text)
                }
              )
            }

            MainTab.CARTEIRA_PERFIL -> {
              WalletProfileScreen(
                currentUser = currentUser,
                walletTransactions = walletTransactions,
                onRechargeClick = { viewModel.setShowRecharge(true) },
                onSwitchUserClick = { viewModel.setShowUserSwitcher(true) },
                onEditProfileClick = { viewModel.setShowEditProfile(true) },
                onSaveProfile = { updated ->
                  viewModel.updateProfile(updated)
                }
              )
            }
          }
        }
      }
    }
  }

  // Dialogs
  if (editingBudgetService != null) {
    EditServiceBudgetDialog(
      service = editingBudgetService!!,
      onDismiss = { viewModel.dismissEditBudgetDialog() },
      onConfirm = { newBudget ->
        viewModel.updateServiceBudget(editingBudgetService!!.id, newBudget)
      }
    )
  }

  if (showNotifications) {
    NotificationsDialog(
      notifications = notifications,
      onDismiss = { viewModel.setShowNotifications(false) },
      onMarkRead = { viewModel.markNotificationAsRead(it) },
      onOpenService = { sId ->
        viewModel.selectService(sId)
        viewModel.setTab(MainTab.SERVICOS)
      }
    )
  }

  if (showAdminReviewDialog) {
    AdminReviewDialog(
      users = users,
      onDismiss = { viewModel.setShowAdminReviewDialog(false) },
      onApprove = { providerId ->
        viewModel.approveProvider(providerId)
      },
      onReject = { providerId, reason ->
        viewModel.rejectProvider(providerId, reason)
      }
    )
  }

  if (showCreateService) {
    CreateServiceDialog(
      onDismiss = { viewModel.setShowCreateService(false) },
      onCreate = { title, category, desc, loc, budget, urgency ->
        viewModel.createService(title, category, desc, loc, budget, urgency)
      }
    )
  }

  if (showRecharge) {
    RechargeWalletDialog(
      currentBalance = currentUser.walletBalance,
      onDismiss = { viewModel.setShowRecharge(false) },
      onConfirmRecharge = { amount ->
        viewModel.rechargeWallet(amount)
      }
    )
  }

  if (showUserSwitcher) {
    UserSwitcherDialog(
      users = users,
      currentUserId = currentUser.id,
      onDismiss = { viewModel.setShowUserSwitcher(false) },
      onSelectUser = { userId ->
        viewModel.switchUser(userId)
      },
      onOpenRegisterClick = {
        viewModel.setShowRegister(true)
      }
    )
  }

  if (showRegister) {
    RegisterUserDialog(
      onDismiss = { viewModel.setShowRegister(false) },
      onProceedToSms = { registration ->
        viewModel.startSmsRegistration(registration)
      }
    )
  }

  if (showSms && pendingRegistration != null) {
    SmsVerificationDialog(
      phone = pendingRegistration!!.phone,
      smsNotificationBanner = smsNotificationBanner,
      onDismiss = { viewModel.setShowSmsDialog(false) },
      onConfirmCode = { code ->
        viewModel.confirmSmsCode(code)
      },
      onResendCode = {
        viewModel.resendSmsCode()
      }
    )
  }

  if (ratingServiceTarget != null) {
    ServiceRatingDialog(
      service = ratingServiceTarget!!,
      onDismiss = { viewModel.dismissRatingDialog() },
      onSubmit = { rating, comment, tags ->
        viewModel.submitServiceRating(ratingServiceTarget!!.id, rating, comment, tags)
      }
    )
  }

  if (insufficientBalanceData != null) {
    InsufficientBalanceDialog(
      data = insufficientBalanceData!!,
      onDismiss = { viewModel.dismissInsufficientBalance() },
      onRechargeClick = {
        viewModel.dismissInsufficientBalance()
        viewModel.setShowRecharge(true)
      }
    )
  }
}
