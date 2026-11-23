package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.example.data.DocumentAttachmentHelper
import com.example.data.LocationAutocompleteMode
import com.example.model.ProviderServicePhoto
import com.example.ui.components.DocumentAttachmentPreviewImage
import com.example.ui.components.GooglePlacesAutocompleteField
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.formatUserRating
import com.example.ui.components.rememberCameraPhotoCaptureLauncher
import com.example.ui.components.resolveAdaptiveSpec
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
import com.example.data.WalletHistoryFilter
import com.example.data.WalletHistoryHelper
import com.example.model.ServiceRequest
import com.example.model.ServiceReview
import com.example.model.TransactionType
import com.example.model.User
import com.example.model.UserRole
import com.example.model.WalletTransaction
import com.example.ui.components.ProviderReviewsSummaryCard
import com.example.ui.components.ReviewItemCard
import com.example.ui.theme.WarningAmber
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File

private fun savePickedProfilePhotoLocally(
  context: android.content.Context,
  uri: android.net.Uri,
  userId: String
): String {
  return try {
    val dir = File(context.filesDir, "profile_photos").apply { mkdirs() }
    val destFile = File(dir, "avatar_${userId}_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
      destFile.outputStream().use { output -> input.copyTo(output) }
    }
    if (destFile.exists() && destFile.length() > 0L) {
      android.net.Uri.fromFile(destFile).toString()
    } else {
      uri.toString()
    }
  } catch (_: Exception) {
    uri.toString()
  }
}

