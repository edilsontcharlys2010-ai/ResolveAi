package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.AppAdminSettings
import com.example.model.AppNotification
import com.example.model.AppSuggestion
import com.example.model.AppTip
import com.example.model.CancellationRefundStatus
import com.example.model.ChatMessage
import com.example.model.ClientCompletedServicePhoto
import com.example.model.CounterOfferEntry
import com.example.model.DocumentReviewAudit
import com.example.model.NotificationType
import com.example.model.PixChargeStatus
import com.example.model.PixRechargeCharge
import com.example.model.Proposal
import com.example.model.ProposalStatus
import com.example.model.ProviderDocuments
import com.example.model.ProviderLocationPoint
import com.example.model.ProviderServicePhoto
import com.example.model.ProviderVerificationStatus
import com.example.model.RegionalPriceEstimate
import com.example.model.ServiceCategoryItem
import com.example.model.ServiceRequest
import com.example.model.ServiceReview
import com.example.model.ServiceStatus
import com.example.model.SuggestionStatus
import com.example.model.TransactionType
import com.example.model.User
import com.example.model.UserRole
import com.example.model.WalletTransaction
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class PixChargeDetectionEvent(
  val txId: String,
  val amount: Double,
  val newBalance: Double?,
  val source: String
)

data class LoginDirectoryEntry(
  val uid: String,
  val authEmail: String,
  val aliasAuthEmail: String? = null,
  val passwordVerifier: String? = null
)

enum class OperationType(val value: String) {
  CREATE("create"),
  UPDATE("update"),
  DELETE("delete"),
  LIST("list"),
  GET("get"),
  WRITE("write")
}

fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
  val causes = generateSequence(exception) { it.cause }
  val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
    ?: causes.firstOrNull {
      it !is CancellationException && it !is java.util.concurrent.CancellationException
    }
    ?: return ""

  if (rootError is CancellationException || rootError is java.util.concurrent.CancellationException) {
    return ""
  }

  val auth = try { FirebaseAuth.getInstance() } catch (e: Throwable) { null }
  val currentUser = auth?.currentUser

  val providerInfoList = currentUser?.providerData?.map { provider ->
    JSONObject().apply {
      put("providerId", provider.providerId)
      put("email", provider.email)
    }
  } ?: emptyList()

  val authInfoJson = JSONObject().apply {
    put("userId", currentUser?.uid)
    put("email", currentUser?.email)
    put("emailVerified", currentUser?.isEmailVerified)
    put("tenantId", currentUser?.tenantId)
    put("providerInfo", JSONArray(providerInfoList))
  }

  val errorInfoJson = JSONObject().apply {
    put("error", rootError.message ?: rootError.toString())
    put("operationType", operationType.value)
    put("path", path)
    put("authInfo", authInfoJson)
  }

  val jsonString = errorInfoJson.toString()
  Log.e("FirestoreError", "Firestore Error: $jsonString")
  return jsonString
}

/**
 * Camada de integração do Firestore estruturada para perfis, análise administrativa de documentos,
 * solicitações de serviço, propostas/contrapropostas e mensagens do chat privado.
 */
