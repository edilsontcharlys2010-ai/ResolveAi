package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ContactValidator
import com.example.data.CpfValidator
import com.example.data.DocumentAttachmentHelper
import com.example.data.FirebaseAuthHelper
import com.example.data.FirebasePhoneAuthManager
import com.example.data.GoogleAccountInfo
import com.example.data.LocationAutocompleteMode
import com.example.model.UserRole
import com.example.ui.AuthFlowStep
import com.example.ui.AuthMode
import com.example.ui.PasswordResetUiState
import com.example.ui.PendingRegistration
import com.example.ui.components.ClientProfilePhotoPickerCard
import com.example.ui.components.GooglePlacesAutocompleteField
import com.example.ui.components.ProviderCameraProfilePhotoCard
import com.example.ui.components.ProviderDocumentAttachmentCard
import com.example.ui.components.rememberCameraPhotoCaptureLauncher
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryVariant
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedLight
import com.example.ui.theme.EmeraldWallet
import com.example.ui.theme.EmeraldWalletLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class CredentialLoginMethod {
  EMAIL_PASSWORD,
  PHONE_PASSWORD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
  authMode: AuthMode,
  authStep: AuthFlowStep,
  pendingGoogleAccount: GoogleAccountInfo?,
  socialProviderLabel: String = "Google",
  prefillCompletionPhone: String = "",
  preferredGoogleRole: UserRole = UserRole.CLIENTE,
  pendingGoogleDraft: PendingRegistration? = null,
  authErrorMessage: String?,
  authSuccessMessage: String? = null,
  isGoogleAuthLoading: Boolean = false,
  isCredentialAuthLoading: Boolean = false,
  passwordResetState: PasswordResetUiState = PasswordResetUiState.Idle,
  onSelectMode: (AuthMode) -> Unit,
  onClearError: () -> Unit,
  onEmailPasswordSignIn: (email: String, password: String) -> Unit = { _, _ -> },
  onPhonePasswordSignIn: (phone: String, countryCode: String, password: String) -> Unit = { _, _, _ -> },
  onSendPasswordReset: (email: String) -> Unit = {},
  onResetPasswordResetState: () -> Unit = {},
  onRegisterWithPassword: (PendingRegistration, countryCode: String, password: String) -> Unit = { _, _, _ -> },
  onGoogleSignInClick: (preferredRoleFromRegisterTab: UserRole?, registrationDraft: PendingRegistration?) -> Unit,
  onBackFromSocialCompletion: () -> Unit = {},
  onCompleteGoogleRegistration: (
    role: UserRole,
    phone: String,
    city: String,
    document: String,
    category: String,
    bio: String,
    rgOrCnh: String,
    criminalRecord: String,
    rgOrCnhFileName: String,
    rgOrCnhAttachmentUri: String?,
    criminalRecordFileName: String,
    criminalRecordAttachmentUri: String?,
    selfieFileName: String,
    selfieAttachmentUri: String?,
    profilePhotoUri: String?
  ) -> Unit,
  onSupportClick: () -> Unit = {},
  onOpenOnboarding: () -> Unit = {},
  availableSpecialties: List<String> = emptyList()
) {
  val context = LocalContext.current

  // Login credential states
  var loginMethod by rememberSaveable { mutableStateOf(CredentialLoginMethod.EMAIL_PASSWORD) }
  var loginEmail by rememberSaveable { mutableStateOf("") }
  var loginEmailPassword by rememberSaveable { mutableStateOf("") }
  var showLoginEmailPassword by rememberSaveable { mutableStateOf(false) }

  var loginPhone by rememberSaveable { mutableStateOf("") }
  var loginPhonePassword by rememberSaveable { mutableStateOf("") }
  var showLoginPhonePassword by rememberSaveable { mutableStateOf(false) }
  var selectedCountryCode by rememberSaveable { mutableStateOf(FirebasePhoneAuthManager.DEFAULT_COUNTRY_CODE) }

  // Registration form fields
  var selectedRole by rememberSaveable { mutableStateOf(UserRole.CLIENTE) }
  var regName by rememberSaveable { mutableStateOf("") }
  var regPhone by rememberSaveable { mutableStateOf("") }
  var regEmail by rememberSaveable { mutableStateOf("") }
  var regPassword by rememberSaveable { mutableStateOf("") }
  var showRegPassword by rememberSaveable { mutableStateOf(false) }
  var regConfirmPassword by rememberSaveable { mutableStateOf("") }
  var showRegConfirmPassword by rememberSaveable { mutableStateOf(false) }
  var regCity by rememberSaveable { mutableStateOf("") }
  var regDocument by rememberSaveable { mutableStateOf("") }
  var regCategory by rememberSaveable { mutableStateOf("Eletricista Residencial & Predial") }
  var regBio by rememberSaveable { mutableStateOf("") }
  val coroutineScope = rememberCoroutineScope()
  var regRgCnhFileName by rememberSaveable { mutableStateOf("") }
  var regRgCnhAttachmentUri by rememberSaveable { mutableStateOf<String?>(null) }
  var regCriminalFileName by rememberSaveable { mutableStateOf("") }
  var regCriminalAttachmentUri by rememberSaveable { mutableStateOf<String?>(null) }
  var regClientProfilePhotoUri by rememberSaveable { mutableStateOf<String?>(null) }
  var regProviderCameraProfilePhotoUri by rememberSaveable { mutableStateOf<String?>(null) }
  var savedAuthModeName by rememberSaveable { mutableStateOf(authMode.name) }
  var hasRestoredSavedAuthMode by remember { mutableStateOf(false) }
  var catExpanded by remember { mutableStateOf(false) }

  // Keep savedAuthModeName in sync and protect against process-death reset to LOGIN while registering
  LaunchedEffect(authMode, authStep) {
    if (!hasRestoredSavedAuthMode) {
      hasRestoredSavedAuthMode = true
      if (authStep == AuthFlowStep.FORM &&
        authMode == AuthMode.LOGIN &&
        savedAuthModeName == AuthMode.REGISTER.name
      ) {
        onSelectMode(AuthMode.REGISTER)
        return@LaunchedEffect
      }
    } else if (authStep == AuthFlowStep.FORM && authMode == AuthMode.LOGIN) {
      DocumentAttachmentHelper.clearPendingAuthReturn(context)
    }
    savedAuthModeName = authMode.name
  }

  val persistAuthReturnBeforeExternalActivity: () -> Unit = {
    DocumentAttachmentHelper.savePendingAuthReturn(
      context = context,
      modeName = if (authStep == AuthFlowStep.FORM) AuthMode.REGISTER.name else authMode.name,
      stepName = authStep.name,
      roleName = selectedRole.name,
      googleUid = pendingGoogleAccount?.uid,
      googleEmail = pendingGoogleAccount?.email,
      googleName = pendingGoogleAccount?.displayName,
      googlePhoto = pendingGoogleAccount?.photoUrl,
      socialProviderLabel = socialProviderLabel,
      pendingPhone = prefillCompletionPhone.takeIf { it.isNotBlank() }
    )
  }

  // Hoisted unconditional camera launchers so ActivityResult is NEVER missed after process recreation
  val launchRegClientProfileCamera = rememberCameraPhotoCaptureLauncher(
    docKey = "client_profile_cam",
    fileNamePrefix = "perfil_cliente",
    permissionDeniedMessage = "Permissão da câmera necessária para tirar sua foto de perfil.",
    onBeforeLaunchCamera = persistAuthReturnBeforeExternalActivity,
    onCaptured = { processed ->
      DocumentAttachmentHelper.clearPendingAuthReturn(context)
      regClientProfilePhotoUri = processed.localFileUri
      if (authStep == AuthFlowStep.FORM && authMode != AuthMode.REGISTER) {
        savedAuthModeName = AuthMode.REGISTER.name
        onSelectMode(AuthMode.REGISTER)
      }
      onClearError()
    }
  )

  val launchRegProviderProfileCamera = rememberCameraPhotoCaptureLauncher(
    docKey = "provider_profile_cam",
    fileNamePrefix = "perfil_prestador",
    permissionDeniedMessage = "Permissão da câmera necessária para tirar a foto de perfil obrigatória do prestador.",
    onBeforeLaunchCamera = persistAuthReturnBeforeExternalActivity,
    onCaptured = { processed ->
      DocumentAttachmentHelper.clearPendingAuthReturn(context)
      regProviderCameraProfilePhotoUri = processed.localFileUri
      if (authStep == AuthFlowStep.FORM && authMode != AuthMode.REGISTER) {
        savedAuthModeName = AuthMode.REGISTER.name
        onSelectMode(AuthMode.REGISTER)
      }
      onClearError()
    }
  )

  val rgCnhPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    DocumentAttachmentHelper.clearPendingAuthReturn(context)
    if (uri != null) {
      coroutineScope.launch {
        val processed = withContext(Dispatchers.IO) {
          DocumentAttachmentHelper.processPickedAttachment(context, uri, "rg_cnh")
        }
        if (processed != null) {
          regRgCnhFileName = processed.fileName
          regRgCnhAttachmentUri = processed.localFileUri
          if (authStep == AuthFlowStep.FORM && authMode != AuthMode.REGISTER) {
            savedAuthModeName = AuthMode.REGISTER.name
            onSelectMode(AuthMode.REGISTER)
          }
          onClearError()
        } else {
          Toast.makeText(context, "Não foi possível anexar o RG/CNH selecionado.", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }
  val criminalPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    DocumentAttachmentHelper.clearPendingAuthReturn(context)
    if (uri != null) {
      coroutineScope.launch {
        val processed = withContext(Dispatchers.IO) {
          DocumentAttachmentHelper.processPickedAttachment(context, uri, "antecedentes")
        }
        if (processed != null) {
          regCriminalFileName = processed.fileName
          regCriminalAttachmentUri = processed.localFileUri
          if (authStep == AuthFlowStep.FORM && authMode != AuthMode.REGISTER) {
            savedAuthModeName = AuthMode.REGISTER.name
            onSelectMode(AuthMode.REGISTER)
          }
          onClearError()
        } else {
          Toast.makeText(context, "Não foi possível anexar a certidão selecionada.", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  val categories = remember(availableSpecialties) {
    if (availableSpecialties.isNotEmpty()) {
      availableSpecialties.filter { it.isNotBlank() }.distinct()
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

  val isBusy = isGoogleAuthLoading || isCredentialAuthLoading

  val buildSocialDraft: () -> PendingRegistration? = {
    val activeProfilePhotoUri = if (selectedRole == UserRole.PRESTADOR) {
      regProviderCameraProfilePhotoUri
    } else {
      regClientProfilePhotoUri
    }
    val hasCustomDraft = regName.isNotBlank() ||
      regPhone.isNotBlank() ||
      regEmail.isNotBlank() ||
      regCity.isNotBlank() ||
      regDocument.isNotBlank() ||
      regBio.isNotBlank() ||
      activeProfilePhotoUri != null ||
      regRgCnhAttachmentUri != null ||
      regCriminalAttachmentUri != null
    if (hasCustomDraft) {
      PendingRegistration(
        name = regName.trim(),
        email = regEmail.trim(),
        phone = regPhone.trim(),
        role = selectedRole,
        category = if (selectedRole == UserRole.PRESTADOR) regCategory else "",
        bio = regBio.trim(),
        city = regCity.trim(),
        document = regDocument.trim(),
        initialWalletBonus = if (selectedRole == UserRole.PRESTADOR) 50.0 else 0.0,
        rgOrCnhNumber = "",
        rgOrCnhFileName = if (selectedRole == UserRole.PRESTADOR) regRgCnhFileName else "",
        rgOrCnhAttachmentUri = if (selectedRole == UserRole.PRESTADOR) regRgCnhAttachmentUri else null,
        criminalRecordNumber = "",
        criminalRecordFileName = if (selectedRole == UserRole.PRESTADOR) regCriminalFileName else "",
        criminalRecordAttachmentUri = if (selectedRole == UserRole.PRESTADOR) regCriminalAttachmentUri else null,
        selfieFileName = "",
        selfieAttachmentUri = null,
        profilePhotoUri = activeProfilePhotoUri
      )
    } else null
  }

  BackHandler(enabled = authStep == AuthFlowStep.GOOGLE_ROLE_COMPLETION) {
    onBackFromSocialCompletion()
  }

  Box(modifier = Modifier.fillMaxSize()) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(Color(0xFF0F172A), BrandPrimaryVariant, BrandPrimary)
        )
      )
      .drawBehind {
        drawCircle(
          color = Color.White.copy(alpha = 0.05f),
          radius = size.width * 0.42f,
          center = Offset(size.width * 0.92f, size.height * 0.08f)
        )
        drawCircle(
          color = BrandSecondary.copy(alpha = 0.14f),
          radius = size.width * 0.28f,
          center = Offset(size.width * 0.08f, size.height * 0.18f)
        )
      }
      .statusBarsPadding()
  ) {
    // 1. HERO BRANDING HEADER
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 18.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          shadowElevation = 6.dp,
          color = Color.White,
          modifier = Modifier.size(56.dp)
        ) {
          Image(
            painter = painterResource(id = R.drawable.resolveai_logo),
            contentDescription = "Logo ResolveAi Serviços",
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxSize()
              .clip(RoundedCornerShape(16.dp))
          )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
          Text(
            text = "ResolveAi Serviços",
            style = MaterialTheme.typography.headlineSmall.copy(
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = (-0.5).sp
            ),
            color = Color.White
          )
          Text(
            text = "Conectando clientes e profissionais",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFCBD5E1)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Surface(
        color = Color.White.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
          .clickable { onOpenOnboarding() }
          .testTag("auth_btn_open_onboarding")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = Color(0xFF5EEAD4),
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.onboarding_how_it_works),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
          )
        }
      }
    }

    // 2. MAIN AUTHENTICATION SHEET
    Surface(
      color = MaterialTheme.colorScheme.background,
      shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
      shadowElevation = 12.dp,
      modifier = Modifier.fillMaxSize()
    ) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
      ) {
        Column(
          modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
          AnimatedContent(
            targetState = authStep,
            transitionSpec = {
              (fadeIn() + expandVertically()).togetherWith(fadeOut() + shrinkVertically())
            },
            label = "auth_step_transition"
          ) { step ->
            when (step) {
              AuthFlowStep.FORM -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                  // Segmented Switcher: Entrar / Criar Conta
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      AuthTabPill(
                        label = "Entrar",
                        selected = authMode == AuthMode.LOGIN,
                        onClick = {
                          savedAuthModeName = AuthMode.LOGIN.name
                          DocumentAttachmentHelper.clearPendingAuthReturn(context)
                          onSelectMode(AuthMode.LOGIN)
                        },
                        modifier = Modifier
                          .weight(1f)
                          .testTag("auth_tab_login")
                      )
                      AuthTabPill(
                        label = "Criar Conta",
                        selected = authMode == AuthMode.REGISTER,
                        onClick = {
                          savedAuthModeName = AuthMode.REGISTER.name
                          onSelectMode(AuthMode.REGISTER)
                        },
                        modifier = Modifier
                          .weight(1f)
                          .testTag("auth_tab_register")
                      )
                    }
                  }

                  if (!authSuccessMessage.isNullOrBlank()) {
                    AuthSuccessCard(message = authSuccessMessage)
                  }

                  if (!authErrorMessage.isNullOrBlank()) {
                    AuthErrorCard(
                      message = authErrorMessage,
                      showRegisterShortcut = authMode == AuthMode.LOGIN && (
                        authErrorMessage.contains("não encontrado", ignoreCase = true) ||
                          authErrorMessage.contains("Nenhuma conta encontrada", ignoreCase = true)
                        ),
                      onSwitchToRegister = { onSelectMode(AuthMode.REGISTER) }
                    )
                  }

                  if (authMode == AuthMode.LOGIN) {
                    // LOGIN FORM: Firebase Google Sign-In em destaque + E-mail e senha | Telefone e senha
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                          text = "Acesse sua conta",
                          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                          color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                          text = "Entre rapidamente com Google pelo Firebase ou utilize e-mail/telefone e senha.",
                          style = MaterialTheme.typography.bodyMedium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }

                      // Primary Firebase Google Sign-In Card
                      Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                          .fillMaxWidth()
                          .testTag("auth_google_login_card")
                      ) {
                        Column(
                          modifier = Modifier.padding(16.dp),
                          verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                          ) {
                            Icon(
                              imageVector = Icons.Default.Verified,
                              contentDescription = null,
                              tint = EmeraldWallet,
                              modifier = Modifier.size(16.dp)
                            )
                            Text(
                              text = stringResource(R.string.auth_google_security_badge),
                              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                              color = EmeraldWallet
                            )
                          }
                          Text(
                            text = stringResource(R.string.auth_google_login_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                          )
                          Text(
                            text = stringResource(R.string.auth_google_login_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                          )
                          GoogleSignInActionButton(
                            label = stringResource(R.string.auth_google_login_button),
                            isLoading = isGoogleAuthLoading,
                            enabled = !isBusy,
                            onClick = { onGoogleSignInClick(null, null) },
                            modifier = Modifier.testTag("auth_btn_google_login")
                          )
                        }
                      }

                      AuthOrDivider(text = "ou acesse com suas credenciais")

                      // Method switcher: E-mail e senha / Telefone e senha
                      Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Row(
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                          horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                          AuthTabPill(
                            label = "E-mail e senha",
                            selected = loginMethod == CredentialLoginMethod.EMAIL_PASSWORD,
                            onClick = {
                              loginMethod = CredentialLoginMethod.EMAIL_PASSWORD
                              onClearError()
                            },
                            modifier = Modifier
                              .weight(1f)
                              .testTag("auth_login_tab_email")
                          )
                          AuthTabPill(
                            label = "Telefone e senha",
                            selected = loginMethod == CredentialLoginMethod.PHONE_PASSWORD,
                            onClick = {
                              loginMethod = CredentialLoginMethod.PHONE_PASSWORD
                              onClearError()
                            },
                            modifier = Modifier
                              .weight(1f)
                              .testTag("auth_login_tab_phone")
                          )
                        }
                      }

                      if (loginMethod == CredentialLoginMethod.EMAIL_PASSWORD) {
                        OutlinedTextField(
                          value = loginEmail,
                          onValueChange = {
                            loginEmail = it
                            onClearError()
                          },
                          label = { Text("E-mail") },
                          placeholder = { Text("seu.email@exemplo.com") },
                          leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                          },
                          trailingIcon = {
                            if (loginEmail.isNotEmpty()) {
                              IconButton(onClick = { loginEmail = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar e-mail")
                              }
                            }
                          },
                          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                          singleLine = true,
                          shape = RoundedCornerShape(14.dp),
                          modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_input_login_email")
                        )

                        OutlinedTextField(
                          value = loginEmailPassword,
                          onValueChange = {
                            loginEmailPassword = it
                            onClearError()
                          },
                          label = { Text("Senha") },
                          placeholder = { Text("Mínimo 6 caracteres") },
                          leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                          },
                          trailingIcon = {
                            IconButton(
                              onClick = { showLoginEmailPassword = !showLoginEmailPassword },
                              modifier = Modifier.testTag("auth_toggle_login_email_password")
                            ) {
                              Icon(
                                imageVector = if (showLoginEmailPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showLoginEmailPassword) "Ocultar senha" else "Mostrar senha"
                              )
                            }
                          },
                          visualTransformation = if (showLoginEmailPassword) VisualTransformation.None else PasswordVisualTransformation(),
                          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                          singleLine = true,
                          shape = RoundedCornerShape(14.dp),
                          modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_input_login_password")
                        )

                        Button(
                          onClick = { onEmailPasswordSignIn(loginEmail, loginEmailPassword) },
                          enabled = !isBusy,
                          shape = RoundedCornerShape(14.dp),
                          modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("auth_btn_login_email")
                        ) {
                          if (isCredentialAuthLoading) {
                            CircularProgressIndicator(
                              color = MaterialTheme.colorScheme.onPrimary,
                              strokeWidth = 2.dp,
                              modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Entrando...")
                          } else {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                              text = "Entrar com E-mail",
                              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                          }
                        }
                      } else {
                        PhoneNumberInputSection(
                          phone = loginPhone,
                          onPhoneChange = {
                            loginPhone = it
                            onClearError()
                          },
                          selectedCountryCode = selectedCountryCode,
                          onCountryCodeChange = {
                            selectedCountryCode = it
                            onClearError()
                          },
                          label = "Telefone com DDD",
                          testTag = "auth_input_login_phone"
                        )

                        OutlinedTextField(
                          value = loginPhonePassword,
                          onValueChange = {
                            loginPhonePassword = it
                            onClearError()
                          },
                          label = { Text("Senha") },
                          placeholder = { Text("Digite sua senha") },
                          leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                          },
                          trailingIcon = {
                            IconButton(
                              onClick = { showLoginPhonePassword = !showLoginPhonePassword },
                              modifier = Modifier.testTag("auth_toggle_login_phone_password")
                            ) {
                              Icon(
                                imageVector = if (showLoginPhonePassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showLoginPhonePassword) "Ocultar senha" else "Mostrar senha"
                              )
                            }
                          },
                          visualTransformation = if (showLoginPhonePassword) VisualTransformation.None else PasswordVisualTransformation(),
                          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                          singleLine = true,
                          shape = RoundedCornerShape(14.dp),
                          modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_input_login_phone_password")
                        )

                        Button(
                          onClick = {
                            onPhonePasswordSignIn(loginPhone, selectedCountryCode, loginPhonePassword)
                          },
                          enabled = !isBusy,
                          shape = RoundedCornerShape(14.dp),
                          modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("auth_btn_login_phone")
                        ) {
                          if (isCredentialAuthLoading) {
                            CircularProgressIndicator(
                              color = MaterialTheme.colorScheme.onPrimary,
                              strokeWidth = 2.dp,
                              modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Autenticando...")
                          } else {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                              text = "Entrar com Telefone",
                              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                          }
                        }
                      }

                      AuthSupportCard(onSupportClick = onSupportClick)
                    }
                  } else {
                    // REGISTRATION FORM
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                          text = "Criar nova conta",
                          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                          color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                          text = "Escolha seu perfil e cadastre-se com e-mail, telefone ou conta Google.",
                          style = MaterialTheme.typography.bodyMedium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }

                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                      ) {
                        RoleCardSelector(
                          title = "Sou Cliente",
                          subtitle = "Solicitar serviços",
                          icon = Icons.Default.Handshake,
                          selected = selectedRole == UserRole.CLIENTE,
                          accentColor = MaterialTheme.colorScheme.primary,
                          containerColor = MaterialTheme.colorScheme.primaryContainer,
                          onClick = { selectedRole = UserRole.CLIENTE },
                          modifier = Modifier
                            .weight(1f)
                            .testTag("auth_reg_role_cliente")
                        )
                        RoleCardSelector(
                          title = "Sou Prestador",
                          subtitle = "Atender pedidos",
                          icon = Icons.Default.AccountBalanceWallet,
                          selected = selectedRole == UserRole.PRESTADOR,
                          accentColor = EmeraldWallet,
                          containerColor = EmeraldWalletLight,
                          onClick = { selectedRole = UserRole.PRESTADOR },
                          modifier = Modifier
                            .weight(1f)
                            .testTag("auth_reg_role_prestador")
                        )
                      }

                      GoogleSignInActionButton(
                        label = if (selectedRole == UserRole.PRESTADOR) {
                          "Cadastrar Prestador com Google"
                        } else {
                          "Cadastrar Cliente com Google"
                        },
                        isLoading = isGoogleAuthLoading,
                        enabled = !isBusy,
                        onClick = { onGoogleSignInClick(selectedRole, buildSocialDraft()) },
                        modifier = Modifier.testTag("auth_btn_google_register")
                      )

                      AuthOrDivider(text = "ou cadastre-se com e-mail / telefone e senha")

                      OutlinedTextField(
                        value = regName,
                        onValueChange = {
                          regName = it
                          onClearError()
                        },
                        label = { Text("Nome Completo") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                          .fillMaxWidth()
                          .testTag("auth_reg_name")
                      )

                      PhoneNumberInputSection(
                        phone = regPhone,
                        onPhoneChange = {
                          regPhone = it
                          onClearError()
                        },
                        selectedCountryCode = selectedCountryCode,
                        onCountryCodeChange = {
                          selectedCountryCode = it
                          onClearError()
                        },
                        label = "Telefone / Celular com DDD *",
                        testTag = "auth_reg_phone"
                      )

                      val regEmailError = if (regEmail.isNotBlank()) ContactValidator.validateEmail(regEmail) else null
                      val isRegEmailValid = ContactValidator.isValidEmail(regEmail)
                      OutlinedTextField(
                        value = regEmail,
                        onValueChange = {
                          regEmail = it
                          onClearError()
                        },
                        label = { Text("E-mail obrigatório *") },
                        placeholder = { Text("seu.email@exemplo.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = regEmailError != null,
                        supportingText = {
                          when {
                            isRegEmailValid -> Text(
                              text = "✓ E-mail válido",
                              color = EmeraldWallet,
                              fontWeight = FontWeight.SemiBold
                            )
                            regEmailError != null -> Text(
                              text = regEmailError,
                              color = DangerRed
                            )
                            else -> Text(
                              text = "Obrigatório informar um e-mail válido.",
                              color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                          }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                          .fillMaxWidth()
                          .testTag("auth_reg_email")
                      )

                      val isRegPasswordTooShort = regPassword.isNotEmpty() && regPassword.length < 6
                      val isRegPasswordValid = regPassword.length >= 6
                      OutlinedTextField(
                        value = regPassword,
                        onValueChange = {
                          regPassword = it
                          onClearError()
                        },
                        label = { Text(stringResource(R.string.auth_create_password_label)) },
                        placeholder = { Text(stringResource(R.string.auth_password_min_length_hint)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                          IconButton(
                            onClick = { showRegPassword = !showRegPassword },
                            modifier = Modifier.testTag("auth_toggle_reg_password")
                          ) {
                            Icon(
                              imageVector = if (showRegPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                              contentDescription = if (showRegPassword) "Ocultar senha" else "Mostrar senha"
                            )
                          }
                        },
                        visualTransformation = if (showRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = isRegPasswordTooShort,
                        supportingText = {
                          when {
                            isRegPasswordValid -> Text(
                              text = "✓ Senha válida",
                              color = EmeraldWallet,
                              fontWeight = FontWeight.SemiBold
                            )
                            isRegPasswordTooShort -> Text(
                              text = "A senha deve ter pelo menos 6 caracteres.",
                              color = DangerRed
                            )
                            else -> Text(
                              text = stringResource(R.string.auth_password_min_length_hint),
                              color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                          }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                          .fillMaxWidth()
                          .testTag("auth_reg_password")
                      )

                      val isConfirmPasswordMismatch = regConfirmPassword.isNotEmpty() && regConfirmPassword != regPassword
                      val isConfirmPasswordMatch = isRegPasswordValid && regConfirmPassword == regPassword
                      OutlinedTextField(
                        value = regConfirmPassword,
                        onValueChange = {
                          regConfirmPassword = it
                          onClearError()
                        },
                        label = { Text(stringResource(R.string.auth_confirm_password_label)) },
                        placeholder = { Text("Repita a senha criada") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                          IconButton(
                            onClick = { showRegConfirmPassword = !showRegConfirmPassword },
                            modifier = Modifier.testTag("auth_toggle_reg_confirm_password")
                          ) {
                            Icon(
                              imageVector = if (showRegConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                              contentDescription = if (showRegConfirmPassword) "Ocultar confirmação de senha" else "Mostrar confirmação de senha"
                            )
                          }
                        },
                        visualTransformation = if (showRegConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = isConfirmPasswordMismatch,
                        supportingText = {
                          when {
                            isConfirmPasswordMatch -> Text(
                              text = stringResource(R.string.auth_password_match_ok),
                              color = EmeraldWallet,
                              fontWeight = FontWeight.SemiBold,
                              modifier = Modifier.testTag("auth_reg_confirm_password_status")
                            )
                            isConfirmPasswordMismatch -> Text(
                              text = stringResource(R.string.auth_password_mismatch_error),
                              color = DangerRed,
                              modifier = Modifier.testTag("auth_reg_confirm_password_status")
                            )
                            else -> Text(
                              text = "Digite novamente a mesma senha para confirmar.",
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                              modifier = Modifier.testTag("auth_reg_confirm_password_status")
                            )
                          }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                          .fillMaxWidth()
                          .testTag("auth_reg_confirm_password")
                      )

                      GooglePlacesAutocompleteField(
                        value = regCity,
                        onValueChange = { regCity = it },
                        mode = LocationAutocompleteMode.CITY,
                        label = "Cidade / UF",
                        fieldTestTag = "auth_reg_city"
                      )

                      val regCpfDigits = regDocument.filter { it.isDigit() }
                      val isRegCpfValid = CpfValidator.isValid(regDocument)
                      val regCpfErrorMessage = if (regCpfDigits.isNotEmpty() && !isRegCpfValid) {
                        if (selectedRole == UserRole.PRESTADOR) {
                          CpfValidator.validateProviderCpf(regDocument)
                        } else {
                          CpfValidator.validateClientCpf(regDocument)
                        }
                      } else {
                        null
                      }

                      OutlinedTextField(
                        value = regDocument,
                        onValueChange = {
                          regDocument = CpfValidator.sanitizeInput(it)
                          onClearError()
                        },
                        label = { Text(if (selectedRole == UserRole.PRESTADOR) "CPF obrigatório *" else "CPF *") },
                        placeholder = { Text("000.000.000-00") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = regCpfErrorMessage != null,
                        supportingText = {
                          when {
                            isRegCpfValid -> Text(
                              text = "✓ CPF válido: ${CpfValidator.formatCpf(regDocument)}",
                              color = EmeraldWallet,
                              fontWeight = FontWeight.SemiBold,
                              modifier = Modifier.testTag("auth_reg_cpf_status")
                            )
                            regCpfErrorMessage != null -> Text(
                              text = regCpfErrorMessage,
                              color = DangerRed,
                              fontWeight = FontWeight.Medium,
                              modifier = Modifier.testTag("auth_reg_cpf_status")
                            )
                            else -> Text(
                              text = if (selectedRole == UserRole.PRESTADOR) {
                                "Obrigatório digitar um CPF válido (11 dígitos) para cadastro de prestador."
                              } else {
                                "Digite seu CPF válido com 11 dígitos."
                              },
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                              modifier = Modifier.testTag("auth_reg_cpf_status")
                            )
                          }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                          .fillMaxWidth()
                          .testTag("auth_reg_document")
                      )

                      AnimatedVisibility(visible = selectedRole == UserRole.CLIENTE) {
                        ClientProfilePhotoPickerCard(
                          photoUri = regClientProfilePhotoUri,
                          onPhotoSelected = { uri ->
                            DocumentAttachmentHelper.clearPendingAuthReturn(context)
                            regClientProfilePhotoUri = uri
                            onClearError()
                          },
                          onClearPhoto = {
                            regClientProfilePhotoUri = null
                            onClearError()
                          },
                          onBeforeLaunchPicker = persistAuthReturnBeforeExternalActivity,
                          onLaunchCameraOverride = launchRegClientProfileCamera
                        )
                      }

                      AnimatedVisibility(visible = selectedRole == UserRole.PRESTADOR) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                          ExposedDropdownMenuBox(
                            expanded = catExpanded,
                            onExpandedChange = { catExpanded = it }
                          ) {
                            OutlinedTextField(
                              value = regCategory,
                              onValueChange = {},
                              readOnly = true,
                              label = { Text("Especialidade Profissional") },
                              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                              shape = RoundedCornerShape(14.dp),
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
                                    regCategory = cat
                                    catExpanded = false
                                  }
                                )
                              }
                            }
                          }

                          OutlinedTextField(
                            value = regBio,
                            onValueChange = { regBio = it },
                            label = { Text("Apresentação profissional") },
                            placeholder = { Text("Ex: 5 anos de experiência em elétrica residencial...") },
                            minLines = 2,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                          )

                          ProviderCameraProfilePhotoCard(
                            photoUri = regProviderCameraProfilePhotoUri,
                            onPhotoCaptured = { uri ->
                              DocumentAttachmentHelper.clearPendingAuthReturn(context)
                              regProviderCameraProfilePhotoUri = uri
                              onClearError()
                            },
                            onClearPhoto = {
                              regProviderCameraProfilePhotoUri = null
                              onClearError()
                            },
                            onBeforeLaunchCamera = persistAuthReturnBeforeExternalActivity,
                            onLaunchCameraOverride = launchRegProviderProfileCamera
                          )

                          Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                          ) {
                            Column(
                              modifier = Modifier.padding(12.dp),
                              verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                              val attachedCount = listOf(
                                regRgCnhAttachmentUri,
                                regCriminalAttachmentUri
                              ).count { !it.isNullOrBlank() }

                              Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                              ) {
                                Text(
                                  text = "Documentação obrigatória para aprovação",
                                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                  color = MaterialTheme.colorScheme.primary,
                                  modifier = Modifier.weight(1f)
                                )
                                Surface(
                                  color = if (attachedCount == 2) EmeraldWalletLight else WarningAmberLight,
                                  shape = RoundedCornerShape(8.dp)
                                ) {
                                  Text(
                                    text = "$attachedCount/2 anexos",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = if (attachedCount == 2) EmeraldWallet else WarningAmber,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                  )
                                }
                              }

                              ProviderDocumentAttachmentCard(
                                title = "Anexo 1: RG ou CNH (Frente e Verso) *",
                                subtitle = "Envie foto nítida ou PDF do documento oficial",
                                fileName = regRgCnhFileName,
                                attachmentUri = regRgCnhAttachmentUri,
                                buttonLabel = "Anexar RG / CNH (Foto ou PDF)",
                                buttonTag = "auth_attach_rg_cnh",
                                onAttachClick = {
                                  try {
                                    persistAuthReturnBeforeExternalActivity()
                                    rgCnhPickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                                  } catch (_: Throwable) {
                                    Toast.makeText(context, "Não foi possível abrir o seletor de arquivos.", Toast.LENGTH_SHORT).show()
                                  }
                                },
                                onClearClick = {
                                  regRgCnhFileName = ""
                                  regRgCnhAttachmentUri = null
                                }
                              )

                              ProviderDocumentAttachmentCard(
                                title = "Anexo 2: Certidão de Antecedentes *",
                                subtitle = "Envie o PDF emitido ou foto legível da certidão",
                                fileName = regCriminalFileName,
                                attachmentUri = regCriminalAttachmentUri,
                                buttonLabel = "Anexar Certidão (Foto ou PDF)",
                                buttonTag = "auth_attach_criminal",
                                onAttachClick = {
                                  try {
                                    persistAuthReturnBeforeExternalActivity()
                                    criminalPickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                                  } catch (_: Throwable) {
                                    Toast.makeText(context, "Não foi possível abrir o seletor de arquivos.", Toast.LENGTH_SHORT).show()
                                  }
                                },
                                onClearClick = {
                                  regCriminalFileName = ""
                                  regCriminalAttachmentUri = null
                                }
                              )

                              Surface(
                                color = EmeraldWalletLight,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                              ) {
                                Row(
                                  modifier = Modifier.padding(10.dp),
                                  verticalAlignment = Alignment.CenterVertically
                                ) {
                                  Icon(
                                    Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = EmeraldWallet,
                                    modifier = Modifier.size(18.dp)
                                  )
                                  Spacer(modifier = Modifier.width(8.dp))
                                  Text(
                                    text = "Bônus de R$ 50,00 na carteira do prestador após concluir o cadastro",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = EmeraldWallet
                                  )
                                }
                              }
                            }
                          }
                        }
                      }

                      val missingRegistrationItems = remember(
                        selectedRole,
                        regName,
                        regEmail,
                        regPhone,
                        selectedCountryCode,
                        regPassword,
                        regConfirmPassword,
                        regCity,
                        regDocument,
                        regProviderCameraProfilePhotoUri,
                        regRgCnhAttachmentUri,
                        regCriminalAttachmentUri
                      ) {
                        val baseMissing = if (selectedRole == UserRole.PRESTADOR) {
                          CpfValidator.providerMissingRequirements(
                            name = regName,
                            requireName = true,
                            email = regEmail,
                            phone = regPhone,
                            countryCode = selectedCountryCode,
                            requireEmailAndPhone = true,
                            password = regPassword,
                            requirePassword = true,
                            city = regCity,
                            document = regDocument,
                            profilePhotoUri = regProviderCameraProfilePhotoUri,
                            requireCameraProfilePhoto = true,
                            rgOrCnhAttachmentUri = regRgCnhAttachmentUri,
                            criminalRecordAttachmentUri = regCriminalAttachmentUri
                          )
                        } else {
                          CpfValidator.clientMissingRequirements(
                            name = regName,
                            requireName = true,
                            email = regEmail,
                            requireEmail = true,
                            phone = regPhone,
                            countryCode = selectedCountryCode,
                            requirePhone = true,
                            password = regPassword,
                            requirePassword = true,
                            city = regCity,
                            document = regDocument
                          )
                        }
                        buildList {
                          addAll(baseMissing)
                          if (regPassword.length >= 6 && regConfirmPassword != regPassword) {
                            add("Confirmar senha igual à senha criada")
                          }
                        }
                      }
                      val canSubmitRegistration = !isBusy && missingRegistrationItems.isEmpty()

                      Surface(
                        color = if (missingRegistrationItems.isEmpty()) EmeraldWalletLight else WarningAmberLight,
                        border = BorderStroke(
                          1.dp,
                          if (missingRegistrationItems.isEmpty()) EmeraldWallet.copy(alpha = 0.4f) else WarningAmber.copy(alpha = 0.45f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                          .fillMaxWidth()
                          .testTag("auth_reg_requirements_status")
                      ) {
                        Column(
                          modifier = Modifier.padding(12.dp),
                          verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                          if (missingRegistrationItems.isEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                              Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldWallet,
                                modifier = Modifier.size(18.dp)
                              )
                              Spacer(modifier = Modifier.width(8.dp))
                              Text(
                                text = if (selectedRole == UserRole.PRESTADOR) {
                                  "Tudo preenchido! CPF válido, foto de perfil na câmera e 2/2 anexos prontos para criar sua conta."
                                } else {
                                  "Dados, e-mail, telefone e CPF válidos! Botão Criar Conta liberado."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF065F46)
                              )
                            }
                          } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                              Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(18.dp)
                              )
                              Spacer(modifier = Modifier.width(8.dp))
                              Text(
                                text = "O botão Criar Conta será liberado após preencher:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF92400E)
                              )
                            }
                            Text(
                              text = missingRegistrationItems.joinToString("\n") { "• $it" },
                              style = MaterialTheme.typography.bodySmall,
                              color = Color(0xFF78350F)
                            )
                          }
                        }
                      }

                      Button(
                        onClick = {
                          val chosenProfilePhoto = if (selectedRole == UserRole.PRESTADOR) {
                            regProviderCameraProfilePhotoUri
                          } else {
                            regClientProfilePhotoUri
                          }
                          val submitRegistration: (String?, String?, String?) -> Unit = { resolvedPhoto, resolvedRg, resolvedCriminal ->
                            onRegisterWithPassword(
                              PendingRegistration(
                                name = regName.trim(),
                                email = regEmail.trim(),
                                phone = regPhone.trim(),
                                role = selectedRole,
                                category = if (selectedRole == UserRole.PRESTADOR) regCategory else "",
                                bio = regBio.trim(),
                                city = regCity.trim(),
                                document = regDocument.trim(),
                                initialWalletBonus = if (selectedRole == UserRole.PRESTADOR) 50.0 else 0.0,
                                rgOrCnhNumber = "",
                                rgOrCnhFileName = if (selectedRole == UserRole.PRESTADOR) regRgCnhFileName else "",
                                rgOrCnhAttachmentUri = if (selectedRole == UserRole.PRESTADOR) resolvedRg else null,
                                criminalRecordNumber = "",
                                criminalRecordFileName = if (selectedRole == UserRole.PRESTADOR) regCriminalFileName else "",
                                criminalRecordAttachmentUri = if (selectedRole == UserRole.PRESTADOR) resolvedCriminal else null,
                                selfieFileName = "",
                                selfieAttachmentUri = null,
                                profilePhotoUri = resolvedPhoto,
                                password = regPassword
                              ),
                              selectedCountryCode,
                              regPassword
                            )
                          }
                          val hasLocalFiles = listOf(chosenProfilePhoto, regRgCnhAttachmentUri, regCriminalAttachmentUri)
                            .any { it?.startsWith("file://", ignoreCase = true) == true }
                          if (hasLocalFiles) {
                            coroutineScope.launch {
                              val resolvedPhoto = withContext(Dispatchers.IO) {
                                DocumentAttachmentHelper.resolveForPersistence(context, chosenProfilePhoto)
                              }
                              val resolvedRg = withContext(Dispatchers.IO) {
                                DocumentAttachmentHelper.resolveForPersistence(context, regRgCnhAttachmentUri)
                              }
                              val resolvedCriminal = withContext(Dispatchers.IO) {
                                DocumentAttachmentHelper.resolveForPersistence(context, regCriminalAttachmentUri)
                              }
                              submitRegistration(resolvedPhoto, resolvedRg, resolvedCriminal)
                            }
                          } else {
                            submitRegistration(chosenProfilePhoto, regRgCnhAttachmentUri, regCriminalAttachmentUri)
                          }
                        },
                        enabled = canSubmitRegistration,
                        colors = ButtonDefaults.buttonColors(
                          containerColor = if (selectedRole == UserRole.PRESTADOR) EmeraldWallet else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                          .fillMaxWidth()
                          .heightIn(min = 52.dp)
                          .testTag("auth_btn_register_firebase")
                      ) {
                        if (isCredentialAuthLoading) {
                          CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                          )
                          Spacer(modifier = Modifier.width(10.dp))
                          Text("Criando conta...")
                        } else {
                          Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(20.dp))
                          Spacer(modifier = Modifier.width(8.dp))
                          Text(
                            text = "Criar Conta com Firebase",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                          )
                        }
                      }

                      AuthSupportCard(onSupportClick = onSupportClick)
                    }
                  }
                }
              }

              AuthFlowStep.GOOGLE_ROLE_COMPLETION -> {
                val socialAccount = pendingGoogleAccount
                if (socialAccount != null) {
                  GoogleRoleCompletionStep(
                    account = socialAccount,
                    providerLabel = socialProviderLabel,
                    prefillPhone = prefillCompletionPhone,
                    preferredRole = preferredGoogleRole,
                    pendingDraft = pendingGoogleDraft,
                    externalErrorMessage = authErrorMessage,
                    onClearExternalError = onClearError,
                    categories = categories,
                    onBack = onBackFromSocialCompletion,
                    onComplete = onCompleteGoogleRegistration
                  )
                }
              }
            }
          }
        }
      }
    }
  }
  }
}