@Composable
fun WalletProfileScreen(
  currentUser: User,
  walletTransactions: List<WalletTransaction>,
  onRechargeClick: () -> Unit,
  onSwitchUserClick: () -> Unit,
  onEditProfileClick: () -> Unit,
  onSaveProfile: (User) -> Unit,
  onBack: () -> Unit = {},
  services: List<ServiceRequest> = emptyList(),
  onRefreshTransactions: () -> Unit = {},
  isRefreshingTransactions: Boolean = false,
  mercadoPagoCharges: List<com.example.model.PixRechargeCharge> = emptyList(),
  isMpPollingActive: Boolean = false,
  isRefreshingMpTransactions: Boolean = false,
  onRefreshMpTransactions: () -> Unit = {},
  onReopenMpCharge: (com.example.model.PixRechargeCharge) -> Unit = {},
  onCancelMpCharge: (com.example.model.PixRechargeCharge) -> Unit = {},
  onOpenSuggestionsTips: () -> Unit = {},
  onOpenAdminCatalogEdits: (() -> Unit)? = null,
  reviews: List<ServiceReview> = emptyList(),
  onReplyToReview: (reviewId: String, replyText: String) -> Unit = { _, _ -> },
  onOpenMenu: (() -> Unit)? = null,
  onChangePasswordClick: () -> Unit = {},
  profileFocusNonce: Int = 0
) {
  val userTransactions = remember(walletTransactions, currentUser.id, currentUser.firebaseUid) {
    walletTransactions
      .filter { currentUser.ownsId(it.providerId) }
      .sortedByDescending { it.timestamp }
  }
  val userMercadoPagoCharges = remember(mercadoPagoCharges, currentUser.id, currentUser.firebaseUid) {
    mercadoPagoCharges
      .filter { currentUser.ownsId(it.providerId) }
      .sortedByDescending { it.createdAt }
  }
  val userReviews = remember(reviews, currentUser.id, currentUser.firebaseUid, currentUser.role) {
    if (currentUser.role == UserRole.PRESTADOR) {
      reviews.filter { currentUser.ownsId(it.providerId) }.sortedByDescending { it.createdAt }
    } else {
      reviews.filter { currentUser.ownsId(it.clientId) }.sortedByDescending { it.createdAt }
    }
  }
  val servicesById = remember(services) {
    services.associateBy { it.id }
  }
  val summary = remember(userTransactions, servicesById) {
    WalletHistoryHelper.summarize(userTransactions, servicesById)
  }

  var selectedFilter by remember { mutableStateOf(WalletHistoryFilter.TODAS) }
  var showFullHistoryScreen by remember { mutableStateOf(false) }
  var showMercadoPagoTransactionsScreen by remember { mutableStateOf(false) }
  var selectedTransaction by remember { mutableStateOf<WalletTransaction?>(null) }
  var showEditDialog by remember { mutableStateOf(false) }
  val listState = rememberLazyListState()

  LaunchedEffect(profileFocusNonce) {
    if (profileFocusNonce > 0) {
      showFullHistoryScreen = false
      showMercadoPagoTransactionsScreen = false
      listState.animateScrollToItem(1)
    }
  }

  val filteredTransactions = remember(userTransactions, selectedFilter) {
    WalletHistoryHelper.filterTransactions(userTransactions, selectedFilter)
  }

  if (showMercadoPagoTransactionsScreen && currentUser.role == UserRole.PRESTADOR) {
    MercadoPagoTransactionsFullScreen(
      charges = userMercadoPagoCharges,
      isPollingActive = isMpPollingActive,
      isRefreshing = isRefreshingMpTransactions,
      onRefreshClick = onRefreshMpTransactions,
      onReopenCharge = { charge ->
        showMercadoPagoTransactionsScreen = false
        onReopenMpCharge(charge)
      },
      onCancelCharge = onCancelMpCharge,
      onNewRechargeClick = {
        showMercadoPagoTransactionsScreen = false
        onRechargeClick()
      },
      onBack = { showMercadoPagoTransactionsScreen = false }
    )
    return
  }

  if (showFullHistoryScreen && currentUser.role == UserRole.PRESTADOR) {
    ProviderWalletHistoryFullScreen(
      currentUser = currentUser,
      userTransactions = userTransactions,
      servicesById = servicesById,
      initialFilter = selectedFilter,
      onSelectFilter = { selectedFilter = it },
      onRechargeClick = onRechargeClick,
      onRefreshClick = onRefreshTransactions,
      isRefreshing = isRefreshingTransactions,
      onBack = { showFullHistoryScreen = false }
    )
    return
  }

  BackHandler { onBack() }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding(),
    contentAlignment = Alignment.TopCenter
  ) {
    val spec = remember(maxWidth) { resolveAdaptiveSpec(maxWidth) }

    Column(
      modifier = Modifier
        .widthIn(max = 840.dp)
        .fillMaxSize()
    ) {
      // Top bar with back button
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (onOpenMenu != null) {
          IconButton(
            onClick = onOpenMenu,
            modifier = Modifier.testTag("btn_wallet_open_menu")
          ) {
            Icon(Icons.Default.Menu, contentDescription = "Abrir Menu")
          }
        }
        IconButton(onClick = onBack) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar para Serviços")
        }
        Text(
          text = if (currentUser.role == UserRole.PRESTADOR) "Carteira & Reputação" else "Meu Perfil",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      LazyColumn(
        state = listState,
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = spec.horizontalPadding)
          .testTag("wallet_profile_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
      ) {
        // 1. Digital Wallet Card (Highlighted for providers, or showing role info)
        item {
          if (currentUser.role == UserRole.PRESTADOR) {
            ProviderWalletCard(
              balance = currentUser.walletBalance,
              totalFeesPaid = summary.totalContractFeesUsed,
              totalPixRecharged = summary.totalPixRecharged,
              transactionsCount = userTransactions.size,
              onRechargeClick = onRechargeClick,
              onOpenHistoryClick = { showFullHistoryScreen = true }
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
            reviewsCount = if (currentUser.role == UserRole.PRESTADOR) maxOf(currentUser.ratingCount, userReviews.size) else userReviews.size,
            onEditClick = { showEditDialog = true },
            onSaveProfile = onSaveProfile,
            onSwitchClick = onSwitchUserClick,
            onChangePasswordClick = onChangePasswordClick
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
                  text = if (currentUser.role == UserRole.CLIENTE) {
                    "Negociação transparente, atendimento verificado e chat seguro na plataforma."
                  } else {
                    "Negociação transparente, carteira integrada com taxa de 10% e chat seguro."
                  },
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        if (currentUser.role == UserRole.ADMIN && onOpenAdminCatalogEdits != null) {
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenAdminCatalogEdits() }
                .testTag("btn_profile_admin_catalog_edits")
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(
                      text = "Categorias de Serviços e Outras Edições",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                      text = "Gerencie categorias, edite pedidos, avisos e configurações do app.",
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
                Icon(
                  imageVector = Icons.Default.ChevronRight,
                  contentDescription = "Abrir Categorias e Edições",
                  tint = MaterialTheme.colorScheme.primary
                )
              }
            }
          }
        }

        // Sugestões e Dicas para o App shortcut
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onOpenSuggestionsTips() }
              .testTag("btn_wallet_suggestions_tips")
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Surface(
                  color = MaterialTheme.colorScheme.primary,
                  shape = CircleShape,
                  modifier = Modifier.size(40.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.Lightbulb,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    text = "Sugestões e Dicas para o App",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                  )
                  Text(
                    text = "Envie melhorias ou veja dicas de uso para ${if (currentUser.role == UserRole.PRESTADOR) "prestadores" else "clientes"}.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
              Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Abrir Sugestões e Dicas",
                tint = MaterialTheme.colorScheme.primary
              )
            }
          }
        }

        // 2b. Vitrine de Serviços Realizados (exibida na propaganda da tela de pedidos)
        if (currentUser.role == UserRole.PRESTADOR) {
          item {
            ProviderPortfolioShowcaseCard(
              user = currentUser,
              onSaveProfile = onSaveProfile
            )
          }
        }

        // 2c. Mercado Pago Pix Transactions List (Status, Value, Date)
        if (currentUser.role == UserRole.PRESTADOR) {
          item {
            MercadoPagoTransactionsPreviewSection(
              charges = userMercadoPagoCharges,
              isPollingActive = isMpPollingActive,
              isRefreshing = isRefreshingMpTransactions,
              onRefreshClick = onRefreshMpTransactions,
              onOpenFullList = { showMercadoPagoTransactionsScreen = true },
              onReopenCharge = onReopenMpCharge,
              onNewRechargeClick = onRechargeClick
            )
          }
        }

        // 3. Detailed Provider Financial History (Pix Recharges + Contract Usage)
        if (currentUser.role == UserRole.PRESTADOR) {
          item {
            ProviderFinancialSummarySection(
              currentBalance = currentUser.walletBalance,
              summary = summary,
              selectedFilter = selectedFilter,
              onSelectFilter = { selectedFilter = it },
              onOpenFullScreen = { showFullHistoryScreen = true },
              onRechargeClick = onRechargeClick,
              onRefreshClick = onRefreshTransactions,
              isRefreshing = isRefreshingTransactions
            )
          }

          if (filteredTransactions.isEmpty()) {
            item {
              WalletHistoryEmptyCard(
                filter = selectedFilter,
                hasQuery = false,
                onRechargeClick = onRechargeClick
              )
            }
          } else {
            items(filteredTransactions, key = { it.id }) { tx ->
              DetailedWalletTransactionCard(
                transaction = tx,
                relatedService = tx.serviceId?.let { servicesById[it] },
                onClick = { selectedTransaction = tx }
              )
            }
          }
        }

        // 4. Reputation & Reviews Section (1 to 5 stars + Provider Reply)
        if (currentUser.role == UserRole.PRESTADOR) {
          item {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .testTag("wallet_provider_reviews_section"),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Text(
                text = "Avaliações de Clientes & Reputação",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              ProviderReviewsSummaryCard(
                averageRating = currentUser.rating,
                reviews = userReviews,
                declaredCount = maxOf(currentUser.ratingCount, userReviews.size)
              )
            }
          }
          if (userReviews.isEmpty()) {
            item {
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "Você ainda não recebeu avaliações de clientes após serviços concluídos.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(14.dp)
                )
              }
            }
          } else {
            items(userReviews, key = { "rev_${it.id}" }) { review ->
              ReviewItemCard(
                review = review,
                canReply = true,
                onReply = { replyText -> onReplyToReview(review.id, replyText) }
              )
            }
          }
        } else if (currentUser.role == UserRole.CLIENTE) {
          item {
            Text(
              text = "Minhas Avaliações Enviadas (${userReviews.size})",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              modifier = Modifier.testTag("wallet_client_reviews_section")
            )
          }
          if (userReviews.isEmpty()) {
            item {
              Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "Quando um serviço for concluído, você poderá avaliar o prestador de 1 a 5 estrelas nos detalhes do pedido.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(14.dp)
                )
              }
            }
          } else {
            items(userReviews, key = { "crev_${it.id}" }) { review ->
              ReviewItemCard(
                review = review,
                canReply = false
              )
            }
          }
        }
      }
    }
  }

  selectedTransaction?.let { tx ->
    TransactionDetailDialog(
      transaction = tx,
      relatedService = tx.serviceId?.let { servicesById[it] },
      onDismiss = { selectedTransaction = null }
    )
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
  totalPixRecharged: Double,
  transactionsCount: Int,
  onRechargeClick: () -> Unit,
  onOpenHistoryClick: () -> Unit
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
          text = when {
            balance < 0.0 -> "Saldo Negativo (será descontado na próxima recarga)"
            balance <= 1.0 -> "Saldo Abaixo do Mínimo (> R$ 1,00 para aceitar)"
            else -> "Saldo Disponível"
          },
          style = MaterialTheme.typography.bodySmall,
          color = if (balance < 0.0) Color(0xFFFECACA) else Color.White.copy(alpha = 0.85f)
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom
        ) {
          Text(
            text = if (balance < 0.0) {
              "-R$ ${String.format("%.2f", -balance)}"
            } else {
              "R$ ${String.format("%.2f", balance)}"
            },
            style = MaterialTheme.typography.headlineLarge.copy(
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = (-0.5).sp
            ),
            color = if (balance < 0.0) Color(0xFFFEE2E2) else Color.White
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
            Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Recarregar Pix", fontWeight = FontWeight.Bold)
          }
        }

        if (balance < 0.0 || balance <= 1.0) {
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            color = Color(0xFF991B1B).copy(alpha = 0.45f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("provider_wallet_negative_notice")
          ) {
            Text(
              text = if (balance < 0.0) {
                "Saldo negativo de R$ ${String.format("%.2f", -balance)}. Ao recarregar via Pix, esse valor será descontado automaticamente."
              } else {
                "Mantenha saldo acima de R$ 1,00 para poder aceitar novos serviços."
              },
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
              color = Color.White,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
          color = Color.Black.copy(alpha = 0.22f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenHistoryClick)
            .testTag("btn_wallet_card_history")
        ) {
          Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Total recarregado via Pix:",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.9f)
              )
              Text(
                text = "R$ ${String.format("%.2f", totalPixRecharged)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = Color(0xFFA7F3D0)
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Total usado em contratações (10%):",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.9f)
              )
              Text(
                text = "R$ ${String.format("%.2f", totalFeesPaid)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = Color(0xFFFDE68A)
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Ver histórico completo ($transactionsCount)",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = Color.White
              )
            }
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
            text = "Perfil de Cliente",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Publique suas solicitações de serviço e contrate profissionais com segurança.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

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
              text = "Conta de Cliente Ativa",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
              text = "Conta protegida via Firebase Authentication. Seus pedidos são visíveis apenas para você.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = "CLIENTE",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun UserProfileCard(
  user: User,
  reviewsCount: Int = user.ratingCount,
  onEditClick: () -> Unit,
  onSaveProfile: (User) -> Unit,
  onSwitchClick: () -> Unit,
  onChangePasswordClick: () -> Unit = {}
) {
  val context = LocalContext.current
  var showPhotoSourceDialog by remember { mutableStateOf(false) }
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      val persistedPhoto = savePickedProfilePhotoLocally(context, uri, user.id)
      onSaveProfile(user.copy(photoUrl = persistedPhoto))
    }
  }
  val launchProfileCamera = rememberCameraPhotoCaptureLauncher(
    docKey = "profile_avatar",
    fileNamePrefix = "perfil_${user.id}",
    permissionDeniedMessage = "Permissão da câmera necessária. Você também pode escolher pela Galeria.",
    onCaptured = { processed ->
      onSaveProfile(user.copy(photoUrl = processed.attachmentUri))
    }
  )

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("user_profile_data_card")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .clickable { showPhotoSourceDialog = true }
              .testTag("btn_change_profile_photo")
          ) {
            ProfileAvatar(
              name = user.name,
              photoUrl = user.photoUrl,
              avatarColorHex = user.avatarColorHex,
              size = 56.dp
            )
            Surface(
              color = MaterialTheme.colorScheme.primary,
              shape = CircleShape,
              shadowElevation = 2.dp,
              modifier = Modifier
                .size(20.dp)
                .align(Alignment.BottomEnd)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.PhotoCamera,
                  contentDescription = "Alterar foto de perfil",
                  tint = Color.White,
                  modifier = Modifier.size(12.dp)
                )
              }
            }
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
              val ratingLine = if (user.role == UserRole.PRESTADOR) {
                val avalLabel = if (reviewsCount == 1) "1 avaliação" else "$reviewsCount avaliações"
                "${formatUserRating(user.rating)} ($avalLabel • ${user.completedJobs} serviços)"
              } else {
                "${formatUserRating(user.rating)} (${user.completedJobs} serviços)"
              }
              Text(
                text = ratingLine,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
              )
            }
          }
        }

        IconButton(
          onClick = onEditClick,
          modifier = Modifier.testTag("btn_profile_edit_icon")
        ) {
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

      FilledTonalButton(
        onClick = onEditClick,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("btn_profile_edit_data")
      ) {
        Icon(
          imageVector = Icons.Default.Edit,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = stringResource(R.string.profile_edit_data_cta),
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      OutlinedButton(
        onClick = onChangePasswordClick,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("btn_profile_change_password")
      ) {
        Icon(
          imageVector = Icons.Default.LockReset,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = stringResource(R.string.change_password_cta),
          fontWeight = FontWeight.SemiBold
        )
      }

      if (user.role == UserRole.ADMIN) {
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
          onClick = onSwitchClick,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("btn_switch_profile_bottom")
        ) {
          Icon(Icons.Default.SwapHoriz, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Trocar para Outro Usuário / Perfil")
        }
      }
    }
  }

  if (showPhotoSourceDialog) {
    AlertDialog(
      onDismissRequest = { showPhotoSourceDialog = false },
      title = { Text("Foto de Perfil", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Escolha como deseja atualizar sua foto de perfil:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          OutlinedButton(
            onClick = {
              showPhotoSourceDialog = false
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_profile_photo_source_gallery")
          ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Buscar na Galeria", fontWeight = FontWeight.SemiBold)
          }
          Button(
            onClick = {
              showPhotoSourceDialog = false
              launchProfileCamera()
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_profile_photo_source_camera")
          ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Abrir Câmera", fontWeight = FontWeight.Bold)
          }
          FilledTonalButton(
            onClick = {
              showPhotoSourceDialog = false
              onEditClick()
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_profile_photo_edit_data")
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.profile_photo_edit_data_cta), fontWeight = FontWeight.Bold)
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showPhotoSourceDialog = false }) {
          Text("Fechar")
        }
      }
    )
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
private fun ProviderPortfolioShowcaseCard(
  user: User,
  onSaveProfile: (User) -> Unit
) {
  val context = LocalContext.current
  var pendingPhotoUri by remember { mutableStateOf<String?>(null) }
  var pendingPhotoTitle by remember { mutableStateOf("") }

  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      DocumentAttachmentHelper.processPickedAttachment(context, uri, "portfolio")?.let { processed ->
        pendingPhotoUri = processed.attachmentUri
        pendingPhotoTitle = user.category.ifBlank { "Serviço realizado" }
      }
    }
  }
  val launchCamera = rememberCameraPhotoCaptureLauncher(
    docKey = "portfolio",
    fileNamePrefix = "vitrine_servico",
    onCaptured = { processed ->
      pendingPhotoUri = processed.attachmentUri
      pendingPhotoTitle = user.category.ifBlank { "Serviço realizado" }
    }
  )

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("provider_portfolio_section")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Text(
        text = "Minha Vitrine de Serviços (Propaganda)",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
      Text(
        text = "As fotos adicionadas aqui aparecem na vitrine em destaque da tela de pedidos junto com seu nome, foto de perfil e nota.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      if (user.portfolioPhotos.isEmpty()) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Nenhuma foto adicionada ainda. Adicione fotos pela galeria ou câmera!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp)
          )
        }
      } else {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          user.portfolioPhotos.forEach { item ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier.width(126.dp)
            ) {
              Column {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                ) {
                  DocumentAttachmentPreviewImage(
                    attachmentUri = item.photoUri,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                  )
                  Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape,
                    modifier = Modifier
                      .align(Alignment.TopEnd)
                      .padding(4.dp)
                      .size(24.dp)
                      .clickable {
                        val updated = user.portfolioPhotos.filterNot { it.id == item.id && it.photoUri == item.photoUri }
                        onSaveProfile(user.copy(portfolioPhotos = updated))
                      }
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remover foto da vitrine",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                      )
                    }
                  }
                }
                Text(
                  text = item.title.ifBlank { item.category.ifBlank { "Serviço" } },
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                )
              }
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = {
            galleryLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          modifier = Modifier
            .weight(1f)
            .testTag("btn_portfolio_gallery")
        ) {
          Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Galeria", maxLines = 1)
        }
        OutlinedButton(
          onClick = { launchCamera() },
          modifier = Modifier
            .weight(1f)
            .testTag("btn_portfolio_camera")
        ) {
          Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Abrir Câmera", maxLines = 1)
        }
      }
    }
  }

  pendingPhotoUri?.let { uri ->
    AlertDialog(
      onDismissRequest = { pendingPhotoUri = null },
      properties = DialogProperties(usePlatformDefaultWidth = false),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .widthIn(max = 520.dp),
      title = { Text("Título do Serviço na Vitrine", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          DocumentAttachmentPreviewImage(
            attachmentUri = uri,
            contentDescription = "Prévia da foto da vitrine",
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxWidth()
              .height(150.dp)
              .clip(RoundedCornerShape(10.dp))
          )
          OutlinedTextField(
            value = pendingPhotoTitle,
            onValueChange = { pendingPhotoTitle = it },
            label = { Text("Descrição curta do trabalho") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val item = ProviderServicePhoto(
              id = "port_${System.currentTimeMillis()}",
              title = pendingPhotoTitle.trim().ifBlank { user.category.ifBlank { "Serviço realizado" } },
              category = user.category.ifBlank { "Serviço" },
              photoUri = uri,
              createdAt = System.currentTimeMillis()
            )
            val updated = (listOf(item) + user.portfolioPhotos.filterNot { it.photoUri == uri }).take(6)
            onSaveProfile(user.copy(portfolioPhotos = updated))
            pendingPhotoUri = null
          }
        ) {
          Text("Publicar na Vitrine")
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingPhotoUri = null }) {
          Text("Cancelar")
        }
      }
    )
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
  var photoUrl by remember { mutableStateOf(currentUser.photoUrl) }
  val context = LocalContext.current
  val dialogPhotoLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      photoUrl = savePickedProfilePhotoLocally(context, uri, currentUser.id)
    }
  }
  val dialogCameraLauncher = rememberCameraPhotoCaptureLauncher(
    docKey = "edit_profile_avatar",
    fileNamePrefix = "perfil_${currentUser.id}",
    permissionDeniedMessage = "Permissão da câmera negada. Escolha pela Galeria.",
    onCaptured = { processed ->
      photoUrl = processed.attachmentUri
    }
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .widthIn(max = 520.dp)
      .testTag("edit_profile_dialog"),
    title = { Text("Editar Cadastro & Perfil", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          ProfileAvatar(
            name = name.ifBlank { currentUser.name },
            photoUrl = photoUrl,
            avatarColorHex = currentUser.avatarColorHex,
            size = 52.dp
          )
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "Foto de perfil",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              OutlinedButton(
                onClick = {
                  dialogPhotoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("btn_edit_profile_photo_gallery")
              ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Galeria", fontSize = 11.sp, maxLines = 1)
              }
              OutlinedButton(
                onClick = { dialogCameraLauncher() },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("btn_edit_profile_photo_camera")
              ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Câmera", fontSize = 11.sp, maxLines = 1)
              }
            }
          }
        }

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nome Completo") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_edit_profile_name")
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

        GooglePlacesAutocompleteField(
          value = city,
          onValueChange = { city = it },
          mode = LocationAutocompleteMode.CITY,
          label = "Cidade / Estado",
          shape = RoundedCornerShape(10.dp),
          fieldTestTag = "input_edit_city",
          autoDetectCurrentCity = false,
          autoRequestLocationPermission = false
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
              document = document,
              photoUrl = photoUrl
            )
          )
        },
        modifier = Modifier.testTag("btn_save_edit_profile")
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
