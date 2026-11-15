package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.CodeXNavHost
import com.example.ui.theme.CodeXTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.WorkspaceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val container = CodeXContainer.getInstance(applicationContext)

        setContent {
            @Suppress("UNCHECKED_CAST")
            val authViewModel: AuthViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return AuthViewModel(container.authRepository) as T
                    }
                }
            )
            @Suppress("UNCHECKED_CAST")
            val workspaceViewModel: WorkspaceViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return WorkspaceViewModel(
                            projectDao = container.database.projectDao(),
                            aiProviderDao = container.database.aiProviderDao(),
                            buildRecordDao = container.database.buildRecordDao(),
                            fileManager = container.fileManager,
                            secureKeyStore = container.secureKeyStore,
                            multiAiClient = container.multiAiClient
                        ) as T
                    }
                }
            )

            val prefs by workspaceViewModel.editorPreferences.collectAsStateWithLifecycle()

            CodeXTheme(darkTheme = prefs.darkTheme) {
                CodeXNavHost(
                    authViewModel = authViewModel,
                    workspaceViewModel = workspaceViewModel
                )
            }
        }
    }
}