@Composable
private fun AuthTabPill(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
    shadowElevation = if (selected) 2.dp else 0.dp,
    shape = RoundedCornerShape(10.dp),
    modifier = modifier
      .heightIn(min = 48.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
      Text(
        text = label,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.titleSmall.copy(
          fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        ),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun RoleCardSelector(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean,
  accentColor: Color,
  containerColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = if (selected) containerColor else MaterialTheme.colorScheme.surfaceVariant,
    border = if (selected) BorderStroke(1.5.dp, accentColor) else null,
    shape = RoundedCornerShape(14.dp),
    modifier = modifier
      .heightIn(min = 74.dp)
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = title,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun AuthErrorCard(
  message: String,
  showRegisterShortcut: Boolean,
  onSwitchToRegister: () -> Unit
) {
  Surface(
    color = DangerRedLight,
    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("auth_error_banner")
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.ErrorOutline,
          contentDescription = null,
          tint = DangerRed,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = message,
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
          color = Color(0xFF991B1B)
        )
      }
      if (showRegisterShortcut) {
        TextButton(
          onClick = onSwitchToRegister,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
          modifier = Modifier.align(Alignment.End)
        ) {
          Text("Ir para Criar Conta →", fontWeight = FontWeight.Bold, color = DangerRed)
        }
      }
    }
  }
}

@Composable
private fun AuthSuccessCard(message: String) {
  Surface(
    color = EmeraldWalletLight,
    border = BorderStroke(1.dp, EmeraldWallet.copy(alpha = 0.5f)),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("auth_success_banner")
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = EmeraldWallet,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        color = Color(0xFF065F46)
      )
    }
  }
}

