package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

sealed class FirebaseCredentialAuthResult {
  data class Success(
    val account: GoogleAccountInfo,
    val providerLabel: String
  ) : FirebaseCredentialAuthResult()

  object Cancelled : FirebaseCredentialAuthResult()
  data class Error(
    val message: String,
    val isProviderUnavailable: Boolean = false
  ) : FirebaseCredentialAuthResult()
}

/**
 * Gerenciador central de autenticação via Firebase Authentication para:
 * - E-mail e senha
 * - Telefone e senha
 * - Recuperação de senha
 */
object FirebaseAuthHelper {
  private const val TAG = "FirebaseAuthHelper"
  private const val MIN_PASSWORD_LENGTH = 6
  private fun defaultAuth(): FirebaseAuth? = try {
    Firebase.auth
  } catch (_: Throwable) {
    try {
      FirebaseAuth.getInstance()
    } catch (_: Throwable) {
      null
    }
  }

  fun isValidEmail(email: String): Boolean = ContactValidator.isValidEmail(email)

  fun normalizeEmail(email: String): String = email.trim().lowercase()

  fun canonicalPhoneDigits(
    phone: String,
    countryCode: String = FirebasePhoneAuthManager.DEFAULT_COUNTRY_CODE
  ): String {
    val rawDigits = phone.filter { it.isDigit() }
    if (rawDigits.length < 8) return rawDigits
    val e164 = FirebasePhoneAuthManager(authProvider = { null }).formatToE164(phone, countryCode)
    val e164Digits = e164.filter { it.isDigit() }
    if (countryCode.filter { it.isDigit() } == "55") {
      // Strip trunk leading zero if user typed 011... or +55011...
      if (e164Digits.startsWith("550") && e164Digits.length in 13..14) {
        return "55" + e164Digits.drop(3)
      }
    }
    return e164Digits.ifBlank { rawDigits }
  }

  private fun sha256Hex(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }

  fun emailLookupKey(email: String): String? {
    val normalized = normalizeEmail(email)
    if (normalized.isBlank() || !isValidEmail(normalized)) return null
    return "email_${sha256Hex("resolveai_lookup_v1:email:$normalized")}"
  }

  fun phoneLookupKey(
    phone: String,
    countryCode: String = FirebasePhoneAuthManager.DEFAULT_COUNTRY_CODE
  ): String? {
    val digits = canonicalPhoneDigits(phone, countryCode)
    if (digits.length < 10) return null
    return "phone_${sha256Hex("resolveai_lookup_v1:phone:$digits")}"
  }

  fun hashDirectoryPassword(uid: String, rawPassword: String): String {
    return sha256Hex("resolveai_dir_v2:${uid.trim()}:$rawPassword")
  }

  fun phoneToSyntheticAuthEmail(
    phone: String,
    countryCode: String = FirebasePhoneAuthManager.DEFAULT_COUNTRY_CODE
  ): String {
    val digits = canonicalPhoneDigits(phone, countryCode)
    return "tel_${digits}@phone.resolveai.app"
  }

  fun buildAdminAliasAuthEmail(userDocId: String): String {
    val cleanId = userDocId.filter { it.isLetterOrDigit() }.take(20).ifBlank { "usr" }
    return "cred_${cleanId}_${System.currentTimeMillis()}@auth.resolveai.app"
  }

  suspend fun signInWithEmailAndPassword(
    email: String,
    password: String,
    extraCandidateEmails: List<String> = emptyList(),
    authProvider: () -> FirebaseAuth? = { defaultAuth() }
  ): FirebaseCredentialAuthResult {
    val cleanEmail = normalizeEmail(email)
    if (!isValidEmail(cleanEmail)) {
      return FirebaseCredentialAuthResult.Error("Informe um endereço de e-mail válido.")
    }
    if (password.length < MIN_PASSWORD_LENGTH) {
      return FirebaseCredentialAuthResult.Error("A senha deve ter pelo menos $MIN_PASSWORD_LENGTH caracteres.")
    }
    val candidates = (listOf(cleanEmail) + extraCandidateEmails.map { normalizeEmail(it) })
      .filter { it.isNotBlank() && isValidEmail(it) }
      .distinct()
    return signInWithEmailCandidates(
      candidateEmails = candidates,
      password = password,
      providerLabel = "E-mail e senha",
      contextLabel = "e-mail e senha",
      authProvider = authProvider
    )
  }

