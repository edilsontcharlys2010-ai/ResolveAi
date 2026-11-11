package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.model.AppNotification
import com.example.model.NotificationType
import com.example.model.ProviderVerificationStatus
import com.example.model.ServiceRequest
import com.example.model.ServiceReview
import com.example.ui.PendingRegistration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.InsufficientBalanceException
import com.example.model.ContactFilterResult
import com.example.model.ProposalStatus
import com.example.model.ServiceStatus
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight

@Composable
fun AppTopBar(
  currentUser: User,
  unreadNotificationCount: Int = 0,
  onSwitchUserClick: () -> Unit,
  onOpenWalletClick: () -> Unit,
  onNotificationsClick: () -> Unit = {},
  onAdminClick: () -> Unit = {}
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    shadowElevation = 2.dp,
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Brand Logo & Title
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f, fill = false)
      ) {
        Image(
          painter = painterResource(id = R.drawable.resolveai_logo),
          contentDescription = "Logo ResolveAi Serviços",
          modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "ResolveAi",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Serviços & Negociação",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
            color = EmeraldWallet
          )
        }
      }

      // Profile & Role Chip with switch option
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (currentUser.role == UserRole.ADMIN) {
          // Admin panel trigger
          Surface(
            color = Color(0xFF1E3A8A),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .clickable { onAdminClick() }
              .padding(end = 6.dp)
              .testTag("top_bar_admin_chip")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = "Admin",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Painel",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        if (currentUser.role == UserRole.PRESTADOR) {
          // Notifications Bell for Provider (New service opportunities alerts)
          IconButton(
            onClick = onNotificationsClick,
            modifier = Modifier
              .padding(end = 4.dp)
              .testTag("top_bar_notifications_button")
          ) {
            BadgedBox(
              badge = {
                if (unreadNotificationCount > 0) {
                  Badge(containerColor = DangerRed) { Text("$unreadNotificationCount") }
                }
              }
            ) {
              Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notificações",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
              )
            }
          }

          // Wallet pill for provider
          Surface(
            color = EmeraldWalletLight,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .clickable { onOpenWalletClick() }
              .padding(end = 8.dp)
              .testTag("top_bar_wallet_chip")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = "Carteira",
                tint = EmeraldWallet,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "R$ ${String.format("%.2f", currentUser.walletBalance)}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = EmeraldWallet
              )
            }
          }
        }

        // User Avatar and Switch button
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .clickable { onSwitchUserClick() }
            .testTag("switch_user_top_button")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(currentUser.avatarColorHex)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = currentUser.name.take(1),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text(
                text = currentUser.name.split(" ").firstOrNull() ?: "",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = if (currentUser.role == UserRole.CLIENTE) "Cliente" else "Prestador",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = if (currentUser.role == UserRole.CLIENTE) InfoBlue else EmeraldWallet
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.Default.SwapHoriz,
              contentDescription = "Trocar usuário",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun StatusBadge(status: ServiceStatus) {
  val (bgColor, textColor, icon) = when (status) {
    ServiceStatus.ABERTO -> Triple(InfoBlueLight, InfoBlue, Icons.Default.Info)
    ServiceStatus.EM_NEGOCIACAO -> Triple(WarningAmberLight, WarningAmber, Icons.Default.Handshake)
    ServiceStatus.ACEITO -> Triple(EmeraldWalletLight, EmeraldWallet, Icons.Default.CheckCircle)
    ServiceStatus.EM_EXECUCAO -> Triple(Color(0xFFE0E7FF), Color(0xFF4338CA), Icons.Default.Handshake)
    ServiceStatus.CONCLUIDO -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), Icons.Default.CheckCircle)
    ServiceStatus.CANCELADO -> Triple(DangerRedLight, DangerRed, Icons.Default.Block)
  }

  Surface(
    color = bgColor,
    shape = RoundedCornerShape(8.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = textColor,
        modifier = Modifier.size(13.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = status.label,
        color = textColor,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
      )
    }
  }
}

@Composable
fun ProposalStatusBadge(status: ProposalStatus) {
  val (bgColor, textColor) = when (status) {
    ProposalStatus.ENVIADA -> Pair(InfoBlueLight, InfoBlue)
    ProposalStatus.CONTRAPROPOSTA_CLIENTE -> Pair(WarningAmberLight, WarningAmber)
    ProposalStatus.CONTRAPROPOSTA_PRESTADOR -> Pair(Color(0xFFEDE9FE), Color(0xFF6D28D9))
    ProposalStatus.ACEITA -> Pair(EmeraldWalletLight, EmeraldWallet)
    ProposalStatus.RECUSADA -> Pair(DangerRedLight, DangerRed)
  }

  Surface(
    color = bgColor,
    shape = RoundedCornerShape(6.dp)
  ) {
    Text(
      text = status.label,
      color = textColor,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
    )
  }
}