@Composable
private fun PhoneNumberInputSection(
  phone: String,
  onPhoneChange: (String) -> Unit,
  selectedCountryCode: String,
  onCountryCodeChange: (String) -> Unit,
  label: String,
  testTag: String
) {
  var ddiMenuExpanded by remember { mutableStateOf(false) }
  val selectedCountry = FirebasePhoneAuthManager.SUPPORTED_COUNTRIES.find {
    it.dialCode == selectedCountryCode
  } ?: FirebasePhoneAuthManager.SUPPORTED_COUNTRIES.first()
  val digitsCount = phone.filter { it.isDigit() }.length
  val e164Preview = remember(phone, selectedCountryCode) {
    val cleanDial = selectedCountryCode.trim().let { if (it.startsWith("+")) it else "+$it" }
    val digits = phone.filter { it.isDigit() }
    val dialDigits = cleanDial.filter { it.isDigit() }
    when {
      digits.isEmpty() -> ""
      phone.trim().startsWith("+") -> "+$digits"
      dialDigits == "55" && digits.startsWith("55") && digits.length >= 12 -> "+$digits"
      else -> "$cleanDial$digits"
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { ddiMenuExpanded = true }
            .testTag("${testTag}_ddi")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(text = "${selectedCountry.flag} ${selectedCountry.dialCode}", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(2.dp))
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Selecionar DDI", modifier = Modifier.size(18.dp))
          }
        }
        DropdownMenu(
          expanded = ddiMenuExpanded,
          onDismissRequest = { ddiMenuExpanded = false }
        ) {
          FirebasePhoneAuthManager.SUPPORTED_COUNTRIES.forEach { option ->
            DropdownMenuItem(
              text = { Text("${option.flag} ${option.countryName} (${option.dialCode})") },
              onClick = {
                onCountryCodeChange(option.dialCode)
                ddiMenuExpanded = false
              }
            )
          }
        }
      }

      OutlinedTextField(
        value = phone,
        onValueChange = onPhoneChange,
        label = { Text(label) },
        placeholder = { Text("(11) 98111-2233") },
        leadingIcon = {
          Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingIcon = {
          if (phone.isNotEmpty()) {
            IconButton(onClick = { onPhoneChange("") }) {
              Icon(Icons.Default.Clear, contentDescription = "Limpar telefone")
            }
          }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .weight(1f)
          .testTag(testTag)
      )
    }

    if (e164Preview.isNotEmpty()) {
      Row(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = if (digitsCount >= 10) Icons.Default.CheckCircle else Icons.Default.Info,
          contentDescription = null,
          tint = if (digitsCount >= 10) EmeraldWallet else WarningAmber,
          modifier = Modifier.size(14.dp)
        )
        Text(
          text = if (digitsCount >= 10) {
            "Telefone formatado: $e164Preview"
          } else {
            "Digite o DDD + número ($digitsCount/10+ dígitos)"
          },
          style = MaterialTheme.typography.labelSmall,
          color = if (digitsCount >= 10) EmeraldWallet else Color(0xFFB45309)
        )
      }
    }
  }
}

