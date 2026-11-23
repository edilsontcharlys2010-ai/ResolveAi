package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.AppAdminSettings
import com.example.model.AppNotification
import com.example.model.AppSuggestion
import com.example.model.AppTip
import com.example.model.ChatMessage
import com.example.model.DocumentReviewAudit
import com.example.model.PixRechargeCharge
import com.example.model.Proposal
import com.example.model.ProviderLocationPoint
import com.example.model.RegionalPriceEstimate
import com.example.model.ServiceCategoryItem
import com.example.model.ServiceRequest
import com.example.model.ServiceReview
import com.example.model.User
import com.example.model.WalletTransaction
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

data class PersistedMarketplaceState(
  val schemaVersion: Int = 1,
  val currentUserId: String = "cli_1",
  val isAuthenticated: Boolean = false,
  val users: List<User> = emptyList(),
  val services: List<ServiceRequest> = emptyList(),
  val proposals: List<Proposal> = emptyList(),
  val walletTransactions: List<WalletTransaction> = emptyList(),
  val mercadoPagoCharges: List<PixRechargeCharge> = emptyList(),
  val chatMessages: List<ChatMessage> = emptyList(),
  val reviews: List<ServiceReview> = emptyList(),
  val notifications: List<AppNotification> = emptyList(),
  val documentReviewAudits: List<DocumentReviewAudit> = emptyList(),
  val appSuggestions: List<AppSuggestion> = emptyList(),
  val serviceLocationHistory: List<ProviderLocationPoint> = emptyList(),
  val hasSeenOnboarding: Boolean = false,
  val serviceCategories: List<ServiceCategoryItem> = emptyList(),
  val appTips: List<AppTip> = emptyList(),
  val appAdminSettings: AppAdminSettings? = null,
  val declinedServiceIdsByProvider: Map<String, List<String>> = emptyMap(),
  val userPasswordHashes: Map<String, String> = emptyMap(),
  val userSavedPasswords: Map<String, String> = emptyMap(),
  val regionalPriceBenchmarks: List<RegionalPriceEstimate> = emptyList()
)

class MarketplaceLocalStore(private val storageDir: File) {

  constructor(context: Context) : this(context.applicationContext.filesDir)

  private val lock = Any()
  private val stateFile: File get() = File(storageDir, "resolveai_marketplace_state.json")

  private val moshi: Moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  private val adapter = moshi.adapter(PersistedMarketplaceState::class.java)

  fun load(): PersistedMarketplaceState? {
    synchronized(lock) {
      return try {
        if (!stateFile.exists() || stateFile.length() == 0L) return null
        val json = stateFile.readText(Charsets.UTF_8)
        if (json.isBlank()) null else adapter.fromJson(json)
      } catch (e: Exception) {
        Log.w("MarketplaceLocalStore", "Falha ao ler estado local: ${e.message}")
        null
      }
    }
  }

  fun save(state: PersistedMarketplaceState) {
    synchronized(lock) {
      try {
        if (!storageDir.exists()) storageDir.mkdirs()
        val tempFile = File(storageDir, "resolveai_marketplace_state.tmp")
        val json = adapter.toJson(state)
        tempFile.writeText(json, Charsets.UTF_8)
        if (!tempFile.renameTo(stateFile)) {
          stateFile.writeText(json, Charsets.UTF_8)
          tempFile.delete()
        }
      } catch (e: Exception) {
        Log.w("MarketplaceLocalStore", "Falha ao salvar estado local: ${e.message}")
      }
    }
  }
}
