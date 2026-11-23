package com.example.data

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"

/**
 * Configura o Firebase App Check protegendo Firestore e Authentication contra acesso não autorizado:
 * - Release (BuildConfig.DEBUG == false): utiliza PlayIntegrityAppCheckProviderFactory (Google Play Integrity).
 * - Debug/Emulador (BuildConfig.DEBUG == true): utiliza DebugAppCheckProviderFactory com gravação síncrona (.commit()).
 */
fun configureAppCheck(context: Context, intent: Intent?) {
  try {
    if (FirebaseApp.getApps(context).isEmpty()) {
      FirebaseApp.initializeApp(context)
    }

    val firebaseAppCheck = FirebaseAppCheck.getInstance()
    firebaseAppCheck.setTokenAutoRefreshEnabled(true)

    if (BuildConfig.DEBUG) {
      val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
      if (!debugToken.isNullOrEmpty()) {
        val persistenceKey = FirebaseApp.getInstance().persistenceKey
        val prefs = context.getSharedPreferences(
          "$DEBUG_STORE_PREFIX$persistenceKey",
          Context.MODE_PRIVATE
        )
        // commit() síncrono garante que o token esteja gravado antes da leitura do DebugAppCheckProviderFactory
        prefs.edit()
          .putString(DEBUG_SECRET_KEY, debugToken)
          .commit()
        Log.d(TAG, "App Check debug secret configurado com sucesso.")
      } else {
        Log.d(TAG, "Nenhum token de debug extra no Intent; usando token padrão do DebugAppCheckProviderFactory.")
      }

      firebaseAppCheck.installAppCheckProviderFactory(
        DebugAppCheckProviderFactory.getInstance()
      )
      Log.d(TAG, "DebugAppCheckProviderFactory instalado (modo debug/emulador).")
    } else {
      firebaseAppCheck.installAppCheckProviderFactory(
        PlayIntegrityAppCheckProviderFactory.getInstance()
      )
      Log.d(TAG, "PlayIntegrityAppCheckProviderFactory instalado com sucesso (produção).")
    }
  } catch (e: Throwable) {
    Log.e(TAG, "Falha ao inicializar Firebase App Check", e)
  }
}
