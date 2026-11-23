package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.DocumentAttachmentHelper
import com.example.data.MarketplaceRepository
import com.example.data.ProviderShowcaseHelper
import com.example.model.CancellationRefundStatus
import com.example.model.ChatMessage
import com.example.model.ClientLocation
import com.example.model.ContactFilter
import com.example.model.Proposal
import com.example.model.ProposalStatus
import com.example.model.ServiceRequest
import com.example.model.ServiceReview
import com.example.model.ServiceStatus
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.components.ClientCompletedPhotosSection
import com.example.ui.components.DocumentAttachmentPreviewImage
import com.example.ui.components.OrderParticipantsSection
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.ProviderReviewsDialog
import com.example.ui.components.ProviderShowcaseSection
import com.example.ui.components.ReviewItemCard
import com.example.ui.components.ServicePhotoAttachmentPickerCard
import com.example.ui.components.ServicePhotoFullScreenDialog
import com.example.ui.components.ServiceVideoAttachmentPickerCard
import com.example.ui.components.ServiceVideoPlayerDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatUserRating
import com.example.ui.components.resolveAdaptiveSpec
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight

@Composable
fun ServicesScreen(
  currentUser: User,
  services: List<ServiceRequest>,
  proposals: List<Proposal>,
  selectedServiceId: String?,
  searchQuery: String,
  categoryFilter: String,
  onSearchChange: (String) -> Unit,
  onCategoryChange: (String) -> Unit,
  onSelectService: (String?) -> Unit,
  onCreateServiceClick: () -> Unit,
  onSubmitProposal: (serviceId: String, price: Double, days: Int, desc: String) -> Unit,
  onOpenChat: (String) -> Unit,
  onUpdateStatus: (String, ServiceStatus) -> Unit,
  onRateService: (String, Float, String) -> Unit,
  onEditBudget: (ServiceRequest) -> Unit = {},
  onAcceptProposal: (String) -> Unit = {},
  onProviderAcceptJob: (String) -> Unit = {},
  onOpenMenu: () -> Unit = {},
  unreadNotificationsCount: Int = 0,
  onNotificationsClick: () -> Unit = {},
  onSupportClick: () -> Unit = {},
  onCancelService: (serviceId: String, reason: String) -> Unit = { _, _ -> },
  onCompleteServiceWithPhoto: (serviceId: String, photoUri: String?, photoName: String?, notes: String?) -> Unit = { sId, _, _, _ ->
    onUpdateStatus(sId, ServiceStatus.CONCLUIDO)
  },
  onCompleteServiceWithMedia: ((
    serviceId: String,
    photoUri: String?,
    photoName: String?,
    videoUri: String?,
    videoName: String?,
    videoThumbnailUri: String?,
    videoDurationMs: Long?,
    notes: String?
  ) -> Unit)? = null,
  providers: List<User> = emptyList(),
  onManageShowcaseClick: (() -> Unit)? = null,
  onAddClientCompletedPhoto: (serviceId: String, photoUri: String, photoName: String, caption: String) -> Unit = { _, _, _, _ -> },
  onRemoveClientCompletedPhoto: (serviceId: String, photoId: String) -> Unit = { _, _ -> },
  dynamicCategories: List<String>? = null,
  globalBannerMessage: String = "",
  onOpenAdminCatalogEdits: (() -> Unit)? = null,
  reviews: List<ServiceReview> = emptyList(),
  onRejectProposal: (String) -> Unit = {},
  onOpenRatingDialog: ((ServiceRequest) -> Unit)? = null,
  onReplyToReview: (reviewId: String, replyText: String) -> Unit = { _, _ -> },
  declinedServiceIds: Set<String> = emptySet(),
  onProviderDeclineService: (String) -> Unit = {},
  onOpenOrderHistory: (() -> Unit)? = null,
  onOpenRatingDialogWithStars: ((ServiceRequest, Float) -> Unit)? = null,
  onConfirmCompletedAndRate: ((ServiceRequest) -> Unit)? = null,
  urgencyFilter: String = "Todas",
  onUrgencyChange: (String) -> Unit = {},
  chatMessages: List<ChatMessage> = emptyList(),
  onProfileClick: () -> Unit = {}
) {
  val effectiveDeclinedServiceIds = remember(currentUser.declinedServiceIds, declinedServiceIds) {
    currentUser.declinedServiceIds.toSet() + declinedServiceIds
  }
  val selectedService = services.find { it.id == selectedServiceId }
  val canUserAccessSelected = selectedService == null || when (currentUser.role) {
    UserRole.CLIENTE -> currentUser.ownsId(selectedService.clientId)
    UserRole.PRESTADOR -> {
      val isAssignedToSomeone = !selectedService.acceptedProviderId.isNullOrBlank() ||
        selectedService.status == ServiceStatus.ACEITO ||
        selectedService.status == ServiceStatus.EM_EXECUCAO ||
        selectedService.status == ServiceStatus.CONCLUIDO
      if (isAssignedToSomeone) {
        currentUser.ownsId(selectedService.acceptedProviderId)
      } else {
        selectedService.id !in effectiveDeclinedServiceIds
      }
    }
    UserRole.ADMIN -> true
  }
  LaunchedEffect(selectedServiceId, selectedService, canUserAccessSelected) {
    if (selectedServiceId != null && (selectedService == null || !canUserAccessSelected)) {
      onSelectService(null)
    }
  }
  var cancelDialogTarget by remember { mutableStateOf<ServiceRequest?>(null) }

  if (selectedService != null && canUserAccessSelected) {
    BackHandler {
      onSelectService(null)
    }
    ServiceDetailView(
      service = selectedService,
      currentUser = currentUser,
      proposals = proposals.filter { it.serviceId == selectedService.id },
      reviews = reviews,
      serviceChatMessages = chatMessages.filter { it.serviceId == selectedService.id },
      onBack = { onSelectService(null) },
      onSubmitProposal = onSubmitProposal,
      onOpenChat = { onOpenChat(selectedService.id) },
      onUpdateStatus = onUpdateStatus,
      onCompleteServiceWithPhoto = onCompleteServiceWithPhoto,
      onCompleteServiceWithMedia = onCompleteServiceWithMedia,
      onRateService = onRateService,
      onOpenRatingDialog = onOpenRatingDialog,
      onOpenRatingDialogWithStars = onOpenRatingDialogWithStars,
      onReplyToReview = onReplyToReview,
      onEditBudget = onEditBudget,
      onAcceptProposal = onAcceptProposal,
      onRejectProposal = onRejectProposal,
      onProviderAcceptJob = onProviderAcceptJob,
      onProviderDeclineService = onProviderDeclineService,
      onCancelClick = { cancelDialogTarget = selectedService },
      onAddClientCompletedPhoto = onAddClientCompletedPhoto,
      onRemoveClientCompletedPhoto = onRemoveClientCompletedPhoto,
      onConfirmCompletedAndRate = onConfirmCompletedAndRate
    )
  } else {
    ServicesListView(
      currentUser = currentUser,
      services = services,
      proposals = proposals,
      providers = providers,
      reviews = reviews,
      chatMessages = chatMessages,
      declinedServiceIds = effectiveDeclinedServiceIds,
      searchQuery = searchQuery,
      categoryFilter = categoryFilter,
      urgencyFilter = urgencyFilter,
      onSearchChange = onSearchChange,
      onCategoryChange = onCategoryChange,
      onUrgencyChange = onUrgencyChange,
      onSelectService = onSelectService,
      onCreateServiceClick = onCreateServiceClick,
      onOpenChat = onOpenChat,
      onEditBudget = onEditBudget,
      onCancelServiceClick = { srv -> cancelDialogTarget = srv },
      onOpenMenu = onOpenMenu,
      unreadNotificationsCount = unreadNotificationsCount,
      onNotificationsClick = onNotificationsClick,
      onSupportClick = onSupportClick,
      onManageShowcaseClick = onManageShowcaseClick,
      dynamicCategories = dynamicCategories,
      globalBannerMessage = globalBannerMessage,
      onOpenAdminCatalogEdits = onOpenAdminCatalogEdits,
      onOpenOrderHistory = onOpenOrderHistory,
      onOpenRatingDialog = onOpenRatingDialog,
      onOpenRatingDialogWithStars = onOpenRatingDialogWithStars,
      onRateService = onRateService,
      onProfileClick = onProfileClick
    )
  }

  cancelDialogTarget?.let { target ->
    CancelServiceReasonDialog(
      service = target,
      actorRole = currentUser.role,
      onDismiss = { cancelDialogTarget = null },
      onConfirm = { reason ->
        onCancelService(target.id, reason)
        cancelDialogTarget = null
      }
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServicesListView(
  currentUser: User,
  services: List<ServiceRequest>,
  proposals: List<Proposal>,
  providers: List<User>,
  reviews: List<ServiceReview> = emptyList(),
  chatMessages: List<ChatMessage> = emptyList(),
  declinedServiceIds: Set<String> = emptySet(),
  searchQuery: String,
  categoryFilter: String,
  urgencyFilter: String = "Todas",
  onSearchChange: (String) -> Unit,
  onCategoryChange: (String) -> Unit,
  onUrgencyChange: (String) -> Unit = {},
  onSelectService: (String) -> Unit,
  onCreateServiceClick: () -> Unit,
  onOpenChat: (String) -> Unit,
  onEditBudget: (ServiceRequest) -> Unit = {},
  onCancelServiceClick: (ServiceRequest) -> Unit = {},
  onOpenMenu: () -> Unit = {},
  unreadNotificationsCount: Int = 0,
  onNotificationsClick: () -> Unit = {},
  onSupportClick: () -> Unit = {},
  onManageShowcaseClick: (() -> Unit)? = null,
  dynamicCategories: List<String>? = null,
  globalBannerMessage: String = "",
  onOpenAdminCatalogEdits: (() -> Unit)? = null,
  onOpenOrderHistory: (() -> Unit)? = null,
  onOpenRatingDialog: ((ServiceRequest) -> Unit)? = null,
  onOpenRatingDialogWithStars: ((ServiceRequest, Float) -> Unit)? = null,
  onRateService: ((String, Float, String) -> Unit)? = null,
  onProfileClick: () -> Unit = {}
) {
  var showOnlyMyServices by remember { mutableStateOf(false) }
  var isSearchExpanded by remember { mutableStateOf(false) }
  val showcaseEntries = remember(providers, services, categoryFilter) {
    ProviderShowcaseHelper.buildShowcaseEntries(
      users = providers,
      services = services,
      categoryFilter = categoryFilter
    )
  }
  val pendingRatingServices = remember(services, reviews, currentUser) {
    if (currentUser.role != UserRole.CLIENTE) {
      emptyList()
    } else {
      val reviewedIds = reviews.map { it.serviceId }.toSet()
      services.filter { srv ->
        currentUser.ownsId(srv.clientId) &&
          srv.status == ServiceStatus.CONCLUIDO &&
          srv.ratingGiven == null &&
          srv.id !in reviewedIds
      }.sortedByDescending { it.completedAt ?: it.createdAt }
    }
  }
  val triggerQuickRating: (ServiceRequest, Float) -> Unit = { targetService, stars ->
    when {
      onOpenRatingDialogWithStars != null -> onOpenRatingDialogWithStars(targetService, stars)
      onOpenRatingDialog != null -> onOpenRatingDialog(targetService)
      onRateService != null -> onRateService(targetService.id, stars, "")
      else -> onSelectService(targetService.id)
    }
  }

  val filteredServices = services.filter { srv ->
    val matchesSearch = srv.title.contains(searchQuery, ignoreCase = true) ||
      srv.description.contains(searchQuery, ignoreCase = true) ||
      srv.category.contains(searchQuery, ignoreCase = true)

    val matchesCat = when (categoryFilter) {
      "Em Andamento" -> srv.status == ServiceStatus.EM_EXECUCAO || srv.status == ServiceStatus.ACEITO
      "Concluído" -> srv.status == ServiceStatus.CONCLUIDO
      "Cancelado" -> srv.status == ServiceStatus.CANCELADO
      "Serviços", "Todos" -> srv.status != ServiceStatus.CONCLUIDO && srv.status != ServiceStatus.CANCELADO
      else -> srv.status != ServiceStatus.CONCLUIDO &&
        srv.status != ServiceStatus.CANCELADO &&
        srv.category.contains(categoryFilter, ignoreCase = true)
    }

    val matchesUrgency = urgencyFilter == "Todas" || srv.urgency.equals(urgencyFilter, ignoreCase = true)

    val matchesOwnership = when (currentUser.role) {
      UserRole.CLIENTE -> {
        // REGRA ESTRITA: Cliente NÃO pode visualizar os pedidos de outros clientes!
        // Cliente só pode ver os seus próprios pedidos.
        currentUser.ownsId(srv.clientId)
      }
      UserRole.PRESTADOR -> {
        MarketplaceRepository.isServiceVisibleForProvider(
          service = srv,
          provider = currentUser,
          showOnlyMyServices = showOnlyMyServices,
          categoryFilter = categoryFilter,
          proposals = proposals,
          declinedServiceIds = declinedServiceIds
        )
      }
      UserRole.ADMIN -> true
    }

    matchesSearch && matchesCat && matchesUrgency && matchesOwnership
  }.sortedWith(
    compareBy<ServiceRequest> { srv ->
      val isInProgress = srv.status == ServiceStatus.EM_EXECUCAO || srv.status == ServiceStatus.ACEITO
      val belongsToActiveUser = currentUser.ownsId(srv.clientId) || currentUser.ownsId(srv.acceptedProviderId)
      when {
        isInProgress && (belongsToActiveUser || currentUser.role == UserRole.ADMIN) -> 0
        else -> 1
      }
    }.thenByDescending { it.createdAt }
  )

  Scaffold(
    floatingActionButton = {
      if (currentUser.role == UserRole.CLIENTE) {
        FloatingActionButton(
          onClick = onCreateServiceClick,
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = Color.White,
          modifier = Modifier.testTag("fab_create_service")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Add, contentDescription = "Solicitar")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Novo Pedido", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
          }
        }
      }
    }
  ) { padding ->
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      contentAlignment = Alignment.TopCenter
    ) {
      val spec = remember(maxWidth) { resolveAdaptiveSpec(maxWidth) }
      val compactProviderRow = maxWidth < 480.dp

      Column(
        modifier = Modifier
          .widthIn(max = 1200.dp)
          .fillMaxSize()
          .padding(horizontal = spec.horizontalPadding)
      ) {
        // IN-DRIVE FLOATING HEADER (status bar & narrow width safe)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp, bottom = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
          ) {
            // Floating Hamburger Menu Button
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = CircleShape,
              shadowElevation = 3.dp,
              tonalElevation = 2.dp,
              modifier = Modifier
                .size(if (spec.isNarrowPhone) 40.dp else 44.dp)
                .clickable { onOpenMenu() }
                .testTag("btn_indrive_menu")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Menu,
                  contentDescription = "Abrir Menu",
                  tint = MaterialTheme.colorScheme.onSurface
                )
              }
            }

            Spacer(modifier = Modifier.width(if (spec.isNarrowPhone) 8.dp else 10.dp))

            // App Logo & Branding
            Row(verticalAlignment = Alignment.CenterVertically) {
              Image(
                painter = painterResource(id = R.drawable.resolveai_logo),
                contentDescription = "Logo",
                modifier = Modifier
                  .size(if (spec.isNarrowPhone) 30.dp else 34.dp)
                  .clip(RoundedCornerShape(8.dp))
              )
              Spacer(modifier = Modifier.width(if (spec.isNarrowPhone) 6.dp else 8.dp))
              Column {
                Text(
                  text = "ResolveAi",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (spec.isNarrowPhone) 15.sp else 16.sp
                  ),
                  color = MaterialTheme.colorScheme.primary,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = if (currentUser.role == UserRole.PRESTADOR) "Oportunidades" else "Serviços",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                  color = EmeraldWallet,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }

          // Quick Actions: Search, Notifications, Support & Wallet Chip
          Row(verticalAlignment = Alignment.CenterVertically) {
            val isSearchActive = isSearchExpanded || searchQuery.isNotBlank()
            IconButton(
              onClick = {
                if (isSearchExpanded) {
                  onSearchChange("")
                  isSearchExpanded = false
                } else {
                  isSearchExpanded = true
                }
              },
              modifier = Modifier
                .size(if (spec.isNarrowPhone) 34.dp else 38.dp)
                .testTag("btn_header_search")
            ) {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = if (isSearchActive) "Fechar busca de serviços" else "Buscar serviços e categorias",
                tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
              )
            }

            IconButton(
              onClick = onNotificationsClick,
              modifier = Modifier
                .size(if (spec.isNarrowPhone) 34.dp else 38.dp)
                .testTag("btn_header_notifications")
            ) {
              BadgedBox(
                badge = {
                  if (unreadNotificationsCount > 0) {
                    Badge(containerColor = DangerRed) { Text("$unreadNotificationsCount", maxLines = 1) }
                  }
                }
              ) {
                Icon(Icons.Default.Notifications, contentDescription = "Notificações", modifier = Modifier.size(20.dp))
              }
            }

            IconButton(
              onClick = onSupportClick,
              modifier = Modifier
                .size(if (spec.isNarrowPhone) 34.dp else 38.dp)
                .testTag("btn_header_support")
            ) {
              Icon(Icons.Default.SupportAgent, contentDescription = "Suporte", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }

            if (currentUser.role == UserRole.PRESTADOR) {
              val isNegativeBalance = currentUser.walletBalance < 0.0
              val isLowBalance = currentUser.walletBalance <= 1.0
              Spacer(modifier = Modifier.width(2.dp))
              Surface(
                color = when {
                  isNegativeBalance -> DangerRedLight
                  isLowBalance -> WarningAmberLight
                  else -> EmeraldWalletLight
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .widthIn(max = if (spec.isNarrowPhone) 96.dp else 130.dp)
                  .clickable { onOpenMenu() }
                  .testTag("header_wallet_chip")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = if (spec.isNarrowPhone) 6.dp else 8.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = when {
                      isNegativeBalance -> DangerRed
                      isLowBalance -> WarningAmber
                      else -> EmeraldWallet
                    },
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = if (isNegativeBalance) {
                      "-R$ ${String.format("%.2f", -currentUser.walletBalance)}"
                    } else {
                      "R$ ${String.format("%.2f", currentUser.walletBalance)}"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = if (spec.isNarrowPhone) 10.sp else 11.sp
                    ),
                    color = when {
                      isNegativeBalance -> DangerRed
                      isLowBalance -> WarningAmber
                      else -> EmeraldWallet
                    },
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }

            Spacer(modifier = Modifier.width(4.dp))
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = CircleShape,
              border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
              shadowElevation = 2.dp,
              modifier = Modifier
                .size(if (spec.isNarrowPhone) 34.dp else 38.dp)
                .clip(CircleShape)
                .clickable { onProfileClick() }
                .testTag("btn_header_profile_avatar")
            ) {
              Box(contentAlignment = Alignment.Center) {
                ProfileAvatar(
                  name = currentUser.name,
                  photoUrl = currentUser.photoUrl,
                  avatarColorHex = currentUser.avatarColorHex,
                  size = if (spec.isNarrowPhone) 32.dp else 36.dp,
                  contentDescription = stringResource(R.string.profile_avatar_open_cd)
                )
              }
            }
          }
        }

        AnimatedVisibility(visible = isSearchExpanded || searchQuery.isNotBlank()) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = searchQuery,
              onValueChange = onSearchChange,
              placeholder = {
                Text(
                  text = "Buscar serviços, categorias...",
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
              trailingIcon = {
                IconButton(
                  onClick = {
                    onSearchChange("")
                    isSearchExpanded = false
                  },
                  modifier = Modifier.testTag("btn_close_search")
                ) {
                  Icon(Icons.Default.Close, contentDescription = "Fechar busca")
                }
              },
              shape = RoundedCornerShape(12.dp),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("search_services_input")
            )
          }
        }

        if (globalBannerMessage.isNotBlank()) {
          Spacer(modifier = Modifier.height(6.dp))
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("global_admin_banner")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = globalBannerMessage,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        if (currentUser.role == UserRole.ADMIN && onOpenAdminCatalogEdits != null) {
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedButton(
            onClick = onOpenAdminCatalogEdits,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .testTag("btn_services_admin_catalog_edits")
          ) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = null,
              modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Admin: Categorias de Serviços e Outras Edições",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        val hasActiveFilters = (categoryFilter != "Serviços" && categoryFilter != "Todos") || urgencyFilter != "Todas"
        if (hasActiveFilters) {
          Spacer(modifier = Modifier.height(6.dp))
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("active_filter_indicator")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = Icons.Default.FilterList,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val filterSummary = buildList {
                  if (categoryFilter != "Serviços" && categoryFilter != "Todos") add(categoryFilter)
                  if (urgencyFilter != "Todas") add("Urgência: $urgencyFilter")
                }.joinToString(" • ")
                Text(
                  text = "Filtro ativo: $filterSummary",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              TextButton(
                onClick = {
                  onCategoryChange("Serviços")
                  onUrgencyChange("Todas")
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                  .heightIn(min = 40.dp)
                  .testTag("btn_clear_active_filter")
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Limpar filtro",
                  modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Limpar", fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val listTitle = when {
          categoryFilter == "Em Andamento" -> "Serviços Em Andamento (${filteredServices.size})"
          categoryFilter == "Concluído" -> "Serviços Concluídos (${filteredServices.size})"
          categoryFilter == "Cancelado" -> "Serviços Cancelados (${filteredServices.size})"
          currentUser.role == UserRole.PRESTADOR && showOnlyMyServices -> "Minhas Propostas (${filteredServices.size})"
          currentUser.role == UserRole.CLIENTE -> "Meus Pedidos (${filteredServices.size})"
          currentUser.role == UserRole.ADMIN -> "Todos os Serviços (${filteredServices.size})"
          else -> "Oportunidades de Trabalho (${filteredServices.size})"
        }

        // Responsive section title & provider scope switcher ("Todas Oportunidades" / "Minhas Propostas")
        if (currentUser.role == UserRole.PRESTADOR && compactProviderRow) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = listTitle,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Surface(
                  color = if (!showOnlyMyServices) MaterialTheme.colorScheme.primary else Color.Transparent,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier
                    .weight(1f)
                    .clickable { showOnlyMyServices = false }
                    .testTag("tab_provider_all_opportunities")
                ) {
                  Text(
                    text = "Todas Oportunidades",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = if (spec.isNarrowPhone) 11.sp else 12.sp
                    ),
                    color = if (!showOnlyMyServices) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                  )
                }
                Surface(
                  color = if (showOnlyMyServices) MaterialTheme.colorScheme.primary else Color.Transparent,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier
                    .weight(1f)
                    .clickable { showOnlyMyServices = true }
                    .testTag("tab_provider_my_proposals")
                ) {
                  Text(
                    text = "Minhas Propostas",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = if (spec.isNarrowPhone) 11.sp else 12.sp
                    ),
                    color = if (showOnlyMyServices) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                  )
                }
              }
            }
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = listTitle,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false)
            )

            if (currentUser.role == UserRole.PRESTADOR) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  color = if (!showOnlyMyServices) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier
                    .clickable { showOnlyMyServices = false }
                    .testTag("tab_provider_all_opportunities")
                ) {
                  Text(
                    text = "Todas Oportunidades",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (!showOnlyMyServices) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                  )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                  color = if (showOnlyMyServices) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier
                    .clickable { showOnlyMyServices = true }
                    .testTag("tab_provider_my_proposals")
                ) {
                  Text(
                    text = "Minhas Propostas",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (showOnlyMyServices) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredServices.isEmpty()) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
              .verticalScroll(rememberScrollState())
              .padding(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            if (pendingRatingServices.isNotEmpty()) {
              ClientPendingReviewsBanner(
                pendingServices = pendingRatingServices,
                onRateWithStars = triggerQuickRating
              )
            }
            if (showcaseEntries.isNotEmpty()) {
              ProviderShowcaseSection(
                entries = showcaseEntries,
                currentUserRole = currentUser.role,
                onRequestServiceClick = onCreateServiceClick,
                onManageShowcaseClick = onManageShowcaseClick
              )
            }
            if (currentUser.role == UserRole.PRESTADOR && categoryFilter != "Concluído" && categoryFilter != "Cancelado") {
              ProviderRadarEmptyState(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 24.dp, horizontal = 16.dp)
              )
            } else {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = when {
                      categoryFilter == "Concluído" -> "Nenhum serviço concluído encontrado."
                      categoryFilter == "Cancelado" -> "Nenhum serviço cancelado encontrado."
                      else ->
                        "Você ainda não possui pedidos ativos.\nClique em '+ Novo Pedido' abaixo para solicitar seu serviço!"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                  )
                }
              }
            }
          }
        } else if (spec.isTablet) {
          LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 330.dp),
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
              .testTag("services_list_lazy_column"),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
          ) {
            if (pendingRatingServices.isNotEmpty()) {
              item(span = { GridItemSpan(maxLineSpan) }, key = "pending_reviews_banner_item") {
                ClientPendingReviewsBanner(
                  pendingServices = pendingRatingServices,
                  onRateWithStars = triggerQuickRating
                )
              }
            }
            if (showcaseEntries.isNotEmpty()) {
              item(span = { GridItemSpan(maxLineSpan) }, key = "provider_showcase_banner") {
                ProviderShowcaseSection(
                  entries = showcaseEntries,
                  currentUserRole = currentUser.role,
                  onRequestServiceClick = onCreateServiceClick,
                  onManageShowcaseClick = onManageShowcaseClick
                )
              }
            }
            items(filteredServices, key = { it.id }) { service ->
              val serviceProposals = proposals.filter { it.serviceId == service.id }
              val matchedRating = service.ratingGiven ?: reviews.firstOrNull { it.serviceId == service.id }?.rating
              val srvChatCount = chatMessages.count { it.serviceId == service.id && !it.isSystemNotice }
              ServiceItemCard(
                service = service,
                currentUser = currentUser,
                proposalCount = serviceProposals.size,
                latestProposal = serviceProposals.firstOrNull(),
                effectiveRatingGiven = matchedRating,
                chatMessageCount = srvChatCount,
                onClick = { onSelectService(service.id) },
                onOpenChat = { onOpenChat(service.id) },
                onEditBudget = { onEditBudget(service) },
                onCancelClick = { onCancelServiceClick(service) },
                onRateClick = { stars -> triggerQuickRating(service, stars) }
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
              .testTag("services_list_lazy_column"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
          ) {
            if (pendingRatingServices.isNotEmpty()) {
              item(key = "pending_reviews_banner_item") {
                ClientPendingReviewsBanner(
                  pendingServices = pendingRatingServices,
                  onRateWithStars = triggerQuickRating
                )
              }
            }
            if (showcaseEntries.isNotEmpty()) {
              item(key = "provider_showcase_banner") {
                ProviderShowcaseSection(
                  entries = showcaseEntries,
                  currentUserRole = currentUser.role,
                  onRequestServiceClick = onCreateServiceClick,
                  onManageShowcaseClick = onManageShowcaseClick
                )
              }
            }
            items(filteredServices, key = { it.id }) { service ->
              val serviceProposals = proposals.filter { it.serviceId == service.id }
              val matchedRating = service.ratingGiven ?: reviews.firstOrNull { it.serviceId == service.id }?.rating
              val srvChatCount = chatMessages.count { it.serviceId == service.id && !it.isSystemNotice }
              ServiceItemCard(
                service = service,
                currentUser = currentUser,
                proposalCount = serviceProposals.size,
                latestProposal = serviceProposals.firstOrNull(),
                effectiveRatingGiven = matchedRating,
                chatMessageCount = srvChatCount,
                onClick = { onSelectService(service.id) },
                onOpenChat = { onOpenChat(service.id) },
                onEditBudget = { onEditBudget(service) },
                onCancelClick = { onCancelServiceClick(service) },
                onRateClick = { stars -> triggerQuickRating(service, stars) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ClientPendingReviewsBanner(
  pendingServices: List<ServiceRequest>,
  onRateWithStars: (ServiceRequest, Float) -> Unit
) {
  val primaryTarget = pendingServices.firstOrNull() ?: return
  Card(
    colors = CardDefaults.cardColors(containerColor = WarningAmberLight),
    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.55f)),
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("client_pending_reviews_banner")
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = WarningAmber,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.review_pending_banner_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF78350F),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        if (pendingServices.size > 1) {
          Surface(
            color = WarningAmber,
            shape = RoundedCornerShape(10.dp)
          ) {
            Text(
              text = "${pendingServices.size} pendentes",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
              color = Color.White,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }
      }

      Text(
        text = "${primaryTarget.title} • ${primaryTarget.acceptedProviderName ?: "Prestador"}",
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        color = Color(0xFF92400E),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          (1..5).forEach { star ->
            IconButton(
              onClick = { onRateWithStars(primaryTarget, star.toFloat()) },
              modifier = Modifier
                .size(48.dp)
                .testTag("banner_star_rate_$star")
            ) {
              Icon(
                imageVector = Icons.Default.StarBorder,
                contentDescription = "Avaliar com $star estrelas",
                tint = WarningAmber,
                modifier = Modifier.size(26.dp)
              )
            }
          }
        }

        Button(
          onClick = { onRateWithStars(primaryTarget, 5.0f) },
          colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("btn_banner_rate_service")
        ) {
          Text(
            text = stringResource(R.string.review_pending_banner_cta),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            softWrap = false
          )
        }
      }
    }
  }
}

@Composable
private fun ServiceItemCard(
  service: ServiceRequest,
  currentUser: User,
  proposalCount: Int,
  latestProposal: Proposal? = null,
  effectiveRatingGiven: Float? = service.ratingGiven,
  chatMessageCount: Int = 0,
  onClick: () -> Unit,
  onOpenChat: () -> Unit,
  onEditBudget: () -> Unit = {},
  onCancelClick: () -> Unit = {},
  onRateClick: ((Float) -> Unit)? = null
) {
  val estimatedFee = (service.finalAgreedPrice ?: service.proposedBudget) * 0.10
  val isChatOpenStatus = !service.acceptedProviderId.isNullOrBlank() &&
    (service.status == ServiceStatus.ACEITO ||
      service.status == ServiceStatus.EM_EXECUCAO ||
      service.status == ServiceStatus.CONCLUIDO ||
      (currentUser.role == UserRole.ADMIN && service.status == ServiceStatus.CANCELADO))
  val isChatUnlocked = isChatOpenStatus && when (currentUser.role) {
    UserRole.CLIENTE -> currentUser.ownsId(service.clientId)
    UserRole.PRESTADOR -> currentUser.ownsId(service.acceptedProviderId)
    UserRole.ADMIN -> true
  }
  val canEditBudget = (currentUser.ownsId(service.clientId) || currentUser.role == UserRole.CLIENTE) &&
    (service.status == ServiceStatus.ABERTO || service.status == ServiceStatus.EM_NEGOCIACAO)
  val canClientCancel = currentUser.role == UserRole.CLIENTE &&
    currentUser.ownsId(service.clientId) &&
    service.status != ServiceStatus.CONCLUIDO &&
    service.status != ServiceStatus.CANCELADO
  val canProviderCancel = currentUser.role == UserRole.PRESTADOR &&
    currentUser.ownsId(service.acceptedProviderId) &&
    (service.status == ServiceStatus.ACEITO || service.status == ServiceStatus.EM_EXECUCAO)
  val canCancel = canClientCancel || canProviderCancel

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("service_card_${service.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Header: Category and Status
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = service.category,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }

        StatusBadge(status = service.status)
      }

      if ((service.status == ServiceStatus.EM_EXECUCAO || service.status == ServiceStatus.ACEITO) &&
        !service.acceptedProviderId.isNullOrBlank()
      ) {
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          color = EmeraldWalletLight.copy(alpha = 0.85f),
          border = BorderStroke(1.dp, EmeraldWallet.copy(alpha = 0.4f)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("service_in_progress_badge_${service.id}")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = EmeraldWallet,
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = "Em Andamento • Prestador: ${service.acceptedProviderName ?: "Confirmado"}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = Color(0xFF065F46),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Title
      Text(
        text = service.title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(4.dp))

      // Description snippet
      Text(
        text = service.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Location
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.LocationOn,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = service.location,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Client & Provider profile photo, rating, and name
      OrderParticipantsSection(
        service = service,
        latestProposal = latestProposal
      )

      // Travel time indicator for provider
      if (currentUser.role == UserRole.PRESTADOR) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            color = Color(0xFFEFF6FF),
            shape = RoundedCornerShape(6.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = InfoBlue,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Deslocamento: ~${service.effectiveLocation.drivingTimeMinutes} min (${service.effectiveLocation.distanceKm} km)",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = InfoBlue
              )
            }
          }
          Text(
            text = "• ${service.effectiveLocation.trafficCondition}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (!service.attachmentPhotoUri.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          color = InfoBlue.copy(alpha = 0.08f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("service_card_photo_${service.id}")
        ) {
          Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            DocumentAttachmentPreviewImage(
              attachmentUri = service.attachmentPhotoUri,
              contentDescription = "Foto anexada no pedido",
              contentScale = ContentScale.Crop,
              accentColor = InfoBlue,
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Foto anexada pelo cliente",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = InfoBlue
              )
              Text(
                text = service.attachmentPhotoName?.takeIf { it.isNotBlank() } ?: "Toque em Ver detalhes para ampliar",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }

      if (!service.attachmentVideoUri.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          color = InfoBlue.copy(alpha = 0.08f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("service_card_video_${service.id}")
        ) {
          Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F172A)),
              contentAlignment = Alignment.Center
            ) {
              if (!service.attachmentVideoThumbnailUri.isNullOrBlank()) {
                DocumentAttachmentPreviewImage(
                  attachmentUri = service.attachmentVideoThumbnailUri,
                  contentDescription = "Vídeo anexado no pedido",
                  contentScale = ContentScale.Crop,
                  accentColor = InfoBlue,
                  modifier = Modifier.fillMaxSize()
                )
              }
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Vídeo anexado pelo cliente (${DocumentAttachmentHelper.formatVideoDuration(service.attachmentVideoDurationMs)})",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = InfoBlue
              )
              Text(
                text = service.attachmentVideoName?.takeIf { it.isNotBlank() } ?: "Toque em Ver detalhes para assistir",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }

      if (service.status == ServiceStatus.CONCLUIDO && !service.completionPhotoUri.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          color = EmeraldWalletLight.copy(alpha = 0.7f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("service_card_completion_photo_${service.id}")
        ) {
          Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            DocumentAttachmentPreviewImage(
              attachmentUri = service.completionPhotoUri,
              contentDescription = "Foto do serviço finalizado",
              contentScale = ContentScale.Crop,
              accentColor = EmeraldWallet,
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Foto do serviço finalizado",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = EmeraldWallet
              )
              Text(
                text = service.completionNotes?.takeIf { it.isNotBlank() }
                  ?: service.completionPhotoName?.takeIf { it.isNotBlank() }
                  ?: "Toque em Ver detalhes para ampliar",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = Color(0xFF065F46),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }

      if (service.status == ServiceStatus.CONCLUIDO && !service.completionVideoUri.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          color = EmeraldWalletLight.copy(alpha = 0.7f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("service_card_completion_video_${service.id}")
        ) {
          Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF064E3B)),
              contentAlignment = Alignment.Center
            ) {
              if (!service.completionVideoThumbnailUri.isNullOrBlank()) {
                DocumentAttachmentPreviewImage(
                  attachmentUri = service.completionVideoThumbnailUri,
                  contentDescription = "Vídeo do serviço finalizado",
                  contentScale = ContentScale.Crop,
                  accentColor = EmeraldWallet,
                  modifier = Modifier.fillMaxSize()
                )
              }
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Vídeo do serviço finalizado (${DocumentAttachmentHelper.formatVideoDuration(service.completionVideoDurationMs)})",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = EmeraldWallet
              )
              Text(
                text = service.completionNotes?.takeIf { it.isNotBlank() }
                  ?: service.completionVideoName?.takeIf { it.isNotBlank() }
                  ?: "Toque em Ver detalhes para assistir",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = Color(0xFF065F46),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }

      if (service.status == ServiceStatus.CANCELADO && !service.cancellationReason.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        ServiceCancellationBannerCard(
          service = service,
          modifier = Modifier.testTag("service_cancellation_reason_card_${service.id}")
        )
      }

      if (service.status == ServiceStatus.CONCLUIDO) {
        if (effectiveRatingGiven != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            color = WarningAmberLight,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("service_rated_badge_${service.id}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              val filledStars = kotlin.math.round(effectiveRatingGiven).toInt().coerceIn(1, 5)
              (1..5).forEach { star ->
                Icon(
                  imageVector = if (star <= filledStars) Icons.Default.Star else Icons.Default.StarBorder,
                  contentDescription = null,
                  tint = WarningAmber,
                  modifier = Modifier.size(13.dp)
                )
              }
              Spacer(modifier = Modifier.width(2.dp))
              Text(
                text = "Avaliado com ${formatUserRating(effectiveRatingGiven.toDouble())} estrelas",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = Color(0xFF92400E)
              )
            }
          }
        } else if (currentUser.role == UserRole.CLIENTE && currentUser.ownsId(service.clientId)) {
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            color = WarningAmberLight.copy(alpha = 0.8f),
            border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onRateClick?.invoke(5.0f) ?: onClick() }
              .testTag("service_pending_rating_badge_${service.id}")
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.StarBorder, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Concluído • Avalie ${service.acceptedProviderName ?: "o prestador"} de 1 a 5 estrelas",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                  color = Color(0xFF92400E)
                )
              }
              Button(
                onClick = { onRateClick?.invoke(5.0f) ?: onClick() },
                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .testTag("btn_card_rate_service_${service.id}")
              ) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = stringResource(R.string.review_card_rate_cta),
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Option to change proposed value on the requested service
      if (canEditBudget) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditBudget() }
            .testTag("btn_quick_change_budget_${service.id}")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Mudar valor proposto",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
              )
            }
            Text(
              text = "Alterar Orçamento ✏️",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
      }

      if (canCancel) {
        OutlinedButton(
          onClick = onCancelClick,
          border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.55f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .testTag("btn_card_cancel_service_${service.id}")
        ) {
          Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (currentUser.role == UserRole.PRESTADOR) "Cancelar Serviço" else "Cancelar Pedido",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
      }

      if (currentUser.role == UserRole.ADMIN && isChatUnlocked) {
        OutlinedButton(
          onClick = onOpenChat,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("btn_admin_card_chat_${service.id}")
        ) {
          Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(17.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${stringResource(R.string.admin_chat_open_cta)} ($chatMessageCount)",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
      }

      // Footer: Budget, Platform fee info, Action
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = if (service.finalAgreedPrice != null) "Valor Acordado" else "Valor Proposto",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = "R$ ${String.format("%.2f", service.finalAgreedPrice ?: service.proposedBudget)}",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (service.status == ServiceStatus.ACEITO || service.status == ServiceStatus.EM_EXECUCAO) EmeraldWallet else MaterialTheme.colorScheme.primary
              )
            )
          }
          if (currentUser.role != UserRole.CLIENTE) {
            // Fee explanation badge (only visible to Provider / Admin, never Client)
            Text(
              text = "Taxa plataforma (10%): R$ ${String.format("%.2f", estimatedFee)}",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
              color = WarningAmber
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Chat option: only appears when proposal has been accepted!
          if (isChatUnlocked) {
            IconButton(
              onClick = onOpenChat,
              modifier = Modifier.testTag("btn_quick_chat_${service.id}")
            ) {
              Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "Chat",
                tint = MaterialTheme.colorScheme.primary
              )
            }
          }

          Button(
            onClick = onClick,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("btn_view_service_${service.id}")
          ) {
            Text(
              text = when {
                proposalCount > 0 && currentUser.role == UserRole.CLIENTE ->
                  if (proposalCount == 1) "1 interessado" else "$proposalCount interessados"
                proposalCount > 0 -> "$proposalCount propostas"
                else -> "Ver detalhes"
              },
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ServiceDetailView(
  service: ServiceRequest,
  currentUser: User,
  proposals: List<Proposal>,
  reviews: List<ServiceReview> = emptyList(),
  serviceChatMessages: List<ChatMessage> = emptyList(),
  onBack: () -> Unit,
  onSubmitProposal: (String, Double, Int, String) -> Unit,
  onOpenChat: () -> Unit,
  onUpdateStatus: (String, ServiceStatus) -> Unit,
  onCompleteServiceWithPhoto: (serviceId: String, photoUri: String?, photoName: String?, notes: String?) -> Unit = { sId, _, _, _ ->
    onUpdateStatus(sId, ServiceStatus.CONCLUIDO)
  },
  onCompleteServiceWithMedia: ((
    serviceId: String,
    photoUri: String?,
    photoName: String?,
    videoUri: String?,
    videoName: String?,
    videoThumbnailUri: String?,
    videoDurationMs: Long?,
    notes: String?
  ) -> Unit)? = null,
  onRateService: (String, Float, String) -> Unit,
  onOpenRatingDialog: ((ServiceRequest) -> Unit)? = null,
  onReplyToReview: (reviewId: String, replyText: String) -> Unit = { _, _ -> },
  onEditBudget: (ServiceRequest) -> Unit = {},
  onAcceptProposal: (String) -> Unit = {},
  onRejectProposal: (String) -> Unit = {},
  onProviderAcceptJob: (String) -> Unit = {},
  onProviderDeclineService: (String) -> Unit = {},
  onCancelClick: () -> Unit = {},
  onAddClientCompletedPhoto: (serviceId: String, photoUri: String, photoName: String, caption: String) -> Unit = { _, _, _, _ -> },
  onRemoveClientCompletedPhoto: (serviceId: String, photoId: String) -> Unit = { _, _ -> },
  onOpenRatingDialogWithStars: ((ServiceRequest, Float) -> Unit)? = null,
  onConfirmCompletedAndRate: ((ServiceRequest) -> Unit)? = null
) {
  if (currentUser.role == UserRole.PRESTADOR) {
    val currentProviderProposal = proposals.firstOrNull { currentUser.ownsId(it.providerId) }
    val hasAcceptedDirectly = proposals.any {
      currentUser.ownsId(it.providerId) &&
        it.isDirectAcceptance &&
        it.status != ProposalStatus.RECUSADA
    }
    ProviderServiceDetailView(
      service = service,
      currentUser = currentUser,
      latestProposal = currentProviderProposal ?: proposals.firstOrNull(),
      hasAcceptedDirectly = hasAcceptedDirectly,
      matchedReview = reviews.firstOrNull { it.serviceId == service.id },
      onBack = onBack,
      onSubmitProposal = onSubmitProposal,
      onOpenChat = onOpenChat,
      onUpdateStatus = onUpdateStatus,
      onCompleteServiceWithPhoto = onCompleteServiceWithPhoto,
      onCompleteServiceWithMedia = onCompleteServiceWithMedia,
      onProviderAcceptJob = onProviderAcceptJob,
      onProviderDeclineService = onProviderDeclineService,
      onReplyToReview = onReplyToReview,
      onCancelClick = onCancelClick
    )
  } else {
    ClientServiceDetailView(
      service = service,
      currentUser = currentUser,
      proposals = proposals,
      reviews = reviews,
      serviceChatMessages = serviceChatMessages,
      onBack = onBack,
      onOpenChat = onOpenChat,
      onUpdateStatus = onUpdateStatus,
      onRateService = onRateService,
      onOpenRatingDialog = onOpenRatingDialog,
      onOpenRatingDialogWithStars = onOpenRatingDialogWithStars,
      onEditBudget = onEditBudget,
      onAcceptProposal = onAcceptProposal,
      onRejectProposal = onRejectProposal,
      onCancelClick = onCancelClick,
      onAddClientCompletedPhoto = onAddClientCompletedPhoto,
      onRemoveClientCompletedPhoto = onRemoveClientCompletedPhoto,
      onConfirmCompletedAndRate = onConfirmCompletedAndRate
    )
  }
}

@Composable
private fun ProviderServiceDetailView(
  service: ServiceRequest,
  currentUser: User,
  latestProposal: Proposal? = null,
  hasAcceptedDirectly: Boolean = false,
  matchedReview: ServiceReview? = null,
  onBack: () -> Unit,
  onSubmitProposal: (String, Double, Int, String) -> Unit,
  onOpenChat: () -> Unit,
  onUpdateStatus: (String, ServiceStatus) -> Unit,
  onCompleteServiceWithPhoto: (serviceId: String, photoUri: String?, photoName: String?, notes: String?) -> Unit = { sId, _, _, _ ->
    onUpdateStatus(sId, ServiceStatus.CONCLUIDO)
  },
  onCompleteServiceWithMedia: ((
    serviceId: String,
    photoUri: String?,
    photoName: String?,
    videoUri: String?,
    videoName: String?,
    videoThumbnailUri: String?,
    videoDurationMs: Long?,
    notes: String?
  ) -> Unit)? = null,
  onProviderAcceptJob: (String) -> Unit,
  onProviderDeclineService: (String) -> Unit = {},
  onReplyToReview: (reviewId: String, replyText: String) -> Unit = { _, _ -> },
  onCancelClick: () -> Unit = {}
) {
  var showSendProposalDialog by remember { mutableStateOf(false) }
  var showRouteDialog by remember { mutableStateOf(false) }
  var showCompleteServiceDialog by remember { mutableStateOf(false) }
  val context = LocalContext.current

  val isChatUnlocked = !service.acceptedProviderId.isNullOrBlank() &&
    currentUser.ownsId(service.acceptedProviderId) &&
    (service.status == ServiceStatus.ACEITO ||
      service.status == ServiceStatus.EM_EXECUCAO ||
      service.status == ServiceStatus.CONCLUIDO)
  val feeAmount = (service.finalAgreedPrice ?: service.proposedBudget) * 0.10

  Scaffold(
    topBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("btn_provider_detail_back")
        ) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
        }
        Text(
          text = "Detalhes do Serviço",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      }
    }
  ) { padding ->
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      contentAlignment = Alignment.TopCenter
    ) {
      val spec = remember(maxWidth) { resolveAdaptiveSpec(maxWidth) }
      LazyColumn(
        modifier = Modifier
          .widthIn(max = spec.detailMaxWidth)
          .fillMaxSize()
          .padding(horizontal = spec.horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
      ) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            // Header: Status badge & Urgency (as in IMG_4866.jpeg)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              StatusBadge(status = service.status)
              Text(
                text = "Urgência: ${service.urgency}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = WarningAmber
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
              text = service.title,
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 26.sp
              )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            Text(
              text = service.description,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            OrderParticipantsSection(
              service = service,
              latestProposal = latestProposal
            )

            if (service.status == ServiceStatus.CANCELADO && !service.cancellationReason.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(12.dp))
              ServiceCancellationBannerCard(
                service = service,
                viewerRole = currentUser.role,
                modifier = Modifier.testTag("detail_cancellation_reason_card")
              )
            }

            if (currentUser.ownsId(service.acceptedProviderId) &&
              (service.status == ServiceStatus.ACEITO || service.status == ServiceStatus.EM_EXECUCAO)
            ) {
              Spacer(modifier = Modifier.height(12.dp))
              Surface(
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, InfoBlue.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("provider_location_tracking_banner")
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = InfoBlue,
                    modifier = Modifier.size(18.dp)
                  )
                  Column {
                    Text(
                      text = "Histórico de localização ativo",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = InfoBlue
                    )
                    Text(
                      text = "${service.locationPointsCount} ponto(s) registrado(s) até a finalização do serviço.",
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                      color = Color(0xFF1E3A8A)
                    )
                  }
                }
              }
            }

            if (!service.attachmentPhotoUri.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(12.dp))
              ServiceAttachedPhotoCard(
                title = "Foto Anexada pelo Cliente",
                photoUri = service.attachmentPhotoUri,
                photoName = service.attachmentPhotoName,
                accentColor = InfoBlue,
                cardTag = "service_detail_photo_card"
              )
            }

            if (!service.attachmentVideoUri.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(12.dp))
              ServiceAttachedVideoCard(
                title = "Vídeo Anexado pelo Cliente",
                videoUri = service.attachmentVideoUri,
                videoName = service.attachmentVideoName,
                thumbnailUri = service.attachmentVideoThumbnailUri,
                durationMs = service.attachmentVideoDurationMs,
                accentColor = InfoBlue,
                cardTag = "service_detail_video_card"
              )
            }

            if (!service.completionPhotoUri.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(12.dp))
              ServiceAttachedPhotoCard(
                title = "Foto do Serviço Finalizado",
                photoUri = service.completionPhotoUri,
                photoName = service.completionPhotoName,
                subtitleNote = service.completionNotes,
                accentColor = EmeraldWallet,
                cardTag = "service_detail_completion_photo_card"
              )
            }

            if (!service.completionVideoUri.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(12.dp))
              ServiceAttachedVideoCard(
                title = "Vídeo do Serviço Finalizado",
                videoUri = service.completionVideoUri,
                videoName = service.completionVideoName,
                thumbnailUri = service.completionVideoThumbnailUri,
                durationMs = service.completionVideoDurationMs,
                subtitleNote = service.completionNotes,
                accentColor = EmeraldWallet,
                cardTag = "service_detail_completion_video_card"
              )
            }

            if (service.status == ServiceStatus.CONCLUIDO && service.clientCompletionPhotos.isNotEmpty()) {
              Spacer(modifier = Modifier.height(12.dp))
              ClientCompletedPhotosSection(
                photos = service.clientCompletionPhotos,
                canEdit = false,
                onAddPhoto = { _, _, _ -> },
                onRemovePhoto = {}
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // CARD: LOCALIZAÇÃO DO CLIENTE (IMG_4866.jpeg)
            // ==========================================
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("provider_client_location_card")
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.LocationOn,
                      contentDescription = null,
                      tint = DangerRed,
                      modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Localização do Cliente",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                  }

                  // Distance Pill Badge
                  Surface(
                    color = InfoBlue.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                  ) {
                    Text(
                      text = "${service.effectiveLocation.distanceKm} km de você",
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = InfoBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                      ),
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Full Street Address & City
                Text(
                  text = service.effectiveLocation.address,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp)
                )
                Text(
                  text = "${service.effectiveLocation.neighborhood} • ${service.effectiveLocation.city}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Reference Point
                if (service.effectiveLocation.referencePoint.isNotBlank()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.Place,
                      contentDescription = null,
                      tint = InfoBlue,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "Ref: ${service.effectiveLocation.referencePoint}",
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Travel Time Header
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "⏱ Tempo estimado de deslocamento:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = service.effectiveLocation.trafficCondition,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold
                    ),
                    color = if (service.effectiveLocation.trafficCondition.contains("moderado", ignoreCase = true)) WarningAmber else EmeraldWallet
                  )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Transport Mode Cards (3 white rounded pills)
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  // Car
                  Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier.weight(1f)
                  ) {
                    Column(
                      modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                      horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                      Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        "${service.effectiveLocation.drivingTimeMinutes} min",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        color = InfoBlue
                      )
                    }
                  }

                  // Moto
                  Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier.weight(1f)
                  ) {
                    Column(
                      modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                      horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                      Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        "${service.effectiveLocation.motoTimeMinutes} min",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        color = WarningAmber
                      )
                    }
                  }

                  // Bus / Metro
                  Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier.weight(1f)
                  ) {
                    Column(
                      modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                      horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                      Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = Color(0xFF4B5563), modifier = Modifier.size(18.dp))
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        "${service.effectiveLocation.transitTimeMinutes} min",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        color = Color(0xFF374151)
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Best route advice
                Text(
                  text = "Via: ${service.effectiveLocation.bestRoute}",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action buttons: "Ver Rota e Custos" & "Traçar GPS"
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  OutlinedButton(
                    onClick = { showRouteDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                      .weight(1f)
                      .testTag("btn_provider_view_route")
                  ) {
                    Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver Rota e Custos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }

                  Button(
                    onClick = {
                      val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode("${service.effectiveLocation.address}, ${service.effectiveLocation.city}")}")
                      val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                        setPackage("com.google.android.apps.maps")
                      }
                      try {
                        context.startActivity(mapIntent)
                      } catch (e: Exception) {
                        showRouteDialog = true
                      }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = InfoBlue),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                      .weight(1f)
                      .testTag("btn_provider_open_gps")
                  ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Traçar GPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // CARD FINANCEIRO & AÇÕES LADO A LADO (IMG_4866.jpeg)
            // ==========================================
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("provider_financial_action_card")
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                // Row 1: Valor Proposto pelo Cliente
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Valor Proposto pelo Cliente:",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                  )
                  Text(
                    text = "R$ ${String.format("%.2f", service.proposedBudget)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = InfoBlue
                    )
                  )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row 2: Taxa de 10% (debitada da carteira)
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Taxa de 10% (debitada da carteira):",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                  )
                  Text(
                    text = "R$ ${String.format("%.2f", feeAmount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = WarningAmber
                    )
                  )
                }

                if (service.status == ServiceStatus.ABERTO || service.status == ServiceStatus.EM_NEGOCIACAO) {
                  if (hasAcceptedDirectly) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                      color = EmeraldWalletLight,
                      border = BorderStroke(1.dp, EmeraldWallet.copy(alpha = 0.45f)),
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("provider_waiting_client_choice_banner")
                    ) {
                      Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                      ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldWallet, modifier = Modifier.size(18.dp))
                        Text(
                          text = "Você aceitou este pedido! Aguardando o cliente escolher você para liberar o chat privado.",
                          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                          color = Color(0xFF065F46)
                        )
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(14.dp))

                  // BOTÕES LADO A LADO (Mudar Valor & Aceitar)
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                    // Botão "Mudar Valor"
                    Button(
                      onClick = { showSendProposalDialog = true },
                      colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF60A5FA),
                        contentColor = Color.White
                      ),
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = PaddingValues(vertical = 12.dp),
                      modifier = Modifier
                        .weight(1f)
                        .testTag("btn_provider_mudar_valor")
                    ) {
                      Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Mudar Valor", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Botão "Aceitar"
                    Button(
                      onClick = { onProviderAcceptJob(service.id) },
                      enabled = !hasAcceptedDirectly,
                      colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldWallet,
                        contentColor = Color.White,
                        disabledContainerColor = EmeraldWallet.copy(alpha = 0.45f),
                        disabledContentColor = Color.White
                      ),
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = PaddingValues(vertical = 12.dp),
                      modifier = Modifier
                        .weight(1f)
                        .testTag("btn_provider_aceitar")
                    ) {
                      Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = if (hasAcceptedDirectly) "Aceite Enviado" else "Aceitar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  OutlinedButton(
                    onClick = { onProviderDeclineService(service.id) },
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .heightIn(min = 48.dp)
                      .testTag("btn_provider_decline_service")
                  ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = stringResource(R.string.provider_decline_service_button),
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // STATUS DE EXECUÇÃO & CHAT SEGURO
            // ==========================================
            if (isChatUnlocked) {
              Surface(
                color = EmeraldWalletLight,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldWallet, modifier = Modifier.size(20.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Serviço contratado! Chat seguro liberado com o cliente.",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = EmeraldWallet
                  )
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              Button(
                onClick = onOpenChat,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("btn_provider_open_chat")
              ) {
                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Abrir Chat Privado Seguro", fontWeight = FontWeight.Bold)
              }

              if (service.status == ServiceStatus.ACEITO) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                  onClick = { onUpdateStatus(service.id, ServiceStatus.EM_EXECUCAO) },
                  colors = ButtonDefaults.buttonColors(containerColor = InfoBlue),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_provider_start_service")
                ) {
                  Text("Iniciar Execução do Serviço", fontWeight = FontWeight.Bold)
                }
                if (currentUser.ownsId(service.acceptedProviderId)) {
                  Spacer(modifier = Modifier.height(8.dp))
                  OutlinedButton(
                    onClick = { showCompleteServiceDialog = true },
                    border = BorderStroke(1.dp, EmeraldWallet),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldWallet),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .heightIn(min = 48.dp)
                      .testTag("btn_provider_finish_service_direct")
                  ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Finalizar & Anexar Foto", fontWeight = FontWeight.Bold)
                  }
                }
              } else if (service.status == ServiceStatus.EM_EXECUCAO) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                  onClick = { showCompleteServiceDialog = true },
                  colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("btn_provider_finish_service")
                ) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Finalizar & Concluir Serviço", fontWeight = FontWeight.Bold)
                }
              }

              if (
                currentUser.ownsId(service.acceptedProviderId) &&
                (service.status == ServiceStatus.ACEITO || service.status == ServiceStatus.EM_EXECUCAO)
              ) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                  onClick = onCancelClick,
                  border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.65f)),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("btn_provider_cancel_service")
                ) {
                  Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Cancelar Serviço", fontWeight = FontWeight.Bold)
                }
              }
            } else if (service.status == ServiceStatus.CANCELADO) {
              Surface(
                color = DangerRedLight,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Cancel, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Este serviço foi cancelado e não aceita mais propostas.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = DangerRed
                  )
                }
              }
            } else {
              Surface(
                color = WarningAmberLight,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Lock, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "O chat privado só é liberado depois que você aceitar o pedido e o cliente escolher você para realizar o serviço.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color(0xFF78350F)
                  )
                }
              }
            }
          }
        }
      }

      if (service.status == ServiceStatus.CONCLUIDO && (matchedReview != null || service.ratingGiven != null)) {
        item {
          val effectiveReview = matchedReview ?: ServiceReview(
            id = service.id,
            serviceId = service.id,
            serviceTitle = service.title,
            serviceCategory = service.category,
            clientId = service.clientId,
            clientName = service.clientName,
            clientPhotoUrl = service.clientPhotoUrl,
            providerId = service.acceptedProviderId ?: currentUser.id,
            providerName = service.acceptedProviderName ?: currentUser.name,
            rating = service.ratingGiven ?: 5f,
            comment = service.reviewComment.orEmpty(),
            complimentTags = service.reviewTags,
            createdAt = service.reviewedAt ?: service.completedAt ?: service.createdAt
          )
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "Avaliação do Cliente",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            ReviewItemCard(
              review = effectiveReview,
              canReply = currentUser.ownsId(service.acceptedProviderId),
              onReply = { replyText -> onReplyToReview(effectiveReview.id, replyText) }
            )
          }
        }
      }
      }
    }
  }

  // Dialog de Contraproposta ("Mudar Valor")
  if (showSendProposalDialog) {
    var priceText by remember { mutableStateOf(String.format("%.2f", service.proposedBudget)) }
    var daysText by remember { mutableStateOf("1") }
    var descText by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showSendProposalDialog = false },
      title = { Text("Mudar Valor / Propor Contraproposta") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "O cliente propôs R$ ${String.format("%.2f", service.proposedBudget)}. Digite seu novo valor:",
            style = MaterialTheme.typography.bodySmall
          )
          OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it },
            label = { Text("Seu Valor Proposto (R$)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = daysText,
            onValueChange = { daysText = it.filter { c -> c.isDigit() } },
            label = { Text("Prazo estimado (dias)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = descText,
            onValueChange = { descText = it },
            label = { Text("Mensagem / Detalhes inclusos") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val price = priceText.replace(",", ".").toDoubleOrNull() ?: service.proposedBudget
            val days = daysText.toIntOrNull() ?: 1
            onSubmitProposal(service.id, price, days, descText)
            showSendProposalDialog = false
          }
        ) {
          Text("Enviar Nova Proposta")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSendProposalDialog = false }) {
          Text("Cancelar")
        }
      }
    )
  }

  if (showRouteDialog) {
    ClientRouteDetailsDialog(
      service = service,
      onDismiss = { showRouteDialog = false }
    )
  }

  if (showCompleteServiceDialog) {
    CompleteServiceWithPhotoDialog(
      service = service,
      onDismiss = { showCompleteServiceDialog = false },
      onConfirm = { photoUri, photoName, notes ->
        onCompleteServiceWithPhoto(service.id, photoUri, photoName, notes)
        showCompleteServiceDialog = false
      },
      onConfirmWithMedia = { photoUri, photoName, videoUri, videoName, videoThumbnailUri, videoDurationMs, notes ->
        if (onCompleteServiceWithMedia != null) {
          onCompleteServiceWithMedia(
            service.id,
            photoUri,
            photoName,
            videoUri,
            videoName,
            videoThumbnailUri,
            videoDurationMs,
            notes
          )
        } else {
          onCompleteServiceWithPhoto(service.id, photoUri, photoName, notes)
        }
        showCompleteServiceDialog = false
      }
    )
  }
}

