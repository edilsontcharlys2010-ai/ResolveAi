package com.example.data

import android.app.Activity
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

enum class PhoneAuthErrorType {
  PROVIDER_DISABLED,
  INVALID_PHONE,
  INVALID_CODE,
  QUOTA_OR_BILLING,
  APP_VERIFICATION_FAILED,
  TOO_MANY_ATTEMPTS,
  COOLDOWN_ACTIVE,
  NETWORK_ERROR,
  UNKNOWN
}

data class CountryDialOption(
  val flag: String,
  val countryName: String,
  val dialCode: String
)

sealed class PhoneAuthState {
  object Idle : PhoneAuthState()
  object SendingCode : PhoneAuthState()
  data class CodeSent(val verificationId: String, val formattedPhone: String) : PhoneAuthState()
  object Verifying : PhoneAuthState()
  data class Verified(val user: FirebaseUser?, val rawPhone: String) : PhoneAuthState()
  data class Error(
    val message: String,
    val type: PhoneAuthErrorType = PhoneAuthErrorType.UNKNOWN
  ) : PhoneAuthState()
}

/**
 * Gerenciador oficial de Autenticação por Telefone (SMS OTP) via Firebase Authentication.
 *
 * Recursos implementados:
 * 1. Normalização obrigatória para E.164 (ex: +5511981112233) com suporte a DDI configurável
 * 2. Idioma pt-BR aplicado às mensagens SMS do Firebase Auth
 * 3. Token de reenvio (ForceResendingToken) e rate limiting com cooldown de 60 segundos por número
 * 4. Auto-verificação instantânea (onVerificationCompleted) e entrada manual OTP de 6 dígitos
 * 5. Limite contra força bruta (máximo 3 tentativas) e diagnóstico claro de provedor/Play Integrity/App Check
 */