@Composable
fun ContactViolationCard(
  violation: ContactFilterResult,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = DangerRedLight),
    border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
    shape = RoundedCornerShape(12.dp),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.Top
    ) {
      Icon(
        imageVector = Icons.Default.Block,
        contentDescription = "Bloqueado",
        tint = DangerRed,
        modifier = Modifier.size(24.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = "⚠️ Troca de Contato Não Permitida",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = DangerRed
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = violation.reasonMessage ?: "Não é permitida a troca de contatos externos.",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF7F1D1D)
        )
        if (!violation.detectedSnippet.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Detectado: \"${violation.detectedSnippet}\" (${violation.violationType ?: "Dado proibido"})",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = DangerRed
          )
        }
      }
    }
  }
}

@Composable
fun InsufficientBalanceDialog(
  data: InsufficientBalanceException,
  onDismiss: () -> Unit,
  onRechargeClick: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = null,
        tint = WarningAmber,
        modifier = Modifier.size(36.dp)
      )
    },
    title = {
      Text(
        text = "Saldo Insuficiente na Carteira",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Para aceitar este serviço e fechar o contrato, é necessário debitar a taxa de 10% da plataforma da carteira do prestador.",
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Taxa necessária (10%):", style = MaterialTheme.typography.bodySmall)
              Text(
                "R$ ${String.format("%.2f", data.requiredFee)}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = WarningAmber
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Saldo atual na carteira:", style = MaterialTheme.typography.bodySmall)
              Text(
                "R$ ${String.format("%.2f", data.currentBalance)}",
                style = MaterialTheme.typography.bodySmall
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Valor faltante:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
              Text(
                "R$ ${String.format("%.2f", data.missingAmount)}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = DangerRed
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onRechargeClick,
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
        modifier = Modifier.testTag("dialog_recharge_button")
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Recarregar Carteira Pix")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    }
  )
}

@Composable
fun RechargeWalletDialog(
  currentBalance: Double,
  onDismiss: () -> Unit,
  onConfirmRecharge: (Double) -> Unit
) {
  var selectedAmount by remember { mutableStateOf(50.0) }
  var customAmountText by remember { mutableStateOf("") }
  var isCustom by remember { mutableStateOf(false) }

  val predefinedAmounts = listOf(20.0, 50.0, 100.0, 200.0)

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.AccountBalanceWallet,
        contentDescription = null,
        tint = EmeraldWallet,
        modifier = Modifier.size(36.dp)
      )
    },
    title = {
      Text(
        text = "Recarga de Carteira Digital",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Saldo atual: R$ ${String.format("%.2f", currentBalance)}",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = EmeraldWallet
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "O saldo é utilizado para o débito automático da taxa de 10% nos serviços contratados. Recarregue instantaneamente via Pix:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Predefined amounts grid
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          predefinedAmounts.forEach { amount ->
            val isSelected = !isCustom && selectedAmount == amount
            Surface(
              color = if (isSelected) EmeraldWallet else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .clickable {
                  selectedAmount = amount
                  isCustom = false
                }
            ) {
              Box(
                modifier = Modifier.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "R$ ${amount.toInt()}",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Custom amount input
        OutlinedTextField(
          value = customAmountText,
          onValueChange = {
            customAmountText = it.filter { char -> char.isDigit() || char == '.' }
            isCustom = true
          },
          label = { Text("Outro valor (R$)") },
          placeholder = { Text("Ex: 75.00") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amountToRecharge = if (isCustom) {
            customAmountText.toDoubleOrNull() ?: 50.0
          } else {
            selectedAmount
          }
          if (amountToRecharge > 0) {
            onConfirmRecharge(amountToRecharge)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
        modifier = Modifier.testTag("confirm_recharge_button")
      ) {
        Text("Pagar & Recarregar Pix")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Fechar")
      }
    }
  )
}

@Composable
fun UserSwitcherDialog(
  users: List<User>,
  currentUserId: String,
  onDismiss: () -> Unit,
  onSelectUser: (String) -> Unit,
  onOpenRegisterClick: () -> Unit = {}
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.SwapHoriz,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp)
      )
    },
    title = {
      Text(
        text = "Alternar ou Criar Conta",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Prominent button to register new user with SMS
        Button(
          onClick = {
            onDismiss()
            onOpenRegisterClick()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("btn_open_sms_register")
        ) {
          Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Novo Cadastro com SMS", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Ou alterne para uma conta existente (Área do Cliente ou Área do Prestador):",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))

        users.forEach { user ->
          val isSelected = user.id == currentUserId
          Card(
            colors = CardDefaults.cardColors(
              containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            ),
            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelectUser(user.id) }
              .testTag("switch_user_item_${user.id}")
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(Color(user.avatarColorHex)),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = user.name.take(1),
                  color = Color.White,
                  fontWeight = FontWeight.Bold
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                  )
                  if (user.isPhoneVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                      imageVector = Icons.Default.Verified,
                      contentDescription = "SMS Verificado",
                      tint = InfoBlue,
                      modifier = Modifier.size(14.dp)
                    )
                  }
                }
                Text(
                  text = when (user.role) {
                    UserRole.CLIENTE -> "Cliente • ${user.city}"
                    UserRole.ADMIN -> "Administrador do App • Análise e Aprovação"
                    UserRole.PRESTADOR -> {
                      if (user.verificationStatus == ProviderVerificationStatus.PENDENTE_ANALISE) {
                        "Prestador (Documentos em Análise pelo Administrador)"
                      } else {
                        "${user.category} • Carteira: R$ ${String.format("%.2f", user.walletBalance)}"
                      }
                    }
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = when (user.role) {
                    UserRole.ADMIN -> Color(0xFF1E3A8A)
                    UserRole.PRESTADOR -> if (user.verificationStatus == ProviderVerificationStatus.PENDENTE_ANALISE) WarningAmber else EmeraldWallet
                    UserRole.CLIENTE -> InfoBlue
                  }
                )
              }
              if (isSelected) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = "Ativo",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Fechar")
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateServiceDialog(
  onDismiss: () -> Unit,
  onCreate: (title: String, category: String, description: String, location: String, budget: Double, urgency: String) -> Unit,
  onOpenSupport: (() -> Unit)? = null
) {
  var title by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("Eletricista") }
  var description by remember { mutableStateOf("") }
  var location by remember { mutableStateOf("São Paulo, SP") }
  var budgetText by remember { mutableStateOf("") }
  var urgency by remember { mutableStateOf("Média") }
  var showSupportModal by remember { mutableStateOf(false) }

  val categories = listOf(
    "Eletricista",
    "Hidráulica & Encanador",
    "Pintura & Acabamento",
    "TI, Redes & Informática",
    "Limpeza & Diarista",
    "Montagem de Móveis",
    "Reformas & Alvenaria",
    "Fretes & Mudanças"
  )
  val urgencies = listOf("Baixa", "Média", "Alta", "Urgente")

  var catExpanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Solicitar Novo Serviço",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Quick Support banner inside Request Service dialog
        Surface(
          color = Color(0xFFEFF6FF),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Default.SupportAgent,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "Dúvidas no pedido?",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary
                )
                Text(
                  text = "Nossa equipe pode te ajudar a estimar o valor.",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            OutlinedButton(
              onClick = {
                if (onOpenSupport != null) {
                  onOpenSupport()
                } else {
                  showSupportModal = true
                }
              },
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
              modifier = Modifier.testTag("btn_dialog_contact_support")
            ) {
              Icon(Icons.Default.Headset, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Falar com Suporte", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Título do Serviço") },
          placeholder = { Text("Ex: Troca de tomada e disjuntor") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_service_title")
        )

        // Category dropdown
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
            modifier = Modifier
              .menuAnchor()
              .fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = catExpanded,
            onDismissRequest = { catExpanded = false }
          ) {
            categories.forEach { cat ->
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

        OutlinedTextField(
          value = budgetText,
          onValueChange = { budgetText = it.filter { c -> c.isDigit() || c == '.' } },
          label = { Text("Valor Proposto Inicial (R$)") },
          placeholder = { Text("Ex: 250.00") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_service_budget")
        )

        OutlinedTextField(
          value = location,
          onValueChange = { location = it },
          label = { Text("Local / Bairro / Cidade") },
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Descrição detalhada do problema") },
          minLines = 3,
          maxLines = 5,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val budget = budgetText.toDoubleOrNull() ?: 100.0
          if (title.isNotBlank()) {
            onCreate(title, category, description, location, budget, urgency)
          }
        },
        enabled = title.isNotBlank() && budgetText.isNotBlank(),
        modifier = Modifier.testTag("btn_submit_create_service")
      ) {
        Text("Publicar Pedido")
      }
    },
    dismissButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(
          onClick = {
            if (onOpenSupport != null) {
              onOpenSupport()
            } else {
              showSupportModal = true
            }
          },
          modifier = Modifier.testTag("btn_dialog_support_footer")
        ) {
          Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Suporte ResolveAi", fontSize = 12.sp)
        }

        TextButton(onClick = onDismiss) {
          Text("Cancelar")
        }
      }
    }
  )

  if (showSupportModal) {
    SupportContactDialog(
      onDismiss = { showSupportModal = false }
    )
  }
}

@Composable
fun SupportContactDialog(
  onDismiss: () -> Unit
) {
  var sentMessage by remember { mutableStateOf(false) }
  var messageText by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.SupportAgent,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(36.dp)
      )
    },
    title = {
      Text(
        text = "Central de Suporte ResolveAi",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        if (!sentMessage) {
          Text(
            text = "Precisa de ajuda para cadastrar seu pedido, definir um orçamento justo ou tirar dúvidas da plataforma?",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Surface(
            color = Color(0xFFF3F4F6),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("⏰ Horário de Atendimento:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
              Text("Segunda a Sábado, das 08h às 20h (Horário de Brasília)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
              Text("⚡ Tempo médio de resposta: ~3 minutos", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = EmeraldWallet))
              Text("📞 Telefone / WhatsApp: 0800 737 6583", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
            }
          }

          OutlinedTextField(
            value = messageText,
            onValueChange = { messageText = it },
            label = { Text("Descreva sua dúvida para o atendente") },
            placeholder = { Text("Ex: Qual o valor médio para trocar fiação de chuveiro?") },
            minLines = 3,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_support_message")
          )
        } else {
          Surface(
            color = Color(0xFFF0FDF4),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldWallet, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Chamado Aberto com Sucesso!", fontWeight = FontWeight.Bold, color = EmeraldWallet)
              }
              Text(
                "Protocolo: #SUP-${(100000..999999).random()}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              )
              Text(
                "Um especialista da equipe ResolveAi Serviços responderá via notificação do aplicativo em instantes.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
              )
            }
          }
        }
      }
    },
    confirmButton = {
      if (!sentMessage) {
        Button(
          onClick = {
            if (messageText.isNotBlank()) {
              sentMessage = true
            } else {
              sentMessage = true
            }
          },
          modifier = Modifier.testTag("btn_send_support_ticket")
        ) {
          Text("Iniciar Atendimento")
        }
      } else {
        Button(onClick = onDismiss) {
          Text("Entendido")
        }
      }
    },
    dismissButton = {
      if (!sentMessage) {
        TextButton(onClick = onDismiss) {
          Text("Voltar")
        }
      }
    }
  )
}

