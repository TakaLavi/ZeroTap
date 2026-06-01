package com.zerotap.app.agent

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.zerotap.app.accessibility.AccessibilityBridge
import com.zerotap.app.core.ActionType
import com.zerotap.app.core.NextAction
import kotlinx.coroutines.delay

data class ActionOutcome(val success: Boolean, val detail: String)

/** Translates a model [NextAction] into a concrete operation on the device. */
class ActionExecutor(private val context: Context) {

    suspend fun execute(action: NextAction): ActionOutcome {
        val svc = AccessibilityBridge.service
        return when (action.type) {
            ActionType.OPEN_APP -> openApp(action.target)
            ActionType.OPEN_URL -> openUrl(action.target)
            ActionType.SEARCH -> webSearch(action.target.ifBlank { action.value })
            ActionType.TAP -> {
                if (svc == null) return notReady()
                val ok = svc.tapByText(action.target.ifBlank { action.value })
                ActionOutcome(ok, if (ok) "Tapped \"${action.target}\"" else "Could not find \"${action.target}\"")
            }
            ActionType.INPUT_TEXT -> {
                if (svc == null) return notReady()
                val text = action.value.ifBlank { action.target }
                val ok = svc.inputText(text)
                ActionOutcome(ok, if (ok) "Typed \"$text\"" else "No input field found")
            }
            ActionType.SCROLL -> {
                if (svc == null) return notReady()
                val down = !action.value.lowercase().contains("up")
                val ok = svc.scroll(down)
                ActionOutcome(ok, if (down) "Scrolled down" else "Scrolled up")
            }
            ActionType.SWIPE -> {
                if (svc == null) return notReady()
                val m = context.resources.displayMetrics
                val cx = m.widthPixels / 2
                val ok = svc.swipe(cx, (m.heightPixels * 0.7).toInt(), cx, (m.heightPixels * 0.3).toInt())
                ActionOutcome(ok, "Swiped")
            }
            ActionType.BACK -> ActionOutcome(svc?.goBack() == true, "Back")
            ActionType.HOME -> ActionOutcome(svc?.goHome() == true, "Home")
            ActionType.RECENTS -> ActionOutcome(svc?.openRecents() == true, "Recents")
            ActionType.WAIT -> {
                delay(1200)
                ActionOutcome(true, "Waited for screen")
            }
            ActionType.EXTRACT -> ActionOutcome(true, "Read screen")
            ActionType.COMPLETE -> ActionOutcome(true, "Task complete")
            ActionType.ASK -> ActionOutcome(true, "Awaiting user input")
            ActionType.UNKNOWN -> ActionOutcome(false, "Unknown action")
        }
    }

    private fun notReady() =
        ActionOutcome(false, "Accessibility service is not enabled")

    private fun openApp(name: String): ActionOutcome {
        if (name.isBlank()) return ActionOutcome(false, "No app name")
        val pm = context.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(main, 0)
        val query = name.lowercase()
        val match = apps.firstOrNull { it.loadLabel(pm).toString().lowercase() == query }
            ?: apps.firstOrNull { it.loadLabel(pm).toString().lowercase().contains(query) }
        val pkg = match?.activityInfo?.packageName
            ?: return ActionOutcome(false, "App \"$name\" not installed")
        val launch = pm.getLaunchIntentForPackage(pkg)
            ?: return ActionOutcome(false, "Cannot launch \"$name\"")
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launch)
            ActionOutcome(true, "Opened $name")
        } catch (e: Exception) {
            ActionOutcome(false, "Failed to open $name: ${e.message}")
        }
    }

    private fun openUrl(url: String): ActionOutcome {
        val normalized = when {
            url.isBlank() -> return ActionOutcome(false, "No URL")
            url.startsWith("http") -> url
            else -> "https://$url"
        }
        return launchView(Uri.parse(normalized), "Opened $normalized")
    }

    private fun webSearch(query: String): ActionOutcome {
        if (query.isBlank()) return ActionOutcome(false, "Empty search")
        val uri = Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
        return launchView(uri, "Searched the web for \"$query\"")
    }

    private fun launchView(uri: Uri, success: String): ActionOutcome {
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            ActionOutcome(true, success)
        } catch (e: Exception) {
            ActionOutcome(false, "No app to open ${uri}: ${e.message}")
        }
    }
}
