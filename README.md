# ZeroTap

**An autonomous phone agent for Android 15.** Give it one plain-language command and it plans
the task, reads the screen, opens apps, taps, types, scrolls, verifies each step, remembers your
preferences, and keeps going until the job is done — or it hits a real blocker.

> ZeroTap is not a chatbot. It is an autonomous phone operator powered by the Xiaomi **MiMo** models.

```
“Find me dinner nearby”            “Message Bethany on Discord”
“Find the cheapest gaming mouse”   “Compare laptops under $500”
```

---

## How it works

ZeroTap runs a tight agent loop:

1. **Parse** the request → classify the task (messaging / shopping / search / navigation / utility)
2. **Plan** an ordered set of steps with the MiMo planning model
3. **Inspect** the current screen (accessibility tree → OCR → vision screenshot → heuristics)
4. **Decide** the single best next action as strict JSON
5. **Execute** it (open app, tap, type, scroll, back/home, web search…)
6. **Verify** progress by re-reading the screen
7. **Repeat** until complete, blocked, or it needs your confirmation

The reasoning brain returns structured JSON like:

```json
{
  "goal": "Find me dinner near me",
  "task_type": "shopping",
  "understanding": "User wants nearby restaurant options, cheap and good",
  "plan": ["Get location", "Search restaurants", "Compare", "Recommend"],
  "next_action": { "type": "search", "target": "restaurants near me",
                   "value": "", "reason": "Need candidates", "expected_result": "A list" },
  "confidence": 0.7, "needs_confirmation": false, "blocker": null
}
```

### Safety
ZeroTap **always stops and asks** before anything irreversible: sending a message, posting
publicly, deleting data, or spending money. It shows you the exact final action to approve.

---

## Architecture

Clean architecture, MVVM, Kotlin, Jetpack Compose (Material 3), Coroutines/Flow, Room + DataStore.

```
com.zerotap.app
├── api/            MiMo OpenAI-compatible client (planning + vision)
├── agent/          AgentEngine loop, ActionExecutor, prompts
├── accessibility/  AccessibilityService: reads the tree, taps/types/scrolls
├── capture/        MediaProjection screen capture (foreground service)
├── vision/         ML Kit OCR + model vision fallback
├── data/           Room (memory + history), DataStore (settings)
├── service/        Foreground services + notifications
├── core/           Domain models, JSON parsing
└── ui/             Liquid-glass Compose UI (Home, Mission, Memory, Settings, Onboarding)
```

Layered screen understanding: **accessibility tree first**, then **OCR**, then **vision
screenshot**, then **heuristics**.

---

## Getting the APK

Every push to `main` triggers a GitHub Actions build that publishes a signed debug
`ZeroTap.apk` to the repo's **[latest release](../../releases/latest)** and as a workflow
artifact. Download it there and sideload it.

## Build it yourself

**In Android Studio:** open the project (Ladybug or newer), let Gradle sync, then
`Run` or `Build > Build APK(s)`.

**Command line** (needs the Android SDK + JDK 17):

```bash
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

- compileSdk / targetSdk **35** (Android 15), minSdk 26
- AGP 8.7.2, Kotlin 2.0.21, Gradle 8.10.2

---

## First run

1. Install and open ZeroTap.
2. **Settings → MiMo API key:** paste your key. The default endpoint is
   `https://token-plan-sgp.xiaomimimo.com/v1` with `mimo-v2.5-pro` (planning) and
   `mimo-v2-omni` (vision). The key is stored only on-device (DataStore) and **never** committed.
3. **Enable the accessibility service** (Settings → Accessibility → ZeroTap).
4. Optionally **grant screen vision** (MediaProjection) for the OCR/vision fallback.
5. Type a command on the Home screen and hit **Run**.

> No secrets are hardcoded. Configure everything at runtime in Settings.

---

## Permissions & why

| Permission | Why |
|---|---|
| Accessibility | Read the screen and perform taps/typing/scrolls |
| MediaProjection | Capture a screenshot when accessibility data is incomplete |
| Foreground service | Keep a task running while operating other apps |
| Notifications | Show the active-task notification |
| Internet | Talk to the MiMo API |

---

## Notes

This is an ambitious first build. The agent loop, accessibility control, screen capture, memory,
and the full premium UI are functional; deep multi-site shopping comparison and richer vision
parsing are scaffolded for iteration. Contributions and refinements welcome.
