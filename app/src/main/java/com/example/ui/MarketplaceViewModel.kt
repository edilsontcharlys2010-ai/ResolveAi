package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CloudSyncStatus
import com.example.data.InsufficientBalanceException
import com.example.data.MarketplaceRepository
import com.example.model.ContactFilter
import com.example.model.ContactFilterResult
import com.example.model.Proposal
import com.example.model.ServiceRequest
import com.example.model.ServiceReview
import com.example.model.ServiceStatus
import com.example.model.User
import com.example.model.AppAdminSettings
import com.example.model.AppNotification
import com.example.model.ProviderDocuments
import com.example.model.ProviderVerificationStatus
import com.example.model.RegionalPriceEstimate
import com.example.model.ServiceCategoryItem
import com.example.model.UserRole
import com.example.model.PixChargeStatus
import com.example.model.PixRechargeCharge
import android.app.Activity
import android.content.Context
import com.example.BuildConfig
import com.example.data.ContactValidator
import com.example.data.CpfValidator
import com.example.data.FirebaseAuthHelper
import com.example.data.FirebaseCredentialAuthResult
import com.example.data.FirebasePhoneAuthManager
import com.example.model.AppSuggestion
import com.example.model.AppTip
import com.example.model.ProviderLocationPoint
import com.example.model.SuggestionStatus
import com.example.data.GoogleAccountInfo
import com.example.data.GoogleAuthManager
import com.example.data.MercadoPagoApi
import com.example.data.MercadoPagoPixService
import com.example.data.PixKeyType
import com.example.data.WalletRechargeHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class MainTab {
  SERVICOS,
  NEGOCIACOES,
  CHAT,
  CARTEIRA_PERFIL,
  HISTORICO_PEDIDOS,
  SUGESTOES_DICAS,
  ADMIN_DOCUMENTOS,
  ADMIN_USUARIOS,
  ADMIN_CATALOG_EDICOES
}

enum class AuthMode {
  LOGIN,
  REGISTER
}

enum class AuthFlowStep {
  FORM,
  GOOGLE_ROLE_COMPLETION
}

sealed class PasswordResetUiState {
  object Idle : PasswordResetUiState()
  object Loading : PasswordResetUiState()
  data class Success(val email: String, val message: String) : PasswordResetUiState()
  data class Error(val message: String) : PasswordResetUiState()
}

sealed class ChangePasswordUiState {
  object Idle : ChangePasswordUiState()
  object Loading : ChangePasswordUiState()
  data class Success(val message: String) : ChangePasswordUiState()
  data class Error(val message: String) : ChangePasswordUiState()
}

sealed class PixRechargeUiState {
  object Idle : PixRechargeUiState()
  data class CreatingCharge(
    val amount: Double,
    val gatewayLabel: String = "Mercado Pago"
  ) : PixRechargeUiState()
  data class AwaitingPayment(
    val charge: PixRechargeCharge,
    val isCheckingNow: Boolean = false,
    val statusNotice: String? = null
  ) : PixRechargeUiState()
  data class Detected(
    val charge: PixRechargeCharge,
    val newBalance: Double,
    val automatic: Boolean
  ) : PixRechargeUiState()
}

data class PendingRegistration(
  val name: String,
  val email: String,
  val phone: String,
  val role: UserRole,
  val category: String = "",
  val bio: String = "",
  val city: String = "",
  val document: String = "",
  val initialWalletBonus: Double = 0.0,
  val rgOrCnhNumber: String = "",
  val rgOrCnhFileName: String = "",
  val rgOrCnhAttachmentUri: String? = null,
  val criminalRecordNumber: String = "",
  val criminalRecordFileName: String = "",
  val criminalRecordAttachmentUri: String? = null,
  val selfieFileName: String = "",
  val selfieAttachmentUri: String? = null,
  val profilePhotoUri: String? = null,
  val password: String = ""
)

