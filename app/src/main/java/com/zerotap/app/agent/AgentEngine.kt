package com.zerotap.app.agent

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Base64
import com.zerotap.app.accessibility.AccessibilityBridge
import com.zerotap.app.api.ApiConfig
import com.zerotap.app.api.ChatTurn
import com.zerotap.app.api.LlmResult
import com.zerotap.app.api.MiMoClient
import com.zerotap.app.capture.ScreenCaptureManager
import com.zerotap.app.core.ActionRecord
import com.zerotap.app.core.ActionType
import com.zerotap.app.core.AgentDecision
import com.zerotap.app.core.ConfirmationRequest
import com.zerotap.app.core.MemoryKind
import com.zerotap.app.core.MissionState
import com.zerotap.app.core.NextAction
import com.zerotap.app.core.RunStatus
import com.zerotap.app.core.Safety
import com.zerotap.app.core.ScreenSnapshot
import com.zerotap.app.core.ScreenSource
import com.zerotap.app.core.StepStatus
import com.zerotap.app.core.TimelineStep
import com.zerotap.app.core.parseDecision
import com.zerotap.app.data.memory.MemoryRepository
import com.zerotap.app.data.memory.TaskHistoryRepository
import com.zerotap.app.data.settings.SettingsRepository
import com.zerotap.app.logging.TraceLog
import com.zerotap.app.service.AgentService
import com.zerotap.app.vision.Ocr
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * The autonomous loop: plan, inspect the screen, decide one action, execute it, verify, repeat.
 * Exposes a single [MissionState] the whole UI observes.
 */