@Composable
private fun AuthSupportCard(
  onSupportClick: () -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Surface(
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
          shape = CircleShape,
          modifier = Modifier.size(38.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.SupportAgent,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.auth_support_card_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = stringResource(R.string.auth_support_card_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      OutlinedButton(
        onClick = onSupportClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .testTag("auth_btn_support")
      ) {
        Icon(
          imageVector = Icons.Default.SupportAgent,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.auth_support_card_action),
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
      }
    }
  }
}

@Composable
private fun GoogleSignInActionButton(
  label: String,
  isLoading: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  OutlinedButton(
    onClick = onClick,
    enabled = enabled,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = Color.White,
      contentColor = Color(0xFF0F172A)
    ),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 52.dp)
  ) {
    if (isLoading) {
      CircularProgressIndicator(
        color = BrandPrimary,
        strokeWidth = 2.dp,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = stringResource(R.string.auth_google_loading),
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = Color(0xFF0F172A)
      )
    } else {
      Surface(
        color = Color(0xFFEA4335).copy(alpha = 0.12f),
        shape = CircleShape,
        modifier = Modifier.size(26.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = "G",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = Color(0xFFDB4437)
          )
        }
      }
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = Color(0xFF0F172A)
      )
    }
  }
}

@Composable
private fun AuthOrDivider(text: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    HorizontalDivider(
      modifier = Modifier.weight(1f),
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    )
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 12.dp)
    )
    HorizontalDivider(
      modifier = Modifier.weight(1f),
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoogleRoleCompletionStep(
  account: GoogleAccountInfo,
  providerLabel: String,
  prefillPhone: String,
  preferredRole: UserRole = UserRole.CLIENTE,
  pendingDraft: PendingRegistration? = null,
  externalErrorMessage: String? = null,
  onClearExternalError: () -> Unit = {},
  categories: List<String>,
  onBack: () -> Unit,
  onComplete: (
    role: UserRole,
    phone: String,
    city: String,
    document: String,
    category: String,
    bio: String,
    rgOrCnh: String,
    criminalRecord: String,
    rgOrCnhFileName: String,
    rgOrCnhAttachmentUri: String?,
    criminalRecordFileName: String,
    criminalRecordAttachmentUri: String?,
    selfieFileName: String,
    selfieAttachmentUri: String?,
    profilePhotoUri: String?
  ) -> Unit
) {
  val context = LocalContext.current
  val initialRole = pendingDraft?.role ?: preferredRole
  val initialPhone = pendingDraft?.phone?.takeIf { it.isNotBlank() } ?: prefillPhone
  val initialCity = pendingDraft?.city?.takeIf { it.isNotBlank() } ?: ""
  val initialCategory = pendingDraft?.category?.takeIf { it.isNotBlank() } ?: "Eletricista Residencial & Predial"

  var selectedRole by rememberSaveable(account.uid, initialRole) { mutableStateOf(initialRole) }
  var phone by rememberSaveable(account.uid, initialPhone) { mutableStateOf(initialPhone) }
  var city by rememberSaveable(account.uid, initialCity) { mutableStateOf(initialCity) }
  var document by rememberSaveable(account.uid, pendingDraft?.document) {
    mutableStateOf(pendingDraft?.document.orEmpty())
  }
  var category by rememberSaveable(account.uid, initialCategory) { mutableStateOf(initialCategory) }
  var bio by rememberSaveable(account.uid, pendingDraft?.bio) {
    mutableStateOf(pendingDraft?.bio.orEmpty())
  }
  var rgCnhFileName by rememberSaveable(account.uid, pendingDraft?.rgOrCnhFileName) {
    mutableStateOf(pendingDraft?.rgOrCnhFileName.orEmpty())
  }
  var rgCnhAttachmentUri by rememberSaveable(account.uid, pendingDraft?.rgOrCnhAttachmentUri) {
    mutableStateOf(pendingDraft?.rgOrCnhAttachmentUri)
  }
  var criminalFileName by rememberSaveable(account.uid, pendingDraft?.criminalRecordFileName) {
    mutableStateOf(pendingDraft?.criminalRecordFileName.orEmpty())
  }
  var criminalAttachmentUri by rememberSaveable(account.uid, pendingDraft?.criminalRecordAttachmentUri) {
    mutableStateOf(pendingDraft?.criminalRecordAttachmentUri)
  }
  val coroutineScope = rememberCoroutineScope()
  var clientProfilePhotoUri by rememberSaveable(account.uid, initialRole, pendingDraft?.profilePhotoUri) {
    mutableStateOf(if (initialRole == UserRole.CLIENTE) pendingDraft?.profilePhotoUri else null)
  }
  var providerCameraProfilePhotoUri by rememberSaveable(account.uid, initialRole, pendingDraft?.profilePhotoUri) {
    mutableStateOf(if (initialRole == UserRole.PRESTADOR) pendingDraft?.profilePhotoUri else null)
  }
  var localValidationError by rememberSaveable(account.uid) { mutableStateOf<String?>(null) }
  var catExpanded by remember { mutableStateOf(false) }

  val clearErrors = {
    localValidationError = null
    onClearExternalError()
  }

  val rgPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      coroutineScope.launch {
        val processed = withContext(Dispatchers.IO) {
          DocumentAttachmentHelper.processPickedAttachment(context, uri, "social_rg_cnh")
        }
        if (processed != null) {
          rgCnhFileName = processed.fileName
          rgCnhAttachmentUri = processed.localFileUri
          clearErrors()
        } else {
          Toast.makeText(context, "Não foi possível anexar o RG/CNH selecionado.", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }
  val criminalPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      coroutineScope.launch {
        val processed = withContext(Dispatchers.IO) {
          DocumentAttachmentHelper.processPickedAttachment(context, uri, "social_criminal")
        }
        if (processed != null) {
          criminalFileName = processed.fileName
          criminalAttachmentUri = processed.localFileUri
          clearErrors()
        } else {
          Toast.makeText(context, "Não foi possível anexar a certidão selecionada.", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    TextButton(
      onClick = onBack,
      contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
      modifier = Modifier
        .heightIn(min = 48.dp)
        .testTag("auth_btn_back_from_google")
    ) {
      Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("Usar outra conta", fontWeight = FontWeight.SemiBold)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = "Documentação e Perfil • $providerLabel",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )
      Text(
        text = "Sua conta $providerLabel foi autenticada. Para concluir o cadastro com segurança, preencha seus dados e documentos obrigatórios:",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    Surface(
      color = EmeraldWalletLight,
      border = BorderStroke(1.dp, EmeraldWallet.copy(alpha = 0.4f)),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = EmeraldWallet,
          shape = CircleShape,
          modifier = Modifier.size(40.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = account.displayName.take(2).uppercase(),
              color = Color.White,
              fontWeight = FontWeight.Bold
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = account.displayName,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF065F46)
          )
          Text(
            text = account.email,
            style = MaterialTheme.typography.bodySmall,
            color = EmeraldWallet
          )
        }
        Icon(
          imageVector = Icons.Default.Verified,
          contentDescription = "Conta verificada via Firebase ($providerLabel)",
          tint = EmeraldWallet,
          modifier = Modifier.size(22.dp)
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      RoleCardSelector(
        title = "Sou Cliente",
        subtitle = "Solicitar serviços",
        icon = Icons.Default.Handshake,
        selected = selectedRole == UserRole.CLIENTE,
        accentColor = MaterialTheme.colorScheme.primary,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        onClick = {
          selectedRole = UserRole.CLIENTE
          clearErrors()
        },
        modifier = Modifier
          .weight(1f)
          .testTag("google_reg_role_cliente")
      )
      RoleCardSelector(
        title = "Sou Prestador",
        subtitle = "Atender pedidos",
        icon = Icons.Default.AccountBalanceWallet,
        selected = selectedRole == UserRole.PRESTADOR,
        accentColor = EmeraldWallet,
        containerColor = EmeraldWalletLight,
        onClick = {
          selectedRole = UserRole.PRESTADOR
          clearErrors()
        },
        modifier = Modifier
          .weight(1f)
          .testTag("google_reg_role_prestador")
      )
    }

    OutlinedTextField(
      value = phone,
      onValueChange = {
        phone = it
        clearErrors()
      },
      label = { Text("Celular com DDD *") },
      placeholder = { Text("(11) 98765-4321") },
      leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
      singleLine = true,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("google_reg_phone")
    )

    GooglePlacesAutocompleteField(
      value = city,
      onValueChange = {
        city = it
        clearErrors()
      },
      mode = LocationAutocompleteMode.CITY,
      label = "Cidade / UF *",
      fieldTestTag = "google_reg_city"
    )

    val googleCpfDigits = document.filter { it.isDigit() }
    val isGoogleCpfValid = CpfValidator.isValid(document)
    val googleCpfError = if (googleCpfDigits.isNotEmpty() && !isGoogleCpfValid) {
      if (selectedRole == UserRole.PRESTADOR) {
        CpfValidator.validateProviderCpf(document)
      } else {
        CpfValidator.validateClientCpf(document)
      }
    } else {
      null
    }

    OutlinedTextField(
      value = document,
      onValueChange = {
        document = CpfValidator.sanitizeInput(it)
        clearErrors()
      },
      label = { Text(if (selectedRole == UserRole.PRESTADOR) "CPF obrigatório *" else "CPF *") },
      placeholder = { Text("000.000.000-00") },
      leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      isError = googleCpfError != null,
      supportingText = {
        when {
          isGoogleCpfValid -> Text(
            text = "✓ CPF válido: ${CpfValidator.formatCpf(document)}",
            color = EmeraldWallet,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("google_reg_cpf_status")
          )
          googleCpfError != null -> Text(
            text = googleCpfError,
            color = DangerRed,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.testTag("google_reg_cpf_status")
          )
          else -> Text(
            text = if (selectedRole == UserRole.PRESTADOR) {
              "Obrigatório informar um CPF válido (11 dígitos)."
            } else {
              "Informe seu CPF válido com 11 dígitos."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("google_reg_cpf_status")
          )
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("google_reg_document")
    )

    val persistSocialAuthReturn: () -> Unit = {
      DocumentAttachmentHelper.savePendingAuthReturn(
        context = context,
        modeName = AuthMode.REGISTER.name,
        stepName = AuthFlowStep.GOOGLE_ROLE_COMPLETION.name,
        roleName = selectedRole.name,
        googleUid = account.uid,
        googleEmail = account.email,
        googleName = account.displayName,
        googlePhoto = account.photoUrl,
        socialProviderLabel = providerLabel,
        pendingPhone = phone.ifBlank { prefillPhone }.takeIf { it.isNotBlank() }
      )
    }

    AnimatedVisibility(visible = selectedRole == UserRole.CLIENTE) {
      ClientProfilePhotoPickerCard(
        photoUri = clientProfilePhotoUri,
        onPhotoSelected = { uri ->
          DocumentAttachmentHelper.clearPendingAuthReturn(context)
          clientProfilePhotoUri = uri
          clearErrors()
        },
        onClearPhoto = {
          clientProfilePhotoUri = null
          clearErrors()
        },
        onBeforeLaunchPicker = persistSocialAuthReturn
      )
    }

    AnimatedVisibility(visible = selectedRole == UserRole.PRESTADOR) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            shape = RoundedCornerShape(14.dp),
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
          label = { Text("Apresentação profissional") },
          minLines = 2,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth()
        )

        ProviderCameraProfilePhotoCard(
          photoUri = providerCameraProfilePhotoUri,
          onPhotoCaptured = { uri ->
            DocumentAttachmentHelper.clearPendingAuthReturn(context)
            providerCameraProfilePhotoUri = uri
            clearErrors()
          },
          onClearPhoto = {
            providerCameraProfilePhotoUri = null
            clearErrors()
          },
          onBeforeLaunchCamera = persistSocialAuthReturn
        )

        val attachedCount = listOf(
          rgCnhAttachmentUri,
          criminalAttachmentUri
        ).count { !it.isNullOrBlank() }

        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Documentação obrigatória para aprovação",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
              )
              Surface(
                color = if (attachedCount == 2) EmeraldWalletLight else WarningAmberLight,
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = "$attachedCount/2 anexos",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                  color = if (attachedCount == 2) EmeraldWallet else WarningAmber,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }

            ProviderDocumentAttachmentCard(
              title = "Anexo 1: RG ou CNH (Frente e Verso) *",
              subtitle = "Envie foto nítida ou PDF do documento oficial",
              fileName = rgCnhFileName,
              attachmentUri = rgCnhAttachmentUri,
              buttonLabel = "Anexar RG / CNH (Foto ou PDF)",
              buttonTag = "google_attach_rg_cnh",
              onAttachClick = {
                try {
                  rgPicker.launch(arrayOf("image/*", "application/pdf"))
                } catch (_: Throwable) {
                  Toast.makeText(context, "Não foi possível abrir o seletor de arquivos.", Toast.LENGTH_SHORT).show()
                }
              },
              onClearClick = {
                rgCnhFileName = ""
                rgCnhAttachmentUri = null
                clearErrors()
              }
            )

            ProviderDocumentAttachmentCard(
              title = "Anexo 2: Certidão de Antecedentes *",
              subtitle = "Envie o PDF emitido ou foto legível da certidão",
              fileName = criminalFileName,
              attachmentUri = criminalAttachmentUri,
              buttonLabel = "Anexar Certidão (Foto ou PDF)",
              buttonTag = "google_attach_criminal",
              onAttachClick = {
                try {
                  criminalPicker.launch(arrayOf("image/*", "application/pdf"))
                } catch (_: Throwable) {
                  Toast.makeText(context, "Não foi possível abrir o seletor de arquivos.", Toast.LENGTH_SHORT).show()
                }
              },
              onClearClick = {
                criminalFileName = ""
                criminalAttachmentUri = null
                clearErrors()
              }
            )
          }
        }
      }
    }

    val googleMissingItems = remember(
      selectedRole,
      account.email,
      phone,
      city,
      document,
      providerCameraProfilePhotoUri,
      rgCnhAttachmentUri,
      criminalAttachmentUri
    ) {
      if (selectedRole == UserRole.PRESTADOR) {
        CpfValidator.providerMissingRequirements(
          email = account.email,
          phone = phone,
          requirePhoneOnly = true,
          city = city,
          document = document,
          profilePhotoUri = providerCameraProfilePhotoUri,
          requireCameraProfilePhoto = true,
          rgOrCnhAttachmentUri = rgCnhAttachmentUri,
          criminalRecordAttachmentUri = criminalAttachmentUri
        )
      } else {
        CpfValidator.clientMissingRequirements(
          email = account.email,
          requireEmail = true,
          phone = phone,
          requirePhone = true,
          city = city,
          document = document
        )
      }
    }
    val canCompleteGoogleReg = googleMissingItems.isEmpty()

    val displayedError = localValidationError ?: externalErrorMessage
    if (!displayedError.isNullOrBlank()) {
      Surface(
        color = DangerRedLight,
        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("google_reg_validation_error")
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = DangerRed,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = displayedError,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF991B1B)
          )
        }
      }
    } else if (googleMissingItems.isNotEmpty()) {
      Surface(
        color = WarningAmberLight,
        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("google_reg_validation_error")
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = null,
              tint = WarningAmber,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Para concluir seu cadastro, preencha:",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = Color(0xFF92400E)
            )
          }
          Text(
            text = googleMissingItems.joinToString("\n") { "• $it" },
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF78350F)
          )
        }
      }
    }

    Button(
      onClick = {
        val err = ContactValidator.validateEmail(account.email)
          ?: ContactValidator.validatePhone(phone)
          ?: when {
            city.trim().length < 3 ->
              "Informe sua cidade e UF."
            selectedRole == UserRole.CLIENTE ->
              CpfValidator.validateClientCpf(document)
            else ->
              CpfValidator.validateProviderCpf(document) ?: when {
                providerCameraProfilePhotoUri.isNullOrBlank() ->
                  "Tire uma foto de perfil obrigatória pela câmera para concluir o cadastro do prestador."
                rgCnhAttachmentUri.isNullOrBlank() ->
                  "Anexe a foto ou PDF do seu RG ou CNH."
                criminalAttachmentUri.isNullOrBlank() ->
                  "Anexe a foto ou PDF da Certidão de Antecedentes Criminais."
                else -> null
              }
          }
        if (err != null) {
          localValidationError = err
        } else {
          localValidationError = null
          val chosenPhoto = if (selectedRole == UserRole.PRESTADOR) {
            providerCameraProfilePhotoUri
          } else {
            clientProfilePhotoUri
          }
          val submitCompletion: (String?, String?, String?) -> Unit = { resolvedPhoto, resolvedRg, resolvedCriminal ->
            onComplete(
              selectedRole,
              phone.trim(),
              city.trim(),
              document.trim(),
              category,
              bio.trim(),
              "",
              "",
              rgCnhFileName,
              resolvedRg,
              criminalFileName,
              resolvedCriminal,
              "",
              null,
              resolvedPhoto
            )
          }
          val hasLocalFiles = listOf(chosenPhoto, rgCnhAttachmentUri, criminalAttachmentUri)
            .any { it?.startsWith("file://", ignoreCase = true) == true }
          if (hasLocalFiles) {
            coroutineScope.launch {
              val resolvedPhoto = withContext(Dispatchers.IO) {
                DocumentAttachmentHelper.resolveForPersistence(context, chosenPhoto)
              }
              val resolvedRg = withContext(Dispatchers.IO) {
                DocumentAttachmentHelper.resolveForPersistence(context, rgCnhAttachmentUri)
              }
              val resolvedCriminal = withContext(Dispatchers.IO) {
                DocumentAttachmentHelper.resolveForPersistence(context, criminalAttachmentUri)
              }
              submitCompletion(resolvedPhoto, resolvedRg, resolvedCriminal)
            }
          } else {
            submitCompletion(chosenPhoto, rgCnhAttachmentUri, criminalAttachmentUri)
          }
        }
      },
      enabled = canCompleteGoogleReg,
      colors = ButtonDefaults.buttonColors(
        containerColor = if (selectedRole == UserRole.PRESTADOR) EmeraldWallet else MaterialTheme.colorScheme.primary
      ),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 52.dp)
        .testTag("auth_btn_complete_google_reg")
    ) {
      Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Concluir Acesso com $providerLabel",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
      )
    }
  }
}

