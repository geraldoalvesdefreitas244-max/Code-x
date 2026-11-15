package com.example.data.ai

import com.example.data.local.AiProviderEntity
import com.example.data.local.ProviderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiCompletionResult(
    val text: String,
    val latencyMs: Long,
    val providerName: String,
    val modelName: String
)

data class AiConnectionTestResult(
    val success: Boolean,
    val message: String,
    val latencyMs: Long
)

class MultiAiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(provider: AiProviderEntity, apiKey: String): AiConnectionTestResult {
        if (apiKey.isBlank()) {
            return AiConnectionTestResult(false, "Chave API não configurada para ${provider.name}.", 0L)
        }
        val start = System.currentTimeMillis()
        return try {
            val result = sendChat(
                provider = provider,
                apiKey = apiKey,
                systemPrompt = "You are a connectivity check. Reply only with OK.",
                history = emptyList(),
                userPrompt = "Reply with OK."
            )
            val latency = System.currentTimeMillis() - start
            AiConnectionTestResult(
                success = true,
                message = "Conectado (${provider.selectedModel}) em ${latency}ms: ${result.text.take(40)}",
                latencyMs = latency
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            AiConnectionTestResult(
                success = false,
                message = e.localizedMessage ?: "Falha de conexão com ${provider.name}",
                latencyMs = latency
            )
        }
    }

    suspend fun sendChat(
        provider: AiProviderEntity,
        apiKey: String,
        systemPrompt: String,
        history: List<Pair<String, String>>,
        userPrompt: String
    ): AiCompletionResult = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) {
            "Nenhuma chave API configurada para ${provider.name}. Acesse AI Providers para inserir sua chave."
        }
        val start = System.currentTimeMillis()
        val type = runCatching { ProviderType.valueOf(provider.providerType) }.getOrDefault(ProviderType.CUSTOM)

        val responseText = when (type) {
            ProviderType.GEMINI -> callGeminiRest(provider, apiKey, systemPrompt, history, userPrompt)
            ProviderType.ANTHROPIC -> callAnthropicMessages(provider, apiKey, systemPrompt, history, userPrompt)
            else -> callOpenAiCompatible(provider, apiKey, systemPrompt, history, userPrompt)
        }

        AiCompletionResult(
            text = responseText,
            latencyMs = System.currentTimeMillis() - start,
            providerName = provider.name,
            modelName = provider.selectedModel
        )
    }

    private fun callGeminiRest(
        provider: AiProviderEntity,
        apiKey: String,
        systemPrompt: String,
        history: List<Pair<String, String>>,
        userPrompt: String
    ): String {
        val base = provider.baseUrl.trimEnd('/')
        val url = "$base/models/${provider.selectedModel}:generateContent"

        val contents = JSONArray()
        for ((role, text) in history.takeLast(12)) {
            val geminiRole = if (role == "ASSISTANT") "model" else "user"
            contents.put(
                JSONObject().apply {
                    put("role", geminiRole)
                    put("parts", JSONArray().put(JSONObject().put("text", text)))
                }
            )
        }
        contents.put(
            JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
            }
        )

        val payload = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            })
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.3)
            })
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException(extractErrorMessage(body, response.code))
            }
            val json = JSONObject(body)
            val candidates = json.optJSONArray("candidates") ?: throw IllegalStateException("Resposta vazia do Gemini.")
            val first = candidates.optJSONObject(0) ?: throw IllegalStateException("Sem candidatos na resposta.")
            val parts = first.optJSONObject("content")?.optJSONArray("parts")
                ?: throw IllegalStateException("Conteúdo ausente na resposta.")
            val textBuilder = StringBuilder()
            for (i in 0 until parts.length()) {
                val partText = parts.optJSONObject(i)?.optString("text").orEmpty()
                if (partText.isNotEmpty()) textBuilder.append(partText)
            }
            return textBuilder.toString().ifBlank { throw IllegalStateException("Texto vazio retornado pelo modelo.") }
        }
    }

    private fun callAnthropicMessages(
        provider: AiProviderEntity,
        apiKey: String,
        systemPrompt: String,
        history: List<Pair<String, String>>,
        userPrompt: String
    ): String {
        val base = provider.baseUrl.trimEnd('/')
        val url = "$base/messages"

        val messages = JSONArray()
        for ((role, text) in history.takeLast(12)) {
            val mappedRole = if (role == "ASSISTANT") "assistant" else "user"
            messages.put(JSONObject().put("role", mappedRole).put("content", text))
        }
        messages.put(JSONObject().put("role", "user").put("content", userPrompt))

        val payload = JSONObject().apply {
            put("model", provider.selectedModel)
            put("max_tokens", 4096)
            put("system", systemPrompt)
            put("messages", messages)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException(extractErrorMessage(body, response.code))
            }
            val json = JSONObject(body)
            val contentArray = json.optJSONArray("content") ?: throw IllegalStateException("Resposta vazia da Anthropic.")
            val sb = StringBuilder()
            for (i in 0 until contentArray.length()) {
                val item = contentArray.optJSONObject(i)
                if (item?.optString("type") == "text") {
                    sb.append(item.optString("text"))
                }
            }
            return sb.toString().ifBlank { throw IllegalStateException("Anthropic retornou conteúdo vazio.") }
        }
    }

    private fun callOpenAiCompatible(
        provider: AiProviderEntity,
        apiKey: String,
        systemPrompt: String,
        history: List<Pair<String, String>>,
        userPrompt: String
    ): String {
        val base = provider.baseUrl.trimEnd('/')
        val url = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"

        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", systemPrompt))
        for ((role, text) in history.takeLast(12)) {
            val mappedRole = if (role == "ASSISTANT") "assistant" else "user"
            messages.put(JSONObject().put("role", mappedRole).put("content", text))
        }
        messages.put(JSONObject().put("role", "user").put("content", userPrompt))

        val payload = JSONObject().apply {
            put("model", provider.selectedModel)
            put("messages", messages)
            put("temperature", 0.3)
        }

        val builder = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")

        if (provider.providerType == ProviderType.OPENROUTER.name) {
            builder.addHeader("HTTP-Referer", "https://codex.android.app")
            builder.addHeader("X-Title", "Code X Mobile IDE")
        }

        val request = builder.post(payload.toString().toRequestBody(jsonMediaType)).build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException(extractErrorMessage(body, response.code))
            }
            val json = JSONObject(body)
            val choices = json.optJSONArray("choices") ?: throw IllegalStateException("Resposta sem choices.")
            val msg = choices.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty()
            return msg.ifBlank { throw IllegalStateException("O provedor retornou uma resposta vazia.") }
        }
    }

    private fun extractErrorMessage(body: String, httpCode: Int): String {
        return try {
            val json = JSONObject(body)
            val errObj = json.optJSONObject("error")
            val msg = errObj?.optString("message")?.takeIf { it.isNotBlank() }
                ?: json.optString("message").takeIf { it.isNotBlank() }
                ?: body.take(220)
            "HTTP $httpCode: $msg"
        } catch (_: Exception) {
            "HTTP $httpCode: ${body.take(220).ifBlank { "Erro na API do provedor" }}"
        }
    }
}
