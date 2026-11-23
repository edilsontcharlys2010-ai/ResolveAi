package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSuggestion
import com.example.model.AppTip
import com.example.model.SuggestionStatus
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.components.resolveAdaptiveSpec
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class TipsAudienceFilter(val label: String) {
  TODOS("Todos"),
  CLIENTE("Clientes"),
  PRESTADOR("Prestadores")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuggestionsTipsScreen(
  currentUser: User,
  tips: List<AppTip>,
  suggestions: List<AppSuggestion>,
  onSubmitSuggestion: (category: String, title: String, description: String) -> Unit,
  onUpdateSuggestionStatus: (suggestionId: String, status: SuggestionStatus, adminReply: String?) -> Unit,
  onBack: () -> Unit
) {
  BackHandler { onBack() }

  var audienceFilter by remember(currentUser.role) {
    mutableStateOf(
      when (currentUser.role) {
        UserRole.CLIENTE -> TipsAudienceFilter.CLIENTE
        UserRole.PRESTADOR -> TipsAudienceFilter.PRESTADOR
        UserRole.ADMIN -> TipsAudienceFilter.TODOS
      }
    )
  }

  val suggestionCategories = remember {
    listOf("Melhoria no App", "Novo Serviço", "Segurança", "Pagamentos", "Outro")
  }
  var selectedCategory by remember { mutableStateOf(suggestionCategories.first()) }
  var titleText by remember { mutableStateOf("") }
  var descriptionText by remember { mutableStateOf("") }
  var adminEditingSuggestion by remember { mutableStateOf<AppSuggestion?>(null) }

  val filteredTips = remember(tips, audienceFilter) {
    when (audienceFilter) {
      TipsAudienceFilter.TODOS -> tips
      TipsAudienceFilter.CLIENTE -> tips.filter { it.targetRole == null || it.targetRole == UserRole.CLIENTE }
      TipsAudienceFilter.PRESTADOR -> tips.filter { it.targetRole == null || it.targetRole == UserRole.PRESTADOR }
    }
  }

  val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding(),
    contentAlignment = Alignment.TopCenter
  ) {
    val spec = remember(maxWidth) { resolveAdaptiveSpec(maxWidth) }

    Column(
      modifier = Modifier
        .widthIn(max = 880.dp)
        .fillMaxSize()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("btn_suggestions_back")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
        }
        Icon(
          imageVector = Icons.Default.Lightbulb,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Sugestões e Dicas",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = spec.horizontalPadding)
          .testTag("suggestions_tips_list"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 36.dp)
      ) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.TipsAndUpdates,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Dicas de Uso do Aplicativo",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              TipsAudienceFilter.entries.forEach { option ->
                val selected = audienceFilter == option
                Surface(
                  color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                  shape = RoundedCornerShape(18.dp),
                  modifier = Modifier
                    .clickable { audienceFilter = option }
                    .testTag("tips_filter_${option.name.lowercase()}")
                ) {
                  Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelMedium.copy(
                      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                  )
                }
              }
            }
          }
        }

        items(filteredTips, key = { it.id }) { tip ->
          val badgeColor = when (tip.targetRole) {
            UserRole.PRESTADOR -> EmeraldWallet
            UserRole.CLIENTE -> InfoBlue
            else -> MaterialTheme.colorScheme.primary
          }
          val badgeLabel = when (tip.targetRole) {
            UserRole.PRESTADOR -> "Prestadores"
            UserRole.CLIENTE -> "Clientes"
            else -> "Todos"
          }
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("tip_card_${tip.id}")
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = tip.category,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = badgeColor
                )
                Surface(
                  color = badgeColor.copy(alpha = 0.14f),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                  )
                }
              }
              Text(
                text = tip.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = tip.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("suggestion_form_card")
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Text(
                text = "Enviar Sugestão para o App",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "Tem alguma ideia de melhoria ou novo tipo de serviço? Envie para nossa equipe.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                suggestionCategories.forEach { cat ->
                  val selected = selectedCategory == cat
                  Surface(
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                      .clickable { selectedCategory = cat }
                      .testTag("suggestion_cat_${cat.lowercase().replace(" ", "_")}")
                  ) {
                    Text(
                      text = cat,
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                      ),
                      color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                  }
                }
              }

              OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                label = { Text("Título da sugestão *") },
                placeholder = { Text("Ex: Adicionar agendamento recorrente") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("input_suggestion_title")
              )

              OutlinedTextField(
                value = descriptionText,
                onValueChange = { descriptionText = it },
                label = { Text("Descrição detalhada *") },
                placeholder = { Text("Explique como essa novidade ajudaria clientes ou prestadores...") },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("input_suggestion_description")
              )

              Button(
                onClick = {
                  onSubmitSuggestion(selectedCategory, titleText.trim(), descriptionText.trim())
                  titleText = ""
                  descriptionText = ""
                },
                enabled = titleText.trim().length >= 3 && descriptionText.trim().length >= 5,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("btn_submit_suggestion")
              ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enviar Sugestão", fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        item {
          Text(
            text = "Sugestões Enviadas (${suggestions.size})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        }

        if (suggestions.isEmpty()) {
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "Nenhuma sugestão enviada ainda. Seja o primeiro a contribuir!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp)
              )
            }
          }
        } else {
          items(suggestions, key = { it.id }) { item ->
            val (statusBg, statusFg) = when (item.status) {
              SuggestionStatus.ENVIADA -> InfoBlueLight to InfoBlue
              SuggestionStatus.EM_ANALISE -> WarningAmberLight to WarningAmber
              SuggestionStatus.IMPLEMENTADA -> EmeraldWalletLight to EmeraldWallet
              SuggestionStatus.ARQUIVADA -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
            }
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("suggestion_item_${item.id}")
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "${item.category} • ${item.userRole.label}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                  )
                  Surface(color = statusBg, shape = RoundedCornerShape(8.dp)) {
                    Text(
                      text = item.status.label,
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                      color = statusFg,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                  }
                }
                Text(
                  text = item.title,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                  text = item.description,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "Por ${item.userName} • ${dateFormat.format(Date(item.createdAt))}",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!item.adminReply.isNullOrBlank()) {
                  Surface(
                    color = EmeraldWalletLight.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = "Resposta da equipe: ${item.adminReply}",
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                      color = Color(0xFF065F46),
                      modifier = Modifier.padding(8.dp)
                    )
                  }
                }

                if (currentUser.role == UserRole.ADMIN) {
                  OutlinedButton(
                    onClick = { adminEditingSuggestion = item },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("btn_admin_reply_suggestion_${item.id}")
                  ) {
                    Text("Responder / Atualizar Status", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  adminEditingSuggestion?.let { target ->
    var selectedStatus by remember(target.id) { mutableStateOf(target.status) }
    var replyText by remember(target.id) { mutableStateOf(target.adminReply.orEmpty()) }
    AlertDialog(
      onDismissRequest = { adminEditingSuggestion = null },
      title = { Text("Gerenciar Sugestão", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(target.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            SuggestionStatus.entries.forEach { st ->
              val selected = selectedStatus == st
              Surface(
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.clickable { selectedStatus = st }
              ) {
                Text(
                  text = st.label,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }
            }
          }
          OutlinedTextField(
            value = replyText,
            onValueChange = { replyText = it },
            label = { Text("Resposta ao usuário (opcional)") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdateSuggestionStatus(
              target.id,
              selectedStatus,
              replyText.trim().takeIf { it.isNotBlank() }
            )
            adminEditingSuggestion = null
          }
        ) {
          Text("Salvar")
        }
      },
      dismissButton = {
        TextButton(onClick = { adminEditingSuggestion = null }) {
          Text("Cancelar")
        }
      }
    )
  }
}
