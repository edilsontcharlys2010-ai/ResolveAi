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
  val rgOrCnhNumber: String = "",
  val rgOrCnhFileName: String = "cnh_digital_frente_verso.pdf",
  val rgOrCnhAttachmentUri: String? = null,
  val criminalRecordCertNumber: String = "",
  val criminalRecordFileName: String = "certidao_antecedentes_criminais.pdf",
  val criminalRecordAttachmentUri: String? = null,
  val selfieFileName: String = "selfie_com_documento.jpg",
  val selfieAttachmentUri: String? = null,
  val submittedAt: Long = System.currentTimeMillis(),
  val reviewedAt: Long? = null,
  val rejectionReason: String? = null,
  val reviewedBy: String? = null,
  val reviewedByEmail: String? = null,
  val idDocVerified: Boolean = false,
  val criminalRecordVerified: Boolean = false,
  val selfieVerified: Boolean = false
)

data class DocumentReviewAudit(
  val id: String = "",
  val providerId: String = "",
  val providerName: String = "",
  val decision: String = "",
  val reason: String? = null,
  val adminUid: String = "",
  val adminEmail: String = "",
  val idDocVerified: Boolean = false,
  val criminalRecordVerified: Boolean = false,
  val selfieVerified: Boolean = false,
  val reviewedAt: Long = System.currentTimeMillis()
)

data class ProviderServicePhoto(
  val id: String = "",
  val title: String = "",
  val category: String = "",
  val photoUri: String = "",
  val createdAt: Long = System.currentTimeMillis()
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
  val ratingCount: Int = 0,
  val completedJobs: Int = 0,
  val avatarColorHex: Long = 0xFF1E3A8A,
  val photoUrl: String? = null,
  val walletBalance: Double = 0.0, // Dedicated provider balance
  val isPhoneVerified: Boolean = true,
  val verificationStatus: ProviderVerificationStatus = ProviderVerificationStatus.APROVADO,
  val documents: ProviderDocuments? = null,
  val portfolioPhotos: List<ProviderServicePhoto> = emptyList(),
  val firebaseUid: String? = null,
  val declinedServiceIds: List<String> = emptyList(),
  val linkedAuthUids: List<String> = emptyList()
) {
  fun ownsId(candidateId: String?): Boolean {
    return !candidateId.isNullOrBlank() && (
      candidateId == id ||
        candidateId == firebaseUid ||
        candidateId in linkedAuthUids
      )
  }
}

data class SmsVerificationSession(
  val phone: String,
  val code: String,
  val generatedAt: Long = System.currentTimeMillis(),
  val isVerified: Boolean = false,
  val verificationId: String? = null,
  val attempts: Int = 0
)

enum class ServiceStatus(val label: String) {
  ABERTO("Aberto (Aguardando)"),
  EM_NEGOCIACAO("Em Negociação"),
  ACEITO("Aceito (Confirmado)"),
  EM_EXECUCAO("Em Andamento"),
  CONCLUIDO("Concluído"),
  CANCELADO("Cancelado")
}

data class ClientCompletedServicePhoto(
  val id: String = "",
  val photoUri: String = "",
  val photoName: String = "",
  val caption: String = "",
  val addedAt: Long = System.currentTimeMillis()
)