class FirebasePhoneAuthManager(
  private val authProvider: () -> FirebaseAuth? = {
    try {
      Firebase.auth
    } catch (_: Throwable) {
      try {
        FirebaseAuth.getInstance()
      } catch (_: Throwable) {
        null
      }
    }
  }
) {
  val auth: FirebaseAuth? get() = authProvider()

  private val _state = MutableStateFlow<PhoneAuthState>(PhoneAuthState.Idle)
  val state: StateFlow<PhoneAuthState> = _state.asStateFlow()

  private var currentVerificationId: String? = null
  private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
  private var lastSentPhone: String? = null
  private var failedAttempts = 0
  private var lastSendTimestamp = 0L

  companion object {
    private const val TAG = "FirebasePhoneAuth"
    const val RESEND_COOLDOWN_MS = 60_000L
    const val MAX_VERIFICATION_ATTEMPTS = 3
    const val DEFAULT_COUNTRY_CODE = "+55"

    val SUPPORTED_COUNTRIES = listOf(
      CountryDialOption("🇧🇷", "Brasil", "+55"),
      CountryDialOption("🇵🇹", "Portugal", "+351"),
      CountryDialOption("🇺🇸", "EUA / Canadá", "+1"),
      CountryDialOption("🇦🇷", "Argentina", "+54"),
      CountryDialOption("🇬🇧", "Reino Unido", "+44")
    )
  }

  /**
   * Verifica se o SDK do FirebaseAuth está inicializado no contexto atual.
   */
  fun isFirebaseConfigured(): Boolean {
    return try {
      auth != null && FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext).isNotEmpty()
    } catch (_: Throwable) {
      auth != null
    }
  }

  /**
   * Normaliza um telefone para o padrão internacional E.164 (ex: +5511999998888).
   */
  fun formatToE164(phone: String, countryCode: String = DEFAULT_COUNTRY_CODE): String {
    val trimmed = phone.trim()
    val cleanDial = countryCode.trim().let { if (it.startsWith("+")) it else "+$it" }
    val dialDigits = cleanDial.filter { it.isDigit() }
    val digits = trimmed.filter { it.isDigit() }

    if (digits.isEmpty()) return cleanDial

    return when {
      trimmed.startsWith("+") && digits.length >= 8 -> "+$digits"
      dialDigits == "55" && digits.startsWith("55") && digits.length >= 12 -> "+$digits"
      dialDigits.isNotEmpty() && digits.startsWith(dialDigits) && digits.length >= dialDigits.length + 8 -> "+$digits"
      else -> "$cleanDial$digits"
    }
  }

  /**
   * Retorna quantos segundos faltam no cooldown de reenvio para o número informado.
   */
  fun getCooldownRemainingSeconds(formattedPhone: String): Int {
    if (lastSentPhone != formattedPhone || currentVerificationId == null) return 0
    val elapsed = System.currentTimeMillis() - lastSendTimestamp
    if (elapsed >= RESEND_COOLDOWN_MS) return 0
    return ((RESEND_COOLDOWN_MS - elapsed) / 1000L).toInt().coerceAtLeast(1)
  }

  /**
   * Inicia o fluxo de envio de OTP por SMS pelo Firebase Authentication.
   */
  fun sendVerificationCode(
    activity: Activity?,
    phone: String,
    countryCode: String = DEFAULT_COUNTRY_CODE,
    isResend: Boolean = false,
    onSuccess: (verificationId: String, formattedPhone: String) -> Unit,
    onAutoVerified: ((FirebaseUser, String) -> Unit)? = null,
    onError: (String, PhoneAuthErrorType) -> Unit
  ) {
    val formattedPhone = formatToE164(phone, countryCode)
    val digitsOnly = formattedPhone.filter { it.isDigit() }
    if (digitsOnly.length < 10 || digitsOnly.length > 15) {
      val msg = "Número inválido para padrão internacional E.164. Informe DDI, DDD e celular."
      _state.value = PhoneAuthState.Error(msg, PhoneAuthErrorType.INVALID_PHONE)
      onError(msg, PhoneAuthErrorType.INVALID_PHONE)
      return
    }

    val now = System.currentTimeMillis()
    val isSamePhone = lastSentPhone == formattedPhone
    if (!isSamePhone) {
      resendToken = null
      currentVerificationId = null
    }

    // Rate limiting por número
    if (isSamePhone && now - lastSendTimestamp < RESEND_COOLDOWN_MS && currentVerificationId != null) {
      val remainingSeconds = ((RESEND_COOLDOWN_MS - (now - lastSendTimestamp)) / 1000).coerceAtLeast(1)
      val msg = "Por favor, aguarde $remainingSeconds segundos antes de solicitar um novo código."
      _state.value = PhoneAuthState.Error(msg, PhoneAuthErrorType.COOLDOWN_ACTIVE)
      onError(msg, PhoneAuthErrorType.COOLDOWN_ACTIVE)
      return
    }

    val currentAuth = auth
    if (currentAuth == null) {
      val msg = "Firebase Authentication indisponível neste ambiente."
      _state.value = PhoneAuthState.Error(msg, PhoneAuthErrorType.APP_VERIFICATION_FAILED)
      onError(msg, PhoneAuthErrorType.APP_VERIFICATION_FAILED)
      return
    }

    try {
      currentAuth.setLanguageCode("pt-BR")
    } catch (_: Throwable) {
    }

    _state.value = PhoneAuthState.SendingCode
    failedAttempts = 0
    lastSendTimestamp = now
    lastSentPhone = formattedPhone

    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
      override fun onVerificationCompleted(credential: PhoneAuthCredential) {
        Log.d(TAG, "onVerificationCompleted: Verificação automática concluída para $formattedPhone")
        signInWithCredential(
          credential = credential,
          onSuccess = { user ->
            _state.value = PhoneAuthState.Verified(user, formattedPhone)
            onAutoVerified?.invoke(user, formattedPhone)
          },
          onError = { errMsg, errType ->
            _state.value = PhoneAuthState.Error(errMsg, errType)
            onError(errMsg, errType)
          }
        )
      }

      override fun onVerificationFailed(e: FirebaseException) {
        Log.e(TAG, "onVerificationFailed: ${e.message}", e)
        lastSendTimestamp = 0L // permite nova tentativa imediata após corrigir configuração/número
        val (errorMessage, errorType) = classifyFirebaseException(e)
        _state.value = PhoneAuthState.Error(errorMessage, errorType)
        onError(errorMessage, errorType)
      }

      override fun onCodeSent(
        verificationId: String,
        token: PhoneAuthProvider.ForceResendingToken
      ) {
        Log.d(TAG, "onCodeSent: SMS OTP despachado para $formattedPhone")
        currentVerificationId = verificationId
        resendToken = token
        _state.value = PhoneAuthState.CodeSent(verificationId, formattedPhone)
        onSuccess(verificationId, formattedPhone)
      }

      override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
        Log.d(TAG, "onCodeAutoRetrievalTimeOut: Entrada manual de OTP ativa.")
        currentVerificationId = verificationId
      }
    }

    val optionsBuilder = PhoneAuthOptions.newBuilder(currentAuth)
      .setPhoneNumber(formattedPhone)
      .setTimeout(60L, TimeUnit.SECONDS)
      .setCallbacks(callbacks)

    if (activity != null) {
      optionsBuilder.setActivity(activity)
    }

    if (isResend) {
      resendToken?.let { optionsBuilder.setForceResendingToken(it) }
    }

    try {
      PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    } catch (e: Exception) {
      Log.e(TAG, "Erro ao disparar verifyPhoneNumber: ${e.message}", e)
      lastSendTimestamp = 0L
      val err = "Erro ao conectar ao Firebase Phone Auth: ${e.localizedMessage ?: "verifique a Activity"}"
      _state.value = PhoneAuthState.Error(err, PhoneAuthErrorType.UNKNOWN)
      onError(err, PhoneAuthErrorType.UNKNOWN)
    }
  }

  /**
   * Sobrecarga simplificada mantendo compatibilidade com chamadas legadas de 2 parâmetros no callback de erro.
   */
  fun sendVerificationCode(
    activity: Activity?,
    phone: String,
    onSuccess: (verificationId: String, formattedPhone: String) -> Unit,
    onError: (String) -> Unit
  ) {
    sendVerificationCode(
      activity = activity,
      phone = phone,
      countryCode = DEFAULT_COUNTRY_CODE,
      isResend = false,
      onSuccess = onSuccess,
      onAutoVerified = null,
      onError = { msg, _ -> onError(msg) }
    )
  }

  /**
   * Valida o código OTP de 6 dígitos digitado pelo usuário com PhoneAuthCredential.
   */
  fun verifySmsCode(
    verificationId: String,
    code: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
  ) {
    val cleanCode = code.filter { it.isDigit() }
    if (cleanCode.length != 6) {
      val msg = "Digite os 6 dígitos do código OTP recebido por SMS."
      _state.value = PhoneAuthState.Error(msg, PhoneAuthErrorType.INVALID_CODE)
      onError(msg)
      return
    }

    if (failedAttempts >= MAX_VERIFICATION_ATTEMPTS) {
      val msg = "Limite de $MAX_VERIFICATION_ATTEMPTS tentativas excedido. Solicite um novo código SMS."
      _state.value = PhoneAuthState.Error(msg, PhoneAuthErrorType.TOO_MANY_ATTEMPTS)
      onError(msg)
      return
    }

    val targetVerificationId = verificationId.ifBlank { currentVerificationId.orEmpty() }
    if (targetVerificationId.isBlank()) {
      val msg = "Sessão de verificação expirada. Solicite o envio de um novo SMS."
      _state.value = PhoneAuthState.Error(msg, PhoneAuthErrorType.UNKNOWN)
      onError(msg)
      return
    }

    _state.value = PhoneAuthState.Verifying

    try {
      val credential = PhoneAuthProvider.getCredential(targetVerificationId, cleanCode)
      signInWithCredential(
        credential = credential,
        onSuccess = { user ->
          failedAttempts = 0
          _state.value = PhoneAuthState.Verified(user, user.phoneNumber ?: lastSentPhone.orEmpty())
          onSuccess()
        },
        onError = { errMsg, errType ->
          failedAttempts++
          val remaining = (MAX_VERIFICATION_ATTEMPTS - failedAttempts).coerceAtLeast(0)
          val msg = when {
            errType == PhoneAuthErrorType.TOO_MANY_ATTEMPTS || remaining == 0 ->
              "Código incorreto. Limite de tentativas atingido. Reenvie o SMS."
            errType == PhoneAuthErrorType.INVALID_CODE ->
              "Código OTP incorreto ou expirado. $remaining tentativa(s) restante(s)."
            else -> errMsg
          }
          _state.value = PhoneAuthState.Error(msg, errType)
          onError(msg)
        }
      )
    } catch (e: Exception) {
      failedAttempts++
      val msg = "Erro na validação do código OTP: ${e.localizedMessage ?: "tente novamente"}"
      _state.value = PhoneAuthState.Error(msg, PhoneAuthErrorType.UNKNOWN)
      onError(msg)
    }
  }

  private fun signInWithCredential(
    credential: PhoneAuthCredential,
    onSuccess: (FirebaseUser) -> Unit,
    onError: (String, PhoneAuthErrorType) -> Unit
  ) {
    val currentAuth = auth ?: run {
      onError("FirebaseAuth indisponível.", PhoneAuthErrorType.UNKNOWN)
      return
    }
    currentAuth.signInWithCredential(credential)
      .addOnCompleteListener { task ->
        if (task.isSuccessful) {
          val user = currentAuth.currentUser
          if (user != null) {
            onSuccess(user)
          } else {
            onError("Credencial validada, mas usuário Firebase não encontrado.", PhoneAuthErrorType.UNKNOWN)
          }
        } else {
          val ex = task.exception
          val (msg, type) = if (ex is FirebaseException) {
            classifyFirebaseException(ex)
          } else {
            (ex?.localizedMessage ?: "Falha ao autenticar com o código SMS.") to PhoneAuthErrorType.INVALID_CODE
          }
          onError(msg, type)
        }
      }
  }

  private fun classifyFirebaseException(e: FirebaseException): Pair<String, PhoneAuthErrorType> {
    val raw = e.message.orEmpty()
    val code = (e as? FirebaseAuthException)?.errorCode.orEmpty()

    return when {
      code == "ERROR_OPERATION_NOT_ALLOWED" ||
        raw.contains("not allowed", ignoreCase = true) ||
        raw.contains("disabled", ignoreCase = true) ||
        raw.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ->
        "O provedor 'Telefone (Phone)' não está ativado no Firebase Console (Authentication > Sign-in method) para este projeto." to
          PhoneAuthErrorType.PROVIDER_DISABLED

      e is FirebaseAuthInvalidCredentialsException && (
        code == "ERROR_INVALID_VERIFICATION_CODE" ||
          code == "ERROR_SESSION_EXPIRED" ||
          raw.contains("verification code", ignoreCase = true) ||
          raw.contains("session", ignoreCase = true)
        ) ->
        "Código OTP inválido ou expirado. Confira os 6 dígitos ou solicite reenvio." to
          PhoneAuthErrorType.INVALID_CODE

      e is FirebaseAuthInvalidCredentialsException ||
        code == "ERROR_INVALID_PHONE_NUMBER" ||
        raw.contains("INVALID_PHONE_NUMBER", ignoreCase = true) ||
        raw.contains("invalid format", ignoreCase = true) ->
        "Número de telefone inválido. Verifique o DDI (+55), DDD e dígitos do celular." to
          PhoneAuthErrorType.INVALID_PHONE

      e is FirebaseTooManyRequestsException ||
        code == "ERROR_QUOTA_EXCEEDED" ||
        raw.contains("quota", ignoreCase = true) ||
        raw.contains("billing", ignoreCase = true) ||
        raw.contains("too-many-requests", ignoreCase = true) ->
        "Cota de SMS ou faturamento do Firebase temporariamente indisponível para este projeto." to
          PhoneAuthErrorType.QUOTA_OR_BILLING

      e is FirebaseNetworkException ||
        raw.contains("network", ignoreCase = true) ->
        "Sem conexão de rede para contatar o Firebase Authentication." to
          PhoneAuthErrorType.NETWORK_ERROR

      code == "ERROR_APP_NOT_AUTHORIZED" ||
        code == "ERROR_MISSING_CLIENT_IDENTIFIER" ||
        code == "ERROR_WEB_CONTEXT_CANCELED" ||
        raw.contains("app-not-authorized", ignoreCase = true) ||
        raw.contains("missing-client-identifier", ignoreCase = true) ||
        raw.contains("web-context", ignoreCase = true) ||
        raw.contains("cancel", ignoreCase = true) ||
        raw.contains("browser", ignoreCase = true) ||
        raw.contains("recaptcha", ignoreCase = true) ||
        raw.contains("integrity", ignoreCase = true) ||
        raw.contains("SHA", ignoreCase = true) ->
        "O Firebase abriu o Chrome para verificação anti-robô (reCAPTCHA) porque o Play Integrity não validou este emulador/aparelho. No Chrome, toque em 'Use without an account' (Usar sem conta) para receber o SMS sem adicionar conta Google, e confirme SHA-1 e SHA-256 no Firebase Console." to
          PhoneAuthErrorType.APP_VERIFICATION_FAILED

      else ->
        (e.localizedMessage ?: "Falha na autenticação por telefone no Firebase.") to PhoneAuthErrorType.UNKNOWN
    }
  }

  fun reset() {
    _state.value = PhoneAuthState.Idle
    failedAttempts = 0
  }
}
