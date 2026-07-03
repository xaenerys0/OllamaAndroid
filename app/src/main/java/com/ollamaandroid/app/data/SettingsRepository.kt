package com.ollamaandroid.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ollamaandroid.app.data.network.ReasoningLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AppSettings(
    val apiKey: String = "",
    val baseUrl: String = SettingsRepository.DEFAULT_BASE_URL,
    val selectedModel: String = SettingsRepository.DEFAULT_MODEL,
    val reasoning: ReasoningLevel = ReasoningLevel.DEFAULT,
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            apiKey = prefs[KEY_API_KEY] ?: "",
            baseUrl = (prefs[KEY_BASE_URL] ?: DEFAULT_BASE_URL).ifBlank { DEFAULT_BASE_URL },
            selectedModel = (prefs[KEY_MODEL] ?: DEFAULT_MODEL).ifBlank { DEFAULT_MODEL },
            reasoning = ReasoningLevel.fromKey(prefs[KEY_REASONING]),
        )
    }

    suspend fun setApiKey(apiKey: String) {
        context.dataStore.edit { it[KEY_API_KEY] = apiKey.trim() }
    }

    suspend fun setBaseUrl(baseUrl: String) {
        context.dataStore.edit { it[KEY_BASE_URL] = baseUrl.trim() }
    }

    suspend fun setSelectedModel(model: String) {
        context.dataStore.edit { it[KEY_MODEL] = model.trim() }
    }

    suspend fun setReasoning(level: ReasoningLevel) {
        context.dataStore.edit { it[KEY_REASONING] = level.key }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://ollama.com"
        const val DEFAULT_MODEL = "gpt-oss:120b"

        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_MODEL = stringPreferencesKey("selected_model")
        private val KEY_REASONING = stringPreferencesKey("reasoning_level")
    }
}
