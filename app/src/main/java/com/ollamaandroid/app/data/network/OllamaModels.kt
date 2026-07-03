package com.ollamaandroid.app.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
)

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
data class ApiError(
    val error: String? = null,
)
