package com.zerotap.app.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** High level classification of what the user asked for. */
@Serializable
enum class TaskType {
    @SerialName("messaging") MESSAGING,
    @SerialName("shopping") SHOPPING,
    @SerialName("search") SEARCH,
    @SerialName("navigation") NAVIGATION,
    @SerialName("utility") UTILITY,
    @SerialName("unknown") UNKNOWN;
}

/** The concrete operation the agent wants to perform next. */
@Serializable
enum class ActionType {
    @SerialName("open_app") OPEN_APP,
    @SerialName("open_url") OPEN_URL,
    @SerialName("search") SEARCH,
    @SerialName("tap") TAP,
    @SerialName("input_text") INPUT_TEXT,
    @SerialName("scroll") SCROLL,
    @SerialName("swipe") SWIPE,
    @SerialName("back") BACK,
    @SerialName("home") HOME,
    @SerialName("recents") RECENTS,
    @SerialName("wait") WAIT,
    @SerialName("extract") EXTRACT,
    @SerialName("complete") COMPLETE,
    @SerialName("ask") ASK,
    @SerialName("unknown") UNKNOWN;

    /** Actions that change the world in a way that is hard or impossible to undo. */
    val isSensitive: Boolean
        get() = false
}

/** A single planned action returned by the reasoning model. */
@Serializable
data class NextAction(
    val type: ActionType = ActionType.UNKNOWN,
    val target: String = "",
    val value: String = "",
    val reason: String = "",
    @SerialName("expected_result") val expectedResult: String = ""
)

/** Full structured decision emitted by the planning model for one turn. */
@Serializable
data class AgentDecision(
    val goal: String = "",
    @SerialName("task_type") val taskType: TaskType = TaskType.UNKNOWN,
    val understanding: String = "",
    val plan: List<String> = emptyList(),
    @SerialName("next_action") val nextAction: NextAction = NextAction(),
    val confidence: Double = 0.0,
    @SerialName("needs_confirmation") val needsConfirmation: Boolean = false,
    val blocker: String? = null
)

/** One rectangle on screen, in absolute pixels. */
data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/** A flattened, model-friendly description of one interactive element. */
data class UiNode(
    val index: Int,
    val text: String,
    val contentDescription: String,
    val className: String,
    val viewId: String,
    val clickable: Boolean,
    val editable: Boolean,
    val scrollable: Boolean,
    val focused: Boolean,
    val bounds: Bounds
) {
    val label: String
        get() = when {
            text.isNotBlank() -> text
            contentDescription.isNotBlank() -> contentDescription
            viewId.isNotBlank() -> viewId.substringAfterLast('/')
            else -> className.substringAfterLast('.')
        }
}

/** A snapshot of what is currently on screen. */
data class ScreenSnapshot(
    val packageName: String,
    val activity: String,
    val timestamp: Long,
    val nodes: List<UiNode>,
    val ocrText: String = "",
    val source: ScreenSource = ScreenSource.ACCESSIBILITY
) {
    val isEmpty: Boolean get() = nodes.isEmpty() && ocrText.isBlank()
}

enum class ScreenSource { ACCESSIBILITY, OCR, VISION, HEURISTIC }

/** Status of one item in the mission timeline. */
enum class StepStatus { PENDING, ACTIVE, DONE, FAILED, SKIPPED }

data class TimelineStep(
    val id: Long,
    val label: String,
    val status: StepStatus = StepStatus.PENDING
)

/** A record of one executed action, surfaced in the action log. */
data class ActionRecord(
    val id: Long,
    val timestamp: Long,
    val actionType: ActionType,
    val target: String,
    val success: Boolean,
    val detail: String
)

/** Overall lifecycle of an agent run. */
enum class RunStatus {
    IDLE, PLANNING, RUNNING, WAITING_CONFIRMATION, PAUSED, COMPLETED, FAILED, BLOCKED
}

/** A request for explicit user sign-off before an irreversible action. */
data class ConfirmationRequest(
    val id: Long,
    val title: String,
    val summary: String,
    val detail: String
)

/** The single observable state of the current (or last) mission. */
data class MissionState(
    val goal: String = "",
    val taskType: TaskType = TaskType.UNKNOWN,
    val understanding: String = "",
    val status: RunStatus = RunStatus.IDLE,
    val plan: List<String> = emptyList(),
    val timeline: List<TimelineStep> = emptyList(),
    val currentObjective: String = "",
    val currentApp: String = "",
    val currentScreen: String = "",
    val currentAction: String = "",
    val actionLog: List<ActionRecord> = emptyList(),
    val confidence: Double = 0.0,
    val progress: Float = 0f,
    val blocker: String? = null,
    val confirmation: ConfirmationRequest? = null,
    val memoryRefs: List<String> = emptyList(),
    val startedAt: Long = 0L,
    val finishedAt: Long = 0L
) {
    val isActive: Boolean
        get() = status == RunStatus.PLANNING ||
            status == RunStatus.RUNNING ||
            status == RunStatus.WAITING_CONFIRMATION ||
            status == RunStatus.PAUSED
}

/** Categories the memory engine can store. */
enum class MemoryKind {
    PERSON, PLACE, FOOD, PRODUCT, BRAND, STORE, APP, WEBSITE, TASK, HABIT, PREFERENCE, SEARCH, DECISION
}

/** A single durable memory the agent has learned about the user. */
data class MemoryItem(
    val id: Long = 0,
    val kind: MemoryKind,
    val key: String,
    val value: String,
    val weight: Double = 1.0,
    val pinned: Boolean = false,
    val locked: Boolean = false,
    val useCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/** A ranked shopping result the researcher agent produced. */
data class ProductCard(
    val title: String,
    val source: String,
    val price: String,
    val rating: String,
    val url: String,
    val pros: List<String> = emptyList(),
    val cons: List<String> = emptyList(),
    val score: Double = 0.0
)

/** Actions that must never run without explicit user confirmation. */
object Safety {
    val sensitiveKeywords = listOf(
        "send", "pay", "buy", "purchase", "order", "checkout", "confirm order",
        "place order", "delete", "remove account", "post", "publish", "tweet",
        "transfer", "subscribe", "unsubscribe", "sign out", "log out",
        "change password", "submit payment"
    )

    fun requiresConfirmation(action: NextAction): Boolean {
        if (action.type == ActionType.ASK) return false
        val haystack = (action.target + " " + action.value + " " + action.reason).lowercase()
        return sensitiveKeywords.any { haystack.contains(it) }
    }
}
