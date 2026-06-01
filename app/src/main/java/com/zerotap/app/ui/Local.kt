package com.zerotap.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import com.zerotap.app.AppContainer

val LocalContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}

enum class Tab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.AutoAwesome),
    MISSION("Mission", Icons.Rounded.Bolt),
    MEMORY("Memory", Icons.Rounded.Bookmarks),
    SETTINGS("Settings", Icons.Rounded.Tune)
}

/** All the system-level actions the UI can trigger, supplied by the host Activity. */
data class ZeroTapActions(
    val onRequestProjection: () -> Unit,
    val onRequestNotifications: () -> Unit,
    val onOpenAccessibility: () -> Unit,
    val onOpenAppSettings: () -> Unit,
    val goToTab: (Tab) -> Unit
)