enum class CancellationRefundStatus(val label: String) {
  NENHUM("Sem taxa envolvida"),
  PENDENTE_ANALISE_ADMIN("Em análise para estorno pelo administrador"),
  ESTORNADO_AUTOMATICO("Taxa estornada automaticamente"),
  ESTORNADO_ADMIN("Estorno aprovado pelo administrador"),
  NEGADO_ADMIN("Estorno negado pelo administrador")
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
  val reviewTags: List<String> = emptyList(),
  val reviewedAt: Long? = null,
  val clientLocation: ClientLocation? = null,
  val clientRating: Double = 5.0,
  val clientAvatarColorHex: Long = 0xFF2563EB,
  val clientPhotoUrl: String? = null,
  val acceptedProviderRating: Double? = 5.0,
  val acceptedProviderAvatarColorHex: Long? = 0xFF0D9488,
  val acceptedProviderPhotoUrl: String? = null,
  val cancellationReason: String? = null,
  val cancelledByRole: UserRole? = null,
  val cancelledByName: String? = null,
  val cancelledAt: Long? = null,
  val attachmentPhotoUri: String? = null,
  val attachmentPhotoName: String? = null,
  val attachmentVideoUri: String? = null,
  val attachmentVideoName: String? = null,
  val attachmentVideoThumbnailUri: String? = null,
  val attachmentVideoDurationMs: Long? = null,
  val completionPhotoUri: String? = null,
  val completionPhotoName: String? = null,
  val completionVideoUri: String? = null,
  val completionVideoName: String? = null,
  val completionVideoThumbnailUri: String? = null,
  val completionVideoDurationMs: Long? = null,
  val completionNotes: String? = null,
  val completedAt: Long? = null,
  val cancellationRefundStatus: CancellationRefundStatus? = null,
  val cancellationRefundReviewedBy: String? = null,
  val cancellationRefundReviewedAt: Long? = null,
  val cancellationRefundNote: String? = null,
  val locationTrackingActive: Boolean = false,
  val locationPointsCount: Int = 0,
  val clientCompletionPhotos: List<ClientCompletedServicePhoto> = emptyList()
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
  val providerAvatarColorHex: Long = 0xFF0D9488,
  val providerPhotoUrl: String? = null,
  val proposedPrice: Double,
  val estimatedDays: Int = 1,
  val description: String = "",
  val status: ProposalStatus = ProposalStatus.ENVIADA,
  val counterOffers: List<CounterOfferEntry> = emptyList(),
  val createdAt: Long = System.currentTimeMillis(),
  val isDirectAcceptance: Boolean = false
) {
  // Returns current effective price being negotiated
  val currentPrice: Double
    get() = counterOffers.lastOrNull()?.price ?: proposedPrice
}

enum class WalletRechargeMethod(val label: String, val subtitle: String) {
  PIX("Pix", "Mercado Pago • Detecção automática")
}

enum class PixChargeStatus(val label: String) {
  AGUARDANDO_PAGAMENTO("Aguardando pagamento Pix"),
  PAGO("Pagamento aprovado"),
  EXPIRADO("QR Code expirado"),
  CANCELADO_OU_REJEITADO("Cancelado / Rejeitado")
}

data class PixRechargeCharge(
  val txId: String,
  val providerId: String,
  val pixKey: String,
  val merchantName: String,
  val amount: Double,
  val brCode: String,
  val status: PixChargeStatus = PixChargeStatus.AGUARDANDO_PAGAMENTO,
  val createdAt: Long = System.currentTimeMillis(),
  val expiresAt: Long = System.currentTimeMillis() + 15 * 60_000L,
  val paidAt: Long? = null,
  val detectedAutomatically: Boolean = false,
  val paymentGateway: String = "PIX_DIRETO",
  val mercadoPagoPaymentId: Long? = null,
  val mpStatus: String? = null,
  val mpStatusDetail: String? = null,
  val ticketUrl: String? = null,
  val qrCodeBase64: String? = null
)

