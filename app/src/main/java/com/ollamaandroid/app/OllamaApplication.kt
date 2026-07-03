package com.ollamaandroid.app

import android.app.Application
import com.ollamaandroid.app.data.ChatRepository
import com.ollamaandroid.app.data.SettingsRepository
import com.ollamaandroid.app.data.db.AppDatabase
import com.ollamaandroid.app.data.network.OllamaClient

/**
 * Simple manual dependency container. The app is small enough that a full DI
 * framework (Hilt/Koin) would add more ceremony than value.
 */
class AppContainer(application: Application) {
    val settingsRepository = SettingsRepository(application)
    private val database = AppDatabase.build(application)
    private val ollamaClient = OllamaClient()
    val chatRepository = ChatRepository(
        chatDao = database.chatDao(),
        client = ollamaClient,
        settingsRepository = settingsRepository,
    )
}

class OllamaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