/**
 * TELA EM DETALHES DE SERVIÇO EXCLUSIVA PARA O CLIENTE
 * Contém:
 * - Endereço de atendimento direto (sem cálculos de rota para si mesmo)
 * - Foto anexada no pedido e foto do serviço finalizado (se houver)
 * - Orçamento proposto com botão de alteração
 * - Status de contratação, cancelamento com motivo e chat com prestador
 * - Lista de propostas recebidas de prestadores com botão para aceitar
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClientServiceDetailView(
  service: ServiceRequest,
  currentUser: User,
  proposals: List<Proposal>,
  reviews: List<ServiceReview> = emptyList(),
  serviceChatMessages: List<ChatMessage> = emptyList(),
  onBack: () -> Unit,
  onOpenChat: () -> Unit,
  onUpdateStatus: (String, ServiceStatus) -> Unit,
  onRateService: (String, Float, String) -> Unit,
  onOpenRatingDialog: ((ServiceRequest) -> Unit)? = null,
  onEditBudget: (ServiceRequest) -> Unit,
  onAcceptProposal: (String) -> Unit,
  onRejectProposal: (String) -> Unit = {},
  onCancelClick: () -> Unit = {},
  onAddClientCompletedPhoto: (serviceId: String, photoUri: String, photoName: String, caption: String) -> Unit = { _, _, _, _ -> },
  onRemoveClientCompletedPhoto: (serviceId: String, photoId: String) -> Unit = { _, _ -> },
  onOpenRatingDialogWithStars: ((ServiceRequest, Float) -> Unit)? = null,
  onConfirmCompletedAndRate: ((ServiceRequest) -> Unit)? = null
) {
  var reviewsModalTarget by remember { mutableStateOf<Proposal?>(null) }
  var acceptedProviderReviewsOpen by remember { mutableStateOf(false) }

  var interestFilter by remember(service.id) { mutableStateOf("TODOS") }
  val isOrderClosedWithProvider = !service.acceptedProviderId.isNullOrBlank() ||
    proposals.any { it.status == ProposalStatus.ACEITA }
  val acceptedProposalId = remember(proposals, service.acceptedProviderId) {
    proposals.firstOrNull { it.status == ProposalStatus.ACEITA }?.id
      ?: if (!service.acceptedProviderId.isNullOrBlank()) {
        proposals.firstOrNull {
          it.status != ProposalStatus.RECUSADA && it.providerId == service.acceptedProviderId
        }?.id
      } else null
  }
  val sortedProposals = remember(proposals, acceptedProposalId, isOrderClosedWithProvider) {
    proposals.sortedWith(
      compareBy<Proposal>(
        {
          when {
            it.id == acceptedProposalId && it.status != ProposalStatus.RECUSADA -> 0
            it.status != ProposalStatus.RECUSADA && !isOrderClosedWithProvider && it.isDirectAcceptance -> 1
            it.status != ProposalStatus.RECUSADA && !isOrderClosedWithProvider -> 2
            else -> 3
          }
        },
        { -it.createdAt }
      )
    )
  }
  val activeInterestedProposals = remember(sortedProposals, acceptedProposalId, isOrderClosedWithProvider) {
    sortedProposals.filter {
      it.status != ProposalStatus.RECUSADA &&
        (!isOrderClosedWithProvider || it.id == acceptedProposalId)
    }
  }
  val refusedInterestedProposals = remember(sortedProposals, acceptedProposalId, isOrderClosedWithProvider) {
    sortedProposals.filter {
      it.status == ProposalStatus.RECUSADA ||
        (isOrderClosedWithProvider && it.id != acceptedProposalId)
    }
  }
  val displayedProposals = remember(sortedProposals, activeInterestedProposals, refusedInterestedProposals, interestFilter) {
    when (interestFilter) {
      "ATIVOS" -> activeInterestedProposals
      "RECUSADOS" -> refusedInterestedProposals
      else -> sortedProposals
    }
  }
  val directAcceptCount = remember(activeInterestedProposals) {
    activeInterestedProposals.count { it.isDirectAcceptance }
  }
  val counterOrCustomCount = remember(activeInterestedProposals) {
    activeInterestedProposals.count { !it.isDirectAcceptance }
  }

  val isChatUnlocked = !service.acceptedProviderId.isNullOrBlank() &&
    (service.status == ServiceStatus.ACEITO ||
      service.status == ServiceStatus.EM_EXECUCAO ||
      service.status == ServiceStatus.CONCLUIDO ||
      (currentUser.role == UserRole.ADMIN && service.status == ServiceStatus.CANCELADO))
  val canEditBudget = (service.status == ServiceStatus.ABERTO || service.status == ServiceStatus.EM_NEGOCIACAO)

  val matchedServiceReview = reviews.firstOrNull { it.serviceId == service.id }
  val effectiveRatingGiven = service.ratingGiven ?: matchedServiceReview?.rating
  val effectiveReviewComment = service.reviewComment ?: matchedServiceReview?.comment?.takeIf { it.isNotBlank() }
  val effectiveReviewTags = if (service.reviewTags.isNotEmpty()) {
    service.reviewTags
  } else {
    matchedServiceReview?.complimentTags.orEmpty()
  }

  Scaffold(
    topBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("btn_client_detail_back")
        ) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
        }
        Text(
          text = if (currentUser.role == UserRole.ADMIN) "Detalhes do Pedido (Admin)" else "Detalhes do Meu Pedido",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      }
    }
  ) { padding ->
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      contentAlignment = Alignment.TopCenter
    ) {
      val spec = remember(maxWidth) { resolveAdaptiveSpec(maxWidth) }
      LazyColumn(
        modifier = Modifier
          .widthIn(max = spec.detailMaxWidth)
          .fillMaxSize()
          .padding(horizontal = spec.horizontalPadding)
          .testTag("client_service_detail_list"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
      ) {
        item(key = "client_order_header_card") {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("client_order_header_card")
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                StatusBadge(status = service.status)
                Text(
                  text = "Urgência: ${service.urgency}",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = WarningAmber
                )
              }

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = service.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
              )

              Spacer(modifier = Modifier.height(6.dp))

              Text(
                text = service.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
              )

              Spacer(modifier = Modifier.height(12.dp))

              OrderParticipantsSection(
                service = service,
                latestProposal = sortedProposals.firstOrNull()
              )

              if (sortedProposals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                  color = if (service.acceptedProviderId.isNullOrBlank()) {
                    InfoBlue.copy(alpha = 0.10f)
                  } else {
                    EmeraldWalletLight.copy(alpha = 0.7f)
                  },
                  border = BorderStroke(
                    1.dp,
                    if (service.acceptedProviderId.isNullOrBlank()) {
                      InfoBlue.copy(alpha = 0.35f)
                    } else {
                      EmeraldWallet.copy(alpha = 0.45f)
                    }
                  ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("client_interested_summary_banner")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                    Icon(
                      imageVector = if (service.acceptedProviderId.isNullOrBlank()) {
                        Icons.Default.Handshake
                      } else {
                        Icons.Default.CheckCircle
                      },
                      contentDescription = null,
                      tint = if (service.acceptedProviderId.isNullOrBlank()) InfoBlue else EmeraldWallet,
                      modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = if (service.acceptedProviderId.isNullOrBlank()) {
                          if (activeInterestedProposals.isNotEmpty()) {
                            "${activeInterestedProposals.size} prestador(es) interessado(s) aguardando sua escolha"
                          } else {
                            "${sortedProposals.size} prestador(es) no histórico deste pedido"
                          }
                        } else {
                          "Prestador confirmado: ${service.acceptedProviderName ?: "Escolhido"}"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (service.acceptedProviderId.isNullOrBlank()) InfoBlue else Color(0xFF065F46)
                      )
                      val breakdownParts = buildList {
                        if (directAcceptCount > 0) add("$directAcceptCount aceite direto")
                        if (counterOrCustomCount > 0) add("$counterOrCustomCount proposta(s)")
                        if (refusedInterestedProposals.isNotEmpty()) add("${refusedInterestedProposals.size} recusado(s)")
                      }
                      if (breakdownParts.isNotEmpty()) {
                        Text(
                          text = breakdownParts.joinToString(" • "),
                          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                    }
                  }
                }
              } else {
                ClientOrderDetailsBodyContent(
                  service = service,
                  currentUser = currentUser,
                  isChatUnlocked = isChatUnlocked,
                  canEditBudget = canEditBudget,
                  matchedServiceReview = matchedServiceReview,
                  effectiveRatingGiven = effectiveRatingGiven,
                  effectiveReviewComment = effectiveReviewComment,
                  effectiveReviewTags = effectiveReviewTags,
                  onOpenChat = onOpenChat,
                  onRateService = onRateService,
                  onOpenRatingDialog = onOpenRatingDialog,
                  onOpenRatingDialogWithStars = onOpenRatingDialogWithStars,
                  onEditBudget = onEditBudget,
                  onCancelClick = onCancelClick,
                  onOpenAcceptedProviderReviews = { acceptedProviderReviewsOpen = true },
                  onAddClientCompletedPhoto = onAddClientCompletedPhoto,
                  onRemoveClientCompletedPhoto = onRemoveClientCompletedPhoto,
                  onConfirmCompletedAndRate = onConfirmCompletedAndRate,
                  showRatingSection = true
                )
              }
            }
          }
        }

        if (currentUser.role == UserRole.ADMIN && isChatUnlocked) {
          item(key = "admin_service_chat_monitor_item") {
            val nonSystemMessages = remember(serviceChatMessages) {
              serviceChatMessages.filterNot { it.isSystemNotice }.sortedBy { it.timestamp }
            }
            val latestPreview = remember(nonSystemMessages) { nonSystemMessages.takeLast(3) }
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_service_chat_monitor_card")
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.Visibility,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Conversa Cliente ↔ Prestador",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.primary
                    )
                  }
                  Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Text(
                      text = "${nonSystemMessages.size} msg(s)",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                  }
                }

                Text(
                  text = "Cliente: ${service.clientName}  ↔  Prestador: ${service.acceptedProviderName ?: "Contratado"}",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (latestPreview.isEmpty()) {
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = stringResource(R.string.admin_chat_empty_messages),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.padding(10.dp)
                    )
                  }
                } else {
                  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    latestPreview.forEach { msg ->
                      val isClientMsg = msg.senderRole == UserRole.CLIENTE
                      Surface(
                        color = if (isClientMsg) InfoBlue.copy(alpha = 0.08f) else EmeraldWalletLight.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                          Text(
                            text = "${if (isClientMsg) "Cliente" else "Prestador"} • ${msg.senderName}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = if (isClientMsg) InfoBlue else Color(0xFF065F46)
                          )
                          Text(
                            text = ContactFilter.maskSensitiveData(msg.content),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                          )
                        }
                      }
                    }
                  }
                }

                Button(
                  onClick = onOpenChat,
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("btn_admin_open_service_chat_${service.id}")
                ) {
                  Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = stringResource(R.string.admin_chat_open_full_cta),
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }

        if (sortedProposals.isNotEmpty() &&
          (service.status == ServiceStatus.CONCLUIDO || effectiveRatingGiven != null) &&
          currentUser.ownsId(service.clientId)
        ) {
          item(key = "client_top_rating_section") {
            ClientPostServiceRatingSection(
              service = service,
              matchedServiceReview = matchedServiceReview,
              effectiveRatingGiven = effectiveRatingGiven,
              effectiveReviewComment = effectiveReviewComment,
              effectiveReviewTags = effectiveReviewTags,
              onRateService = onRateService,
              onOpenRatingDialog = onOpenRatingDialog,
              onOpenRatingDialogWithStars = onOpenRatingDialogWithStars
            )
          }
        }

        // Todos os Prestadores Interessados e Propostas (aparece logo no topo quando houver interessados)
        item(key = "client_interested_providers_header") {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("client_interested_providers_section"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Prestadores Interessados (${sortedProposals.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              if (activeInterestedProposals.isNotEmpty() && service.acceptedProviderId.isNullOrBlank()) {
                Surface(
                  color = EmeraldWalletLight,
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Text(
                    text = "${activeInterestedProposals.size} ativo(s)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = Color(0xFF065F46),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                  )
                }
              }
            }

            if (sortedProposals.isNotEmpty()) {
              Text(
                text = if (service.acceptedProviderId.isNullOrBlank()) {
                  "Todos os prestadores interessados no seu pedido aparecem aqui. Escolha com quem fechar ou recuse."
                } else {
                  "Histórico completo de todos os prestadores interessados neste pedido."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            if (refusedInterestedProposals.isNotEmpty() && activeInterestedProposals.isNotEmpty()) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                val filterChips = listOf(
                  "TODOS" to "Todos (${sortedProposals.size})",
                  "ATIVOS" to if (isOrderClosedWithProvider) "Aceito (${activeInterestedProposals.size})" else "Aguardando (${activeInterestedProposals.size})",
                  "RECUSADOS" to "Recusados (${refusedInterestedProposals.size})"
                )
                filterChips.forEach { (key, label) ->
                  val selected = interestFilter == key
                  Surface(
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                      .heightIn(min = 40.dp)
                      .clickable { interestFilter = key }
                      .testTag("client_interest_filter_${key.lowercase()}")
                  ) {
                    Box(
                      modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                      )
                    }
                  }
                }
              }
            }
          }
        }

        if (displayedProposals.isEmpty()) {
          item(key = "client_interested_empty") {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("client_interested_providers_empty")
            ) {
              Text(
                text = if (sortedProposals.isEmpty()) {
                  "Nenhum prestador aceitou ou enviou proposta ainda. Os prestadores qualificados da região podem aceitar seu valor ou enviar propostas em breve."
                } else {
                  "Nenhum prestador neste filtro."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp)
              )
            }
          }
        } else {
          items(displayedProposals, key = { it.id }) { proposal ->
            ClientInterestedProviderCard(
              proposal = proposal,
              service = service,
              reviews = reviews,
              acceptedProposalId = acceptedProposalId,
              onAcceptProposal = onAcceptProposal,
              onRejectProposal = onRejectProposal,
              onOpenChat = onOpenChat,
              onOpenReviewsModal = { reviewsModalTarget = proposal }
            )
          }
        }

        if (sortedProposals.isNotEmpty()) {
          item(key = "client_order_full_details_card") {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("client_order_full_details_card")
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = "Informações e Ações do Pedido",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary
                )
                ClientOrderDetailsBodyContent(
                  service = service,
                  currentUser = currentUser,
                  isChatUnlocked = isChatUnlocked,
                  canEditBudget = canEditBudget,
                  matchedServiceReview = matchedServiceReview,
                  effectiveRatingGiven = effectiveRatingGiven,
                  effectiveReviewComment = effectiveReviewComment,
                  effectiveReviewTags = effectiveReviewTags,
                  onOpenChat = onOpenChat,
                  onRateService = onRateService,
                  onOpenRatingDialog = onOpenRatingDialog,
                  onOpenRatingDialogWithStars = onOpenRatingDialogWithStars,
                  onEditBudget = onEditBudget,
                  onCancelClick = onCancelClick,
                  onOpenAcceptedProviderReviews = { acceptedProviderReviewsOpen = true },
                  onAddClientCompletedPhoto = onAddClientCompletedPhoto,
                  onRemoveClientCompletedPhoto = onRemoveClientCompletedPhoto,
                  onConfirmCompletedAndRate = onConfirmCompletedAndRate,
                  showRatingSection = !currentUser.ownsId(service.clientId)
                )
              }
            }
          }
        }
      }
    }
  }

  reviewsModalTarget?.let { targetProposal ->
    val targetReviews = reviews.filter { it.providerId == targetProposal.providerId }
    ProviderReviewsDialog(
      providerName = targetProposal.providerName,
      providerCategory = targetProposal.providerCategory,
      providerPhotoUrl = targetProposal.providerPhotoUrl,
      providerAvatarColorHex = targetProposal.providerAvatarColorHex,
      averageRating = targetProposal.providerRating,
      reviews = targetReviews,
      onDismiss = { reviewsModalTarget = null }
    )
  }

  if (acceptedProviderReviewsOpen && service.acceptedProviderId != null) {
    val acceptedId = service.acceptedProviderId
    val acceptedReviews = reviews.filter { it.providerId == acceptedId }
    ProviderReviewsDialog(
      providerName = service.acceptedProviderName ?: "Prestador",
      providerCategory = service.category,
      providerPhotoUrl = service.acceptedProviderPhotoUrl,
      providerAvatarColorHex = service.acceptedProviderAvatarColorHex ?: 0xFF0D9488,
      averageRating = service.acceptedProviderRating ?: 5.0,
      reviews = acceptedReviews,
      onDismiss = { acceptedProviderReviewsOpen = false }
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClientOrderDetailsBodyContent(
  service: ServiceRequest,
  currentUser: User,
  isChatUnlocked: Boolean,
  canEditBudget: Boolean,
  matchedServiceReview: ServiceReview?,
  effectiveRatingGiven: Float?,
  effectiveReviewComment: String?,
  effectiveReviewTags: List<String>,
  onOpenChat: () -> Unit,
  onRateService: (String, Float, String) -> Unit,
  onOpenRatingDialog: ((ServiceRequest) -> Unit)?,
  onOpenRatingDialogWithStars: ((ServiceRequest, Float) -> Unit)? = null,
  onEditBudget: (ServiceRequest) -> Unit,
  onCancelClick: () -> Unit,
  onOpenAcceptedProviderReviews: () -> Unit,
  onAddClientCompletedPhoto: (serviceId: String, photoUri: String, photoName: String, caption: String) -> Unit,
  onRemoveClientCompletedPhoto: (serviceId: String, photoId: String) -> Unit,
  onConfirmCompletedAndRate: ((ServiceRequest) -> Unit)? = null,
  showRatingSection: Boolean = true
) {
  if (service.status == ServiceStatus.CANCELADO && !service.cancellationReason.isNullOrBlank()) {
    Spacer(modifier = Modifier.height(12.dp))
    ServiceCancellationBannerCard(
      service = service,
      viewerRole = currentUser.role,
      modifier = Modifier.testTag("detail_cancellation_reason_card")
    )
  }

  if (!service.attachmentPhotoUri.isNullOrBlank()) {
    Spacer(modifier = Modifier.height(12.dp))
    ServiceAttachedPhotoCard(
      title = "Foto Anexada no Pedido",
      photoUri = service.attachmentPhotoUri,
      photoName = service.attachmentPhotoName,
      accentColor = InfoBlue,
      cardTag = "service_detail_photo_card"
    )
  }

  if (!service.attachmentVideoUri.isNullOrBlank()) {
    Spacer(modifier = Modifier.height(12.dp))
    ServiceAttachedVideoCard(
      title = "Vídeo Anexado no Pedido",
      videoUri = service.attachmentVideoUri,
      videoName = service.attachmentVideoName,
      thumbnailUri = service.attachmentVideoThumbnailUri,
      durationMs = service.attachmentVideoDurationMs,
      accentColor = InfoBlue,
      cardTag = "service_detail_video_card"
    )
  }

  if (!service.completionPhotoUri.isNullOrBlank()) {
    Spacer(modifier = Modifier.height(12.dp))
    ServiceAttachedPhotoCard(
      title = "Foto do Serviço Finalizado",
      photoUri = service.completionPhotoUri,
      photoName = service.completionPhotoName,
      subtitleNote = service.completionNotes,
      accentColor = EmeraldWallet,
      cardTag = "service_detail_completion_photo_card"
    )
  }

  if (!service.completionVideoUri.isNullOrBlank()) {
    Spacer(modifier = Modifier.height(12.dp))
    ServiceAttachedVideoCard(
      title = "Vídeo do Serviço Finalizado",
      videoUri = service.completionVideoUri,
      videoName = service.completionVideoName,
      thumbnailUri = service.completionVideoThumbnailUri,
      durationMs = service.completionVideoDurationMs,
      subtitleNote = service.completionNotes,
      accentColor = EmeraldWallet,
      cardTag = "service_detail_completion_video_card"
    )
  }

  if (service.status == ServiceStatus.CONCLUIDO) {
    Spacer(modifier = Modifier.height(12.dp))
    ClientCompletedPhotosSection(
      photos = service.clientCompletionPhotos,
      canEdit = currentUser.ownsId(service.clientId),
      onAddPhoto = { photoUri, photoName, caption ->
        onAddClientCompletedPhoto(service.id, photoUri, photoName, caption)
      },
      onRemovePhoto = { photoId ->
        onRemoveClientCompletedPhoto(service.id, photoId)
      }
    )
  }

  Spacer(modifier = Modifier.height(12.dp))

  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    shape = RoundedCornerShape(10.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.LocationOn,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Column {
        Text(
          text = "Endereço do Atendimento:",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "${service.effectiveLocation.address}, ${service.effectiveLocation.neighborhood} - ${service.effectiveLocation.city}",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        if (service.effectiveLocation.referencePoint.isNotBlank()) {
          Text(
            text = "Ref: ${service.effectiveLocation.referencePoint}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }

  Spacer(modifier = Modifier.height(12.dp))

  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Meu Orçamento Proposto:", style = MaterialTheme.typography.bodySmall)
        Text(
          "R$ ${String.format("%.2f", service.proposedBudget)}",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        )
      }

      if (service.finalAgreedPrice != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Valor Final Acordado:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
          Text(
            "R$ ${String.format("%.2f", service.finalAgreedPrice)}",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = EmeraldWallet
            )
          )
        }
      }

      if (service.acceptedProviderName != null && isChatUnlocked) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Prestador Contratado: ${service.acceptedProviderName}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = EmeraldWallet,
            modifier = Modifier.weight(1f)
          )
          service.acceptedProviderId?.let { acceptedId ->
            TextButton(
              onClick = onOpenAcceptedProviderReviews,
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.testTag("btn_summary_provider_reviews_$acceptedId")
            ) {
              Icon(Icons.Default.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "${formatUserRating(service.acceptedProviderRating ?: 5.0)} • Ver avaliações",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
              )
            }
          }
        }
      }

      if (canEditBudget) {
        Spacer(modifier = Modifier.height(10.dp))
        Button(
          onClick = { onEditBudget(service) },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("btn_client_edit_budget")
        ) {
          Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Mudar Valor Proposto (Orçamento)", fontWeight = FontWeight.Bold)
        }
      }
    }
  }

  Spacer(modifier = Modifier.height(12.dp))

  if (isChatUnlocked) {
    Button(
      onClick = onOpenChat,
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .testTag("btn_client_open_chat")
    ) {
      Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "Abrir Chat Privado com ${service.acceptedProviderName ?: "o Prestador"}",
        fontWeight = FontWeight.Bold
      )
    }
  } else {
    Surface(
      color = WarningAmberLight,
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "🔒 Chat Privado Bloqueado: Vários prestadores podem aceitar ou enviar propostas. O chat só é liberado somente com o prestador que você aceitar.",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = Color(0xFF78350F)
        )
      }
    }
  }

  if ((service.status == ServiceStatus.ACEITO || service.status == ServiceStatus.EM_EXECUCAO) &&
    currentUser.role == UserRole.CLIENTE &&
    currentUser.ownsId(service.clientId) &&
    !service.acceptedProviderId.isNullOrBlank() &&
    onConfirmCompletedAndRate != null
  ) {
    Spacer(modifier = Modifier.height(10.dp))
    Button(
      onClick = { onConfirmCompletedAndRate(service) },
      colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .testTag("btn_client_confirm_completion")
    ) {
      Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = stringResource(R.string.review_client_confirm_done_cta),
        fontWeight = FontWeight.Bold
      )
    }
  }

  if (showRatingSection && (service.status == ServiceStatus.CONCLUIDO || effectiveRatingGiven != null)) {
    Spacer(modifier = Modifier.height(12.dp))
    ClientPostServiceRatingSection(
      service = service,
      matchedServiceReview = matchedServiceReview,
      effectiveRatingGiven = effectiveRatingGiven,
      effectiveReviewComment = effectiveReviewComment,
      effectiveReviewTags = effectiveReviewTags,
      onRateService = onRateService,
      onOpenRatingDialog = onOpenRatingDialog,
      onOpenRatingDialogWithStars = onOpenRatingDialogWithStars
    )
  }

  if (service.status != ServiceStatus.CONCLUIDO && service.status != ServiceStatus.CANCELADO) {
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedButton(
      onClick = onCancelClick,
      border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.65f)),
      colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .testTag("btn_client_cancel_service")
    ) {
      Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("Cancelar Pedido", fontWeight = FontWeight.Bold)
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClientPostServiceRatingSection(
  service: ServiceRequest,
  matchedServiceReview: ServiceReview?,
  effectiveRatingGiven: Float?,
  effectiveReviewComment: String?,
  effectiveReviewTags: List<String>,
  onRateService: (String, Float, String) -> Unit,
  onOpenRatingDialog: ((ServiceRequest) -> Unit)?,
  onOpenRatingDialogWithStars: ((ServiceRequest, Float) -> Unit)? = null
) {
  if (service.status == ServiceStatus.CONCLUIDO && effectiveRatingGiven == null) {
    Card(
      colors = CardDefaults.cardColors(containerColor = WarningAmberLight),
      border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.55f)),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("post_service_rating_card")
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Serviço Concluído! Como foi o atendimento?",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF78350F)
          )
        }
        Text(
          text = "Avalie ${service.acceptedProviderName ?: "o prestador"} de 1 a 5 estrelas para fortalecer a confiança na plataforma.",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF92400E)
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          (1..5).forEach { star ->
            IconButton(
              onClick = {
                when {
                  onOpenRatingDialogWithStars != null -> onOpenRatingDialogWithStars(service, star.toFloat())
                  onOpenRatingDialog != null -> onOpenRatingDialog(service)
                  else -> onRateService(service.id, star.toFloat(), "")
                }
              },
              modifier = Modifier
                .size(48.dp)
                .testTag("detail_quick_star_$star")
            ) {
              Icon(
                imageVector = Icons.Default.StarBorder,
                contentDescription = "Avaliar com $star estrelas",
                tint = WarningAmber,
                modifier = Modifier.size(30.dp)
              )
            }
          }
        }
        Button(
          onClick = {
            when {
              onOpenRatingDialogWithStars != null -> onOpenRatingDialogWithStars(service, 5.0f)
              onOpenRatingDialog != null -> onOpenRatingDialog(service)
              else -> onRateService(service.id, 5.0f, "")
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("btn_rate_service")
        ) {
          Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(stringResource(R.string.review_rate_provider_cta), fontWeight = FontWeight.Bold)
        }
      }
    }
  }

  if (effectiveRatingGiven != null) {
    Surface(
      color = WarningAmberLight,
      border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.45f)),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("submitted_service_review_card")
    ) {
      Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Sua Avaliação Enviada",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF78350F)
          )
          Text(
            text = "${formatUserRating(effectiveRatingGiven.toDouble())} / 5",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF92400E)
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
          val filledStars = kotlin.math.round(effectiveRatingGiven).toInt().coerceIn(1, 5)
          (1..5).forEach { star ->
            Icon(
              imageVector = if (star <= filledStars) Icons.Default.Star else Icons.Default.StarBorder,
              contentDescription = null,
              tint = WarningAmber,
              modifier = Modifier.size(18.dp)
            )
          }
        }
        if (effectiveReviewTags.isNotEmpty()) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            effectiveReviewTags.forEach { tag ->
              Surface(
                color = Color.White.copy(alpha = 0.75f),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = "✓ $tag",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                  color = Color(0xFF92400E),
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
              }
            }
          }
        }
        if (!effectiveReviewComment.isNullOrBlank()) {
          Text(
            text = "\"$effectiveReviewComment\"",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF78350F)
          )
        }
        matchedServiceReview?.providerReply?.takeIf { it.isNotBlank() }?.let { reply ->
          Surface(
            color = EmeraldWalletLight.copy(alpha = 0.85f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Text(
                text = "Resposta de ${service.acceptedProviderName ?: "Prestador"}:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = EmeraldWallet
              )
              Text(
                text = reply,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ClientInterestedProviderCard(
  proposal: Proposal,
  service: ServiceRequest,
  reviews: List<ServiceReview>,
  acceptedProposalId: String? = null,
  onAcceptProposal: (String) -> Unit,
  onRejectProposal: (String) -> Unit,
  onOpenChat: () -> Unit,
  onOpenReviewsModal: () -> Unit
) {
  val isOrderClosedWithProvider = acceptedProposalId != null || !service.acceptedProviderId.isNullOrBlank()
  val isThisProviderAccepted =
    proposal.status != ProposalStatus.RECUSADA &&
      if (acceptedProposalId != null) {
        proposal.id == acceptedProposalId
      } else {
        proposal.status == ProposalStatus.ACEITA ||
          (!service.acceptedProviderId.isNullOrBlank() && service.acceptedProviderId == proposal.providerId)
      }
  val isProposalRefused =
    proposal.status == ProposalStatus.RECUSADA ||
      (!isThisProviderAccepted && isOrderClosedWithProvider)
  val canClientDecide =
    (service.status == ServiceStatus.ABERTO || service.status == ServiceStatus.EM_NEGOCIACAO) &&
      !isProposalRefused &&
      !isThisProviderAccepted &&
      !isOrderClosedWithProvider
  val providerReviewsCount = reviews.count { it.providerId == proposal.providerId }

  Card(
    colors = CardDefaults.cardColors(
      containerColor = when {
        isThisProviderAccepted -> EmeraldWalletLight.copy(alpha = 0.35f)
        isProposalRefused -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surface
      }
    ),
    border = if (isThisProviderAccepted) BorderStroke(1.dp, EmeraldWallet.copy(alpha = 0.55f)) else null,
    shape = RoundedCornerShape(12.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isProposalRefused) 0.dp else 2.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("client_proposal_card_${proposal.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      if (isProposalRefused) {
        Surface(
          color = DangerRed.copy(alpha = 0.12f),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier
            .padding(bottom = 8.dp)
            .testTag("badge_provider_refused_${proposal.id}")
        ) {
          Text(
            text = "✕ Recusado",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = DangerRed,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      } else if (isThisProviderAccepted || proposal.isDirectAcceptance) {
        Surface(
          color = EmeraldWalletLight,
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier
            .padding(bottom = 8.dp)
            .testTag("badge_provider_accepted_${proposal.id}")
        ) {
          Text(
            text = when {
              isThisProviderAccepted && proposal.isDirectAcceptance -> "✓ Aceite Direto Confirmado"
              isThisProviderAccepted -> "✓ Proposta Aceita"
              else -> "✓ Aceite Direto • Aguardando sua escolha"
            },
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = Color(0xFF065F46),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      } else {
        Surface(
          color = InfoBlue.copy(alpha = 0.12f),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier
            .padding(bottom = 8.dp)
            .testTag("badge_provider_proposal_${proposal.id}")
        ) {
          Text(
            text = if (proposal.counterOffers.isNotEmpty()) {
              "🔁 Proposta em Negociação (${proposal.counterOffers.size})"
            } else {
              "💬 Proposta Enviada"
            },
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = InfoBlue,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (isThisProviderAccepted) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .weight(1f)
              .testTag("accepted_provider_profile_${proposal.providerId}")
          ) {
            ProfileAvatar(
              name = proposal.providerName,
              photoUrl = proposal.providerPhotoUrl,
              avatarColorHex = proposal.providerAvatarColorHex,
              size = 40.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = proposal.providerName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .clickable { onOpenReviewsModal() }
                  .padding(vertical = 2.dp)
                  .testTag("btn_view_provider_reviews_${proposal.providerId}")
              ) {
                Icon(
                  imageVector = Icons.Default.Star,
                  contentDescription = "Nota",
                  tint = WarningAmber,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = formatUserRating(proposal.providerRating),
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                  text = if (providerReviewsCount > 0) " ($providerReviewsCount)" else " • Ver perfil",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  color = MaterialTheme.colorScheme.primary
                )
              }
              if (proposal.providerCategory.isNotBlank()) {
                Text(
                  text = proposal.providerCategory,
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        } else {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
              shape = CircleShape,
              modifier = Modifier.size(38.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Handshake,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(19.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = proposal.providerName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = buildString {
                  append(if (proposal.isDirectAcceptance) "Aceitou seu valor" else "Enviou proposta")
                  if (proposal.providerCategory.isNotBlank()) {
                    append(" • ${proposal.providerCategory}")
                  }
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = when {
                  isProposalRefused && isOrderClosedWithProvider -> "Outro prestador foi selecionado"
                  isProposalRefused -> "Recusada pelo cliente"
                  else -> "Perfil e nota liberados após aceitar"
                },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (isProposalRefused) DangerRed else MaterialTheme.colorScheme.primary
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "R$ ${String.format("%.2f", proposal.currentPrice)}",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = if (proposal.isDirectAcceptance) {
          proposal.description
        } else {
          "Previsão: ${proposal.estimatedDays} dia(s) • ${proposal.description}"
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      if (canClientDecide) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = { onRejectProposal(proposal.id) },
            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.65f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("btn_client_reject_proposal_${proposal.id}")
          ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Recusar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }

          Button(
            onClick = { onAcceptProposal(proposal.id) },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1.35f)
              .heightIn(min = 48.dp)
              .testTag("btn_client_accept_proposal_${proposal.id}")
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Aceitar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      } else if (isThisProviderAccepted) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          color = EmeraldWalletLight,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "✓ Prestador escolhido • Chat liberado",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = EmeraldWallet,
              modifier = Modifier.weight(1f)
            )
            Button(
              onClick = onOpenChat,
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier
                .heightIn(min = 36.dp)
                .testTag("btn_accepted_proposal_chat_${proposal.id}")
            ) {
              Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Chat", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
          }
        }
      } else if (isProposalRefused) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = if (isOrderClosedWithProvider) {
            "Outro prestador foi escolhido para este pedido."
          } else {
            "Você recusou este prestador."
          },
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = DangerRed
        )
      }
    }
  }
}

@Composable
fun ProviderRadarEmptyState(
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "provider_radar_transition")
  val sweepAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2600, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "provider_radar_sweep"
  )
  val pulseProgress by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "provider_radar_pulse"
  )
  val primaryColor = MaterialTheme.colorScheme.primary
  val accentColor = EmeraldWallet

  Column(
    modifier = modifier.testTag("provider_radar_empty_state"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier.size(164.dp),
      contentAlignment = Alignment.Center
    ) {
      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .testTag("provider_radar_canvas")
      ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f

        listOf(0.32f, 0.58f, 0.84f, 1.0f).forEach { fraction ->
          drawCircle(
            color = primaryColor.copy(alpha = 0.16f),
            radius = maxRadius * fraction,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
          )
        }

        drawCircle(
          color = accentColor.copy(alpha = (1f - pulseProgress) * 0.35f),
          radius = maxRadius * (0.25f + 0.75f * pulseProgress),
          center = center,
          style = Stroke(width = 2.5.dp.toPx())
        )

        rotate(degrees = sweepAngle, pivot = center) {
          drawCircle(
            brush = Brush.sweepGradient(
              0.0f to Color.Transparent,
              0.72f to Color.Transparent,
              0.92f to accentColor.copy(alpha = 0.24f),
              1.0f to primaryColor.copy(alpha = 0.42f)
            ),
            radius = maxRadius,
            center = center
          )
          drawLine(
            color = accentColor.copy(alpha = 0.85f),
            start = center,
            end = Offset(center.x + maxRadius, center.y),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
          )
        }
      }

      Surface(
        color = primaryColor.copy(alpha = 0.14f),
        shape = CircleShape,
        modifier = Modifier.size(52.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Radar,
            contentDescription = "Procurando serviços",
            tint = primaryColor,
            modifier = Modifier.size(28.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = "Procurando serviços",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onBackground,
      modifier = Modifier.testTag("provider_radar_title")
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = "Nenhum pedido pendente no momento. Assim que um cliente publicar um serviço ou aceitar você, ele aparecerá aqui.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center,
      modifier = Modifier.widthIn(max = 320.dp)
    )
  }
}

@Composable
fun ClientRouteDetailsDialog(
  service: ServiceRequest,
  onDismiss: () -> Unit
) {
  val loc = service.effectiveLocation
  val context = LocalContext.current
  val estimatedFuelCost = loc.distanceKm * 0.75 // Approx R$ 0,75/km in Brazil

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Route, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Deslocamento & Rota", fontWeight = FontWeight.Bold, fontSize = 18.sp)
      }
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        // Destination card
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text("Destino do Cliente:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(loc.address, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("${loc.neighborhood} • ${loc.city}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (loc.referencePoint.isNotBlank()) {
              Spacer(modifier = Modifier.height(2.dp))
              Text("Ref: ${loc.referencePoint}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.primary)
            }
          }
        }

        // Distance & Times breakdown
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text("Distância:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${loc.distanceKm} km", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
          }
          Column(horizontalAlignment = Alignment.End) {
            Text("Trânsito atual:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(loc.trafficCondition, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EmeraldWallet)
          }
        }

        // Travel mode cards
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
            .padding(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = InfoBlue, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("De Carro", fontSize = 12.sp)
            }
            Text("~${loc.drivingTimeMinutes} minutos", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = InfoBlue)
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("De Moto", fontSize = 12.sp)
            }
            Text("~${loc.motoTimeMinutes} minutos", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WarningAmber)
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = Color(0xFF4B5563), modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Transporte Público", fontSize = 12.sp)
            }
            Text("~${loc.transitTimeMinutes} minutos", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF374151))
          }
        }

        // Estimate Fuel / Transport Cost
        Surface(
          color = Color(0xFFF0FDF4),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Custo estimado de ida (combustível):", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp))
            Text("R$ ${String.format("%.2f", estimatedFuelCost)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EmeraldWallet)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode("${loc.address}, ${loc.city}")}")
          val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
          }
          try {
            context.startActivity(mapIntent)
          } catch (e: Exception) {
            val browserIntent = Intent(
              Intent.ACTION_VIEW,
              Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode("${loc.address}, ${loc.city}")}")
            )
            context.startActivity(browserIntent)
          }
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = InfoBlue)
      ) {
        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Abrir Navegação GPS")
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
private fun ServiceCancellationBannerCard(
  service: ServiceRequest,
  viewerRole: UserRole = UserRole.CLIENTE,
  modifier: Modifier = Modifier
) {
  val actorLabel = when (service.cancelledByRole) {
    UserRole.PRESTADOR -> "Cancelado pelo Prestador"
    UserRole.ADMIN -> "Cancelado pelo Suporte"
    else -> "Cancelado pelo Cliente"
  }
  val bySuffix = service.cancelledByName?.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()

  Surface(
    color = DangerRedLight,
    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.35f)),
    shape = RoundedCornerShape(10.dp),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(10.dp),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Cancel,
        contentDescription = null,
        tint = DangerRed,
        modifier = Modifier.size(18.dp)
      )
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "$actorLabel$bySuffix",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = DangerRed
        )
        Text(
          text = "Motivo: ${service.cancellationReason.orEmpty()}",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF7F1D1D)
        )
        if (viewerRole != UserRole.CLIENTE &&
          service.cancellationRefundStatus != null &&
          service.cancellationRefundStatus != CancellationRefundStatus.NENHUM
        ) {
          Surface(
            color = Color.White.copy(alpha = 0.75f),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = service.cancellationRefundStatus.label,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
              color = Color(0xFF92400E),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ServiceAttachedPhotoCard(
  title: String,
  photoUri: String,
  photoName: String?,
  accentColor: Color,
  cardTag: String,
  subtitleNote: String? = null
) {
  var showFullScreen by remember { mutableStateOf(false) }

  Surface(
    color = accentColor.copy(alpha = 0.08f),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { showFullScreen = true }
      .testTag(cardTag)
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = Icons.Default.PhotoCamera,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(17.dp)
          )
          Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.ZoomIn,
            contentDescription = "Ampliar foto",
            tint = accentColor,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = "Ampliar",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = accentColor
          )
        }
      }

      DocumentAttachmentPreviewImage(
        attachmentUri = photoUri,
        contentDescription = title,
        contentScale = ContentScale.Crop,
        accentColor = accentColor,
        modifier = Modifier
          .fillMaxWidth()
          .height(175.dp)
          .clip(RoundedCornerShape(10.dp))
      )

      if (!subtitleNote.isNullOrBlank()) {
        Text(
          text = "Observação: $subtitleNote",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      if (!photoName.isNullOrBlank()) {
        Text(
          text = photoName,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }

  if (showFullScreen) {
    ServicePhotoFullScreenDialog(
      title = title,
      photoUri = photoUri,
      photoName = photoName,
      subtitleNote = subtitleNote,
      onDismiss = { showFullScreen = false }
    )
  }
}

@Composable
private fun ServiceAttachedVideoCard(
  title: String,
  videoUri: String,
  videoName: String?,
  thumbnailUri: String?,
  durationMs: Long?,
  accentColor: Color,
  cardTag: String,
  subtitleNote: String? = null
) {
  var showPlayerDialog by remember { mutableStateOf(false) }
  val durationLabel = remember(durationMs) {
    DocumentAttachmentHelper.formatVideoDuration(durationMs)
  }

  Surface(
    color = accentColor.copy(alpha = 0.08f),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { showPlayerDialog = true }
      .testTag(cardTag)
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = Icons.Default.Videocam,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(17.dp)
          )
          Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Assistir vídeo",
            tint = accentColor,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = "Assistir ($durationLabel)",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = accentColor
          )
        }
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(175.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
      ) {
        if (!thumbnailUri.isNullOrBlank()) {
          DocumentAttachmentPreviewImage(
            attachmentUri = thumbnailUri,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            accentColor = accentColor,
            modifier = Modifier.fillMaxSize()
          )
        }
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f)),
          contentAlignment = Alignment.Center
        ) {
          Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = CircleShape,
            modifier = Modifier.size(52.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Reproduzir vídeo",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
              )
            }
          }
        }
      }

      if (!subtitleNote.isNullOrBlank()) {
        Text(
          text = "Observação: $subtitleNote",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      if (!videoName.isNullOrBlank()) {
        Text(
          text = videoName,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }

  if (showPlayerDialog) {
    ServiceVideoPlayerDialog(
      title = title,
      videoUri = videoUri,
      videoName = videoName,
      thumbnailUri = thumbnailUri,
      durationMs = durationMs,
      subtitleNote = subtitleNote,
      onDismiss = { showPlayerDialog = false }
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CancelServiceReasonDialog(
  service: ServiceRequest,
  actorRole: UserRole,
  onDismiss: () -> Unit,
  onConfirm: (reason: String) -> Unit
) {
  var reasonText by remember { mutableStateOf("") }
  val isProvider = actorRole == UserRole.PRESTADOR
  val quickReasons = if (isProvider) {
    listOf(
      "Imprevisto de agenda",
      "Falta de material específico",
      "Endereço fora da área de atendimento",
      "Acordo mútuo com o cliente"
    )
  } else {
    listOf(
      "Não preciso mais do serviço",
      "Encontrei outro profissional",
      "Horário incompatível",
      "Valor fora do orçamento"
    )
  }
  val hasFee = (service.status == ServiceStatus.ACEITO || service.status == ServiceStatus.EM_EXECUCAO) &&
    (service.platformFee ?: 0.0) > 0.0
  val isReasonValid = reasonText.trim().length >= 5

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp)
      .widthIn(max = 520.dp),
    icon = {
      Icon(
        imageVector = Icons.Default.Cancel,
        contentDescription = null,
        tint = DangerRed,
        modifier = Modifier.size(32.dp)
      )
    },
    title = {
      Text(
        text = if (isProvider) "Cancelar Serviço" else "Cancelar Pedido",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        Text(
          text = "Pedido: \"${service.title}\"",
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface
        )

        if (isProvider) {
          Surface(
            color = WarningAmberLight,
            border = BorderStroke(1.dp, WarningAmber),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("provider_cancel_warning_banner")
          ) {
            Column(
              modifier = Modifier.padding(10.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(
                text = "⚠️ Evite cancelamento, sua conta pode ser bloqueada.",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = DangerRed
              )
              Text(
                text = if (hasFee) {
                  "Após a análise, o estorno da taxa de R$ ${String.format("%.2f", service.platformFee ?: 0.0)} será feito por um administrador."
                } else {
                  "Após a análise, o estorno será feito por um administrador."
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Color(0xFF78350F)
              )
            }
          }
        }

        Text(
          text = "Selecione ou digite o motivo do cancelamento *:",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          quickReasons.forEachIndexed { idx, suggestion ->
            val selected = reasonText.trim().equals(suggestion, ignoreCase = true)
            Surface(
              color = if (selected) DangerRedLight else MaterialTheme.colorScheme.surfaceVariant,
              border = BorderStroke(
                1.dp,
                if (selected) DangerRed else MaterialTheme.colorScheme.outlineVariant
              ),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .clickable { reasonText = suggestion }
                .testTag("chip_cancel_reason_$idx")
            ) {
              Text(
                text = suggestion,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) DangerRed else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }
        }

        OutlinedTextField(
          value = reasonText,
          onValueChange = { reasonText = it },
          label = { Text("Motivo do cancelamento *") },
          placeholder = { Text("Descreva brevemente o motivo (mín. 5 caracteres)...") },
          minLines = 3,
          maxLines = 4,
          isError = reasonText.isNotEmpty() && !isReasonValid,
          supportingText = {
            if (reasonText.isNotEmpty() && !isReasonValid) {
              Text("Digite pelo menos 5 caracteres.")
            } else {
              Text("O motivo ficará visível no pedido e na notificação.")
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_cancel_service_reason")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(reasonText.trim()) },
        enabled = isReasonValid,
        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
        modifier = Modifier.testTag("btn_confirm_cancel_service")
      ) {
        Text("Confirmar Cancelamento", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Voltar")
      }
    }
  )
}

@Composable
private fun CompleteServiceWithPhotoDialog(
  service: ServiceRequest,
  onDismiss: () -> Unit,
  onConfirm: (photoUri: String?, photoName: String?, notes: String?) -> Unit,
  onConfirmWithMedia: ((
    photoUri: String?,
    photoName: String?,
    videoUri: String?,
    videoName: String?,
    videoThumbnailUri: String?,
    videoDurationMs: Long?,
    notes: String?
  ) -> Unit)? = null
) {
  var photoUri by remember { mutableStateOf<String?>(null) }
  var photoName by remember { mutableStateOf("") }
  var videoUri by remember { mutableStateOf<String?>(null) }
  var videoName by remember { mutableStateOf("") }
  var videoThumbnailUri by remember { mutableStateOf<String?>(null) }
  var videoDurationMs by remember { mutableStateOf<Long?>(null) }
  var notes by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp)
      .widthIn(max = 520.dp),
    icon = {
      Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = EmeraldWallet,
        modifier = Modifier.size(32.dp)
      )
    },
    title = {
      Text(
        text = "Finalizar Serviço",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        Text(
          text = "Pedido: \"${service.title}\"",
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface
        )

        ServicePhotoAttachmentPickerCard(
          title = "Foto do Serviço Finalizado (Opcional)",
          subtitle = "Anexe uma foto da galeria ou abra a câmera para registrar o resultado.",
          docKey = "service_completion_photo",
          fileNamePrefix = "conclusao_servico",
          photoUri = photoUri,
          photoName = photoName,
          galleryButtonTag = "btn_completion_photo_gallery",
          cameraButtonTag = "btn_completion_photo_camera",
          removeButtonTag = "btn_remove_completion_photo",
          previewTag = "completion_photo_preview",
          onPhotoChanged = { newUri, newName ->
            photoUri = newUri
            photoName = newName
          }
        )

        ServiceVideoAttachmentPickerCard(
          title = "Vídeo do Serviço Finalizado (Opcional)",
          subtitle = "Anexe um vídeo da galeria ou grave na hora para comprovar o serviço.",
          docKey = "service_completion_video",
          fileNamePrefix = "conclusao_video",
          videoUri = videoUri,
          videoName = videoName,
          videoThumbnailUri = videoThumbnailUri,
          videoDurationMs = videoDurationMs,
          galleryButtonTag = "btn_completion_video_gallery",
          cameraButtonTag = "btn_completion_video_camera",
          removeButtonTag = "btn_remove_completion_video",
          previewTag = "completion_video_preview",
          onVideoChanged = { newUri, newName, newThumb, newDuration ->
            videoUri = newUri
            videoName = newName
            videoThumbnailUri = newThumb
            videoDurationMs = newDuration
          }
        )

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Observação final (opcional)") },
          placeholder = { Text("Ex: Serviço testado e entregue funcionando perfeitamente.") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_completion_notes")
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val trimmedNotes = notes.trim().takeIf { it.isNotBlank() }
          if (onConfirmWithMedia != null) {
            onConfirmWithMedia(
              photoUri,
              photoName.takeIf { it.isNotBlank() },
              videoUri,
              videoName.takeIf { it.isNotBlank() },
              videoThumbnailUri,
              videoDurationMs,
              trimmedNotes
            )
          } else {
            onConfirm(
              photoUri,
              photoName.takeIf { it.isNotBlank() },
              trimmedNotes
            )
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldWallet),
        modifier = Modifier.testTag("btn_confirm_complete_service")
      ) {
        Text("Confirmar Conclusão", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Voltar")
      }
    }
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServiceFilterDialog(
  categories: List<String>,
  selectedCategory: String,
  selectedUrgency: String,
  onDismiss: () -> Unit,
  onApply: (category: String, urgency: String) -> Unit,
  onReset: () -> Unit
) {
  var tempCategory by remember { mutableStateOf(selectedCategory) }
  var tempUrgency by remember { mutableStateOf(selectedUrgency) }
  val urgencies = listOf("Todas", "Urgente", "Alta", "Média", "Baixa")

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp)
      .widthIn(max = 520.dp)
      .testTag("service_filter_dialog"),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Filtrar Serviços", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Categoria ou Status",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          categories.forEach { cat ->
            val selected = tempCategory == cat || (cat == "Serviços" && tempCategory == "Todos")
            Surface(
              color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier
                .clickable { tempCategory = cat }
                .testTag("filter_dialog_cat_${cat.lowercase().replace(" ", "_")}")
            ) {
              Text(
                text = cat,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
              )
            }
          }
        }

        Text(
          text = "Urgência",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          urgencies.forEach { urg ->
            val selected = tempUrgency == urg
            Surface(
              color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier
                .clickable { tempUrgency = urg }
                .testTag("filter_dialog_urg_${urg.lowercase()}")
            ) {
              Text(
                text = urg,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onApply(tempCategory, tempUrgency) },
        modifier = Modifier.testTag("btn_apply_service_filter")
      ) {
        Text("Aplicar", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(
          onClick = onReset,
          modifier = Modifier.testTag("btn_reset_service_filter")
        ) {
          Text("Limpar")
        }
        TextButton(onClick = onDismiss) {
          Text("Fechar")
        }
      }
    }
  )
}

