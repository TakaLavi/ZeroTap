package com.zerotap.app.ui.components

import androidx.compose.ui.graphics.Color
import com.zerotap.app.core.RunStatus
import com.zerotap.app.ui.theme.ZtAmber
import com.zerotap.app.ui.theme.ZtBlue
import com.zerotap.app.ui.theme.ZtGreen
import com.zerotap.app.ui.theme.ZtInkFaint
import com.zerotap.app.ui.theme.ZtRose
import com.zerotap.app.ui.theme.ZtTeal

fun relativeTime(ts: Long): String {
    if (ts <= 0L) return ""
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000L -> "just now"
        diff < 3_600_000L -> "${diff / 60_000L}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000L}h ago"
        else -> "${diff / 86_400_000L}d ago"
    }
}

fun runStatusLabel(status: RunStatus): String = when (status) {
    RunStatus.IDLE -> "Idle"
    RunStatus.PLANNING -> "Planning"
    RunStatus.RUNNING -> "Running"
    RunStatus.WAITING_CONFIRMATION -> "Needs you"
    RunStatus.PAUSED -> "Paused"
    RunStatus.COMPLETED -> "Completed"
    RunStatus.FAILED -> "Failed"
    RunStatus.BLOCKED -> "Blocked"
}

fun runStatusColor(status: RunStatus): Color = when (status) {
    RunStatus.IDLE -> ZtInkFaint
    RunStatus.PLANNING -> ZtBlue
    RunStatus.RUNNING -> ZtTeal
    RunStatus.WAITING_CONFIRMATION -> ZtAmber
    RunStatus.PAUSED -> ZtInkFaint
    RunStatus.COMPLETED -> ZtGreen
    RunStatus.FAILED -> ZtRose
    RunStatus.BLOCKED -> ZtAmber
}