class FirestoreMarketplaceDataSource(
  private val firestoreProvider: () -> FirebaseFirestore?
) {

  val db: FirebaseFirestore? get() = firestoreProvider()

  companion object {
    fun fromContext(context: Context): FirestoreMarketplaceDataSource {
      val appContext = context.applicationContext
      val dbId = FirestoreHelper.resolveDatabaseId(appContext)
      return FirestoreMarketplaceDataSource {
        FirestoreHelper.obtainNamedFirestoreInstance(dbId)
      }
    }
  }

  private fun ProviderDocuments.toFirestoreMap(): Map<String, Any> {
    val map = hashMapOf<String, Any>(
      "rgOrCnhNumber" to rgOrCnhNumber,
      "rgOrCnhFileName" to rgOrCnhFileName,
      "criminalRecordCertNumber" to criminalRecordCertNumber,
      "criminalRecordFileName" to criminalRecordFileName,
      "selfieFileName" to selfieFileName,
      "submittedAt" to submittedAt,
      "idDocVerified" to idDocVerified,
      "criminalRecordVerified" to criminalRecordVerified,
      "selfieVerified" to selfieVerified
    )
    rgOrCnhAttachmentUri?.takeIf { it.isNotBlank() }?.let { map["rgOrCnhAttachmentUri"] = it }
    criminalRecordAttachmentUri?.takeIf { it.isNotBlank() }?.let { map["criminalRecordAttachmentUri"] = it }
    selfieAttachmentUri?.takeIf { it.isNotBlank() }?.let { map["selfieAttachmentUri"] = it }
    reviewedAt?.let { map["reviewedAt"] = it }
    rejectionReason?.let { map["rejectionReason"] = it }
    reviewedBy?.let { map["reviewedBy"] = it }
    reviewedByEmail?.let { map["reviewedByEmail"] = it }
    return map
  }

  private fun parseTimestampMillis(raw: Any?): Long? {
    return when (raw) {
      is Long -> raw
      is Int -> raw.toLong()
      is Double -> raw.toLong()
      is Timestamp -> raw.toDate().time
      else -> null
    }
  }

  private fun DocumentSnapshot.toUserOrNull(): User? {
    return try {
      val roleStr = getString("role") ?: UserRole.CLIENTE.name
      val role = try { UserRole.valueOf(roleStr) } catch (_: Exception) { UserRole.CLIENTE }
      val vStatusStr = getString("verificationStatus") ?: ProviderVerificationStatus.APROVADO.name
      val vStatus = when (vStatusStr) {
        "REPROVADO", "REJEITADO" -> ProviderVerificationStatus.REJEITADO
        "PENDENTE_ANALISE" -> ProviderVerificationStatus.PENDENTE_ANALISE
        else -> ProviderVerificationStatus.APROVADO
      }
      val docsMap = get("documents") as? Map<*, *>
      val docs = docsMap?.let { m ->
        ProviderDocuments(
          rgOrCnhNumber = m["rgOrCnhNumber"] as? String ?: "",
          rgOrCnhFileName = m["rgOrCnhFileName"] as? String ?: "cnh_digital_frente_verso.pdf",
          rgOrCnhAttachmentUri = m["rgOrCnhAttachmentUri"] as? String,
          criminalRecordCertNumber = m["criminalRecordCertNumber"] as? String ?: "",
          criminalRecordFileName = m["criminalRecordFileName"] as? String ?: "certidao_antecedentes_criminais.pdf",
          criminalRecordAttachmentUri = m["criminalRecordAttachmentUri"] as? String,
          selfieFileName = m["selfieFileName"] as? String ?: "selfie_com_documento.jpg",
          selfieAttachmentUri = m["selfieAttachmentUri"] as? String,
          submittedAt = parseTimestampMillis(m["submittedAt"]) ?: System.currentTimeMillis(),
          reviewedAt = parseTimestampMillis(m["reviewedAt"]),
          rejectionReason = m["rejectionReason"] as? String,
          reviewedBy = m["reviewedBy"] as? String,
          reviewedByEmail = m["reviewedByEmail"] as? String,
          idDocVerified = m["idDocVerified"] as? Boolean ?: false,
          criminalRecordVerified = m["criminalRecordVerified"] as? Boolean ?: false,
          selfieVerified = m["selfieVerified"] as? Boolean ?: false
        )
      }
      val rawPortfolio = get("portfolioPhotos") as? List<*>
      val parsedPortfolio = rawPortfolio?.mapNotNull { item ->
        val m = item as? Map<*, *> ?: return@mapNotNull null
        val photoUri = (m["photoUri"] as? String)?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        ProviderServicePhoto(
          id = (m["id"] as? String).orEmpty(),
          title = (m["title"] as? String).orEmpty(),
          category = (m["category"] as? String).orEmpty(),
          photoUri = photoUri,
          createdAt = parseTimestampMillis(m["createdAt"]) ?: System.currentTimeMillis()
        )
      }.orEmpty()
      User(
        id = id,
        name = getString("name") ?: "Usuário",
        email = getString("email") ?: "",
        phone = getString("phone") ?: "(11) 90000-0000",
        role = role,
        category = getString("category") ?: "",
        bio = getString("bio") ?: "",
        city = getString("city") ?: "São Paulo, SP",
        document = getString("document") ?: "",
        rating = (get("rating") as? Number)?.toDouble() ?: 5.0,
        ratingCount = (get("ratingCount") as? Number)?.toInt() ?: 0,
        completedJobs = (get("completedJobs") as? Number)?.toInt() ?: 0,
        avatarColorHex = (get("avatarColorHex") as? Number)?.toLong() ?: 0xFF1E3A8A,
        photoUrl = getString("photoUrl"),
        walletBalance = (get("walletBalance") as? Number)?.toDouble() ?: 0.0,
        isPhoneVerified = getBoolean("isPhoneVerified") ?: true,
        verificationStatus = vStatus,
        documents = docs,
        portfolioPhotos = parsedPortfolio,
        firebaseUid = getString("userId") ?: id,
        linkedAuthUids = (get("linkedAuthUids") as? List<*>)?.mapNotNull { it as? String }?.distinct().orEmpty(),
        declinedServiceIds = (get("declinedServiceIds") as? List<*>)?.mapNotNull { it as? String }?.distinct().orEmpty()
      )
    } catch (_: Exception) {
      null
    }
  }

  private fun DocumentSnapshot.toDocumentReviewAuditOrNull(): DocumentReviewAudit? {
    return try {
      val createdMillis = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?.toDate()?.time ?: System.currentTimeMillis()
      DocumentReviewAudit(
        id = id,
        providerId = getString("providerId") ?: "",
        providerName = getString("providerName") ?: "",
        decision = getString("decision") ?: "",
        reason = getString("reason"),
        adminUid = getString("adminUid") ?: "",
        adminEmail = getString("adminEmail") ?: "",
        idDocVerified = getBoolean("idDocVerified") ?: false,
        criminalRecordVerified = getBoolean("criminalRecordVerified") ?: false,
        selfieVerified = getBoolean("selfieVerified") ?: false,
        reviewedAt = createdMillis
      )
    } catch (_: Exception) {
      null
    }
  }

  // 1. Salvar ou atualizar Perfil de Usuário (/users/{userId})
  suspend fun saveUserProfile(user: User, targetDocId: String = user.id): Result<Unit> {
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val path = "users/$targetDocId"
    val docRef = firestore.collection("users").document(targetDocId)

    val safeName = user.name.trim().let { if (it.length >= 2) it else "Usuário ResolveAi" }
    val safePhone = user.phone.trim().let { if (it.length >= 8) it else "(11) 90000-0000" }

    val commonFields = hashMapOf<String, Any>(
      "name" to safeName,
      "email" to user.email.trim(),
      "phone" to safePhone,
      "category" to user.category,
      "bio" to user.bio,
      "city" to user.city.ifBlank { "São Paulo, SP" },
      "document" to user.document,
      "rating" to user.rating,
      "ratingCount" to user.ratingCount,
      "completedJobs" to user.completedJobs,
      "walletBalance" to user.walletBalance,
      "avatarColorHex" to user.avatarColorHex,
      "verificationStatus" to user.verificationStatus.name,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    val cleanPhotoUrl = user.photoUrl?.takeIf { it.isNotBlank() }?.take(250_000)
    if (cleanPhotoUrl != null) {
      commonFields["photoUrl"] = cleanPhotoUrl
    }
    user.documents?.let { commonFields["documents"] = it.toFirestoreMap() }
    if (user.portfolioPhotos.isNotEmpty()) {
      commonFields["portfolioPhotos"] = user.portfolioPhotos.take(6).map { item ->
        mapOf(
          "id" to item.id.take(64),
          "title" to item.title.take(120),
          "category" to item.category.take(100),
          "photoUri" to item.photoUri.take(250_000),
          "createdAt" to item.createdAt
        )
      }
    }
    if (user.declinedServiceIds.isNotEmpty()) {
      commonFields["declinedServiceIds"] = user.declinedServiceIds.distinct().take(500)
    }

    return try {
      firestore.runTransaction { tx ->
        val snap = tx.get(docRef)
        if (!snap.exists()) {
          val createPayload = HashMap(commonFields).apply {
            put("userId", targetDocId)
            put("role", user.role.name)
            put("isPhoneVerified", user.isPhoneVerified)
            put("createdAt", FieldValue.serverTimestamp())
          }
          tx.set(docRef, createPayload)
        } else {
          val updatePayload = HashMap(commonFields)
          if (cleanPhotoUrl == null) {
            updatePayload["photoUrl"] = FieldValue.delete()
          }
          tx.update(docRef, updatePayload)
        }
      }.await()
      Result.success(Unit)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  // 1.0 Carregar Perfil de Usuário (/users/{uid})
  suspend fun loadUserProfile(uid: String): User? {
    if (uid.isBlank() || uid.contains("/")) return null
    val firestore = db ?: return null
    val path = "users/$uid"
    return try {
      val snap = firestore.collection("users").document(uid).get().await()
      if (snap.exists()) snap.toUserOrNull() else null
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.GET, path)
      null
    }
  }

  // 1.0b Observar Perfil de Usuário em Tempo Real (/users/{uid})
  fun observeUserProfile(uid: String): Flow<User?> {
    if (uid.isBlank() || uid.contains("/")) return flowOf(null)
    val firestore = db ?: return flowOf(null)
    val path = "users/$uid"
    return firestore.collection("users")
      .document(uid)
      .snapshots()
      .map { snap -> if (snap.exists()) snap.toUserOrNull() else null }
      .catch { err ->
        val firestoreError = generateSequence(err) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && err is CancellationException) throw err
        handleFirestoreError(err, OperationType.GET, path)
        emit(null)
      }
  }

  // 1.0c Atualização parcial segura no Perfil de Usuário (/users/{userId})
  suspend fun updateUserFields(userId: String, updates: Map<String, Any>): Result<Unit> {
    if (userId.isBlank() || userId.contains("/")) {
      return Result.failure(IllegalArgumentException("ID de usuário inválido"))
    }
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val path = "users/$userId"
    val payload = HashMap(updates).apply {
      remove("userId")
      remove("createdAt")
      put("updatedAt", FieldValue.serverTimestamp())
    }
    return try {
      firestore.collection("users").document(userId).update(payload).await()
      Result.success(Unit)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  // 1.1 Listar Prestadores para Análise do Administrador (/users where role == PRESTADOR)
  suspend fun fetchProvidersForAdmin(): Result<List<User>> {
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val path = "users"
    return try {
      val snapshot = firestore.collection(path)
        .whereEqualTo("role", UserRole.PRESTADOR.name)
        .get()
        .await()
      val providers = snapshot.documents.mapNotNull { it.toUserOrNull() }
      Result.success(providers)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.LIST, path)
      Result.failure(e)
    }
  }

  // 1.1b Listar Todos os Usuários para Gestão Exclusiva do Administrador (/users)
  suspend fun fetchAllUsersForAdmin(): Result<List<User>> {
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val path = "users"
    return try {
      val snapshot = firestore.collection(path)
        .get()
        .await()
      val allUsers = snapshot.documents.mapNotNull { it.toUserOrNull() }
      Result.success(allUsers)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.LIST, path)
      Result.failure(e)
    }
  }

  // 1.1c Atualizar Qualquer Usuário pelo Administrador (inclui role, verificationStatus e saldo)
  suspend fun adminUpdateUserProfile(
    user: User,
    targetDocId: String = user.firebaseUid?.takeIf { it.isNotBlank() } ?: user.id
  ): Result<Unit> {
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val path = "users/$targetDocId"
    val docRef = firestore.collection("users").document(targetDocId)

    val safeName = user.name.trim().let { if (it.length >= 2) it.take(100) else "Usuário ResolveAi" }
    val safePhone = user.phone.trim().let { if (it.length >= 8) it.take(30) else "(11) 90000-0000" }

    val commonFields = hashMapOf<String, Any>(
      "name" to safeName,
      "email" to user.email.trim().take(150),
      "phone" to safePhone,
      "role" to user.role.name,
      "category" to user.category.trim().take(100),
      "bio" to user.bio.trim().take(500),
      "city" to user.city.trim().ifBlank { "São Paulo, SP" }.take(100),
      "document" to user.document.trim().take(50),
      "rating" to user.rating,
      "ratingCount" to user.ratingCount,
      "completedJobs" to user.completedJobs,
      "walletBalance" to user.walletBalance,
      "avatarColorHex" to user.avatarColorHex,
      "isPhoneVerified" to user.isPhoneVerified,
      "verificationStatus" to user.verificationStatus.name,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    user.photoUrl?.takeIf { it.isNotBlank() }?.let { commonFields["photoUrl"] = it.take(250_000) }
    user.documents?.let { commonFields["documents"] = it.toFirestoreMap() }
    if (user.portfolioPhotos.isNotEmpty()) {
      commonFields["portfolioPhotos"] = user.portfolioPhotos.take(6).map { item ->
        mapOf(
          "id" to item.id.take(64),
          "title" to item.title.take(120),
          "category" to item.category.take(100),
          "photoUri" to item.photoUri.take(250_000),
          "createdAt" to item.createdAt
        )
      }
    }
    val cleanedLinkedUids = user.linkedAuthUids
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .distinct()
      .take(20)
    if (cleanedLinkedUids.isNotEmpty()) {
      commonFields["linkedAuthUids"] = cleanedLinkedUids
    }

    return try {
      firestore.runTransaction { tx ->
        val snap = tx.get(docRef)
        if (!snap.exists()) {
          val createPayload = HashMap(commonFields).apply {
            put("userId", targetDocId)
            put("createdAt", FieldValue.serverTimestamp())
          }
          tx.set(docRef, createPayload)
        } else {
          tx.update(docRef, commonFields)
        }
      }.await()
      Result.success(Unit)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  // 1.1d Excluir Usuário pelo Administrador (/users/{targetDocId})
  suspend fun deleteUserInFirestore(targetDocId: String): Result<Unit> {
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val path = "users/$targetDocId"
    return try {
      firestore.collection("users").document(targetDocId).delete().await()
      Result.success(Unit)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  // 1.2 Observar Prestadores em Tempo Real para o Painel Exclusivo do Administrador
  fun observeProvidersForAdmin(): Flow<List<User>> {
    val firestore = db ?: return flowOf(emptyList())
    val path = "users"
    return firestore.collection(path)
      .whereEqualTo("role", UserRole.PRESTADOR.name)
      .snapshots()
      .map { snapshot -> snapshot.documents.mapNotNull { it.toUserOrNull() } }
      .catch { err ->
        val firestoreError = generateSequence(err) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && err is CancellationException) throw err
        handleFirestoreError(err, OperationType.LIST, path)
        emit(emptyList())
      }
  }

  // 1.2b Observar Prestadores Aprovados em Tempo Real para Vitrine na Tela de Pedidos
  fun observeApprovedProviders(): Flow<List<User>> {
    return observeProvidersForAdmin().map { providers ->
      providers.filter { it.verificationStatus == ProviderVerificationStatus.APROVADO }
    }
  }

  // 1.3 Aprovar ou Reprovar Documentos do Prestador Atomicamente no Firestore + Registrar Auditoria Imutável
  suspend fun reviewProviderDocumentsInFirestore(
    provider: User,
    decision: ProviderVerificationStatus,
    reason: String?,
    idDocVerified: Boolean,
    criminalRecordVerified: Boolean,
    selfieVerified: Boolean,
    adminUid: String,
    adminEmail: String
  ): Result<DocumentReviewAudit> {
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val providerDocId = provider.id
    val providerRef = firestore.collection("users").document(providerDocId)
    val reviewId = "rev_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val reviewRef = providerRef.collection("documentReviews").document(reviewId)
    val nowMillis = System.currentTimeMillis()
    val cleanReason = reason?.trim()?.takeIf { it.isNotBlank() }

    val baseDocs = provider.documents ?: ProviderDocuments(
      rgOrCnhNumber = provider.document.ifBlank { "CNH Pendente" },
      criminalRecordCertNumber = "CERT-PF-VALIDACAO",
      selfieFileName = "selfie_com_documento.jpg"
    )
    val updatedDocs = baseDocs.copy(
      reviewedAt = nowMillis,
      rejectionReason = if (decision == ProviderVerificationStatus.REJEITADO) cleanReason else null,
      reviewedBy = adminUid,
      reviewedByEmail = adminEmail.takeIf { it.isNotBlank() },
      idDocVerified = idDocVerified,
      criminalRecordVerified = criminalRecordVerified,
      selfieVerified = selfieVerified
    )

    val auditPayload = hashMapOf<String, Any>(
      "providerId" to providerDocId,
      "providerName" to provider.name,
      "decision" to decision.name,
      "adminUid" to adminUid,
      "adminEmail" to adminEmail,
      "idDocVerified" to idDocVerified,
      "criminalRecordVerified" to criminalRecordVerified,
      "selfieVerified" to selfieVerified,
      "createdAt" to FieldValue.serverTimestamp()
    )
    cleanReason?.let { auditPayload["reason"] = it }

    return try {
      firestore.runTransaction { tx ->
        val snap = tx.get(providerRef)
        if (!snap.exists()) {
          val fullUserPayload = hashMapOf<String, Any>(
            "userId" to providerDocId,
            "name" to provider.name.ifBlank { "Prestador" },
            "email" to provider.email,
            "phone" to provider.phone.ifBlank { "(11) 90000-0000" },
            "role" to UserRole.PRESTADOR.name,
            "category" to provider.category,
            "bio" to provider.bio,
            "city" to provider.city.ifBlank { "São Paulo, SP" },
            "document" to provider.document,
            "rating" to provider.rating,
            "ratingCount" to provider.ratingCount,
            "completedJobs" to provider.completedJobs,
            "walletBalance" to provider.walletBalance,
            "isPhoneVerified" to provider.isPhoneVerified,
            "verificationStatus" to decision.name,
            "documents" to updatedDocs.toFirestoreMap(),
            "avatarColorHex" to provider.avatarColorHex,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
          tx.set(providerRef, fullUserPayload)
        } else {
          tx.update(
            providerRef,
            mapOf(
              "verificationStatus" to decision.name,
              "documents" to updatedDocs.toFirestoreMap(),
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        }

        tx.set(reviewRef, auditPayload)
      }.await()

      Result.success(
        DocumentReviewAudit(
          id = reviewId,
          providerId = providerDocId,
          providerName = provider.name,
          decision = decision.name,
          reason = cleanReason,
          adminUid = adminUid,
          adminEmail = adminEmail,
          idDocVerified = idDocVerified,
          criminalRecordVerified = criminalRecordVerified,
          selfieVerified = selfieVerified,
          reviewedAt = nowMillis
        )
      )
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.WRITE, "users/$providerDocId/documentReviews/$reviewId")
      Result.failure(e)
    }
  }

  // 1.4 Observar Histórico de Análises de Documentos (/users/{providerId}/documentReviews)
  fun observeProviderReviewHistory(providerId: String): Flow<List<DocumentReviewAudit>> {
    val firestore = db ?: return flowOf(emptyList())
    val path = "users/$providerId/documentReviews"
    return firestore.collection("users")
      .document(providerId)
      .collection("documentReviews")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snapshot -> snapshot.documents.mapNotNull { it.toDocumentReviewAuditOrNull() } }
      .catch { err ->
        handleFirestoreError(err, OperationType.LIST, path)
        emit(emptyList())
      }
  }

  // 2. Publicar Solicitação de Serviço (/services/{serviceId})
  suspend fun publishServiceRequest(request: ServiceRequest): Boolean {
    val firestore = db ?: return false
    val path = "services/${request.id}"
    val safeClientName = request.clientName.trim().let { if (it.length >= 2) it.take(100) else "Cliente ResolveAi" }
    val safeTitle = request.title.trim().let { if (it.length >= 3) it.take(200) else "Serviço Solicitado" }
    val payload = hashMapOf<String, Any>(
      "clientId" to request.clientId,
      "clientName" to safeClientName,
      "clientRating" to request.clientRating,
      "clientAvatarColorHex" to request.clientAvatarColorHex,
      "title" to safeTitle,
      "category" to request.category.take(100),
      "description" to request.description.take(2000),
      "location" to request.location.take(300),
      "proposedBudget" to request.proposedBudget.coerceAtLeast(0.0),
      "urgency" to request.urgency.take(50),
      "status" to request.status.name,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )

    request.clientPhotoUrl?.trim()?.takeIf { it.isNotBlank() }?.take(250_000)?.let { payload["clientPhotoUrl"] = it }
    request.finalAgreedPrice?.let { payload["finalAgreedPrice"] = it }
    request.platformFee?.let { payload["platformFee"] = it }
    request.acceptedProviderId?.let {
      payload["assignedProviderId"] = it
      payload["acceptedProviderId"] = it
    }
    request.acceptedProviderName?.let {
      payload["assignedProviderName"] = it.take(100)
      payload["acceptedProviderName"] = it.take(100)
    }
    request.acceptedProviderRating?.let { payload["acceptedProviderRating"] = it }
    request.acceptedProviderAvatarColorHex?.let { payload["acceptedProviderAvatarColorHex"] = it }
    request.acceptedProviderPhotoUrl?.trim()?.takeIf { it.isNotBlank() }?.take(250_000)?.let { payload["acceptedProviderPhotoUrl"] = it }
    request.cancellationReason?.trim()?.takeIf { it.isNotBlank() }?.take(500)?.let { payload["cancellationReason"] = it }
    request.cancelledByRole?.let { payload["cancelledByRole"] = it.name }
    request.cancelledByName?.trim()?.takeIf { it.isNotBlank() }?.take(100)?.let { payload["cancelledByName"] = it }
    request.cancelledAt?.let { payload["cancelledAt"] = it }
    request.cancellationRefundStatus?.let { payload["cancellationRefundStatus"] = it.name }
    request.cancellationRefundReviewedBy?.trim()?.takeIf { it.isNotBlank() }?.take(150)?.let { payload["cancellationRefundReviewedBy"] = it }
    request.cancellationRefundReviewedAt?.let { payload["cancellationRefundReviewedAt"] = it }
    request.cancellationRefundNote?.trim()?.takeIf { it.isNotBlank() }?.take(500)?.let { payload["cancellationRefundNote"] = it }
    payload["locationTrackingActive"] = request.locationTrackingActive
    payload["locationPointsCount"] = request.locationPointsCount
    if (request.clientCompletionPhotos.isNotEmpty()) {
      payload["clientCompletionPhotos"] = request.clientCompletionPhotos.take(6).map { p ->
        mapOf(
          "id" to p.id.take(100),
          "photoUri" to p.photoUri.take(250_000),
          "photoName" to p.photoName.take(300),
          "caption" to p.caption.take(300),
          "addedAt" to p.addedAt
        )
      }
    }
    request.attachmentPhotoUri?.trim()?.takeIf { it.isNotBlank() }?.take(250_000)?.let { payload["attachmentPhotoUri"] = it }
    request.attachmentPhotoName?.trim()?.takeIf { it.isNotBlank() }?.take(300)?.let { payload["attachmentPhotoName"] = it }
    request.attachmentVideoUri?.trim()?.takeIf { it.isNotBlank() }?.take(2000)?.let { payload["attachmentVideoUri"] = it }
    request.attachmentVideoName?.trim()?.takeIf { it.isNotBlank() }?.take(300)?.let { payload["attachmentVideoName"] = it }
    request.attachmentVideoThumbnailUri?.trim()?.takeIf { it.isNotBlank() }?.take(250_000)?.let { payload["attachmentVideoThumbnailUri"] = it }
    request.attachmentVideoDurationMs?.let { payload["attachmentVideoDurationMs"] = it }
    request.completionPhotoUri?.trim()?.takeIf { it.isNotBlank() }?.take(250_000)?.let { payload["completionPhotoUri"] = it }
    request.completionPhotoName?.trim()?.takeIf { it.isNotBlank() }?.take(300)?.let { payload["completionPhotoName"] = it }
    request.completionVideoUri?.trim()?.takeIf { it.isNotBlank() }?.take(2000)?.let { payload["completionVideoUri"] = it }
    request.completionVideoName?.trim()?.takeIf { it.isNotBlank() }?.take(300)?.let { payload["completionVideoName"] = it }
    request.completionVideoThumbnailUri?.trim()?.takeIf { it.isNotBlank() }?.take(250_000)?.let { payload["completionVideoThumbnailUri"] = it }
    request.completionVideoDurationMs?.let { payload["completionVideoDurationMs"] = it }
    request.completionNotes?.trim()?.takeIf { it.isNotBlank() }?.take(500)?.let { payload["completionNotes"] = it }
    request.completedAt?.let { payload["completedAt"] = it }
    request.ratingGiven?.let { payload["ratingGiven"] = it.toDouble().coerceIn(1.0, 5.0) }
    request.reviewComment?.trim()?.takeIf { it.isNotBlank() }?.take(1000)?.let { payload["reviewComment"] = it }
    if (request.reviewTags.isNotEmpty()) {
      payload["reviewTags"] = request.reviewTags.take(10).map { it.take(60) }
    }
    request.reviewedAt?.let { payload["reviewedAt"] = it }

    return try {
      val docRef = firestore.collection("services").document(request.id)
      val existingSnap = try { docRef.get().await() } catch (_: Exception) { null }
      if (existingSnap != null && existingSnap.exists()) {
        val updatePayload = HashMap(payload).apply {
          remove("clientId")
          remove("createdAt")
          remove("locationPointsCount")
          remove("cancellationRefundReviewedBy")
          remove("cancellationRefundReviewedAt")
          remove("cancellationRefundNote")
          if (request.status != ServiceStatus.CONCLUIDO || existingSnap.get("ratingGiven") != null) {
            remove("ratingGiven")
          }
          put("updatedAt", FieldValue.serverTimestamp())
        }
        docRef.update(updatePayload).await()
      } else {
        docRef.set(payload).await()
      }
      true
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.WRITE, path)
      false
    }
  }

  // 3. Atualizar Orçamento Proposto pelo Cliente
  suspend fun updateServiceBudget(serviceId: String, newBudget: Double): Boolean {
    val firestore = db ?: return false
    val path = "services/$serviceId"
    return try {
      firestore.collection("services").document(serviceId).update(
        mapOf(
          "proposedBudget" to newBudget,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      true
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.UPDATE, path)
      false
    }
  }

  // 4. Submeter Proposta ou Contraproposta (/proposals/{proposalId})
  suspend fun submitProposal(proposal: Proposal, clientId: String): Boolean {
    val firestore = db ?: return false
    val safeProposalId = proposal.id.trim()
    val safeServiceId = proposal.serviceId.trim().take(128)
    val safeClientId = clientId.trim().take(128)
    val safeProviderId = proposal.providerId.trim().take(128)
    if (safeProposalId.isBlank() || safeServiceId.isBlank() || safeClientId.isBlank() || safeProviderId.isBlank()) {
      return false
    }
    val path = "proposals/$safeProposalId"
    val counterList = proposal.counterOffers.takeLast(50).map { entry ->
      mapOf(
        "price" to entry.price.coerceAtLeast(0.0),
        "authorId" to entry.authorId.trim().take(128),
        "authorName" to entry.authorName.trim().ifBlank { "Usuário" }.take(100),
        "authorRole" to entry.authorRole.name,
        "note" to entry.note.trim().take(500),
        "timestamp" to entry.timestamp
      )
    }

    val safeProviderName = proposal.providerName.trim().ifBlank { "Prestador" }.take(100)
    val safeProviderCategory = proposal.providerCategory.trim().take(300)
    val safeDescription = proposal.description.trim().take(2000)
    val safeProposedPrice = proposal.proposedPrice.coerceAtLeast(0.0)
    val safeEstimatedDays = proposal.estimatedDays.coerceAtLeast(1)
    val safePhotoUrl = proposal.providerPhotoUrl?.trim()?.takeIf { it.isNotBlank() && it.length <= 250_000 }

    val mutableFields = hashMapOf<String, Any>(
      "providerName" to safeProviderName,
      "providerRating" to proposal.providerRating,
      "providerCategory" to safeProviderCategory,
      "providerAvatarColorHex" to proposal.providerAvatarColorHex,
      "proposedPrice" to safeProposedPrice,
      "estimatedDays" to safeEstimatedDays,
      "description" to safeDescription,
      "status" to proposal.status.name,
      "isDirectAcceptance" to proposal.isDirectAcceptance,
      "counterOffers" to counterList,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    safePhotoUrl?.let { mutableFields["providerPhotoUrl"] = it }

    val docRef = firestore.collection("proposals").document(safeProposalId)
    return try {
      val existingSnap = try {
        docRef.get().await()
      } catch (_: Exception) {
        null
      }
      if (existingSnap != null && existingSnap.exists()) {
        // Documento já existe: usa update apenas nos campos mutáveis sem alterar createdAt/clientId/providerId
        docRef.update(mutableFields).await()
      } else {
        val createPayload = HashMap<String, Any>(mutableFields).apply {
          put("serviceId", safeServiceId)
          put("clientId", safeClientId)
          put("providerId", safeProviderId)
          put("createdAt", FieldValue.serverTimestamp())
        }
        docRef.set(createPayload).await()
      }
      true
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.CREATE, path)
      false
    }
  }

  // 5. Enviar Mensagem no Chat Privado (/services/{serviceId}/messages/{messageId})
  suspend fun sendChatMessage(serviceId: String, message: ChatMessage): Boolean {
    return sendChatMessageWithDiagnostics(serviceId = serviceId, message = message, attempt = 1).delivered
  }

  suspend fun sendChatMessageWithDiagnostics(
    serviceId: String,
    message: ChatMessage,
    attempt: Int = 1
  ): ChatSendOutcome {
    val startMs = System.currentTimeMillis()
    val safeServiceId = serviceId.trim()
    val firestore = db
    if (firestore == null) {
      ChatDiagnosticLogger.record(
        serviceId = safeServiceId.ifBlank { "unknown" },
        category = ChatDiagnosticCategory.SEND_AUTH_MISSING,
        severity = ChatDiagnosticSeverity.ERROR,
        firestoreCode = "FIRESTORE_NOT_INITIALIZED",
        summary = "Firestore não inicializado ao enviar mensagem (tentativa #$attempt).",
        details = mapOf("attempt" to attempt, "messageId" to message.id)
      )
      return ChatSendOutcome(
        delivered = false,
        failureKind = ChatFailureKind.OTHER,
        firestoreCode = "FIRESTORE_NOT_INITIALIZED",
        errorMessage = "Firestore não inicializado"
      )
    }
    if (safeServiceId.isBlank() || safeServiceId.contains("/")) {
      ChatDiagnosticLogger.record(
        serviceId = safeServiceId.ifBlank { "blank" },
        category = ChatDiagnosticCategory.SEND_ERROR,
        severity = ChatDiagnosticSeverity.ERROR,
        firestoreCode = "INVALID_SERVICE_ID",
        summary = "ID do serviço inválido ao enviar mensagem.",
        details = mapOf("attempt" to attempt, "rawServiceId" to serviceId)
      )
      return ChatSendOutcome(
        delivered = false,
        failureKind = ChatFailureKind.OTHER,
        firestoreCode = "INVALID_SERVICE_ID",
        errorMessage = "ID de serviço inválido"
      )
    }
    val safeMessageId = message.id
      .trim()
      .replace(Regex("[^a-zA-Z0-9_\\-]"), "")
      .ifBlank { "msg_${java.util.UUID.randomUUID().toString().replace("-", "").take(12)}" }
      .take(128)
    val path = "services/$safeServiceId/messages/$safeMessageId"
    val safeContent = message.content.trim().take(2000)
    if (safeContent.isBlank()) {
      return ChatSendOutcome(
        delivered = false,
        failureKind = ChatFailureKind.OTHER,
        firestoreCode = "EMPTY_CONTENT",
        errorMessage = "Mensagem vazia"
      )
    }
    val currentAuthUid = try { FirebaseAuth.getInstance().currentUser?.uid } catch (_: Throwable) { null }
    if (currentAuthUid.isNullOrBlank()) {
      ChatDiagnosticLogger.record(
        serviceId = safeServiceId,
        category = ChatDiagnosticCategory.SEND_AUTH_MISSING,
        severity = ChatDiagnosticSeverity.ERROR,
        firestoreCode = "UNAUTHENTICATED",
        summary = "FirebaseAuth.currentUser está nulo ao tentar enviar mensagem (tentativa #$attempt).",
        details = mapOf("attempt" to attempt, "path" to path, "messageId" to safeMessageId)
      )
      return ChatSendOutcome(
        delivered = false,
        failureKind = ChatFailureKind.UNAUTHENTICATED,
        firestoreCode = "UNAUTHENTICATED",
        errorMessage = "Sessão Firebase Auth ausente"
      )
    }

    val resolvedRole = if (message.isSystemNotice) "SISTEMA" else message.senderRole.name
    val rawSenderId = message.senderId.trim()
    val safeSenderId = when {
      message.isSystemNotice -> "system"
      rawSenderId.isBlank() || rawSenderId.startsWith("cli_") || rawSenderId.startsWith("pro_") || rawSenderId.startsWith("u_") ->
        currentAuthUid.take(128)
      else -> rawSenderId.take(128)
    }
    val payload = hashMapOf<String, Any>(
      "serviceId" to safeServiceId,
      "senderId" to safeSenderId,
      "senderName" to message.senderName.trim().ifBlank { "Usuário" }.take(100),
      "senderRole" to resolvedRole,
      "content" to safeContent,
      "isSystemNotice" to message.isSystemNotice,
      "createdAt" to FieldValue.serverTimestamp()
    )

    ChatDiagnosticLogger.record(
      serviceId = safeServiceId,
      category = ChatDiagnosticCategory.SEND_ATTEMPT,
      severity = ChatDiagnosticSeverity.INFO,
      summary = "Enviando mensagem #$attempt ($resolvedRole) para Firestore.",
      details = mapOf(
        "attempt" to attempt,
        "path" to path,
        "messageId" to safeMessageId,
        "senderId" to safeSenderId,
        "senderRole" to resolvedRole,
        "isSystemNotice" to message.isSystemNotice,
        "contentLength" to safeContent.length
      )
    )

    return try {
      withTimeout(ChatDiagnosticLogger.SEND_WRITE_TIMEOUT_MS) {
        firestore.collection("services").document(safeServiceId)
          .collection("messages").document(safeMessageId)
          .set(payload).await()
      }
      val elapsed = System.currentTimeMillis() - startMs
      ChatDiagnosticLogger.record(
        serviceId = safeServiceId,
        category = ChatDiagnosticCategory.SEND_SUCCESS,
        severity = ChatDiagnosticSeverity.INFO,
        firestoreCode = "OK",
        summary = "Mensagem entregue no Firestore em ${elapsed}ms (tentativa #$attempt).",
        details = mapOf(
          "attempt" to attempt,
          "path" to path,
          "messageId" to safeMessageId,
          "durationMs" to elapsed
        )
      )
      ChatSendOutcome(delivered = true, firestoreCode = "OK", durationMs = elapsed)
    } catch (e: TimeoutCancellationException) {
      val elapsed = System.currentTimeMillis() - startMs
      handleFirestoreError(e, OperationType.CREATE, path)
      ChatDiagnosticLogger.record(
        serviceId = safeServiceId,
        category = ChatDiagnosticCategory.SEND_NETWORK_TIMEOUT,
        severity = ChatDiagnosticSeverity.ERROR,
        firestoreCode = "CLIENT_WRITE_TIMEOUT_15S",
        summary = "Timeout de rede (${elapsed}ms) aguardando confirmação do Firestore no envio #$attempt.",
        details = mapOf(
          "attempt" to attempt,
          "path" to path,
          "messageId" to safeMessageId,
          "senderId" to safeSenderId,
          "durationMs" to elapsed,
          "errorMessage" to (e.message ?: "Timeout after ${ChatDiagnosticLogger.SEND_WRITE_TIMEOUT_MS}ms")
        )
      )
      ChatSendOutcome(
        delivered = false,
        failureKind = ChatFailureKind.NETWORK_TIMEOUT,
        firestoreCode = "CLIENT_WRITE_TIMEOUT_15S",
        errorMessage = e.message ?: "Timeout de escrita no Firestore",
        durationMs = elapsed
      )
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      val elapsed = System.currentTimeMillis() - startMs
      handleFirestoreError(e, OperationType.CREATE, path)
      val classified = ChatDiagnosticLogger.classifyFirestoreFailure(e)
      val diagCategory = when (classified.kind) {
        ChatFailureKind.PERMISSION_DENIED -> ChatDiagnosticCategory.SEND_PERMISSION_DENIED
        ChatFailureKind.NETWORK_TIMEOUT -> ChatDiagnosticCategory.SEND_NETWORK_TIMEOUT
        ChatFailureKind.UNAUTHENTICATED -> ChatDiagnosticCategory.SEND_AUTH_MISSING
        ChatFailureKind.OTHER -> ChatDiagnosticCategory.SEND_ERROR
      }
      val diagSummary = when (classified.kind) {
        ChatFailureKind.PERMISSION_DENIED ->
          "PERMISSION_DENIED nas regras do Firestore ao enviar mensagem (tentativa #$attempt)."
        ChatFailureKind.NETWORK_TIMEOUT ->
          "Falha de rede/timeout (${classified.firestoreCode}) ao enviar mensagem (tentativa #$attempt)."
        ChatFailureKind.UNAUTHENTICATED ->
          "Sessão Firebase não autenticada (${classified.firestoreCode}) ao enviar mensagem."
        ChatFailureKind.OTHER ->
          "Erro Firestore (${classified.firestoreCode}) ao enviar mensagem (tentativa #$attempt)."
      }
      ChatDiagnosticLogger.record(
        serviceId = safeServiceId,
        category = diagCategory,
        severity = ChatDiagnosticSeverity.ERROR,
        firestoreCode = classified.firestoreCode,
        summary = diagSummary,
        details = mapOf(
          "attempt" to attempt,
          "path" to path,
          "messageId" to safeMessageId,
          "senderId" to safeSenderId,
          "senderRole" to resolvedRole,
          "durationMs" to elapsed,
          "errorMessage" to classified.message
        )
      )
      ChatSendOutcome(
        delivered = false,
        failureKind = classified.kind,
        firestoreCode = classified.firestoreCode,
        errorMessage = classified.message,
        durationMs = elapsed
      )
    }
  }

  private fun DocumentSnapshot.toChatMessageOrNull(): ChatMessage? {
    return try {
      val content = getString("content") ?: return null
      val createdMillis = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?.toDate()?.time
        ?: parseTimestampMillis(get("createdAt"))
        ?: System.currentTimeMillis()
      val rawRole = getString("senderRole") ?: UserRole.CLIENTE.name
      val isSys = (getBoolean("isSystemNotice") == true) || rawRole == "SISTEMA"
      val role = when (rawRole) {
        "PRESTADOR" -> UserRole.PRESTADOR
        "ADMIN" -> UserRole.ADMIN
        else -> UserRole.CLIENTE
      }
      ChatMessage(
        id = id,
        serviceId = getString("serviceId") ?: reference.parent.parent?.id ?: "",
        senderId = getString("senderId") ?: "",
        senderName = getString("senderName") ?: "Usuário",
        senderRole = role,
        content = content,
        timestamp = createdMillis,
        isSystemNotice = isSys
      )
    } catch (_: Exception) {
      null
    }
  }

  // 5.1 Observar Mensagens do Chat Privado em Tempo Real (/services/{serviceId}/messages) com Diagnóstico
  fun observeChatMessages(serviceId: String): Flow<List<ChatMessage>> {
    if (serviceId.isBlank() || serviceId.contains("/")) return flowOf(emptyList())
    val firestore = db ?: return flowOf(emptyList())
    val path = "services/$serviceId/messages"
    return callbackFlow {
      val attachedAtMs = System.currentTimeMillis()
      var receivedServerSnapshot = false
      var lastLoggedCount = -1
      var lastFromCache: Boolean? = null
      var lastHasPendingWrites: Boolean? = null

      ChatDiagnosticLogger.record(
        serviceId = serviceId,
        category = ChatDiagnosticCategory.LISTENER_ATTACHED,
        severity = ChatDiagnosticSeverity.INFO,
        summary = "Listener em tempo real anexado em $path.",
        details = mapOf("path" to path)
      )

      val serverSyncWatchdog = launch {
        delay(ChatDiagnosticLogger.LISTENER_SERVER_SYNC_TIMEOUT_MS)
        if (!receivedServerSnapshot) {
          val elapsed = System.currentTimeMillis() - attachedAtMs
          ChatDiagnosticLogger.record(
            serviceId = serviceId,
            category = ChatDiagnosticCategory.LISTENER_NETWORK_TIMEOUT,
            severity = ChatDiagnosticSeverity.WARNING,
            firestoreCode = "SERVER_SYNC_TIMEOUT_8S",
            summary = "Listener sem resposta do servidor após ${elapsed}ms (possível lentidão/rede offline ou cache pendente).",
            details = mapOf(
              "path" to path,
              "elapsedMs" to elapsed,
              "lastFromCache" to lastFromCache,
              "lastHasPendingWrites" to lastHasPendingWrites,
              "lastDocCount" to lastLoggedCount
            )
          )
        }
      }

      val registration = firestore.collection("services")
        .document(serviceId)
        .collection("messages")
        .orderBy("createdAt", Query.Direction.ASCENDING)
        .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
          if (error != null) {
            serverSyncWatchdog.cancel()
            handleFirestoreError(error, OperationType.LIST, path)
            val classified = ChatDiagnosticLogger.classifyFirestoreFailure(error)
            val diagCategory = when (classified.kind) {
              ChatFailureKind.PERMISSION_DENIED -> ChatDiagnosticCategory.LISTENER_PERMISSION_DENIED
              ChatFailureKind.NETWORK_TIMEOUT -> ChatDiagnosticCategory.LISTENER_NETWORK_TIMEOUT
              else -> ChatDiagnosticCategory.LISTENER_ERROR
            }
            val diagSummary = when (classified.kind) {
              ChatFailureKind.PERMISSION_DENIED ->
                "PERMISSION_DENIED no listener do chat: o usuário atual não foi aceito como participante em $path."
              ChatFailureKind.NETWORK_TIMEOUT ->
                "Timeout/queda de rede (${classified.firestoreCode}) no listener do chat em $path."
              ChatFailureKind.UNAUTHENTICATED ->
                "Sessão expirada/não autenticada (${classified.firestoreCode}) no listener do chat."
              ChatFailureKind.OTHER ->
                "Erro (${classified.firestoreCode}) no listener do chat em $path."
            }
            ChatDiagnosticLogger.record(
              serviceId = serviceId,
              category = diagCategory,
              severity = ChatDiagnosticSeverity.ERROR,
              firestoreCode = classified.firestoreCode,
              summary = diagSummary,
              details = mapOf(
                "path" to path,
                "elapsedMs" to (System.currentTimeMillis() - attachedAtMs),
                "errorMessage" to classified.message
              )
            )
            trySend(emptyList())
            close()
            return@addSnapshotListener
          }

          if (snapshot != null) {
            val fromCache = snapshot.metadata.isFromCache
            val hasPendingWrites = snapshot.metadata.hasPendingWrites()
            val messages = snapshot.documents.mapNotNull { it.toChatMessageOrNull() }
            if (!fromCache) {
              receivedServerSnapshot = true
              serverSyncWatchdog.cancel()
            }
            if (messages.size != lastLoggedCount || fromCache != lastFromCache || hasPendingWrites != lastHasPendingWrites) {
              val sourceLabel = if (fromCache) "CACHE LOCAL" else "SERVIDOR"
              val pendingLabel = if (hasPendingWrites) " • escritas pendentes" else ""
              ChatDiagnosticLogger.record(
                serviceId = serviceId,
                category = ChatDiagnosticCategory.LISTENER_SNAPSHOT,
                severity = if (fromCache && hasPendingWrites) ChatDiagnosticSeverity.WARNING else ChatDiagnosticSeverity.INFO,
                firestoreCode = if (fromCache) "FROM_CACHE" else "FROM_SERVER",
                summary = "Snapshot ($sourceLabel$pendingLabel): ${messages.size} msg(s).",
                details = mapOf(
                  "path" to path,
                  "docCount" to messages.size,
                  "fromCache" to fromCache,
                  "hasPendingWrites" to hasPendingWrites,
                  "elapsedMs" to (System.currentTimeMillis() - attachedAtMs)
                )
              )
              lastLoggedCount = messages.size
              lastFromCache = fromCache
              lastHasPendingWrites = hasPendingWrites
            }
            trySend(messages)
          }
        }

      awaitClose {
        serverSyncWatchdog.cancel()
        registration.remove()
      }
    }
  }

  // 5.2 Buscar Histórico de Mensagens do Chat Privado (/services/{serviceId}/messages)
  suspend fun fetchChatMessages(serviceId: String): Result<List<ChatMessage>> {
    if (serviceId.isBlank() || serviceId.contains("/")) {
      return Result.failure(IllegalArgumentException("ID de serviço inválido"))
    }
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val path = "services/$serviceId/messages"
    return try {
      val snapshot = firestore.collection("services")
        .document(serviceId)
        .collection("messages")
        .orderBy("createdAt", Query.Direction.ASCENDING)
        .get()
        .await()
      Result.success(snapshot.documents.mapNotNull { it.toChatMessageOrNull() })
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.LIST, path)
      Result.failure(e)
    }
  }

  private fun DocumentSnapshot.toServiceRequestOrNull(): ServiceRequest? {
    return try {
      val createdMillis = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?.toDate()?.time
        ?: parseTimestampMillis(get("createdAt"))
        ?: System.currentTimeMillis()
      val statusStr = getString("status") ?: ServiceStatus.ABERTO.name
      val parsedStatus = when (statusStr) {
        "EM_ANDAMENTO" -> ServiceStatus.EM_EXECUCAO
        else -> try { ServiceStatus.valueOf(statusStr) } catch (_: Exception) { ServiceStatus.ABERTO }
      }
      val rawClientPhotos = get("clientCompletionPhotos") as? List<*> ?: emptyList<Any>()
      val clientPhotos = rawClientPhotos.mapNotNull { item ->
        val map = item as? Map<*, *> ?: return@mapNotNull null
        val photoUri = map["photoUri"] as? String ?: return@mapNotNull null
        ClientCompletedServicePhoto(
          id = (map["id"] as? String).orEmpty(),
          photoUri = photoUri,
          photoName = (map["photoName"] as? String).orEmpty(),
          caption = (map["caption"] as? String).orEmpty(),
          addedAt = (map["addedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
      }
      ServiceRequest(
        id = id,
        clientId = getString("clientId") ?: "",
        clientName = getString("clientName") ?: "Cliente",
        clientRating = (get("clientRating") as? Number)?.toDouble() ?: 5.0,
        clientAvatarColorHex = (get("clientAvatarColorHex") as? Number)?.toLong() ?: 0xFF2563EB,
        clientPhotoUrl = getString("clientPhotoUrl"),
        title = getString("title") ?: "",
        category = getString("category") ?: "",
        description = getString("description") ?: "",
        location = getString("location") ?: "São Paulo, SP",
        proposedBudget = (get("proposedBudget") as? Number)?.toDouble() ?: 0.0,
        finalAgreedPrice = (get("finalAgreedPrice") as? Number)?.toDouble(),
        platformFee = (get("platformFee") as? Number)?.toDouble(),
        acceptedProviderId = getString("assignedProviderId") ?: getString("acceptedProviderId"),
        acceptedProviderName = getString("assignedProviderName") ?: getString("acceptedProviderName"),
        acceptedProviderRating = (get("acceptedProviderRating") as? Number)?.toDouble(),
        acceptedProviderAvatarColorHex = (get("acceptedProviderAvatarColorHex") as? Number)?.toLong(),
        acceptedProviderPhotoUrl = getString("acceptedProviderPhotoUrl"),
        status = parsedStatus,
        urgency = getString("urgency") ?: "Média",
        createdAt = createdMillis,
        ratingGiven = (get("ratingGiven") as? Number)?.toFloat(),
        reviewComment = getString("reviewComment"),
        reviewTags = (get("reviewTags") as? List<*>)
          ?.mapNotNull { (it as? String)?.trim()?.takeIf { tag -> tag.isNotBlank() } }
          ?: emptyList(),
        reviewedAt = getTimestamp("reviewedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          ?.toDate()?.time
          ?: parseTimestampMillis(get("reviewedAt")),
        cancellationReason = getString("cancellationReason"),
        cancelledByRole = getString("cancelledByRole")?.let { rawRole ->
          try { UserRole.valueOf(rawRole) } catch (_: Exception) { null }
        },
        cancelledByName = getString("cancelledByName"),
        cancelledAt = parseTimestampMillis(get("cancelledAt")),
        attachmentPhotoUri = getString("attachmentPhotoUri"),
        attachmentPhotoName = getString("attachmentPhotoName"),
        attachmentVideoUri = getString("attachmentVideoUri"),
        attachmentVideoName = getString("attachmentVideoName"),
        attachmentVideoThumbnailUri = getString("attachmentVideoThumbnailUri"),
        attachmentVideoDurationMs = (get("attachmentVideoDurationMs") as? Number)?.toLong(),
        completionPhotoUri = getString("completionPhotoUri"),
        completionPhotoName = getString("completionPhotoName"),
        completionVideoUri = getString("completionVideoUri"),
        completionVideoName = getString("completionVideoName"),
        completionVideoThumbnailUri = getString("completionVideoThumbnailUri"),
        completionVideoDurationMs = (get("completionVideoDurationMs") as? Number)?.toLong(),
        completionNotes = getString("completionNotes"),
        completedAt = parseTimestampMillis(get("completedAt")),
        cancellationRefundStatus = getString("cancellationRefundStatus")?.let { raw ->
          try { CancellationRefundStatus.valueOf(raw) } catch (_: Exception) { null }
        },
        cancellationRefundReviewedBy = getString("cancellationRefundReviewedBy"),
        cancellationRefundReviewedAt = parseTimestampMillis(get("cancellationRefundReviewedAt")),
        cancellationRefundNote = getString("cancellationRefundNote"),
        locationTrackingActive = getBoolean("locationTrackingActive") == true,
        locationPointsCount = (get("locationPointsCount") as? Number)?.toInt() ?: 0,
        clientCompletionPhotos = clientPhotos
      )
    } catch (_: Exception) {
      null
    }
  }

  private fun DocumentSnapshot.toProposalOrNull(): Proposal? {
    return try {
      val createdMillis = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?.toDate()?.time
        ?: parseTimestampMillis(get("createdAt"))
        ?: System.currentTimeMillis()
      val rawOffers = get("counterOffers") as? List<*> ?: emptyList<Any>()
      val offers = rawOffers.mapNotNull { item ->
        val map = item as? Map<*, *> ?: return@mapNotNull null
        val role = try {
          UserRole.valueOf(map["authorRole"] as? String ?: UserRole.CLIENTE.name)
        } catch (_: Exception) {
          UserRole.CLIENTE
        }
        CounterOfferEntry(
          price = (map["price"] as? Number)?.toDouble() ?: 0.0,
          authorId = map["authorId"] as? String ?: "",
          authorName = map["authorName"] as? String ?: "",
          authorRole = role,
          note = map["note"] as? String ?: "",
          timestamp = parseTimestampMillis(map["timestamp"]) ?: createdMillis
        )
      }
      Proposal(
        id = id,
        serviceId = getString("serviceId") ?: "",
        providerId = getString("providerId") ?: "",
        providerName = getString("providerName") ?: "Prestador",
        providerRating = (get("providerRating") as? Number)?.toDouble() ?: 4.9,
        providerCategory = getString("providerCategory") ?: "",
        providerAvatarColorHex = (get("providerAvatarColorHex") as? Number)?.toLong() ?: 0xFF0D9488,
        providerPhotoUrl = getString("providerPhotoUrl"),
        proposedPrice = (get("proposedPrice") as? Number)?.toDouble() ?: 0.0,
        estimatedDays = (get("estimatedDays") as? Number)?.toInt() ?: 1,
        description = getString("description") ?: "",
        status = try {
          ProposalStatus.valueOf(getString("status") ?: ProposalStatus.ENVIADA.name)
        } catch (_: Exception) {
          ProposalStatus.ENVIADA
        },
        counterOffers = offers,
        createdAt = createdMillis,
        isDirectAcceptance = getBoolean("isDirectAcceptance") ?: false
      )
    } catch (_: Exception) {
      null
    }
  }

  // 6. Observar Serviços Abertos (para o mural do Prestador — somente sem prestador aceito)
  fun observeOpenServices(): Flow<List<ServiceRequest>> {
    val firestore = db ?: return flowOf(emptyList())
    val path = "services"
    return firestore.collection(path)
      .whereIn("status", listOf(ServiceStatus.ABERTO.name, ServiceStatus.EM_NEGOCIACAO.name))
      .snapshots()
      .map { snapshot ->
        snapshot.documents
          .mapNotNull { it.toServiceRequestOrNull() }
          .filter { srv ->
            srv.acceptedProviderId.isNullOrBlank() &&
              (srv.status == ServiceStatus.ABERTO || srv.status == ServiceStatus.EM_NEGOCIACAO)
          }
      }
      .catch { err ->
        val firestoreError = generateSequence(err) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && err is CancellationException) throw err
        handleFirestoreError(err, OperationType.LIST, path)
      }
  }

  // 6.0b Buscar snapshot atual de Serviços Abertos (para reconciliação imediata na sincronização)
  suspend fun fetchOpenServicesOnce(): Result<List<ServiceRequest>> {
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val path = "services"
    return try {
      val snapshot = firestore.collection(path)
        .whereIn("status", listOf(ServiceStatus.ABERTO.name, ServiceStatus.EM_NEGOCIACAO.name))
        .get()
        .await()
      val openServices = snapshot.documents
        .mapNotNull { it.toServiceRequestOrNull() }
        .filter { srv ->
          srv.acceptedProviderId.isNullOrBlank() &&
            (srv.status == ServiceStatus.ABERTO || srv.status == ServiceStatus.EM_NEGOCIACAO)
        }
      Result.success(openServices)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.LIST, path)
      Result.failure(e)
    }
  }

  // 6.0c Atualiza status para EM_NEGOCIACAO somente se o serviço ainda estiver ABERTO e sem prestador aceito
  suspend fun markServiceNegotiatingIfStillOpen(serviceId: String): Result<Boolean> {
    if (serviceId.isBlank() || serviceId.contains("/")) {
      return Result.failure(IllegalArgumentException("ID de serviço inválido"))
    }
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val docRef = firestore.collection("services").document(serviceId)
    return try {
      var updated = false
      firestore.runTransaction { tx ->
        val snap = tx.get(docRef)
        if (!snap.exists()) return@runTransaction
        val currentStatus = snap.getString("status").orEmpty()
        val assigned = snap.getString("assignedProviderId") ?: snap.getString("acceptedProviderId")
        if (currentStatus == ServiceStatus.ABERTO.name && assigned.isNullOrBlank()) {
          tx.update(
            docRef,
            mapOf(
              "status" to ServiceStatus.EM_NEGOCIACAO.name,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
          updated = true
        }
      }.await()
      Result.success(updated)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.UPDATE, "services/$serviceId")
      Result.failure(e)
    }
  }

  // 6.1 Observar Serviços atribuídos ao Prestador
  fun observeProviderAssignedServices(providerId: String): Flow<List<ServiceRequest>> {
    if (providerId.isBlank() || providerId.contains("/")) return flowOf(emptyList())
    val firestore = db ?: return flowOf(emptyList())
    val path = "services"
    return firestore.collection(path)
      .whereEqualTo("assignedProviderId", providerId)
      .snapshots()
      .map { snapshot -> snapshot.documents.mapNotNull { it.toServiceRequestOrNull() } }
      .catch { err ->
        val firestoreError = generateSequence(err) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && err is CancellationException) throw err
        handleFirestoreError(err, OperationType.LIST, path)
        emit(emptyList())
      }
  }

  // 7. Observar Serviços do Cliente (Isolamento total: apenas onde clientId == uid)
  fun observeClientServices(clientId: String): Flow<List<ServiceRequest>> {
    if (clientId.isBlank() || clientId.contains("/")) return flowOf(emptyList())
    val firestore = db ?: return flowOf(emptyList())
    val path = "services"
    return firestore.collection(path)
      .whereEqualTo("clientId", clientId)
      .snapshots()
      .map { snapshot -> snapshot.documents.mapNotNull { it.toServiceRequestOrNull() } }
      .catch { err ->
        val firestoreError = generateSequence(err) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && err is CancellationException) throw err
        handleFirestoreError(err, OperationType.LIST, path)
        emit(emptyList())
      }
  }

  // 7.0a Buscar Serviço Único por ID (/services/{serviceId})
  suspend fun fetchServiceById(serviceId: String): Result<ServiceRequest?> {
    if (serviceId.isBlank() || serviceId.contains("/")) {
      return Result.failure(IllegalArgumentException("ID de serviço inválido"))
    }
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val path = "services/$serviceId"
    return try {
      val snap = firestore.collection("services").document(serviceId).get().await()
      Result.success(if (snap.exists()) snap.toServiceRequestOrNull() else null)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.GET, path)
      Result.failure(e)
    }
  }

  // 7.0b Observar Serviço Único em Tempo Real (/services/{serviceId})
  fun observeServiceById(serviceId: String): Flow<ServiceRequest?> {
    if (serviceId.isBlank() || serviceId.contains("/")) return flowOf(null)
    val firestore = db ?: return flowOf(null)
    val path = "services/$serviceId"
    return firestore.collection("services")
      .document(serviceId)
      .snapshots()
      .map { snap -> if (snap.exists()) snap.toServiceRequestOrNull() else null }
      .catch { err ->
        val firestoreError = generateSequence(err) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && err is CancellationException) throw err
        handleFirestoreError(err, OperationType.GET, path)
        emit(null)
      }
  }

  // 7.0c Excluir Serviço (/services/{serviceId})
  suspend fun deleteServiceInFirestore(serviceId: String): Result<Unit> {
    if (serviceId.isBlank() || serviceId.contains("/")) {
      return Result.failure(IllegalArgumentException("ID de serviço inválido"))
    }
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val path = "services/$serviceId"
    return try {
      firestore.collection("services").document(serviceId).delete().await()
      Result.success(Unit)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  // 7.1 Observar Propostas vinculadas ao usuário (Cliente ou Prestador)
  fun observeUserProposals(uid: String, role: UserRole): Flow<List<Proposal>> {
    if (uid.isBlank() || uid.contains("/")) return flowOf(emptyList())
    val firestore = db ?: return flowOf(emptyList())
    val path = "proposals"
    val field = if (role == UserRole.CLIENTE) "clientId" else "providerId"
    return firestore.collection(path)
      .whereEqualTo(field, uid)
      .snapshots()
      .map { snapshot -> snapshot.documents.mapNotNull { it.toProposalOrNull() } }
      .catch { err ->
        val firestoreError = generateSequence(err) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && err is CancellationException) throw err
        handleFirestoreError(err, OperationType.LIST, path)
        emit(emptyList())
      }
  }

  // 7.1b Observar em tempo real TODAS as propostas de um serviço específico
  fun observeProposalsForService(
    serviceId: String,
    uid: String,
    role: UserRole
  ): Flow<List<Proposal>> {
    if (serviceId.isBlank() || serviceId.contains("/") || uid.isBlank() || uid.contains("/")) {
      return flowOf(emptyList())
    }
    val firestore = db ?: return flowOf(emptyList())
    val path = "proposals"
    val baseQuery = firestore.collection(path).whereEqualTo("serviceId", serviceId)
    val scopedQuery = when (role) {
      // Para o cliente dono do serviço e admin, busca TODAS as propostas pelo serviceId (índice simples e sem perda por alias/clientId)
      UserRole.CLIENTE, UserRole.ADMIN -> baseQuery
      UserRole.PRESTADOR -> baseQuery.whereEqualTo("providerId", uid)
    }
    return scopedQuery
      .snapshots()
      .map { snapshot -> snapshot.documents.mapNotNull { it.toProposalOrNull() } }
      .catch { err ->
        val firestoreError = generateSequence(err) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && err is CancellationException) throw err
        handleFirestoreError(err, OperationType.LIST, path)
        emit(emptyList())
      }
  }

  // 7.1c Buscar todas as propostas / prestadores interessados para um serviço específico
  suspend fun fetchProposalsForService(
    serviceId: String,
    uid: String,
    role: UserRole
  ): List<Proposal> {
    if (serviceId.isBlank() || serviceId.contains("/") || uid.isBlank() || uid.contains("/")) {
      return emptyList()
    }
    val firestore = db ?: return emptyList()
    val path = "proposals"
    return try {
      val baseQuery = firestore.collection(path).whereEqualTo("serviceId", serviceId)
      val scopedQuery = when (role) {
        UserRole.CLIENTE, UserRole.ADMIN -> baseQuery
        UserRole.PRESTADOR -> baseQuery.whereEqualTo("providerId", uid)
      }
      val snapshot = scopedQuery.get().await()
      snapshot.documents.mapNotNull { it.toProposalOrNull() }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.LIST, path)
      emptyList()
    }
  }

  suspend fun updateServiceFields(serviceId: String, updates: Map<String, Any>): Boolean {
    if (serviceId.isBlank() || serviceId.contains("/")) return false
    val firestore = db ?: return false
    val path = "services/$serviceId"
    val payload = HashMap(updates).apply {
      remove("clientId")
      remove("createdAt")
      put("updatedAt", FieldValue.serverTimestamp())
    }
    return try {
      firestore.collection("services").document(serviceId).update(payload).await()
      true
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.UPDATE, path)
      false
    }
  }

  suspend fun updateProposalFields(proposalId: String, updates: Map<String, Any>): Boolean {
    val firestore = db ?: return false
    val path = "proposals/$proposalId"
    val payload = HashMap(updates).apply { put("updatedAt", FieldValue.serverTimestamp()) }
    return try {
      firestore.collection("proposals").document(proposalId).update(payload).await()
      true
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.UPDATE, path)
      false
    }
  }

  /**
   * Gatilho de Transação Atômica no Firestore:
   * 1. Lê a carteira do prestador em /users/{providerId}.
   * 2. Calcula a taxa de 10% (agreedPrice * 0.10).
   * 3. Valida saldo suficiente (se insuficiente, lança InsufficientBalanceException).
   * 4. Executa atomicamente:
   *    - Débito da taxa no saldo da carteira do prestador.
   *    - Criação do lançamento em /users/{providerId}/transactions/{transactionId}.
   *    - Atualização da proposta para ACEITA.
   *    - Atualização do pedido para ACEITO com prestador atribuído e taxa gravada.
   *    - Envio de mensagem de confirmação do sistema no chat liberado.
   */
  suspend fun acceptProposalAndDebitFeeTransaction(
    proposalId: String,
    serviceId: String,
    providerId: String,
    providerName: String,
    serviceTitle: String,
    agreedPrice: Double
  ): Result<Double> {
    val firestore = db ?: return Result.failure(Exception("Firestore não inicializado"))
    val platformFee = agreedPrice * 0.10
    val providerRef = firestore.collection("users").document(providerId)
    val proposalRef = firestore.collection("proposals").document(proposalId)
    val serviceRef = firestore.collection("services").document(serviceId)
    val txId = "tx_${java.util.UUID.randomUUID().toString().take(8)}"
    val txRef = providerRef.collection("transactions").document(txId)
    val msgId = "msg_sys_${java.util.UUID.randomUUID().toString().take(8)}"
    val msgRef = serviceRef.collection("messages").document(msgId)

    return try {
      firestore.runTransaction { tx ->
        val providerSnap = tx.get(providerRef)
        val currentBalance = providerSnap.getDouble("walletBalance") ?: 0.0

        if (currentBalance <= MarketplaceRepository.MIN_PROVIDER_ACCEPTANCE_BALANCE) {
          throw InsufficientBalanceException(
            requiredFee = platformFee,
            currentBalance = currentBalance,
            missingAmount = (MarketplaceRepository.MIN_PROVIDER_ACCEPTANCE_BALANCE + 0.01 - currentBalance).coerceAtLeast(0.01),
            providerName = providerName
          )
        }

        val newBalance = currentBalance - platformFee
        val completedCount = (providerSnap.getLong("completedJobs") ?: 0L) + 1

        // 1. Débito da Taxa de 10% na Carteira do Prestador
        tx.update(
          providerRef,
          mapOf(
            "walletBalance" to newBalance,
            "completedJobs" to completedCount,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )

        // 2. Registro no Extrato da Carteira
        tx.set(
          txRef,
          mapOf(
            "providerId" to providerId,
            "type" to "DEBITO_TAXA_10",
            "amount" to platformFee,
            "serviceId" to serviceId,
            "serviceTitle" to serviceTitle,
            "description" to "Taxa de intermediação de 10% referente ao serviço contratado #$serviceId",
            "balanceAfter" to newBalance,
            "createdAt" to FieldValue.serverTimestamp()
          )
        )

        // 3. Atualizar Status da Proposta
        tx.update(
          proposalRef,
          mapOf(
            "status" to ProposalStatus.ACEITA.name,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )

        // 4. Atualizar Pedido de Serviço (Em Andamento)
        val serviceUpdates = hashMapOf<String, Any>(
          "status" to ServiceStatus.EM_EXECUCAO.name,
          "assignedProviderId" to providerId,
          "assignedProviderName" to providerName,
          "acceptedProviderId" to providerId,
          "acceptedProviderName" to providerName,
          "acceptedProviderRating" to ((providerSnap.get("rating") as? Number)?.toDouble() ?: 5.0),
          "acceptedProviderAvatarColorHex" to ((providerSnap.get("avatarColorHex") as? Number)?.toLong() ?: 0xFF0D9488),
          "finalAgreedPrice" to agreedPrice,
          "platformFee" to platformFee,
          "updatedAt" to FieldValue.serverTimestamp()
        )
        providerSnap.getString("photoUrl")?.takeIf { it.isNotBlank() }?.let {
          serviceUpdates["acceptedProviderPhotoUrl"] = it
        }
        tx.update(serviceRef, serviceUpdates)

        // 5. Publicar Mensagem do Sistema no Chat Privado
        tx.set(
          msgRef,
          mapOf(
            "serviceId" to serviceId,
            "senderId" to "system",
            "senderName" to "Sistema ResolveAi",
            "senderRole" to "SISTEMA",
            "content" to "🎉 Proposta aceita! Serviço confirmado e chat liberado para alinhamento!",
            "isSystemNotice" to true,
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
      }.await()

      Result.success(platformFee)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      if (e is InsufficientBalanceException) {
        Result.failure(e)
      } else {
        handleFirestoreError(e, OperationType.WRITE, "services/$serviceId")
        Result.failure(e)
      }
    }
  }

  /**
   * Registra a cobrança Pix no Firestore para permitir detecção automática em tempo real.
   */
  suspend fun createPixRechargeCharge(charge: PixRechargeCharge): Result<Unit> {
    val firestore = db ?: return Result.failure(Exception("Firestore indisponível"))
    val chargeRef = firestore.collection("users")
      .document(charge.providerId)
      .collection("pixCharges")
      .document(charge.txId)

    val data = hashMapOf<String, Any>(
      "txId" to charge.txId,
      "providerId" to charge.providerId,
      "pixKey" to charge.pixKey,
      "merchantName" to charge.merchantName,
      "amount" to charge.amount,
      "brCode" to charge.brCode,
      "status" to charge.status.name,
      "paymentGateway" to charge.paymentGateway,
      "createdAtMillis" to charge.createdAt,
      "expiresAtMillis" to charge.expiresAt,
      "createdAt" to FieldValue.serverTimestamp()
    )
    charge.mercadoPagoPaymentId?.let { data["mercadoPagoPaymentId"] = it }
    charge.ticketUrl?.takeIf { it.isNotBlank() }?.let { data["ticketUrl"] = it }

    return try {
      chargeRef.set(data).await()
      Result.success(Unit)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.WRITE, "users/${charge.providerId}/pixCharges/${charge.txId}")
      Result.failure(e)
    }
  }

  /**
   * Verifica pontualmente (ex.: ao retornar do app bancário) se a cobrança Pix já foi paga.
   */
  suspend fun checkPixChargeStatusOnce(
    providerId: String,
    txId: String,
    expectedAmount: Double,
    knownInitialBalance: Double
  ): PixChargeDetectionEvent? {
    val firestore = db ?: return null
    val userRef = firestore.collection("users").document(providerId)
    return try {
      val chargeSnap = userRef.collection("pixCharges").document(txId).get().await()
      val chargeStatus = chargeSnap.getString("status").orEmpty().uppercase()
      if (chargeStatus in setOf("PAGO", "PAID", "CONFIRMED", "CONCLUIDO", "APPROVED")) {
        val userSnap = userRef.get().await()
        val remoteBalance = userSnap.getDouble("walletBalance")
        return PixChargeDetectionEvent(
          txId = txId,
          amount = chargeSnap.getDouble("amount") ?: expectedAmount,
          newBalance = remoteBalance,
          source = "pixCharges"
        )
      }

      val txSnap = userRef.collection("transactions")
        .whereEqualTo("pixTxId", txId)
        .limit(1)
        .get()
        .await()
      val matchingTx = txSnap.documents.firstOrNull()
      if (matchingTx != null) {
        return PixChargeDetectionEvent(
          txId = txId,
          amount = matchingTx.getDouble("amount") ?: expectedAmount,
          newBalance = matchingTx.getDouble("balanceAfter"),
          source = "transactions"
        )
      }

      val userSnap = userRef.get().await()
      val remoteBalance = userSnap.getDouble("walletBalance")
      if (remoteBalance != null && remoteBalance >= knownInitialBalance + expectedAmount - 0.01) {
        return PixChargeDetectionEvent(
          txId = txId,
          amount = expectedAmount,
          newBalance = remoteBalance,
          source = "walletBalance"
        )
      }
      null
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      null
    }
  }

  /**
   * Escuta em tempo real (Firestore SnapshotListener) a confirmação automática de uma cobrança Pix.
   */
  fun observePixChargePayment(
    providerId: String,
    txId: String,
    expectedAmount: Double,
    knownInitialBalance: Double
  ): Flow<PixChargeDetectionEvent> {
    val firestore = db ?: return emptyFlow()
    return callbackFlow {
      val userRef = firestore.collection("users").document(providerId)
      val chargeRef = userRef.collection("pixCharges").document(txId)
      val txQuery = userRef.collection("transactions").whereEqualTo("pixTxId", txId).limit(1)
      var alreadyEmitted = false

      fun emitOnce(event: PixChargeDetectionEvent) {
        if (!alreadyEmitted) {
          alreadyEmitted = true
          trySend(event)
        }
      }

      val chargeReg = chargeRef.addSnapshotListener { snap, _ ->
        val status = snap?.getString("status").orEmpty().uppercase()
        if (status in setOf("PAGO", "PAID", "CONFIRMED", "CONCLUIDO", "APPROVED")) {
          emitOnce(
            PixChargeDetectionEvent(
              txId = txId,
              amount = snap?.getDouble("amount") ?: expectedAmount,
              newBalance = snap?.getDouble("balanceAfter"),
              source = "pixCharges"
            )
          )
        }
      }

      val userReg = userRef.addSnapshotListener { snap, _ ->
        val remoteBal = snap?.getDouble("walletBalance")
        if (remoteBal != null && remoteBal >= knownInitialBalance + expectedAmount - 0.01) {
          emitOnce(
            PixChargeDetectionEvent(
              txId = txId,
              amount = expectedAmount,
              newBalance = remoteBal,
              source = "walletBalance"
            )
          )
        }
      }

      val txReg = txQuery.addSnapshotListener { snap, _ ->
        val doc = snap?.documents?.firstOrNull()
        if (doc != null) {
          emitOnce(
            PixChargeDetectionEvent(
              txId = txId,
              amount = doc.getDouble("amount") ?: expectedAmount,
              newBalance = doc.getDouble("balanceAfter"),
              source = "transactions"
            )
          )
        }
      }

      awaitClose {
        chargeReg.remove()
        userReg.remove()
        txReg.remove()
      }
    }
  }

  /**
   * Recarrega a carteira digital do prestador exclusivamente via Pix com registro no Firestore.
   */
  suspend fun rechargeProviderWallet(
    providerId: String,
    amount: Double,
    txId: String? = null
  ): Result<Double> {
    val firestore = db ?: return Result.failure(Exception("Firestore indisponível"))
    val providerRef = firestore.collection("users").document(providerId)
    val cleanTxId = txId?.filter { it.isLetterOrDigit() }?.takeIf { it.isNotBlank() }
    val docId = "tx_pix_${cleanTxId ?: UUID.randomUUID().toString().take(8)}"
    val txRef = providerRef.collection("transactions").document(docId)
    val description = if (cleanTxId != null) {
      "Recarga de saldo via Pix ($cleanTxId)"
    } else {
      "Recarga de saldo via Pix instantâneo"
    }

    return try {
      var newBalanceResult = 0.0
      firestore.runTransaction { tx ->
        val providerSnap = tx.get(providerRef)
        val currentBalance = providerSnap.getDouble("walletBalance") ?: 0.0
        val newBalance = currentBalance + amount
        newBalanceResult = newBalance

        if (providerSnap.exists()) {
          tx.update(
            providerRef,
            mapOf(
              "walletBalance" to newBalance,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        } else {
          tx.set(
            providerRef,
            mapOf(
              "userId" to providerId,
              "walletBalance" to newBalance,
              "updatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
          )
        }

        val txData = hashMapOf<String, Any>(
          "providerId" to providerId,
          "type" to "RECARGA_PIX",
          "paymentMethod" to "PIX",
          "amount" to amount,
          "description" to description,
          "balanceAfter" to newBalance,
          "createdAt" to FieldValue.serverTimestamp()
        )
        cleanTxId?.let { txData["pixTxId"] = it }
        tx.set(txRef, txData)

        if (cleanTxId != null) {
          val chargeRef = providerRef.collection("pixCharges").document(cleanTxId)
          tx.set(
            chargeRef,
            mapOf(
              "txId" to cleanTxId,
              "providerId" to providerId,
              "amount" to amount,
              "status" to "PAGO",
              "balanceAfter" to newBalance,
              "paidAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
          )
        }
      }.await()

      Result.success(newBalanceResult)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.WRITE, "users/$providerId/transactions/$docId")
      Result.failure(e)
    }
  }

  suspend fun refundProviderPlatformFee(
    providerId: String,
    serviceId: String,
    serviceTitle: String,
    amount: Double,
    reason: String
  ): Result<Double> {
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    if (amount <= 0.0) return Result.failure(IllegalArgumentException("Valor de estorno inválido"))
    val providerRef = firestore.collection("users").document(providerId)
    val docId = "tx_refund_${serviceId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")}".take(120)
    val txRef = providerRef.collection("transactions").document(docId)

    return try {
      var newBalanceResult = 0.0
      firestore.runTransaction { tx ->
        val existingRefund = tx.get(txRef)
        val providerSnap = tx.get(providerRef)
        val currentBalance = providerSnap.getDouble("walletBalance") ?: 0.0
        if (existingRefund.exists()) {
          newBalanceResult = currentBalance
          return@runTransaction
        }
        val newBalance = currentBalance + amount
        newBalanceResult = newBalance
        if (providerSnap.exists()) {
          tx.update(
            providerRef,
            mapOf(
              "walletBalance" to newBalance,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        }
        tx.set(
          txRef,
          hashMapOf(
            "providerId" to providerId,
            "type" to TransactionType.REEMBOLSO.name,
            "amount" to amount,
            "serviceId" to serviceId,
            "serviceTitle" to serviceTitle.take(200),
            "description" to "Estorno da taxa de 10% pelo cancelamento: $reason".take(300),
            "balanceAfter" to newBalance,
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
      }.await()
      Result.success(newBalanceResult)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.WRITE, "users/$providerId/transactions/$docId")
      Result.failure(e)
    }
  }

  suspend fun adminAddProviderBalance(
    providerId: String,
    amount: Double,
    reason: String,
    adminUid: String,
    adminEmail: String
  ): Result<Double> {
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    if (amount <= 0.0 || amount.isNaN()) {
      return Result.failure(IllegalArgumentException("Valor de crédito inválido"))
    }
    val providerRef = firestore.collection("users").document(providerId)
    val docId = "tx_admin_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}"
    val txRef = providerRef.collection("transactions").document(docId)
    val cleanReason = reason.trim()
    val description = if (cleanReason.isNotBlank()) {
      "Crédito manual pelo administrador: $cleanReason"
    } else {
      "Crédito manual adicionado pelo administrador"
    }.take(300)

    return try {
      var newBalanceResult = 0.0
      firestore.runTransaction { tx ->
        val providerSnap = tx.get(providerRef)
        val currentBalance = providerSnap.getDouble("walletBalance") ?: 0.0
        val newBalance = currentBalance + amount
        newBalanceResult = newBalance
        if (providerSnap.exists()) {
          tx.update(
            providerRef,
            mapOf(
              "walletBalance" to newBalance,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        }
        tx.set(
          txRef,
          hashMapOf(
            "providerId" to providerId,
            "type" to TransactionType.CREDITO_ADMIN.name,
            "amount" to amount,
            "description" to description,
            "balanceAfter" to newBalance,
            "adminUid" to adminUid.take(128),
            "adminEmail" to adminEmail.take(150),
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
      }.await()
      Result.success(newBalanceResult)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.WRITE, "users/$providerId/transactions/$docId")
      Result.failure(e)
    }
  }

  private fun DocumentSnapshot.toWalletTransactionOrNull(fallbackProviderId: String): WalletTransaction? {
    return try {
      val typeStr = getString("type").orEmpty()
      val txType = when (typeStr.uppercase()) {
        "DEBITO_TAXA_10" -> TransactionType.DEBITO_TAXA_10
        "RECARGA_PIX" -> TransactionType.RECARGA_PIX
        "RECARGA_CARTAO" -> TransactionType.RECARGA_CARTAO
        "REEMBOLSO" -> TransactionType.REEMBOLSO
        "CREDITO_ADMIN" -> TransactionType.CREDITO_ADMIN
        else -> if (getString("paymentMethod")?.equals("PIX", ignoreCase = true) == true) {
          TransactionType.RECARGA_PIX
        } else {
          return null
        }
      }
      val amount = (get("amount") as? Number)?.toDouble() ?: return null
      val providerId = getString("providerId")?.takeIf { it.isNotBlank() } ?: fallbackProviderId
      val serviceId = getString("serviceId")?.takeIf { it.isNotBlank() }
      val serviceTitle = getString("serviceTitle")?.takeIf { it.isNotBlank() }
      val pixTxId = getString("pixTxId")?.takeIf { it.isNotBlank() }
      val rawDescription = getString("description").orEmpty()
      val description = if (rawDescription.isNotBlank()) {
        if (pixTxId != null && !rawDescription.contains(pixTxId)) {
          "$rawDescription ($pixTxId)"
        } else {
          rawDescription
        }
      } else {
        when (txType) {
          TransactionType.RECARGA_PIX -> pixTxId?.let { "Recarga de saldo via Pix ($it)" } ?: "Recarga de saldo via Pix"
          TransactionType.DEBITO_TAXA_10 -> serviceTitle?.let { "Taxa de 10% referente a $it" } ?: "Taxa de intermediação de 10%"
          TransactionType.RECARGA_CARTAO -> "Recarga histórica na carteira"
          TransactionType.REEMBOLSO -> "Estorno de taxa de intermediação"
          TransactionType.CREDITO_ADMIN -> "Crédito manual adicionado pelo administrador"
        }
      }
      val balanceAfter = (get("balanceAfter") as? Number)?.toDouble() ?: 0.0
      val timestamp = parseTimestampMillis(get("createdAt"))
        ?: parseTimestampMillis(get("timestamp"))
        ?: System.currentTimeMillis()

      WalletTransaction(
        id = id,
        providerId = providerId,
        type = txType,
        amount = amount,
        serviceId = serviceId,
        serviceTitle = serviceTitle,
        description = description,
        timestamp = timestamp,
        balanceAfter = balanceAfter
      )
    } catch (_: Exception) {
      null
    }
  }

  suspend fun fetchUserTransactions(providerId: String): Result<List<WalletTransaction>> {
    val firestore = db ?: return Result.failure(Exception("Firestore indisponível"))
    return try {
      val snapshot = firestore.collection("users")
        .document(providerId)
        .collection("transactions")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .get()
        .await()
      val list = snapshot.documents.mapNotNull { it.toWalletTransactionOrNull(providerId) }
      Result.success(list)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.LIST, "users/$providerId/transactions")
      Result.failure(e)
    }
  }

  fun observeUserTransactions(providerId: String): Flow<List<WalletTransaction>> {
    val firestore = db ?: return emptyFlow()
    return firestore.collection("users")
      .document(providerId)
      .collection("transactions")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snap -> snap.documents.mapNotNull { it.toWalletTransactionOrNull(providerId) } }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, "users/$providerId/transactions")
        emit(emptyList())
      }
  }

  // Histórico de Localização do Prestador (/services/{serviceId}/locationHistory/{pointId})
  suspend fun recordProviderLocationPoint(point: ProviderLocationPoint): Boolean {
    val firestore = db ?: return false
    val path = "services/${point.serviceId}/locationHistory/${point.id}"
    val payload = hashMapOf<String, Any>(
      "serviceId" to point.serviceId.take(128),
      "providerId" to point.providerId.take(128),
      "latitude" to point.latitude,
      "longitude" to point.longitude,
      "accuracyMeters" to point.accuracyMeters,
      "speedMps" to point.speedMps,
      "serviceStatus" to point.serviceStatus.name,
      "recordedAt" to point.recordedAt,
      "source" to point.source.take(50),
      "createdAt" to FieldValue.serverTimestamp()
    )
    return try {
      firestore.collection("services")
        .document(point.serviceId)
        .collection("locationHistory")
        .document(point.id)
        .set(payload)
        .await()
      true
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.CREATE, path)
      false
    }
  }

  private fun DocumentSnapshot.toLocationPointOrNull(): ProviderLocationPoint? {
    return try {
      val lat = (get("latitude") as? Number)?.toDouble() ?: return null
      val lng = (get("longitude") as? Number)?.toDouble() ?: return null
      val statusStr = getString("serviceStatus") ?: ServiceStatus.ACEITO.name
      val parsedStatus = try { ServiceStatus.valueOf(statusStr) } catch (_: Exception) { ServiceStatus.ACEITO }
      ProviderLocationPoint(
        id = id,
        serviceId = getString("serviceId") ?: reference.parent.parent?.id.orEmpty(),
        providerId = getString("providerId").orEmpty(),
        latitude = lat,
        longitude = lng,
        accuracyMeters = (get("accuracyMeters") as? Number)?.toFloat() ?: 0f,
        speedMps = (get("speedMps") as? Number)?.toFloat() ?: 0f,
        serviceStatus = parsedStatus,
        recordedAt = parseTimestampMillis(get("recordedAt"))
          ?: getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
          ?: System.currentTimeMillis(),
        source = getString("source") ?: "GPS"
      )
    } catch (_: Exception) {
      null
    }
  }

  suspend fun fetchServiceLocationHistory(serviceId: String): List<ProviderLocationPoint> {
    val firestore = db ?: return emptyList()
    val path = "services/$serviceId/locationHistory"
    return try {
      val snapshot = firestore.collection("services")
        .document(serviceId)
        .collection("locationHistory")
        .orderBy("createdAt", Query.Direction.ASCENDING)
        .get()
        .await()
      snapshot.documents.mapNotNull { it.toLocationPointOrNull() }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.LIST, path)
      emptyList()
    }
  }

  fun observeServiceLocationHistory(serviceId: String): Flow<List<ProviderLocationPoint>> {
    val firestore = db ?: return flowOf(emptyList())
    val path = "services/$serviceId/locationHistory"
    return firestore.collection("services")
      .document(serviceId)
      .collection("locationHistory")
      .orderBy("createdAt", Query.Direction.ASCENDING)
      .snapshots()
      .map { snap -> snap.documents.mapNotNull { it.toLocationPointOrNull() } }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, path)
        emit(emptyList())
      }
  }

  // Sugestões para o Aplicativo (/appSuggestions/{suggestionId})
  suspend fun saveAppSuggestion(suggestion: AppSuggestion): Boolean {
    val firestore = db ?: return false
    val path = "appSuggestions/${suggestion.id}"
    val payload = hashMapOf<String, Any>(
      "userId" to suggestion.userId.take(128),
      "userName" to suggestion.userName.trim().let { if (it.length >= 2) it.take(100) else "Usuário" },
      "userRole" to suggestion.userRole.name,
      "category" to suggestion.category.take(80),
      "title" to suggestion.title.trim().let { if (it.length >= 3) it.take(150) else "Sugestão" },
      "description" to suggestion.description.trim().let { if (it.length >= 5) it.take(1500) else "Sugestão enviada" },
      "status" to suggestion.status.name,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    suggestion.adminReply?.trim()?.takeIf { it.isNotBlank() }?.take(500)?.let {
      payload["adminReply"] = it
    }
    return try {
      firestore.collection("appSuggestions").document(suggestion.id).set(payload).await()
      true
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.CREATE, path)
      false
    }
  }

  suspend fun updateAppSuggestionAdmin(
    suggestionId: String,
    status: SuggestionStatus,
    adminReply: String?
  ): Boolean {
    val firestore = db ?: return false
    val path = "appSuggestions/$suggestionId"
    val updates = hashMapOf<String, Any>(
      "status" to status.name,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (!adminReply.isNullOrBlank()) {
      updates["adminReply"] = adminReply.trim().take(500)
    }
    return try {
      firestore.collection("appSuggestions").document(suggestionId).update(updates).await()
      true
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.UPDATE, path)
      false
    }
  }

  private fun DocumentSnapshot.toAppSuggestionOrNull(): AppSuggestion? {
    return try {
      val title = getString("title") ?: return null
      val role = try { UserRole.valueOf(getString("userRole") ?: UserRole.CLIENTE.name) } catch (_: Exception) { UserRole.CLIENTE }
      val status = try { SuggestionStatus.valueOf(getString("status") ?: SuggestionStatus.ENVIADA.name) } catch (_: Exception) { SuggestionStatus.ENVIADA }
      val createdMillis = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
        ?: parseTimestampMillis(get("createdAt"))
        ?: System.currentTimeMillis()
      val updatedMillis = getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
        ?: parseTimestampMillis(get("updatedAt"))
        ?: createdMillis
      AppSuggestion(
        id = id,
        userId = getString("userId").orEmpty(),
        userName = getString("userName") ?: "Usuário",
        userRole = role,
        category = getString("category") ?: "Melhoria no App",
        title = title,
        description = getString("description").orEmpty(),
        status = status,
        adminReply = getString("adminReply"),
        createdAt = createdMillis,
        updatedAt = updatedMillis
      )
    } catch (_: Exception) {
      null
    }
  }

  fun observeAppSuggestions(userId: String, isAdmin: Boolean): Flow<List<AppSuggestion>> {
    val firestore = db ?: return emptyFlow()
    val path = "appSuggestions"
    val query: Query = if (isAdmin) {
      firestore.collection("appSuggestions").orderBy("createdAt", Query.Direction.DESCENDING)
    } else {
      firestore.collection("appSuggestions")
        .whereEqualTo("userId", userId)
        .orderBy("createdAt", Query.Direction.DESCENDING)
    }
    return query.snapshots()
      .map { snap -> snap.documents.mapNotNull { it.toAppSuggestionOrNull() } }
      .catch { e ->
        handleFirestoreError(e, OperationType.LIST, path)
        emit(emptyList())
      }
  }

  // Categorias de Serviços Gerenciadas pelo Admin (/serviceCategories/{categoryId})
  suspend fun saveServiceCategory(category: ServiceCategoryItem): Boolean {
    if (category.id.isBlank() || category.id.contains("/")) return false
    val firestore = db ?: return false
    val path = "serviceCategories/${category.id}"
    val payload = hashMapOf<String, Any>(
      "id" to category.id.take(128),
      "name" to category.name.trim().take(80),
      "specialtyLabel" to category.effectiveSpecialtyLabel.take(120),
      "isActive" to category.isActive,
      "orderIndex" to category.orderIndex,
      "updatedAt" to category.updatedAt
    )
    return try {
      firestore.collection("serviceCategories").document(category.id).set(payload).await()
      true
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.CREATE, path)
      false
    }
  }

  suspend fun deleteServiceCategory(categoryId: String): Boolean {
    if (categoryId.isBlank() || categoryId.contains("/")) return false
    val firestore = db ?: return false
    val path = "serviceCategories/$categoryId"
    return try {
      firestore.collection("serviceCategories").document(categoryId).delete().await()
      true
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.DELETE, path)
      false
    }
  }

  private fun DocumentSnapshot.toServiceCategoryOrNull(): ServiceCategoryItem? {
    return try {
      val name = getString("name")?.trim()?.takeIf { it.isNotBlank() } ?: return null
      ServiceCategoryItem(
        id = getString("id") ?: id,
        name = name,
        specialtyLabel = getString("specialtyLabel").orEmpty().ifBlank { name },
        isActive = getBoolean("isActive") ?: true,
        orderIndex = (get("orderIndex") as? Number)?.toInt() ?: 0,
        updatedAt = parseTimestampMillis(get("updatedAt")) ?: System.currentTimeMillis()
      )
    } catch (_: Exception) {
      null
    }
  }

  fun observeServiceCategories(): Flow<List<ServiceCategoryItem>> {
    val firestore = db ?: return emptyFlow()
    val path = "serviceCategories"
    return firestore.collection(path)
      .orderBy("orderIndex", Query.Direction.ASCENDING)
      .snapshots()
      .map { snap -> snap.documents.mapNotNull { it.toServiceCategoryOrNull() } }
      .catch { e ->
        val firestoreError = generateSequence(e) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && e is CancellationException) throw e
        handleFirestoreError(e, OperationType.LIST, path)
        emit(emptyList())
      }
  }

  // Configurações Gerais e Dicas do App (/appConfig/marketplace)
  suspend fun saveAppAdminConfig(settings: AppAdminSettings, tips: List<AppTip>): Boolean {
    val firestore = db ?: return false
    val path = "appConfig/marketplace"
    val serializedTips = tips.take(50).map { tip ->
      hashMapOf<String, Any>(
        "id" to tip.id,
        "targetRole" to (tip.targetRole?.name ?: "ALL"),
        "category" to tip.category.take(60),
        "title" to tip.title.take(150),
        "description" to tip.description.take(600)
      )
    }
    val payload = hashMapOf<String, Any>(
      "appDisplayName" to settings.appDisplayName.trim().ifBlank { "ResolveAi Serviços" }.take(80),
      "globalBannerMessage" to settings.globalBannerMessage.trim().take(400),
      "supportWhatsappNumberE164" to settings.supportWhatsappNumberE164.filter { it.isDigit() }.ifBlank { SupportLauncher.SUPPORT_WHATSAPP_NUMBER_E164 }.take(25),
      "supportWhatsappDisplay" to settings.supportWhatsappDisplay.trim().ifBlank { SupportLauncher.SUPPORT_WHATSAPP_DISPLAY }.take(40),
      "defaultServiceUrgency" to settings.defaultServiceUrgency.takeIf { it in listOf("Baixa", "Média", "Alta", "Urgente") }.orEmpty().ifBlank { "Média" },
      "tips" to serializedTips,
      "updatedAt" to settings.updatedAt
    )
    return try {
      firestore.collection("appConfig").document("marketplace").set(payload).await()
      true
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.UPDATE, path)
      false
    }
  }

  fun observeAppAdminConfig(): Flow<Pair<AppAdminSettings, List<AppTip>>?> {
    val firestore = db ?: return emptyFlow()
    val path = "appConfig/marketplace"
    return firestore.collection("appConfig").document("marketplace")
      .snapshots()
      .map { snap ->
        if (!snap.exists()) return@map null
        val settings = AppAdminSettings(
          appDisplayName = snap.getString("appDisplayName") ?: "ResolveAi Serviços",
          globalBannerMessage = snap.getString("globalBannerMessage") ?: "",
          supportWhatsappNumberE164 = snap.getString("supportWhatsappNumberE164") ?: SupportLauncher.SUPPORT_WHATSAPP_NUMBER_E164,
          supportWhatsappDisplay = snap.getString("supportWhatsappDisplay") ?: SupportLauncher.SUPPORT_WHATSAPP_DISPLAY,
          defaultServiceUrgency = snap.getString("defaultServiceUrgency") ?: "Média",
          updatedAt = parseTimestampMillis(snap.get("updatedAt")) ?: System.currentTimeMillis()
        )
        val rawTips = snap.get("tips") as? List<*> ?: emptyList<Any>()
        val parsedTips = rawTips.mapNotNull { item ->
          val map = item as? Map<*, *> ?: return@mapNotNull null
          val id = (map["id"] as? String)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
          val title = (map["title"] as? String)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
          val roleStr = map["targetRole"] as? String
          val targetRole = when (roleStr) {
            UserRole.CLIENTE.name -> UserRole.CLIENTE
            UserRole.PRESTADOR.name -> UserRole.PRESTADOR
            else -> null
          }
          AppTip(
            id = id,
            targetRole = targetRole,
            category = (map["category"] as? String).orEmpty().ifBlank { "Geral" },
            title = title,
            description = (map["description"] as? String).orEmpty()
          )
        }
        settings to parsedTips
      }
      .catch { e ->
        val firestoreError = generateSequence(e) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && e is CancellationException) throw e
        handleFirestoreError(e, OperationType.GET, path)
        emit(null)
      }
  }

  // Edição Administrativa Completa de Serviços (/services/{serviceId})
  suspend fun adminUpdateServiceRequest(service: ServiceRequest): Boolean {
    if (service.id.isBlank() || service.id.contains("/")) return false
    val firestore = db ?: return false
    val path = "services/${service.id}"
    val updates = hashMapOf<String, Any>(
      "title" to service.title.trim().take(200),
      "category" to service.category.trim().take(100),
      "description" to service.description.trim().take(2000),
      "location" to service.location.trim().take(300),
      "proposedBudget" to service.proposedBudget.coerceAtLeast(0.0),
      "urgency" to service.urgency.trim().take(50),
      "status" to service.status.name,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return try {
      firestore.collection("services").document(service.id).update(updates).await()
      true
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.UPDATE, path)
      false
    }
  }

  private fun DocumentSnapshot.toServiceReviewOrNull(): ServiceReview? {
    return try {
      val serviceId = getString("serviceId")?.takeIf { it.isNotBlank() } ?: id
      val clientId = getString("clientId")?.takeIf { it.isNotBlank() } ?: return null
      val providerId = getString("providerId")?.takeIf { it.isNotBlank() } ?: return null
      val rating = (get("rating") as? Number)?.toFloat() ?: return null
      val createdMillis = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?.toDate()?.time
        ?: parseTimestampMillis(get("createdAt"))
        ?: System.currentTimeMillis()
      val repliedMillis = getTimestamp("repliedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?.toDate()?.time
        ?: parseTimestampMillis(get("repliedAt"))
      val tags = (get("complimentTags") as? List<*>)
        ?.mapNotNull { (it as? String)?.trim()?.takeIf { tag -> tag.isNotBlank() } }
        .orEmpty()

      ServiceReview(
        id = id,
        serviceId = serviceId,
        serviceTitle = getString("serviceTitle") ?: "Serviço ResolveAi",
        serviceCategory = getString("serviceCategory").orEmpty(),
        clientId = clientId,
        clientName = getString("clientName") ?: "Cliente",
        clientPhotoUrl = getString("clientPhotoUrl"),
        providerId = providerId,
        providerName = getString("providerName") ?: "Prestador",
        rating = rating.coerceIn(1.0f, 5.0f),
        comment = getString("comment").orEmpty(),
        complimentTags = tags,
        createdAt = createdMillis,
        providerReply = getString("providerReply"),
        repliedAt = repliedMillis
      )
    } catch (_: Exception) {
      null
    }
  }

  // 11. Avaliações Pós-Serviço (/reviews/{reviewId}) e sincronização atômica em /services/{serviceId}
  suspend fun submitServiceReview(
    review: ServiceReview,
    updatedProviderAverageRating: Double
  ): Result<Unit> {
    if (review.serviceId.isBlank() || review.serviceId.contains("/")) {
      return Result.failure(IllegalArgumentException("ID de serviço inválido"))
    }
    if (review.clientId.isBlank() || review.providerId.isBlank()) {
      return Result.failure(IllegalArgumentException("Participantes da avaliação inválidos"))
    }
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val reviewDocId = review.serviceId
    val path = "reviews/$reviewDocId"

    val safeClientName = review.clientName.trim().let { if (it.length >= 2) it.take(100) else "Cliente ResolveAi" }
    val safeProviderName = review.providerName.trim().ifBlank { "Prestador" }.take(100)
    val safeRating = review.rating.toDouble().coerceIn(1.0, 5.0)
    val cleanComment = review.comment.trim().take(1000)
    val cleanTags = review.complimentTags.mapNotNull { it.trim().takeIf { t -> t.isNotBlank() }?.take(60) }.take(10)

    val reviewPayload = hashMapOf<String, Any>(
      "serviceId" to review.serviceId,
      "serviceTitle" to review.serviceTitle.trim().take(200),
      "serviceCategory" to review.serviceCategory.trim().take(100),
      "clientId" to review.clientId,
      "clientName" to safeClientName,
      "providerId" to review.providerId,
      "providerName" to safeProviderName,
      "rating" to safeRating,
      "comment" to cleanComment,
      "complimentTags" to cleanTags,
      "isPublic" to true,
      "createdAt" to FieldValue.serverTimestamp()
    )
    review.clientPhotoUrl?.trim()?.takeIf { it.isNotBlank() }?.take(250_000)?.let {
      reviewPayload["clientPhotoUrl"] = it
    }

    val serviceUpdates = hashMapOf<String, Any>(
      "ratingGiven" to safeRating,
      "reviewComment" to cleanComment,
      "reviewTags" to cleanTags,
      "acceptedProviderRating" to updatedProviderAverageRating.coerceIn(1.0, 5.0),
      "reviewedAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )

    return try {
      val batch = firestore.batch()
      val reviewRef = firestore.collection("reviews").document(reviewDocId)
      val serviceRef = firestore.collection("services").document(review.serviceId)
      batch.set(reviewRef, reviewPayload)
      batch.update(serviceRef, serviceUpdates)
      batch.commit().await()
      Result.success(Unit)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun replyToServiceReview(reviewId: String, replyText: String): Result<Unit> {
    if (reviewId.isBlank() || reviewId.contains("/")) {
      return Result.failure(IllegalArgumentException("ID de avaliação inválido"))
    }
    val cleanReply = replyText.trim().take(500)
    if (cleanReply.isBlank()) {
      return Result.failure(IllegalArgumentException("Digite uma resposta para a avaliação"))
    }
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val path = "reviews/$reviewId"
    return try {
      firestore.collection("reviews")
        .document(reviewId)
        .update(
          mapOf(
            "providerReply" to cleanReply,
            "repliedAt" to FieldValue.serverTimestamp()
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  fun observePublicReviews(): Flow<List<ServiceReview>> {
    val firestore = db ?: return flowOf(emptyList())
    val path = "reviews"
    return firestore.collection(path)
      .whereEqualTo("isPublic", true)
      .snapshots()
      .map { snapshot ->
        snapshot.documents
          .mapNotNull { it.toServiceReviewOrNull() }
          .sortedByDescending { it.createdAt }
      }
      .catch { err ->
        val firestoreError = generateSequence(err) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && err is CancellationException) throw err
        handleFirestoreError(err, OperationType.LIST, path)
        emit(emptyList())
      }
  }

  suspend fun fetchProviderReviews(providerId: String): Result<List<ServiceReview>> {
    if (providerId.isBlank() || providerId.contains("/")) {
      return Result.failure(IllegalArgumentException("ID de prestador inválido"))
    }
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val path = "reviews"
    return try {
      val snapshot = firestore.collection(path)
        .whereEqualTo("providerId", providerId)
        .whereEqualTo("isPublic", true)
        .get()
        .await()
      val list = snapshot.documents
        .mapNotNull { it.toServiceReviewOrNull() }
        .sortedByDescending { it.createdAt }
      Result.success(list)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.LIST, path)
      Result.failure(e)
    }
  }

  suspend fun fetchClientReviews(clientId: String): Result<List<ServiceReview>> {
    if (clientId.isBlank() || clientId.contains("/")) {
      return Result.failure(IllegalArgumentException("ID de cliente inválido"))
    }
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val path = "reviews"
    return try {
      val snapshot = firestore.collection(path)
        .whereEqualTo("clientId", clientId)
        .get()
        .await()
      val list = snapshot.documents
        .mapNotNull { it.toServiceReviewOrNull() }
        .sortedByDescending { it.createdAt }
      Result.success(list)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.LIST, path)
      Result.failure(e)
    }
  }

  // 12. Observadores Administrativos Completos em Tempo Real (/users, /services, /proposals, /adminUserCredentials)
  fun observeAllUsersForAdmin(): Flow<List<User>> {
    val firestore = db ?: return flowOf(emptyList())
    val path = "users"
    return firestore.collection(path)
      .snapshots()
      .map { snap -> snap.documents.mapNotNull { it.toUserOrNull() } }
      .catch { err ->
        val firestoreError = generateSequence(err) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && err is CancellationException) throw err
        handleFirestoreError(err, OperationType.LIST, path)
        emit(emptyList())
      }
  }

  fun observeAllServicesForAdmin(): Flow<List<ServiceRequest>> {
    val firestore = db ?: return flowOf(emptyList())
    val path = "services"
    return firestore.collection(path)
      .snapshots()
      .map { snap ->
        snap.documents
          .mapNotNull { it.toServiceRequestOrNull() }
          .sortedByDescending { it.createdAt }
      }
      .catch { err ->
        val firestoreError = generateSequence(err) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && err is CancellationException) throw err
        handleFirestoreError(err, OperationType.LIST, path)
        emit(emptyList())
      }
  }

  fun observeAllProposalsForAdmin(): Flow<List<Proposal>> {
    val firestore = db ?: return flowOf(emptyList())
    val path = "proposals"
    return firestore.collection(path)
      .snapshots()
      .map { snap ->
        snap.documents
          .mapNotNull { it.toProposalOrNull() }
          .sortedByDescending { it.createdAt }
      }
      .catch { err ->
        val firestoreError = generateSequence(err) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && err is CancellationException) throw err
        handleFirestoreError(err, OperationType.LIST, path)
        emit(emptyList())
      }
  }

  // 13. Cofre de Credenciais para Gerenciamento Exclusivo do Administrador (/adminUserCredentials/{userId})
  suspend fun saveAdminUserCredential(
    targetDocId: String,
    passwordHash: String?,
    displayPassword: String?,
    updatedBy: String?
  ): Result<Unit> {
    if (targetDocId.isBlank() || targetDocId.contains("/")) {
      return Result.failure(IllegalArgumentException("ID de usuário inválido"))
    }
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val path = "adminUserCredentials/$targetDocId"
    val payload = hashMapOf<String, Any>(
      "userId" to targetDocId.take(128),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    passwordHash?.trim()?.takeIf { it.length >= 16 }?.take(128)?.let {
      payload["passwordHash"] = it
    }
    displayPassword?.takeIf { it.length >= 6 }?.take(128)?.let {
      payload["displayPassword"] = it
    }
    updatedBy?.trim()?.takeIf { it.isNotBlank() }?.take(128)?.let {
      payload["updatedBy"] = it
    }
    return try {
      firestore.collection("adminUserCredentials")
        .document(targetDocId)
        .set(payload, SetOptions.merge())
        .await()
      Result.success(Unit)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteAdminUserCredential(targetDocId: String): Result<Unit> {
    if (targetDocId.isBlank() || targetDocId.contains("/")) {
      return Result.failure(IllegalArgumentException("ID de usuário inválido"))
    }
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val path = "adminUserCredentials/$targetDocId"
    return try {
      firestore.collection("adminUserCredentials").document(targetDocId).delete().await()
      Result.success(Unit)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  fun observeAdminUserCredentials(): Flow<Map<String, Pair<String?, String?>>> {
    val firestore = db ?: return flowOf(emptyMap())
    val path = "adminUserCredentials"
    return firestore.collection(path)
      .snapshots()
      .map { snap ->
        snap.documents.associate { doc ->
          val uid = doc.getString("userId")?.takeIf { it.isNotBlank() } ?: doc.id
          val hash = doc.getString("passwordHash")?.takeIf { it.isNotBlank() }
          val display = doc.getString("displayPassword")?.takeIf { it.isNotBlank() }
          uid to (hash to display)
        }
      }
      .catch { err ->
        val firestoreError = generateSequence(err) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && err is CancellationException) throw err
        handleFirestoreError(err, OperationType.LIST, path)
        emit(emptyMap())
      }
  }

  // 14. Notificações em Tempo Real (/users/{userId}/notifications/{notificationId})
  private fun DocumentSnapshot.toAppNotificationOrNull(fallbackRecipientId: String): AppNotification? {
    return try {
      val title = getString("title")?.takeIf { it.isNotBlank() } ?: return null
      val message = getString("message")?.takeIf { it.isNotBlank() } ?: return null
      val typeStr = getString("type") ?: NotificationType.STATUS_ATUALIZADO.name
      val type = try {
        NotificationType.valueOf(typeStr)
      } catch (_: Exception) {
        NotificationType.STATUS_ATUALIZADO
      }
      val timestamp = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?.toDate()?.time
        ?: parseTimestampMillis(get("createdAt"))
        ?: System.currentTimeMillis()
      AppNotification(
        id = id,
        recipientId = getString("recipientId")?.takeIf { it.isNotBlank() } ?: fallbackRecipientId,
        title = title,
        message = message,
        type = type,
        serviceId = getString("serviceId")?.takeIf { it.isNotBlank() },
        timestamp = timestamp,
        isRead = getBoolean("isRead") ?: false
      )
    } catch (_: Exception) {
      null
    }
  }

  suspend fun saveUserNotification(
    recipientDocId: String,
    notification: AppNotification,
    senderId: String? = null
  ): Boolean {
    if (recipientDocId.isBlank() || recipientDocId.contains("/") || notification.id.isBlank() || notification.id.contains("/")) {
      return false
    }
    val firestore = db ?: return false
    val path = "users/$recipientDocId/notifications/${notification.id}"
    val payload = hashMapOf<String, Any>(
      "recipientId" to recipientDocId.take(128),
      "title" to notification.title.trim().ifBlank { "Atualização ResolveAi" }.take(200),
      "message" to notification.message.trim().ifBlank { "Nova atualização disponível" }.take(1000),
      "type" to notification.type.name,
      "isRead" to notification.isRead,
      "createdAt" to FieldValue.serverTimestamp()
    )
    notification.serviceId?.trim()?.takeIf { it.isNotBlank() }?.take(128)?.let {
      payload["serviceId"] = it
    }
    senderId?.trim()?.takeIf { it.isNotBlank() }?.take(128)?.let {
      payload["senderId"] = it
    }
    return try {
      firestore.collection("users")
        .document(recipientDocId)
        .collection("notifications")
        .document(notification.id)
        .set(payload)
        .await()
      true
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.CREATE, path)
      false
    }
  }

  fun observeUserNotifications(userId: String): Flow<List<AppNotification>> {
    if (userId.isBlank() || userId.contains("/")) return flowOf(emptyList())
    val firestore = db ?: return flowOf(emptyList())
    val path = "users/$userId/notifications"
    return firestore.collection("users")
      .document(userId)
      .collection("notifications")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .limit(100)
      .snapshots()
      .map { snap -> snap.documents.mapNotNull { it.toAppNotificationOrNull(userId) } }
      .catch { err ->
        val firestoreError = generateSequence(err) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && err is CancellationException) throw err
        handleFirestoreError(err, OperationType.LIST, path)
        emit(emptyList())
      }
  }

  suspend fun markUserNotificationAsRead(userId: String, notificationId: String): Boolean {
    if (userId.isBlank() || userId.contains("/") || notificationId.isBlank() || notificationId.contains("/")) {
      return false
    }
    val firestore = db ?: return false
    val path = "users/$userId/notifications/$notificationId"
    return try {
      firestore.collection("users")
        .document(userId)
        .collection("notifications")
        .document(notificationId)
        .update(
          mapOf(
            "isRead" to true,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        .await()
      true
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.UPDATE, path)
      false
    }
  }

  // 15. Cobranças Pix em Tempo Real (/users/{userId}/pixCharges/{chargeId})
  private fun DocumentSnapshot.toPixRechargeChargeOrNull(fallbackProviderId: String): PixRechargeCharge? {
    return try {
      val txId = getString("txId")?.takeIf { it.isNotBlank() } ?: id
      val amount = (get("amount") as? Number)?.toDouble() ?: return null
      val statusStr = getString("status").orEmpty().uppercase()
      val status = when (statusStr) {
        "PAGO", "PAID", "CONFIRMED", "APPROVED", "CONCLUIDO" -> PixChargeStatus.PAGO
        "EXPIRADO", "EXPIRED" -> PixChargeStatus.EXPIRADO
        "CANCELADO_OU_REJEITADO", "CANCELLED", "REJECTED" -> PixChargeStatus.CANCELADO_OU_REJEITADO
        else -> PixChargeStatus.AGUARDANDO_PAGAMENTO
      }
      val createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?.toDate()?.time
        ?: parseTimestampMillis(get("createdAt"))
        ?: System.currentTimeMillis()
      val expiresAt = parseTimestampMillis(get("expiresAt")) ?: (createdAt + 15 * 60_000L)
      val paidAt = getTimestamp("paidAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?.toDate()?.time
        ?: parseTimestampMillis(get("paidAt"))
      PixRechargeCharge(
        txId = txId,
        providerId = getString("providerId")?.takeIf { it.isNotBlank() } ?: fallbackProviderId,
        pixKey = getString("pixKey").orEmpty(),
        merchantName = getString("merchantName") ?: "ResolveAi Serviços",
        amount = amount,
        brCode = getString("brCode").orEmpty(),
        status = status,
        createdAt = createdAt,
        expiresAt = expiresAt,
        paidAt = paidAt,
        detectedAutomatically = status == PixChargeStatus.PAGO,
        paymentGateway = getString("paymentGateway") ?: "PIX_DIRETO",
        mercadoPagoPaymentId = (get("mercadoPagoPaymentId") as? Number)?.toLong(),
        mpStatus = getString("mpStatus"),
        mpStatusDetail = getString("mpStatusDetail"),
        ticketUrl = getString("ticketUrl"),
        qrCodeBase64 = getString("qrCodeBase64")
      )
    } catch (_: Exception) {
      null
    }
  }

  fun observeUserPixCharges(providerId: String): Flow<List<PixRechargeCharge>> {
    if (providerId.isBlank() || providerId.contains("/")) return flowOf(emptyList())
    val firestore = db ?: return flowOf(emptyList())
    val path = "users/$providerId/pixCharges"
    return firestore.collection("users")
      .document(providerId)
      .collection("pixCharges")
      .snapshots()
      .map { snap ->
        snap.documents
          .mapNotNull { it.toPixRechargeChargeOrNull(providerId) }
          .sortedByDescending { it.createdAt }
      }
      .catch { err ->
        val firestoreError = generateSequence(err) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && err is CancellationException) throw err
        handleFirestoreError(err, OperationType.LIST, path)
        emit(emptyList())
      }
  }

  suspend fun updatePixChargeStatus(
    providerId: String,
    txId: String,
    status: PixChargeStatus,
    mpStatus: String? = null,
    mpStatusDetail: String? = null,
    paidAt: Long? = null
  ): Boolean {
    if (providerId.isBlank() || providerId.contains("/") || txId.isBlank() || txId.contains("/")) {
      return false
    }
    val firestore = db ?: return false
    val path = "users/$providerId/pixCharges/$txId"
    val updates = hashMapOf<String, Any>(
      "status" to status.name,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    mpStatus?.takeIf { it.isNotBlank() }?.let { updates["mpStatus"] = it.take(60) }
    mpStatusDetail?.takeIf { it.isNotBlank() }?.let { updates["mpStatusDetail"] = it.take(120) }
    if (paidAt != null || status == PixChargeStatus.PAGO) {
      updates["paidAt"] = FieldValue.serverTimestamp()
    }
    return try {
      firestore.collection("users")
        .document(providerId)
        .collection("pixCharges")
        .document(txId)
        .update(updates)
        .await()
      true
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.UPDATE, path)
      false
    }
  }

  // 16. Débito Idempotente da Taxa de 10% e Bônus de Boas-Vindas na Carteira do Prestador
  suspend fun recordProviderWalletFeeDebitIfMissing(
    providerId: String,
    serviceId: String,
    serviceTitle: String,
    platformFee: Double
  ): Result<Double> {
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    if (providerId.isBlank() || serviceId.isBlank() || platformFee <= 0.0) {
      return Result.failure(IllegalArgumentException("Dados de débito de taxa inválidos"))
    }
    val providerRef = firestore.collection("users").document(providerId)
    val safeServiceKey = serviceId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    val docId = "tx_fee_$safeServiceKey".take(120)
    val txRef = providerRef.collection("transactions").document(docId)

    return try {
      var newBalanceResult = 0.0
      firestore.runTransaction { tx ->
        val existingDebit = tx.get(txRef)
        val providerSnap = tx.get(providerRef)
        val currentBalance = providerSnap.getDouble("walletBalance") ?: 0.0
        if (existingDebit.exists()) {
          newBalanceResult = currentBalance
          return@runTransaction
        }
        val newBalance = (currentBalance - platformFee).coerceAtLeast(0.0)
        newBalanceResult = newBalance
        if (providerSnap.exists()) {
          tx.update(
            providerRef,
            mapOf(
              "walletBalance" to newBalance,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        }
        tx.set(
          txRef,
          hashMapOf(
            "providerId" to providerId,
            "type" to TransactionType.DEBITO_TAXA_10.name,
            "amount" to platformFee,
            "serviceId" to serviceId.take(128),
            "serviceTitle" to serviceTitle.take(200),
            "description" to "Taxa de 10% referente ao serviço \"$serviceTitle\"".take(300),
            "balanceAfter" to newBalance,
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
      }.await()
      Result.success(newBalanceResult)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.WRITE, "users/$providerId/transactions/$docId")
      Result.failure(e)
    }
  }

  suspend fun recordProviderWelcomeBonusIfMissing(
    providerId: String,
    amount: Double,
    description: String
  ): Result<Double> {
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    if (providerId.isBlank() || amount <= 0.0) {
      return Result.failure(IllegalArgumentException("Dados de bônus inválidos"))
    }
    val providerRef = firestore.collection("users").document(providerId)
    val safeKey = providerId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    val docId = "tx_welcome_$safeKey".take(120)
    val txRef = providerRef.collection("transactions").document(docId)

    return try {
      var newBalanceResult = 0.0
      firestore.runTransaction { tx ->
        val existingBonus = tx.get(txRef)
        val providerSnap = tx.get(providerRef)
        val currentBalance = providerSnap.getDouble("walletBalance") ?: 0.0
        if (existingBonus.exists()) {
          newBalanceResult = currentBalance
          return@runTransaction
        }
        val newBalance = if (currentBalance >= amount) currentBalance else currentBalance + amount
        newBalanceResult = newBalance
        if (providerSnap.exists()) {
          tx.update(
            providerRef,
            mapOf(
              "walletBalance" to newBalance,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        }
        tx.set(
          txRef,
          hashMapOf(
            "providerId" to providerId,
            "type" to TransactionType.RECARGA_PIX.name,
            "amount" to amount,
            "description" to description.take(300),
            "balanceAfter" to newBalance,
            "createdAt" to FieldValue.serverTimestamp()
          )
        )
      }.await()
      Result.success(newBalanceResult)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.WRITE, "users/$providerId/transactions/$docId")
      Result.failure(e)
    }
  }

  // Diretório cross-device de credenciais (/loginDirectory/{lookupKey}) — zero PII, lookup somente por hash SHA-256
  suspend fun saveLoginDirectoryEntry(
    lookupKey: String,
    uid: String,
    authEmail: String,
    aliasAuthEmail: String? = null,
    passwordVerifier: String? = null
  ): Result<Unit> {
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val cleanKey = lookupKey.trim()
    val cleanUid = uid.trim()
    val cleanAuthEmail = FirebaseAuthHelper.normalizeEmail(authEmail)
    if (cleanKey.isBlank() || cleanUid.isBlank() || cleanAuthEmail.isBlank()) {
      return Result.failure(IllegalArgumentException("Dados inválidos para loginDirectory"))
    }
    val path = "loginDirectory/$cleanKey"
    val payload = hashMapOf<String, Any>(
      "uid" to cleanUid.take(128),
      "authEmail" to cleanAuthEmail.take(150),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    aliasAuthEmail?.trim()?.lowercase()?.takeIf { it.isNotBlank() }?.let {
      payload["aliasAuthEmail"] = it.take(150)
    }
    passwordVerifier?.trim()?.takeIf { it.isNotBlank() }?.let {
      payload["passwordVerifier"] = it.take(128)
    }
    return try {
      firestore.collection("loginDirectory")
        .document(cleanKey)
        .set(payload)
        .await()
      Result.success(Unit)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun lookupLoginDirectoryByKey(lookupKey: String): Result<LoginDirectoryEntry?> {
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val cleanKey = lookupKey.trim()
    if (cleanKey.isBlank()) return Result.success(null)
    val path = "loginDirectory/$cleanKey"
    return try {
      val snap = firestore.collection("loginDirectory").document(cleanKey).get().await()
      if (!snap.exists()) {
        Result.success(null)
      } else {
        val uid = snap.getString("uid")?.trim().orEmpty()
        val authEmail = snap.getString("authEmail")?.trim()?.lowercase().orEmpty()
        if (uid.isBlank() || authEmail.isBlank()) {
          Result.success(null)
        } else {
          Result.success(
            LoginDirectoryEntry(
              uid = uid,
              authEmail = authEmail,
              aliasAuthEmail = snap.getString("aliasAuthEmail")?.trim()?.lowercase()?.takeIf { it.isNotBlank() },
              passwordVerifier = snap.getString("passwordVerifier")?.trim()?.takeIf { it.isNotBlank() }
            )
          )
        }
      }
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.GET, path)
      Result.failure(e)
    }
  }

  suspend fun lookupLoginDirectoryByEmail(email: String): Result<LoginDirectoryEntry?> {
    val key = FirebaseAuthHelper.emailLookupKey(email) ?: return Result.success(null)
    return lookupLoginDirectoryByKey(key)
  }

  suspend fun lookupLoginDirectoryByPhone(phone: String, countryCode: String = "+55"): Result<LoginDirectoryEntry?> {
    val key = FirebaseAuthHelper.phoneLookupKey(phone, countryCode) ?: return Result.success(null)
    return lookupLoginDirectoryByKey(key)
  }

  // Referências Diárias de Preço Médio Regional por Categoria (/regionalPriceBenchmarks/{benchmarkId})
  suspend fun fetchRegionalPriceBenchmark(benchmarkId: String): Result<RegionalPriceEstimate?> {
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val cleanId = benchmarkId.trim()
    if (cleanId.isBlank() || cleanId.contains("/")) return Result.success(null)
    val path = "regionalPriceBenchmarks/$cleanId"
    return try {
      val snap = firestore.collection("regionalPriceBenchmarks").document(cleanId).get().await()
      Result.success(if (snap.exists()) snap.toRegionalPriceEstimateOrNull() else null)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.GET, path)
      Result.failure(e)
    }
  }

  suspend fun fetchTodayRegionalBenchmarks(dateKey: String): Result<List<RegionalPriceEstimate>> {
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val cleanDate = dateKey.trim()
    if (cleanDate.isBlank()) return Result.success(emptyList())
    val path = "regionalPriceBenchmarks"
    return try {
      val snapshot = firestore.collection(path)
        .whereEqualTo("dateKey", cleanDate)
        .limit(100)
        .get()
        .await()
      Result.success(snapshot.documents.mapNotNull { it.toRegionalPriceEstimateOrNull() })
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.LIST, path)
      Result.failure(e)
    }
  }

  suspend fun upsertRegionalPriceBenchmark(estimate: RegionalPriceEstimate): Result<Unit> {
    val firestore = db ?: return Result.failure(IllegalStateException("Firestore não inicializado"))
    val cleanId = estimate.id.trim()
    if (cleanId.isBlank() || cleanId.contains("/")) {
      return Result.failure(IllegalArgumentException("ID de benchmark regional inválido"))
    }
    if (estimate.averagePrice < 20.0 || estimate.minPrice < 10.0 || estimate.maxPrice < estimate.averagePrice) {
      return Result.failure(IllegalArgumentException("Faixa de preço regional inválida"))
    }
    val path = "regionalPriceBenchmarks/$cleanId"
    val payload = hashMapOf<String, Any>(
      "id" to cleanId.take(128),
      "dateKey" to estimate.dateKey.trim().take(10),
      "displayDate" to estimate.displayDate.trim().take(20),
      "category" to estimate.category.trim().take(100).ifBlank { "Geral" },
      "regionKey" to estimate.regionKey.trim().take(80).ifBlank { "br_nacional" },
      "regionLabel" to estimate.regionLabel.trim().take(120).ifBlank { "Brasil" },
      "stateCode" to estimate.stateCode.trim().take(4),
      "macroRegion" to estimate.macroRegion.trim().take(40),
      "averagePrice" to estimate.averagePrice.coerceIn(20.0, 50000.0),
      "minPrice" to estimate.minPrice.coerceIn(10.0, estimate.averagePrice),
      "maxPrice" to estimate.maxPrice.coerceIn(estimate.averagePrice, 100000.0),
      "regionalFactor" to estimate.regionalFactor.coerceIn(0.5, 3.0),
      "localSamplesCount" to estimate.localSamplesCount.coerceIn(0, 1_000_000),
      "sourceLabel" to estimate.sourceLabel.trim().take(200),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return try {
      firestore.collection("regionalPriceBenchmarks")
        .document(cleanId)
        .set(payload)
        .await()
      Result.success(Unit)
    } catch (e: Exception) {
      if (e is CancellationException) throw e
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  private fun DocumentSnapshot.toRegionalPriceEstimateOrNull(): RegionalPriceEstimate? {
    val dateKey = getString("dateKey")?.trim().orEmpty()
    val category = getString("category")?.trim().orEmpty()
    val regionKey = getString("regionKey")?.trim().orEmpty()
    val regionLabel = getString("regionLabel")?.trim().orEmpty()
    val averagePrice = (get("averagePrice") as? Number)?.toDouble() ?: return null
    val minPrice = (get("minPrice") as? Number)?.toDouble() ?: (averagePrice * 0.78)
    val maxPrice = (get("maxPrice") as? Number)?.toDouble() ?: (averagePrice * 1.28)
    if (dateKey.isBlank() || category.isBlank() || regionKey.isBlank() || averagePrice <= 0.0) return null
    val updatedAtMillis = when (val raw = get("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)) {
      is Timestamp -> raw.toDate().time
      is Number -> raw.toLong()
      else -> System.currentTimeMillis()
    }
    return RegionalPriceEstimate(
      id = getString("id")?.trim()?.takeIf { it.isNotBlank() } ?: id,
      dateKey = dateKey,
      displayDate = getString("displayDate")?.trim()?.takeIf { it.isNotBlank() }
        ?: RegionalPriceIndexEngine.formatDisplayDate(dateKey),
      category = category,
      regionKey = regionKey,
      regionLabel = regionLabel.ifBlank { "Brasil (Média Nacional)" },
      stateCode = getString("stateCode")?.trim().orEmpty(),
      macroRegion = getString("macroRegion")?.trim().orEmpty(),
      averagePrice = averagePrice,
      minPrice = minPrice,
      maxPrice = maxPrice,
      regionalFactor = (get("regionalFactor") as? Number)?.toDouble() ?: 1.0,
      localSamplesCount = (get("localSamplesCount") as? Number)?.toInt() ?: 0,
      sourceLabel = getString("sourceLabel")?.trim().orEmpty().ifBlank { "Índice regional diário sincronizado" },
      syncedFromCloud = true,
      updatedAt = updatedAtMillis
    )
  }
}