@Composable
fun CounterOfferDialog(
  currentPrice: Double,
  onDismiss: () -> Unit,
  onConfirm: (newPrice: Double, note: String) -> Unit
) {
  var priceText by remember { mutableStateOf(String.format("%.2f", currentPrice)) }
  var noteText by remember { mutableStateOf("") }

  val parsedPrice = priceText.replace(",", ".").toDoubleOrNull() ?: 0.0
  val feeEstimate = parsedPrice * 0.10

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Handshake,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp)
      )
    },
    title = {
      Text(
        text = "Enviar Contraproposta",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Valor atual da negociação: R$ ${String.format("%.2f", currentPrice)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
          value = priceText,
          onValueChange = { priceText = it },
          label = { Text("Novo Valor Proposto (R$)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_counter_offer_price")
        )

        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it },
          label = { Text("Observação / Justificativa") },
          placeholder = { Text("Ex: Consigo fechar se for para começar amanhã.") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth()
        )

        // Fee reminder
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Ao fechar em R$ ${String.format("%.2f", parsedPrice)}, a taxa de 10% da plataforma (R$ ${String.format("%.2f", feeEstimate)}) será debitada da carteira do prestador na aceitação.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (parsedPrice > 0) {
            onConfirm(parsedPrice, noteText)
          }
        },
        enabled = parsedPrice > 0,
        modifier = Modifier.testTag("btn_confirm_counter_offer")
      ) {
        Text("Enviar Contraproposta")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterUserDialog(
  onDismiss: () -> Unit,
  onProceedToSms: (PendingRegistration) -> Unit
) {
  var selectedRole by remember { mutableStateOf(UserRole.CLIENTE) }
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var city by remember { mutableStateOf("São Paulo, SP") }
  var document by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("Eletricista Residencial") }
  var bio by remember { mutableStateOf("") }

  val categories = listOf(
    "Eletricista Residencial & Predial",
    "Hidráulica & Encanador",
    "Pintura & Acabamento",
    "TI, Redes & Informática",
    "Limpeza & Diarista",
    "Montagem de Móveis",
    "Reformas & Alvenaria",
    "Ar Condicionado & Refrigeração"
  )
  var catExpanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Criar Cadastro no ResolveAi",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Role Switcher Cards
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            color = if (selectedRole == UserRole.CLIENTE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            border = if (selectedRole == UserRole.CLIENTE) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier
              .weight(1f)
              .clickable { selectedRole = UserRole.CLIENTE }
              .testTag("register_role_cliente")
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = Icons.Default.Handshake,
                contentDescription = null,
                tint = if (selectedRole == UserRole.CLIENTE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Sou Cliente",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (selectedRole == UserRole.CLIENTE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
              )
              Text("Contratar", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }

          Surface(
            color = if (selectedRole == UserRole.PRESTADOR) EmeraldWalletLight else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            border = if (selectedRole == UserRole.PRESTADOR) androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldWallet) else null,
            modifier = Modifier
              .weight(1f)
              .clickable { selectedRole = UserRole.PRESTADOR }
              .testTag("register_role_prestador")
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = if (selectedRole == UserRole.PRESTADOR) EmeraldWallet else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Sou Prestador",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (selectedRole == UserRole.PRESTADOR) EmeraldWallet else MaterialTheme.colorScheme.onSurface
              )
              Text("Trabalhar", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nome Completo") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_reg_name")
        )

        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Celular (para SMS)") },
          placeholder = { Text("(11) 98765-4321") },
          leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_reg_phone")
        )

        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("E-mail") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = document,
          onValueChange = { document = it },
          label = { Text(if (selectedRole == UserRole.CLIENTE) "CPF" else "CPF ou CNPJ") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        if (selectedRole == UserRole.PRESTADOR) {
          ExposedDropdownMenuBox(
            expanded = catExpanded,
            onExpandedChange = { catExpanded = it }
          ) {
            OutlinedTextField(
              value = category,
              onValueChange = {},
              readOnly = true,
              label = { Text("Especialidade Profissional") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
              modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
            )
            ExposedDropdownMenu(
              expanded = catExpanded,
              onDismissRequest = { catExpanded = false }
            ) {
              categories.forEach { cat ->
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

          OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio / Apresentação de Serviços") },
            placeholder = { Text("Ex: 5 anos de experiência com instalações...") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
          )

          // Document Section for Providers
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "📄 Documentação Obrigatória para Aprovação:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
              )

              var rgCnh by remember { mutableStateOf("") }
              var criminalRecord by remember { mutableStateOf("") }
              var selfieChecked by remember { mutableStateOf(true) }

              OutlinedTextField(
                value = rgCnh,
                onValueChange = { rgCnh = it },
                label = { Text("Número do RG ou CNH") },
                placeholder = { Text("Ex: CNH 04829103948") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_reg_cnh")
              )

              OutlinedTextField(
                value = criminalRecord,
                onValueChange = { criminalRecord = it },
                label = { Text("Nº Certidão de Antecedentes Criminais") },
                placeholder = { Text("Ex: CERT-2026-99120-PF") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("input_reg_antecedentes")
              )

              Surface(
                color = Color(0xFFDCFCE7),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Verified, contentDescription = null, tint = EmeraldWallet, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Foto Selfie com documento anexada (selfie_documento.jpg)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldWallet
                  )
                }
              }

              Text(
                text = "⚠️ Um administrador do ResolveAi analisará seus documentos e selfie antes de liberar seu acesso aos serviços.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = WarningAmber
              )
            }
          }

          // Bonus welcome badge
          Surface(
            color = EmeraldWalletLight,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldWallet, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Bônus de R$ 50,00 na carteira concedido na ativação por SMS para cobrir taxas de 10%!",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = EmeraldWallet
              )
            }
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Sms, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Na próxima etapa você receberá um SMS com código de 6 dígitos para autenticação da conta.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank() && phone.isNotBlank()) {
            onProceedToSms(
              PendingRegistration(
                name = name,
                email = email.ifBlank { "$name@email.com".lowercase().replace(" ", "") },
                phone = phone,
                role = selectedRole,
                category = if (selectedRole == UserRole.PRESTADOR) category else "",
                bio = bio,
                city = city,
                document = document,
                initialWalletBonus = if (selectedRole == UserRole.PRESTADOR) 50.0 else 0.0,
                rgOrCnhNumber = if (selectedRole == UserRole.PRESTADOR) "CNH ${(10000000000..99999999999).random()}" else "",
                criminalRecordNumber = if (selectedRole == UserRole.PRESTADOR) "CERT-2026-PF-${(10000..99999).random()}" else "",
                selfieFileName = if (selectedRole == UserRole.PRESTADOR) "selfie_com_documento.jpg" else ""
              )
            )
          }
        },
        enabled = name.isNotBlank() && phone.isNotBlank(),
        modifier = Modifier.testTag("btn_proceed_to_sms")
      ) {
        Text("Receber Código SMS")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    }
  )
}

