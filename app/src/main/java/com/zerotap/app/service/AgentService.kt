package com.zerotap.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/** Keeps the process alive while the agent operates other apps. State lives in AgentEngine. */
class AgentService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AgentNotifications.ensureChannels(this)
        ServiceCompat.startForeground(
            this,
            AgentNotifications.NOTIF_AGENT,
            AgentNotifications.agentNotification(this, "ZeroTap is working", "Completing your task…"),
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
        )
        return START_NOT_STICKY
    }

    companion object {
        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, AgentService::class.java))
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, AgentService::class.java)) }
        }
    }
}