  suspend fun signInWithEmailCandidates(
    candidateEmails: List<String>,
    password: String,
    providerLabel: String,
    contextLabel: String,
    authProvider: () -> FirebaseAuth? = { defaultAuth() }
  ): FirebaseCredentialAuthResult {
    val candidates = candidateEmails
      .map { normalizeEmail(it) }
      .filter { it.isNotBlank() && isValidEmail(it) }
      .distinct()
    if (candidates.isEmpty()) {
      return FirebaseCredentialAuthResult.Error("Informe um e-mail ou telefone válido.")
    }
    if (password.length < MIN_PASSWORD_LENGTH) {
      return FirebaseCredentialAuthResult.Error("A senha deve ter pelo menos $MIN_PASSWORD_LENGTH caracteres.")
    }
    val auth = authProvider()
      ?: return FirebaseCredentialAuthResult.Error(
        message = "Firebase Authentication indisponível no momento.",
        isProviderUnavailable = true
      )

    var lastException: Exception? = null
    for (candidateEmail in candidates) {
      try {
        auth.setLanguageCode("pt-BR")
        val result = auth.signInWithEmailAndPassword(candidateEmail, password).await()
        return extractAccountInfo(result, fallbackEmail = candidateEmail, providerLabel = providerLabel)
      } catch (e: Exception) {
        Log.w(TAG, "Tentativa de login ($contextLabel) falhou para candidato: ${e.message}")
        lastException = e
      }
    }

    return FirebaseCredentialAuthResult.Error(
      message = lastException?.let { mapFirebaseAuthException(it, contextLabel = contextLabel) }
        ?: "Credenciais inválidas. Confira os dados e a senha informados.",
      isProviderUnavailable = lastException?.let { isProviderUnavailableException(it) } ?: false
    )
  }

  suspend fun registerWithEmailAndPassword(
    name: String,
    email: String,
    password: String,
    authProvider: () -> FirebaseAuth? = { defaultAuth() }
  ): FirebaseCredentialAuthResult {
    val cleanEmail = normalizeEmail(email)
    val cleanName = name.trim()
    if (cleanName.length < 2) {
      return FirebaseCredentialAuthResult.Error("Informe seu nome completo para concluir o cadastro.")
    }
    if (!isValidEmail(cleanEmail)) {
      return FirebaseCredentialAuthResult.Error("Informe um endereço de e-mail válido.")
    }
    if (password.length < MIN_PASSWORD_LENGTH) {
      return FirebaseCredentialAuthResult.Error("A senha deve ter no mínimo $MIN_PASSWORD_LENGTH caracteres.")
    }
    val auth = authProvider()
      ?: return FirebaseCredentialAuthResult.Error(
        message = "Firebase Authentication indisponível no momento.",
        isProviderUnavailable = true
      )

    return try {
      auth.setLanguageCode("pt-BR")
      val result = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
      updateUserDisplayNameQuietly(result, cleanName)
      extractAccountInfo(
        result = result,
        fallbackEmail = cleanEmail,
        fallbackName = cleanName,
        providerLabel = "E-mail e senha"
      )
    } catch (e: Exception) {
      Log.w(TAG, "Erro cadastro e-mail/senha: ${e.message}")
      FirebaseCredentialAuthResult.Error(
        message = mapFirebaseAuthException(e, contextLabel = "e-mail e senha"),
        isProviderUnavailable = isProviderUnavailableException(e)
      )
    }
  }

