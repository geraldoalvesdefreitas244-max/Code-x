package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.CodeXContainer
import com.example.ui.screens.AiProvidersScreen
import com.example.ui.screens.ApiSettingsScreen
import com.example.ui.screens.ApkCompilerScreen
import com.example.ui.screens.CodeEditorScreen
import com.example.ui.screens.CodeXAiScreen
import com.example.ui.screens.CreateProjectScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeviceMonitorScreen
import com.example.ui.screens.FileManagerScreen
import com.example.ui.screens.ForgotPasswordScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TouchTesterScreen
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BuildAndMonitorViewModel
import com.example.ui.viewmodel.EditorViewModel
import com.example.ui.viewmodel.WorkspaceViewModel

@Composable
fun CodeXNavHost(
    authViewModel: AuthViewModel,
    workspaceViewModel: WorkspaceViewModel
) {
    val context = LocalContext.current
    val container = CodeXContainer.getInstance(context)
    val navController = rememberNavController()

    val editorViewModel: EditorViewModel = viewModel(
        factory = simpleFactory {
            EditorViewModel(
                projectDao = container.database.projectDao(),
                fileHistoryDao = container.database.fileHistoryDao(),
                fileManager = container.fileManager,
                secureKeyStore = container.secureKeyStore
            )
        }
    )

    val aiViewModel: AiViewModel = viewModel(
        factory = simpleFactory {
            AiViewModel(
                projectDao = container.database.projectDao(),
                aiProviderDao = container.database.aiProviderDao(),
                aiChatDao = container.database.aiChatDao(),
                fileHistoryDao = container.database.fileHistoryDao(),
                fileManager = container.fileManager,
                secureKeyStore = container.secureKeyStore,
                multiAiClient = container.multiAiClient
            )
        }
    )

    val buildMonitorViewModel: BuildAndMonitorViewModel = viewModel(
        factory = simpleFactory {
            BuildAndMonitorViewModel(
                projectDao = container.database.projectDao(),
                buildRecordDao = container.database.buildRecordDao(),
                fileManager = container.fileManager,
                secureKeyStore = container.secureKeyStore,
                buildServerClient = container.buildServerClient,
                monitorRepository = container.deviceMonitorRepository
            )
        }
    )

    NavHost(
        navController = navController,
        startDestination = CodeXRoutes.SPLASH
    ) {
        composable(CodeXRoutes.SPLASH) {
            SplashScreen(
                authViewModel = authViewModel,
                onNavigateDashboard = {
                    navController.navigate(CodeXRoutes.DASHBOARD) {
                        popUpTo(CodeXRoutes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateLogin = {
                    navController.navigate(CodeXRoutes.LOGIN) {
                        popUpTo(CodeXRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(CodeXRoutes.LOGIN) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(CodeXRoutes.DASHBOARD) {
                        popUpTo(CodeXRoutes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateRegister = { navController.navigate(CodeXRoutes.REGISTER) },
                onNavigateForgotPassword = { navController.navigate(CodeXRoutes.FORGOT_PASSWORD) }
            )
        }

        composable(CodeXRoutes.REGISTER) {
            RegisterScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(CodeXRoutes.DASHBOARD) {
                        popUpTo(CodeXRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(CodeXRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(CodeXRoutes.DASHBOARD) {
            DashboardScreen(
                authViewModel = authViewModel,
                workspaceViewModel = workspaceViewModel,
                onNavigateCreateProject = { navController.navigate(CodeXRoutes.CREATE_PROJECT) },
                onNavigateProjects = { navController.navigate(CodeXRoutes.PROJECTS) },
                onNavigateAiAssistant = { pid -> navController.navigate(CodeXRoutes.aiAssistant(pid)) },
                onNavigateAiProviders = { navController.navigate(CodeXRoutes.AI_PROVIDERS) },
                onNavigateEditor = { pid -> navController.navigate(CodeXRoutes.editor(pid)) },
                onNavigateCompiler = { pid -> navController.navigate(CodeXRoutes.compiler(pid)) },
                onNavigateDeviceMonitor = { navController.navigate(CodeXRoutes.DEVICE_MONITOR) },
                onNavigateTouchTester = { navController.navigate(CodeXRoutes.TOUCH_TESTER) },
                onNavigateSettings = { navController.navigate(CodeXRoutes.SETTINGS) },
                onNavigateProfile = { navController.navigate(CodeXRoutes.PROFILE) }
            )
        }

        composable(CodeXRoutes.CREATE_PROJECT) {
            CreateProjectScreen(
                authViewModel = authViewModel,
                workspaceViewModel = workspaceViewModel,
                onBack = { navController.popBackStack() },
                onProjectCreated = { pid ->
                    navController.navigate(CodeXRoutes.editor(pid)) {
                        popUpTo(CodeXRoutes.CREATE_PROJECT) { inclusive = true }
                    }
                }
            )
        }

        composable(CodeXRoutes.PROJECTS) {
            ProjectsScreen(
                workspaceViewModel = workspaceViewModel,
                onBack = { navController.popBackStack() },
                onCreateNew = { navController.navigate(CodeXRoutes.CREATE_PROJECT) },
                onOpenEditor = { pid -> navController.navigate(CodeXRoutes.editor(pid)) },
                onOpenFileManager = { pid -> navController.navigate(CodeXRoutes.fileManager(pid)) },
                onOpenCompiler = { pid -> navController.navigate(CodeXRoutes.compiler(pid)) },
                onOpenAi = { pid -> navController.navigate(CodeXRoutes.aiAssistant(pid)) }
            )
        }

        composable(
            route = CodeXRoutes.EDITOR,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { entry ->
            val projectId = entry.arguments?.getString("projectId").orEmpty()
            CodeEditorScreen(
                projectId = projectId,
                editorViewModel = editorViewModel,
                workspaceViewModel = workspaceViewModel,
                onBack = { navController.popBackStack() },
                onOpenFileManager = { pid -> navController.navigate(CodeXRoutes.fileManager(pid)) },
                onOpenAi = { pid -> navController.navigate(CodeXRoutes.aiAssistant(pid)) },
                onOpenCompiler = { pid -> navController.navigate(CodeXRoutes.compiler(pid)) }
            )
        }

        composable(
            route = CodeXRoutes.FILE_MANAGER,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { entry ->
            val projectId = entry.arguments?.getString("projectId").orEmpty()
            FileManagerScreen(
                projectId = projectId,
                editorViewModel = editorViewModel,
                onBack = { navController.popBackStack() },
                onOpenInEditor = { pid ->
                    navController.navigate(CodeXRoutes.editor(pid)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = CodeXRoutes.AI_ASSISTANT,
            arguments = listOf(navArgument("projectId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { entry ->
            val projectId = entry.arguments?.getString("projectId")
            CodeXAiScreen(
                initialProjectId = projectId,
                aiViewModel = aiViewModel,
                workspaceViewModel = workspaceViewModel,
                onBack = { navController.popBackStack() },
                onOpenProviders = { navController.navigate(CodeXRoutes.AI_PROVIDERS) }
            )
        }

        composable(CodeXRoutes.AI_PROVIDERS) {
            AiProvidersScreen(
                workspaceViewModel = workspaceViewModel,
                onBack = { navController.popBackStack() },
                onConfigureProvider = { id -> navController.navigate(CodeXRoutes.apiSettings(id)) }
            )
        }

        composable(
            route = CodeXRoutes.API_SETTINGS,
            arguments = listOf(navArgument("providerId") { type = NavType.StringType })
        ) { entry ->
            val providerId = entry.arguments?.getString("providerId").orEmpty()
            ApiSettingsScreen(
                providerId = providerId,
                workspaceViewModel = workspaceViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = CodeXRoutes.COMPILER,
            arguments = listOf(navArgument("projectId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { entry ->
            val projectId = entry.arguments?.getString("projectId")
            ApkCompilerScreen(
                initialProjectId = projectId,
                workspaceViewModel = workspaceViewModel,
                buildViewModel = buildMonitorViewModel,
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(CodeXRoutes.SETTINGS) }
            )
        }

        composable(CodeXRoutes.DEVICE_MONITOR) {
            DeviceMonitorScreen(
                monitorViewModel = buildMonitorViewModel,
                onBack = { navController.popBackStack() },
                onOpenTouchTester = { navController.navigate(CodeXRoutes.TOUCH_TESTER) }
            )
        }

        composable(CodeXRoutes.TOUCH_TESTER) {
            TouchTesterScreen(onBack = { navController.popBackStack() })
        }

        composable(CodeXRoutes.PROFILE) {
            ProfileScreen(
                authViewModel = authViewModel,
                workspaceViewModel = workspaceViewModel,
                onBack = { navController.popBackStack() },
                onSignedOut = {
                    navController.navigate(CodeXRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(CodeXRoutes.SETTINGS) {
            SettingsScreen(
                workspaceViewModel = workspaceViewModel,
                buildViewModel = buildMonitorViewModel,
                onBack = { navController.popBackStack() },
                onOpenAiProviders = { navController.navigate(CodeXRoutes.AI_PROVIDERS) }
            )
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T : ViewModel> simpleFactory(creator: () -> T): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = creator() as VM
    }
}
