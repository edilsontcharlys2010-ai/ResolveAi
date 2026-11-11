package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.ContactFilter
import com.example.model.ContactFilterResult
import com.example.model.ServiceRequest
import com.example.model.ServiceStatus
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.components.ContactViolationCard
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
  currentUser: User,
  services: List<ServiceRequest>,
  selectedServiceId: String?,
  chatMessages: List<ChatMessage>,
  onSelectService: (String) -> Unit,
  onSendMessage: (serviceId: String, text: String) -> ContactFilterResult
) {
  // Chat option only appears/unlocks when client accepts proposal/counter-proposal
  val acceptedServices = services.filter {
    it.status == ServiceStatus.ACEITO ||
      it.status == ServiceStatus.EM_EXECUCAO ||
      it.status == ServiceStatus.CONCLUIDO
  }

  if (acceptedServices.isEmpty()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Surface(
        color = WarningAmberLight,
        shape = CircleShape,
        modifier = Modifier.size(72.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = WarningAmber,
            modifier = Modifier.size(36.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Chat Privado Bloqueado",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "A opção de chat privado entre cliente e prestador só aparece e é liberada quando o cliente aceitar a proposta ou contraproposta.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(8.dp))

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "🛡️ Por que esta trava de segurança existe?",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Para proteger ambas as partes, evitar tentativas de evasão e assegurar que a taxa de 10% da plataforma seja debitada da carteira do prestador na aceitação oficial, liberando o canal direto e a garantia de atendimento.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
    return
  }

  val activeServiceId = if (acceptedServices.any { it.id == selectedServiceId }) {
    selectedServiceId!!
  } else {
    acceptedServices.first().id
  }
  val activeService = acceptedServices.find { it.id == activeServiceId }

  val messagesForActiveService = chatMessages.filter { it.serviceId == activeServiceId }

  var messageText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()

  // Real-time evaluation of anti-contact sharing rule
  val filterResult by remember(messageText) {
    derivedStateOf {
      if (messageText.isBlank()) {
        ContactFilterResult(isAllowed = true)
      } else {
        ContactFilter.evaluate(messageText)
      }
    }
  }

  // Auto-scroll on new message
  LaunchedEffect(messagesForActiveService.size) {
    if (messagesForActiveService.isNotEmpty()) {
      listState.animateScrollToItem(messagesForActiveService.size - 1)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Service selector chips bar (if user is in multiple accepted services)
    if (acceptedServices.size > 1) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        acceptedServices.forEach { srv ->
          val isSelected = srv.id == activeServiceId
          Surface(
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.clickable { onSelectService(srv.id) }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = srv.title.take(24) + (if (srv.title.length > 24) "..." else ""),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    }

    // Chat Service Top Info Bar
    if (activeService != null) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = activeService.title,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Negociação • ${activeService.category}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "R$ ${String.format("%.2f", activeService.finalAgreedPrice ?: activeService.proposedBudget)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = EmeraldWallet
              )
            }
          }

          Surface(
            color = Color(0xFFDCFCE7),
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = EmeraldWallet,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Anti-Vazamento Ativo",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                color = EmeraldWallet
              )
            }
          }
        }
      }
    }

    // Security Notice Banner (Always visible in Chat)
    Surface(
      color = Color(0xFFFFFBEB),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Security,
          contentDescription = null,
          tint = WarningAmber,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Regra de Proteção: Bloqueio automático de telefones, e-mails, WhatsApp e redes sociais. Mantenha todas as tratativas no app.",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
          color = Color(0xFF92400E)
        )
      }
    }

    // Messages Stream
    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = PaddingValues(vertical = 12.dp)
    ) {
      items(messagesForActiveService, key = { it.id }) { msg ->
        ChatMessageBubble(
          message = msg,
          isFromMe = msg.senderId == currentUser.id
        )
      }
    }

    // Real-Time Contact Violation Alert Card
    AnimatedVisibility(
      visible = !filterResult.isAllowed,
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically()
    ) {
      ContactViolationCard(
        violation = filterResult,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
      )
    }

    // Quick Test Chips (To verify anti-leak behavior conveniently)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 12.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Text(
        text = "Testar filtro:",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.align(Alignment.CenterVertically)
      )

      QuickTestChip(
        label = "⚠️ Tel/Whats",
        snippet = "Chama no zap 11 98888-7777",
        onSelect = { messageText = it }
      )
      QuickTestChip(
        label = "⚠️ Email",
        snippet = "Manda email para contato@pro.com",
        onSelect = { messageText = it }
      )
      QuickTestChip(
        label = "⚠️ Insta",
        snippet = "Segue meu insta @reparos_sp",
        onSelect = { messageText = it }
      )
      QuickTestChip(
        label = "⚠️ Chave Pix",
        snippet = "Minha chave pix é 123.456.789-00",
        onSelect = { messageText = it }
      )
      QuickTestChip(
        label = "✅ Válida",
        snippet = "Perfeito! Posso ir amanhã às 14h com as ferramentas.",
        onSelect = { messageText = it }
      )
    }

    // Message Input Area
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 4.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = messageText,
          onValueChange = { messageText = it },
          placeholder = { Text("Digite sua mensagem segura...") },
          isError = !filterResult.isAllowed,
          maxLines = 4,
          shape = RoundedCornerShape(24.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (filterResult.isAllowed) MaterialTheme.colorScheme.primary else DangerRed,
            unfocusedBorderColor = if (filterResult.isAllowed) MaterialTheme.colorScheme.outlineVariant else DangerRed
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("chat_input_field")
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Send Button
        FilledIconButton(
          onClick = {
            if (messageText.isNotBlank() && filterResult.isAllowed) {
              onSendMessage(activeServiceId, messageText)
              messageText = ""
              scope.launch {
                if (messagesForActiveService.isNotEmpty()) {
                  listState.animateScrollToItem(messagesForActiveService.size)
                }
              }
            }
          },
          enabled = messageText.isNotBlank() && filterResult.isAllowed,
          colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (filterResult.isAllowed) MaterialTheme.colorScheme.primary else DangerRed,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
          ),
          modifier = Modifier
            .size(48.dp)
            .testTag("chat_send_button")
        ) {
          Icon(
            imageVector = if (filterResult.isAllowed) Icons.Default.Send else Icons.Default.Block,
            contentDescription = "Enviar",
            tint = if (messageText.isNotBlank() && filterResult.isAllowed) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
private fun QuickTestChip(
  label: String,
  snippet: String,
  onSelect: (String) -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.clickable { onSelect(snippet) }
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
}

@Composable
private fun ChatMessageBubble(
  message: ChatMessage,
  isFromMe: Boolean
) {
  val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
  val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

  if (message.isSystemNotice) {
    // System message banner
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
      contentAlignment = Alignment.Center
    ) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.widthIn(max = 320.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = message.content,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  } else {
    // User message bubble
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
      // Sender label
      Text(
        text = if (isFromMe) "Você" else "${message.senderName} (${if (message.senderRole == UserRole.CLIENTE) "Cliente" else "Prestador"})",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
      )

      Surface(
        color = if (isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(
          topStart = 16.dp,
          topEnd = 16.dp,
          bottomStart = if (isFromMe) 16.dp else 4.dp,
          bottomEnd = if (isFromMe) 4.dp else 16.dp
        ),
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.widthIn(max = 280.dp)
      ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
          Text(
            text = message.content,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isFromMe) Color.White else MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = formattedTime,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
            color = if (isFromMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End)
          )
        }
      }
    }
  }
}
