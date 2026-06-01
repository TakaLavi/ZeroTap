package com.zerotap.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerotap.app.ZeroTapApp
import com.zerotap.app.accessibility.AccessibilityBridge
import com.zerotap.app.api.ApiConfig
import com.zerotap.app.core.MemoryItem
import com.zerotap.app.core.MissionState
import com.zerotap.app.data.db.TaskRecordEntity
import com.zerotap.app.logging.TraceLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Single ViewModel backing the whole single-activity UI. */
class ZeroTapViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as ZeroTapApp).container

    val mission: StateFlow<MissionState> = container.agentEngine.state

    val accessibilityConnected: StateFlow<Boolean> = AccessibilityBridge.connected
    val captureActive: StateFlow<Boolean> = container.captureManager.active

    val memories: StateFlow<List<MemoryItem>> =
        container.memoryRepository.all.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val history: StateFlow<List<TaskRecordEntity>> = container.taskHistoryRepository
        .recent(30)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val config: StateFlow<ApiConfig> = container.settingsRepository.config
        .stateIn(viewModelScope, SharingStarted.Eagerly, ApiConfig("", "", "", ""))

    val onboarded: StateFlow<Boolean> = container.settingsRepository.onboarded
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val trace = TraceLog.records

    // ---- Agent control -------------------------------------------------------

    fun run(goal: String) = container.agentEngine.start(goal)
    fun stop() = container.agentEngine.stop()
    fun confirm(approved: Boolean) = container.agentEngine.confirm(approved)
    fun reset() = container.agentEngine.reset()

    // ---- Settings ------------------------------------------------------------

    fun saveApiKey(value: String) = viewModelScope.launch {
        container.settingsRepository.setApiKey(value.trim())
    }

    fun saveBaseUrl(value: String) = viewModelScope.launch {
        container.settingsRepository.setBaseUrl(value.trim())
    }

    fun savePlanningModel(value: String) = viewModelScope.launch {
        container.settingsRepository.setPlanningModel(value.trim())
    }

    fun saveVisionModel(value: String) = viewModelScope.launch {
        container.settingsRepository.setVisionModel(value.trim())
    }

    fun setOnboarded(value: Boolean) = viewModelScope.launch {
        container.settingsRepository.setOnboarded(value)
    }

    // ---- Memory --------------------------------------------------------------

    fun pinMemory(item: MemoryItem) = viewModelScope.launch {
        container.memoryRepository.setPinned(item.id, !item.pinned)
    }

    fun lockMemory(item: MemoryItem) = viewModelScope.launch {
        container.memoryRepository.setLocked(item.id, !item.locked)
    }

    fun deleteMemory(item: MemoryItem) = viewModelScope.launch {
        container.memoryRepository.delete(item)
    }
}
