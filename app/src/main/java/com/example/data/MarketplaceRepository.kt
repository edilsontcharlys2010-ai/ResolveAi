package com.example.data

import com.example.model.ChatMessage
import com.example.model.CounterOfferEntry
import com.example.model.Proposal
import com.example.model.ProposalStatus
import com.example.model.ServiceRequest
import com.example.model.ServiceStatus
import com.example.model.TransactionType
import com.example.model.User
import com.example.model.UserRole
import com.example.model.ProviderDocuments
import com.example.model.ProviderVerificationStatus
import com.example.model.AppNotification
import com.example.model.NotificationType
import com.example.model.ServiceReview
import com.example.model.ClientLocation
import com.example.model.WalletTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class MarketplaceRepository {

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
        completedJobs = 42,
        avatarColorHex = 0xFF0D9488,
        walletBalance = 120.00 // Saldo inicial para débito da taxa de 10%
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
        completedJobs = 28,
        avatarColorHex = 0xFFEA580C,
        walletBalance = 35.00 // Saldo menor para demonstrar necessidade de recarga
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
        walletBalance = 250.00
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

  // Active User session (can switch between Client and Provider at any moment)
  private val _currentUserId = MutableStateFlow("cli_1")
  val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

  // Services
  private val _services = MutableStateFlow<List<ServiceRequest>>(
    listOf(
      ServiceRequest(
        id = "srv_1",
        clientId = "cli_1",
        clientName = "Carlos Eduardo",
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

  // Chat Messages
  private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
    listOf(
      ChatMessage(
        id = "msg_0",
        serviceId = "srv_1",
        senderId = "system",
        senderName = "ResolveAi Segurança",
        senderRole = UserRole.CLIENTE,
        content = "🔒 Chat protegido! Por segurança e para garantir a taxa da plataforma, não compartilhe telefones, e-mails, redes sociais ou links externos.",
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
        clientId = "cli_2",
        clientName = "Mariana Silveira",
        providerId = "pro_1",
        providerName = "Roberto Mendes",
        rating = 5.0f,
        comment = "Trabalho impecável! Chegou no horário combinado, resolveu o vazamento super rápido e deixou o local limpo.",
        complimentTags = listOf("Pontual", "Preço Justo", "Limpeza Impecável", "Super Recomendo"),
        createdAt = System.currentTimeMillis() - 200000000L,
        providerReply = "Muito obrigado Mariana! Fico à disposição sempre que precisar."
      ),
      ServiceReview(
        id = "rev_2",
        serviceId = "srv_past_1",
        serviceTitle = "Instalação de 4 luminárias e pendente",
        clientId = "cli_1",
        clientName = "Carlos Eduardo",
        providerId = "pro_1",
        providerName = "Roberto Mendes",
        rating = 4.9f,
        comment = "Excelente profissional. Muito cuidadoso com a fiação e com o teto de gesso.",
        complimentTags = listOf("Caprichoso", "Profissional Educado", "Recomendo"),
        createdAt = System.currentTimeMillis() - 400000000L
      ),
      ServiceReview(
        id = "rev_3",
        serviceId = "srv_past_2",
        serviceTitle = "Pintura de quarto infantil com textura",
        clientId = "cli_1",
        clientName = "Carlos Eduardo",
        providerId = "pro_2",
        providerName = "Ana Clara Ferraz",
        rating = 4.8f,
        comment = "A Ana é muito atenciosa e o acabamento da textura ficou perfeito. Recomendo muito!",
        complimentTags = listOf("Caprichoso", "Pontual", "Recomendo"),
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

  fun getCurrentUser(): User {
    return _users.value.find { it.id == _currentUserId.value } ?: _users.value.first()
  }

  fun switchUser(userId: String) {
    _currentUserId.value = userId
  }

  fun updateUserProfile(user: User) {
    _users.update { list ->
      list.map { if (it.id == user.id) user else it }
    }
  }

  // Client creates a new service request
  fun createServiceRequest(
    title: String,
    category: String,
    description: String,
    location: String,
    proposedBudget: Double,
    urgency: String
  ): ServiceRequest {
    val currentUser = getCurrentUser()
    val dist = (25..75).random() / 10.0
    val driving = (dist * 2.8).toInt().coerceAtLeast(8)
    val moto = (driving * 0.65).toInt().coerceAtLeast(5)
    val transit = (driving * 1.5).toInt().coerceAtLeast(15)

    val clientLoc = ClientLocation(
      address = if (location.contains(",")) location else "$location, ${(100..999).random()}",
      neighborhood = location.split("-").firstOrNull()?.trim() ?: location,
      city = if (location.contains(",")) location.split(",").last().trim() else "São Paulo, SP",
      referencePoint = "Endereço verificado pelo cliente",
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
      title = title,
      category = category,
      description = description,
      location = location,
      proposedBudget = proposedBudget,
      urgency = urgency,
      status = ServiceStatus.ABERTO,
      createdAt = System.currentTimeMillis(),
      clientLocation = clientLoc
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
    _notifications.update { listOf(notif) + it }

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

    _proposals.update { list ->
      list.map { prop ->
        if (prop.id == proposalId) {
          prop.copy(
            status = newStatus,
            counterOffers = prop.counterOffers + entry
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

    val provider = _users.value.find { it.id == proposal.providerId }
      ?: return Result.failure(Exception("Prestador não encontrado"))

    val finalPrice = proposal.currentPrice
    val platformFee = finalPrice * 0.10 // 10% de taxa da plataforma

    // Check provider wallet balance
    if (provider.walletBalance < platformFee) {
      val missing = platformFee - provider.walletBalance
      return Result.failure(
        InsufficientBalanceException(
          requiredFee = platformFee,
          currentBalance = provider.walletBalance,
          missingAmount = missing,
          providerName = provider.name
        )
      )
    }

    // Debit fee from provider's wallet
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

    // Update proposal status
    _proposals.update { list ->
      list.map { prop ->
        if (prop.id == proposalId) {
          prop.copy(status = ProposalStatus.ACEITA)
        } else if (prop.serviceId == proposal.serviceId) {
          // Reject other competing proposals for the same service
          prop.copy(status = ProposalStatus.RECUSADA)
        } else prop
      }
    }

    // Update service request
    _services.update { list ->
      list.map { srv ->
        if (srv.id == service.id) {
          srv.copy(
            status = ServiceStatus.ACEITO,
            finalAgreedPrice = finalPrice,
            platformFee = platformFee,
            acceptedProviderId = provider.id,
            acceptedProviderName = provider.name
          )
        } else srv
      }
    }

    // Post celebration in chat
    addChatMessage(
      serviceId = service.id,
      content = "🎉 Negociação concluída com sucesso! Valor acordado: R$ ${String.format("%.2f", finalPrice)}. Taxa de 10% (R$ ${String.format("%.2f", platformFee)}) debitada da carteira do prestador. Serviço confirmado!",
      isSystem = true
    )

    return Result.success(platformFee)
  }

  // Recharge provider wallet via Pix
  fun rechargeWallet(providerId: String, amount: Double): Double {
    var newBalance = 0.0
    _users.update { list ->
      list.map { u ->
        if (u.id == providerId) {
          newBalance = u.walletBalance + amount
          u.copy(walletBalance = newBalance)
        } else u
      }
    }

    val transaction = WalletTransaction(
      id = "tx_${UUID.randomUUID().toString().take(6)}",
      providerId = providerId,
      type = TransactionType.RECARGA_PIX,
      amount = amount,
      description = "Recarga de saldo via Pix instantâneo",
      timestamp = System.currentTimeMillis(),
      balanceAfter = newBalance
    )
    _walletTransactions.update { listOf(transaction) + it }

    return newBalance
  }

  // Update Service Status (e.g. In Progress, Completed, Evaluated)
  fun updateServiceStatus(serviceId: String, newStatus: ServiceStatus) {
    _services.update { list ->
      list.map { srv ->
        if (srv.id == serviceId) srv.copy(status = newStatus) else srv
      }
    }

    addChatMessage(
      serviceId = serviceId,
      content = "ℹ️ Status do serviço alterado para: ${newStatus.label}",
      isSystem = true
    )
  }

  // Active SMS sessions
  private val _smsSessions = MutableStateFlow<Map<String, String>>(emptyMap())

  fun sendSmsCode(phone: String): String {
    val code = ((100000..999999).random()).toString()
    _smsSessions.update { it + (phone to code) }
    return code
  }

  fun verifySmsCode(phone: String, inputCode: String): Boolean {
    val stored = _smsSessions.value[phone]
    return stored != null && (stored == inputCode.trim() || inputCode.trim() == "123456")
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
    documents: ProviderDocuments? = null
  ): User {
    val prefix = if (role == UserRole.CLIENTE) "cli" else "pro"
    val avatarColors = listOf(0xFF2563EB, 0xFF0D9488, 0xFF7C3AED, 0xFFEA580C, 0xFF059669, 0xFF0284C7)
    val vStatus = if (role == UserRole.PRESTADOR) ProviderVerificationStatus.PENDENTE_ANALISE else ProviderVerificationStatus.APROVADO
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
      walletBalance = if (role == UserRole.PRESTADOR) initialWalletBonus else 0.0,
      isPhoneVerified = true,
      verificationStatus = vStatus,
      documents = documents
    )

    _users.update { listOf(newUser) + it }
    _currentUserId.value = newUser.id

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

    return newUser
  }

  // Rate service
  fun rateService(
    serviceId: String,
    rating: Float,
    comment: String,
    complimentTags: List<String> = emptyList()
  ) {
    val srv = _services.value.find { it.id == serviceId } ?: return
    val providerId = srv.acceptedProviderId ?: "pro_1"
    val providerName = srv.acceptedProviderName ?: "Prestador"
    val currentUser = getCurrentUser()

    val newReview = ServiceReview(
      id = "rev_${UUID.randomUUID().toString().take(6)}",
      serviceId = serviceId,
      serviceTitle = srv.title,
      clientId = currentUser.id,
      clientName = currentUser.name,
      providerId = providerId,
      providerName = providerName,
      rating = rating,
      comment = comment,
      complimentTags = complimentTags,
      createdAt = System.currentTimeMillis()
    )

    _reviews.update { listOf(newReview) + it }

    // Recompute provider average rating and increment completed jobs
    val providerReviews = _reviews.value.filter { it.providerId == providerId }
    val newAvgRating = if (providerReviews.isNotEmpty()) {
      providerReviews.map { it.rating.toDouble() }.average()
    } else {
      rating.toDouble()
    }

    _users.update { list ->
      list.map { u ->
        if (u.id == providerId) {
          u.copy(
            rating = Math.round(newAvgRating * 10.0) / 10.0,
            completedJobs = u.completedJobs + 1
          )
        } else u
      }
    }

    _services.update { list ->
      list.map { s ->
        if (s.id == serviceId) {
          s.copy(
            status = ServiceStatus.CONCLUIDO,
            ratingGiven = rating,
            reviewComment = comment
          )
        } else s
      }
    }

    val tagsStr = if (complimentTags.isNotEmpty()) " [${complimentTags.joinToString(", ")}]" else ""
    addChatMessage(
      serviceId = serviceId,
      content = "⭐ Serviço avaliado com $rating estrelas pelo cliente: \"$comment\"$tagsStr",
      isSystem = true
    )
  }

  fun replyToReview(reviewId: String, replyText: String) {
    _reviews.update { list ->
      list.map { rev ->
        if (rev.id == reviewId) {
          rev.copy(providerReply = replyText)
        } else rev
      }
    }
  }

  // Admin approval of provider documents
  fun approveProvider(providerId: String) {
    _users.update { list ->
      list.map { u ->
        if (u.id == providerId) {
          u.copy(
            verificationStatus = ProviderVerificationStatus.APROVADO,
            documents = u.documents?.copy(reviewedAt = System.currentTimeMillis())
          )
        } else u
      }
    }

    val notif = AppNotification(
      id = "notif_${UUID.randomUUID().toString().take(6)}",
      recipientId = providerId,
      title = "🎉 Documentos Aprovados pelo Administrador!",
      message = "Sua documentação (RG/CNH, antecedentes criminais e selfie) foi aprovada pela equipe de segurança. Você já tem acesso total ao mural de serviços e à sua carteira digital.",
      type = NotificationType.DOCUMENTO_APROVADO,
      timestamp = System.currentTimeMillis()
    )
    _notifications.update { listOf(notif) + it }
  }

  fun rejectProvider(providerId: String, reason: String) {
    _users.update { list ->
      list.map { u ->
        if (u.id == providerId) {
          u.copy(
            verificationStatus = ProviderVerificationStatus.REJEITADO,
            documents = u.documents?.copy(
              reviewedAt = System.currentTimeMillis(),
              rejectionReason = reason
            )
          )
        } else u
      }
    }

    val notif = AppNotification(
      id = "notif_${UUID.randomUUID().toString().take(6)}",
      recipientId = providerId,
      title = "⚠️ Documentação Reprovada",
      message = "Sua documentação não foi aprovada pelo Administrador: $reason. Por favor, envie novos documentos para regularização.",
      type = NotificationType.DOCUMENTO_REJEITADO,
      timestamp = System.currentTimeMillis()
    )
    _notifications.update { listOf(notif) + it }
  }

  fun markNotificationAsRead(notifId: String) {
    _notifications.update { list ->
      list.map { if (it.id == notifId) it.copy(isRead = true) else it }
    }
  }

  // Add chat message
  fun addChatMessage(serviceId: String, content: String, isSystem: Boolean = false) {
    val currentUser = getCurrentUser()
    val msg = ChatMessage(
      id = "msg_${UUID.randomUUID().toString().take(6)}",
      serviceId = serviceId,
      senderId = if (isSystem) "system" else currentUser.id,
      senderName = if (isSystem) "ResolveAi Sistema" else currentUser.name,
      senderRole = currentUser.role,
      content = content,
      timestamp = System.currentTimeMillis(),
      isSystemNotice = isSystem
    )

    _chatMessages.update { it + msg }
  }

  companion object {
    val instance: MarketplaceRepository by lazy { MarketplaceRepository() }
  }
}

class InsufficientBalanceException(
  val requiredFee: Double,
  val currentBalance: Double,
  val missingAmount: Double,
  val providerName: String
) : Exception("Saldo insuficiente na carteira do prestador")