@Composable
fun SmsVerificationDialog(
  phone: String,
  smsNotificationBanner: String?,
  onDismiss: () -> Unit,
  onConfirmCode: (String) -> Unit,
  onResendCode: () -> Unit
) {
  var smsCode by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Sms,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(36.dp)
      )
    },
    title = {
      Text(
        text = "Autenticação por SMS",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Enviamos uma mensagem SMS com um código de segurança de 6 dígitos para:",
          style = MaterialTheme.typography.bodySmall,
          textAlign = TextAlign.Center
        )

        Text(
          text = phone,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
          color = MaterialTheme.colorScheme.primary
        )

        // Simulated incoming SMS notification banner
        if (smsNotificationBanner != null) {
          Surface(
            color = Color(0xFFFEF3C7),
            border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Sms, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text("Notificação de SMS Recebida", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF78350F))
                Text(smsNotificationBanner, fontSize = 10.sp, color = Color(0xFF92400E))
              }
            }
          }
        }

        OutlinedTextField(
          value = smsCode,
          onValueChange = { if (it.length <= 6) smsCode = it.filter { c -> c.isDigit() } },
          label = { Text("Código de 6 dígitos") },
          placeholder = { Text("000000") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_sms_code")
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Não recebeu o código?",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          TextButton(onClick = onResendCode) {
            Text("Reenviar SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (smsCode.length >= 4) {
            onConfirmCode(smsCode)
          }
        },
        enabled = smsCode.length >= 4,
        modifier = Modifier.testTag("btn_confirm_sms_auth")
      ) {
        Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Autenticar & Entrar")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    }
  )
}