  suspend fun signInWithPhoneAndPassword(
    phone: String,
    countryCode: String = FirebasePhoneAuthManager.DEFAULT_COUNTRY_CODE,
    password: String,
    knownEmailForPhone: String? = null,
    extraCandidateEmails: List<String> = emptyList(),
    authProvider: () -> FirebaseAuth? = { defaultAuth() }
  ): FirebaseCredentialAuthResult {
    val phoneDigits = phone.filter { it.isDigit() }
    if (phoneDigits.length < 8) {
      return FirebaseCredentialAuthResult.Error("Informe um número de telefone válido com DDD.")
    }
    if (password.length < MIN_PASSWORD_LENGTH) {
      return FirebaseCredentialAuthResult.Error("A senha deve ter pelo menos $MIN_PASSWORD_LENGTH caracteres.")
    }

    val syntheticEmail = phoneToSyntheticAuthEmail(phone, countryCode)
    val candidates = (
      listOfNotNull(knownEmailForPhone?.let { normalizeEmail(it) }) +
        extraCandidateEmails.map { normalizeEmail(it) } +
        listOf(syntheticEmail)
      )
      .filter { it.isNotBlank() && isValidEmail(it) }
      .distinct()

    return signInWithEmailCandidates(
      candidateEmails = candidates,
      password = password,
      providerLabel = "Telefone e senha",
      contextLabel = "telefone e senha",
      authProvider = authProvider
    )
  }

  suspend fun registerWithPhoneAndPassword(
    name: String,
    phone: String,
    countryCode: String = FirebasePhoneAuthManager.DEFAULT_COUNTRY_CODE,
    optionalEmail: String = "",
    password: String,
    authProvider: () -> FirebaseAuth? = { defaultAuth() }
  ): FirebaseCredentialAuthResult {
    val cleanName = name.trim()
    val phoneError = ContactValidator.validatePhone(phone, countryCode)
    if (cleanName.length < 2) {
      return FirebaseCredentialAuthResult.Error("Informe seu nome completo para criar a conta.")
    }
    if (phoneError != null) {
      return FirebaseCredentialAuthResult.Error(phoneError)
    }
    if (password.length < MIN_PASSWORD_LENGTH) {
      return FirebaseCredentialAuthResult.Error("A senha deve ter no mínimo $MIN_PASSWORD_LENGTH caracteres.")
    }
    val cleanEmail = optionalEmail.trim()
    val authEmail = if (cleanEmail.isNotBlank() && isValidEmail(cleanEmail)) {
      cleanEmail
    } else {
      phoneToSyntheticAuthEmail(phone, countryCode)
    }

    val auth = authProvider()
      ?: return FirebaseCredentialAuthResult.Error(
        message = "Firebase Authentication indisponível no momento.",
        isProviderUnavailable = true
      )

    return try {
      auth.setLanguageCode("pt-BR")
      val result = auth.createUserWithEmailAndPassword(authEmail, password).await()
      updateUserDisplayNameQuietly(result, cleanName)
      extractAccountInfo(
        result = result,
        fallbackEmail = authEmail,
        fallbackName = cleanName,
        providerLabel = "Telefone e senha"
      )
    } catch (e: Exception) {
      Log.w(TAG, "Erro cadastro telefone/senha: ${e.message}")
      FirebaseCredentialAuthResult.Error(
        message = mapFirebaseAuthException(e, contextLabel = "telefone e senha"),
        isProviderUnavailable = isProviderUnavailableException(e)
      )
    }
  }

  suspend fun sendPasswordReset(
    email: String,
    authProvider: () -> FirebaseAuth? = { defaultAuth() }
  ): Result<String> {
    val cleanEmail = email.trim()
    if (!isValidEmail(cleanEmail)) {
      return Result.failure(IllegalArgumentException("Informe um e-mail válido para receber o link de redefinição."))
    }
    val auth = authProvider()
      ?: return Result.failure(IllegalStateException("Firebase Authentication indisponível no momento."))

    return try {
      auth.setLanguageCode("pt-BR")
      auth.sendPasswordResetEmail(cleanEmail).await()
      Result.success("E-mail de redefinição de senha enviado para $cleanEmail.")
    } catch (e: Exception) {
      Result.failure(IllegalStateException(mapFirebaseAuthException(e, contextLabel = "recuperação de senha")))
    }
  }

