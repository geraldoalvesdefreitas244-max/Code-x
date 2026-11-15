package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TechStack(val label: String, val shortLabel: String) {
    KOTLIN_COMPOSE("Android Nativo (Kotlin + Compose)", "Kotlin"),
    JAVA_ANDROID("Android Nativo (Java)", "Java"),
    WEB_HTML_JS("Web App (HTML + CSS + JS)", "HTML/JS")
}

enum class ProviderType(val label: String, val defaultBaseUrl: String) {
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com/v1beta"),
    OPENAI("OpenAI", "https://api.openai.com/v1"),
    ANTHROPIC("Anthropic Claude", "https://api.anthropic.com/v1"),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com"),
    MISTRAL("Mistral AI", "https://api.mistral.ai/v1"),
    GROQ("Groq", "https://api.groq.com/openai/v1"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1"),
    XAI("xAI Grok", "https://api.x.ai/v1"),
    CUSTOM("Provedor Personalizado (OpenAI API)", "https://api.example.com/v1")
}

enum class BuildStatus(val label: String) {
    NONE("Não compilado"),
    RUNNING("Compilando..."),
    SUCCESS("Sucesso"),
    FAILED("Falha")
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val isEmailVerified: Boolean,
    val verificationCode: String,
    val authProvider: String,
    val cloudSyncEnabled: Boolean,
    val createdAt: Long
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val ownerUserId: String,
    val name: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val techStack: String,
    val iconPreset: String,
    val rootPath: String,
    val lastBuildStatus: String = BuildStatus.NONE.name,
    val lastBuildMessage: String = "Pronto para compilar",
    val lastBuiltAt: Long = 0L,
    val cloudSynced: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "ai_providers")
data class AiProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val providerType: String,
    val baseUrl: String,
    val selectedModel: String,
    val availableModelsCsv: String,
    val isEnabled: Boolean,
    val isDefault: Boolean,
    val lastTestStatus: String = "Não testado",
    val lastLatencyMs: Long = 0L,
    val updatedAt: Long
)

@Entity(tableName = "file_history")
data class FileHistoryEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val relativePath: String,
    val content: String,
    val summary: String,
    val createdAt: Long
)

@Entity(tableName = "build_records")
data class BuildRecordEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val projectName: String,
    val buildType: String,
    val status: String,
    val apkPath: String,
    val apkSizeBytes: Long,
    val logs: String,
    val durationMs: Long,
    val createdAt: Long
)

@Entity(tableName = "ai_messages")
data class AiChatMessageEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val role: String,
    val providerName: String,
    val modelName: String,
    val content: String,
    val generatedFilesJson: String,
    val createdAt: Long
)
