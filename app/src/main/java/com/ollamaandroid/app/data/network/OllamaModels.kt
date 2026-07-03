package com.ollamaandroid.app.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class ApiChatMessage(
    val role: String,
    val content: String = "",
    val thinking: String? = null,
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ApiChatMessage>,
    val stream: Boolean = true,
    /**
     * Reasoning control for thinking-capable models. Ollama accepts either a
     * boolean (`true`/`false`) or an effort level (`"low"`/`"medium"`/`"high"`,
     * e.g. for gpt-oss). Omitted entirely when null, leaving the model default.
     */
    val think: JsonElement? = null,
)

/** User-facing reasoning choices, mapped to the API's `think` field. */
enum class ReasoningLevel(val key: String, val label: String) {
    DEFAULT("default", "Default"),
    OFF("off", "Off"),
    ON("on", "On"),
    LOW("low", "Low"),
    MEDIUM("medium", "Medium"),
    HIGH("high", "High");

    fun toThinkValue(): JsonElement? = when (this) {
        DEFAULT -> null
        OFF -> JsonPrimitive(false)
        ON -> JsonPrimitive(true)
        LOW, MEDIUM, HIGH -> JsonPrimitive(key)
    }

    companion object {
        fun fromKey(key: String?): ReasoningLevel =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

@Serializable
data class ChatResponseChunk(
    val model: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val message: ApiChatMessage? = null,
    val done: Boolean = false,
    @SerialName("done_reason") val doneReason: String? = null,
    @SerialName("total_duration") val totalDuration: Long? = null,
    @SerialName("eval_count") val evalCount: Long? = null,
)

@Serializable
data class ModelTag(
    val name: String? = null,
    val model: String? = null,
) {
    val id: String get() = name ?: model ?: ""
}

@Serializable
data class ModelsResponse(
    val models: List<ModelTag> = emptyList(),
)

@Serializable
data class ShowRequest(
    val model: String,
)

@Serializable
data class ShowResponse(
    val capabilities: List<String> = emptyList(),
) {
    val supportsThinking: Boolean get() = "thinking" in capabilities
}

@Serializable
data class ApiError(
    val error: String? = null,
)
