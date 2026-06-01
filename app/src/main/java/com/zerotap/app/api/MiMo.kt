package com.zerotap.app.api

import com.zerotap.app.core.ZeroTapJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Runtime configuration for the reasoning backend. Never hardcode the key here. */
data class ApiConfig(
    val baseUrl: String,
    val apiKey: String,
    val planningModel: String,
    val visionModel: String,
    val temperature: Double = 0.2,
    val maxTokens: Int = 1200
) {
    val isReady: Boolean get() = apiKey.isNotBlank() && baseUrl.isNotBlank()
}

sealed interface LlmResult {
    data class Ok(val content: String) : LlmResult
    data class Err(val message: String, val code: Int = -1) : LlmResult
}

@Serializable
private data class ChatResponse(val choices: List<Choice> = emptyList())

@Serializable
private data class Choice(val message: ChatMsg = ChatMsg())

@Serializable
private data class ChatMsg(val role: String = "", val content: String? = null)

/**
 * Thin OpenAI-compatible client for the Xiaomi MiMo endpoint
 * (https://token-plan-sgp.xiaomimimo.com/v1).
 */
class MiMoClient(private val http: OkHttpClient = defaultHttp()) {

    /** Plain text reasoning / planning call. */
    suspend fun chat(
        config: ApiConfig,
        messages: List<ChatTurn>,
        model: String = config.planningModel
    ): LlmResult = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("model", model)
            put("temperature", config.temperature)
            put("max_tokens", config.maxTokens)
            putJsonArray("messages") {
                messages.forEach { turn ->
                    addJsonObject {
                        put("role", turn.role)
                        put("content", turn.content)
                    }
                }
            }
        }
        execute(config, body)
    }

    /** Multimodal call: sends a screenshot to the omni model for screen understanding. */
    suspend fun vision(
        config: ApiConfig,
        systemPrompt: String,
        userPrompt: String,
        imageBase64Jpeg: String,
        model: String = config.visionModel
    ): LlmResult = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("model", model)
            put("temperature", config.temperature)
            put("max_tokens", config.maxTokens)
            putJsonArray("messages") {
                if (systemPrompt.isNotBlank()) {
                    addJsonObject {
                        put("role", "system")
                        put("content", systemPrompt)
                    }
                }
                addJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        addJsonObject {
                            put("type", "text")
                            put("text", userPrompt)
                        }
                        addJsonObject {
                            put("type", "image_url")
                            putJsonObject("image_url") {
                                put("url", "data:image/jpeg;base64,$imageBase64Jpeg")
                            }
                        }
                    }
                }
            }
        }
        execute(config, body)
    }

    private fun execute(config: ApiConfig, body: JsonObject): LlmResult {
        if (config.apiKey.isBlank()) {
            return LlmResult.Err("No API key configured. Add your MiMo key in Settings.")
        }
        val url = config.baseUrl.trimEnd('/') + "/chat/completions"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()
        return try {
            http.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return LlmResult.Err("HTTP ${resp.code}: ${text.take(280)}", resp.code)
                }
                val parsed = runCatching {
                    ZeroTapJson.decodeFromString<ChatResponse>(text)
                }.getOrNull()
                val content = parsed?.choices?.firstOrNull()?.message?.content
                if (content.isNullOrBlank()) {
                    LlmResult.Err("Empty response from model")
                } else {
                    LlmResult.Ok(content)
                }
            }
        } catch (e: Exception) {
            LlmResult.Err(e.message ?: "Network error")
        }
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        fun defaultHttp(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}

/** One message in a conversation sent to the model. */
data class ChatTurn(val role: String, val content: String) {
    companion object {
        fun system(content: String) = ChatTurn("system", content)
        fun user(content: String) = ChatTurn("user", content)
        fun assistant(content: String) = ChatTurn("assistant", content)
    }
}
