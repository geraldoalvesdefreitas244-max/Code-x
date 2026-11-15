package com.example.data.ai

import com.example.data.local.AiProviderEntity
import com.example.data.local.ProviderType

object AiProviderCatalog {

    fun defaultProviders(): List<AiProviderEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            AiProviderEntity(
                id = "gemini",
                name = ProviderType.GEMINI.label,
                providerType = ProviderType.GEMINI.name,
                baseUrl = ProviderType.GEMINI.defaultBaseUrl,
                selectedModel = "gemini-2.5-flash",
                availableModelsCsv = "gemini-2.5-flash,gemini-2.5-pro,gemini-2.5-flash-lite",
                isEnabled = true,
                isDefault = true,
                updatedAt = now
            ),
            AiProviderEntity(
                id = "openai",
                name = ProviderType.OPENAI.label,
                providerType = ProviderType.OPENAI.name,
                baseUrl = ProviderType.OPENAI.defaultBaseUrl,
                selectedModel = "gpt-4.1-mini",
                availableModelsCsv = "gpt-4.1,gpt-4.1-mini,gpt-4o,o4-mini",
                isEnabled = true,
                isDefault = false,
                updatedAt = now
            ),
            AiProviderEntity(
                id = "anthropic",
                name = ProviderType.ANTHROPIC.label,
                providerType = ProviderType.ANTHROPIC.name,
                baseUrl = ProviderType.ANTHROPIC.defaultBaseUrl,
                selectedModel = "claude-sonnet-4-20250514",
                availableModelsCsv = "claude-sonnet-4-20250514,claude-3-7-sonnet-latest,claude-3-5-haiku-latest",
                isEnabled = true,
                isDefault = false,
                updatedAt = now
            ),
            AiProviderEntity(
                id = "deepseek",
                name = ProviderType.DEEPSEEK.label,
                providerType = ProviderType.DEEPSEEK.name,
                baseUrl = ProviderType.DEEPSEEK.defaultBaseUrl,
                selectedModel = "deepseek-chat",
                availableModelsCsv = "deepseek-chat,deepseek-reasoner",
                isEnabled = true,
                isDefault = false,
                updatedAt = now
            ),
            AiProviderEntity(
                id = "mistral",
                name = ProviderType.MISTRAL.label,
                providerType = ProviderType.MISTRAL.name,
                baseUrl = ProviderType.MISTRAL.defaultBaseUrl,
                selectedModel = "codestral-latest",
                availableModelsCsv = "codestral-latest,mistral-large-latest,mistral-small-latest",
                isEnabled = true,
                isDefault = false,
                updatedAt = now
            ),
            AiProviderEntity(
                id = "groq",
                name = ProviderType.GROQ.label,
                providerType = ProviderType.GROQ.name,
                baseUrl = ProviderType.GROQ.defaultBaseUrl,
                selectedModel = "llama-3.3-70b-versatile",
                availableModelsCsv = "llama-3.3-70b-versatile,qwen-qwq-32b,mixtral-8x7b-32768",
                isEnabled = true,
                isDefault = false,
                updatedAt = now
            ),
            AiProviderEntity(
                id = "openrouter",
                name = ProviderType.OPENROUTER.label,
                providerType = ProviderType.OPENROUTER.name,
                baseUrl = ProviderType.OPENROUTER.defaultBaseUrl,
                selectedModel = "anthropic/claude-3.7-sonnet",
                availableModelsCsv = "anthropic/claude-3.7-sonnet,openai/gpt-4.1-mini,google/gemini-2.5-flash",
                isEnabled = true,
                isDefault = false,
                updatedAt = now
            ),
            AiProviderEntity(
                id = "xai",
                name = ProviderType.XAI.label,
                providerType = ProviderType.XAI.name,
                baseUrl = ProviderType.XAI.defaultBaseUrl,
                selectedModel = "grok-3-mini",
                availableModelsCsv = "grok-3,grok-3-mini,grok-2-latest",
                isEnabled = true,
                isDefault = false,
                updatedAt = now
            )
        )
    }
}
