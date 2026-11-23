package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.ProviderDocuments
import com.example.model.ProviderVerificationStatus
import com.example.model.User
import com.example.model.UserRole
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class AdminDocumentReviewRuleTest : FirestoreEmulatorTestBase() {

  private suspend fun seedAdminMarkerInEmulator(adminUid: String, email: String) = withContext(Dispatchers.IO) {
    val endpoint = "http://$EMULATOR_HOST:$FIRESTORE_PORT/v1/projects/$PROJECT_ID/databases/$databaseId/documents/admins?documentId=$adminUid"
    val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
      requestMethod = "POST"
      setRequestProperty("Authorization", "Bearer owner")
      setRequestProperty("Content-Type", "application/json")
      doOutput = true
    }
    val body = """
      {
        "fields": {
          "role": { "stringValue": "ADMIN" },
          "email": { "stringValue": "$email" }
        }
      }
    """.trimIndent()
    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
    val code = conn.responseCode
    conn.disconnect()
    check(code in 200..299 || code == 409) { "Falha ao semear /admins/$adminUid no emulador (HTTP $code)" }
  }

  @Test
  fun providerSaveProfileWithDocuments_succeedsAsPendingAnalysis() = runBlocking {
    val providerUid = signInTestUser("provider_${UUID.randomUUID().toString().take(8)}@test.com")
    val ds = FirestoreMarketplaceDataSource { firestore }
    val provider = User(
      id = providerUid,
      name = "Lucas Eletricista",
      email = "lucas@test.com",
      phone = "(11) 98765-4321",
      role = UserRole.PRESTADOR,
      category = "Eletricista",
      city = "São Paulo, SP",
      document = "123.456.789-00",
      walletBalance = 50.0,
      verificationStatus = ProviderVerificationStatus.PENDENTE_ANALISE,
      documents = ProviderDocuments(
        rgOrCnhNumber = "CNH 04829103948",
        rgOrCnhFileName = "cnh_lucas.pdf",
        criminalRecordCertNumber = "CERT-2026-99120-PF",
        criminalRecordFileName = "antecedentes_lucas.pdf",
        selfieFileName = "selfie_lucas.jpg"
      ),
      firebaseUid = providerUid
    )

    val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) { ds.saveUserProfile(provider, targetDocId = providerUid) }
    assertTrue("Prestador deve conseguir salvar perfil e documentos como PENDENTE_ANALISE", saveResult.isSuccess)
  }

  @Test
  fun nonAdmin_approveOrRejectProvider_failsWithPermissionDenied() = runBlocking {
    val providerUid = signInTestUser("provider_target_${UUID.randomUUID().toString().take(6)}@test.com")
    val ds = FirestoreMarketplaceDataSource { firestore }
    val provider = User(
      id = providerUid,
      name = "Fernanda Pintora",
      email = "fernanda@test.com",
      phone = "(11) 98888-7777",
      role = UserRole.PRESTADOR,
      category = "Pintura",
      verificationStatus = ProviderVerificationStatus.PENDENTE_ANALISE,
      documents = ProviderDocuments(
        rgOrCnhNumber = "RG 12.345.678-9",
        criminalRecordCertNumber = "CERT-PF-001",
        selfieFileName = "selfie_fernanda.jpg"
      ),
      firebaseUid = providerUid
    )
    withTimeout(DEFAULT_TIMEOUT_MS) { ds.saveUserProfile(provider, targetDocId = providerUid).getOrThrow() }

    //Outro usuário comum (não-admin) tenta aprovar a prestadora
    val regularUid = signInTestUser("regular_client_${UUID.randomUUID().toString().take(6)}@test.com")
    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      ds.reviewProviderDocumentsInFirestore(
        provider = provider,
        decision = ProviderVerificationStatus.APROVADO,
        reason = null,
        idDocVerified = true,
        criminalRecordVerified = true,
        selfieVerified = true,
        adminUid = regularUid,
        adminEmail = "regular@test.com"
      )
    }

    assertTrue("Usuário comum NÃO pode aprovar cadastro no Firestore", result.isFailure)
    val fsErr = result.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull("Esperado FirebaseFirestoreException", fsErr)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr?.code)
  }

  @Test
  fun admin_rejectAndApproveProviderDocuments_persistsStatusAndImmutableAuditInFirestore() = runBlocking {
    val providerUid = signInTestUser("provider_review_${UUID.randomUUID().toString().take(6)}@test.com")
    val ds = FirestoreMarketplaceDataSource { firestore }
    val provider = User(
      id = providerUid,
      name = "Marcos Encanador",
      email = "marcos@test.com",
      phone = "(11) 99123-4567",
      role = UserRole.PRESTADOR,
      category = "Hidráulica",
      verificationStatus = ProviderVerificationStatus.PENDENTE_ANALISE,
      documents = ProviderDocuments(
        rgOrCnhNumber = "CNH 7788990011",
        criminalRecordCertNumber = "CERT-PF-7788",
        selfieFileName = "selfie_marcos.jpg"
      ),
      firebaseUid = providerUid
    )
    withTimeout(DEFAULT_TIMEOUT_MS) { ds.saveUserProfile(provider, targetDocId = providerUid).getOrThrow() }

    // Autentica Admin e semeia marcador /admins/{adminUid} no emulador
    val adminEmail = "admin_${UUID.randomUUID().toString().take(6)}@resolveai.com"
    val adminUid = signInTestUser(adminEmail)
    seedAdminMarkerInEmulator(adminUid, adminEmail)

    // 1. Admin reprova documentos com motivo detalhado
    val rejectResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      ds.reviewProviderDocumentsInFirestore(
        provider = provider,
        decision = ProviderVerificationStatus.REJEITADO,
        reason = "Selfie com documento desfocada",
        idDocVerified = true,
        criminalRecordVerified = true,
        selfieVerified = false,
        adminUid = adminUid,
        adminEmail = adminEmail
      )
    }
    assertTrue("Admin deve conseguir reprovar documentos no Firestore", rejectResult.isSuccess)
    assertEquals("REJEITADO", rejectResult.getOrThrow().decision)

    // 2. Admin aprova após conferência completa
    val approveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      ds.reviewProviderDocumentsInFirestore(
        provider = provider,
        decision = ProviderVerificationStatus.APROVADO,
        reason = null,
        idDocVerified = true,
        criminalRecordVerified = true,
        selfieVerified = true,
        adminUid = adminUid,
        adminEmail = adminEmail
      )
    }
    assertTrue("Admin deve conseguir aprovar documentos no Firestore", approveResult.isSuccess)
    assertEquals("APROVADO", approveResult.getOrThrow().decision)

    // 3. Confirma que fetchProvidersForAdmin traz o prestador atualizado como APROVADO
    val fetched = withTimeout(DEFAULT_TIMEOUT_MS) { ds.fetchProvidersForAdmin().getOrThrow() }
    val updated = fetched.find { it.id == providerUid }
    assertNotNull("Prestador analisado deve ser retornado na consulta do Admin", updated)
    assertEquals(ProviderVerificationStatus.APROVADO, updated?.verificationStatus)
    assertEquals(true, updated?.documents?.selfieVerified)

    // 4. Confirma que o histórico de auditoria contém os dois registros
    val history = withTimeout(FLOW_TIMEOUT_MS) {
      ds.observeProviderReviewHistory(providerUid).first { it.size >= 2 }
    }
    assertTrue(history.any { it.decision == "REJEITADO" && it.reason == "Selfie com documento desfocada" })
    assertTrue(history.any { it.decision == "APROVADO" })
  }

  @Test
  fun unauthenticated_fetchProvidersForAdmin_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val ds = FirestoreMarketplaceDataSource { firestore }

    val result = withTimeout(DEFAULT_TIMEOUT_MS) { ds.fetchProvidersForAdmin() }
    assertTrue("Consulta sem autenticação deve falhar", result.isFailure)
    val fsErr = result.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(fsErr)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr?.code)
  }

  @Test
  fun firestoreHelper_managesUserProfilesServicesAndChatMessages() = runBlocking {
    val clientEmail = "helper_client_${UUID.randomUUID().toString().take(6)}@test.com"
    val clientUid = signInTestUser(clientEmail)
    val helper = FirestoreHelper(firestore = firestore, databaseId = databaseId)
    assertTrue(helper.isInitialized)
    assertNotNull(helper.userDocument(clientUid))

    val profile = User(
      id = clientUid,
      name = "Mariana Cliente",
      email = clientEmail,
      phone = "(11) 98765-4321",
      role = UserRole.CLIENTE,
      city = "São Paulo, SP",
      firebaseUid = clientUid
    )
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { helper.saveUserProfile(profile, targetDocId = clientUid) }.isSuccess)
    val loadedProfile = withTimeout(DEFAULT_TIMEOUT_MS) { helper.getUserProfile(clientUid) }.getOrNull()
    assertEquals("Mariana Cliente", loadedProfile?.name)

    assertTrue(
      withTimeout(DEFAULT_TIMEOUT_MS) {
        helper.updateUserProfileFields(clientUid, mapOf("city" to "Campinas, SP"))
      }.isSuccess
    )
    val observedProfile = withTimeout(FLOW_TIMEOUT_MS) {
      helper.observeUserProfile(clientUid).first { it?.city == "Campinas, SP" }
    }
    assertEquals("Campinas, SP", observedProfile?.city)

    val serviceId = "srv_helper_${UUID.randomUUID().toString().take(6)}"
    assertNotNull(helper.serviceDocument(serviceId))
    assertNotNull(helper.chatMessagesCollection(serviceId))

    val service = com.example.model.ServiceRequest(
      id = serviceId,
      clientId = clientUid,
      clientName = "Mariana Cliente",
      title = "Instalação de ventilador de teto",
      category = "Eletricista",
      description = "Instalação com ponto já existente.",
      location = "Campinas, SP",
      proposedBudget = 180.0,
      status = com.example.model.ServiceStatus.ABERTO
    )
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { helper.saveService(service) }.isSuccess)
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { helper.updateServiceBudget(serviceId, 210.0) }.isSuccess)
    val updatedSrv = withTimeout(DEFAULT_TIMEOUT_MS) { helper.getService(serviceId) }.getOrNull()
    assertEquals(210.0, updatedSrv?.proposedBudget ?: 0.0, 0.01)

    // Activate accepted service status so chat participant rules allow messages
    assertTrue(
      withTimeout(DEFAULT_TIMEOUT_MS) {
        helper.updateServiceFields(
          serviceId,
          mapOf(
            "status" to com.example.model.ServiceStatus.ACEITO.name,
            "assignedProviderId" to "pro_helper_1",
            "acceptedProviderId" to "pro_helper_1",
            "assignedProviderName" to "Roberto Mendes",
            "acceptedProviderName" to "Roberto Mendes"
          )
        )
      }.isSuccess
    )

    val msgId = "msg_helper_${UUID.randomUUID().toString().take(6)}"
    val chatMsg = com.example.model.ChatMessage(
      id = msgId,
      serviceId = serviceId,
      senderId = clientUid,
      senderName = "Mariana Cliente",
      senderRole = UserRole.CLIENTE,
      content = "Olá! Podemos combinar o atendimento para amanhã às 14h?"
    )
    assertTrue(withTimeout(DEFAULT_TIMEOUT_MS) { helper.sendChatMessage(serviceId, chatMsg) }.isSuccess)
    val messages = withTimeout(FLOW_TIMEOUT_MS) {
      helper.observeChatMessages(serviceId).first { list -> list.any { it.id == msgId } }
    }
    assertEquals(1, messages.count { it.id == msgId })
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
