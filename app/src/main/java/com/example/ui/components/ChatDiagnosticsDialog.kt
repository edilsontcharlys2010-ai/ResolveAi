package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ChatDiagnosticCategory
import com.example.data.ChatDiagnosticLogEntry
import com.example.data.ChatDiagnosticSeverity
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ChatDiagFilter(val label: String, val tag: String) {
  ALL("Todos", "diag_filter_all"),
  PERMISSION("Permissão", "diag_filter_permission"),
  TIMEOUT("Timeout/Rede", "diag_filter_timeout"),
  EVENTS("Listener/Envio", "diag_filter_events")
}

@Composable
fun ChatDiagnosticsDialog(
  serviceId: String,
  serviceTitle: String,
  logs: List<ChatDiagnosticLogEntry>,
  onClearLogs: () -> Unit,
  onExportReport: (String?) -> String,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var selectedFilter by remember { mutableStateOf(ChatDiagFilter.ALL) }
  var showAllServices by remember { mutableStateOf(false) }
  var copiedFeedback by remember { mutableStateOf(false) }

  val scopedLogs = remember(logs, serviceId, showAllServices) {
    if (showAllServices) logs else logs.filter { it.serviceId == serviceId }
  }

  val permissionCount = remember(scopedLogs) {
    scopedLogs.count {
      it.category == ChatDiagnosticCategory.SEND_PERMISSION_DENIED ||
        it.category == ChatDiagnosticCategory.LISTENER_PERMISSION_DENIED ||
        it.category == ChatDiagnosticCategory.SEND_AUTH_MISSING
    }
  }
  val timeoutCount = remember(scopedLogs) {
    scopedLogs.count {
      it.category == ChatDiagnosticCategory.SEND_NETWORK_TIMEOUT ||
        it.category == ChatDiagnosticCategory.LISTENER_NETWORK_TIMEOUT
    }
  }

  val filteredLogs = remember(scopedLogs, selectedFilter) {
    when (selectedFilter) {
      ChatDiagFilter.ALL -> scopedLogs
      ChatDiagFilter.PERMISSION -> scopedLogs.filter {
        it.category == ChatDiagnosticCategory.SEND_PERMISSION_DENIED ||
          it.category == ChatDiagnosticCategory.LISTENER_PERMISSION_DENIED ||
          it.category == ChatDiagnosticCategory.SEND_AUTH_MISSING
      }
      ChatDiagFilter.TIMEOUT -> scopedLogs.filter {
        it.category == ChatDiagnosticCategory.SEND_NETWORK_TIMEOUT ||
          it.category == ChatDiagnosticCategory.LISTENER_NETWORK_TIMEOUT
      }
      ChatDiagFilter.EVENTS -> scopedLogs.filter {
        it.category == ChatDiagnosticCategory.LISTENER_ATTACHED ||
          it.category == ChatDiagnosticCategory.LISTENER_SNAPSHOT ||
          it.category == ChatDiagnosticCategory.SEND_ATTEMPT ||
          it.category == ChatDiagnosticCategory.SEND_SUCCESS ||
          it.category == ChatDiagnosticCategory.SEND_RECOVERY
      }
    }
  }

  val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .widthIn(max = 560.dp)
        .heightIn(max = 680.dp)
        .testTag("chat_diagnostics_dialog")
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = "Diagnóstico do Chat Firestore",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Diagnóstico Firestore (Chat)",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "$serviceTitle • #$serviceId",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("btn_close_chat_diagnostics")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Fechar diagnóstico")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Health Summary Banner
        val (bannerBg, bannerTint, bannerIcon, bannerTitle, bannerSubtitle) = when {
          permissionCount > 0 -> Quintuple(
            DangerRedLight,
            DangerRed,
            Icons.Default.Error,
            "Erro de Permissão/Auth Detectado ($permissionCount)",
            "Regras do Firestore ou sessão Auth bloquearam leitura/envio neste chat."
          )
          timeoutCount > 0 -> Quintuple(
            WarningAmberLight,
            WarningAmber,
            Icons.Default.Warning,
            "Timeout ou Rede Instável ($timeoutCount)",
            "O servidor Firestore demorou a confirmar snapshots ou envios."
          )
          else -> Quintuple(
            EmeraldWalletLight,
            EmeraldWallet,
            Icons.Default.CheckCircle,
            "Canal Firestore Saudável",
            "Nenhum erro de permissão ou timeout de rede registrado."
          )
        }

        Surface(
          color = bannerBg,
          border = BorderStroke(1.dp, bannerTint.copy(alpha = 0.35f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_diag_health_banner")
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = bannerIcon,
              contentDescription = null,
              tint = bannerTint,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = bannerTitle,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = bannerTint
              )
              Text(
                text = bannerSubtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter chips + Scope toggle
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          ChatDiagFilter.values().forEach { filter ->
            val selected = selectedFilter == filter
            val badgeCount = when (filter) {
              ChatDiagFilter.ALL -> scopedLogs.size
              ChatDiagFilter.PERMISSION -> permissionCount
              ChatDiagFilter.TIMEOUT -> timeoutCount
              ChatDiagFilter.EVENTS -> scopedLogs.size - permissionCount - timeoutCount
            }
            Surface(
              color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .clickable { selectedFilter = filter }
                .testTag(filter.tag)
            ) {
              Text(
                text = "${filter.label} ($badgeCount)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
              )
            }
          }

          Surface(
            color = if (showAllServices) InfoBlueLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .clickable { showAllServices = !showAllServices }
              .testTag("diag_toggle_all_services")
          ) {
            Text(
              text = if (showAllServices) "Todos os pedidos" else "Só este pedido",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = if (showAllServices) InfoBlue else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        // Log entries list
        if (filteredLogs.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f, fill = false)
              .padding(vertical = 28.dp)
              .testTag("chat_diag_empty_state"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Nenhum evento de diagnóstico neste filtro ainda. Envie uma mensagem ou aguarde o listener do Firestore.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f, fill = false)
              .heightIn(max = 360.dp)
              .testTag("chat_diag_log_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
          ) {
            items(filteredLogs, key = { it.id }) { entry ->
              ChatDiagnosticEntryCard(
                entry = entry,
                formattedTime = timeFormatter.format(Date(entry.timestampMillis))
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              onClearLogs()
              copiedFeedback = false
            },
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("btn_clear_chat_diagnostics")
          ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Limpar", fontWeight = FontWeight.SemiBold)
          }

          Button(
            onClick = {
              val report = onExportReport(if (showAllServices) null else serviceId)
              val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
              cm?.setPrimaryClip(ClipData.newPlainText("FirestoreChatDiagnostics", report))
              copiedFeedback = true
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (copiedFeedback) EmeraldWallet else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
              .weight(1.3f)
              .heightIn(min = 48.dp)
              .testTag("btn_copy_chat_diagnostics")
          ) {
            Icon(
              imageVector = if (copiedFeedback) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (copiedFeedback) "Logs Copiados" else "Copiar Logs",
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ChatDiagnosticEntryCard(
  entry: ChatDiagnosticLogEntry,
  formattedTime: String
) {
  val (accent, badgeBg) = when (entry.severity) {
    ChatDiagnosticSeverity.ERROR -> DangerRed to DangerRedLight
    ChatDiagnosticSeverity.WARNING -> WarningAmber to WarningAmberLight
    ChatDiagnosticSeverity.INFO -> InfoBlue to InfoBlueLight
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("chat_diag_item_${entry.id}")
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = badgeBg,
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = entry.category.label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = accent,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
        Text(
          text = formattedTime,
          style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      if (!entry.firestoreCode.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Código: ${entry.firestoreCode}",
          style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          ),
          color = accent
        )
      }

      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = entry.summary,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(6.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
          .padding(8.dp)
      ) {
        Text(
          text = entry.detailsJson,
          style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            lineHeight = 14.sp
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

private data class Quintuple<A, B, C, D, E>(
  val first: A,
  val second: B,
  val third: C,
  val fourth: D,
  val fifth: E
)
