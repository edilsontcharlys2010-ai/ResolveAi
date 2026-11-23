package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.User
import com.example.model.UserRole
import com.example.ui.MainTab
import com.example.ui.theme.DangerRed

enum class AppWidthClass {
  NARROW_PHONE,     // < 360.dp (compact phones)
  PHONE,            // 360.dp .. 599.dp (standard & large phones)
  TABLET_MEDIUM,    // 600.dp .. 839.dp (7"-8" tablets, unfolded foldables)
  TABLET_EXPANDED   // >= 840.dp (10"+ tablets, landscape tablets, ChromeOS)
}

@Immutable
data class AdaptiveSpec(
  val maxWidth: Dp,
  val widthClass: AppWidthClass
) {
  val isNarrowPhone: Boolean get() = widthClass == AppWidthClass.NARROW_PHONE
  val isPhone: Boolean get() = widthClass == AppWidthClass.NARROW_PHONE || widthClass == AppWidthClass.PHONE
  val isTablet: Boolean get() = widthClass == AppWidthClass.TABLET_MEDIUM || widthClass == AppWidthClass.TABLET_EXPANDED
  val isExpandedTablet: Boolean get() = widthClass == AppWidthClass.TABLET_EXPANDED

  val horizontalPadding: Dp
    get() = when (widthClass) {
      AppWidthClass.NARROW_PHONE -> 12.dp
      AppWidthClass.PHONE -> 16.dp
      AppWidthClass.TABLET_MEDIUM -> 22.dp
      AppWidthClass.TABLET_EXPANDED -> 28.dp
    }

  val contentMaxWidth: Dp
    get() = when (widthClass) {
      AppWidthClass.NARROW_PHONE -> 600.dp
      AppWidthClass.PHONE -> 600.dp
      AppWidthClass.TABLET_MEDIUM -> 820.dp
      AppWidthClass.TABLET_EXPANDED -> 1140.dp
    }

  val detailMaxWidth: Dp
    get() = when (widthClass) {
      AppWidthClass.NARROW_PHONE, AppWidthClass.PHONE -> 600.dp
      AppWidthClass.TABLET_MEDIUM -> 720.dp
      AppWidthClass.TABLET_EXPANDED -> 820.dp
    }
}

fun resolveAdaptiveSpec(maxWidth: Dp): AdaptiveSpec {
  val widthClass = when {
    maxWidth < 360.dp -> AppWidthClass.NARROW_PHONE
    maxWidth < 600.dp -> AppWidthClass.PHONE
    maxWidth < 840.dp -> AppWidthClass.TABLET_MEDIUM
    else -> AppWidthClass.TABLET_EXPANDED
  }
  return AdaptiveSpec(maxWidth = maxWidth, widthClass = widthClass)
}

private data class AppNavDestination(
  val tab: MainTab,
  val label: String,
  val narrowLabel: String,
  val icon: ImageVector,
  val badgeCount: Int = 0
)

private fun buildNavDestinations(
  currentUser: User,
  pendingProposalsCount: Int,
  pendingAdminReviewsCount: Int
): List<AppNavDestination> {
  val items = mutableListOf(
    AppNavDestination(
      tab = MainTab.SERVICOS,
      label = "Serviços",
      narrowLabel = "Serviços",
      icon = if (currentUser.role == UserRole.PRESTADOR) Icons.Default.Work else Icons.Default.Build
    ),
    AppNavDestination(
      tab = MainTab.NEGOCIACOES,
      label = "Negociações",
      narrowLabel = "Ofertas",
      icon = Icons.Default.Handshake,
      badgeCount = pendingProposalsCount
    ),
    AppNavDestination(
      tab = MainTab.CHAT,
      label = "Chat",
      narrowLabel = "Chat",
      icon = Icons.AutoMirrored.Filled.Chat
    ),
    AppNavDestination(
      tab = MainTab.CARTEIRA_PERFIL,
      label = if (currentUser.role == UserRole.PRESTADOR) "Carteira" else "Perfil",
      narrowLabel = if (currentUser.role == UserRole.PRESTADOR) "Carteira" else "Perfil",
      icon = if (currentUser.role == UserRole.PRESTADOR) Icons.Default.AccountBalanceWallet else Icons.Default.Person
    )
  )

  if (currentUser.role == UserRole.ADMIN) {
    items += AppNavDestination(
      tab = MainTab.ADMIN_DOCUMENTOS,
      label = "Admin",
      narrowLabel = "Admin",
      icon = Icons.Default.AdminPanelSettings,
      badgeCount = pendingAdminReviewsCount
    )
  }

  return items
}

