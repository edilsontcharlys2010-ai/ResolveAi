package com.example.data

import com.example.model.ServiceRequest
import com.example.model.TransactionType
import com.example.model.WalletTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class WalletHistoryFilter(val label: String) {
  TODAS("Todas"),
  RECARGAS_PIX("Recargas Pix"),
  USO_CONTRATACOES("Uso em Contratações")
}

data class ProviderWalletSummary(
  val totalPixRecharged: Double,
  val pixRechargesCount: Int,
  val autoDetectedPixCount: Int,
  val totalContractFeesUsed: Double,
  val contractUsageCount: Int,
  val totalEstimatedContractedValue: Double,
  val totalRefunds: Double,
  val totalMovementsCount: Int
)

object WalletHistoryHelper {

  private val pixTxIdRegex = Regex("""\(([A-Za-z0-9]{6,})\)""")

  fun isPixRecharge(tx: WalletTransaction): Boolean {
    return tx.type == TransactionType.RECARGA_PIX
  }

  fun isContractUsage(tx: WalletTransaction): Boolean {
    return tx.type == TransactionType.DEBITO_TAXA_10 || tx.type == TransactionType.REEMBOLSO
  }

  fun isAutoDetectedPix(tx: WalletTransaction): Boolean {
    return tx.type == TransactionType.RECARGA_PIX &&
      tx.description.contains("detectada automaticamente", ignoreCase = true)
  }

  fun isWelcomeOrAdminBonus(tx: WalletTransaction): Boolean {
    return tx.type == TransactionType.CREDITO_ADMIN ||
      (tx.type == TransactionType.RECARGA_PIX &&
        tx.description.contains("bônus", ignoreCase = true))
  }

  fun extractPixTxId(tx: WalletTransaction): String? {
    if (tx.id.startsWith("tx_pix_") && tx.id.length > 7) {
      return tx.id.removePrefix("tx_pix_")
    }
    val match = pixTxIdRegex.find(tx.description)
    return match?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
  }

  fun computeBalanceBefore(tx: WalletTransaction): Double {
    return when (tx.type) {
      TransactionType.DEBITO_TAXA_10 -> (tx.balanceAfter + tx.amount).coerceAtLeast(0.0)
      TransactionType.RECARGA_PIX,
      TransactionType.RECARGA_CARTAO,
      TransactionType.REEMBOLSO,
      TransactionType.CREDITO_ADMIN -> (tx.balanceAfter - tx.amount).coerceAtLeast(0.0)
    }
  }

  fun resolveContractedServiceValue(
    tx: WalletTransaction,
    service: ServiceRequest?
  ): Double? {
    if (tx.type != TransactionType.DEBITO_TAXA_10) return null
    val explicitAgreed = service?.finalAgreedPrice
    if (explicitAgreed != null && explicitAgreed > 0.0) {
      return explicitAgreed
    }
    return if (tx.amount > 0.0) tx.amount * 10.0 else null
  }

  fun filterTransactions(
    transactions: List<WalletTransaction>,
    filter: WalletHistoryFilter,
    query: String = ""
  ): List<WalletTransaction> {
    val cleanQuery = query.trim().lowercase(Locale.getDefault())
    return transactions
      .asSequence()
      .filter { tx ->
        when (filter) {
          WalletHistoryFilter.TODAS -> true
          WalletHistoryFilter.RECARGAS_PIX ->
            tx.type == TransactionType.RECARGA_PIX ||
              tx.type == TransactionType.RECARGA_CARTAO ||
              tx.type == TransactionType.CREDITO_ADMIN
          WalletHistoryFilter.USO_CONTRATACOES ->
            isContractUsage(tx)
        }
      }
      .filter { tx ->
        if (cleanQuery.isBlank()) {
          true
        } else {
          val txId = extractPixTxId(tx).orEmpty()
          tx.description.lowercase(Locale.getDefault()).contains(cleanQuery) ||
            tx.serviceTitle.orEmpty().lowercase(Locale.getDefault()).contains(cleanQuery) ||
            tx.serviceId.orEmpty().lowercase(Locale.getDefault()).contains(cleanQuery) ||
            tx.id.lowercase(Locale.getDefault()).contains(cleanQuery) ||
            txId.lowercase(Locale.getDefault()).contains(cleanQuery)
        }
      }
      .sortedByDescending { it.timestamp }
      .toList()
  }

  fun summarize(
    transactions: List<WalletTransaction>,
    servicesById: Map<String, ServiceRequest> = emptyMap()
  ): ProviderWalletSummary {
    val pixRecharges = transactions.filter { it.type == TransactionType.RECARGA_PIX }
    val contractDebits = transactions.filter { it.type == TransactionType.DEBITO_TAXA_10 }
    val refunds = transactions.filter { it.type == TransactionType.REEMBOLSO }

    val totalPix = pixRecharges.sumOf { it.amount }
    val autoCount = pixRecharges.count { isAutoDetectedPix(it) }
    val totalContractFees = contractDebits.sumOf { it.amount }
    val totalContractedValue = contractDebits.sumOf { tx ->
      val srv = tx.serviceId?.let { servicesById[it] }
      resolveContractedServiceValue(tx, srv) ?: 0.0
    }
    val totalRefunds = refunds.sumOf { it.amount }

    return ProviderWalletSummary(
      totalPixRecharged = totalPix,
      pixRechargesCount = pixRecharges.size,
      autoDetectedPixCount = autoCount,
      totalContractFeesUsed = totalContractFees,
      contractUsageCount = contractDebits.size,
      totalEstimatedContractedValue = totalContractedValue,
      totalRefunds = totalRefunds,
      totalMovementsCount = transactions.size
    )
  }

  fun formatDateTime(timestamp: Long, withSeconds: Boolean = false): String {
    val pattern = if (withSeconds) "dd/MM/yyyy 'às' HH:mm:ss" else "dd/MM/yyyy 'às' HH:mm"
    return SimpleDateFormat(pattern, Locale.forLanguageTag("pt-BR")).format(Date(timestamp))
  }

  fun formatBrl(value: Double): String {
    return "R$ ${String.format("%.2f", value)}"
  }
}
