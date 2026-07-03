package com.ollamaandroid.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ollamaandroid.app.ui.chat.ChatScreen
import com.ollamaandroid.app.ui.chat.ChatViewModel
import com.ollamaandroid.app.ui.settings.SettingsScreen
import com.ollamaandroid.app.ui.settings.SettingsViewModel
import com.ollamaandroid.app.ui.theme.OllamaAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OllamaAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    OllamaNavHost()
                }
            }
        }
    }
}

@Composable
private fun OllamaNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "chat") {
        composable("chat") {
            val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory)
            ChatScreen(
                viewModel = chatViewModel,
                onOpenSettings = { navController.navigate("settings") },
            )
        }
        composable("settings") {
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