class AgentEngine(
    private val appContext: Context,
    private val mimo: MiMoClient,
    private val settings: SettingsRepository,
    private val memory: MemoryRepository,
    private val history: TaskHistoryRepository,
    private val executor: ActionExecutor,
    private val capture: ScreenCaptureManager,
    private val ocr: Ocr
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(MissionState())
    val state: StateFlow<MissionState> = _state.asStateFlow()

    private var job: Job? = null
    private var confirmation: CompletableDeferred<Boolean>? = null
    private val recentActions = mutableListOf<String>()

    fun start(goal: String) {
        if (goal.isBlank() || _state.value.isActive) return
        recentActions.clear()
        TraceLog.clear()
        job?.cancel()
        job = scope.launch { runMission(goal.trim()) }
    }

    fun stop() {
        job?.cancel()
        confirmation?.complete(false)
        confirmation = null
        if (_state.value.isActive) {
            _state.value = _state.value.copy(
                status = RunStatus.PAUSED,
                finishedAt = now(),
                currentAction = "Stopped by user"
            )
        }
        AgentService.stop(appContext)
    }

    fun confirm(approved: Boolean) {
        confirmation?.complete(approved)
    }

    fun followUp(text: String) {
        if (!_state.value.isActive) start(text)
    }

    fun reset() {
        if (!_state.value.isActive) _state.value = MissionState()
    }

    private suspend fun runMission(goal: String) {
        AgentService.start(appContext)
        _state.value = MissionState(
            goal = goal,
            status = RunStatus.PLANNING,
            startedAt = now(),
            currentAction = "Understanding your request"
        )

        val config = settings.config.first()
        if (!config.isReady) {
            fail("Add your MiMo API key in Settings to start a task.")
            return
        }

        val mem = runCatching { memory.contextBlock() }.getOrDefault("")
        val device = "Android ${Build.VERSION.RELEASE} • ${Build.MANUFACTURER} ${Build.MODEL}"

        val planResult = mimo.chat(
            config,
            listOf(
                ChatTurn.system(Prompts.PLANNER_SYSTEM),
                ChatTurn.user(Prompts.initialPlan(goal, mem, device))
            )
        )
        val decision = (planResult as? LlmResult.Ok)?.let { parseDecision(it.content) }
        if (decision == null) {
            fail((planResult as? LlmResult.Err)?.message ?: "Could not understand the task.")
            return
        }

        val plan = decision.plan.ifEmpty { listOf("Work toward: $goal") }
        val historyId = runCatching { history.start(goal, decision.taskType.name) }.getOrDefault(0L)

        update {
            it.copy(
                taskType = decision.taskType,
                understanding = decision.understanding,
                plan = plan,
                timeline = plan.mapIndexed { i, label -> TimelineStep(i.toLong(), label) },
                status = RunStatus.RUNNING,
                currentObjective = plan.firstOrNull().orEmpty(),
                memoryRefs = mem.lines().filter { l -> l.startsWith("- ") }
                    .take(4).map { l -> l.removePrefix("- ").substringBefore(":").trim() }
            )
        }
        learnFromGoal(goal, decision)

        var iteration = 0
        var completed = false
        while (iteration < MAX_STEPS && _state.value.status == RunStatus.RUNNING) {
            iteration++

            val snapshot = readScreen(config, goal)
            update {
                it.copy(
                    currentApp = friendlyApp(snapshot.packageName),
                    currentScreen = snapshot.activity.substringAfterLast('.')
                )
            }

            val stepResult = mimo.chat(
                config,
                listOf(
                    ChatTurn.system(Prompts.PLANNER_SYSTEM),
                    ChatTurn.user(
                        Prompts.step(goal, decision.taskType.name, plan, recentActions, snapshot, mem, iteration)
                    )
                )
            )
            if (stepResult is LlmResult.Err && stepResult.code == 401) {
                fail("API key was rejected (401). Check it in Settings.")
                return
            }
            val step = (stepResult as? LlmResult.Ok)?.let { parseDecision(it.content) }
            if (step == null) {
                recentActions.add("no actionable decision")
                if (recentActions.takeLast(3).count { it.contains("no actionable") } >= 3) {
                    block("I couldn't decide a next step. Try rephrasing the request.")
                    return
                }
                continue
            }

            val action = step.nextAction
            advanceTimeline(iteration, plan.size)
            update {
                it.copy(
                    currentAction = describe(action),
                    confidence = step.confidence,
                    currentObjective = plan.getOrElse(minOf(iteration - 1, plan.size - 1)) { plan.last() }
                )
            }

            if (step.blocker != null && action.type != ActionType.COMPLETE) {
                block(step.blocker!!)
                return
            }
            if (action.type == ActionType.COMPLETE) {
                completed = true
                break
            }
            if (action.type == ActionType.ASK) {
                block(action.target.ifBlank { "I need your input to continue." })
                return
            }

            if (step.needsConfirmation || Safety.requiresConfirmation(action)) {
                val approved = requestConfirmation(action)
                if (!approved) {
                    recentActions.add("user declined: ${describe(action)}")
                    update { it.copy(currentAction = "Skipped: ${describe(action)}") }
                    continue
                }
            }

            val outcome = executor.execute(action)
            TraceLog.log(action.type, action.target, outcome.success, outcome.detail)
            recentActions.add("${action.type.name.lowercase()} ${action.target} -> ${if (outcome.success) "ok" else "fail"}")
            update {
                it.copy(
                    actionLog = (it.actionLog + ActionRecord(
                        iteration.toLong(), now(), action.type, action.target, outcome.success, outcome.detail
                    )).takeLast(60),
                    progress = minOf(0.95f, iteration.toFloat() / (plan.size + 2))
                )
            }
            delay(900)
        }

        if (completed) {
            update {
                it.copy(
                    status = RunStatus.COMPLETED,
                    progress = 1f,
                    finishedAt = now(),
                    currentAction = "Task complete",
                    timeline = it.timeline.map { s -> s.copy(status = StepStatus.DONE) }
                )
            }
            runCatching { history.finish(historyId, "COMPLETED", _state.value.understanding) }
        } else if (_state.value.status == RunStatus.RUNNING) {
            update {
                it.copy(
                    status = RunStatus.COMPLETED,
                    progress = 1f,
                    finishedAt = now(),
                    currentAction = "Reached the step limit"
                )
            }
            runCatching { history.finish(historyId, "STOPPED", "Reached step limit") }
        }
        AgentService.stop(appContext)
    }

    private suspend fun requestConfirmation(action: NextAction): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        confirmation = deferred
        val request = ConfirmationRequest(
            id = now(),
            title = confirmTitle(action),
            summary = describe(action),
            detail = action.reason.ifBlank { action.expectedResult }
        )
        update { it.copy(status = RunStatus.WAITING_CONFIRMATION, confirmation = request) }
        val approved = deferred.await()
        confirmation = null
        update { it.copy(status = RunStatus.RUNNING, confirmation = null) }
        return approved
    }

    private suspend fun readScreen(config: ApiConfig, goal: String): ScreenSnapshot {
        val service = AccessibilityBridge.service
        val base = service?.snapshot()
            ?: ScreenSnapshot("", "", now(), emptyList())
        if (base.nodes.size >= 3) return base

        if (capture.isReady) {
            val bitmap = capture.capture()
            if (bitmap != null) {
                val ocrText = runCatching { ocr.recognize(bitmap) }.getOrDefault("")
                var vision = ""
                val useVision = runCatching { settings.useVision.first() }.getOrDefault(true)
                if (useVision) {
                    val result = mimo.vision(
                        config, Prompts.VISION_SYSTEM, Prompts.visionUser(goal), encodeJpeg(bitmap)
                    )
                    if (result is LlmResult.Ok) vision = result.content
                }
                return base.copy(
                    ocrText = listOf(base.ocrText, ocrText, vision).filter { it.isNotBlank() }.joinToString("\n"),
                    source = if (vision.isNotBlank()) ScreenSource.VISION else ScreenSource.OCR
                )
            }
        }
        return base
    }

    private fun encodeJpeg(bitmap: Bitmap): String {
        val maxWidth = 1024
        val scaled = if (bitmap.width > maxWidth) {
            val ratio = maxWidth.toFloat() / bitmap.width
            Bitmap.createScaledBitmap(bitmap, maxWidth, (bitmap.height * ratio).toInt(), true)
        } else {
            bitmap
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun learnFromGoal(goal: String, decision: AgentDecision) {
        scope.launch {
            runCatching {
                memory.remember(MemoryKind.TASK, goal.take(60), goal, 1.0)
                memory.remember(MemoryKind.SEARCH, decision.taskType.name.lowercase(), goal, 0.5)
            }
        }
    }

    private fun advanceTimeline(iteration: Int, planSize: Int) {
        if (planSize == 0) return
        val idx = minOf(iteration - 1, planSize - 1)
        update { st ->
            st.copy(
                timeline = st.timeline.mapIndexed { i, s ->
                    when {
                        i < idx -> s.copy(status = StepStatus.DONE)
                        i == idx -> s.copy(status = StepStatus.ACTIVE)
                        else -> s
                    }
                }
            )
        }
    }

    private fun describe(action: NextAction): String = when (action.type) {
        ActionType.OPEN_APP -> "Opening ${action.target}"
        ActionType.OPEN_URL -> "Opening ${action.target}"
        ActionType.SEARCH -> "Searching \"${action.target}\""
        ActionType.TAP -> "Tapping \"${action.target}\""
        ActionType.INPUT_TEXT -> "Typing \"${action.value.ifBlank { action.target }}\""
        ActionType.SCROLL -> "Scrolling ${action.value.ifBlank { "down" }}"
        ActionType.SWIPE -> "Swiping"
        ActionType.BACK -> "Going back"
        ActionType.HOME -> "Going to home screen"
        ActionType.RECENTS -> "Opening recents"
        ActionType.WAIT -> "Waiting for the screen"
        ActionType.EXTRACT -> "Reading the screen"
        ActionType.COMPLETE -> "Completing the task"
        ActionType.ASK -> "Asking you"
        ActionType.UNKNOWN -> "Thinking"
    }

    private fun confirmTitle(action: NextAction): String = when (action.type) {
        ActionType.INPUT_TEXT -> "Confirm before sending"
        else -> "Confirm this action"
    }

    private fun friendlyApp(pkg: String): String = when {
        pkg.isBlank() -> ""
        pkg.contains("launcher") -> "Home screen"
        else -> pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }

    private fun fail(message: String) {
        _state.value = _state.value.copy(
            status = RunStatus.FAILED,
            blocker = message,
            finishedAt = now(),
            currentAction = message
        )
        AgentService.stop(appContext)
    }

    private fun block(message: String) {
        _state.value = _state.value.copy(
            status = RunStatus.BLOCKED,
            blocker = message,
            finishedAt = now(),
            currentAction = "Blocked"
        )
        AgentService.stop(appContext)
    }

    private fun now() = System.currentTimeMillis()

    private inline fun update(transform: (MissionState) -> MissionState) {
        _state.value = transform(_state.value)
    }

    private companion object {
        const val MAX_STEPS = 18
    }
}