@Composable
fun ServiceRatingDialog(
  service: ServiceRequest,
  onDismiss: () -> Unit,
  onSubmit: (rating: Float, comment: String, complimentTags: List<String>) -> Unit
) {
  var rating by remember { mutableStateOf(5.0f) }
  var comment by remember { mutableStateOf("") }
  var selectedTags by remember { mutableStateOf(setOf("Pontual", "Preço Justo")) }

  val availableTags = listOf(
    "Pontual",
    "Preço Justo",
    "Caprichoso",
    "Limpeza Impecável",
    "Profissional Educado",
    "Trabalho Rápido",
    "Super Recomendo"
  )

  val sentimentLabel = when (rating.toInt()) {
    1 -> "Ruim"
    2 -> "Regular"
    3 -> "Bom"
    4 -> "Muito Bom"
    else -> "Excelente! Recomendo muito"
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Star,
        contentDescription = null,
        tint = WarningAmber,
        modifier = Modifier.size(36.dp)
      )
    },
    title = {
      Text(
        text = "Avaliar Atendimento",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Prestador: ${service.acceptedProviderName ?: "Profissional"}",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = service.title,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )

        // 5 Interactive Stars
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          (1..5).forEach { star ->
            val isFilled = star <= rating
            IconButton(
              onClick = { rating = star.toFloat() },
              modifier = Modifier.testTag("star_rate_$star")
            ) {
              Icon(
                imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "$star estrelas",
                tint = if (isFilled) WarningAmber else Color.LightGray,
                modifier = Modifier.size(36.dp)
              )
            }
          }
        }

        // Sentiment dynamic text
        Text(
          text = sentimentLabel,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = WarningAmber
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Compliments chips
        Text(
          text = "Destaques do serviço:",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.align(Alignment.Start)
        )

        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          availableTags.chunked(3).forEach { rowTags ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              rowTags.forEach { tag ->
                val isSelected = selectedTags.contains(tag)
                Surface(
                  color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                  border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                  shape = RoundedCornerShape(16.dp),
                  modifier = Modifier
                    .weight(1f)
                    .clickable {
                      selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag
                    }
                ) {
                  Box(
                    modifier = Modifier.padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = tag,
                      style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                      ),
                      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                  }
                }
              }
            }
          }
        }

        OutlinedTextField(
          value = comment,
          onValueChange = { comment = it },
          label = { Text("Comentário sobre o serviço") },
          placeholder = { Text("Conte como foi a execução, agilidade e resultado...") },
          minLines = 3,
          maxLines = 5,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_rating_comment")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSubmit(rating, comment.ifBlank { sentimentLabel }, selectedTags.toList())
        },
        modifier = Modifier.testTag("btn_submit_rating")
      ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Enviar Avaliação")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    }
  )
}

