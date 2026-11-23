package com.example.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class GoogleAccountInfo(
  val uid: String,
  val email: String,
  val displayName: String,
  val photoUrl: String? = null
)

/**
 * Gerenciador oficial de inicialização do Firebase Authentication e Login/Cadastro
 * com Conta Google via Jetpack Credential Manager.
 */
object GoogleAuthManager {

  private const val TAG = "GoogleAuthManager"

  /**
   * Inicializa com segurança o FirebaseApp e o Firebase Authentication no contexto fornecido.
   */
  fun ensureFirebaseAuthInitialized(context: Context): FirebaseAuth? {
    return try {
      val appCtx = context.applicationContext ?: context
      if (FirebaseApp.getApps(appCtx).isEmpty()) {
        FirebaseApp.initializeApp(appCtx)
      }
      Firebase.auth.apply {
        setLanguageCode("pt-BR")
      }
    } catch (e: Throwable) {
      Log.w(TAG, "Não foi possível inicializar FirebaseAuth: ${e.message}")
      null
    }
  }

  /**
   * Converte o FirebaseAuth.AuthStateListener em um Flow reativo para observar alterações de sessão.
   */
  fun authStateFlow(auth: FirebaseAuth? = runCatching { Firebase.auth }.getOrNull()): Flow<FirebaseUser?> {
    if (auth == null) return flowOf(null)
    return callbackFlow {
      val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
      }
      auth.addAuthStateListener(listener)
      awaitClose { auth.removeAuthStateListener(listener) }
    }
  }

  fun FirebaseUser.toGoogleAccountInfo(): GoogleAccountInfo {
    val resolvedEmail = email?.takeIf { it.isNotBlank() }
      ?: "user_${uid}@auth.resolveai.app"
    val resolvedName = displayName?.takeIf { it.isNotBlank() }
      ?: resolvedEmail.substringBefore("@")
    return GoogleAccountInfo(
      uid = uid,
      email = resolvedEmail,
      displayName = resolvedName,
      photoUrl = photoUrl?.toString()
    )
  }

  private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }

  /**
   * Tenta login silencioso ao iniciar o app usando conta Google já autorizada.
   * Utiliza GetGoogleIdOption isolado em sua própria GetCredentialRequest.
   */
  fun attemptAutoSignIn(
    context: Context,
    scope: CoroutineScope,
    onAuthSuccess: (GoogleAccountInfo) -> Unit,
    onUnauthenticated: () -> Unit
  ) {
    val auth = ensureFirebaseAuthInitialized(context)
    val existingFirebaseUser = try {
      auth?.currentUser
    } catch (_: Throwable) {
      null
    }

    if (existingFirebaseUser != null) {
      onAuthSuccess(existingFirebaseUser.toGoogleAccountInfo())
      return
    }

    if (auth == null) {
      onUnauthenticated()
      return
    }

    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      Log.w(TAG, "default_web_client_id ausente para auto sign-in: ${e.message}")
      onUnauthenticated()
      return
    }

    val credentialManager = CredentialManager.create(context)
    val googleIdOption = GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

    val request = GetCredentialRequest.Builder()
      .addCredentialOption(googleIdOption)
      .build()

    val hostContext: Context = context.findActivity() ?: context
    scope.launch {
      try {
        val result = credentialManager.getCredential(hostContext, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken.idToken, null)
          val authResult = auth.signInWithCredential(authCredential).await()
          val fbUser = authResult.user
          val email = fbUser?.email ?: googleIdToken.id
          val name = fbUser?.displayName?.takeIf { it.isNotBlank() }
            ?: googleIdToken.displayName?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@")
          if (fbUser != null && email.isNotBlank()) {
            onAuthSuccess(
              GoogleAccountInfo(
                uid = fbUser.uid,
                email = email,
                displayName = name,
                photoUrl = fbUser.photoUrl?.toString()
              )
            )
          } else {
            onUnauthenticated()
          }
        } else {
          onUnauthenticated()
        }
      } catch (e: Exception) {
        Log.d(TAG, "Auto sign-in silencioso não aplicado: ${e.message}")
        onUnauthenticated()
      }
    }
  }

  /**
   * Inicia o fluxo interativo oficial de Login/Cadastro com Google via Credential Manager
   * e autentica no Firebase Authentication com GoogleAuthProvider.
   */
  fun onGoogleSignInClicked(
    context: Context,
    scope: CoroutineScope,
    onAuthSuccess: (GoogleAccountInfo) -> Unit,
    onAuthError: (String) -> Unit,
    onAuthCancelled: () -> Unit = {}
  ) {
    val auth = ensureFirebaseAuthInitialized(context)
    if (auth == null) {
      onAuthError("Firebase Authentication não pôde ser inicializado neste dispositivo.")
      return
    }

    val activity = context.findActivity()
    if (activity == null) {
      onAuthError("Contexto de tela inválido para abrir seletor de contas Google.")
      return
    }

    val clientId = try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      onAuthError("Configuração Google Sign-In ausente (default_web_client_id não encontrado).")
      return
    }

    val credentialManager = CredentialManager.create(activity)
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
      .addCredentialOption(signInOption)
      .build()

    scope.launch {
      try {
        val result = credentialManager.getCredential(activity, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken.idToken, null)
          val authResult = auth.signInWithCredential(authCredential).await()
          val fbUser = authResult.user
          val email = fbUser?.email ?: googleIdToken.id
          val displayName = fbUser?.displayName?.takeIf { it.isNotBlank() }
            ?: googleIdToken.displayName?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@")

          if (fbUser != null && email.isNotBlank()) {
            onAuthSuccess(
              GoogleAccountInfo(
                uid = fbUser.uid,
                email = email,
                displayName = displayName,
                photoUrl = fbUser.photoUrl?.toString()
              )
            )
          } else {
            onAuthError("Conta Google autenticada, mas não foi possível obter o e-mail do usuário.")
          }
        } else {
          onAuthError("Tipo de credencial inesperado retornado pelo Google.")
        }
      } catch (e: GetCredentialCancellationException) {
        Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
        onAuthCancelled()
      } catch (e: NoCredentialException) {
        Log.w("Auth", "Nenhuma conta Google disponível no dispositivo: ${e.message}", e)
        onAuthError("Nenhuma conta Google encontrada neste aparelho/emulador. Adicione uma conta Google nas Configurações do Android e tente novamente.")
      } catch (e: Exception) {
        Log.e("Auth", "Google Sign-In failed", e)
        val msg = FirebaseAuthHelper.mapFirebaseAuthException(e, contextLabel = "Google")
        onAuthError(msg)
      }
    }
  }

  /**
   * Encerra a sessão limpando o estado do Credential Manager ANTES do signOut do Firebase,
   * protegido em NonCancellable para evitar interrupção ao desmontar a tela.
   */
  fun signOut(
    context: Context,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
  ) {
    val credentialManager = CredentialManager.create(context)
    scope.launch {
      withContext(NonCancellable) {
        try {
          credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
          Log.w("Auth", "Failed to clear credential state: ${e.message}")
        } finally {
          try {
            Firebase.auth.signOut()
          } catch (_: Throwable) {
          }
          onSignOutComplete()
        }
      }
    }
  }
}
