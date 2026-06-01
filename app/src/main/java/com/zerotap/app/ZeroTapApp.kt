package com.zerotap.app

import android.app.Application
import android.content.Context
import com.zerotap.app.agent.ActionExecutor
import com.zerotap.app.agent.AgentEngine
import com.zerotap.app.api.MiMoClient
import com.zerotap.app.capture.ScreenCaptureManager
import com.zerotap.app.data.db.AppDatabase
import com.zerotap.app.data.memory.MemoryRepository
import com.zerotap.app.data.memory.TaskHistoryRepository
import com.zerotap.app.data.settings.SettingsRepository
import com.zerotap.app.service.AgentNotifications
import com.zerotap.app.vision.Ocr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ZeroTapApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        AgentNotifications.ensureChannels(this)
        container.appScope.launch {
            runCatching { container.memoryRepository.seedIfEmpty() }
        }
    }
}

/** Manual dependency container — deliberately simple, no DI framework. */
class AppContainer(context: Context) {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsRepository = SettingsRepository(context)

    private val database = AppDatabase.build(context)
    val memoryRepository = MemoryRepository(database.memoryDao())
    val taskHistoryRepository = TaskHistoryRepository(database.taskDao())

    val mimoClient = MiMoClient()
    val captureManager = ScreenCaptureManager(context)
    private val ocr = Ocr()
    private val executor = ActionExecutor(context)

    val agentEngine = AgentEngine(
        appContext = context.applicationContext,
        mimo = mimoClient,
        settings = settingsRepository,
        memory = memoryRepository,
        history = taskHistoryRepository,
        executor = executor,
        capture = captureManager,
        ocr = ocr
    )
}