  fun hashUserPassword(userId: String, rawPassword: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest("resolveai_v1:$userId:$rawPassword".toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }

  suspend fun updateCurrentUserPassword(
    newPassword: String,
    authProvider: () -> FirebaseAuth? = { defaultAuth() }
  ): Result<Unit> {
    if (newPassword.length < MIN_PASSWORD_LENGTH) {
      return Result.failure(IllegalArgumentException("A senha deve ter no mínimo $MIN_PASSWORD_LENGTH caracteres."))
    }
    val user = authProvider()?.currentUser
      ?: return Result.failure(IllegalStateException("Nenhuma sessão Firebase ativa para atualizar."))
    return try {
      user.updatePassword(newPassword).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(IllegalStateException(mapFirebaseAuthException(e, contextLabel = "alteração de senha")))
    }
  }

  /**
   * Vincula credencial de E-mail e Senha à conta Firebase Auth atual (ex: usuário entrou via Google
   * e definiu uma senha), permitindo entrar por E-mail/Telefone + Senha em qualquer aparelho.
   */
  suspend fun linkEmailPasswordCredential(
    email: String,
    newPassword: String,
    authProvider: () -> FirebaseAuth? = { defaultAuth() }
  ): Result<Boolean> {
    val cleanEmail = normalizeEmail(email)
    if (!isValidEmail(cleanEmail) || newPassword.length < MIN_PASSWORD_LENGTH) {
      return Result.success(false)
    }
    val user = authProvider()?.currentUser ?: return Result.success(false)
    val hasPasswordProvider = user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
    if (hasPasswordProvider) {
      return updateCurrentUserPassword(newPassword, authProvider).map { true }
    }
    return try {
      val credential = EmailAuthProvider.getCredential(cleanEmail, newPassword)
      user.linkWithCredential(credential).await()
      Result.success(true)
    } catch (e: Exception) {
      Log.w(TAG, "Não foi possível vincular credencial de senha ao usuário Google: ${e.message}")
      Result.failure(e)
    }
  }

  data class SecondaryProvisionedAccount(
    val uid: String,
    val authEmail: String
  )

  /**
   * Provisiona uma credencial real no Firebase Authentication usando uma instância secundária
   * de FirebaseApp para NÃO desconectar a sessão atual do Administrador.
   * Tenta primeiro o [primaryEmail]; se já existir no Firebase Auth, cria uma credencial auxiliar
   * com [fallbackAliasEmail] para que o usuário consiga logar com a nova senha em qualquer aparelho.
   */
  suspend fun provisionSecondaryAuthAccount(
    context: Context?,
    displayName: String,
    primaryEmail: String,
    fallbackAliasEmail: String,
    password: String
  ): SecondaryProvisionedAccount? {
    if (context == null || password.length < MIN_PASSWORD_LENGTH) return null
    val secondaryAuth = try {
      val appContext = context.applicationContext
      val defaultApp = FirebaseApp.getInstance()
      val secondaryName = "resolveai_secondary_auth"
      val secondaryApp = FirebaseApp.getApps(appContext).firstOrNull { it.name == secondaryName }
        ?: FirebaseApp.initializeApp(appContext, defaultApp.options, secondaryName)
      FirebaseAuth.getInstance(secondaryApp)
    } catch (t: Throwable) {
      Log.w(TAG, "Não foi possível inicializar FirebaseApp secundário: ${t.message}")
      return null
    }

    val cleanPrimary = normalizeEmail(primaryEmail)
    val cleanAlias = normalizeEmail(fallbackAliasEmail)
    val candidateEmails = listOf(cleanPrimary, cleanAlias)
      .filter { it.isNotBlank() && isValidEmail(it) }
      .distinct()

    for (candidate in candidateEmails) {
      try {
        secondaryAuth.setLanguageCode("pt-BR")
        val res = secondaryAuth.createUserWithEmailAndPassword(candidate, password).await()
        updateUserDisplayNameQuietly(res, displayName.trim())
        val createdUid = res.user?.uid
        try {
          secondaryAuth.signOut()
        } catch (_: Throwable) {
        }
        if (!createdUid.isNullOrBlank()) {
          return SecondaryProvisionedAccount(uid = createdUid, authEmail = candidate)
        }
      } catch (e: FirebaseAuthUserCollisionException) {
        Log.i(TAG, "E-mail $candidate já existe no Firebase Auth; tentando credencial auxiliar.")
      } catch (e: Exception) {
        val code = (e as? FirebaseAuthException)?.errorCode.orEmpty()
        if (code == "ERROR_EMAIL_ALREADY_IN_USE") {
          Log.i(TAG, "E-mail $candidate já em uso no Firebase Auth; tentando credencial auxiliar.")
        } else {
          Log.w(TAG, "Falha ao provisionar conta secundária ($candidate): ${e.message}")
          break
        }
      }
    }
    return null
  }

  /**
   * Reautentica o usuário atual no Firebase Auth com a senha atual e aplica a nova senha.
   * Retorna `Result.success(true)` quando atualizado no Firebase Auth, ou `Result.success(false)`
   * quando a sessão atual não utiliza provedor de senha no Firebase (permitindo validação local).
   */
  suspend fun reauthenticateAndChangePassword(
    currentPassword: String,
    newPassword: String,
    expectedUserIds: Set<String> = emptySet(),
    authProvider: () -> FirebaseAuth? = { defaultAuth() }
  ): Result<Boolean> {
    if (currentPassword.isBlank()) {
      return Result.failure(IllegalArgumentException("Informe sua senha atual."))
    }
    if (newPassword.length < MIN_PASSWORD_LENGTH) {
      return Result.failure(IllegalArgumentException("A nova senha deve ter no mínimo $MIN_PASSWORD_LENGTH caracteres."))
    }
    if (newPassword == currentPassword) {
      return Result.failure(IllegalArgumentException("A nova senha deve ser diferente da senha atual."))
    }
    val user = authProvider()?.currentUser ?: return Result.success(false)
    if (expectedUserIds.isNotEmpty() && user.uid !in expectedUserIds) {
      return Result.success(false)
    }
    val email = user.email?.trim().orEmpty()
    val hasPasswordProvider = user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
    if (email.isBlank() || !hasPasswordProvider) {
      return Result.success(false)
    }
    return try {
      val credential = EmailAuthProvider.getCredential(email, currentPassword)
      user.reauthenticate(credential).await()
      user.updatePassword(newPassword).await()
      Result.success(true)
    } catch (e: FirebaseAuthInvalidCredentialsException) {
      Log.w(TAG, "Senha atual incorreta ao reautenticar: ${e.message}")
      Result.failure(IllegalArgumentException("Senha atual incorreta. Verifique e tente novamente."))
    } catch (e: FirebaseAuthException) {
      val code = e.errorCode
      if (code == "ERROR_WRONG_PASSWORD" || code == "ERROR_INVALID_CREDENTIAL") {
        Result.failure(IllegalArgumentException("Senha atual incorreta. Verifique e tente novamente."))
      } else {
        Result.failure(IllegalStateException(mapFirebaseAuthException(e, contextLabel = "alteração de senha")))
      }
    } catch (e: Exception) {
      Log.w(TAG, "Erro ao alterar senha no Firebase Auth: ${e.message}")
      Result.failure(IllegalStateException(mapFirebaseAuthException(e, contextLabel = "alteração de senha")))
    }
  }

  private suspend fun updateUserDisplayNameQuietly(result: AuthResult, displayName: String) {
    if (displayName.isBlank()) return
    try {
      val profileUpdate = UserProfileChangeRequest.Builder()
        .setDisplayName(displayName)
        .build()
      result.user?.updateProfile(profileUpdate)?.await()
    } catch (_: Throwable) {
    }
  }

  private fun extractAccountInfo(
    result: AuthResult,
    fallbackEmail: String,
    fallbackName: String = "",
    providerLabel: String
  ): FirebaseCredentialAuthResult {
    val fbUser = result.user
      ?: return FirebaseCredentialAuthResult.Error("Não foi possível obter os dados do usuário no Firebase.")
    val resolvedEmail = fbUser.email?.takeIf { it.isNotBlank() } ?: fallbackEmail
    val resolvedName = fbUser.displayName?.takeIf { it.isNotBlank() }
      ?: fallbackName.takeIf { it.isNotBlank() }
      ?: resolvedEmail.substringBefore("@")
    return FirebaseCredentialAuthResult.Success(
      account = GoogleAccountInfo(
        uid = fbUser.uid,
        email = resolvedEmail,
        displayName = resolvedName,
        photoUrl = fbUser.photoUrl?.toString()
      ),
      providerLabel = providerLabel
    )
  }

  internal fun isProviderUnavailableException(e: Throwable): Boolean {
    val code = (e as? FirebaseAuthException)?.errorCode.orEmpty()
    val rawMsg = e.message.orEmpty()
    return code == "ERROR_OPERATION_NOT_ALLOWED" ||
      code == "ERROR_CONFIGURATION_NOT_FOUND" ||
      code == "ERROR_ADMIN_ONLY_OPERATION" ||
      rawMsg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ||
      rawMsg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
      rawMsg.contains("ADMIN_ONLY_OPERATION", ignoreCase = true)
  }

  internal fun mapFirebaseAuthException(e: Throwable, contextLabel: String): String {
    val code = (e as? FirebaseAuthException)?.errorCode.orEmpty()
    return when {
      code == "ERROR_OPERATION_NOT_ALLOWED" ||
        code == "ERROR_CONFIGURATION_NOT_FOUND" ||
        e.message.orEmpty().contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) -> {
        "Provedor de $contextLabel ainda não habilitado no Firebase Console (Authentication > Sign-in method)."
      }
      code == "ERROR_INVALID_CREDENTIAL" || code == "ERROR_WRONG_PASSWORD" -> {
        "Credenciais inválidas. Confira os dados e a senha informados."
      }
      code == "ERROR_USER_NOT_FOUND" || e is FirebaseAuthInvalidUserException -> {
        "Nenhuma conta encontrada com esses dados. Crie uma conta na aba 'Criar Conta'."
      }
      code == "ERROR_EMAIL_ALREADY_IN_USE" || e is FirebaseAuthUserCollisionException -> {
        "Este e-mail ou telefone já possui cadastro no aplicativo. Tente entrar."
      }
      code == "ERROR_WEAK_PASSWORD" || e is FirebaseAuthWeakPasswordException -> {
        "Senha muito fraca. Escolha uma senha com pelo menos 6 caracteres."
      }
      code == "ERROR_INVALID_EMAIL" -> {
        "Endereço de e-mail inválido."
      }
      e is FirebaseAuthInvalidCredentialsException -> {
        "Dados ou senha inválidos. Verifique e tente novamente."
      }
      e is FirebaseTooManyRequestsException -> {
        "Muitas tentativas seguidas. Aguarde alguns instantes antes de tentar novamente."
      }
      e is FirebaseNetworkException -> {
        "Sem conexão com a internet para autenticar no Firebase."
      }
      else -> {
        e.localizedMessage?.takeIf { it.isNotBlank() }
          ?: "Não foi possível concluir a autenticação com $contextLabel."
      }
    }
  }
}
