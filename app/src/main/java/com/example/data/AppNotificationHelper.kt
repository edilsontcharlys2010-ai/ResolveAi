package com.example.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.AppNotification
import com.example.model.NotificationType

object AppNotificationHelper {

  const val CHANNEL_ORDERS = "resolveai_orders"
  const val CHANNEL_MESSAGES = "resolveai_messages"
  const val CHANNEL_STATUS = "resolveai_status"

  const val EXTRA_SERVICE_ID = "extra_service_id"
  const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
  const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

  fun ensureChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    val channels = listOf(
      NotificationChannel(
        CHANNEL_ORDERS,
        "Novos Pedidos e Propostas",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Alertas de novas solicitações de serviço e propostas recebidas"
        enableVibration(true)
      },
      NotificationChannel(
        CHANNEL_MESSAGES,
        "Mensagens do Chat",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Mensagens em tempo real entre cliente e prestador"
        enableVibration(true)
      },
      NotificationChannel(
        CHANNEL_STATUS,
        "Aceitações e Atualizações",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Confirmações de aceitação, status do serviço, avaliações e documentos"
        enableVibration(true)
      }
    )
    manager.createNotificationChannels(channels)
  }

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
  }

  fun channelForType(type: NotificationType): String {
    return when (type) {
      NotificationType.NOVA_MENSAGEM -> CHANNEL_MESSAGES
      NotificationType.NOVO_SERVICO,
      NotificationType.PROPOSTA_RECEBIDA -> CHANNEL_ORDERS
      NotificationType.PROPOSTA_ACEITA,
      NotificationType.STATUS_ATUALIZADO,
      NotificationType.AVALIACAO_RECEBIDA,
      NotificationType.DOCUMENTO_APROVADO,
      NotificationType.DOCUMENTO_REJEITADO,
      NotificationType.PAGAMENTO_PIX_CONFIRMADO -> CHANNEL_STATUS
    }
  }

  fun showNotification(context: Context, notification: AppNotification) {
    try {
      ensureChannels(context)
      if (!hasNotificationPermission(context)) return

      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        notification.serviceId?.let { putExtra(EXTRA_SERVICE_ID, it) }
        putExtra(EXTRA_NOTIFICATION_TYPE, notification.type.name)
        putExtra(EXTRA_NOTIFICATION_ID, notification.id)
      }

      val requestCode = notification.id.hashCode()
      val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      } else {
        PendingIntent.FLAG_UPDATE_CURRENT
      }
      val pendingIntent = PendingIntent.getActivity(context, requestCode, intent, pendingIntentFlags)

      val builder = NotificationCompat.Builder(context, channelForType(notification.type))
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(notification.title)
        .setContentText(notification.message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)

      NotificationManagerCompat.from(context).notify(requestCode, builder.build())
    } catch (_: SecurityException) {
      // Usuário optou por não conceder permissão de notificações
    } catch (_: Throwable) {
      // Ambiente sem suporte ao NotificationManager (ex: testes JVM puros)
    }
  }
}
