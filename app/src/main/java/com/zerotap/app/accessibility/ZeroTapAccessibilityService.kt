package com.zerotap.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.zerotap.app.core.Bounds
import com.zerotap.app.core.ScreenSnapshot
import com.zerotap.app.core.ScreenSource
import com.zerotap.app.core.UiNode
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * The hands and eyes of ZeroTap: reads the live view hierarchy and performs
 * taps, swipes, scrolls and text entry on the user's behalf.
 */
class ZeroTapAccessibilityService : AccessibilityService() {

    @Volatile private var lastPackage: String = ""
    @Volatile private var lastClass: String = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityBridge.onConnected(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.let { lastPackage = it.toString() }
            event.className?.let { lastClass = it.toString() }
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        AccessibilityBridge.onDisconnected()
        return super.onUnbind(intent)
    }

    // ---- Reading -------------------------------------------------------------

    fun snapshot(): ScreenSnapshot {
        val root = rootInActiveWindow
        val nodes = ArrayList<UiNode>()
        val textDump = StringBuilder()
        if (root != null) {
            traverse(root, 0, nodes, textDump)
        }
        return ScreenSnapshot(
            packageName = root?.packageName?.toString() ?: lastPackage,
            activity = lastClass,
            timestamp = System.currentTimeMillis(),
            nodes = nodes,
            ocrText = textDump.toString().trim(),
            source = ScreenSource.ACCESSIBILITY
        )
    }

    private fun traverse(
        node: AccessibilityNodeInfo,
        depth: Int,
        out: MutableList<UiNode>,
        textDump: StringBuilder
    ) {
        if (out.size >= MAX_NODES || depth > MAX_DEPTH) return

        val text = node.text?.toString().orEmpty().trim()
        val desc = node.contentDescription?.toString().orEmpty().trim()
        val interesting = node.isClickable || node.isEditable || node.isScrollable ||
            text.isNotEmpty() || desc.isNotEmpty()

        if (interesting) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0) {
                out.add(
                    UiNode(
                        index = out.size,
                        text = text.take(120),
                        contentDescription = desc.take(120),
                        className = node.className?.toString().orEmpty(),
                        viewId = node.viewIdResourceName.orEmpty(),
                        clickable = node.isClickable,
                        editable = node.isEditable,
                        scrollable = node.isScrollable,
                        focused = node.isFocused,
                        bounds = Bounds(rect.left, rect.top, rect.right, rect.bottom)
                    )
                )
                if (text.isNotEmpty()) textDump.append(text).append('\n')
            }
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { traverse(it, depth + 1, out, textDump) }
        }
    }

    val currentPackage: String get() = lastPackage

    // ---- Acting --------------------------------------------------------------

    fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun openRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)

    suspend fun tapAt(x: Int, y: Int): Boolean {
        val path = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
            lineTo(x.toFloat() + 1f, y.toFloat() + 1f)
        }
        return dispatch(path, 60L)
    }

    suspend fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long = 320L): Boolean {
        val path = Path().apply {
            moveTo(x1.toFloat(), y1.toFloat())
            lineTo(x2.toFloat(), y2.toFloat())
        }
        return dispatch(path, durationMs)
    }

    /** Find a node by visible text or description and click it (or tap its centre). */
    suspend fun tapByText(query: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val match = findByText(root, query.lowercase()) ?: return false
        val clickable = nearestClickable(match)
        if (clickable != null && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true
        }
        val rect = Rect()
        match.getBoundsInScreen(rect)
        return if (rect.width() > 0 && rect.height() > 0) {
            tapAt(rect.centerX(), rect.centerY())
        } else {
            false
        }
    }

    fun inputText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val field = findFocusedEditable(root) ?: findEditable(root) ?: return false
        field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun scroll(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollable = findScrollable(root) ?: return false
        val action = if (forward) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }
        return scrollable.performAction(action)
    }

    // ---- Node search helpers -------------------------------------------------

    private fun findByText(node: AccessibilityNodeInfo, query: String): AccessibilityNodeInfo? {
        val text = node.text?.toString()?.lowercase().orEmpty()
        val desc = node.contentDescription?.toString()?.lowercase().orEmpty()
        if (query.isNotEmpty() && (text.contains(query) || desc.contains(query))) return node
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findByText(child, query)?.let { return it }
            }
        }
        return null
    }

    private fun nearestClickable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var current = node
        var hops = 0
        while (current != null && hops < 6) {
            if (current.isClickable) return current
            current = current.parent
            hops++
        }
        return null
    }

    private fun findFocusedEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isFocused) return node
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findFocusedEditable(it)?.let { f -> return f } }
        }
        return null
    }

    private fun findEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findEditable(it)?.let { f -> return f } }
        }
        return null
    }

    private fun findScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findScrollable(it)?.let { f -> return f } }
        }
        return null
    }

    private suspend fun dispatch(path: Path, durationMs: Long): Boolean =
        suspendCancellableCoroutine { cont ->
            val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            val dispatched = dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(g: GestureDescription?) {
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onCancelled(g: GestureDescription?) {
                        if (cont.isActive) cont.resume(false)
                    }
                },
                null
            )
            if (!dispatched && cont.isActive) cont.resume(false)
        }

    companion object {
        private const val MAX_NODES = 90
        private const val MAX_DEPTH = 40
    }
}
