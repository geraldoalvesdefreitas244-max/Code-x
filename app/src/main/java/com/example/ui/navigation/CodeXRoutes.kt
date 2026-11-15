package com.example.ui.navigation

object CodeXRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val DASHBOARD = "dashboard"
    const val CREATE_PROJECT = "create_project"
    const val PROJECTS = "projects"
    const val EDITOR = "editor/{projectId}"
    const val AI_ASSISTANT = "ai_assistant?projectId={projectId}"
    const val AI_PROVIDERS = "ai_providers"
    const val API_SETTINGS = "api_settings/{providerId}"
    const val COMPILER = "compiler?projectId={projectId}"
    const val FILE_MANAGER = "file_manager/{projectId}"
    const val DEVICE_MONITOR = "device_monitor"
    const val TOUCH_TESTER = "touch_tester"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"

    fun editor(projectId: String) = "editor/$projectId"
    fun aiAssistant(projectId: String? = null) =
        if (projectId.isNullOrBlank()) "ai_assistant" else "ai_assistant?projectId=$projectId"
    fun apiSettings(providerId: String) = "api_settings/$providerId"
    fun compiler(projectId: String? = null) =
        if (projectId.isNullOrBlank()) "compiler" else "compiler?projectId=$projectId"
    fun fileManager(projectId: String) = "file_manager/$projectId"
}
