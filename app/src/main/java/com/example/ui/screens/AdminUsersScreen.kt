package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import android.widget.Toast
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.FirebaseAuthHelper
import com.example.data.LocationAutocompleteMode
import com.example.model.ProviderVerificationStatus
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.components.GooglePlacesAutocompleteField
import com.example.ui.components.ProfileAvatar
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryVariant
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight

private enum class AdminUserRoleFilter(val label: String) {
  TODOS("Todos"),
  CLIENTES("Clientes"),
  PRESTADORES("Prestadores"),
  ADMINS("Admins")
}

@Composable
fun AdminUsersScreen(
  currentUser: User,
  users: List<User>,
  isLoading: Boolean,
  syncStatusMessage: String?,
  onSyncUsersClick: () -> Unit,
  onUpdateUser: (User) -> Result<Unit>,
  onAddProviderBalance: (providerId: String, amount: Double, reason: String) -> Result<Double> = { _, _, _ ->
    Result.success(0.0)
  },
  onChangeUserPassword: (userId: String, newPassword: String) -> Result<Unit> = { _, _ ->
    Result.success(Unit)
  },
  onSendPasswordResetEmail: (User) -> Unit = {},
  onDeleteUser: (String) -> Result<Unit>,
  onAddUserClick: () -> Unit = {},
  onOpenDocumentsReview: () -> Unit = {},
  onOpenCatalogAndEdits: () -> Unit = {},
  availableSpecialties: List<String> = emptyList(),
  userPasswords: Map<String, String> = emptyMap(),
  onOpenMenu: () -> Unit,
  onBackToHome: () -> Unit
) {
  if (currentUser.role != UserRole.ADMIN) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 520.dp)
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Surface(
            color = DangerRedLight,
            shape = CircleShape,
            modifier = Modifier.size(68.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Acesso restrito",
                tint = DangerRed,
                modifier = Modifier.size(34.dp)
              )
            }
          }
          Text(
            text = "Acesso Restrito ao Administrador",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
          )
          Text(
            text = "Somente administradores podem visualizar, editar ou excluir contas de usuários da plataforma.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
          Button(
            onClick = onBackToHome,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
          ) {
            Text("Voltar para Serviços", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
    return
  }

  BackHandler { onBackToHome() }

  var searchQuery by rememberSaveable { mutableStateOf("") }
  var selectedFilter by rememberSaveable { mutableStateOf(AdminUserRoleFilter.TODOS) }
  var editingUser by remember { mutableStateOf<User?>(null) }
  var addBalanceUser by remember { mutableStateOf<User?>(null) }
  var changePasswordUser by remember { mutableStateOf<User?>(null) }
  var deletingUser by remember { mutableStateOf<User?>(null) }

  val clientsCount = remember(users) { users.count { it.role == UserRole.CLIENTE } }
  val providersCount = remember(users) { users.count { it.role == UserRole.PRESTADOR } }
  val adminsCount = remember(users) { users.count { it.role == UserRole.ADMIN } }

  val filteredUsers = remember(users, searchQuery, selectedFilter) {
    val q = searchQuery.trim().lowercase()
    users.filter { user ->
      val matchesRole = when (selectedFilter) {
        AdminUserRoleFilter.TODOS -> true
        AdminUserRoleFilter.CLIENTES -> user.role == UserRole.CLIENTE
        AdminUserRoleFilter.PRESTADORES -> user.role == UserRole.PRESTADOR
        AdminUserRoleFilter.ADMINS -> user.role == UserRole.ADMIN
      }
      val matchesQuery = q.isBlank() ||
        user.name.lowercase().contains(q) ||
        user.email.lowercase().contains(q) ||
        user.phone.lowercase().contains(q) ||
        user.city.lowercase().contains(q) ||
        user.document.lowercase().contains(q) ||
        user.category.lowercase().contains(q)
      matchesRole && matchesQuery
    }
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    val isWide = maxWidth >= 720.dp

    Column(modifier = Modifier.fillMaxSize()) {
      AdminUsersHeader(
        adminUser = currentUser,
        isLoading = isLoading,
        syncStatusMessage = syncStatusMessage,
        onMenuClick = onOpenMenu,
        onSyncClick = onSyncUsersClick
      )

      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = if (isWide) 24.dp else 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 960.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar por nome, e-mail, telefone, cidade ou CPF/CNPJ...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar usuários") },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(
                  onClick = { searchQuery = "" },
                  modifier = Modifier.testTag("btn_admin_users_clear_search")
                ) {
                  Icon(Icons.Default.Close, contentDescription = "Limpar busca")
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_admin_users_search")
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            AdminUserFilterChip(
              label = "Todos (${users.size})",
              selected = selectedFilter == AdminUserRoleFilter.TODOS,
              onClick = { selectedFilter = AdminUserRoleFilter.TODOS },
              testTag = "filter_users_todos"
            )
            AdminUserFilterChip(
              label = "Clientes ($clientsCount)",
              selected = selectedFilter == AdminUserRoleFilter.CLIENTES,
              onClick = { selectedFilter = AdminUserRoleFilter.CLIENTES },
              testTag = "filter_users_cliente"
            )
            AdminUserFilterChip(
              label = "Prestadores ($providersCount)",
              selected = selectedFilter == AdminUserRoleFilter.PRESTADORES,
              onClick = { selectedFilter = AdminUserRoleFilter.PRESTADORES },
              testTag = "filter_users_prestador"
            )
            AdminUserFilterChip(
              label = "Admins ($adminsCount)",
              selected = selectedFilter == AdminUserRoleFilter.ADMINS,
              onClick = { selectedFilter = AdminUserRoleFilter.ADMINS },
              testTag = "filter_users_admin"
            )
          }

          if (filteredUsers.isEmpty()) {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Group,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(40.dp)
                )
                Text(
                  text = "Nenhum usuário encontrado",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                  text = "Ajuste os filtros ou a busca para visualizar os usuários cadastrados.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier
                .fillMaxSize()
                .testTag("admin_users_list"),
              verticalArrangement = Arrangement.spacedBy(10.dp),
              contentPadding = PaddingValues(bottom = 24.dp)
            ) {
              item(key = "admin_users_overview_bar") {
                AdminUsersOverviewCard(
                  totalCount = users.size,
                  clientsCount = clientsCount,
                  providersCount = providersCount,
                  adminsCount = adminsCount,
                  onOpenDocsClick = onOpenDocumentsReview,
                  onOpenCatalogAndEdits = onOpenCatalogAndEdits,
                  onAddUserClick = onAddUserClick
                )
              }

              items(filteredUsers, key = { it.id }) { user ->
                val isCurrentSession = user.id == currentUser.id
                val isOnlyAdmin = user.role == UserRole.ADMIN && adminsCount <= 1
                val savedPassword = userPasswords[user.id] ?: user.firebaseUid?.let { userPasswords[it] }
                AdminUserManagementCard(
                  user = user,
                  savedPassword = savedPassword,
                  isCurrentSession = isCurrentSession,
                  canDelete = !isCurrentSession && !isOnlyAdmin,
                  onEditClick = { editingUser = user },
                  onAddBalanceClick = { addBalanceUser = user },
                  onChangePasswordClick = { changePasswordUser = user },
                  onDeleteClick = { deletingUser = user }
                )
              }
            }
          }
        }
      }
    }
  }

  editingUser?.let { target ->
    AdminEditUserDialog(
      user = target,
      isCurrentSession = target.id == currentUser.id,
      isOnlyAdmin = target.role == UserRole.ADMIN && adminsCount <= 1,
      availableSpecialties = availableSpecialties,
      onDismiss = { editingUser = null },
      onSave = { updated ->
        val res = onUpdateUser(updated)
        if (res.isSuccess) {
          editingUser = null
        }
        res
      }
    )
  }

  addBalanceUser?.let { target ->
    AdminAddProviderBalanceDialog(
      provider = target,
      onDismiss = { addBalanceUser = null },
      onConfirm = { amount, reason ->
        val res = onAddProviderBalance(target.id, amount, reason)
        if (res.isSuccess) {
          addBalanceUser = null
        }
        res
      }
    )
  }

  changePasswordUser?.let { target ->
    val targetSavedPassword = userPasswords[target.id] ?: target.firebaseUid?.let { userPasswords[it] }
    AdminChangePasswordDialog(
      user = target,
      currentSavedPassword = targetSavedPassword,
      onDismiss = { changePasswordUser = null },
      onConfirmChangePassword = { newPassword ->
        val res = onChangeUserPassword(target.id, newPassword)
        if (res.isSuccess) {
          changePasswordUser = null
        }
        res
      },
      onSendPasswordResetEmail = {
        onSendPasswordResetEmail(target)
        changePasswordUser = null
      }
    )
  }

  deletingUser?.let { target ->
    AdminDeleteUserDialog(
      user = target,
      isCurrentSession = target.id == currentUser.id,
      isOnlyAdmin = target.role == UserRole.ADMIN && adminsCount <= 1,
      onDismiss = { deletingUser = null },
      onConfirmDelete = {
        val res = onDeleteUser(target.id)
        if (res.isSuccess) {
          deletingUser = null
        }
        res
      }
    )
  }
}

