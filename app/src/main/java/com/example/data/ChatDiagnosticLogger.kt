package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ChatDiagnosticSeverity {
  INFO,
  WARNING,
  ERROR
}

enum class ChatDiagnosticCategory(val label: String) {
  LISTENER_ATTACHED("Listener Conectado"),
  LISTENER_SNAPSHOT("Snapshot Recebido"),
  LISTENER_PERMISSION_DENIED("Permissão Negada (Listener)"),
  LISTENER_NETWORK_TIMEOUT("Timeout/Rede (Listener)"),
  LISTENER_ERROR("Erro no Listener"),
  SEND_ATTEMPT("Tentativa de Envio"),
  SEND_SUCCESS("Mensagem Entregue"),
  SEND_PERMISSION_DENIED("Permissão Negada (Envio)"),
  SEND_NETWORK_TIMEOUT("Timeout/Rede (Envio)"),
  SEND_AUTH_MISSING("Sessão Auth Ausente"),
  SEND_RECOVERY("Recuperação do Pedido"),
  SEND_ERROR("Erro no Envio")
}

enum class ChatFailureKind {
  PERMISSION_DENIED,
  NETWORK_TIMEOUT,
  UNAUTHENTICATED,
  OTHER
}

data class ChatFailureClassification(
  val kind: ChatFailureKind,
  val firestoreCode: String,
  val message: String
)

data class ChatSendOutcome(
  val delivered: Boolean,
  val failureKind: ChatFailureKind? = null,
  val firestoreCode: String? = null,
  val errorMessage: String? = null,
  val durationMs: Long = 0L
)

data class ChatDiagnosticLogEntry(
  val id: String = "diag_${UUID.randomUUID().toString().replace("-", "").take(10)}",
  val timestampMillis: Long = System.currentTimeMillis(),
  val serviceId: String,
  val category: ChatDiagnosticCategory,
  val severity: ChatDiagnosticSeverity,
  val firestoreCode: String? = null,
  val summary: String,
  val detailsJson: String
)

/**
 * Centralized diagnostic logger for the Firestore private chat listener and message delivery flow.
 * Captures structured Logcat diagnostics (tag: FirestoreChatDiag) and an in-memory ring buffer
 * so permission errors (PERMISSION_DENIED), network/timeout triggers (UNAVAILABLE, DEADLINE_EXCEEDED,
 * client write timeout, server snapshot sync timeout), and recovery steps can be inspected live.
 */
object ChatDiagnosticLogger {
  private const val TAG = "FirestoreChatDiag"
  private const val MAX_ENTRIES = 120

  const val LISTENER_SERVER_SYNC_TIMEOUT_MS = 8_000L
  const val SEND_WRITE_TIMEOUT_MS = 15_000L

  private val _logs = MutableStateFlow<List<ChatDiagnosticLogEntry>>(emptyList())
  val logs: StateFlow<List<ChatDiagnosticLogEntry>> = _logs.asStateFlow()

  fun classifyFirestoreFailure(throwable: Throwable): ChatFailureClassification {
    val causes = generateSequence(throwable) { it.cause }.toList()
    val firestoreEx = causes.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
    if (firestoreEx != null) {
      val code = firestoreEx.code
      val kind = when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> ChatFailureKind.PERMISSION_DENIED
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> ChatFailureKind.NETWORK_TIMEOUT
        FirebaseFirestoreException.Code.UNAUTHENTICATED -> ChatFailureKind.UNAUTHENTICATED
        else -> ChatFailureKind.OTHER
      }
      return ChatFailureClassification(
        kind = kind,
        firestoreCode = code.name,
        message = firestoreEx.message ?: firestoreEx.toString()
      )
    }

    val timeoutEx = causes.firstOrNull {
      it is TimeoutCancellationException ||
        it is SocketTimeoutException ||
        it is UnknownHostException
    }
    if (timeoutEx != null) {
      val code = when (timeoutEx) {
        is TimeoutCancellationException -> "CLIENT_TIMEOUT"
        is SocketTimeoutException -> "SOCKET_TIMEOUT"
        is UnknownHostException -> "NETWORK_UNREACHABLE"
        else -> "NETWORK_TIMEOUT"
      }
      return ChatFailureClassification(
        kind = ChatFailureKind.NETWORK_TIMEOUT,
        firestoreCode = code,
        message = timeoutEx.message ?: timeoutEx::class.java.simpleName
      )
    }

    val root = causes.firstOrNull { it !is CancellationException } ?: throwable
    return ChatFailureClassification(
      kind = ChatFailureKind.OTHER,
      firestoreCode = root::class.java.simpleName,
      message = root.message ?: root.toString()
    )
  }

  fun record(
    serviceId: String,
    category: ChatDiagnosticCategory,
    severity: ChatDiagnosticSeverity,
    summary: String,
    firestoreCode: String? = null,
    details: Map<String, Any?> = emptyMap()
  ): ChatDiagnosticLogEntry {
    val authUid = try {
      FirebaseAuth.getInstance().currentUser?.uid
    } catch (_: Throwable) {
      null
    }

    val json = JSONObject().apply {
      put("serviceId", serviceId)
      put("category", category.name)
      put("severity", severity.name)
      if (!firestoreCode.isNullOrBlank()) put("firestoreCode", firestoreCode)
      put("authUid", authUid ?: JSONObject.NULL)
      details.forEach { (k, v) ->
        put(k, v ?: JSONObject.NULL)
      }
    }

    val entry = ChatDiagnosticLogEntry(
      serviceId = serviceId,
      category = category,
      severity = severity,
      firestoreCode = firestoreCode,
      summary = summary,
      detailsJson = json.toString()
    )

    _logs.update { current ->
      (listOf(entry) + current).take(MAX_ENTRIES)
    }

    val logLine = "[${category.name}] service=$serviceId code=${firestoreCode ?: "OK"} | $summary | ${entry.detailsJson}"
    try {
      when (severity) {
        ChatDiagnosticSeverity.INFO -> Log.i(TAG, logLine)
        ChatDiagnosticSeverity.WARNING -> Log.w(TAG, logLine)
        ChatDiagnosticSeverity.ERROR -> Log.e(TAG, logLine)
      }
    } catch (_: Throwable) {
      // Safe no-op in pure JVM unit tests without android.util.Log stubs
    }

    return entry
  }

  fun clearLogs() {
    _logs.value = emptyList()
  }

  fun exportReport(serviceId: String? = null): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    val filtered = if (serviceId.isNullOrBlank()) {
      _logs.value
    } else {
      _logs.value.filter { it.serviceId == serviceId }
    }
    if (filtered.isEmpty()) {
      return "ResolveAi Firestore Chat Diagnostics: nenhum evento registrado."
    }
    return buildString {
      appendLine("=== ResolveAi Firestore Chat Diagnostics ===")
      if (!serviceId.isNullOrBlank()) {
        appendLine("Service ID: $serviceId")
      }
      appendLine("Eventos: ${filtered.size}")
      appendLine()
      filtered.forEach { entry ->
        val ts = formatter.format(Date(entry.timestampMillis))
        appendLine("[$ts] [${entry.severity.name}] [${entry.category.name}] code=${entry.firestoreCode ?: "NONE"}")
        appendLine("  Resumo: ${entry.summary}")
        appendLine("  JSON: ${entry.detailsJson}")
      }
    }.trim()
  }
}
