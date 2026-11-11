package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TransactionType
import com.example.model.User
import com.example.model.UserRole
import com.example.model.WalletTransaction
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WalletProfileScreen(
  currentUser: User,
  walletTransactions: List<WalletTransaction>,
  onRechargeClick: () -> Unit,
  onSwitchUserClick: () -> Unit,
  onEditProfileClick: () -> Unit,
  onSaveProfile: (User) -> Unit
) {
  val userTransactions = walletTransactions.filter { it.providerId == currentUser.id }
  val totalFeesPaid = userTransactions
    .filter { it.type == TransactionType.DEBITO_TAXA_10 }
    .sumOf { it.amount }

  var showEditDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
  ) {
    // 1. Digital Wallet Card (Highlighted for providers, or showing role info)
    item {
      if (currentUser.role == UserRole.PRESTADOR) {
        ProviderWalletCard(
          balance = currentUser.walletBalance,
          totalFeesPaid = totalFeesPaid,
          onRechargeClick = onRechargeClick
        )
      } else {
        ClientOverviewCard(
          currentUser = currentUser,
          onSwitchToProviderClick = onSwitchUserClick
        )
      }
    }

    // 2. User Profile / Registration Card
    item {
      UserProfileCard(
        user = currentUser,
        onEditClick = { showEditDialog = true },
        onSwitchClick = onSwitchUserClick
      )
    }

    // App Branding & Logo Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Image(
            painter = painterResource(id = R.drawable.resolveai_logo),
            contentDescription = "Logo ResolveAi Serviços",
            modifier = Modifier
              .size(46.dp)
              .clip(RoundedCornerShape(10.dp))
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "ResolveAi Serviços",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Negociação transparente, carteira integrada com taxa de 10% e chat seguro.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // 3. Wallet Transactions Statement (Extrato) if Provider
    if (currentUser.role == UserRole.PRESTADOR) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Receipt,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Extrato da Carteira (${userTransactions.size})",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          }

          TextButton(onClick = onRechargeClick) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text("Recarregar Pix")
          }
        }
      }

      if (userTransactions.isEmpty()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Nenhuma movimentação registrada na carteira ainda.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      } else {
        items(userTransactions, key = { it.id }) { tx ->
          TransactionItemCard(transaction = tx)
        }
      }
    }
  }

  // Edit Profile Dialog
  if (showEditDialog) {
    EditProfileDialog(
      currentUser = currentUser,
      onDismiss = { showEditDialog = false },
      onSave = { updated ->
        onSaveProfile(updated)
        showEditDialog = false
      }
    )
  }
}

@Composable
private fun ProviderWalletCard(
  balance: Double,
  totalFeesPaid: Double,
  onRechargeClick: () -> Unit
) {
  val brush = Brush.horizontalGradient(
    colors = listOf(Color(0xFF047857), Color(0xFF0D9488), Color(0xFF1E3A8A))
  )

  Card(
    shape = RoundedCornerShape(20.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("provider_wallet_card")
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(brush)
        .padding(20.dp)
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.AccountBalanceWallet,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Carteira Digital do Prestador",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = Color.White
            )
          }

          Surface(
            color = Color.White.copy(alpha = 0.2f),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text(
              text = "Taxa: 10% por serviço",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Saldo Disponível",
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.8f)
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom
        ) {
          Text(
            text = "R$ ${String.format("%.2f", balance)}",
            style = MaterialTheme.typography.headlineLarge.copy(
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = (-0.5).sp
            ),
            color = Color.White
          )

          Button(
            onClick = onRechargeClick,
            colors = ButtonDefaults.buttonColors(
              containerColor = Color.White,
              contentColor = Color(0xFF047857)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("btn_wallet_recharge")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Recarregar Pix", fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
          color = Color.Black.copy(alpha = 0.2f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Total de taxas pagas nos contratos:",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = Color.White.copy(alpha = 0.9f)
            )
            Text(
              text = "R$ ${String.format("%.2f", totalFeesPaid)}",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
              color = Color(0xFFFDE68A)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ClientOverviewCard(
  currentUser: User,
  onSwitchToProviderClick: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Perfil de Cliente Ativo",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Você pode publicar solicitações e negociar propostas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(
              text = "Quer atuar como Prestador?",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "Alterne para ver a carteira digital e receber 100% dos seus pedidos pagando só 10% de taxa.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Button(
            onClick = onSwitchToProviderClick,
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("Alternar", fontSize = 11.sp)
          }
        }
      }
    }
  }
}

