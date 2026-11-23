package com.example.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object SupportLauncher {
  const val SUPPORT_WHATSAPP_NUMBER_E164 = "5561992907464"
  const val SUPPORT_WHATSAPP_DISPLAY = "+55 (61) 99290-7464"

  @Volatile
  var activeWhatsappNumberE164: String = SUPPORT_WHATSAPP_NUMBER_E164
    private set

  @Volatile
  var activeWhatsappDisplay: String = SUPPORT_WHATSAPP_DISPLAY
    private set

  fun updateSupportContact(numberE164: String?, display: String?) {
    val digits = numberE164?.filter { it.isDigit() }.orEmpty()
    activeWhatsappNumberE164 = if (digits.length >= 10) digits else SUPPORT_WHATSAPP_NUMBER_E164
    activeWhatsappDisplay = display?.trim()?.takeIf { it.isNotBlank() } ?: SUPPORT_WHATSAPP_DISPLAY
  }

  fun buildWhatsAppUrl(
    message: String = "Olá! Preciso de atendimento do suporte ResolveAi Serviços."
  ): String {
    val cleanMessage = message.trim().ifBlank {
      "Olá! Preciso de atendimento do suporte ResolveAi Serviços."
    }
    return "https://wa.me/$activeWhatsappNumberE164?text=${Uri.encode(cleanMessage)}"
  }

  fun openWhatsAppSupport(
    context: Context,
    customMessage: String? = null
  ): Boolean {
    val url = buildWhatsAppUrl(
      customMessage ?: "Olá! Preciso de atendimento do suporte ResolveAi Serviços."
    )
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
      context.startActivity(intent)
      true
    } catch (_: ActivityNotFoundException) {
      Toast.makeText(
        context,
        "WhatsApp/navegador não encontrado. Contato: $activeWhatsappDisplay",
        Toast.LENGTH_LONG
      ).show()
      false
    } catch (_: Exception) {
      false
    }
  }
}