@Composable
fun ReviewItemCard(
  review: ServiceReview,
  canReply: Boolean = false,
  onReply: (String) -> Unit = {}
) {
  var showReplyField by remember { mutableStateOf(false) }
  var replyText by remember { mutableStateOf("") }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(12.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = review.clientName,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
          Text(
            text = "Serviço: ${review.serviceTitle}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Stars badge
        Surface(
          color = Color(0xFFFEF3C7),
          shape = RoundedCornerShape(8.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text(
              text = "${review.rating}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = Color(0xFF78350F)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Compliment tags
      if (review.complimentTags.isNotEmpty()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          review.complimentTags.take(3).forEach { tag ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = "✓ $tag",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
      }

      // Review comment
      Text(
        text = "\"${review.comment}\"",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface
      )

      // Provider Reply if present
      if (!review.providerReply.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            Text(
              text = "Resposta do Prestador:",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = EmeraldWallet
            )
            Text(
              text = review.providerReply,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      } else if (canReply) {
        if (!showReplyField) {
          TextButton(
            onClick = { showReplyField = true },
            modifier = Modifier.align(Alignment.End)
          ) {
            Text("Responder Avaliação", fontSize = 11.sp)
          }
        } else {
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = replyText,
              onValueChange = { replyText = it },
              placeholder = { Text("Agradeça o cliente...") },
              modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                if (replyText.isNotBlank()) {
                  onReply(replyText)
                  showReplyField = false
                }
              }
            ) {
              Text("Enviar")
            }
          }
        }
      }
    }
  }
}

@Composable
fun EditServiceBudgetDialog(
  service: ServiceRequest,
  onDismiss: () -> Unit,
  onConfirm: (Double) -> Unit
) {
  var budgetText by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", service.proposedBudget)) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Edit,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp)
      )
    },
    title = {
      Text(
        text = "Mudar Valor Proposto",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = service.title,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = "Defina um novo valor proposto para o serviço. Prestadores verão a alteração ao enviar ou negociar propostas.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
          value = budgetText,
          onValueChange = { budgetText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
          label = { Text("Novo Orçamento Proposto (R$)") },
          placeholder = { Text("Ex: 350.00") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_edit_budget")
        )
        val parsed = budgetText.replace(",", ".").toDoubleOrNull() ?: service.proposedBudget
        val fee = parsed * 0.10
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Taxa estimada da plataforma (10%): R$ ${String.format(java.util.Locale.US, "%.2f", fee)} (debitada do prestador ao aceitar)",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val newBudget = budgetText.replace(",", ".").toDoubleOrNull()
          if (newBudget != null && newBudget > 0) {
            onConfirm(newBudget)
          }
        },
        modifier = Modifier.testTag("btn_confirm_edit_budget")
      ) {
        Text("Atualizar Valor")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    }
  )
}