class MarketplaceViewModel(
  private val repository: MarketplaceRepository = MarketplaceRepository.instance
) : ViewModel() {

  val users = repository.users
  val currentUserId = repository.currentUserId
  val services = repository.services
  val proposals = repository.proposals
  val walletTransactions = repository.walletTransactions
  val mercadoPagoCharges = repository.mercadoPagoCharges
  val chatMessages = repository.chatMessages
  val reviews = repository.reviews
  val notifications = repository.notifications
  val documentReviewAudits = repository.documentReviewAudits
  val appSuggestions: StateFlow<List<AppSuggestion>> = repository.appSuggestions
  val appTipsFlow: StateFlow<List<AppTip>> = repository.appTipsFlow
  val appTips: List<AppTip> get() = repository.appTips
  val serviceCategories: StateFlow<List<ServiceCategoryItem>> = repository.serviceCategories
  val appAdminSettings: StateFlow<AppAdminSettings> = repository.appAdminSettings
  val serviceLocationHistory: StateFlow<List<ProviderLocationPoint>> = repository.serviceLocationHistory
  val declinedServiceIdsByProvider: StateFlow<Map<String, Set<String>>> = repository.declinedServiceIdsByProvider
  val hasSeenOnboarding: StateFlow<Boolean> = repository.hasSeenOnboarding
  val isAuthenticated: StateFlow<Boolean> = repository.isAuthenticated
  val cloudSyncStatus: StateFlow<CloudSyncStatus> = repository.cloudSyncStatus
  val chatDiagnosticLogs: StateFlow<List<com.example.data.ChatDiagnosticLogEntry>> = repository.chatDiagnosticLogs

  private val _isRefreshingMpTransactions = MutableStateFlow(false)
  val isRefreshingMpTransactions: StateFlow<Boolean> = _isRefreshingMpTransactions.asStateFlow()

  private val _isMercadoPagoPollingActive = MutableStateFlow(false)
  val isMercadoPagoPollingActive: StateFlow<Boolean> = _isMercadoPagoPollingActive.asStateFlow()

  private var pendingMpPollingJob: Job? = null

  init {
    viewModelScope.launch {
      repository.chatDeliveryError.collect { err ->
        if (err != null) {
          _snackBarMessage.value = "⚠️ $err"
          repository.clearChatDeliveryError()
        }
      }
    }
    viewModelScope.launch {
      repository.reviewSyncError.collect { err ->
        if (err != null) {
          _snackBarMessage.value = "⚠️ $err"
          repository.clearReviewSyncError()
        }
      }
    }
    if (repository.mercadoPagoCharges.value.any {
        it.status == PixChargeStatus.AGUARDANDO_PAGAMENTO && it.mercadoPagoPaymentId != null
      }
    ) {
      startPendingMercadoPagoPolling()
    }
  }

  // Exclusive Admin Document Review states
  private val _isAdminReviewLoading = MutableStateFlow(false)
  val isAdminReviewLoading: StateFlow<Boolean> = _isAdminReviewLoading.asStateFlow()

  private val _adminSyncStatusMessage = MutableStateFlow<String?>(null)
  val adminSyncStatusMessage: StateFlow<String?> = _adminSyncStatusMessage.asStateFlow()

  // Exclusive Admin Users Management states
  private val _isAdminUsersLoading = MutableStateFlow(false)
  val isAdminUsersLoading: StateFlow<Boolean> = _isAdminUsersLoading.asStateFlow()

  private val _adminUsersSyncMessage = MutableStateFlow<String?>(null)
  val adminUsersSyncMessage: StateFlow<String?> = _adminUsersSyncMessage.asStateFlow()

  fun initFirestore(context: Context) {
    repository.attachFirestore(context.applicationContext)
  }

  // Initial Login / Registration Auth Screen states
  private val _authMode = MutableStateFlow(AuthMode.LOGIN)
  val authMode: StateFlow<AuthMode> = _authMode.asStateFlow()

  private val _authStep = MutableStateFlow(AuthFlowStep.FORM)
  val authStep: StateFlow<AuthFlowStep> = _authStep.asStateFlow()

  private val _pendingLoginPhone = MutableStateFlow<String?>(null)
  val pendingLoginPhone: StateFlow<String?> = _pendingLoginPhone.asStateFlow()

  private val _authErrorMessage = MutableStateFlow<String?>(null)
  val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

  private val _authSuccessMessage = MutableStateFlow<String?>(null)
  val authSuccessMessage: StateFlow<String?> = _authSuccessMessage.asStateFlow()

  private val _pendingGoogleAccount = MutableStateFlow<GoogleAccountInfo?>(null)
  val pendingGoogleAccount: StateFlow<GoogleAccountInfo?> = _pendingGoogleAccount.asStateFlow()

  private val _pendingSocialProviderLabel = MutableStateFlow("Google")
  val pendingSocialProviderLabel: StateFlow<String> = _pendingSocialProviderLabel.asStateFlow()

  private val _pendingPreferredRole = MutableStateFlow(UserRole.CLIENTE)
  val pendingPreferredRole: StateFlow<UserRole> = _pendingPreferredRole.asStateFlow()

  private val _pendingGoogleDraft = MutableStateFlow<PendingRegistration?>(null)
  val pendingGoogleDraft: StateFlow<PendingRegistration?> = _pendingGoogleDraft.asStateFlow()

  private val _isGoogleAuthLoading = MutableStateFlow(false)
  val isGoogleAuthLoading: StateFlow<Boolean> = _isGoogleAuthLoading.asStateFlow()

  private val _isCredentialAuthLoading = MutableStateFlow(false)
  val isCredentialAuthLoading: StateFlow<Boolean> = _isCredentialAuthLoading.asStateFlow()

  private val _passwordResetState = MutableStateFlow<PasswordResetUiState>(PasswordResetUiState.Idle)
  val passwordResetState: StateFlow<PasswordResetUiState> = _passwordResetState.asStateFlow()

  private val _pixRechargeState = MutableStateFlow<PixRechargeUiState>(PixRechargeUiState.Idle)
  val pixRechargeState: StateFlow<PixRechargeUiState> = _pixRechargeState.asStateFlow()

  private val _pixRechargeError = MutableStateFlow<String?>(null)
  val pixRechargeError: StateFlow<String?> = _pixRechargeError.asStateFlow()

  private val _isWalletHistoryLoading = MutableStateFlow(false)
  val isWalletHistoryLoading: StateFlow<Boolean> = _isWalletHistoryLoading.asStateFlow()

  private var pixPaymentObserverJob: Job? = null
  private var pixPollingJob: Job? = null

  private val _showNotificationsDialog = MutableStateFlow(false)
  val showNotificationsDialog: StateFlow<Boolean> = _showNotificationsDialog.asStateFlow()

  fun setShowNotifications(show: Boolean) {
    _showNotificationsDialog.value = show
  }

  fun markNotificationAsRead(notifId: String) {
    repository.markNotificationAsRead(notifId)
  }

  fun markAllNotificationsAsRead() {
    repository.markAllNotificationsAsReadForCurrentUser()
  }

  fun syncAdminProvidersFromFirestore() {
    if (currentUser.value.role != UserRole.ADMIN) return
    viewModelScope.launch {
      _isAdminReviewLoading.value = true
      val result = repository.syncProvidersFromFirestore()
      _isAdminReviewLoading.value = false
      result.onSuccess { count ->
        _adminSyncStatusMessage.value = if (count > 0) {
          "Sincronizado com Firestore ($count prestador(es) na nuvem)"
        } else {
          "Conectado ao Firestore • Fila pronta para análise"
        }
      }.onFailure { err ->
        _adminSyncStatusMessage.value = err.message ?: "Modo local ativo (sem sessão Firebase)"
      }
    }
  }

  fun syncAllUsersFromFirestore() {
    if (currentUser.value.role != UserRole.ADMIN) return
    viewModelScope.launch {
      _isAdminUsersLoading.value = true
      val result = repository.syncAllUsersFromFirestore()
      _isAdminUsersLoading.value = false
      result.onSuccess { count ->
        _adminUsersSyncMessage.value = if (count > 0) {
          "Sincronizado com Firestore ($count usuário(s) na nuvem)"
        } else {
          "Conectado ao Firestore • Lista pronta para gestão"
        }
      }.onFailure { err ->
        _adminUsersSyncMessage.value = err.message ?: "Modo local ativo (sem sessão Firebase)"
      }
    }
  }

  fun adminUpdateUser(updatedUser: User): Result<Unit> {
    val result = repository.adminUpdateUser(updatedUser)
    result.onSuccess {
      _snackBarMessage.value = "✅ Usuário ${updatedUser.name.trim()} atualizado com sucesso!"
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível atualizar o usuário."}"
    }
    return result
  }

  fun adminAddProviderBalance(
    providerId: String,
    amount: Double,
    reason: String = ""
  ): Result<Double> {
    val targetName = users.value.find { it.id == providerId || it.firebaseUid == providerId }?.name
    val result = repository.adminAddProviderBalance(providerId, amount, reason)
    result.onSuccess { newBalance ->
      val label = targetName?.let { " para $it" } ?: ""
      _snackBarMessage.value = "💰 +R$ ${String.format("%.2f", amount)} adicionado$label! Novo saldo: R$ ${String.format("%.2f", newBalance)}"
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível adicionar saldo."}"
    }
    return result
  }

  fun adminDeleteUser(userId: String): Result<Unit> {
    val targetName = users.value.find { it.id == userId }?.name
    val result = repository.adminDeleteUser(userId)
    result.onSuccess {
      val label = targetName?.let { " $it" } ?: ""
      _snackBarMessage.value = "🗑️ Usuário$label excluído com sucesso."
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível excluir o usuário."}"
    }
    return result
  }

  fun adminChangeUserPassword(userId: String, newPassword: String): Result<Unit> {
    val result = repository.adminSetUserPassword(userId, newPassword)
    result.onSuccess { target ->
      val activeFbUid = try {
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
      } catch (_: Throwable) {
        null
      }
      viewModelScope.launch {
        if (!activeFbUid.isNullOrBlank() && target.ownsId(activeFbUid)) {
          FirebaseAuthHelper.updateCurrentUserPassword(newPassword)
          repository.syncLoginDirectoryForUserAwait(target, rawPassword = newPassword)
        } else {
          repository.provisionAndLinkAdminAuthCredential(target.id, newPassword)
        }
      }
      _snackBarMessage.value = "🔑 Senha de ${target.name} alterada e sincronizada com sucesso!"
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível alterar a senha do usuário."}"
    }
    return result.map { Unit }
  }

  fun adminSendPasswordResetEmail(user: User) {
    if (currentUser.value.role != UserRole.ADMIN) return
    val cleanEmail = user.email.trim()
    if (!ContactValidator.isValidEmail(cleanEmail)) {
      _snackBarMessage.value = "⚠️ O usuário ${user.name} não possui um e-mail válido cadastrado."
      return
    }
    viewModelScope.launch {
      val result = FirebaseAuthHelper.sendPasswordReset(cleanEmail)
      result.onSuccess { msg ->
        _snackBarMessage.value = "📧 $msg"
      }.onFailure { err ->
        _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível enviar o link de redefinição."}"
      }
    }
  }

  fun adminAddServiceCategory(name: String, specialtyLabel: String = ""): Result<ServiceCategoryItem> {
    val result = repository.adminAddServiceCategory(name, specialtyLabel)
    result.onSuccess { cat ->
      _snackBarMessage.value = "✅ Categoria \"${cat.name}\" adicionada com sucesso!"
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível adicionar a categoria."}"
    }
    return result
  }

  fun adminUpdateServiceCategory(
    categoryId: String,
    name: String,
    specialtyLabel: String,
    isActive: Boolean
  ): Result<ServiceCategoryItem> {
    val result = repository.adminUpdateServiceCategory(categoryId, name, specialtyLabel, isActive)
    result.onSuccess { cat ->
      _snackBarMessage.value = "✅ Categoria \"${cat.name}\" atualizada!"
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível atualizar a categoria."}"
    }
    return result
  }

  fun adminToggleServiceCategory(categoryId: String, isActive: Boolean): Result<ServiceCategoryItem> {
    val result = repository.adminToggleServiceCategory(categoryId, isActive)
    result.onSuccess { cat ->
      val statusTxt = if (cat.isActive) "ativada" else "desativada"
      _snackBarMessage.value = "🏷️ Categoria \"${cat.name}\" $statusTxt."
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível alterar a categoria."}"
    }
    return result
  }

  fun adminMoveServiceCategory(categoryId: String, moveUp: Boolean): Result<Unit> {
    return repository.adminMoveServiceCategory(categoryId, moveUp)
  }

  fun adminDeleteServiceCategory(categoryId: String): Result<Unit> {
    val result = repository.adminDeleteServiceCategory(categoryId)
    result.onSuccess {
      _snackBarMessage.value = "🗑️ Categoria removida do catálogo."
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível remover a categoria."}"
    }
    return result
  }

  fun adminResetDefaultCategories(): Result<List<ServiceCategoryItem>> {
    val result = repository.adminResetDefaultCategories()
    result.onSuccess {
      _snackBarMessage.value = "🔄 Categorias padrão restauradas!"
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível restaurar as categorias."}"
    }
    return result
  }

  fun adminUpdateServiceRequest(updated: ServiceRequest): Result<ServiceRequest> {
    val result = repository.adminUpdateServiceRequest(updated)
    result.onSuccess { srv ->
      _snackBarMessage.value = "✅ Serviço \"${srv.title}\" atualizado pelo Administrador!"
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível atualizar o serviço."}"
    }
    return result
  }

  fun adminUpdateAppSettings(updated: AppAdminSettings): Result<AppAdminSettings> {
    val result = repository.adminUpdateAppSettings(updated)
    result.onSuccess {
      _snackBarMessage.value = "✅ Configurações do aplicativo salvas!"
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível salvar as configurações."}"
    }
    return result
  }

  fun adminSaveAppTip(tip: AppTip): Result<AppTip> {
    val result = repository.adminSaveAppTip(tip)
    result.onSuccess { saved ->
      _snackBarMessage.value = "✅ Dica \"${saved.title}\" salva com sucesso!"
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível salvar a dica."}"
    }
    return result
  }

  fun adminDeleteAppTip(tipId: String): Result<Unit> {
    val result = repository.adminDeleteAppTip(tipId)
    result.onSuccess {
      _snackBarMessage.value = "🗑️ Dica removida."
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível remover a dica."}"
    }
    return result
  }

  fun approveProvider(providerId: String) {
    approveProviderDocuments(
      providerId = providerId,
      idDocVerified = true,
      criminalRecordVerified = true,
      selfieVerified = true
    )
  }

  fun approveProviderDocuments(
    providerId: String,
    idDocVerified: Boolean,
    criminalRecordVerified: Boolean,
    selfieVerified: Boolean
  ) {
    viewModelScope.launch {
      _isAdminReviewLoading.value = true
      val result = repository.reviewProviderDocumentsInFirestore(
        providerId = providerId,
        decision = ProviderVerificationStatus.APROVADO,
        reason = null,
        idDocVerified = idDocVerified,
        criminalRecordVerified = criminalRecordVerified,
        selfieVerified = selfieVerified
      )
      _isAdminReviewLoading.value = false
      result.onSuccess { syncedCloud ->
        _adminSyncStatusMessage.value = if (syncedCloud) {
          "Aprovação gravada no Firestore com auditoria imutável"
        } else {
          "Aprovado localmente (entre com Google Admin para gravar no Firestore)"
        }
        _snackBarMessage.value = if (syncedCloud) {
          "✅ Prestador aprovado e salvo no Firestore com trilha de auditoria!"
        } else {
          "✅ Prestador aprovado! Acesso liberado ao mural e carteira."
        }
      }.onFailure { err ->
        _adminSyncStatusMessage.value = "Erro Firestore: ${err.localizedMessage ?: "Falha de permissão"}"
        _snackBarMessage.value = "⚠️ Aprovado localmente, mas falhou ao gravar no Firestore: ${err.localizedMessage}"
      }
    }
  }

  fun rejectProvider(providerId: String, reason: String) {
    rejectProviderDocuments(
      providerId = providerId,
      reason = reason,
      idDocVerified = false,
      criminalRecordVerified = false,
      selfieVerified = false
    )
  }

  fun rejectProviderDocuments(
    providerId: String,
    reason: String,
    idDocVerified: Boolean,
    criminalRecordVerified: Boolean,
    selfieVerified: Boolean
  ) {
    val cleanReason = reason.trim()
    if (cleanReason.isBlank()) {
      _snackBarMessage.value = "Informe o motivo da reprovação antes de confirmar."
      return
    }
    viewModelScope.launch {
      _isAdminReviewLoading.value = true
      val result = repository.reviewProviderDocumentsInFirestore(
        providerId = providerId,
        decision = ProviderVerificationStatus.REJEITADO,
        reason = cleanReason,
        idDocVerified = idDocVerified,
        criminalRecordVerified = criminalRecordVerified,
        selfieVerified = selfieVerified
      )
      _isAdminReviewLoading.value = false
      result.onSuccess { syncedCloud ->
        _adminSyncStatusMessage.value = if (syncedCloud) {
          "Reprovação gravada no Firestore com motivo registrado"
        } else {
          "Reprovado localmente (entre com Google Admin para gravar no Firestore)"
        }
        _snackBarMessage.value = if (syncedCloud) {
          "⛔ Cadastro reprovado e salvo no Firestore: $cleanReason"
        } else {
          "⚠️ Documentação do prestador reprovada: $cleanReason"
        }
      }.onFailure { err ->
        _adminSyncStatusMessage.value = "Erro Firestore: ${err.localizedMessage ?: "Falha de permissão"}"
        _snackBarMessage.value = "⚠️ Reprovado localmente, mas falhou ao gravar no Firestore: ${err.localizedMessage}"
      }
    }
  }

  private fun completeRegistration(pending: PendingRegistration, originLabel: String = "Firebase Authentication") {
    val formattedDoc = CpfValidator.formatCpf(pending.document)
    val docs = if (pending.role == UserRole.PRESTADOR) {
      ProviderDocuments(
        rgOrCnhNumber = pending.rgOrCnhNumber.trim().ifBlank { pending.rgOrCnhFileName.ifBlank { "RG/CNH anexado" } },
        rgOrCnhFileName = pending.rgOrCnhFileName,
        rgOrCnhAttachmentUri = pending.rgOrCnhAttachmentUri?.takeIf { it.isNotBlank() },
        criminalRecordCertNumber = pending.criminalRecordNumber.trim().ifBlank { pending.criminalRecordFileName.ifBlank { "Certidão anexada" } },
        criminalRecordFileName = pending.criminalRecordFileName,
        criminalRecordAttachmentUri = pending.criminalRecordAttachmentUri?.takeIf { it.isNotBlank() },
        selfieFileName = pending.selfieFileName,
        selfieAttachmentUri = pending.selfieAttachmentUri?.takeIf { it.isNotBlank() }
      )
    } else null

    val newUser = repository.registerUser(
      name = pending.name,
      email = pending.email,
      phone = pending.phone,
      role = pending.role,
      category = pending.category,
      bio = pending.bio,
      city = pending.city,
      document = formattedDoc,
      initialWalletBonus = pending.initialWalletBonus,
      photoUrl = pending.profilePhotoUri?.takeIf { it.isNotBlank() },
      documents = docs
    )
    _showRegisterDialog.value = false
    _pendingRegistration.value = null
    _pendingLoginPhone.value = null
    _authErrorMessage.value = null
    _authSuccessMessage.value = null
    _authStep.value = AuthFlowStep.FORM
    if (newUser.role == UserRole.PRESTADOR) {
      _snackBarMessage.value = "📋 Cadastro de Prestador realizado! Seus documentos foram enviados para análise do Administrador."
    } else {
      _snackBarMessage.value = "🎉 Bem-vindo(a), ${newUser.name}! Conta ativada via $originLabel."
    }
  }

  fun setAuthMode(mode: AuthMode) {
    _authMode.value = mode
    _authStep.value = AuthFlowStep.FORM
    _pendingGoogleAccount.value = null
    _pendingPreferredRole.value = UserRole.CLIENTE
    _pendingGoogleDraft.value = null
    _authErrorMessage.value = null
    _authSuccessMessage.value = null
    _passwordResetState.value = PasswordResetUiState.Idle
  }

  /**
   * Restores the AuthScreen navigation state (e.g., AuthMode.REGISTER or GOOGLE_ROLE_COMPLETION)
   * after Activity recreation or process death while returning from the system Camera/Gallery.
   */
  fun restoreAuthNavigationState(
    mode: AuthMode,
    step: AuthFlowStep,
    pendingGoogleAccount: GoogleAccountInfo? = null,
    socialProviderLabel: String = "Google",
    preferredRole: UserRole = UserRole.CLIENTE,
    pendingLoginPhone: String? = null
  ) {
    _authMode.value = mode
    _authStep.value = if (step == AuthFlowStep.GOOGLE_ROLE_COMPLETION && pendingGoogleAccount == null) {
      AuthFlowStep.FORM
    } else {
      step
    }
    if (pendingGoogleAccount != null) {
      _pendingGoogleAccount.value = pendingGoogleAccount
      _pendingSocialProviderLabel.value = socialProviderLabel
    }
    _pendingPreferredRole.value = preferredRole
    if (!pendingLoginPhone.isNullOrBlank()) {
      _pendingLoginPhone.value = pendingLoginPhone
    }
  }

  fun syncAllDataWithFirebase() {
    viewModelScope.launch {
      val result = repository.syncAllDataWithFirebase()
      result.onSuccess { summary ->
        _snackBarMessage.value = "☁️ $summary"
      }.onFailure { err ->
        _snackBarMessage.value = "⚠️ ${err.localizedMessage ?: "Não foi possível sincronizar com o Firebase."}"
      }
    }
  }

  fun clearAuthError() {
    _authErrorMessage.value = null
    _authSuccessMessage.value = null
  }

  fun setAuthError(message: String) {
    _isGoogleAuthLoading.value = false
    _isCredentialAuthLoading.value = false
    _authSuccessMessage.value = null
    _authErrorMessage.value = message
  }

  fun setGoogleAuthLoading(loading: Boolean) {
    _isGoogleAuthLoading.value = loading
  }

  fun backFromSocialCompletion() {
    _authStep.value = AuthFlowStep.FORM
    _pendingGoogleAccount.value = null
    _pendingLoginPhone.value = null
    _pendingPreferredRole.value = UserRole.CLIENTE
    _pendingGoogleDraft.value = null
    _authErrorMessage.value = null
    _authSuccessMessage.value = null
  }

  private fun PendingRegistration.hasCompleteDocumentsForGoogle(): Boolean {
    return validateRegistrationDocuments(
      role = role,
      email = email,
      phone = phone,
      city = city,
      document = document,
      rgOrCnh = rgOrCnhNumber,
      rgOrCnhAttachmentUri = rgOrCnhAttachmentUri,
      criminalRecord = criminalRecordNumber,
      criminalRecordAttachmentUri = criminalRecordAttachmentUri,
      selfieAttachmentUri = selfieAttachmentUri,
      profilePhotoUri = profilePhotoUri,
      requireEmail = false,
      requirePhone = true,
      requireProviderProfilePhoto = true
    ) == null
  }

  private fun validateRegistrationDocuments(
    role: UserRole,
    email: String = "",
    phone: String,
    city: String,
    document: String,
    rgOrCnh: String,
    rgOrCnhAttachmentUri: String?,
    criminalRecord: String,
    criminalRecordAttachmentUri: String?,
    selfieAttachmentUri: String?,
    profilePhotoUri: String? = null,
    requireEmail: Boolean = false,
    requirePhone: Boolean = true,
    requireProviderProfilePhoto: Boolean = false,
    isAdminManualRegistration: Boolean = false,
    countryCode: String = FirebasePhoneAuthManager.DEFAULT_COUNTRY_CODE
  ): String? {
    if (requireEmail) {
      ContactValidator.validateEmail(email)?.let { return it }
    } else if (email.isNotBlank()) {
      ContactValidator.validateEmail(email)?.let { return it }
    }
    if (requirePhone) {
      ContactValidator.validatePhone(phone, countryCode)?.let { return it }
    } else if (phone.isNotBlank()) {
      ContactValidator.validatePhone(phone, countryCode)?.let { return it }
    }
    if (city.trim().length < 3) {
      return "Informe sua cidade e UF."
    }
    if (role == UserRole.PRESTADOR) {
      CpfValidator.validateProviderCpf(document)?.let { return it }
      if (requireProviderProfilePhoto && profilePhotoUri.isNullOrBlank()) {
        return if (isAdminManualRegistration) {
          "Anexe uma foto de perfil para concluir o cadastro do prestador."
        } else {
          "Tire sua foto de perfil na câmera ao vivo para concluir o cadastro do prestador."
        }
      }
      if (rgOrCnhAttachmentUri.isNullOrBlank()) {
        return "Anexe a foto ou PDF do seu RG ou CNH."
      }
      if (criminalRecordAttachmentUri.isNullOrBlank()) {
        return "Anexe a foto ou PDF da Certidão de Antecedentes Criminais."
      }
    } else {
      CpfValidator.validateClientCpf(document)?.let { return it }
    }
    return null
  }

  fun syncFirebaseAuthSession(account: GoogleAccountInfo?) {
    if (account == null) return
    if (isAuthenticated.value) return
    if (_isCredentialAuthLoading.value || _isGoogleAuthLoading.value) return
    if (_authStep.value == AuthFlowStep.GOOGLE_ROLE_COMPLETION &&
      _pendingGoogleAccount.value?.uid == account.uid
    ) {
      return
    }
    handleGoogleAuthSuccess(account = account)
  }

  fun handleGoogleAuthSuccess(
    account: GoogleAccountInfo,
    preferredRoleFromRegisterTab: UserRole? = null,
    registrationDraft: PendingRegistration? = null
  ) {
    handleAuthenticatedFirebaseAccount(
      account = account,
      providerLabel = "Google",
      preferredRoleFromRegisterTab = preferredRoleFromRegisterTab,
      registrationDraft = registrationDraft,
      requireGoogleDocumentStepIfIncomplete = true
    )
  }

  private fun completeLoginTransition(existing: com.example.model.User, providerLabel: String) {
    _isGoogleAuthLoading.value = false
    _isCredentialAuthLoading.value = false
    _pendingGoogleAccount.value = null
    _pendingRegistration.value = null
    _pendingLoginPhone.value = null
    _pendingPreferredRole.value = UserRole.CLIENTE
    _pendingGoogleDraft.value = null
    _authStep.value = AuthFlowStep.FORM
    if (existing.role == UserRole.ADMIN) {
      _selectedTab.value = MainTab.ADMIN_DOCUMENTOS
      syncAdminProvidersFromFirestore()
    }
    _snackBarMessage.value = "👋 Bem-vindo(a), ${existing.name} ($providerLabel)!"
  }

  private fun handleAuthenticatedFirebaseAccount(
    account: GoogleAccountInfo,
    providerLabel: String,
    preferredRoleFromRegisterTab: UserRole? = null,
    registrationDraft: PendingRegistration? = null,
    phoneOverride: String = "",
    requireGoogleDocumentStepIfIncomplete: Boolean = false,
    preferredProfileDocId: String? = null,
    authenticatedPassword: String? = null
  ) {
    _authErrorMessage.value = null
    _authSuccessMessage.value = null

    val existingByAccount = repository.loginWithGoogleAccount(
      uid = account.uid,
      email = account.email,
      displayName = account.displayName,
      photoUrl = account.photoUrl,
      preferredProfileDocId = preferredProfileDocId
    )
    val existing = existingByAccount ?: if (phoneOverride.filter { it.isDigit() }.length >= 10) {
      repository.loginWithVerifiedPhone(phoneOverride)
    } else {
      null
    }

    if (existing != null) {
      if (!authenticatedPassword.isNullOrBlank() && authenticatedPassword.length >= 6) {
        repository.recordLocalPasswordHash(existing.id, authenticatedPassword)
      }
      completeLoginTransition(existing, providerLabel)
      return
    }

    if ((requireGoogleDocumentStepIfIncomplete || registrationDraft == null) &&
      (registrationDraft == null || !registrationDraft.hasCompleteDocumentsForGoogle()) &&
      repository.isFirestoreInitialized
    ) {
      _isGoogleAuthLoading.value = true
      viewModelScope.launch {
        val restored = try {
          repository.restoreGoogleUserFromFirestoreIfPresent(
            uid = account.uid,
            email = account.email,
            displayName = account.displayName,
            photoUrl = account.photoUrl,
            preferredProfileDocId = preferredProfileDocId
          )
        } catch (_: Throwable) {
          null
        }
        _isGoogleAuthLoading.value = false
        _isCredentialAuthLoading.value = false
        if (restored != null) {
          if (!authenticatedPassword.isNullOrBlank() && authenticatedPassword.length >= 6) {
            repository.recordLocalPasswordHash(restored.id, authenticatedPassword)
          }
          completeLoginTransition(restored, providerLabel)
        } else {
          val resolvedRole = registrationDraft?.role ?: preferredRoleFromRegisterTab ?: UserRole.CLIENTE
          _pendingSocialProviderLabel.value = providerLabel
          _pendingLoginPhone.value = registrationDraft?.phone?.takeIf { it.isNotBlank() }
            ?: phoneOverride.takeIf { it.isNotBlank() }
          _pendingPreferredRole.value = resolvedRole
          _pendingGoogleDraft.value = registrationDraft
          _pendingGoogleAccount.value = account
          _authStep.value = AuthFlowStep.GOOGLE_ROLE_COMPLETION
        }
      }
      return
    }

    _isGoogleAuthLoading.value = false
    _isCredentialAuthLoading.value = false

    if (requireGoogleDocumentStepIfIncomplete &&
      (registrationDraft == null || !registrationDraft.hasCompleteDocumentsForGoogle())
    ) {
      val resolvedRole = registrationDraft?.role ?: preferredRoleFromRegisterTab ?: UserRole.CLIENTE
      _pendingSocialProviderLabel.value = providerLabel
      _pendingLoginPhone.value = registrationDraft?.phone?.takeIf { it.isNotBlank() }
        ?: phoneOverride.takeIf { it.isNotBlank() }
      _pendingPreferredRole.value = resolvedRole
      _pendingGoogleDraft.value = registrationDraft
      _pendingGoogleAccount.value = account
      _authStep.value = AuthFlowStep.GOOGLE_ROLE_COMPLETION
      return
    }

    if (registrationDraft != null) {
      val cleanPhone = registrationDraft.phone.ifBlank { phoneOverride }
      val resolvedDraftPhoto = registrationDraft.profilePhotoUri?.takeIf { it.isNotBlank() }
        ?: if (registrationDraft.role == UserRole.CLIENTE) account.photoUrl else null
      val created = repository.registerUserWithGoogle(
        uid = account.uid,
        email = FirebaseAuthHelper.normalizeEmail(account.email).ifBlank { FirebaseAuthHelper.normalizeEmail(registrationDraft.email) },
        displayName = registrationDraft.name.ifBlank { account.displayName },
        phone = cleanPhone,
        role = registrationDraft.role,
        category = registrationDraft.category,
        bio = registrationDraft.bio,
        city = registrationDraft.city,
        document = CpfValidator.formatCpf(registrationDraft.document),
        rgOrCnhNumber = registrationDraft.rgOrCnhNumber,
        criminalRecordNumber = registrationDraft.criminalRecordNumber,
        photoUrl = resolvedDraftPhoto,
        rgOrCnhFileName = registrationDraft.rgOrCnhFileName,
        rgOrCnhAttachmentUri = registrationDraft.rgOrCnhAttachmentUri,
        criminalRecordFileName = registrationDraft.criminalRecordFileName,
        criminalRecordAttachmentUri = registrationDraft.criminalRecordAttachmentUri,
        selfieFileName = registrationDraft.selfieFileName,
        selfieAttachmentUri = registrationDraft.selfieAttachmentUri
      )
      if (!authenticatedPassword.isNullOrBlank() && authenticatedPassword.length >= 6) {
        repository.recordLocalPasswordHash(created.id, authenticatedPassword)
        viewModelScope.launch {
          repository.persistUserAndDirectoryOnRegister(created, authenticatedPassword)
        }
      }
      _pendingGoogleAccount.value = null
      _pendingLoginPhone.value = null
      _pendingPreferredRole.value = UserRole.CLIENTE
      _pendingGoogleDraft.value = null
      _authStep.value = AuthFlowStep.FORM
      _snackBarMessage.value = if (created.role == UserRole.PRESTADOR) {
        "📋 Cadastro de Prestador via $providerLabel realizado! Documentos enviados para análise e bônus de R$ 50 creditado."
      } else {
        "🎉 Bem-vindo(a), ${created.name}! Conta conectada via $providerLabel."
      }
    } else if (preferredRoleFromRegisterTab != null) {
      val created = repository.registerUserWithGoogle(
        uid = account.uid,
        email = FirebaseAuthHelper.normalizeEmail(account.email),
        displayName = account.displayName,
        phone = phoneOverride,
        role = preferredRoleFromRegisterTab,
        photoUrl = account.photoUrl
      )
      if (!authenticatedPassword.isNullOrBlank() && authenticatedPassword.length >= 6) {
        repository.recordLocalPasswordHash(created.id, authenticatedPassword)
        viewModelScope.launch {
          repository.persistUserAndDirectoryOnRegister(created, authenticatedPassword)
        }
      }
      _pendingGoogleAccount.value = null
      _pendingLoginPhone.value = null
      _pendingPreferredRole.value = UserRole.CLIENTE
      _pendingGoogleDraft.value = null
      _authStep.value = AuthFlowStep.FORM
      _snackBarMessage.value = "🎉 Bem-vindo(a), ${created.name}! Conta conectada via $providerLabel."
    } else {
      _pendingSocialProviderLabel.value = providerLabel
      _pendingLoginPhone.value = phoneOverride.takeIf { it.isNotBlank() }
      _pendingPreferredRole.value = UserRole.CLIENTE
      _pendingGoogleDraft.value = null
      _pendingGoogleAccount.value = account
      _authStep.value = AuthFlowStep.GOOGLE_ROLE_COMPLETION
    }
  }

  fun completeGoogleRegistration(
    role: UserRole,
    phone: String,
    city: String,
    document: String,
    category: String,
    bio: String,
    rgOrCnh: String,
    criminalRecord: String,
    rgOrCnhFileName: String = "",
    rgOrCnhAttachmentUri: String? = null,
    criminalRecordFileName: String = "",
    criminalRecordAttachmentUri: String? = null,
    selfieFileName: String = "",
    selfieAttachmentUri: String? = null,
    profilePhotoUri: String? = null
  ) {
    val account = _pendingGoogleAccount.value ?: return
    val providerLabel = _pendingSocialProviderLabel.value
    val effectivePhone = phone.ifBlank { _pendingLoginPhone.value.orEmpty() }
    val resolvedProviderPhoto = profilePhotoUri?.takeIf { it.isNotBlank() }
    val validationError = validateRegistrationDocuments(
      role = role,
      email = account.email,
      phone = effectivePhone,
      city = city,
      document = document,
      rgOrCnh = rgOrCnh,
      rgOrCnhAttachmentUri = rgOrCnhAttachmentUri,
      criminalRecord = criminalRecord,
      criminalRecordAttachmentUri = criminalRecordAttachmentUri,
      selfieAttachmentUri = selfieAttachmentUri,
      profilePhotoUri = resolvedProviderPhoto,
      requireEmail = false,
      requirePhone = true,
      requireProviderProfilePhoto = role == UserRole.PRESTADOR
    )
    if (validationError != null) {
      _authErrorMessage.value = validationError
      return
    }
    val finalPhotoUrl = resolvedProviderPhoto
      ?: if (role == UserRole.CLIENTE) account.photoUrl else null
    val created = repository.registerUserWithGoogle(
      uid = account.uid,
      email = account.email,
      displayName = account.displayName,
      phone = effectivePhone,
      role = role,
      category = category,
      bio = bio,
      city = city,
      document = CpfValidator.formatCpf(document),
      rgOrCnhNumber = rgOrCnh,
      criminalRecordNumber = criminalRecord,
      photoUrl = finalPhotoUrl,
      rgOrCnhFileName = rgOrCnhFileName,
      rgOrCnhAttachmentUri = rgOrCnhAttachmentUri,
      criminalRecordFileName = criminalRecordFileName,
      criminalRecordAttachmentUri = criminalRecordAttachmentUri,
      selfieFileName = selfieFileName,
      selfieAttachmentUri = selfieAttachmentUri
    )
    _pendingGoogleAccount.value = null
    _pendingLoginPhone.value = null
    _pendingPreferredRole.value = UserRole.CLIENTE
    _pendingGoogleDraft.value = null
    _authStep.value = AuthFlowStep.FORM
    _authErrorMessage.value = null
    _authSuccessMessage.value = null
    _snackBarMessage.value = if (created.role == UserRole.PRESTADOR) {
      "📋 Cadastro de Prestador via $providerLabel concluído! Seus documentos estão em análise."
    } else {
      "🎉 Bem-vindo(a), ${created.name}! Acesso liberado via $providerLabel."
    }
  }

  fun signInWithEmailAndPassword(email: String, password: String) {
    val cleanEmail = FirebaseAuthHelper.normalizeEmail(email)
    _authErrorMessage.value = null
    _authSuccessMessage.value = null

    ContactValidator.validateEmail(cleanEmail)?.let { err ->
      _authErrorMessage.value = err
      return
    }
    if (password.length < 6) {
      _authErrorMessage.value = "A senha deve ter pelo menos 6 caracteres."
      return
    }

    _isCredentialAuthLoading.value = true
    viewModelScope.launch {
      val dirEntry = try {
        repository.lookupCrossDeviceLoginByEmail(cleanEmail)
      } catch (_: Throwable) {
        null
      }
      val extraCandidates = listOfNotNull(
        dirEntry?.authEmail,
        dirEntry?.aliasAuthEmail
      )

      when (
        val result = FirebaseAuthHelper.signInWithEmailAndPassword(
          email = cleanEmail,
          password = password,
          extraCandidateEmails = extraCandidates
        )
      ) {
        is FirebaseCredentialAuthResult.Success -> {
          handleAuthenticatedFirebaseAccount(
            account = result.account,
            providerLabel = result.providerLabel,
            preferredProfileDocId = dirEntry?.uid,
            authenticatedPassword = password
          )
        }
        is FirebaseCredentialAuthResult.Error -> {
          val localMatchedUser = repository.verifyAdminManagedPasswordByEmail(cleanEmail, password)
          val dirVerifierMatches = dirEntry != null &&
            !dirEntry.passwordVerifier.isNullOrBlank() &&
            dirEntry.passwordVerifier == FirebaseAuthHelper.hashDirectoryPassword(dirEntry.uid, password)

          if (dirVerifierMatches || localMatchedUser != null) {
            val migrationEmail = dirEntry?.authEmail?.takeIf { it.isNotBlank() } ?: cleanEmail
            val migrationName = localMatchedUser?.name?.takeIf { it.isNotBlank() }
              ?: cleanEmail.substringBefore("@")
            val migrated = FirebaseAuthHelper.registerWithEmailAndPassword(
              name = migrationName,
              email = migrationEmail,
              password = password
            )
            if (migrated is FirebaseCredentialAuthResult.Success) {
              handleAuthenticatedFirebaseAccount(
                account = migrated.account,
                providerLabel = "E-mail e senha",
                preferredProfileDocId = dirEntry?.uid ?: localMatchedUser?.firebaseUid,
                authenticatedPassword = password
              )
            } else if (localMatchedUser != null) {
              repository.authenticateExistingUserLocally(localMatchedUser.id)
              repository.recordLocalPasswordHash(localMatchedUser.id, password)
              completeLoginTransition(localMatchedUser, "E-mail e senha")
            } else {
              _isCredentialAuthLoading.value = false
              _authErrorMessage.value = "E-mail ou senha incorretos. Verifique os dados informados."
            }
          } else {
            _isCredentialAuthLoading.value = false
            val existingUser = repository.findUserByEmail(cleanEmail)
            _authErrorMessage.value = when {
              dirEntry != null || (existingUser != null && repository.hasLocalPasswordHash(existingUser.id)) -> {
                "E-mail ou senha incorretos. Verifique os dados informados."
              }
              existingUser != null && result.isProviderUnavailable -> {
                "Esta conta ainda não possui senha definida. Entre com Google ou peça ao administrador para definir sua senha."
              }
              existingUser == null && result.isProviderUnavailable -> {
                "Nenhuma conta encontrada com este e-mail. Crie uma conta na aba 'Criar Conta'."
              }
              else -> result.message
            }
          }
        }
        FirebaseCredentialAuthResult.Cancelled -> {
          _isCredentialAuthLoading.value = false
        }
      }
    }
  }

  fun signInWithPhoneAndPassword(
    phone: String,
    countryCode: String = FirebasePhoneAuthManager.DEFAULT_COUNTRY_CODE,
    password: String
  ) {
    _authErrorMessage.value = null
    _authSuccessMessage.value = null

    val knownUser = repository.findUserByPhone(phone, countryCode)
    if (knownUser == null) {
      ContactValidator.validatePhone(phone, countryCode)?.let { err ->
        _authErrorMessage.value = err
        return
      }
    }
    if (password.length < 6) {
      _authErrorMessage.value = "A senha deve ter pelo menos 6 caracteres."
      return
    }

    _isCredentialAuthLoading.value = true
    val formattedE164 = FirebasePhoneAuthManager(authProvider = { null }).formatToE164(phone, countryCode)
    viewModelScope.launch {
      val dirEntry = try {
        repository.lookupCrossDeviceLoginByPhone(phone, countryCode)
          ?: repository.lookupCrossDeviceLoginByPhone(formattedE164, countryCode)
      } catch (_: Throwable) {
        null
      }
      val resolvedKnownEmail = dirEntry?.authEmail?.takeIf { it.isNotBlank() } ?: knownUser?.email
      val extraCandidates = listOfNotNull(
        dirEntry?.authEmail,
        dirEntry?.aliasAuthEmail,
        knownUser?.email
      )

      when (
        val result = FirebaseAuthHelper.signInWithPhoneAndPassword(
          phone = phone,
          countryCode = countryCode,
          password = password,
          knownEmailForPhone = resolvedKnownEmail,
          extraCandidateEmails = extraCandidates
        )
      ) {
        is FirebaseCredentialAuthResult.Success -> {
          handleAuthenticatedFirebaseAccount(
            account = result.account,
            providerLabel = result.providerLabel,
            phoneOverride = formattedE164,
            preferredProfileDocId = dirEntry?.uid,
            authenticatedPassword = password
          )
        }
        is FirebaseCredentialAuthResult.Error -> {
          val localMatchedUser = repository.verifyAdminManagedPasswordByPhone(phone, password, countryCode)
            ?: repository.verifyAdminManagedPasswordByPhone(formattedE164, password, countryCode)
          val dirVerifierMatches = dirEntry != null &&
            !dirEntry.passwordVerifier.isNullOrBlank() &&
            dirEntry.passwordVerifier == FirebaseAuthHelper.hashDirectoryPassword(dirEntry.uid, password)

          if (dirVerifierMatches || localMatchedUser != null) {
            val migrationEmail = dirEntry?.authEmail?.takeIf { it.isNotBlank() }
              ?: localMatchedUser?.email?.takeIf { ContactValidator.isValidEmail(it) }
            val migrationName = localMatchedUser?.name?.takeIf { it.isNotBlank() } ?: "Usuário ResolveAi"
            val migrated = if (!migrationEmail.isNullOrBlank()) {
              FirebaseAuthHelper.registerWithEmailAndPassword(
                name = migrationName,
                email = migrationEmail,
                password = password
              )
            } else {
              FirebaseAuthHelper.registerWithPhoneAndPassword(
                name = migrationName,
                phone = formattedE164,
                countryCode = countryCode,
                password = password
              )
            }
            if (migrated is FirebaseCredentialAuthResult.Success) {
              handleAuthenticatedFirebaseAccount(
                account = migrated.account,
                providerLabel = "Telefone e senha",
                phoneOverride = formattedE164,
                preferredProfileDocId = dirEntry?.uid ?: localMatchedUser?.firebaseUid,
                authenticatedPassword = password
              )
            } else if (localMatchedUser != null) {
              repository.authenticateExistingUserLocally(localMatchedUser.id)
              repository.recordLocalPasswordHash(localMatchedUser.id, password)
              completeLoginTransition(localMatchedUser, "Telefone e senha")
            } else {
              _isCredentialAuthLoading.value = false
              _authErrorMessage.value = "Telefone ou senha incorretos. Verifique os dados informados."
            }
          } else {
            _isCredentialAuthLoading.value = false
            val existingUser = knownUser ?: repository.findUserByPhone(formattedE164, countryCode)
            _authErrorMessage.value = when {
              dirEntry != null || (existingUser != null && repository.hasLocalPasswordHash(existingUser.id)) -> {
                "Telefone ou senha incorretos. Verifique os dados informados."
              }
              existingUser != null && result.isProviderUnavailable -> {
                "Esta conta ainda não possui senha definida. Entre com Google ou peça ao administrador para definir sua senha."
              }
              existingUser == null && result.isProviderUnavailable -> {
                "Nenhuma conta encontrada com este telefone. Crie uma conta na aba 'Criar Conta'."
              }
              else -> result.message
            }
          }
        }
        FirebaseCredentialAuthResult.Cancelled -> {
          _isCredentialAuthLoading.value = false
        }
      }
    }
  }

  fun registerWithPassword(
    registration: PendingRegistration,
    countryCode: String = FirebasePhoneAuthManager.DEFAULT_COUNTRY_CODE,
    password: String
  ) {
    _authErrorMessage.value = null
    _authSuccessMessage.value = null

    val cleanName = registration.name.trim()
    val cleanEmail = FirebaseAuthHelper.normalizeEmail(registration.email)
    val hasValidEmail = ContactValidator.isValidEmail(cleanEmail)
    val hasValidPhone = ContactValidator.isValidPhone(registration.phone, countryCode)

    if (cleanName.length < 2) {
      _authErrorMessage.value = "Informe seu nome completo."
      return
    }
    ContactValidator.validateEmail(cleanEmail)?.let {
      _authErrorMessage.value = it
      return
    }
    ContactValidator.validatePhone(registration.phone, countryCode)?.let {
      _authErrorMessage.value = it
      return
    }
    if (password.length < 6) {
      _authErrorMessage.value = "A senha deve ter pelo menos 6 caracteres."
      return
    }
    val docsError = validateRegistrationDocuments(
      role = registration.role,
      email = cleanEmail,
      phone = registration.phone,
      city = registration.city,
      document = registration.document,
      rgOrCnh = registration.rgOrCnhNumber,
      rgOrCnhAttachmentUri = registration.rgOrCnhAttachmentUri,
      criminalRecord = registration.criminalRecordNumber,
      criminalRecordAttachmentUri = registration.criminalRecordAttachmentUri,
      selfieAttachmentUri = registration.selfieAttachmentUri,
      profilePhotoUri = registration.profilePhotoUri,
      requireEmail = true,
      requirePhone = true,
      requireProviderProfilePhoto = registration.role == UserRole.PRESTADOR,
      countryCode = countryCode
    )
    if (docsError != null) {
      _authErrorMessage.value = docsError
      return
    }

    val formattedPhone = if (hasValidPhone) {
      FirebasePhoneAuthManager(authProvider = { null }).formatToE164(registration.phone, countryCode)
    } else {
      registration.phone.trim()
    }
    if (repository.findUserByEmail(cleanEmail) != null) {
      _authErrorMessage.value = "Este e-mail já possui cadastro no aplicativo. Tente entrar na aba 'Entrar'."
      return
    }
    if (repository.findUserByPhone(formattedPhone, countryCode) != null) {
      _authErrorMessage.value = "Este telefone já possui cadastro no aplicativo. Tente entrar na aba 'Entrar'."
      return
    }

    val preparedDraft = registration.copy(
      name = cleanName,
      email = cleanEmail,
      phone = formattedPhone,
      password = password
    )

    _isCredentialAuthLoading.value = true
    viewModelScope.launch {
      val result = if (hasValidEmail) {
        FirebaseAuthHelper.registerWithEmailAndPassword(
          name = cleanName,
          email = cleanEmail,
          password = password
        )
      } else {
        FirebaseAuthHelper.registerWithPhoneAndPassword(
          name = cleanName,
          phone = formattedPhone,
          countryCode = countryCode,
          password = password
        )
      }

      when (result) {
        is FirebaseCredentialAuthResult.Success -> {
          handleAuthenticatedFirebaseAccount(
            account = result.account,
            providerLabel = result.providerLabel,
            preferredRoleFromRegisterTab = preparedDraft.role,
            registrationDraft = preparedDraft,
            phoneOverride = formattedPhone,
            authenticatedPassword = password
          )
        }
        is FirebaseCredentialAuthResult.Error -> {
          if (result.isProviderUnavailable) {
            completeRegistration(
              pending = preparedDraft.copy(document = CpfValidator.formatCpf(preparedDraft.document)),
              originLabel = "E-mail e senha"
            )
            repository.recordLocalPasswordHash(repository.getCurrentUser().id, password)
            _isCredentialAuthLoading.value = false
          } else {
            _isCredentialAuthLoading.value = false
            _authErrorMessage.value = result.message
          }
        }
        FirebaseCredentialAuthResult.Cancelled -> {
          _isCredentialAuthLoading.value = false
        }
      }
    }
  }

  fun resetPasswordResetState() {
    _passwordResetState.value = PasswordResetUiState.Idle
  }

  fun sendPasswordReset(email: String) {
    val cleanEmail = email.trim()
    _authErrorMessage.value = null
    _authSuccessMessage.value = null
    _passwordResetState.value = PasswordResetUiState.Loading
    viewModelScope.launch {
      val result = FirebaseAuthHelper.sendPasswordReset(cleanEmail)
      result.onSuccess { msg ->
        _passwordResetState.value = PasswordResetUiState.Success(email = cleanEmail, message = msg)
        _authSuccessMessage.value = msg
        _snackBarMessage.value = msg
      }.onFailure { err ->
        val errMsg = err.message ?: "Não foi possível enviar o e-mail de redefinição."
        _passwordResetState.value = PasswordResetUiState.Error(errMsg)
      }
    }
  }

  fun registerUserDirectly(registration: PendingRegistration) {
    val validationError = validateRegistrationDocuments(
      role = registration.role,
      email = registration.email,
      phone = registration.phone,
      city = registration.city,
      document = registration.document,
      rgOrCnh = registration.rgOrCnhNumber,
      rgOrCnhAttachmentUri = registration.rgOrCnhAttachmentUri,
      criminalRecord = registration.criminalRecordNumber,
      criminalRecordAttachmentUri = registration.criminalRecordAttachmentUri,
      selfieAttachmentUri = registration.selfieAttachmentUri,
      profilePhotoUri = registration.profilePhotoUri,
      requireEmail = true,
      requirePhone = true,
      requireProviderProfilePhoto = registration.role == UserRole.PRESTADOR,
      isAdminManualRegistration = currentUser.value.role == UserRole.ADMIN
    )
    if (validationError != null) {
      _snackBarMessage.value = "⚠️ $validationError"
      return
    }

    val normalizedDocument = if (registration.document.filter { it.isDigit() }.length == 11) {
      CpfValidator.formatCpf(registration.document)
    } else {
      registration.document.trim()
    }

    if (currentUser.value.role == UserRole.ADMIN) {
      val docs = if (registration.role == UserRole.PRESTADOR) {
        ProviderDocuments(
          rgOrCnhNumber = registration.rgOrCnhNumber.trim().ifBlank { registration.rgOrCnhFileName.ifBlank { "RG/CNH anexado" } },
          rgOrCnhFileName = registration.rgOrCnhFileName,
          rgOrCnhAttachmentUri = registration.rgOrCnhAttachmentUri?.takeIf { it.isNotBlank() },
          criminalRecordCertNumber = registration.criminalRecordNumber.trim().ifBlank { registration.criminalRecordFileName.ifBlank { "Certidão anexada" } },
          criminalRecordFileName = registration.criminalRecordFileName,
          criminalRecordAttachmentUri = registration.criminalRecordAttachmentUri?.takeIf { it.isNotBlank() },
          selfieFileName = registration.selfieFileName,
          selfieAttachmentUri = registration.selfieAttachmentUri?.takeIf { it.isNotBlank() }
        )
      } else null
      val result = repository.adminCreateUser(
        name = registration.name,
        email = registration.email,
        phone = registration.phone,
        role = registration.role,
        category = registration.category,
        bio = registration.bio,
        city = registration.city,
        document = normalizedDocument,
        initialWalletBonus = registration.initialWalletBonus,
        photoUrl = registration.profilePhotoUri?.takeIf { it.isNotBlank() },
        documents = docs
      )
      result.onSuccess { created ->
        if (registration.password.length >= 6) {
          repository.adminSetUserPassword(created.id, registration.password)
          viewModelScope.launch {
            repository.provisionAndLinkAdminAuthCredential(created.id, registration.password)
          }
        }
        _showRegisterDialog.value = false
        _snackBarMessage.value = "✅ Usuário ${created.name} (${created.role.label}) cadastrado e sincronizado no Firebase!"
      }.onFailure { err ->
        _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível cadastrar o usuário."}"
      }
    } else {
      completeRegistration(
        pending = registration.copy(document = normalizedDocument),
        originLabel = "Cadastro Administrativo"
      )
    }
  }

  fun logout(context: Context? = null) {
    val resetState = {
      repository.logout()
      _authStep.value = AuthFlowStep.FORM
      _authMode.value = AuthMode.LOGIN
      _pendingRegistration.value = null
      _pendingLoginPhone.value = null
      _pendingGoogleAccount.value = null
      _pendingPreferredRole.value = UserRole.CLIENTE
      _pendingGoogleDraft.value = null
      _pendingSocialProviderLabel.value = "Google"
      _isGoogleAuthLoading.value = false
      _isCredentialAuthLoading.value = false
      _authErrorMessage.value = null
      _authSuccessMessage.value = null
      _passwordResetState.value = PasswordResetUiState.Idle
      _showChangePasswordDialog.value = false
      _changePasswordState.value = ChangePasswordUiState.Idle
      resetPixRecharge()
      _showRechargeDialog.value = false
      _selectedServiceId.value = null
      _selectedTab.value = MainTab.SERVICOS
      _snackBarMessage.value = "Sessão encerrada. Faça login para acessar o aplicativo."
    }

    if (context != null) {
      GoogleAuthManager.signOut(
        context = context,
        scope = viewModelScope,
        onSignOutComplete = resetState
      )
    } else {
      resetState()
    }
  }
  val currentUser: StateFlow<User> = combine(users, currentUserId) { userList, id ->
    userList.find { it.id == id } ?: userList.first()
  }.stateIn(
    viewModelScope,
    SharingStarted.Eagerly,
    repository.getCurrentUser()
  )

  val adminUserPasswords: StateFlow<Map<String, String>> = combine(
    currentUser,
    repository.userSavedPasswords
  ) { user, savedPasswords ->
    if (user.role == UserRole.ADMIN) savedPasswords else emptyMap()
  }.stateIn(
    viewModelScope,
    SharingStarted.Eagerly,
    emptyMap()
  )

  // Current main tab
  private val _selectedTab = MutableStateFlow(MainTab.SERVICOS)
  val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

  // Selected service for detail view
  private val _selectedServiceId = MutableStateFlow<String?>(null)
  val selectedServiceId: StateFlow<String?> = _selectedServiceId.asStateFlow()

  // Selected chat service
  private val _selectedChatServiceId = MutableStateFlow<String?>("srv_1")
  val selectedChatServiceId: StateFlow<String?> = _selectedChatServiceId.asStateFlow()

  // Dialogs & Modals
  private val _showCreateServiceDialog = MutableStateFlow(false)
  val showCreateServiceDialog: StateFlow<Boolean> = _showCreateServiceDialog.asStateFlow()

  private val _showRechargeDialog = MutableStateFlow(false)
  val showRechargeDialog: StateFlow<Boolean> = _showRechargeDialog.asStateFlow()

  private val _showUserSwitcherDialog = MutableStateFlow(false)
  val showUserSwitcherDialog: StateFlow<Boolean> = _showUserSwitcherDialog.asStateFlow()

  private val _showEditProfileDialog = MutableStateFlow(false)
  val showEditProfileDialog: StateFlow<Boolean> = _showEditProfileDialog.asStateFlow()

  private val _profileFocusNonce = MutableStateFlow(0)
  val profileFocusNonce: StateFlow<Int> = _profileFocusNonce.asStateFlow()

  private val _showChangePasswordDialog = MutableStateFlow(false)
  val showChangePasswordDialog: StateFlow<Boolean> = _showChangePasswordDialog.asStateFlow()

  private val _changePasswordState = MutableStateFlow<ChangePasswordUiState>(ChangePasswordUiState.Idle)
  val changePasswordState: StateFlow<ChangePasswordUiState> = _changePasswordState.asStateFlow()

  fun setShowChangePasswordDialog(show: Boolean) {
    _showChangePasswordDialog.value = show
    if (!show) {
      _changePasswordState.value = ChangePasswordUiState.Idle
    }
  }

  fun resetChangePasswordState() {
    _changePasswordState.value = ChangePasswordUiState.Idle
  }

  fun changeOwnPassword(
    currentPassword: String,
    newPassword: String,
    confirmNewPassword: String
  ) {
    if (currentPassword.isBlank()) {
      _changePasswordState.value = ChangePasswordUiState.Error("Informe sua senha atual.")
      return
    }
    if (newPassword.length < 6) {
      _changePasswordState.value = ChangePasswordUiState.Error("A nova senha deve ter no mínimo 6 caracteres.")
      return
    }
    if (newPassword != confirmNewPassword) {
      _changePasswordState.value = ChangePasswordUiState.Error("As novas senhas informadas não coincidem.")
      return
    }
    if (newPassword == currentPassword) {
      _changePasswordState.value = ChangePasswordUiState.Error("A nova senha deve ser diferente da senha atual.")
      return
    }

    val activeUser = repository.getCurrentUser()
    val expectedIds = setOfNotNull(activeUser.id, activeUser.firebaseUid?.takeIf { it.isNotBlank() })
    _changePasswordState.value = ChangePasswordUiState.Loading

    viewModelScope.launch {
      val fbResult = FirebaseAuthHelper.reauthenticateAndChangePassword(
        currentPassword = currentPassword,
        newPassword = newPassword,
        expectedUserIds = expectedIds
      )

      val localResult = if (fbResult.isSuccess) {
        val updatedInFirebase = fbResult.getOrDefault(false)
        val res = repository.changeCurrentUserPassword(
          currentPassword = currentPassword,
          newPassword = newPassword,
          verifiedByFirebaseAuth = updatedInFirebase
        )
        if (res.isSuccess && !updatedInFirebase && ContactValidator.isValidEmail(activeUser.email)) {
          FirebaseAuthHelper.linkEmailPasswordCredential(activeUser.email, newPassword)
        }
        res
      } else {
        if (repository.verifyCurrentUserLocalPassword(currentPassword)) {
          val updated = repository.changeCurrentUserPassword(
            currentPassword = currentPassword,
            newPassword = newPassword,
            verifiedByFirebaseAuth = false
          )
          if (updated.isSuccess) {
            val fbUpdated = FirebaseAuthHelper.updateCurrentUserPassword(newPassword)
            if (fbUpdated.isFailure && ContactValidator.isValidEmail(activeUser.email)) {
              FirebaseAuthHelper.linkEmailPasswordCredential(activeUser.email, newPassword)
            }
          }
          updated
        } else {
          Result.failure(
            fbResult.exceptionOrNull()
              ?: IllegalArgumentException("Senha atual incorreta. Verifique e tente novamente.")
          )
        }
      }

      localResult.onSuccess { updatedUser ->
        repository.syncLoginDirectoryForUserAwait(updatedUser, rawPassword = newPassword)
        val successMsg = "🔑 Senha alterada com sucesso!"
        _changePasswordState.value = ChangePasswordUiState.Success(successMsg)
        _showChangePasswordDialog.value = false
        _snackBarMessage.value = successMsg
      }.onFailure { err ->
        _changePasswordState.value = ChangePasswordUiState.Error(
          err.message ?: "Não foi possível alterar a senha."
        )
      }
    }
  }

  // Admin Register Dialog State
  private val _showRegisterDialog = MutableStateFlow(false)
  val showRegisterDialog: StateFlow<Boolean> = _showRegisterDialog.asStateFlow()

  private val _pendingRegistration = MutableStateFlow<PendingRegistration?>(null)
  val pendingRegistration: StateFlow<PendingRegistration?> = _pendingRegistration.asStateFlow()

  // Rating Dialog State
  private val _ratingServiceTarget = MutableStateFlow<ServiceRequest?>(null)
  val ratingServiceTarget: StateFlow<ServiceRequest?> = _ratingServiceTarget.asStateFlow()

  private val _ratingInitialStars = MutableStateFlow(5.0f)
  val ratingInitialStars: StateFlow<Float> = _ratingInitialStars.asStateFlow()

  // Edit proposed budget dialog state
  private val _editingBudgetService = MutableStateFlow<ServiceRequest?>(null)
  val editingBudgetService: StateFlow<ServiceRequest?> = _editingBudgetService.asStateFlow()

  // Admin Review Dialog State
  private val _showAdminReviewDialog = MutableStateFlow(false)
  val showAdminReviewDialog: StateFlow<Boolean> = _showAdminReviewDialog.asStateFlow()

  // Insufficient Balance Alert state
  private val _insufficientBalanceData = MutableStateFlow<InsufficientBalanceException?>(null)
  val insufficientBalanceData: StateFlow<InsufficientBalanceException?> = _insufficientBalanceData.asStateFlow()

  // Success Snackbar/Banner
  private val _snackBarMessage = MutableStateFlow<String?>(null)
  val snackBarMessage: StateFlow<String?> = _snackBarMessage.asStateFlow()

  // Search and Category filters
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedCategoryFilter = MutableStateFlow("Serviços")
  val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

  private val _urgencyFilter = MutableStateFlow("Todas")
  val urgencyFilter: StateFlow<String> = _urgencyFilter.asStateFlow()

  fun setTab(tab: MainTab) {
    _selectedTab.value = tab
    if (tab == MainTab.CARTEIRA_PERFIL && repository.getCurrentUser().role == UserRole.PRESTADOR) {
      syncWalletTransactionsFromFirestore()
    }
    if (tab == MainTab.CHAT) {
      repository.syncChatListenersForEligibleServices()
    }
  }

  fun syncWalletTransactionsFromFirestore() {
    if (repository.getCurrentUser().role != UserRole.PRESTADOR) return
    viewModelScope.launch {
      _isWalletHistoryLoading.value = true
      repository.syncWalletTransactionsFromFirestore()
      _isWalletHistoryLoading.value = false
    }
  }

  fun selectService(serviceId: String?) {
    _selectedServiceId.value = serviceId
    repository.observeSelectedServiceProposals(serviceId)
    if (!serviceId.isNullOrBlank()) {
      viewModelScope.launch {
        repository.syncProposalsForService(serviceId)
      }
      if (currentUser.value.role == UserRole.ADMIN) {
        repository.ensureChatSyncForService(serviceId)
      }
    }
  }

  fun openChatForService(serviceId: String) {
    val srv = repository.services.value.find { it.id == serviceId }
    if (srv != null && !repository.canUserAccessServiceChat(srv)) {
      _snackBarMessage.value = "🔒 O chat privado só é liberado exclusivamente com o prestador aceito pelo cliente."
      return
    }
    _selectedChatServiceId.value = serviceId
    _selectedTab.value = MainTab.CHAT
    repository.ensureChatSyncForService(serviceId)
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setCategoryFilter(category: String) {
    _selectedCategoryFilter.value = category
  }

  fun setUrgencyFilter(urgency: String) {
    _urgencyFilter.value = urgency
  }

  fun switchUser(userId: String) {
    repository.switchUser(userId)
    _showUserSwitcherDialog.value = false
    val newUser = repository.getCurrentUser()
    if (newUser.role == UserRole.ADMIN) {
      _selectedTab.value = MainTab.ADMIN_DOCUMENTOS
      syncAdminProvidersFromFirestore()
    } else if (_selectedTab.value == MainTab.ADMIN_DOCUMENTOS ||
      _selectedTab.value == MainTab.ADMIN_USUARIOS ||
      _selectedTab.value == MainTab.ADMIN_CATALOG_EDICOES ||
      (_selectedTab.value == MainTab.HISTORICO_PEDIDOS && newUser.role != UserRole.PRESTADOR)
    ) {
      _selectedTab.value = MainTab.SERVICOS
    }
    _snackBarMessage.value = "Conectado como ${newUser.name} (${newUser.role.label})"
  }

  fun setShowCreateService(show: Boolean) {
    _showCreateServiceDialog.value = show
  }

  fun setShowRecharge(show: Boolean) {
    _showRechargeDialog.value = show
    if (!show) {
      resetPixRecharge()
    }
  }

  fun setShowUserSwitcher(show: Boolean) {
    _showUserSwitcherDialog.value = show && currentUser.value.role == UserRole.ADMIN
  }

  fun setShowEditProfile(show: Boolean) {
    _showEditProfileDialog.value = show
  }

  fun openProfileData() {
    _selectedServiceId.value = null
    _selectedTab.value = MainTab.CARTEIRA_PERFIL
    _profileFocusNonce.value = _profileFocusNonce.value + 1
  }

  fun setShowRegister(show: Boolean) {
    _showRegisterDialog.value = show && currentUser.value.role == UserRole.ADMIN
  }

  fun dismissInsufficientBalance() {
    _insufficientBalanceData.value = null
  }

  fun dismissSnackbar() {
    _snackBarMessage.value = null
  }

  fun openEditBudgetDialog(service: ServiceRequest) {
    _editingBudgetService.value = service
  }

  fun dismissEditBudgetDialog() {
    _editingBudgetService.value = null
  }

  fun updateServiceBudget(serviceId: String, newBudget: Double) {
    val success = repository.updateServiceBudget(serviceId, newBudget)
    _editingBudgetService.value = null
    if (success) {
      _snackBarMessage.value = "💰 Valor proposto atualizado para R$ ${String.format("%.2f", newBudget)} com sucesso!"
    } else {
      _snackBarMessage.value = "Não foi possível atualizar o valor deste serviço."
    }
  }

  fun setShowAdminReviewDialog(show: Boolean) {
    _showAdminReviewDialog.value = show
  }


  private val _detectedLocalCity = MutableStateFlow("")
  val detectedLocalCity: StateFlow<String> = _detectedLocalCity.asStateFlow()

  fun updateDetectedLocalCity(city: String) {
    val clean = city.trim()
    if (clean.isNotBlank() && clean != _detectedLocalCity.value) {
      _detectedLocalCity.value = clean
    }
  }

  private fun effectiveRegionalFallbackCity(): String {
    return _detectedLocalCity.value.trim().ifBlank { currentUser.value.city }
  }

  fun resolveRegionalPriceEstimate(
    category: String,
    locationText: String
  ): RegionalPriceEstimate {
    return repository.getRegionalPriceEstimate(
      category = category,
      locationText = locationText,
      fallbackCity = effectiveRegionalFallbackCity()
    )
  }

  suspend fun refreshDailyRegionalPriceEstimate(
    category: String,
    locationText: String
  ): RegionalPriceEstimate {
    return repository.refreshDailyRegionalPriceEstimate(
      category = category,
      locationText = locationText,
      fallbackCity = effectiveRegionalFallbackCity()
    )
  }

  // Create Service
  fun createService(
    title: String,
    category: String,
    description: String,
    location: String,
    proposedBudget: Double,
    urgency: String,
    attachmentPhotoUri: String? = null,
    attachmentPhotoName: String? = null,
    attachmentVideoUri: String? = null,
    attachmentVideoName: String? = null,
    attachmentVideoThumbnailUri: String? = null,
    attachmentVideoDurationMs: Long? = null
  ) {
    val created = repository.createServiceRequest(
      title = title,
      category = category,
      description = description,
      location = location,
      proposedBudget = proposedBudget,
      urgency = urgency,
      attachmentPhotoUri = attachmentPhotoUri,
      attachmentPhotoName = attachmentPhotoName,
      attachmentVideoUri = attachmentVideoUri,
      attachmentVideoName = attachmentVideoName,
      attachmentVideoThumbnailUri = attachmentVideoThumbnailUri,
      attachmentVideoDurationMs = attachmentVideoDurationMs
    )
    _showCreateServiceDialog.value = false
    _snackBarMessage.value = "Serviço solicitado com sucesso! Prestadores enviarão propostas."
    _selectedServiceId.value = created.id
  }

  // Submit Proposal
  fun submitProposal(
    serviceId: String,
    proposedPrice: Double,
    estimatedDays: Int,
    description: String
  ) {
    repository.submitProposal(
      serviceId = serviceId,
      proposedPrice = proposedPrice,
      estimatedDays = estimatedDays,
      description = description
    )
    _snackBarMessage.value = "Proposta de R$ ${String.format("%.2f", proposedPrice)} enviada ao cliente!"
  }

  // Send Counter Offer
  fun sendCounterOffer(
    proposalId: String,
    newPrice: Double,
    note: String
  ) {
    repository.sendCounterOffer(proposalId, newPrice, note)
    _snackBarMessage.value = "Contraproposta de R$ ${String.format("%.2f", newPrice)} enviada com sucesso!"
  }

  // Accept Proposal (debitting 10% fee from provider wallet silently for the client)
  fun acceptProposal(proposalId: String) {
    val acceptedServiceId = repository.proposals.value.find { it.id == proposalId }?.serviceId
    val result = repository.acceptProposalAndDebitFee(proposalId)
    result.onSuccess { _ ->
      if (!acceptedServiceId.isNullOrBlank()) {
        _selectedChatServiceId.value = acceptedServiceId
      }
      _selectedServiceId.value = null
      _selectedTab.value = MainTab.NEGOCIACOES
      _snackBarMessage.value = "🎉 Proposta aceita! Pedido movido para Minhas Negociações com status Em Andamento."
      _insufficientBalanceData.value = null
    }.onFailure { err ->
      if (err is InsufficientBalanceException) {
        _insufficientBalanceData.value = err
      } else {
        _snackBarMessage.value = err.message ?: "Erro ao aceitar proposta"
      }
    }
  }

  fun rejectProposal(proposalId: String) {
    val result = repository.rejectProposal(proposalId)
    result.onSuccess {
      _snackBarMessage.value = "Prestador/proposta recusado. Você pode escolher outro prestador."
    }.onFailure { err ->
      _snackBarMessage.value = err.message ?: "Não foi possível recusar a proposta"
    }
  }

  // Provider accepts open service opportunity at client's proposed budget (awaits client confirmation)
  fun acceptServiceDirectlyByProvider(serviceId: String) {
    val result = repository.acceptServiceDirectlyByProvider(serviceId)
    result.onSuccess { agreedValue ->
      _snackBarMessage.value =
        "✅ Você aceitou o pedido (R$ ${String.format("%.2f", agreedValue)})! Aguardando o cliente aceitar você para liberar o chat."
      _insufficientBalanceData.value = null
    }.onFailure { err ->
      if (err is InsufficientBalanceException) {
        _insufficientBalanceData.value = err
      } else {
        _snackBarMessage.value = err.message ?: "Erro ao aceitar serviço"
      }
    }
  }

  fun declineServiceByProvider(serviceId: String) {
    val result = repository.declineServiceByProvider(serviceId)
    result.onSuccess {
      if (_selectedServiceId.value == serviceId) {
        _selectedServiceId.value = null
      }
      _snackBarMessage.value = "Pedido recusado e removido da sua lista."
    }.onFailure { err ->
      _snackBarMessage.value = err.message ?: "Não foi possível recusar este pedido."
    }
  }

  fun clearPixRechargeError() {
    _pixRechargeError.value = null
  }

  fun resetPixRecharge() {
    pixPaymentObserverJob?.cancel()
    pixPaymentObserverJob = null
    pixPollingJob?.cancel()
    pixPollingJob = null
    _pixRechargeError.value = null
    _pixRechargeState.value = PixRechargeUiState.Idle
  }

  fun startMercadoPagoPixRecharge(
    amount: Double,
    apiOverride: MercadoPagoApi? = null,
    accessTokenOverride: String? = null
  ) {
    val amountErr = WalletRechargeHelper.validateAmount(amount)
    if (amountErr != null) {
      _pixRechargeError.value = amountErr
      _snackBarMessage.value = "⚠️ $amountErr"
      return
    }
    val effectiveToken = MercadoPagoPixService.normalizeAccessToken(
      accessTokenOverride ?: BuildConfig.MERCADO_PAGO_ACCESS_TOKEN
    )
    if (!MercadoPagoPixService.isConfigured(effectiveToken)) {
      val msg = "Configure MERCADO_PAGO_ACCESS_TOKEN nos Secrets do AI Studio para cobrar via Mercado Pago."
      _pixRechargeError.value = msg
      _snackBarMessage.value = "⚠️ $msg"
      return
    }

    val user = repository.getCurrentUser()
    val txId = WalletRechargeHelper.generateUniqueTxId(user.id)
    pixPaymentObserverJob?.cancel()
    pixPollingJob?.cancel()
    _pixRechargeError.value = null
    _pixRechargeState.value = PixRechargeUiState.CreatingCharge(
      amount = amount,
      gatewayLabel = "Mercado Pago"
    )

    viewModelScope.launch {
      val creationRes = if (apiOverride != null) {
        MercadoPagoPixService.createPixPayment(
          amount = amount,
          user = user,
          txId = txId,
          accessToken = effectiveToken,
          api = apiOverride
        )
      } else {
        MercadoPagoPixService.createPixPayment(
          amount = amount,
          user = user,
          txId = txId,
          accessToken = effectiveToken
        )
      }

      creationRes.onSuccess { created ->
        val charge = PixRechargeCharge(
          txId = created.txId,
          providerId = user.id,
          pixKey = "Mercado Pago • Pagamento #${created.paymentId}",
          merchantName = WalletRechargeHelper.DEFAULT_MERCHANT_NAME,
          amount = amount,
          brCode = created.brCode,
          status = PixChargeStatus.AGUARDANDO_PAGAMENTO,
          expiresAt = created.expiresAtMillis,
          paymentGateway = "MERCADO_PAGO",
          mercadoPagoPaymentId = created.paymentId,
          mpStatus = created.status,
          mpStatusDetail = created.statusDetail,
          ticketUrl = created.ticketUrl,
          qrCodeBase64 = created.qrCodeBase64
        )
        _pixRechargeState.value = PixRechargeUiState.AwaitingPayment(
          charge = charge,
          isCheckingNow = false,
          statusNotice = "Pix Mercado Pago #${created.paymentId} ativo • Aguardando confirmação do banco"
        )
        repository.registerPixCharge(charge)
        observeChargeConfirmation(
          user = user,
          charge = charge,
          apiOverride = apiOverride,
          accessTokenOverride = effectiveToken
        )
        startPendingMercadoPagoPolling(
          apiOverride = apiOverride,
          accessTokenOverride = effectiveToken
        )
      }.onFailure { err ->
        val errMsg = err.message ?: "Não foi possível gerar a cobrança Pix no Mercado Pago."
        _pixRechargeState.value = PixRechargeUiState.Idle
        _pixRechargeError.value = errMsg
        _snackBarMessage.value = "⚠️ $errMsg"
      }
    }
  }

  fun startPixRecharge(
    amount: Double,
    pixKey: String = WalletRechargeHelper.DEFAULT_PIX_KEY,
    pixKeyType: PixKeyType = PixKeyType.AUTO
  ) {
    val amountErr = WalletRechargeHelper.validateAmount(amount)
    if (amountErr != null) {
      _pixRechargeError.value = amountErr
      _snackBarMessage.value = "⚠️ $amountErr"
      return
    }
    val keyErr = WalletRechargeHelper.validatePixKey(pixKey, pixKeyType)
    if (keyErr != null) {
      _pixRechargeError.value = keyErr
      _snackBarMessage.value = "⚠️ $keyErr"
      return
    }

    val normalizedKey = WalletRechargeHelper.normalizePixKey(pixKey, pixKeyType)
      .ifBlank { WalletRechargeHelper.DEFAULT_PIX_KEY }
    val user = repository.getCurrentUser()
    val txId = WalletRechargeHelper.generateUniqueTxId(user.id)
    val brCode = WalletRechargeHelper.buildPixCopyPasteCode(
      amount = amount,
      providerId = user.id,
      pixKey = normalizedKey,
      txId = txId,
      pixKeyType = pixKeyType
    )
    val charge = PixRechargeCharge(
      txId = txId,
      providerId = user.id,
      pixKey = normalizedKey,
      merchantName = WalletRechargeHelper.DEFAULT_MERCHANT_NAME,
      amount = amount,
      brCode = brCode,
      status = PixChargeStatus.AGUARDANDO_PAGAMENTO,
      paymentGateway = "PIX_DIRETO"
    )

    pixPaymentObserverJob?.cancel()
    pixPollingJob?.cancel()
    _pixRechargeError.value = null
    _pixRechargeState.value = PixRechargeUiState.AwaitingPayment(
      charge = charge,
      isCheckingNow = false,
      statusNotice = "Detecção automática ativa • Aguardando confirmação do Pix"
    )
    repository.registerPixCharge(charge)
    observeChargeConfirmation(user = user, charge = charge)
  }

  private fun observeChargeConfirmation(
    user: User,
    charge: PixRechargeCharge,
    apiOverride: MercadoPagoApi? = null,
    accessTokenOverride: String? = null
  ) {
    val initialBalance = user.walletBalance
    pixPaymentObserverJob = viewModelScope.launch {
      repository.observePixChargePayment(
        providerId = user.id,
        txId = charge.txId,
        expectedAmount = charge.amount,
        knownInitialBalance = initialBalance
      ).collect { event ->
        handleDetectedPixPayment(
          charge = charge,
          remoteBalance = event.newBalance,
          automatic = true
        )
      }
    }

    val mpPaymentId = charge.mercadoPagoPaymentId
    if (mpPaymentId != null) {
      pixPollingJob = viewModelScope.launch {
        while (isActive && System.currentTimeMillis() < charge.expiresAt) {
          delay(3500L)
          if (_pixRechargeState.value !is PixRechargeUiState.AwaitingPayment) break
          val statusRes = if (apiOverride != null && accessTokenOverride != null) {
            MercadoPagoPixService.checkPixPaymentStatus(
              paymentId = mpPaymentId,
              accessToken = accessTokenOverride,
              api = apiOverride
            )
          } else {
            MercadoPagoPixService.checkPixPaymentStatus(paymentId = mpPaymentId)
          }
          statusRes.onSuccess { status ->
            if (status.isApproved) {
              handleDetectedPixPayment(
                charge = charge,
                remoteBalance = null,
                automatic = true
              )
              return@launch
            } else if (status.isRejectedOrCancelled) {
              _pixRechargeError.value = "Pagamento Pix #${mpPaymentId} ${status.status}. Gere um novo QR Code."
              resetPixRecharge()
              return@launch
            }
          }
        }
      }
    }
  }

  fun checkPixPaymentStatusNow(
    apiOverride: MercadoPagoApi? = null,
    accessTokenOverride: String? = null
  ) {
    val currentState = _pixRechargeState.value as? PixRechargeUiState.AwaitingPayment ?: return
    val user = repository.getCurrentUser()
    _pixRechargeState.value = currentState.copy(
      isCheckingNow = true,
      statusNotice = if (currentState.charge.mercadoPagoPaymentId != null) {
        "Consultando aprovação no Mercado Pago..."
      } else {
        "Consultando confirmação do Pix..."
      }
    )
    viewModelScope.launch {
      val mpId = currentState.charge.mercadoPagoPaymentId
      if (mpId != null) {
        val token = MercadoPagoPixService.normalizeAccessToken(
          accessTokenOverride ?: BuildConfig.MERCADO_PAGO_ACCESS_TOKEN
        )
        val mpStatus = if (apiOverride != null) {
          MercadoPagoPixService.checkPixPaymentStatus(paymentId = mpId, accessToken = token, api = apiOverride)
        } else {
          MercadoPagoPixService.checkPixPaymentStatus(paymentId = mpId, accessToken = token)
        }
        mpStatus.onSuccess { status ->
          if (status.isApproved) {
            handleDetectedPixPayment(
              charge = currentState.charge,
              remoteBalance = null,
              automatic = true
            )
            return@launch
          } else if (status.isRejectedOrCancelled) {
            repository.updateMercadoPagoChargeStatus(
              txId = currentState.charge.txId,
              status = PixChargeStatus.CANCELADO_OU_REJEITADO,
              mpStatus = status.status,
              mpStatusDetail = status.statusDetail
            )
            val msg = "Pagamento Mercado Pago #$mpId foi ${status.status}. Gere uma nova cobrança."
            _pixRechargeError.value = msg
            resetPixRecharge()
            _snackBarMessage.value = "⚠️ $msg"
            return@launch
          }
        }
      }

      val detected = repository.checkPixChargeStatusOnce(
        providerId = user.id,
        txId = currentState.charge.txId,
        expectedAmount = currentState.charge.amount,
        knownInitialBalance = user.walletBalance
      )
      if (detected != null) {
        handleDetectedPixPayment(
          charge = currentState.charge,
          remoteBalance = detected.newBalance,
          automatic = true
        )
      } else {
        val latest = _pixRechargeState.value
        if (latest is PixRechargeUiState.AwaitingPayment && latest.charge.txId == currentState.charge.txId) {
          _pixRechargeState.value = latest.copy(
            isCheckingNow = false,
            statusNotice = if (mpId != null) {
              "Pagamento #$mpId ainda pendente no Mercado Pago. Assim que compensar, o saldo entra na hora!"
            } else {
              "Ainda aguardando compensação Pix... A detecção automática continua ativa."
            }
          )
        }
      }
    }
  }

  /**
   * Inicia fluxo periódico de polling no ViewModel para todas as cobranças Pix do Mercado Pago
   * pendentes, mesmo após o usuário fechar a janela de QR Code.
   */
  fun startPendingMercadoPagoPolling(
    intervalMs: Long = 5000L,
    apiOverride: MercadoPagoApi? = null,
    accessTokenOverride: String? = null
  ) {
    pendingMpPollingJob?.cancel()
    pendingMpPollingJob = viewModelScope.launch {
      _isMercadoPagoPollingActive.value = true
      try {
        while (isActive) {
          val hasPending = pollPendingMercadoPagoPaymentsOnce(
            apiOverride = apiOverride,
            accessTokenOverride = accessTokenOverride
          )
          if (!hasPending) break
          delay(intervalMs.coerceAtLeast(100L))
        }
      } finally {
        _isMercadoPagoPollingActive.value = false
      }
    }
  }

  suspend fun pollPendingMercadoPagoPaymentsOnce(
    apiOverride: MercadoPagoApi? = null,
    accessTokenOverride: String? = null
  ): Boolean {
    val now = System.currentTimeMillis()
    val token = MercadoPagoPixService.normalizeAccessToken(
      accessTokenOverride ?: BuildConfig.MERCADO_PAGO_ACCESS_TOKEN
    )
    if (!MercadoPagoPixService.isConfigured(token)) return false

    val allMpCharges = repository.mercadoPagoCharges.value
    var remainingPending = false

    for (charge in allMpCharges) {
      if (charge.status != PixChargeStatus.AGUARDANDO_PAGAMENTO) continue
      val mpId = charge.mercadoPagoPaymentId ?: continue

      if (now >= charge.expiresAt) {
        repository.updateMercadoPagoChargeStatus(
          txId = charge.txId,
          status = PixChargeStatus.EXPIRADO,
          mpStatus = "expired"
        )
        continue
      }

      val statusRes = if (apiOverride != null) {
        MercadoPagoPixService.checkPixPaymentStatus(paymentId = mpId, accessToken = token, api = apiOverride)
      } else {
        MercadoPagoPixService.checkPixPaymentStatus(paymentId = mpId, accessToken = token)
      }

      var stillPendingThisCharge = true
      statusRes.onSuccess { status ->
        when {
          status.isApproved -> {
            val activeDialogCharge = (_pixRechargeState.value as? PixRechargeUiState.AwaitingPayment)?.charge
            if (activeDialogCharge?.txId == charge.txId) {
              handleDetectedPixPayment(
                charge = charge,
                remoteBalance = null,
                automatic = true
              )
            } else {
              confirmBackgroundMercadoPagoCharge(
                charge = charge,
                mpStatus = status.status,
                mpStatusDetail = status.statusDetail
              )
            }
            stillPendingThisCharge = false
          }
          status.isRejectedOrCancelled -> {
            repository.updateMercadoPagoChargeStatus(
              txId = charge.txId,
              status = PixChargeStatus.CANCELADO_OU_REJEITADO,
              mpStatus = status.status,
              mpStatusDetail = status.statusDetail
            )
            stillPendingThisCharge = false
          }
          else -> {
            repository.updateMercadoPagoChargeStatus(
              txId = charge.txId,
              status = PixChargeStatus.AGUARDANDO_PAGAMENTO,
              mpStatus = status.status,
              mpStatusDetail = status.statusDetail
            )
          }
        }
      }
      if (stillPendingThisCharge) {
        remainingPending = true
      }
    }
    return remainingPending
  }

  private fun confirmBackgroundMercadoPagoCharge(
    charge: PixRechargeCharge,
    mpStatus: String?,
    mpStatusDetail: String?
  ) {
    val paidAt = System.currentTimeMillis()
    val previousBalance = repository.users.value.firstOrNull { it.id == charge.providerId }?.walletBalance ?: 0.0
    val newBal = repository.rechargeWallet(
      providerId = charge.providerId,
      amount = charge.amount,
      txId = charge.txId,
      detectedAutomatically = true,
      remoteConfirmedBalance = null
    )
    repository.updateMercadoPagoChargeStatus(
      txId = charge.txId,
      status = PixChargeStatus.PAGO,
      mpStatus = mpStatus ?: "approved",
      mpStatusDetail = mpStatusDetail ?: "accredited",
      paidAt = paidAt
    )
    repository.notifyPixPaymentConfirmed(
      providerId = charge.providerId,
      amount = charge.amount,
      txId = charge.txId,
      paymentId = charge.mercadoPagoPaymentId,
      newBalance = newBal
    )
    _insufficientBalanceData.value = null
    val deductedNegative = if (previousBalance < 0.0) kotlin.math.min(-previousBalance, charge.amount) else 0.0
    _snackBarMessage.value = if (deductedNegative > 0.0) {
      "🎉 Pix Mercado Pago de R$ ${String.format("%.2f", charge.amount)} confirmado! Desconto de R$ ${String.format("%.2f", deductedNegative)} do saldo negativo aplicado. Novo saldo: R$ ${String.format("%.2f", newBal)}"
    } else {
      "🎉 Pix Mercado Pago de R$ ${String.format("%.2f", charge.amount)} confirmado! Novo saldo: R$ ${String.format("%.2f", newBal)}"
    }
  }

  fun refreshMercadoPagoTransactionsStatus(
    apiOverride: MercadoPagoApi? = null,
    accessTokenOverride: String? = null
  ) {
    viewModelScope.launch {
      _isRefreshingMpTransactions.value = true
      try {
        pollPendingMercadoPagoPaymentsOnce(
          apiOverride = apiOverride,
          accessTokenOverride = accessTokenOverride
        )
      } finally {
        _isRefreshingMpTransactions.value = false
      }
    }
  }

  fun reopenPendingMercadoPagoCharge(charge: PixRechargeCharge) {
    _pixRechargeError.value = null
    _pixRechargeState.value = PixRechargeUiState.AwaitingPayment(
      charge = charge,
      isCheckingNow = false,
      statusNotice = "Pix Mercado Pago #${charge.mercadoPagoPaymentId ?: charge.txId} • Aguardando pagamento"
    )
    _showRechargeDialog.value = true
    observeChargeConfirmation(user = repository.getCurrentUser(), charge = charge)
    startPendingMercadoPagoPolling()
  }

  fun cancelPendingMercadoPagoCharge(
    charge: PixRechargeCharge,
    apiOverride: MercadoPagoApi? = null,
    accessTokenOverride: String? = null
  ) {
    val mpId = charge.mercadoPagoPaymentId ?: return
    val token = MercadoPagoPixService.normalizeAccessToken(
      accessTokenOverride ?: BuildConfig.MERCADO_PAGO_ACCESS_TOKEN
    )
    viewModelScope.launch {
      val res = if (apiOverride != null) {
        MercadoPagoPixService.cancelPixPayment(paymentId = mpId, accessToken = token, api = apiOverride)
      } else {
        MercadoPagoPixService.cancelPixPayment(paymentId = mpId, accessToken = token)
      }
      res.onSuccess {
        repository.updateMercadoPagoChargeStatus(
          txId = charge.txId,
          status = PixChargeStatus.CANCELADO_OU_REJEITADO,
          mpStatus = "cancelled",
          mpStatusDetail = it.statusDetail
        )
        _snackBarMessage.value = "Cobrança Pix #$mpId cancelada no Mercado Pago."
      }.onFailure { err ->
        _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível cancelar a cobrança."}"
      }
    }
  }

  fun confirmPixPaymentManually() {
    val currentState = _pixRechargeState.value as? PixRechargeUiState.AwaitingPayment ?: return
    if (currentState.charge.mercadoPagoPaymentId != null) {
      checkPixPaymentStatusNow()
      return
    }
    handleDetectedPixPayment(
      charge = currentState.charge,
      remoteBalance = null,
      automatic = false
    )
  }

  private fun handleDetectedPixPayment(
    charge: PixRechargeCharge,
    remoteBalance: Double?,
    automatic: Boolean
  ) {
    val currentUi = _pixRechargeState.value
    if (currentUi is PixRechargeUiState.Detected && currentUi.charge.txId == charge.txId) return
    pixPaymentObserverJob?.cancel()
    pixPaymentObserverJob = null
    pixPollingJob?.cancel()
    pixPollingJob = null

    val paidAt = System.currentTimeMillis()
    val previousBalance = repository.users.value.firstOrNull { it.id == charge.providerId }?.walletBalance ?: 0.0
    val newBal = repository.rechargeWallet(
      providerId = charge.providerId,
      amount = charge.amount,
      txId = charge.txId,
      detectedAutomatically = automatic,
      remoteConfirmedBalance = remoteBalance
    )
    if (charge.paymentGateway == "MERCADO_PAGO" || charge.mercadoPagoPaymentId != null) {
      repository.updateMercadoPagoChargeStatus(
        txId = charge.txId,
        status = PixChargeStatus.PAGO,
        mpStatus = "approved",
        mpStatusDetail = "accredited",
        paidAt = paidAt
      )
      repository.notifyPixPaymentConfirmed(
        providerId = charge.providerId,
        amount = charge.amount,
        txId = charge.txId,
        paymentId = charge.mercadoPagoPaymentId,
        newBalance = newBal
      )
    }
    val paidCharge = charge.copy(
      status = PixChargeStatus.PAGO,
      detectedAutomatically = automatic,
      paidAt = paidAt,
      mpStatus = if (charge.paymentGateway == "MERCADO_PAGO") "approved" else charge.mpStatus
    )
    _pixRechargeState.value = PixRechargeUiState.Detected(
      charge = paidCharge,
      newBalance = newBal,
      automatic = automatic
    )
    _insufficientBalanceData.value = null
    val deductedNegative = if (previousBalance < 0.0) kotlin.math.min(-previousBalance, charge.amount) else 0.0
    _snackBarMessage.value = when {
      deductedNegative > 0.0 && automatic ->
        "🎉 Pix de R$ ${String.format("%.2f", charge.amount)} aprovado! R$ ${String.format("%.2f", deductedNegative)} de saldo negativo descontado. Novo saldo: R$ ${String.format("%.2f", newBal)}"
      deductedNegative > 0.0 ->
        "✅ Recarga Pix de R$ ${String.format("%.2f", charge.amount)} confirmada! R$ ${String.format("%.2f", deductedNegative)} de saldo negativo descontado. Novo saldo: R$ ${String.format("%.2f", newBal)}"
      automatic ->
        "🎉 Pix de R$ ${String.format("%.2f", charge.amount)} aprovado! Novo saldo: R$ ${String.format("%.2f", newBal)}"
      else ->
        "✅ Recarga Pix de R$ ${String.format("%.2f", charge.amount)} confirmada! Novo saldo: R$ ${String.format("%.2f", newBal)}"
    }
  }

  // Recharge Wallet via Pix (kept for direct calls & unit tests)
  fun rechargeWallet(amount: Double, txId: String? = null) {
    val user = repository.getCurrentUser()
    val previousBalance = user.walletBalance
    val newBal = repository.rechargeWallet(
      providerId = user.id,
      amount = amount,
      txId = txId
    )
    _showRechargeDialog.value = false
    resetPixRecharge()
    val deductedNegative = if (previousBalance < 0.0) kotlin.math.min(-previousBalance, amount) else 0.0
    _snackBarMessage.value = if (deductedNegative > 0.0) {
      "✅ Recarga Pix de R$ ${String.format("%.2f", amount)} concluída! Desconto de R$ ${String.format("%.2f", deductedNegative)} do saldo negativo aplicado. Novo saldo: R$ ${String.format("%.2f", newBal)}"
    } else {
      "✅ Recarga via Pix de R$ ${String.format("%.2f", amount)} concluída! Novo saldo: R$ ${String.format("%.2f", newBal)}"
    }
    _insufficientBalanceData.value = null
  }

  // Update Status
  fun updateServiceStatus(
    serviceId: String,
    status: ServiceStatus,
    completionPhotoUri: String? = null,
    completionPhotoName: String? = null,
    completionVideoUri: String? = null,
    completionVideoName: String? = null,
    completionVideoThumbnailUri: String? = null,
    completionVideoDurationMs: Long? = null,
    completionNotes: String? = null
  ) {
    repository.updateServiceStatus(
      serviceId = serviceId,
      newStatus = status,
      completionPhotoUri = completionPhotoUri,
      completionPhotoName = completionPhotoName,
      completionVideoUri = completionVideoUri,
      completionVideoName = completionVideoName,
      completionVideoThumbnailUri = completionVideoThumbnailUri,
      completionVideoDurationMs = completionVideoDurationMs,
      completionNotes = completionNotes
    )
    _snackBarMessage.value = when {
      status == ServiceStatus.CONCLUIDO && !completionPhotoUri.isNullOrBlank() && !completionVideoUri.isNullOrBlank() ->
        "✅ Serviço finalizado e concluído com foto e vídeo anexados!"
      status == ServiceStatus.CONCLUIDO && !completionVideoUri.isNullOrBlank() ->
        "✅ Serviço finalizado e concluído com vídeo anexado!"
      status == ServiceStatus.CONCLUIDO && !completionPhotoUri.isNullOrBlank() ->
        "✅ Serviço finalizado e concluído com foto anexada!"
      status == ServiceStatus.CONCLUIDO ->
        "✅ Serviço finalizado e concluído!"
      else -> "Status atualizado para: ${status.label}"
    }
  }

  fun cancelServiceRequest(serviceId: String, reason: String): Result<ServiceRequest> {
    val result = repository.cancelServiceRequest(serviceId, reason)
    result.onSuccess { cancelled ->
      if (cancelled.cancelledByRole == UserRole.PRESTADOR) {
        _snackBarMessage.value = "🚫 Pedido cancelado pelo prestador. Estorno sujeito à análise de um administrador."
      } else {
        _snackBarMessage.value = "🚫 Pedido cancelado pelo cliente. Motivo registrado."
      }
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível cancelar o pedido."}"
    }
    return result
  }

  fun adminProcessCancellationRefund(
    serviceId: String,
    approve: Boolean,
    note: String = ""
  ): Result<ServiceRequest> {
    val result = repository.adminProcessCancellationRefund(serviceId, approve, note)
    result.onSuccess {
      _snackBarMessage.value = if (approve) {
        "💰 Estorno aprovado e creditado na carteira do prestador."
      } else {
        "❌ Solicitação de estorno negada e registrada na auditoria."
      }
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível processar a análise do estorno."}"
    }
    return result
  }

  fun addClientCompletedServicePhoto(
    serviceId: String,
    photoUri: String,
    photoName: String = "",
    caption: String = ""
  ): Result<ServiceRequest> {
    val result = repository.addClientCompletedServicePhoto(serviceId, photoUri, photoName, caption)
    result.onSuccess {
      _snackBarMessage.value = "📸 Foto adicionada ao serviço concluído!"
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível adicionar a foto."}"
    }
    return result
  }

  fun removeClientCompletedServicePhoto(serviceId: String, photoId: String): Result<ServiceRequest> {
    val result = repository.removeClientCompletedServicePhoto(serviceId, photoId)
    result.onSuccess {
      _snackBarMessage.value = "🗑️ Foto removida do serviço."
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível remover a foto."}"
    }
    return result
  }

  fun recordProviderLocationPoint(
    serviceId: String,
    latitude: Double,
    longitude: Double,
    accuracyMeters: Float = 0f,
    speedMps: Float = 0f,
    source: String = "GPS"
  ): ProviderLocationPoint? {
    return repository.recordProviderLocationPoint(
      serviceId = serviceId,
      latitude = latitude,
      longitude = longitude,
      accuracyMeters = accuracyMeters,
      speedMps = speedMps,
      source = source
    )
  }

  fun syncLocationHistoryForService(serviceId: String) {
    viewModelScope.launch {
      repository.syncLocationHistoryForService(serviceId)
    }
  }

  fun submitAppSuggestion(
    category: String,
    title: String,
    description: String
  ): Result<AppSuggestion> {
    val result = repository.submitAppSuggestion(category, title, description)
    result.onSuccess {
      _snackBarMessage.value = "💡 Sugestão enviada com sucesso! Obrigado por ajudar a melhorar o ResolveAi."
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível enviar sua sugestão."}"
    }
    return result
  }

  fun updateAppSuggestionStatus(
    suggestionId: String,
    status: SuggestionStatus,
    adminReply: String? = null
  ): Result<AppSuggestion> {
    val result = repository.updateAppSuggestionStatus(suggestionId, status, adminReply)
    result.onSuccess {
      _snackBarMessage.value = "✅ Sugestão atualizada para: ${status.label}"
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível atualizar a sugestão."}"
    }
    return result
  }

  // Open & Submit Rating
  fun openRatingDialog(service: ServiceRequest, initialRating: Float = 5.0f) {
    _ratingInitialStars.value = initialRating.coerceIn(1.0f, 5.0f)
    _ratingServiceTarget.value = service
  }

  fun dismissRatingDialog() {
    _ratingServiceTarget.value = null
  }

  fun confirmServiceCompletedByClientAndRate(serviceId: String) {
    val current = repository.services.value.firstOrNull { it.id == serviceId } ?: return
    val user = repository.getCurrentUser()
    if (!user.ownsId(current.clientId) && user.role != UserRole.ADMIN) return
    if (current.status != ServiceStatus.CONCLUIDO) {
      repository.updateServiceStatus(serviceId = serviceId, newStatus = ServiceStatus.CONCLUIDO)
    }
    val updated = repository.services.value.firstOrNull { it.id == serviceId } ?: current.copy(status = ServiceStatus.CONCLUIDO)
    if (updated.ratingGiven == null && repository.reviews.value.none { it.serviceId == serviceId }) {
      _ratingInitialStars.value = 5.0f
      _ratingServiceTarget.value = updated
    }
    _snackBarMessage.value = "✅ Serviço marcado como concluído! Avalie o prestador de 1 a 5 estrelas."
  }

  fun submitServiceRating(
    serviceId: String,
    rating: Float,
    comment: String,
    complimentTags: List<String>
  ) {
    val result = repository.rateService(serviceId, rating, comment, complimentTags)
    result.onSuccess {
      _ratingServiceTarget.value = null
      val starsText = if (rating % 1.0f == 0.0f) rating.toInt().toString() else rating.toString()
      _snackBarMessage.value = "⭐ Avaliação de $starsText estrelas enviada com sucesso! Obrigado pelo feedback."
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível enviar sua avaliação."}"
    }
  }

  fun replyToReview(reviewId: String, replyText: String) {
    val result = repository.replyToReview(reviewId, replyText)
    result.onSuccess {
      _snackBarMessage.value = "✅ Resposta enviada à avaliação do cliente!"
    }.onFailure { err ->
      _snackBarMessage.value = "⚠️ ${err.message ?: "Não foi possível enviar a resposta."}"
    }
  }

  // Update Profile
  fun updateProfile(user: User) {
    repository.updateUserProfile(user)
    _showEditProfileDialog.value = false
    _snackBarMessage.value = "Perfil atualizado com sucesso!"
  }

  private val _showOnboardingReplay = MutableStateFlow(false)
  val showOnboardingReplay: StateFlow<Boolean> = _showOnboardingReplay.asStateFlow()

  fun openOnboarding() {
    _showOnboardingReplay.value = true
  }

  fun dismissOnboarding() {
    _showOnboardingReplay.value = false
    repository.markOnboardingSeen()
  }

  // Send chat message with safety check
  fun sendChatMessage(serviceId: String, text: String): ContactFilterResult {
    val check = ContactFilter.evaluate(text)
    if (!check.isAllowed) {
      _snackBarMessage.value = "⛔ Mensagem bloqueada pela Proteção de Dados: ${check.violationType ?: "Troca de contatos proibida"}"
      return check
    }

    repository.addChatMessage(serviceId, text)
    return check
  }

  fun clearChatDiagnosticLogs() {
    repository.clearChatDiagnosticLogs()
  }

  fun exportChatDiagnostics(serviceId: String? = null): String {
    return repository.exportChatDiagnostics(serviceId)
  }
}
