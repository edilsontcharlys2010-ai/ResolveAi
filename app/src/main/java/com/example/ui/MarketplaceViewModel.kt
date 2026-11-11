package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.InsufficientBalanceException
import com.example.data.MarketplaceRepository
import com.example.model.ContactFilter
import com.example.model.ContactFilterResult
import com.example.model.Proposal
import com.example.model.ServiceRequest
import com.example.model.ServiceReview
import com.example.model.ServiceStatus
import com.example.model.User
import com.example.model.AppNotification
import com.example.model.ProviderDocuments
import com.example.model.ProviderVerificationStatus
import com.example.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
  SERVICOS,
  NEGOCIACOES,
  CHAT,
  CARTEIRA_PERFIL
}

data class PendingRegistration(
  val name: String,
  val email: String,
  val phone: String,
  val role: UserRole,
  val category: String = "",
  val bio: String = "",
  val city: String = "São Paulo, SP",
  val document: String = "",
  val initialWalletBonus: Double = 0.0,
  val rgOrCnhNumber: String = "",
  val criminalRecordNumber: String = "",
  val selfieFileName: String = ""
)

class MarketplaceViewModel(
  private val repository: MarketplaceRepository = MarketplaceRepository.instance
) : ViewModel() {

  val users = repository.users
  val currentUserId = repository.currentUserId
  val services = repository.services
  val proposals = repository.proposals
  val walletTransactions = repository.walletTransactions
  val chatMessages = repository.chatMessages
  val reviews = repository.reviews
  val notifications = repository.notifications

  private val _showNotificationsDialog = MutableStateFlow(false)
  val showNotificationsDialog: StateFlow<Boolean> = _showNotificationsDialog.asStateFlow()

  fun setShowNotifications(show: Boolean) {
    _showNotificationsDialog.value = show
  }

  fun markNotificationAsRead(notifId: String) {
    repository.markNotificationAsRead(notifId)
  }

  fun approveProvider(providerId: String) {
    repository.approveProvider(providerId)
    _snackBarMessage.value = "✅ Prestador aprovado com sucesso! Acesso liberado ao mural e carteira."
  }

  fun rejectProvider(providerId: String, reason: String) {
    repository.rejectProvider(providerId, reason)
    _snackBarMessage.value = "⚠️ Documentação do prestador reprovada: $reason"
  }

  fun confirmSmsCode(inputCode: String): Boolean {
    val pending = _pendingRegistration.value ?: return false
    val isValid = repository.verifySmsCode(pending.phone, inputCode)
    if (isValid) {
      val docs = if (pending.role == UserRole.PRESTADOR) {
        ProviderDocuments(
          rgOrCnhNumber = pending.rgOrCnhNumber.ifBlank { "CNH ${((10000000000..99999999999).random())}" },
          criminalRecordCertNumber = pending.criminalRecordNumber.ifBlank { "CERT-2026-${((10000..99999).random())}-PF" },
          selfieFileName = pending.selfieFileName.ifBlank { "selfie_autenticada.jpg" }
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
        document = pending.document,
        initialWalletBonus = pending.initialWalletBonus,
        documents = docs
      )
      _showSmsDialog.value = false
      _pendingRegistration.value = null
      _smsNotificationBanner.value = null
      if (newUser.role == UserRole.PRESTADOR) {
        _snackBarMessage.value = "📋 Cadastro de Prestador realizado! Seus documentos foram enviados para análise do Administrador."
      } else {
        _snackBarMessage.value = "🎉 Bem-vindo(a), ${newUser.name}! Cadastro de Cliente ativado com sucesso via SMS."
      }
      return true
    } else {
      _snackBarMessage.value = "Código SMS inválido. Verifique os dígitos informados."
      return false
    }
  }
  val currentUser: StateFlow<User> = combine(users, currentUserId) { userList, id ->
    userList.find { it.id == id } ?: userList.first()
  }.stateIn(
    viewModelScope,
    SharingStarted.Eagerly,
    repository.getCurrentUser()
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

  // Registration & SMS Auth Dialogs
  private val _showRegisterDialog = MutableStateFlow(false)
  val showRegisterDialog: StateFlow<Boolean> = _showRegisterDialog.asStateFlow()

  private val _showSmsDialog = MutableStateFlow(false)
  val showSmsDialog: StateFlow<Boolean> = _showSmsDialog.asStateFlow()

  private val _pendingRegistration = MutableStateFlow<PendingRegistration?>(null)
  val pendingRegistration: StateFlow<PendingRegistration?> = _pendingRegistration.asStateFlow()

  private val _smsNotificationBanner = MutableStateFlow<String?>(null)
  val smsNotificationBanner: StateFlow<String?> = _smsNotificationBanner.asStateFlow()

  // Rating Dialog State
  private val _ratingServiceTarget = MutableStateFlow<ServiceRequest?>(null)
  val ratingServiceTarget: StateFlow<ServiceRequest?> = _ratingServiceTarget.asStateFlow()

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

  private val _selectedCategoryFilter = MutableStateFlow("Todos")
  val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

  fun setTab(tab: MainTab) {
    _selectedTab.value = tab
  }

  fun selectService(serviceId: String?) {
    _selectedServiceId.value = serviceId
  }

  fun openChatForService(serviceId: String) {
    _selectedChatServiceId.value = serviceId
    _selectedTab.value = MainTab.CHAT
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setCategoryFilter(category: String) {
    _selectedCategoryFilter.value = category
  }

  fun switchUser(userId: String) {
    repository.switchUser(userId)
    _showUserSwitcherDialog.value = false
    val newUser = repository.getCurrentUser()
    _snackBarMessage.value = "Conectado como ${newUser.name} (${newUser.role.label})"
  }

  fun setShowCreateService(show: Boolean) {
    _showCreateServiceDialog.value = show
  }

  fun setShowRecharge(show: Boolean) {
    _showRechargeDialog.value = show
  }

  fun setShowUserSwitcher(show: Boolean) {
    _showUserSwitcherDialog.value = show
  }

  fun setShowEditProfile(show: Boolean) {
    _showEditProfileDialog.value = show
  }

  fun setShowRegister(show: Boolean) {
    _showRegisterDialog.value = show
  }

  fun setShowSmsDialog(show: Boolean) {
    _showSmsDialog.value = show
  }

  fun dismissInsufficientBalance() {
    _insufficientBalanceData.value = null
  }

  fun dismissSnackbar() {
    _snackBarMessage.value = null
  }

  fun dismissSmsBanner() {
    _smsNotificationBanner.value = null
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

  // Start SMS Registration Flow
  fun startSmsRegistration(registration: PendingRegistration) {
    _pendingRegistration.value = registration
    val code = repository.sendSmsCode(registration.phone)
    _smsNotificationBanner.value = "📱 SMS recebido no ${registration.phone}: Seu código de ativação ResolveAi é $code"
    _showRegisterDialog.value = false
    _showSmsDialog.value = true
  }

  fun resendSmsCode() {
    val pending = _pendingRegistration.value ?: return
    val code = repository.sendSmsCode(pending.phone)
    _smsNotificationBanner.value = "📱 Novo SMS enviado: Seu código ResolveAi é $code"
  }


  // Create Service
  fun createService(
    title: String,
    category: String,
    description: String,
    location: String,
    proposedBudget: Double,
    urgency: String
  ) {
    val created = repository.createServiceRequest(
      title = title,
      category = category,
      description = description,
      location = location,
      proposedBudget = proposedBudget,
      urgency = urgency
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

  // Accept Proposal (debitting 10% fee from provider wallet)
  fun acceptProposal(proposalId: String) {
    val result = repository.acceptProposalAndDebitFee(proposalId)
    result.onSuccess { fee ->
      _snackBarMessage.value = "🎉 Proposta aceita! Taxa de 10% (R$ ${String.format("%.2f", fee)}) debitada da carteira do prestador."
      _insufficientBalanceData.value = null
    }.onFailure { err ->
      if (err is InsufficientBalanceException) {
        _insufficientBalanceData.value = err
      } else {
        _snackBarMessage.value = err.message ?: "Erro ao aceitar proposta"
      }
    }
  }

  // Recharge Wallet
  fun rechargeWallet(amount: Double) {
    val user = currentUser.value
    val newBal = repository.rechargeWallet(user.id, amount)
    _showRechargeDialog.value = false
    _snackBarMessage.value = "Recarga de R$ ${String.format("%.2f", amount)} concluída! Novo saldo: R$ ${String.format("%.2f", newBal)}"
    _insufficientBalanceData.value = null
  }

  // Update Status
  fun updateServiceStatus(serviceId: String, status: ServiceStatus) {
    repository.updateServiceStatus(serviceId, status)
    _snackBarMessage.value = "Status atualizado para: ${status.label}"
  }

  // Open & Submit Rating
  fun openRatingDialog(service: ServiceRequest) {
    _ratingServiceTarget.value = service
  }

  fun dismissRatingDialog() {
    _ratingServiceTarget.value = null
  }

  fun submitServiceRating(
    serviceId: String,
    rating: Float,
    comment: String,
    complimentTags: List<String>
  ) {
    repository.rateService(serviceId, rating, comment, complimentTags)
    _ratingServiceTarget.value = null
    _snackBarMessage.value = "⭐ Avaliação de $rating estrelas enviada com sucesso! Obrigado pelo feedback."
  }

  fun replyToReview(reviewId: String, replyText: String) {
    repository.replyToReview(reviewId, replyText)
    _snackBarMessage.value = "Resposta enviada à avaliação do cliente!"
  }

  // Update Profile
  fun updateProfile(user: User) {
    repository.updateUserProfile(user)
    _showEditProfileDialog.value = false
    _snackBarMessage.value = "Perfil atualizado com sucesso!"
  }

  // Send chat message with safety check
  fun sendChatMessage(serviceId: String, text: String): ContactFilterResult {
    val check = ContactFilter.evaluate(text)
    if (!check.isAllowed) {
      return check
    }

    repository.addChatMessage(serviceId, text)
    return check
  }
}
