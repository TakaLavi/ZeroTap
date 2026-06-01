package com.zerotap.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.zerotap.app.MainActivity
import com.zerotap.app.R

object AgentNotifications {

    const val CHANNEL_AGENT = "zerotap_agent"
    const val CHANNEL_CAPTURE = "zerotap_capture"
    const val NOTIF_AGENT = 1001
    const val NOTIF_CAPTURE = 1002

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_AGENT,
                "ZeroTap Agent",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shown while ZeroTap is completing a task." }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CAPTURE,
                "Screen Capture",
                NotificationManager.IMPORTANCE_MIN
            ).apply { description = "Active while ZeroTap can see the screen." }
        )
    }

    private fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun agentNotification(context: Context, title: String, text: String): Notification =
        Notification.Builder(context, CHANNEL_AGENT)
            .setSmallIcon(R.drawable.ic_stat_zerotap)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(contentIntent(context))
            .build()

    fun captureNotification(context: Context): Notification =
        Notification.Builder(context, CHANNEL_CAPTURE)
            .setSmallIcon(R.drawable.ic_stat_zerotap)
            .setContentTitle("ZeroTap vision active")
            .setContentText("ZeroTap can read the screen to complete your task.")
            .setOngoing(true)
            .setContentIntent(contentIntent(context))
            .build()
}
