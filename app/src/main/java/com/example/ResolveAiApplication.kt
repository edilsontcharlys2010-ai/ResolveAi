package com.example

import android.app.Application
import android.util.Log
import com.example.data.GoogleAuthManager
import com.google.firebase.FirebaseApp

/**
 * Classe Application principal do ResolveAi Serviços.
 * Garante a inicialização antecipada do FirebaseApp e do Firebase Authentication
 * antes da criação de Activities, ViewModels ou listeners do Firestore.
 */
class ResolveAiApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    try {
      if (FirebaseApp.getApps(this).isEmpty()) {
        FirebaseApp.initializeApp(this)
      }
      val auth = GoogleAuthManager.ensureFirebaseAuthInitialized(this)
      Log.d(TAG, "Firebase Authentication inicializado (sessão ativa=${auth?.currentUser != null}).")
    } catch (e: Throwable) {
      Log.w(TAG, "Inicialização do Firebase Authentication ignorada neste ambiente: ${e.message}")
    }
  }

  private companion object {
    const val TAG = "ResolveAiApplication"
  }
}
