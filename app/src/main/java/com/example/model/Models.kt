package com.example.model

enum class UserRole(val label: String) {
  CLIENTE("Cliente"),
  PRESTADOR("Prestador de Serviço"),
  ADMIN("Administrador")
}

enum class ProviderVerificationStatus(val label: String) {
  PENDENTE_ANALISE("Em Análise pelo Administrador"),
  APROVADO("Aprovado"),
  REJEITADO("Reprovado")
}

data class ProviderDocuments(
  val rgOrCnhNumber: String,
  val rgOrCnhFileName: String = "cnh_digital_frente_verso.pdf",
  val criminalRecordCertNumber: String,
  val criminalRecordFileName: String = "certidao_antecedentes_criminais.pdf",
  val selfieFileName: String = "selfie_com_documento.jpg",
  val submittedAt: Long = System.currentTimeMillis(),
  val reviewedAt: Long? = null,
  val rejectionReason: String? = null
)

data class User(
  val id: String,
  val name: String,
  val email: String,
  val phone: String,
  val role: UserRole,
  val category: String = "", // e.g. "Eletricista", "Encanador", etc. (for providers)
  val bio: String = "",
  val city: String = "São Paulo, SP",
  val document: String = "", // CPF or CNPJ
  val rating: Double = 5.0,
  val completedJobs: Int = 0,
  val avatarColorHex: Long = 0xFF1E3A8A,
  val walletBalance: Double = 0.0, // Dedicated provider balance
  val isPhoneVerified: Boolean = true,
  val verificationStatus: ProviderVerificationStatus = ProviderVerificationStatus.APROVADO,
  val documents: ProviderDocuments? = null
)

data class SmsVerificationSession(
  val phone: String,
  val code: String,
  val generatedAt: Long = System.currentTimeMillis(),
  val isVerified: Boolean = false
)

enum class ServiceStatus(val label: String) {
  ABERTO("Aberto (Aguardando)"),
  EM_NEGOCIACAO("Em Negociação"),
  ACEITO("Aceito (Taxa Paga)"),
  EM_EXECUCAO("Em Execução"),
  CONCLUIDO("Concluído"),
  CANCELADO("Cancelado")
}

data class ClientLocation(
  val address: String,
  val neighborhood: String = "Bairro Central",
  val city: String = "São Paulo, SP",
  val referencePoint: String = "Próximo ao comércio local e transporte público",
  val distanceKm: Double = 3.8,
  val drivingTimeMinutes: Int = 12,
  val motoTimeMinutes: Int = 8,
  val transitTimeMinutes: Int = 20,
  val trafficCondition: String = "Trânsito fluido",
  val bestRoute: String = "Via corredor principal de acesso rápido"
)

data class ServiceRequest(
  val id: String,
  val clientId: String,
  val clientName: String,
  val title: String,
  val category: String,
  val description: String,
  val location: String,
  val proposedBudget: Double, // Valor inicial proposto pelo cliente
  val finalAgreedPrice: Double? = null,
  val platformFee: Double? = null, // 10% do valor final acordado
  val status: ServiceStatus = ServiceStatus.ABERTO,
  val acceptedProviderId: String? = null,
  val acceptedProviderName: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val urgency: String = "Média", // Baixa, Média, Alta, Urgente
  val ratingGiven: Float? = null,
  val reviewComment: String? = null,
  val clientLocation: ClientLocation? = null
) {
  val effectiveLocation: ClientLocation
    get() = clientLocation ?: ClientLocation(
      address = location,
      neighborhood = location.split("-").firstOrNull()?.trim() ?: location,
      city = location.split("-").lastOrNull()?.trim() ?: "São Paulo, SP",
      distanceKm = 4.2,
      drivingTimeMinutes = 14,
      motoTimeMinutes = 9,
      transitTimeMinutes = 22
    )
}

enum class ProposalStatus(val label: String) {
  ENVIADA("Proposta Enviada"),
  CONTRAPROPOSTA_CLIENTE("Contraproposta do Cliente"),
  CONTRAPROPOSTA_PRESTADOR("Contraproposta do Prestador"),
  ACEITA("Aceita"),
  RECUSADA("Recusada")
}

data class CounterOfferEntry(
  val price: Double,
  val authorId: String,
  val authorName: String,
  val authorRole: UserRole,
  val note: String,
  val timestamp: Long = System.currentTimeMillis()
)

data class Proposal(
  val id: String,
  val serviceId: String,
  val providerId: String,
  val providerName: String,
  val providerRating: Double = 4.9,
  val providerCategory: String = "",
  val proposedPrice: Double,
  val estimatedDays: Int = 1,
  val description: String = "",
  val status: ProposalStatus = ProposalStatus.ENVIADA,
  val counterOffers: List<CounterOfferEntry> = emptyList(),
  val createdAt: Long = System.currentTimeMillis()
) {
  // Returns current effective price being negotiated
  val currentPrice: Double
    get() = counterOffers.lastOrNull()?.price ?: proposedPrice
}

enum class TransactionType(val label: String) {
  DEBITO_TAXA_10("Taxa de Intermediação 10%"),
  RECARGA_PIX("Recarga de Carteira Pix"),
  REEMBOLSO("Estorno de Taxa")
}

data class WalletTransaction(
  val id: String,
  val providerId: String,
  val type: TransactionType,
  val amount: Double,
  val serviceId: String? = null,
  val serviceTitle: String? = null,
  val description: String,
  val timestamp: Long = System.currentTimeMillis(),
  val balanceAfter: Double
)

data class ChatMessage(
  val id: String,
  val serviceId: String,
  val senderId: String,
  val senderName: String,
  val senderRole: UserRole,
  val content: String,
  val timestamp: Long = System.currentTimeMillis(),
  val isSystemNotice: Boolean = false
)

data class ServiceReview(
  val id: String,
  val serviceId: String,
  val serviceTitle: String,
  val clientId: String,
  val clientName: String,
  val providerId: String,
  val providerName: String,
  val rating: Float, // 1.0 to 5.0
  val comment: String,
  val complimentTags: List<String> = emptyList(),
  val createdAt: Long = System.currentTimeMillis(),
  val providerReply: String? = null
)

enum class NotificationType(val label: String) {
  NOVO_SERVICO("Novo Serviço Disponível"),
  PROPOSTA_RECEBIDA("Proposta Recebida"),
  PROPOSTA_ACEITA("Proposta Aceita"),
  AVALIACAO_RECEBIDA("Avaliação Recebida"),
  DOCUMENTO_APROVADO("Documentação Aprovada"),
  DOCUMENTO_REJEITADO("Documentação Reprovada")
}

data class AppNotification(
  val id: String,
  val recipientId: String, // providerId or "all_providers" or clientId
  val title: String,
  val message: String,
  val type: NotificationType,
  val serviceId: String? = null,
  val timestamp: Long = System.currentTimeMillis(),
  val isRead: Boolean = false
)