@Composable
fun AdaptiveAppNavigationBar(
  spec: AdaptiveSpec,
  currentUser: User,
  selectedTab: MainTab,
  pendingProposalsCount: Int,
  pendingAdminReviewsCount: Int,
  onSelectTab: (MainTab) -> Unit
) {
  val destinations = buildNavDestinations(
    currentUser = currentUser,
    pendingProposalsCount = pendingProposalsCount,
    pendingAdminReviewsCount = pendingAdminReviewsCount
  )
  val isCompactTabs = spec.isNarrowPhone || destinations.size >= 5

  NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 4.dp,
    windowInsets = NavigationBarDefaults.windowInsets,
    modifier = Modifier.testTag("adaptive_bottom_nav_bar")
  ) {
    destinations.forEach { dest ->
      val selected = selectedTab == dest.tab ||
        (dest.tab == MainTab.ADMIN_DOCUMENTOS &&
          (selectedTab == MainTab.ADMIN_USUARIOS || selectedTab == MainTab.ADMIN_CATALOG_EDICOES))
      val displayLabel = if (isCompactTabs) dest.narrowLabel else dest.label
      NavigationBarItem(
        selected = selected,
        onClick = { onSelectTab(dest.tab) },
        alwaysShowLabel = true,
        icon = {
          BadgedBox(
            badge = {
              if (dest.badgeCount > 0) {
                Badge(containerColor = DangerRed) {
                  Text(
                    text = if (dest.badgeCount > 99) "99+" else "${dest.badgeCount}",
                    fontSize = 9.sp,
                    maxLines = 1
                  )
                }
              }
            }
          ) {
            Icon(
              imageVector = dest.icon,
              contentDescription = dest.label,
              modifier = Modifier.size(if (isCompactTabs) 20.dp else 22.dp)
            )
          }
        },
        label = {
          Text(
            text = displayLabel,
            fontSize = if (isCompactTabs) 10.sp else 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = if (isCompactTabs) (-0.2).sp else 0.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = MaterialTheme.colorScheme.primary,
          selectedTextColor = MaterialTheme.colorScheme.primary,
          indicatorColor = MaterialTheme.colorScheme.primaryContainer,
          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.testTag("bottom_tab_${dest.tab.name.lowercase()}")
      )
    }
  }
}

@Composable
fun AdaptiveAppNavigationRail(
  currentUser: User,
  selectedTab: MainTab,
  pendingProposalsCount: Int,
  pendingAdminReviewsCount: Int,
  onSelectTab: (MainTab) -> Unit,
  onOpenMenu: () -> Unit,
  windowInsets: WindowInsets = NavigationRailDefaults.windowInsets
) {
  val destinations = buildNavDestinations(
    currentUser = currentUser,
    pendingProposalsCount = pendingProposalsCount,
    pendingAdminReviewsCount = pendingAdminReviewsCount
  )

  NavigationRail(
    containerColor = MaterialTheme.colorScheme.surface,
    windowInsets = windowInsets,
    header = {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
      ) {
        IconButton(
          onClick = onOpenMenu,
          modifier = Modifier.testTag("rail_menu_button")
        ) {
          Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Abrir Menu"
          )
        }
        Image(
          painter = painterResource(id = R.drawable.resolveai_logo),
          contentDescription = "ResolveAi",
          modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
        )
        HorizontalDivider(modifier = Modifier.widthIn(max = 44.dp))
      }
    },
    modifier = Modifier
      .fillMaxHeight()
      .testTag("adaptive_navigation_rail")
  ) {
    Spacer(modifier = Modifier.height(4.dp))
    destinations.forEach { dest ->
      val selected = selectedTab == dest.tab ||
        (dest.tab == MainTab.ADMIN_DOCUMENTOS &&
          (selectedTab == MainTab.ADMIN_USUARIOS || selectedTab == MainTab.ADMIN_CATALOG_EDICOES))
      NavigationRailItem(
        selected = selected,
        onClick = { onSelectTab(dest.tab) },
        alwaysShowLabel = true,
        icon = {
          BadgedBox(
            badge = {
              if (dest.badgeCount > 0) {
                Badge(containerColor = DangerRed) {
                  Text(
                    text = if (dest.badgeCount > 99) "99+" else "${dest.badgeCount}",
                    fontSize = 9.sp,
                    maxLines = 1
                  )
                }
              }
            }
          ) {
            Icon(
              imageVector = dest.icon,
              contentDescription = dest.label,
              modifier = Modifier.size(22.dp)
            )
          }
        },
        label = {
          Text(
            text = dest.label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        },
        colors = NavigationRailItemDefaults.colors(
          selectedIconColor = MaterialTheme.colorScheme.primary,
          selectedTextColor = MaterialTheme.colorScheme.primary,
          indicatorColor = MaterialTheme.colorScheme.primaryContainer,
          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.testTag("rail_tab_${dest.tab.name.lowercase()}")
      )
    }
  }
}
