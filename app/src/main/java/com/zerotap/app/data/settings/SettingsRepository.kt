package com.zerotap.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zerotap.app.api.ApiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zerotap_settings")

/** Persists runtime configuration. The API key lives only here, never in source. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val baseUrl = stringPreferencesKey("base_url")
        val apiKey = stringPreferencesKey("api_key")
        val planningModel = stringPreferencesKey("planning_model")
        val visionModel = stringPreferencesKey("vision_model")
        val temperature = doublePreferencesKey("temperature")
        val onboarded = booleanPreferencesKey("onboarded")
        val useVision = booleanPreferencesKey("use_vision")
    }

    val config: Flow<ApiConfig> = context.dataStore.data.map { p ->
        ApiConfig(
            baseUrl = p[Keys.baseUrl]?.takeIf { it.isNotBlank() } ?: DEFAULT_BASE_URL,
            apiKey = p[Keys.apiKey] ?: "",
            planningModel = p[Keys.planningModel]?.takeIf { it.isNotBlank() } ?: DEFAULT_PLANNING_MODEL,
            visionModel = p[Keys.visionModel]?.takeIf { it.isNotBlank() } ?: DEFAULT_VISION_MODEL,
            temperature = p[Keys.temperature] ?: 0.2
        )
    }

    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.onboarded] ?: false }
    val useVision: Flow<Boolean> = context.dataStore.data.map { it[Keys.useVision] ?: true }

    suspend fun setBaseUrl(value: String) = context.dataStore.edit { it[Keys.baseUrl] = value }
    suspend fun setApiKey(value: String) = context.dataStore.edit { it[Keys.apiKey] = value }
    suspend fun setPlanningModel(value: String) = context.dataStore.edit { it[Keys.planningModel] = value }
    suspend fun setVisionModel(value: String) = context.dataStore.edit { it[Keys.visionModel] = value }
    suspend fun setTemperature(value: Double) = context.dataStore.edit { it[Keys.temperature] = value }
    suspend fun setUseVision(value: Boolean) = context.dataStore.edit { it[Keys.useVision] = value }
    suspend fun setOnboarded(value: Boolean) = context.dataStore.edit { it[Keys.onboarded] = value }

    companion object {
        const val DEFAULT_BASE_URL = "https://token-plan-sgp.xiaomimimo.com/v1"
        const val DEFAULT_PLANNING_MODEL = "mimo-v2.5-pro"
        const val DEFAULT_VISION_MODEL = "mimo-v2-omni"
    }
}
