package com.ollamaandroid.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ollamaandroid.app.OllamaApplication
import com.ollamaandroid.app.data.ChatRepository
import com.ollamaandroid.app.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKey: String = "",
    val baseUrl: String = SettingsRepository.DEFAULT_BASE_URL,
    val selectedModel: String = SettingsRepository.DEFAULT_MODEL,
    val loaded: Boolean = false,
    val testing: Boolean = false,
    val testResult: TestResult? = null,
)

sealed interface TestResult {
    data class Success(val modelCount: Int) : TestResult
    data class Failure(val message: String) : TestResult
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            val current = settingsRepository.settings.first()
            _uiState.update {
                it.copy(
                    apiKey = current.apiKey,
                    baseUrl = current.baseUrl,
                    selectedModel = current.selectedModel,
                    loaded = true,
                )
            }
        }
    }

    fun onApiKeyChange(value: String) {
        _uiState.update { it.copy(apiKey = value, testResult = null) }
    }

    fun onBaseUrlChange(value: String) {
        _uiState.update { it.copy(baseUrl = value, testResult = null) }
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            settingsRepository.setApiKey(state.apiKey)
            settingsRepository.setBaseUrl(state.baseUrl.ifBlank { SettingsRepository.DEFAULT_BASE_URL })
            onSaved()
        }
    }

    fun testConnection() {
        if (_uiState.value.testing) return
        _uiState.update { it.copy(testing = true, testResult = null) }
        viewModelScope.launch {
            val state = _uiState.value
            runCatching {
                chatRepository.listModels(
                    baseUrl = state.baseUrl.ifBlank { SettingsRepository.DEFAULT_BASE_URL },
                    apiKey = state.apiKey,
                )
            }.onSuccess { models ->
                _uiState.update {
                    it.copy(testing = false, testResult = TestResult.Success(models.size))
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        testing = false,
                        testResult = TestResult.Failure(error.message ?: "Connection failed"),
                    )
                }
            }
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[APPLICATION_KEY] as OllamaApplication
                return SettingsViewModel(
                    settingsRepository = app.container.settingsRepository,
                    chatRepository = app.container.chatRepository,
                ) as T
            }
        }
    }
}
