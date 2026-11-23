package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.model.AppTip
import com.example.model.ServiceCategoryItem
import com.example.model.ServiceRequest
import com.example.model.ServiceStatus
import com.example.model.UserRole
import com.example.ui.theme.DangerRed

@Composable
fun AdminCategoryEditorDialog(
  initialCategory: ServiceCategoryItem?,
  onDismiss: () -> Unit,
  onSave: (name: String, specialtyLabel: String, isActive: Boolean) -> Result<ServiceCategoryItem>
) {
  var name by remember(initialCategory?.id) { mutableStateOf(initialCategory?.name.orEmpty()) }
  var specialtyLabel by remember(initialCategory?.id) {
    mutableStateOf(initialCategory?.specialtyLabel.orEmpty())
  }
  var isActive by remember(initialCategory?.id) { mutableStateOf(initialCategory?.isActive ?: true) }
  var errorMsg by remember(initialCategory?.id) { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp)
      .widthIn(max = 500.dp),
    icon = {
      Icon(
        imageVector = Icons.Default.Category,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary
      )
    },
    title = {
      Text(
        text = if (initialCategory == null) "Nova Categoria de Serviço" else "Editar Categoria",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            errorMsg = null
          },
          label = { Text("Nome da categoria *") },
          placeholder = { Text("Ex: Jardinagem, Marido de Aluguel...") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_admin_category_name")
        )

        OutlinedTextField(
          value = specialtyLabel,
          onValueChange = {
            specialtyLabel = it
            errorMsg = null
          },
          label = { Text("Especialidade para prestadores (opcional)") },
          placeholder = { Text("Ex: Jardinagem & Paisagismo") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_admin_category_specialty")
        )

        if (initialCategory != null) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Categoria ativa no catálogo",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                  text = "Quando ativada, aparece nos filtros e na criação de pedidos.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Switch(
                checked = isActive,
                onCheckedChange = { isActive = it },
                modifier = Modifier.testTag("switch_dialog_category_active")
              )
            }
          }
        }

        if (!errorMsg.isNullOrBlank()) {
          Text(
            text = errorMsg.orEmpty(),
            color = DangerRed,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val res = onSave(name, specialtyLabel.ifBlank { name }, isActive)
          res.onSuccess { onDismiss() }
            .onFailure { err -> errorMsg = err.message ?: "Erro ao salvar categoria." }
        },
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("btn_confirm_admin_category")
      ) {
        Text(if (initialCategory == null) "Adicionar Categoria" else "Salvar Alterações")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.heightIn(min = 48.dp)
      ) {
        Text("Cancelar")
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminEditServiceDialog(
  service: ServiceRequest,
  availableCategories: List<String>,
  onDismiss: () -> Unit,
  onSave: (ServiceRequest) -> Result<ServiceRequest>
) {
  val categoryOptions = remember(availableCategories, service.category) {
    (availableCategories + service.category).filter { it.isNotBlank() }.distinct()
  }
  var title by remember(service.id) { mutableStateOf(service.title) }
  var category by remember(service.id) { mutableStateOf(service.category) }
  var description by remember(service.id) { mutableStateOf(service.description) }
  var location by remember(service.id) { mutableStateOf(service.location) }
  var budgetText by remember(service.id) {
    mutableStateOf(String.format(java.util.Locale.US, "%.2f", service.proposedBudget))
  }
  var urgency by remember(service.id) { mutableStateOf(service.urgency) }
  var status by remember(service.id) { mutableStateOf(service.status) }
  var catExpanded by remember { mutableStateOf(false) }
  var statusExpanded by remember { mutableStateOf(false) }
  var errorMsg by remember(service.id) { mutableStateOf<String?>(null) }

  val urgencies = listOf("Baixa", "Média", "Alta", "Urgente")

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp)
      .widthIn(max = 540.dp),
    icon = {
      Icon(
        imageVector = Icons.Default.Edit,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary
      )
    },
    title = {
      Text(
        text = "Editar Serviço (Admin)",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = {
            title = it
            errorMsg = null
          },
          label = { Text("Título do serviço *") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_admin_edit_service_title")
        )

        ExposedDropdownMenuBox(
          expanded = catExpanded,
          onExpandedChange = { catExpanded = it }
        ) {
          OutlinedTextField(
            value = category,
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoria") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .menuAnchor()
              .fillMaxWidth()
              .testTag("dropdown_admin_edit_service_category")
          )
          ExposedDropdownMenu(
            expanded = catExpanded,
            onDismissRequest = { catExpanded = false }
          ) {
            categoryOptions.forEach { cat ->
              DropdownMenuItem(
                text = { Text(cat) },
                onClick = {
                  category = cat
                  catExpanded = false
                }
              )
            }
          }
        }

        ExposedDropdownMenuBox(
          expanded = statusExpanded,
          onExpandedChange = { statusExpanded = it }
        ) {
          OutlinedTextField(
            value = status.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Status do serviço") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .menuAnchor()
              .fillMaxWidth()
              .testTag("dropdown_admin_edit_service_status")
          )
          ExposedDropdownMenu(
            expanded = statusExpanded,
            onDismissRequest = { statusExpanded = false }
          ) {
            ServiceStatus.entries.forEach { st ->
              DropdownMenuItem(
                text = { Text(st.label) },
                onClick = {
                  status = st
                  statusExpanded = false
                }
              )
            }
          }
        }

        OutlinedTextField(
          value = location,
          onValueChange = { location = it },
          label = { Text("Localização / Bairro") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = budgetText,
          onValueChange = {
            budgetText = it
            errorMsg = null
          },
          label = { Text("Orçamento proposto (R$)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_admin_edit_service_budget")
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Urgência",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            urgencies.forEach { urg ->
              FilterChip(
                selected = urgency.equals(urg, ignoreCase = true),
                onClick = { urgency = urg },
                label = { Text(urg, fontSize = 12.sp) }
              )
            }
          }
        }

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Descrição detalhada") },
          minLines = 3,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )

        if (!errorMsg.isNullOrBlank()) {
          Text(
            text = errorMsg.orEmpty(),
            color = DangerRed,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsedBudget = budgetText.replace(",", ".").trim().toDoubleOrNull()
          if (parsedBudget == null || parsedBudget < 0.0) {
            errorMsg = "Informe um orçamento numérico válido."
            return@Button
          }
          val res = onSave(
            service.copy(
              title = title,
              category = category,
              description = description,
              location = location,
              proposedBudget = parsedBudget,
              urgency = urgency,
              status = status
            )
          )
          res.onSuccess { onDismiss() }
            .onFailure { err -> errorMsg = err.message ?: "Não foi possível salvar." }
        },
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("btn_confirm_admin_edit_service")
      ) {
        Text("Salvar Serviço")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.heightIn(min = 48.dp)
      ) {
        Text("Cancelar")
      }
    }
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminTipEditorDialog(
  initialTip: AppTip?,
  onDismiss: () -> Unit,
  onSave: (AppTip) -> Result<AppTip>
) {
  var title by remember(initialTip?.id) { mutableStateOf(initialTip?.title.orEmpty()) }
  var category by remember(initialTip?.id) { mutableStateOf(initialTip?.category.orEmpty().ifBlank { "Geral" }) }
  var description by remember(initialTip?.id) { mutableStateOf(initialTip?.description.orEmpty()) }
  var targetRole by remember(initialTip?.id) { mutableStateOf(initialTip?.targetRole) }
  var errorMsg by remember(initialTip?.id) { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp)
      .widthIn(max = 520.dp),
    icon = {
      Icon(
        imageVector = Icons.Default.TipsAndUpdates,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary
      )
    },
    title = {
      Text(
        text = if (initialTip == null) "Nova Dica do Aplicativo" else "Editar Dica",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Público da dica",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = targetRole == null,
            onClick = { targetRole = null },
            label = { Text("Todos") }
          )
          FilterChip(
            selected = targetRole == UserRole.CLIENTE,
            onClick = { targetRole = UserRole.CLIENTE },
            label = { Text("Clientes") }
          )
          FilterChip(
            selected = targetRole == UserRole.PRESTADOR,
            onClick = { targetRole = UserRole.PRESTADOR },
            label = { Text("Prestadores") }
          )
        }

        OutlinedTextField(
          value = category,
          onValueChange = { category = it },
          label = { Text("Etiqueta / Tema") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = title,
          onValueChange = {
            title = it
            errorMsg = null
          },
          label = { Text("Título da dica *") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_admin_tip_title")
        )

        OutlinedTextField(
          value = description,
          onValueChange = {
            description = it
            errorMsg = null
          },
          label = { Text("Descrição *") },
          minLines = 3,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_admin_tip_description")
        )

        if (!errorMsg.isNullOrBlank()) {
          Text(
            text = errorMsg.orEmpty(),
            color = DangerRed,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val res = onSave(
            AppTip(
              id = initialTip?.id.orEmpty(),
              targetRole = targetRole,
              category = category,
              title = title,
              description = description
            )
          )
          res.onSuccess { onDismiss() }
            .onFailure { err -> errorMsg = err.message ?: "Erro ao salvar dica." }
        },
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("btn_confirm_admin_tip")
      ) {
        Text("Salvar Dica")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.heightIn(min = 48.dp)
      ) {
        Text("Cancelar")
      }
    }
  )
}

@Composable
fun AdminConfirmDeleteCategoryDialog(
  category: ServiceCategoryItem,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.DeleteOutline,
        contentDescription = null,
        tint = DangerRed
      )
    },
    title = { Text("Remover Categoria?", fontWeight = FontWeight.Bold) },
    text = {
      Text(
        text = "Deseja remover \"${category.name}\" do catálogo de serviços? Pedidos antigos já publicados manterão seu histórico.",
        style = MaterialTheme.typography.bodyMedium
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("btn_confirm_delete_category")
      ) {
        Text("Remover")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.heightIn(min = 48.dp)
      ) {
        Text("Cancelar")
      }
    }
  )
}