@Composable
private fun UserProfileCard(
  user: User,
  onEditClick: () -> Unit,
  onSwitchClick: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(52.dp)
              .clip(CircleShape)
              .background(Color(user.avatarColorHex)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = user.name.take(2).uppercase(),
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = user.name,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = if (user.role == UserRole.PRESTADOR) user.category else "Cliente Solicitante",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text(
                text = "${user.rating} (${user.completedJobs} serviços)",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
              )
            }
          }
        }

        IconButton(onClick = onEditClick) {
          Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
        }
      }

      if (user.bio.isNotBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = user.bio,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Details grid
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          ProfileInfoRow(label = "E-mail de cadastro", value = user.email)
          ProfileInfoRow(label = "Telefone cadastrado", value = user.phone)
          ProfileInfoRow(label = "Cidade / Região", value = user.city)
          ProfileInfoRow(label = "Documento (CPF/CNPJ)", value = user.document)
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedButton(
        onClick = onSwitchClick,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("btn_switch_profile_bottom")
      ) {
        Icon(Icons.Default.SwapHoriz, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Trocar para Outro Usuário / Perfil")
      }
    }
  }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text = value.ifBlank { "Não informado" }, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
  }
}

@Composable
private fun TransactionItemCard(transaction: WalletTransaction) {
  val isDebit = transaction.type == TransactionType.DEBITO_TAXA_10
  val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
  val dateStr = remember(transaction.timestamp) { dateFormat.format(Date(transaction.timestamp)) }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(12.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(if (isDebit) DangerRedLight else EmeraldWalletLight),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (isDebit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
          contentDescription = null,
          tint = if (isDebit) DangerRed else EmeraldWallet,
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = transaction.description,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "$dateStr • Saldo após: R$ ${String.format("%.2f", transaction.balanceAfter)}",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Text(
        text = "${if (isDebit) "-" else "+"} R$ ${String.format("%.2f", transaction.amount)}",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          color = if (isDebit) DangerRed else EmeraldWallet
        )
      )
    }
  }
}

@Composable
private fun EditProfileDialog(
  currentUser: User,
  onDismiss: () -> Unit,
  onSave: (User) -> Unit
) {
  var name by remember { mutableStateOf(currentUser.name) }
  var category by remember { mutableStateOf(currentUser.category) }
  var bio by remember { mutableStateOf(currentUser.bio) }
  var city by remember { mutableStateOf(currentUser.city) }
  var phone by remember { mutableStateOf(currentUser.phone) }
  var email by remember { mutableStateOf(currentUser.email) }
  var document by remember { mutableStateOf(currentUser.document) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Editar Cadastro & Perfil", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nome Completo") },
          modifier = Modifier.fillMaxWidth()
        )

        if (currentUser.role == UserRole.PRESTADOR) {
          OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Especialidade / Categoria") },
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Apresentação / Bio") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
          )
        }

        OutlinedTextField(
          value = city,
          onValueChange = { city = it },
          label = { Text("Cidade / Estado") },
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Telefone / WhatsApp") },
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("E-mail") },
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = document,
          onValueChange = { document = it },
          label = { Text("CPF ou CNPJ") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            currentUser.copy(
              name = name,
              category = category,
              bio = bio,
              city = city,
              phone = phone,
              email = email,
              document = document
            )
          )
        }
      ) {
        Text("Salvar Alterações")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    }
  )
}
