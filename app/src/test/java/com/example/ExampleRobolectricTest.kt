package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Base64
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.CurrentCityLocationHelper
import com.example.data.CurrentCityResult
import com.example.data.LocationAutocompleteMode
import com.example.data.SupportLauncher
import com.example.data.configureAppCheck
import com.example.model.Proposal
import com.example.model.ProposalStatus
import com.example.model.ProviderDocuments
import com.example.model.ProviderVerificationStatus
import com.example.model.ServiceRequest
import com.example.model.ServiceStatus
import com.example.model.TransactionType
import com.example.model.User
import com.example.model.UserRole
import com.example.model.WalletTransaction
import com.example.ui.AuthFlowStep
import com.example.ui.AuthMode
import com.example.ui.MainTab
import com.example.ui.components.AdaptiveAppNavigationBar
import com.example.ui.components.AdaptiveAppNavigationRail
import com.example.ui.components.AppMenuDrawerContent
import com.example.ui.components.GooglePlacesAutocompleteField
import com.example.ui.components.ProviderPendingApprovalScreen
import com.example.ui.components.SupportContactDialog
import com.example.ui.components.resolveAdaptiveSpec
import com.example.ui.screens.AdminReviewScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ProposalsScreen
import com.example.ui.screens.ServicesScreen
import com.example.ui.screens.WalletProfileScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.FirebaseApp
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @org.junit.Before
  fun resetFileProviderStaticCacheForRobolectric() {
    androidx.core.content.FileProvider::class.java.declaredFields
      .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) }
      .forEach { field ->
        runCatching {
          field.isAccessible = true
          (field.get(null) as? MutableMap<*, *>)?.clear()
        }
      }
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ResolveAi Serviços", appName)
  }

  @Test
  fun `configureAppCheck installs provider and persists debug token synchronously`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val token = "test-debug-token-1234"
    val intent = Intent().putExtra("FIREBASE_APPCHECK_DEBUG_TOKEN", token)

    configureAppCheck(context, intent)

    val persistenceKey = FirebaseApp.getInstance().persistenceKey
    val prefs = context.getSharedPreferences(
      "com.google.firebase.appcheck.debug.store.$persistenceKey",
      Context.MODE_PRIVATE
    )
    assertEquals(token, prefs.getString("com.google.firebase.appcheck.debug.DEBUG_SECRET", null))
  }

  @Test
  fun `login screen support button opens SupportContactDialog`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        var showSupport by remember { mutableStateOf(false) }
        AuthScreen(
          authMode = AuthMode.LOGIN,
          authStep = AuthFlowStep.FORM,
          pendingGoogleAccount = null,
          authErrorMessage = null,
          onSelectMode = {},
          onClearError = {},
          onGoogleSignInClick = { _, _ -> },
          onCompleteGoogleRegistration = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
          onSupportClick = { showSupport = true }
        )
        if (showSupport) {
          SupportContactDialog(
            onDismiss = { showSupport = false },
            isLoginContext = true
          )
        }
      }
    }

    composeTestRule.onNodeWithTag("auth_demo_client").assertDoesNotExist()
    composeTestRule.onNodeWithTag("auth_demo_provider").assertDoesNotExist()
    composeTestRule.onNodeWithTag("auth_demo_admin").assertDoesNotExist()
    composeTestRule.onNodeWithTag("auth_btn_header_support").assertDoesNotExist()
    composeTestRule.onNodeWithTag("auth_btn_support").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.onNodeWithText("Central de Suporte ResolveAi").assertIsDisplayed()
    composeTestRule.onNodeWithText("+55 (61) 99290-7464", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_send_support_ticket").performClick()
    composeTestRule.onNodeWithText("Chamado Aberto com Sucesso!").assertIsDisplayed()

    val startedIntent = Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity
    assertNotNull(startedIntent)
    assertEquals(Intent.ACTION_VIEW, startedIntent.action)
    assertTrue(startedIntent.dataString?.startsWith("https://wa.me/5561992907464") == true)
  }

  @Test
  fun `drawer hides switch mode and register user for client and provider`() {
    val clientUser = User(
      id = "c1",
      name = "Maria Cliente",
      email = "maria@example.com",
      phone = "(11) 99999-0001",
      role = UserRole.CLIENTE
    )
    val providerUser = User(
      id = "p1",
      name = "Carlos Prestador",
      email = "carlos@example.com",
      phone = "(11) 99999-0002",
      role = UserRole.PRESTADOR,
      category = "Eletricista"
    )
    val adminUser = User(
      id = "a1",
      name = "Admin ResolveAi",
      email = "admin@example.com",
      phone = "(11) 99999-0003",
      role = UserRole.ADMIN
    )

    var activeUser by mutableStateOf(clientUser)

    composeTestRule.setContent {
      MyApplicationTheme {
        AppMenuDrawerContent(
          currentUser = activeUser,
          selectedTab = MainTab.SERVICOS,
          pendingProposalsCount = 0,
          unreadNotificationsCount = 0,
          onSelectTab = {},
          onNotificationsClick = {},
          onSupportClick = {},
          onAdminClick = {},
          onSwitchUserClick = {},
          onRegisterClick = {},
          onRechargeClick = {},
          onLogoutClick = {},
          onCloseDrawer = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("menu_item_switch_user").assertDoesNotExist()
    composeTestRule.onNodeWithTag("menu_item_register_user").assertDoesNotExist()
    composeTestRule.onNodeWithTag("menu_item_admin_users").assertDoesNotExist()
    composeTestRule.onNodeWithTag("menu_item_logout").performScrollTo().assertIsDisplayed()

    composeTestRule.runOnUiThread { activeUser = providerUser }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("menu_item_switch_user").assertDoesNotExist()
    composeTestRule.onNodeWithTag("menu_item_register_user").assertDoesNotExist()
    composeTestRule.onNodeWithTag("menu_item_admin_users").assertDoesNotExist()
    composeTestRule.onNodeWithTag("menu_item_logout").performScrollTo().assertIsDisplayed()

    composeTestRule.runOnUiThread { activeUser = adminUser }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("menu_item_admin_users").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("menu_item_switch_user").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("menu_item_register_user").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `wallet profile hides switch user button for client and provider`() {
    val clientUser = User(
      id = "c1",
      name = "Maria Cliente",
      email = "maria@example.com",
      phone = "(11) 99999-0001",
      role = UserRole.CLIENTE
    )
    val providerUser = User(
      id = "p1",
      name = "Carlos Prestador",
      email = "carlos@example.com",
      phone = "(11) 99999-0002",
      role = UserRole.PRESTADOR,
      category = "Eletricista"
    )

    var activeUser by mutableStateOf(clientUser)

    composeTestRule.setContent {
      MyApplicationTheme {
        WalletProfileScreen(
          currentUser = activeUser,
          walletTransactions = emptyList(),
          onRechargeClick = {},
          onSwitchUserClick = {},
          onEditProfileClick = {},
          onSaveProfile = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("btn_switch_profile_bottom").assertDoesNotExist()
    composeTestRule.onNodeWithText("Trocar para Outro Usuário / Perfil").assertDoesNotExist()

    composeTestRule.runOnUiThread { activeUser = providerUser }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("btn_switch_profile_bottom").assertDoesNotExist()
    composeTestRule.onNodeWithText("Trocar para Outro Usuário / Perfil").assertDoesNotExist()
  }

  @Test
  fun `pending provider screen shows logout instead of switch user`() {
    val pendingProvider = User(
      id = "p2",
      name = "João Pendente",
      email = "joao@example.com",
      phone = "(11) 98888-1234",
      role = UserRole.PRESTADOR,
      verificationStatus = ProviderVerificationStatus.PENDENTE_ANALISE
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        ProviderPendingApprovalScreen(
          currentUser = pendingProvider,
          onFastApproveClick = {},
          onLogoutClick = {}
        )
      }
    }

    composeTestRule.onNodeWithText("Trocar de Usuário / Acessar como Administrador").assertDoesNotExist()
    composeTestRule.onNodeWithTag("btn_pending_provider_logout").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Sair da Conta").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `service order card shows client and provider photo, rating, and name`() {
    val clientUser = User(
      id = "cli_1",
      name = "Carlos Eduardo",
      email = "carlos@example.com",
      phone = "(11) 99999-0001",
      role = UserRole.CLIENTE,
      rating = 4.9
    )
    val acceptedOrder = ServiceRequest(
      id = "srv_test_1",
      clientId = "cli_1",
      clientName = "Carlos Eduardo",
      clientRating = 4.9,
      clientAvatarColorHex = 0xFF2563EB,
      clientPhotoUrl = "https://example.com/carlos.jpg",
      title = "Reparo elétrico completo",
      category = "Eletricista",
      description = "Troca de disjuntor principal",
      location = "Pinheiros - São Paulo, SP",
      proposedBudget = 250.0,
      finalAgreedPrice = 250.0,
      status = ServiceStatus.ACEITO,
      acceptedProviderId = "pro_1",
      acceptedProviderName = "Roberto Mendes",
      acceptedProviderRating = 4.95,
      acceptedProviderAvatarColorHex = 0xFF0D9488,
      acceptedProviderPhotoUrl = "https://example.com/roberto.jpg"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        ServicesScreen(
          currentUser = clientUser,
          services = listOf(acceptedOrder),
          proposals = emptyList(),
          selectedServiceId = null,
          searchQuery = "",
          categoryFilter = "Serviços",
          onSearchChange = {},
          onCategoryChange = {},
          onSelectService = {},
          onCreateServiceClick = {},
          onSubmitProposal = { _, _, _, _ -> },
          onOpenChat = {},
          onUpdateStatus = { _, _ -> },
          onRateService = { _, _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithTag("order_participants_srv_test_1", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule.onNodeWithTag("order_client_badge_srv_test_1", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("order_provider_badge_srv_test_1", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Foto de perfil de Carlos Eduardo", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Foto de perfil de Roberto Mendes", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Carlos Eduardo", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("4.9", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Roberto Mendes", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("4.95", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `proposals negotiation card shows client and provider participants`() {
    val providerUser = User(
      id = "pro_1",
      name = "Roberto Mendes",
      email = "roberto@example.com",
      phone = "(11) 99999-0002",
      role = UserRole.PRESTADOR,
      category = "Eletricista",
      rating = 4.95
    )
    val order = ServiceRequest(
      id = "srv_neg_1",
      clientId = "cli_2",
      clientName = "Mariana Silveira",
      clientRating = 4.8,
      title = "Instalação de ventilador de teto",
      category = "Eletricista",
      description = "Quarto principal",
      location = "Cambuí - Campinas, SP",
      proposedBudget = 180.0,
      status = ServiceStatus.EM_NEGOCIACAO
    )
    val proposal = Proposal(
      id = "prop_neg_1",
      serviceId = "srv_neg_1",
      providerId = "pro_1",
      providerName = "Roberto Mendes",
      providerRating = 4.95,
      providerCategory = "Eletricista",
      proposedPrice = 200.0,
      status = ProposalStatus.ENVIADA
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        ProposalsScreen(
          currentUser = providerUser,
          proposals = listOf(proposal),
          services = listOf(order),
          onAcceptProposal = {},
          onSendCounterOffer = { _, _, _ -> },
          onOpenChat = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("order_participants_srv_neg_1").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Mariana Silveira").assertIsDisplayed()
    composeTestRule.onNodeWithText("4.8").assertIsDisplayed()
    composeTestRule.onNodeWithText("Roberto Mendes").assertIsDisplayed()
    composeTestRule.onNodeWithText("4.95").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `services screen concluido tab filters completed services`() {
    val clientUser = User(
      id = "cli_1",
      name = "Carlos Eduardo",
      email = "carlos@example.com",
      phone = "(11) 99999-0001",
      role = UserRole.CLIENTE
    )
    val openOrder = ServiceRequest(
      id = "srv_open_1",
      clientId = "cli_1",
      clientName = "Carlos Eduardo",
      title = "Troca de fechadura aberta",
      category = "Chaveiro",
      description = "Porta da sala",
      location = "Pinheiros - São Paulo, SP",
      proposedBudget = 180.0,
      status = ServiceStatus.ABERTO
    )
    val doneOrder = ServiceRequest(
      id = "srv_done_1",
      clientId = "cli_1",
      clientName = "Carlos Eduardo",
      title = "Pintura concluída do quarto",
      category = "Pintura",
      description = "Pintura finalizada",
      location = "Pinheiros - São Paulo, SP",
      proposedBudget = 350.0,
      status = ServiceStatus.CONCLUIDO,
      acceptedProviderId = "pro_1",
      acceptedProviderName = "Roberto Mendes"
    )

    var selectedFilter by mutableStateOf("Serviços")
    composeTestRule.setContent {
      MyApplicationTheme {
        ServicesScreen(
          currentUser = clientUser,
          services = listOf(openOrder, doneOrder),
          proposals = emptyList(),
          selectedServiceId = null,
          searchQuery = "",
          categoryFilter = selectedFilter,
          onSearchChange = {},
          onCategoryChange = { selectedFilter = it },
          onSelectService = {},
          onCreateServiceClick = {},
          onSubmitProposal = { _, _, _, _ -> },
          onOpenChat = {},
          onUpdateStatus = { _, _ -> },
          onRateService = { _, _, _ -> }
        )
      }
    }

    // Category filter bar was moved to side drawer menu to maximize screen space
    composeTestRule.onNodeWithTag("filter_tab_servicos").assertDoesNotExist()
    composeTestRule.onNodeWithTag("filter_tab_concluido").assertDoesNotExist()
    composeTestRule.onNodeWithText("Troca de fechadura aberta").assertIsDisplayed()
    composeTestRule.onNodeWithText("Pintura concluída do quarto").assertDoesNotExist()

    // Applying "Concluído" filter from drawer filters strictly to concluded orders & shows active filter indicator
    selectedFilter = "Concluído"
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("active_filter_indicator").assertIsDisplayed()
    composeTestRule.onNodeWithText("Pintura concluída do quarto").assertIsDisplayed()
    composeTestRule.onNodeWithText("Troca de fechadura aberta").assertDoesNotExist()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w320dp-h640dp")
  fun `narrow phone renders adaptive compact bottom navigation bar`() {
    val clientUser = User(
      id = "cli_narrow",
      name = "Carlos Eduardo",
      email = "carlos@example.com",
      phone = "(11) 99999-0001",
      role = UserRole.CLIENTE
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        AdaptiveAppNavigationBar(
          spec = resolveAdaptiveSpec(320.dp),
          currentUser = clientUser,
          selectedTab = MainTab.SERVICOS,
          pendingProposalsCount = 2,
          pendingAdminReviewsCount = 0,
          onSelectTab = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("adaptive_bottom_nav_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("bottom_tab_servicos").assertIsDisplayed()
    composeTestRule.onNodeWithTag("bottom_tab_negociacoes").assertIsDisplayed()
    composeTestRule.onNodeWithTag("bottom_tab_chat").assertIsDisplayed()
    composeTestRule.onNodeWithTag("bottom_tab_carteira_perfil").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w840dp-h1000dp")
  fun `tablet renders adaptive navigation rail`() {
    val adminUser = User(
      id = "adm_tab",
      name = "Admin ResolveAi",
      email = "admin@example.com",
      phone = "(61) 99290-7464",
      role = UserRole.ADMIN
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        AdaptiveAppNavigationRail(
          currentUser = adminUser,
          selectedTab = MainTab.SERVICOS,
          pendingProposalsCount = 1,
          pendingAdminReviewsCount = 3,
          onSelectTab = {},
          onOpenMenu = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("adaptive_navigation_rail").assertIsDisplayed()
    composeTestRule.onNodeWithTag("rail_tab_servicos").assertIsDisplayed()
    composeTestRule.onNodeWithTag("rail_tab_negociacoes").assertIsDisplayed()
    composeTestRule.onNodeWithTag("rail_tab_admin_documentos").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `provider registration form exposes attachment buttons for each document`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        AuthScreen(
          authMode = AuthMode.REGISTER,
          authStep = AuthFlowStep.FORM,
          pendingGoogleAccount = null,
          authErrorMessage = null,
          onSelectMode = {},
          onClearError = {},
          onGoogleSignInClick = { _, _ -> },
          onCompleteGoogleRegistration = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
          onSupportClick = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("auth_reg_role_prestador").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("auth_attach_rg_cnh").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_attach_criminal").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_attach_selfie").assertDoesNotExist()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `admin review screen shows document attachment preview and opens zoom modal`() {
    val bmp = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
    val baos = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos)
    val dataUri = "data:image/jpeg;base64,${Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)}"

    val adminUser = User(
      id = "adm_1",
      name = "Admin ResolveAi",
      email = "admin@resolveai.com",
      phone = "(61) 99290-7464",
      role = UserRole.ADMIN
    )
    val providerWithDocs = User(
      id = "pro_attach_1",
      name = "Lucas Anexo",
      email = "lucas@example.com",
      phone = "(11) 98888-7777",
      role = UserRole.PRESTADOR,
      category = "Eletricista",
      verificationStatus = ProviderVerificationStatus.PENDENTE_ANALISE,
      documents = ProviderDocuments(
        rgOrCnhNumber = "CNH 12345678900",
        rgOrCnhFileName = "cnh_lucas.jpg",
        rgOrCnhAttachmentUri = dataUri,
        criminalRecordCertNumber = "CERT-2026-001",
        criminalRecordFileName = "certidao_lucas.pdf",
        criminalRecordAttachmentUri = dataUri,
        selfieFileName = "selfie_lucas.jpg",
        selfieAttachmentUri = dataUri
      )
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        AdminReviewScreen(
          currentUser = adminUser,
          users = listOf(adminUser, providerWithDocs),
          auditHistory = emptyList(),
          isLoading = false,
          syncStatusMessage = null,
          onSyncFirestoreClick = {},
          onApproveProvider = { _, _, _, _ -> },
          onRejectProvider = { _, _, _, _, _ -> },
          onOpenMenu = {},
          onBackToHome = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("admin_provider_card_pro_attach_1").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("btn_preview_doc_id").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("cnh_lucas.jpg", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_preview_doc_id").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("admin_zoom_attachment_preview").assertIsDisplayed()
  }

  @Test
  fun `support launcher builds whatsapp url for 5561992907464 and launches intent`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val url = SupportLauncher.buildWhatsAppUrl("Preciso de suporte")
    assertTrue(url.startsWith("https://wa.me/5561992907464?text="))
    assertTrue(SupportLauncher.openWhatsAppSupport(context, "Ajuda urgente"))

    val startedIntent = Shadows.shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity
    assertNotNull(startedIntent)
    assertEquals(Intent.ACTION_VIEW, startedIntent.action)
    assertTrue(startedIntent.dataString?.startsWith("https://wa.me/5561992907464") == true)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `city registration field shows GPS current city option and fills detected city`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    Shadows.shadowOf(app).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)

    var selectedCity by mutableStateOf("")

    composeTestRule.setContent {
      MyApplicationTheme {
        GooglePlacesAutocompleteField(
          value = selectedCity,
          onValueChange = { selectedCity = it },
          mode = LocationAutocompleteMode.CITY,
          label = "Cidade / UF",
          fieldTestTag = "auth_reg_city",
          detectCityProvider = {
            CurrentCityResult.Success(
              cityFormatted = "Brasília, DF",
              latitude = -15.793889,
              longitude = -47.882778
            )
          }
        )
      }
    }

    composeTestRule.onNodeWithTag("btn_gps_current_city", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_reg_city").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("gps_current_city_option").assertIsDisplayed()
    composeTestRule.onNodeWithText("Usar cidade atual (GPS)").assertIsDisplayed()
    composeTestRule.onNodeWithTag("gps_current_city_option").performClick()
    composeTestRule.waitForIdle()

    assertEquals("Brasília, DF", selectedCity)
    composeTestRule.onNodeWithTag("gps_city_success_hint").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `provider registration form city field opens current city GPS option on focus`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        AuthScreen(
          authMode = AuthMode.REGISTER,
          authStep = AuthFlowStep.FORM,
          pendingGoogleAccount = null,
          authErrorMessage = null,
          onSelectMode = {},
          onClearError = {},
          onGoogleSignInClick = { _, _ -> },
          onCompleteGoogleRegistration = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
          onSupportClick = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("auth_reg_city").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("gps_current_city_option").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `current city helper returns clear error when location permission is missing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val result = kotlinx.coroutines.runBlocking {
      CurrentCityLocationHelper.detectCurrentCity(context)
    }
    assertTrue(result is CurrentCityResult.Error)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `login screen renders Email Password Phone Password and Google without SMS or Facebook`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        AuthScreen(
          authMode = AuthMode.LOGIN,
          authStep = AuthFlowStep.FORM,
          pendingGoogleAccount = null,
          authErrorMessage = null,
          onSelectMode = {},
          onClearError = {},
          onGoogleSignInClick = { _, _ -> },
          onCompleteGoogleRegistration = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
        )
      }
    }

    // SMS & Facebook elements removed
    composeTestRule.onNodeWithTag("auth_btn_send_login_sms").assertDoesNotExist()
    composeTestRule.onNodeWithTag("auth_btn_register_sms").assertDoesNotExist()
    composeTestRule.onNodeWithTag("auth_recaptcha_hint_card").assertDoesNotExist()
    composeTestRule.onNodeWithTag("auth_btn_facebook_login").assertDoesNotExist()
    composeTestRule.onNodeWithTag("auth_btn_facebook_register").assertDoesNotExist()

    // Email & Password method active by default
    composeTestRule.onNodeWithTag("auth_login_tab_email").assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_login_tab_phone").assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_input_login_email").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_input_login_password").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_btn_forgot_password").assertDoesNotExist()
    composeTestRule.onNodeWithTag("auth_btn_login_email").performScrollTo().assertIsDisplayed()

    // Social login: Google only
    composeTestRule.onNodeWithTag("auth_btn_google_login").performScrollTo().assertIsDisplayed()

    // Switch to Phone & Password login
    composeTestRule.onNodeWithTag("auth_login_tab_phone").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("auth_input_login_phone").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_input_login_phone_password").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_btn_login_phone").performScrollTo().assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `register screen exposes create password with confirmation and Google without SMS or Facebook`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        AuthScreen(
          authMode = AuthMode.REGISTER,
          authStep = AuthFlowStep.FORM,
          pendingGoogleAccount = null,
          authErrorMessage = null,
          onSelectMode = {},
          onClearError = {},
          onGoogleSignInClick = { _, _ -> },
          onCompleteGoogleRegistration = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithTag("auth_btn_register_sms").assertDoesNotExist()
    composeTestRule.onNodeWithTag("auth_recaptcha_hint_card").assertDoesNotExist()
    composeTestRule.onNodeWithTag("auth_btn_facebook_register").assertDoesNotExist()
    composeTestRule.onNodeWithTag("auth_btn_google_register").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_reg_password").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_reg_confirm_password").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_btn_register_firebase").performScrollTo().assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `login removes forgot password and register validates password confirmation live`() {
    var mode by mutableStateOf(AuthMode.LOGIN)
    composeTestRule.setContent {
      MyApplicationTheme {
        AuthScreen(
          authMode = mode,
          authStep = AuthFlowStep.FORM,
          pendingGoogleAccount = null,
          authErrorMessage = null,
          onSelectMode = { mode = it },
          onClearError = {},
          onGoogleSignInClick = { _, _ -> },
          onCompleteGoogleRegistration = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
        )
      }
    }

    // Forgot password option is removed from Login screen
    composeTestRule.onNodeWithTag("auth_btn_forgot_password").assertDoesNotExist()
    composeTestRule.onNodeWithTag("auth_btn_login_email").performScrollTo().assertIsDisplayed()

    // Switch to Create Account and verify live password + confirmation feedback
    mode = AuthMode.REGISTER
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("auth_reg_password").performScrollTo().performTextInput("SenhaForte1")
    composeTestRule.onNodeWithTag("auth_reg_confirm_password").performScrollTo().performTextInput("OutraSenha")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("As senhas informadas não coincidem.").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("auth_reg_confirm_password").performScrollTo().performTextReplacement("SenhaForte1")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Senhas conferem ✓").performScrollTo().assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `provider wallet recharge dialog removes direct pix key and uses mercado pago exclusively`() {
    var rechargeState by mutableStateOf<com.example.ui.PixRechargeUiState>(com.example.ui.PixRechargeUiState.Idle)
    var requestedMpAmount = 0.0

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.components.RechargeWalletDialog(
          currentBalance = -20.0,
          pixRechargeState = rechargeState,
          isMercadoPagoConfigured = true,
          onGenerateMercadoPagoPix = { amount ->
            requestedMpAmount = amount
            val charge = com.example.model.PixRechargeCharge(
              txId = "MPPIX123456",
              providerId = "pro_1",
              pixKey = "Mercado Pago • Pagamento #123456",
              merchantName = "RESOLVEAI SERVICOS",
              amount = amount,
              brCode = com.example.data.WalletRechargeHelper.buildPixCopyPasteCode(
                amount = amount,
                providerId = "pro_1",
                pixKey = "mp@mercadopago.com.br",
                txId = "MPPIX123456"
              ),
              paymentGateway = "MERCADO_PAGO",
              mercadoPagoPaymentId = 123456L,
              ticketUrl = "https://www.mercadopago.com.br/payments/123456/ticket"
            )
            rechargeState = com.example.ui.PixRechargeUiState.AwaitingPayment(
              charge = charge,
              statusNotice = "Pix Mercado Pago #123456 ativo"
            )
          },
          onCheckPixPaymentNow = {},
          onResetPixCharge = { rechargeState = com.example.ui.PixRechargeUiState.Idle },
          onDismiss = {}
        )
      }
    }

    // Opção de cartão e chave Pix direta foram completamente removidas
    composeTestRule.onNodeWithTag("recharge_method_card").assertDoesNotExist()
    composeTestRule.onNodeWithTag("recharge_card_panel").assertDoesNotExist()
    composeTestRule.onNodeWithTag("recharge_gateway_direct").assertDoesNotExist()
    composeTestRule.onNodeWithTag("recharge_pix_key_input").assertDoesNotExist()
    composeTestRule.onNodeWithTag("recharge_copy_pix_key_btn").assertDoesNotExist()

    // Status Mercado Pago visível
    composeTestRule.onNodeWithTag("recharge_mercadopago_status_card").assertIsDisplayed()

    // Valor customizado abaixo de R$ 5,00 é bloqueado
    composeTestRule.onNodeWithTag("recharge_custom_amount_input").performScrollTo().performTextInput("2")
    composeTestRule.onNodeWithTag("recharge_generate_qr_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("recharge_validation_error").performScrollTo().assertIsDisplayed()

    // Seleciona R$ 100 e gera Pix Mercado Pago
    composeTestRule.onNodeWithTag("recharge_amount_100").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("recharge_generate_qr_btn").performClick()
    composeTestRule.waitForIdle()

    assertEquals(100.0, requestedMpAmount, 0.01)
    composeTestRule.onNodeWithTag("recharge_pix_auto_detect_banner").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("recharge_pix_qr_code").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("recharge_copy_pix_key_btn").assertDoesNotExist()
    composeTestRule.onNodeWithTag("recharge_copy_pix_btn").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("recharge_check_pix_btn").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirm_recharge_button").assertIsDisplayed()

    // Simula aprovação automática (descontando os -20,00 anteriores -> saldo 80,00)
    val activeCharge = (rechargeState as com.example.ui.PixRechargeUiState.AwaitingPayment).charge
    rechargeState = com.example.ui.PixRechargeUiState.Detected(
      charge = activeCharge.copy(
        status = com.example.model.PixChargeStatus.PAGO,
        detectedAutomatically = true
      ),
      newBalance = 80.0,
      automatic = true
    )
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("recharge_pix_detected_banner").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("recharge_done_button").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `recharge dialog shows change amount and reset controls without direct pix key fields`() {
    composeTestRule.setContent {
      var rechargeState by remember {
        mutableStateOf<com.example.ui.PixRechargeUiState>(
          com.example.ui.PixRechargeUiState.AwaitingPayment(
            charge = com.example.model.PixRechargeCharge(
              txId = "MPPIX999",
              providerId = "pro_1",
              pixKey = "Mercado Pago • Pagamento #999",
              merchantName = "RESOLVEAI SERVICOS",
              amount = 50.0,
              brCode = com.example.data.WalletRechargeHelper.buildPixCopyPasteCode(
                amount = 50.0,
                providerId = "pro_1",
                pixKey = "mp@mercadopago.com.br",
                txId = "MPPIX999"
              ),
              paymentGateway = "MERCADO_PAGO",
              mercadoPagoPaymentId = 999L
            )
          )
        )
      }
      MyApplicationTheme {
        com.example.ui.components.RechargeWalletDialog(
          currentBalance = 15.0,
          pixRechargeState = rechargeState,
          onResetPixCharge = { rechargeState = com.example.ui.PixRechargeUiState.Idle },
          onDismiss = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("recharge_pix_qr_code").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("recharge_copy_pix_key_btn").assertDoesNotExist()
    composeTestRule.onNodeWithTag("recharge_change_amount_btn").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("recharge_mercadopago_status_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("recharge_gateway_direct").assertDoesNotExist()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `mercado pago gateway displays real dynamic qr code ticket url and verification button`() {
    var mpRequestedAmount = 0.0

    composeTestRule.setContent {
      var rechargeState by remember {
        mutableStateOf<com.example.ui.PixRechargeUiState>(com.example.ui.PixRechargeUiState.Idle)
      }
      MyApplicationTheme {
        com.example.ui.components.RechargeWalletDialog(
          currentBalance = 20.0,
          pixRechargeState = rechargeState,
          isMercadoPagoConfigured = true,
          onGenerateMercadoPagoPix = { amount ->
            mpRequestedAmount = amount
            val dynamicBrCode = com.example.data.WalletRechargeHelper.buildPixCopyPasteCode(
              amount = amount,
              providerId = "pro_1",
              pixKey = "mp@mercadopago.com.br",
              txId = "MPPIX555001"
            )
            rechargeState = com.example.ui.PixRechargeUiState.AwaitingPayment(
              charge = com.example.model.PixRechargeCharge(
                txId = "MPPIX555001",
                providerId = "pro_1",
                pixKey = "Mercado Pago • Pagamento #555001",
                merchantName = "RESOLVEAI SERVICOS",
                amount = amount,
                brCode = dynamicBrCode,
                paymentGateway = "MERCADO_PAGO",
                mercadoPagoPaymentId = 555001L,
                ticketUrl = "https://www.mercadopago.com.br/payments/555001/ticket"
              ),
              statusNotice = "Pix Mercado Pago #555001 ativo"
            )
          },
          onDismiss = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("recharge_mercadopago_status_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("recharge_amount_100").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("recharge_generate_qr_btn").performClick()
    composeTestRule.waitForIdle()

    assertEquals(100.0, mpRequestedAmount, 0.01)
    composeTestRule.onNodeWithTag("mp_pix_expiration_timer").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("recharge_pix_qr_code").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("recharge_copy_pix_btn").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Copiar Código").assertIsDisplayed()
    composeTestRule.onNodeWithTag("recharge_open_mp_ticket_btn").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("recharge_copy_pix_key_btn").assertDoesNotExist()
    composeTestRule.onNodeWithText("Confirmar no Mercado Pago").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `mercado pago pix qr component displays qr code, copy code button, and expiration timer`() {
    val sampleBrCode = com.example.data.WalletRechargeHelper.buildPixCopyPasteCode(
      amount = 85.5,
      providerId = "pro_1",
      pixKey = "pagamentos@mercadopago.com.br",
      txId = "MPCARDTEST1"
    )
    var copiedPayload = ""
    var expiredTriggered = false
    var expiresAtState by mutableStateOf(System.currentTimeMillis() + 20 * 60_000L)

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.components.MercadoPagoPixQrCard(
          amount = 85.5,
          qrCodePayload = sampleBrCode,
          expiresAtMillis = expiresAtState,
          txId = "MPCARDTEST1",
          paymentId = 777888L,
          isMercadoPago = true,
          onExpired = { expiredTriggered = true },
          onCopyCode = { copiedPayload = it }
        )
      }
    }

    composeTestRule.onNodeWithText("Pix Mercado Pago").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cobrança #777888").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mp_pix_expiration_timer").assertIsDisplayed()
    composeTestRule.onNodeWithText("Tempo para pagar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("recharge_pix_qr_code").assertIsDisplayed()
    composeTestRule.onNodeWithText("Copiar Código").assertIsDisplayed()

    // Tap 'Copiar Código'
    composeTestRule.onNodeWithTag("recharge_copy_pix_btn").performClick()
    composeTestRule.waitForIdle()
    assertEquals(sampleBrCode, copiedPayload)
    composeTestRule.onNodeWithText("Código copiado!").assertIsDisplayed()

    // Simulate expired payment timestamp
    expiresAtState = System.currentTimeMillis() - 5_000L
    composeTestRule.waitForIdle()
    assertTrue(expiredTriggered)
    composeTestRule.onNodeWithText("Pagamento expirado").assertIsDisplayed()
    composeTestRule.onNodeWithText("00:00").assertIsDisplayed()
    composeTestRule.onNodeWithTag("recharge_copy_pix_btn").assertIsNotEnabled()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `admin users screen filters searches edits and deletes users`() {
    val adminUser = User(
      id = "adm_1",
      name = "Edilson Admin",
      email = "admin@resolveai.com",
      phone = "(61) 99290-7464",
      role = UserRole.ADMIN,
      city = "Brasília, DF"
    )
    val clientUser = User(
      id = "cli_1",
      name = "Mariana Cliente",
      email = "mariana@example.com",
      phone = "(11) 98888-1111",
      role = UserRole.CLIENTE,
      city = "São Paulo, SP"
    )
    val providerUser = User(
      id = "pro_1",
      name = "Roberto Prestador",
      email = "roberto@example.com",
      phone = "(11) 97777-2222",
      role = UserRole.PRESTADOR,
      category = "Eletricista",
      city = "Campinas, SP",
      walletBalance = 80.0
    )

    var usersState by mutableStateOf(listOf(adminUser, clientUser, providerUser))

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.AdminUsersScreen(
          currentUser = adminUser,
          users = usersState,
          isLoading = false,
          syncStatusMessage = null,
          onSyncUsersClick = {},
          onUpdateUser = { updated ->
            usersState = usersState.map { if (it.id == updated.id) updated else it }
            Result.success(Unit)
          },
          onDeleteUser = { userId ->
            usersState = usersState.filterNot { it.id == userId }
            Result.success(Unit)
          },
          onAddUserClick = {},
          onOpenDocumentsReview = {},
          onOpenMenu = {},
          onBackToHome = {}
        )
      }
    }

    // All 3 users listed initially (scroll LazyColumn items into view)
    composeTestRule.onNodeWithTag("admin_users_list")
      .performScrollToNode(hasTestTag("admin_user_card_adm_1"))
    composeTestRule.onNodeWithTag("admin_user_card_adm_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_users_list")
      .performScrollToNode(hasTestTag("admin_user_card_cli_1"))
    composeTestRule.onNodeWithTag("admin_user_card_cli_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_users_list")
      .performScrollToNode(hasTestTag("admin_user_card_pro_1"))
    composeTestRule.onNodeWithTag("admin_user_card_pro_1").assertIsDisplayed()

    // Filter by Prestadores
    composeTestRule.onNodeWithTag("filter_users_prestador").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("admin_users_list")
      .performScrollToNode(hasTestTag("admin_user_card_pro_1"))
    composeTestRule.onNodeWithTag("admin_user_card_pro_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_user_card_cli_1").assertDoesNotExist()

    // Back to Todos & edit clientUser
    composeTestRule.onNodeWithTag("filter_users_todos").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("admin_users_list")
      .performScrollToNode(hasTestTag("admin_user_card_cli_1"))
    composeTestRule.onNodeWithTag("btn_admin_edit_user_cli_1").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("input_edit_user_name").performTextReplacement("Mariana Atualizada")
    composeTestRule.onNodeWithTag("btn_confirm_edit_user").performClick()
    composeTestRule.waitForIdle()

    assertEquals("Mariana Atualizada", usersState.first { it.id == "cli_1" }.name)
    composeTestRule.onNodeWithText("Mariana Atualizada").assertIsDisplayed()

    // Search for providerUser and delete it via confirmation modal
    composeTestRule.onNodeWithTag("input_admin_users_search").performTextReplacement("Roberto")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("admin_users_list")
      .performScrollToNode(hasTestTag("admin_user_card_pro_1"))
    composeTestRule.onNodeWithTag("admin_user_card_pro_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_user_card_cli_1").assertDoesNotExist()

    composeTestRule.onNodeWithTag("btn_admin_delete_user_pro_1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("btn_confirm_delete_user").performClick()
    composeTestRule.waitForIdle()

    assertTrue(usersState.none { it.id == "pro_1" })
    composeTestRule.onNodeWithTag("admin_user_card_pro_1").assertDoesNotExist()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `google role completion enforces required documents before completing registration`() {
    val googleAccount = com.example.data.GoogleAccountInfo(
      uid = "google_ui_test_1",
      email = "novo.usuario@gmail.com",
      displayName = "Ana Google"
    )
    var completedRole: UserRole? = null
    var completedDocument: String? = null
    var completedPhone: String? = null

    composeTestRule.setContent {
      MyApplicationTheme {
        AuthScreen(
          authMode = com.example.ui.AuthMode.REGISTER,
          authStep = com.example.ui.AuthFlowStep.GOOGLE_ROLE_COMPLETION,
          pendingGoogleAccount = googleAccount,
          preferredGoogleRole = UserRole.PRESTADOR,
          authErrorMessage = null,
          onSelectMode = {},
          onClearError = {},
          onGoogleSignInClick = { _, _ -> },
          onCompleteGoogleRegistration = { role, phone, _, document, _, _, _, _, _, _, _, _, _, _, _ ->
            completedRole = role
            completedPhone = phone
            completedDocument = document
          }
        )
      }
    }

    // Prestador role selected from preferredGoogleRole -> CPF and 2 attachment cards exist (no selfie with document card)
    composeTestRule.onNodeWithTag("google_reg_phone").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("google_reg_document").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("google_reg_cnh").assertDoesNotExist()
    composeTestRule.onNodeWithTag("google_attach_rg_cnh").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("google_reg_criminal").assertDoesNotExist()
    composeTestRule.onNodeWithTag("google_attach_criminal").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("google_attach_selfie").assertDoesNotExist()

    // Button remains disabled and requirements banner is visible when Prestador fields/attachments are incomplete
    composeTestRule.onNodeWithTag("google_reg_validation_error").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_btn_complete_google_reg").performScrollTo().assertIsNotEnabled()
    assertEquals(null, completedRole)

    // Switch to Cliente role -> still disabled until valid phone and valid CPF
    composeTestRule.onNodeWithTag("google_reg_role_cliente").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("google_reg_validation_error").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_btn_complete_google_reg").performScrollTo().assertIsNotEnabled()
    assertEquals(null, completedRole)

    // Invalid CPF keeps button disabled and shows error
    composeTestRule.onNodeWithTag("google_reg_phone").performScrollTo().performTextInput("11987654321")
    composeTestRule.onNodeWithTag("google_reg_document").performScrollTo().performTextInput("12345678901")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("CPF inválido: confira os dígitos informados.").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_btn_complete_google_reg").performScrollTo().assertIsNotEnabled()

    // Replace with mathematically valid 11-digit CPF + city -> button becomes enabled and completes
    composeTestRule.onNodeWithTag("google_reg_city").performScrollTo().performTextInput("Curitiba, PR")
    composeTestRule.onNodeWithTag("google_reg_document").performScrollTo().performTextReplacement("52998224725")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("auth_btn_complete_google_reg").performScrollTo().assertIsEnabled()
    composeTestRule.onNodeWithTag("auth_btn_complete_google_reg").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    assertEquals(UserRole.CLIENTE, completedRole)
    assertEquals("11987654321", completedPhone)
    assertEquals("52998224725", completedDocument)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `provider profile shows detailed pix recharge and contract usage history with filters and full screen`() {
    val providerUser = User(
      id = "pro_1",
      name = "Roberto Mendes",
      email = "roberto@resolveai.com",
      phone = "(11) 98765-4321",
      role = UserRole.PRESTADOR,
      category = "Eletricista",
      walletBalance = 170.0
    )

    val services = listOf(
      ServiceRequest(
        id = "srv_4",
        clientId = "cli_2",
        clientName = "Mariana Silveira",
        title = "Troca de Disjuntores Residenciais",
        category = "Eletricista",
        description = "Substituição do quadro",
        location = "Pinheiros, SP",
        proposedBudget = 300.0,
        finalAgreedPrice = 300.0,
        platformFee = 30.0,
        status = ServiceStatus.ACEITO,
        acceptedProviderId = "pro_1"
      )
    )

    val transactions = listOf(
      WalletTransaction(
        id = "tx_pix_PIXPRO1AUTO99",
        providerId = "pro_1",
        type = TransactionType.RECARGA_PIX,
        amount = 200.0,
        description = "Recarga Pix detectada automaticamente (PIXPRO1AUTO99)",
        timestamp = System.currentTimeMillis() - 10_000L,
        balanceAfter = 200.0
      ),
      WalletTransaction(
        id = "tx_srv_fee_4",
        providerId = "pro_1",
        type = TransactionType.DEBITO_TAXA_10,
        amount = 30.0,
        serviceId = "srv_4",
        serviceTitle = "Troca de Disjuntores Residenciais",
        description = "Taxa de intermediação de 10% referente ao serviço contratado #srv_4",
        timestamp = System.currentTimeMillis() - 5_000L,
        balanceAfter = 170.0
      )
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        WalletProfileScreen(
          currentUser = providerUser,
          walletTransactions = transactions,
          onRechargeClick = {},
          onSwitchUserClick = {},
          onEditProfileClick = {},
          onSaveProfile = {},
          services = services
        )
      }
    }

    // Section & KPIs visible in provider profile
    composeTestRule.onNodeWithTag("wallet_profile_list")
      .performScrollToNode(hasTestTag("provider_financial_history_section"))
    composeTestRule.onNodeWithTag("provider_financial_history_section").assertIsDisplayed()
    composeTestRule.onNodeWithTag("kpi_pix_recharges").assertIsDisplayed()
    composeTestRule.onNodeWithTag("kpi_contract_usage").assertIsDisplayed()

    // Both Pix and Contract cards are shown under "Todas"
    composeTestRule.onNodeWithTag("wallet_profile_list")
      .performScrollToNode(hasTestTag("wallet_tx_card_tx_srv_fee_4"))
    composeTestRule.onNodeWithTag("wallet_tx_card_tx_srv_fee_4").assertIsDisplayed()

    composeTestRule.onNodeWithTag("wallet_profile_list")
      .performScrollToNode(hasTestTag("wallet_tx_card_tx_pix_PIXPRO1AUTO99"))
    composeTestRule.onNodeWithTag("wallet_tx_card_tx_pix_PIXPRO1AUTO99").assertIsDisplayed()

    // Filter only Pix Recharges
    composeTestRule.onNodeWithTag("wallet_profile_list")
      .performScrollToNode(hasTestTag("provider_financial_history_section"))
    composeTestRule.onNodeWithTag("filter_tx_pix").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("wallet_profile_list")
      .performScrollToNode(hasTestTag("wallet_tx_card_tx_pix_PIXPRO1AUTO99"))
    composeTestRule.onNodeWithTag("wallet_tx_card_tx_pix_PIXPRO1AUTO99").assertIsDisplayed()
    composeTestRule.onNodeWithTag("wallet_tx_card_tx_srv_fee_4").assertDoesNotExist()

    // Filter only Contract Usage
    composeTestRule.onNodeWithTag("wallet_profile_list")
      .performScrollToNode(hasTestTag("provider_financial_history_section"))
    composeTestRule.onNodeWithTag("filter_tx_contracts").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("wallet_profile_list")
      .performScrollToNode(hasTestTag("wallet_tx_card_tx_srv_fee_4"))
    composeTestRule.onNodeWithTag("wallet_tx_card_tx_srv_fee_4").assertIsDisplayed()
    composeTestRule.onNodeWithTag("wallet_tx_card_tx_pix_PIXPRO1AUTO99").assertDoesNotExist()

    // Reset to All and open Dedicated Full Screen
    composeTestRule.onNodeWithTag("wallet_profile_list")
      .performScrollToNode(hasTestTag("provider_financial_history_section"))
    composeTestRule.onNodeWithTag("filter_tx_all").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("btn_open_full_wallet_history").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("provider_wallet_history_fullscreen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("input_wallet_history_search").performTextReplacement("PIXPRO1AUTO99")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("wallet_tx_card_tx_pix_PIXPRO1AUTO99").assertIsDisplayed()
    composeTestRule.onNodeWithTag("wallet_tx_card_tx_srv_fee_4").assertDoesNotExist()

    // Tap transaction card to open detailed receipt modal
    composeTestRule.onNodeWithTag("wallet_tx_card_tx_pix_PIXPRO1AUTO99").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tx_detail_dialog").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `provider profile photo camera opens safely with uri permissions and selfie with document option is removed`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    Shadows.shadowOf(app).grantPermissions(
      Manifest.permission.CAMERA,
      Manifest.permission.ACCESS_COARSE_LOCATION
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        AuthScreen(
          authMode = AuthMode.REGISTER,
          authStep = AuthFlowStep.FORM,
          pendingGoogleAccount = null,
          authErrorMessage = null,
          onSelectMode = {},
          onClearError = {},
          onGoogleSignInClick = { _, _ -> },
          onCompleteGoogleRegistration = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
          onSupportClick = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("auth_reg_role_prestador").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Selfie with document card is completely removed and counter shows 0/2 anexos
    composeTestRule.onNodeWithTag("auth_attach_selfie").assertDoesNotExist()
    composeTestRule.onNodeWithText("0/2 anexos").performScrollTo().assertIsDisplayed()

    while (Shadows.shadowOf(app).nextStartedActivity != null) {
      // Drain any pending intents from previous tests
    }

    // Provider profile photo button opens camera safely with FileProvider EXTRA_OUTPUT + URI grant flags
    composeTestRule.onNodeWithTag("btn_provider_profile_camera").performScrollTo()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("btn_provider_profile_camera").performClick()
    composeTestRule.waitForIdle()

    val startedIntent = Shadows.shadowOf(app).nextStartedActivity
    assertNotNull("A câmera deve abrir ao clicar em tirar foto de perfil", startedIntent)
    assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, startedIntent!!.action)
    assertTrue(
      "Captura deve usar FileProvider URI no EXTRA_OUTPUT",
      startedIntent.hasExtra(MediaStore.EXTRA_OUTPUT)
    )
    assertTrue(
      "Intent da câmera deve conceder permissão de leitura e escrita na URI do FileProvider",
      (startedIntent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0 &&
        (startedIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0
    )
    assertNotNull("Intent da câmera deve incluir ClipData para compatibilidade Android 11+", startedIntent.clipData)

    // Verify FileProvider capture target, safe file-based processing, compact localFileUri, and persistence conversion
    val target = com.example.data.DocumentAttachmentHelper.createSelfieCaptureTarget(app, "provider_profile_cam")
    assertNotNull(target)
    com.example.data.DocumentAttachmentHelper.savePendingCapturePath(app, "provider_profile_cam", target!!.file.absolutePath)
    assertEquals(
      target.file.absolutePath,
      com.example.data.DocumentAttachmentHelper.consumePendingCapturePath(app, "provider_profile_cam")
    )

    val bmp = Bitmap.createBitmap(1200, 900, Bitmap.Config.ARGB_8888)
    target.file.outputStream().use { out ->
      bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    val processed = com.example.data.DocumentAttachmentHelper.processCapturedPhotoFile(
      context = app,
      captureFile = target.file,
      docKey = "provider_profile_cam",
      fileNamePrefix = "perfil_prestador"
    )
    assertNotNull(processed)
    assertTrue(processed!!.fileName.startsWith("perfil_prestador_"))
    assertTrue(processed.localFileUri.startsWith("file://"))
    assertTrue("localFileUri deve ser curto para evitar TransactionTooLargeException", processed.localFileUri.length < 300)
    assertTrue(processed.attachmentUri.startsWith("data:image/jpeg;base64,"))

    val persisted = com.example.data.DocumentAttachmentHelper.resolveForPersistence(app, processed.localFileUri)
    assertNotNull(persisted)
    assertTrue(persisted!!.startsWith("data:image/jpeg;base64,"))
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `provider registration requires valid cpf and attachments before enabling create account button`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        AuthScreen(
          authMode = AuthMode.REGISTER,
          authStep = AuthFlowStep.FORM,
          pendingGoogleAccount = null,
          authErrorMessage = null,
          onSelectMode = {},
          onClearError = {},
          onGoogleSignInClick = { _, _ -> },
          onCompleteGoogleRegistration = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
          onSupportClick = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("auth_reg_role_prestador").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Create account button must be disabled initially with pending checklist visible
    composeTestRule.onNodeWithTag("auth_reg_requirements_status").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_btn_register_firebase").performScrollTo().assertIsNotEnabled()

    // Verify RG/CNH and Criminal Record number fields are removed (attachments only)
    composeTestRule.onNodeWithTag("auth_reg_cnh").assertDoesNotExist()
    composeTestRule.onNodeWithTag("auth_reg_antecedentes").assertDoesNotExist()

    // Fill basic fields plus an INVALID CPF
    composeTestRule.onNodeWithTag("auth_reg_name").performScrollTo().performTextInput("Marcos Silva")
    composeTestRule.onNodeWithTag("auth_reg_email").performScrollTo().performTextInput("marcos@prestador.com")
    composeTestRule.onNodeWithTag("auth_reg_password").performScrollTo().performTextInput("senha123")
    composeTestRule.onNodeWithTag("auth_reg_document").performScrollTo().performTextInput("12345678901")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("CPF inválido: confira os dígitos informados e digite um CPF válido.")
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_btn_register_firebase").performScrollTo().assertIsNotEnabled()

    // Enter a mathematically VALID CPF -> shows valid feedback, but button still stays disabled without 3 attachments
    composeTestRule.onNodeWithTag("auth_reg_document").performScrollTo().performTextReplacement("52998224725")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("✓ CPF válido: 529.982.247-25")
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_btn_register_firebase").performScrollTo().assertIsNotEnabled()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `mercado pago transactions screen displays status, value, date, and filters`() {
    val charges = listOf(
      com.example.model.PixRechargeCharge(
        txId = "MP_TX_PAID_1",
        providerId = "pro_1",
        pixKey = "Mercado Pago • Pagamento #10001",
        merchantName = "RESOLVEAI SERVICOS",
        amount = 150.0,
        brCode = "000201...",
        status = com.example.model.PixChargeStatus.PAGO,
        createdAt = 1763200000000L,
        paidAt = 1763200120000L,
        paymentGateway = "MERCADO_PAGO",
        mercadoPagoPaymentId = 10001L,
        mpStatusDetail = "accredited"
      ),
      com.example.model.PixRechargeCharge(
        txId = "MP_TX_PEND_2",
        providerId = "pro_1",
        pixKey = "Mercado Pago • Pagamento #10002",
        merchantName = "RESOLVEAI SERVICOS",
        amount = 65.0,
        brCode = "000201...",
        status = com.example.model.PixChargeStatus.AGUARDANDO_PAGAMENTO,
        createdAt = 1763205000000L,
        paymentGateway = "MERCADO_PAGO",
        mercadoPagoPaymentId = 10002L,
        mpStatusDetail = "pending_waiting_transfer",
        ticketUrl = "https://www.mercadopago.com.br/payments/10002/ticket"
      ),
      com.example.model.PixRechargeCharge(
        txId = "MP_TX_EXP_3",
        providerId = "pro_1",
        pixKey = "Mercado Pago • Pagamento #10003",
        merchantName = "RESOLVEAI SERVICOS",
        amount = 40.0,
        brCode = "000201...",
        status = com.example.model.PixChargeStatus.EXPIRADO,
        createdAt = 1763190000000L,
        paymentGateway = "MERCADO_PAGO",
        mercadoPagoPaymentId = 10003L
      )
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.MercadoPagoTransactionsFullScreen(
          charges = charges,
          isPollingActive = true,
          isRefreshing = false,
          onRefreshClick = {},
          onReopenCharge = {},
          onCancelCharge = {},
          onNewRechargeClick = {},
          onBack = {}
        )
      }
    }

    composeTestRule.onNodeWithText("Transações Pix Mercado Pago").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mp_pix_transactions_list")
      .performScrollToNode(hasTestTag("mp_tx_item_MP_TX_PAID_1"))
    composeTestRule.onNodeWithTag("mp_tx_item_MP_TX_PAID_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mp_pix_transactions_list")
      .performScrollToNode(hasTestTag("mp_tx_item_MP_TX_PEND_2"))
    composeTestRule.onNodeWithTag("mp_tx_item_MP_TX_PEND_2").assertIsDisplayed()
    composeTestRule.onNodeWithText("Aprovado").assertIsDisplayed()
    assertTrue(composeTestRule.onAllNodes(hasText("Aguardando")).fetchSemanticsNodes().size >= 2)
    composeTestRule.onNodeWithText(com.example.data.WalletHistoryHelper.formatBrl(65.0)).assertIsDisplayed()

    // Filter only approved transactions
    composeTestRule.onNodeWithTag("mp_filter_aprovadas").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("mp_tx_item_MP_TX_PAID_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mp_tx_item_MP_TX_PEND_2").assertDoesNotExist()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `city field auto detects local city on initial composition when permission is granted`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    Shadows.shadowOf(app).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)

    var selectedCity by mutableStateOf("")

    composeTestRule.setContent {
      MyApplicationTheme {
        GooglePlacesAutocompleteField(
          value = selectedCity,
          onValueChange = { selectedCity = it },
          mode = LocationAutocompleteMode.CITY,
          label = "Cidade / UF",
          fieldTestTag = "auth_reg_city",
          detectCityProvider = {
            CurrentCityResult.Success(
              cityFormatted = "Curitiba, PR",
              latitude = -25.4284,
              longitude = -49.2733
            )
          }
        )
      }
    }

    composeTestRule.waitForIdle()
    assertEquals("Curitiba, PR", selectedCity)
    composeTestRule.onNodeWithTag("gps_city_success_hint").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `client can open cancel dialog enter reason and see cancellation banner`() {
    val client = com.example.model.User(
      id = "cli_1",
      name = "Mariana Cliente",
      email = "cli@test.com",
      phone = "11999990000",
      role = UserRole.CLIENTE,
      city = "Curitiba, PR"
    )
    var serviceState by mutableStateOf(
      com.example.model.ServiceRequest(
        id = "srv_cancel_ui",
        clientId = "cli_1",
        clientName = "Mariana Cliente",
        title = "Instalar Ventilador de Teto",
        category = "Eletricista",
        description = "Preciso instalar 1 ventilador.",
        location = "Centro - Curitiba, PR",
        proposedBudget = 160.0,
        status = com.example.model.ServiceStatus.ABERTO
      )
    )
    var selectedId by mutableStateOf<String?>(null)
    var categoryFilter by mutableStateOf("Serviços")
    var capturedReason = ""

    composeTestRule.setContent {
      MyApplicationTheme {
        ServicesScreen(
          currentUser = client,
          services = listOf(serviceState),
          proposals = emptyList(),
          selectedServiceId = selectedId,
          searchQuery = "",
          categoryFilter = categoryFilter,
          onSearchChange = {},
          onCategoryChange = { categoryFilter = it },
          onSelectService = { selectedId = it },
          onCreateServiceClick = {},
          onSubmitProposal = { _, _, _, _ -> },
          onOpenChat = {},
          onUpdateStatus = { _, _ -> },
          onRateService = { _, _, _ -> },
          onCancelService = { _, reason ->
            capturedReason = reason
            serviceState = serviceState.copy(
              status = com.example.model.ServiceStatus.CANCELADO,
              cancellationReason = reason,
              cancelledByRole = UserRole.CLIENTE,
              cancelledByName = client.name
            )
            categoryFilter = "Cancelado"
          }
        )
      }
    }

    composeTestRule.onNodeWithTag("btn_card_cancel_service_srv_cancel_ui").performClick()
    composeTestRule.waitForIdle()

    // Confirm disabled until >= 5 chars entered
    composeTestRule.onNodeWithTag("btn_confirm_cancel_service").assertIsNotEnabled()
    composeTestRule.onNodeWithTag("input_cancel_service_reason").performTextInput("Mudei a data da reforma")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("btn_confirm_cancel_service").assertIsEnabled().performClick()
    composeTestRule.waitForIdle()

    assertEquals("Mudei a data da reforma", capturedReason)
    composeTestRule.onNodeWithTag(
      "service_cancellation_reason_card_srv_cancel_ui",
      useUnmergedTree = true
    ).assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `provider detail shows completion photo dialog with gallery and camera and cancel option`() {
    val provider = com.example.model.User(
      id = "pro_1",
      name = "Roberto Eletricista",
      email = "pro@test.com",
      phone = "11988880000",
      role = UserRole.PRESTADOR,
      category = "Eletricista",
      city = "Curitiba, PR",
      walletBalance = 120.0
    )
    var serviceState by mutableStateOf(
      com.example.model.ServiceRequest(
        id = "srv_exec_ui",
        clientId = "cli_1",
        clientName = "Mariana Cliente",
        title = "Troca de Fiação",
        category = "Eletricista",
        description = "Passar cabeamento novo.",
        location = "Batel - Curitiba, PR",
        proposedBudget = 300.0,
        finalAgreedPrice = 300.0,
        platformFee = 30.0,
        acceptedProviderId = "pro_1",
        acceptedProviderName = "Roberto Eletricista",
        status = com.example.model.ServiceStatus.EM_EXECUCAO,
        attachmentPhotoUri = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
        attachmentPhotoName = "quadro_cliente.jpg"
      )
    )
    var completedNotes: String? = null

    composeTestRule.setContent {
      MyApplicationTheme {
        ServicesScreen(
          currentUser = provider,
          services = listOf(serviceState),
          proposals = emptyList(),
          selectedServiceId = "srv_exec_ui",
          searchQuery = "",
          categoryFilter = "Serviços",
          onSearchChange = {},
          onCategoryChange = {},
          onSelectService = {},
          onCreateServiceClick = {},
          onSubmitProposal = { _, _, _, _ -> },
          onOpenChat = {},
          onUpdateStatus = { _, _ -> },
          onRateService = { _, _, _ -> },
          onCompleteServiceWithPhoto = { _, _, _, notes ->
            completedNotes = notes
            serviceState = serviceState.copy(
              status = com.example.model.ServiceStatus.CONCLUIDO,
              completionPhotoUri = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
              completionPhotoName = "conclusao.jpg",
              completionNotes = notes
            )
          }
        )
      }
    }

    // Client photo card and Provider Cancel button are visible in detail
    composeTestRule.onNodeWithTag("service_detail_photo_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_provider_cancel_service").performScrollTo().assertIsDisplayed()

    // Click Finalizar & Concluir Serviço -> opens photo + notes modal with Galeria and Abrir Câmera
    composeTestRule.onNodeWithTag("btn_provider_finish_service").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("btn_completion_photo_gallery").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_completion_photo_camera").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_completion_video_gallery").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_completion_video_camera").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("input_completion_notes").performScrollTo().performTextInput("Concluído e testado.")
    composeTestRule.onNodeWithTag("btn_confirm_complete_service").performClick()
    composeTestRule.waitForIdle()

    assertEquals("Concluído e testado.", completedNotes)
    composeTestRule.onNodeWithTag("service_detail_completion_photo_card").performScrollTo().assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `create service dialog displays gallery and open camera buttons without hardcoded city`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.components.CreateServiceDialog(
          onDismiss = {},
          onCreate = { _, _, _, _, _, _, _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithTag("input_service_location").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("São Paulo, SP").assertDoesNotExist()
    composeTestRule.onNodeWithTag("btn_service_photo_gallery").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_service_photo_camera").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_service_video_gallery").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_service_video_camera").performScrollTo().assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `create service dialog launches video capture intent when camera permission is granted`() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    Shadows.shadowOf(context).grantPermissions(Manifest.permission.CAMERA)

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.components.CreateServiceDialog(
          onDismiss = {},
          onCreate = { _, _, _, _, _, _, _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithTag("btn_service_video_camera").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    val nextStartedIntent = Shadows.shadowOf(context).nextStartedActivity
    assertNotNull(nextStartedIntent)
    assertEquals(MediaStore.ACTION_VIDEO_CAPTURE, nextStartedIntent.action)
    assertNotNull(nextStartedIntent.getParcelableExtra<android.net.Uri>(MediaStore.EXTRA_OUTPUT))
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `service detail displays attached client and completion videos and opens video player modal`() {
    val client = User(
      id = "cli_1",
      name = "Mariana Cliente",
      email = "mariana@resolveai.com",
      phone = "41999990000",
      role = UserRole.CLIENTE
    )
    val completedServiceWithVideos = ServiceRequest(
      id = "srv_vid_1",
      clientId = "cli_1",
      clientName = "Mariana Cliente",
      title = "Troca de Registro de Água",
      category = "Hidráulica & Encanador",
      description = "Vídeo mostrando o vazamento no registro.",
      location = "Centro - Curitiba, PR",
      proposedBudget = 240.0,
      finalAgreedPrice = 240.0,
      platformFee = 24.0,
      acceptedProviderId = "pro_1",
      acceptedProviderName = "Roberto Encanador",
      status = ServiceStatus.CONCLUIDO,
      attachmentVideoUri = "file:///tmp/inexistente_cliente.mp4",
      attachmentVideoName = "vazamento_inicial.mp4",
      attachmentVideoThumbnailUri = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      attachmentVideoDurationMs = 15_000L,
      completionVideoUri = "file:///tmp/inexistente_prestador.mp4",
      completionVideoName = "conserto_concluido.mp4",
      completionVideoThumbnailUri = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      completionVideoDurationMs = 22_000L,
      completionNotes = "Registro substituído sem vazamentos."
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        ServicesScreen(
          currentUser = client,
          services = listOf(completedServiceWithVideos),
          proposals = emptyList(),
          selectedServiceId = "srv_vid_1",
          searchQuery = "",
          categoryFilter = "Serviços",
          onSearchChange = {},
          onCategoryChange = {},
          onSelectService = {},
          onCreateServiceClick = {},
          onSubmitProposal = { _, _, _, _ -> },
          onOpenChat = {},
          onUpdateStatus = { _, _ -> },
          onRateService = { _, _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithTag("service_detail_video_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("service_detail_completion_video_card").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("service_detail_completion_video_card").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("service_video_player_dialog").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `services screen displays provider promotional showcase with service photo, profile avatar, name and rating`() {
    val client = User(
      id = "cli_1",
      name = "Carlos Eduardo",
      email = "carlos@resolveai.com",
      phone = "11988880000",
      role = UserRole.CLIENTE
    )
    val provider1 = User(
      id = "pro_1",
      name = "Roberto Mendes",
      email = "roberto@resolveai.com",
      phone = "11993334455",
      role = UserRole.PRESTADOR,
      category = "Eletricista Residencial & Predial",
      city = "São Paulo, SP",
      rating = 4.95,
      completedJobs = 42,
      photoUrl = "drawable://img_avatar_roberto",
      portfolioPhotos = listOf(
        com.example.model.ProviderServicePhoto(
          id = "p_1",
          title = "Quadro de disjuntores e iluminação LED",
          category = "Eletricista",
          photoUri = "drawable://img_showcase_eletrica",
          createdAt = 1000L
        )
      )
    )
    val provider2 = User(
      id = "pro_2",
      name = "Ana Clara Ferraz",
      email = "ana@resolveai.com",
      phone = "11984445566",
      role = UserRole.PRESTADOR,
      category = "Pintura & Acabamento",
      city = "Campinas, SP",
      rating = 4.88,
      completedJobs = 28,
      photoUrl = "drawable://img_avatar_anaclara",
      portfolioPhotos = listOf(
        com.example.model.ProviderServicePhoto(
          id = "p_2",
          title = "Pintura sala de estar",
          category = "Pintura",
          photoUri = "drawable://img_showcase_pintura",
          createdAt = 900L
        )
      )
    )
    var createClicked = false

    composeTestRule.setContent {
      MyApplicationTheme {
        ServicesScreen(
          currentUser = client,
          services = emptyList(),
          proposals = emptyList(),
          selectedServiceId = null,
          searchQuery = "",
          categoryFilter = "Serviços",
          onSearchChange = {},
          onCategoryChange = {},
          onSelectService = {},
          onCreateServiceClick = { createClicked = true },
          onSubmitProposal = { _, _, _, _ -> },
          onOpenChat = {},
          onUpdateStatus = { _, _ -> },
          onRateService = { _, _, _ -> },
          providers = listOf(provider1, provider2)
        )
      }
    }

    composeTestRule.onNodeWithTag("provider_showcase_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("promo_provider_card_pro_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("promo_provider_avatar_pro_1", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("promo_provider_rating_pro_1", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Roberto Mendes", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("4.95", useUnmergedTree = true).assertIsDisplayed()

    composeTestRule.onNodeWithTag("promo_provider_card_pro_1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("promo_provider_detail_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_promo_request_service").performClick()
    composeTestRule.waitForIdle()
    assertTrue(createClicked)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `wallet profile displays provider portfolio showcase upload controls for providers`() {
    val provider = User(
      id = "pro_1",
      name = "Roberto Mendes",
      email = "roberto@resolveai.com",
      phone = "11993334455",
      role = UserRole.PRESTADOR,
      category = "Eletricista",
      rating = 4.95,
      completedJobs = 42,
      photoUrl = "drawable://img_avatar_roberto",
      portfolioPhotos = listOf(
        com.example.model.ProviderServicePhoto(
          id = "p_1",
          title = "Quadro elétrico",
          category = "Eletricista",
          photoUri = "drawable://img_showcase_eletrica"
        )
      )
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        WalletProfileScreen(
          currentUser = provider,
          walletTransactions = emptyList(),
          onRechargeClick = {},
          onSwitchUserClick = {},
          onEditProfileClick = {},
          onSaveProfile = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("wallet_profile_list")
      .performScrollToNode(hasTestTag("provider_portfolio_section"))
    composeTestRule.onNodeWithTag("provider_portfolio_section").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_portfolio_gallery").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_portfolio_camera").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `services screen displays Cabeleireiro Manicure chips opens filter dialog and hides fee notices from client`() {
    val client = User(
      id = "cli_1",
      name = "Carlos Eduardo",
      email = "carlos@resolveai.com",
      phone = "(11) 98888-0001",
      role = UserRole.CLIENTE
    )
    val acceptedHairOrder = ServiceRequest(
      id = "srv_hair_1",
      clientId = "cli_1",
      clientName = "Carlos Eduardo",
      title = "Corte e Escova em Domicílio",
      category = "Cabeleireiro",
      description = "Corte feminino",
      location = "Pinheiros - São Paulo, SP",
      proposedBudget = 150.0,
      finalAgreedPrice = 150.0,
      platformFee = 15.0,
      status = ServiceStatus.ACEITO,
      acceptedProviderId = "pro_h1",
      acceptedProviderName = "Juliana Hair"
    )
    val openManicureOrder = ServiceRequest(
      id = "srv_mani_1",
      clientId = "cli_1",
      clientName = "Carlos Eduardo",
      title = "Manicure e Pedicure Completa",
      category = "Manicure",
      description = "Esmaltação em gel",
      location = "Pinheiros - São Paulo, SP",
      proposedBudget = 90.0,
      status = ServiceStatus.ABERTO
    )

    var selectedCategory by mutableStateOf("Serviços")
    composeTestRule.setContent {
      MyApplicationTheme {
        ServicesScreen(
          currentUser = client,
          services = listOf(acceptedHairOrder, openManicureOrder),
          proposals = emptyList(),
          selectedServiceId = null,
          searchQuery = "",
          categoryFilter = selectedCategory,
          onSearchChange = {},
          onCategoryChange = { selectedCategory = it },
          onSelectService = {},
          onCreateServiceClick = {},
          onSubmitProposal = { _, _, _, _ -> },
          onOpenChat = {},
          onUpdateStatus = { _, _ -> },
          onRateService = { _, _, _ -> }
        )
      }
    }

    // Verify zero fee notices for client
    composeTestRule.onNodeWithText("Taxa plataforma", substring = true).assertDoesNotExist()
    composeTestRule.onNodeWithText("Taxa de 10%", substring = true).assertDoesNotExist()

    // Fixed search bar is hidden initially to give more screen space; header Search icon next to bell toggles it
    composeTestRule.onNodeWithTag("search_services_input").assertDoesNotExist()
    composeTestRule.onNodeWithTag("btn_header_search").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("search_services_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_close_search").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("search_services_input").assertDoesNotExist()

    // Categories & Filter row moved out of ServicesScreen into AppMenuDrawerContent
    composeTestRule.onNodeWithTag("filter_tab_cabeleireiro").assertDoesNotExist()
    composeTestRule.onNodeWithTag("filter_tab_manicure").assertDoesNotExist()
    composeTestRule.onNodeWithTag("btn_open_service_filter").assertDoesNotExist()

    // Applying category filter externally (as drawer does) filters list and shows clearable indicator
    selectedCategory = "Manicure"
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("active_filter_indicator").assertIsDisplayed()
    composeTestRule.onNodeWithText("Manicure e Pedicure Completa").assertIsDisplayed()
    composeTestRule.onNodeWithText("Corte e Escova em Domicílio").assertDoesNotExist()

    composeTestRule.onNodeWithTag("btn_clear_active_filter").performClick()
    composeTestRule.waitForIdle()
    assertEquals("Serviços", selectedCategory)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `provider cancel service dialog displays account block and admin refund warning banner`() {
    val provider = User(
      id = "pro_1",
      name = "Roberto Eletricista",
      email = "roberto@resolveai.com",
      phone = "(11) 98888-0002",
      role = UserRole.PRESTADOR,
      category = "Eletricista"
    )
    val acceptedOrder = ServiceRequest(
      id = "srv_prov_cancel",
      clientId = "cli_1",
      clientName = "Mariana Cliente",
      title = "Instalação de Chuveiro 220V",
      category = "Eletricista",
      description = "Troca de fiação e disjuntor",
      location = "Centro - Curitiba, PR",
      proposedBudget = 200.0,
      finalAgreedPrice = 200.0,
      platformFee = 20.0,
      acceptedProviderId = "pro_1",
      acceptedProviderName = "Roberto Eletricista",
      status = ServiceStatus.ACEITO
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        ServicesScreen(
          currentUser = provider,
          services = listOf(acceptedOrder),
          proposals = emptyList(),
          selectedServiceId = "srv_prov_cancel",
          searchQuery = "",
          categoryFilter = "Serviços",
          onSearchChange = {},
          onCategoryChange = {},
          onSelectService = {},
          onCreateServiceClick = {},
          onSubmitProposal = { _, _, _, _ -> },
          onOpenChat = {},
          onUpdateStatus = { _, _ -> },
          onRateService = { _, _, _ -> },
          onCancelService = { _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithTag("btn_provider_cancel_service").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("provider_cancel_warning_banner").assertIsDisplayed()
    composeTestRule.onNodeWithText("Evite cancelamento", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("administrador", substring = true).assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `client completed service detail displays client completed photos section with gallery and camera`() {
    val client = User(
      id = "cli_1",
      name = "Mariana Cliente",
      email = "mariana@resolveai.com",
      phone = "(41) 99999-0000",
      role = UserRole.CLIENTE
    )
    val concludedOrder = ServiceRequest(
      id = "srv_done_photos",
      clientId = "cli_1",
      clientName = "Mariana Cliente",
      title = "Pintura da Sala",
      category = "Pintura",
      description = "Duas demãos em parede branca",
      location = "Batel - Curitiba, PR",
      proposedBudget = 400.0,
      finalAgreedPrice = 400.0,
      platformFee = 40.0,
      acceptedProviderId = "pro_1",
      acceptedProviderName = "Ana Pintora",
      status = ServiceStatus.CONCLUIDO
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        ServicesScreen(
          currentUser = client,
          services = listOf(concludedOrder),
          proposals = emptyList(),
          selectedServiceId = "srv_done_photos",
          searchQuery = "",
          categoryFilter = "Concluído",
          onSearchChange = {},
          onCategoryChange = {},
          onSelectService = {},
          onCreateServiceClick = {},
          onSubmitProposal = { _, _, _, _ -> },
          onOpenChat = {},
          onUpdateStatus = { _, _ -> },
          onRateService = { _, _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithTag("client_completed_photos_section").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_client_completed_photo_gallery").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_client_completed_photo_camera").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Taxa", substring = true).assertDoesNotExist()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `auth registration shows gallery and camera for client profile photo and camera only for provider`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        AuthScreen(
          authMode = AuthMode.REGISTER,
          authStep = AuthFlowStep.FORM,
          pendingGoogleAccount = null,
          authErrorMessage = null,
          onSelectMode = {},
          onClearError = {},
          onGoogleSignInClick = { _, _ -> },
          onCompleteGoogleRegistration = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
        )
      }
    }

    // Default Cliente role offers both Gallery and Camera profile photo controls
    composeTestRule.onNodeWithTag("client_profile_photo_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_client_profile_gallery").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_client_profile_camera").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("provider_camera_profile_photo_card").assertDoesNotExist()

    // Switch to Prestador -> requires camera-only profile photo (no gallery button)
    composeTestRule.onNodeWithTag("auth_reg_role_prestador").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("provider_camera_profile_photo_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_client_profile_gallery").assertDoesNotExist()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `suggestions and tips screen filters tips by role and submits suggestion`() {
    val client = User(
      id = "cli_1",
      name = "Carlos Cliente",
      email = "carlos@resolveai.com",
      phone = "(11) 98888-1111",
      role = UserRole.CLIENTE
    )
    val tips = com.example.data.MarketplaceRepository().appTips
    var submittedTitle = ""
    var submittedDesc = ""

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.SuggestionsTipsScreen(
          currentUser = client,
          tips = tips,
          suggestions = emptyList(),
          onSubmitSuggestion = { _, title, desc ->
            submittedTitle = title
            submittedDesc = desc
          },
          onUpdateSuggestionStatus = { _, _, _ -> },
          onBack = {}
        )
      }
    }

    composeTestRule.onNodeWithText("Dicas de Uso do Aplicativo").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tips_filter_prestador").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("input_suggestion_title").performScrollTo().performTextInput("Agendamento recorrente")
    composeTestRule.onNodeWithTag("input_suggestion_description").performScrollTo().performTextInput("Seria ótimo poder agendar diarista semanal automaticamente.")
    composeTestRule.onNodeWithTag("btn_submit_suggestion").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    assertEquals("Agendamento recorrente", submittedTitle)
    assertEquals("Seria ótimo poder agendar diarista semanal automaticamente.", submittedDesc)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `onboarding component navigates across all 3 slides explaining proposals counter offers and private chat`() {
    var finishedCount = 0
    var skippedCount = 0

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.components.ResolveAiOnboardingComponent(
          onFinish = { finishedCount++ },
          onSkip = { skippedCount++ }
        )
      }
    }

    // Slide 1: Propostas
    composeTestRule.onNodeWithTag("onboarding_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_slide_0").assertIsDisplayed()
    composeTestRule.onNodeWithText("1 DE 3 • PROPOSTAS").assertIsDisplayed()
    composeTestRule.onNodeWithText("Envie ou receba propostas sob medida").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_onboarding_prev").assertDoesNotExist()

    // Advance to Slide 2: Contrapropostas
    composeTestRule.onNodeWithTag("btn_onboarding_next").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_slide_1").assertIsDisplayed()
    composeTestRule.onNodeWithText("2 DE 3 • CONTRAPROPOSTAS").assertIsDisplayed()
    composeTestRule.onNodeWithText("Negocie valores com contrapropostas").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_onboarding_prev").assertIsDisplayed()

    // Advance to Slide 3: Chat Privado
    composeTestRule.onNodeWithTag("btn_onboarding_next").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_slide_2").assertIsDisplayed()
    composeTestRule.onNodeWithText("3 DE 3 • CHAT PRIVADO").assertIsDisplayed()
    composeTestRule.onNodeWithText("Chat privado com proteção de dados").assertIsDisplayed()
    composeTestRule.onNodeWithText("Começar a usar").assertIsDisplayed()

    // Go back one slide then forward and finish
    composeTestRule.onNodeWithTag("btn_onboarding_prev").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_slide_1").assertIsDisplayed()

    composeTestRule.onNodeWithTag("btn_onboarding_next").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("btn_onboarding_next").performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, finishedCount)

    // Skip button triggers onSkip
    composeTestRule.onNodeWithTag("btn_onboarding_skip").performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, skippedCount)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `auth screen and menu drawer expose entry points to open onboarding`() {
    var authReplayCount = 0
    var drawerReplayCount = 0

    val client = User(
      id = "cli_1",
      name = "Maria Cliente",
      email = "maria@resolveai.com",
      phone = "(11) 98888-1111",
      role = UserRole.CLIENTE
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        var showDrawer by remember { mutableStateOf(false) }
        if (!showDrawer) {
          AuthScreen(
            authMode = AuthMode.LOGIN,
            authStep = AuthFlowStep.FORM,
            pendingGoogleAccount = null,
            authErrorMessage = null,
            onSelectMode = {},
            onClearError = {},
            onGoogleSignInClick = { _, _ -> },
            onCompleteGoogleRegistration = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
            onOpenOnboarding = {
              authReplayCount++
              showDrawer = true
            }
          )
        } else {
          AppMenuDrawerContent(
            currentUser = client,
            selectedTab = MainTab.SERVICOS,
            pendingProposalsCount = 0,
            unreadNotificationsCount = 0,
            onSelectTab = {},
            onNotificationsClick = {},
            onSupportClick = {},
            onAdminClick = {},
            onSwitchUserClick = {},
            onRegisterClick = {},
            onRechargeClick = {},
            onOpenOnboarding = { drawerReplayCount++ },
            onCloseDrawer = {}
          )
        }
      }
    }

    composeTestRule.onNodeWithTag("auth_btn_open_onboarding").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, authReplayCount)

    composeTestRule.onNodeWithTag("menu_item_onboarding").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, drawerReplayCount)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `admin catalog and edits screen manages categories edits service and saves app settings`() {
    val repo = com.example.data.MarketplaceRepository()
    val admin = repo.users.value.first { it.role == UserRole.ADMIN }
    repo.switchUser(admin.id)

    composeTestRule.setContent {
      val categories by repo.serviceCategories.collectAsState()
      val services by repo.services.collectAsState()
      val settings by repo.appAdminSettings.collectAsState()
      val tips = repo.appTips

      MyApplicationTheme {
        com.example.ui.screens.AdminCatalogAndEditsScreen(
          currentUser = admin,
          categories = categories,
          services = services,
          appSettings = settings,
          appTips = tips,
          onAddCategory = { name, spec -> repo.adminAddServiceCategory(name, spec) },
          onUpdateCategory = { id, name, spec, active -> repo.adminUpdateServiceCategory(id, name, spec, active) },
          onToggleCategory = { id, active -> repo.adminToggleServiceCategory(id, active) },
          onMoveCategory = { id, up -> repo.adminMoveServiceCategory(id, up) },
          onDeleteCategory = { id -> repo.adminDeleteServiceCategory(id) },
          onResetDefaultCategories = { repo.adminResetDefaultCategories() },
          onUpdateService = { srv -> repo.adminUpdateServiceRequest(srv) },
          onUpdateAppSettings = { cfg -> repo.adminUpdateAppSettings(cfg) },
          onSaveTip = { tip -> repo.adminSaveAppTip(tip) },
          onDeleteTip = { id -> repo.adminDeleteAppTip(id) },
          onOpenManageUsers = {},
          onOpenDocumentsReview = {},
          onOpenSuggestions = {},
          onOpenMenu = {},
          onBackToHome = {}
        )
      }
    }

    // 1. Adicionar nova categoria pela interface Admin
    composeTestRule.onNodeWithTag("btn_admin_add_category").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("input_admin_category_name").performTextInput("Jardinagem")
    composeTestRule.onNodeWithTag("input_admin_category_specialty").performTextInput("Jardinagem & Paisagismo")
    composeTestRule.onNodeWithTag("btn_confirm_admin_category").performClick()
    composeTestRule.waitForIdle()
    assertTrue(repo.serviceCategories.value.any { it.name == "Jardinagem" })

    // 2. Ir para aba "Editar Serviços" e editar o primeiro serviço
    val targetSrv = repo.services.value.first()
    composeTestRule.onNodeWithTag("admin_catalog_tab_servicos").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("admin_edit_services_list")
      .performScrollToNode(hasTestTag("btn_admin_edit_service_${targetSrv.id}"))
    composeTestRule.onNodeWithTag("btn_admin_edit_service_${targetSrv.id}").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("input_admin_edit_service_title").performTextReplacement("Título Editado Admin")
    composeTestRule.onNodeWithTag("btn_confirm_admin_edit_service").performClick()
    composeTestRule.waitForIdle()
    assertEquals("Título Editado Admin", repo.services.value.first { it.id == targetSrv.id }.title)

    // 3. Ir para aba "Outras Edições" e salvar banner global
    composeTestRule.onNodeWithTag("admin_catalog_tab_outras_edicoes").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("input_admin_global_banner").performTextReplacement("Promoção de atendimento hoje!")
    composeTestRule.onNodeWithTag("btn_admin_save_settings").performClick()
    composeTestRule.waitForIdle()
    assertEquals("Promoção de atendimento hoje!", repo.appAdminSettings.value.globalBannerMessage)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `admin entry button appears in drawer and services screen only for admin`() {
    val admin = User(
      id = "adm_1",
      name = "Admin ResolveAi",
      email = "admin@resolveai.com",
      phone = "(61) 99290-7464",
      role = UserRole.ADMIN
    )
    var openedCatalogCount = 0

    composeTestRule.setContent {
      MyApplicationTheme {
        Column {
          AppMenuDrawerContent(
            currentUser = admin,
            selectedTab = MainTab.SERVICOS,
            pendingProposalsCount = 0,
            unreadNotificationsCount = 0,
            onSelectTab = { if (it == MainTab.ADMIN_CATALOG_EDICOES) openedCatalogCount++ },
            onNotificationsClick = {},
            onSupportClick = {},
            onAdminClick = {},
            onSwitchUserClick = {},
            onRegisterClick = {},
            onRechargeClick = {},
            onCloseDrawer = {}
          )
        }
      }
    }

    composeTestRule.onNodeWithTag("menu_item_admin_catalog_edits").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, openedCatalogCount)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `provider direct acceptance waits for client choice and client sees choose provider action`() {
    val client = User(
      id = "cli_1",
      name = "Carlos Cliente",
      email = "carlos@resolveai.com",
      phone = "(11) 98888-0001",
      role = UserRole.CLIENTE
    )
    val provider1 = User(
      id = "pro_1",
      name = "Roberto Eletricista",
      email = "roberto@resolveai.com",
      phone = "(11) 98888-0002",
      role = UserRole.PRESTADOR,
      category = "Eletricista",
      walletBalance = 10.0
    )
    val order = ServiceRequest(
      id = "srv_multi_accept",
      clientId = "cli_1",
      clientName = "Carlos Cliente",
      title = "Troca de Chuveiro 220V",
      category = "Eletricista",
      description = "Instalação completa com fiação",
      location = "Centro - SP",
      proposedBudget = 250.0,
      status = ServiceStatus.EM_NEGOCIACAO
    )
    val acceptancePro1 = Proposal(
      id = "prop_acc_1",
      serviceId = "srv_multi_accept",
      providerId = "pro_1",
      providerName = "Roberto Eletricista",
      proposedPrice = 250.0,
      status = ProposalStatus.ENVIADA,
      isDirectAcceptance = true,
      description = "Aceitou realizar o serviço pelo valor proposto de R$ 250,00."
    )
    val acceptancePro2 = Proposal(
      id = "prop_acc_2",
      serviceId = "srv_multi_accept",
      providerId = "pro_2",
      providerName = "Ana Eletricista",
      proposedPrice = 250.0,
      status = ProposalStatus.ENVIADA,
      isDirectAcceptance = true,
      description = "Aceitou realizar o serviço pelo valor proposto de R$ 250,00."
    )

    var showAsClient by mutableStateOf(false)
    var chosenProposalId = ""

    composeTestRule.setContent {
      MyApplicationTheme {
        ServicesScreen(
          currentUser = if (showAsClient) client else provider1,
          services = listOf(order),
          proposals = listOf(acceptancePro1, acceptancePro2),
          selectedServiceId = "srv_multi_accept",
          searchQuery = "",
          categoryFilter = "Serviços",
          onSearchChange = {},
          onCategoryChange = {},
          onSelectService = {},
          onCreateServiceClick = {},
          onSubmitProposal = { _, _, _, _ -> },
          onAcceptProposal = { chosenProposalId = it },
          onOpenChat = {},
          onUpdateStatus = { _, _ -> },
          onRateService = { _, _, _ -> }
        )
      }
    }

    // Visão do Prestador 1 que já aceitou: botão fica "Aceite Enviado" (desabilitado), chat não abriu ainda
    composeTestRule.onNodeWithTag("provider_waiting_client_choice_banner").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_provider_aceitar").performScrollTo().assertIsNotEnabled()
    composeTestRule.onNodeWithText("Aceite Enviado").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_provider_open_chat").assertDoesNotExist()

    // Troca para visão do Cliente: vê os 2 prestadores que aceitaram e pode escolher um antes de abrir o chat
    showAsClient = true
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("btn_client_open_chat").assertDoesNotExist()
    composeTestRule.onNodeWithTag("client_service_detail_list")
      .performScrollToNode(hasTestTag("badge_provider_accepted_prop_acc_1"))
    composeTestRule.onNodeWithTag("badge_provider_accepted_prop_acc_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("client_service_detail_list")
      .performScrollToNode(hasTestTag("btn_client_accept_proposal_prop_acc_1"))
    composeTestRule.onNodeWithTag("btn_client_accept_proposal_prop_acc_1").performClick()
    composeTestRule.waitForIdle()
    assertEquals("prop_acc_1", chosenProposalId)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `wallet profile displays negative balance notice and insufficient dialog hides fee for client`() {
    val negativeProvider = User(
      id = "pro_neg",
      name = "Carlos Prestador",
      email = "carlos.pro@resolveai.com",
      phone = "(11) 97777-3333",
      role = UserRole.PRESTADOR,
      category = "Pintura",
      walletBalance = -24.50
    )

    var showDialogForClient by mutableStateOf(false)

    composeTestRule.setContent {
      MyApplicationTheme {
        WalletProfileScreen(
          currentUser = negativeProvider,
          walletTransactions = emptyList(),
          onRechargeClick = {},
          onSwitchUserClick = {},
          onEditProfileClick = {},
          onSaveProfile = {}
        )
        if (showDialogForClient) {
          com.example.ui.components.InsufficientBalanceDialog(
            data = com.example.data.InsufficientBalanceException(
              requiredFee = 30.0,
              currentBalance = -5.0,
              missingAmount = 6.01,
              providerName = "Carlos Prestador"
            ),
            viewerRole = UserRole.CLIENTE,
            onDismiss = {},
            onRechargeClick = {}
          )
        }
      }
    }

    composeTestRule.onNodeWithTag("provider_wallet_negative_notice").assertIsDisplayed()
    val expectedNegFormatted = "-R$ ${String.format("%.2f", 24.50)}"
    composeTestRule.onNodeWithText(expectedNegFormatted, substring = true).assertIsDisplayed()

    showDialogForClient = true
    composeTestRule.waitForIdle()
    // Cliente nunca vê avisos de taxa (10%) no diálogo
    composeTestRule.onNodeWithText("Taxa deste serviço (10%)", substring = true).assertDoesNotExist()
    composeTestRule.onNodeWithText("Carteira do Prestador Indisponível").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `service rating dialog selects 1 to 5 stars tags and submits review without fee copy`() {
    val concludedService = ServiceRequest(
      id = "srv_rate_dialog_1",
      clientId = "cli_1",
      clientName = "Mariana Cliente",
      title = "Instalação de Ventilador de Teto",
      category = "Eletricista",
      description = "Quarto casal",
      location = "Centro - SP",
      proposedBudget = 180.0,
      finalAgreedPrice = 180.0,
      status = ServiceStatus.CONCLUIDO,
      acceptedProviderId = "pro_1",
      acceptedProviderName = "Roberto Eletricista"
    )
    var submittedRating = 0f
    var submittedComment = ""
    var submittedTags: List<String> = emptyList()

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.components.ServiceRatingDialog(
          service = concludedService,
          onDismiss = {},
          onSubmit = { rating, comment, tags ->
            submittedRating = rating
            submittedComment = comment
            submittedTags = tags
          }
        )
      }
    }

    composeTestRule.onNodeWithTag("service_rating_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithText("Taxa", substring = true).assertDoesNotExist()
    composeTestRule.onNodeWithTag("star_rate_4").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("4 / 5 • Muito Bom (4 estrelas)").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tag_rating_Pontual").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("input_rating_comment").performScrollTo().performTextInput("Atendimento excelente!")
    composeTestRule.onNodeWithTag("btn_submit_rating").performClick()
    composeTestRule.waitForIdle()

    assertEquals(4.0f, submittedRating, 0.01f)
    assertEquals("Atendimento excelente!", submittedComment)
    assertEquals(listOf("Pontual"), submittedTags)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `client service detail hides provider profile and rating until client accepts and exposes reject button`() {
    val client = User(
      id = "cli_1",
      name = "Carlos Cliente",
      email = "carlos@resolveai.com",
      phone = "(11) 98888-0001",
      role = UserRole.CLIENTE
    )
    val openOrder = ServiceRequest(
      id = "srv_vis_1",
      clientId = "cli_1",
      clientName = "Carlos Cliente",
      title = "Instalação de Chuveiro Elétrico",
      category = "Eletricista",
      description = "Chuveiro 220V novo",
      location = "Moema - SP",
      proposedBudget = 220.0,
      status = ServiceStatus.EM_NEGOCIACAO
    )
    val directProposal = Proposal(
      id = "prop_vis_1",
      serviceId = "srv_vis_1",
      providerId = "pro_1",
      providerName = "Roberto Eletricista",
      providerRating = 4.92,
      providerCategory = "Eletricista",
      proposedPrice = 220.0,
      status = ProposalStatus.ENVIADA,
      isDirectAcceptance = true,
      description = "Aceitou realizar o serviço pelo valor proposto de R$ 220,00."
    )
    val reviewForRoberto = com.example.model.ServiceReview(
      id = "rev_prev_1",
      serviceId = "srv_old_1",
      serviceTitle = "Troca de Tomadas",
      clientId = "cli_2",
      clientName = "Ana",
      providerId = "pro_1",
      providerName = "Roberto Eletricista",
      rating = 5f,
      comment = "Excelente trabalho!",
      createdAt = 1000L
    )

    var activeService by mutableStateOf(openOrder)
    var activeProposals by mutableStateOf(listOf(directProposal))
    var rejectedProposalId = ""

    composeTestRule.setContent {
      MyApplicationTheme {
        ServicesScreen(
          currentUser = client,
          services = listOf(activeService),
          proposals = activeProposals,
          reviews = listOf(reviewForRoberto),
          selectedServiceId = "srv_vis_1",
          searchQuery = "",
          categoryFilter = "Serviços",
          onSearchChange = {},
          onCategoryChange = {},
          onSelectService = {},
          onCreateServiceClick = {},
          onSubmitProposal = { _, _, _, _ -> },
          onRejectProposal = { rejectedProposalId = it },
          onOpenChat = {},
          onUpdateStatus = { _, _ -> },
          onRateService = { _, _, _ -> }
        )
      }
    }

    // Antes de aceitar o prestador: perfil/avatar e nota NÃO aparecem no serviço do cliente
    composeTestRule.onNodeWithText("Aguardando escolha").assertIsDisplayed()
    composeTestRule.onNodeWithTag("accepted_provider_profile_pro_1").assertDoesNotExist()
    composeTestRule.onNodeWithTag("btn_view_provider_reviews_pro_1").assertDoesNotExist()
    composeTestRule.onNodeWithText("4.92").assertDoesNotExist()
    composeTestRule.onNodeWithTag("btn_client_open_chat").assertDoesNotExist()

    // Selo de aceite direto e opção de Recusar + Aceitar visíveis
    composeTestRule.onNodeWithTag("client_service_detail_list")
      .performScrollToNode(hasTestTag("btn_client_reject_proposal_prop_vis_1"))
    composeTestRule.onNodeWithTag("badge_provider_accepted_prop_vis_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_client_accept_proposal_prop_vis_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_client_reject_proposal_prop_vis_1").performClick()
    composeTestRule.waitForIdle()
    assertEquals("prop_vis_1", rejectedProposalId)

    // Após cliente aceitar o prestador: perfil + nota + chat exclusivo aparecem!
    activeService = openOrder.copy(
      status = ServiceStatus.ACEITO,
      acceptedProviderId = "pro_1",
      acceptedProviderName = "Roberto Eletricista",
      acceptedProviderRating = 4.92,
      finalAgreedPrice = 220.0
    )
    activeProposals = listOf(directProposal.copy(status = ProposalStatus.ACEITA))
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("client_service_detail_list")
      .performScrollToNode(hasTestTag("accepted_provider_profile_pro_1"))
    composeTestRule.onNodeWithTag("accepted_provider_profile_pro_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_view_provider_reviews_pro_1", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("client_service_detail_list")
      .performScrollToNode(hasTestTag("btn_accepted_proposal_chat_prop_vis_1"))
    composeTestRule.onNodeWithTag("btn_accepted_proposal_chat_prop_vis_1").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `provider orders screen shows radar animation when no pending services and hides service accepted by another provider`() {
    val pro1 = User(
      id = "pro_1",
      name = "Roberto Eletricista",
      email = "roberto@resolveai.com",
      phone = "(11) 98888-0002",
      role = UserRole.PRESTADOR,
      category = "Eletricista"
    )
    val pro2 = User(
      id = "pro_2",
      name = "Marcos Eletricista",
      email = "marcos@resolveai.com",
      phone = "(11) 98888-0003",
      role = UserRole.PRESTADOR,
      category = "Eletricista"
    )
    val acceptedByPro1 = ServiceRequest(
      id = "srv_excl_1",
      clientId = "cli_1",
      clientName = "Carlos Cliente",
      title = "Troca de Quadro Elétrico Exclusivo",
      category = "Eletricista",
      description = "Serviço fechado com pro_1",
      location = "Centro - SP",
      proposedBudget = 400.0,
      finalAgreedPrice = 400.0,
      status = ServiceStatus.ACEITO,
      acceptedProviderId = "pro_1",
      acceptedProviderName = "Roberto Eletricista"
    )

    var activeProvider by mutableStateOf(pro2)
    composeTestRule.setContent {
      MyApplicationTheme {
        ServicesScreen(
          currentUser = activeProvider,
          services = listOf(acceptedByPro1),
          proposals = emptyList(),
          selectedServiceId = null,
          searchQuery = "",
          categoryFilter = "Serviços",
          onSearchChange = {},
          onCategoryChange = {},
          onSelectService = {},
          onCreateServiceClick = {},
          onSubmitProposal = { _, _, _, _ -> },
          onOpenChat = {},
          onUpdateStatus = { _, _ -> },
          onRateService = { _, _, _ -> }
        )
      }
    }

    // Para pro_2: o serviço fechado com pro_1 saiu da lista, exibindo o radar "Procurando serviços"
    composeTestRule.onNodeWithTag("service_card_srv_excl_1").assertDoesNotExist()
    composeTestRule.onNodeWithTag("provider_radar_empty_state").assertIsDisplayed()
    composeTestRule.onNodeWithTag("provider_radar_canvas").assertIsDisplayed()
    composeTestRule.onNodeWithText("Procurando serviços").assertIsDisplayed()

    // Para pro_1 (prestador aceito pelo cliente): o serviço aparece normalmente com opção de chat!
    activeProvider = pro1
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("provider_radar_empty_state").assertDoesNotExist()
    composeTestRule.onNodeWithTag("service_card_srv_excl_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_quick_chat_srv_excl_1").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `wallet profile displays provider reviews summary card and sends inline reply`() {
    val provider = User(
      id = "pro_1",
      name = "Roberto Eletricista",
      email = "roberto@resolveai.com",
      phone = "(11) 98888-0002",
      role = UserRole.PRESTADOR,
      category = "Eletricista",
      rating = 4.8,
      ratingCount = 1
    )
    val review = com.example.model.ServiceReview(
      id = "srv_rev_w1",
      serviceId = "srv_rev_w1",
      serviceTitle = "Revisão Elétrica Completa",
      serviceCategory = "Eletricista",
      clientId = "cli_1",
      clientName = "Carlos Eduardo",
      providerId = "pro_1",
      providerName = "Roberto Eletricista",
      rating = 5f,
      comment = "Serviço impecável e rápido!",
      complimentTags = listOf("Pontual", "Trabalho Rápido"),
      createdAt = 1_700_000_000_000L
    )
    var repliedId = ""
    var repliedText = ""

    composeTestRule.setContent {
      MyApplicationTheme {
        WalletProfileScreen(
          currentUser = provider,
          walletTransactions = emptyList(),
          reviews = listOf(review),
          onRechargeClick = {},
          onSwitchUserClick = {},
          onEditProfileClick = {},
          onSaveProfile = {},
          onReplyToReview = { id, text ->
            repliedId = id
            repliedText = text
          }
        )
      }
    }

    composeTestRule.onNodeWithTag("wallet_profile_list")
      .performScrollToNode(hasTestTag("provider_reviews_summary_card"))
    composeTestRule.onNodeWithTag("provider_reviews_summary_card").assertIsDisplayed()

    composeTestRule.onNodeWithTag("wallet_profile_list")
      .performScrollToNode(hasTestTag("review_card_srv_rev_w1"))
    composeTestRule.onNodeWithTag("review_card_srv_rev_w1").assertIsDisplayed()

    composeTestRule.onNodeWithTag("wallet_profile_list")
      .performScrollToNode(hasTestTag("btn_reply_review_srv_rev_w1"))
    composeTestRule.onNodeWithTag("btn_reply_review_srv_rev_w1").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("input_review_reply_srv_rev_w1").performTextInput("Muito obrigado, Carlos!")
    composeTestRule.onNodeWithTag("btn_send_review_reply_srv_rev_w1").performClick()
    composeTestRule.waitForIdle()

    assertEquals("srv_rev_w1", repliedId)
    assertEquals("Muito obrigado, Carlos!", repliedText)
  }

  @Test
  fun `application and GoogleAuthManager initialize Firebase Authentication and authStateFlow`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val auth = com.example.data.GoogleAuthManager.ensureFirebaseAuthInitialized(context)
    assertNotNull("FirebaseAuth deve estar inicializado na aplicação", auth)
    assertEquals("pt-BR", auth?.languageCode)
    assertTrue(FirebaseApp.getApps(context).isNotEmpty())
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `login screen features Firebase Google Sign-In card prominently and handles loading and click`() {
    var googleClicks = 0
    var loadingState by mutableStateOf(false)

    composeTestRule.setContent {
      MyApplicationTheme {
        AuthScreen(
          authMode = AuthMode.LOGIN,
          authStep = AuthFlowStep.FORM,
          pendingGoogleAccount = null,
          authErrorMessage = null,
          isGoogleAuthLoading = loadingState,
          onSelectMode = {},
          onClearError = {},
          onGoogleSignInClick = { _, _ -> googleClicks++ },
          onCompleteGoogleRegistration = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
        )
      }
    }

    // Sem necessidade de scroll prévio, o card e botão do Firebase Google Sign-In aparecem em destaque no topo
    composeTestRule.onNodeWithTag("auth_google_login_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("Autenticação segura via Firebase").assertIsDisplayed()
    composeTestRule.onNodeWithText("Entrar com sua Conta Google").assertIsDisplayed()
    composeTestRule.onNodeWithTag("auth_btn_google_login").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, googleClicks)

    // Estado de carregamento ao autenticar com Google
    loadingState = true
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Conectando com Google...").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `new service dialog shows current location button on address bar and fills detected address`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    Shadows.shadowOf(app).grantPermissions(
      Manifest.permission.ACCESS_FINE_LOCATION,
      Manifest.permission.ACCESS_COARSE_LOCATION
    )

    var capturedLocation = ""

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.components.CreateServiceDialog(
          onDismiss = {},
          onCreate = { _, _, _, location, _, _, _, _ ->
            capturedLocation = location
          },
          detectAddressProvider = {
            CurrentCityResult.Success(
              cityFormatted = "Av. Paulista, 1000 - Bela Vista - São Paulo, SP",
              latitude = -23.561414,
              longitude = -46.655881
            )
          }
        )
      }
    }

    composeTestRule.onNodeWithTag("input_service_title").performTextInput("Troca de disjuntor")
    composeTestRule.onNodeWithTag("input_service_description").performScrollTo().performTextInput("Disjuntor geral desarmando")
    composeTestRule.onNodeWithTag("btn_gps_current_address", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_inline_current_address").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("gps_city_success_hint").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("input_service_budget").performScrollTo().performTextInput("250")
    composeTestRule.onNodeWithTag("btn_submit_create_service").performClick()
    composeTestRule.waitForIdle()

    assertEquals("Av. Paulista, 1000 - Bela Vista - São Paulo, SP", capturedLocation)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `admin users screen shows manual add balance button only for providers and credits balance`() {
    val adminUser = User(
      id = "adm_1",
      name = "Edilson Admin",
      email = "admin@resolveai.com",
      phone = "(61) 99290-7464",
      role = UserRole.ADMIN
    )
    val clientUser = User(
      id = "cli_1",
      name = "Mariana Cliente",
      email = "mariana@example.com",
      phone = "(11) 98888-1111",
      role = UserRole.CLIENTE
    )
    val providerUser = User(
      id = "pro_1",
      name = "Roberto Prestador",
      email = "roberto@example.com",
      phone = "(11) 97777-2222",
      role = UserRole.PRESTADOR,
      category = "Eletricista",
      walletBalance = 40.0
    )

    var usersState by mutableStateOf(listOf(adminUser, clientUser, providerUser))
    var creditedAmount = 0.0
    var creditedReason = ""

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.AdminUsersScreen(
          currentUser = adminUser,
          users = usersState,
          isLoading = false,
          syncStatusMessage = null,
          onSyncUsersClick = {},
          onUpdateUser = { Result.success(Unit) },
          onAddProviderBalance = { providerId, amount, reason ->
            creditedAmount = amount
            creditedReason = reason
            var updatedBalance = 0.0
            usersState = usersState.map { u ->
              if (u.id == providerId) {
                updatedBalance = u.walletBalance + amount
                u.copy(walletBalance = updatedBalance)
              } else {
                u
              }
            }
            Result.success(updatedBalance)
          },
          onDeleteUser = { Result.success(Unit) },
          onOpenMenu = {},
          onBackToHome = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("admin_users_list")
      .performScrollToNode(hasTestTag("admin_user_card_cli_1"))
    composeTestRule.onNodeWithTag("btn_admin_add_balance_cli_1").assertDoesNotExist()

    composeTestRule.onNodeWithTag("admin_users_list")
      .performScrollToNode(hasTestTag("admin_user_card_pro_1"))
    composeTestRule.onNodeWithTag("btn_admin_add_balance_pro_1").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("chip_add_balance_50").performClick()
    composeTestRule.onNodeWithTag("input_admin_add_balance_reason").performTextInput("Bônus fidelidade")
    composeTestRule.onNodeWithTag("btn_confirm_add_balance").performClick()
    composeTestRule.waitForIdle()

    assertEquals(50.0, creditedAmount, 0.01)
    assertEquals("Bônus fidelidade", creditedReason)
    assertEquals(90.0, usersState.first { it.id == "pro_1" }.walletBalance, 0.01)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `provider service details shows decline button and removes declined order from list`() {
    val provider = User(
      id = "pro_1",
      name = "Roberto Mendes",
      email = "roberto@example.com",
      phone = "(11) 99999-0001",
      role = UserRole.PRESTADOR,
      category = "Eletricista",
      walletBalance = 120.0
    )
    val order = ServiceRequest(
      id = "srv_dec_ui_1",
      clientId = "cli_1",
      clientName = "Carlos Eduardo",
      title = "Instalação de refletor externo",
      category = "Eletricista",
      description = "Área da garagem",
      location = "Pinheiros - São Paulo, SP",
      proposedBudget = 190.0,
      status = ServiceStatus.ABERTO
    )

    var selectedId by mutableStateOf<String?>("srv_dec_ui_1")
    var declinedIds by mutableStateOf(emptySet<String>())

    composeTestRule.setContent {
      MyApplicationTheme {
        ServicesScreen(
          currentUser = provider,
          services = listOf(order),
          proposals = emptyList(),
          selectedServiceId = selectedId,
          searchQuery = "",
          categoryFilter = "Serviços",
          onSearchChange = {},
          onCategoryChange = {},
          onSelectService = { selectedId = it },
          onCreateServiceClick = {},
          onSubmitProposal = { _, _, _, _ -> },
          onOpenChat = {},
          onUpdateStatus = { _, _ -> },
          onRateService = { _, _, _ -> },
          declinedServiceIds = declinedIds,
          onProviderDeclineService = { sId ->
            declinedIds = declinedIds + sId
            selectedId = null
          }
        )
      }
    }

    composeTestRule.onNodeWithTag("btn_provider_decline_service").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    assertTrue("srv_dec_ui_1" in declinedIds)
    assertEquals(null, selectedId)
    composeTestRule.onNodeWithTag("service_card_srv_dec_ui_1").assertDoesNotExist()
    composeTestRule.onNodeWithTag("provider_radar_empty_state").assertIsDisplayed()
  }

  @Test
  fun `main activity keeps screen on while in foreground`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    val hasKeepScreenOn = (activity.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
    assertTrue("MainActivity deve manter FLAG_KEEP_SCREEN_ON ativa em primeiro plano", hasKeepScreenOn)
    controller.pause().stop().destroy()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `provider order history screen displays finished canceled declined orders filters and detail dialog`() {
    val provider = User(
      id = "pro_1",
      name = "Roberto Mendes",
      email = "roberto@example.com",
      phone = "(11) 99999-0001",
      role = UserRole.PRESTADOR,
      category = "Eletricista",
      declinedServiceIds = listOf("srv_h_declined")
    )
    val doneSrv = ServiceRequest(
      id = "srv_h_done",
      clientId = "cli_1",
      clientName = "Carlos Eduardo",
      title = "Manutenção de disjuntor geral",
      category = "Eletricista",
      description = "Substituição concluída",
      location = "Pinheiros - São Paulo, SP",
      proposedBudget = 320.0,
      finalAgreedPrice = 350.0,
      status = ServiceStatus.CONCLUIDO,
      acceptedProviderId = "pro_1",
      completionNotes = "Disjuntor trocado e testado"
    )
    val canceledSrv = ServiceRequest(
      id = "srv_h_canceled",
      clientId = "cli_2",
      clientName = "Mariana Silveira",
      title = "Instalação de lustre sala",
      category = "Eletricista",
      description = "Lustre pendente",
      location = "Campinas, SP",
      proposedBudget = 210.0,
      status = ServiceStatus.CANCELADO,
      acceptedProviderId = "pro_1",
      cancellationReason = "Cliente adiou a reforma"
    )
    val declinedSrv = ServiceRequest(
      id = "srv_h_declined",
      clientId = "cli_1",
      clientName = "Carlos Eduardo",
      title = "Ponto de tomada externa",
      category = "Eletricista",
      description = "Área gourmet",
      location = "Moema - São Paulo, SP",
      proposedBudget = 140.0,
      status = ServiceStatus.ABERTO
    )

    var backPressed = false

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.ProviderOrderHistoryScreen(
          currentUser = provider,
          services = listOf(doneSrv, canceledSrv, declinedSrv),
          proposals = emptyList(),
          declinedServiceIds = setOf("srv_h_declined"),
          reviews = emptyList(),
          onBack = { backPressed = true }
        )
      }
    }

    composeTestRule.onNodeWithText("Histórico de Pedidos").assertIsDisplayed()
    composeTestRule.onNodeWithTag("provider_history_summary_card").assertIsDisplayed()

    composeTestRule.onNodeWithTag("provider_history_list")
      .performScrollToNode(hasTestTag("history_order_card_srv_h_done"))
    composeTestRule.onNodeWithTag("history_order_card_srv_h_done").assertIsDisplayed()

    // Filter strictly by Cancelados
    composeTestRule.onNodeWithTag("history_filter_cancelado").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("history_order_card_srv_h_canceled").assertIsDisplayed()
    composeTestRule.onNodeWithTag("history_order_card_srv_h_done").assertDoesNotExist()

    // Filter strictly by Recusados
    composeTestRule.onNodeWithTag("history_filter_recusado").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("history_order_card_srv_h_declined").assertIsDisplayed()
    composeTestRule.onNodeWithTag("history_order_card_srv_h_canceled").assertDoesNotExist()

    // Open detail modal for declined order
    composeTestRule.onNodeWithTag("history_order_card_srv_h_declined").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("provider_history_detail_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_close_history_detail_dialog").performClick()
    composeTestRule.waitForIdle()

    // Back returns to opportunities
    composeTestRule.onNodeWithTag("btn_provider_history_back").performClick()
    assertTrue(backPressed)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `drawer and services screen expose order history entry only for providers`() {
    val providerUser = User(
      id = "pro_1",
      name = "Roberto Prestador",
      email = "roberto@example.com",
      phone = "(11) 99999-0002",
      role = UserRole.PRESTADOR,
      category = "Eletricista"
    )
    val clientUser = User(
      id = "cli_1",
      name = "Carlos Cliente",
      email = "carlos@example.com",
      phone = "(11) 99999-0001",
      role = UserRole.CLIENTE
    )

    var activeUser by mutableStateOf(providerUser)
    var showDrawer by mutableStateOf(false)
    var openedFromDrawer = false
    var openedFromServices = false

    composeTestRule.setContent {
      MyApplicationTheme {
        if (!showDrawer) {
          Column {
            ServicesScreen(
              currentUser = activeUser,
              services = emptyList(),
              proposals = emptyList(),
              selectedServiceId = null,
              searchQuery = "",
              categoryFilter = "Serviços",
              onSearchChange = {},
              onCategoryChange = {},
              onSelectService = {},
              onCreateServiceClick = {},
              onSubmitProposal = { _, _, _, _ -> },
              onOpenChat = {},
              onUpdateStatus = { _, _ -> },
              onRateService = { _, _, _ -> },
              onOpenOrderHistory = if (activeUser.role == UserRole.PRESTADOR) {
                { openedFromServices = true }
              } else {
                null
              }
            )
          }
        } else {
          AppMenuDrawerContent(
            currentUser = providerUser,
            selectedTab = MainTab.SERVICOS,
            pendingProposalsCount = 0,
            unreadNotificationsCount = 0,
            onSelectTab = { if (it == MainTab.HISTORICO_PEDIDOS) openedFromDrawer = true },
            onNotificationsClick = {},
            onSupportClick = {},
            onAdminClick = {},
            onSwitchUserClick = {},
            onRegisterClick = {},
            onRechargeClick = {},
            onLogoutClick = {},
            onCloseDrawer = {}
          )
        }
      }
    }

    // Order History button was moved from ServicesScreen into side drawer menu
    composeTestRule.onNodeWithTag("btn_open_provider_history").assertDoesNotExist()
    assertTrue(!openedFromServices)

    activeUser = clientUser
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("btn_open_provider_history").assertDoesNotExist()

    // Verify drawer shows menu_item_history for provider
    showDrawer = true
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("menu_item_history").performScrollTo().assertIsDisplayed().performClick()
    assertTrue(openedFromDrawer)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `client service detail displays all interested providers upfront with filters and actions`() {
    val client = User(
      id = "cli_all_1",
      name = "Carlos Cliente",
      email = "carlos@resolveai.com",
      phone = "(11) 98888-0001",
      role = UserRole.CLIENTE
    )
    val order = ServiceRequest(
      id = "srv_all_int_1",
      clientId = "cli_all_1",
      clientName = "Carlos Cliente",
      title = "Reforma de Quadro Elétrico",
      category = "Eletricista",
      description = "Troca de 6 disjuntores e barramento.",
      location = "Moema - São Paulo, SP",
      proposedBudget = 350.0,
      status = ServiceStatus.EM_NEGOCIACAO
    )
    val propDirect1 = Proposal(
      id = "prop_int_1",
      serviceId = "srv_all_int_1",
      providerId = "pro_1",
      providerName = "Roberto Mendes",
      providerRating = 4.9,
      providerCategory = "Eletricista",
      proposedPrice = 350.0,
      status = ProposalStatus.ENVIADA,
      isDirectAcceptance = true,
      createdAt = 3000L,
      description = "Aceito realizar pelo orçamento proposto."
    )
    val propCustom2 = Proposal(
      id = "prop_int_2",
      serviceId = "srv_all_int_1",
      providerId = "pro_2",
      providerName = "Marcos Silva",
      providerRating = 4.8,
      providerCategory = "Eletricista Industrial",
      proposedPrice = 390.0,
      estimatedDays = 1,
      status = ProposalStatus.ENVIADA,
      isDirectAcceptance = false,
      createdAt = 2000L,
      description = "Inclui disjuntores novos DIN."
    )
    val propRefused3 = Proposal(
      id = "prop_int_3",
      serviceId = "srv_all_int_1",
      providerId = "pro_3",
      providerName = "Lucas Oliveira",
      providerRating = 4.5,
      providerCategory = "Eletricista",
      proposedPrice = 480.0,
      status = ProposalStatus.RECUSADA,
      isDirectAcceptance = false,
      createdAt = 1000L,
      description = "Faço no fim de semana."
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        ServicesScreen(
          currentUser = client,
          services = listOf(order),
          proposals = listOf(propRefused3, propCustom2, propDirect1),
          selectedServiceId = "srv_all_int_1",
          searchQuery = "",
          categoryFilter = "Serviços",
          onSearchChange = {},
          onCategoryChange = {},
          onSelectService = {},
          onCreateServiceClick = {},
          onSubmitProposal = { _, _, _, _ -> },
          onOpenChat = {},
          onUpdateStatus = { _, _ -> },
          onRateService = { _, _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithTag("client_interested_summary_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("client_interested_providers_section").assertIsDisplayed()
    composeTestRule.onNodeWithText("Prestadores Interessados (3)").assertIsDisplayed()

    // Todos os 3 interessados estão disponíveis no detalhe do pedido do cliente
    composeTestRule.onNodeWithTag("client_service_detail_list")
      .performScrollToNode(hasTestTag("client_proposal_card_prop_int_1"))
    composeTestRule.onNodeWithTag("client_proposal_card_prop_int_1").assertIsDisplayed()

    composeTestRule.onNodeWithTag("client_service_detail_list")
      .performScrollToNode(hasTestTag("client_proposal_card_prop_int_2"))
    composeTestRule.onNodeWithTag("client_proposal_card_prop_int_2").assertIsDisplayed()

    composeTestRule.onNodeWithTag("client_service_detail_list")
      .performScrollToNode(hasTestTag("client_proposal_card_prop_int_3"))
    composeTestRule.onNodeWithTag("client_proposal_card_prop_int_3").assertIsDisplayed()

    // Filtrar apenas ativos (Aguardando) oculta recusado e mantém os 2 ativos
    composeTestRule.onNodeWithTag("client_service_detail_list")
      .performScrollToNode(hasTestTag("client_interest_filter_ativos"))
    composeTestRule.onNodeWithTag("client_interest_filter_ativos").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("client_proposal_card_prop_int_3").assertDoesNotExist()
  }

  @Test
  fun clientDetails_whenProposalAccepted_showsOnlyOneAcceptedAndOthersRefusedWithEmAndamento() {
    val client = User(
      id = "cli_dual_1",
      name = "Edilson Tcharlys",
      email = "cliente@example.com",
      phone = "11999990000",
      role = UserRole.CLIENTE
    )
    val order = ServiceRequest(
      id = "srv_dual_1",
      clientId = client.id,
      clientName = client.name,
      title = "Troca de Tomadas",
      category = "Eletricista",
      description = "Instalação de 2 tomadas 20A",
      location = "Centro - SP",
      proposedBudget = 10.0,
      finalAgreedPrice = 9.0,
      urgency = "Normal",
      status = ServiceStatus.EM_EXECUCAO,
      acceptedProviderId = "shared_uid_1",
      acceptedProviderName = "Edilson Neres Dos Santos",
      createdAt = 1000L
    )
    val acceptedProp = Proposal(
      id = "prop_win_1",
      serviceId = order.id,
      providerId = "shared_uid_1",
      providerName = "Edilson Neres Dos Santos",
      providerRating = 5.0,
      providerCategory = "Eletricista Residencial & Predial",
      proposedPrice = 9.0,
      status = ProposalStatus.ACEITA,
      createdAt = 2000L,
      description = "Posso atender hoje."
    )
    // Mesmo que uma proposta concorrente compartilhe o mesmo providerId ou ainda tenha status ENVIADA,
    // apenas UMA proposta pode aparecer como aceita e a outra deve aparecer como recusada.
    val competingProp = Proposal(
      id = "prop_lose_2",
      serviceId = order.id,
      providerId = "shared_uid_1",
      providerName = "Carlos Eduardo",
      providerRating = 4.9,
      providerCategory = "Eletricista Residencial & Predial",
      proposedPrice = 8.0,
      status = ProposalStatus.ENVIADA,
      createdAt = 1500L,
      description = "Atendo ainda hoje."
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        ServicesScreen(
          currentUser = client,
          services = listOf(order),
          proposals = listOf(acceptedProp, competingProp),
          selectedServiceId = order.id,
          searchQuery = "",
          categoryFilter = "Serviços",
          onSearchChange = {},
          onCategoryChange = {},
          onSelectService = {},
          onCreateServiceClick = {},
          onSubmitProposal = { _, _, _, _ -> },
          onOpenChat = {},
          onUpdateStatus = { _, _ -> },
          onRateService = { _, _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithText("Em Andamento").assertIsDisplayed()

    composeTestRule.onNodeWithTag("client_service_detail_list")
      .performScrollToNode(hasTestTag("client_proposal_card_prop_win_1"))
    composeTestRule.onNodeWithTag("btn_accepted_proposal_chat_prop_win_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("badge_provider_accepted_prop_win_1").assertIsDisplayed()

    composeTestRule.onNodeWithTag("client_service_detail_list")
      .performScrollToNode(hasTestTag("client_proposal_card_prop_lose_2"))
    composeTestRule.onNodeWithTag("badge_provider_refused_prop_lose_2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_accepted_proposal_chat_prop_lose_2").assertDoesNotExist()
    composeTestRule.onNodeWithText("Outro prestador foi escolhido para este pedido.").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `client services list shows pending review banner and completed card rate CTA opening preselected stars`() {
    val client = User(
      id = "cli_rate_1",
      name = "Mariana Cliente",
      email = "mariana@example.com",
      phone = "11999990001",
      role = UserRole.CLIENTE
    )
    val completedService = ServiceRequest(
      id = "srv_rate_list_1",
      clientId = client.id,
      clientName = client.name,
      title = "Instalação de Ventilador",
      category = "Eletricista",
      description = "Quarto principal",
      location = "Pinheiros - SP",
      proposedBudget = 150.0,
      finalAgreedPrice = 150.0,
      status = ServiceStatus.CONCLUIDO,
      acceptedProviderId = "pro_1",
      acceptedProviderName = "Roberto Mendes",
      completedAt = 5000L
    )

    var categoryFilter by mutableStateOf("Serviços")
    var ratingTarget by mutableStateOf<ServiceRequest?>(null)
    var initialStars by mutableStateOf(5.0f)

    composeTestRule.setContent {
      MyApplicationTheme {
        ServicesScreen(
          currentUser = client,
          services = listOf(completedService),
          proposals = emptyList(),
          selectedServiceId = null,
          searchQuery = "",
          categoryFilter = categoryFilter,
          onSearchChange = {},
          onCategoryChange = { categoryFilter = it },
          onSelectService = {},
          onCreateServiceClick = {},
          onSubmitProposal = { _, _, _, _ -> },
          onOpenChat = {},
          onUpdateStatus = { _, _ -> },
          onRateService = { _, _, _ -> },
          onOpenRatingDialogWithStars = { srv, stars ->
            ratingTarget = srv
            initialStars = stars
          }
        )
        ratingTarget?.let { target ->
          com.example.ui.components.ServiceRatingDialog(
            service = target,
            initialRating = initialStars,
            onDismiss = { ratingTarget = null },
            onSubmit = { _, _, _ -> ratingTarget = null }
          )
        }
      }
    }

    // Even on default "Serviços" tab, client sees the pending evaluation banner
    composeTestRule.onNodeWithTag("client_pending_reviews_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("banner_star_rate_4").performClick()
    composeTestRule.waitForIdle()

    // Dialog opens pre-selected with 4 stars
    composeTestRule.onNodeWithTag("service_rating_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithText("4 / 5", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancelar").performClick()
    composeTestRule.waitForIdle()

    // Switching categoryFilter to Concluído (via side menu) reveals the completed service card with explicit rate CTA button
    categoryFilter = "Concluído"
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("service_pending_rating_badge_srv_rate_list_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_card_rate_service_srv_rate_list_1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("service_rating_dialog").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `client service detail surfaces confirm completion CTA and top rating card above proposals`() {
    val client = User(
      id = "cli_det_rate_1",
      name = "Mariana Cliente",
      email = "mariana@example.com",
      phone = "11999990001",
      role = UserRole.CLIENTE
    )
    val runningOrder = ServiceRequest(
      id = "srv_det_rate_1",
      clientId = client.id,
      clientName = client.name,
      title = "Reparo Elétrico Completo",
      category = "Eletricista",
      description = "Troca de fiação e disjuntor",
      location = "Centro - SP",
      proposedBudget = 280.0,
      finalAgreedPrice = 280.0,
      status = ServiceStatus.EM_EXECUCAO,
      acceptedProviderId = "pro_1",
      acceptedProviderName = "Roberto Mendes"
    )
    val acceptedProp = Proposal(
      id = "prop_det_rate_1",
      serviceId = runningOrder.id,
      providerId = "pro_1",
      providerName = "Roberto Mendes",
      proposedPrice = 280.0,
      status = ProposalStatus.ACEITA
    )

    var orderState by mutableStateOf(runningOrder)
    var ratingTarget by mutableStateOf<ServiceRequest?>(null)
    var initialStars by mutableStateOf(5.0f)

    composeTestRule.setContent {
      MyApplicationTheme {
        ServicesScreen(
          currentUser = client,
          services = listOf(orderState),
          proposals = listOf(acceptedProp),
          selectedServiceId = orderState.id,
          searchQuery = "",
          categoryFilter = "Serviços",
          onSearchChange = {},
          onCategoryChange = {},
          onSelectService = {},
          onCreateServiceClick = {},
          onSubmitProposal = { _, _, _, _ -> },
          onOpenChat = {},
          onUpdateStatus = { _, _ -> },
          onRateService = { _, _, _ -> },
          onOpenRatingDialogWithStars = { srv, stars ->
            ratingTarget = srv
            initialStars = stars
          },
          onConfirmCompletedAndRate = { srv ->
            val completed = srv.copy(status = ServiceStatus.CONCLUIDO)
            orderState = completed
            ratingTarget = completed
            initialStars = 5.0f
          }
        )
        ratingTarget?.let { target ->
          com.example.ui.components.ServiceRatingDialog(
            service = target,
            initialRating = initialStars,
            onDismiss = { ratingTarget = null },
            onSubmit = { stars, comment, tags ->
              orderState = orderState.copy(
                ratingGiven = stars,
                reviewComment = comment,
                reviewTags = tags
              )
              ratingTarget = null
            }
          )
        }
      }
    }

    // Client confirms completion directly from details -> status moves to CONCLUIDO & dialog opens
    composeTestRule.onNodeWithTag("client_service_detail_list")
      .performScrollToNode(hasTestTag("btn_client_confirm_completion"))
    composeTestRule.onNodeWithTag("btn_client_confirm_completion").performClick()
    composeTestRule.waitForIdle()
    assertEquals(ServiceStatus.CONCLUIDO, orderState.status)
    composeTestRule.onNodeWithTag("service_rating_dialog").assertIsDisplayed()

    // Dismiss dialog, scroll up to confirm top-level rating card is above proposals, and tap quick star 3
    composeTestRule.onNodeWithText("Cancelar").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("client_service_detail_list")
      .performScrollToNode(hasTestTag("post_service_rating_card"))
    composeTestRule.onNodeWithTag("detail_quick_star_3").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("3 / 5", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("input_rating_comment").performScrollTo().performTextInput("Bom serviço!")
    composeTestRule.onNodeWithTag("btn_submit_rating").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("submitted_service_review_card").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `chat screen displays post completion rating banner and switches to summary after rating`() {
    val client = User(
      id = "cli_chat_rate_1",
      name = "Mariana Cliente",
      email = "mariana@example.com",
      phone = "11999990001",
      role = UserRole.CLIENTE
    )
    var chatService by mutableStateOf(
      ServiceRequest(
        id = "srv_chat_rate_1",
        clientId = client.id,
        clientName = client.name,
        title = "Troca de Tomada",
        category = "Eletricista",
        description = "Sala",
        location = "Centro - SP",
        proposedBudget = 100.0,
        finalAgreedPrice = 100.0,
        status = ServiceStatus.CONCLUIDO,
        acceptedProviderId = "pro_1",
        acceptedProviderName = "Roberto Mendes"
      )
    )
    var dialogOpen by mutableStateOf(false)

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.ChatScreen(
          currentUser = client,
          services = listOf(chatService),
          selectedServiceId = chatService.id,
          chatMessages = emptyList(),
          onSelectService = {},
          onSendMessage = { _, _ -> com.example.model.ContactFilterResult(isAllowed = true) },
          onRateServiceClick = { dialogOpen = true }
        )
        if (dialogOpen) {
          com.example.ui.components.ServiceRatingDialog(
            service = chatService,
            onDismiss = { dialogOpen = false },
            onSubmit = { stars, comment, tags ->
              chatService = chatService.copy(ratingGiven = stars, reviewComment = comment, reviewTags = tags)
              dialogOpen = false
            }
          )
        }
      }
    }

    composeTestRule.onNodeWithTag("chat_pending_rating_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_chat_rate_service").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("service_rating_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_submit_rating").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("chat_rated_summary_banner").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `side menu drawer renders categories urgency filters and admin chat monitoring entry`() {
    val adminUser = User(
      id = "admin_menu_1",
      name = "Admin ResolveAi",
      email = "admin@resolveai.com",
      phone = "61999990000",
      role = UserRole.ADMIN
    )
    var selectedCategory by mutableStateOf("Serviços")
    var selectedUrgency by mutableStateOf("Todas")
    var selectedTab by mutableStateOf(MainTab.SERVICOS)

    composeTestRule.setContent {
      MyApplicationTheme {
        AppMenuDrawerContent(
          currentUser = adminUser,
          selectedTab = selectedTab,
          pendingProposalsCount = 2,
          unreadNotificationsCount = 1,
          pendingAdminReviewsCount = 1,
          onSelectTab = { selectedTab = it },
          onNotificationsClick = {},
          onSupportClick = {},
          onAdminClick = {},
          onSwitchUserClick = {},
          onRegisterClick = {},
          onRechargeClick = {},
          onLogoutClick = {},
          onCloseDrawer = {},
          categoryFilter = selectedCategory,
          urgencyFilter = selectedUrgency,
          availableCategories = listOf("Eletricista", "Manicure"),
          onSelectCategory = { selectedCategory = it },
          onSelectUrgency = { selectedUrgency = it },
          onResetFilters = {
            selectedCategory = "Serviços"
            selectedUrgency = "Todas"
          },
          agreedChatsCount = 3
        )
      }
    }

    // Categories and Urgency chips start minimized (hidden) until their respective headers are clicked
    composeTestRule.onNodeWithTag("menu_categories_flow").assertDoesNotExist()
    composeTestRule.onNodeWithTag("menu_filter_cat_eletricista").assertDoesNotExist()
    composeTestRule.onNodeWithTag("menu_urgency_filter_flow").assertDoesNotExist()
    composeTestRule.onNodeWithTag("menu_filter_urgency_urgente").assertDoesNotExist()

    // Click "CATEGORIAS E FILTROS" -> expands only categories, urgency stays minimized
    composeTestRule.onNodeWithTag("btn_toggle_categories_filters").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("menu_filter_cat_eletricista").performScrollTo().assertIsDisplayed().performClick()
    assertEquals("Eletricista", selectedCategory)
    composeTestRule.onNodeWithTag("menu_urgency_filter_flow").assertDoesNotExist()

    // Clicking "CATEGORIAS E FILTROS" again collapses categories back
    composeTestRule.onNodeWithTag("btn_toggle_categories_filters").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("menu_categories_flow").assertDoesNotExist()

    // Click "Urgência" -> expands urgency options independently
    composeTestRule.onNodeWithTag("btn_toggle_urgency_filters").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("menu_filter_urgency_urgente").performScrollTo().assertIsDisplayed().performClick()
    assertEquals("Urgente", selectedUrgency)

    // Clicking "Urgência" again collapses urgency back
    composeTestRule.onNodeWithTag("btn_toggle_urgency_filters").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("menu_urgency_filter_flow").assertDoesNotExist()

    composeTestRule.onNodeWithTag("btn_menu_clear_filters").performScrollTo().assertIsDisplayed().performClick()
    assertEquals("Serviços", selectedCategory)
    assertEquals("Todas", selectedUrgency)

    composeTestRule.onNodeWithTag("menu_item_chat").performScrollTo().assertIsDisplayed().performClick()
    assertEquals(MainTab.CHAT, selectedTab)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `admin register user dialog exposes profile photo attachment and optional password field`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.components.RegisterUserDialog(
          onDismiss = {},
          onConfirmRegister = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("input_reg_password").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_attach_profile_photo_card").performScrollTo().assertIsDisplayed()

    // Switch to provider role: uses profile photo attachment and does not show selfie-with-document option
    composeTestRule.onNodeWithTag("register_role_prestador").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("admin_attach_profile_photo_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("dlg_attach_rg_cnh").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("dlg_attach_criminal").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("dlg_attach_selfie").assertDoesNotExist()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `admin users screen opens change password dialog validates match and submits new password`() {
    val adminUser = User(
      id = "admin_pwd_1",
      name = "Admin Master",
      email = "admin@resolveai.com",
      phone = "61999990000",
      role = UserRole.ADMIN
    )
    val clientUser = User(
      id = "cli_target_1",
      name = "Carlos Cliente",
      email = "carlos@resolveai.com",
      phone = "11988887777",
      role = UserRole.CLIENTE
    )
    var capturedUserId: String? = null
    var capturedNewPassword: String? = null

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.AdminUsersScreen(
          currentUser = adminUser,
          users = listOf(adminUser, clientUser),
          isLoading = false,
          syncStatusMessage = null,
          onSyncUsersClick = {},
          onUpdateUser = { Result.success(Unit) },
          onChangeUserPassword = { userId, newPassword ->
            capturedUserId = userId
            capturedNewPassword = newPassword
            Result.success(Unit)
          },
          onDeleteUser = { Result.success(Unit) },
          onOpenMenu = {},
          onBackToHome = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("admin_users_list")
      .performScrollToNode(hasTestTag("btn_admin_change_password_cli_target_1"))
    composeTestRule.onNodeWithTag("btn_admin_change_password_cli_target_1").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("admin_change_password_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_admin_send_password_reset").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_admin_confirm_change_password").assertIsNotEnabled()

    composeTestRule.onNodeWithTag("input_admin_new_password").performTextInput("NovaSenha2026")
    composeTestRule.onNodeWithTag("input_admin_confirm_password").performTextInput("Diferente")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("btn_admin_confirm_change_password").assertIsNotEnabled()

    composeTestRule.onNodeWithTag("input_admin_confirm_password").performTextReplacement("NovaSenha2026")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("btn_admin_confirm_change_password").assertIsEnabled().performClick()
    composeTestRule.waitForIdle()

    assertEquals("cli_target_1", capturedUserId)
    assertEquals("NovaSenha2026", capturedNewPassword)
    composeTestRule.onNodeWithTag("admin_change_password_dialog").assertDoesNotExist()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `admin read only chat monitor displays client provider messages and hides send controls`() {
    val adminUser = User(
      id = "admin_chat_1",
      name = "Admin ResolveAi",
      email = "admin@resolveai.com",
      phone = "61999990000",
      role = UserRole.ADMIN
    )
    val agreedService = ServiceRequest(
      id = "srv_agreed_mon_1",
      clientId = "cli_1",
      clientName = "Carlos Cliente",
      title = "Instalação de Ar-Condicionado",
      category = "Refrigeração",
      description = "Aparelho 12000 BTUs",
      location = "Centro - SP",
      proposedBudget = 400.0,
      finalAgreedPrice = 380.0,
      status = ServiceStatus.EM_EXECUCAO,
      acceptedProviderId = "pro_1",
      acceptedProviderName = "Roberto Mendes"
    )
    val msgs = listOf(
      com.example.model.ChatMessage(
        id = "m_cli_1",
        serviceId = agreedService.id,
        senderId = "cli_1",
        senderName = "Carlos Cliente",
        senderRole = UserRole.CLIENTE,
        content = "Bom dia Roberto, pode vir às 14h?"
      ),
      com.example.model.ChatMessage(
        id = "m_pro_1",
        serviceId = agreedService.id,
        senderId = "pro_1",
        senderName = "Roberto Mendes",
        senderRole = UserRole.PRESTADOR,
        content = "Combinado Carlos, estarei aí pontualmente às 14h."
      )
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.ChatScreen(
          currentUser = adminUser,
          services = listOf(agreedService),
          selectedServiceId = agreedService.id,
          chatMessages = msgs,
          onSelectService = {},
          onSendMessage = { _, _ -> com.example.model.ContactFilterResult(isAllowed = false) }
        )
      }
    }

    composeTestRule.onNodeWithTag("admin_chat_monitor_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_chat_service_chip_srv_agreed_mon_1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Bom dia Roberto, pode vir às 14h?").assertIsDisplayed()
    composeTestRule.onNodeWithText("Combinado Carlos, estarei aí pontualmente às 14h.").assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_chat_readonly_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chat_input_field").assertDoesNotExist()
    composeTestRule.onNodeWithTag("chat_send_button").assertDoesNotExist()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `admin can open service conversation from service detail and catalog screen`() {
    val adminUser = User(
      id = "admin_srv_chat_1",
      name = "Admin ResolveAi",
      email = "admin@resolveai.com",
      phone = "61999990000",
      role = UserRole.ADMIN
    )
    val agreedService = ServiceRequest(
      id = "srv_admin_open_1",
      clientId = "cli_1",
      clientName = "Carlos Cliente",
      title = "Reforma Elétrica Completa",
      category = "Eletricista",
      description = "Troca de fiação",
      location = "Centro - SP",
      proposedBudget = 500.0,
      finalAgreedPrice = 480.0,
      status = ServiceStatus.EM_EXECUCAO,
      acceptedProviderId = "pro_1",
      acceptedProviderName = "Roberto Mendes"
    )
    var openedChatServiceId: String? = null

    composeTestRule.setContent {
      MyApplicationTheme {
        ServicesScreen(
          currentUser = adminUser,
          services = listOf(agreedService),
          proposals = emptyList(),
          selectedServiceId = agreedService.id,
          searchQuery = "",
          categoryFilter = "Serviços",
          onSearchChange = {},
          onCategoryChange = {},
          onSelectService = {},
          onCreateServiceClick = {},
          onSubmitProposal = { _, _, _, _ -> },
          onOpenChat = { openedChatServiceId = it },
          onUpdateStatus = { _, _ -> },
          onRateService = { _, _, _ -> },
          chatMessages = listOf(
            com.example.model.ChatMessage(
              id = "msg_preview_1",
              serviceId = agreedService.id,
              senderId = "cli_1",
              senderName = "Carlos Cliente",
              senderRole = UserRole.CLIENTE,
              content = "Tudo certo para iniciar?"
            )
          )
        )
      }
    }

    composeTestRule.onNodeWithTag("client_service_detail_list")
      .performScrollToNode(hasTestTag("admin_service_chat_monitor_card"))
    composeTestRule.onNodeWithTag("admin_service_chat_monitor_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_admin_open_service_chat_srv_admin_open_1").performClick()
    assertEquals("srv_admin_open_1", openedChatServiceId)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `all user roles see change password option in menu drawer and profile screen`() {
    val roles = listOf(
      User(id = "u_cli", name = "Ana Cliente", email = "ana@cli.com", phone = "11999990001", role = UserRole.CLIENTE),
      User(id = "u_pro", name = "João Prestador", email = "joao@pro.com", phone = "11999990002", role = UserRole.PRESTADOR, category = "Eletricista"),
      User(id = "u_adm", name = "Admin Sistema", email = "adm@sys.com", phone = "11999990003", role = UserRole.ADMIN)
    )
    var roleIndex by mutableStateOf(0)
    var showProfile by mutableStateOf(false)
    var drawerClicks = 0
    var profileClicks = 0

    composeTestRule.setContent {
      MyApplicationTheme {
        val activeUser = roles[roleIndex]
        if (!showProfile) {
          AppMenuDrawerContent(
            currentUser = activeUser,
            selectedTab = MainTab.SERVICOS,
            pendingProposalsCount = 0,
            unreadNotificationsCount = 0,
            onSelectTab = {},
            onNotificationsClick = {},
            onSupportClick = {},
            onAdminClick = {},
            onSwitchUserClick = {},
            onRegisterClick = {},
            onRechargeClick = {},
            onChangePasswordClick = { drawerClicks++ },
            onCloseDrawer = {}
          )
        } else {
          WalletProfileScreen(
            currentUser = activeUser,
            walletTransactions = emptyList(),
            onRechargeClick = {},
            onSwitchUserClick = {},
            onEditProfileClick = {},
            onSaveProfile = {},
            onChangePasswordClick = { profileClicks++ }
          )
        }
      }
    }

    // Check menu_item_change_password for Cliente, Prestador, and Admin
    roles.indices.forEach { idx ->
      roleIndex = idx
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("menu_item_change_password").performScrollTo().assertIsDisplayed().performClick()
      composeTestRule.waitForIdle()
    }
    assertEquals(3, drawerClicks)

    // Check btn_profile_change_password on Profile screen for Cliente and Prestador
    showProfile = true
    listOf(0, 1).forEach { idx ->
      roleIndex = idx
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("wallet_profile_list")
        .performScrollToNode(hasTestTag("btn_profile_change_password"))
      composeTestRule.onNodeWithTag("btn_profile_change_password").assertIsDisplayed().performClick()
      composeTestRule.waitForIdle()
    }
    assertEquals(2, profileClicks)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `change own password dialog validates current password new password and confirmation`() {
    val user = User(
      id = "cli_pwd_1",
      name = "Lucas Cliente",
      email = "lucas@resolveai.com",
      phone = "(11) 98888-7777",
      role = UserRole.CLIENTE
    )
    var submittedTriple: Triple<String, String, String>? = null
    var dialogState by mutableStateOf<com.example.ui.ChangePasswordUiState>(com.example.ui.ChangePasswordUiState.Idle)

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.components.ChangeOwnPasswordDialog(
          currentUser = user,
          state = dialogState,
          onDismiss = {},
          onClearState = { dialogState = com.example.ui.ChangePasswordUiState.Idle },
          onConfirmChangePassword = { currentPwd, newPwd, confirmPwd ->
            submittedTriple = Triple(currentPwd, newPwd, confirmPwd)
          }
        )
      }
    }

    composeTestRule.onNodeWithTag("change_own_password_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_confirm_change_own_password").assertIsNotEnabled()

    // Same password as current -> still disabled and shows error hint
    composeTestRule.onNodeWithTag("input_current_password").performTextInput("senha123")
    composeTestRule.onNodeWithTag("input_new_password").performTextInput("senha123")
    composeTestRule.onNodeWithTag("input_confirm_new_password").performTextInput("senha123")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("A nova senha deve ser diferente da senha atual.").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_confirm_change_own_password").assertIsNotEnabled()

    // Mismatch confirmation -> disabled and shows mismatch hint
    composeTestRule.onNodeWithTag("input_new_password").performTextReplacement("novaSenha9")
    composeTestRule.onNodeWithTag("input_confirm_new_password").performTextReplacement("outraSenha9")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("As senhas informadas não coincidem.").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_confirm_change_own_password").assertIsNotEnabled()

    // Remote wrong-current-password error displays error banner
    dialogState = com.example.ui.ChangePasswordUiState.Error("Senha atual incorreta. Verifique e tente novamente.")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("change_own_password_error").assertIsDisplayed()

    // Matching confirmation enables submit and invokes callback with all three fields
    composeTestRule.onNodeWithTag("input_confirm_new_password").performTextReplacement("novaSenha9")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("btn_confirm_change_own_password").assertIsEnabled().performClick()
    composeTestRule.waitForIdle()

    assertEquals(Triple("senha123", "novaSenha9", "novaSenha9"), submittedTriple)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `admin users screen masks password by default reveals on toggle and shows fallback when empty`() {
    val adminUser = User(
      id = "adm_pwd_view",
      name = "Admin ResolveAi",
      email = "admin@resolveai.com",
      phone = "(61) 99290-7464",
      role = UserRole.ADMIN
    )
    val clientWithPwd = User(
      id = "cli_with_pwd",
      name = "Fernanda Cliente",
      email = "fernanda@resolveai.com",
      phone = "(11) 98888-1111",
      role = UserRole.CLIENTE
    )
    val clientNoPwd = User(
      id = "cli_no_pwd",
      name = "Pedro Google",
      email = "pedro@resolveai.com",
      phone = "(11) 98888-2222",
      role = UserRole.CLIENTE
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.AdminUsersScreen(
          currentUser = adminUser,
          users = listOf(adminUser, clientWithPwd, clientNoPwd),
          isLoading = false,
          syncStatusMessage = null,
          onSyncUsersClick = {},
          onUpdateUser = { Result.success(Unit) },
          onDeleteUser = { Result.success(Unit) },
          userPasswords = mapOf("cli_with_pwd" to "Segredo#2026"),
          onOpenMenu = {},
          onBackToHome = {}
        )
      }
    }

    // Scroll to clientWithPwd card and verify password is masked by default
    composeTestRule.onNodeWithTag("admin_users_list")
      .performScrollToNode(hasTestTag("admin_user_card_cli_with_pwd"))
    composeTestRule.onNodeWithTag("admin_user_password_row_cli_with_pwd").assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_user_password_value_cli_with_pwd").assertIsDisplayed()
    composeTestRule.onNodeWithText("••••••••").assertIsDisplayed()
    composeTestRule.onNodeWithText("Segredo#2026").assertDoesNotExist()

    // Tap eye toggle to reveal password
    composeTestRule.onNodeWithTag("btn_admin_toggle_password_cli_with_pwd").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Segredo#2026").assertIsDisplayed()

    // Tap eye toggle again to hide password
    composeTestRule.onNodeWithTag("btn_admin_toggle_password_cli_with_pwd").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Segredo#2026").assertDoesNotExist()
    composeTestRule.onNodeWithText("••••••••").assertIsDisplayed()

    // Scroll to clientNoPwd card and verify fallback message is displayed
    composeTestRule.onNodeWithTag("admin_users_list")
      .performScrollToNode(hasTestTag("admin_user_card_cli_no_pwd"))
    composeTestRule.onNodeWithTag("admin_user_password_empty_cli_no_pwd").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_admin_toggle_password_cli_no_pwd").assertDoesNotExist()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `drawer displays firebase sync status card and triggers manual sync`() {
    val clientUser = User(
      id = "cli_sync_1",
      name = "Carla Online",
      email = "carla@resolveai.com",
      phone = "(11) 97777-3333",
      role = UserRole.CLIENTE
    )
    var syncClicks = 0

    composeTestRule.setContent {
      MyApplicationTheme {
        AppMenuDrawerContent(
          currentUser = clientUser,
          selectedTab = MainTab.SERVICOS,
          pendingProposalsCount = 0,
          unreadNotificationsCount = 0,
          onSelectTab = {},
          onNotificationsClick = {},
          onSupportClick = {},
          onAdminClick = {},
          onSwitchUserClick = {},
          onRegisterClick = {},
          onRechargeClick = {},
          onCloseDrawer = {},
          cloudSyncStatus = com.example.data.CloudSyncStatus(
            state = com.example.data.CloudSyncState.ONLINE_SYNCED,
            isFirebaseSignedIn = true,
            activeListenersCount = 5,
            statusMessage = "Conectado online (5 canais em tempo real)"
          ),
          onSyncFirebaseClick = { syncClicks++ }
        )
      }
    }

    composeTestRule.onNodeWithTag("drawer_firebase_sync_status").assertIsDisplayed()
    composeTestRule.onNodeWithText("Firebase Online • Sincronizado").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_drawer_sync_firebase").assertIsEnabled().performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, syncClicks)
  }

  @Test
  fun `pending auth return state survives recreation via SharedPreferences`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.DocumentAttachmentHelper.clearPendingAuthReturn(context)

    com.example.data.DocumentAttachmentHelper.savePendingAuthReturn(
      context = context,
      modeName = AuthMode.REGISTER.name,
      stepName = AuthFlowStep.FORM.name,
      roleName = UserRole.PRESTADOR.name,
      pendingPhone = "(11) 98888-7777"
    )

    val restored = com.example.data.DocumentAttachmentHelper.peekPendingAuthReturn(context)
    assertNotNull(restored)
    assertEquals(AuthMode.REGISTER.name, restored?.modeName)
    assertEquals(AuthFlowStep.FORM.name, restored?.stepName)
    assertEquals(UserRole.PRESTADOR.name, restored?.roleName)
    assertEquals("(11) 98888-7777", restored?.pendingPhone)

    com.example.data.DocumentAttachmentHelper.clearPendingAuthReturn(context)
    assertEquals(null, com.example.data.DocumentAttachmentHelper.peekPendingAuthReturn(context))
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `create service dialog auto fills daily regional category average and restores after manual edit`() {
    val expectedEletricista = com.example.data.RegionalPriceIndexEngine.estimate(
      category = "Eletricista",
      locationText = "",
      fallbackCity = "São Paulo, SP"
    )
    val expectedManicure = com.example.data.RegionalPriceIndexEngine.estimate(
      category = "Manicure",
      locationText = "",
      fallbackCity = "São Paulo, SP"
    )
    var submittedBudget = 0.0

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.components.CreateServiceDialog(
          onDismiss = {},
          onCreate = { _, _, _, _, budget, _, _, _ ->
            submittedBudget = budget
          },
          availableCategories = listOf("Eletricista", "Manicure", "Pintura & Acabamento"),
          defaultCity = "São Paulo, SP"
        )
      }
    }

    // 1. Shows daily regional benchmark card with region & today's date, and auto-fills budget field
    composeTestRule.onNodeWithTag("regional_price_benchmark_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("regional_price_daily_badge", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("regional_price_region_label", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("regional_price_average_value", useUnmergedTree = true)
      .assertTextContains(com.example.data.RegionalPriceIndexEngine.formatCurrencyBrl(expectedEletricista.averagePrice))
    composeTestRule.onNodeWithTag("input_service_budget")
      .assertTextContains(com.example.data.RegionalPriceIndexEngine.formatEditableDecimal(expectedEletricista.averagePrice))

    // 2. Switching category updates the regional average & auto-filled budget automatically
    composeTestRule.onNodeWithTag("dropdown_service_category").performScrollTo().performClick()
    composeTestRule.onNodeWithText("Manicure").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("regional_price_average_value", useUnmergedTree = true)
      .assertTextContains(com.example.data.RegionalPriceIndexEngine.formatCurrencyBrl(expectedManicure.averagePrice))
    composeTestRule.onNodeWithTag("input_service_budget")
      .assertTextContains(com.example.data.RegionalPriceIndexEngine.formatEditableDecimal(expectedManicure.averagePrice))

    // 3. Clearing and entering a custom value reveals "Usar média" button; tapping it restores regional average
    composeTestRule.onNodeWithTag("btn_clear_service_budget", useUnmergedTree = true).performClick()
    composeTestRule.onNodeWithTag("input_service_budget").performTextInput("50")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("btn_apply_regional_price").performScrollTo().assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // 4. Publishing submits the measured regional daily average cleanly
    composeTestRule.onNodeWithTag("input_service_title").performScrollTo().performTextInput("Manicure em domicílio")
    composeTestRule.onNodeWithTag("btn_submit_create_service").performClick()
    composeTestRule.waitForIdle()

    assertEquals(expectedManicure.averagePrice, submittedBudget, 0.01)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `clicking profile photo navigates to profile data and opens edit profile dialog`() {
    val testUser = User(
      id = "u_profile_nav",
      name = "Carlos Pereira",
      email = "carlos@example.com",
      phone = "11988887777",
      city = "São Paulo, SP",
      role = UserRole.CLIENTE
    )
    var openProfileFromDrawerCount = 0
    var drawerClosedCount = 0
    var savedUser: User? = null
    var showProfileScreen by mutableStateOf(false)

    composeTestRule.setContent {
      MyApplicationTheme {
        if (!showProfileScreen) {
          AppMenuDrawerContent(
            currentUser = testUser,
            selectedTab = MainTab.SERVICOS,
            pendingProposalsCount = 0,
            unreadNotificationsCount = 0,
            onSelectTab = {},
            onNotificationsClick = {},
            onSupportClick = {},
            onAdminClick = {},
            onSwitchUserClick = {},
            onRegisterClick = {},
            onRechargeClick = {},
            onCloseDrawer = { drawerClosedCount++ },
            onOpenProfileClick = {
              openProfileFromDrawerCount++
              showProfileScreen = true
            }
          )
        } else {
          com.example.ui.screens.WalletProfileScreen(
            currentUser = savedUser ?: testUser,
            walletTransactions = emptyList(),
            onRechargeClick = {},
            onSwitchUserClick = {},
            onEditProfileClick = {},
            onSaveProfile = { savedUser = it },
            profileFocusNonce = openProfileFromDrawerCount
          )
        }
      }
    }

    // 1. Clicking the profile photo in the side menu opens the user's profile data screen
    composeTestRule.onNodeWithTag("drawer_profile_avatar").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, openProfileFromDrawerCount)
    assertEquals(1, drawerClosedCount)

    // 2. Profile data card and Edit Profile CTA are visible
    composeTestRule.onNodeWithTag("user_profile_data_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("btn_profile_edit_data").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // 3. Editing the name in EditProfileDialog and saving updates the user data
    composeTestRule.onNodeWithTag("input_edit_profile_name").assertIsDisplayed()
    composeTestRule.onNodeWithTag("input_edit_profile_name").performTextReplacement("Carlos Silva")
    composeTestRule.onNodeWithTag("btn_save_edit_profile").performClick()
    composeTestRule.waitForIdle()

    assertEquals("Carlos Silva", savedUser?.name)

    // 4. Clicking the profile photo inside Profile screen also exposes "Editar Dados do Perfil"
    composeTestRule.onNodeWithTag("btn_change_profile_photo").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("btn_profile_photo_edit_data").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `client sees all provider proposals on their service request including direct acceptances and linked device ids`() {
    val clientUser = User(
      id = "cli_primary_uid",
      firebaseUid = "fb_cli_device2_uid",
      linkedAuthUids = listOf("cli_primary_uid", "fb_cli_device2_uid"),
      name = "Ana Beatriz",
      email = "ana@example.com",
      phone = "11988881111",
      city = "São Paulo, SP",
      role = UserRole.CLIENTE
    )
    val service = ServiceRequest(
      id = "srv_all_props_1",
      clientId = "cli_primary_uid",
      clientName = "Ana Beatriz",
      title = "Instalação de ar-condicionado split",
      category = "Ar Condicionado",
      description = "Instalação completa em sala comercial com tubulação de 3 metros.",
      location = "Moema, São Paulo, SP",
      proposedBudget = 450.0,
      status = ServiceStatus.EM_NEGOCIACAO,
      urgency = "Hoje"
    )
    val allProposals = listOf(
      Proposal(
        id = "prop_direct_1",
        serviceId = service.id,
        providerId = "pro_uid_1",
        providerName = "Carlos Clima",
        providerRating = 4.9,
        providerCategory = "Ar Condicionado, Eletricista",
        proposedPrice = 450.0,
        estimatedDays = 1,
        description = "Aceito o valor proposto pelo cliente e posso atender hoje.",
        status = ProposalStatus.ENVIADA,
        isDirectAcceptance = true,
        createdAt = 1_760_000_003_000L
      ),
      Proposal(
        id = "prop_custom_2",
        serviceId = service.id,
        providerId = "pro_uid_2",
        providerName = "Marcos Refrigeração",
        providerRating = 4.7,
        providerCategory = "Ar Condicionado, Hidráulica, Elétrica, Manutenção Predial, Automação Residencial, Reformas Gerais",
        proposedPrice = 420.0,
        estimatedDays = 1,
        description = "Levo suporte e tubulação inclusos no valor.",
        status = ProposalStatus.ENVIADA,
        isDirectAcceptance = false,
        createdAt = 1_760_000_002_000L
      ),
      Proposal(
        id = "prop_counter_3",
        serviceId = service.id,
        providerId = "pro_uid_3",
        providerName = "Fernanda Técnica",
        providerRating = 4.8,
        providerCategory = "Ar Condicionado",
        proposedPrice = 480.0,
        estimatedDays = 2,
        description = "Inclui higienização e garantia de 6 meses.",
        status = ProposalStatus.CONTRAPROPOSTA_PRESTADOR,
        isDirectAcceptance = false,
        createdAt = 1_760_000_001_000L
      )
    )

    var selectedServiceId by mutableStateOf<String?>(service.id)

    composeTestRule.setContent {
      MyApplicationTheme {
        ServicesScreen(
          currentUser = clientUser,
          services = listOf(service),
          proposals = allProposals,
          selectedServiceId = selectedServiceId,
          searchQuery = "",
          categoryFilter = "Todas",
          onSearchChange = {},
          onCategoryChange = {},
          onSelectService = { selectedServiceId = it },
          onCreateServiceClick = {},
          onSubmitProposal = { _, _, _, _ -> },
          onOpenChat = {},
          onUpdateStatus = { _, _ -> },
          onRateService = { _, _, _ -> }
        )
      }
    }

    // 1. Summary banner and section header reflect all 3 interested providers
    composeTestRule.onNodeWithTag("client_interested_summary_banner").assertIsDisplayed()
    composeTestRule.onNodeWithText("3 prestador(es) interessado(s) aguardando sua escolha").assertIsDisplayed()
    composeTestRule.onNodeWithTag("client_service_detail_list")
      .performScrollToNode(hasTestTag("client_interested_providers_section"))
    composeTestRule.onNodeWithText("Prestadores Interessados (3)").assertIsDisplayed()

    // 2. All 3 provider proposal cards render inside the order detail view
    allProposals.forEach { prop ->
      composeTestRule.onNodeWithTag("client_service_detail_list")
        .performScrollToNode(hasTestTag("client_proposal_card_${prop.id}"))
      composeTestRule.onNodeWithTag("client_proposal_card_${prop.id}").assertIsDisplayed()
      composeTestRule.onNodeWithText(prop.providerName).assertIsDisplayed()
    }
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `firestore chat diagnostic logger classifies permission and timeout errors and renders in chat diagnostics dialog`() {
    com.example.data.ChatDiagnosticLogger.clearLogs()

    // 1. Verify classification of Firestore permission errors & network timeout triggers
    val permEx = com.google.firebase.firestore.FirebaseFirestoreException(
      "Missing or insufficient permissions.",
      com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED
    )
    val permClass = com.example.data.ChatDiagnosticLogger.classifyFirestoreFailure(permEx)
    assertEquals(com.example.data.ChatFailureKind.PERMISSION_DENIED, permClass.kind)
    assertEquals("PERMISSION_DENIED", permClass.firestoreCode)

    val deadlineEx = com.google.firebase.firestore.FirebaseFirestoreException(
      "Deadline exceeded",
      com.google.firebase.firestore.FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
    )
    val deadlineClass = com.example.data.ChatDiagnosticLogger.classifyFirestoreFailure(deadlineEx)
    assertEquals(com.example.data.ChatFailureKind.NETWORK_TIMEOUT, deadlineClass.kind)
    assertEquals("DEADLINE_EXCEEDED", deadlineClass.firestoreCode)

    val unavailableEx = com.google.firebase.firestore.FirebaseFirestoreException(
      "Unavailable",
      com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE
    )
    val unavailableClass = com.example.data.ChatDiagnosticLogger.classifyFirestoreFailure(unavailableEx)
    assertEquals(com.example.data.ChatFailureKind.NETWORK_TIMEOUT, unavailableClass.kind)
    assertEquals("UNAVAILABLE", unavailableClass.firestoreCode)

    val socketTimeoutClass = com.example.data.ChatDiagnosticLogger.classifyFirestoreFailure(
      java.net.SocketTimeoutException("Read timed out")
    )
    assertEquals(com.example.data.ChatFailureKind.NETWORK_TIMEOUT, socketTimeoutClass.kind)
    assertEquals("SOCKET_TIMEOUT", socketTimeoutClass.firestoreCode)

    // 2. Record both a permission error and a listener network timeout for an active service chat
    val srvId = "srv_diag_chat_1"
    val permEntry = com.example.data.ChatDiagnosticLogger.record(
      serviceId = srvId,
      category = com.example.data.ChatDiagnosticCategory.SEND_PERMISSION_DENIED,
      severity = com.example.data.ChatDiagnosticSeverity.ERROR,
      firestoreCode = "PERMISSION_DENIED",
      summary = "PERMISSION_DENIED nas regras do Firestore ao enviar mensagem (tentativa #1).",
      details = mapOf("path" to "services/$srvId/messages/msg_1")
    )
    val timeoutEntry = com.example.data.ChatDiagnosticLogger.record(
      serviceId = srvId,
      category = com.example.data.ChatDiagnosticCategory.LISTENER_NETWORK_TIMEOUT,
      severity = com.example.data.ChatDiagnosticSeverity.WARNING,
      firestoreCode = "SERVER_SYNC_TIMEOUT_8S",
      summary = "Listener sem resposta do servidor após 8000ms.",
      details = mapOf("path" to "services/$srvId/messages")
    )

    val report = com.example.data.ChatDiagnosticLogger.exportReport(srvId)
    assertTrue(report.contains("PERMISSION_DENIED"))
    assertTrue(report.contains("SERVER_SYNC_TIMEOUT_8S"))

    val clientUser = User(
      id = "cli_diag_1",
      name = "Ana Cliente",
      email = "ana@example.com",
      phone = "11999990001",
      role = UserRole.CLIENTE
    )
    val activeService = ServiceRequest(
      id = srvId,
      clientId = "cli_diag_1",
      clientName = "Ana Cliente",
      title = "Reparo elétrico urgente",
      category = "Eletricista",
      description = "Troca de disjuntor",
      location = "São Paulo, SP",
      proposedBudget = 250.0,
      status = ServiceStatus.EM_EXECUCAO,
      acceptedProviderId = "pro_diag_1",
      acceptedProviderName = "Carlos Eletricista"
    )
    var logsState by mutableStateOf(com.example.data.ChatDiagnosticLogger.logs.value)

    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.ChatScreen(
          currentUser = clientUser,
          services = listOf(activeService),
          selectedServiceId = srvId,
          chatMessages = emptyList(),
          onSelectService = {},
          onSendMessage = { _, _ -> com.example.model.ContactFilterResult(isAllowed = true) },
          chatDiagnosticLogs = logsState,
          onClearChatDiagnostics = {
            com.example.data.ChatDiagnosticLogger.clearLogs()
            logsState = emptyList()
          },
          onExportChatDiagnostics = { sid -> com.example.data.ChatDiagnosticLogger.exportReport(sid) }
        )
      }
    }

    // 3. Open diagnostics dialog from ChatScreen top bar
    composeTestRule.onNodeWithTag("btn_chat_diagnostics").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("chat_diagnostics_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chat_diag_health_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chat_diag_item_${permEntry.id}").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chat_diag_item_${timeoutEntry.id}").assertIsDisplayed()

    // 4. Filter only Timeout/Rede -> only timeout entry shown
    composeTestRule.onNodeWithTag("diag_filter_timeout").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("chat_diag_item_${timeoutEntry.id}").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chat_diag_item_${permEntry.id}").assertDoesNotExist()

    // 5. Filter only Permissão -> only permission entry shown
    composeTestRule.onNodeWithTag("diag_filter_permission").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("chat_diag_item_${permEntry.id}").assertIsDisplayed()
    composeTestRule.onNodeWithTag("chat_diag_item_${timeoutEntry.id}").assertDoesNotExist()

    // 6. Clear logs empties the dialog
    composeTestRule.onNodeWithTag("btn_clear_chat_diagnostics").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("chat_diag_empty_state").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w412dp-h915dp")
  fun `create service dialog automatically pulls local city regional average instead of Sao Paulo`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    CurrentCityLocationHelper.clearCachedLocalCity(app)
    Shadows.shadowOf(app).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)

    try {
      val expectedSalvadorEletricista = com.example.data.RegionalPriceIndexEngine.estimate(
        category = "Eletricista",
        locationText = "Salvador, BA",
        fallbackCity = "São Paulo, SP"
      )
      val expectedSaoPauloEletricista = com.example.data.RegionalPriceIndexEngine.estimate(
        category = "Eletricista",
        locationText = "",
        fallbackCity = "São Paulo, SP"
      )
      assertTrue(
        "Salvador regional average should differ from São Paulo",
        expectedSalvadorEletricista.averagePrice != expectedSaoPauloEletricista.averagePrice
      )

      val expectedSalvadorManicure = com.example.data.RegionalPriceIndexEngine.estimate(
        category = "Manicure",
        locationText = "Salvador, BA",
        fallbackCity = "São Paulo, SP"
      )

      var detectedCityCallback = ""
      var submittedBudget = 0.0
      var submittedLocation = ""

      composeTestRule.setContent {
        MyApplicationTheme {
          com.example.ui.components.CreateServiceDialog(
            onDismiss = {},
            onCreate = { _, _, _, loc, budget, _, _, _ ->
              submittedLocation = loc
              submittedBudget = budget
            },
            availableCategories = listOf("Eletricista", "Manicure", "Pintura & Acabamento"),
            defaultCity = "São Paulo, SP",
            onLocalCityDetected = { detectedCityCallback = it },
            detectLocalCityProvider = {
              CurrentCityResult.Success(
                cityFormatted = "Salvador, BA",
                latitude = -12.9714,
                longitude = -38.5014
              )
            }
          )
        }
      }

      composeTestRule.waitForIdle()

      // 1. Automatically detected Salvador, BA instead of defaulting to São Paulo, SP
      assertEquals("Salvador, BA", detectedCityCallback)
      composeTestRule.onNodeWithTag("regional_price_region_label", useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
      composeTestRule.onNodeWithText("Salvador - BA", substring = true)
        .assertIsDisplayed()
      composeTestRule.onNodeWithTag("regional_price_average_value", useUnmergedTree = true)
        .assertTextContains(
          com.example.data.RegionalPriceIndexEngine.formatCurrencyBrl(expectedSalvadorEletricista.averagePrice)
        )
      composeTestRule.onNodeWithTag("input_service_budget")
        .assertTextContains(
          com.example.data.RegionalPriceIndexEngine.formatEditableDecimal(expectedSalvadorEletricista.averagePrice)
        )

      // 2. Switching category keeps using local city (Salvador, BA) automatically
      composeTestRule.onNodeWithTag("dropdown_service_category").performScrollTo().performClick()
      composeTestRule.onNodeWithText("Manicure").performClick()
      composeTestRule.waitForIdle()

      composeTestRule.onNodeWithTag("regional_price_average_value", useUnmergedTree = true)
        .assertTextContains(
          com.example.data.RegionalPriceIndexEngine.formatCurrencyBrl(expectedSalvadorManicure.averagePrice)
        )

      // 3. Submitting without manually typing an address preserves detected local city & its regional average
      composeTestRule.onNodeWithTag("input_service_title").performScrollTo().performTextInput("Manicure em casa")
      composeTestRule.onNodeWithTag("btn_submit_create_service").performClick()
      composeTestRule.waitForIdle()

      assertEquals("Salvador, BA", submittedLocation)
      assertEquals(expectedSalvadorManicure.averagePrice, submittedBudget, 0.01)
    } finally {
      CurrentCityLocationHelper.clearCachedLocalCity(app)
    }
  }

  @Test
  fun `regional price index engine resolves non-capital cities with full state names without Sao Paulo collision`() {
    val campinas = com.example.data.RegionalPriceIndexEngine.resolveRegion("Campinas, São Paulo")
    assertEquals("Campinas - SP", campinas.regionLabel)
    assertEquals("SP", campinas.stateCode)

    val mogi = com.example.data.RegionalPriceIndexEngine.resolveRegion("Mogi das Cruzes, São Paulo")
    assertEquals("Mogi das Cruzes - SP", mogi.regionLabel)
    assertEquals("SP", mogi.stateCode)

    val niteroi = com.example.data.RegionalPriceIndexEngine.resolveRegion("Niterói, Rio de Janeiro")
    assertEquals("Niterói - RJ", niteroi.regionLabel)
    assertEquals("RJ", niteroi.stateCode)

    val bh = com.example.data.RegionalPriceIndexEngine.resolveRegion("Belo Horizonte, MG")
    assertEquals("Belo Horizonte - MG", bh.regionLabel)
    assertEquals("MG", bh.stateCode)
  }
}




