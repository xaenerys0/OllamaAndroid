package com.ollamaandroid.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ollamaandroid.app.OllamaApplication
import com.ollamaandroid.app.data.AppSettings
import com.ollamaandroid.app.data.ChatRepository
import com.ollamaandroid.app.data.SettingsRepository
import com.ollamaandroid.app.data.db.ConversationEntity
import com.ollamaandroid.app.data.db.MessageEntity
import com.ollamaandroid.app.data.network.ReasoningLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** In-flight assistant reply, rendered as the last bubble while streaming. */
data class StreamingReply(
    val conversationId: Long,
    val content: String = "",
    val thinking: String = "",
)

data class ChatUiState(
    val currentConversationId: Long? = null,
    val streaming: StreamingReply? = null,
    val errorMessage: String? = null,
    val availableModels: List<String> = emptyList(),
    val modelsLoading: Boolean = false,
    /** Whether the currently selected model supports the `think` parameter. */
    val supportsThinking: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val conversations: StateFlow<List<ConversationEntity>> = chatRepository.conversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val currentConversationId = MutableStateFlow<Long?>(null)

    val messages: StateFlow<List<MessageEntity>> = currentConversationId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else chatRepository.messages(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var streamJob: Job? = null

    val isStreaming: Boolean get() = streamJob?.isActive == true

    /** Cache of model → thinking capability, so /api/show runs once per model. */
    private val thinkingSupportCache = mutableMapOf<String, Boolean>()

    init {
        viewModelScope.launch {
            settingsRepository.settings
                .map { it.selectedModel to it.isConfigured }
                .distinctUntilChanged()
                .collect { (model, configured) ->
                    if (!configured) return@collect
                    val cached = thinkingSupportCache[model]
                    val supported = cached ?: runCatching { chatRepository.modelSupportsThinking(model) }
                        .getOrDefault(false)
                        .also { thinkingSupportCache[model] = it }
                    _uiState.update { it.copy(supportsThinking = supported) }
                }
        }
    }

    fun selectReasoning(level: ReasoningLevel) {
        viewModelScope.launch { settingsRepository.setReasoning(level) }
    }

    fun selectConversation(id: Long?) {
        if (currentConversationId.value == id) return
        stopStreaming()
        currentConversationId.value = id
        _uiState.update { it.copy(currentConversationId = id, errorMessage = null) }
    }

    fun newConversation() = selectConversation(null)

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            if (currentConversationId.value == id) selectConversation(null)
            chatRepository.deleteConversation(id)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun selectModel(model: String) {
        viewModelScope.launch { settingsRepository.setSelectedModel(model) }
    }

    fun refreshModels() {
        if (_uiState.value.modelsLoading) return
        _uiState.update { it.copy(modelsLoading = true) }
        viewModelScope.launch {
            runCatching { chatRepository.listModels() }
                .onSuccess { models ->
                    _uiState.update { it.copy(availableModels = models, modelsLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            modelsLoading = false,
                            errorMessage = error.message ?: "Failed to load models",
                        )
                    }
                }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || isStreaming) return

        streamJob = viewModelScope.launch {
            val model = settings.value.selectedModel
            val conversationId = currentConversationId.value
                ?: chatRepository.createConversation(trimmed, model).also { newId ->
                    currentConversationId.value = newId
                    _uiState.update { it.copy(currentConversationId = newId) }
                }

            chatRepository.addUserMessage(conversationId, trimmed)

            val history = chatRepository.messages(conversationId).first()
            _uiState.update {
                it.copy(streaming = StreamingReply(conversationId), errorMessage = null)
            }

            val received = StringBuilder()
            val thinkingReceived = StringBuilder()
            try {
                chatRepository.streamAssistantReply(
                    history = history,
                    modelSupportsThinking = _uiState.value.supportsThinking,
                ).collect { chunk ->
                    val delta = chunk.message?.content.orEmpty()
                    val thinkingDelta = chunk.message?.thinking.orEmpty()
                    if (delta.isNotEmpty() || thinkingDelta.isNotEmpty()) {
                        received.append(delta)
                        thinkingReceived.append(thinkingDelta)
                        _uiState.update {
                            it.copy(
                                streaming = StreamingReply(
                                    conversationId = conversationId,
                                    content = received.toString(),
                                    thinking = thinkingReceived.toString(),
                                )
                            )
                        }
                    }
                }
                chatRepository.saveAssistantMessage(
                    conversationId = conversationId,
                    content = received.toString(),
                    model = model,
                    interrupted = false,
                    thinking = thinkingReceived.toString(),
                )
            } catch (e: CancellationException) {
                if (received.isNotEmpty() || thinkingReceived.isNotEmpty()) {
                    withContext(NonCancellable) {
                        chatRepository.saveAssistantMessage(
                            conversationId = conversationId,
                            content = received.toString(),
                            model = model,
                            interrupted = true,
                            thinking = thinkingReceived.toString(),
                        )
                    }
                }
                throw e
            } catch (e: Exception) {
                if (received.isNotEmpty() || thinkingReceived.isNotEmpty()) {
                    chatRepository.saveAssistantMessage(
                        conversationId = conversationId,
                        content = received.toString(),
                        model = model,
                        interrupted = true,
                        thinking = thinkingReceived.toString(),
                    )
                }
                _uiState.update { it.copy(errorMessage = e.message ?: "Something went wrong") }
            } finally {
                _uiState.update { it.copy(streaming = null) }
            }
        }
    }

    fun stopStreaming() {
        streamJob?.cancel()
        streamJob = null
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[APPLICATION_KEY] as OllamaApplication
                return ChatViewModel(
                    chatRepository = app.container.chatRepository,
                    settingsRepository = app.container.settingsRepository,
                ) as T
            }
        }
    }
}
