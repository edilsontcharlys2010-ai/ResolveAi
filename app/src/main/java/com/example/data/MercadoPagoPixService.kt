package com.example.data

import com.example.BuildConfig
import com.example.model.User
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class MercadoPagoIdentification(
  val type: String,
  val number: String
)

data class MercadoPagoPayer(
  val email: String,
  @param:Json(name = "first_name") val firstName: String? = null,
  @param:Json(name = "last_name") val lastName: String? = null,
  val identification: MercadoPagoIdentification? = null
)

data class MercadoPagoPixPaymentRequest(
  @param:Json(name = "transaction_amount") val transactionAmount: Double,
  val description: String,
  @param:Json(name = "payment_method_id") val paymentMethodId: String = "pix",
  @param:Json(name = "external_reference") val externalReference: String,
  @param:Json(name = "date_of_expiration") val dateOfExpiration: String,
  val payer: MercadoPagoPayer,
  @param:Json(name = "notification_url") val notificationUrl: String? = null
)

data class MercadoPagoCancelPaymentRequest(
  val status: String = "cancelled"
)

data class MercadoPagoTransactionData(
  @param:Json(name = "qr_code") val qrCode: String? = null,
  @param:Json(name = "qr_code_base64") val qrCodeBase64: String? = null,
  @param:Json(name = "ticket_url") val ticketUrl: String? = null
)

data class MercadoPagoPointOfInteraction(
  @param:Json(name = "transaction_data") val transactionData: MercadoPagoTransactionData? = null
)

data class MercadoPagoTransactionDetails(
  @param:Json(name = "net_received_amount") val netReceivedAmount: Double? = null,
  @param:Json(name = "total_paid_amount") val totalPaidAmount: Double? = null
)

data class MercadoPagoPaymentResponse(
  val id: Long = 0L,
  val status: String = "",
  @param:Json(name = "status_detail") val statusDetail: String? = null,
  @param:Json(name = "transaction_amount") val transactionAmount: Double? = null,
  @param:Json(name = "external_reference") val externalReference: String? = null,
  @param:Json(name = "payment_method_id") val paymentMethodId: String? = null,
  @param:Json(name = "date_created") val dateCreated: String? = null,
  @param:Json(name = "date_approved") val dateApproved: String? = null,
  @param:Json(name = "date_of_expiration") val dateOfExpiration: String? = null,
  @param:Json(name = "point_of_interaction") val pointOfInteraction: MercadoPagoPointOfInteraction? = null,
  @param:Json(name = "transaction_details") val transactionDetails: MercadoPagoTransactionDetails? = null
)

data class MercadoPagoErrorCause(
  val code: Any? = null,
  val description: String? = null
)

data class MercadoPagoErrorResponse(
  val message: String? = null,
  val error: String? = null,
  val status: Int? = null,
  val cause: List<MercadoPagoErrorCause>? = null
)

data class MercadoPagoPixCreationResult(
  val paymentId: Long,
  val txId: String,
  val status: String,
  val statusDetail: String?,
  val brCode: String,
  val qrCodeBase64: String?,
  val ticketUrl: String?,
  val expiresAtMillis: Long
)

data class MercadoPagoPixStatusResult(
  val paymentId: Long,
  val status: String,
  val statusDetail: String?,
  val isApproved: Boolean,
  val isRejectedOrCancelled: Boolean,
  val transactionAmount: Double?
)

interface MercadoPagoApi {
  @Headers("Accept: application/json")
  @POST("v1/payments")
  suspend fun createPayment(
    @Header("Authorization") authorization: String,
    @Header("X-Idempotency-Key") idempotencyKey: String,
    @Body request: MercadoPagoPixPaymentRequest
  ): Response<MercadoPagoPaymentResponse>

  @Headers("Accept: application/json")
  @GET("v1/payments/{id}")
  suspend fun getPayment(
    @Header("Authorization") authorization: String,
    @Path("id") paymentId: Long
  ): Response<MercadoPagoPaymentResponse>

  @Headers("Accept: application/json")
  @PUT("v1/payments/{id}")
  suspend fun cancelPayment(
    @Header("Authorization") authorization: String,
    @Path("id") paymentId: Long,
    @Body request: MercadoPagoCancelPaymentRequest = MercadoPagoCancelPaymentRequest()
  ): Response<MercadoPagoPaymentResponse>
}