@Composable
fun NotificationsDialog(
  notifications: List<AppNotification>,
  onDismiss: () -> Unit,
  onMarkRead: (String) -> Unit,
  onOpenService: (String) -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Notifications,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(36.dp)
      )
    },
    title = {
      Text(
        text = "Notificações do Prestador",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Novos pedidos de clientes e alertas de aprovação de documentos em tempo real:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (notifications.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(140.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Nenhuma notificação no momento.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(notifications, key = { it.id }) { notif ->
              Card(
                colors = CardDefaults.cardColors(
                  containerColor = if (!notif.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(10.dp),
                border = if (!notif.isRead) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    onMarkRead(notif.id)
                    if (notif.serviceId != null) {
                      onOpenService(notif.serviceId)
                      onDismiss()
                    }
                  }
                  .testTag("notification_item_${notif.id}")
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = notif.title,
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.primary
                    )
                    if (!notif.isRead) {
                      Surface(color = DangerRed, shape = CircleShape) {
                        Box(modifier = Modifier.size(8.dp))
                      }
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = notif.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  if (notif.serviceId != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      text = "👉 Toque para abrir este serviço",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = EmeraldWallet
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Fechar")
      }
    }
  )
}

@Composable
fun AdminReviewDialog(
  users: List<User>,
  onDismiss: () -> Unit,
  onApprove: (String) -> Unit,
  onReject: (String, String) -> Unit
) {
  val pendingProviders = users.filter {
    it.role == UserRole.PRESTADOR && it.verificationStatus == ProviderVerificationStatus.PENDENTE_ANALISE
  }
  val approvedCount = users.count { it.role == UserRole.PRESTADOR && it.verificationStatus == ProviderVerificationStatus.APROVADO }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Security,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(36.dp)
      )
    },
    title = {
      Text(
        text = "Administração ResolveAi",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Análise de Documentos de Prestadores (RG/CNH, Antecedentes e Selfie):",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Surface(
            color = WarningAmberLight,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Text("${pendingProviders.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = WarningAmber)
              Text("Pendentes", fontSize = 10.sp, color = Color(0xFF78350F))
            }
          }
          Surface(
            color = EmeraldWalletLight,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Text("$approvedCount", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EmeraldWallet)
              Text("Aprovados", fontSize = 10.sp, color = Color(0xFF065F46))
            }
          }
        }

        if (pendingProviders.isEmpty()) {
          Surface(
            color = Color(0xFFDCFCE7),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldWallet, modifier = Modifier.size(28.dp))
              Spacer(modifier = Modifier.height(6.dp))
              Text("Nenhum cadastro pendente!", fontWeight = FontWeight.Bold, color = EmeraldWallet, fontSize = 12.sp)
              Text("Todos os documentos de prestadores foram aprovados com sucesso.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(300.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(pendingProviders, key = { it.id }) { provider ->
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber),
                modifier = Modifier.fillMaxWidth().testTag("admin_pending_provider_${provider.id}")
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(provider.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Surface(color = WarningAmberLight, shape = RoundedCornerShape(6.dp)) {
                      Text("Pendente", color = WarningAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                  }
                  Text("Especialidade: ${provider.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                  Text("Celular SMS: ${provider.phone} • ${provider.city}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                  // Documents submitted
                  val docs = provider.documents
                  Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      Text("📋 Documentação Anexada:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      Text("• RG/CNH: ${docs?.rgOrCnhNumber ?: provider.document} (${docs?.rgOrCnhFileName ?: "cnh.pdf"})", fontSize = 10.sp)
                      Text("• Antecedentes: ${docs?.criminalRecordCertNumber ?: "CERT-PF"} (${docs?.criminalRecordFileName ?: "certidao.pdf"})", fontSize = 10.sp)
                      Text("• Foto Selfie com Documento: ${docs?.selfieFileName ?: "selfie.jpg"} [Conferida ✓]", fontSize = 10.sp, color = EmeraldWallet, fontWeight = FontWeight.SemiBold)
                    }
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    OutlinedButton(
                      onClick = { onReject(provider.id, "Documento com foto ilegível") },
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                      modifier = Modifier.weight(1f)
                    ) {
                      Text("Reprovar", fontSize = 11.sp)
                    }
                    Button(
                      onClick = { onApprove(provider.id) },
                      colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
                      modifier = Modifier.weight(1f).testTag("btn_approve_provider_${provider.id}")
                    ) {
                      Text("Aprovar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Fechar")
      }
    }
  )
}

@Composable
fun ProviderPendingApprovalScreen(
  currentUser: User,
  onSwitchUserClick: () -> Unit,
  onFastApproveClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
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
          imageVector = Icons.Default.Security,
          contentDescription = null,
          tint = WarningAmber,
          modifier = Modifier.size(40.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "Cadastro em Análise de Segurança",
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Olá, ${currentUser.name}! Seus documentos pessoais (RG/CNH, Certidão de Antecedentes Criminais e Selfie) estão sob análise da equipe de administração do ResolveAi.",
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(16.dp))

    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Documentos Enviados:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("• Documento: ${currentUser.documents?.rgOrCnhNumber ?: currentUser.document}", fontSize = 11.sp)
        Text("• Certidão: ${currentUser.documents?.criminalRecordCertNumber ?: "Pendente"}", fontSize = 11.sp)
        Text("• Foto Selfie com documento anexada ✓", fontSize = 11.sp, color = EmeraldWallet, fontWeight = FontWeight.SemiBold)
        Text("• Telefone verificado por SMS: ${currentUser.phone} ✓", fontSize = 11.sp, color = InfoBlue, fontWeight = FontWeight.SemiBold)
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Button(
      onClick = onFastApproveClick,
      colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.fillMaxWidth().testTag("btn_fast_approve_test")
    ) {
      Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("Aprovar como Administrador (Teste Rápido)", fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(
      onClick = onSwitchUserClick,
      modifier = Modifier.fillMaxWidth()
    ) {
      Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("Trocar de Usuário / Acessar como Administrador")
    }
  }
}


