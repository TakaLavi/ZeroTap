package com.zerotap.app.core

import kotlinx.serialization.json.Json

/** Shared lenient JSON used for both API payloads and model output parsing. */
val ZeroTapJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
    encodeDefaults = true
}

/**
 * Models frequently wrap JSON in markdown code fences and add prose around it.
 * This pulls the most plausible JSON object out of an arbitrary string.
 */
fun extractJsonObject(raw: String): String? {
    if (raw.isBlank()) return null
    var s = raw.trim()

    // Strip ```json ... ``` or ``` ... ``` fences.
    val fence = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
    fence.find(s)?.let { s = it.groupValues[1].trim() }

    // Otherwise, slice from the first balanced brace.
    val start = s.indexOf('{')
    if (start < 0) return null
    var depth = 0
    var inString = false
    var escaped = false
    for (i in start until s.length) {
        val c = s[i]
        if (inString) {
            when {
                escaped -> escaped = false
                c == '\\' -> escaped = true
                c == '"' -> inString = false
            }
        } else {
            when (c) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return s.substring(start, i + 1)
                }
            }
        }
    }
    return null
}

/** Parse a model response into an [AgentDecision], or null if it could not be understood. */
fun parseDecision(raw: String): AgentDecision? {
    val json = extractJsonObject(raw) ?: return null
    return runCatching { ZeroTapJson.decodeFromString<AgentDecision>(json) }.getOrNull()
}