object MercadoPagoPixService {
  private const val BASE_URL = "https://api.mercadopago.com/"
  const val PLACEHOLDER_ACCESS_TOKEN = "MY_MERCADO_PAGO_ACCESS_TOKEN"

  // Mercado Pago /v1/payments exige expiração entre 30 minutos e 30 dias.
  // Usamos 35 minutos por padrão para evitar rejeição por diferença de relógio.
  const val MERCADO_PAGO_MIN_EXPIRATION_MS = 30 * 60_000L
  const val MERCADO_PAGO_DEFAULT_EXPIRATION_MS = 35 * 60_000L
  const val MERCADO_PAGO_MAX_EXPIRATION_MS = 30L * 24 * 60 * 60_000L

  internal val moshi: Moshi by lazy {
    Moshi.Builder()
      .addLast(KotlinJsonAdapterFactory())
      .build()
  }

  private val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .addInterceptor { chain ->
        val req = chain.request().newBuilder()
          .header("User-Agent", "ResolveAi-Android/1.0")
          .build()
        chain.proceed(req)
      }
      .connectTimeout(12, TimeUnit.SECONDS)
      .readTimeout(15, TimeUnit.SECONDS)
      .writeTimeout(12, TimeUnit.SECONDS)
      .build()
  }

  fun buildApi(baseUrl: String = BASE_URL): MercadoPagoApi =
    Retrofit.Builder()
      .baseUrl(baseUrl)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(MercadoPagoApi::class.java)

  private val defaultApi: MercadoPagoApi by lazy { buildApi(BASE_URL) }

  fun normalizeAccessToken(rawToken: String?): String {
    var clean = (rawToken ?: "").trim()
    if ((clean.startsWith("\"") && clean.endsWith("\"")) ||
      (clean.startsWith("'") && clean.endsWith("'"))
    ) {
      clean = clean.substring(1, clean.length - 1).trim()
    }
    if (clean.startsWith("Bearer ", ignoreCase = true)) {
      clean = clean.substring(7).trim()
    }
    return clean
  }

  fun isConfigured(tokenOverride: String? = null): Boolean {
    val token = normalizeAccessToken(tokenOverride ?: BuildConfig.MERCADO_PAGO_ACCESS_TOKEN)
    return token.isNotEmpty() &&
      token != PLACEHOLDER_ACCESS_TOKEN &&
      !token.startsWith("MY_")
  }

  fun isSandboxToken(tokenOverride: String? = null): Boolean {
    val token = normalizeAccessToken(tokenOverride ?: BuildConfig.MERCADO_PAGO_ACCESS_TOKEN)
    return token.startsWith("TEST-")
  }

  fun buildExpirationIso(expiresAtMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'-03:00'", Locale.US)
    formatter.timeZone = TimeZone.getTimeZone("GMT-03:00")
    return formatter.format(Date(expiresAtMillis))
  }

  fun buildPayer(user: User): MercadoPagoPayer {
    val cleanEmail = user.email.trim().lowercase(Locale.US)
      .takeIf { FirebaseAuthHelper.isValidEmail(it) }
      ?: "pagador.${user.id.filter { it.isLetterOrDigit() }.ifBlank { "resolveai" }}@resolveai.com.br"

    val parts = user.name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val firstName = parts.firstOrNull()?.take(40) ?: "Prestador"
    val lastName = parts.drop(1).joinToString(" ").take(60).ifBlank { "ResolveAi" }

    val docDigits = user.document.filter { it.isDigit() }
    val identification = when {
      docDigits.length == 11 && CpfValidator.isValid(docDigits) ->
        MercadoPagoIdentification(type = "CPF", number = docDigits)
      docDigits.length == 14 ->
        MercadoPagoIdentification(type = "CNPJ", number = docDigits)
      else -> null
    }

    return MercadoPagoPayer(
      email = cleanEmail,
      firstName = firstName,
      lastName = lastName,
      identification = identification
    )
  }

  suspend fun createPixPayment(
    amount: Double,
    user: User,
    txId: String,
    accessToken: String = BuildConfig.MERCADO_PAGO_ACCESS_TOKEN,
    api: MercadoPagoApi = defaultApi,
    expirationDurationMs: Long = MERCADO_PAGO_DEFAULT_EXPIRATION_MS
  ): Result<MercadoPagoPixCreationResult> = withContext(Dispatchers.IO) {
    val token = normalizeAccessToken(accessToken)
    if (!isConfigured(token)) {
      return@withContext Result.failure(
        IllegalStateException("MERCADO_PAGO_ACCESS_TOKEN não configurado no painel Secrets do AI Studio.")
      )
    }

    if (amount.isNaN() || amount <= 0.0) {
      return@withContext Result.failure(
        IllegalArgumentException("Informe um valor de pagamento Pix maior que zero.")
      )
    }

    val safeAmount = Math.round(amount * 100.0) / 100.0
    val clampedDurationMs = expirationDurationMs.coerceIn(
      MERCADO_PAGO_MIN_EXPIRATION_MS,
      MERCADO_PAGO_MAX_EXPIRATION_MS
    )
    val expiresAtMillis = System.currentTimeMillis() + clampedDurationMs
    val request = MercadoPagoPixPaymentRequest(
      transactionAmount = safeAmount,
      description = "Recarga Carteira ResolveAi ($txId)",
      paymentMethodId = "pix",
      externalReference = txId,
      dateOfExpiration = buildExpirationIso(expiresAtMillis),
      payer = buildPayer(user)
    )

    try {
      val response = api.createPayment(
        authorization = "Bearer $token",
        idempotencyKey = txId,
        request = request
      )
      if (!response.isSuccessful) {
        return@withContext Result.failure(
          IllegalStateException(parseHttpError(response.code(), response.errorBody()?.string()))
        )
      }

      val body = response.body()
        ?: return@withContext Result.failure(IllegalStateException("Resposta vazia do Mercado Pago."))
      val txData = body.pointOfInteraction?.transactionData
      val qrCode = txData?.qrCode?.trim().orEmpty()
      if (qrCode.isBlank()) {
        return@withContext Result.failure(
          IllegalStateException("Mercado Pago não retornou o código Pix Copia e Cola. Verifique se a chave Pix está cadastrada na sua conta Mercado Pago.")
        )
      }

      Result.success(
        MercadoPagoPixCreationResult(
          paymentId = body.id,
          txId = body.externalReference?.takeIf { it.isNotBlank() } ?: txId,
          status = body.status.ifBlank { "pending" },
          statusDetail = body.statusDetail,
          brCode = qrCode,
          qrCodeBase64 = txData?.qrCodeBase64,
          ticketUrl = txData?.ticketUrl,
          expiresAtMillis = expiresAtMillis
        )
      )
    } catch (e: Exception) {
      Result.failure(
        IllegalStateException("Falha ao conectar ao Mercado Pago: ${e.localizedMessage ?: "verifique sua conexão."}")
      )
    }
  }

  suspend fun checkPixPaymentStatus(
    paymentId: Long,
    accessToken: String = BuildConfig.MERCADO_PAGO_ACCESS_TOKEN,
    api: MercadoPagoApi = defaultApi
  ): Result<MercadoPagoPixStatusResult> = withContext(Dispatchers.IO) {
    val token = normalizeAccessToken(accessToken)
    if (!isConfigured(token)) {
      return@withContext Result.failure(
        IllegalStateException("MERCADO_PAGO_ACCESS_TOKEN não configurado.")
      )
    }
    try {
      val response = api.getPayment(
        authorization = "Bearer $token",
        paymentId = paymentId
      )
      if (!response.isSuccessful) {
        return@withContext Result.failure(
          IllegalStateException(parseHttpError(response.code(), response.errorBody()?.string()))
        )
      }
      val body = response.body()
        ?: return@withContext Result.failure(IllegalStateException("Resposta vazia do Mercado Pago."))
      val normalizedStatus = body.status.trim().lowercase(Locale.US)
      Result.success(
        MercadoPagoPixStatusResult(
          paymentId = body.id,
          status = normalizedStatus,
          statusDetail = body.statusDetail,
          isApproved = normalizedStatus == "approved",
          isRejectedOrCancelled = normalizedStatus in setOf("rejected", "cancelled", "refunded", "charged_back"),
          transactionAmount = body.transactionAmount
        )
      )
    } catch (e: Exception) {
      Result.failure(
        IllegalStateException("Erro ao consultar Mercado Pago: ${e.localizedMessage ?: "erro de rede"}")
      )
    }
  }

  suspend fun cancelPixPayment(
    paymentId: Long,
    accessToken: String = BuildConfig.MERCADO_PAGO_ACCESS_TOKEN,
    api: MercadoPagoApi = defaultApi
  ): Result<MercadoPagoPixStatusResult> = withContext(Dispatchers.IO) {
    val token = normalizeAccessToken(accessToken)
    if (!isConfigured(token)) {
      return@withContext Result.failure(
        IllegalStateException("MERCADO_PAGO_ACCESS_TOKEN não configurado.")
      )
    }
    try {
      val response = api.cancelPayment(
        authorization = "Bearer $token",
        paymentId = paymentId
      )
      if (!response.isSuccessful) {
        return@withContext Result.failure(
          IllegalStateException(parseHttpError(response.code(), response.errorBody()?.string()))
        )
      }
      val body = response.body()
        ?: return@withContext Result.failure(IllegalStateException("Resposta vazia do Mercado Pago."))
      val normalizedStatus = body.status.trim().lowercase(Locale.US)
      Result.success(
        MercadoPagoPixStatusResult(
          paymentId = body.id,
          status = normalizedStatus,
          statusDetail = body.statusDetail,
          isApproved = normalizedStatus == "approved",
          isRejectedOrCancelled = normalizedStatus in setOf("rejected", "cancelled", "refunded", "charged_back"),
          transactionAmount = body.transactionAmount
        )
      )
    } catch (e: Exception) {
      Result.failure(
        IllegalStateException("Erro ao cancelar cobrança Mercado Pago: ${e.localizedMessage ?: "erro de rede"}")
      )
    }
  }

  internal fun parseHttpError(code: Int, rawErrorBody: String?): String {
    val parsed = rawErrorBody?.let {
      runCatching { moshi.adapter(MercadoPagoErrorResponse::class.java).fromJson(it) }.getOrNull()
    }
    val causeCode = parsed?.cause?.firstOrNull()?.code?.toString()?.takeIf { it.isNotBlank() }
    val causeDesc = parsed?.cause?.firstOrNull()?.description?.takeIf { it.isNotBlank() }
    val msg = parsed?.message?.takeIf { it.isNotBlank() }
    val err = parsed?.error?.takeIf { it.isNotBlank() }
    val apiDetail = listOfNotNull(causeDesc, msg, err, causeCode).firstOrNull()
    val detailSuffix = if (!apiDetail.isNullOrBlank()) " ($apiDetail)" else ""

    if (code == 401) {
      return "Mercado Pago recusou a autenticação (HTTP 401)$detailSuffix. Confira se MERCADO_PAGO_ACCESS_TOKEN nos Secrets do AI Studio é o Access Token ativo (não a Public Key) e sem prefixo 'Bearer '."
    }
    if (code == 403) {
      val isPolicyBlock = rawErrorBody?.contains("PA_UNAUTHORIZED", ignoreCase = true) == true ||
        causeCode?.contains("PA_UNAUTHORIZED", ignoreCase = true) == true
      return if (isPolicyBlock) {
        "Mercado Pago bloqueou a cobrança por política da conta (HTTP 403)$detailSuffix. Ative as Credenciais de Produção (Checkout Transparente / Pix) no painel Mercado Pago Developers ou gere um novo Access Token nos Secrets."
      } else {
        "Mercado Pago negou permissão (HTTP 403)$detailSuffix. Verifique se a chave Pix e as Credenciais de Produção do Checkout Transparente estão ativas na conta Mercado Pago."
      }
    }
    return when {
      causeDesc != null -> "Mercado Pago ($code): $causeDesc"
      msg != null -> "Mercado Pago ($code): $msg"
      else -> "Erro ao comunicar com Mercado Pago (HTTP $code)."
    }
  }
}
