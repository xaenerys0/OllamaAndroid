package com.ollamaandroid.app.data

import com.ollamaandroid.app.data.db.ChatDao
import com.ollamaandroid.app.data.db.ConversationEntity
import com.ollamaandroid.app.data.db.MessageEntity
import com.ollamaandroid.app.data.network.ApiChatMessage
import com.ollamaandroid.app.data.network.ChatRequest
import com.ollamaandroid.app.data.network.ChatResponseChunk
import com.ollamaandroid.app.data.network.OllamaClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ChatRepository(
    private val chatDao: ChatDao,
    private val client: OllamaClient,
    private val settingsRepository: SettingsRepository,
) {
    val conversations: Flow<List<ConversationEntity>> = chatDao.observeConversations()

    fun messages(conversationId: Long): Flow<List<MessageEntity>> =
        chatDao.observeMessages(conversationId)

    suspend fun createConversation(firstUserMessage: String, model: String): Long {
        val now = System.currentTimeMillis()
        val title = firstUserMessage.trim().replace(Regex("\\s+"), " ").take(60)
        return chatDao.insertConversation(
            ConversationEntity(title = title.ifBlank { "New chat" }, model = model, createdAt = now, updatedAt = now)
        )
    }

    suspend fun addUserMessage(conversationId: Long, content: String) {
        val now = System.currentTimeMillis()
        chatDao.insertMessage(
            MessageEntity(conversationId = conversationId, role = "user", content = content, createdAt = now)
        )
        chatDao.touchConversation(conversationId, now)
    }

    suspend fun saveAssistantMessage(
        conversationId: Long,
        content: String,
        model: String,
        interrupted: Boolean,
        thinking: String? = null,
    ) {
        val now = System.currentTimeMillis()
        chatDao.insertMessage(
            MessageEntity(
                conversationId = conversationId,
                role = "assistant",
                content = content,
                thinking = thinking?.takeIf { it.isNotBlank() },
                model = model,
                interrupted = interrupted,
                createdAt = now,
            )
        )
        chatDao.touchConversation(conversationId, now)
    }

    suspend fun deleteConversation(conversationId: Long) {
        chatDao.deleteConversation(conversationId)
    }

    /**
     * Streams the assistant reply for the given message history using the
     * currently configured server, key, and model. The reasoning preference is
     * only sent when the model advertises the `thinking` capability, since
     * non-thinking models reject the `think` parameter.
     */
    suspend fun streamAssistantReply(
        history: List<MessageEntity>,
        modelSupportsThinking: Boolean,
    ): Flow<ChatResponseChunk> {
        val settings = settingsRepository.settings.first()
        val request = ChatRequest(
            model = settings.selectedModel,
            messages = history.map { ApiChatMessage(role = it.role, content = it.content) },
            think = if (modelSupportsThinking) settings.reasoning.toThinkValue() else null,
        )
        return client.streamChat(settings.baseUrl, settings.apiKey, request)
    }

    /** True when the model reports the `thinking` capability via /api/show. */
    suspend fun modelSupportsThinking(model: String): Boolean {
        val settings = settingsRepository.settings.first()
        return client.showModel(settings.baseUrl, settings.apiKey, model).supportsThinking
    }

    suspend fun listModels(): List<String> {
        val settings = settingsRepository.settings.first()
        return client.listModels(settings.baseUrl, settings.apiKey)
    }

    suspend fun listModels(baseUrl: String, apiKey: String): List<String> =
        client.listModels(baseUrl, apiKey)
}
