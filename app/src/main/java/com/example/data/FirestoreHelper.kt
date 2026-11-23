package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.ChatMessage
import com.example.model.ServiceRequest
import com.example.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Helper centralizado para inicialização e gerenciamento das coleções principais do Cloud Firestore:
 * - Perfis de Usuários (/users/{userId})
 * - Solicitações de Serviço (/services/{serviceId})
 * - Mensagens do Chat Privado (/services/{serviceId}/messages/{messageId})
 *
 * Sempre utiliza o banco nomeado configurado em R.string.firestore_database_id.
 */
class FirestoreHelper(
  private val databaseId: String,
  private val firestoreProvider: () -> FirebaseFirestore?,
  val dataSource: FirestoreMarketplaceDataSource = FirestoreMarketplaceDataSource(firestoreProvider)
) {

  constructor(context: Context) : this(
    databaseId = resolveDatabaseId(context),
    firestoreProvider = {
      obtainNamedFirestoreInstance(resolveDatabaseId(context))
    }
  )

  constructor(firestore: FirebaseFirestore, databaseId: String = DEFAULT_NAMED_DATABASE_ID) : this(
    databaseId = databaseId,
    firestoreProvider = { firestore },
    dataSource = FirestoreMarketplaceDataSource { firestore }
  )

  val firestore: FirebaseFirestore?
    get() = firestoreProvider()

  val isInitialized: Boolean
    get() = firestore != null

  fun usersCollection(): CollectionReference? = firestore?.collection(COLLECTION_USERS)

  fun userDocument(userId: String): DocumentReference? {
    if (!isValidDocumentId(userId)) return null
    return usersCollection()?.document(userId)
  }

  fun servicesCollection(): CollectionReference? = firestore?.collection(COLLECTION_SERVICES)

  fun serviceDocument(serviceId: String): DocumentReference? {
    if (!isValidDocumentId(serviceId)) return null
    return servicesCollection()?.document(serviceId)
  }

  fun chatMessagesCollection(serviceId: String): CollectionReference? {
    return serviceDocument(serviceId)?.collection(SUBCOLLECTION_MESSAGES)
  }

  fun chatMessageDocument(serviceId: String, messageId: String): DocumentReference? {
    if (!isValidDocumentId(messageId)) return null
    return chatMessagesCollection(serviceId)?.document(messageId)
  }

  fun getConfiguredDatabaseId(): String = databaseId

  fun currentAuthenticatedUserIdOrNull(): String? {
    return try {
      FirebaseAuth.getInstance().currentUser?.uid?.takeIf { it.isNotBlank() }
    } catch (_: Throwable) {
      null
    }
  }

  fun requireAuthenticatedUserId(): String {
    return currentAuthenticatedUserIdOrNull()
      ?: throw IllegalStateException("Usuário precisa estar autenticado antes de acessar o Firestore.")
  }

  // ============================================================================================
  // 1. Coleção de Perfis de Usuários (/users/{userId})
  // ============================================================================================

  suspend fun saveUserProfile(user: User, targetDocId: String = user.id): Result<Unit> {
    if (!isValidDocumentId(targetDocId)) {
      return Result.failure(IllegalArgumentException("ID de perfil inválido para o Firestore."))
    }
    return dataSource.saveUserProfile(user = user, targetDocId = targetDocId)
  }

  suspend fun getUserProfile(userId: String): Result<User?> {
    if (!isValidDocumentId(userId)) {
      return Result.failure(IllegalArgumentException("ID de usuário inválido."))
    }
    if (firestore == null) {
      return Result.failure(IllegalStateException("Firestore não inicializado."))
    }
    return try {
      Result.success(dataSource.loadUserProfile(userId))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, "$COLLECTION_USERS/$userId")
      Result.failure(e)
    }
  }

  fun observeUserProfile(userId: String): Flow<User?> {
    if (!isValidDocumentId(userId)) return flowOf(null)
    return dataSource.observeUserProfile(userId)
  }

  fun observeApprovedProviders(): Flow<List<User>> = dataSource.observeApprovedProviders()

  fun observeProvidersForAdmin(): Flow<List<User>> = dataSource.observeProvidersForAdmin()

  suspend fun updateUserProfileFields(userId: String, updates: Map<String, Any>): Result<Unit> {
    if (!isValidDocumentId(userId)) {
      return Result.failure(IllegalArgumentException("ID de usuário inválido."))
    }
    return dataSource.updateUserFields(userId = userId, updates = updates)
  }

  suspend fun deleteUserProfile(userId: String): Result<Unit> {
    if (!isValidDocumentId(userId)) {
      return Result.failure(IllegalArgumentException("ID de usuário inválido."))
    }
    return dataSource.deleteUserInFirestore(targetDocId = userId)
  }

  // ============================================================================================
  // 2. Coleção de Serviços (/services/{serviceId})
  // ============================================================================================

  suspend fun saveService(service: ServiceRequest): Result<ServiceRequest> {
    if (!isValidDocumentId(service.id)) {
      return Result.failure(IllegalArgumentException("ID de serviço inválido."))
    }
    if (firestore == null) {
      return Result.failure(IllegalStateException("Firestore não inicializado."))
    }
    return try {
      val saved = dataSource.publishServiceRequest(service)
      if (saved) {
        Result.success(service)
      } else {
        Result.failure(IllegalStateException("Não foi possível salvar o serviço no Firestore."))
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "$COLLECTION_SERVICES/${service.id}")
      Result.failure(e)
    }
  }

  suspend fun getService(serviceId: String): Result<ServiceRequest?> {
    if (!isValidDocumentId(serviceId)) {
      return Result.failure(IllegalArgumentException("ID de serviço inválido."))
    }
    return dataSource.fetchServiceById(serviceId)
  }

  fun observeService(serviceId: String): Flow<ServiceRequest?> {
    if (!isValidDocumentId(serviceId)) return flowOf(null)
    return dataSource.observeServiceById(serviceId)
  }

  fun observeOpenServices(): Flow<List<ServiceRequest>> = dataSource.observeOpenServices()

  fun observeClientServices(clientId: String): Flow<List<ServiceRequest>> {
    if (!isValidDocumentId(clientId)) return flowOf(emptyList())
    return dataSource.observeClientServices(clientId)
  }

  fun observeProviderAssignedServices(providerId: String): Flow<List<ServiceRequest>> {
    if (!isValidDocumentId(providerId)) return flowOf(emptyList())
    return dataSource.observeProviderAssignedServices(providerId)
  }

  suspend fun updateServiceBudget(serviceId: String, newBudget: Double): Result<Unit> {
    if (!isValidDocumentId(serviceId)) {
      return Result.failure(IllegalArgumentException("ID de serviço inválido."))
    }
    if (newBudget < 0.0) {
      return Result.failure(IllegalArgumentException("O orçamento não pode ser negativo."))
    }
    return try {
      val updated = dataSource.updateServiceBudget(serviceId, newBudget)
      if (updated) Result.success(Unit)
      else Result.failure(IllegalStateException("Falha ao atualizar o orçamento no Firestore."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "$COLLECTION_SERVICES/$serviceId")
      Result.failure(e)
    }
  }

  suspend fun updateServiceFields(serviceId: String, updates: Map<String, Any>): Result<Unit> {
    if (!isValidDocumentId(serviceId)) {
      return Result.failure(IllegalArgumentException("ID de serviço inválido."))
    }
    return try {
      val updated = dataSource.updateServiceFields(serviceId, updates)
      if (updated) Result.success(Unit)
      else Result.failure(IllegalStateException("Falha ao atualizar os campos do serviço no Firestore."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "$COLLECTION_SERVICES/$serviceId")
      Result.failure(e)
    }
  }

  suspend fun deleteService(serviceId: String): Result<Unit> {
    if (!isValidDocumentId(serviceId)) {
      return Result.failure(IllegalArgumentException("ID de serviço inválido."))
    }
    return dataSource.deleteServiceInFirestore(serviceId)
  }

  // ============================================================================================
  // 3. Subcoleção de Mensagens do Chat (/services/{serviceId}/messages/{messageId})
  // ============================================================================================

  suspend fun sendChatMessage(serviceId: String, message: ChatMessage): Result<ChatMessage> {
    if (!isValidDocumentId(serviceId) || !isValidDocumentId(message.id)) {
      return Result.failure(IllegalArgumentException("Identificador inválido para mensagem do chat."))
    }
    val cleanContent = message.content.trim()
    if (cleanContent.isBlank()) {
      return Result.failure(IllegalArgumentException("A mensagem do chat não pode estar vazia."))
    }
    return try {
      val normalized = message.copy(serviceId = serviceId, content = cleanContent.take(2000))
      val sent = dataSource.sendChatMessage(serviceId = serviceId, message = normalized)
      if (sent) Result.success(normalized)
      else Result.failure(IllegalStateException("Não foi possível enviar a mensagem para o Firestore."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(
        e,
        OperationType.CREATE,
        "$COLLECTION_SERVICES/$serviceId/$SUBCOLLECTION_MESSAGES/${message.id}"
      )
      Result.failure(e)
    }
  }

  suspend fun getChatMessages(serviceId: String): Result<List<ChatMessage>> {
    if (!isValidDocumentId(serviceId)) {
      return Result.failure(IllegalArgumentException("ID de serviço inválido para histórico do chat."))
    }
    return dataSource.fetchChatMessages(serviceId)
  }

  fun observeChatMessages(serviceId: String): Flow<List<ChatMessage>> {
    if (!isValidDocumentId(serviceId)) return flowOf(emptyList())
    return dataSource.observeChatMessages(serviceId)
  }

  companion object {
    private const val TAG = "FirestoreHelper"
    const val COLLECTION_USERS = "users"
    const val COLLECTION_SERVICES = "services"
    const val SUBCOLLECTION_MESSAGES = "messages"
    const val SUBCOLLECTION_DOCUMENT_REVIEWS = "documentReviews"
    const val SUBCOLLECTION_TRANSACTIONS = "transactions"
    const val SUBCOLLECTION_LOCATION_HISTORY = "locationHistory"
    private const val DEFAULT_NAMED_DATABASE_ID = "ai-studio-android-2944595a-9bac-47e3-98f7-2cdf5dcec5df"
    private val DOCUMENT_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")

    @Volatile
    private var sharedInstance: FirestoreHelper? = null

    fun isValidDocumentId(id: String): Boolean {
      return id.isNotBlank() && id.length <= 128 && DOCUMENT_ID_REGEX.matches(id)
    }

    internal fun resolveDatabaseId(context: Context): String {
      return context.applicationContext.getString(R.string.firestore_database_id)
        .trim()
        .ifBlank { DEFAULT_NAMED_DATABASE_ID }
    }

    internal fun obtainNamedFirestoreInstance(databaseId: String): FirebaseFirestore? {
      return try {
        val db = FirebaseFirestore.getInstance(databaseId)
        configureOfflinePersistenceSafely(db)
        db
      } catch (e: Throwable) {
        Log.w(TAG, "Falha ao inicializar FirebaseFirestore para o banco $databaseId: ${e.message}")
        null
      }
    }

    private fun configureOfflinePersistenceSafely(db: FirebaseFirestore) {
      try {
        val settings = FirebaseFirestoreSettings.Builder()
          .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
          .build()
        db.firestoreSettings = settings
      } catch (_: IllegalStateException) {
        // Instância já iniciou operações em background; mantém as configurações ativas.
      } catch (e: Throwable) {
        Log.d(TAG, "Configuração de cache persistente ignorada: ${e.message}")
      }
    }

    fun initialize(context: Context): FirestoreHelper {
      val existing = sharedInstance
      if (existing != null && existing.isInitialized) return existing
      synchronized(this) {
        val doubleChecked = sharedInstance
        if (doubleChecked != null && doubleChecked.isInitialized) return doubleChecked
        val helper = FirestoreHelper(context.applicationContext)
        sharedInstance = helper
        return helper
      }
    }

    fun getInstanceOrNull(): FirestoreHelper? = sharedInstance
  }
}
