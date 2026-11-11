package com.example

import com.example.data.InsufficientBalanceException
import com.example.data.MarketplaceRepository
import com.example.model.ContactFilter
import com.example.model.ProposalStatus
import com.example.model.ServiceStatus
import com.example.model.UserRole
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

    // Verify service status updated to ACEITO
    val updatedService = repo.services.value.first { it.id == proposal.serviceId }
    assertEquals(ServiceStatus.ACEITO, updatedService.status)
    assertEquals(300.00, updatedService.finalAgreedPrice ?: 0.0, 0.01)
    assertEquals(30.00, updatedService.platformFee ?: 0.0, 0.01)
  }

  @Test
  fun testProposalAcceptance_blocksWhenInsufficientBalance() {
    val repo = MarketplaceRepository()

    // prop_2 is for Ana Clara (balance R$ 35.00, negotiated price R$ 550.00 -> 10% fee is R$ 55.00)
    val result = repo.acceptProposalAndDebitFee("prop_2")

    assertTrue("Deve falhar por saldo insuficiente", result.isFailure)
    val exception = result.exceptionOrNull() as? InsufficientBalanceException
    assertTrue(exception != null)
    assertEquals(55.00, exception?.requiredFee ?: 0.0, 0.01)
    assertEquals(35.00, exception?.currentBalance ?: 0.0, 0.01)
    assertEquals(20.00, exception?.missingAmount ?: 0.0, 0.01)

    // Now recharge wallet by R$ 50.00
    repo.rechargeWallet("pro_2", 50.00)
    val rechargedBalance = repo.users.value.first { it.id == "pro_2" }.walletBalance
    assertEquals(85.00, rechargedBalance, 0.01)

    // Try accepting again -> now succeeds!
    val secondAttempt = repo.acceptProposalAndDebitFee("prop_2")
    assertTrue("Deve ter sucesso após recarga", secondAttempt.isSuccess)
    val finalBalance = repo.users.value.first { it.id == "pro_2" }.walletBalance
    assertEquals(30.00, finalBalance, 0.01) // 85 - 55 = 30
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

    repo.approveProvider("pro_pending")
    val approvedUser = repo.users.value.first { it.id == "pro_pending" }
    assertEquals(com.example.model.ProviderVerificationStatus.APROVADO, approvedUser.verificationStatus)
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
}
