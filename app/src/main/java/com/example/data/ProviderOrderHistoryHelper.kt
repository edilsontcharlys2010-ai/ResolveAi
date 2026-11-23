package com.example.data

import com.example.model.Proposal
import com.example.model.ProposalStatus
import com.example.model.ServiceRequest
import com.example.model.ServiceReview
import com.example.model.ServiceStatus
import com.example.model.User

enum class ProviderHistoryCategory(val label: String) {
  FINALIZADO("Finalizados"),
  CANCELADO("Cancelados"),
  RECUSADO("Recusados")
}

enum class ProviderHistoryFilter(val label: String) {
  TODOS("Todos"),
  FINALIZADO("Finalizados"),
  CANCELADO("Cancelados"),
  RECUSADO("Recusados")
}

data class ProviderOrderHistoryItem(
  val service: ServiceRequest,
  val category: ProviderHistoryCategory,
  val providerProposal: Proposal? = null,
  val review: ServiceReview? = null,
  val outcomeLabel: String,
  val reasonSummary: String?,
  val sortTimestamp: Long
)

data class ProviderOrderHistorySummary(
  val totalCount: Int = 0,
  val completedCount: Int = 0,
  val canceledCount: Int = 0,
  val declinedCount: Int = 0,
  val totalCompletedValue: Double = 0.0
)

object ProviderOrderHistoryHelper {

  fun buildHistory(
    provider: User,
    services: List<ServiceRequest>,
    proposals: List<Proposal>,
    declinedServiceIds: Set<String> = emptySet(),
    reviews: List<ServiceReview> = emptyList()
  ): List<ProviderOrderHistoryItem> {
    val effectiveDeclined = provider.declinedServiceIds.toSet() + declinedServiceIds
    val proposalsByServiceId = proposals
      .filter { provider.ownsId(it.providerId) }
      .groupBy { it.serviceId }
    val reviewsByServiceId = reviews.associateBy { it.serviceId }

    return services.mapNotNull { srv ->
      val matchingProposals = proposalsByServiceId[srv.id].orEmpty()
      val bestProposal = matchingProposals.maxByOrNull { it.createdAt }
      val isAssignedToProvider = provider.ownsId(srv.acceptedProviderId)
      val hasAnyProposal = matchingProposals.isNotEmpty()
      val isDeclinedByProvider = srv.id in effectiveDeclined
      val hasRejectedProposal = matchingProposals.any { it.status == ProposalStatus.RECUSADA }
      val takenByOtherProvider = !srv.acceptedProviderId.isNullOrBlank() &&
        !isAssignedToProvider &&
        hasAnyProposal &&
        (srv.status == ServiceStatus.ACEITO ||
          srv.status == ServiceStatus.EM_EXECUCAO ||
          srv.status == ServiceStatus.CONCLUIDO)

      val historyCategory: ProviderHistoryCategory? = when {
        srv.status == ServiceStatus.CONCLUIDO && isAssignedToProvider ->
          ProviderHistoryCategory.FINALIZADO
        srv.status == ServiceStatus.CANCELADO && (isAssignedToProvider || hasAnyProposal || isDeclinedByProvider) ->
          ProviderHistoryCategory.CANCELADO
        isDeclinedByProvider || hasRejectedProposal || takenByOtherProvider ->
          ProviderHistoryCategory.RECUSADO
        else -> null
      }

      historyCategory?.let { cat ->
        val outcomeLabel = when (cat) {
          ProviderHistoryCategory.FINALIZADO -> "Concluído"
          ProviderHistoryCategory.CANCELADO -> "Cancelado"
          ProviderHistoryCategory.RECUSADO -> when {
            isDeclinedByProvider -> "Recusado por você"
            takenByOtherProvider -> "Outro prestador escolhido"
            else -> "Proposta recusada"
          }
        }

        val reasonSummary = when (cat) {
          ProviderHistoryCategory.FINALIZADO ->
            srv.completionNotes?.takeIf { it.isNotBlank() }
          ProviderHistoryCategory.CANCELADO ->
            srv.cancellationReason?.takeIf { it.isNotBlank() }
              ?: "Pedido cancelado"
          ProviderHistoryCategory.RECUSADO -> when {
            isDeclinedByProvider -> "Você recusou este pedido na tela de detalhes."
            takenByOtherProvider -> "O cliente optou por outro profissional para este atendimento."
            else -> "Sua proposta ou aceite não foi selecionado pelo cliente."
          }
        }

        val timestamp = maxOf(
          srv.reviewedAt ?: 0L,
          srv.cancelledAt ?: 0L,
          srv.createdAt,
          bestProposal?.createdAt ?: 0L
        )

        ProviderOrderHistoryItem(
          service = srv,
          category = cat,
          providerProposal = bestProposal,
          review = reviewsByServiceId[srv.id],
          outcomeLabel = outcomeLabel,
          reasonSummary = reasonSummary,
          sortTimestamp = timestamp
        )
      }
    }.sortedByDescending { it.sortTimestamp }
  }

  fun summarize(items: List<ProviderOrderHistoryItem>): ProviderOrderHistorySummary {
    val completed = items.filter { it.category == ProviderHistoryCategory.FINALIZADO }
    val canceled = items.filter { it.category == ProviderHistoryCategory.CANCELADO }
    val declined = items.filter { it.category == ProviderHistoryCategory.RECUSADO }
    return ProviderOrderHistorySummary(
      totalCount = items.size,
      completedCount = completed.size,
      canceledCount = canceled.size,
      declinedCount = declined.size,
      totalCompletedValue = completed.sumOf { it.service.finalAgreedPrice ?: it.service.proposedBudget }
    )
  }

  fun filterItems(
    items: List<ProviderOrderHistoryItem>,
    filter: ProviderHistoryFilter,
    query: String = ""
  ): List<ProviderOrderHistoryItem> {
    val cleanQuery = query.trim()
    return items.filter { item ->
      val matchesCategory = when (filter) {
        ProviderHistoryFilter.TODOS -> true
        ProviderHistoryFilter.FINALIZADO -> item.category == ProviderHistoryCategory.FINALIZADO
        ProviderHistoryFilter.CANCELADO -> item.category == ProviderHistoryCategory.CANCELADO
        ProviderHistoryFilter.RECUSADO -> item.category == ProviderHistoryCategory.RECUSADO
      }
      val matchesQuery = cleanQuery.isBlank() ||
        item.service.title.contains(cleanQuery, ignoreCase = true) ||
        item.service.category.contains(cleanQuery, ignoreCase = true) ||
        item.service.clientName.contains(cleanQuery, ignoreCase = true) ||
        item.service.location.contains(cleanQuery, ignoreCase = true) ||
        item.outcomeLabel.contains(cleanQuery, ignoreCase = true)
      matchesCategory && matchesQuery
    }
  }
}
