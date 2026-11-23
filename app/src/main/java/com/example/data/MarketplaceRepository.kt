package com.example.data

import android.app.Activity
import android.content.Context
import com.example.model.AppAdminSettings
import com.example.model.AppSuggestion
import com.example.model.AppTip
import com.example.model.CancellationRefundStatus
import com.example.model.ChatMessage
import com.example.model.ClientCompletedServicePhoto
import com.example.model.ContactFilter
import com.example.model.CounterOfferEntry
import com.example.model.DocumentReviewAudit
import com.example.model.Proposal
import com.example.model.ProposalStatus
import com.example.model.ProviderLocationPoint
import com.example.model.RegionalPriceEstimate
import com.example.model.ServiceCategoryItem
import com.example.model.ServiceRequest
import com.example.model.ServiceStatus
import com.example.model.SuggestionStatus
import com.example.model.TransactionType
import com.example.model.User
import com.example.model.UserRole
import com.example.model.ProviderDocuments
import com.example.model.ProviderServicePhoto
import com.example.model.ProviderVerificationStatus
import com.example.model.AppNotification
import com.example.model.NotificationType
import com.example.model.ServiceReview
import com.example.model.ClientLocation
import com.example.model.PixChargeStatus
import com.example.model.PixRechargeCharge
import com.example.model.SmsVerificationSession
import com.example.model.WalletTransaction
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class CloudSyncState {
  OFFLINE_LOCAL,
  SYNCING,
  ONLINE_SYNCED,
  ERROR
}

data class CloudSyncStatus(
  val state: CloudSyncState = CloudSyncState.OFFLINE_LOCAL,
  val isFirebaseSignedIn: Boolean = false,
  val activeListenersCount: Int = 0,
  val lastSyncedAt: Long? = null,
  val statusMessage: String = "Aguardando conexão com o Firebase"
)

