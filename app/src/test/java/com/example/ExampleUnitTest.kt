package com.example

import com.example.data.InsufficientBalanceException
import com.example.data.MarketplaceRepository
import com.example.model.ContactFilter
import com.example.model.ProposalStatus
import com.example.model.ProviderVerificationStatus
import com.example.model.ServiceRequest
import com.example.model.ServiceStatus
import com.example.model.User
import com.example.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketplaceUnitTest {

  @Test
  fun testContactFilter_blocksPhoneNumbers() {
    val phone1 = ContactFilter.evaluate("Me liga no (11) 98765-4321")
    assertFalse("Deve bloquear telefone com DDD e parênteses", phone1.isAllowed)

    val phone2 = ContactFilter.evaluate("Meu whats é 11988887777")
    assertFalse("Deve bloquear celular direto com zap", phone2.isAllowed)

    val phone3 = ContactFilter.evaluate("chama la 98765-4321")
    assertFalse("Deve bloquear telefone com keyword", phone3.isAllowed)
  }

  @Test
  fun testContactFilter_blocksEmails() {
    val emailResult = ContactFilter.evaluate("Manda a planta para arquiteto@empresa.com.br")
    assertFalse("Deve bloquear e-mails externos", emailResult.isAllowed)

    val obfuscated = ContactFilter.evaluate("meu contato é roberto [at] gmail.com")
    assertFalse("Deve bloquear e-mail obfuscado", obfuscated.isAllowed)
  }

  @Test
  fun testContactFilter_blocksSocialHandlesAndUrls() {
    val instaResult = ContactFilter.evaluate("Veja meus trabalhos no @eletricista_top")
    assertFalse("Deve bloquear arroba de rede social", instaResult.isAllowed)

    val urlResult = ContactFilter.evaluate("Acesse www.reparosrapidos.com.br")
    assertFalse("Deve bloquear links externos", urlResult.isAllowed)
  }

  @Test
  fun testContactFilter_allowsSafeMessages() {
    val safe1 = ContactFilter.evaluate("Olá! Posso ir amanhã às 14h para avaliar o serviço?")
    assertTrue("Mensagem de agendamento normal deve ser permitida", safe1.isAllowed)

    val safe2 = ContactFilter.evaluate("O valor de R$ 300,00 inclui todo o material necessário.")
    assertTrue("Mensagem com valores em reais deve ser permitida", safe2.isAllowed)
  }

  @Test
  fun testProposalAcceptance_debitsTenPercentFeeFromProviderWallet() {
    val repo = MarketplaceRepository()

    // Find proposal prop_1 (Roberto Mendes, initial balance R$ 120.00, proposed R$ 320, counter-offer R$ 300)
    val proposal = repo.proposals.value.first { it.id == "prop_1" }
    val initialBalance = repo.users.value.first { it.id == proposal.providerId }.walletBalance
    assertEquals(120.00, initialBalance, 0.01)

    // Current price is R$ 300.00 -> 10% fee is R$ 30.00
    val expectedFee = 300.00 * 0.10
    val result = repo.acceptProposalAndDebitFee("prop_1")

    assertTrue("Aceitação deve ter sucesso pois saldo R$ 120 >= R$ 30", result.isSuccess)
    assertEquals(expectedFee, result.getOrNull() ?: 0.0, 0.01)

    // Verify provider new balance = 120.00 - 30.00 = 90.00
    val updatedBalance = repo.users.value.first { it.id == proposal.providerId }.walletBalance
    assertEquals(90.00, updatedBalance, 0.01)

    // Verify service status updated to EM_EXECUCAO ("Em Andamento")
    val updatedService = repo.services.value.first { it.id == proposal.serviceId }
    assertEquals(ServiceStatus.EM_EXECUCAO, updatedService.status)
    assertEquals("Em Andamento", updatedService.status.label)
    assertEquals(300.00, updatedService.finalAgreedPrice ?: 0.0, 0.01)
    assertEquals(30.00, updatedService.platformFee ?: 0.0, 0.01)
  }

  @Test
  fun testProposalAcceptance_blocksWhenInsufficientBalance() {
    val repo = MarketplaceRepository()

    // Nova regra: saldo > R$ 1,00 permite aceitar o serviço mesmo ficando negativo,
    // descontando o saldo negativo automaticamente na próxima recarga.
    // pro_2 começa com R$ 35,00 (> R$ 1,00) e prop_2 tem taxa de R$ 55,00 -> fica com -R$ 20,00.
    val firstAccept = repo.acceptProposalAndDebitFee("prop_2")
    assertTrue("Saldo de R$ 35,00 (> R$ 1,00) deve permitir aceitar ficando negativo", firstAccept.isSuccess)
    val negativeBalance = repo.users.value.first { it.id == "pro_2" }.walletBalance
    assertEquals(-20.00, negativeBalance, 0.01)

    // Com saldo negativo (-R$ 20,00 <= R$ 1,00), novo aceite deve ser bloqueado
    repo.switchUser("cli_1")
    val extraSrv = repo.createServiceRequest(
      title = "Instalação de lustre",
      category = "Eletricista",
      description = "Lustre na sala de jantar",
      location = "Centro - SP",
      proposedBudget = 200.0,
      urgency = "Média"
    )
    repo.switchUser("pro_2")
    val extraProp = repo.submitProposal(
      serviceId = extraSrv.id,
      proposedPrice = 200.0,
      estimatedDays = 1,
      description = "Posso fazer hoje"
    )
    val blockedAttempt = repo.acceptProposalAndDebitFee(extraProp.id)
    assertTrue("Saldo <= R$ 1,00 deve bloquear novo aceite", blockedAttempt.isFailure)
    val exception = blockedAttempt.exceptionOrNull() as? InsufficientBalanceException
    assertTrue(exception != null)
    assertEquals(-20.00, exception?.currentBalance ?: 0.0, 0.01)
    assertEquals(21.01, exception?.missingAmount ?: 0.0, 0.01)

    // Recarga de R$ 50,00 desconta automaticamente os -R$ 20,00 deixando saldo positivo R$ 30,00
    val rechargedBalance = repo.rechargeWallet("pro_2", 50.00)
    assertEquals(30.00, rechargedBalance, 0.01)
    assertEquals(30.00, repo.users.value.first { it.id == "pro_2" }.walletBalance, 0.01)
    val rechargeTx = repo.walletTransactions.value.first { it.providerId == "pro_2" }
    assertTrue(rechargeTx.description.contains("Desconto automático de R$ 20", ignoreCase = true) || rechargeTx.description.contains("20.00"))

    // Agora com R$ 30,00 (> R$ 1,00) pode aceitar novamente (taxa R$ 20,00 -> fica R$ 10,00)
    val secondAttempt = repo.acceptProposalAndDebitFee(extraProp.id)
    assertTrue("Deve ter sucesso após recarga quitar saldo negativo", secondAttempt.isSuccess)
    val finalBalance = repo.users.value.first { it.id == "pro_2" }.walletBalance
    assertEquals(10.00, finalBalance, 0.01)
  }

  @Test
  fun testUpdateServiceBudget() {
    val repo = MarketplaceRepository()
    val initialBudget = repo.services.value.first { it.id == "srv_1" }.proposedBudget
    assertEquals(280.00, initialBudget, 0.01)

    val updated = repo.updateServiceBudget("srv_1", 350.00)
    assertTrue("Deve atualizar orçamento com sucesso", updated)
    val newBudget = repo.services.value.first { it.id == "srv_1" }.proposedBudget
    assertEquals(350.00, newBudget, 0.01)
  }

  @Test
  fun testCreateService_generatesNotificationForProviders() {
    val repo = MarketplaceRepository()
    val initialCount = repo.notifications.value.size

    repo.createServiceRequest(
      title = "Troca de fechadura tetra",
      category = "Chaveiro",
      description = "Fechadura emperrada na porta principal",
      location = "Moema - São Paulo, SP",
      proposedBudget = 150.00,
      urgency = "Alta"
    )

    assertEquals(initialCount + 1, repo.notifications.value.size)
    val latestNotif = repo.notifications.value.first()
    assertEquals("🔔 Novo Serviço Disponível: Chaveiro", latestNotif.title)
    assertTrue(latestNotif.message.contains("150,00") || latestNotif.message.contains("150.00"))
  }

  @Test
  fun testAdminApproveProvider_updatesVerificationStatus() {
    val repo = MarketplaceRepository()
    val pendingUser = repo.users.value.first { it.id == "pro_pending" }
    assertEquals(com.example.model.ProviderVerificationStatus.PENDENTE_ANALISE, pendingUser.verificationStatus)

    repo.approveProvider("pro_pending", idDocVerified = true, criminalRecordVerified = true, selfieVerified = true)
    val approvedUser = repo.users.value.first { it.id == "pro_pending" }
    assertEquals(com.example.model.ProviderVerificationStatus.APROVADO, approvedUser.verificationStatus)
    assertTrue(approvedUser.documents?.idDocVerified == true)
    assertTrue(approvedUser.documents?.criminalRecordVerified == true)
    assertTrue(approvedUser.documents?.selfieVerified == true)
    assertTrue(repo.documentReviewAudits.value.any { it.providerId == "pro_pending" && it.decision == "APROVADO" })
  }

  @Test
  fun testAdminRejectProvider_recordsReasonChecklistAndAudit() {
    val repo = MarketplaceRepository()
    val pendingUser = repo.users.value.first { it.id == "pro_pending_2" }
    assertEquals(com.example.model.ProviderVerificationStatus.PENDENTE_ANALISE, pendingUser.verificationStatus)

    repo.rejectProvider(
      providerId = "pro_pending_2",
      reason = "Foto do RG com reflexo ilegível",
      idDocVerified = false,
      criminalRecordVerified = true,
      selfieVerified = true
    )

    val rejectedUser = repo.users.value.first { it.id == "pro_pending_2" }
    assertEquals(com.example.model.ProviderVerificationStatus.REJEITADO, rejectedUser.verificationStatus)
    assertEquals("Foto do RG com reflexo ilegível", rejectedUser.documents?.rejectionReason)
    assertFalse(rejectedUser.documents?.idDocVerified ?: true)
    assertTrue(rejectedUser.documents?.criminalRecordVerified == true)
    assertTrue(
      repo.documentReviewAudits.value.any {
        it.providerId == "pro_pending_2" &&
          it.decision == "REJEITADO" &&
          it.reason == "Foto do RG com reflexo ilegível"
      }
    )
  }

  @Test
  fun testSmsVerification_validatesCode() {
    val repo = MarketplaceRepository()
    val phone = "(11) 99999-8888"
    val code = repo.sendSmsCode(phone)

    assertTrue("Código gerado deve ser válido", repo.verifySmsCode(phone, code))
    assertTrue("Código mestre de teste 123456 deve ser aceito", repo.verifySmsCode(phone, "123456"))
    assertFalse("Código errado deve falhar", repo.verifySmsCode(phone, "000000"))
  }

  @Test
  fun testAuthGate_requiresSmsLoginAndSupportsLogout() {
    val repo = MarketplaceRepository()
    assertFalse("Aplicativo deve iniciar bloqueado exigindo autenticação SMS", repo.isAuthenticated.value)

    // Lookup by formatted or E.164 phone
    val foundFormatted = repo.findUserByPhone("(11) 99333-4455")
    val foundE164 = repo.findUserByPhone("+5511993334455")
    assertEquals("pro_1", foundFormatted?.id)
    assertEquals("pro_1", foundE164?.id)

    // SMS verification + login
    val code = repo.sendSmsCode("(11) 99333-4455")
    assertTrue(repo.verifySmsCode("(11) 99333-4455", code))
    val loggedUser = repo.loginWithVerifiedPhone("+5511993334455")
    assertTrue("Login verificado por SMS deve autenticar a sessão", repo.isAuthenticated.value)
    assertEquals("pro_1", loggedUser?.id)
    assertEquals("pro_1", repo.currentUserId.value)

    // Logout must lock the app again
    repo.logout()
    assertFalse("Logout deve retornar ao bloqueio de autenticação", repo.isAuthenticated.value)
  }

  @Test
  fun testRegisterUser_authenticatesSessionAfterSms() {
    val repo = MarketplaceRepository()
    assertFalse(repo.isAuthenticated.value)

    val newClient = repo.registerUser(
      name = "Juliana Costa",
      email = "juliana@email.com",
      phone = "(11) 98888-1234",
      role = UserRole.CLIENTE
    )

    assertTrue("Cadastro concluído após SMS deve liberar acesso ao app", repo.isAuthenticated.value)
    assertEquals(newClient.id, repo.currentUserId.value)
  }

  @Test
  fun testGoogleSignIn_authenticatesExistingAndNewUsers() {
    val repo = MarketplaceRepository()
    assertFalse(repo.isAuthenticated.value)

    // 1. Existing user login by Google email
    val existing = repo.loginWithGoogleAccount(
      uid = "google_uid_carlos",
      email = "carlos.eduardo@email.com",
      displayName = "Carlos Eduardo"
    )
    assertEquals("cli_1", existing?.id)
    assertTrue(repo.isAuthenticated.value)
    assertEquals("cli_1", repo.currentUserId.value)

    // Logout
    repo.logout()
    assertFalse(repo.isAuthenticated.value)

    // 2. New Provider registration via Google email
    val newProvider = repo.registerUserWithGoogle(
      uid = "google_uid_novo",
      email = "novo.prestador@gmail.com",
      displayName = "Lucas Eletricista",
      role = UserRole.PRESTADOR,
      category = "Eletricista Residencial & Predial",
      rgOrCnhNumber = "CNH 12345678900"
    )
    assertTrue(repo.isAuthenticated.value)
    assertEquals(newProvider.id, repo.currentUserId.value)
    assertEquals(50.0, newProvider.walletBalance, 0.01)
    assertEquals("google_uid_novo", newProvider.firebaseUid)
  }

  @Test
  fun testFirebasePhoneAuthManager_formatsE164AndValidatesInputs() {
    val manager = com.example.data.FirebasePhoneAuthManager(authProvider = { null })
    assertEquals("+5511981112233", manager.formatToE164("(11) 98111-2233", "+55"))
    assertEquals("+5511981112233", manager.formatToE164("+55 11 98111-2233", "+55"))
    assertEquals("+351912345678", manager.formatToE164("912 345 678", "+351"))

    var capturedType: com.example.data.PhoneAuthErrorType? = null
    manager.sendVerificationCode(
      activity = null,
      phone = "12345",
      countryCode = "+55",
      onSuccess = { _, _ -> },
      onError = { _, type -> capturedType = type }
    )
    assertEquals(com.example.data.PhoneAuthErrorType.INVALID_PHONE, capturedType)

    var otpErr: String? = null
    manager.verifySmsCode(
      verificationId = "verif_123",
      code = "123",
      onSuccess = {},
      onError = { otpErr = it }
    )
    assertTrue("Código menor que 6 dígitos deve retornar erro", otpErr?.contains("6 dígitos") == true)
  }

  @Test
  fun testGooglePlacesService_formatsAndMapsPredictions() {
    assertFalse(com.example.data.GooglePlacesService.hasConfiguredApiKey(""))
    assertFalse(com.example.data.GooglePlacesService.hasConfiguredApiKey("MY_MAPS_API_KEY"))
    assertTrue(com.example.data.GooglePlacesService.hasConfiguredApiKey("AIzaSyFakeValidKeyFormat1234"))

    val cityResponse = com.example.data.PlacesAutocompleteResponse(
      status = "OK",
      predictions = listOf(
        com.example.data.PlacePredictionDto(
          placeId = "place_campinas",
          description = "Campinas, SP, Brasil",
          structuredFormatting = com.example.data.StructuredFormattingDto(
            mainText = "Campinas",
            secondaryText = "SP, Brasil"
          )
        )
      )
    )
    val cityMapped = com.example.data.GooglePlacesService.mapPlacesResponse(
      cityResponse,
      com.example.data.LocationAutocompleteMode.CITY
    )
    assertEquals(1, cityMapped.suggestions.size)
    assertEquals("Campinas, SP", cityMapped.suggestions.first().formattedValue)

    val addressResponse = com.example.data.PlacesAutocompleteResponse(
      status = "OK",
      predictions = listOf(
        com.example.data.PlacePredictionDto(
          placeId = "place_paulista",
          description = "Av. Paulista, 1000 - Bela Vista, São Paulo - SP, Brasil",
          structuredFormatting = com.example.data.StructuredFormattingDto(
            mainText = "Av. Paulista, 1000",
            secondaryText = "Bela Vista, São Paulo - SP, Brasil"
          )
        )
      )
    )
    val addressMapped = com.example.data.GooglePlacesService.mapPlacesResponse(
      addressResponse,
      com.example.data.LocationAutocompleteMode.ADDRESS
    )
    assertEquals(1, addressMapped.suggestions.size)
    assertEquals(
      "Av. Paulista, 1000 - Bela Vista, São Paulo - SP",
      addressMapped.suggestions.first().formattedValue
    )

    val deniedResponse = com.example.data.PlacesAutocompleteResponse(
      status = "REQUEST_DENIED",
      errorMessage = "API key invalid"
    )
    val deniedMapped = com.example.data.GooglePlacesService.mapPlacesResponse(
      deniedResponse,
      com.example.data.LocationAutocompleteMode.ADDRESS
    )
    assertTrue(deniedMapped.requiresApiKeyConfig)
  }

  @Test
  fun testOrderParticipants_syncPhotoRatingAndName() {
    val repo = MarketplaceRepository()
    repo.switchUser("cli_1")
    val updatedClient = repo.getCurrentUser().copy(
      name = "Carlos Eduardo Atualizado",
      photoUrl = "https://example.com/carlos.jpg"
    )
    repo.updateUserProfile(updatedClient)

    val createdService = repo.createServiceRequest(
      title = "Troca de tomada dupla",
      category = "Eletricista",
      description = "Instalação de tomada 20A na cozinha",
      location = "Pinheiros - São Paulo, SP",
      proposedBudget = 150.0,
      urgency = "Média"
    )
    assertEquals("Carlos Eduardo Atualizado", createdService.clientName)
    assertEquals("https://example.com/carlos.jpg", createdService.clientPhotoUrl)
    assertEquals(4.9, createdService.clientRating, 0.01)

    repo.switchUser("pro_1")
    val updatedProvider = repo.getCurrentUser().copy(
      photoUrl = "https://example.com/roberto.jpg"
    )
    repo.updateUserProfile(updatedProvider)

    val proposal = repo.submitProposal(
      serviceId = createdService.id,
      proposedPrice = 160.0,
      estimatedDays = 1,
      description = "Faço hoje mesmo"
    )
    assertEquals("Roberto Mendes", proposal.providerName)
    assertEquals("https://example.com/roberto.jpg", proposal.providerPhotoUrl)
    assertEquals(4.95, proposal.providerRating, 0.01)

    repo.switchUser("cli_1")
    val result = repo.acceptProposalAndDebitFee(proposal.id)
    assertTrue(result.isSuccess)

    val updatedOrder = repo.services.value.first { it.id == createdService.id }
    assertEquals("Roberto Mendes", updatedOrder.acceptedProviderName)
    assertEquals("https://example.com/roberto.jpg", updatedOrder.acceptedProviderPhotoUrl)
    assertEquals(4.95, updatedOrder.acceptedProviderRating ?: 0.0, 0.01)
  }

  @Test
  fun testLocalPersistence_preservesOrdersAndSessionAcrossRestart() {
    val tempDir = kotlin.io.path.createTempDirectory("resolveai_persist_test").toFile()
    try {
      val store1 = com.example.data.MarketplaceLocalStore(tempDir)
      val repo1 = MarketplaceRepository(localStore = store1)

      // 1. Register new Google user & create an order
      val googleUser = repo1.registerUserWithGoogle(
        uid = "firebase_uid_persist_1",
        email = "cliente.persist@gmail.com",
        displayName = "Cliente Persistente",
        phone = "(11) 97777-8888",
        role = UserRole.CLIENTE,
        photoUrl = "https://example.com/persist.jpg"
      )
      assertEquals("firebase_uid_persist_1", googleUser.id)

      val createdOrder = repo1.createServiceRequest(
        title = "Instalação de ar-condicionado split",
        category = "Refrigeração",
        description = "Quarto casal 12000 BTUs",
        location = "Moema - São Paulo, SP",
        proposedBudget = 450.0,
        urgency = "Alta"
      )
      repo1.updateServiceBudget(createdOrder.id, 520.0)

      // 2. Simulate app exit and cold restart with fresh repository instance
      val store2 = com.example.data.MarketplaceLocalStore(tempDir)
      val repo2 = MarketplaceRepository(localStore = store2)

      assertTrue("Sessão autenticada deve ser restaurada ao reabrir o app", repo2.isAuthenticated.value)
      assertEquals(googleUser.id, repo2.currentUserId.value)
      assertEquals("Cliente Persistente", repo2.getCurrentUser().name)

      val restoredOrder = repo2.services.value.find { it.id == createdOrder.id }
      assertTrue("Pedido criado antes de sair do app deve continuar salvo", restoredOrder != null)
      assertEquals(520.0, restoredOrder?.proposedBudget ?: 0.0, 0.01)
      assertEquals("Instalação de ar-condicionado split", restoredOrder?.title)
      assertTrue(repo2.getCurrentUser().ownsId(restoredOrder?.clientId))

      // 3. Simulate explicit logout -> app restart -> login again -> order must still exist
      repo2.logout()
      val repo3 = MarketplaceRepository(localStore = com.example.data.MarketplaceLocalStore(tempDir))
      assertFalse("Após logout explícito o app deve permanecer bloqueado", repo3.isAuthenticated.value)

      val relogged = repo3.loginWithGoogleAccount(
        uid = "firebase_uid_persist_1",
        email = "cliente.persist@gmail.com",
        displayName = "Cliente Persistente"
      )
      assertEquals(googleUser.id, relogged?.id)
      assertTrue("Pedido salvo deve continuar visível ao entrar novamente", repo3.services.value.any { it.id == createdOrder.id })
    } finally {
      tempDir.deleteRecursively()
    }
  }

  @Test
  fun testChatMessages_syncSenderIdWithOwnsIdAndTriggerNotificationsForActions() {
    val repo = MarketplaceRepository()
    repo.switchUser("cli_1")
    val client = repo.getCurrentUser()
    assertTrue(client.ownsId("cli_1"))

    val service = repo.createServiceRequest(
      title = "Revisão elétrica no quadro",
      category = "Eletricista",
      description = "Disjuntor desarmando com frequência",
      location = "Pinheiros - São Paulo, SP",
      proposedBudget = 250.0,
      urgency = "Alta"
    )

    // 1. Provider submits proposal -> Client gets PROPOSTA_RECEBIDA notification
    repo.switchUser("pro_1")
    val proposal = repo.submitProposal(
      serviceId = service.id,
      proposedPrice = 240.0,
      estimatedDays = 1,
      description = "Atendo amanhã cedo"
    )
    assertTrue(
      "Cliente deve receber notificação de nova proposta",
      repo.notifications.value.any {
        it.type == com.example.model.NotificationType.PROPOSTA_RECEBIDA &&
          it.serviceId == service.id &&
          client.ownsId(it.recipientId)
      }
    )

    // 2. Client sends counter-offer -> Provider gets PROPOSTA_RECEBIDA notification
    repo.switchUser("cli_1")
    assertTrue(repo.sendCounterOffer(proposal.id, 220.0, "Consegue fechar por R$ 220?"))
    assertTrue(
      "Prestador deve receber notificação de contraproposta",
      repo.notifications.value.any {
        it.type == com.example.model.NotificationType.PROPOSTA_RECEBIDA &&
          it.serviceId == service.id &&
          it.recipientId == "pro_1"
      }
    )

    // 3. Client accepts proposal -> Provider gets PROPOSTA_ACEITA notification + system chat message
    val acceptResult = repo.acceptProposalAndDebitFee(proposal.id)
    assertTrue(acceptResult.isSuccess)
    assertTrue(
      "Prestador deve receber notificação de proposta aceita",
      repo.notifications.value.any {
        it.type == com.example.model.NotificationType.PROPOSTA_ACEITA &&
          it.serviceId == service.id &&
          it.recipientId == "pro_1"
      }
    )
    assertTrue(
      "Chat deve conter aviso de sistema após aceite",
      repo.chatMessages.value.any { it.serviceId == service.id && it.isSystemNotice }
    )

    // 4. Bidirectional chat messages between client and provider
    val sentByClient = repo.addChatMessage(
      serviceId = service.id,
      content = "Olá Roberto, pode chegar às 9h?",
      isSystem = false
    )
    assertTrue("SenderId da mensagem do cliente deve pertencer ao cliente", client.ownsId(sentByClient.senderId))

    repo.switchUser("pro_1")
    val provider = repo.getCurrentUser()
    val sentByProvider = repo.addChatMessage(
      serviceId = service.id,
      content = "Combinado, estarei lá às 9h!",
      isSystem = false
    )
    assertTrue("SenderId da mensagem do prestador deve pertencer ao prestador", provider.ownsId(sentByProvider.senderId))

    val serviceChat = repo.chatMessages.value.filter { it.serviceId == service.id && !it.isSystemNotice }
    assertEquals(2, serviceChat.size)

    // 5. Status update & review generate STATUS_ATUALIZADO and AVALIACAO_RECEBIDA notifications
    repo.updateServiceStatus(service.id, ServiceStatus.EM_EXECUCAO)
    assertTrue(
      repo.notifications.value.any {
        it.type == com.example.model.NotificationType.STATUS_ATUALIZADO &&
          it.serviceId == service.id
      }
    )
    repo.updateServiceStatus(service.id, ServiceStatus.CONCLUIDO)
    assertTrue(
      repo.services.value.any { it.id == service.id && it.status == ServiceStatus.CONCLUIDO }
    )

    repo.switchUser("cli_1")
    repo.rateService(service.id, 5f, "Excelente atendimento!")
    assertTrue(
      repo.notifications.value.any {
        it.type == com.example.model.NotificationType.AVALIACAO_RECEBIDA &&
          it.serviceId == service.id &&
          it.recipientId == "pro_1"
      }
    )

    // 6. Mark all read clears unread notifications for active user
    repo.markAllNotificationsAsReadForCurrentUser()
    val unreadForClient = repo.notifications.value.count { notif ->
      !notif.isRead && (client.ownsId(notif.recipientId) || (notif.recipientId == "all_providers" && client.role == UserRole.PRESTADOR))
    }
    assertEquals(0, unreadForClient)
  }

  @Test
  fun testHandleFirestoreError_ignoresCoroutineCancellation() {
    val result = com.example.data.handleFirestoreError(
      kotlinx.coroutines.CancellationException("StandaloneCoroutine was cancelled"),
      com.example.data.OperationType.GET,
      "users/test_uid"
    )
    assertEquals("", result)
  }

  @Test
  fun testLocalPersistence_preservesProviderDocumentAttachmentUris() {
    val tempDir = kotlin.io.path.createTempDirectory("resolveai_docs_test").toFile()
    try {
      val store1 = com.example.data.MarketplaceLocalStore(tempDir)
      val repo1 = MarketplaceRepository(localStore = store1)

      val provider = repo1.registerUserWithGoogle(
        uid = "pro_docs_uid_1",
        email = "prestador.docs@gmail.com",
        displayName = "Marcos Pintor",
        phone = "(61) 99123-4567",
        role = UserRole.PRESTADOR,
        category = "Pintura Residencial",
        rgOrCnhNumber = "CNH 9988776655",
        criminalRecordNumber = "CERT-DF-2026-100",
        rgOrCnhFileName = "cnh_marcos.jpg",
        rgOrCnhAttachmentUri = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
        criminalRecordFileName = "certidao_marcos.pdf",
        criminalRecordAttachmentUri = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
        selfieFileName = "selfie_marcos.jpg",
        selfieAttachmentUri = "data:image/jpeg;base64,/9j/4AAQSkZJRg=="
      )

      val store2 = com.example.data.MarketplaceLocalStore(tempDir)
      val repo2 = MarketplaceRepository(localStore = store2)
      val restoredProvider = repo2.users.value.first { it.id == provider.id }

      assertEquals("cnh_marcos.jpg", restoredProvider.documents?.rgOrCnhFileName)
      assertEquals("data:image/jpeg;base64,/9j/4AAQSkZJRg==", restoredProvider.documents?.rgOrCnhAttachmentUri)
      assertEquals("certidao_marcos.pdf", restoredProvider.documents?.criminalRecordFileName)
      assertEquals("data:image/jpeg;base64,/9j/4AAQSkZJRg==", restoredProvider.documents?.criminalRecordAttachmentUri)
      assertEquals("selfie_marcos.jpg", restoredProvider.documents?.selfieFileName)
      assertEquals("data:image/jpeg;base64,/9j/4AAQSkZJRg==", restoredProvider.documents?.selfieAttachmentUri)
    } finally {
      tempDir.deleteRecursively()
    }
  }

  @Test
  fun testGooglePlacesService_extractsCityFromReverseGeocode() {
    val response = com.example.data.ReverseGeocodeResponse(
      status = "OK",
      results = listOf(
        com.example.data.ReverseGeocodeResultDto(
          formattedAddress = "Brasília, DF, Brasil",
          addressComponents = listOf(
            com.example.data.ReverseGeocodeComponentDto(
              longName = "Brasília",
              shortName = "Brasília",
              types = listOf("locality", "political")
            ),
            com.example.data.ReverseGeocodeComponentDto(
              longName = "Distrito Federal",
              shortName = "DF",
              types = listOf("administrative_area_level_1", "political")
            )
          )
        )
      )
    )

    val extracted = com.example.data.GooglePlacesService.extractCityFromReverseGeocode(response)
    assertEquals("Brasília, DF", extracted)
    assertEquals("Campinas, SP", com.example.data.GooglePlacesService.formatCityAndState("Campinas", "São Paulo"))
  }

  @Test
  fun testFirebaseAuthHelper_validatesCredentialsAndFormatsPhoneSyntheticEmail() {
    assertTrue(com.example.data.FirebaseAuthHelper.isValidEmail("usuario@resolveai.com.br"))
    assertFalse(com.example.data.FirebaseAuthHelper.isValidEmail("email-invalido"))

    val synthetic = com.example.data.FirebaseAuthHelper.phoneToSyntheticAuthEmail("(11) 98111-2233", "+55")
    assertEquals("tel_5511981112233@phone.resolveai.app", synthetic)

    kotlinx.coroutines.runBlocking {
      val badEmailRes = com.example.data.FirebaseAuthHelper.signInWithEmailAndPassword(
        email = "invalido",
        password = "123456",
        authProvider = { null }
      )
      assertTrue(badEmailRes is com.example.data.FirebaseCredentialAuthResult.Error)

      val shortPassRes = com.example.data.FirebaseAuthHelper.signInWithPhoneAndPassword(
        phone = "(11) 98111-2233",
        password = "123",
        authProvider = { null }
      )
      assertTrue(shortPassRes is com.example.data.FirebaseCredentialAuthResult.Error)

      val badPhoneRes = com.example.data.FirebaseAuthHelper.registerWithPhoneAndPassword(
        name = "Maria Silva",
        phone = "1234",
        password = "password123",
        authProvider = { null }
      )
      assertTrue(badPhoneRes is com.example.data.FirebaseCredentialAuthResult.Error)

      val resetErr = com.example.data.FirebaseAuthHelper.sendPasswordReset(
        email = "nao-valido",
        authProvider = { null }
      )
      assertTrue(resetErr.isFailure)
    }
  }

  @Test
  fun testViewModelPasswordReset_setsErrorOnInvalidEmailAndResetsToIdle() {
    val repo = MarketplaceRepository()
    val vm = com.example.ui.MarketplaceViewModel(repo)
    assertTrue(vm.passwordResetState.value is com.example.ui.PasswordResetUiState.Idle)

    vm.sendPasswordReset("email-invalido")
    // Allow viewModelScope coroutine (Dispatchers.Main.immediate in JVM if set or wait briefly)
    kotlinx.coroutines.runBlocking {
      val res = com.example.data.FirebaseAuthHelper.sendPasswordReset("email-invalido", authProvider = { null })
      assertTrue(res.isFailure)
      assertTrue(
        res.exceptionOrNull()?.message?.contains("válido") == true
      )
    }
    vm.resetPasswordResetState()
    assertTrue(vm.passwordResetState.value is com.example.ui.PasswordResetUiState.Idle)
  }

  @Test
  fun testWalletRecharge_pixQrCodeCrc16AndIdempotentDetection() {
    val repo = MarketplaceRepository()
    val vm = com.example.ui.MarketplaceViewModel(repo)
    val initialBalance = repo.users.value.first { it.id == "pro_2" }.walletBalance

    // 0. Canonical CRC16-CCITT-FALSE check vector ("123456789" -> "29B1") & Static BR Code CRC
    assertEquals("29B1", com.example.data.WalletRechargeHelper.computeCrc16Ccitt("123456789"))
    val bacenVector = "00020126330014br.gov.bcb.pix0111123456789015204000053039865802BR5913Fulano de Tal6008BRASILIA62070503***6304"
    val vectorCrc = com.example.data.WalletRechargeHelper.computeCrc16Ccitt(bacenVector)
    assertEquals("9F79", vectorCrc)
    assertTrue(com.example.data.WalletRechargeHelper.isValidPixBrCode(bacenVector + vectorCrc))

    // 1. EMVCo Pix BR Code with Chave Pix + lowercase BACEN GUI + CRC16-CCITT validation
    val customKey = "financeiro@resolveai.com.br"
    val txId = "PIXPRO2TEST12345"
    val pixCode = com.example.data.WalletRechargeHelper.buildPixCopyPasteCode(
      amount = 100.0,
      providerId = "pro_2",
      pixKey = customKey,
      txId = txId
    )
    assertTrue(pixCode.contains("0014br.gov.bcb.pix"))
    assertFalse(pixCode.contains("BR.GOV.BCB.PIX"))
    assertTrue(pixCode.contains("01${customKey.length.toString().padStart(2, '0')}$customKey"))
    assertTrue(pixCode.contains("5406100.00"))
    assertTrue(pixCode.contains("6304"))
    assertTrue(com.example.data.WalletRechargeHelper.isValidPixBrCode(pixCode))
    val expectedCrc = com.example.data.WalletRechargeHelper.computeCrc16Ccitt(pixCode.dropLast(4))
    assertEquals(expectedCrc, pixCode.takeLast(4))

    // 2. Real ZXing QR Code matrix generation + integer pixel render round-trip decode
    val qrMatrix = com.example.data.WalletRechargeHelper.buildQrMatrix(pixCode)
    assertTrue(qrMatrix.size >= 29)
    assertEquals(qrMatrix.size, qrMatrix[0].size)
    assertTrue(qrMatrix.any { row -> row.any { it } })

    val rows = qrMatrix.size
    val cols = qrMatrix[0].size
    val scale = 8
    val imgW = cols * scale
    val imgH = rows * scale
    val pixels = IntArray(imgW * imgH) { -1 } // white
    for (y in 0 until rows) {
      for (x in 0 until cols) {
        if (qrMatrix[y][x]) {
          for (dy in 0 until scale) {
            val rowOff = (y * scale + dy) * imgW + (x * scale)
            for (dx in 0 until scale) {
              pixels[rowOff + dx] = -16777216 // black
            }
          }
        }
      }
    }
    val binaryBitmap = com.google.zxing.BinaryBitmap(
      com.google.zxing.common.HybridBinarizer(
        com.google.zxing.RGBLuminanceSource(imgW, imgH, pixels)
      )
    )
    val decodedPixCode = com.google.zxing.qrcode.QRCodeReader().decode(binaryBitmap).text
    assertEquals(pixCode, decodedPixCode)

    // 3. Key normalization for CPF, CNPJ, Phone (+55 E.164), and Email
    assertEquals(
      "12345678901",
      com.example.data.WalletRechargeHelper.normalizePixKey("123.456.789-01", com.example.data.PixKeyType.CPF)
    )
    assertEquals(
      "12345678000195",
      com.example.data.WalletRechargeHelper.normalizePixKey("12.345.678/0001-95", com.example.data.PixKeyType.CNPJ)
    )
    assertEquals(
      "+5561992907464",
      com.example.data.WalletRechargeHelper.normalizePixKey("(61) 99290-7464", com.example.data.PixKeyType.TELEFONE)
    )
    assertEquals(
      "joao@banco.com.br",
      com.example.data.WalletRechargeHelper.normalizePixKey("  Joao@Banco.COM.BR ", com.example.data.PixKeyType.EMAIL)
    )

    // 4. Idempotent Pix recharge (automatic detection never double-credits the same txId)
    val afterAutoPix = repo.rechargeWallet(
      providerId = "pro_2",
      amount = 100.0,
      txId = txId,
      detectedAutomatically = true
    )
    assertEquals(initialBalance + 100.0, afterAutoPix, 0.01)
    val duplicateAttemptBalance = repo.rechargeWallet(
      providerId = "pro_2",
      amount = 100.0,
      txId = txId,
      detectedAutomatically = true
    )
    assertEquals(afterAutoPix, duplicateAttemptBalance, 0.01)

    val pixTx = repo.walletTransactions.value.first { it.providerId == "pro_2" }
    assertEquals(com.example.model.TransactionType.RECARGA_PIX, pixTx.type)
    assertTrue(pixTx.description.contains("detectada automaticamente"))

    // 5. ViewModel Pix QR state transition + formatted phone normalization
    repo.switchUser("pro_2")
    vm.startPixRecharge(
      amount = 50.0,
      pixKey = "(61) 99290-7464",
      pixKeyType = com.example.data.PixKeyType.TELEFONE
    )
    val awaiting = vm.pixRechargeState.value as com.example.ui.PixRechargeUiState.AwaitingPayment
    assertEquals("+5561992907464", awaiting.charge.pixKey)
    assertEquals(50.0, awaiting.charge.amount, 0.01)
    assertTrue(awaiting.charge.brCode.contains("0014br.gov.bcb.pix"))
    assertTrue(awaiting.charge.brCode.contains("0114+5561992907464"))
    assertTrue(awaiting.charge.brCode.contains("540550.00"))
    assertTrue(com.example.data.WalletRechargeHelper.isValidPixBrCode(awaiting.charge.brCode))

    vm.confirmPixPaymentManually()
    val detected = vm.pixRechargeState.value as com.example.ui.PixRechargeUiState.Detected
    assertEquals(50.0, detected.charge.amount, 0.01)
    assertEquals(afterAutoPix + 50.0, detected.newBalance, 0.01)
  }

  @Test
  fun testAdminUserManagement_editDeleteAndSafetyGuards() {
    val repo = MarketplaceRepository()

    // Non-admin cannot edit or delete users
    repo.switchUser("cli_1")
    val clientEditAttempt = repo.adminUpdateUser(
      repo.users.value.first { it.id == "cli_2" }.copy(name = "Alteração Indevida")
    )
    assertTrue(clientEditAttempt.isFailure)
    assertTrue(repo.adminDeleteUser("cli_2").isFailure)

    // Switch to Admin
    val admin = repo.users.value.first { it.role == UserRole.ADMIN }
    repo.switchUser(admin.id)

    // Admin can create user without losing Admin session
    val createdRes = repo.adminCreateUser(
      name = "Paula Nova",
      email = "paula@resolveai.com",
      phone = "(11) 97777-8888",
      role = UserRole.CLIENTE,
      city = "Curitiba, PR"
    )
    assertTrue(createdRes.isSuccess)
    val createdUser = createdRes.getOrThrow()
    assertEquals(admin.id, repo.getCurrentUser().id)

    // Admin can edit another user (name, city, role, walletBalance, verificationStatus)
    val editRes = repo.adminUpdateUser(
      createdUser.copy(
        name = "Paula Editada",
        role = UserRole.PRESTADOR,
        category = "Pintura & Acabamento",
        city = "Florianópolis, SC",
        walletBalance = 150.0,
        verificationStatus = ProviderVerificationStatus.APROVADO
      )
    )
    assertTrue(editRes.isSuccess)
    val reloaded = repo.users.value.first { it.id == createdUser.id }
    assertEquals("Paula Editada", reloaded.name)
    assertEquals(UserRole.PRESTADOR, reloaded.role)
    assertEquals(150.0, reloaded.walletBalance, 0.01)
    assertEquals(ProviderVerificationStatus.APROVADO, reloaded.verificationStatus)

    // Admin cannot delete or demote self while connected
    assertTrue(repo.adminDeleteUser(admin.id).isFailure)
    assertTrue(repo.adminUpdateUser(admin.copy(role = UserRole.CLIENTE)).isFailure)

    // Admin can delete another user
    val deleteRes = repo.adminDeleteUser(createdUser.id)
    assertTrue(deleteRes.isSuccess)
    assertTrue(repo.users.value.none { it.id == createdUser.id })
  }

  @Test
  fun testGoogleSignup_alwaysRequiresDocumentsAndValidatesBeforeCompletion() {
    val repo = MarketplaceRepository()
    val vm = com.example.ui.MarketplaceViewModel(repo)

    val newGoogleClient = com.example.data.GoogleAccountInfo(
      uid = "google_new_cli_1",
      email = "novo.cliente@gmail.com",
      displayName = "Lucas Google"
    )

    // 1. Even when coming from Register tab as Cliente with null draft, must stop at document step
    vm.handleGoogleAuthSuccess(
      account = newGoogleClient,
      preferredRoleFromRegisterTab = UserRole.CLIENTE,
      registrationDraft = null
    )
    assertFalse(vm.isAuthenticated.value)
    assertEquals(com.example.ui.AuthFlowStep.GOOGLE_ROLE_COMPLETION, vm.authStep.value)
    assertEquals(UserRole.CLIENTE, vm.pendingPreferredRole.value)

    // Incomplete Cliente submission (blank CPF / invalid phone) is blocked
    vm.completeGoogleRegistration(
      role = UserRole.CLIENTE,
      phone = "11999",
      city = "São Paulo, SP",
      document = "",
      category = "",
      bio = "",
      rgOrCnh = "",
      criminalRecord = ""
    )
    assertFalse(vm.isAuthenticated.value)
    assertEquals(com.example.ui.AuthFlowStep.GOOGLE_ROLE_COMPLETION, vm.authStep.value)
    assertTrue(!vm.authErrorMessage.value.isNullOrBlank())

    // 2. Coming from Register tab as Prestador with partial draft (missing docs) must also stop at document step
    val newGoogleProvider = com.example.data.GoogleAccountInfo(
      uid = "google_new_pro_1",
      email = "novo.prestador@gmail.com",
      displayName = "Marcos Google Pro"
    )
    val incompleteProviderDraft = com.example.ui.PendingRegistration(
      name = "Marcos Google Pro",
      email = "novo.prestador@gmail.com",
      phone = "(11) 98888-7777",
      role = UserRole.PRESTADOR,
      category = "Eletricista Residencial & Predial",
      city = "Campinas, SP",
      document = "529.982.247-25"
      // Missing rgOrCnhNumber, criminalRecordNumber, and all 3 attachment URIs
    )
    vm.handleGoogleAuthSuccess(
      account = newGoogleProvider,
      preferredRoleFromRegisterTab = UserRole.PRESTADOR,
      registrationDraft = incompleteProviderDraft
    )
    assertFalse(vm.isAuthenticated.value)
    assertEquals(com.example.ui.AuthFlowStep.GOOGLE_ROLE_COMPLETION, vm.authStep.value)
    assertEquals(UserRole.PRESTADOR, vm.pendingPreferredRole.value)
    assertEquals("Campinas, SP", vm.pendingGoogleDraft.value?.city)

    // Prestador completion with invalid CPF must fail validation
    vm.completeGoogleRegistration(
      role = UserRole.PRESTADOR,
      phone = "(11) 98888-7777",
      city = "Campinas, SP",
      document = "12345678901",
      category = "Eletricista Residencial & Predial",
      bio = "10 anos de experiência",
      rgOrCnh = "CNH 12345678900",
      criminalRecord = "CERT-2026-001",
      rgOrCnhFileName = "cnh.pdf",
      rgOrCnhAttachmentUri = "data:image/jpeg;base64,abc",
      criminalRecordFileName = "certidao.pdf",
      criminalRecordAttachmentUri = "data:image/jpeg;base64,def",
      selfieFileName = "selfie.jpg",
      selfieAttachmentUri = "data:image/jpeg;base64,ghi"
    )
    assertFalse(vm.isAuthenticated.value)
    assertTrue(vm.authErrorMessage.value?.contains("CPF inválido") == true)

    // Prestador completion without attachments must fail validation even with valid CPF
    vm.completeGoogleRegistration(
      role = UserRole.PRESTADOR,
      phone = "(11) 98888-7777",
      city = "Campinas, SP",
      document = "52998224725",
      category = "Eletricista Residencial & Predial",
      bio = "10 anos de experiência",
      rgOrCnh = "CNH 12345678900",
      criminalRecord = "CERT-2026-001",
      rgOrCnhFileName = "",
      rgOrCnhAttachmentUri = null,
      criminalRecordFileName = "",
      criminalRecordAttachmentUri = null,
      selfieFileName = "",
      selfieAttachmentUri = null
    )
    assertFalse(vm.isAuthenticated.value)
    assertTrue(!vm.authErrorMessage.value.isNullOrBlank())

    // Prestador completion with valid CPF, camera profile photo, and all 3 required attachments (without typing doc numbers) succeeds
    vm.completeGoogleRegistration(
      role = UserRole.PRESTADOR,
      phone = "(11) 98888-7777",
      city = "Campinas, SP",
      document = "52998224725",
      category = "Eletricista Residencial & Predial",
      bio = "10 anos de experiência",
      rgOrCnh = "",
      criminalRecord = "",
      rgOrCnhFileName = "cnh.pdf",
      rgOrCnhAttachmentUri = "data:image/jpeg;base64,abc",
      criminalRecordFileName = "certidao.pdf",
      criminalRecordAttachmentUri = "data:image/jpeg;base64,def",
      selfieFileName = "selfie.jpg",
      selfieAttachmentUri = "data:image/jpeg;base64,ghi",
      profilePhotoUri = "data:image/jpeg;base64,profile_cam"
    )
    assertTrue(vm.isAuthenticated.value)
    assertEquals(com.example.ui.AuthFlowStep.FORM, vm.authStep.value)
    val saved = repo.getCurrentUser()
    assertEquals(UserRole.PRESTADOR, saved.role)
    assertEquals("529.982.247-25", saved.document)
    assertEquals("data:image/jpeg;base64,profile_cam", saved.photoUrl)
    assertEquals("cnh.pdf", saved.documents?.rgOrCnhNumber)
    assertEquals("data:image/jpeg;base64,abc", saved.documents?.rgOrCnhAttachmentUri)
    assertEquals("data:image/jpeg;base64,def", saved.documents?.criminalRecordAttachmentUri)
    assertEquals("data:image/jpeg;base64,ghi", saved.documents?.selfieAttachmentUri)
  }

  @Test
  fun testProviderWalletHistoryHelper_summarizesPixAndContractUsageAccurately() {
    val transactions = listOf(
      com.example.model.WalletTransaction(
        id = "tx_pix_PIXPRO1AAA111",
        providerId = "pro_1",
        type = com.example.model.TransactionType.RECARGA_PIX,
        amount = 150.0,
        description = "Recarga Pix detectada automaticamente (PIXPRO1AAA111)",
        timestamp = 3000L,
        balanceAfter = 250.0
      ),
      com.example.model.WalletTransaction(
        id = "tx_contract_1",
        providerId = "pro_1",
        type = com.example.model.TransactionType.DEBITO_TAXA_10,
        amount = 30.0,
        serviceId = "srv_10",
        serviceTitle = "Instalação de Ar Condicionado Split",
        description = "Taxa de intermediação de 10% referente ao serviço contratado #srv_10",
        timestamp = 2000L,
        balanceAfter = 100.0
      ),
      com.example.model.WalletTransaction(
        id = "tx_pix_manual",
        providerId = "pro_1",
        type = com.example.model.TransactionType.RECARGA_PIX,
        amount = 50.0,
        description = "Recarga de saldo via Pix instantâneo",
        timestamp = 1000L,
        balanceAfter = 130.0
      )
    )

    val servicesById = mapOf(
      "srv_10" to com.example.model.ServiceRequest(
        id = "srv_10",
        clientId = "cli_1",
        clientName = "Carlos Eduardo",
        title = "Instalação de Ar Condicionado Split",
        category = "Climatização",
        description = "Split 12000 BTUs",
        location = "Pinheiros, SP",
        proposedBudget = 300.0,
        finalAgreedPrice = 300.0,
        platformFee = 30.0,
        status = com.example.model.ServiceStatus.ACEITO
      )
    )

    val summary = com.example.data.WalletHistoryHelper.summarize(transactions, servicesById)
    assertEquals(200.0, summary.totalPixRecharged, 0.01)
    assertEquals(2, summary.pixRechargesCount)
    assertEquals(1, summary.autoDetectedPixCount)
    assertEquals(30.0, summary.totalContractFeesUsed, 0.01)
    assertEquals(1, summary.contractUsageCount)
    assertEquals(300.0, summary.totalEstimatedContractedValue, 0.01)
    assertEquals(3, summary.totalMovementsCount)

    // Balance before and TX ID extraction
    assertEquals("PIXPRO1AAA111", com.example.data.WalletHistoryHelper.extractPixTxId(transactions[0]))
    assertEquals(100.0, com.example.data.WalletHistoryHelper.computeBalanceBefore(transactions[0]), 0.01)
    assertEquals(130.0, com.example.data.WalletHistoryHelper.computeBalanceBefore(transactions[1]), 0.01)

    // Filtering
    val onlyPix = com.example.data.WalletHistoryHelper.filterTransactions(
      transactions,
      com.example.data.WalletHistoryFilter.RECARGAS_PIX
    )
    assertEquals(2, onlyPix.size)
    assertEquals("tx_pix_PIXPRO1AAA111", onlyPix.first().id)

    val onlyContracts = com.example.data.WalletHistoryHelper.filterTransactions(
      transactions,
      com.example.data.WalletHistoryFilter.USO_CONTRATACOES
    )
    assertEquals(1, onlyContracts.size)
    assertEquals("srv_10", onlyContracts.first().serviceId)

    val searchHit = com.example.data.WalletHistoryHelper.filterTransactions(
      transactions,
      com.example.data.WalletHistoryFilter.TODAS,
      query = "PIXPRO1AAA111"
    )
    assertEquals(1, searchHit.size)
    assertEquals("tx_pix_PIXPRO1AAA111", searchHit.first().id)
  }

  @Test
  fun testCpfValidator_andProviderRegistrationEnforcement() {
    // Mathematically valid CPFs
    assertTrue(com.example.data.CpfValidator.isValid("52998224725"))
    assertTrue(com.example.data.CpfValidator.isValid("529.982.247-25"))
    assertTrue(com.example.data.CpfValidator.isValid("12345678909"))
    assertEquals("529.982.247-25", com.example.data.CpfValidator.formatCpf("52998224725"))

    // Invalid CPFs: wrong check digit, repeated digits, incomplete, CNPJ length
    assertFalse(com.example.data.CpfValidator.isValid("12345678901"))
    assertFalse(com.example.data.CpfValidator.isValid("11111111111"))
    assertFalse(com.example.data.CpfValidator.isValid("00000000000"))
    assertFalse(com.example.data.CpfValidator.isValid("5299822472"))
    assertFalse(com.example.data.CpfValidator.isValid("12345678000195"))

    // Provider missing requirements list checks CPF + camera profile photo + 2 document attachments (RG/CNH + Antecedentes; selfie removed)
    val missingWithBadCpfAndNoAttach = com.example.data.CpfValidator.providerMissingRequirements(
      name = "João Prestador",
      requireName = true,
      email = "joao@teste.com",
      phone = "(11) 98765-4321",
      requireEmailOrPhone = true,
      password = "password123",
      requirePassword = true,
      city = "São Paulo, SP",
      document = "12345678901",
      profilePhotoUri = null,
      requireCameraProfilePhoto = true,
      rgOrCnhAttachmentUri = null,
      criminalRecordAttachmentUri = null
    )
    assertEquals(4, missingWithBadCpfAndNoAttach.size)
    assertFalse(missingWithBadCpfAndNoAttach.any { it.contains("Selfie", ignoreCase = true) })

    val missingAllReady = com.example.data.CpfValidator.providerMissingRequirements(
      name = "João Prestador",
      requireName = true,
      email = "joao@teste.com",
      phone = "(11) 98765-4321",
      requireEmailOrPhone = true,
      password = "password123",
      requirePassword = true,
      city = "São Paulo, SP",
      document = "52998224725",
      profilePhotoUri = "data:image/jpeg;base64,profile_cam",
      requireCameraProfilePhoto = true,
      rgOrCnhAttachmentUri = "data:image/jpeg;base64,rg",
      criminalRecordAttachmentUri = "data:image/jpeg;base64,cert",
      selfieAttachmentUri = null
    )
    assertTrue(missingAllReady.isEmpty())

    // ViewModel registerWithPassword blocks invalid CPF before calling Firebase
    val repo = MarketplaceRepository()
    val vm = com.example.ui.MarketplaceViewModel(repo)
    vm.logout()
    vm.registerWithPassword(
      registration = com.example.ui.PendingRegistration(
        name = "Paulo Eletricista",
        email = "paulo@teste.com",
        phone = "(11) 98765-4321",
        role = UserRole.PRESTADOR,
        category = "Eletricista",
        city = "São Paulo, SP",
        document = "111.111.111-11",
        profilePhotoUri = "data:image/jpeg;base64,profile_cam",
        rgOrCnhAttachmentUri = "data:image/jpeg;base64,a",
        criminalRecordAttachmentUri = "data:image/jpeg;base64,b",
        selfieAttachmentUri = "data:image/jpeg;base64,c"
      ),
      password = "password123"
    )
    assertFalse(vm.isAuthenticated.value)
    assertTrue(vm.authErrorMessage.value?.contains("CPF inválido") == true)

    // Admin registerUserDirectly blocks provider with invalid CPF or missing attachments
    val admin = repo.users.value.first { it.role == UserRole.ADMIN }
    repo.switchUser(admin.id)
    val initialCount = repo.users.value.size
    vm.registerUserDirectly(
      com.example.ui.PendingRegistration(
        name = "Novo Pro Sem Anexo",
        email = "novopro@teste.com",
        phone = "(11) 99999-1111",
        role = UserRole.PRESTADOR,
        category = "Pintor",
        city = "Curitiba, PR",
        document = "52998224725",
        profilePhotoUri = "data:image/jpeg;base64,profile_cam",
        rgOrCnhAttachmentUri = null,
        criminalRecordAttachmentUri = "data:image/jpeg;base64,b",
        selfieAttachmentUri = "data:image/jpeg;base64,c"
      )
    )
    assertEquals(initialCount, repo.users.value.size)

    // Valid CPF + camera profile photo + 2 required attachments (without selfie) succeeds cleanly with formatted CPF
    vm.registerUserDirectly(
      com.example.ui.PendingRegistration(
        name = "Novo Pro Completo",
        email = "novopro@teste.com",
        phone = "(11) 99999-1111",
        role = UserRole.PRESTADOR,
        category = "Pintor",
        city = "Curitiba, PR",
        document = "52998224725",
        profilePhotoUri = "data:image/jpeg;base64,profile_cam",
        rgOrCnhFileName = "cnh.jpg",
        rgOrCnhAttachmentUri = "data:image/jpeg;base64,a",
        criminalRecordFileName = "cert.pdf",
        criminalRecordAttachmentUri = "data:image/jpeg;base64,b",
        selfieFileName = "",
        selfieAttachmentUri = null
      )
    )
    assertEquals(initialCount + 1, repo.users.value.size)
    val createdProvider = repo.users.value.first { it.email == "novopro@teste.com" }
    assertEquals("529.982.247-25", createdProvider.document)
    assertEquals("data:image/jpeg;base64,profile_cam", createdProvider.photoUrl)
  }

  @Test
  fun testMercadoPagoPixService_createsChargeAndRequiresRealApproval() = kotlinx.coroutines.runBlocking {
    // 1. Token detection & sandbox classification
    assertFalse(com.example.data.MercadoPagoPixService.isConfigured("MY_MERCADO_PAGO_ACCESS_TOKEN"))
    assertFalse(com.example.data.MercadoPagoPixService.isConfigured(""))
    assertTrue(com.example.data.MercadoPagoPixService.isConfigured("APP_USR-1234567890"))
    assertTrue(com.example.data.MercadoPagoPixService.isSandboxToken("TEST-1234567890"))
    assertFalse(com.example.data.MercadoPagoPixService.isSandboxToken("APP_USR-1234567890"))

    val repo = MarketplaceRepository()
    repo.switchUser("pro_2")
    val currentUser = repo.getCurrentUser().copy(document = "529.982.247-25")
    val initialBalance = currentUser.walletBalance

    // 2. Payer payload includes valid CPF automatically
    val payer = com.example.data.MercadoPagoPixService.buildPayer(currentUser)
    assertEquals(currentUser.email.lowercase(), payer.email)
    assertEquals("CPF", payer.identification?.type)
    assertEquals("52998224725", payer.identification?.number)

    var statusToReturn = "pending"
    var capturedAuthHeader = ""
    var capturedIdempotencyKey = ""
    val sampleQrCode = com.example.data.WalletRechargeHelper.buildPixCopyPasteCode(
      amount = 75.0,
      providerId = currentUser.id,
      pixKey = "pagamentos@mercadopago.com.br",
      txId = "MPDYNAMIC999"
    )
    val fakeApi = object : com.example.data.MercadoPagoApi {
      override suspend fun createPayment(
        authorization: String,
        idempotencyKey: String,
        request: com.example.data.MercadoPagoPixPaymentRequest
      ): retrofit2.Response<com.example.data.MercadoPagoPaymentResponse> {
        capturedAuthHeader = authorization
        capturedIdempotencyKey = idempotencyKey
        return retrofit2.Response.success(
          com.example.data.MercadoPagoPaymentResponse(
            id = 987654321L,
            status = statusToReturn,
            statusDetail = "pending_waiting_transfer",
            transactionAmount = request.transactionAmount,
            externalReference = request.externalReference,
            pointOfInteraction = com.example.data.MercadoPagoPointOfInteraction(
              transactionData = com.example.data.MercadoPagoTransactionData(
                qrCode = sampleQrCode,
                ticketUrl = "https://www.mercadopago.com.br/payments/987654321/ticket"
              )
            )
          )
        )
      }

      override suspend fun getPayment(
        authorization: String,
        paymentId: Long
      ): retrofit2.Response<com.example.data.MercadoPagoPaymentResponse> {
        return retrofit2.Response.success(
          com.example.data.MercadoPagoPaymentResponse(
            id = paymentId,
            status = statusToReturn,
            statusDetail = if (statusToReturn == "approved") "accredited" else "pending_waiting_transfer",
            transactionAmount = 75.0
          )
        )
      }

      override suspend fun cancelPayment(
        authorization: String,
        paymentId: Long,
        request: com.example.data.MercadoPagoCancelPaymentRequest
      ): retrofit2.Response<com.example.data.MercadoPagoPaymentResponse> {
        statusToReturn = request.status
        return retrofit2.Response.success(
          com.example.data.MercadoPagoPaymentResponse(
            id = paymentId,
            status = statusToReturn,
            statusDetail = "by_collector",
            transactionAmount = 75.0
          )
        )
      }
    }

    // 3. Direct service call validates headers, parsed response, and >= 30min expiration window
    val beforeCreate = System.currentTimeMillis()
    val created = com.example.data.MercadoPagoPixService.createPixPayment(
      amount = 75.0,
      user = currentUser,
      txId = "PIXMPCHECK1",
      accessToken = "APP_USR-prod-token-999",
      api = fakeApi,
      expirationDurationMs = 5 * 60_000L // clamped up to >= 30 minutes
    ).getOrThrow()

    assertEquals("Bearer APP_USR-prod-token-999", capturedAuthHeader)
    assertEquals("PIXMPCHECK1", capturedIdempotencyKey)
    assertEquals(987654321L, created.paymentId)
    assertEquals(sampleQrCode, created.brCode)
    assertTrue(
      "Mercado Pago exige expiracao minima de 30 minutos",
      created.expiresAtMillis - beforeCreate >= com.example.data.MercadoPagoPixService.MERCADO_PAGO_MIN_EXPIRATION_MS - 1000L
    )
    assertTrue(
      com.example.data.MercadoPagoPixService.buildExpirationIso(created.expiresAtMillis).endsWith("-03:00")
    )

    // 3b. Numeric cause codes in HTTP errors must parse cleanly
    val errMsg = com.example.data.MercadoPagoPixService.parseHttpError(
      400,
      """{"message":"Invalid parameter","status":400,"cause":[{"code":4002,"description":"date_of_expiration invalid"}]}"""
    )
    assertEquals("Mercado Pago (400): date_of_expiration invalid", errMsg)

    // 4. ViewModel flow blocks unapproved credit while status == "pending", credits only after "approved"
    @OptIn(ExperimentalCoroutinesApi::class)
    val testDispatcher = UnconfinedTestDispatcher()
    @OptIn(ExperimentalCoroutinesApi::class)
    Dispatchers.setMain(testDispatcher)
    val vm = com.example.ui.MarketplaceViewModel(repo)
    try {
      vm.startMercadoPagoPixRecharge(
        amount = 75.0,
        apiOverride = fakeApi,
        accessTokenOverride = "APP_USR-prod-token-999"
      )

      val awaiting = withTimeout(3000L) {
        vm.pixRechargeState.first { it is com.example.ui.PixRechargeUiState.AwaitingPayment }
      } as com.example.ui.PixRechargeUiState.AwaitingPayment
      assertEquals("MERCADO_PAGO", awaiting.charge.paymentGateway)
      assertEquals(987654321L, awaiting.charge.mercadoPagoPaymentId)
      assertEquals("https://www.mercadopago.com.br/payments/987654321/ticket", awaiting.charge.ticketUrl)

      // While pending, checking MUST NOT credit balance
      vm.checkPixPaymentStatusNow(apiOverride = fakeApi, accessTokenOverride = "APP_USR-prod-token-999")
      val stillAwaiting = withTimeout(3000L) {
        vm.pixRechargeState.first {
          it is com.example.ui.PixRechargeUiState.AwaitingPayment && !it.isCheckingNow
        }
      }
      assertTrue(stillAwaiting is com.example.ui.PixRechargeUiState.AwaitingPayment)
      assertEquals(initialBalance, repo.getCurrentUser().walletBalance, 0.01)

      // Once Mercado Pago approves the real Pix transfer, balance is automatically credited once
      statusToReturn = "approved"
      vm.checkPixPaymentStatusNow(apiOverride = fakeApi, accessTokenOverride = "APP_USR-prod-token-999")
      val detected = withTimeout(3000L) {
        vm.pixRechargeState.first { it is com.example.ui.PixRechargeUiState.Detected }
      } as com.example.ui.PixRechargeUiState.Detected
      assertTrue(detected.automatic)
      assertEquals(initialBalance + 75.0, detected.newBalance, 0.01)
      assertEquals(initialBalance + 75.0, repo.getCurrentUser().walletBalance, 0.01)

      // Cancel endpoint works via Retrofit PUT v1/payments/{id}
      val cancelRes = com.example.data.MercadoPagoPixService.cancelPixPayment(
        paymentId = 987654321L,
        accessToken = "APP_USR-prod-token-999",
        api = fakeApi
      ).getOrThrow()
      assertTrue(cancelRes.isRejectedOrCancelled)
      assertEquals("cancelled", cancelRes.status)
    } finally {
      vm.resetPixRecharge()
      @OptIn(ExperimentalCoroutinesApi::class)
      Dispatchers.resetMain()
    }
  }

  @Test
  fun testMoshiReflectionSerializationAndDeserialization_withoutKspCodegen() {
    val moshi = com.squareup.moshi.Moshi.Builder()
      .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
      .build()

    // 1. Outgoing MercadoPagoPixPaymentRequest serialization must emit snake_case JSON keys
    val req = com.example.data.MercadoPagoPixPaymentRequest(
      transactionAmount = 49.9,
      description = "Recarga Pix",
      paymentMethodId = "pix",
      externalReference = "PIXREF1",
      dateOfExpiration = "2026-11-15T18:00:00.000-03:00",
      payer = com.example.data.MercadoPagoPayer(
        email = "prestador@teste.com",
        firstName = "Joao",
        lastName = "Silva",
        identification = com.example.data.MercadoPagoIdentification(
          type = "CPF",
          number = "52998224725"
        )
      )
    )
    val reqJson = moshi.adapter(com.example.data.MercadoPagoPixPaymentRequest::class.java).toJson(req)
    assertTrue("Expected snake_case transaction_amount in $reqJson", reqJson.contains("\"transaction_amount\":49.9"))
    assertTrue("Expected snake_case payment_method_id in $reqJson", reqJson.contains("\"payment_method_id\":\"pix\""))
    assertTrue("Expected snake_case external_reference in $reqJson", reqJson.contains("\"external_reference\":\"PIXREF1\""))
    assertTrue("Expected snake_case first_name in $reqJson", reqJson.contains("\"first_name\":\"Joao\""))

    // 2. Incoming MercadoPagoPaymentResponse deserialization must parse nested snake_case fields
    val rawMpJson = """
      {
        "id": 123456789,
        "status": "pending",
        "status_detail": "pending_waiting_transfer",
        "transaction_amount": 49.9,
        "external_reference": "PIXREF1",
        "point_of_interaction": {
          "transaction_data": {
            "qr_code": "00020101021226...",
            "qr_code_base64": "iVBORw0KGgo=",
            "ticket_url": "https://www.mercadopago.com.br/payments/123456789/ticket"
          }
        }
      }
    """.trimIndent()
    val parsedMp = moshi.adapter(com.example.data.MercadoPagoPaymentResponse::class.java).fromJson(rawMpJson)!!
    assertEquals(123456789L, parsedMp.id)
    assertEquals("pending_waiting_transfer", parsedMp.statusDetail)
    assertEquals("00020101021226...", parsedMp.pointOfInteraction?.transactionData?.qrCode)
    assertEquals("https://www.mercadopago.com.br/payments/123456789/ticket", parsedMp.pointOfInteraction?.transactionData?.ticketUrl)

    // 3. Incoming PlacesAutocompleteResponse deserialization
    val rawPlacesJson = """
      {
        "status": "OK",
        "predictions": [
          {
            "place_id": "ChIJ01234",
            "description": "Curitiba, PR, Brasil",
            "structured_formatting": {
              "main_text": "Curitiba",
              "secondary_text": "PR, Brasil"
            }
          }
        ]
      }
    """.trimIndent()
    val parsedPlaces = moshi.adapter(com.example.data.PlacesAutocompleteResponse::class.java).fromJson(rawPlacesJson)!!
    assertEquals("OK", parsedPlaces.status)
    assertEquals("ChIJ01234", parsedPlaces.predictions.first().placeId)
    assertEquals("Curitiba", parsedPlaces.predictions.first().structuredFormatting?.mainText)
  }

  @Test
  fun testMercadoPagoTokenNormalizationAndErrorDiagnostics() {
    assertEquals(
      "APP_USR-1234-abcd",
      com.example.data.MercadoPagoPixService.normalizeAccessToken("  \"Bearer APP_USR-1234-abcd\"  ")
    )
    assertEquals(
      "TEST-9999",
      com.example.data.MercadoPagoPixService.normalizeAccessToken("'bearer TEST-9999'")
    )

    val err401 = com.example.data.MercadoPagoPixService.parseHttpError(
      401,
      """{"message":"invalid_token","error":"unauthorized","status":401}"""
    )
    assertTrue(err401.contains("401"))
    assertTrue(err401.contains("invalid_token"))

    val err403 = com.example.data.MercadoPagoPixService.parseHttpError(
      403,
      """{"message":"At least one policy returned UNAUTHORIZED.","error":"forbidden","status":403,"cause":[{"code":"PA_UNAUTHORIZED_RESULT_FROM_POLICIES","description":"Unauthorized use of live credentials"}]}"""
    )
    assertTrue(err403.contains("403"))
    assertTrue(err403.contains("Credenciais de Produção"))
  }

  @Test
  fun testMercadoPagoPolling_notifiesUserAndUpdatesHistoryEvenAfterDialogClosed() = kotlinx.coroutines.runBlocking {
    val repo = MarketplaceRepository()
    repo.switchUser("pro_2")
    val provider = repo.getCurrentUser().copy(document = "529.982.247-25")
    val initialBalance = provider.walletBalance
    val initialNotifCount = repo.notifications.value.size

    var currentStatus = "pending"
    val fakeApi = object : com.example.data.MercadoPagoApi {
      override suspend fun createPayment(
        authorization: String,
        idempotencyKey: String,
        request: com.example.data.MercadoPagoPixPaymentRequest
      ): retrofit2.Response<com.example.data.MercadoPagoPaymentResponse> {
        return retrofit2.Response.success(
          com.example.data.MercadoPagoPaymentResponse(
            id = 888777666L,
            status = "pending",
            statusDetail = "pending_waiting_transfer",
            transactionAmount = request.transactionAmount,
            externalReference = request.externalReference,
            pointOfInteraction = com.example.data.MercadoPagoPointOfInteraction(
              transactionData = com.example.data.MercadoPagoTransactionData(
                qrCode = "00020101021226...",
                ticketUrl = "https://www.mercadopago.com.br/payments/888777666/ticket"
              )
            )
          )
        )
      }

      override suspend fun getPayment(
        authorization: String,
        paymentId: Long
      ): retrofit2.Response<com.example.data.MercadoPagoPaymentResponse> {
        return retrofit2.Response.success(
          com.example.data.MercadoPagoPaymentResponse(
            id = paymentId,
            status = currentStatus,
            statusDetail = if (currentStatus == "approved") "accredited" else "pending_waiting_transfer",
            transactionAmount = 120.0
          )
        )
      }

      override suspend fun cancelPayment(
        authorization: String,
        paymentId: Long,
        request: com.example.data.MercadoPagoCancelPaymentRequest
      ): retrofit2.Response<com.example.data.MercadoPagoPaymentResponse> {
        return retrofit2.Response.success(
          com.example.data.MercadoPagoPaymentResponse(id = paymentId, status = "cancelled")
        )
      }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val testDispatcher = UnconfinedTestDispatcher()
    @OptIn(ExperimentalCoroutinesApi::class)
    Dispatchers.setMain(testDispatcher)
    val vm = com.example.ui.MarketplaceViewModel(repo)
    try {
      vm.startMercadoPagoPixRecharge(
        amount = 120.0,
        apiOverride = fakeApi,
        accessTokenOverride = "APP_USR-prod-token-888"
      )
      val awaiting = withTimeout(3000L) {
        vm.pixRechargeState.first { it is com.example.ui.PixRechargeUiState.AwaitingPayment }
      } as com.example.ui.PixRechargeUiState.AwaitingPayment
      assertEquals(1, repo.mercadoPagoCharges.value.size)
      assertEquals(com.example.model.PixChargeStatus.AGUARDANDO_PAGAMENTO, repo.mercadoPagoCharges.value.first().status)

      // Simulate user closing the QR dialog before paying in the bank app
      vm.resetPixRecharge()

      // Periodic ViewModel polling checks API and confirms payment once approved
      currentStatus = "approved"
      val stillPending = vm.pollPendingMercadoPagoPaymentsOnce(
        apiOverride = fakeApi,
        accessTokenOverride = "APP_USR-prod-token-888"
      )
      assertFalse(stillPending)

      // Balance credited + Mercado Pago charge status updated to PAGO + AppNotification emitted
      assertEquals(initialBalance + 120.0, repo.getCurrentUser().walletBalance, 0.01)
      val updatedCharge = repo.mercadoPagoCharges.value.first { it.txId == awaiting.charge.txId }
      assertEquals(com.example.model.PixChargeStatus.PAGO, updatedCharge.status)
      assertTrue(updatedCharge.paidAt != null)
      assertTrue(repo.notifications.value.size > initialNotifCount)
      val pixNotif = repo.notifications.value.firstOrNull {
        it.type == com.example.model.NotificationType.PAGAMENTO_PIX_CONFIRMADO
      }
      assertTrue(pixNotif != null)
      assertTrue(pixNotif!!.message.contains("120,00"))
    } finally {
      vm.resetPixRecharge()
      @OptIn(ExperimentalCoroutinesApi::class)
      Dispatchers.resetMain()
    }
  }

  @Test
  fun testCancelService_byClientWithReason_updatesStatusRejectsProposalsAndRefundsFee() {
    val repo = MarketplaceRepository()
    // Ensure provider pro_1 accepts prop_1 on srv_1 so platformFee = 30.00 is debited
    val acceptRes = repo.acceptProposalAndDebitFee("prop_1")
    assertTrue(acceptRes.isSuccess)
    val balanceAfterDebit = repo.users.value.first { it.id == "pro_1" }.walletBalance
    assertEquals(90.00, balanceAfterDebit, 0.01)

    // Switch back to client cli_1 who owns srv_1
    repo.switchUser("cli_1")

    // Short / blank reason must fail
    val invalidRes = repo.cancelServiceRequest("srv_1", "   ")
    assertTrue(invalidRes.isFailure)

    // Cancel with valid reason
    val cancelRes = repo.cancelServiceRequest("srv_1", "Precisei viajar e remarcar")
    assertTrue(cancelRes.isSuccess)
    val cancelled = cancelRes.getOrThrow()
    assertEquals(ServiceStatus.CANCELADO, cancelled.status)
    assertEquals("Precisei viajar e remarcar", cancelled.cancellationReason)
    assertEquals(UserRole.CLIENTE, cancelled.cancelledByRole)
    assertTrue(cancelled.cancelledAt != null)

    // Proposals for srv_1 must be RECUSADA
    val proposalsForSrv = repo.proposals.value.filter { it.serviceId == "srv_1" }
    assertTrue(proposalsForSrv.isNotEmpty())
    assertTrue(proposalsForSrv.all { it.status == ProposalStatus.RECUSADA })

    // 10% platform fee (R$ 30.00) must be refunded idempotently to pro_1
    val refundedBalance = repo.users.value.first { it.id == "pro_1" }.walletBalance
    assertEquals(120.00, refundedBalance, 0.01)
    val refundTxs = repo.walletTransactions.value.filter {
      it.serviceId == "srv_1" && it.type == com.example.model.TransactionType.REEMBOLSO
    }
    assertEquals(1, refundTxs.size)
    assertEquals(30.00, refundTxs.first().amount, 0.01)

    // Notification emitted to provider with cancellation reason
    val providerNotif = repo.notifications.value.firstOrNull {
      it.serviceId == "srv_1" && it.recipientId == "pro_1" &&
        it.type == com.example.model.NotificationType.STATUS_ATUALIZADO
    }
    assertTrue(providerNotif != null)
    assertTrue(providerNotif!!.message.contains("Precisei viajar e remarcar"))
  }

  @Test
  fun testCancelService_byAssignedProviderWithReason_notifiesClientAndBlocksUnassignedProvider() {
    val repo = MarketplaceRepository()
    val acceptRes = repo.acceptProposalAndDebitFee("prop_1")
    assertTrue(acceptRes.isSuccess)

    // Unassigned provider pro_2 CANNOT cancel pro_1's service
    repo.switchUser("pro_2")
    val unauthorized = repo.cancelServiceRequest("srv_1", "Tentando cancelar serviço alheio")
    assertTrue(unauthorized.isFailure)

    // Assigned provider pro_1 CAN cancel ACEITO service with reason -> refund awaits Admin review
    repo.switchUser("pro_1")
    val cancelRes = repo.cancelServiceRequest("srv_1", "Imprevisto de agenda hoje")
    assertTrue(cancelRes.isSuccess)
    val cancelled = cancelRes.getOrThrow()
    assertEquals(ServiceStatus.CANCELADO, cancelled.status)
    assertEquals(UserRole.PRESTADOR, cancelled.cancelledByRole)
    assertEquals("Imprevisto de agenda hoje", cancelled.cancellationReason)
    assertEquals(
      com.example.model.CancellationRefundStatus.PENDENTE_ANALISE_ADMIN,
      cancelled.cancellationRefundStatus
    )
    // Fee remains held (90.00) until administrator processes the refund
    assertEquals(90.00, repo.users.value.first { it.id == "pro_1" }.walletBalance, 0.01)

    // Client cli_1 notified (without fee value notices)
    val clientNotif = repo.notifications.value.firstOrNull {
      it.serviceId == "srv_1" && it.recipientId == "cli_1" &&
        it.type == com.example.model.NotificationType.STATUS_ATUALIZADO
    }
    assertTrue(clientNotif != null)
    assertTrue(clientNotif!!.message.contains("Imprevisto de agenda hoje"))
    assertFalse(clientNotif.message.contains("taxa", ignoreCase = true))

    // Admin reviews and approves refund -> wallet balance returns to 120.00
    val admin = repo.users.value.first { it.role == UserRole.ADMIN }
    repo.switchUser(admin.id)
    val approvedRefund = repo.adminProcessCancellationRefund("srv_1", approve = true, note = "Imprevisto justificado.")
    assertTrue(approvedRefund.isSuccess)
    assertEquals(
      com.example.model.CancellationRefundStatus.ESTORNADO_ADMIN,
      approvedRefund.getOrThrow().cancellationRefundStatus
    )
    assertEquals(120.00, repo.users.value.first { it.id == "pro_1" }.walletBalance, 0.01)
  }

  @Test
  fun testCreateServiceRequest_savesAttachmentPhotoAndUsesCurrentUserCity() {
    val repo = MarketplaceRepository()
    repo.switchUser("cli_1")
    val updatedClient = repo.getCurrentUser().copy(city = "Curitiba, PR")
    repo.updateUserProfile(updatedClient)

    val created = repo.createServiceRequest(
      title = "Troca de Disjuntor",
      category = "Eletricista",
      description = "Quadro com disjuntor desarmando.",
      location = "Centro",
      proposedBudget = 180.0,
      urgency = "Alta",
      attachmentPhotoUri = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      attachmentPhotoName = "quadro_eletrico.jpg"
    )

    assertEquals("data:image/jpeg;base64,/9j/4AAQSkZJRg==", created.attachmentPhotoUri)
    assertEquals("quadro_eletrico.jpg", created.attachmentPhotoName)
    assertEquals("Curitiba, PR", created.clientLocation?.city)
  }

  @Test
  fun testCompleteServiceWithPhoto_storesCompletionPhotoAndNotesAndNotifiesClient() {
    val repo = MarketplaceRepository()
    val acceptRes = repo.acceptProposalAndDebitFee("prop_1")
    assertTrue(acceptRes.isSuccess)
    repo.switchUser("pro_1")

    repo.updateServiceStatus("srv_1", ServiceStatus.EM_EXECUCAO)
    repo.updateServiceStatus(
      serviceId = "srv_1",
      newStatus = ServiceStatus.CONCLUIDO,
      completionPhotoUri = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      completionPhotoName = "conclusao_servico_final.jpg",
      completionNotes = "Instalação finalizada e testada com multímetro."
    )

    val completed = repo.services.value.first { it.id == "srv_1" }
    assertEquals(ServiceStatus.CONCLUIDO, completed.status)
    assertEquals("data:image/jpeg;base64,/9j/4AAQSkZJRg==", completed.completionPhotoUri)
    assertEquals("conclusao_servico_final.jpg", completed.completionPhotoName)
    assertEquals("Instalação finalizada e testada com multímetro.", completed.completionNotes)
    assertTrue(completed.completedAt != null)

    val clientNotif = repo.notifications.value.firstOrNull {
      it.serviceId == "srv_1" && it.recipientId == "cli_1" &&
        it.type == com.example.model.NotificationType.STATUS_ATUALIZADO
    }
    assertTrue(clientNotif != null)
    assertTrue(clientNotif!!.message.contains("foto anexada"))
  }

  @Test
  fun testCreateServiceRequest_savesAttachmentVideoMetadataAndThumbnail() {
    val repo = MarketplaceRepository()
    repo.switchUser("cli_1")

    val created = repo.createServiceRequest(
      title = "Vazamento sob a pia",
      category = "Hidráulica & Encanador",
      description = "Gravado vídeo mostrando o gotejamento.",
      location = "Centro",
      proposedBudget = 220.0,
      urgency = "Urgente",
      attachmentVideoUri = "file:///data/user/0/com.example/files/provider_docs/video_pia.mp4",
      attachmentVideoName = "vazamento_pia.mp4",
      attachmentVideoThumbnailUri = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      attachmentVideoDurationMs = 14_500L
    )

    assertEquals("file:///data/user/0/com.example/files/provider_docs/video_pia.mp4", created.attachmentVideoUri)
    assertEquals("vazamento_pia.mp4", created.attachmentVideoName)
    assertEquals("data:image/jpeg;base64,/9j/4AAQSkZJRg==", created.attachmentVideoThumbnailUri)
    assertEquals(14_500L, created.attachmentVideoDurationMs)
  }

  @Test
  fun testCompleteServiceWithVideo_storesCompletionVideoAndNotifiesClient() {
    val repo = MarketplaceRepository()
    val acceptRes = repo.acceptProposalAndDebitFee("prop_1")
    assertTrue(acceptRes.isSuccess)
    repo.switchUser("pro_1")

    repo.updateServiceStatus("srv_1", ServiceStatus.EM_EXECUCAO)
    repo.updateServiceStatus(
      serviceId = "srv_1",
      newStatus = ServiceStatus.CONCLUIDO,
      completionPhotoUri = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      completionPhotoName = "foto_final.jpg",
      completionVideoUri = "file:///data/user/0/com.example/files/provider_docs/conclusao_final.mp4",
      completionVideoName = "conclusao_final.mp4",
      completionVideoThumbnailUri = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      completionVideoDurationMs = 19_000L,
      completionNotes = "Serviço entregue funcionando perfeitamente."
    )

    val completed = repo.services.value.first { it.id == "srv_1" }
    assertEquals(ServiceStatus.CONCLUIDO, completed.status)
    assertEquals("file:///data/user/0/com.example/files/provider_docs/conclusao_final.mp4", completed.completionVideoUri)
    assertEquals("conclusao_final.mp4", completed.completionVideoName)
    assertEquals("data:image/jpeg;base64,/9j/4AAQSkZJRg==", completed.completionVideoThumbnailUri)
    assertEquals(19_000L, completed.completionVideoDurationMs)

    val clientNotif = repo.notifications.value.firstOrNull {
      it.serviceId == "srv_1" && it.recipientId == "cli_1" &&
        it.type == com.example.model.NotificationType.STATUS_ATUALIZADO
    }
    assertTrue(clientNotif != null)
    assertTrue(clientNotif!!.message.contains("foto e vídeo anexados"))
  }

  @Test
  fun testDocumentAttachmentHelper_formatsVideoDuration() {
    assertEquals("00:00", com.example.data.DocumentAttachmentHelper.formatVideoDuration(null))
    assertEquals("00:00", com.example.data.DocumentAttachmentHelper.formatVideoDuration(-100L))
    assertEquals("00:12", com.example.data.DocumentAttachmentHelper.formatVideoDuration(12_400L))
    assertEquals("01:05", com.example.data.DocumentAttachmentHelper.formatVideoDuration(65_000L))
  }

  @Test
  fun testProviderShowcaseHelper_combinesCompletedServicePhotosAndPortfolioPhotos() {
    val approvedElec = User(
      id = "pro_a",
      name = "Roberto Mendes",
      email = "r@resolveai.com",
      phone = "11999990001",
      role = UserRole.PRESTADOR,
      category = "Eletricista",
      city = "São Paulo, SP",
      rating = 4.95,
      completedJobs = 42,
      photoUrl = "drawable://img_avatar_roberto",
      verificationStatus = com.example.model.ProviderVerificationStatus.APROVADO,
      portfolioPhotos = listOf(
        com.example.model.ProviderServicePhoto(
          id = "p1",
          title = "Quadro LED",
          category = "Eletricista",
          photoUri = "drawable://img_showcase_eletrica",
          createdAt = 1000L
        )
      )
    )
    val approvedPainter = User(
      id = "pro_b",
      name = "Ana Clara Ferraz",
      email = "a@resolveai.com",
      phone = "11999990002",
      role = UserRole.PRESTADOR,
      category = "Pintura",
      city = "Campinas, SP",
      rating = 4.99,
      completedJobs = 28,
      photoUrl = "drawable://img_avatar_anaclara",
      verificationStatus = com.example.model.ProviderVerificationStatus.APROVADO,
      portfolioPhotos = listOf(
        com.example.model.ProviderServicePhoto(
          id = "p2",
          title = "Pintura Sala",
          category = "Pintura",
          photoUri = "drawable://img_showcase_pintura",
          createdAt = 2000L
        )
      )
    )
    val pendingProvider = User(
      id = "pro_pending_x",
      name = "Pendente Não Exibir",
      email = "p@resolveai.com",
      phone = "11999990003",
      role = UserRole.PRESTADOR,
      category = "Hidráulica",
      rating = 5.0,
      verificationStatus = com.example.model.ProviderVerificationStatus.PENDENTE_ANALISE,
      portfolioPhotos = listOf(
        com.example.model.ProviderServicePhoto(
          id = "px",
          title = "Não deve aparecer",
          category = "Hidráulica",
          photoUri = "drawable://img_showcase_redes",
          createdAt = 3000L
        )
      )
    )
    val completedService = ServiceRequest(
      id = "srv_done_1",
      clientId = "cli_1",
      clientName = "Carlos",
      title = "Troca de Tomadas",
      category = "Eletricista",
      description = "Troca completa",
      location = "São Paulo, SP",
      proposedBudget = 200.0,
      status = ServiceStatus.CONCLUIDO,
      acceptedProviderId = "pro_a",
      acceptedProviderName = "Roberto Mendes",
      completionPhotoUri = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      completedAt = 5000L
    )

    val entries = com.example.data.ProviderShowcaseHelper.buildShowcaseEntries(
      users = listOf(approvedElec, approvedPainter, pendingProvider),
      services = listOf(completedService),
      categoryFilter = "Eletricista"
    )

    assertEquals(2, entries.size)
    // Category filter prioritizes Eletricista first even with slightly lower rating than Pintura
    assertEquals("pro_a", entries[0].providerId)
    assertEquals("Roberto Mendes", entries[0].providerName)
    assertEquals("drawable://img_avatar_roberto", entries[0].providerPhotoUrl)
    assertEquals(4.95, entries[0].providerRating, 0.001)
    assertEquals(2, entries[0].photos.size)
    assertEquals("data:image/jpeg;base64,/9j/4AAQSkZJRg==", entries[0].featuredPhoto.photoUri)
  }

  @Test
  fun testCompleteServiceWithPhoto_addsCompletionPhotoToProviderPortfolio() {
    val repo = MarketplaceRepository()
    assertTrue(repo.acceptProposalAndDebitFee("prop_1").isSuccess)
    repo.switchUser("pro_1")
    repo.updateServiceStatus(
      serviceId = "srv_1",
      newStatus = ServiceStatus.CONCLUIDO,
      completionPhotoUri = "data:image/jpeg;base64,/9j/PORTFOLIO_NEW",
      completionPhotoName = "quadro_novo.jpg",
      completionNotes = "Finalizado."
    )

    val pro1 = repo.users.value.first { it.id == "pro_1" }
    assertTrue(pro1.portfolioPhotos.any { it.photoUri == "data:image/jpeg;base64,/9j/PORTFOLIO_NEW" })
  }

  @Test
  fun testContactValidator_validatesStrictEmailAndBrazilianPhone() {
    assertTrue(com.example.data.ContactValidator.isValidEmail("cliente.silva@gmail.com"))
    assertFalse(com.example.data.ContactValidator.isValidEmail("sem-arroba.com"))
    assertFalse(com.example.data.ContactValidator.isValidEmail("a@b"))
    assertFalse(com.example.data.ContactValidator.isValidEmail("user..dup@gmail.com"))

    assertTrue(com.example.data.ContactValidator.isValidPhone("(11) 98765-4321"))
    assertTrue(com.example.data.ContactValidator.isValidPhone("61992907464"))
    assertFalse(com.example.data.ContactValidator.isValidPhone("11999"))
    assertFalse(com.example.data.ContactValidator.isValidPhone("(00) 99999-9999"))
    assertFalse(com.example.data.ContactValidator.isValidPhone("(11) 99999-9999"))
    assertFalse(com.example.data.ContactValidator.isValidPhone("(11) 88765-4321")) // 11-digit mobile must start with 9
  }

  @Test
  fun testRegistrationValidation_requiresCameraProfilePhotoForProviderAndOptionalForClient() {
    val repo = MarketplaceRepository()
    val vm = com.example.ui.MarketplaceViewModel(repo)
    vm.logout()

    // Provider registration fails when profilePhotoUri is missing
    vm.registerWithPassword(
      registration = com.example.ui.PendingRegistration(
        name = "Marcos Eletricista",
        email = "marcos.pro@resolveai.com",
        phone = "(11) 98765-4321",
        role = UserRole.PRESTADOR,
        category = "Eletricista",
        city = "São Paulo, SP",
        document = "52998224725",
        profilePhotoUri = null,
        rgOrCnhAttachmentUri = "data:image/jpeg;base64,a",
        criminalRecordAttachmentUri = "data:image/jpeg;base64,b",
        selfieAttachmentUri = "data:image/jpeg;base64,c"
      ),
      password = "password123"
    )
    assertFalse(vm.isAuthenticated.value)
    assertTrue(vm.authErrorMessage.value?.contains("foto de perfil", ignoreCase = true) == true)

    // Client registration via Admin with optional gallery/camera photo saves photoUrl cleanly
    val admin = repo.users.value.first { it.role == UserRole.ADMIN }
    repo.switchUser(admin.id)
    vm.registerUserDirectly(
      com.example.ui.PendingRegistration(
        name = "Fernanda Cliente",
        email = "fernanda.cli@resolveai.com",
        phone = "(41) 99876-5432",
        role = UserRole.CLIENTE,
        city = "Curitiba, PR",
        document = "52998224725",
        profilePhotoUri = "data:image/jpeg;base64,client_gallery_avatar"
      )
    )
    val savedClient = repo.users.value.firstOrNull { it.email == "fernanda.cli@resolveai.com" }
    assertTrue(savedClient != null)
    assertEquals("data:image/jpeg;base64,client_gallery_avatar", savedClient!!.photoUrl)
  }

  @Test
  fun testAdminRejectionOfProviderCancellationRefund_recordsDenialWithoutRefunding() {
    val repo = MarketplaceRepository()
    assertTrue(repo.acceptProposalAndDebitFee("prop_1").isSuccess)
    assertEquals(90.00, repo.users.value.first { it.id == "pro_1" }.walletBalance, 0.01)

    repo.switchUser("pro_1")
    val cancelled = repo.cancelServiceRequest("srv_1", "Não compareci sem justificativa").getOrThrow()
    assertEquals(com.example.model.CancellationRefundStatus.PENDENTE_ANALISE_ADMIN, cancelled.cancellationRefundStatus)

    val admin = repo.users.value.first { it.role == UserRole.ADMIN }
    repo.switchUser(admin.id)
    val denied = repo.adminProcessCancellationRefund("srv_1", approve = false, note = "Cancelamento recorrente sem aviso.")
    assertTrue(denied.isSuccess)
    assertEquals(com.example.model.CancellationRefundStatus.NEGADO_ADMIN, denied.getOrThrow().cancellationRefundStatus)
    assertEquals(90.00, repo.users.value.first { it.id == "pro_1" }.walletBalance, 0.01)
  }

  @Test
  fun testLocationHistory_capturesPointsWhileAcceptedOrInExecutionAndStopsAfterConcluded() {
    val repo = MarketplaceRepository()
    assertTrue(repo.acceptProposalAndDebitFee("prop_1").isSuccess)
    repo.switchUser("pro_1")

    val p1 = repo.recordProviderLocationPoint("srv_1", -23.55052, -46.633308, accuracyMeters = 8f, source = "GPS")
    assertTrue(p1 != null)
    repo.updateServiceStatus("srv_1", ServiceStatus.EM_EXECUCAO)
    val p2 = repo.recordProviderLocationPoint("srv_1", -23.55100, -46.634000, accuracyMeters = 10f, source = "GPS")
    assertTrue(p2 != null)

    val historyDuringService = repo.serviceLocationHistory.value.filter { it.serviceId == "srv_1" }
    assertEquals(2, historyDuringService.size)

    // Once concluded, tracking point capture stops returning null
    repo.updateServiceStatus("srv_1", ServiceStatus.CONCLUIDO)
    val pAfterDone = repo.recordProviderLocationPoint("srv_1", -23.55200, -46.635000, accuracyMeters = 12f, source = "GPS")
    assertEquals(null, pAfterDone)
  }

  @Test
  fun testCompletedService_sendsAutoMessageRequestingPhotosAndClientAttachesPhotos() {
    val repo = MarketplaceRepository()
    assertTrue(repo.acceptProposalAndDebitFee("prop_1").isSuccess)
    repo.switchUser("pro_1")
    repo.updateServiceStatus("srv_1", ServiceStatus.EM_EXECUCAO)
    repo.updateServiceStatus("srv_1", ServiceStatus.CONCLUIDO)

    // System message requesting photos from client must be automatically posted
    val srvMessages = repo.chatMessages.value.filter { it.serviceId == "srv_1" && it.isSystemNotice }
    assertTrue(srvMessages.any { it.content.contains("foto", ignoreCase = true) && it.content.contains("serviço concluído", ignoreCase = true) })

    // Client attaches and removes completed service photo
    repo.switchUser("cli_1")
    val addRes = repo.addClientCompletedServicePhoto(
      serviceId = "srv_1",
      photoUri = "data:image/jpeg;base64,/9j/CLIENT_AFTER",
      photoName = "resultado_cliente.jpg",
      caption = "Ficou excelente"
    )
    assertTrue(addRes.isSuccess)
    val updatedSrv = addRes.getOrThrow()
    assertEquals(1, updatedSrv.clientCompletionPhotos.size)
    val photoId = updatedSrv.clientCompletionPhotos.first().id

    val removeRes = repo.removeClientCompletedServicePhoto("srv_1", photoId)
    assertTrue(removeRes.isSuccess)
    assertTrue(removeRes.getOrThrow().clientCompletionPhotos.isEmpty())
  }

  @Test
  fun testClientAcceptProposal_neverShowsFeeValuesOrPercentagesToClient() {
    val repo = MarketplaceRepository()
    val vm = com.example.ui.MarketplaceViewModel(repo)
    repo.switchUser("cli_1")
    vm.acceptProposal("prop_1")

    val clientSnackbar = vm.snackBarMessage.value.orEmpty()
    assertFalse("Snackbar do cliente não deve mencionar taxa", clientSnackbar.contains("taxa", ignoreCase = true))
    assertFalse("Snackbar do cliente não deve mencionar 10%", clientSnackbar.contains("10%"))

    val clientSystemMessages = repo.chatMessages.value.filter { it.serviceId == "srv_1" && it.isSystemNotice }
    assertTrue(clientSystemMessages.none { it.content.contains("taxa", ignoreCase = true) || it.content.contains("10%") })
  }

  @Test
  fun testSuggestionsAndTips_submitByClientOrProviderAndAdminReview() {
    val repo = MarketplaceRepository()
    val initialTips = repo.appTips
    assertTrue(initialTips.any { it.targetRole == UserRole.CLIENTE })
    assertTrue(initialTips.any { it.targetRole == UserRole.PRESTADOR })

    repo.switchUser("cli_1")
    val created = repo.submitAppSuggestion(
      category = "Novo Serviço",
      title = "Categoria Jardinagem Express",
      description = "Poderia ter opção rápida para poda de gramado."
    )
    assertTrue(created.isSuccess)
    val suggestionId = created.getOrThrow().id
    assertEquals(com.example.model.SuggestionStatus.ENVIADA, created.getOrThrow().status)

    val admin = repo.users.value.first { it.role == UserRole.ADMIN }
    repo.switchUser(admin.id)
    val updated = repo.updateAppSuggestionStatus(
      suggestionId = suggestionId,
      status = com.example.model.SuggestionStatus.IMPLEMENTADA,
      adminReply = "Adicionada ao nosso roadmap!"
    )
    assertTrue(updated.isSuccess)
    assertEquals(com.example.model.SuggestionStatus.IMPLEMENTADA, updated.getOrThrow().status)
    assertEquals("Adicionada ao nosso roadmap!", updated.getOrThrow().adminReply)
  }

  @Test
  fun testOnboardingState_marksSeenPersistsAndAllowsReplay() {
    val tempDir = kotlin.io.path.createTempDirectory("resolveai_onboarding_test").toFile()
    try {
      val store = com.example.data.MarketplaceLocalStore(tempDir)
      val repo = MarketplaceRepository(localStore = store)
      val vm = com.example.ui.MarketplaceViewModel(repo)

      assertFalse(vm.hasSeenOnboarding.value)
      assertFalse(vm.showOnboardingReplay.value)

      vm.dismissOnboarding()
      assertTrue(vm.hasSeenOnboarding.value)
      assertFalse(vm.showOnboardingReplay.value)
      assertEquals(true, store.load()?.hasSeenOnboarding)

      vm.openOnboarding()
      assertTrue(vm.showOnboardingReplay.value)

      vm.dismissOnboarding()
      assertFalse(vm.showOnboardingReplay.value)
      assertTrue(vm.hasSeenOnboarding.value)
    } finally {
      tempDir.deleteRecursively()
    }
  }

  @Test
  fun testOnboardingSlidesCatalog_coversProposalsCounterOffersAndPrivateChatWithoutFees() {
    val slides = com.example.ui.components.OnboardingSlidesCatalog.slides
    assertEquals(3, slides.size)
    assertEquals(com.example.ui.components.OnboardingSlideTopic.PROPOSALS, slides[0].topic)
    assertEquals(com.example.ui.components.OnboardingSlideTopic.COUNTER_OFFERS, slides[1].topic)
    assertEquals(com.example.ui.components.OnboardingSlideTopic.PRIVATE_CHAT, slides[2].topic)

    assertTrue(slides[0].title.contains("propostas", ignoreCase = true))
    assertTrue(slides[1].title.contains("contrapropostas", ignoreCase = true))
    assertTrue(slides[2].title.contains("Chat privado", ignoreCase = true))

    // Onboarding must never display fee percentages
    assertTrue(slides.none { it.description.contains("10%") || it.highlights.any { h -> h.contains("10%") } })
  }

  @Test
  fun testFirestoreHelper_validatesIdsAndFailsGracefullyWhenUninitialized() = kotlinx.coroutines.runBlocking {
    assertTrue(com.example.data.FirestoreHelper.isValidDocumentId("usr_123-ABC"))
    assertFalse(com.example.data.FirestoreHelper.isValidDocumentId(""))
    assertFalse(com.example.data.FirestoreHelper.isValidDocumentId("users/evil"))
    assertFalse(com.example.data.FirestoreHelper.isValidDocumentId("bad.dot"))

    val helper = com.example.data.FirestoreHelper(
      databaseId = "ai-studio-android-2944595a-9bac-47e3-98f7-2cdf5dcec5df",
      firestoreProvider = { null }
    )
    assertFalse(helper.isInitialized)
    assertEquals(null, helper.usersCollection())
    assertEquals(null, helper.servicesCollection())
    assertEquals(null, helper.chatMessagesCollection("srv_1"))

    val invalidProfileRes = helper.getUserProfile("bad/id")
    assertTrue(invalidProfileRes.isFailure)

    val uninitServiceRes = helper.saveService(
      ServiceRequest(
        id = "srv_test_1",
        clientId = "cli_1",
        clientName = "Carlos Teste",
        title = "Troca de tomada",
        category = "Eletricista",
        description = "Substituição de tomada 20A",
        location = "São Paulo, SP",
        proposedBudget = 120.0
      )
    )
    assertTrue(uninitServiceRes.isFailure)

    val blankChatRes = helper.sendChatMessage(
      serviceId = "srv_test_1",
      message = com.example.model.ChatMessage(
        id = "msg_1",
        serviceId = "srv_test_1",
        senderId = "cli_1",
        senderName = "Carlos Teste",
        senderRole = UserRole.CLIENTE,
        content = "   "
      )
    )
    assertTrue(blankChatRes.isFailure)
    assertTrue(helper.observeChatMessages("bad/id").first().isEmpty())
    assertEquals(null, helper.observeUserProfile("bad/id").first())
  }

  @Test
  fun testMultiProviderDirectAcceptance_requiresClientSelectionBeforeUnlockingChatAndAllowsNegativeBalanceAboveOneReal() {
    val repo = MarketplaceRepository()
    repo.switchUser("cli_1")
    val order = repo.createServiceRequest(
      title = "Pintura externa completa",
      category = "Pintura",
      description = "Fachada residencial",
      location = "Moema - São Paulo, SP",
      proposedBudget = 300.0,
      urgency = "Alta"
    )

    // Ajusta pro_1 para R$ 2,00 (> R$ 1,00) e pro_2 para R$ 1,00 (exatamente no limite bloqueado)
    val admin = repo.users.value.first { it.role == UserRole.ADMIN }
    repo.switchUser(admin.id)
    assertTrue(repo.adminUpdateUser(repo.users.value.first { it.id == "pro_1" }.copy(walletBalance = 2.00)).isSuccess)
    assertTrue(repo.adminUpdateUser(repo.users.value.first { it.id == "pro_2" }.copy(walletBalance = 1.00)).isSuccess)

    // pro_2 com R$ 1,00 (não é > 1,00) é bloqueado
    repo.switchUser("pro_2")
    val blockedAccept = repo.acceptServiceDirectlyByProvider(order.id)
    assertTrue(blockedAccept.isFailure)
    assertTrue(blockedAccept.exceptionOrNull() is InsufficientBalanceException)

    // Recarrega R$ 10,00 em pro_2 (vai para R$ 11,00) e aceita o mesmo serviço
    repo.switchUser(admin.id)
    repo.rechargeWallet("pro_2", 10.0)

    // Primeiro pro_1 (R$ 2,00 > R$ 1,00) aceita o serviço diretamente -> não fecha ainda e não abre chat!
    repo.switchUser("pro_1")
    val accept1 = repo.acceptServiceDirectlyByProvider(order.id)
    assertTrue(accept1.isSuccess)
    val srvAfterPro1 = repo.services.value.first { it.id == order.id }
    assertEquals(ServiceStatus.EM_NEGOCIACAO, srvAfterPro1.status)
    assertEquals(null, srvAfterPro1.acceptedProviderId)
    assertEquals(2.00, repo.users.value.first { it.id == "pro_1" }.walletBalance, 0.01)

    // Segundo prestador pro_2 também pode aceitar o mesmo serviço para o cliente escolher
    repo.switchUser("pro_2")
    val accept2 = repo.acceptServiceDirectlyByProvider(order.id)
    assertTrue(accept2.isSuccess)

    val directAcceptances = repo.proposals.value.filter { it.serviceId == order.id && it.isDirectAcceptance }
    assertEquals(2, directAcceptances.size)

    // Cliente escolhe pro_1 -> debita R$ 30,00 (10% de R$ 300), pro_1 fica com -R$ 28,00,
    // pro_2 é recusado e chat é liberado apenas com pro_1!
    repo.switchUser("cli_1")
    val pro1Proposal = directAcceptances.first { it.providerId == "pro_1" }
    val chooseResult = repo.acceptProposalAndDebitFee(pro1Proposal.id)
    assertTrue(chooseResult.isSuccess)

    val finalService = repo.services.value.first { it.id == order.id }
    assertEquals(ServiceStatus.EM_EXECUCAO, finalService.status)
    assertEquals("Em Andamento", finalService.status.label)
    assertEquals("pro_1", finalService.acceptedProviderId)
    assertEquals(-28.00, repo.users.value.first { it.id == "pro_1" }.walletBalance, 0.01)

    val pro2ProposalAfter = repo.proposals.value.first { it.serviceId == order.id && it.providerId == "pro_2" }
    assertEquals(ProposalStatus.RECUSADA, pro2ProposalAfter.status)

    // Na próxima recarga de R$ 50,00 de pro_1, o saldo negativo de -R$ 28,00 é descontado -> saldo final R$ 22,00
    val afterRecharge = repo.rechargeWallet("pro_1", 50.0)
    assertEquals(22.00, afterRecharge, 0.01)
  }

  @Test
  fun testAdminCatalogAndEdits_managesCategoriesServicesSettingsTipsAndGuardsNonAdmin() {
    val repo = MarketplaceRepository()

    // Cliente não-admin não pode editar catálogo nem configurações
    repo.switchUser("cli_1")
    assertTrue(repo.adminAddServiceCategory("Jardinagem", "Paisagismo").isFailure)
    assertTrue(repo.adminDeleteServiceCategory("cat_eletricista").isFailure)
    assertTrue(repo.adminDeleteAppTip("tip_cli_1").isFailure)

    // Muda para Admin
    val admin = repo.users.value.first { it.role == UserRole.ADMIN }
    repo.switchUser(admin.id)

    // 1. Adicionar categoria + impedir duplicata + editar e propagar nome nos serviços + mover + alternar + excluir
    val addedCat = repo.adminAddServiceCategory("Jardinagem", "Jardinagem & Paisagismo")
    assertTrue(addedCat.isSuccess)
    val cat = addedCat.getOrThrow()
    assertTrue(repo.adminAddServiceCategory("jardinagem", "Duplicada").isFailure)

    // Cria serviço nessa categoria para validar propagação ao renomear categoria
    val catSrv = repo.createServiceRequest(
      title = "Poda de árvore",
      category = "Jardinagem",
      description = "Quintal",
      location = "Curitiba, PR",
      proposedBudget = 180.0,
      urgency = "Média"
    )
    val renamedCat = repo.adminUpdateServiceCategory(cat.id, "Paisagismo", "Paisagismo Completo", true)
    assertTrue(renamedCat.isSuccess)
    assertEquals("Paisagismo", repo.services.value.first { it.id == catSrv.id }.category)

    assertTrue(repo.adminMoveServiceCategory(cat.id, moveUp = true).isSuccess)
    assertTrue(repo.adminToggleServiceCategory(cat.id, isActive = false).isSuccess)
    assertFalse(repo.serviceCategories.value.first { it.id == cat.id }.isActive)
    assertTrue(repo.adminDeleteServiceCategory(cat.id).isSuccess)
    assertTrue(repo.serviceCategories.value.none { it.id == cat.id })

    // 2. Admin edita serviço existente
    val editedSrv = repo.adminUpdateServiceRequest(
      catSrv.copy(
        title = "Poda de árvores e gramado",
        proposedBudget = 240.0,
        urgency = "Alta"
      )
    )
    assertTrue(editedSrv.isSuccess)
    assertEquals("Poda de árvores e gramado", editedSrv.getOrThrow().title)
    assertEquals(240.0, editedSrv.getOrThrow().proposedBudget, 0.01)

    // 3. Outras Edições: configurações gerais + SupportLauncher + Dicas
    val savedCfg = repo.adminUpdateAppSettings(
      com.example.model.AppAdminSettings(
        appDisplayName = "ResolveAi Serviços",
        globalBannerMessage = "Plantão 24h ativo hoje!",
        supportWhatsappNumberE164 = "5561999998888",
        supportWhatsappDisplay = "(61) 99999-8888",
        defaultServiceUrgency = "Alta"
      )
    )
    assertTrue(savedCfg.isSuccess)
    assertEquals("Plantão 24h ativo hoje!", repo.appAdminSettings.value.globalBannerMessage)
    assertEquals("5561999998888", com.example.data.SupportLauncher.activeWhatsappNumberE164)
    assertEquals("(61) 99999-8888", com.example.data.SupportLauncher.activeWhatsappDisplay)
    com.example.data.SupportLauncher.updateSupportContact(
      com.example.data.SupportLauncher.SUPPORT_WHATSAPP_NUMBER_E164,
      com.example.data.SupportLauncher.SUPPORT_WHATSAPP_DISPLAY
    )

    val savedTip = repo.adminSaveAppTip(
      com.example.model.AppTip(
        id = "",
        targetRole = UserRole.CLIENTE,
        category = "Agendamento",
        title = "Informe horários disponíveis",
        description = "Coloque na descrição os melhores horários para receber visitas."
      )
    )
    assertTrue(savedTip.isSuccess)
    val tipId = savedTip.getOrThrow().id
    assertTrue(repo.appTips.any { it.id == tipId })

    assertTrue(repo.adminDeleteAppTip(tipId).isSuccess)
    assertTrue(repo.appTips.none { it.id == tipId })
  }

  @Test
  fun testPostServiceRating_validatesConstraintsUpdatesProviderStatsPersistsAndSupportsReply() {
    val tempDir = kotlin.io.path.createTempDirectory("resolveai_reviews_test").toFile()
    try {
      val store1 = com.example.data.MarketplaceLocalStore(tempDir)
      val repo1 = MarketplaceRepository(localStore = store1)

      repo1.switchUser("cli_1")
      val srv = repo1.createServiceRequest(
        title = "Troca de disjuntor geral",
        category = "Eletricista",
        description = "Quadro antigo",
        location = "Moema - SP",
        proposedBudget = 250.0,
        urgency = "Alta"
      )

      repo1.switchUser("pro_1")
      val prop = repo1.submitProposal(
        serviceId = srv.id,
        proposedPrice = 250.0,
        estimatedDays = 1,
        description = "Atendo amanhã"
      )

      repo1.switchUser("cli_1")
      assertTrue(repo1.acceptProposalAndDebitFee(prop.id).isSuccess)

      // Cannot rate before CONCLUIDO
      assertTrue(repo1.rateService(srv.id, 5f, "Antes da hora").isFailure)

      repo1.switchUser("pro_1")
      repo1.updateServiceStatus(srv.id, ServiceStatus.EM_EXECUCAO)
      repo1.updateServiceStatus(srv.id, ServiceStatus.CONCLUIDO)

      // Provider cannot rate their own service
      assertTrue(repo1.rateService(srv.id, 5f, "Autoavaliação").isFailure)

      // Client cannot submit out-of-range rating
      repo1.switchUser("cli_1")
      assertTrue(repo1.rateService(srv.id, 0f, "Nota zero").isFailure)
      assertTrue(repo1.rateService(srv.id, 6f, "Nota seis").isFailure)

      // Valid 4-star rating with compliment tags & comment
      val rateRes = repo1.rateService(
        serviceId = srv.id,
        rating = 4f,
        comment = "Ótimo profissional, muito organizado!",
        complimentTags = listOf("Pontual", "Preço Justo")
      )
      assertTrue(rateRes.isSuccess)
      val review = rateRes.getOrThrow()
      assertEquals(4f, review.rating, 0.01f)
      assertEquals(listOf("Pontual", "Preço Justo"), review.complimentTags)
      assertEquals("Ótimo profissional, muito organizado!", review.comment)

      // Duplicate rating for the same service is blocked
      assertTrue(repo1.rateService(srv.id, 5f, "Segunda nota").isFailure)

      // Service & provider average rating + ratingCount updated
      val updatedService = repo1.services.value.first { it.id == srv.id }
      assertEquals(4f, updatedService.ratingGiven ?: 0f, 0.01f)
      assertEquals(listOf("Pontual", "Preço Justo"), updatedService.reviewTags)
      assertTrue(updatedService.reviewedAt != null)

      val providerAfterRate = repo1.users.value.first { it.id == "pro_1" }
      val expectedAvg = repo1.getReviewsForProvider("pro_1").map { it.rating.toDouble() }.average()
      assertEquals(Math.round(expectedAvg * 100.0) / 100.0, providerAfterRate.rating, 0.01)
      assertEquals(repo1.getReviewsForProvider("pro_1").size, providerAfterRate.ratingCount)

      // Another provider cannot reply; target provider replies once
      repo1.switchUser("pro_2")
      assertTrue(repo1.replyToReview(review.id, "Obrigado falso").isFailure)

      repo1.switchUser("pro_1")
      assertTrue(repo1.replyToReview(review.id, "   ").isFailure)
      assertTrue(repo1.replyToReview(review.id, "Muito obrigado pela confiança, Carlos!").isSuccess)
      assertTrue(repo1.replyToReview(review.id, "Tentativa duplicada").isFailure)

      // Local store persistence across restart preserves reviews & reply
      val repo2 = MarketplaceRepository(localStore = com.example.data.MarketplaceLocalStore(tempDir))
      val restoredReview = repo2.reviews.value.firstOrNull { it.serviceId == srv.id }
      assertTrue(restoredReview != null)
      assertEquals("Muito obrigado pela confiança, Carlos!", restoredReview?.providerReply)
      assertEquals(listOf("Pontual", "Preço Justo"), restoredReview?.complimentTags)
    } finally {
      tempDir.deleteRecursively()
    }
  }

  @Test
  fun testRejectProposal_marksRefusedAndRevertsServiceToOpenWhenAllRefused() {
    val repo = MarketplaceRepository()
    repo.switchUser("cli_1")
    val order = repo.createServiceRequest(
      title = "Instalação de ar-condicionado",
      category = "Refrigeração",
      description = "Aparelho 9000 BTUs",
      location = "Pinheiros - SP",
      proposedBudget = 320.0,
      urgency = "Média"
    )

    repo.switchUser("pro_1")
    val prop1 = repo.submitProposal(order.id, 320.0, 1, "Posso ir amanhã")
    repo.switchUser("pro_2")
    val prop2 = repo.submitProposal(order.id, 310.0, 1, "Atendo hoje")

    // Provider cannot reject competing proposal
    assertTrue(repo.rejectProposal(prop1.id).isFailure)

    // Client rejects pro_1 -> prop2 stays active so service stays EM_NEGOCIACAO
    repo.switchUser("cli_1")
    assertTrue(repo.rejectProposal(prop1.id).isSuccess)
    assertEquals(ProposalStatus.RECUSADA, repo.proposals.value.first { it.id == prop1.id }.status)
    assertEquals(ServiceStatus.EM_NEGOCIACAO, repo.services.value.first { it.id == order.id }.status)

    // Client rejects pro_2 as well -> no active proposals remain so service reverts to ABERTO
    assertTrue(repo.rejectProposal(prop2.id).isSuccess)
    assertEquals(ProposalStatus.RECUSADA, repo.proposals.value.first { it.id == prop2.id }.status)
    assertEquals(ServiceStatus.ABERTO, repo.services.value.first { it.id == order.id }.status)
  }

  @Test
  fun testAcceptedProviderExclusivity_removesServiceFromOtherProvidersAndLocksChatToAcceptedPair() {
    val repo = MarketplaceRepository()
    repo.switchUser("cli_1")
    val order = repo.createServiceRequest(
      title = "Montagem de guarda-roupa",
      category = "Montagem",
      description = "6 portas com espelho",
      location = "Vila Mariana - SP",
      proposedBudget = 240.0,
      urgency = "Média"
    )

    val cli1 = repo.users.value.first { it.id == "cli_1" }
    val pro1 = repo.users.value.first { it.id == "pro_1" }
    val pro2 = repo.users.value.first { it.id == "pro_2" }

    // Both pro_1 and pro_2 accept directly -> still visible to both, chat closed for everyone
    repo.switchUser("pro_1")
    assertTrue(repo.acceptServiceDirectlyByProvider(order.id).isSuccess)
    repo.switchUser("pro_2")
    assertTrue(repo.acceptServiceDirectlyByProvider(order.id).isSuccess)

    val negotiatingOrder = repo.services.value.first { it.id == order.id }
    assertTrue(MarketplaceRepository.isServiceVisibleForProvider(negotiatingOrder, pro1))
    assertTrue(MarketplaceRepository.isServiceVisibleForProvider(negotiatingOrder, pro2))
    assertFalse(repo.canUserAccessServiceChat(negotiatingOrder, cli1))
    assertFalse(repo.canUserAccessServiceChat(negotiatingOrder, pro1))
    assertFalse(repo.canUserAccessServiceChat(negotiatingOrder, pro2))

    // Client accepts pro_1 -> service disappears from pro_2 and chat unlocks ONLY for cli_1 and pro_1
    repo.switchUser("cli_1")
    val pro1Proposal = repo.proposals.value.first { it.serviceId == order.id && it.providerId == "pro_1" }
    assertTrue(repo.acceptProposalAndDebitFee(pro1Proposal.id).isSuccess)

    val acceptedOrder = repo.services.value.first { it.id == order.id }
    assertTrue(MarketplaceRepository.isServiceVisibleForProvider(acceptedOrder, pro1))
    assertFalse(
      "Serviço aceito por pro_1 deve sair da lista de pro_2",
      MarketplaceRepository.isServiceVisibleForProvider(acceptedOrder, pro2)
    )
    assertTrue(repo.canUserAccessServiceChat(acceptedOrder, cli1))
    assertTrue(repo.canUserAccessServiceChat(acceptedOrder, pro1))
    assertFalse(
      "pro_2 não pode ter acesso ao chat de um serviço fechado com pro_1",
      repo.canUserAccessServiceChat(acceptedOrder, pro2)
    )
  }

  @Test
  fun testFirebaseAuthSessionSync_authenticatesKnownGoogleUserAndRoutesNewUsersToCompletion() {
    val repo = MarketplaceRepository()
    repo.logout()
    val vm = com.example.ui.MarketplaceViewModel(repo)

    assertFalse(vm.isAuthenticated.value)

    // Novo usuário Google ainda não cadastrado -> abre etapa de complementação de perfil
    val newGoogle = com.example.data.GoogleAccountInfo(
      uid = "fb_new_google_uid",
      email = "novo.google@resolveai.com",
      displayName = "Novo Usuário Google"
    )
    vm.syncFirebaseAuthSession(newGoogle)
    assertEquals(com.example.ui.AuthFlowStep.GOOGLE_ROLE_COMPLETION, vm.authStep.value)
    assertEquals("fb_new_google_uid", vm.pendingGoogleAccount.value?.uid)
    assertFalse(vm.isAuthenticated.value)

    // Usuário Google já existente no repositório -> autentica sessão imediatamente
    val knownClient = repo.users.value.first { it.id == "cli_1" }
    val knownGoogle = com.example.data.GoogleAccountInfo(
      uid = "fb_existing_cli_1",
      email = knownClient.email,
      displayName = knownClient.name
    )
    vm.syncFirebaseAuthSession(knownGoogle)
    assertTrue(vm.isAuthenticated.value)
    assertEquals("cli_1", vm.currentUserId.value)
    assertEquals(com.example.ui.AuthFlowStep.FORM, vm.authStep.value)
  }

  @Test
  fun testGooglePlacesService_extractsFullAddressFromReverseGeocode() {
    val response = com.example.data.ReverseGeocodeResponse(
      status = "OK",
      results = listOf(
        com.example.data.ReverseGeocodeResultDto(
          formattedAddress = "Av. Paulista, 1000 - Bela Vista, São Paulo - SP, Brasil",
          addressComponents = listOf(
            com.example.data.ReverseGeocodeComponentDto(
              longName = "Av. Paulista",
              shortName = "Av. Paulista",
              types = listOf("route")
            ),
            com.example.data.ReverseGeocodeComponentDto(
              longName = "1000",
              shortName = "1000",
              types = listOf("street_number")
            ),
            com.example.data.ReverseGeocodeComponentDto(
              longName = "Bela Vista",
              shortName = "Bela Vista",
              types = listOf("sublocality_level_1", "sublocality")
            ),
            com.example.data.ReverseGeocodeComponentDto(
              longName = "São Paulo",
              shortName = "São Paulo",
              types = listOf("locality", "political")
            ),
            com.example.data.ReverseGeocodeComponentDto(
              longName = "São Paulo",
              shortName = "SP",
              types = listOf("administrative_area_level_1", "political")
            )
          )
        )
      )
    )

    val address = com.example.data.GooglePlacesService.extractAddressFromReverseGeocode(response)
    assertEquals("Av. Paulista, 1000 - Bela Vista - São Paulo, SP", address)
    assertEquals(
      "Rua das Flores, 120 - Centro - Campinas, SP",
      com.example.data.GooglePlacesService.formatFullAddress(
        street = "Rua das Flores",
        number = "120",
        neighborhood = "Centro",
        city = "Campinas",
        adminArea = "São Paulo"
      )
    )
  }

  @Test
  fun testAdminAddProviderBalance_validatesRoleAndRecordsCreditoAdminTransaction() {
    val repo = MarketplaceRepository()
    val provider = repo.users.value.first { it.id == "pro_1" }
    val initialBalance = provider.walletBalance

    // Non-admin cannot credit balance
    repo.switchUser("cli_1")
    assertTrue(repo.adminAddProviderBalance("pro_1", 50.0, "Teste").isFailure)

    // Switch to Admin
    val admin = repo.users.value.first { it.role == UserRole.ADMIN }
    repo.switchUser(admin.id)

    // Invalid amount <= 0 or target CLIENT rejected
    assertTrue(repo.adminAddProviderBalance("pro_1", 0.0, "Zero").isFailure)
    assertTrue(repo.adminAddProviderBalance("pro_1", -10.0, "Negativo").isFailure)
    assertTrue(repo.adminAddProviderBalance("cli_1", 50.0, "Cliente").isFailure)

    // Valid credit adds balance, records CREDITO_ADMIN transaction & notification
    val res = repo.adminAddProviderBalance("pro_1", 75.50, "Bônus de campanha")
    assertTrue(res.isSuccess)
    assertEquals(initialBalance + 75.50, res.getOrThrow(), 0.01)

    val updatedProvider = repo.users.value.first { it.id == "pro_1" }
    assertEquals(initialBalance + 75.50, updatedProvider.walletBalance, 0.01)

    val tx = repo.walletTransactions.value.first {
      it.providerId == "pro_1" && it.type == com.example.model.TransactionType.CREDITO_ADMIN
    }
    assertEquals(75.50, tx.amount, 0.01)
    assertTrue(tx.description.contains("Bônus de campanha"))
    assertEquals(initialBalance + 75.50, tx.balanceAfter, 0.01)

    assertTrue(
      repo.notifications.value.any {
        it.recipientId == "pro_1" && it.title.contains("Saldo Adicionado")
      }
    )
  }

  @Test
  fun testProviderDeclineService_removesFromDecliningProviderListOnly() {
    val tempDir = kotlin.io.path.createTempDirectory("resolveai_decline_test").toFile()
    try {
      val repo = MarketplaceRepository(localStore = com.example.data.MarketplaceLocalStore(tempDir))
      repo.switchUser("cli_1")
      val order = repo.createServiceRequest(
        title = "Troca de resistência do chuveiro",
        category = "Eletricista",
        description = "Chuveiro 220V sem esquentar",
        location = "Moema - São Paulo, SP",
        proposedBudget = 160.0,
        urgency = "Alta"
      )

      // Client cannot invoke provider decline
      assertTrue(repo.declineServiceByProvider(order.id).isFailure)

      // pro_1 submits proposal then declines in order details
      repo.switchUser("pro_1")
      val prop = repo.submitProposal(order.id, 170.0, 1, "Posso hoje")
      assertEquals(ServiceStatus.EM_NEGOCIACAO, repo.services.value.first { it.id == order.id }.status)

      val declineRes = repo.declineServiceByProvider(order.id)
      assertTrue(declineRes.isSuccess)

      val pro1Updated = repo.users.value.first { it.id == "pro_1" }
      val pro2 = repo.users.value.first { it.id == "pro_2" }

      // Declined order disappears from pro_1, stays visible to pro_2, cancels pro_1 proposal & reverts status to ABERTO
      assertTrue(order.id in pro1Updated.declinedServiceIds)
      assertTrue(order.id in repo.declinedServiceIdsByProvider.value["pro_1"].orEmpty())
      assertEquals(ProposalStatus.RECUSADA, repo.proposals.value.first { it.id == prop.id }.status)
      assertEquals(ServiceStatus.ABERTO, repo.services.value.first { it.id == order.id }.status)
      assertFalse(MarketplaceRepository.isServiceVisibleForProvider(repo.services.value.first { it.id == order.id }, pro1Updated))
      assertTrue(MarketplaceRepository.isServiceVisibleForProvider(repo.services.value.first { it.id == order.id }, pro2))

      // Cold restart preserves declinedServiceIds
      val repoRestored = MarketplaceRepository(localStore = com.example.data.MarketplaceLocalStore(tempDir))
      val pro1Restored = repoRestored.users.value.first { it.id == "pro_1" }
      assertFalse(MarketplaceRepository.isServiceVisibleForProvider(repoRestored.services.value.first { it.id == order.id }, pro1Restored))
    } finally {
      tempDir.deleteRecursively()
    }
  }

  @Test
  fun testProviderOrderHistoryHelper_classifiesSummarizesAndFiltersFinishedCanceledDeclined() {
    val provider = User(
      id = "pro_1",
      firebaseUid = "fb_pro_1",
      name = "Roberto Mendes",
      email = "roberto@example.com",
      phone = "(11) 99999-0001",
      role = UserRole.PRESTADOR,
      declinedServiceIds = listOf("srv_hist_declined_self")
    )

    val completedSrv = ServiceRequest(
      id = "srv_hist_done",
      clientId = "cli_1",
      clientName = "Carlos Eduardo",
      title = "Troca de quadro elétrico",
      category = "Eletricista",
      description = "Quadro completo",
      location = "Pinheiros - São Paulo, SP",
      proposedBudget = 400.0,
      finalAgreedPrice = 450.0,
      status = ServiceStatus.CONCLUIDO,
      acceptedProviderId = "pro_1",
      acceptedProviderName = "Roberto Mendes",
      completionNotes = "Tudo testado com sucesso",
      createdAt = 1000L
    )
    val canceledSrv = ServiceRequest(
      id = "srv_hist_canceled",
      clientId = "cli_2",
      clientName = "Mariana Silveira",
      title = "Instalação de ventilador",
      category = "Eletricista",
      description = "Sala de estar",
      location = "Campinas, SP",
      proposedBudget = 180.0,
      status = ServiceStatus.CANCELADO,
      acceptedProviderId = "fb_pro_1",
      cancellationReason = "Cliente precisou remarcar viagem",
      cancelledAt = 3000L,
      createdAt = 1500L
    )
    val declinedBySelfSrv = ServiceRequest(
      id = "srv_hist_declined_self",
      clientId = "cli_1",
      clientName = "Carlos Eduardo",
      title = "Passagem de cabo de rede",
      category = "TI, Redes & Informática",
      description = "2 pontos",
      location = "Moema - São Paulo, SP",
      proposedBudget = 200.0,
      status = ServiceStatus.ABERTO,
      createdAt = 2500L
    )
    val rejectedProposalSrv = ServiceRequest(
      id = "srv_hist_rejected_prop",
      clientId = "cli_2",
      clientName = "Mariana Silveira",
      title = "Luminária LED",
      category = "Eletricista",
      description = "Cozinha",
      location = "Cambuí - Campinas, SP",
      proposedBudget = 120.0,
      status = ServiceStatus.ABERTO,
      createdAt = 2000L
    )
    val unrelatedOpenSrv = ServiceRequest(
      id = "srv_unrelated_open",
      clientId = "cli_1",
      clientName = "Carlos Eduardo",
      title = "Tomada simples",
      category = "Eletricista",
      description = "Quarto",
      location = "Centro - SP",
      proposedBudget = 90.0,
      status = ServiceStatus.ABERTO,
      createdAt = 4000L
    )

    val rejectedProp = com.example.model.Proposal(
      id = "prop_rej_hist",
      serviceId = "srv_hist_rejected_prop",
      providerId = "pro_1",
      providerName = "Roberto Mendes",
      proposedPrice = 150.0,
      status = ProposalStatus.RECUSADA,
      createdAt = 2100L
    )

    val review = com.example.model.ServiceReview(
      id = "rev_hist_done",
      serviceId = "srv_hist_done",
      serviceTitle = completedSrv.title,
      serviceCategory = completedSrv.category,
      clientId = "cli_1",
      clientName = "Carlos Eduardo",
      providerId = "pro_1",
      providerName = "Roberto Mendes",
      rating = 5.0f,
      comment = "Impecável!",
      createdAt = 5000L
    )

    val items = com.example.data.ProviderOrderHistoryHelper.buildHistory(
      provider = provider,
      services = listOf(completedSrv, canceledSrv, declinedBySelfSrv, rejectedProposalSrv, unrelatedOpenSrv),
      proposals = listOf(rejectedProp),
      reviews = listOf(review)
    )

    assertEquals(4, items.size)
    assertFalse(items.any { it.service.id == "srv_unrelated_open" })

    val summary = com.example.data.ProviderOrderHistoryHelper.summarize(items)
    assertEquals(4, summary.totalCount)
    assertEquals(1, summary.completedCount)
    assertEquals(1, summary.canceledCount)
    assertEquals(2, summary.declinedCount)
    assertEquals(450.0, summary.totalCompletedValue, 0.01)

    val completedFiltered = com.example.data.ProviderOrderHistoryHelper.filterItems(
      items,
      com.example.data.ProviderHistoryFilter.FINALIZADO
    )
    assertEquals(listOf("srv_hist_done"), completedFiltered.map { it.service.id })
    assertEquals(5.0f, completedFiltered.first().review?.rating ?: 0f, 0.01f)

    val declinedSelf = items.first { it.service.id == "srv_hist_declined_self" }
    assertEquals(com.example.data.ProviderHistoryCategory.RECUSADO, declinedSelf.category)
    assertEquals("Recusado por você", declinedSelf.outcomeLabel)

    val queryFiltered = com.example.data.ProviderOrderHistoryHelper.filterItems(
      items,
      com.example.data.ProviderHistoryFilter.TODOS,
      "ventilador"
    )
    assertEquals(listOf("srv_hist_canceled"), queryFiltered.map { it.service.id })
  }

  @Test
  fun testProposalAcceptance_transitionsToEmAndamentoAndRefusesCompetingProposals() {
    val repo = MarketplaceRepository()
    repo.switchUser("cli_1")
    val order = repo.createServiceRequest(
      title = "Troca de Disjuntor Geral",
      category = "Eletricista",
      description = "Quadro residencial",
      location = "Centro - SP",
      proposedBudget = 100.0,
      urgency = "Alta"
    )

    repo.switchUser("pro_1")
    val chosenProp = repo.submitProposal(
      serviceId = order.id,
      proposedPrice = 90.0,
      estimatedDays = 1,
      description = "Faço hoje mesmo"
    )

    repo.switchUser("pro_2")
    val competingProp = repo.submitProposal(
      serviceId = order.id,
      proposedPrice = 80.0,
      estimatedDays = 1,
      description = "Tenho disponibilidade imediata"
    )

    repo.switchUser("cli_1")
    val acceptResult = repo.acceptProposalAndDebitFee(chosenProp.id)
    assertTrue(acceptResult.isSuccess)

    val updatedOrder = repo.services.value.first { it.id == order.id }
    assertEquals(ServiceStatus.EM_EXECUCAO, updatedOrder.status)
    assertEquals("Em Andamento", updatedOrder.status.label)
    assertEquals("pro_1", updatedOrder.acceptedProviderId)
    assertEquals(chosenProp.providerName, updatedOrder.acceptedProviderName)

    val proposalsForOrder = repo.proposals.value.filter { it.serviceId == order.id }
    val acceptedCount = proposalsForOrder.count { it.status == ProposalStatus.ACEITA }
    val refusedCount = proposalsForOrder.count { it.status == ProposalStatus.RECUSADA }
    assertEquals(1, acceptedCount)
    assertEquals(1, refusedCount)
    assertEquals(ProposalStatus.ACEITA, proposalsForOrder.first { it.id == chosenProp.id }.status)
    assertEquals(ProposalStatus.RECUSADA, proposalsForOrder.first { it.id == competingProp.id }.status)
  }

  @Test
  fun testClientCompletionAndMultipleFiveStarRatings_recalculatesProviderAverageAndBlocksNonOwner() {
    val repo = MarketplaceRepository()
    repo.switchUser("cli_1")
    val firstOrder = repo.createServiceRequest(
      title = "Instalação de Chuveiro",
      category = "Eletricista",
      description = "Banheiro principal",
      location = "Centro - SP",
      proposedBudget = 120.0,
      urgency = "Normal"
    )
    val secondOrder = repo.createServiceRequest(
      title = "Troca de Tomadas",
      category = "Eletricista",
      description = "Sala de estar",
      location = "Centro - SP",
      proposedBudget = 150.0,
      urgency = "Normal"
    )

    repo.switchUser("pro_1")
    val p1 = repo.submitProposal(firstOrder.id, 120.0, 1, "Atendo hoje")
    val p2 = repo.submitProposal(secondOrder.id, 150.0, 1, "Atendo amanhã")

    repo.switchUser("cli_1")
    assertTrue(repo.acceptProposalAndDebitFee(p1.id).isSuccess)
    assertTrue(repo.acceptProposalAndDebitFee(p2.id).isSuccess)

    // Client completes both orders directly
    repo.updateServiceStatus(firstOrder.id, ServiceStatus.CONCLUIDO)
    repo.updateServiceStatus(secondOrder.id, ServiceStatus.CONCLUIDO)
    assertEquals(ServiceStatus.CONCLUIDO, repo.services.value.first { it.id == firstOrder.id }.status)
    assertEquals(ServiceStatus.CONCLUIDO, repo.services.value.first { it.id == secondOrder.id }.status)

    val baselineReviews = repo.getReviewsForProvider("pro_1")
    val res1 = repo.rateService(firstOrder.id, 5f, "Excelente serviço!", listOf("Pontual", "Super Recomendo"))
    val res2 = repo.rateService(secondOrder.id, 3f, "Bom atendimento.", listOf("Preço Justo"))
    assertTrue(res1.isSuccess)
    assertTrue(res2.isSuccess)

    val allProviderReviews = repo.getReviewsForProvider("pro_1")
    assertEquals(baselineReviews.size + 2, allProviderReviews.size)
    val expectedAverage = Math.round(allProviderReviews.map { it.rating.toDouble() }.average() * 100.0) / 100.0
    val updatedProvider = repo.users.value.first { it.id == "pro_1" }
    assertEquals(expectedAverage, updatedProvider.rating, 0.01)
    assertEquals(allProviderReviews.size, updatedProvider.ratingCount)

    // Non-owner client cannot submit review for another client's order
    val thirdOrder = repo.createServiceRequest(
      title = "Revisão Quadro",
      category = "Eletricista",
      description = "Quadro antigo",
      location = "Centro - SP",
      proposedBudget = 200.0,
      urgency = "Normal"
    )
    repo.switchUser("pro_1")
    val p3 = repo.submitProposal(thirdOrder.id, 200.0, 1, "Ok")
    repo.switchUser("cli_1")
    assertTrue(repo.acceptProposalAndDebitFee(p3.id).isSuccess)
    repo.updateServiceStatus(thirdOrder.id, ServiceStatus.CONCLUIDO)
    assertEquals(ServiceStatus.CONCLUIDO, repo.services.value.first { it.id == thirdOrder.id }.status)

    repo.switchUser("cli_2")
    assertTrue(repo.rateService(thirdOrder.id, 5f, "Tentativa indevida").isFailure)
  }

  @Test
  fun testAdminPasswordChange_hashesPersistsAndAuthenticatesByEmailOrPhone() {
    val tempDir = kotlin.io.path.createTempDirectory("resolveai_admin_pwd_test").toFile()
    try {
      val store = com.example.data.MarketplaceLocalStore(tempDir)
      val repo = MarketplaceRepository(localStore = store)

      // Non-admin cannot alter another user's password
      repo.switchUser("cli_1")
      assertTrue(repo.adminSetUserPassword("pro_1", "NovaSenha123").isFailure)

      val admin = repo.users.value.first { it.role == UserRole.ADMIN }
      repo.switchUser(admin.id)

      // Password shorter than 6 characters is rejected
      assertTrue(repo.adminSetUserPassword("pro_1", "12345").isFailure)

      // Admin sets new password for pro_1
      val changeRes = repo.adminSetUserPassword("pro_1", "Segura2026")
      assertTrue(changeRes.isSuccess)

      val pro1 = repo.users.value.first { it.id == "pro_1" }
      assertEquals("pro_1", repo.verifyAdminManagedPasswordByEmail(pro1.email, "Segura2026")?.id)
      assertEquals(null, repo.verifyAdminManagedPasswordByEmail(pro1.email, "Errada999"))
      assertEquals("pro_1", repo.verifyAdminManagedPasswordByPhone(pro1.phone, "Segura2026")?.id)

      // Verify persistence across repository reload
      val reloadedRepo = MarketplaceRepository(localStore = com.example.data.MarketplaceLocalStore(tempDir))
      assertEquals("pro_1", reloadedRepo.verifyAdminManagedPasswordByEmail(pro1.email, "Segura2026")?.id)

      // Admin manual registration with password also persists and allows credential sign-in
      val vm = com.example.ui.MarketplaceViewModel(reloadedRepo)
      reloadedRepo.switchUser(admin.id)
      vm.registerUserDirectly(
        com.example.ui.PendingRegistration(
          name = "Patrícia Nova",
          email = "patricia.nova@resolveai.com",
          phone = "(11) 98123-4567",
          role = UserRole.CLIENTE,
          city = "São Paulo, SP",
          document = "52998224725",
          password = "MinhaSenha99"
        )
      )
      assertEquals(
        "patricia.nova@resolveai.com",
        reloadedRepo.verifyAdminManagedPasswordByEmail("patricia.nova@resolveai.com", "MinhaSenha99")?.email
      )
    } finally {
      tempDir.deleteRecursively()
    }
  }

  @Test
  fun testAdminChatMonitoringAccess_allowsAdminOnAgreedServicesIncludingCancelledAndBlocksOpen() {
    val repo = MarketplaceRepository()
    repo.switchUser("cli_1")
    val openOrder = repo.createServiceRequest(
      title = "Orçamento aberto sem acordo",
      category = "Eletricista",
      description = "Sem prestador aceito ainda",
      location = "Centro - SP",
      proposedBudget = 150.0,
      urgency = "Normal"
    )

    // Accept prop_1 so srv_1 becomes agreed (EM_EXECUCAO)
    assertTrue(repo.acceptProposalAndDebitFee("prop_1").isSuccess)

    val admin = repo.users.value.first { it.role == UserRole.ADMIN }
    val agreedService = repo.services.value.first { it.id == "srv_1" }

    // Admin can view chat for agreed service, but not for open service without agreement
    assertTrue(repo.canUserAccessServiceChat(agreedService, admin))
    assertFalse(repo.canUserAccessServiceChat(openOrder, admin))

    // Admin can still view chat history if agreed service is later cancelled
    repo.switchUser("cli_1")
    assertTrue(repo.cancelServiceRequest("srv_1", "Cliente precisou reagendar").isSuccess)
    val cancelledAgreed = repo.services.value.first { it.id == "srv_1" }
    assertTrue(repo.canUserAccessServiceChat(cancelledAgreed, admin))

    // Unrelated user cannot view the chat
    val otherProvider = repo.users.value.first { it.id == "pro_2" }
    assertFalse(repo.canUserAccessServiceChat(cancelledAgreed, otherProvider))
  }

  @Test
  fun testAcceptProposal_navigatesToNegociacoesWithEmAndamentoAndValidatesAdminAttachedPhoto() {
    val repo = MarketplaceRepository()
    val vm = com.example.ui.MarketplaceViewModel(repo)
    repo.switchUser("cli_1")
    vm.setTab(com.example.ui.MainTab.SERVICOS)

    vm.acceptProposal("prop_1")

    assertEquals(com.example.ui.MainTab.NEGOCIACOES, vm.selectedTab.value)
    val updatedSrv = repo.services.value.first { it.id == "srv_1" }
    assertEquals(ServiceStatus.EM_EXECUCAO, updatedSrv.status)
    assertEquals("Em Andamento", updatedSrv.status.label)

    // Admin attached profile photo requirement passes with gallery/file URI
    val missingWithoutPhoto = com.example.data.CpfValidator.providerMissingRequirements(
      name = "João Prestador",
      requireName = true,
      email = "joao@pro.com",
      phone = "(11) 98765-4321",
      requireEmailAndPhone = true,
      city = "São Paulo, SP",
      document = "52998224725",
      profilePhotoUri = null,
      requireCameraProfilePhoto = false,
      requireAttachedProfilePhoto = true,
      rgOrCnhAttachmentUri = "data:image/jpeg;base64,rg",
      criminalRecordAttachmentUri = "data:image/jpeg;base64,crim",
      selfieAttachmentUri = "data:image/jpeg;base64,selfie"
    )
    assertTrue(missingWithoutPhoto.any { it.contains("Foto de perfil anexada", ignoreCase = true) })

    val missingWithPhoto = com.example.data.CpfValidator.providerMissingRequirements(
      name = "João Prestador",
      requireName = true,
      email = "joao@pro.com",
      phone = "(11) 98765-4321",
      requireEmailAndPhone = true,
      city = "São Paulo, SP",
      document = "52998224725",
      profilePhotoUri = "data:image/jpeg;base64,avatar",
      requireCameraProfilePhoto = false,
      requireAttachedProfilePhoto = true,
      rgOrCnhAttachmentUri = "data:image/jpeg;base64,rg",
      criminalRecordAttachmentUri = "data:image/jpeg;base64,crim",
      selfieAttachmentUri = "data:image/jpeg;base64,selfie"
    )
    assertTrue(missingWithPhoto.isEmpty())
  }

  @Test
  fun testAllUsersCanChangeOwnPasswordWithCurrentAndNewPassword() {
    val repo = MarketplaceRepository()
    val client = repo.users.value.first { it.role == UserRole.CLIENTE }
    val provider = repo.users.value.first { it.role == UserRole.PRESTADOR }
    val admin = repo.users.value.first { it.role == UserRole.ADMIN }

    // 1. User without registered password hash gets clear error
    repo.switchUser(client.id)
    val noPwdResult = repo.changeCurrentUserPassword("old123", "new456")
    assertTrue(noPwdResult.isFailure)
    assertTrue(noPwdResult.exceptionOrNull()?.message?.contains("Nenhuma senha atual", ignoreCase = true) == true)

    // Seed initial passwords for all three roles (Cliente, Prestador, Admin)
    listOf(client to "cliOld123", provider to "proOld123", admin to "admOld123").forEach { (user, initialPwd) ->
      repo.recordLocalPasswordHash(user.id, initialPwd)
    }

    // 2. Test each role (Cliente, Prestador, Admin)
    val cases = listOf(
      Triple(client, "cliOld123", "cliNew456"),
      Triple(provider, "proOld123", "proNew456"),
      Triple(admin, "admOld123", "admNew456")
    )
    cases.forEach { (user, oldPwd, newPwd) ->
      repo.switchUser(user.id)

      // Rejects wrong current password
      val wrongRes = repo.changeCurrentUserPassword("wrong999", newPwd)
      assertTrue("Expected failure on wrong current password for ${user.role}", wrongRes.isFailure)
      assertTrue(wrongRes.exceptionOrNull()?.message?.contains("Senha atual incorreta", ignoreCase = true) == true)

      // Rejects short new password
      val shortRes = repo.changeCurrentUserPassword(oldPwd, "12345")
      assertTrue(shortRes.isFailure)

      // Rejects identical new password
      val sameRes = repo.changeCurrentUserPassword(oldPwd, oldPwd)
      assertTrue(sameRes.isFailure)

      // Accepts valid current + new password
      val okRes = repo.changeCurrentUserPassword(oldPwd, newPwd)
      assertTrue("Expected success for ${user.role}: ${okRes.exceptionOrNull()?.message}", okRes.isSuccess)

      // Old password no longer verifies, new password verifies
      org.junit.Assert.assertNull(repo.verifyAdminManagedPasswordByEmail(user.email, oldPwd))
      assertEquals(user.id, repo.verifyAdminManagedPasswordByEmail(user.email, newPwd)?.id)
      assertTrue(repo.verifyCurrentUserLocalPassword(newPwd))
      assertFalse(repo.verifyCurrentUserLocalPassword(oldPwd))
    }
  }

  @Test
  fun testViewModelChangeOwnPasswordStateTransitions() = kotlinx.coroutines.runBlocking {
    @OptIn(ExperimentalCoroutinesApi::class)
    val testDispatcher = UnconfinedTestDispatcher()
    @OptIn(ExperimentalCoroutinesApi::class)
    Dispatchers.setMain(testDispatcher)
    try {
      val repo = MarketplaceRepository()
      val client = repo.users.value.first { it.role == UserRole.CLIENTE }
      repo.switchUser(client.id)
      repo.recordLocalPasswordHash(client.id, "senhaAtual1")

      val vm = com.example.ui.MarketplaceViewModel(repo)
      vm.setShowChangePasswordDialog(true)
      assertTrue(vm.showChangePasswordDialog.value)

      // Mismatch confirmation
      vm.changeOwnPassword("senhaAtual1", "novaSenha2", "outraSenha3")
      val mismatchState = vm.changePasswordState.value
      assertTrue(mismatchState is com.example.ui.ChangePasswordUiState.Error)
      assertTrue((mismatchState as com.example.ui.ChangePasswordUiState.Error).message.contains("não coincidem", ignoreCase = true))

      // Wrong current password
      vm.changeOwnPassword("errada999", "novaSenha2", "novaSenha2")
      val wrongState = vm.changePasswordState.value
      assertTrue(wrongState is com.example.ui.ChangePasswordUiState.Error)
      assertTrue((wrongState as com.example.ui.ChangePasswordUiState.Error).message.contains("Senha atual incorreta", ignoreCase = true))
      assertTrue(vm.showChangePasswordDialog.value)

      // Valid change succeeds, closes dialog, updates hash
      vm.changeOwnPassword("senhaAtual1", "novaSenha2", "novaSenha2")
      val successState = vm.changePasswordState.value
      assertTrue(successState is com.example.ui.ChangePasswordUiState.Success)
      assertFalse(vm.showChangePasswordDialog.value)
      assertTrue(repo.verifyCurrentUserLocalPassword("novaSenha2"))
      assertFalse(repo.verifyCurrentUserLocalPassword("senhaAtual1"))
      assertTrue(vm.snackBarMessage.value?.contains("Senha alterada", ignoreCase = true) == true)
    } finally {
      @OptIn(ExperimentalCoroutinesApi::class)
      Dispatchers.resetMain()
    }
  }

  @Test
  fun testRegisterAndSignInWithEmailAndPhoneAndPassword_endToEnd() = kotlinx.coroutines.runBlocking {
    @OptIn(ExperimentalCoroutinesApi::class)
    val testDispatcher = UnconfinedTestDispatcher()
    @OptIn(ExperimentalCoroutinesApi::class)
    Dispatchers.setMain(testDispatcher)
    try {
      val repo = MarketplaceRepository()
      val vm = com.example.ui.MarketplaceViewModel(repo)

      val draft = com.example.ui.PendingRegistration(
        name = "Fernanda Souza",
        email = "fernanda.souza@teste.com",
        phone = "(11) 98765-4321",
        role = UserRole.CLIENTE,
        city = "São Paulo, SP",
        document = "52998224725"
      )

      // 1. Register with email, phone, and password (works even when Firebase Email/Password provider is unavailable)
      vm.registerWithPassword(draft, countryCode = "+55", password = "senhaInicial1")
      assertTrue("Expected user to be authenticated after registerWithPassword", repo.isAuthenticated.value)
      assertEquals("fernanda.souza@teste.com", repo.getCurrentUser().email)
      org.junit.Assert.assertNull(vm.authErrorMessage.value)

      // 2. Logout and login with Email + Password (with mixed case and spaces)
      vm.logout()
      assertFalse(repo.isAuthenticated.value)

      vm.signInWithEmailAndPassword("  FERNANDA.SOUZA@TESTE.COM ", "senhaErrada99")
      assertFalse(repo.isAuthenticated.value)
      assertTrue(vm.authErrorMessage.value?.contains("incorretos", ignoreCase = true) == true)

      vm.signInWithEmailAndPassword("  FERNANDA.SOUZA@TESTE.COM ", "senhaInicial1")
      assertTrue("Expected login with Email + Password to succeed", repo.isAuthenticated.value)
      assertEquals("fernanda.souza@teste.com", repo.getCurrentUser().email)

      // 3. Logout and login with Phone + Password across multiple input formats
      val phoneFormats = listOf("11987654321", "(11) 98765-4321", "011987654321", "+5511987654321")
      phoneFormats.forEach { formattedInput ->
        vm.logout()
        assertFalse(repo.isAuthenticated.value)
        vm.signInWithPhoneAndPassword(formattedInput, "+55", "senhaInicial1")
        assertTrue("Expected login with phone format '$formattedInput' to succeed", repo.isAuthenticated.value)
        assertEquals("fernanda.souza@teste.com", repo.getCurrentUser().email)
      }

      // 4. Wrong password with phone shows clear error (never Firebase Console error)
      vm.logout()
      vm.signInWithPhoneAndPassword("11987654321", "+55", "errada123")
      assertFalse(repo.isAuthenticated.value)
      assertTrue(vm.authErrorMessage.value?.contains("Telefone ou senha incorretos", ignoreCase = true) == true)

      // 5. Login, change password, logout, verify old fails and new succeeds via email and phone
      vm.signInWithPhoneAndPassword("11987654321", "+55", "senhaInicial1")
      assertTrue(repo.isAuthenticated.value)
      vm.changeOwnPassword("senhaInicial1", "novaSenha777", "novaSenha777")
      vm.logout()

      vm.signInWithEmailAndPassword("fernanda.souza@teste.com", "senhaInicial1")
      assertFalse(repo.isAuthenticated.value)

      vm.signInWithEmailAndPassword("fernanda.souza@teste.com", "novaSenha777")
      assertTrue(repo.isAuthenticated.value)

      vm.logout()
      vm.signInWithPhoneAndPassword("(11) 98765-4321", "+55", "novaSenha777")
      assertTrue(repo.isAuthenticated.value)

      // 6. Duplicate registration with same email is rejected cleanly
      vm.logout()
      vm.registerWithPassword(draft, countryCode = "+55", password = "outraSenha1")
      assertFalse(repo.isAuthenticated.value)
      assertTrue(vm.authErrorMessage.value?.contains("já possui cadastro", ignoreCase = true) == true)
    } finally {
      @OptIn(ExperimentalCoroutinesApi::class)
      Dispatchers.resetMain()
    }
  }

  @Test
  fun testAdminUserPasswords_tracksLifecycleAndStrictlyGatesBehindAdminRole() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val testDispatcher = UnconfinedTestDispatcher()
    @OptIn(ExperimentalCoroutinesApi::class)
    Dispatchers.setMain(testDispatcher)
    try {
      val repo = MarketplaceRepository()
      val vm = com.example.ui.MarketplaceViewModel(repo)

      // 1. Admin sets password for cli_1
      repo.switchUser("admin_1")
      assertEquals(UserRole.ADMIN, vm.currentUser.value.role)
      val setRes = repo.adminSetUserPassword("cli_1", "SenhaAdmin2026")
      assertTrue(setRes.isSuccess)
      assertEquals("SenhaAdmin2026", vm.adminUserPasswords.value["cli_1"])

      // 2. Non-admin user MUST NOT see any saved passwords through ViewModel
      repo.switchUser("cli_1")
      assertEquals(UserRole.CLIENTE, vm.currentUser.value.role)
      assertTrue("Cliente nunca pode enxergar adminUserPasswords", vm.adminUserPasswords.value.isEmpty())

      // 3. When user changes own password, repository updates saved password for admin view
      val changeRes = repo.changeCurrentUserPassword("SenhaAdmin2026", "MinhaNova99")
      assertTrue(changeRes.isSuccess)
      assertTrue(vm.adminUserPasswords.value.isEmpty())

      repo.switchUser("admin_1")
      assertEquals("MinhaNova99", vm.adminUserPasswords.value["cli_1"])

      // 4. recordLocalPasswordHash updates saved password for another user
      repo.recordLocalPasswordHash("pro_1", "Eletricista@10")
      assertEquals("Eletricista@10", vm.adminUserPasswords.value["pro_1"])

      // 5. Deleting a user cleans up their saved password
      val deleteRes = repo.adminDeleteUser("cli_1")
      assertTrue(deleteRes.isSuccess)
      assertFalse(vm.adminUserPasswords.value.containsKey("cli_1"))
    } finally {
      @OptIn(ExperimentalCoroutinesApi::class)
      Dispatchers.resetMain()
    }
  }

  @Test
  fun testRestoreAuthNavigationState_preservesRegisterModeAndGoogleCompletionStep() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val testDispatcher = UnconfinedTestDispatcher()
    @OptIn(ExperimentalCoroutinesApi::class)
    Dispatchers.setMain(testDispatcher)
    try {
      val repo = MarketplaceRepository()
      val vm = com.example.ui.MarketplaceViewModel(repo)

      // Default starts in LOGIN / FORM
      assertEquals(com.example.ui.AuthMode.LOGIN, vm.authMode.value)
      assertEquals(com.example.ui.AuthFlowStep.FORM, vm.authStep.value)

      // Restore REGISTER after camera process death
      vm.restoreAuthNavigationState(
        mode = com.example.ui.AuthMode.REGISTER,
        step = com.example.ui.AuthFlowStep.FORM,
        preferredRole = UserRole.PRESTADOR
      )
      assertEquals(com.example.ui.AuthMode.REGISTER, vm.authMode.value)
      assertEquals(com.example.ui.AuthFlowStep.FORM, vm.authStep.value)
      assertEquals(UserRole.PRESTADOR, vm.pendingPreferredRole.value)

      // Restore GOOGLE_ROLE_COMPLETION with pendingGoogleAccount
      val googleAcc = com.example.data.GoogleAccountInfo(
        uid = "uid_google_camera",
        email = "camera@teste.com",
        displayName = "Usuário Câmera",
        photoUrl = "https://example.com/p.jpg"
      )
      vm.restoreAuthNavigationState(
        mode = com.example.ui.AuthMode.REGISTER,
        step = com.example.ui.AuthFlowStep.GOOGLE_ROLE_COMPLETION,
        pendingGoogleAccount = googleAcc,
        socialProviderLabel = "Google",
        preferredRole = UserRole.CLIENTE,
        pendingLoginPhone = "+5511999998888"
      )
      assertEquals(com.example.ui.AuthFlowStep.GOOGLE_ROLE_COMPLETION, vm.authStep.value)
      assertEquals("uid_google_camera", vm.pendingGoogleAccount.value?.uid)
      assertEquals("+5511999998888", vm.pendingLoginPhone.value)
    } finally {
      @OptIn(ExperimentalCoroutinesApi::class)
      Dispatchers.resetMain()
    }
  }

  @Test
  fun testCloudSyncStatus_initialStateAndUnauthenticatedSyncHandling() {
    val repo = MarketplaceRepository()
    assertEquals(com.example.data.CloudSyncState.OFFLINE_LOCAL, repo.cloudSyncStatus.value.state)
    assertFalse(repo.cloudSyncStatus.value.isFirebaseSignedIn)

    val result = kotlinx.coroutines.runBlocking { repo.syncAllDataWithFirebase() }
    assertTrue("Sem sessão Firebase ativa, syncAllDataWithFirebase deve retornar falha informativa", result.isFailure)
    assertEquals(com.example.data.CloudSyncState.OFFLINE_LOCAL, repo.cloudSyncStatus.value.state)
  }

  @Test
  fun testCrossDeviceLoginDirectoryKeys_normalizePhonesEmailsAndLinkedAuthUids() {
    // 1. All Brazilian phone formats MUST produce identical canonical digits and SHA-256 phone lookup key
    val phoneFormats = listOf(
      "(61) 99290-7464",
      "61992907464",
      "061992907464",
      "+5561992907464",
      "+55 (61) 99290-7464",
      "55 61 99290 7464"
    )
    val canonicalResults = phoneFormats.map { com.example.data.FirebaseAuthHelper.canonicalPhoneDigits(it, "+55") }.distinct()
    assertEquals(listOf("5561992907464"), canonicalResults)

    val phoneKeys = phoneFormats.mapNotNull { com.example.data.FirebaseAuthHelper.phoneLookupKey(it, "+55") }.distinct()
    assertEquals(1, phoneKeys.size)
    assertTrue(
      "Chave de telefone deve seguir exatamente ^phone_[a-f0-9]{64}$",
      Regex("^phone_[a-f0-9]{64}$").matches(phoneKeys.first())
    )

    // 2. Email normalization is case-insensitive and whitespace-safe
    val emailKey1 = com.example.data.FirebaseAuthHelper.emailLookupKey("  Maria.Silva@Email.COM ")
    val emailKey2 = com.example.data.FirebaseAuthHelper.emailLookupKey("maria.silva@email.com")
    assertEquals(emailKey1, emailKey2)
    assertTrue(
      "Chave de e-mail deve seguir exatamente ^email_[a-f0-9]{64}$",
      emailKey1 != null && Regex("^email_[a-f0-9]{64}$").matches(emailKey1)
    )

    // 3. Directory password verifier is deterministic and unique per uid/password
    val v1 = com.example.data.FirebaseAuthHelper.hashDirectoryPassword("uid_1", "Senha123")
    val v2 = com.example.data.FirebaseAuthHelper.hashDirectoryPassword("uid_1", "Senha123")
    val v3 = com.example.data.FirebaseAuthHelper.hashDirectoryPassword("uid_2", "Senha123")
    assertEquals(v1, v2)
    assertFalse(v1 == v3)

    // 4. User.ownsId matches id, firebaseUid, and all linkedAuthUids
    val linkedUser = User(
      id = "cli_orig",
      name = "Maria Silva",
      email = "maria.silva@email.com",
      phone = "(61) 99290-7464",
      role = UserRole.CLIENTE,
      firebaseUid = "fb_uid_orig",
      linkedAuthUids = listOf("fb_uid_alias_1", "fb_uid_alias_2")
    )
    assertTrue(linkedUser.ownsId("cli_orig"))
    assertTrue(linkedUser.ownsId("fb_uid_orig"))
    assertTrue(linkedUser.ownsId("fb_uid_alias_1"))
    assertTrue(linkedUser.ownsId("fb_uid_alias_2"))
    assertFalse(linkedUser.ownsId("fb_uid_other"))

    // 5. Network/reCAPTCHA errors MUST NOT be misclassified as provider-unavailable (prevents local-only ghost accounts)
    assertFalse(
      "Erro de rede não pode acionar fallback local silencioso no cadastro",
      com.example.data.FirebaseAuthHelper.isProviderUnavailableException(
        RuntimeException("A network error (such as timeout, interrupted connection or unreachable host) has occurred.")
      )
    )
    assertFalse(
      "Erro de reCAPTCHA não pode acionar fallback local silencioso no cadastro",
      com.example.data.FirebaseAuthHelper.isProviderUnavailableException(
        RuntimeException("RECAPTCHA_CHECK_FAILED")
      )
    )
    assertTrue(
      "OPERATION_NOT_ALLOWED deve ser identificado como provedor indisponível",
      com.example.data.FirebaseAuthHelper.isProviderUnavailableException(
        RuntimeException("OPERATION_NOT_ALLOWED")
      )
    )
  }

  @Test
  fun testReconcileRemoteOpenServices_removesAcceptedOrderFromOtherProvidersImmediately() {
    val repo = MarketplaceRepository()
    // Log in as provider pro_2 (non-accepted provider for srv_1 and srv_3)
    repo.switchUser("pro_2")
    val pro2 = repo.getCurrentUser()

    val srvOpenA = ServiceRequest(
      id = "srv_live_a",
      clientId = "cli_1",
      clientName = "Carlos Eduardo",
      title = "Troca de tomadas",
      category = "Eletricista",
      description = "Troca de 4 tomadas",
      location = "Pinheiros, SP",
      proposedBudget = 180.0,
      status = ServiceStatus.ABERTO
    )
    val srvOpenB = ServiceRequest(
      id = "srv_live_b",
      clientId = "cli_2",
      clientName = "Mariana Silveira",
      title = "Pintura quarto",
      category = "Pintura",
      description = "Pintura completa",
      location = "Cambuí, Campinas",
      proposedBudget = 420.0,
      status = ServiceStatus.EM_NEGOCIACAO
    )
    // Provider pro_2 also has an assigned job in progress that MUST be preserved
    repo.acceptProposalAndDebitFee("prop_2")
    val assignedToPro2Id = "srv_2"
    assertTrue(repo.services.value.any { it.id == assignedToPro2Id && it.acceptedProviderId == "pro_2" })

    // 1. Live snapshot brings both open opportunities
    repo.reconcileRemoteOpenServices(listOf(srvOpenA, srvOpenB))
    assertTrue(repo.services.value.any { it.id == "srv_live_a" })
    assertTrue(repo.services.value.any { it.id == "srv_live_b" })
    assertTrue("Serviço atribuído ao próprio prestador deve ser preservado", repo.services.value.any { it.id == assignedToPro2Id })

    // 2. Client accepts ANOTHER provider for srv_live_a -> observeOpenServices() drops srv_live_a
    repo.reconcileRemoteOpenServices(listOf(srvOpenB))
    assertFalse(
      "Pedido aceito entre cliente e outro prestador deve sair imediatamente da lista de oportunidades",
      repo.services.value.any { it.id == "srv_live_a" }
    )
    assertTrue(repo.services.value.any { it.id == "srv_live_b" })
    assertTrue(repo.services.value.any { it.id == assignedToPro2Id })

    // 3. Last remaining open order srv_live_b is also accepted by another provider -> snapshot becomes emptyList()
    repo.reconcileRemoteOpenServices(emptyList())
    assertFalse(
      "Quando o último pedido aberto é aceito (snapshot vazio), ele também deve sair automaticamente da lista",
      repo.services.value.any { it.id == "srv_live_b" }
    )
    assertTrue(
      "Serviço em andamento do próprio prestador continua visível mesmo sem novas oportunidades abertas",
      repo.services.value.any { it.id == assignedToPro2Id && it.acceptedProviderId == "pro_2" }
    )

    // 4. Defensive check: an order with acceptedProviderId != null or an ACEITA proposal for another provider is never visible
    val staleNegotiatingWithAccepted = srvOpenA.copy(
      status = ServiceStatus.EM_NEGOCIACAO,
      acceptedProviderId = "pro_1"
    )
    assertFalse(
      MarketplaceRepository.isServiceVisibleForProvider(staleNegotiatingWithAccepted, pro2)
    )
    val acceptedByOtherProposal = listOf(
      com.example.model.Proposal(
        id = "prop_other_acc",
        serviceId = srvOpenA.id,
        providerId = "pro_1",
        providerName = "Roberto Mendes",
        providerRating = 4.95,
        providerCategory = "Eletricista",
        proposedPrice = 180.0,
        estimatedDays = 1,
        description = "Aceito",
        status = ProposalStatus.ACEITA
      )
    )
    assertFalse(
      MarketplaceRepository.isServiceVisibleForProvider(
        service = srvOpenA,
        provider = pro2,
        proposals = acceptedByOtherProposal
      )
    )
  }

  @Test
  fun testRegionalPriceIndexEngine_regionalCategoryDailyAndLocalSampleBlending() {
    val dayOneMillis = 1763640000000L // 2025-11-20 12:00 UTC (09:00 BRT)
    val dayTwoMillis = dayOneMillis + 86_400_000L // 2025-11-21

    // 1. Regional differentiation for the same category on the same day
    val spEletricista = com.example.data.RegionalPriceIndexEngine.estimate(
      category = "Eletricista",
      locationText = "Av. Paulista, 1000 - Bela Vista, São Paulo - SP",
      nowMillis = dayOneMillis
    )
    val piEletricista = com.example.data.RegionalPriceIndexEngine.estimate(
      category = "Eletricista",
      locationText = "Centro, Teresina - PI",
      nowMillis = dayOneMillis
    )
    assertEquals("SP", spEletricista.stateCode)
    assertEquals("Sudeste", spEletricista.macroRegion)
    assertEquals("PI", piEletricista.stateCode)
    assertEquals("Nordeste", piEletricista.macroRegion)
    assertTrue(
      "Média regional de São Paulo (${spEletricista.averagePrice}) deve ser maior que Teresina (${piEletricista.averagePrice})",
      spEletricista.averagePrice > piEletricista.averagePrice
    )
    assertTrue(spEletricista.minPrice < spEletricista.averagePrice)
    assertTrue(spEletricista.maxPrice > spEletricista.averagePrice)

    // 2. Category differentiation in the same region
    val spManicure = com.example.data.RegionalPriceIndexEngine.estimate(
      category = "Manicure",
      locationText = "São Paulo - SP",
      nowMillis = dayOneMillis
    )
    val spReformas = com.example.data.RegionalPriceIndexEngine.estimate(
      category = "Reformas & Alvenaria",
      locationText = "São Paulo - SP",
      nowMillis = dayOneMillis
    )
    assertTrue(spManicure.averagePrice < spEletricista.averagePrice)
    assertTrue(spReformas.averagePrice > spEletricista.averagePrice)

    // 3. Daily rollover updates dateKey, displayDate and benchmark ID automatically
    val spEletricistaNextDay = com.example.data.RegionalPriceIndexEngine.estimate(
      category = "Eletricista",
      locationText = "São Paulo - SP",
      nowMillis = dayTwoMillis
    )
    assertTrue(spEletricista.dateKey != spEletricistaNextDay.dateKey)
    assertTrue(spEletricista.displayDate != spEletricistaNextDay.displayDate)
    assertTrue(spEletricista.id != spEletricistaNextDay.id)
    assertTrue(spEletricistaNextDay.id.matches(Regex("^[a-zA-Z0-9_\\-]+$")))

    // 4. Real regional orders shift the measured average and report local sample count
    val localCuritibaOrders = listOf(
      ServiceRequest(
        id = "srv_cwb_1",
        clientId = "cli_1",
        clientName = "Carlos",
        title = "Troca de fiação",
        category = "Eletricista",
        description = "Quadro elétrico",
        location = "Batel, Curitiba - PR",
        proposedBudget = 360.0,
        finalAgreedPrice = 370.0,
        status = ServiceStatus.CONCLUIDO,
        urgency = "Média"
      ),
      ServiceRequest(
        id = "srv_cwb_2",
        clientId = "cli_2",
        clientName = "Ana",
        title = "Instalação de luminárias",
        category = "Eletricista",
        description = "Sala e cozinha",
        location = "Água Verde, Curitiba - PR",
        proposedBudget = 350.0,
        finalAgreedPrice = 360.0,
        status = ServiceStatus.CONCLUIDO,
        urgency = "Média"
      )
    )
    val cwbWithoutHistory = com.example.data.RegionalPriceIndexEngine.estimate(
      category = "Eletricista",
      locationText = "Curitiba - PR",
      historicalServices = emptyList(),
      nowMillis = dayOneMillis
    )
    val cwbWithHistory = com.example.data.RegionalPriceIndexEngine.estimate(
      category = "Eletricista",
      locationText = "Curitiba - PR",
      historicalServices = localCuritibaOrders,
      nowMillis = dayOneMillis
    )
    assertEquals(0, cwbWithoutHistory.localSamplesCount)
    assertEquals(2, cwbWithHistory.localSamplesCount)
    assertTrue(
      "Pedidos reais mais altos em Curitiba devem elevar a média medida",
      cwbWithHistory.averagePrice > cwbWithoutHistory.averagePrice
    )

    // 5. Repository resolves fallback from currentUser.city when locationText is blank
    val repo = MarketplaceRepository()
    repo.switchUser("cli_1")
    val repoEstimate = repo.getRegionalPriceEstimate(category = "Eletricista", locationText = "")
    assertTrue(repoEstimate.averagePrice >= 40.0)
    assertEquals(com.example.data.RegionalPriceIndexEngine.todayDateKey(), repoEstimate.dateKey)
  }
}