enum class TransactionType(val label: String) {
  DEBITO_TAXA_10("Taxa de Intermediação 10%"),
  RECARGA_PIX("Recarga de Carteira Pix"),
  RECARGA_CARTAO("Recarga com Cartão de Crédito"),
  REEMBOLSO("Estorno de Taxa"),
  CREDITO_ADMIN("Crédito Manual do Administrador")
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

@androidx.annotation.Keep
@com.google.firebase.firestore.IgnoreExtraProperties
data class ServiceReview(
  @com.google.firebase.firestore.DocumentId
  val id: String = "",
  val serviceId: String = "",
  val serviceTitle: String = "",
  val serviceCategory: String = "",
  val clientId: String = "",
  val clientName: String = "",
  val clientPhotoUrl: String? = null,
  val providerId: String = "",
  val providerName: String = "",
  val rating: Float = 5.0f, // 1.0 to 5.0
  val comment: String = "",
  val complimentTags: List<String> = emptyList(),
  val createdAt: Long = System.currentTimeMillis(),
  val providerReply: String? = null,
  val repliedAt: Long? = null
)

enum class NotificationType(val label: String) {
  NOVO_SERVICO("Novo Serviço Disponível"),
  PROPOSTA_RECEBIDA("Proposta Recebida"),
  PROPOSTA_ACEITA("Proposta Aceita"),
  NOVA_MENSAGEM("Nova Mensagem"),
  STATUS_ATUALIZADO("Atualização de Serviço"),
  AVALIACAO_RECEBIDA("Avaliação Recebida"),
  DOCUMENTO_APROVADO("Documentação Aprovada"),
  DOCUMENTO_REJEITADO("Documentação Reprovada"),
  PAGAMENTO_PIX_CONFIRMADO("Pagamento Pix Confirmado")
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

data class ProviderLocationPoint(
  val id: String = "",
  val serviceId: String = "",
  val providerId: String = "",
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
  val accuracyMeters: Float = 0f,
  val speedMps: Float = 0f,
  val serviceStatus: ServiceStatus = ServiceStatus.ACEITO,
  val recordedAt: Long = System.currentTimeMillis(),
  val source: String = "GPS"
)

enum class SuggestionStatus(val label: String) {
  ENVIADA("Enviada"),
  EM_ANALISE("Em Análise"),
  IMPLEMENTADA("Implementada"),
  ARQUIVADA("Arquivada")
}

data class AppSuggestion(
  val id: String = "",
  val userId: String = "",
  val userName: String = "",
  val userRole: UserRole = UserRole.CLIENTE,
  val category: String = "Melhoria no App",
  val title: String = "",
  val description: String = "",
  val status: SuggestionStatus = SuggestionStatus.ENVIADA,
  val adminReply: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)

data class AppTip(
  val id: String,
  val targetRole: UserRole?, // null = Todos, CLIENTE = Clientes, PRESTADOR = Prestadores
  val category: String,
  val title: String,
  val description: String
)

data class ServiceCategoryItem(
  val id: String = "",
  val name: String = "",
  val specialtyLabel: String = "",
  val isActive: Boolean = true,
  val orderIndex: Int = 0,
  val updatedAt: Long = System.currentTimeMillis()
) {
  val effectiveSpecialtyLabel: String
    get() = specialtyLabel.trim().ifBlank { name.trim() }
}

data class AppAdminSettings(
  val appDisplayName: String = "ResolveAi Serviços",
  val globalBannerMessage: String = "",
  val supportWhatsappNumberE164: String = "5561992907464",
  val supportWhatsappDisplay: String = "+55 (61) 99290-7464",
  val defaultServiceUrgency: String = "Média",
  val updatedAt: Long = System.currentTimeMillis()
)

data class RegionalPriceEstimate(
  val id: String = "",
  val dateKey: String = "", // yyyy-MM-dd (atualizado diariamente)
  val displayDate: String = "", // dd/MM/yyyy
  val category: String = "",
  val regionKey: String = "", // ex: sp_sao_paulo, pr_curitiba, br_nacional
  val regionLabel: String = "", // ex: São Paulo - SP, Curitiba - PR
  val stateCode: String = "", // ex: SP, PR, RJ, ""
  val macroRegion: String = "", // Sudeste, Sul, Nordeste, Centro-Oeste, Norte, Nacional
  val averagePrice: Double = 0.0,
  val minPrice: Double = 0.0,
  val maxPrice: Double = 0.0,
  val regionalFactor: Double = 1.0,
  val localSamplesCount: Int = 0,
  val sourceLabel: String = "",
  val syncedFromCloud: Boolean = false,
  val updatedAt: Long = System.currentTimeMillis()
)