@Composable
private fun AdminUsersHeader(
  adminUser: User,
  isLoading: Boolean,
  syncStatusMessage: String?,
  onMenuClick: () -> Unit,
  onSyncClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        Brush.linearGradient(
          colors = listOf(Color(0xFF0F172A), BrandPrimaryVariant, BrandPrimary)
        )
      )
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          IconButton(
            onClick = onMenuClick,
            modifier = Modifier
              .size(48.dp)
              .testTag("btn_admin_users_menu")
          ) {
            Icon(
              imageVector = Icons.Default.Menu,
              contentDescription = "Abrir menu",
              tint = Color.White
            )
          }
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = Color(0xFF93C5FD),
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "PAINEL EXCLUSIVO ADMIN",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = 0.8.sp
                ),
                color = Color(0xFF93C5FD)
              )
            }
            Text(
              text = "Gerenciar Usuários",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${adminUser.name} • Edição e exclusão de contas",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = Color.White.copy(alpha = 0.78f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        OutlinedButton(
          onClick = onSyncClick,
          enabled = !isLoading,
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("btn_admin_users_sync")
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              color = Color.White,
              strokeWidth = 2.dp,
              modifier = Modifier.size(16.dp)
            )
          } else {
            Icon(Icons.Default.CloudSync, contentDescription = "Sincronizar usuários", modifier = Modifier.size(17.dp))
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text("Sincronizar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }

      if (!syncStatusMessage.isNullOrBlank()) {
        Surface(
          color = Color.White.copy(alpha = 0.12f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.CloudDone,
              contentDescription = null,
              tint = Color(0xFF6EE7B7),
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = syncStatusMessage,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = Color.White,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminUsersOverviewCard(
  totalCount: Int,
  clientsCount: Int,
  providersCount: Int,
  adminsCount: Int,
  onOpenDocsClick: () -> Unit,
  onOpenCatalogAndEdits: () -> Unit = {},
  onAddUserClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        AdminUserKpiPill(
          count = totalCount,
          label = "Total",
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
          accentColor = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f)
        )
        AdminUserKpiPill(
          count = clientsCount,
          label = "Clientes",
          containerColor = InfoBlueLight,
          accentColor = InfoBlue,
          modifier = Modifier.weight(1f)
        )
        AdminUserKpiPill(
          count = providersCount,
          label = "Prestadores",
          containerColor = EmeraldWalletLight,
          accentColor = EmeraldWallet,
          modifier = Modifier.weight(1f)
        )
        AdminUserKpiPill(
          count = adminsCount,
          label = "Admins",
          containerColor = WarningAmberLight,
          accentColor = WarningAmber,
          modifier = Modifier.weight(1f)
        )
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onOpenDocsClick,
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("btn_admin_go_docs")
        ) {
          Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Análise Documental", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
          onClick = onOpenCatalogAndEdits,
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("btn_admin_users_open_catalog_edits")
        ) {
          Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Categorias e Edições", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Button(
          onClick = onAddUserClick,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet, contentColor = Color.White),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("btn_admin_add_user")
        ) {
          Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Novo Usuário", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun AdminUserKpiPill(
  count: Int,
  label: String,
  containerColor: Color,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    color = containerColor,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "$count",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
        color = accentColor
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
        color = accentColor.copy(alpha = 0.9f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun AdminUserFilterChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String
) {
  FilterChip(
    selected = selected,
    onClick = onClick,
    label = {
      Text(
        text = label,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        fontSize = 12.sp
      )
    },
    colors = FilterChipDefaults.filterChipColors(
      selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
      selectedLabelColor = MaterialTheme.colorScheme.primary
    ),
    modifier = Modifier
      .heightIn(min = 40.dp)
      .testTag(testTag)
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminUserManagementCard(
  user: User,
  savedPassword: String? = null,
  isCurrentSession: Boolean,
  canDelete: Boolean,
  onEditClick: () -> Unit,
  onAddBalanceClick: () -> Unit = {},
  onChangePasswordClick: () -> Unit = {},
  onDeleteClick: () -> Unit
) {
  var isPasswordVisible by remember(user.id) { mutableStateOf(false) }
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current
  val copiedFeedback = stringResource(R.string.admin_user_password_copied)
  val roleColors = when (user.role) {
    UserRole.CLIENTE -> InfoBlueLight to InfoBlue
    UserRole.PRESTADOR -> EmeraldWalletLight to EmeraldWallet
    UserRole.ADMIN -> WarningAmberLight to WarningAmber
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("admin_user_card_${user.id}")
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        ProfileAvatar(
          name = user.name,
          photoUrl = user.photoUrl,
          avatarColorHex = user.avatarColorHex,
          size = 48.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = user.name,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false)
            )
            if (isCurrentSession) {
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = "Você",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Surface(
              color = roleColors.first,
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = user.role.label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                color = roleColors.second,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }

            if (user.role == UserRole.PRESTADOR) {
              val statusStyle = when (user.verificationStatus) {
                ProviderVerificationStatus.APROVADO -> EmeraldWalletLight to EmeraldWallet
                ProviderVerificationStatus.PENDENTE_ANALISE -> WarningAmberLight to WarningAmber
                ProviderVerificationStatus.REJEITADO -> DangerRedLight to DangerRed
              }
              Surface(
                color = statusStyle.first,
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = user.verificationStatus.label,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                  color = statusStyle.second,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (user.email.isNotBlank()) {
          UserInfoMetaRow(icon = Icons.Default.Email, text = user.email)
        }
        UserInfoMetaRow(icon = Icons.Default.Phone, text = user.phone)
        UserInfoMetaRow(icon = Icons.Default.LocationOn, text = user.city.ifBlank { "Não informada" })
        if (user.document.isNotBlank()) {
          UserInfoMetaRow(icon = Icons.Default.Badge, text = "Doc: ${user.document}")
        }
        if (user.role == UserRole.PRESTADOR) {
          if (user.category.isNotBlank()) {
            UserInfoMetaRow(icon = Icons.Default.Work, text = user.category)
          }
          UserInfoMetaRow(
            icon = Icons.Default.AccountBalanceWallet,
            text = "Saldo: R$ ${String.format("%.2f", user.walletBalance)} • ⭐ ${user.rating}"
          )
        }
      }

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("admin_user_password_row_${user.id}")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = stringResource(R.string.admin_user_password_label),
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val passwordText = savedPassword?.takeIf { it.isNotBlank() }
            if (passwordText != null) {
              Text(
                text = if (isPasswordVisible) passwordText else "••••••••",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("admin_user_password_value_${user.id}")
              )
            } else {
              Text(
                text = stringResource(R.string.admin_user_password_not_set),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("admin_user_password_empty_${user.id}")
              )
            }
          }

          val passwordToShow = savedPassword?.takeIf { it.isNotBlank() }
          if (passwordToShow != null) {
            IconButton(
              onClick = { isPasswordVisible = !isPasswordVisible },
              modifier = Modifier
                .size(48.dp)
                .testTag("btn_admin_toggle_password_${user.id}")
            ) {
              Icon(
                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (isPasswordVisible) {
                  stringResource(R.string.admin_user_password_hide_cd)
                } else {
                  stringResource(R.string.admin_user_password_show_cd)
                },
                tint = MaterialTheme.colorScheme.primary
              )
            }
            IconButton(
              onClick = {
                clipboardManager.setText(AnnotatedString(passwordToShow))
                Toast.makeText(context, copiedFeedback, Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier
                .size(48.dp)
                .testTag("btn_admin_copy_password_${user.id}")
            ) {
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = stringResource(R.string.admin_user_password_copy_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      if (user.role == UserRole.PRESTADOR) {
        Button(
          onClick = onAddBalanceClick,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = EmeraldWallet,
            contentColor = Color.White
          ),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("btn_admin_add_balance_${user.id}")
        ) {
          Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Adicionar Saldo Manual", fontWeight = FontWeight.Bold)
        }
      }

      OutlinedButton(
        onClick = onChangePasswordClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("btn_admin_change_password_${user.id}")
      ) {
        Icon(
          imageVector = Icons.Default.LockReset,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.admin_change_password_cta), fontWeight = FontWeight.Bold)
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = onEditClick,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag("btn_admin_edit_user_${user.id}")
        ) {
          Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Editar", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = onDeleteClick,
          enabled = canDelete,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(
            1.dp,
            if (canDelete) DangerRed.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant
          ),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = DangerRed,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
          ),
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .testTag("btn_admin_delete_user_${user.id}")
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (isCurrentSession) "Em Uso" else "Excluir", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun UserInfoMetaRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(15.dp)
    )
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminEditUserDialog(
  user: User,
  isCurrentSession: Boolean,
  isOnlyAdmin: Boolean,
  availableSpecialties: List<String> = emptyList(),
  onDismiss: () -> Unit,
  onSave: (User) -> Result<Unit>
) {
  var name by remember(user.id) { mutableStateOf(user.name) }
  var email by remember(user.id) { mutableStateOf(user.email) }
  var phone by remember(user.id) { mutableStateOf(user.phone) }
  var city by remember(user.id) { mutableStateOf(user.city) }
  var document by remember(user.id) { mutableStateOf(user.document) }
  var role by remember(user.id) { mutableStateOf(user.role) }
  var category by remember(user.id) { mutableStateOf(user.category.ifBlank { "Eletricista Residencial & Predial" }) }
  var bio by remember(user.id) { mutableStateOf(user.bio) }
  var walletText by remember(user.id) { mutableStateOf(String.format(java.util.Locale.US, "%.2f", user.walletBalance)) }
  var verificationStatus by remember(user.id) { mutableStateOf(user.verificationStatus) }
  var errorMessage by remember(user.id) { mutableStateOf<String?>(null) }
  var catExpanded by remember { mutableStateOf(false) }

  val categories = remember(availableSpecialties) {
    if (availableSpecialties.isNotEmpty()) {
      availableSpecialties.distinct()
    } else {
      listOf(
        "Cabeleireiro",
        "Manicure",
        "Eletricista Residencial & Predial",
        "Hidráulica & Encanador",
        "Pintura & Acabamento",
        "TI, Redes & Informática",
        "Limpeza & Diarista",
        "Montagem de Móveis",
        "Reformas & Alvenaria",
        "Ar Condicionado & Refrigeração"
      )
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp)
      .widthIn(max = 520.dp),
    icon = {
      Icon(
        imageVector = Icons.Default.Edit,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(30.dp)
      )
    },
    title = {
      Text(
        text = "Editar Usuário",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 460.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Perfil de Acesso",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val canDemoteAdmin = !isCurrentSession && !isOnlyAdmin
          FilterChip(
            selected = role == UserRole.CLIENTE,
            onClick = {
              if (user.role == UserRole.ADMIN && !canDemoteAdmin) {
                errorMessage = if (isCurrentSession) {
                  "Não é possível alterar o perfil do administrador em uso."
                } else {
                  "Não é possível remover o único administrador do sistema."
                }
              } else {
                errorMessage = null
                role = UserRole.CLIENTE
              }
            },
            label = { Text("Cliente") },
            modifier = Modifier.testTag("edit_user_role_cliente")
          )
          FilterChip(
            selected = role == UserRole.PRESTADOR,
            onClick = {
              if (user.role == UserRole.ADMIN && !canDemoteAdmin) {
                errorMessage = if (isCurrentSession) {
                  "Não é possível alterar o perfil do administrador em uso."
                } else {
                  "Não é possível remover o único administrador do sistema."
                }
              } else {
                errorMessage = null
                role = UserRole.PRESTADOR
              }
            },
            label = { Text("Prestador") },
            modifier = Modifier.testTag("edit_user_role_prestador")
          )
          FilterChip(
            selected = role == UserRole.ADMIN,
            onClick = {
              errorMessage = null
              role = UserRole.ADMIN
            },
            label = { Text("Admin") },
            modifier = Modifier.testTag("edit_user_role_admin")
          )
        }

        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            errorMessage = null
          },
          label = { Text("Nome Completo *") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_edit_user_name")
        )

        OutlinedTextField(
          value = email,
          onValueChange = {
            email = it
            errorMessage = null
          },
          label = { Text("E-mail") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_edit_user_email")
        )

        OutlinedTextField(
          value = phone,
          onValueChange = {
            phone = it
            errorMessage = null
          },
          label = { Text("Telefone com DDD *") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_edit_user_phone")
        )

        GooglePlacesAutocompleteField(
          value = city,
          onValueChange = { city = it },
          mode = LocationAutocompleteMode.CITY,
          label = "Cidade / UF",
          shape = RoundedCornerShape(10.dp),
          fieldTestTag = "input_edit_user_city",
          autoDetectCurrentCity = false
        )

        OutlinedTextField(
          value = document,
          onValueChange = { document = it },
          label = { Text("CPF / CNPJ") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_edit_user_document")
        )

        if (role == UserRole.PRESTADOR) {
          ExposedDropdownMenuBox(
            expanded = catExpanded,
            onExpandedChange = { catExpanded = it }
          ) {
            OutlinedTextField(
              value = category,
              onValueChange = { category = it },
              label = { Text("Categoria / Especialidade") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
              singleLine = true,
              modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                .fillMaxWidth()
                .testTag("input_edit_user_category")
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
            value = walletText,
            onValueChange = { raw ->
              walletText = raw.filter { c -> c.isDigit() || c == '.' || c == ',' }
              errorMessage = null
            },
            label = { Text("Saldo da Carteira (R$)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_edit_user_wallet")
          )

          Text(
            text = "Status de Verificação",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = verificationStatus == ProviderVerificationStatus.APROVADO,
              onClick = { verificationStatus = ProviderVerificationStatus.APROVADO },
              label = { Text("Aprovado") },
              modifier = Modifier.testTag("edit_user_status_aprovado")
            )
            FilterChip(
              selected = verificationStatus == ProviderVerificationStatus.PENDENTE_ANALISE,
              onClick = { verificationStatus = ProviderVerificationStatus.PENDENTE_ANALISE },
              label = { Text("Em Análise") },
              modifier = Modifier.testTag("edit_user_status_pendente")
            )
            FilterChip(
              selected = verificationStatus == ProviderVerificationStatus.REJEITADO,
              onClick = { verificationStatus = ProviderVerificationStatus.REJEITADO },
              label = { Text("Reprovado") },
              modifier = Modifier.testTag("edit_user_status_rejeitado")
            )
          }

          OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio / Experiência") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_edit_user_bio")
          )
        }

        if (!errorMessage.isNullOrBlank()) {
          Surface(
            color = DangerRedLight,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("admin_edit_user_error")
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
              Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = DangerRed
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsedWallet = if (role == UserRole.PRESTADOR) {
            walletText.replace(",", ".").toDoubleOrNull()
          } else {
            user.walletBalance
          }
          if (parsedWallet == null || (role != UserRole.PRESTADOR && parsedWallet < 0.0)) {
            errorMessage = "Informe um valor de saldo válido."
            return@Button
          }
          val updated = user.copy(
            name = name,
            email = email,
            phone = phone,
            city = city,
            document = document,
            role = role,
            category = if (role == UserRole.PRESTADOR) category else "",
            bio = bio,
            walletBalance = parsedWallet,
            verificationStatus = if (role == UserRole.PRESTADOR) verificationStatus else ProviderVerificationStatus.APROVADO
          )
          val result = onSave(updated)
          result.onFailure { err ->
            errorMessage = err.message ?: "Não foi possível salvar as alterações."
          }
        },
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("btn_confirm_edit_user")
      ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Salvar Alterações", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("btn_cancel_edit_user")
      ) {
        Text("Cancelar")
      }
    }
  )
}

@Composable
fun AdminDeleteUserDialog(
  user: User,
  isCurrentSession: Boolean,
  isOnlyAdmin: Boolean,
  onDismiss: () -> Unit,
  onConfirmDelete: () -> Result<Unit>
) {
  var errorMessage by remember(user.id) { mutableStateOf<String?>(null) }
  val blockedReason = when {
    isCurrentSession -> "Não é possível excluir a conta de Administrador que está conectada agora."
    isOnlyAdmin -> "Não é possível excluir o único Administrador do sistema."
    else -> null
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp)
      .widthIn(max = 460.dp),
    icon = {
      Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = null,
        tint = DangerRed,
        modifier = Modifier.size(34.dp)
      )
    },
    title = {
      Text(
        text = "Excluir Usuário",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Tem certeza que deseja excluir permanentemente este usuário?",
          style = MaterialTheme.typography.bodyMedium
        )
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(user.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(user.role.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            if (user.email.isNotBlank()) {
              Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(user.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        Text(
          text = "Esta ação remove o cadastro da lista de usuários e do Firestore.",
          style = MaterialTheme.typography.bodySmall,
          color = DangerRed
        )
        val displayError = errorMessage ?: blockedReason
        if (!displayError.isNullOrBlank()) {
          Surface(
            color = DangerRedLight,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("admin_delete_user_error")
          ) {
            Text(
              text = displayError,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              color = DangerRed,
              modifier = Modifier.padding(10.dp)
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirmDelete().onFailure { err ->
            errorMessage = err.message ?: "Não foi possível excluir este usuário."
          }
        },
        enabled = blockedReason == null,
        colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("btn_confirm_delete_user")
      ) {
        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Excluir Usuário", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("btn_cancel_delete_user")
      ) {
        Text("Cancelar")
      }
    }
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminAddProviderBalanceDialog(
  provider: User,
  onDismiss: () -> Unit,
  onConfirm: (amount: Double, reason: String) -> Result<Double>
) {
  var amountText by remember(provider.id) { mutableStateOf("50.00") }
  var reasonText by remember(provider.id) { mutableStateOf("") }
  var errorMessage by remember(provider.id) { mutableStateOf<String?>(null) }

  val presetAmounts = listOf(20.0, 50.0, 100.0, 200.0)
  val parsedAmount = amountText.replace(",", ".").toDoubleOrNull()
  val previewBalance = if (parsedAmount != null && parsedAmount > 0.0) {
    provider.walletBalance + parsedAmount
  } else {
    provider.walletBalance
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp)
      .widthIn(max = 480.dp)
      .testTag("admin_add_balance_dialog"),
    icon = {
      Icon(
        imageVector = Icons.Default.AccountBalanceWallet,
        contentDescription = null,
        tint = EmeraldWallet,
        modifier = Modifier.size(32.dp)
      )
    },
    title = {
      Text(
        text = "Adicionar Saldo Manual",
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
        Surface(
          color = EmeraldWalletLight,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = provider.name,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = Color(0xFF065F46)
            )
            if (provider.category.isNotBlank()) {
              Text(
                text = provider.category,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF047857)
              )
            }
            Text(
              text = "Saldo atual: R$ ${String.format("%.2f", provider.walletBalance)}",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              color = EmeraldWallet
            )
          }
        }

        Text(
          text = "Valores rápidos",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          presetAmounts.forEach { preset ->
            val presetFormatted = String.format(java.util.Locale.US, "%.2f", preset)
            FilterChip(
              selected = parsedAmount == preset,
              onClick = {
                amountText = presetFormatted
                errorMessage = null
              },
              label = { Text("+R$ ${preset.toInt()}") },
              modifier = Modifier.testTag("chip_add_balance_${preset.toInt()}")
            )
          }
        }

        OutlinedTextField(
          value = amountText,
          onValueChange = { raw ->
            amountText = raw.filter { c -> c.isDigit() || c == '.' || c == ',' }
            errorMessage = null
          },
          label = { Text("Valor a Adicionar (R$)") },
          placeholder = { Text("Ex: 50.00") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_admin_add_balance_amount")
        )

        OutlinedTextField(
          value = reasonText,
          onValueChange = {
            reasonText = it
            errorMessage = null
          },
          label = { Text("Motivo / Observação (Opcional)") },
          placeholder = { Text("Ex: Bônus promocional, ajuste manual ou crédito") },
          minLines = 2,
          maxLines = 3,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_admin_add_balance_reason")
        )

        Surface(
          color = InfoBlueLight,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Novo saldo após crédito:",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              color = InfoBlue
            )
            Text(
              text = "R$ ${String.format("%.2f", previewBalance)}",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
              color = InfoBlue
            )
          }
        }

        if (!errorMessage.isNullOrBlank()) {
          Surface(
            color = DangerRedLight,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("admin_add_balance_error")
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
              Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = DangerRed
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val value = amountText.replace(",", ".").toDoubleOrNull()
          if (value == null || value <= 0.0) {
            errorMessage = "Informe um valor maior que R$ 0,00."
            return@Button
          }
          onConfirm(value, reasonText).onFailure { err ->
            errorMessage = err.message ?: "Não foi possível adicionar saldo."
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet, contentColor = Color.White),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("btn_confirm_add_balance")
      ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Confirmar Crédito", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("btn_cancel_add_balance")
      ) {
        Text("Cancelar")
      }
    }
  )
}

@Composable
private fun AdminChangePasswordDialog(
  user: User,
  currentSavedPassword: String? = null,
  onDismiss: () -> Unit,
  onConfirmChangePassword: (newPassword: String) -> Result<Unit>,
  onSendPasswordResetEmail: () -> Unit
) {
  var newPassword by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var showCurrentPassword by remember { mutableStateOf(false) }
  var showNewPassword by remember { mutableStateOf(false) }
  var showConfirmPassword by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val hasValidLength = newPassword.length >= 6
  val passwordsMatch = hasValidLength && newPassword == confirmPassword
  val hasValidEmail = FirebaseAuthHelper.isValidEmail(user.email)

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .widthIn(max = 520.dp)
      .testTag("admin_change_password_dialog"),
    icon = {
      Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = CircleShape,
        modifier = Modifier.size(52.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.LockReset,
            contentDescription = "Alterar senha do usuário",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp)
          )
        }
      }
    },
    title = {
      Text(
        text = stringResource(R.string.admin_change_password_dialog_title),
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            Text(
              text = user.name,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            if (user.email.isNotBlank()) {
              Text(
                text = user.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Text(
              text = "${user.role.label} • ${user.phone}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!currentSavedPassword.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(6.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = stringResource(R.string.admin_user_current_password_label),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = if (showCurrentPassword) currentSavedPassword else "••••••••",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("admin_dialog_current_password_value")
                  )
                }
                IconButton(
                  onClick = { showCurrentPassword = !showCurrentPassword },
                  modifier = Modifier
                    .size(48.dp)
                    .testTag("toggle_admin_current_password")
                ) {
                  Icon(
                    imageVector = if (showCurrentPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showCurrentPassword) {
                      stringResource(R.string.admin_user_password_hide_cd)
                    } else {
                      stringResource(R.string.admin_user_password_show_cd)
                    },
                    tint = MaterialTheme.colorScheme.primary
                  )
                }
              }
            }
          }
        }

        OutlinedTextField(
          value = newPassword,
          onValueChange = {
            newPassword = it
            errorMessage = null
          },
          label = { Text(stringResource(R.string.admin_new_password_label)) },
          placeholder = { Text(stringResource(R.string.auth_password_min_length_hint)) },
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
          trailingIcon = {
            IconButton(
              onClick = { showNewPassword = !showNewPassword },
              modifier = Modifier.testTag("toggle_admin_new_password")
            ) {
              Icon(
                imageVector = if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (showNewPassword) "Ocultar nova senha" else "Mostrar nova senha"
              )
            }
          },
          visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          isError = newPassword.isNotEmpty() && !hasValidLength,
          supportingText = {
            when {
              hasValidLength -> Text("✓ Senha válida", color = EmeraldWallet, fontWeight = FontWeight.SemiBold)
              newPassword.isNotEmpty() -> Text("Mínimo de 6 caracteres.", color = DangerRed)
              else -> Text("Informe a nova senha de acesso do usuário.")
            }
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_admin_new_password")
        )

        OutlinedTextField(
          value = confirmPassword,
          onValueChange = {
            confirmPassword = it
            errorMessage = null
          },
          label = { Text(stringResource(R.string.admin_confirm_new_password_label)) },
          placeholder = { Text("Repita a nova senha") },
          leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
          trailingIcon = {
            IconButton(
              onClick = { showConfirmPassword = !showConfirmPassword },
              modifier = Modifier.testTag("toggle_admin_confirm_password")
            ) {
              Icon(
                imageVector = if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (showConfirmPassword) "Ocultar confirmação de senha" else "Mostrar confirmação de senha"
              )
            }
          },
          visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
          supportingText = {
            when {
              passwordsMatch -> Text(
                text = stringResource(R.string.auth_password_match_ok),
                color = EmeraldWallet,
                fontWeight = FontWeight.SemiBold
              )
              confirmPassword.isNotEmpty() && confirmPassword != newPassword -> Text(
                text = stringResource(R.string.auth_password_mismatch_error),
                color = DangerRed
              )
              else -> Text("Repita exatamente a nova senha informada acima.")
            }
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_admin_confirm_password")
        )

        if (!errorMessage.isNullOrBlank()) {
          Surface(
            color = DangerRedLight,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("admin_change_password_error")
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
              Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = DangerRed
              )
            }
          }
        }

        if (hasValidEmail) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          OutlinedButton(
            onClick = onSendPasswordResetEmail,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .testTag("btn_admin_send_password_reset")
          ) {
            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.admin_send_password_reset_cta), fontWeight = FontWeight.SemiBold)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          when {
            !hasValidLength -> errorMessage = "A nova senha deve ter no mínimo 6 caracteres."
            newPassword != confirmPassword -> errorMessage = "As senhas informadas não coincidem."
            else -> {
              onConfirmChangePassword(newPassword).onFailure { err ->
                errorMessage = err.message ?: "Não foi possível alterar a senha."
              }
            }
          }
        },
        enabled = passwordsMatch,
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("btn_admin_confirm_change_password")
      ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.admin_save_new_password_cta), fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("btn_admin_cancel_change_password")
      ) {
        Text("Cancelar")
      }
    }
  )
}

