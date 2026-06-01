package com.zerotap.app.accessibility

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The accessibility service is created by the system, so the rest of the app reaches it
 * through this process-wide bridge rather than by construction.
 */
object AccessibilityBridge {

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    @Volatile
    var service: ZeroTapAccessibilityService? = null
        private set

    val isReady: Boolean get() = service != null

    fun onConnected(s: ZeroTapAccessibilityService) {
        service = s
        _connected.value = true
    }

    fun onDisconnected() {
        service = null
        _connected.value = false
    }
}
