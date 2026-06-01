package com.zerotap.app.agent

import com.zerotap.app.core.ScreenSnapshot

/** Every prompt the reasoning brain receives, kept in one place. */
object Prompts {

    val PLANNER_SYSTEM = """
        You are ZeroTap, an autonomous Android phone operator. You do not chat. You complete a
        user's task by operating the phone one action at a time: opening apps, reading the current
        screen, tapping, typing, scrolling and verifying results.

        You MUST reply with STRICT JSON only (no markdown, no prose) matching exactly:
        {
          "goal": string,
          "task_type": "messaging" | "shopping" | "search" | "navigation" | "utility" | "unknown",
          "understanding": string,
          "plan": [string, ...],
          "next_action": {
            "type": "open_app" | "open_url" | "search" | "tap" | "input_text" | "scroll" |
                    "swipe" | "back" | "home" | "recents" | "wait" | "extract" | "complete" | "ask",
            "target": string,
            "value": string,
            "reason": string,
            "expected_result": string
          },
          "confidence": number,
          "needs_confirmation": boolean,
          "blocker": string | null
        }

        Action rules:
        - Choose exactly ONE next_action that makes sense for the CURRENT screen described below.
        - "open_app": target = app name (e.g. "Discord", "Chrome").
        - "open_url": target = full URL. "search": target = the search query (opens a web search).
        - "tap": target = the visible text/label of the element to tap.
        - "input_text": value = the exact text to type into the focused field.
        - "scroll": value = "down" or "up". "back"/"home"/"recents" navigate the system.
        - "wait": use when the screen is loading. "extract": when you only need to read results.
        - "complete": ONLY when the goal is verifiably done. "ask": when you need the user to decide.

        Safety: set "needs_confirmation": true and DO NOT perform any action that sends a message,
        posts publicly, deletes data, or spends money. Instead describe the exact final action in
        target/value so the user can approve it.

        Never claim success without on-screen evidence. If blocked (login, permission, captcha),
        set "blocker" to a short explanation. Return ONLY the JSON object.
    """.trimIndent()

    val VISION_SYSTEM = """
        You are ZeroTap's vision module. Given a phone screenshot and the user's goal, describe
        only what is relevant: the current app/screen, key tappable controls and their labels,
        any input fields, and the most useful next step. Be concise and factual.
    """.trimIndent()

    fun initialPlan(goal: String, memory: String, device: String): String = """
        USER GOAL: $goal

        DEVICE: $device

        WHAT YOU REMEMBER ABOUT THE USER (use this to personalize choices):
        $memory

        The phone is currently at the home screen or wherever the user left it. Produce your
        understanding, a short ordered plan, and the single best first action.
    """.trimIndent()

    fun step(
        goal: String,
        taskType: String,
        plan: List<String>,
        recentActions: List<String>,
        snapshot: ScreenSnapshot,
        memory: String,
        iteration: Int
    ): String = """
        USER GOAL: $goal
        TASK TYPE: $taskType
        PLAN: ${plan.joinToString(" | ").ifBlank { "(forming)" }}
        STEP: $iteration

        RECENT ACTIONS (most recent last):
        ${recentActions.takeLast(8).joinToString("\n").ifBlank { "(none yet)" }}

        CURRENT SCREEN
        app: ${snapshot.packageName} ${snapshot.activity}
        source: ${snapshot.source}
        elements:
        ${screenSummary(snapshot)}

        screen text:
        ${snapshot.ocrText.take(1200).ifBlank { "(no text read)" }}

        Decide the single best next_action to make progress. Reply with STRICT JSON only.
    """.trimIndent()

    fun visionUser(goal: String): String =
        "User goal: $goal. Describe the relevant elements on this screen and the best next step."

    private fun screenSummary(snapshot: ScreenSnapshot): String {
        if (snapshot.nodes.isEmpty()) return "(no accessible elements)"
        return snapshot.nodes.take(48).joinToString("\n") { n ->
            val flags = buildList {
                if (n.clickable) add("tap")
                if (n.editable) add("input")
                if (n.scrollable) add("scroll")
            }.joinToString(",")
            "[${n.index}] \"${n.label}\" ${if (flags.isNotEmpty()) "($flags) " else ""}@${n.bounds.centerX},${n.bounds.centerY}"
        }
    }
}
