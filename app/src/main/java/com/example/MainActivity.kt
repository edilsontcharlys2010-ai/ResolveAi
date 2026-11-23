package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppNotificationHelper
import com.example.data.CurrentCityLocationHelper
import com.example.data.CurrentCityResult
import com.example.data.DocumentAttachmentHelper
import com.example.data.GoogleAccountInfo
import com.example.data.GoogleAuthManager
import com.example.data.GoogleAuthManager.toGoogleAccountInfo
import com.example.data.MarketplaceRepository
import com.example.data.ProviderLocationTracker
import com.example.data.SupportLauncher
import com.example.data.configureAppCheck
import com.example.model.NotificationType
import com.example.model.ProviderVerificationStatus
import com.example.model.ServiceStatus
import com.example.model.UserRole
import com.example.ui.AuthFlowStep
import com.example.ui.AuthMode
import com.example.ui.MainTab
import com.example.ui.MarketplaceViewModel
import com.example.ui.components.AdaptiveAppNavigationBar
import com.example.ui.components.AdaptiveAppNavigationRail
import com.example.ui.components.AdminReviewDialog
import com.example.ui.components.AppMenuDrawerContent
import com.example.ui.components.ChangeOwnPasswordDialog
import com.example.ui.components.resolveAdaptiveSpec
import com.example.ui.components.CreateServiceDialog
import com.example.ui.components.EditServiceBudgetDialog
import com.example.ui.components.InsufficientBalanceDialog
import com.example.ui.components.NotificationsDialog
import com.example.ui.components.ProviderPendingApprovalScreen
import com.example.ui.components.RechargeWalletDialog
import com.example.ui.components.RegisterUserDialog
import com.example.ui.components.ResolveAiOnboardingComponent
import com.example.ui.components.ServiceRatingDialog
import com.example.ui.components.SupportContactDialog
import com.example.ui.components.UserSwitcherDialog
import com.example.ui.screens.AdminCatalogAndEditsScreen
import com.example.ui.screens.AdminReviewScreen
import com.example.ui.screens.AdminUsersScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ProposalsScreen
import com.example.ui.screens.ProviderOrderHistoryScreen
import com.example.ui.screens.ServicesScreen
import com.example.ui.screens.SuggestionsTipsScreen
import com.example.ui.screens.WalletProfileScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private val viewModel: MarketplaceViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    GoogleAuthManager.ensureFirebaseAuthInitialized(this)
    AppNotificationHelper.ensureChannels(this)
    MarketplaceRepository.instance.attachContext(this)
    restorePendingAuthStateIfNeeded(savedInstanceState)
    handleNotificationIntent(intent)
    enableEdgeToEdge()
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    setContent {
      MyApplicationTheme {
        MainApp(viewModel = viewModel)
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(KEY_AUTH_MODE, viewModel.authMode.value.name)
    outState.putString(KEY_AUTH_STEP, viewModel.authStep.value.name)
    outState.putString(KEY_PREFERRED_ROLE, viewModel.pendingPreferredRole.value.name)
    outState.putString(KEY_SOCIAL_PROVIDER_LABEL, viewModel.pendingSocialProviderLabel.value)
    outState.putString(KEY_PENDING_LOGIN_PHONE, viewModel.pendingLoginPhone.value)
    viewModel.pendingGoogleAccount.value?.let { account ->
      outState.putString(KEY_GOOGLE_UID, account.uid)
      outState.putString(KEY_GOOGLE_EMAIL, account.email)
      outState.putString(KEY_GOOGLE_NAME, account.displayName)
      outState.putString(KEY_GOOGLE_PHOTO, account.photoUrl)
    }
  }

  private fun restorePendingAuthStateIfNeeded(savedInstanceState: Bundle?) {
    if (viewModel.isAuthenticated.value) return

    if (savedInstanceState != null) {
      val mode = savedInstanceState.getString(KEY_AUTH_MODE)?.let {
        runCatching { AuthMode.valueOf(it) }.getOrNull()
      }
      val step = savedInstanceState.getString(KEY_AUTH_STEP)?.let {
        runCatching { AuthFlowStep.valueOf(it) }.getOrNull()
      }
      val role = savedInstanceState.getString(KEY_PREFERRED_ROLE)?.let {
        runCatching { UserRole.valueOf(it) }.getOrNull()
      } ?: UserRole.CLIENTE
      val googleUid = savedInstanceState.getString(KEY_GOOGLE_UID)
      val googleEmail = savedInstanceState.getString(KEY_GOOGLE_EMAIL)
      val googleAccount = if (!googleUid.isNullOrBlank() && !googleEmail.isNullOrBlank()) {
        GoogleAccountInfo(
          uid = googleUid,
          email = googleEmail,
          displayName = savedInstanceState.getString(KEY_GOOGLE_NAME).orEmpty(),
          photoUrl = savedInstanceState.getString(KEY_GOOGLE_PHOTO)
        )
      } else {
        null
      }
      if (mode != null || step != null) {
        viewModel.restoreAuthNavigationState(
          mode = mode ?: AuthMode.LOGIN,
          step = step ?: AuthFlowStep.FORM,
          pendingGoogleAccount = googleAccount,
          socialProviderLabel = savedInstanceState.getString(KEY_SOCIAL_PROVIDER_LABEL) ?: "Google",
          preferredRole = role,
          pendingLoginPhone = savedInstanceState.getString(KEY_PENDING_LOGIN_PHONE)
        )
        return
      }
    }

    val pendingReturn = DocumentAttachmentHelper.peekPendingAuthReturn(this) ?: return
    val mode = runCatching { AuthMode.valueOf(pendingReturn.modeName) }.getOrDefault(AuthMode.REGISTER)
    val step = runCatching { AuthFlowStep.valueOf(pendingReturn.stepName) }.getOrDefault(AuthFlowStep.FORM)
    val role = pendingReturn.roleName?.let {
      runCatching { UserRole.valueOf(it) }.getOrNull()
    } ?: UserRole.CLIENTE
    val googleAccount = if (!pendingReturn.googleUid.isNullOrBlank() && !pendingReturn.googleEmail.isNullOrBlank()) {
      GoogleAccountInfo(
        uid = pendingReturn.googleUid,
        email = pendingReturn.googleEmail,
        displayName = pendingReturn.googleName.orEmpty(),
        photoUrl = pendingReturn.googlePhoto
      )
    } else {
      null
    }
    viewModel.restoreAuthNavigationState(
      mode = mode,
      step = step,
      pendingGoogleAccount = googleAccount,
      socialProviderLabel = pendingReturn.socialProviderLabel ?: "Google",
      preferredRole = role,
      pendingLoginPhone = pendingReturn.pendingPhone
    )
  }

  companion object {
    private const val KEY_AUTH_MODE = "saved_auth_mode"
    private const val KEY_AUTH_STEP = "saved_auth_step"
    private const val KEY_PREFERRED_ROLE = "saved_preferred_role"
    private const val KEY_SOCIAL_PROVIDER_LABEL = "saved_social_provider_label"
    private const val KEY_PENDING_LOGIN_PHONE = "saved_pending_login_phone"
    private const val KEY_GOOGLE_UID = "saved_google_uid"
    private const val KEY_GOOGLE_EMAIL = "saved_google_email"
    private const val KEY_GOOGLE_NAME = "saved_google_name"
    private const val KEY_GOOGLE_PHOTO = "saved_google_photo"
  }

  override fun onResume() {
    super.onResume()
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleNotificationIntent(intent)
  }

  private fun handleNotificationIntent(intent: Intent?) {
    if (intent == null) return
    val notifId = intent.getStringExtra(AppNotificationHelper.EXTRA_NOTIFICATION_ID)
    if (!notifId.isNullOrBlank()) {
      viewModel.markNotificationAsRead(notifId)
    }
    val serviceId = intent.getStringExtra(AppNotificationHelper.EXTRA_SERVICE_ID)
    val typeName = intent.getStringExtra(AppNotificationHelper.EXTRA_NOTIFICATION_TYPE)
    if (!serviceId.isNullOrBlank()) {
      val notifType = typeName?.let {
        try { NotificationType.valueOf(it) } catch (_: Exception) { null }
      }
      if (notifType == NotificationType.NOVA_MENSAGEM || notifType == NotificationType.PROPOSTA_ACEITA) {
        viewModel.openChatForService(serviceId)
      } else {
        viewModel.selectService(serviceId)
        viewModel.setTab(MainTab.SERVICOS)
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainApp(viewModel: MarketplaceViewModel) {
  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
  val users by viewModel.users.collectAsStateWithLifecycle()
  val services by viewModel.services.collectAsStateWithLifecycle()
  val proposals by viewModel.proposals.collectAsStateWithLifecycle()
  val walletTransactions by viewModel.walletTransactions.collectAsStateWithLifecycle()
  val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
  val notifications by viewModel.notifications.collectAsStateWithLifecycle()
  val reviews by viewModel.reviews.collectAsStateWithLifecycle()

  val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
  val selectedServiceId by viewModel.selectedServiceId.collectAsStateWithLifecycle()
  val selectedChatServiceId by viewModel.selectedChatServiceId.collectAsStateWithLifecycle()
  val profileFocusNonce by viewModel.profileFocusNonce.collectAsStateWithLifecycle()

  val showCreateService by viewModel.showCreateServiceDialog.collectAsStateWithLifecycle()
  val showRecharge by viewModel.showRechargeDialog.collectAsStateWithLifecycle()
  val showUserSwitcher by viewModel.showUserSwitcherDialog.collectAsStateWithLifecycle()
  val showRegister by viewModel.showRegisterDialog.collectAsStateWithLifecycle()
  val ratingServiceTarget by viewModel.ratingServiceTarget.collectAsStateWithLifecycle()
  val ratingInitialStars by viewModel.ratingInitialStars.collectAsStateWithLifecycle()
  val editingBudgetService by viewModel.editingBudgetService.collectAsStateWithLifecycle()
  val showNotifications by viewModel.showNotificationsDialog.collectAsStateWithLifecycle()
  val showAdminReviewDialog by viewModel.showAdminReviewDialog.collectAsStateWithLifecycle()
  val showChangePasswordDialog by viewModel.showChangePasswordDialog.collectAsStateWithLifecycle()
  val changePasswordState by viewModel.changePasswordState.collectAsStateWithLifecycle()
  val insufficientBalanceData by viewModel.insufficientBalanceData.collectAsStateWithLifecycle()
  val snackBarMessage by viewModel.snackBarMessage.collectAsStateWithLifecycle()

  val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
  val authMode by viewModel.authMode.collectAsStateWithLifecycle()
  val authStep by viewModel.authStep.collectAsStateWithLifecycle()
  val pendingLoginPhone by viewModel.pendingLoginPhone.collectAsStateWithLifecycle()
  val pendingGoogleAccount by viewModel.pendingGoogleAccount.collectAsStateWithLifecycle()
  val pendingSocialProviderLabel by viewModel.pendingSocialProviderLabel.collectAsStateWithLifecycle()
  val pendingPreferredRole by viewModel.pendingPreferredRole.collectAsStateWithLifecycle()
  val pendingGoogleDraft by viewModel.pendingGoogleDraft.collectAsStateWithLifecycle()
  val authErrorMessage by viewModel.authErrorMessage.collectAsStateWithLifecycle()
  val authSuccessMessage by viewModel.authSuccessMessage.collectAsStateWithLifecycle()
  val isGoogleAuthLoading by viewModel.isGoogleAuthLoading.collectAsStateWithLifecycle()
  val isCredentialAuthLoading by viewModel.isCredentialAuthLoading.collectAsStateWithLifecycle()
  val passwordResetState by viewModel.passwordResetState.collectAsStateWithLifecycle()
  val pixRechargeState by viewModel.pixRechargeState.collectAsStateWithLifecycle()
  val pixRechargeError by viewModel.pixRechargeError.collectAsStateWithLifecycle()
  val isWalletHistoryLoading by viewModel.isWalletHistoryLoading.collectAsStateWithLifecycle()
  val mercadoPagoCharges by viewModel.mercadoPagoCharges.collectAsStateWithLifecycle()
  val isMpPollingActive by viewModel.isMercadoPagoPollingActive.collectAsStateWithLifecycle()
  val isRefreshingMpTransactions by viewModel.isRefreshingMpTransactions.collectAsStateWithLifecycle()

  val documentReviewAudits by viewModel.documentReviewAudits.collectAsStateWithLifecycle()
  val isAdminReviewLoading by viewModel.isAdminReviewLoading.collectAsStateWithLifecycle()
  val adminSyncStatusMessage by viewModel.adminSyncStatusMessage.collectAsStateWithLifecycle()
  val isAdminUsersLoading by viewModel.isAdminUsersLoading.collectAsStateWithLifecycle()
  val adminUsersSyncMessage by viewModel.adminUsersSyncMessage.collectAsStateWithLifecycle()
  val adminUserPasswords by viewModel.adminUserPasswords.collectAsStateWithLifecycle()
  val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()
  val chatDiagnosticLogs by viewModel.chatDiagnosticLogs.collectAsStateWithLifecycle()

  val appSuggestions by viewModel.appSuggestions.collectAsStateWithLifecycle()
  val appTips by viewModel.appTipsFlow.collectAsStateWithLifecycle()
  val serviceCategories by viewModel.serviceCategories.collectAsStateWithLifecycle()
  val appAdminSettings by viewModel.appAdminSettings.collectAsStateWithLifecycle()
  val serviceLocationHistory by viewModel.serviceLocationHistory.collectAsStateWithLifecycle()
  val declinedServiceIdsByProvider by viewModel.declinedServiceIdsByProvider.collectAsStateWithLifecycle()
  val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsStateWithLifecycle()
  val showOnboardingReplay by viewModel.showOnboardingReplay.collectAsStateWithLifecycle()
  val detectedLocalCity by viewModel.detectedLocalCity.collectAsStateWithLifecycle()

  val activeServiceCategoryNames = remember(serviceCategories) {
    serviceCategories.filter { it.isActive }.sortedBy { it.orderIndex }.map { it.name }.distinct()
  }
  val activeProviderSpecialties = remember(serviceCategories) {
    serviceCategories.filter { it.isActive }.sortedBy { it.orderIndex }.map { it.effectiveSpecialtyLabel }.distinct()
  }

  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val categoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
  val urgencyFilter by viewModel.urgencyFilter.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  var showSupportDialog by remember { mutableStateOf(false) }
  var hasCheckedAutoSignIn by rememberSaveable { mutableStateOf(false) }
  var hasRequestedNotifPermission by remember { mutableStateOf(false) }
  var hasLocationPermission by remember { mutableStateOf(ProviderLocationTracker.hasLocationPermission(context)) }
  var hasRequestedLocationPermission by remember { mutableStateOf(false) }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      AppNotificationHelper.ensureChannels(context)
    }
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { grants ->
    hasLocationPermission = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
      ProviderLocationTracker.hasLocationPermission(context)
  }

  // Pré-carrega e atualiza em segundo plano a cidade local real do aparelho (quando a permissão já estiver ativa)
  // para que a média regional em "Novo Pedido" já abra na cidade local do usuário em vez de São Paulo.
  LaunchedEffect(isAuthenticated, hasLocationPermission) {
    val cachedCity = CurrentCityLocationHelper.getCachedLocalCity(context)
    if (!cachedCity.isNullOrBlank()) {
      viewModel.updateDetectedLocalCity(cachedCity)
    }
    if (isAuthenticated && CurrentCityLocationHelper.hasLocationPermission(context)) {
      val result = CurrentCityLocationHelper.detectCurrentCity(context)
      if (result is CurrentCityResult.Success && result.cityFormatted.isNotBlank()) {
        viewModel.updateDetectedLocalCity(result.cityFormatted)
      }
    }
  }

  val activeTrackedServiceIds = remember(isAuthenticated, currentUser.id, currentUser.role, services) {
    if (!isAuthenticated || currentUser.role != UserRole.PRESTADOR) {
      emptyList()
    } else {
      services.filter {
        currentUser.ownsId(it.acceptedProviderId) &&
          (it.status == ServiceStatus.ACEITO || it.status == ServiceStatus.EM_EXECUCAO)
      }.map { it.id }
    }
  }

  LaunchedEffect(isAuthenticated, currentUser.id, activeTrackedServiceIds, hasLocationPermission) {
    if (isAuthenticated && activeTrackedServiceIds.isNotEmpty()) {
      if (!ProviderLocationTracker.hasLocationPermission(context)) {
        if (!hasRequestedLocationPermission) {
          hasRequestedLocationPermission = true
          locationPermissionLauncher.launch(
            arrayOf(
              Manifest.permission.ACCESS_FINE_LOCATION,
              Manifest.permission.ACCESS_COARSE_LOCATION
            )
          )
        }
      } else {
        hasLocationPermission = true
        ProviderLocationTracker.captureCurrentLocation(context)?.let { initialPoint ->
          activeTrackedServiceIds.forEach { sId ->
            viewModel.recordProviderLocationPoint(
              serviceId = sId,
              latitude = initialPoint.latitude,
              longitude = initialPoint.longitude,
              accuracyMeters = initialPoint.accuracyMeters,
              speedMps = initialPoint.speedMps,
              source = "GPS_INICIAL"
            )
          }
        }
        ProviderLocationTracker.observeLocationUpdates(context).collect { sample ->
          activeTrackedServiceIds.forEach { sId ->
            viewModel.recordProviderLocationPoint(
              serviceId = sId,
              latitude = sample.latitude,
              longitude = sample.longitude,
              accuracyMeters = sample.accuracyMeters,
              speedMps = sample.speedMps,
              source = "GPS_AO_VIVO"
            )
          }
        }
      }
    } else {
      hasRequestedLocationPermission = false
    }
  }

  LaunchedEffect(isAuthenticated) {
    if (isAuthenticated && !hasRequestedNotifPermission) {
      hasRequestedNotifPermission = true
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !AppNotificationHelper.hasNotificationPermission(context)
      ) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  val firebaseAuth = remember { GoogleAuthManager.ensureFirebaseAuthInitialized(context) }
  val firebaseUser by remember(firebaseAuth) {
    GoogleAuthManager.authStateFlow(firebaseAuth)
  }.collectAsStateWithLifecycle(initialValue = firebaseAuth?.currentUser)

  LaunchedEffect(firebaseUser?.uid) {
    val fbUser = firebaseUser
    if (fbUser != null) {
      viewModel.syncFirebaseAuthSession(fbUser.toGoogleAccountInfo())
    }
  }

  LaunchedEffect(Unit) {
    viewModel.initFirestore(context.applicationContext)
    if (!hasCheckedAutoSignIn && !isAuthenticated &&
      authMode == AuthMode.LOGIN && authStep == AuthFlowStep.FORM
    ) {
      hasCheckedAutoSignIn = true
      GoogleAuthManager.attemptAutoSignIn(
        context = context,
        scope = scope,
        onAuthSuccess = { account ->
          viewModel.handleGoogleAuthSuccess(account = account)
        },
        onUnauthenticated = {
          // Mantém o usuário na AuthScreen para login com Google, E-mail ou Telefone
        }
      )
    }
  }

  val pendingProposalsCount = proposals.count { prop ->
    val srv = services.find { it.id == prop.serviceId }
    if (currentUser.role == UserRole.CLIENTE) {
      srv != null && currentUser.ownsId(srv.clientId)
    } else {
      currentUser.ownsId(prop.providerId)
    }
  }

  val unreadNotificationsCount = notifications.count {
    !it.isRead && if (currentUser.role == UserRole.PRESTADOR) {
      it.recipientId == "all_providers" || currentUser.ownsId(it.recipientId)
    } else {
      currentUser.ownsId(it.recipientId)
    }
  }

  LaunchedEffect(snackBarMessage) {
    snackBarMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.dismissSnackbar()
    }
  }

  val pendingAdminReviewsCount = users.count {
    it.role == UserRole.PRESTADOR && it.verificationStatus == ProviderVerificationStatus.PENDENTE_ANALISE
  }
  val agreedChatsCount = services.count { srv ->
    !srv.acceptedProviderId.isNullOrBlank() && (
      srv.status == ServiceStatus.ACEITO ||
        srv.status == ServiceStatus.EM_EXECUCAO ||
        srv.status == ServiceStatus.CONCLUIDO ||
        srv.status == ServiceStatus.CANCELADO
      )
  }

  // Pending/Rejected approval gate: Provider with unapproved documents cannot access opportunities/wallet until approved
  val isProviderPendingApproval = currentUser.role == UserRole.PRESTADOR &&
    currentUser.verificationStatus != ProviderVerificationStatus.APROVADO

  Box(modifier = Modifier.fillMaxSize()) {
    Crossfade(targetState = isAuthenticated, label = "auth_gate_crossfade") { authenticated ->
      if (!authenticated) {
        AuthScreen(
          authMode = authMode,
          authStep = authStep,
          pendingGoogleAccount = pendingGoogleAccount,
          socialProviderLabel = pendingSocialProviderLabel,
          prefillCompletionPhone = pendingLoginPhone.orEmpty(),
          preferredGoogleRole = pendingPreferredRole,
          pendingGoogleDraft = pendingGoogleDraft,
          authErrorMessage = authErrorMessage,
          authSuccessMessage = authSuccessMessage,
          isGoogleAuthLoading = isGoogleAuthLoading,
          isCredentialAuthLoading = isCredentialAuthLoading,
          passwordResetState = passwordResetState,
          onSelectMode = { viewModel.setAuthMode(it) },
          onClearError = { viewModel.clearAuthError() },
          onEmailPasswordSignIn = { email, password ->
            viewModel.signInWithEmailAndPassword(email, password)
          },
          onPhonePasswordSignIn = { phone, countryCode, password ->
            viewModel.signInWithPhoneAndPassword(phone, countryCode, password)
          },
          onSendPasswordReset = { email ->
            viewModel.sendPasswordReset(email)
          },
          onResetPasswordResetState = {
            viewModel.resetPasswordResetState()
          },
          onRegisterWithPassword = { reg, countryCode, password ->
            viewModel.registerWithPassword(reg, countryCode, password)
          },
          onBackFromSocialCompletion = { viewModel.backFromSocialCompletion() },
          onGoogleSignInClick = { preferredRole, regDraft ->
            viewModel.clearAuthError()
            viewModel.setGoogleAuthLoading(true)
            GoogleAuthManager.onGoogleSignInClicked(
              context = context,
              scope = scope,
              onAuthSuccess = { account ->
                viewModel.handleGoogleAuthSuccess(
                  account = account,
                  preferredRoleFromRegisterTab = preferredRole,
                  registrationDraft = regDraft
                )
              },
              onAuthError = { errMsg ->
                viewModel.setAuthError(errMsg)
              },
              onAuthCancelled = {
                viewModel.setGoogleAuthLoading(false)
              }
            )
          },
          onCompleteGoogleRegistration = { role, phone, city, doc, cat, bio, rgCnh, criminal, rgFile, rgUri, crimFile, crimUri, selfieFile, selfieUri, profilePhotoUri ->
            viewModel.completeGoogleRegistration(
              role = role,
              phone = phone,
              city = city,
              document = doc,
              category = cat,
              bio = bio,
              rgOrCnh = rgCnh,
              criminalRecord = criminal,
              rgOrCnhFileName = rgFile,
              rgOrCnhAttachmentUri = rgUri,
              criminalRecordFileName = crimFile,
              criminalRecordAttachmentUri = crimUri,
              selfieFileName = selfieFile,
              selfieAttachmentUri = selfieUri,
              profilePhotoUri = profilePhotoUri
            )
          },
          onSupportClick = { SupportLauncher.openWhatsAppSupport(context) },
          onOpenOnboarding = { viewModel.openOnboarding() },
          availableSpecialties = activeProviderSpecialties
        )
      } else {
        ModalNavigationDrawer(
          drawerState = drawerState,
          drawerContent = {
            AppMenuDrawerContent(
              currentUser = currentUser,
              selectedTab = selectedTab,
              pendingProposalsCount = pendingProposalsCount,
              unreadNotificationsCount = unreadNotificationsCount,
              pendingAdminReviewsCount = pendingAdminReviewsCount,
              onSelectTab = { tab ->
                viewModel.selectService(null)
                viewModel.setTab(tab)
                if (tab == MainTab.ADMIN_USUARIOS) {
                  viewModel.syncAllUsersFromFirestore()
                }
              },
              onNotificationsClick = { viewModel.setShowNotifications(true) },
              onSupportClick = { SupportLauncher.openWhatsAppSupport(context) },
              onAdminClick = {
                viewModel.selectService(null)
                viewModel.setTab(MainTab.ADMIN_DOCUMENTOS)
              },
              onSwitchUserClick = { viewModel.setShowUserSwitcher(true) },
              onRegisterClick = { viewModel.setShowRegister(true) },
              onRechargeClick = { viewModel.setShowRecharge(true) },
              onChangePasswordClick = { viewModel.setShowChangePasswordDialog(true) },
              onLogoutClick = { viewModel.logout(context) },
              onOpenOnboarding = { viewModel.openOnboarding() },
              onCloseDrawer = { scope.launch { drawerState.close() } },
              categoryFilter = categoryFilter,
              urgencyFilter = urgencyFilter,
              availableCategories = activeServiceCategoryNames,
              onSelectCategory = { cat ->
                viewModel.setCategoryFilter(cat)
                viewModel.selectService(null)
                viewModel.setTab(MainTab.SERVICOS)
              },
              onSelectUrgency = { urgency ->
                viewModel.setUrgencyFilter(urgency)
                viewModel.selectService(null)
                viewModel.setTab(MainTab.SERVICOS)
              },
              onResetFilters = {
                viewModel.setCategoryFilter("Serviços")
                viewModel.setUrgencyFilter("Todas")
              },
              agreedChatsCount = agreedChatsCount,
              cloudSyncStatus = cloudSyncStatus,
              onSyncFirebaseClick = { viewModel.syncAllDataWithFirebase() },
              onOpenProfileClick = { viewModel.openProfileData() }
            )
          }
        ) {
          BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val adaptiveSpec = remember(maxWidth) { resolveAdaptiveSpec(maxWidth) }
            val showNavigationChrome = !isProviderPendingApproval
            Row(modifier = Modifier.fillMaxSize()) {
              if (adaptiveSpec.isTablet && showNavigationChrome) {
                AdaptiveAppNavigationRail(
                  currentUser = currentUser,
                  selectedTab = selectedTab,
                  pendingProposalsCount = pendingProposalsCount,
                  pendingAdminReviewsCount = pendingAdminReviewsCount,
                  onSelectTab = { tab ->
                    viewModel.selectService(null)
                    viewModel.setTab(tab)
                  },
                  onOpenMenu = { scope.launch { drawerState.open() } }
                )
                VerticalDivider()
              }

              Scaffold(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxSize()
              ) { innerPadding ->
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                ) {
        if (isProviderPendingApproval) {
          ProviderPendingApprovalScreen(
            currentUser = currentUser,
            onFastApproveClick = { viewModel.approveProvider(currentUser.id) },
            onLogoutClick = { viewModel.logout(context) }
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
                  urgencyFilter = urgencyFilter,
                  onSearchChange = { viewModel.setSearchQuery(it) },
                  onCategoryChange = { viewModel.setCategoryFilter(it) },
                  onUrgencyChange = { viewModel.setUrgencyFilter(it) },
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
                  },
                  onProviderAcceptJob = { serviceId ->
                    viewModel.acceptServiceDirectlyByProvider(serviceId)
                  },
                  onOpenMenu = { scope.launch { drawerState.open() } },
                  unreadNotificationsCount = unreadNotificationsCount,
                  onNotificationsClick = { viewModel.setShowNotifications(true) },
                  onSupportClick = { SupportLauncher.openWhatsAppSupport(context) },
                  onProfileClick = { viewModel.openProfileData() },
                  onCancelService = { sId, reason ->
                    viewModel.cancelServiceRequest(sId, reason)
                  },
                  onCompleteServiceWithPhoto = { sId, photoUri, photoName, notes ->
                    viewModel.updateServiceStatus(
                      serviceId = sId,
                      status = ServiceStatus.CONCLUIDO,
                      completionPhotoUri = photoUri,
                      completionPhotoName = photoName,
                      completionNotes = notes
                    )
                  },
                  onCompleteServiceWithMedia = { sId, photoUri, photoName, videoUri, videoName, videoThumbnailUri, videoDurationMs, notes ->
                    viewModel.updateServiceStatus(
                      serviceId = sId,
                      status = ServiceStatus.CONCLUIDO,
                      completionPhotoUri = photoUri,
                      completionPhotoName = photoName,
                      completionVideoUri = videoUri,
                      completionVideoName = videoName,
                      completionVideoThumbnailUri = videoThumbnailUri,
                      completionVideoDurationMs = videoDurationMs,
                      completionNotes = notes
                    )
                  },
                  providers = users,
                  onManageShowcaseClick = if (currentUser.role == UserRole.PRESTADOR) {
                    { viewModel.setTab(MainTab.CARTEIRA_PERFIL) }
                  } else {
                    null
                  },
                  onAddClientCompletedPhoto = { sId, uri, name, caption ->
                    viewModel.addClientCompletedServicePhoto(sId, uri, name, caption)
                  },
                  onRemoveClientCompletedPhoto = { sId, photoId ->
                    viewModel.removeClientCompletedServicePhoto(sId, photoId)
                  },
                  dynamicCategories = activeServiceCategoryNames,
                  globalBannerMessage = appAdminSettings.globalBannerMessage,
                  onOpenAdminCatalogEdits = if (currentUser.role == UserRole.ADMIN) {
                    { viewModel.setTab(MainTab.ADMIN_CATALOG_EDICOES) }
                  } else {
                    null
                  },
                  reviews = reviews,
                  onRejectProposal = { proposalId ->
                    viewModel.rejectProposal(proposalId)
                  },
                  onOpenRatingDialog = { targetService ->
                    viewModel.openRatingDialog(targetService)
                  },
                  onReplyToReview = { reviewId, replyText ->
                    viewModel.replyToReview(reviewId, replyText)
                  },
                  declinedServiceIds = remember(currentUser, declinedServiceIdsByProvider) {
                    val uidSet = currentUser.firebaseUid?.let { declinedServiceIdsByProvider[it] }.orEmpty()
                    (currentUser.declinedServiceIds + declinedServiceIdsByProvider[currentUser.id].orEmpty() + uidSet).toSet()
                  },
                  onProviderDeclineService = { serviceId ->
                    viewModel.declineServiceByProvider(serviceId)
                  },
                  onOpenOrderHistory = if (currentUser.role == UserRole.PRESTADOR) {
                    { viewModel.setTab(MainTab.HISTORICO_PEDIDOS) }
                  } else {
                    null
                  },
                  onOpenRatingDialogWithStars = { targetService, initialStars ->
                    viewModel.openRatingDialog(targetService, initialStars)
                  },
                  onConfirmCompletedAndRate = { targetService ->
                    viewModel.confirmServiceCompletedByClientAndRate(targetService.id)
                  },
                  chatMessages = chatMessages
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
                  },
                  onBack = { viewModel.setTab(MainTab.SERVICOS) },
                  onOpenMenu = { scope.launch { drawerState.open() } },
                  onRejectProposal = { proposalId ->
                    viewModel.rejectProposal(proposalId)
                  },
                  reviews = reviews,
                  onOpenService = { serviceId ->
                    viewModel.selectService(serviceId)
                    viewModel.setTab(MainTab.SERVICOS)
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
                  },
                  onBack = { viewModel.setTab(MainTab.SERVICOS) },
                  onOpenMenu = { scope.launch { drawerState.open() } },
                  onRateServiceClick = { srv ->
                    viewModel.openRatingDialog(srv)
                  },
                  chatDiagnosticLogs = chatDiagnosticLogs,
                  onClearChatDiagnostics = { viewModel.clearChatDiagnosticLogs() },
                  onExportChatDiagnostics = { srvId -> viewModel.exportChatDiagnostics(srvId) }
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
                  },
                  onBack = { viewModel.setTab(MainTab.SERVICOS) },
                  onOpenMenu = { scope.launch { drawerState.open() } },
                  services = services,
                  onRefreshTransactions = { viewModel.syncWalletTransactionsFromFirestore() },
                  isRefreshingTransactions = isWalletHistoryLoading,
                  mercadoPagoCharges = mercadoPagoCharges,
                  isMpPollingActive = isMpPollingActive,
                  isRefreshingMpTransactions = isRefreshingMpTransactions,
                  onRefreshMpTransactions = { viewModel.refreshMercadoPagoTransactionsStatus() },
                  onReopenMpCharge = { charge -> viewModel.reopenPendingMercadoPagoCharge(charge) },
                  onCancelMpCharge = { charge -> viewModel.cancelPendingMercadoPagoCharge(charge) },
                  onOpenSuggestionsTips = { viewModel.setTab(MainTab.SUGESTOES_DICAS) },
                  onOpenAdminCatalogEdits = if (currentUser.role == UserRole.ADMIN) {
                    { viewModel.setTab(MainTab.ADMIN_CATALOG_EDICOES) }
                  } else {
                    null
                  },
                  reviews = reviews,
                  onReplyToReview = { reviewId, replyText ->
                    viewModel.replyToReview(reviewId, replyText)
                  },
                  onChangePasswordClick = { viewModel.setShowChangePasswordDialog(true) },
                  profileFocusNonce = profileFocusNonce
                )
              }

              MainTab.HISTORICO_PEDIDOS -> {
                val effectiveDeclined = remember(currentUser, declinedServiceIdsByProvider) {
                  val uidSet = currentUser.firebaseUid?.let { declinedServiceIdsByProvider[it] }.orEmpty()
                  (currentUser.declinedServiceIds + declinedServiceIdsByProvider[currentUser.id].orEmpty() + uidSet).toSet()
                }
                ProviderOrderHistoryScreen(
                  currentUser = currentUser,
                  services = services,
                  proposals = proposals,
                  declinedServiceIds = effectiveDeclined,
                  reviews = reviews,
                  onOpenMenu = { scope.launch { drawerState.open() } },
                  onBack = { viewModel.setTab(MainTab.SERVICOS) }
                )
              }

              MainTab.SUGESTOES_DICAS -> {
                SuggestionsTipsScreen(
                  currentUser = currentUser,
                  tips = appTips,
                  suggestions = appSuggestions,
                  onSubmitSuggestion = { cat, title, desc ->
                    viewModel.submitAppSuggestion(cat, title, desc)
                  },
                  onUpdateSuggestionStatus = { suggestionId, status, adminReply ->
                    viewModel.updateAppSuggestionStatus(suggestionId, status, adminReply)
                  },
                  onBack = { viewModel.setTab(MainTab.SERVICOS) }
                )
              }

              MainTab.ADMIN_DOCUMENTOS -> {
                AdminReviewScreen(
                  currentUser = currentUser,
                  users = users,
                  auditHistory = documentReviewAudits,
                  isLoading = isAdminReviewLoading,
                  syncStatusMessage = adminSyncStatusMessage,
                  onSyncFirestoreClick = { viewModel.syncAdminProvidersFromFirestore() },
                  onApproveProvider = { providerId, idOk, crimOk, selfieOk ->
                    viewModel.approveProviderDocuments(providerId, idOk, crimOk, selfieOk)
                  },
                  onRejectProvider = { providerId, reason, idOk, crimOk, selfieOk ->
                    viewModel.rejectProviderDocuments(providerId, reason, idOk, crimOk, selfieOk)
                  },
                  onOpenMenu = { scope.launch { drawerState.open() } },
                  onBackToHome = { viewModel.setTab(MainTab.SERVICOS) },
                  onOpenManageUsers = {
                    viewModel.setTab(MainTab.ADMIN_USUARIOS)
                    viewModel.syncAllUsersFromFirestore()
                  },
                  onOpenCatalogAndEdits = {
                    viewModel.setTab(MainTab.ADMIN_CATALOG_EDICOES)
                  },
                  services = services,
                  serviceLocationHistory = remember(serviceLocationHistory) {
                    serviceLocationHistory.groupBy { it.serviceId }
                  },
                  onProcessCancellationRefund = { serviceId, approve, note ->
                    viewModel.adminProcessCancellationRefund(serviceId, approve, note)
                  },
                  onSyncServiceLocationHistory = { serviceId ->
                    viewModel.syncLocationHistoryForService(serviceId)
                  }
                )
              }

              MainTab.ADMIN_USUARIOS -> {
                AdminUsersScreen(
                  currentUser = currentUser,
                  users = users,
                  isLoading = isAdminUsersLoading,
                  syncStatusMessage = adminUsersSyncMessage,
                  onSyncUsersClick = { viewModel.syncAllUsersFromFirestore() },
                  onUpdateUser = { updated -> viewModel.adminUpdateUser(updated) },
                  onAddProviderBalance = { providerId, amount, reason ->
                    viewModel.adminAddProviderBalance(providerId, amount, reason)
                  },
                  onChangeUserPassword = { userId, newPassword ->
                    viewModel.adminChangeUserPassword(userId, newPassword)
                  },
                  onSendPasswordResetEmail = { targetUser ->
                    viewModel.adminSendPasswordResetEmail(targetUser)
                  },
                  onDeleteUser = { userId -> viewModel.adminDeleteUser(userId) },
                  onAddUserClick = { viewModel.setShowRegister(true) },
                  onOpenDocumentsReview = {
                    viewModel.setTab(MainTab.ADMIN_DOCUMENTOS)
                    viewModel.syncAdminProvidersFromFirestore()
                  },
                  onOpenCatalogAndEdits = {
                    viewModel.setTab(MainTab.ADMIN_CATALOG_EDICOES)
                  },
                  availableSpecialties = activeProviderSpecialties,
                  userPasswords = adminUserPasswords,
                  onOpenMenu = { scope.launch { drawerState.open() } },
                  onBackToHome = { viewModel.setTab(MainTab.SERVICOS) }
                )
              }

              MainTab.ADMIN_CATALOG_EDICOES -> {
                AdminCatalogAndEditsScreen(
                  currentUser = currentUser,
                  categories = serviceCategories,
                  services = services,
                  appSettings = appAdminSettings,
                  appTips = appTips,
                  onAddCategory = { name, specialty -> viewModel.adminAddServiceCategory(name, specialty) },
                  onUpdateCategory = { id, name, specialty, active ->
                    viewModel.adminUpdateServiceCategory(id, name, specialty, active)
                  },
                  onToggleCategory = { id, active -> viewModel.adminToggleServiceCategory(id, active) },
                  onMoveCategory = { id, up -> viewModel.adminMoveServiceCategory(id, up) },
                  onDeleteCategory = { id -> viewModel.adminDeleteServiceCategory(id) },
                  onResetDefaultCategories = { viewModel.adminResetDefaultCategories() },
                  onUpdateService = { updated -> viewModel.adminUpdateServiceRequest(updated) },
                  onUpdateAppSettings = { updated -> viewModel.adminUpdateAppSettings(updated) },
                  onSaveTip = { tip -> viewModel.adminSaveAppTip(tip) },
                  onDeleteTip = { tipId -> viewModel.adminDeleteAppTip(tipId) },
                  onOpenManageUsers = {
                    viewModel.setTab(MainTab.ADMIN_USUARIOS)
                    viewModel.syncAllUsersFromFirestore()
                  },
                  onOpenDocumentsReview = {
                    viewModel.setTab(MainTab.ADMIN_DOCUMENTOS)
                    viewModel.syncAdminProvidersFromFirestore()
                  },
                  onOpenSuggestions = { viewModel.setTab(MainTab.SUGESTOES_DICAS) },
                  onOpenMenu = { scope.launch { drawerState.open() } },
                  onBackToHome = { viewModel.setTab(MainTab.SERVICOS) },
                  chatMessages = chatMessages,
                  onOpenServiceChat = { serviceId -> viewModel.openChatForService(serviceId) }
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
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .imePadding()
    )
  }

  if (showSupportDialog) {
    SupportContactDialog(
      onDismiss = { showSupportDialog = false },
      isLoginContext = !isAuthenticated
    )
  }

  // Dialogs (only active when authenticated)
  if (isAuthenticated) {

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
    val userNotifications = notifications.filter {
      if (currentUser.role == UserRole.PRESTADOR) {
        it.recipientId == "all_providers" || currentUser.ownsId(it.recipientId)
      } else {
        currentUser.ownsId(it.recipientId)
      }
    }
    NotificationsDialog(
      notifications = userNotifications,
      onDismiss = { viewModel.setShowNotifications(false) },
      onMarkRead = { viewModel.markNotificationAsRead(it) },
      onOpenService = { sId ->
        viewModel.selectService(sId)
        viewModel.setTab(MainTab.SERVICOS)
      },
      onOpenChat = { sId ->
        viewModel.openChatForService(sId)
      },
      onMarkAllRead = { viewModel.markAllNotificationsAsRead() }
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
      onCreate = { title, category, desc, loc, budget, urgency, photoUri, photoName ->
        viewModel.createService(
          title = title,
          category = category,
          description = desc,
          location = loc,
          proposedBudget = budget,
          urgency = urgency,
          attachmentPhotoUri = photoUri,
          attachmentPhotoName = photoName
        )
      },
      onOpenSupport = { SupportLauncher.openWhatsAppSupport(context) },
      onCreateWithMedia = { title, category, desc, loc, budget, urgency, photoUri, photoName, videoUri, videoName, videoThumbnailUri, videoDurationMs ->
        viewModel.createService(
          title = title,
          category = category,
          description = desc,
          location = loc,
          proposedBudget = budget,
          urgency = urgency,
          attachmentPhotoUri = photoUri,
          attachmentPhotoName = photoName,
          attachmentVideoUri = videoUri,
          attachmentVideoName = videoName,
          attachmentVideoThumbnailUri = videoThumbnailUri,
          attachmentVideoDurationMs = videoDurationMs
        )
      },
      availableCategories = activeServiceCategoryNames,
      defaultUrgency = appAdminSettings.defaultServiceUrgency,
      defaultCity = currentUser.city,
      initialDetectedLocalCity = detectedLocalCity,
      onLocalCityDetected = { city ->
        viewModel.updateDetectedLocalCity(city)
      },
      autoDetectLocalCityForRegionalPrice = true,
      autoRequestLocalCityPermission = true,
      estimateRegionalPrice = { cat, loc ->
        viewModel.resolveRegionalPriceEstimate(category = cat, locationText = loc)
      },
      syncDailyRegionalPrice = { cat, loc ->
        viewModel.refreshDailyRegionalPriceEstimate(category = cat, locationText = loc)
      }
    )
  }

  if (showRecharge) {
    RechargeWalletDialog(
      currentBalance = currentUser.walletBalance,
      pixRechargeState = pixRechargeState,
      pixGatewayError = pixRechargeError,
      isMercadoPagoConfigured = com.example.data.MercadoPagoPixService.isConfigured(),
      onGenerateMercadoPagoPix = { amount ->
        viewModel.startMercadoPagoPixRecharge(amount = amount)
      },
      onCheckPixPaymentNow = {
        viewModel.checkPixPaymentStatusNow()
      },
      onResetPixCharge = {
        viewModel.resetPixRecharge()
      },
      onDismiss = { viewModel.setShowRecharge(false) }
    )
  }

  if (showUserSwitcher && currentUser.role == UserRole.ADMIN) {
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

  if (showRegister && currentUser.role == UserRole.ADMIN) {
    RegisterUserDialog(
      onDismiss = { viewModel.setShowRegister(false) },
      onConfirmRegister = { registration ->
        viewModel.registerUserDirectly(registration)
      },
      availableSpecialties = activeProviderSpecialties
    )
  }

  if (ratingServiceTarget != null) {
    ServiceRatingDialog(
      service = ratingServiceTarget!!,
      initialRating = ratingInitialStars,
      onDismiss = { viewModel.dismissRatingDialog() },
      onSubmit = { rating, comment, tags ->
        viewModel.submitServiceRating(ratingServiceTarget!!.id, rating, comment, tags)
      }
    )
  }

  if (showChangePasswordDialog) {
    ChangeOwnPasswordDialog(
      currentUser = currentUser,
      state = changePasswordState,
      onDismiss = { viewModel.setShowChangePasswordDialog(false) },
      onClearState = { viewModel.resetChangePasswordState() },
      onConfirmChangePassword = { currentPwd, newPwd, confirmPwd ->
        viewModel.changeOwnPassword(currentPwd, newPwd, confirmPwd)
      }
    )
  }

  if (insufficientBalanceData != null) {
    InsufficientBalanceDialog(
      data = insufficientBalanceData!!,
      viewerRole = currentUser.role,
      onDismiss = { viewModel.dismissInsufficientBalance() },
      onRechargeClick = {
        viewModel.dismissInsufficientBalance()
        viewModel.setShowRecharge(true)
      }
    )
  }
  }

    AnimatedVisibility(
      visible = !hasSeenOnboarding || showOnboardingReplay,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      ResolveAiOnboardingComponent(
        onFinish = { viewModel.dismissOnboarding() },
        onSkip = { viewModel.dismissOnboarding() }
      )
    }
}