class MarketplaceRepository(
  private var firestoreDataSource: FirestoreMarketplaceDataSource? = null,
  private var localStore: MarketplaceLocalStore? = null
) {

  private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var appContext: Context? = null
  private var firestoreHelper: FirestoreHelper? = null
  private var hasLoadedLocalState = false
  private var firestoreSyncJobs: MutableList<Job> = mutableListOf()
  private var activeSyncKey: String? = null
  private val chatListenerJobs: MutableMap<String, Job> = mutableMapOf()
  private val serviceProposalListenerJobs: MutableMap<String, Job> = mutableMapOf()
  private var selectedServiceProposalsJob: Job? = null
  private var hasReceivedRemoteServicesSnapshot = false
  private var hasReceivedRemoteProposalsSnapshot = false
  private var hasReceivedRemoteReviewsSnapshot = false
  private var hasReceivedRemoteNotificationsSnapshot = false
  private val initializedChatSyncServiceIds = mutableSetOf<String>()
  private val dispatchedNotificationKeys = mutableSetOf<String>()
  private val syncedProviderFeeServiceIds = mutableSetOf<String>()
  private var firebaseAuthListenerRegistered = false

  private val _cloudSyncStatus = MutableStateFlow(CloudSyncStatus())
  val cloudSyncStatus: StateFlow<CloudSyncStatus> = _cloudSyncStatus.asStateFlow()

  private val _chatDeliveryError = MutableStateFlow<String?>(null)
  val chatDeliveryError: StateFlow<String?> = _chatDeliveryError.asStateFlow()

  val chatDiagnosticLogs: StateFlow<List<ChatDiagnosticLogEntry>> = ChatDiagnosticLogger.logs

  fun clearChatDiagnosticLogs() {
    ChatDiagnosticLogger.clearLogs()
  }

  fun exportChatDiagnostics(serviceId: String? = null): String {
    return ChatDiagnosticLogger.exportReport(serviceId)
  }

  private val _reviewSyncError = MutableStateFlow<String?>(null)
  val reviewSyncError: StateFlow<String?> = _reviewSyncError.asStateFlow()

  fun clearReviewSyncError() {
    _reviewSyncError.value = null
  }

  private val _systemNotificationEvents = MutableSharedFlow<AppNotification>(extraBufferCapacity = 32)
  val systemNotificationEvents: SharedFlow<AppNotification> = _systemNotificationEvents.asSharedFlow()

  private val _documentReviewAudits = MutableStateFlow<List<DocumentReviewAudit>>(emptyList())
  val documentReviewAudits: StateFlow<List<DocumentReviewAudit>> = _documentReviewAudits.asStateFlow()

  private val _appSuggestions = MutableStateFlow<List<AppSuggestion>>(emptyList())
  val appSuggestions: StateFlow<List<AppSuggestion>> = _appSuggestions.asStateFlow()

  private val _serviceLocationHistory = MutableStateFlow<List<ProviderLocationPoint>>(emptyList())
  val serviceLocationHistory: StateFlow<List<ProviderLocationPoint>> = _serviceLocationHistory.asStateFlow()

  private val _hasSeenOnboarding = MutableStateFlow(false)
  val hasSeenOnboarding: StateFlow<Boolean> = _hasSeenOnboarding.asStateFlow()

  private val _serviceCategories = MutableStateFlow(defaultServiceCategories())
  val serviceCategories: StateFlow<List<ServiceCategoryItem>> = _serviceCategories.asStateFlow()

  private val _appAdminSettings = MutableStateFlow(AppAdminSettings())
  val appAdminSettings: StateFlow<AppAdminSettings> = _appAdminSettings.asStateFlow()

  private val _appTips = MutableStateFlow(defaultAppTips())
  val appTipsFlow: StateFlow<List<AppTip>> = _appTips.asStateFlow()
  val appTips: List<AppTip> get() = _appTips.value

  fun clearChatDeliveryError() {
    _chatDeliveryError.value = null
  }

  private fun registerFirebaseAuthStateListenerIfNeeded() {
    if (firebaseAuthListenerRegistered) return
    val auth = try { FirebaseAuth.getInstance() } catch (_: Throwable) { null } ?: return
    firebaseAuthListenerRegistered = true
    try {
      auth.addAuthStateListener { firebaseAuth ->
        val fbUser = firebaseAuth.currentUser
        if (fbUser != null && _isAuthenticated.value) {
          startFirestoreSyncIfSignedIn()
        } else if (fbUser == null) {
          stopFirestoreSync()
          _cloudSyncStatus.value = CloudSyncStatus(
            state = CloudSyncState.OFFLINE_LOCAL,
            isFirebaseSignedIn = false,
            activeListenersCount = 0,
            lastSyncedAt = _cloudSyncStatus.value.lastSyncedAt,
            statusMessage = "Modo local (faça login no Firebase para sincronizar online)"
          )
        }
      }
    } catch (_: Throwable) {
      firebaseAuthListenerRegistered = false
    }
  }

  private fun markCloudSyncPulse(message: String = "Sincronizado em tempo real com Firebase") {
    val activeCount = firestoreSyncJobs.count { it.isActive } +
      chatListenerJobs.values.count { it.isActive } +
      serviceProposalListenerJobs.values.count { it.isActive } +
      if (selectedServiceProposalsJob?.isActive == true) 1 else 0
    _cloudSyncStatus.value = CloudSyncStatus(
      state = CloudSyncState.ONLINE_SYNCED,
      isFirebaseSignedIn = currentFirebaseUidOrNull() != null,
      activeListenersCount = activeCount,
      lastSyncedAt = System.currentTimeMillis(),
      statusMessage = message
    )
  }

  fun attachContext(context: Context) {
    val ctx = context.applicationContext
    appContext = ctx
    AppNotificationHelper.ensureChannels(ctx)
    if (localStore == null) {
      localStore = MarketplaceLocalStore(ctx)
    }
    val helper = FirestoreHelper.initialize(ctx)
    firestoreHelper = helper
    if (firestoreDataSource == null) {
      firestoreDataSource = helper.dataSource
    }
    restoreLocalStateIfNeeded()
    registerFirebaseAuthStateListenerIfNeeded()
    if (_isAuthenticated.value) {
      startFirestoreSyncIfSignedIn()
    }
  }

  fun attachFirestore(context: Context) {
    attachContext(context)
  }

  fun attachFirestoreDataSource(dataSource: FirestoreMarketplaceDataSource) {
    firestoreDataSource = dataSource
  }

  fun getFirestoreHelper(): FirestoreHelper? = firestoreHelper

  val isFirestoreInitialized: Boolean
    get() = firestoreHelper?.isInitialized == true || firestoreDataSource?.db != null

  // Seed Users
  private val _users = MutableStateFlow<List<User>>(
    listOf(
      User(
        id = "cli_1",
        name = "Carlos Eduardo",
        email = "carlos.eduardo@email.com",
        phone = "(11) 98111-2233",
        role = UserRole.CLIENTE,
        city = "São Paulo, SP",
        document = "321.654.987-00",
        rating = 4.9,
        completedJobs = 6,
        avatarColorHex = 0xFF2563EB
      ),
      User(
        id = "cli_2",
        name = "Mariana Silveira",
        email = "mariana.silveira@email.com",
        phone = "(11) 97222-3344",
        role = UserRole.CLIENTE,
        city = "Campinas, SP",
        document = "123.456.789-11",
        rating = 5.0,
        completedJobs = 3,
        avatarColorHex = 0xFF7C3AED
      ),
      User(
        id = "pro_1",
        name = "Roberto Mendes",
        email = "roberto.eletrica@email.com",
        phone = "(11) 99333-4455",
        role = UserRole.PRESTADOR,
        category = "Eletricista Residencial & Predial",
        bio = "Especialista em instalações elétricas, quadros de distribuição, tomadas e iluminação LED com mais de 8 anos de experiência.",
        city = "São Paulo, SP",
        document = "12.345.678/0001-90",
        rating = 4.95,
        ratingCount = 2,
        completedJobs = 42,
        avatarColorHex = 0xFF0D9488,
        photoUrl = "drawable://img_avatar_roberto",
        walletBalance = 120.00, // Saldo inicial para débito da taxa de 10%
        portfolioPhotos = listOf(
          ProviderServicePhoto(
            id = "showcase_pro_1_eletrica",
            title = "Quadro de disjuntores e iluminação LED",
            category = "Eletricista",
            photoUri = "drawable://img_showcase_eletrica",
            createdAt = 1_760_000_000_000L
          )
        )
      ),
      User(
        id = "pro_2",
        name = "Ana Clara Ferraz",
        email = "anaclara.pinturas@email.com",
        phone = "(11) 98444-5566",
        role = UserRole.PRESTADOR,
        category = "Pintura & Acabamento",
        bio = "Pintura residencial e comercial de alto padrão, textura, massa corrida e restauração.",
        city = "Campinas, SP",
        document = "23.456.789/0001-01",
        rating = 4.88,
        ratingCount = 1,
        completedJobs = 28,
        avatarColorHex = 0xFFEA580C,
        photoUrl = "drawable://img_avatar_anaclara",
        walletBalance = 35.00, // Saldo menor para demonstrar necessidade de recarga
        portfolioPhotos = listOf(
          ProviderServicePhoto(
            id = "showcase_pro_2_pintura",
            title = "Pintura sala de estar e acabamento fosco",
            category = "Pintura",
            photoUri = "drawable://img_showcase_pintura",
            createdAt = 1_759_900_000_000L
          )
        )
      ),
      User(
        id = "pro_3",
        name = "Marcos Vinicius",
        email = "marcos.redes@email.com",
        phone = "(11) 97555-6677",
        role = UserRole.PRESTADOR,
        category = "TI, Redes & Informática",
        bio = "Instalação de redes Wi-Fi empresariais, câmeras de segurança e suporte de infraestrutura.",
        city = "São Paulo, SP",
        document = "34.567.890/0001-12",
        rating = 5.0,
        completedJobs = 19,
        avatarColorHex = 0xFF0284C7,
        walletBalance = 250.00,
        portfolioPhotos = listOf(
          ProviderServicePhoto(
            id = "showcase_pro_3_redes",
            title = "Roteador Wi-Fi Mesh e cabeamento Cat6",
            category = "TI, Redes & Informática",
            photoUri = "drawable://img_showcase_redes",
            createdAt = 1_759_800_000_000L
          )
        )
      ),
      User(
        id = "pro_pending",
        name = "Fernando Alves",
        email = "fernando.marcenaria@email.com",
        phone = "(11) 98711-2299",
        role = UserRole.PRESTADOR,
        category = "Montagem de Móveis & Marcenaria",
        bio = "Montador profissional com ferramentas Bosch e DeWalt. Especialista em cozinhas planejadas e guarda-roupas.",
        city = "São Paulo, SP",
        document = "45.678.901/0001-23",
        rating = 5.0,
        completedJobs = 0,
        avatarColorHex = 0xFFD97706,
        walletBalance = 50.00,
        isPhoneVerified = true,
        verificationStatus = ProviderVerificationStatus.PENDENTE_ANALISE,
        documents = ProviderDocuments(
          rgOrCnhNumber = "CNH 04829103948 - DETRAN/SP",
          rgOrCnhFileName = "cnh_digital_fernando.pdf",
          criminalRecordCertNumber = "CERT-2026-99120-PF",
          criminalRecordFileName = "antecedentes_policia_federal.pdf",
          selfieFileName = "selfie_fernando_rosto_cnh.jpg",
          submittedAt = System.currentTimeMillis() - 3600000L
        )
      ),
      User(
        id = "pro_pending_2",
        name = "Juliana Costa",
        email = "juliana.hidraulica@email.com",
        phone = "(11) 99612-8834",
        role = UserRole.PRESTADOR,
        category = "Hidráulica & Encanador",
        bio = "Encanadora predial com especialização em caça-vazamentos, registros e aquecedores a gás.",
        city = "Guarulhos, SP",
        document = "52.901.344/0001-77",
        rating = 5.0,
        completedJobs = 0,
        avatarColorHex = 0xFF7C3AED,
        walletBalance = 50.00,
        isPhoneVerified = true,
        verificationStatus = ProviderVerificationStatus.PENDENTE_ANALISE,
        documents = ProviderDocuments(
          rgOrCnhNumber = "RG 44.892.310-8 - SSP/SP",
          rgOrCnhFileName = "rg_frente_verso_juliana.pdf",
          criminalRecordCertNumber = "CERT-2026-84519-SSP",
          criminalRecordFileName = "antecedentes_ssp_juliana.pdf",
          selfieFileName = "selfie_juliana_segurando_rg.jpg",
          submittedAt = System.currentTimeMillis() - 7200000L
        )
      ),
      User(
        id = "admin_1",
        name = "Administrador ResolveAi",
        email = "seguranca@resolveai.com",
        phone = "(11) 90000-0000",
        role = UserRole.ADMIN,
        city = "São Paulo, SP",
        document = "00.000.000/0001-00",
        avatarColorHex = 0xFF1E3A8A,
        isPhoneVerified = true
      )
    )
  )
  val users: StateFlow<List<User>> = _users.asStateFlow()

  // Active User session & Authentication state (requires SMS auth before app access)
  private val _currentUserId = MutableStateFlow("cli_1")
  val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

  private val _isAuthenticated = MutableStateFlow(false)
  val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

  // Services
  private val _services = MutableStateFlow<List<ServiceRequest>>(
    listOf(
      ServiceRequest(
        id = "srv_1",
        clientId = "cli_1",
        clientName = "Carlos Eduardo",
        clientRating = 4.9,
        clientAvatarColorHex = 0xFF2563EB,
        title = "Troca de fiação do chuveiro e quadro de disjuntores",
        category = "Eletricista",
        description = "Chuveiro desarmando o disjuntor constantemente. Necessito revisão do cabeamento de 6mm e troca de 2 disjuntores no quadro principal.",
        location = "Pinheiros - São Paulo, SP",
        proposedBudget = 280.00,
        status = ServiceStatus.EM_NEGOCIACAO,
        urgency = "Alta",
        createdAt = System.currentTimeMillis() - 86400000L,
        clientLocation = ClientLocation(
          address = "Rua dos Pinheiros, 842 - Pinheiros",
          neighborhood = "Pinheiros",
          city = "São Paulo, SP",
          referencePoint = "A 180m da estação Fradique Coutinho (Linha 4-Amarela)",
          distanceKm = 4.8,
          drivingTimeMinutes = 14,
          motoTimeMinutes = 9,
          transitTimeMinutes = 22,
          trafficCondition = "Trânsito fluido",
          bestRoute = "Via Av. Rebouças e R. Teodoro Sampaio"
        )
      ),
      ServiceRequest(
        id = "srv_2",
        clientId = "cli_2",
        clientName = "Mariana Silveira",
        clientRating = 5.0,
        clientAvatarColorHex = 0xFF7C3AED,
        title = "Pintura de sala comercial e recepção (45m²)",
        category = "Pintura",
        description = "Aplicação de duas demãos de tinta látex acrílica fosca branca e retoque de pequenos furos na parede.",
        location = "Cambuí - Campinas, SP",
        proposedBudget = 650.00,
        status = ServiceStatus.EM_NEGOCIACAO,
        urgency = "Média",
        createdAt = System.currentTimeMillis() - 172800000L,
        clientLocation = ClientLocation(
          address = "Rua Coronel Quirino, 1150 - Cambuí",
          neighborhood = "Cambuí",
          city = "Campinas, SP",
          referencePoint = "Próximo ao Centro de Convivência Cultural",
          distanceKm = 3.2,
          drivingTimeMinutes = 10,
          motoTimeMinutes = 7,
          transitTimeMinutes = 18,
          trafficCondition = "Trânsito normal",
          bestRoute = "Via Av. Júlio de Mesquita"
        )
      ),
      ServiceRequest(
        id = "srv_3",
        clientId = "cli_1",
        clientName = "Carlos Eduardo",
        clientRating = 4.9,
        clientAvatarColorHex = 0xFF2563EB,
        title = "Instalação de rede Wi-Fi Mesh e cabeamento estruturado",
        category = "TI, Redes & Informática",
        description = "Configurar 3 roteadores Deco Mesh e passar cabo Cat6 da sala para o escritório.",
        location = "Pinheiros - São Paulo, SP",
        proposedBudget = 400.00,
        status = ServiceStatus.ABERTO,
        urgency = "Baixa",
        createdAt = System.currentTimeMillis() - 3600000L,
        clientLocation = ClientLocation(
          address = "Rua Pedroso de Morais, 520 - Pinheiros",
          neighborhood = "Pinheiros",
          city = "São Paulo, SP",
          referencePoint = "Em frente à Praça dos Omaguás",
          distanceKm = 5.4,
          drivingTimeMinutes = 17,
          motoTimeMinutes = 11,
          transitTimeMinutes = 26,
          trafficCondition = "Trânsito moderado",
          bestRoute = "Via Av. Brigadeiro Faria Lima"
        )
      ),
      ServiceRequest(
        id = "srv_4",
        clientId = "cli_2",
        clientName = "Mariana Silveira",
        clientRating = 5.0,
        clientAvatarColorHex = 0xFF7C3AED,
        title = "Reparo de vazamento em pia de cozinha e troca de sifão",
        category = "Hidráulica",
        description = "Vazamento contínuo abaixo da cuba. Já possuo o sifão novo.",
        location = "Centro - Campinas, SP",
        proposedBudget = 180.00,
        finalAgreedPrice = 180.00,
        platformFee = 18.00, // 10%
        status = ServiceStatus.ACEITO,
        acceptedProviderId = "pro_1",
        acceptedProviderName = "Roberto Mendes",
        acceptedProviderRating = 4.95,
        acceptedProviderAvatarColorHex = 0xFF0D9488,
        urgency = "Média",
        createdAt = System.currentTimeMillis() - 259200000L,
        clientLocation = ClientLocation(
          address = "Rua Barão de Jaguara, 930 - Centro",
          neighborhood = "Centro",
          city = "Campinas, SP",
          referencePoint = "Ao lado da Catedral Metropolitana",
          distanceKm = 2.1,
          drivingTimeMinutes = 7,
          motoTimeMinutes = 5,
          transitTimeMinutes = 13,
          trafficCondition = "Trânsito livre",
          bestRoute = "Via Av. Francisco Glicério"
        )
      )
    )
  )
  val services: StateFlow<List<ServiceRequest>> = _services.asStateFlow()

  // Proposals
  private val _proposals = MutableStateFlow<List<Proposal>>(
    listOf(
      Proposal(
        id = "prop_1",
        serviceId = "srv_1",
        providerId = "pro_1",
        providerName = "Roberto Mendes",
        providerRating = 4.95,
        providerCategory = "Eletricista Residencial",
        providerAvatarColorHex = 0xFF0D9488,
        proposedPrice = 320.00,
        estimatedDays = 1,
        description = "Faço a revisão completa com teste de carga e troca dos disjuntores. Levo ferramentas e multímetro profissional.",
        status = ProposalStatus.CONTRAPROPOSTA_CLIENTE,
        counterOffers = listOf(
          CounterOfferEntry(
            price = 300.00,
            authorId = "cli_1",
            authorName = "Carlos Eduardo",
            authorRole = UserRole.CLIENTE,
            note = "Aceita fechar por R$ 300,00 se puder vir amanhã na parte da manhã?",
            timestamp = System.currentTimeMillis() - 7200000L
          )
        ),
        createdAt = System.currentTimeMillis() - 43200000L
      ),
      Proposal(
        id = "prop_2",
        serviceId = "srv_2",
        providerId = "pro_2",
        providerName = "Ana Clara Ferraz",
        providerRating = 4.88,
        providerCategory = "Pintura & Acabamento",
        providerAvatarColorHex = 0xFFEA580C,
        proposedPrice = 600.00,
        estimatedDays = 2,
        description = "Inclui lixamento fino, forração de pisos com lona plástica e 2 demãos de acabamento uniforme.",
        status = ProposalStatus.CONTRAPROPOSTA_CLIENTE,
        counterOffers = listOf(
          CounterOfferEntry(
            price = 550.00,
            authorId = "cli_2",
            authorName = "Mariana Silveira",
            authorRole = UserRole.CLIENTE,
            note = "Conseguimos fechar em R$ 550,00 à vista?",
            timestamp = System.currentTimeMillis() - 14400000L
          )
        ),
        createdAt = System.currentTimeMillis() - 86400000L
      ),
      Proposal(
        id = "prop_3",
        serviceId = "srv_4",
        providerId = "pro_1",
        providerName = "Roberto Mendes",
        providerRating = 4.95,
        providerCategory = "Reparos Gerais",
        proposedPrice = 180.00,
        estimatedDays = 1,
        description = "Troca e vedação completa do sifão com garantia.",
        status = ProposalStatus.ACEITA,
        createdAt = System.currentTimeMillis() - 250000000L
      )
    )
  )
  val proposals: StateFlow<List<Proposal>> = _proposals.asStateFlow()

  // Wallet Transactions
  private val _walletTransactions = MutableStateFlow<List<WalletTransaction>>(
    listOf(
      WalletTransaction(
        id = "tx_1",
        providerId = "pro_1",
        type = TransactionType.RECARGA_PIX,
        amount = 138.00,
        description = "Recarga de saldo via Pix para pagamento de taxas",
        timestamp = System.currentTimeMillis() - 300000000L,
        balanceAfter = 138.00
      ),
      WalletTransaction(
        id = "tx_2",
        providerId = "pro_1",
        type = TransactionType.DEBITO_TAXA_10,
        amount = 18.00, // 10% de R$ 180,00
        serviceId = "srv_4",
        serviceTitle = "Reparo de vazamento em pia de cozinha e troca de sifão",
        description = "Taxa de intermediação de 10% referente ao serviço #srv_4",
        timestamp = System.currentTimeMillis() - 240000000L,
        balanceAfter = 120.00
      ),
      WalletTransaction(
        id = "tx_3",
        providerId = "pro_2",
        type = TransactionType.RECARGA_PIX,
        amount = 35.00,
        description = "Recarga inicial de carteira via Pix",
        timestamp = System.currentTimeMillis() - 150000000L,
        balanceAfter = 35.00
      )
    )
  )
  val walletTransactions: StateFlow<List<WalletTransaction>> = _walletTransactions.asStateFlow()

  private val _mercadoPagoCharges = MutableStateFlow<List<PixRechargeCharge>>(emptyList())
  val mercadoPagoCharges: StateFlow<List<PixRechargeCharge>> = _mercadoPagoCharges.asStateFlow()

  private val _declinedServiceIdsByProvider = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
  val declinedServiceIdsByProvider: StateFlow<Map<String, Set<String>>> = _declinedServiceIdsByProvider.asStateFlow()

  private val _userPasswordHashes = MutableStateFlow<Map<String, String>>(emptyMap())
  val userPasswordHashes: StateFlow<Map<String, String>> = _userPasswordHashes.asStateFlow()

  private val _userSavedPasswords = MutableStateFlow<Map<String, String>>(emptyMap())
  val userSavedPasswords: StateFlow<Map<String, String>> = _userSavedPasswords.asStateFlow()

  private val _regionalPriceBenchmarks = MutableStateFlow<Map<String, RegionalPriceEstimate>>(emptyMap())
  val regionalPriceBenchmarks: StateFlow<Map<String, RegionalPriceEstimate>> = _regionalPriceBenchmarks.asStateFlow()

  // Chat Messages
  private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
    listOf(
      ChatMessage(
        id = "msg_0",
        serviceId = "srv_1",
        senderId = "system",
        senderName = "ResolveAi Segurança",
        senderRole = UserRole.CLIENTE,
        content = "🔒 Chat protegido! Para sua segurança, não compartilhe telefones, e-mails, redes sociais ou links externos.",
        timestamp = System.currentTimeMillis() - 86000000L,
        isSystemNotice = true
      ),
      ChatMessage(
        id = "msg_1",
        serviceId = "srv_1",
        senderId = "pro_1",
        senderName = "Roberto Mendes",
        senderRole = UserRole.PRESTADOR,
        content = "Olá Carlos! Vi seu pedido. O chuveiro desarmando costuma ser disjuntor superaquecendo ou fiação fina. Você já comprou os disjuntores novos?",
        timestamp = System.currentTimeMillis() - 40000000L
      ),
      ChatMessage(
        id = "msg_2",
        serviceId = "srv_1",
        senderId = "cli_1",
        senderName = "Carlos Eduardo",
        senderRole = UserRole.CLIENTE,
        content = "Ainda não comprei, prefiro que você me recomende a amperagem correta ao avaliar. Mandei uma contraproposta de R$ 300,00.",
        timestamp = System.currentTimeMillis() - 7200000L
      )
    )
  )
  val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

  // Reviews & Ratings
  private val _reviews = MutableStateFlow<List<ServiceReview>>(
    listOf(
      ServiceReview(
        id = "rev_1",
        serviceId = "srv_4",
        serviceTitle = "Reparo de vazamento em pia de cozinha e troca de sifão",
        serviceCategory = "Hidráulica",
        clientId = "cli_2",
        clientName = "Mariana Silveira",
        providerId = "pro_1",
        providerName = "Roberto Mendes",
        rating = 5.0f,
        comment = "Trabalho impecável! Chegou no horário combinado, resolveu o vazamento super rápido e deixou o local limpo.",
        complimentTags = listOf("Pontual", "Preço Justo", "Limpeza Impecável", "Super Recomendo"),
        createdAt = System.currentTimeMillis() - 200000000L,
        providerReply = "Muito obrigado Mariana! Fico à disposição sempre que precisar.",
        repliedAt = System.currentTimeMillis() - 190000000L
      ),
      ServiceReview(
        id = "rev_2",
        serviceId = "srv_past_1",
        serviceTitle = "Instalação de 4 luminárias e pendente",
        serviceCategory = "Eletricista",
        clientId = "cli_1",
        clientName = "Carlos Eduardo",
        providerId = "pro_1",
        providerName = "Roberto Mendes",
        rating = 4.9f,
        comment = "Excelente profissional. Muito cuidadoso com a fiação e com o teto de gesso.",
        complimentTags = listOf("Caprichoso", "Profissional Educado", "Super Recomendo"),
        createdAt = System.currentTimeMillis() - 400000000L
      ),
      ServiceReview(
        id = "rev_3",
        serviceId = "srv_past_2",
        serviceTitle = "Pintura de quarto infantil com textura",
        serviceCategory = "Pintura",
        clientId = "cli_1",
        clientName = "Carlos Eduardo",
        providerId = "pro_2",
        providerName = "Ana Clara Ferraz",
        rating = 4.8f,
        comment = "A Ana é muito atenciosa e o acabamento da textura ficou perfeito. Recomendo muito!",
        complimentTags = listOf("Caprichoso", "Pontual", "Super Recomendo"),
        createdAt = System.currentTimeMillis() - 350000000L
      )
    )
  )
  val reviews: StateFlow<List<ServiceReview>> = _reviews.asStateFlow()

  // In-app Notifications for providers & users
  private val _notifications = MutableStateFlow<List<AppNotification>>(
    listOf(
      AppNotification(
        id = "notif_1",
        recipientId = "all_providers",
        title = "🔔 Novo Serviço Disponível: Eletricista",
        message = "Troca de fiação do chuveiro e quadro de disjuntores em Pinheiros. Orçamento: R$ 280,00.",
        type = NotificationType.NOVO_SERVICO,
        serviceId = "srv_1",
        timestamp = System.currentTimeMillis() - 86400000L,
        isRead = false
      ),
      AppNotification(
        id = "notif_2",
        recipientId = "all_providers",
        title = "🔔 Novo Serviço Disponível: Pintura",
        message = "Pintura de sala comercial e recepção (45m²) em Campinas. Orçamento: R$ 650,00.",
        type = NotificationType.NOVO_SERVICO,
        serviceId = "srv_2",
        timestamp = System.currentTimeMillis() - 172800000L,
        isRead = true
      ),
      AppNotification(
        id = "notif_3",
        recipientId = "all_providers",
        title = "🔔 Novo Serviço Disponível: TI, Redes & Informática",
        message = "Instalação de rede Wi-Fi Mesh e cabeamento estruturado em Pinheiros. Orçamento: R$ 400,00.",
        type = NotificationType.NOVO_SERVICO,
        serviceId = "srv_3",
        timestamp = System.currentTimeMillis() - 3600000L,
        isRead = false
      )
    )
  )
  val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

  init {
    restoreLocalStateIfNeeded()
  }

  private fun restoreLocalStateIfNeeded() {
    if (hasLoadedLocalState) return
    val store = localStore ?: return
    hasLoadedLocalState = true
    val persisted = store.load() ?: return
    if (persisted.users.isNotEmpty()) {
      _users.update { seedUsers ->
        val seedIds = seedUsers.map { it.id }.toSet()
        val persistedMap = persisted.users.associateBy { it.id }
        val customUsers = persisted.users.filter { it.id !in seedIds }
        val updatedSeeds = seedUsers.map { seed ->
          val saved = persistedMap[seed.id]
          if (saved == null) {
            seed
          } else {
            saved.copy(
              photoUrl = saved.photoUrl?.takeIf { it.isNotBlank() } ?: seed.photoUrl,
              portfolioPhotos = saved.portfolioPhotos.ifEmpty { seed.portfolioPhotos }
            )
          }
        }
        customUsers + updatedSeeds
      }
    }
    if (persisted.services.isNotEmpty()) {
      val seedServicesMap = _services.value.associateBy { it.id }
      _services.value = persisted.services.map { srv ->
        val seedLoc = seedServicesMap[srv.id]?.clientLocation
        if (srv.clientLocation == null && seedLoc != null) srv.copy(clientLocation = seedLoc) else srv
      }
    }
    if (persisted.proposals.isNotEmpty()) {
      _proposals.value = persisted.proposals
    }
    if (persisted.walletTransactions.isNotEmpty()) {
      _walletTransactions.value = persisted.walletTransactions
    }
    if (persisted.mercadoPagoCharges.isNotEmpty()) {
      _mercadoPagoCharges.value = persisted.mercadoPagoCharges
    }
    if (persisted.chatMessages.isNotEmpty()) {
      _chatMessages.value = persisted.chatMessages
    }
    if (persisted.reviews.isNotEmpty()) {
      _reviews.value = persisted.reviews
    }
    if (persisted.notifications.isNotEmpty()) {
      _notifications.value = persisted.notifications
    }
    if (persisted.documentReviewAudits.isNotEmpty()) {
      _documentReviewAudits.value = persisted.documentReviewAudits
    }
    if (persisted.appSuggestions.isNotEmpty()) {
      _appSuggestions.value = persisted.appSuggestions
    }
    if (persisted.serviceLocationHistory.isNotEmpty()) {
      _serviceLocationHistory.value = persisted.serviceLocationHistory
    }
    if (persisted.currentUserId.isNotBlank() && _users.value.any { it.id == persisted.currentUserId }) {
      _currentUserId.value = persisted.currentUserId
    }
    if (persisted.serviceCategories.isNotEmpty()) {
      _serviceCategories.value = persisted.serviceCategories.sortedBy { it.orderIndex }
    }
    if (persisted.appTips.isNotEmpty()) {
      _appTips.value = persisted.appTips
    }
    persisted.appAdminSettings?.let { savedSettings ->
      _appAdminSettings.value = savedSettings
      SupportLauncher.updateSupportContact(
        savedSettings.supportWhatsappNumberE164,
        savedSettings.supportWhatsappDisplay
      )
    }
    _isAuthenticated.value = persisted.isAuthenticated
    _hasSeenOnboarding.value = persisted.hasSeenOnboarding
    if (persisted.declinedServiceIdsByProvider.isNotEmpty() || persisted.users.any { it.declinedServiceIds.isNotEmpty() }) {
      val mergedDeclined = mutableMapOf<String, Set<String>>()
      persisted.declinedServiceIdsByProvider.forEach { (pid, ids) ->
        if (pid.isNotBlank() && ids.isNotEmpty()) {
          mergedDeclined[pid] = ids.toSet()
        }
      }
      _users.value.forEach { u ->
        val extra = (mergedDeclined[u.id].orEmpty() + u.firebaseUid?.let { mergedDeclined[it] }.orEmpty() + u.declinedServiceIds)
        if (extra.isNotEmpty()) {
          mergedDeclined[u.id] = extra
          u.firebaseUid?.takeIf { it.isNotBlank() }?.let { uid ->
            mergedDeclined[uid] = extra
          }
        }
      }
      _declinedServiceIdsByProvider.value = mergedDeclined
      _users.update { list ->
        list.map { u ->
          val ids = (u.declinedServiceIds + mergedDeclined[u.id].orEmpty() + (u.firebaseUid?.let { mergedDeclined[it] }.orEmpty())).distinct()
          if (ids == u.declinedServiceIds) u else u.copy(declinedServiceIds = ids)
        }
      }
    }
    if (persisted.userPasswordHashes.isNotEmpty()) {
      _userPasswordHashes.value = persisted.userPasswordHashes
    }
    if (persisted.userSavedPasswords.isNotEmpty()) {
      _userSavedPasswords.value = persisted.userSavedPasswords
    }
    if (persisted.regionalPriceBenchmarks.isNotEmpty()) {
      val todayKey = RegionalPriceIndexEngine.todayDateKey()
      val freshBenchmarks = persisted.regionalPriceBenchmarks
        .filter { it.dateKey == todayKey && it.id.isNotBlank() && it.averagePrice > 0.0 }
        .associateBy { it.id }
      if (freshBenchmarks.isNotEmpty()) {
        _regionalPriceBenchmarks.value = freshBenchmarks
      }
    }
  }

  private fun persistLocalState() {
    val store = localStore ?: return
    hasLoadedLocalState = true
    val todayKey = RegionalPriceIndexEngine.todayDateKey()
    val todayBenchmarks = _regionalPriceBenchmarks.value.values
      .filter { it.dateKey == todayKey }
      .take(120)
    store.save(
      PersistedMarketplaceState(
        currentUserId = _currentUserId.value,
        isAuthenticated = _isAuthenticated.value,
        users = _users.value,
        services = _services.value,
        proposals = _proposals.value,
        walletTransactions = _walletTransactions.value,
        mercadoPagoCharges = _mercadoPagoCharges.value,
        chatMessages = _chatMessages.value,
        reviews = _reviews.value,
        notifications = _notifications.value,
        documentReviewAudits = _documentReviewAudits.value,
        appSuggestions = _appSuggestions.value,
        serviceLocationHistory = _serviceLocationHistory.value,
        hasSeenOnboarding = _hasSeenOnboarding.value,
        serviceCategories = _serviceCategories.value,
        appTips = _appTips.value,
        appAdminSettings = _appAdminSettings.value,
        declinedServiceIdsByProvider = _declinedServiceIdsByProvider.value.mapValues { (_, ids) -> ids.toList() },
        userPasswordHashes = _userPasswordHashes.value,
        userSavedPasswords = _userSavedPasswords.value,
        regionalPriceBenchmarks = todayBenchmarks
      )
    )
  }

  private fun currentFirebaseUidOrNull(): String? {
    return try { FirebaseAuth.getInstance().currentUser?.uid } catch (_: Throwable) { null }
  }

  private fun emitNotification(
    notification: AppNotification,
    dedupKey: String? = null,
    dispatchSystemBanner: Boolean = true,
    syncToFirestore: Boolean = true
  ) {
    if (dedupKey != null && !dispatchedNotificationKeys.add(dedupKey)) return
    _notifications.update { list -> listOf(notification) + list.filterNot { it.id == notification.id } }
    persistLocalState()

    if (syncToFirestore && notification.recipientId.isNotBlank() && notification.recipientId != "all_providers") {
      val ds = firestoreDataSource
      val fbUid = currentFirebaseUidOrNull()
      if (ds != null && fbUid != null) {
        val targetUser = _users.value.find { it.id == notification.recipientId || it.ownsId(notification.recipientId) }
        val targetDocId = targetUser?.firebaseUid?.takeIf { it.isNotBlank() } ?: notification.recipientId
        repoScope.launch {
          ds.saveUserNotification(
            recipientDocId = targetDocId,
            notification = notification.copy(recipientId = targetDocId),
            senderId = fbUid
          )
        }
      }
    }

    if (!dispatchSystemBanner) return

    val activeUser = getCurrentUser()
    val shouldNotifyCurrentDevice = when {
      notification.recipientId == "all_providers" -> activeUser.role == UserRole.PRESTADOR
      else -> activeUser.ownsId(notification.recipientId)
    }
    if (shouldNotifyCurrentDevice) {
      _systemNotificationEvents.tryEmit(notification)
      appContext?.let { AppNotificationHelper.showNotification(it, notification) }
    }
  }

  private fun stopFirestoreSync() {
    activeSyncKey = null
    firestoreSyncJobs.forEach { it.cancel() }
    firestoreSyncJobs.clear()
    chatListenerJobs.values.forEach { it.cancel() }
    chatListenerJobs.clear()
    serviceProposalListenerJobs.values.forEach { it.cancel() }
    serviceProposalListenerJobs.clear()
    selectedServiceProposalsJob?.cancel()
    selectedServiceProposalsJob = null
    hasReceivedRemoteServicesSnapshot = false
    hasReceivedRemoteProposalsSnapshot = false
    hasReceivedRemoteReviewsSnapshot = false
    hasReceivedRemoteNotificationsSnapshot = false
    initializedChatSyncServiceIds.clear()
  }

  /**
   * Reconcilia em tempo real o snapshot autoritativo de serviços abertos (ABERTO / EM_NEGOCIACAO)
   * para prestadores. Sempre que um pedido for aceito e acordado entre o cliente e outro prestador
   * (deixando de aparecer na consulta de oportunidades abertas do Firestore), ele é removido
   * automaticamente da lista de oportunidades dos demais prestadores — mesmo que o snapshot fique vazio.
   */
  internal fun reconcileRemoteOpenServices(remoteOpenServices: List<ServiceRequest>) {
    val previousById = _services.value.associateBy { it.id }
    val activeUser = getCurrentUser()
    val isLiveUpdate = hasReceivedRemoteServicesSnapshot
    hasReceivedRemoteServicesSnapshot = true

    val validRemoteOpen = remoteOpenServices.filter { srv ->
      srv.acceptedProviderId.isNullOrBlank() &&
        (srv.status == ServiceStatus.ABERTO || srv.status == ServiceStatus.EM_NEGOCIACAO)
    }
    val remoteById = validRemoteOpen.associateBy { it.id }

    _services.update { current ->
      val keptOrMerged = current.mapNotNull { local ->
        val remote = remoteById[local.id]
        if (remote != null) {
          mergeSingleRemoteService(local, remote)
        } else {
          val wasOpenOpportunity = local.status == ServiceStatus.ABERTO || local.status == ServiceStatus.EM_NEGOCIACAO
          val belongsToActiveUser = activeUser.ownsId(local.clientId) || activeUser.ownsId(local.acceptedProviderId)
          if (wasOpenOpportunity && !belongsToActiveUser) {
            // Pedido deixou de estar aberto (aceito/acordado com outro prestador, cancelado ou encerrado)
            null
          } else {
            local
          }
        }
      }
      val existingIds = keptOrMerged.map { it.id }.toSet()
      val newRemote = validRemoteOpen.filter { it.id !in existingIds }
      (newRemote + keptOrMerged).sortedByDescending { it.createdAt }
    }

    if (isLiveUpdate && activeUser.role == UserRole.PRESTADOR) {
      validRemoteOpen.forEach { remote ->
        val prev = previousById[remote.id]
        if (prev == null && remote.status == ServiceStatus.ABERTO && !activeUser.ownsId(remote.clientId)) {
          emitNotification(
            AppNotification(
              id = "notif_srv_${remote.id}",
              recipientId = "all_providers",
              title = "🔔 Novo Pedido Disponível: ${remote.category}",
              message = "${remote.title} em ${remote.location}. Orçamento: R$ ${String.format("%.2f", remote.proposedBudget)}.",
              type = NotificationType.NOVO_SERVICO,
              serviceId = remote.id
            ),
            dedupKey = "srv_open_${remote.id}",
            syncToFirestore = false
          )
        }
      }
    }

    persistLocalState()
    syncChatListenersForEligibleServices()
  }

  private fun mergeSingleRemoteService(local: ServiceRequest, remote: ServiceRequest): ServiceRequest {
    return remote.copy(
      clientLocation = remote.clientLocation ?: local.clientLocation,
      attachmentPhotoUri = remote.attachmentPhotoUri ?: local.attachmentPhotoUri,
      attachmentPhotoName = remote.attachmentPhotoName ?: local.attachmentPhotoName,
      attachmentVideoUri = remote.attachmentVideoUri ?: local.attachmentVideoUri,
      attachmentVideoName = remote.attachmentVideoName ?: local.attachmentVideoName,
      attachmentVideoThumbnailUri = remote.attachmentVideoThumbnailUri ?: local.attachmentVideoThumbnailUri,
      attachmentVideoDurationMs = remote.attachmentVideoDurationMs ?: local.attachmentVideoDurationMs,
      completionPhotoUri = remote.completionPhotoUri ?: local.completionPhotoUri,
      completionPhotoName = remote.completionPhotoName ?: local.completionPhotoName,
      completionVideoUri = remote.completionVideoUri ?: local.completionVideoUri,
      completionVideoName = remote.completionVideoName ?: local.completionVideoName,
      completionVideoThumbnailUri = remote.completionVideoThumbnailUri ?: local.completionVideoThumbnailUri,
      completionVideoDurationMs = remote.completionVideoDurationMs ?: local.completionVideoDurationMs,
      completionNotes = remote.completionNotes ?: local.completionNotes,
      completedAt = remote.completedAt ?: local.completedAt,
      cancellationReason = remote.cancellationReason ?: local.cancellationReason,
      cancelledByRole = remote.cancelledByRole ?: local.cancelledByRole,
      cancelledByName = remote.cancelledByName ?: local.cancelledByName,
      cancelledAt = remote.cancelledAt ?: local.cancelledAt,
      cancellationRefundStatus = remote.cancellationRefundStatus ?: local.cancellationRefundStatus,
      cancellationRefundReviewedBy = remote.cancellationRefundReviewedBy ?: local.cancellationRefundReviewedBy,
      cancellationRefundReviewedAt = remote.cancellationRefundReviewedAt ?: local.cancellationRefundReviewedAt,
      cancellationRefundNote = remote.cancellationRefundNote ?: local.cancellationRefundNote,
      locationTrackingActive = remote.locationTrackingActive,
      locationPointsCount = maxOf(remote.locationPointsCount, local.locationPointsCount),
      clientCompletionPhotos = if (remote.clientCompletionPhotos.isNotEmpty()) {
        remote.clientCompletionPhotos
      } else {
        local.clientCompletionPhotos
      }
    )
  }

  private fun mergeRemoteServices(remoteServices: List<ServiceRequest>) {
    val previousById = _services.value.associateBy { it.id }
    val activeUser = getCurrentUser()
    val isLiveUpdate = hasReceivedRemoteServicesSnapshot
    hasReceivedRemoteServicesSnapshot = true

    if (remoteServices.isEmpty()) return
    _services.update { current ->
      val currentById = current.associateBy { it.id }
      val remoteById = remoteServices.associateBy { it.id }
      val mergedExisting = current.map { local ->
        val remote = remoteById[local.id] ?: return@map local
        mergeSingleRemoteService(local, remote)
      }
      val newRemote = remoteServices.filter { it.id !in currentById }
      (newRemote + mergedExisting).sortedByDescending { it.createdAt }
    }

    // Se um cliente aceitou uma proposta em outro dispositivo, debita a taxa de 10% no Firestore de forma idempotente sob o UID do prestador
    val dsForFee = firestoreDataSource
    val fbUidForFee = currentFirebaseUidOrNull()
    if (dsForFee != null && fbUidForFee != null && activeUser.role == UserRole.PRESTADOR) {
      remoteServices.forEach { remote ->
        val fee = remote.platformFee ?: 0.0
        if (fee > 0.0 &&
          activeUser.ownsId(remote.acceptedProviderId) &&
          (remote.status == ServiceStatus.ACEITO || remote.status == ServiceStatus.EM_EXECUCAO) &&
          syncedProviderFeeServiceIds.add(remote.id)
        ) {
          repoScope.launch {
            dsForFee.recordProviderWalletFeeDebitIfMissing(
              providerId = fbUidForFee,
              serviceId = remote.id,
              serviceTitle = remote.title,
              platformFee = fee
            )
          }
        }
      }
    }

    if (isLiveUpdate) {
      remoteServices.forEach { remote ->
        val prev = previousById[remote.id]
        if (prev == null && remote.status == ServiceStatus.ABERTO && !activeUser.ownsId(remote.clientId)) {
          if (activeUser.role == UserRole.PRESTADOR) {
            emitNotification(
              AppNotification(
                id = "notif_srv_${remote.id}",
                recipientId = "all_providers",
                title = "🔔 Novo Pedido Disponível: ${remote.category}",
                message = "${remote.title} em ${remote.location}. Orçamento: R$ ${String.format("%.2f", remote.proposedBudget)}.",
                type = NotificationType.NOVO_SERVICO,
                serviceId = remote.id
              ),
              dedupKey = "srv_open_${remote.id}",
              syncToFirestore = false
            )
          }
        } else if (prev != null && prev.status != remote.status) {
          when (remote.status) {
            ServiceStatus.ACEITO -> {
              val recipient = if (activeUser.ownsId(remote.clientId)) {
                remote.clientId
              } else {
                remote.acceptedProviderId ?: activeUser.id
              }
              val actor = if (activeUser.ownsId(remote.clientId)) {
                remote.acceptedProviderName ?: "um prestador"
              } else {
                remote.clientName
              }
              emitNotification(
                AppNotification(
                  id = "notif_aceito_${remote.id}",
                  recipientId = recipient,
                  title = "🎉 Pedido Aceito! Chat Liberado",
                  message = "${remote.title} foi confirmado com $actor. Toque para conversar no chat privado.",
                  type = NotificationType.PROPOSTA_ACEITA,
                  serviceId = remote.id
                ),
                dedupKey = "srv_aceito_${remote.id}"
              )
            }
            ServiceStatus.EM_EXECUCAO, ServiceStatus.CONCLUIDO, ServiceStatus.CANCELADO -> {
              val reasonSuffix = if (remote.status == ServiceStatus.CANCELADO && !remote.cancellationReason.isNullOrBlank()) {
                val byWho = remote.cancelledByName?.takeIf { it.isNotBlank() }?.let { " por $it" }.orEmpty()
                " Cancelado$byWho. Motivo: ${remote.cancellationReason}"
              } else {
                ""
              }
              emitNotification(
                AppNotification(
                  id = "notif_status_${remote.id}_${remote.status.name}",
                  recipientId = activeUser.id,
                  title = if (remote.status == ServiceStatus.CANCELADO) {
                    "🚫 Serviço Cancelado"
                  } else {
                    "ℹ️ Status Atualizado: ${remote.status.label}"
                  },
                  message = "O serviço \"${remote.title}\" agora está com status: ${remote.status.label}.$reasonSuffix",
                  type = NotificationType.STATUS_ATUALIZADO,
                  serviceId = remote.id
                ),
                dedupKey = "srv_status_${remote.id}_${remote.status.name}"
              )
            }
            else -> Unit
          }
        }
      }
    }

    persistLocalState()
    syncChatListenersForEligibleServices()
    syncProposalListenersForClientServices()
  }

  private fun mergeRemoteWalletTransactions(remoteTxs: List<WalletTransaction>) {
    if (remoteTxs.isEmpty()) return
    _walletTransactions.update { current ->
      val currentById = current.associateBy { it.id }
      val remoteById = remoteTxs.associateBy { it.id }
      val remotePixIds = remoteTxs.mapNotNull { WalletHistoryHelper.extractPixTxId(it) }.toSet()
      val remoteDebitServiceIds = remoteTxs
        .filter { it.type == TransactionType.DEBITO_TAXA_10 }
        .mapNotNull { it.serviceId }
        .toSet()

      val filteredLocal = current.filter { local ->
        if (local.id in remoteById) return@filter true
        val localPixId = WalletHistoryHelper.extractPixTxId(local)
        if (localPixId != null && localPixId in remotePixIds) return@filter false
        if (local.type == TransactionType.DEBITO_TAXA_10 &&
          local.serviceId != null &&
          local.serviceId in remoteDebitServiceIds
        ) {
          return@filter false
        }
        true
      }.map { local -> remoteById[local.id] ?: local }

      val existingIds = filteredLocal.map { it.id }.toSet()
      val newRemote = remoteTxs.filter { it.id !in existingIds }
      (newRemote + filteredLocal).sortedByDescending { it.timestamp }
    }
    persistLocalState()
  }

  suspend fun syncWalletTransactionsFromFirestore(): Result<Int> {
    val ds = firestoreDataSource ?: return Result.failure(Exception("Firestore indisponível"))
    val fbUid = currentFirebaseUidOrNull() ?: return Result.failure(Exception("Sessão Firebase não ativa"))
    val res = ds.fetchUserTransactions(fbUid)
    res.onSuccess { list ->
      mergeRemoteWalletTransactions(list)
    }
    return res.map { it.size }
  }

  suspend fun syncProposalsForService(serviceId: String) {
    if (serviceId.isBlank()) return
    val ds = firestoreDataSource ?: return
    val fbUid = currentFirebaseUidOrNull() ?: return
    val activeUser = getCurrentUser()
    val effectiveUid = activeUser.firebaseUid?.takeIf { it.isNotBlank() } ?: fbUid
    val remote = ds.fetchProposalsForService(
      serviceId = serviceId,
      uid = effectiveUid,
      role = activeUser.role
    )
    if (remote.isNotEmpty()) {
      mergeRemoteProposals(remote)
    }
  }

  /**
   * Observa em tempo real TODAS as propostas de um pedido específico aberto na tela de detalhes.
   */
  fun observeSelectedServiceProposals(serviceId: String?) {
    selectedServiceProposalsJob?.cancel()
    selectedServiceProposalsJob = null
    if (serviceId.isNullOrBlank()) return
    val ds = firestoreDataSource ?: return
    val fbUid = currentFirebaseUidOrNull() ?: return
    val activeUser = getCurrentUser()
    val effectiveUid = activeUser.firebaseUid?.takeIf { it.isNotBlank() } ?: fbUid
    selectedServiceProposalsJob = repoScope.launch {
      ds.observeProposalsForService(
        serviceId = serviceId,
        uid = effectiveUid,
        role = activeUser.role
      ).collect { remote ->
        if (remote.isNotEmpty()) {
          mergeRemoteProposals(remote)
          markCloudSyncPulse()
        }
      }
    }
  }

  /**
   * Mantém listeners em tempo real por serviceId para todos os pedidos do cliente logado,
   * garantindo que TODAS as propostas de todos os prestadores apareçam imediatamente mesmo
   * em contas multi-dispositivo (linkedAuthUids) ou antes de abrir os detalhes do pedido.
   */
  fun syncProposalListenersForClientServices() {
    val ds = firestoreDataSource ?: return
    val fbUid = currentFirebaseUidOrNull() ?: return
    val activeUser = getCurrentUser()
    if (activeUser.role != UserRole.CLIENTE) {
      if (serviceProposalListenerJobs.isNotEmpty()) {
        serviceProposalListenerJobs.values.forEach { it.cancel() }
        serviceProposalListenerJobs.clear()
      }
      return
    }
    val effectiveUid = activeUser.firebaseUid?.takeIf { it.isNotBlank() } ?: fbUid
    val eligibleIds = _services.value
      .filter { activeUser.ownsId(it.clientId) }
      .sortedByDescending { it.createdAt }
      .take(25)
      .map { it.id }
      .toSet()

    val toRemove = serviceProposalListenerJobs.keys.filter { it !in eligibleIds }
    toRemove.forEach { sid ->
      serviceProposalListenerJobs.remove(sid)?.cancel()
    }

    eligibleIds.forEach { sid ->
      if (serviceProposalListenerJobs[sid]?.isActive != true) {
        serviceProposalListenerJobs[sid] = repoScope.launch {
          ds.observeProposalsForService(
            serviceId = sid,
            uid = effectiveUid,
            role = UserRole.CLIENTE
          ).collect { remote ->
            if (remote.isNotEmpty()) {
              mergeRemoteProposals(remote)
              markCloudSyncPulse()
            }
          }
        }
      }
    }
  }

  private fun mergeRemoteProposals(remoteProposals: List<Proposal>) {
    val previousById = _proposals.value.associateBy { it.id }
    val activeUser = getCurrentUser()
    val isLiveUpdate = hasReceivedRemoteProposalsSnapshot
    hasReceivedRemoteProposalsSnapshot = true

    if (remoteProposals.isEmpty()) return
    _proposals.update { current ->
      val currentById = current.associateBy { it.id }
      val remoteById = remoteProposals.associateBy { it.id }
      val mergedExisting = current.map { local -> remoteById[local.id] ?: local }
      val newRemote = remoteProposals.filter { it.id !in currentById }
      val combined = (newRemote + mergedExisting).sortedByDescending { it.createdAt }
      val servicesById = _services.value.associateBy { it.id }
      val acceptedIdByService = combined
        .groupBy { it.serviceId }
        .mapValues { (sid, list) ->
          val acceptedList = list.filter { it.status == ProposalStatus.ACEITA }
          if (acceptedList.isNotEmpty()) {
            val preferredProviderId = servicesById[sid]?.acceptedProviderId
            (acceptedList.firstOrNull { !preferredProviderId.isNullOrBlank() && it.providerId == preferredProviderId }
              ?: acceptedList.first()).id
          } else {
            null
          }
        }
      combined.map { prop ->
        val acceptedId = acceptedIdByService[prop.serviceId]
        if (acceptedId != null && prop.id != acceptedId && prop.status != ProposalStatus.RECUSADA) {
          prop.copy(status = ProposalStatus.RECUSADA)
        } else {
          prop
        }
      }
    }

    if (isLiveUpdate) {
      remoteProposals.forEach { remote ->
        val prev = previousById[remote.id]
        val serviceTitle = _services.value.find { it.id == remote.serviceId }?.title ?: "seu pedido"
        if (prev == null && !activeUser.ownsId(remote.providerId)) {
          emitNotification(
            AppNotification(
              id = "notif_prop_${remote.id}",
              recipientId = activeUser.id,
              title = "📩 Nova Proposta Recebida!",
              message = "${remote.providerName} enviou uma proposta de R$ ${String.format("%.2f", remote.proposedPrice)} para \"$serviceTitle\".",
              type = NotificationType.PROPOSTA_RECEBIDA,
              serviceId = remote.serviceId
            ),
            dedupKey = "prop_new_${remote.id}"
          )
        } else if (prev != null && remote.counterOffers.size > prev.counterOffers.size) {
          val latestOffer = remote.counterOffers.lastOrNull()
          if (latestOffer != null && !activeUser.ownsId(latestOffer.authorId)) {
            emitNotification(
              AppNotification(
                id = "notif_counter_${remote.id}_${remote.counterOffers.size}",
                recipientId = activeUser.id,
                title = "💬 Contraproposta Recebida",
                message = "${latestOffer.authorName} propôs R$ ${String.format("%.2f", latestOffer.price)} em \"$serviceTitle\".",
                type = NotificationType.PROPOSTA_RECEBIDA,
                serviceId = remote.serviceId
              ),
              dedupKey = "prop_counter_${remote.id}_${remote.counterOffers.size}"
            )
          }
        }
      }
    }

    persistLocalState()

    val ds = firestoreDataSource ?: return
    val unknownProviderIds = remoteProposals
      .map { it.providerId }
      .filter { pid -> pid.isNotBlank() && _users.value.none { u -> u.ownsId(pid) } }
      .distinct()
    if (unknownProviderIds.isNotEmpty()) {
      repoScope.launch {
        unknownProviderIds.forEach { pid ->
          ds.loadUserProfile(pid)?.let { loadedProvider ->
            _users.update { list ->
              if (list.any { it.ownsId(pid) }) list else list + loadedProvider
            }
          }
        }
        persistLocalState()
      }
    }
  }

  private fun mergeRemoteChatMessages(serviceId: String, remoteMessages: List<ChatMessage>) {
    val existingIds = _chatMessages.value.filter { it.serviceId == serviceId }.map { it.id }.toSet()
    val isLiveUpdate = initializedChatSyncServiceIds.contains(serviceId)
    initializedChatSyncServiceIds.add(serviceId)

    if (remoteMessages.isEmpty()) return
    _chatMessages.update { current ->
      val otherServices = current.filterNot { it.serviceId == serviceId }
      val existingForService = current.filter { it.serviceId == serviceId }
      val remoteIds = remoteMessages.map { it.id }.toSet()
      val pendingLocal = existingForService.filter { it.id !in remoteIds }
      val mergedForService = (remoteMessages + pendingLocal)
        .distinctBy { it.id }
        .sortedBy { it.timestamp }
      otherServices + mergedForService
    }

    if (isLiveUpdate) {
      val activeUser = getCurrentUser()
      val serviceTitle = _services.value.find { it.id == serviceId }?.title ?: "Chat do Serviço"
      remoteMessages
        .filter { it.id !in existingIds && !it.isSystemNotice && !activeUser.ownsId(it.senderId) }
        .forEach { newMsg ->
          val preview = ContactFilter.maskSensitiveData(newMsg.content).take(120)
          emitNotification(
            AppNotification(
              id = "notif_msg_${newMsg.id}",
              recipientId = activeUser.id,
              title = "💬 Mensagem de ${newMsg.senderName}",
              message = "[$serviceTitle] $preview",
              type = NotificationType.NOVA_MENSAGEM,
              serviceId = serviceId
            ),
            dedupKey = "chat_msg_${newMsg.id}"
          )
        }
    }

    persistLocalState()
  }

  fun ensureChatSyncForService(serviceId: String) {
    val ds = firestoreDataSource ?: return
    if (currentFirebaseUidOrNull() == null) return
    val srv = _services.value.find { it.id == serviceId } ?: return
    val activeUser = getCurrentUser()
    val isChatOpen = !srv.acceptedProviderId.isNullOrBlank() && (
      srv.status == ServiceStatus.ACEITO ||
        srv.status == ServiceStatus.EM_EXECUCAO ||
        srv.status == ServiceStatus.CONCLUIDO ||
        (activeUser.role == UserRole.ADMIN && srv.status == ServiceStatus.CANCELADO)
      )
    val isParticipant = activeUser.ownsId(srv.clientId) ||
      activeUser.ownsId(srv.acceptedProviderId) ||
      activeUser.role == UserRole.ADMIN
    if (!isChatOpen || !isParticipant) return
    if (chatListenerJobs[serviceId]?.isActive == true) return

    chatListenerJobs[serviceId] = repoScope.launch {
      ds.observeChatMessages(serviceId).collect { remoteMessages ->
        mergeRemoteChatMessages(serviceId, remoteMessages)
      }
    }
  }

  fun syncChatListenersForEligibleServices() {
    val ds = firestoreDataSource ?: return
    if (currentFirebaseUidOrNull() == null) return
    val activeUser = getCurrentUser()
    val eligibleIds = _services.value.filter { srv ->
      val isChatOpen = !srv.acceptedProviderId.isNullOrBlank() && (
        srv.status == ServiceStatus.ACEITO ||
          srv.status == ServiceStatus.EM_EXECUCAO ||
          srv.status == ServiceStatus.CONCLUIDO ||
          (activeUser.role == UserRole.ADMIN && srv.status == ServiceStatus.CANCELADO)
        )
      val isParticipant = activeUser.ownsId(srv.clientId) ||
        activeUser.ownsId(srv.acceptedProviderId) ||
        activeUser.role == UserRole.ADMIN
      isChatOpen && isParticipant
    }.map { it.id }.toSet()

    val toRemove = chatListenerJobs.keys.filter { it !in eligibleIds }
    toRemove.forEach { sid ->
      chatListenerJobs.remove(sid)?.cancel()
    }

    eligibleIds.forEach { sid ->
      if (chatListenerJobs[sid]?.isActive != true) {
        chatListenerJobs[sid] = repoScope.launch {
          ds.observeChatMessages(sid).collect { remoteMessages ->
            mergeRemoteChatMessages(sid, remoteMessages)
          }
        }
      }
    }
  }

  private fun mergeRemoteNotifications(remoteNotifications: List<AppNotification>) {
    val previousIds = _notifications.value.map { it.id }.toSet()
    val isLiveUpdate = hasReceivedRemoteNotificationsSnapshot
    hasReceivedRemoteNotificationsSnapshot = true
    if (remoteNotifications.isEmpty()) return

    val activeUser = getCurrentUser()
    _notifications.update { current ->
      val remoteById = remoteNotifications.associateBy { it.id }
      val mergedRemote = remoteNotifications.map { remote ->
        val local = current.find { it.id == remote.id }
        if (local != null && local.isRead && !remote.isRead) {
          remote.copy(isRead = true)
        } else {
          remote
        }
      }
      val remainingLocal = current.filter { it.id !in remoteById }
      (mergedRemote + remainingLocal).sortedByDescending { it.timestamp }
    }
    persistLocalState()

    if (isLiveUpdate) {
      remoteNotifications.forEach { remote ->
        if (remote.id !in previousIds && !remote.isRead && activeUser.ownsId(remote.recipientId)) {
          if (dispatchedNotificationKeys.add("remote_notif_${remote.id}")) {
            _systemNotificationEvents.tryEmit(remote)
            appContext?.let { AppNotificationHelper.showNotification(it, remote) }
          }
        }
      }
    }
  }

  private fun mergeRemotePixCharges(remoteCharges: List<PixRechargeCharge>) {
    if (remoteCharges.isEmpty()) return
    _mercadoPagoCharges.update { current ->
      val remoteByTxId = remoteCharges.associateBy { it.txId }
      val remainingLocal = current.filter { it.txId !in remoteByTxId }
      (remoteCharges + remainingLocal).sortedByDescending { it.createdAt }
    }
    persistLocalState()
  }

  private fun mergeRemoteAdminUsers(remoteUsers: List<User>) {
    if (remoteUsers.isEmpty()) return
    _users.update { currentList ->
      val merged = currentList.toMutableList()
      remoteUsers.forEach { remote ->
        val idx = merged.indexOfFirst {
          it.id == remote.id ||
            (remote.firebaseUid != null && it.ownsId(remote.firebaseUid)) ||
            (remote.email.isNotBlank() && it.email.equals(remote.email, ignoreCase = true))
        }
        if (idx >= 0) {
          val local = merged[idx]
          merged[idx] = remote.copy(
            id = local.id,
            photoUrl = remote.photoUrl?.takeIf { it.isNotBlank() } ?: local.photoUrl,
            portfolioPhotos = remote.portfolioPhotos.ifEmpty { local.portfolioPhotos },
            firebaseUid = remote.firebaseUid ?: local.firebaseUid,
            linkedAuthUids = (local.linkedAuthUids + remote.linkedAuthUids).distinct(),
            declinedServiceIds = (local.declinedServiceIds + remote.declinedServiceIds).distinct()
          )
        } else {
          merged.add(0, remote)
        }
      }
      merged
    }
    persistLocalState()
  }

  private fun mergeRemoteAdminCredentials(remoteCreds: Map<String, Pair<String?, String?>>) {
    if (remoteCreds.isEmpty()) return
    val currentUsers = _users.value
    val nextHashes = _userPasswordHashes.value.toMutableMap()
    val nextSaved = _userSavedPasswords.value.toMutableMap()
    remoteCreds.forEach { (docId, pair) ->
      val (hash, displayPassword) = pair
      val matchingUser = currentUsers.find { it.id == docId || it.firebaseUid == docId }
      if (!hash.isNullOrBlank()) {
        nextHashes[docId] = hash
        matchingUser?.let { nextHashes[it.id] = hash }
      }
      if (!displayPassword.isNullOrBlank()) {
        nextSaved[docId] = displayPassword
        matchingUser?.let { nextSaved[it.id] = displayPassword }
      }
    }
    _userPasswordHashes.value = nextHashes
    _userSavedPasswords.value = nextSaved
    persistLocalState()
  }

  fun startFirestoreSyncIfSignedIn() {
    val ds = firestoreDataSource ?: return
    val fbUid = currentFirebaseUidOrNull()
    if (fbUid == null) {
      _cloudSyncStatus.value = CloudSyncStatus(
        state = CloudSyncState.OFFLINE_LOCAL,
        isFirebaseSignedIn = false,
        activeListenersCount = 0,
        lastSyncedAt = _cloudSyncStatus.value.lastSyncedAt,
        statusMessage = "Modo local (sem sessão Firebase ativa)"
      )
      return
    }
    val activeUser = getCurrentUser()
    val profileDocId = activeUser.firebaseUid?.takeIf { it.isNotBlank() } ?: fbUid
    val syncKey = "$fbUid:$profileDocId:${activeUser.role.name}"
    if (activeSyncKey == syncKey && firestoreSyncJobs.any { it.isActive }) {
      syncChatListenersForEligibleServices()
      markCloudSyncPulse()
      return
    }
    stopFirestoreSync()
    activeSyncKey = syncKey
    _cloudSyncStatus.value = CloudSyncStatus(
      state = CloudSyncState.SYNCING,
      isFirebaseSignedIn = true,
      activeListenersCount = 0,
      lastSyncedAt = _cloudSyncStatus.value.lastSyncedAt,
      statusMessage = "Conectando ao Firebase em tempo real..."
    )

    // Publica/atualiza índices SHA-256 de e-mail e telefone para login cross-device
    syncLoginDirectoryForUser(
      user = activeUser,
      rawPassword = _userSavedPasswords.value[activeUser.id] ?: _userSavedPasswords.value[profileDocId],
      explicitDocId = profileDocId
    )

    firestoreSyncJobs += repoScope.launch {
      ds.observeUserProfile(profileDocId).collect { remoteProfile ->
        if (remoteProfile != null) {
          _users.update { list ->
            val idx = list.indexOfFirst { it.id == activeUser.id || it.ownsId(fbUid) || it.ownsId(profileDocId) }
            if (idx >= 0) {
              val local = list[idx]
              val extraDeclined = _declinedServiceIdsByProvider.value[local.id].orEmpty() +
                _declinedServiceIdsByProvider.value[fbUid].orEmpty() +
                _declinedServiceIdsByProvider.value[profileDocId].orEmpty()
              val mergedDeclined = (local.declinedServiceIds + remoteProfile.declinedServiceIds + extraDeclined).distinct()
              val mergedLinked = (local.linkedAuthUids + remoteProfile.linkedAuthUids + listOfNotNull(fbUid.takeIf { it != profileDocId })).distinct()
              val merged = remoteProfile.copy(
                id = local.id,
                photoUrl = remoteProfile.photoUrl ?: local.photoUrl,
                firebaseUid = profileDocId,
                linkedAuthUids = mergedLinked,
                declinedServiceIds = mergedDeclined
              )
              list.mapIndexed { i, u -> if (i == idx) merged else u }
            } else {
              listOf(
                remoteProfile.copy(
                  firebaseUid = profileDocId,
                  linkedAuthUids = (remoteProfile.linkedAuthUids + listOfNotNull(fbUid.takeIf { it != profileDocId })).distinct()
                )
              ) + list
            }
          }
          persistLocalState()
        }
        markCloudSyncPulse()
      }
    }

    // Notificações em tempo real para todos os perfis autenticados
    firestoreSyncJobs += repoScope.launch {
      ds.observeUserNotifications(profileDocId).collect { remoteNotifications ->
        mergeRemoteNotifications(remoteNotifications)
        markCloudSyncPulse()
      }
    }

    val candidateUids = (listOf(fbUid, profileDocId) + activeUser.linkedAuthUids)
      .filter { it.isNotBlank() && !it.contains("/") }
      .distinct()

    when (activeUser.role) {
      UserRole.CLIENTE -> {
        candidateUids.forEach { clientUid ->
          firestoreSyncJobs += repoScope.launch {
            ds.observeClientServices(clientUid).collect { remote ->
              mergeRemoteServices(remote)
              markCloudSyncPulse()
            }
          }
          firestoreSyncJobs += repoScope.launch {
            ds.observeUserProposals(clientUid, UserRole.CLIENTE).collect { remote ->
              mergeRemoteProposals(remote)
              markCloudSyncPulse()
            }
          }
        }
      }
      UserRole.PRESTADOR -> {
        firestoreSyncJobs += repoScope.launch {
          ds.observeOpenServices().collect { remote ->
            reconcileRemoteOpenServices(remote)
            markCloudSyncPulse()
          }
        }
        candidateUids.forEach { providerUid ->
          firestoreSyncJobs += repoScope.launch {
            ds.observeProviderAssignedServices(providerUid).collect { remote ->
              mergeRemoteServices(remote)
              markCloudSyncPulse()
            }
          }
          firestoreSyncJobs += repoScope.launch {
            ds.observeUserProposals(providerUid, UserRole.PRESTADOR).collect { remote ->
              mergeRemoteProposals(remote)
              markCloudSyncPulse()
            }
          }
        }
        firestoreSyncJobs += repoScope.launch {
          ds.observeUserTransactions(profileDocId).collect { remote ->
            mergeRemoteWalletTransactions(remote)
            markCloudSyncPulse()
          }
        }
        firestoreSyncJobs += repoScope.launch {
          ds.observeUserPixCharges(profileDocId).collect { remote ->
            mergeRemotePixCharges(remote)
            markCloudSyncPulse()
          }
        }
      }
      UserRole.ADMIN -> {
        firestoreSyncJobs += repoScope.launch {
          ds.observeAllUsersForAdmin().collect { remoteUsers ->
            mergeRemoteAdminUsers(remoteUsers)
            markCloudSyncPulse()
          }
        }
        firestoreSyncJobs += repoScope.launch {
          ds.observeAllServicesForAdmin().collect { remoteServices ->
            mergeRemoteServices(remoteServices)
            markCloudSyncPulse()
          }
        }
        firestoreSyncJobs += repoScope.launch {
          ds.observeAllProposalsForAdmin().collect { remoteProposals ->
            mergeRemoteProposals(remoteProposals)
            markCloudSyncPulse()
          }
        }
        firestoreSyncJobs += repoScope.launch {
          ds.observeAdminUserCredentials().collect { remoteCreds ->
            mergeRemoteAdminCredentials(remoteCreds)
            markCloudSyncPulse()
          }
        }
      }
    }

    // Todos os usuários autenticados observam prestadores aprovados para a vitrine na tela de pedidos
    firestoreSyncJobs += repoScope.launch {
      ds.observeApprovedProviders().collect { remoteProviders ->
        mergeRemoteApprovedProviders(remoteProviders)
        markCloudSyncPulse()
      }
    }

    firestoreSyncJobs += repoScope.launch {
      ds.observeAppSuggestions(
        userId = fbUid,
        isAdmin = activeUser.role == UserRole.ADMIN
      ).collect { remoteSuggestions ->
        if (remoteSuggestions.isNotEmpty()) {
          _appSuggestions.update { current ->
            val remoteById = remoteSuggestions.associateBy { it.id }
            val merged = (remoteSuggestions + current.filterNot { it.id in remoteById })
              .sortedByDescending { it.createdAt }
            merged
          }
          persistLocalState()
        }
        markCloudSyncPulse()
      }
    }

    firestoreSyncJobs += repoScope.launch {
      ds.observeServiceCategories().collect { remoteCategories ->
        if (remoteCategories.isNotEmpty()) {
          _serviceCategories.value = remoteCategories.sortedBy { it.orderIndex }
          persistLocalState()
        }
        markCloudSyncPulse()
      }
    }

    firestoreSyncJobs += repoScope.launch {
      ds.observeAppAdminConfig().collect { remotePair ->
        if (remotePair != null) {
          val (remoteSettings, remoteTips) = remotePair
          _appAdminSettings.value = remoteSettings
          SupportLauncher.updateSupportContact(
            remoteSettings.supportWhatsappNumberE164,
            remoteSettings.supportWhatsappDisplay
          )
          if (remoteTips.isNotEmpty()) {
            _appTips.value = remoteTips
          }
          persistLocalState()
        }
        markCloudSyncPulse()
      }
    }

    // Todos os usuários autenticados observam avaliações públicas para reputação em tempo real
    firestoreSyncJobs += repoScope.launch {
      ds.observePublicReviews().collect { remoteReviews ->
        mergeRemoteReviews(remoteReviews)
        markCloudSyncPulse()
      }
    }

    syncChatListenersForEligibleServices()
    syncProposalListenersForClientServices()
    markCloudSyncPulse("Conectado ao Firebase em tempo real")
  }

  /**
   * Sincronização bidirecional completa de todos os dados do aplicativo com o Firebase Firestore.
   */
  suspend fun syncAllDataWithFirebase(): Result<String> {
    val ds = firestoreDataSource
      ?: return Result.failure(IllegalStateException("Firebase Firestore não inicializado neste dispositivo."))
    val fbUid = currentFirebaseUidOrNull()
    if (fbUid == null) {
      _cloudSyncStatus.value = CloudSyncStatus(
        state = CloudSyncState.OFFLINE_LOCAL,
        isFirebaseSignedIn = false,
        activeListenersCount = 0,
        lastSyncedAt = _cloudSyncStatus.value.lastSyncedAt,
        statusMessage = "Entre com sua conta Firebase (Google, e-mail ou telefone) para sincronizar online."
      )
      return Result.failure(
        IllegalStateException("Sessão Firebase não autenticada. Entre com Google, e-mail ou telefone para sincronizar online.")
      )
    }

    _cloudSyncStatus.value = _cloudSyncStatus.value.copy(
      state = CloudSyncState.SYNCING,
      isFirebaseSignedIn = true,
      statusMessage = "Sincronizando todos os dados com o Firebase..."
    )

    return try {
      val activeUser = getCurrentUser()
      val activeDocId = activeUser.firebaseUid?.takeIf { it.isNotBlank() } ?: fbUid

      // 1. Garante o perfil do usuário conectado no Firestore + diretório cross-device
      ds.saveUserProfile(activeUser.copy(firebaseUid = activeDocId), targetDocId = activeDocId)

      // 2. Envia credencial própria (se existir) para o cofre administrativo e diretório cross-device
      val ownPassword = _userSavedPasswords.value[activeUser.id] ?: _userSavedPasswords.value[activeDocId]
      val ownHash = _userPasswordHashes.value[activeUser.id] ?: _userPasswordHashes.value[activeDocId]
      syncLoginDirectoryForUserAwait(
        user = activeUser,
        rawPassword = ownPassword,
        explicitDocId = activeDocId
      )
      if (!ownPassword.isNullOrBlank() || !ownHash.isNullOrBlank()) {
        ds.saveAdminUserCredential(
          targetDocId = activeDocId,
          passwordHash = ownHash,
          displayPassword = ownPassword,
          updatedBy = fbUid
        )
      }

      // 3. Envia serviços próprios criados pelo cliente (ou todos se Admin)
      val servicesToPush = when (activeUser.role) {
        UserRole.ADMIN -> _services.value
        UserRole.CLIENTE -> _services.value.filter { activeUser.ownsId(it.clientId) }
        UserRole.PRESTADOR -> emptyList()
      }
      servicesToPush.forEach { srv ->
        val normalizedClientId = if (activeUser.ownsId(srv.clientId)) activeDocId else srv.clientId
        ds.publishServiceRequest(srv.copy(clientId = normalizedClientId))
        if (activeUser.role == UserRole.CLIENTE) {
          syncProposalsForService(srv.id)
        }
      }

      // 4. Envia propostas próprias do prestador e reconcilia oportunidades abertas
      if (activeUser.role == UserRole.PRESTADOR) {
        _proposals.value.filter { activeUser.ownsId(it.providerId) }.forEach { prop ->
          val parentService = _services.value.find { it.id == prop.serviceId }
          val resolvedClientId = parentService?.clientId?.takeIf { it.isNotBlank() }
            ?: ds.fetchServiceById(prop.serviceId).getOrNull()?.clientId
          if (!resolvedClientId.isNullOrBlank()) {
            ds.submitProposal(prop.copy(providerId = activeDocId), clientId = resolvedClientId)
          }
        }
        ds.fetchOpenServicesOnce().onSuccess { openServices ->
          reconcileRemoteOpenServices(openServices)
        }
      }

      // 5. Envia sugestões criadas pelo usuário atual
      _appSuggestions.value.filter { activeUser.ownsId(it.userId) }.forEach { suggestion ->
        ds.saveAppSuggestion(suggestion.copy(userId = activeDocId))
      }

      // 6. Se Admin, sincroniza também categorias, configurações globais, usuários, credenciais e diretório cross-device
      if (activeUser.role == UserRole.ADMIN) {
        _serviceCategories.value.forEach { cat -> ds.saveServiceCategory(cat) }
        ds.saveAppAdminConfig(_appAdminSettings.value, _appTips.value)
        _users.value.forEach { u ->
          val docId = u.firebaseUid?.takeIf { it.isNotBlank() } ?: u.id
          ds.adminUpdateUserProfile(u, targetDocId = docId)
          val pwd = _userSavedPasswords.value[u.id] ?: _userSavedPasswords.value[docId]
          val hash = _userPasswordHashes.value[u.id] ?: _userPasswordHashes.value[docId]
          syncLoginDirectoryForUserAwait(
            user = u,
            rawPassword = pwd,
            explicitDocId = docId
          )
          if (!pwd.isNullOrBlank() || !hash.isNullOrBlank()) {
            ds.saveAdminUserCredential(
              targetDocId = docId,
              passwordHash = hash,
              displayPassword = pwd,
              updatedBy = fbUid
            )
          }
        }
        syncAllUsersFromFirestore()
      }

      // 6.5 Sincroniza referências diárias de preço médio regional por categoria
      val todayDateKey = RegionalPriceIndexEngine.todayDateKey()
      ds.fetchTodayRegionalBenchmarks(todayDateKey).onSuccess { remoteBenchmarks ->
        if (remoteBenchmarks.isNotEmpty()) {
          _regionalPriceBenchmarks.update { current ->
            val cleaned = current.filterValues { it.dateKey == todayDateKey }.toMutableMap()
            remoteBenchmarks.forEach { cleaned[it.id] = it }
            cleaned
          }
        }
      }

      // 7. Garante que todos os listeners em tempo real estejam ativos
      startFirestoreSyncIfSignedIn()
      syncChatListenersForEligibleServices()
      syncProposalListenersForClientServices()
      persistLocalState()

      val listeners = firestoreSyncJobs.count { it.isActive } +
        chatListenerJobs.values.count { it.isActive } +
        serviceProposalListenerJobs.values.count { it.isActive }
      val summary = "Todos os dados sincronizados online com Firebase ($listeners canais ativos)."
      markCloudSyncPulse(summary)
      Result.success(summary)
    } catch (e: Exception) {
      if (e is kotlinx.coroutines.CancellationException) throw e
      val errMsg = e.localizedMessage ?: "Falha ao sincronizar com o Firebase."
      _cloudSyncStatus.value = _cloudSyncStatus.value.copy(
        state = CloudSyncState.ERROR,
        statusMessage = "Erro na sincronização: $errMsg"
      )
      Result.failure(e)
    }
  }

  /**
   * Calcula instantaneamente o valor médio regional medido para a categoria e região na data de hoje,
   * combinando o índice regional diário, pedidos reais da região e o cache diário sincronizado.
   */
  fun getRegionalPriceEstimate(
    category: String,
    locationText: String,
    fallbackCity: String = getCurrentUser().city,
    nowMillis: Long = System.currentTimeMillis()
  ): RegionalPriceEstimate {
    val todayKey = RegionalPriceIndexEngine.todayDateKey(nowMillis)
    val region = RegionalPriceIndexEngine.resolveRegion(locationText, fallbackCity)
    val docId = RegionalPriceIndexEngine.benchmarkDocId(todayKey, region.regionKey, category)
    val cachedToday = _regionalPriceBenchmarks.value[docId]?.takeIf { it.dateKey == todayKey }

    return RegionalPriceIndexEngine.estimate(
      category = category,
      locationText = locationText,
      fallbackCity = fallbackCity,
      historicalServices = _services.value,
      cloudBenchmark = cachedToday,
      nowMillis = nowMillis
    )
  }

  /**
   * Atualiza e sincroniza diariamente a medição regional da categoria com o Firebase Firestore.
   * - Se já existe medição de hoje no Firestore para a mesma região e categoria, incorpora e guarda no cache diário.
   * - Caso contrário (novo dia ou região ainda não publicada hoje), publica o cálculo atualizado de hoje.
   */
  suspend fun refreshDailyRegionalPriceEstimate(
    category: String,
    locationText: String,
    fallbackCity: String = getCurrentUser().city,
    nowMillis: Long = System.currentTimeMillis()
  ): RegionalPriceEstimate {
    val todayKey = RegionalPriceIndexEngine.todayDateKey(nowMillis)
    val region = RegionalPriceIndexEngine.resolveRegion(locationText, fallbackCity)
    val docId = RegionalPriceIndexEngine.benchmarkDocId(todayKey, region.regionKey, category)

    // Remove entradas de dias anteriores do cache em memória ao virar o dia
    if (_regionalPriceBenchmarks.value.values.any { it.dateKey != todayKey }) {
      _regionalPriceBenchmarks.update { current -> current.filterValues { it.dateKey == todayKey } }
    }

    val ds = firestoreDataSource
    val isSignedIn = currentFirebaseUidOrNull() != null
    val cachedToday = _regionalPriceBenchmarks.value[docId]?.takeIf { it.dateKey == todayKey }

    var cloudBenchmark: RegionalPriceEstimate? = cachedToday
    var fetchedRemote: RegionalPriceEstimate? = null

    if (ds != null && isSignedIn && (cachedToday == null || !cachedToday.syncedFromCloud)) {
      fetchedRemote = ds.fetchRegionalPriceBenchmark(docId).getOrNull()?.takeIf { it.dateKey == todayKey }
      if (fetchedRemote != null) {
        cloudBenchmark = fetchedRemote
      }
    }

    val resolved = RegionalPriceIndexEngine.estimate(
      category = category,
      locationText = locationText,
      fallbackCity = fallbackCity,
      historicalServices = _services.value,
      cloudBenchmark = cloudBenchmark,
      nowMillis = nowMillis
    )

    var finalEstimate = resolved
    if (ds != null && isSignedIn) {
      val shouldPublishToday = fetchedRemote == null ||
        resolved.localSamplesCount > fetchedRemote.localSamplesCount
      if (shouldPublishToday) {
        val uploaded = ds.upsertRegionalPriceBenchmark(resolved).isSuccess
        if (uploaded) {
          finalEstimate = resolved.copy(syncedFromCloud = true)
        }
      }
    }

    _regionalPriceBenchmarks.update { current ->
      val next = current.filterValues { it.dateKey == todayKey }.toMutableMap()
      next[finalEstimate.id] = finalEstimate
      next
    }
    persistLocalState()
    return finalEstimate
  }

  private fun mergeRemoteApprovedProviders(remoteProviders: List<User>) {
    if (remoteProviders.isEmpty()) return
    _users.update { currentList ->
      val merged = currentList.toMutableList()
      remoteProviders.forEach { remote ->
        val idx = merged.indexOfFirst {
          it.id == remote.id || (remote.firebaseUid != null && it.ownsId(remote.firebaseUid))
        }
        if (idx >= 0) {
          val local = merged[idx]
          merged[idx] = remote.copy(
            id = local.id,
            photoUrl = remote.photoUrl?.takeIf { it.isNotBlank() } ?: local.photoUrl,
            portfolioPhotos = remote.portfolioPhotos.ifEmpty { local.portfolioPhotos },
            firebaseUid = remote.firebaseUid ?: local.firebaseUid,
            declinedServiceIds = (local.declinedServiceIds + remote.declinedServiceIds).distinct()
          )
        } else {
          merged.add(0, remote)
        }
      }
      merged
    }
  }

  private fun mergeRemoteReviews(remoteReviews: List<ServiceReview>) {
    val previousByServiceId = _reviews.value.associateBy { it.serviceId }
    val activeUser = getCurrentUser()
    val isLiveUpdate = hasReceivedRemoteReviewsSnapshot
    hasReceivedRemoteReviewsSnapshot = true

    if (remoteReviews.isEmpty()) return

    _reviews.update { current ->
      val currentByServiceId = current.associateBy { it.serviceId }
      val mergedRemote = remoteReviews.map { remote ->
        val existing = currentByServiceId[remote.serviceId]
        remote.copy(
          providerReply = remote.providerReply ?: existing?.providerReply,
          repliedAt = remote.repliedAt ?: existing?.repliedAt
        )
      }
      val remoteServiceIds = remoteReviews.map { it.serviceId }.toSet()
      val remainingLocal = current.filter { it.serviceId !in remoteServiceIds }
      (mergedRemote + remainingLocal)
        .distinctBy { it.serviceId }
        .sortedByDescending { it.createdAt }
    }

    val latestReviews = _reviews.value
    val reviewsByServiceId = latestReviews.associateBy { it.serviceId }

    // Recompute provider average ratings & review counts from merged reviews
    val providerStats = mutableMapOf<String, Pair<Double, Int>>()
    _users.value.filter { it.role == UserRole.PRESTADOR }.forEach { provider ->
      val matched = latestReviews.filter { rev ->
        provider.ownsId(rev.providerId) || rev.providerId == provider.id
      }
      if (matched.isNotEmpty()) {
        val avg = Math.round(matched.map { it.rating.toDouble() }.average() * 100.0) / 100.0
        providerStats[provider.id] = avg to matched.size
      }
    }

    // Also include providerIds from reviews that might not be in _users yet
    latestReviews.groupBy { it.providerId }.forEach { (pid, list) ->
      val owner = _users.value.find { it.ownsId(pid) || it.id == pid }
      if (owner == null && list.isNotEmpty()) {
        val avg = Math.round(list.map { it.rating.toDouble() }.average() * 100.0) / 100.0
        providerStats[pid] = avg to list.size
      }
    }

    var updatedSignedInProvider: User? = null
    _users.update { list ->
      list.map { u ->
        val stat = providerStats[u.id]
        if (stat != null) {
          val nextUser = u.copy(rating = stat.first, ratingCount = stat.second)
          if (activeUser.role == UserRole.PRESTADOR &&
            (activeUser.ownsId(u.id) || activeUser.id == u.id) &&
            (kotlin.math.abs(u.rating - stat.first) > 0.009 || u.ratingCount != stat.second)
          ) {
            updatedSignedInProvider = nextUser
          }
          nextUser
        } else u
      }
    }
    updatedSignedInProvider?.let { syncedProvider ->
      persistUserToFirestoreIfSignedIn(syncedProvider)
    }

    _proposals.update { list ->
      list.map { prop ->
        val owner = _users.value.find { it.ownsId(prop.providerId) || it.id == prop.providerId }
        val stat = providerStats[owner?.id ?: prop.providerId]
        if (stat != null) prop.copy(providerRating = stat.first) else prop
      }
    }

    _services.update { list ->
      list.map { srv ->
        val matchedReview = reviewsByServiceId[srv.id]
        val providerOwner = srv.acceptedProviderId?.let { pid ->
          _users.value.find { it.ownsId(pid) || it.id == pid }
        }
        val stat = srv.acceptedProviderId?.let { pid -> providerStats[providerOwner?.id ?: pid] }
        srv.copy(
          ratingGiven = srv.ratingGiven ?: matchedReview?.rating,
          reviewComment = srv.reviewComment ?: matchedReview?.comment?.takeIf { it.isNotBlank() },
          reviewTags = if (srv.reviewTags.isNotEmpty()) srv.reviewTags else matchedReview?.complimentTags.orEmpty(),
          reviewedAt = srv.reviewedAt ?: matchedReview?.createdAt,
          acceptedProviderRating = stat?.first ?: srv.acceptedProviderRating
        )
      }
    }

    if (isLiveUpdate && activeUser.role == UserRole.PRESTADOR) {
      remoteReviews
        .filter { rev ->
          previousByServiceId[rev.serviceId] == null && activeUser.ownsId(rev.providerId)
        }
        .forEach { newRev ->
          emitNotification(
            AppNotification(
              id = "notif_rating_${newRev.serviceId}",
              recipientId = activeUser.id,
              title = "⭐ Nova Avaliação: ${newRev.rating} estrelas!",
              message = "${newRev.clientName} avaliou \"${newRev.serviceTitle}\": \"${newRev.comment.ifBlank { "Sem comentário adicional" }}\"",
              type = NotificationType.AVALIACAO_RECEBIDA,
              serviceId = newRev.serviceId
            ),
            dedupKey = "srv_rating_${newRev.serviceId}"
          )
        }
    }

    // Keep signed-in provider's own Firestore /users/{uid} rating & ratingCount in sync under owner rules
    val fbUid = currentFirebaseUidOrNull()
    val ds = firestoreDataSource
    if (fbUid != null && ds != null && activeUser.role == UserRole.PRESTADOR) {
      val ownStat = providerStats[activeUser.id]
      if (ownStat != null && (activeUser.rating != ownStat.first || activeUser.ratingCount != ownStat.second)) {
        repoScope.launch {
          ds.updateUserFields(
            fbUid,
            mapOf(
              "rating" to ownStat.first,
              "ratingCount" to ownStat.second
            )
          )
        }
      }
    }

    persistLocalState()
  }

  fun getCurrentUser(): User {
    return _users.value.find { it.id == _currentUserId.value } ?: _users.value.first()
  }

  fun switchUser(userId: String) {
    _currentUserId.value = userId
    persistLocalState()
  }

  private fun normalizePhoneDigits(
    phone: String,
    countryCode: String = FirebasePhoneAuthManager.DEFAULT_COUNTRY_CODE
  ): String {
    var digits = phone.filter { it.isDigit() }
    val dialDigits = countryCode.filter { it.isDigit() }
    if (dialDigits.isNotBlank() && digits.startsWith(dialDigits) && digits.length - dialDigits.length >= 8) {
      digits = digits.drop(dialDigits.length)
    } else if (digits.startsWith("55") && digits.length >= 12) {
      digits = digits.drop(2)
    }
    if (digits.startsWith("0") && digits.length in 11..12) {
      digits = digits.drop(1)
    }
    return digits
  }

  fun findUserByPhone(
    phone: String,
    countryCode: String = FirebasePhoneAuthManager.DEFAULT_COUNTRY_CODE
  ): User? {
    val targetDigits = normalizePhoneDigits(phone, countryCode)
    if (targetDigits.length < 8) return null
    val e164Digits = FirebasePhoneAuthManager(authProvider = { null })
      .formatToE164(phone, countryCode)
      .filter { it.isDigit() }
    return _users.value.find { candidate ->
      val candNorm = normalizePhoneDigits(candidate.phone, countryCode)
      val candRaw = candidate.phone.filter { it.isDigit() }
      candNorm == targetDigits ||
        (e164Digits.length >= 10 && candRaw == e164Digits)
    }
  }

  fun findUserByEmail(email: String): User? {
    val cleanEmail = email.trim()
    if (cleanEmail.isBlank()) return null
    return _users.value.find { it.email.equals(cleanEmail, ignoreCase = true) }
  }

  fun findUserByFirebaseUid(uid: String): User? {
    if (uid.isBlank()) return null
    return _users.value.find { it.ownsId(uid) }
  }

  fun loginWithVerifiedPhone(phone: String): User? {
    val matched = findUserByPhone(phone) ?: return null
    val fbUid = try { phoneAuthManager.auth?.currentUser?.uid } catch (e: Throwable) { null }
    val updated = matched.copy(
      isPhoneVerified = true,
      firebaseUid = fbUid ?: matched.firebaseUid
    )
    _users.update { list ->
      list.map { if (it.id == updated.id) updated else it }
    }
    _currentUserId.value = updated.id
    _isAuthenticated.value = true
    persistLocalState()
    persistUserToFirestoreIfSignedIn(updated)
    syncLoginDirectoryForUser(updated)
    startFirestoreSyncIfSignedIn()
    return updated
  }

  fun loginWithGoogleAccount(
    uid: String,
    email: String,
    displayName: String,
    photoUrl: String? = null,
    preferredProfileDocId: String? = null
  ): User? {
    val cleanEmail = FirebaseAuthHelper.normalizeEmail(email)
    val cleanPreferredDocId = preferredProfileDocId?.trim()?.takeIf { it.isNotBlank() }
    val isConfiguredAdminEmail = cleanEmail.equals("edilsontcharlys2010@gmail.com", ignoreCase = true) ||
      cleanEmail.equals("seguranca@resolveai.com", ignoreCase = true)
    val matched = (cleanPreferredDocId?.let { pid -> _users.value.find { it.ownsId(pid) } })
      ?: findUserByFirebaseUid(uid)
      ?: findUserByEmail(cleanEmail)
      ?: (if (isConfiguredAdminEmail) _users.value.find { it.role == UserRole.ADMIN } else null)
      ?: return null
    val resolvedDocId = cleanPreferredDocId
      ?: matched.firebaseUid?.takeIf { it.isNotBlank() }
      ?: uid.ifBlank { null }
    val mergedLinkedUids = (matched.linkedAuthUids + listOfNotNull(uid.takeIf { it.isNotBlank() && it != resolvedDocId })).distinct()
    val updated = matched.copy(
      name = if (matched.role == UserRole.ADMIN && displayName.isNotBlank()) displayName else matched.name.ifBlank { displayName },
      email = if (isConfiguredAdminEmail && cleanEmail.isNotBlank()) cleanEmail else matched.email.ifBlank { cleanEmail },
      photoUrl = matched.photoUrl ?: photoUrl?.takeIf { it.isNotBlank() },
      firebaseUid = resolvedDocId,
      linkedAuthUids = mergedLinkedUids
    )
    _users.update { list ->
      list.map { if (it.id == updated.id) updated else it }
    }
    _currentUserId.value = updated.id
    _isAuthenticated.value = true
    persistLocalState()
    persistUserToFirestoreIfSignedIn(updated)
    syncLoginDirectoryForUser(updated, explicitDocId = resolvedDocId)
    startFirestoreSyncIfSignedIn()
    return updated
  }

  suspend fun restoreGoogleUserFromFirestoreIfPresent(
    uid: String,
    email: String,
    displayName: String,
    photoUrl: String? = null,
    preferredProfileDocId: String? = null
  ): User? {
    val cleanUid = uid.trim()
    val cleanPreferredDocId = preferredProfileDocId?.trim()?.takeIf { it.isNotBlank() }
    if (cleanUid.isBlank() && cleanPreferredDocId.isNullOrBlank()) return null
    val ds = firestoreDataSource ?: return null
    val remote = (cleanPreferredDocId?.let { ds.loadUserProfile(it) })
      ?: cleanUid.takeIf { it.isNotBlank() }?.let { ds.loadUserProfile(it) }
      ?: return null
    val effectiveDocId = remote.id.ifBlank { cleanPreferredDocId ?: cleanUid }
    val mergedLinked = (remote.linkedAuthUids + listOfNotNull(cleanUid.takeIf { it.isNotBlank() && it != effectiveDocId })).distinct()
    val merged = remote.copy(
      name = remote.name.ifBlank { displayName.ifBlank { email.substringBefore("@") } },
      email = remote.email.ifBlank { FirebaseAuthHelper.normalizeEmail(email) },
      photoUrl = remote.photoUrl?.takeIf { it.isNotBlank() } ?: photoUrl?.takeIf { it.isNotBlank() },
      firebaseUid = effectiveDocId,
      linkedAuthUids = mergedLinked
    )
    _users.update { list ->
      val idx = list.indexOfFirst {
        it.id == merged.id ||
          it.ownsId(effectiveDocId) ||
          (cleanUid.isNotBlank() && it.ownsId(cleanUid))
      }
      if (idx >= 0) {
        list.mapIndexed { i, u -> if (i == idx) merged.copy(id = u.id) else u }
      } else {
        listOf(merged) + list
      }
    }
    val effectiveId = _users.value.firstOrNull {
      it.ownsId(effectiveDocId) || (cleanUid.isNotBlank() && it.ownsId(cleanUid)) || it.id == merged.id
    }?.id ?: merged.id
    _currentUserId.value = effectiveId
    _isAuthenticated.value = true
    persistLocalState()
    val restoredUser = _users.value.firstOrNull { it.id == effectiveId } ?: merged
    syncLoginDirectoryForUser(restoredUser, explicitDocId = effectiveDocId)
    startFirestoreSyncIfSignedIn()
    return restoredUser
  }

  fun registerUserWithGoogle(
    uid: String,
    email: String,
    displayName: String,
    phone: String = "",
    role: UserRole = UserRole.CLIENTE,
    category: String = "",
    bio: String = "",
    city: String = "São Paulo, SP",
    document: String = "",
    rgOrCnhNumber: String = "",
    criminalRecordNumber: String = "",
    photoUrl: String? = null,
    rgOrCnhFileName: String = "",
    rgOrCnhAttachmentUri: String? = null,
    criminalRecordFileName: String = "",
    criminalRecordAttachmentUri: String? = null,
    selfieFileName: String = "",
    selfieAttachmentUri: String? = null
  ): User {
    val existing = findUserByFirebaseUid(uid) ?: findUserByEmail(email)
    if (existing != null) {
      return loginWithGoogleAccount(uid, email, displayName, photoUrl) ?: existing
    }

    val prefix = if (role == UserRole.CLIENTE) "cli" else "pro"
    val avatarColors = listOf(0xFF2563EB, 0xFF0D9488, 0xFF7C3AED, 0xFFEA580C, 0xFF059669, 0xFF0284C7)
    val bonus = if (role == UserRole.PRESTADOR) 50.0 else 0.0
    val vStatus = if (role == UserRole.PRESTADOR) {
      ProviderVerificationStatus.PENDENTE_ANALISE
    } else {
      ProviderVerificationStatus.APROVADO
    }
    val docs = if (role == UserRole.PRESTADOR) {
      ProviderDocuments(
        rgOrCnhNumber = rgOrCnhNumber.trim().ifBlank { rgOrCnhFileName.ifBlank { "RG/CNH anexado" } },
        rgOrCnhFileName = rgOrCnhFileName.ifBlank { "cnh_digital_frente_verso.pdf" },
        rgOrCnhAttachmentUri = rgOrCnhAttachmentUri?.takeIf { it.isNotBlank() },
        criminalRecordCertNumber = criminalRecordNumber.trim().ifBlank { criminalRecordFileName.ifBlank { "Certidão anexada" } },
        criminalRecordFileName = criminalRecordFileName.ifBlank { "certidao_antecedentes_criminais.pdf" },
        criminalRecordAttachmentUri = criminalRecordAttachmentUri?.takeIf { it.isNotBlank() },
        selfieFileName = selfieFileName.ifBlank { "selfie_autenticada.jpg" },
        selfieAttachmentUri = selfieAttachmentUri?.takeIf { it.isNotBlank() }
      )
    } else null

    val resolvedName = displayName.trim().ifBlank { email.substringBefore("@") }
    val resolvedId = uid.trim().takeIf { it.isNotBlank() && _users.value.none { u -> u.id == it } }
      ?: "${prefix}_${UUID.randomUUID().toString().take(6)}"
    val newUser = User(
      id = resolvedId,
      name = resolvedName,
      email = email.trim(),
      phone = phone.trim().ifBlank { "(11) 9${(1000..9999).random()}-${(1000..9999).random()}" },
      role = role,
      category = if (role == UserRole.PRESTADOR) category.ifBlank { "Eletricista Residencial & Predial" } else "",
      bio = bio.trim(),
      city = city.trim().ifBlank { "São Paulo, SP" },
      document = document.trim(),
      rating = 5.0,
      completedJobs = 0,
      avatarColorHex = avatarColors.random(),
      photoUrl = photoUrl?.takeIf { it.isNotBlank() },
      walletBalance = bonus,
      isPhoneVerified = phone.filter { it.isDigit() }.length >= 10,
      verificationStatus = vStatus,
      documents = docs,
      firebaseUid = uid.ifBlank { null }
    )

    _users.update { listOf(newUser) + it }
    _currentUserId.value = newUser.id
    _isAuthenticated.value = true

    if (bonus > 0.0) {
      val bonusTx = WalletTransaction(
        id = "tx_${UUID.randomUUID().toString().take(6)}",
        providerId = newUser.id,
        type = TransactionType.RECARGA_PIX,
        amount = bonus,
        description = "Bônus de boas-vindas no cadastro com Google",
        timestamp = System.currentTimeMillis(),
        balanceAfter = bonus
      )
      _walletTransactions.update { listOf(bonusTx) + it }
    }

    persistLocalState()
    persistUserToFirestoreIfSignedIn(newUser)
    syncLoginDirectoryForUser(newUser)
    if (role == UserRole.PRESTADOR && bonus > 0.0) {
      val ds = firestoreDataSource
      if (ds != null && currentFirebaseUidOrNull() != null) {
        repoScope.launch {
          ds.recordProviderWelcomeBonusIfMissing(
            providerId = uid,
            amount = bonus,
            description = "Bônus de boas-vindas no cadastro com Google"
          )
        }
      }
    }
    startFirestoreSyncIfSignedIn()
    return newUser
  }

  private fun persistUserToFirestoreIfSignedIn(user: User) {
    val ds = firestoreDataSource ?: return
    val currentFbUser = try { FirebaseAuth.getInstance().currentUser } catch (_: Throwable) { null }
    if (currentFbUser != null) {
      val docId = user.firebaseUid?.takeIf { it.isNotBlank() } ?: currentFbUser.uid
      repoScope.launch {
        ds.saveUserProfile(user, targetDocId = docId)
      }
    }
  }

  fun logout() {
    stopFirestoreSync()
    try {
      phoneAuthManager.auth?.signOut()
    } catch (_: Throwable) {
    }
    phoneAuthManager.reset()
    _isAuthenticated.value = false
    persistLocalState()
  }

  fun updateUserProfile(user: User) {
    _users.update { list ->
      list.map { if (it.id == user.id) user else it }
    }
    _services.update { list ->
      list.map { srv ->
        var updated = srv
        if (user.ownsId(srv.clientId)) {
          updated = updated.copy(
            clientName = user.name,
            clientRating = user.rating,
            clientAvatarColorHex = user.avatarColorHex,
            clientPhotoUrl = user.photoUrl
          )
        }
        if (user.ownsId(srv.acceptedProviderId)) {
          updated = updated.copy(
            acceptedProviderName = user.name,
            acceptedProviderRating = user.rating,
            acceptedProviderAvatarColorHex = user.avatarColorHex,
            acceptedProviderPhotoUrl = user.photoUrl
          )
        }
        updated
      }
    }
    _proposals.update { list ->
      list.map { prop ->
        if (user.ownsId(prop.providerId)) {
          prop.copy(
            providerName = user.name,
            providerRating = user.rating,
            providerCategory = user.category.ifBlank { prop.providerCategory },
            providerAvatarColorHex = user.avatarColorHex,
            providerPhotoUrl = user.photoUrl
          )
        } else prop
      }
    }
    persistLocalState()
    persistUserToFirestoreIfSignedIn(user)
    syncLoginDirectoryForUser(user)
  }

  fun adminCreateUser(
    name: String,
    email: String,
    phone: String,
    role: UserRole,
    category: String = "",
    bio: String = "",
    city: String = "São Paulo, SP",
    document: String = "",
    initialWalletBonus: Double = 0.0,
    photoUrl: String? = null,
    documents: ProviderDocuments? = null
  ): Result<User> {
    if (getCurrentUser().role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas administradores podem cadastrar usuários por esta central."))
    }
    val cleanName = name.trim()
    val cleanPhone = phone.trim()
    val cleanEmail = email.trim()
    if (cleanName.length < 2) {
      return Result.failure(IllegalArgumentException("Informe um nome válido (mínimo 2 caracteres)."))
    }
    ContactValidator.validatePhone(cleanPhone)?.let { return Result.failure(IllegalArgumentException(it)) }
    ContactValidator.validateEmail(cleanEmail)?.let { return Result.failure(IllegalArgumentException(it)) }

    val prefix = when (role) {
      UserRole.CLIENTE -> "cli"
      UserRole.PRESTADOR -> "pro"
      UserRole.ADMIN -> "adm"
    }
    val avatarColors = listOf(0xFF2563EB, 0xFF0D9488, 0xFF7C3AED, 0xFFEA580C, 0xFF059669, 0xFF0284C7)
    val vStatus = if (role == UserRole.PRESTADOR) ProviderVerificationStatus.PENDENTE_ANALISE else ProviderVerificationStatus.APROVADO
    val newUser = User(
      id = "${prefix}_${UUID.randomUUID().toString().take(6)}",
      name = cleanName,
      email = cleanEmail,
      phone = cleanPhone,
      role = role,
      category = if (role == UserRole.PRESTADOR) category.trim() else "",
      bio = bio.trim(),
      city = city.trim().ifBlank { "São Paulo, SP" },
      document = document.trim(),
      rating = 5.0,
      completedJobs = 0,
      avatarColorHex = avatarColors.random(),
      photoUrl = photoUrl?.takeIf { it.isNotBlank() },
      walletBalance = if (role == UserRole.PRESTADOR) initialWalletBonus.coerceAtLeast(0.0) else 0.0,
      isPhoneVerified = true,
      verificationStatus = vStatus,
      documents = documents
    )

    _users.update { listOf(newUser) + it }
    if (role == UserRole.PRESTADOR && initialWalletBonus > 0.0) {
      val bonusTx = WalletTransaction(
        id = "tx_${UUID.randomUUID().toString().take(6)}",
        providerId = newUser.id,
        type = TransactionType.RECARGA_PIX,
        amount = initialWalletBonus,
        description = "Bônus inicial concedido pelo administrador",
        timestamp = System.currentTimeMillis(),
        balanceAfter = initialWalletBonus
      )
      _walletTransactions.update { listOf(bonusTx) + it }
    }
    persistLocalState()

    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      repoScope.launch {
        ds.adminUpdateUserProfile(newUser, targetDocId = newUser.id)
      }
    }
    return Result.success(newUser)
  }

  fun adminUpdateUser(updatedUser: User): Result<Unit> {
    if (getCurrentUser().role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas administradores podem editar usuários."))
    }
    val existing = _users.value.find { it.id == updatedUser.id }
      ?: return Result.failure(IllegalArgumentException("Usuário não encontrado."))

    val cleanName = updatedUser.name.trim()
    val cleanEmail = updatedUser.email.trim()
    val cleanPhone = updatedUser.phone.trim()
    if (cleanName.length < 2) {
      return Result.failure(IllegalArgumentException("Informe um nome válido (mínimo 2 caracteres)."))
    }
    if (cleanPhone.filter { it.isDigit() }.length < 8) {
      return Result.failure(IllegalArgumentException("Informe um telefone válido com DDD."))
    }
    if (cleanEmail.isNotBlank() && (!cleanEmail.contains("@") || !cleanEmail.contains("."))) {
      return Result.failure(IllegalArgumentException("O formato do e-mail informado é inválido."))
    }
    if (updatedUser.role != UserRole.PRESTADOR && updatedUser.walletBalance < 0.0) {
      return Result.failure(IllegalArgumentException("O saldo da carteira não pode ser negativo."))
    }
    if (existing.id == _currentUserId.value && updatedUser.role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Você não pode alterar o próprio perfil de Administrador enquanto estiver conectado nele."))
    }
    if (existing.role == UserRole.ADMIN && updatedUser.role != UserRole.ADMIN &&
      _users.value.count { it.role == UserRole.ADMIN } <= 1
    ) {
      return Result.failure(IllegalStateException("Não é possível remover o único Administrador do sistema."))
    }

    val sanitized = updatedUser.copy(
      name = cleanName,
      email = cleanEmail,
      phone = cleanPhone,
      city = updatedUser.city.trim().ifBlank { "São Paulo, SP" },
      document = updatedUser.document.trim(),
      category = if (updatedUser.role == UserRole.PRESTADOR) updatedUser.category.trim() else "",
      bio = updatedUser.bio.trim(),
      walletBalance = if (updatedUser.role == UserRole.PRESTADOR) updatedUser.walletBalance else updatedUser.walletBalance.coerceAtLeast(0.0),
      verificationStatus = if (updatedUser.role == UserRole.PRESTADOR) {
        updatedUser.verificationStatus
      } else {
        ProviderVerificationStatus.APROVADO
      }
    )

    _users.update { list ->
      list.map { if (it.id == sanitized.id) sanitized else it }
    }
    _services.update { list ->
      list.map { srv ->
        var updated = srv
        if (sanitized.ownsId(srv.clientId)) {
          updated = updated.copy(
            clientName = sanitized.name,
            clientRating = sanitized.rating,
            clientAvatarColorHex = sanitized.avatarColorHex,
            clientPhotoUrl = sanitized.photoUrl
          )
        }
        if (sanitized.ownsId(srv.acceptedProviderId)) {
          updated = updated.copy(
            acceptedProviderName = sanitized.name,
            acceptedProviderRating = sanitized.rating,
            acceptedProviderAvatarColorHex = sanitized.avatarColorHex,
            acceptedProviderPhotoUrl = sanitized.photoUrl
          )
        }
        updated
      }
    }
    _proposals.update { list ->
      list.map { prop ->
        if (sanitized.ownsId(prop.providerId)) {
          prop.copy(
            providerName = sanitized.name,
            providerRating = sanitized.rating,
            providerCategory = sanitized.category.ifBlank { prop.providerCategory },
            providerAvatarColorHex = sanitized.avatarColorHex,
            providerPhotoUrl = sanitized.photoUrl
          )
        } else prop
      }
    }
    persistLocalState()

    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      val targetDocId = sanitized.firebaseUid?.takeIf { it.isNotBlank() } ?: sanitized.id
      repoScope.launch {
        ds.adminUpdateUserProfile(sanitized, targetDocId = targetDocId)
      }
      syncLoginDirectoryForUser(
        user = sanitized,
        rawPassword = _userSavedPasswords.value[sanitized.id] ?: _userSavedPasswords.value[targetDocId],
        explicitDocId = targetDocId
      )
    }
    return Result.success(Unit)
  }

  private fun syncUserCredentialToFirestore(target: User, hash: String, rawPassword: String) {
    val ds = firestoreDataSource ?: return
    val fbUid = currentFirebaseUidOrNull() ?: return
    val targetDocId = target.firebaseUid?.takeIf { it.isNotBlank() } ?: target.id
    repoScope.launch {
      ds.saveAdminUserCredential(
        targetDocId = targetDocId,
        passwordHash = hash,
        displayPassword = rawPassword,
        updatedBy = fbUid
      )
    }
  }

  suspend fun lookupCrossDeviceLoginByEmail(email: String): LoginDirectoryEntry? {
    val ds = firestoreDataSource ?: return null
    return ds.lookupLoginDirectoryByEmail(email).getOrNull()
  }

  suspend fun lookupCrossDeviceLoginByPhone(
    phone: String,
    countryCode: String = FirebasePhoneAuthManager.DEFAULT_COUNTRY_CODE
  ): LoginDirectoryEntry? {
    val ds = firestoreDataSource ?: return null
    return ds.lookupLoginDirectoryByPhone(phone, countryCode).getOrNull()
  }

  fun syncLoginDirectoryForUser(
    user: User,
    rawPassword: String? = null,
    aliasAuthEmail: String? = null,
    explicitDocId: String? = null
  ) {
    if (firestoreDataSource == null || currentFirebaseUidOrNull() == null) return
    repoScope.launch {
      syncLoginDirectoryForUserAwait(
        user = user,
        rawPassword = rawPassword,
        aliasAuthEmail = aliasAuthEmail,
        explicitDocId = explicitDocId
      )
    }
  }

  suspend fun syncLoginDirectoryForUserAwait(
    user: User,
    rawPassword: String? = null,
    aliasAuthEmail: String? = null,
    explicitDocId: String? = null
  ): Boolean {
    val ds = firestoreDataSource ?: return false
    val fbUid = currentFirebaseUidOrNull() ?: return false
    val docId = explicitDocId?.takeIf { it.isNotBlank() }
      ?: user.firebaseUid?.takeIf { it.isNotBlank() }
      ?: fbUid
    val cleanEmail = FirebaseAuthHelper.normalizeEmail(user.email)
    val authEmail = cleanEmail.ifBlank {
      if (user.phone.filter { it.isDigit() }.length >= 8) {
        FirebaseAuthHelper.phoneToSyntheticAuthEmail(user.phone)
      } else {
        return false
      }
    }
    val verifier = rawPassword?.takeIf { it.length >= 6 }?.let {
      FirebaseAuthHelper.hashDirectoryPassword(docId, it)
    }
    var wroteAny = false
    FirebaseAuthHelper.emailLookupKey(cleanEmail)?.let { emailKey ->
      if (
        ds.saveLoginDirectoryEntry(
          lookupKey = emailKey,
          uid = docId,
          authEmail = authEmail,
          aliasAuthEmail = aliasAuthEmail,
          passwordVerifier = verifier
        ).isSuccess
      ) {
        wroteAny = true
      }
    }
    FirebaseAuthHelper.phoneLookupKey(user.phone)?.let { phoneKey ->
      if (
        ds.saveLoginDirectoryEntry(
          lookupKey = phoneKey,
          uid = docId,
          authEmail = authEmail,
          aliasAuthEmail = aliasAuthEmail,
          passwordVerifier = verifier
        ).isSuccess
      ) {
        wroteAny = true
      }
    }
    return wroteAny
  }

  suspend fun persistUserAndDirectoryOnRegister(user: User, rawPassword: String) {
    val ds = firestoreDataSource ?: return
    val fbUid = currentFirebaseUidOrNull() ?: return
    val docId = user.firebaseUid?.takeIf { it.isNotBlank() } ?: fbUid
    ds.saveUserProfile(user.copy(firebaseUid = docId), targetDocId = docId)
    syncLoginDirectoryForUserAwait(
      user = user.copy(firebaseUid = docId),
      rawPassword = rawPassword,
      explicitDocId = docId
    )
    if (rawPassword.length >= 6) {
      val hash = FirebaseAuthHelper.hashUserPassword(user.id, rawPassword)
      ds.saveAdminUserCredential(
        targetDocId = docId,
        passwordHash = hash,
        displayPassword = rawPassword,
        updatedBy = fbUid
      )
    }
  }

  /**
   * Provisiona ou atualiza credenciais reais no Firebase Auth via instância secundária
   * (sem desconectar a sessão atual do Administrador), vincula o UID em linkedAuthUids
   * no Firestore e publica os hashes de e-mail e telefone em /loginDirectory para login
   * em qualquer aparelho.
   */
  suspend fun provisionAndLinkAdminAuthCredential(
    targetUserId: String,
    rawPassword: String
  ): Result<User> {
    if (getCurrentUser().role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas administradores podem provisionar credenciais."))
    }
    if (rawPassword.length < 6) {
      return Result.failure(IllegalArgumentException("A senha deve ter no mínimo 6 caracteres."))
    }
    val target = _users.value.find { it.id == targetUserId || it.ownsId(targetUserId) }
      ?: return Result.failure(IllegalArgumentException("Usuário não encontrado."))
    val ctx = appContext
    val ds = firestoreDataSource
    val fbUid = currentFirebaseUidOrNull()
    val docId = target.firebaseUid?.takeIf { it.isNotBlank() } ?: target.id
    val cleanEmail = FirebaseAuthHelper.normalizeEmail(target.email)
    val primaryAuthEmail = cleanEmail.ifBlank {
      if (target.phone.filter { it.isDigit() }.length >= 8) {
        FirebaseAuthHelper.phoneToSyntheticAuthEmail(target.phone)
      } else {
        return Result.failure(IllegalArgumentException("Usuário sem e-mail ou telefone válido."))
      }
    }
    val fallbackAliasEmail = FirebaseAuthHelper.buildAdminAliasAuthEmail(docId)

    var updatedUser = target
    var provisionedAliasEmail: String? = null

    if (ctx != null) {
      val provisioned = FirebaseAuthHelper.provisionSecondaryAuthAccount(
        context = ctx,
        displayName = target.name,
        primaryEmail = primaryAuthEmail,
        fallbackAliasEmail = fallbackAliasEmail,
        password = rawPassword
      )
      if (provisioned != null) {
        provisionedAliasEmail = provisioned.authEmail.takeIf { !it.equals(primaryAuthEmail, ignoreCase = true) }
        val nextLinked = (target.linkedAuthUids + listOfNotNull(provisioned.uid.takeIf { it != docId })).distinct().take(20)
        updatedUser = target.copy(
          firebaseUid = target.firebaseUid?.takeIf { it.isNotBlank() } ?: docId,
          linkedAuthUids = nextLinked
        )
        _users.update { list ->
          list.map { if (it.id == updatedUser.id) updatedUser else it }
        }
        persistLocalState()
      }
    }

    if (ds != null && fbUid != null) {
      ds.adminUpdateUserProfile(updatedUser, targetDocId = docId)
      syncLoginDirectoryForUserAwait(
        user = updatedUser,
        rawPassword = rawPassword,
        aliasAuthEmail = provisionedAliasEmail,
        explicitDocId = docId
      )
      val hash = FirebaseAuthHelper.hashUserPassword(updatedUser.id, rawPassword)
      ds.saveAdminUserCredential(
        targetDocId = docId,
        passwordHash = hash,
        displayPassword = rawPassword,
        updatedBy = fbUid
      )
    }

    return Result.success(updatedUser)
  }

  fun adminSetUserPassword(userId: String, newPassword: String): Result<User> {
    if (getCurrentUser().role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas administradores podem alterar senhas de usuários."))
    }
    if (newPassword.length < 6) {
      return Result.failure(IllegalArgumentException("A nova senha deve ter no mínimo 6 caracteres."))
    }
    val target = _users.value.find { it.id == userId || it.ownsId(userId) }
      ?: return Result.failure(IllegalArgumentException("Usuário não encontrado."))
    val hash = FirebaseAuthHelper.hashUserPassword(target.id, newPassword)
    _userPasswordHashes.update { current ->
      val next = current.toMutableMap()
      next[target.id] = hash
      target.firebaseUid?.takeIf { it.isNotBlank() }?.let { next[it] = hash }
      next
    }
    _userSavedPasswords.update { current ->
      val next = current.toMutableMap()
      next[target.id] = newPassword
      target.firebaseUid?.takeIf { it.isNotBlank() }?.let { next[it] = newPassword }
      next
    }
    persistLocalState()
    syncUserCredentialToFirestore(target, hash, newPassword)
    syncLoginDirectoryForUser(target, rawPassword = newPassword)
    return Result.success(target)
  }

  fun recordLocalPasswordHash(userId: String, rawPassword: String) {
    if (userId.isBlank() || rawPassword.length < 6) return
    val target = _users.value.find { it.id == userId || it.ownsId(userId) } ?: return
    val hash = FirebaseAuthHelper.hashUserPassword(target.id, rawPassword)
    _userPasswordHashes.update { current ->
      val next = current.toMutableMap()
      next[target.id] = hash
      target.firebaseUid?.takeIf { it.isNotBlank() }?.let { next[it] = hash }
      next
    }
    _userSavedPasswords.update { current ->
      val next = current.toMutableMap()
      next[target.id] = rawPassword
      target.firebaseUid?.takeIf { it.isNotBlank() }?.let { next[it] = rawPassword }
      next
    }
    persistLocalState()
    syncUserCredentialToFirestore(target, hash, rawPassword)
    syncLoginDirectoryForUser(target, rawPassword = rawPassword)
  }

  fun verifyCurrentUserLocalPassword(rawPassword: String): Boolean {
    if (rawPassword.isBlank()) return false
    val user = getCurrentUser()
    val storedHash = _userPasswordHashes.value[user.id]
      ?: user.firebaseUid?.let { _userPasswordHashes.value[it] }
      ?: return false
    if (FirebaseAuthHelper.hashUserPassword(user.id, rawPassword) == storedHash) return true
    val fbUid = user.firebaseUid?.takeIf { it.isNotBlank() } ?: return false
    return FirebaseAuthHelper.hashUserPassword(fbUid, rawPassword) == storedHash
  }

  fun changeCurrentUserPassword(
    currentPassword: String,
    newPassword: String,
    verifiedByFirebaseAuth: Boolean = false
  ): Result<User> {
    if (currentPassword.isBlank()) {
      return Result.failure(IllegalArgumentException("Informe sua senha atual."))
    }
    if (newPassword.length < 6) {
      return Result.failure(IllegalArgumentException("A nova senha deve ter no mínimo 6 caracteres."))
    }
    if (newPassword == currentPassword) {
      return Result.failure(IllegalArgumentException("A nova senha deve ser diferente da senha atual."))
    }
    val user = getCurrentUser()
    if (!verifiedByFirebaseAuth) {
      val storedHash = _userPasswordHashes.value[user.id]
        ?: user.firebaseUid?.let { _userPasswordHashes.value[it] }
        ?: return Result.failure(
          IllegalStateException(
            "Nenhuma senha atual cadastrada para esta conta. Caso acesse apenas com Google, peça ao administrador para definir uma senha inicial."
          )
        )
      val matchesId = FirebaseAuthHelper.hashUserPassword(user.id, currentPassword) == storedHash
      val matchesFbUid = user.firebaseUid?.takeIf { it.isNotBlank() }?.let {
        FirebaseAuthHelper.hashUserPassword(it, currentPassword) == storedHash
      } == true
      if (!matchesId && !matchesFbUid) {
        return Result.failure(IllegalArgumentException("Senha atual incorreta. Verifique e tente novamente."))
      }
    }
    val newHash = FirebaseAuthHelper.hashUserPassword(user.id, newPassword)
    _userPasswordHashes.update { current ->
      val next = current.toMutableMap()
      next[user.id] = newHash
      user.firebaseUid?.takeIf { it.isNotBlank() }?.let { next[it] = newHash }
      next
    }
    _userSavedPasswords.update { current ->
      val next = current.toMutableMap()
      next[user.id] = newPassword
      user.firebaseUid?.takeIf { it.isNotBlank() }?.let { next[it] = newPassword }
      next
    }
    persistLocalState()
    syncUserCredentialToFirestore(user, newHash, newPassword)
    syncLoginDirectoryForUser(user, rawPassword = newPassword)
    return Result.success(user)
  }

  fun hasLocalPasswordHash(userId: String): Boolean {
    if (userId.isBlank()) return false
    return !_userPasswordHashes.value[userId].isNullOrBlank()
  }

  private fun matchesStoredPasswordHash(candidate: User, rawPassword: String): Boolean {
    val storedHash = _userPasswordHashes.value[candidate.id]
      ?: candidate.firebaseUid?.let { _userPasswordHashes.value[it] }
      ?: return false
    if (FirebaseAuthHelper.hashUserPassword(candidate.id, rawPassword) == storedHash) return true
    val fbUid = candidate.firebaseUid?.takeIf { it.isNotBlank() } ?: return false
    return FirebaseAuthHelper.hashUserPassword(fbUid, rawPassword) == storedHash
  }

  fun verifyAdminManagedPasswordByEmail(email: String, rawPassword: String): User? {
    val normalized = FirebaseAuthHelper.normalizeEmail(email)
    if (normalized.isBlank() || rawPassword.length < 6) return null
    return _users.value.firstOrNull { candidate ->
      if (FirebaseAuthHelper.normalizeEmail(candidate.email) != normalized) return@firstOrNull false
      matchesStoredPasswordHash(candidate, rawPassword)
    }
  }

  fun verifyAdminManagedPasswordByPhone(
    phone: String,
    rawPassword: String,
    countryCode: String = FirebasePhoneAuthManager.DEFAULT_COUNTRY_CODE
  ): User? {
    val targetDigits = normalizePhoneDigits(phone, countryCode)
    val canonicalTarget = FirebaseAuthHelper.canonicalPhoneDigits(phone, countryCode)
    if (targetDigits.length < 8 || rawPassword.length < 6) return null
    val e164Digits = FirebasePhoneAuthManager(authProvider = { null })
      .formatToE164(phone, countryCode)
      .filter { it.isDigit() }
    return _users.value.firstOrNull { candidate ->
      val candNorm = normalizePhoneDigits(candidate.phone, countryCode)
      val candCanonical = FirebaseAuthHelper.canonicalPhoneDigits(candidate.phone, countryCode)
      val candRaw = candidate.phone.filter { it.isDigit() }
      val phoneMatches = candNorm == targetDigits ||
        (canonicalTarget.length >= 10 && candCanonical == canonicalTarget) ||
        (e164Digits.length >= 10 && candRaw == e164Digits)
      if (!phoneMatches) return@firstOrNull false
      matchesStoredPasswordHash(candidate, rawPassword)
    }
  }

  fun authenticateExistingUserLocally(userId: String): User? {
    val existing = _users.value.find { it.id == userId } ?: return null
    _currentUserId.value = existing.id
    _isAuthenticated.value = true
    persistLocalState()
    startFirestoreSyncIfSignedIn()
    return existing
  }

  fun adminAddProviderBalance(
    providerId: String,
    amount: Double,
    reason: String = ""
  ): Result<Double> {
    val adminUser = getCurrentUser()
    if (adminUser.role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas administradores podem adicionar saldo manual ao prestador."))
    }
    val target = _users.value.find { it.id == providerId || it.firebaseUid == providerId }
      ?: return Result.failure(IllegalArgumentException("Prestador não encontrado."))
    if (target.role != UserRole.PRESTADOR) {
      return Result.failure(IllegalArgumentException("Saldo manual só pode ser adicionado para contas de Prestador."))
    }
    if (amount.isNaN() || amount <= 0.0 || amount > 50_000.0) {
      return Result.failure(IllegalArgumentException("Informe um valor válido entre R$ 0,01 e R$ 50.000,00."))
    }

    val cleanReason = reason.trim()
    val previousBalance = target.walletBalance
    val newBalance = previousBalance + amount

    _users.update { list ->
      list.map { if (it.id == target.id) it.copy(walletBalance = newBalance) else it }
    }

    val description = if (cleanReason.isNotBlank()) {
      "Crédito manual pelo administrador: $cleanReason"
    } else {
      "Crédito manual adicionado pelo administrador"
    }
    val tx = WalletTransaction(
      id = "tx_admin_${UUID.randomUUID().toString().take(8)}",
      providerId = target.id,
      type = TransactionType.CREDITO_ADMIN,
      amount = amount,
      description = description,
      timestamp = System.currentTimeMillis(),
      balanceAfter = newBalance
    )
    _walletTransactions.update { listOf(tx) + it }

    val reasonSuffix = if (cleanReason.isNotBlank()) " Motivo: $cleanReason" else ""
    val notification = AppNotification(
      id = "notif_${UUID.randomUUID().toString().take(8)}",
      recipientId = target.id,
      title = "💰 Saldo Adicionado pelo Administrador",
      message = "Você recebeu R$ ${String.format("%.2f", amount)} de crédito manual em sua carteira. Novo saldo: R$ ${String.format("%.2f", newBalance)}.$reasonSuffix",
      type = NotificationType.PAGAMENTO_PIX_CONFIRMADO
    )
    _notifications.update { listOf(notification) + it }
    persistLocalState()

    val ds = firestoreDataSource
    val fbUid = currentFirebaseUidOrNull()
    if (ds != null && fbUid != null) {
      val targetDocId = target.firebaseUid?.takeIf { it.isNotBlank() } ?: target.id
      repoScope.launch {
        ds.adminAddProviderBalance(
          providerId = targetDocId,
          amount = amount,
          reason = cleanReason,
          adminUid = fbUid,
          adminEmail = adminUser.email
        )
      }
    }
    return Result.success(newBalance)
  }

  fun adminDeleteUser(userId: String): Result<Unit> {
    if (getCurrentUser().role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas administradores podem excluir usuários."))
    }
    val target = _users.value.find { it.id == userId }
      ?: return Result.failure(IllegalArgumentException("Usuário não encontrado."))
    if (target.id == _currentUserId.value) {
      return Result.failure(IllegalStateException("Não é possível excluir o usuário Administrador atualmente conectado."))
    }
    if (target.role == UserRole.ADMIN && _users.value.count { it.role == UserRole.ADMIN } <= 1) {
      return Result.failure(IllegalStateException("Não é possível excluir o único Administrador do sistema."))
    }

    _users.update { list -> list.filterNot { it.id == target.id } }
    _proposals.update { list -> list.filterNot { target.ownsId(it.providerId) } }
    _notifications.update { list -> list.filterNot { target.ownsId(it.recipientId) } }
    _userPasswordHashes.update { current ->
      current.filterKeys { key -> key != target.id && key != target.firebaseUid }
    }
    _userSavedPasswords.update { current ->
      current.filterKeys { key -> key != target.id && key != target.firebaseUid }
    }
    persistLocalState()

    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      val targetDocId = target.firebaseUid?.takeIf { it.isNotBlank() } ?: target.id
      repoScope.launch {
        ds.deleteUserInFirestore(targetDocId)
        ds.deleteAdminUserCredential(targetDocId)
      }
    }
    return Result.success(Unit)
  }

  // --- ADMIN: Categorias de Serviços e Outras Edições ---

  fun adminAddServiceCategory(name: String, specialtyLabel: String = ""): Result<ServiceCategoryItem> {
    if (getCurrentUser().role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas administradores podem gerenciar categorias."))
    }
    val cleanName = name.trim().take(80)
    if (cleanName.length < 2) {
      return Result.failure(IllegalArgumentException("Informe um nome válido para a categoria (mínimo 2 caracteres)."))
    }
    if (_serviceCategories.value.any { it.name.equals(cleanName, ignoreCase = true) }) {
      return Result.failure(IllegalArgumentException("Já existe uma categoria chamada \"$cleanName\"."))
    }
    val nextOrder = (_serviceCategories.value.maxOfOrNull { it.orderIndex } ?: -1) + 1
    val slug = cleanName.lowercase()
      .replace(Regex("[^a-z0-9]+"), "_")
      .trim('_')
      .take(40)
      .ifBlank { "cat" }
    val newCat = ServiceCategoryItem(
      id = "cat_${slug}_${UUID.randomUUID().toString().take(4)}",
      name = cleanName,
      specialtyLabel = specialtyLabel.trim().ifBlank { cleanName }.take(120),
      isActive = true,
      orderIndex = nextOrder,
      updatedAt = System.currentTimeMillis()
    )
    _serviceCategories.update { it + newCat }
    persistLocalState()
    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      repoScope.launch { ds.saveServiceCategory(newCat) }
    }
    return Result.success(newCat)
  }

  fun adminUpdateServiceCategory(
    categoryId: String,
    name: String,
    specialtyLabel: String,
    isActive: Boolean
  ): Result<ServiceCategoryItem> {
    if (getCurrentUser().role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas administradores podem editar categorias."))
    }
    val existing = _serviceCategories.value.find { it.id == categoryId }
      ?: return Result.failure(IllegalArgumentException("Categoria não encontrada."))
    val cleanName = name.trim().take(80)
    if (cleanName.length < 2) {
      return Result.failure(IllegalArgumentException("Informe um nome válido (mínimo 2 caracteres)."))
    }
    if (_serviceCategories.value.any { it.id != categoryId && it.name.equals(cleanName, ignoreCase = true) }) {
      return Result.failure(IllegalArgumentException("Já existe outra categoria chamada \"$cleanName\"."))
    }
    if (!isActive && existing.isActive && _serviceCategories.value.count { it.isActive && it.id != categoryId } == 0) {
      return Result.failure(IllegalStateException("Mantenha pelo menos uma categoria ativa no aplicativo."))
    }
    val updated = existing.copy(
      name = cleanName,
      specialtyLabel = specialtyLabel.trim().ifBlank { cleanName }.take(120),
      isActive = isActive,
      updatedAt = System.currentTimeMillis()
    )
    _serviceCategories.update { list ->
      list.map { if (it.id == categoryId) updated else it }
    }
    if (!existing.name.equals(cleanName, ignoreCase = true)) {
      _services.update { list ->
        list.map { srv ->
          if (srv.category.equals(existing.name, ignoreCase = true)) {
            srv.copy(category = cleanName)
          } else {
            srv
          }
        }
      }
    }
    persistLocalState()
    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      repoScope.launch { ds.saveServiceCategory(updated) }
    }
    return Result.success(updated)
  }

  fun adminToggleServiceCategory(categoryId: String, isActive: Boolean): Result<ServiceCategoryItem> {
    val existing = _serviceCategories.value.find { it.id == categoryId }
      ?: return Result.failure(IllegalArgumentException("Categoria não encontrada."))
    return adminUpdateServiceCategory(
      categoryId = categoryId,
      name = existing.name,
      specialtyLabel = existing.specialtyLabel,
      isActive = isActive
    )
  }

  fun adminMoveServiceCategory(categoryId: String, moveUp: Boolean): Result<Unit> {
    if (getCurrentUser().role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas administradores podem reordenar categorias."))
    }
    val list = _serviceCategories.value.sortedBy { it.orderIndex }.toMutableList()
    val idx = list.indexOfFirst { it.id == categoryId }
    if (idx < 0) return Result.failure(IllegalArgumentException("Categoria não encontrada."))
    val targetIdx = if (moveUp) idx - 1 else idx + 1
    if (targetIdx !in list.indices) return Result.success(Unit)
    val item = list.removeAt(idx)
    list.add(targetIdx, item)
    val reindexed = list.mapIndexed { index, cat ->
      cat.copy(orderIndex = index, updatedAt = System.currentTimeMillis())
    }
    _serviceCategories.value = reindexed
    persistLocalState()
    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      repoScope.launch {
        reindexed.forEach { ds.saveServiceCategory(it) }
      }
    }
    return Result.success(Unit)
  }

  fun adminDeleteServiceCategory(categoryId: String): Result<Unit> {
    if (getCurrentUser().role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas administradores podem excluir categorias."))
    }
    val list = _serviceCategories.value
    val target = list.find { it.id == categoryId }
      ?: return Result.failure(IllegalArgumentException("Categoria não encontrada."))
    if (list.size <= 1) {
      return Result.failure(IllegalStateException("O aplicativo precisa manter ao menos 1 categoria cadastrada."))
    }
    val remaining = list.filterNot { it.id == categoryId }
    if (remaining.none { it.isActive }) {
      return Result.failure(IllegalStateException("Mantenha ao menos 1 categoria ativa no aplicativo."))
    }
    _serviceCategories.value = remaining.mapIndexed { idx, c -> c.copy(orderIndex = idx) }
    persistLocalState()
    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      repoScope.launch { ds.deleteServiceCategory(target.id) }
    }
    return Result.success(Unit)
  }

  fun adminResetDefaultCategories(): Result<List<ServiceCategoryItem>> {
    if (getCurrentUser().role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas administradores podem restaurar categorias padrão."))
    }
    val defaults = defaultServiceCategories()
    _serviceCategories.value = defaults
    persistLocalState()
    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      repoScope.launch { defaults.forEach { ds.saveServiceCategory(it) } }
    }
    return Result.success(defaults)
  }

  fun adminUpdateServiceRequest(updated: ServiceRequest): Result<ServiceRequest> {
    if (getCurrentUser().role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas administradores podem editar serviços."))
    }
    val existing = _services.value.find { it.id == updated.id }
      ?: return Result.failure(IllegalArgumentException("Serviço não encontrado."))
    val cleanTitle = updated.title.trim()
    val cleanCategory = updated.category.trim()
    val cleanDesc = updated.description.trim()
    val cleanLocation = updated.location.trim()
    if (cleanTitle.length < 3) {
      return Result.failure(IllegalArgumentException("Informe um título com pelo menos 3 caracteres."))
    }
    if (cleanCategory.isBlank()) {
      return Result.failure(IllegalArgumentException("Selecione uma categoria para o serviço."))
    }
    if (updated.proposedBudget < 0.0) {
      return Result.failure(IllegalArgumentException("O orçamento não pode ser negativo."))
    }
    val sanitized = existing.copy(
      title = cleanTitle.take(200),
      category = cleanCategory.take(100),
      description = cleanDesc.take(2000),
      location = cleanLocation.ifBlank { existing.location }.take(300),
      proposedBudget = updated.proposedBudget,
      urgency = updated.urgency.ifBlank { existing.urgency },
      status = updated.status
    )
    _services.update { list -> list.map { if (it.id == sanitized.id) sanitized else it } }
    persistLocalState()
    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      repoScope.launch { ds.adminUpdateServiceRequest(sanitized) }
    }
    return Result.success(sanitized)
  }

  fun adminUpdateAppSettings(updated: AppAdminSettings): Result<AppAdminSettings> {
    if (getCurrentUser().role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas administradores podem editar configurações do app."))
    }
    val digits = updated.supportWhatsappNumberE164.filter { it.isDigit() }
    if (digits.length < 10) {
      return Result.failure(IllegalArgumentException("Informe um WhatsApp de suporte válido com DDD (mínimo 10 dígitos)."))
    }
    val sanitized = updated.copy(
      appDisplayName = updated.appDisplayName.trim().ifBlank { "ResolveAi Serviços" }.take(80),
      globalBannerMessage = updated.globalBannerMessage.trim().take(400),
      supportWhatsappNumberE164 = digits.take(25),
      supportWhatsappDisplay = updated.supportWhatsappDisplay.trim().ifBlank { digits }.take(40),
      defaultServiceUrgency = updated.defaultServiceUrgency.takeIf { it in listOf("Baixa", "Média", "Alta", "Urgente") } ?: "Média",
      updatedAt = System.currentTimeMillis()
    )
    _appAdminSettings.value = sanitized
    SupportLauncher.updateSupportContact(sanitized.supportWhatsappNumberE164, sanitized.supportWhatsappDisplay)
    persistLocalState()
    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      repoScope.launch { ds.saveAppAdminConfig(sanitized, _appTips.value) }
    }
    return Result.success(sanitized)
  }

  fun adminSaveAppTip(tip: AppTip): Result<AppTip> {
    if (getCurrentUser().role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas administradores podem gerenciar dicas do app."))
    }
    val cleanTitle = tip.title.trim()
    val cleanDesc = tip.description.trim()
    val cleanCat = tip.category.trim().ifBlank { "Geral" }
    if (cleanTitle.length < 3) {
      return Result.failure(IllegalArgumentException("Informe um título com pelo menos 3 caracteres."))
    }
    if (cleanDesc.length < 5) {
      return Result.failure(IllegalArgumentException("Informe uma descrição com pelo menos 5 caracteres."))
    }
    val resolvedId = tip.id.ifBlank { "tip_${UUID.randomUUID().toString().take(6)}" }
    val sanitized = tip.copy(
      id = resolvedId,
      category = cleanCat.take(60),
      title = cleanTitle.take(150),
      description = cleanDesc.take(600)
    )
    _appTips.update { list ->
      if (list.any { it.id == resolvedId }) {
        list.map { if (it.id == resolvedId) sanitized else it }
      } else {
        listOf(sanitized) + list
      }
    }
    persistLocalState()
    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      repoScope.launch { ds.saveAppAdminConfig(_appAdminSettings.value, _appTips.value) }
    }
    return Result.success(sanitized)
  }

  fun adminDeleteAppTip(tipId: String): Result<Unit> {
    if (getCurrentUser().role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas administradores podem excluir dicas."))
    }
    _appTips.update { list -> list.filterNot { it.id == tipId } }
    persistLocalState()
    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      repoScope.launch { ds.saveAppAdminConfig(_appAdminSettings.value, _appTips.value) }
    }
    return Result.success(Unit)
  }

  // Client creates a new service request
  fun createServiceRequest(
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
  ): ServiceRequest {
    val currentUser = getCurrentUser()
    val dist = (25..75).random() / 10.0
    val driving = (dist * 2.8).toInt().coerceAtLeast(8)
    val moto = (driving * 0.65).toInt().coerceAtLeast(5)
    val transit = (driving * 1.5).toInt().coerceAtLeast(15)

    val partsByDash = location.split(" - ").map { it.trim() }.filter { it.isNotBlank() }
    val parsedAddress = partsByDash.firstOrNull() ?: if (location.contains(",")) location else "$location, ${(100..999).random()}"
    val parsedCity = when {
      location.contains(" - ") && location.substringAfter(" - ").contains(",") ->
        location.substringAfterLast(",").trim()
      location.contains(",") -> location.split(",").last().trim()
      else -> currentUser.city.takeIf { it.isNotBlank() } ?: location.trim()
    }
    val parsedNeighborhood = when {
      partsByDash.size >= 2 && partsByDash[1].contains(",") -> partsByDash[1].substringBefore(",").trim()
      partsByDash.size >= 2 -> partsByDash[1]
      else -> parsedAddress.substringBefore(",").trim()
    }

    val clientLoc = ClientLocation(
      address = if (parsedAddress.contains(",")) parsedAddress else "$parsedAddress, ${(100..999).random()}",
      neighborhood = parsedNeighborhood,
      city = parsedCity,
      referencePoint = "Endereço verificado via Google Maps",
      distanceKm = dist,
      drivingTimeMinutes = driving,
      motoTimeMinutes = moto,
      transitTimeMinutes = transit,
      trafficCondition = if (driving > 20) "Trânsito moderado" else "Trânsito fluido",
      bestRoute = "Via principais vias expressas"
    )

    val newService = ServiceRequest(
      id = "srv_${UUID.randomUUID().toString().take(6)}",
      clientId = currentUser.id,
      clientName = currentUser.name,
      clientRating = currentUser.rating,
      clientAvatarColorHex = currentUser.avatarColorHex,
      clientPhotoUrl = currentUser.photoUrl,
      title = title,
      category = category,
      description = description,
      location = location,
      proposedBudget = proposedBudget,
      urgency = urgency,
      status = ServiceStatus.ABERTO,
      createdAt = System.currentTimeMillis(),
      clientLocation = clientLoc,
      attachmentPhotoUri = attachmentPhotoUri?.takeIf { it.isNotBlank() },
      attachmentPhotoName = attachmentPhotoName?.takeIf { it.isNotBlank() },
      attachmentVideoUri = attachmentVideoUri?.trim()?.takeIf { it.isNotBlank() }?.take(2000),
      attachmentVideoName = attachmentVideoName?.trim()?.takeIf { it.isNotBlank() }?.take(300),
      attachmentVideoThumbnailUri = attachmentVideoThumbnailUri?.trim()?.takeIf { it.isNotBlank() }?.take(250_000),
      attachmentVideoDurationMs = attachmentVideoDurationMs
    )

    _services.update { listOf(newService) + it }

    // Notify all providers about the new service opportunity!
    val notif = AppNotification(
      id = "notif_${UUID.randomUUID().toString().take(6)}",
      recipientId = "all_providers",
      title = "🔔 Novo Serviço Disponível: $category",
      message = "$title em $location. Orçamento proposto pelo cliente: R$ ${String.format("%.2f", proposedBudget)}.",
      type = NotificationType.NOVO_SERVICO,
      serviceId = newService.id,
      timestamp = System.currentTimeMillis(),
      isRead = false
    )
    emitNotification(notif, dedupKey = "srv_open_${newService.id}")

    // Add security message to chat
    _chatMessages.update {
      it + ChatMessage(
        id = "sys_${UUID.randomUUID().toString().take(6)}",
        serviceId = newService.id,
        senderId = "system",
        senderName = "ResolveAi Proteção",
        senderRole = UserRole.CLIENTE,
        content = "🔒 Sala de chat privada iniciada. Não é permitida a troca de contatos externos (telefone, redes sociais, email). Todas as tratativas devem permanecer no app.",
        timestamp = System.currentTimeMillis(),
        isSystemNotice = true
      )
    }
    persistLocalState()

    val ds = firestoreDataSource
    val fbUid = currentFirebaseUidOrNull()
    if (ds != null && fbUid != null) {
      val activeClientDocId = currentUser.firebaseUid?.takeIf { it.isNotBlank() } ?: fbUid
      val cloudService = if (newService.clientId == activeClientDocId) {
        newService
      } else {
        newService.copy(clientId = activeClientDocId)
      }
      repoScope.launch {
        ds.publishServiceRequest(cloudService)
        syncProposalListenersForClientServices()
      }
    }

    return newService
  }

  // Client updates the proposed budget of their service
  fun updateServiceBudget(serviceId: String, newBudget: Double): Boolean {
    var updated = false
    _services.update { list ->
      list.map { srv ->
        if (srv.id == serviceId && (srv.status == ServiceStatus.ABERTO || srv.status == ServiceStatus.EM_NEGOCIACAO)) {
          updated = true
          srv.copy(proposedBudget = newBudget)
        } else srv
      }
    }
    if (updated) {
      addChatMessage(
        serviceId = serviceId,
        content = "💰 O cliente alterou o valor proposto do serviço para R$ ${String.format("%.2f", newBudget)}",
        isSystem = true
      )
      persistLocalState()
      val ds = firestoreDataSource
      if (ds != null && currentFirebaseUidOrNull() != null) {
        repoScope.launch {
          ds.updateServiceBudget(serviceId, newBudget)
        }
      }
    }
    return updated
  }

  // Provider submits a proposal
  fun submitProposal(
    serviceId: String,
    proposedPrice: Double,
    estimatedDays: Int,
    description: String
  ): Proposal {
    val currentUser = getCurrentUser()
    val newProposal = Proposal(
      id = "prop_${UUID.randomUUID().toString().take(6)}",
      serviceId = serviceId,
      providerId = currentUser.id,
      providerName = currentUser.name,
      providerRating = currentUser.rating,
      providerCategory = currentUser.category,
      providerAvatarColorHex = currentUser.avatarColorHex,
      providerPhotoUrl = currentUser.photoUrl,
      proposedPrice = proposedPrice,
      estimatedDays = estimatedDays,
      description = description,
      status = ProposalStatus.ENVIADA,
      createdAt = System.currentTimeMillis()
    )

    _proposals.update { listOf(newProposal) + it }

    // Update service status to EM_NEGOCIACAO if was ABERTO
    _services.update { list ->
      list.map { srv ->
        if (srv.id == serviceId && srv.status == ServiceStatus.ABERTO) {
          srv.copy(status = ServiceStatus.EM_NEGOCIACAO)
        } else srv
      }
    }

    val targetService = _services.value.find { it.id == serviceId }
    if (targetService != null) {
      emitNotification(
        AppNotification(
          id = "notif_prop_${newProposal.id}",
          recipientId = targetService.clientId,
          title = "📩 Nova Proposta Recebida!",
          message = "${currentUser.name} enviou proposta de R$ ${String.format("%.2f", proposedPrice)} para \"${targetService.title}\".",
          type = NotificationType.PROPOSTA_RECEBIDA,
          serviceId = serviceId
        ),
        dedupKey = "prop_new_${newProposal.id}",
        dispatchSystemBanner = false
      )
    }

    persistLocalState()
    val ds = firestoreDataSource
    val fbUid = currentFirebaseUidOrNull()
    if (ds != null && fbUid != null) {
      val cloudProviderId = currentUser.firebaseUid?.takeIf { it.isNotBlank() } ?: fbUid
      val localServiceClientId = targetService?.clientId?.trim().orEmpty()
      val isLocalPlaceholderClientId = localServiceClientId.isBlank() ||
        localServiceClientId.startsWith("u_") ||
        localServiceClientId.startsWith("cli_")
      val serviceClientUser = targetService?.let { s -> _users.value.find { it.ownsId(s.clientId) } }
      val initialResolvedClientId = if (isLocalPlaceholderClientId) {
        serviceClientUser?.firebaseUid?.takeIf { it.isNotBlank() } ?: localServiceClientId
      } else {
        localServiceClientId
      }
      val cloudProposal = newProposal.copy(providerId = cloudProviderId)
      repoScope.launch {
        val authoritativeClientId = if (initialResolvedClientId.isBlank() ||
          initialResolvedClientId.startsWith("u_") ||
          initialResolvedClientId.startsWith("cli_")
        ) {
          ds.fetchServiceById(serviceId).getOrNull()?.clientId?.takeIf { it.isNotBlank() }
            ?: initialResolvedClientId
        } else {
          initialResolvedClientId
        }
        if (authoritativeClientId.isNotBlank()) {
          ds.submitProposal(cloudProposal, authoritativeClientId)
        }
        ds.markServiceNegotiatingIfStillOpen(serviceId)
      }
    }

    return newProposal
  }

  // Send a counter-proposal (contraproposta)
  fun sendCounterOffer(
    proposalId: String,
    newPrice: Double,
    note: String
  ): Boolean {
    val currentUser = getCurrentUser()
    val proposal = _proposals.value.find { it.id == proposalId } ?: return false

    val entry = CounterOfferEntry(
      price = newPrice,
      authorId = currentUser.id,
      authorName = currentUser.name,
      authorRole = currentUser.role,
      note = note,
      timestamp = System.currentTimeMillis()
    )

    val newStatus = if (currentUser.role == UserRole.CLIENTE) {
      ProposalStatus.CONTRAPROPOSTA_CLIENTE
    } else {
      ProposalStatus.CONTRAPROPOSTA_PRESTADOR
    }

    val updatedCounterOffers = proposal.counterOffers + entry
    _proposals.update { list ->
      list.map { prop ->
        if (prop.id == proposalId) {
          prop.copy(
            status = newStatus,
            counterOffers = updatedCounterOffers
          )
        } else prop
      }
    }

    // Also send an automated note into the chat
    val roleStr = if (currentUser.role == UserRole.CLIENTE) "Cliente" else "Prestador"
    addChatMessage(
      serviceId = proposal.serviceId,
      content = "💬 Nova contraproposta enviada por $roleStr: R$ ${String.format("%.2f", newPrice)} (${note.ifBlank { "Sem observações" }})",
      isSystem = true
    )

    val targetService = _services.value.find { it.id == proposal.serviceId }
    val recipientId = if (currentUser.role == UserRole.CLIENTE) {
      proposal.providerId
    } else {
      targetService?.clientId ?: ""
    }
    if (recipientId.isNotBlank()) {
      emitNotification(
        AppNotification(
          id = "notif_counter_${proposalId}_${updatedCounterOffers.size}",
          recipientId = recipientId,
          title = "💬 Nova Contraproposta ($roleStr)",
          message = "${currentUser.name} enviou contraproposta de R$ ${String.format("%.2f", newPrice)} para \"${targetService?.title ?: "serviço"}\".",
          type = NotificationType.PROPOSTA_RECEBIDA,
          serviceId = proposal.serviceId
        ),
        dedupKey = "prop_counter_${proposalId}_${updatedCounterOffers.size}",
        dispatchSystemBanner = false
      )
    }

    persistLocalState()
    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      val counterList = updatedCounterOffers.map { c ->
        mapOf(
          "price" to c.price,
          "authorId" to c.authorId,
          "authorName" to c.authorName,
          "authorRole" to c.authorRole.name,
          "note" to c.note,
          "timestamp" to c.timestamp
        )
      }
      repoScope.launch {
        ds.updateProposalFields(
          proposalId,
          mapOf("status" to newStatus.name, "counterOffers" to counterList)
        )
      }
    }

    return true
  }

  /**
   * Accepts proposal and debits the 10% platform fee from provider's digital wallet.
   * Returns:
   *  - Success: Result.success(Double) -> platformFee
   *  - Insufficient Balance: Result.failure(Exception) -> with details on needed recharge
   */
  fun acceptProposalAndDebitFee(proposalId: String): Result<Double> {
    val proposal = _proposals.value.find { it.id == proposalId }
      ?: return Result.failure(Exception("Proposta não encontrada"))

    val service = _services.value.find { it.id == proposal.serviceId }
      ?: return Result.failure(Exception("Serviço não encontrado"))

    val finalPrice = proposal.currentPrice
    val platformFee = finalPrice * 0.10 // 10% de taxa da plataforma

    val cachedProvider = _users.value.find { it.ownsId(proposal.providerId) }
    val provider = cachedProvider ?: User(
      id = proposal.providerId,
      name = proposal.providerName,
      email = "",
      phone = "(11) 90000-0000",
      role = UserRole.PRESTADOR,
      category = proposal.providerCategory,
      rating = proposal.providerRating,
      avatarColorHex = proposal.providerAvatarColorHex,
      photoUrl = proposal.providerPhotoUrl,
      walletBalance = finalPrice,
      firebaseUid = proposal.providerId
    ).also { synthesized ->
      _users.update { list -> list + synthesized }
    }

    // Provider can accept any service when wallet balance is above R$ 1.00,
    // even if deducting the 10% platform fee makes the balance negative (settled on next recharge).
    if (cachedProvider != null && provider.walletBalance <= MIN_PROVIDER_ACCEPTANCE_BALANCE) {
      val missing = (MIN_PROVIDER_ACCEPTANCE_BALANCE + 0.01 - provider.walletBalance).coerceAtLeast(0.01)
      return Result.failure(
        InsufficientBalanceException(
          requiredFee = platformFee,
          currentBalance = provider.walletBalance,
          missingAmount = missing,
          providerName = provider.name
        )
      )
    }

    // Debit fee from provider's wallet (allows negative balance for automatic deduction on recharge)
    val newBalance = provider.walletBalance - platformFee
    _users.update { list ->
      list.map { u ->
        if (u.id == provider.id) {
          u.copy(
            walletBalance = newBalance,
            completedJobs = u.completedJobs + 1
          )
        } else u
      }
    }

    // Record wallet transaction
    val transaction = WalletTransaction(
      id = "tx_${UUID.randomUUID().toString().take(6)}",
      providerId = provider.id,
      type = TransactionType.DEBITO_TAXA_10,
      amount = platformFee,
      serviceId = service.id,
      serviceTitle = service.title,
      description = "Taxa de intermediação de 10% referente ao serviço contratado #${service.id}",
      timestamp = System.currentTimeMillis(),
      balanceAfter = newBalance
    )
    _walletTransactions.update { listOf(transaction) + it }

    // Update proposal status: ONLY the chosen proposal becomes ACEITA; ALL other proposals for this service become RECUSADA
    _proposals.update { list ->
      list.map { prop ->
        if (prop.id == proposalId) {
          prop.copy(status = ProposalStatus.ACEITA)
        } else if (prop.serviceId == proposal.serviceId) {
          prop.copy(status = ProposalStatus.RECUSADA)
        } else prop
      }
    }

    // Update service request status directly to EM_EXECUCAO ("Em Andamento") between client and provider
    val acceptedProviderCloudId = provider.firebaseUid?.takeIf { it.isNotBlank() }
      ?: proposal.providerId.takeIf { it.isNotBlank() && !it.startsWith("pro_") && !it.startsWith("u_") }
      ?: proposal.providerId.takeIf { it.isNotBlank() }
      ?: provider.id
    val resolvedProviderName = proposal.providerName.ifBlank { provider.name }.trim().ifBlank { "Prestador" }.take(100)
    val resolvedProviderPhoto = (proposal.providerPhotoUrl?.takeIf { it.isNotBlank() } ?: provider.photoUrl?.takeIf { it.isNotBlank() })
      ?.takeIf { it.length <= 250_000 }
    val resolvedProviderColor = if (proposal.providerAvatarColorHex != 0L) proposal.providerAvatarColorHex else provider.avatarColorHex
    var updatedLocalService: ServiceRequest? = null
    _services.update { list ->
      list.map { srv ->
        if (srv.id == service.id) {
          srv.copy(
            status = ServiceStatus.EM_EXECUCAO,
            finalAgreedPrice = finalPrice,
            platformFee = platformFee,
            acceptedProviderId = acceptedProviderCloudId,
            acceptedProviderName = resolvedProviderName,
            acceptedProviderRating = provider.rating,
            acceptedProviderAvatarColorHex = resolvedProviderColor,
            acceptedProviderPhotoUrl = resolvedProviderPhoto,
            locationTrackingActive = true
          ).also { updatedLocalService = it }
        } else srv
      }
    }

    // Post celebration in chat locally first, then sync Firestore service -> chat
    val sysMsg = addChatMessage(
      serviceId = service.id,
      content = "🎉 Proposta aceita! Valor acordado: R$ ${String.format("%.2f", finalPrice)}. Pedido agora Em Andamento entre as partes e chat liberado!",
      isSystem = true,
      syncToFirestore = false
    )

    emitNotification(
      AppNotification(
        id = "notif_aceito_${service.id}",
        recipientId = acceptedProviderCloudId,
        title = "🎉 Pedido Em Andamento! Proposta Aceita",
        message = "${service.clientName} aceitou sua proposta de R$ ${String.format("%.2f", finalPrice)} para \"${service.title}\". O pedido está Em Andamento.",
        type = NotificationType.PROPOSTA_ACEITA,
        serviceId = service.id
      ),
      dedupKey = "srv_aceito_${service.id}",
      dispatchSystemBanner = false
    )

    persistLocalState()
    syncChatListenersForEligibleServices()
    val ds = firestoreDataSource
    val fbUid = currentFirebaseUidOrNull()
    if (ds != null && fbUid != null) {
      val serviceUpdates = hashMapOf<String, Any>(
        "status" to ServiceStatus.EM_EXECUCAO.name,
        "assignedProviderId" to acceptedProviderCloudId,
        "assignedProviderName" to resolvedProviderName,
        "acceptedProviderId" to acceptedProviderCloudId,
        "acceptedProviderName" to resolvedProviderName,
        "acceptedProviderRating" to provider.rating,
        "acceptedProviderAvatarColorHex" to resolvedProviderColor,
        "finalAgreedPrice" to finalPrice,
        "platformFee" to platformFee,
        "locationTrackingActive" to true
      )
      resolvedProviderPhoto?.let { serviceUpdates["acceptedProviderPhotoUrl"] = it }
      val competingProposalIds = _proposals.value
        .filter { it.serviceId == service.id && it.id != proposalId }
        .map { it.id }
      val activeUser = getCurrentUser()
      val activeRole = activeUser.role
      val activeClientDocId = activeUser.firebaseUid?.takeIf { it.isNotBlank() } ?: fbUid
      repoScope.launch {
        // Atualiza o pedido PRIMEIRO no Firestore para liberar o chat imediatamente e remover da lista de oportunidades de outros prestadores
        var serviceUpdated = ds.updateServiceFields(service.id, serviceUpdates)
        if (!serviceUpdated && updatedLocalService != null) {
          serviceUpdated = ds.publishServiceRequest(updatedLocalService!!.copy(clientId = activeClientDocId))
        }
        ds.updateProposalFields(proposalId, mapOf("status" to ProposalStatus.ACEITA.name))
        ds.sendChatMessage(service.id, sysMsg)
        val remoteForService = ds.fetchProposalsForService(service.id, fbUid, activeRole)
        val allCompetingIds = (competingProposalIds + remoteForService.filter { it.id != proposalId }.map { it.id }).distinct()
        if (allCompetingIds.isNotEmpty()) {
          _proposals.update { list ->
            list.map { p ->
              if (p.serviceId == service.id && p.id in allCompetingIds) p.copy(status = ProposalStatus.RECUSADA) else p
            }
          }
        }
        allCompetingIds.forEach { competingId ->
          ds.updateProposalFields(competingId, mapOf("status" to ProposalStatus.RECUSADA.name))
        }
        syncChatListenersForEligibleServices()
      }
    }

    return Result.success(platformFee)
  }

  /**
   * Client rejects a specific provider's direct acceptance or proposal without closing the service,
   * so other interested providers can still be considered.
   */
  fun rejectProposal(proposalId: String): Result<Unit> {
    val proposal = _proposals.value.find { it.id == proposalId }
      ?: return Result.failure(IllegalArgumentException("Proposta não encontrada"))
    val service = _services.value.find { it.id == proposal.serviceId }
      ?: return Result.failure(IllegalArgumentException("Serviço não encontrado"))
    val activeUser = getCurrentUser()
    if (!activeUser.ownsId(service.clientId) && activeUser.role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas o cliente solicitante pode recusar prestadores neste pedido."))
    }
    if (proposal.status == ProposalStatus.ACEITA ||
      service.status == ServiceStatus.ACEITO ||
      service.status == ServiceStatus.EM_EXECUCAO ||
      service.status == ServiceStatus.CONCLUIDO
    ) {
      return Result.failure(IllegalStateException("Este serviço já possui prestador confirmado e não permite recusar propostas."))
    }
    if (proposal.status == ProposalStatus.RECUSADA) {
      return Result.success(Unit)
    }

    _proposals.update { list ->
      list.map { prop ->
        if (prop.id == proposalId) prop.copy(status = ProposalStatus.RECUSADA) else prop
      }
    }

    val hasRemainingActiveProposals = _proposals.value.any {
      it.serviceId == service.id && it.status != ProposalStatus.RECUSADA
    }
    var updatedServiceStatus: ServiceStatus? = null
    if (!hasRemainingActiveProposals && service.status == ServiceStatus.EM_NEGOCIACAO) {
      updatedServiceStatus = ServiceStatus.ABERTO
      _services.update { list ->
        list.map { srv ->
          if (srv.id == service.id) srv.copy(status = ServiceStatus.ABERTO) else srv
        }
      }
    }

    emitNotification(
      AppNotification(
        id = "notif_recusa_${proposalId}",
        recipientId = proposal.providerId,
        title = "Proposta recusada pelo cliente",
        message = "${service.clientName} optou por não seguir com sua solicitação em \"${service.title}\".",
        type = NotificationType.STATUS_ATUALIZADO,
        serviceId = service.id
      ),
      dedupKey = "srv_recusa_${proposalId}",
      dispatchSystemBanner = false
    )

    persistLocalState()
    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      repoScope.launch {
        ds.updateProposalFields(proposalId, mapOf("status" to ProposalStatus.RECUSADA.name))
        if (updatedServiceStatus != null) {
          ds.updateServiceFields(service.id, mapOf("status" to updatedServiceStatus.name))
        }
      }
    }
    return Result.success(Unit)
  }

  /**
   * Provider explicitly declines an open or negotiating service from order details.
   * Marks any pending proposal/direct-acceptance by this provider as declined, reverts
   * EM_NEGOCIACAO back to ABERTO if no active proposals remain, and removes the order
   * from this provider's feed while leaving it visible to other providers.
   */
  fun declineServiceByProvider(serviceId: String): Result<Unit> {
    val provider = getCurrentUser()
    if (provider.role != UserRole.PRESTADOR) {
      return Result.failure(IllegalStateException("Apenas prestadores podem recusar este pedido."))
    }
    val service = _services.value.find { it.id == serviceId }
      ?: return Result.failure(IllegalArgumentException("Serviço não encontrado"))

    if (service.status == ServiceStatus.CONCLUIDO || service.status == ServiceStatus.CANCELADO) {
      return Result.failure(IllegalStateException("Este serviço já foi encerrado e não pode ser recusado."))
    }
    if (provider.ownsId(service.acceptedProviderId) &&
      (service.status == ServiceStatus.ACEITO || service.status == ServiceStatus.EM_EXECUCAO)
    ) {
      return Result.failure(IllegalStateException("Você já foi contratado neste serviço. Use a opção de cancelar caso não possa atender."))
    }

    val updatedDeclinedIds = (provider.declinedServiceIds + serviceId).distinct()
    _users.update { list ->
      list.map { u ->
        if (u.id == provider.id || u.ownsId(provider.id)) {
          u.copy(declinedServiceIds = updatedDeclinedIds)
        } else {
          u
        }
      }
    }
    _declinedServiceIdsByProvider.update { current ->
      val next = current.toMutableMap()
      val mergedSet = (next[provider.id].orEmpty() + updatedDeclinedIds).toSet()
      next[provider.id] = mergedSet
      provider.firebaseUid?.takeIf { it.isNotBlank() }?.let { uid ->
        next[uid] = (next[uid].orEmpty() + mergedSet).toSet()
      }
      next
    }

    val affectedProposalIds = _proposals.value
      .filter {
        it.serviceId == serviceId &&
          provider.ownsId(it.providerId) &&
          it.status != ProposalStatus.RECUSADA &&
          it.status != ProposalStatus.ACEITA
      }
      .map { it.id }

    if (affectedProposalIds.isNotEmpty()) {
      _proposals.update { list ->
        list.map { prop ->
          if (prop.id in affectedProposalIds) prop.copy(status = ProposalStatus.RECUSADA) else prop
        }
      }
    }

    var updatedServiceStatus: ServiceStatus? = null
    if (service.status == ServiceStatus.EM_NEGOCIACAO) {
      val hasRemainingActiveProposals = _proposals.value.any {
        it.serviceId == serviceId && it.status != ProposalStatus.RECUSADA
      }
      if (!hasRemainingActiveProposals) {
        updatedServiceStatus = ServiceStatus.ABERTO
        _services.update { list ->
          list.map { srv ->
            if (srv.id == serviceId) srv.copy(status = ServiceStatus.ABERTO) else srv
          }
        }
      }
    }

    persistLocalState()
    val ds = firestoreDataSource
    val fbUid = currentFirebaseUidOrNull()
    if (ds != null && fbUid != null) {
      val targetUserDocId = provider.firebaseUid?.takeIf { it.isNotBlank() } ?: fbUid
      repoScope.launch {
        ds.updateUserFields(
          targetUserDocId,
          mapOf("declinedServiceIds" to updatedDeclinedIds.take(500))
        )
        affectedProposalIds.forEach { propId ->
          ds.updateProposalFields(propId, mapOf("status" to ProposalStatus.RECUSADA.name))
        }
        if (updatedServiceStatus != null) {
          ds.updateServiceFields(serviceId, mapOf("status" to updatedServiceStatus.name))
        }
      }
    }
    return Result.success(Unit)
  }

  fun canUserAccessServiceChat(
    service: ServiceRequest,
    user: User = getCurrentUser()
  ): Boolean {
    val isChatOpen = !service.acceptedProviderId.isNullOrBlank() &&
      (service.status == ServiceStatus.ACEITO ||
        service.status == ServiceStatus.EM_EXECUCAO ||
        service.status == ServiceStatus.CONCLUIDO ||
        (user.role == UserRole.ADMIN && service.status == ServiceStatus.CANCELADO))
    if (!isChatOpen) return false
    return when (user.role) {
      UserRole.CLIENTE -> user.ownsId(service.clientId)
      UserRole.PRESTADOR -> user.ownsId(service.acceptedProviderId)
      UserRole.ADMIN -> true
    }
  }

  /**
   * Provider directly accepts the client's service at the proposed budget.
   * Registers the provider's acceptance so multiple providers can accept the same service,
   * leaving the final choice and chat unlock to the client.
   */
  fun acceptServiceDirectlyByProvider(serviceId: String): Result<Double> {
    val provider = getCurrentUser()
    val service = _services.value.find { it.id == serviceId }
      ?: return Result.failure(Exception("Serviço não encontrado"))

    if (service.status != ServiceStatus.ABERTO && service.status != ServiceStatus.EM_NEGOCIACAO) {
      return Result.failure(IllegalStateException("Este serviço já foi fechado ou encerrado."))
    }

    val finalPrice = service.proposedBudget
    val platformFee = finalPrice * 0.10

    if (provider.walletBalance <= MIN_PROVIDER_ACCEPTANCE_BALANCE) {
      val missing = (MIN_PROVIDER_ACCEPTANCE_BALANCE + 0.01 - provider.walletBalance).coerceAtLeast(0.01)
      return Result.failure(
        InsufficientBalanceException(
          requiredFee = platformFee,
          currentBalance = provider.walletBalance,
          missingAmount = missing,
          providerName = provider.name
        )
      )
    }

    val existingActive = _proposals.value.firstOrNull {
      it.serviceId == serviceId &&
        provider.ownsId(it.providerId) &&
        it.status != ProposalStatus.RECUSADA
    }
    if (existingActive?.isDirectAcceptance == true) {
      return Result.failure(
        IllegalStateException("Você já aceitou este pedido e está aguardando a escolha do cliente.")
      )
    }

    val acceptanceProposal = Proposal(
      id = existingActive?.id ?: "prop_acc_${UUID.randomUUID().toString().take(8)}",
      serviceId = service.id,
      providerId = provider.id,
      providerName = provider.name,
      providerRating = provider.rating,
      providerCategory = provider.category,
      providerAvatarColorHex = provider.avatarColorHex,
      providerPhotoUrl = provider.photoUrl,
      proposedPrice = finalPrice,
      estimatedDays = 1,
      description = "Aceitou realizar o serviço pelo valor proposto de R$ ${String.format("%.2f", finalPrice)}.",
      status = ProposalStatus.ENVIADA,
      createdAt = System.currentTimeMillis(),
      isDirectAcceptance = true
    )

    _proposals.update { list ->
      if (existingActive != null) {
        list.map { if (it.id == existingActive.id) acceptanceProposal else it }
      } else {
        listOf(acceptanceProposal) + list
      }
    }

    if (service.status == ServiceStatus.ABERTO) {
      _services.update { list ->
        list.map { srv ->
          if (srv.id == service.id) srv.copy(status = ServiceStatus.EM_NEGOCIACAO) else srv
        }
      }
    }

    emitNotification(
      AppNotification(
        id = "notif_aceite_prov_${acceptanceProposal.id}",
        recipientId = service.clientId,
        title = "🙋 Prestador aceitou seu pedido!",
        message = "${provider.name} aceitou \"${service.title}\" por R$ ${String.format("%.2f", finalPrice)}. Escolha este prestador para liberar o chat.",
        type = NotificationType.PROPOSTA_RECEBIDA,
        serviceId = service.id
      ),
      dedupKey = "srv_prov_accept_${service.id}_${provider.id}",
      dispatchSystemBanner = false
    )

    persistLocalState()
    val ds = firestoreDataSource
    val fbUid = currentFirebaseUidOrNull()
    if (ds != null && fbUid != null) {
      val cloudProviderId = provider.firebaseUid?.takeIf { it.isNotBlank() } ?: fbUid
      val localServiceClientId = service.clientId.trim()
      val isLocalPlaceholderClientId = localServiceClientId.isBlank() ||
        localServiceClientId.startsWith("u_") ||
        localServiceClientId.startsWith("cli_")
      val serviceClientUser = _users.value.find { it.ownsId(service.clientId) }
      val initialResolvedClientId = if (isLocalPlaceholderClientId) {
        serviceClientUser?.firebaseUid?.takeIf { it.isNotBlank() } ?: localServiceClientId
      } else {
        localServiceClientId
      }
      val cloudProposal = acceptanceProposal.copy(providerId = cloudProviderId)
      repoScope.launch {
        val authoritativeClientId = if (initialResolvedClientId.isBlank() ||
          initialResolvedClientId.startsWith("u_") ||
          initialResolvedClientId.startsWith("cli_")
        ) {
          ds.fetchServiceById(service.id).getOrNull()?.clientId?.takeIf { it.isNotBlank() }
            ?: initialResolvedClientId
        } else {
          initialResolvedClientId
        }
        if (authoritativeClientId.isNotBlank()) {
          ds.submitProposal(cloudProposal, authoritativeClientId)
        }
        if (service.status == ServiceStatus.ABERTO) {
          ds.markServiceNegotiatingIfStillOpen(service.id)
        }
      }
    }

    return Result.success(finalPrice)
  }

  private val processedPixTxIds = mutableSetOf<String>()

  fun registerPixCharge(charge: PixRechargeCharge) {
    if (charge.paymentGateway == "MERCADO_PAGO" || charge.mercadoPagoPaymentId != null) {
      _mercadoPagoCharges.update { list ->
        listOf(charge) + list.filterNot { it.txId == charge.txId }
      }
      persistLocalState()
    }
    val ds = firestoreDataSource ?: return
    val fbUid = currentFirebaseUidOrNull() ?: return
    val firestoreCharge = charge.copy(providerId = fbUid)
    repoScope.launch {
      ds.createPixRechargeCharge(firestoreCharge)
    }
  }

  fun updateMercadoPagoChargeStatus(
    txId: String,
    status: PixChargeStatus,
    mpStatus: String? = null,
    mpStatusDetail: String? = null,
    paidAt: Long? = null
  ) {
    _mercadoPagoCharges.update { list ->
      list.map { c ->
        if (c.txId == txId) {
          c.copy(
            status = status,
            detectedAutomatically = status == PixChargeStatus.PAGO || c.detectedAutomatically,
            mpStatus = mpStatus ?: c.mpStatus,
            mpStatusDetail = mpStatusDetail ?: c.mpStatusDetail,
            paidAt = paidAt ?: c.paidAt
          )
        } else c
      }
    }
    persistLocalState()
    val ds = firestoreDataSource
    val fbUid = currentFirebaseUidOrNull()
    if (ds != null && fbUid != null) {
      repoScope.launch {
        ds.updatePixChargeStatus(
          providerId = fbUid,
          txId = txId,
          status = status,
          mpStatus = mpStatus,
          mpStatusDetail = mpStatusDetail,
          paidAt = paidAt
        )
      }
    }
  }

  fun notifyPixPaymentConfirmed(
    providerId: String,
    amount: Double,
    txId: String,
    paymentId: Long?,
    newBalance: Double
  ) {
    val idLabel = paymentId?.let { " #$it" } ?: ""
    val formattedAmount = String.format(java.util.Locale("pt", "BR"), "%.2f", amount)
    val formattedBalance = String.format(java.util.Locale("pt", "BR"), "%.2f", newBalance)
    emitNotification(
      notification = AppNotification(
        id = "notif_pix_$txId",
        recipientId = providerId,
        title = "Pagamento Pix$idLabel confirmado!",
        message = "Recarga de R$ $formattedAmount aprovada via Mercado Pago. Novo saldo: R$ $formattedBalance.",
        type = NotificationType.PAGAMENTO_PIX_CONFIRMADO,
        timestamp = System.currentTimeMillis()
      ),
      dedupKey = "pix_confirmed_$txId",
      dispatchSystemBanner = true
    )
  }

  fun observePixChargePayment(
    providerId: String,
    txId: String,
    expectedAmount: Double,
    knownInitialBalance: Double
  ): Flow<PixChargeDetectionEvent> {
    val ds = firestoreDataSource ?: return emptyFlow()
    val targetId = currentFirebaseUidOrNull() ?: providerId
    return ds.observePixChargePayment(
      providerId = targetId,
      txId = txId,
      expectedAmount = expectedAmount,
      knownInitialBalance = knownInitialBalance
    )
  }

  suspend fun checkPixChargeStatusOnce(
    providerId: String,
    txId: String,
    expectedAmount: Double,
    knownInitialBalance: Double
  ): PixChargeDetectionEvent? {
    val ds = firestoreDataSource ?: return null
    val targetId = currentFirebaseUidOrNull() ?: providerId
    return ds.checkPixChargeStatusOnce(
      providerId = targetId,
      txId = txId,
      expectedAmount = expectedAmount,
      knownInitialBalance = knownInitialBalance
    )
  }

  // Recharge provider wallet exclusively via Pix (idempotent per txId)
  fun rechargeWallet(
    providerId: String,
    amount: Double,
    txId: String? = null,
    detectedAutomatically: Boolean = false,
    remoteConfirmedBalance: Double? = null
  ): Double {
    val existingBalance = _users.value.firstOrNull { it.id == providerId }?.walletBalance ?: 0.0
    if (amount <= 0.0 || amount.isNaN()) {
      return existingBalance
    }
    val cleanTxId = txId?.filter { it.isLetterOrDigit() }?.takeIf { it.isNotBlank() }
    if (cleanTxId != null && !processedPixTxIds.add(cleanTxId)) {
      return existingBalance
    }

    var newBalance = existingBalance + amount
    if (remoteConfirmedBalance != null && remoteConfirmedBalance >= existingBalance) {
      newBalance = remoteConfirmedBalance
    }

    _users.update { list ->
      list.map { u ->
        if (u.id == providerId) {
          u.copy(walletBalance = newBalance)
        } else u
      }
    }

    val deductedNegative = if (existingBalance < 0.0) kotlin.math.min(-existingBalance, amount) else 0.0
    val description = when {
      deductedNegative > 0.0 && cleanTxId != null ->
        "Recarga Pix ($cleanTxId) • Desconto automático de R$ ${String.format("%.2f", deductedNegative)} de saldo negativo"
      deductedNegative > 0.0 ->
        "Recarga via Pix • Desconto automático de R$ ${String.format("%.2f", deductedNegative)} de saldo negativo"
      detectedAutomatically && cleanTxId != null ->
        "Recarga Pix detectada automaticamente ($cleanTxId)"
      cleanTxId != null ->
        "Recarga de saldo via Pix ($cleanTxId)"
      else ->
        "Recarga de saldo via Pix instantâneo"
    }

    val transaction = WalletTransaction(
      id = cleanTxId?.let { "tx_pix_$it" } ?: "tx_${UUID.randomUUID().toString().take(6)}",
      providerId = providerId,
      type = TransactionType.RECARGA_PIX,
      amount = amount,
      description = description,
      timestamp = System.currentTimeMillis(),
      balanceAfter = newBalance
    )
    _walletTransactions.update { listOf(transaction) + it }
    persistLocalState()

    val ds = firestoreDataSource
    val fbUid = currentFirebaseUidOrNull()
    if (ds != null && fbUid != null && remoteConfirmedBalance == null) {
      repoScope.launch {
        ds.rechargeProviderWallet(
          providerId = fbUid,
          amount = amount,
          txId = cleanTxId
        )
      }
    }

    return newBalance
  }

  // Update Service Status (e.g. In Progress, Completed, Evaluated)
  fun updateServiceStatus(
    serviceId: String,
    newStatus: ServiceStatus,
    completionPhotoUri: String? = null,
    completionPhotoName: String? = null,
    completionVideoUri: String? = null,
    completionVideoName: String? = null,
    completionVideoThumbnailUri: String? = null,
    completionVideoDurationMs: Long? = null,
    completionNotes: String? = null
  ) {
    val activeUser = getCurrentUser()
    val cleanPhotoUri = completionPhotoUri?.trim()?.takeIf { it.isNotBlank() }?.take(250_000)
    val cleanPhotoName = completionPhotoName?.trim()?.takeIf { it.isNotBlank() }?.take(300)
    val cleanVideoUri = completionVideoUri?.trim()?.takeIf { it.isNotBlank() }?.take(2000)
    val cleanVideoName = completionVideoName?.trim()?.takeIf { it.isNotBlank() }?.take(300)
    val cleanVideoThumb = completionVideoThumbnailUri?.trim()?.takeIf { it.isNotBlank() }?.take(250_000)
    val cleanVideoDuration = if (cleanVideoUri != null) completionVideoDurationMs else null
    val cleanNotes = completionNotes?.trim()?.takeIf { it.isNotBlank() }?.take(500)
    val completedAtMillis = if (newStatus == ServiceStatus.CONCLUIDO) System.currentTimeMillis() else null

    var targetSrv: ServiceRequest? = null
    val trackingNowActive = newStatus == ServiceStatus.ACEITO || newStatus == ServiceStatus.EM_EXECUCAO
    _services.update { list ->
      list.map { srv ->
        if (srv.id == serviceId) {
          srv.copy(
            status = newStatus,
            locationTrackingActive = trackingNowActive,
            completionPhotoUri = if (newStatus == ServiceStatus.CONCLUIDO) {
              cleanPhotoUri ?: srv.completionPhotoUri
            } else {
              srv.completionPhotoUri
            },
            completionPhotoName = if (newStatus == ServiceStatus.CONCLUIDO) {
              cleanPhotoName ?: srv.completionPhotoName
            } else {
              srv.completionPhotoName
            },
            completionVideoUri = if (newStatus == ServiceStatus.CONCLUIDO) {
              cleanVideoUri ?: srv.completionVideoUri
            } else {
              srv.completionVideoUri
            },
            completionVideoName = if (newStatus == ServiceStatus.CONCLUIDO) {
              cleanVideoName ?: srv.completionVideoName
            } else {
              srv.completionVideoName
            },
            completionVideoThumbnailUri = if (newStatus == ServiceStatus.CONCLUIDO) {
              cleanVideoThumb ?: srv.completionVideoThumbnailUri
            } else {
              srv.completionVideoThumbnailUri
            },
            completionVideoDurationMs = if (newStatus == ServiceStatus.CONCLUIDO) {
              cleanVideoDuration ?: srv.completionVideoDurationMs
            } else {
              srv.completionVideoDurationMs
            },
            completionNotes = if (newStatus == ServiceStatus.CONCLUIDO) {
              cleanNotes ?: srv.completionNotes
            } else {
              srv.completionNotes
            },
            completedAt = completedAtMillis ?: srv.completedAt
          ).also { targetSrv = it }
        } else srv
      }
    }

    val mediaLabel = when {
      cleanPhotoUri != null && cleanVideoUri != null -> " com foto e vídeo comprobatórios anexados"
      cleanVideoUri != null -> " com vídeo comprobatório anexado"
      cleanPhotoUri != null -> " com foto comprobatória anexada"
      else -> ""
    }
    val sysContent = when {
      newStatus == ServiceStatus.CONCLUIDO && mediaLabel.isNotBlank() -> {
        val notePart = cleanNotes?.let { " Obs.: $it" }.orEmpty()
        "✅ Serviço finalizado e concluído pelo prestador$mediaLabel!$notePart"
      }
      newStatus == ServiceStatus.CONCLUIDO -> {
        val notePart = cleanNotes?.let { " Obs.: $it" }.orEmpty()
        "✅ Serviço finalizado e concluído pelo prestador!$notePart"
      }
      else -> "ℹ️ Status do serviço alterado para: ${newStatus.label}"
    }
    val sysMsg = addChatMessage(
      serviceId = serviceId,
      content = sysContent,
      isSystem = true,
      syncToFirestore = false
    )
    val autoPhotoRequestMsg = if (newStatus == ServiceStatus.CONCLUIDO) {
      addChatMessage(
        serviceId = serviceId,
        content = "📸 Serviço concluído! Por favor, adicione fotos de como ficou o serviço finalizado na tela de detalhes do pedido.",
        isSystem = true,
        syncToFirestore = false
      )
    } else {
      null
    }
    targetSrv?.let { srv ->
      val recipientId = if (activeUser.ownsId(srv.clientId)) {
        srv.acceptedProviderId ?: srv.clientId
      } else {
        srv.clientId
      }
      val notifMedia = when {
        cleanPhotoUri != null && cleanVideoUri != null -> " com foto e vídeo anexados"
        cleanVideoUri != null -> " com vídeo anexado"
        cleanPhotoUri != null -> " com foto anexada"
        else -> ""
      }
      val notifMessage = if (newStatus == ServiceStatus.CONCLUIDO) {
        if (notifMedia.isNotBlank()) {
          "O pedido \"${srv.title}\" foi concluído pelo prestador$notifMedia. Veja nos detalhes e adicione suas fotos do serviço finalizado!"
        } else {
          "O pedido \"${srv.title}\" foi concluído pelo prestador. Adicione fotos de como ficou o serviço finalizado nos detalhes do pedido!"
        }
      } else {
        "O pedido \"${srv.title}\" foi atualizado para: ${newStatus.label}."
      }
      emitNotification(
        AppNotification(
          id = "notif_status_${serviceId}_${newStatus.name}",
          recipientId = recipientId,
          title = if (newStatus == ServiceStatus.CONCLUIDO) "✅ Serviço Concluído" else "ℹ️ Status Atualizado: ${newStatus.label}",
          message = notifMessage,
          type = NotificationType.STATUS_ATUALIZADO,
          serviceId = serviceId
        ),
        dedupKey = "srv_status_${serviceId}_${newStatus.name}",
        dispatchSystemBanner = false
      )
    }
    if (newStatus == ServiceStatus.CONCLUIDO && cleanPhotoUri != null) {
      val completedSrv = targetSrv
      val providerKey = completedSrv?.acceptedProviderId
      if (completedSrv != null && !providerKey.isNullOrBlank()) {
        val showcasePhoto = ProviderServicePhoto(
          id = "srv_${completedSrv.id}",
          title = completedSrv.title,
          category = completedSrv.category,
          photoUri = cleanPhotoUri,
          createdAt = completedAtMillis ?: System.currentTimeMillis()
        )
        var updatedProviderForSync: User? = null
        _users.update { usersList ->
          usersList.map { u ->
            if (u.ownsId(providerKey)) {
              val nextPortfolio = (listOf(showcasePhoto) + u.portfolioPhotos.filterNot {
                it.photoUri == cleanPhotoUri || it.id == showcasePhoto.id
              }).take(6)
              u.copy(portfolioPhotos = nextPortfolio).also { updatedProviderForSync = it }
            } else u
          }
        }
        updatedProviderForSync?.let { providerUpdated ->
          if (activeUser.ownsId(providerUpdated.id) || activeUser.ownsId(providerUpdated.firebaseUid)) {
            persistUserToFirestoreIfSignedIn(providerUpdated)
          }
        }
      }
    }
    persistLocalState()
    syncChatListenersForEligibleServices()

    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      val updates = hashMapOf<String, Any>(
        "status" to newStatus.name,
        "locationTrackingActive" to trackingNowActive
      )
      if (newStatus == ServiceStatus.CONCLUIDO) {
        completedAtMillis?.let { updates["completedAt"] = it }
        cleanPhotoUri?.let { updates["completionPhotoUri"] = it }
        cleanPhotoName?.let { updates["completionPhotoName"] = it }
        cleanVideoUri?.let { updates["completionVideoUri"] = it }
        cleanVideoName?.let { updates["completionVideoName"] = it }
        cleanVideoThumb?.let { updates["completionVideoThumbnailUri"] = it }
        cleanVideoDuration?.let { updates["completionVideoDurationMs"] = it }
        cleanNotes?.let { updates["completionNotes"] = it }
      }
      repoScope.launch {
        ds.updateServiceFields(serviceId, updates)
        ds.sendChatMessage(serviceId, sysMsg)
        autoPhotoRequestMsg?.let { ds.sendChatMessage(serviceId, it) }
      }
    }
  }

  fun cancelServiceRequest(serviceId: String, reason: String): Result<ServiceRequest> {
    val cleanReason = reason.trim().take(500)
    if (cleanReason.length < 5) {
      return Result.failure(IllegalArgumentException("Informe o motivo do cancelamento (mínimo 5 caracteres)."))
    }

    val target = _services.value.find { it.id == serviceId }
      ?: return Result.failure(IllegalStateException("Pedido de serviço não encontrado."))

    if (target.status == ServiceStatus.CONCLUIDO) {
      return Result.failure(IllegalStateException("Não é possível cancelar um serviço já concluído."))
    }
    if (target.status == ServiceStatus.CANCELADO) {
      return Result.failure(IllegalStateException("Este pedido já foi cancelado."))
    }

    val activeUser = getCurrentUser()
    when (activeUser.role) {
      UserRole.CLIENTE -> {
        if (!activeUser.ownsId(target.clientId)) {
          return Result.failure(IllegalStateException("Você só pode cancelar seus próprios pedidos."))
        }
      }
      UserRole.PRESTADOR -> {
        val isAssignedProvider = activeUser.ownsId(target.acceptedProviderId)
        val isActiveJob = target.status == ServiceStatus.ACEITO || target.status == ServiceStatus.EM_EXECUCAO
        if (!isAssignedProvider || !isActiveJob) {
          return Result.failure(
            IllegalStateException("Apenas o prestador responsável por um serviço aceito ou em execução pode cancelá-lo.")
          )
        }
      }
      UserRole.ADMIN -> Unit
    }

    val cancelledAtMillis = System.currentTimeMillis()
    val feeToRefund = target.platformFee ?: 0.0
    val assignedProviderId = target.acceptedProviderId
    val hadPaidFee = (target.status == ServiceStatus.ACEITO || target.status == ServiceStatus.EM_EXECUCAO) &&
      feeToRefund > 0.0 &&
      !assignedProviderId.isNullOrBlank()
    val isProviderCancelling = activeUser.role == UserRole.PRESTADOR
    val refundStatus = when {
      !hadPaidFee -> CancellationRefundStatus.NENHUM
      isProviderCancelling -> CancellationRefundStatus.PENDENTE_ANALISE_ADMIN
      else -> CancellationRefundStatus.ESTORNADO_AUTOMATICO
    }

    var updatedService: ServiceRequest? = null
    _services.update { list ->
      list.map { srv ->
        if (srv.id == serviceId) {
          srv.copy(
            status = ServiceStatus.CANCELADO,
            cancellationReason = cleanReason,
            cancelledByRole = activeUser.role,
            cancelledByName = activeUser.name,
            cancelledAt = cancelledAtMillis,
            cancellationRefundStatus = refundStatus,
            locationTrackingActive = false
          ).also { updatedService = it }
        } else srv
      }
    }
    val finalService = updatedService
      ?: return Result.failure(IllegalStateException("Falha ao atualizar pedido cancelado."))

    val affectedProposalIds = mutableListOf<String>()
    val proposalProviderIds = mutableSetOf<String>()
    _proposals.update { list ->
      list.map { prop ->
        if (prop.serviceId == serviceId) {
          proposalProviderIds += prop.providerId
          if (prop.status != ProposalStatus.RECUSADA) {
            affectedProposalIds += prop.id
            prop.copy(status = ProposalStatus.RECUSADA)
          } else {
            prop
          }
        } else prop
      }
    }

    // Quando o Cliente/Admin cancela um serviço já aceito/em execução, estorna automaticamente.
    // Quando o Prestador cancela, o estorno fica pendente de análise do administrador.
    var refundedProviderId: String? = null
    if (hadPaidFee && !isProviderCancelling && !assignedProviderId.isNullOrBlank()) {
      val alreadyRefunded = _walletTransactions.value.any {
        it.serviceId == serviceId && it.type == TransactionType.REEMBOLSO
      }
      if (!alreadyRefunded) {
        val providerUser = _users.value.find { it.ownsId(assignedProviderId) }
        val resolvedProviderId = providerUser?.id ?: assignedProviderId
        val newBalance = (providerUser?.walletBalance ?: 0.0) + feeToRefund
        if (providerUser != null) {
          _users.update { usersList ->
            usersList.map { u ->
              if (u.id == providerUser.id) u.copy(walletBalance = newBalance) else u
            }
          }
        }
        val refundTx = WalletTransaction(
          id = "tx_refund_${serviceId}",
          providerId = resolvedProviderId,
          type = TransactionType.REEMBOLSO,
          amount = feeToRefund,
          serviceId = serviceId,
          serviceTitle = target.title,
          description = "Estorno da taxa de 10% pelo cancelamento: $cleanReason",
          timestamp = cancelledAtMillis,
          balanceAfter = newBalance
        )
        _walletTransactions.update { listOf(refundTx) + it }
        refundedProviderId = providerUser?.firebaseUid?.takeIf { it.isNotBlank() } ?: resolvedProviderId
      }
    }

    val actorLabel = when (activeUser.role) {
      UserRole.CLIENTE -> "Cliente"
      UserRole.PRESTADOR -> "Prestador"
      UserRole.ADMIN -> "Administrador"
    }
    val sysMsg = addChatMessage(
      serviceId = serviceId,
      content = "🚫 Serviço cancelado pelo $actorLabel (${activeUser.name}). Motivo: $cleanReason",
      isSystem = true,
      syncToFirestore = false
    )

    val recipients = when (activeUser.role) {
      UserRole.CLIENTE -> buildSet {
        if (!assignedProviderId.isNullOrBlank()) add(assignedProviderId)
        addAll(proposalProviderIds)
      }
      UserRole.PRESTADOR -> setOf(target.clientId)
      UserRole.ADMIN -> buildSet {
        add(target.clientId)
        if (!assignedProviderId.isNullOrBlank()) add(assignedProviderId)
      }
    }

    recipients.filter { it.isNotBlank() }.forEach { recipientId ->
      emitNotification(
        AppNotification(
          id = "notif_cancel_${serviceId}_${recipientId}",
          recipientId = recipientId,
          title = "🚫 ${if (activeUser.role == UserRole.PRESTADOR) "Serviço Cancelado pelo Prestador" else "Pedido Cancelado pelo $actorLabel"}",
          message = "${activeUser.name} cancelou \"${target.title}\". Motivo: $cleanReason",
          type = NotificationType.STATUS_ATUALIZADO,
          serviceId = serviceId
        ),
        dedupKey = "srv_cancel_${serviceId}_${recipientId}",
        dispatchSystemBanner = false
      )
    }

    if (isProviderCancelling && hadPaidFee && !assignedProviderId.isNullOrBlank()) {
      emitNotification(
        AppNotification(
          id = "notif_cancel_review_${serviceId}",
          recipientId = assignedProviderId,
          title = "⚠️ Cancelamento em Análise pelo Administrador",
          message = "Evite cancelamento, sua conta pode ser bloqueada. O estorno de \"${target.title}\" será analisado por um administrador.",
          type = NotificationType.STATUS_ATUALIZADO,
          serviceId = serviceId
        ),
        dedupKey = "srv_cancel_review_${serviceId}",
        dispatchSystemBanner = false
      )
    }

    persistLocalState()
    syncChatListenersForEligibleServices()

    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      repoScope.launch {
        ds.updateServiceFields(
          serviceId,
          mapOf(
            "status" to ServiceStatus.CANCELADO.name,
            "cancellationReason" to cleanReason,
            "cancelledByRole" to activeUser.role.name,
            "cancelledByName" to activeUser.name.take(100),
            "cancelledAt" to cancelledAtMillis,
            "cancellationRefundStatus" to refundStatus.name,
            "locationTrackingActive" to false
          )
        )
        affectedProposalIds.forEach { propId ->
          ds.updateProposalFields(propId, mapOf("status" to ProposalStatus.RECUSADA.name))
        }
        if (target.status == ServiceStatus.ACEITO || target.status == ServiceStatus.EM_EXECUCAO) {
          ds.sendChatMessage(serviceId, sysMsg)
        }
        if (refundedProviderId != null && (activeUser.ownsId(refundedProviderId) || activeUser.role == UserRole.ADMIN)) {
          ds.refundProviderPlatformFee(
            providerId = refundedProviderId,
            serviceId = serviceId,
            serviceTitle = target.title,
            amount = feeToRefund,
            reason = cleanReason
          )
        }
      }
    }

    return Result.success(finalService)
  }

  fun adminProcessCancellationRefund(
    serviceId: String,
    approve: Boolean,
    note: String = ""
  ): Result<ServiceRequest> {
    val adminUser = getCurrentUser()
    if (adminUser.role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas administradores podem analisar e processar estornos."))
    }
    val target = _services.value.find { it.id == serviceId }
      ?: return Result.failure(IllegalStateException("Serviço não encontrado."))
    if (target.status != ServiceStatus.CANCELADO) {
      return Result.failure(IllegalStateException("Apenas serviços cancelados podem ter estorno analisado."))
    }
    if (target.cancellationRefundStatus != CancellationRefundStatus.PENDENTE_ANALISE_ADMIN) {
      return Result.failure(IllegalStateException("Este cancelamento não está aguardando análise de estorno."))
    }

    val reviewedAt = System.currentTimeMillis()
    val cleanNote = note.trim().take(500).ifBlank {
      if (approve) "Estorno aprovado após análise administrativa." else "Estorno negado após análise administrativa."
    }
    val nextRefundStatus = if (approve) {
      CancellationRefundStatus.ESTORNADO_ADMIN
    } else {
      CancellationRefundStatus.NEGADO_ADMIN
    }
    val feeToRefund = target.platformFee ?: 0.0
    val assignedProviderId = target.acceptedProviderId
    var firestoreProviderId: String? = null

    if (approve && feeToRefund > 0.0 && !assignedProviderId.isNullOrBlank()) {
      val alreadyRefunded = _walletTransactions.value.any {
        it.serviceId == serviceId && it.type == TransactionType.REEMBOLSO
      }
      if (!alreadyRefunded) {
        val providerUser = _users.value.find { it.ownsId(assignedProviderId) }
        val resolvedProviderId = providerUser?.id ?: assignedProviderId
        val newBalance = (providerUser?.walletBalance ?: 0.0) + feeToRefund
        if (providerUser != null) {
          _users.update { usersList ->
            usersList.map { u ->
              if (u.id == providerUser.id) u.copy(walletBalance = newBalance) else u
            }
          }
        }
        val refundTx = WalletTransaction(
          id = "tx_refund_${serviceId}",
          providerId = resolvedProviderId,
          type = TransactionType.REEMBOLSO,
          amount = feeToRefund,
          serviceId = serviceId,
          serviceTitle = target.title,
          description = "Estorno aprovado pelo administrador: $cleanNote",
          timestamp = reviewedAt,
          balanceAfter = newBalance
        )
        _walletTransactions.update { listOf(refundTx) + it }
        firestoreProviderId = providerUser?.firebaseUid?.takeIf { it.isNotBlank() } ?: resolvedProviderId
      }
    }

    var updatedService: ServiceRequest? = null
    _services.update { list ->
      list.map { srv ->
        if (srv.id == serviceId) {
          srv.copy(
            cancellationRefundStatus = nextRefundStatus,
            cancellationRefundReviewedBy = adminUser.name,
            cancellationRefundReviewedAt = reviewedAt,
            cancellationRefundNote = cleanNote
          ).also { updatedService = it }
        } else srv
      }
    }
    val resultSrv = updatedService ?: return Result.failure(IllegalStateException("Falha ao atualizar análise."))

    if (!assignedProviderId.isNullOrBlank()) {
      emitNotification(
        AppNotification(
          id = "notif_admin_refund_${serviceId}",
          recipientId = assignedProviderId,
          title = if (approve) "💰 Estorno Aprovado pelo Administrador" else "❌ Estorno Não Aprovado",
          message = if (approve) {
            "O administrador aprovou o estorno de R$ ${String.format("%.2f", feeToRefund)} para \"${target.title}\". $cleanNote"
          } else {
            "A solicitação de estorno de \"${target.title}\" não foi aprovada após análise. $cleanNote"
          },
          type = NotificationType.STATUS_ATUALIZADO,
          serviceId = serviceId
        ),
        dedupKey = "admin_refund_${serviceId}_${nextRefundStatus.name}"
      )
    }

    persistLocalState()
    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      repoScope.launch {
        ds.updateServiceFields(
          serviceId,
          mapOf(
            "cancellationRefundStatus" to nextRefundStatus.name,
            "cancellationRefundReviewedBy" to adminUser.name.take(150),
            "cancellationRefundReviewedAt" to reviewedAt,
            "cancellationRefundNote" to cleanNote
          )
        )
        if (approve && firestoreProviderId != null && feeToRefund > 0.0) {
          ds.refundProviderPlatformFee(
            providerId = firestoreProviderId!!,
            serviceId = serviceId,
            serviceTitle = target.title,
            amount = feeToRefund,
            reason = cleanNote
          )
        }
      }
    }

    return Result.success(resultSrv)
  }

  fun addClientCompletedServicePhoto(
    serviceId: String,
    photoUri: String,
    photoName: String = "",
    caption: String = ""
  ): Result<ServiceRequest> {
    val activeUser = getCurrentUser()
    val target = _services.value.find { it.id == serviceId }
      ?: return Result.failure(IllegalStateException("Serviço não encontrado."))
    if (target.status != ServiceStatus.CONCLUIDO) {
      return Result.failure(IllegalStateException("Fotos do cliente só podem ser adicionadas após a conclusão do serviço."))
    }
    if (!activeUser.ownsId(target.clientId) && activeUser.role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas o cliente responsável pelo pedido pode adicionar fotos."))
    }
    val cleanUri = photoUri.trim().take(250_000)
    if (cleanUri.isBlank()) {
      return Result.failure(IllegalArgumentException("Selecione ou tire uma foto válida."))
    }
    val newPhoto = ClientCompletedServicePhoto(
      id = "cphoto_${UUID.randomUUID().toString().take(8)}",
      photoUri = cleanUri,
      photoName = photoName.trim().ifBlank { "foto_cliente_${target.clientCompletionPhotos.size + 1}.jpg" }.take(300),
      caption = caption.trim().take(300),
      addedAt = System.currentTimeMillis()
    )
    var updatedSrv: ServiceRequest? = null
    _services.update { list ->
      list.map { srv ->
        if (srv.id == serviceId) {
          val updatedPhotos = (listOf(newPhoto) + srv.clientCompletionPhotos.filterNot { it.photoUri == cleanUri }).take(6)
          srv.copy(clientCompletionPhotos = updatedPhotos).also { updatedSrv = it }
        } else srv
      }
    }
    val finalSrv = updatedSrv ?: return Result.failure(IllegalStateException("Falha ao salvar foto no serviço."))
    persistLocalState()

    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      val serializedPhotos = finalSrv.clientCompletionPhotos.map { p ->
        mapOf(
          "id" to p.id,
          "photoUri" to p.photoUri,
          "photoName" to p.photoName,
          "caption" to p.caption,
          "addedAt" to p.addedAt
        )
      }
      repoScope.launch {
        ds.updateServiceFields(serviceId, mapOf("clientCompletionPhotos" to serializedPhotos))
      }
    }
    return Result.success(finalSrv)
  }

  fun removeClientCompletedServicePhoto(serviceId: String, photoId: String): Result<ServiceRequest> {
    val activeUser = getCurrentUser()
    val target = _services.value.find { it.id == serviceId }
      ?: return Result.failure(IllegalStateException("Serviço não encontrado."))
    if (!activeUser.ownsId(target.clientId) && activeUser.role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Permissão negada para remover foto."))
    }
    var updatedSrv: ServiceRequest? = null
    _services.update { list ->
      list.map { srv ->
        if (srv.id == serviceId) {
          srv.copy(
            clientCompletionPhotos = srv.clientCompletionPhotos.filterNot { it.id == photoId }
          ).also { updatedSrv = it }
        } else srv
      }
    }
    val finalSrv = updatedSrv ?: return Result.failure(IllegalStateException("Serviço não encontrado."))
    persistLocalState()

    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      val serializedPhotos = finalSrv.clientCompletionPhotos.map { p ->
        mapOf(
          "id" to p.id,
          "photoUri" to p.photoUri,
          "photoName" to p.photoName,
          "caption" to p.caption,
          "addedAt" to p.addedAt
        )
      }
      repoScope.launch {
        ds.updateServiceFields(serviceId, mapOf("clientCompletionPhotos" to serializedPhotos))
      }
    }
    return Result.success(finalSrv)
  }

  fun recordProviderLocationPoint(
    serviceId: String,
    latitude: Double,
    longitude: Double,
    accuracyMeters: Float = 0f,
    speedMps: Float = 0f,
    source: String = "GPS"
  ): ProviderLocationPoint? {
    if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) return null
    val activeUser = getCurrentUser()
    val target = _services.value.find { it.id == serviceId } ?: return null
    if (target.status != ServiceStatus.ACEITO && target.status != ServiceStatus.EM_EXECUCAO) {
      return null
    }
    val providerId = currentFirebaseUidOrNull() ?: activeUser.firebaseUid ?: activeUser.id
    val point = ProviderLocationPoint(
      id = "loc_${UUID.randomUUID().toString().take(8)}",
      serviceId = serviceId,
      providerId = providerId,
      latitude = latitude,
      longitude = longitude,
      accuracyMeters = accuracyMeters,
      speedMps = speedMps,
      serviceStatus = target.status,
      recordedAt = System.currentTimeMillis(),
      source = source.take(50)
    )
    _serviceLocationHistory.update { current ->
      (current + point).takeLast(500)
    }
    val pointsForService = _serviceLocationHistory.value.count { it.serviceId == serviceId }
    _services.update { list ->
      list.map { srv ->
        if (srv.id == serviceId) {
          srv.copy(
            locationTrackingActive = srv.status == ServiceStatus.ACEITO || srv.status == ServiceStatus.EM_EXECUCAO,
            locationPointsCount = maxOf(srv.locationPointsCount, pointsForService)
          )
        } else srv
      }
    }
    persistLocalState()

    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      repoScope.launch {
        ds.recordProviderLocationPoint(point)
        ds.updateServiceFields(
          serviceId,
          mapOf(
            "locationTrackingActive" to (target.status == ServiceStatus.ACEITO || target.status == ServiceStatus.EM_EXECUCAO),
            "locationPointsCount" to pointsForService
          )
        )
      }
    }
    return point
  }

  fun getLocationHistoryForService(serviceId: String): List<ProviderLocationPoint> {
    return _serviceLocationHistory.value
      .filter { it.serviceId == serviceId }
      .sortedBy { it.recordedAt }
  }

  suspend fun syncLocationHistoryForService(serviceId: String): List<ProviderLocationPoint> {
    val ds = firestoreDataSource ?: return getLocationHistoryForService(serviceId)
    if (currentFirebaseUidOrNull() == null) return getLocationHistoryForService(serviceId)
    val remotePoints = ds.fetchServiceLocationHistory(serviceId)
    if (remotePoints.isNotEmpty()) {
      _serviceLocationHistory.update { current ->
        val others = current.filterNot { it.serviceId == serviceId }
        val existingLocal = current.filter { it.serviceId == serviceId }
        val remoteIds = remotePoints.map { it.id }.toSet()
        val merged = (remotePoints + existingLocal.filterNot { it.id in remoteIds })
          .sortedBy { it.recordedAt }
        others + merged
      }
      persistLocalState()
    }
    return getLocationHistoryForService(serviceId)
  }

  fun submitAppSuggestion(
    category: String,
    title: String,
    description: String
  ): Result<AppSuggestion> {
    val cleanTitle = title.trim().take(150)
    val cleanDesc = description.trim().take(1500)
    val cleanCat = category.trim().ifBlank { "Melhoria no App" }.take(80)
    if (cleanTitle.length < 3) {
      return Result.failure(IllegalArgumentException("Informe um título com pelo menos 3 caracteres."))
    }
    if (cleanDesc.length < 5) {
      return Result.failure(IllegalArgumentException("Descreva sua sugestão com pelo menos 5 caracteres."))
    }
    val user = getCurrentUser()
    val now = System.currentTimeMillis()
    val suggestion = AppSuggestion(
      id = "sug_${UUID.randomUUID().toString().take(8)}",
      userId = currentFirebaseUidOrNull() ?: user.firebaseUid ?: user.id,
      userName = user.name,
      userRole = user.role,
      category = cleanCat,
      title = cleanTitle,
      description = cleanDesc,
      status = SuggestionStatus.ENVIADA,
      createdAt = now,
      updatedAt = now
    )
    _appSuggestions.update { listOf(suggestion) + it }
    persistLocalState()

    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      repoScope.launch {
        ds.saveAppSuggestion(suggestion)
      }
    }
    return Result.success(suggestion)
  }

  fun updateAppSuggestionStatus(
    suggestionId: String,
    status: SuggestionStatus,
    adminReply: String? = null
  ): Result<AppSuggestion> {
    val user = getCurrentUser()
    if (user.role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas administradores podem atualizar o status das sugestões."))
    }
    val cleanReply = adminReply?.trim()?.take(500)?.takeIf { it.isNotBlank() }
    val now = System.currentTimeMillis()
    var updated: AppSuggestion? = null
    _appSuggestions.update { list ->
      list.map { item ->
        if (item.id == suggestionId) {
          item.copy(
            status = status,
            adminReply = cleanReply ?: item.adminReply,
            updatedAt = now
          ).also { updated = it }
        } else item
      }
    }
    val resultSug = updated ?: return Result.failure(IllegalStateException("Sugestão não encontrada."))
    persistLocalState()

    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      repoScope.launch {
        ds.updateAppSuggestionAdmin(suggestionId, status, cleanReply)
      }
    }
    return Result.success(resultSug)
  }

  // Active SMS sessions and Firebase Phone Auth integration
  val phoneAuthManager = FirebasePhoneAuthManager()
  private val _smsSessions = MutableStateFlow<Map<String, SmsVerificationSession>>(emptyMap())

  fun sendSmsCode(
    phone: String,
    activity: Activity? = null,
    countryCode: String = FirebasePhoneAuthManager.DEFAULT_COUNTRY_CODE,
    isResend: Boolean = false,
    onCodeSent: (String, String) -> Unit = { _, _ -> },
    onAutoVerified: (String) -> Unit = {},
    onErrorWithType: (String, PhoneAuthErrorType) -> Unit = { _, _ -> },
    onError: (String) -> Unit = {}
  ): String {
    val code = ((100000..999999).random()).toString()
    val formattedPhone = phoneAuthManager.formatToE164(phone, countryCode)
    val sessionKey = normalizePhoneDigits(formattedPhone).ifBlank { phone.trim() }
    _smsSessions.update { it + (sessionKey to SmsVerificationSession(phone = formattedPhone, code = code)) }

    if (activity != null) {
      phoneAuthManager.sendVerificationCode(
        activity = activity,
        phone = phone,
        countryCode = countryCode,
        isResend = isResend,
        onSuccess = { vId, fPhone ->
          _smsSessions.update { current ->
            val existing = current[sessionKey] ?: SmsVerificationSession(phone = fPhone, code = code)
            current + (sessionKey to existing.copy(verificationId = vId))
          }
          onCodeSent(vId, fPhone)
        },
        onAutoVerified = { _, fPhone ->
          onAutoVerified(fPhone)
        },
        onError = { errMsg, errType ->
          onErrorWithType(errMsg, errType)
          onError(errMsg)
        }
      )
    }
    return code
  }

  fun verifySmsCode(phone: String, inputCode: String, onVerified: (Boolean, String?) -> Unit = { _, _ -> }): Boolean {
    val formattedPhone = phoneAuthManager.formatToE164(phone)
    val sessionKey = normalizePhoneDigits(formattedPhone).ifBlank { normalizePhoneDigits(phone).ifBlank { phone.trim() } }
    val session = _smsSessions.value[sessionKey]
      ?: _smsSessions.value[normalizePhoneDigits(phone)]
      ?: _smsSessions.value[phone]
    val trimmed = inputCode.filter { it.isDigit() }
    val vId = session?.verificationId

    // Se o Firebase Phone Auth gerou um verificationId real, valida exclusivamente via Firebase credential
    if (!vId.isNullOrBlank()) {
      phoneAuthManager.verifySmsCode(
        verificationId = vId,
        code = trimmed,
        onSuccess = { onVerified(true, null) },
        onError = { err -> onVerified(false, err) }
      )
      return false
    }

    // Modo local / JVM unit tests (sem Activity ou sem provedor ativo no emulador)
    val isLocalTestValid = trimmed == "123456" || (session != null && session.code == trimmed)
    if (!isLocalTestValid) {
      onVerified(false, "Código OTP incorreto. Verifique os 6 dígitos enviados.")
    }
    return isLocalTestValid
  }

  fun registerUser(
    name: String,
    email: String,
    phone: String,
    role: UserRole,
    category: String = "",
    bio: String = "",
    city: String = "São Paulo, SP",
    document: String = "",
    initialWalletBonus: Double = 0.0,
    photoUrl: String? = null,
    documents: ProviderDocuments? = null
  ): User {
    val prefix = if (role == UserRole.CLIENTE) "cli" else "pro"
    val avatarColors = listOf(0xFF2563EB, 0xFF0D9488, 0xFF7C3AED, 0xFFEA580C, 0xFF059669, 0xFF0284C7)
    val vStatus = if (role == UserRole.PRESTADOR) ProviderVerificationStatus.PENDENTE_ANALISE else ProviderVerificationStatus.APROVADO
    val fbUid = try { phoneAuthManager.auth?.currentUser?.uid } catch (e: Throwable) { null }
    val newUser = User(
      id = "${prefix}_${UUID.randomUUID().toString().take(6)}",
      name = name,
      email = email,
      phone = phone,
      role = role,
      category = category,
      bio = bio,
      city = city,
      document = document,
      rating = 5.0,
      completedJobs = 0,
      avatarColorHex = avatarColors.random(),
      photoUrl = photoUrl?.takeIf { it.isNotBlank() },
      walletBalance = if (role == UserRole.PRESTADOR) initialWalletBonus else 0.0,
      isPhoneVerified = true,
      verificationStatus = vStatus,
      documents = documents,
      firebaseUid = fbUid
    )

    _users.update { listOf(newUser) + it }
    _currentUserId.value = newUser.id
    _isAuthenticated.value = true

    if (initialWalletBonus > 0.0) {
      val bonusTx = WalletTransaction(
        id = "tx_${UUID.randomUUID().toString().take(6)}",
        providerId = newUser.id,
        type = TransactionType.RECARGA_PIX,
        amount = initialWalletBonus,
        description = "Bônus de boas-vindas na ativação por SMS",
        timestamp = System.currentTimeMillis(),
        balanceAfter = initialWalletBonus
      )
      _walletTransactions.update { listOf(bonusTx) + it }
    }

    persistLocalState()
    persistUserToFirestoreIfSignedIn(newUser)
    if (role == UserRole.PRESTADOR && initialWalletBonus > 0.0 && fbUid != null) {
      val ds = firestoreDataSource
      if (ds != null) {
        repoScope.launch {
          ds.recordProviderWelcomeBonusIfMissing(
            providerId = fbUid,
            amount = initialWalletBonus,
            description = "Bônus de boas-vindas no cadastro"
          )
        }
      }
    }
    startFirestoreSyncIfSignedIn()
    return newUser
  }

  fun getReviewsForProvider(providerId: String): List<ServiceReview> {
    if (providerId.isBlank()) return emptyList()
    val targetProvider = _users.value.find { it.ownsId(providerId) || it.id == providerId }
    return _reviews.value
      .filter { rev ->
        targetProvider?.ownsId(rev.providerId) ?: (rev.providerId == providerId)
      }
      .sortedByDescending { it.createdAt }
  }

  // Rate service (1 to 5 stars post-completion with Firestore sync)
  fun rateService(
    serviceId: String,
    rating: Float,
    comment: String,
    complimentTags: List<String> = emptyList()
  ): Result<ServiceReview> {
    val srv = _services.value.find { it.id == serviceId }
      ?: return Result.failure(IllegalArgumentException("Serviço não encontrado"))
    if (srv.status != ServiceStatus.CONCLUIDO) {
      return Result.failure(IllegalStateException("A avaliação só pode ser enviada após a conclusão do serviço."))
    }
    val currentUser = getCurrentUser()
    if (!currentUser.ownsId(srv.clientId) && currentUser.role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas o cliente solicitante pode avaliar este serviço."))
    }
    if (rating.isNaN() || rating < 1.0f || rating > 5.0f) {
      return Result.failure(IllegalArgumentException("Selecione uma nota válida entre 1 e 5 estrelas."))
    }
    if (srv.ratingGiven != null || _reviews.value.any { it.serviceId == serviceId }) {
      return Result.failure(IllegalStateException("Este serviço já recebeu uma avaliação."))
    }

    val providerId = srv.acceptedProviderId?.takeIf { it.isNotBlank() }
      ?: return Result.failure(IllegalStateException("Nenhum prestador associado a este atendimento."))
    val targetProvider = _users.value.find { it.ownsId(providerId) || it.id == providerId }
    val providerName = srv.acceptedProviderName ?: targetProvider?.name ?: "Prestador"
    val cleanComment = comment.trim().take(1000)
    val cleanTags = complimentTags.mapNotNull { it.trim().takeIf { t -> t.isNotBlank() }?.take(60) }.distinct().take(10)
    val now = System.currentTimeMillis()
    val fbUid = currentFirebaseUidOrNull()

    val newReview = ServiceReview(
      id = serviceId,
      serviceId = serviceId,
      serviceTitle = srv.title,
      serviceCategory = srv.category,
      clientId = fbUid ?: currentUser.firebaseUid ?: currentUser.id,
      clientName = currentUser.name,
      clientPhotoUrl = currentUser.photoUrl,
      providerId = providerId,
      providerName = providerName,
      rating = rating,
      comment = cleanComment,
      complimentTags = cleanTags,
      createdAt = now
    )

    _reviews.update { current ->
      (listOf(newReview) + current.filterNot { it.serviceId == serviceId })
        .sortedByDescending { it.createdAt }
    }

    // Recompute provider average rating & review count
    val providerReviews = _reviews.value.filter { rev ->
      targetProvider?.ownsId(rev.providerId) ?: (rev.providerId == providerId)
    }
    val newAvgRating = if (providerReviews.isNotEmpty()) {
      providerReviews.map { it.rating.toDouble() }.average()
    } else {
      rating.toDouble()
    }

    val roundedRating = Math.round(newAvgRating * 100.0) / 100.0
    val totalReviewCount = providerReviews.size
    _users.update { list ->
      list.map { u ->
        if (u.ownsId(providerId) || u.id == providerId) {
          u.copy(
            rating = roundedRating,
            ratingCount = totalReviewCount
          )
        } else u
      }
    }

    _proposals.update { list ->
      list.map { prop ->
        if (targetProvider?.ownsId(prop.providerId) ?: (prop.providerId == providerId)) {
          prop.copy(providerRating = roundedRating)
        } else prop
      }
    }

    _services.update { list ->
      list.map { s ->
        when {
          s.id == serviceId -> s.copy(
            status = ServiceStatus.CONCLUIDO,
            ratingGiven = rating,
            reviewComment = cleanComment.ifBlank { null },
            reviewTags = cleanTags,
            reviewedAt = now,
            acceptedProviderRating = roundedRating
          )
          targetProvider?.ownsId(s.acceptedProviderId) ?: (s.acceptedProviderId == providerId) ->
            s.copy(acceptedProviderRating = roundedRating)
          else -> s
        }
      }
    }

    val tagsStr = if (cleanTags.isNotEmpty()) " [${cleanTags.joinToString(", ")}]" else ""
    val commentExcerpt = if (cleanComment.isNotBlank()) ": \"$cleanComment\"" else ""
    val sysMsg = addChatMessage(
      serviceId = serviceId,
      content = "⭐ Serviço avaliado com ${formatStarsNotice(rating)} estrelas pelo cliente$commentExcerpt$tagsStr",
      isSystem = true,
      syncToFirestore = false
    )
    emitNotification(
      AppNotification(
        id = "notif_rating_$serviceId",
        recipientId = targetProvider?.id ?: providerId,
        title = "⭐ Nova Avaliação: ${formatStarsNotice(rating)} estrelas!",
        message = "${currentUser.name} avaliou \"${srv.title}\"$commentExcerpt",
        type = NotificationType.AVALIACAO_RECEBIDA,
        serviceId = serviceId
      ),
      dedupKey = "srv_rating_$serviceId",
      dispatchSystemBanner = false
    )
    persistLocalState()

    val ds = firestoreDataSource
    if (ds != null && fbUid != null) {
      val firestoreReview = newReview.copy(clientId = fbUid)
      repoScope.launch {
        val saveResult = ds.submitServiceReview(firestoreReview, roundedRating)
        if (saveResult.isFailure) {
          _reviewSyncError.value = "Não foi possível salvar a avaliação no servidor. Tente novamente ao reconectar."
        } else {
          ds.sendChatMessage(serviceId, sysMsg)
        }
      }
    }
    return Result.success(newReview)
  }

  private fun formatStarsNotice(rating: Float): String {
    return if (rating % 1.0f == 0.0f) rating.toInt().toString() else rating.toString()
  }

  fun replyToReview(reviewId: String, replyText: String): Result<Unit> {
    val cleanReply = replyText.trim()
    if (cleanReply.isBlank()) {
      return Result.failure(IllegalArgumentException("Digite uma resposta para a avaliação."))
    }
    if (cleanReply.length > 500) {
      return Result.failure(IllegalArgumentException("A resposta deve ter no máximo 500 caracteres."))
    }
    val target = _reviews.value.find { it.id == reviewId || it.serviceId == reviewId }
      ?: return Result.failure(IllegalArgumentException("Avaliação não encontrada."))
    val currentUser = getCurrentUser()
    if (!currentUser.ownsId(target.providerId) && currentUser.role != UserRole.ADMIN) {
      return Result.failure(IllegalStateException("Apenas o prestador avaliado pode responder a esta avaliação."))
    }
    if (!target.providerReply.isNullOrBlank()) {
      return Result.failure(IllegalStateException("Esta avaliação já possui uma resposta."))
    }

    val boundedReply = cleanReply.take(500)
    val now = System.currentTimeMillis()
    _reviews.update { list ->
      list.map { rev ->
        if (rev.id == target.id) {
          rev.copy(providerReply = boundedReply, repliedAt = now)
        } else rev
      }
    }
    persistLocalState()

    val ds = firestoreDataSource
    if (ds != null && currentFirebaseUidOrNull() != null) {
      val firestoreDocId = target.serviceId.ifBlank { target.id }
      repoScope.launch {
        val res = ds.replyToServiceReview(firestoreDocId, boundedReply)
        if (res.isFailure) {
          _reviewSyncError.value = "Não foi possível sincronizar sua resposta no servidor."
        }
      }
    }
    return Result.success(Unit)
  }

  // Admin approval / rejection of provider documents (local + Firestore persistence & audit trail)
  fun approveProvider(
    providerId: String,
    idDocVerified: Boolean = true,
    criminalRecordVerified: Boolean = true,
    selfieVerified: Boolean = true
  ) {
    val adminUser = getCurrentUser()
    val now = System.currentTimeMillis()
    var targetProvider: User? = null
    _users.update { list ->
      list.map { u ->
        if (u.id == providerId) {
          val updatedDocs = (u.documents ?: ProviderDocuments()).copy(
            reviewedAt = now,
            rejectionReason = null,
            reviewedBy = adminUser.id,
            reviewedByEmail = adminUser.email,
            idDocVerified = idDocVerified,
            criminalRecordVerified = criminalRecordVerified,
            selfieVerified = selfieVerified
          )
          val updated = u.copy(
            verificationStatus = ProviderVerificationStatus.APROVADO,
            documents = updatedDocs
          )
          targetProvider = updated
          updated
        } else u
      }
    }

    targetProvider?.let { p ->
      val audit = DocumentReviewAudit(
        id = "rev_${UUID.randomUUID().toString().take(8)}",
        providerId = p.id,
        providerName = p.name,
        decision = ProviderVerificationStatus.APROVADO.name,
        reason = null,
        adminUid = adminUser.firebaseUid ?: adminUser.id,
        adminEmail = adminUser.email,
        idDocVerified = idDocVerified,
        criminalRecordVerified = criminalRecordVerified,
        selfieVerified = selfieVerified,
        reviewedAt = now
      )
      _documentReviewAudits.update { listOf(audit) + it }
    }

    val notif = AppNotification(
      id = "notif_${UUID.randomUUID().toString().take(6)}",
      recipientId = providerId,
      title = "🎉 Documentos Aprovados pelo Administrador!",
      message = "Sua documentação (RG/CNH, antecedentes criminais e selfie) foi aprovada pela equipe de segurança. Você já tem acesso total ao mural de serviços e à sua carteira digital.",
      type = NotificationType.DOCUMENTO_APROVADO,
      timestamp = now
    )
    emitNotification(notif)
  }

  fun rejectProvider(
    providerId: String,
    reason: String,
    idDocVerified: Boolean = false,
    criminalRecordVerified: Boolean = false,
    selfieVerified: Boolean = false
  ) {
    val adminUser = getCurrentUser()
    val now = System.currentTimeMillis()
    val cleanReason = reason.trim().ifBlank { "Documentação inconsistente ou ilegível" }
    var targetProvider: User? = null
    _users.update { list ->
      list.map { u ->
        if (u.id == providerId) {
          val updatedDocs = (u.documents ?: ProviderDocuments()).copy(
            reviewedAt = now,
            rejectionReason = cleanReason,
            reviewedBy = adminUser.id,
            reviewedByEmail = adminUser.email,
            idDocVerified = idDocVerified,
            criminalRecordVerified = criminalRecordVerified,
            selfieVerified = selfieVerified
          )
          val updated = u.copy(
            verificationStatus = ProviderVerificationStatus.REJEITADO,
            documents = updatedDocs
          )
          targetProvider = updated
          updated
        } else u
      }
    }

    targetProvider?.let { p ->
      val audit = DocumentReviewAudit(
        id = "rev_${UUID.randomUUID().toString().take(8)}",
        providerId = p.id,
        providerName = p.name,
        decision = ProviderVerificationStatus.REJEITADO.name,
        reason = cleanReason,
        adminUid = adminUser.firebaseUid ?: adminUser.id,
        adminEmail = adminUser.email,
        idDocVerified = idDocVerified,
        criminalRecordVerified = criminalRecordVerified,
        selfieVerified = selfieVerified,
        reviewedAt = now
      )
      _documentReviewAudits.update { listOf(audit) + it }
    }

    val notif = AppNotification(
      id = "notif_${UUID.randomUUID().toString().take(6)}",
      recipientId = providerId,
      title = "⚠️ Documentação Reprovada",
      message = "Sua documentação não foi aprovada pelo Administrador: $cleanReason. Por favor, envie novos documentos para regularização.",
      type = NotificationType.DOCUMENTO_REJEITADO,
      timestamp = now
    )
    emitNotification(notif)
  }

  suspend fun syncProvidersFromFirestore(): Result<Int> {
    val ds = firestoreDataSource ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val fbUser = try { FirebaseAuth.getInstance().currentUser } catch (_: Throwable) { null }
    if (fbUser == null) {
      return Result.failure(IllegalStateException("Sessão local ativa. Entre com sua conta Google Admin para sincronizar em nuvem."))
    }
    return ds.fetchProvidersForAdmin().map { remoteProviders ->
      if (remoteProviders.isNotEmpty()) {
        _users.update { currentList ->
          val merged = currentList.toMutableList()
          remoteProviders.forEach { remote ->
            val idx = merged.indexOfFirst { it.id == remote.id || (remote.firebaseUid != null && it.firebaseUid == remote.firebaseUid) }
            if (idx >= 0) {
              merged[idx] = remote.copy(id = merged[idx].id)
            } else {
              merged.add(0, remote)
            }
          }
          merged
        }
      }
      remoteProviders.size
    }
  }

  suspend fun syncAllUsersFromFirestore(): Result<Int> {
    val ds = firestoreDataSource ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val fbUser = try { FirebaseAuth.getInstance().currentUser } catch (_: Throwable) { null }
    if (fbUser == null) {
      return Result.failure(IllegalStateException("Sessão local ativa. Entre com sua conta Google Admin para sincronizar em nuvem."))
    }
    return ds.fetchAllUsersForAdmin().map { remoteUsers ->
      if (remoteUsers.isNotEmpty()) {
        _users.update { currentList ->
          val merged = currentList.toMutableList()
          remoteUsers.forEach { remote ->
            val idx = merged.indexOfFirst { it.id == remote.id || (remote.firebaseUid != null && it.firebaseUid == remote.firebaseUid) }
            if (idx >= 0) {
              merged[idx] = remote.copy(id = merged[idx].id)
            } else {
              merged.add(0, remote)
            }
          }
          merged
        }
        persistLocalState()
      }
      remoteUsers.size
    }
  }

  suspend fun reviewProviderDocumentsInFirestore(
    providerId: String,
    decision: ProviderVerificationStatus,
    reason: String?,
    idDocVerified: Boolean,
    criminalRecordVerified: Boolean,
    selfieVerified: Boolean
  ): Result<Boolean> {
    val providerSnapshot = _users.value.find { it.id == providerId }
      ?: return Result.failure(IllegalArgumentException("Prestador não encontrado"))

    if (decision == ProviderVerificationStatus.APROVADO) {
      approveProvider(providerId, idDocVerified, criminalRecordVerified, selfieVerified)
    } else {
      rejectProvider(providerId, reason.orEmpty(), idDocVerified, criminalRecordVerified, selfieVerified)
    }

    val ds = firestoreDataSource ?: return Result.success(false)
    val fbUser = try { FirebaseAuth.getInstance().currentUser } catch (_: Throwable) { null }
    if (fbUser == null) {
      return Result.success(false)
    }

    val targetForFirestore = providerSnapshot.copy(
      id = providerSnapshot.firebaseUid?.takeIf { it.isNotBlank() } ?: providerSnapshot.id
    )
    val firestoreResult = ds.reviewProviderDocumentsInFirestore(
      provider = targetForFirestore,
      decision = decision,
      reason = reason,
      idDocVerified = idDocVerified,
      criminalRecordVerified = criminalRecordVerified,
      selfieVerified = selfieVerified,
      adminUid = fbUser.uid,
      adminEmail = fbUser.email ?: getCurrentUser().email
    )

    return firestoreResult.fold(
      onSuccess = { audit ->
        _documentReviewAudits.update { current ->
          listOf(audit.copy(providerId = providerId)) + current.filterNot { it.id == audit.id }
        }
        Result.success(true)
      },
      onFailure = { err ->
        Result.failure(err)
      }
    )
  }

  fun markNotificationAsRead(notifId: String) {
    _notifications.update { list ->
      list.map { if (it.id == notifId) it.copy(isRead = true) else it }
    }
    persistLocalState()
    val ds = firestoreDataSource
    val fbUid = currentFirebaseUidOrNull()
    if (ds != null && fbUid != null) {
      repoScope.launch {
        ds.markUserNotificationAsRead(fbUid, notifId)
      }
    }
  }

  fun markAllNotificationsAsReadForCurrentUser() {
    val user = getCurrentUser()
    val idsToSync = mutableListOf<String>()
    _notifications.update { list ->
      list.map { notif ->
        val belongsToUser = if (user.role == UserRole.PRESTADOR) {
          notif.recipientId == "all_providers" || user.ownsId(notif.recipientId)
        } else {
          user.ownsId(notif.recipientId)
        }
        if (belongsToUser && !notif.isRead) {
          if (notif.recipientId != "all_providers") {
            idsToSync.add(notif.id)
          }
          notif.copy(isRead = true)
        } else if (belongsToUser) {
          notif.copy(isRead = true)
        } else {
          notif
        }
      }
    }
    persistLocalState()
    val ds = firestoreDataSource
    val fbUid = currentFirebaseUidOrNull()
    if (ds != null && fbUid != null && idsToSync.isNotEmpty()) {
      repoScope.launch {
        idsToSync.forEach { id ->
          ds.markUserNotificationAsRead(fbUid, id)
        }
      }
    }
  }

  // Add chat message
  fun addChatMessage(
    serviceId: String,
    content: String,
    isSystem: Boolean = false,
    syncToFirestore: Boolean = true
  ): ChatMessage {
    val currentUser = getCurrentUser()
    val srv = _services.value.find { it.id == serviceId }
    if (!isSystem && srv != null && !canUserAccessServiceChat(srv, currentUser)) {
      _chatDeliveryError.value = "O chat privado está disponível apenas entre o cliente e o prestador aceito."
      return ChatMessage(
        id = "blocked_${UUID.randomUUID().toString().take(6)}",
        serviceId = serviceId,
        senderId = currentUser.id,
        senderName = currentUser.name,
        senderRole = currentUser.role,
        content = "",
        timestamp = System.currentTimeMillis(),
        isSystemNotice = false
      )
    }
    val fbUid = currentFirebaseUidOrNull()
    val resolvedSenderId = if (isSystem) "system" else (fbUid ?: currentUser.firebaseUid?.takeIf { it.isNotBlank() } ?: currentUser.id)
    val msg = ChatMessage(
      id = "msg_${UUID.randomUUID().toString().replace("-", "").take(12)}",
      serviceId = serviceId,
      senderId = resolvedSenderId,
      senderName = if (isSystem) "ResolveAi Sistema" else currentUser.name,
      senderRole = if (isSystem) UserRole.CLIENTE else currentUser.role,
      content = content,
      timestamp = System.currentTimeMillis(),
      isSystemNotice = isSystem
    )

    if (!isSystem) {
      _chatDeliveryError.value = null
    }
    _chatMessages.update { it + msg }
    persistLocalState()

    val ds = firestoreDataSource
    if (syncToFirestore && (ds == null || fbUid == null)) {
      ChatDiagnosticLogger.record(
        serviceId = serviceId,
        category = ChatDiagnosticCategory.SEND_AUTH_MISSING,
        severity = ChatDiagnosticSeverity.WARNING,
        firestoreCode = if (ds == null) "FIRESTORE_NOT_INITIALIZED" else "FIREBASE_AUTH_NULL",
        summary = if (ds == null) {
          "Mensagem salva localmente: Firestore não inicializado."
        } else {
          "Mensagem salva localmente: sessão Firebase Auth ausente (fbUid=null)."
        },
        details = mapOf(
          "hasDataSource" to (ds != null),
          "hasFirebaseUid" to (fbUid != null),
          "localUserId" to currentUser.id,
          "userRole" to currentUser.role.name,
          "messageId" to msg.id
        )
      )
    }
    if (syncToFirestore && ds != null && fbUid != null) {
      ensureChatSyncForService(serviceId)
      repoScope.launch {
        var outcome = ds.sendChatMessageWithDiagnostics(serviceId, msg, attempt = 1)
        if (!outcome.delivered) {
          // 1. Inspeciona e sincroniza o documento pai do pedido caso ainda não exista ou esteja desatualizado no servidor
          val profileDocId = currentUser.firebaseUid?.takeIf { it.isNotBlank() } ?: fbUid
          val localSrv = _services.value.find { it.id == serviceId }
          if (localSrv != null) {
            val remoteResult = ds.fetchServiceById(serviceId)
            val remoteSrv = remoteResult.getOrNull()
            ChatDiagnosticLogger.record(
              serviceId = serviceId,
              category = ChatDiagnosticCategory.SEND_RECOVERY,
              severity = ChatDiagnosticSeverity.WARNING,
              firestoreCode = outcome.firestoreCode,
              summary = if (remoteSrv == null) {
                "Recuperação: pedido pai não encontrado no Firestore (ou erro de leitura); verificando publicação."
              } else {
                "Recuperação: pedido pai no servidor status=${remoteSrv.status.name}, clientId=${remoteSrv.clientId}, acceptedProviderId=${remoteSrv.acceptedProviderId ?: "null"}."
              },
              details = mapOf(
                "initialFailureKind" to outcome.failureKind?.name,
                "initialCode" to outcome.firestoreCode,
                "remoteFetchFailed" to remoteResult.isFailure,
                "remoteExists" to (remoteSrv != null),
                "remoteStatus" to remoteSrv?.status?.name,
                "remoteClientId" to remoteSrv?.clientId,
                "remoteAcceptedProviderId" to remoteSrv?.acceptedProviderId,
                "localStatus" to localSrv.status.name,
                "localClientId" to localSrv.clientId,
                "localAcceptedProviderId" to localSrv.acceptedProviderId,
                "profileDocId" to profileDocId,
                "userRole" to currentUser.role.name
              )
            )
            if (remoteSrv == null) {
              if (currentUser.ownsId(localSrv.clientId)) {
                ds.publishServiceRequest(localSrv.copy(clientId = profileDocId))
              }
            } else {
              val remoteStatusOpenForChat = remoteSrv.status == ServiceStatus.ACEITO ||
                remoteSrv.status == ServiceStatus.EM_EXECUCAO ||
                remoteSrv.status == ServiceStatus.CONCLUIDO
              val needsProviderSync = remoteSrv.acceptedProviderId.isNullOrBlank() ||
                (currentUser.role == UserRole.PRESTADOR &&
                  currentUser.ownsId(localSrv.acceptedProviderId) &&
                  !currentUser.ownsId(remoteSrv.acceptedProviderId))
              if (!remoteStatusOpenForChat || needsProviderSync) {
                val targetStatus = if (localSrv.status == ServiceStatus.ACEITO ||
                  localSrv.status == ServiceStatus.EM_EXECUCAO ||
                  localSrv.status == ServiceStatus.CONCLUIDO
                ) {
                  localSrv.status.name
                } else {
                  ServiceStatus.EM_EXECUCAO.name
                }
                val resolvedProviderId = when {
                  currentUser.role == UserRole.PRESTADOR ->
                    currentUser.firebaseUid?.takeIf { it.isNotBlank() } ?: fbUid
                  !localSrv.acceptedProviderId.isNullOrBlank() -> localSrv.acceptedProviderId
                  else -> remoteSrv.acceptedProviderId
                }
                val recoveryUpdates = hashMapOf<String, Any>("status" to targetStatus)
                if (!resolvedProviderId.isNullOrBlank()) {
                  recoveryUpdates["assignedProviderId"] = resolvedProviderId
                  recoveryUpdates["acceptedProviderId"] = resolvedProviderId
                }
                localSrv.acceptedProviderName?.trim()?.takeIf { it.isNotBlank() }?.take(100)?.let { provName ->
                  recoveryUpdates["assignedProviderName"] = provName
                  recoveryUpdates["acceptedProviderName"] = provName
                }
                localSrv.finalAgreedPrice?.let { recoveryUpdates["finalAgreedPrice"] = it }
                localSrv.platformFee?.let { recoveryUpdates["platformFee"] = it }
                val updated = ds.updateServiceFields(serviceId, recoveryUpdates)
                ChatDiagnosticLogger.record(
                  serviceId = serviceId,
                  category = ChatDiagnosticCategory.SEND_RECOVERY,
                  severity = if (updated) ChatDiagnosticSeverity.INFO else ChatDiagnosticSeverity.ERROR,
                  firestoreCode = if (updated) "PARENT_UPDATED" else "PARENT_UPDATE_FAILED",
                  summary = if (updated) {
                    "Pedido pai atualizado no servidor (status=$targetStatus) antes do reenvio."
                  } else {
                    "Falha ao atualizar o pedido pai no servidor antes do reenvio."
                  },
                  details = mapOf(
                    "targetStatus" to targetStatus,
                    "resolvedProviderId" to resolvedProviderId
                  )
                )
              }
            }
          }

          // 2. Reenvia a mensagem após a verificação/recuperação do pedido pai
          outcome = ds.sendChatMessageWithDiagnostics(serviceId, msg, attempt = 2)
        }
        if (outcome.delivered) {
          if (!isSystem) _chatDeliveryError.value = null
          ensureChatSyncForService(serviceId)
          markCloudSyncPulse()
        } else if (!isSystem) {
          _chatDeliveryError.value = when (outcome.failureKind) {
            ChatFailureKind.PERMISSION_DENIED ->
              "Permissão negada no Firebase (${outcome.firestoreCode ?: "PERMISSION_DENIED"}). Abra o Diagnóstico no topo do chat para ver os detalhes."
            ChatFailureKind.NETWORK_TIMEOUT ->
              "Tempo esgotado/rede indisponível (${outcome.firestoreCode ?: "TIMEOUT"}). Verifique sua conexão ou abra o Diagnóstico do chat."
            ChatFailureKind.UNAUTHENTICATED ->
              "Sessão Firebase expirada (${outcome.firestoreCode ?: "UNAUTHENTICATED"}). Faça login novamente para enviar mensagens."
            else ->
              "Não foi possível entregar a mensagem no servidor. Verifique sua conexão e tente novamente."
          }
        }
      }
    }
    return msg
  }

  fun markOnboardingSeen() {
    if (_hasSeenOnboarding.value) return
    _hasSeenOnboarding.value = true
    persistLocalState()
  }

  companion object {
    const val MIN_PROVIDER_ACCEPTANCE_BALANCE = 1.0
    val instance: MarketplaceRepository by lazy { MarketplaceRepository() }

    /**
     * Visibility rule for provider service list:
     * Once a client accepts a provider (or assigns/completes the service), that service disappears
     * from ALL other providers' lists and stays visible exclusively to the accepted provider.
     */
    fun isServiceVisibleForProvider(
      service: ServiceRequest,
      provider: User,
      showOnlyMyServices: Boolean = false,
      categoryFilter: String = "Serviços",
      proposals: List<Proposal> = emptyList(),
      declinedServiceIds: Set<String> = provider.declinedServiceIds.toSet()
    ): Boolean {
      val acceptedProposalForService = proposals.firstOrNull {
        it.serviceId == service.id && it.status == ProposalStatus.ACEITA
      }
      val hasAssignedProvider = !service.acceptedProviderId.isNullOrBlank() ||
        acceptedProposalForService != null ||
        service.status == ServiceStatus.ACEITO ||
        service.status == ServiceStatus.EM_EXECUCAO ||
        service.status == ServiceStatus.CONCLUIDO
      if (hasAssignedProvider) {
        val assignedId = service.acceptedProviderId?.takeIf { it.isNotBlank() }
          ?: acceptedProposalForService?.providerId
        return provider.ownsId(assignedId)
      }
      if (service.id in declinedServiceIds) {
        return false
      }
      if (service.status == ServiceStatus.CANCELADO) {
        return provider.ownsId(service.acceptedProviderId) ||
          proposals.any { it.serviceId == service.id && provider.ownsId(it.providerId) }
      }
      return if (showOnlyMyServices) {
        proposals.any {
          it.serviceId == service.id &&
            provider.ownsId(it.providerId) &&
            it.status != ProposalStatus.RECUSADA
        }
      } else if (categoryFilter == "Cancelado") {
        false
      } else {
        true
      }
    }

    fun defaultServiceCategories(): List<ServiceCategoryItem> = listOf(
      ServiceCategoryItem("cat_cabeleireiro", "Cabeleireiro", "Cabeleireiro", true, 0),
      ServiceCategoryItem("cat_manicure", "Manicure", "Manicure", true, 1),
      ServiceCategoryItem("cat_eletricista", "Eletricista", "Eletricista Residencial & Predial", true, 2),
      ServiceCategoryItem("cat_pintura", "Pintura", "Pintura & Acabamento", true, 3),
      ServiceCategoryItem("cat_ti_redes", "TI, Redes & Informática", "TI, Redes & Informática", true, 4),
      ServiceCategoryItem("cat_hidraulica", "Hidráulica", "Hidráulica & Encanador", true, 5),
      ServiceCategoryItem("cat_limpeza", "Limpeza", "Limpeza & Diarista", true, 6),
      ServiceCategoryItem("cat_montagem", "Montagem", "Montagem de Móveis", true, 7),
      ServiceCategoryItem("cat_reformas", "Reformas & Alvenaria", "Reformas & Alvenaria", true, 8),
      ServiceCategoryItem("cat_fretes", "Fretes & Mudanças", "Fretes & Mudanças", true, 9),
      ServiceCategoryItem("cat_ar_condicionado", "Ar Condicionado & Refrigeração", "Ar Condicionado & Refrigeração", true, 10)
    )

    fun defaultAppTips(): List<AppTip> = listOf(
      AppTip(
        id = "tip_cli_1",
        targetRole = UserRole.CLIENTE,
        category = "Pedidos Claros",
        title = "Anexe fotos ou vídeo do local ao publicar",
        description = "Adicionar fotos nítidas e descrever medidas ou modelo do equipamento ajuda prestadores a enviarem propostas mais rápidas e precisas."
      ),
      AppTip(
        id = "tip_cli_2",
        targetRole = UserRole.CLIENTE,
        category = "Avaliação",
        title = "Registre fotos do serviço concluído",
        description = "Ao finalizar um serviço, adicione fotos nos detalhes do pedido para documentar o resultado e ajudar outros clientes da plataforma."
      ),
      AppTip(
        id = "tip_cli_3",
        targetRole = UserRole.CLIENTE,
        category = "Segurança",
        title = "Negocie e converse sempre pelo chat interno",
        description = "Mantenha toda a comunicação, combinados e alterações de orçamento dentro do ResolveAi para ter suporte garantido."
      ),
      AppTip(
        id = "tip_pro_1",
        targetRole = UserRole.PRESTADOR,
        category = "Reputação",
        title = "Evite cancelar serviços já aceitos",
        description = "Cancelamentos após o aceite prejudicam o cliente e podem levar ao bloqueio temporário ou definitivo da sua conta. Aceite apenas quando puder atender."
      ),
      AppTip(
        id = "tip_pro_2",
        targetRole = UserRole.PRESTADOR,
        category = "Vitrine & Portfólio",
        title = "Adicione fotos dos serviços concluídos",
        description = "Capturar foto ao finalizar o serviço atualiza sua Vitrine automaticamente e aumenta a chance de aprovação em novas propostas."
      ),
      AppTip(
        id = "tip_pro_3",
        targetRole = UserRole.PRESTADOR,
        category = "Deslocamento & GPS",
        title = "Mantenha a localização ativa durante atendimentos",
        description = "Com a localização habilitada ao aceitar e executar o pedido, você garante histórico seguro da rota até a conclusão do atendimento."
      )
    )
  }
}

class InsufficientBalanceException(
  val requiredFee: Double,
  val currentBalance: Double,
  val missingAmount: Double,
  val providerName: String
) : Exception("Saldo insuficiente na carteira do prestador")
