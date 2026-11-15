package com.example.data.build

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class BuildServerHealth(
    val online: Boolean,
    val message: String,
    val androidSdkDetected: Boolean = false,
    val gradleVersion: String = ""
)

data class RemoteBuildSnapshot(
    val buildId: String,
    val status: String, // queued, running, success, failed
    val logs: String,
    val apkDownloadPath: String? = null,
    val errorMessage: String? = null
)

class BuildServerClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun checkHealth(baseUrl: String): BuildServerHealth = withContext(Dispatchers.IO) {
        val cleanUrl = baseUrl.trim().trimEnd('/')
        if (cleanUrl.isBlank()) {
            return@withContext BuildServerHealth(false, "URL do servidor de build não configurada.")
        }
        return@withContext try {
            val req = Request.Builder().url("$cleanUrl/health").get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    BuildServerHealth(false, "Servidor respondeu HTTP ${resp.code}")
                } else {
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    BuildServerHealth(
                        online = true,
                        message = json?.optString("message")?.ifBlank { "Servidor Gradle online" }
                            ?: "Servidor Gradle online",
                        androidSdkDetected = json?.optBoolean("androidSdk", false) ?: false,
                        gradleVersion = json?.optString("gradleVersion", "").orEmpty()
                    )
                }
            }
        } catch (e: Exception) {
            BuildServerHealth(
                online = false,
                message = "Servidor inacessível: ${e.localizedMessage ?: "conexão recusada"}"
            )
        }
    }

    suspend fun submitBuildAndStream(
        baseUrl: String,
        projectZip: File,
        projectName: String,
        packageName: String,
        buildType: String, // DEBUG or RELEASE
        keystoreAlias: String = "",
        keystorePassword: String = "",
        onProgress: suspend (String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val cleanUrl = baseUrl.trim().trimEnd('/')
        return@withContext runCatching {
            onProgress("[Code X] Enviando projeto (${projectZip.length() / 1024} KB) para $cleanUrl...\n")

            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "projectZip",
                    projectZip.name,
                    projectZip.asRequestBody("application/zip".toMediaType())
                )
                .addFormDataPart("projectName", projectName)
                .addFormDataPart("packageName", packageName)
                .addFormDataPart("buildType", buildType)
                .addFormDataPart("keystoreAlias", keystoreAlias)
                .addFormDataPart("keystorePassword", keystorePassword)
                .build()

            val postReq = Request.Builder()
                .url("$cleanUrl/api/builds")
                .post(multipart)
                .build()

            val buildId = client.newCall(postReq).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    error("Falha ao enviar build (HTTP ${resp.code}): $body")
                }
                JSONObject(body).getString("buildId")
            }

            onProgress("[Code X] Build ID: $buildId — executando Gradle...\n")

            var finalSnapshot: RemoteBuildSnapshot? = null
            var lastLogLen = 0
            for (attempt in 0 until 300) {
                delay(1500)
                val pollReq = Request.Builder().url("$cleanUrl/api/builds/$buildId").get().build()
                val snapshot = client.newCall(pollReq).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        error("Erro ao consultar status do build (HTTP ${resp.code})")
                    }
                    val json = JSONObject(body)
                    RemoteBuildSnapshot(
                        buildId = buildId,
                        status = json.optString("status", "running"),
                        logs = json.optString("logs", ""),
                        apkDownloadPath = json.optString("apkUrl", "").takeIf { it.isNotBlank() },
                        errorMessage = json.optString("error", "").takeIf { it.isNotBlank() }
                    )
                }

                if (snapshot.logs.length > lastLogLen) {
                    onProgress(snapshot.logs.substring(lastLogLen))
                    lastLogLen = snapshot.logs.length
                }

                if (snapshot.status == "success" || snapshot.status == "failed") {
                    finalSnapshot = snapshot
                    break
                }
            }

            val done = finalSnapshot ?: error("Tempo limite excedido aguardando Gradle Build Server.")
            if (done.status == "failed") {
                error(done.errorMessage ?: "Compilação Gradle falhou. Verifique os logs acima.")
            }

            val apkRel = done.apkDownloadPath ?: "/api/builds/$buildId/apk"
            val apkUrl = if (apkRel.startsWith("http")) apkRel else "$cleanUrl$apkRel"
            onProgress("\n[Code X] Baixando APK compilado de $apkUrl...\n")

            val outDir = File(projectZip.parentFile, "apks").apply { mkdirs() }
            val safeName = projectName.replace(Regex("[^a-zA-Z0-9_-]"), "_").lowercase()
            val outApk = File(outDir, "${safeName}_${buildType.lowercase()}_${System.currentTimeMillis()}.apk")

            val dlReq = Request.Builder().url(apkUrl).get().build()
            client.newCall(dlReq).execute().use { resp ->
                if (!resp.isSuccessful) error("Falha ao baixar APK (HTTP ${resp.code})")
                val stream = resp.body?.byteStream() ?: error("Corpo do APK vazio.")
                FileOutputStream(outApk).use { fos -> stream.copyTo(fos) }
            }
            onProgress("[Code X] APK salvo com sucesso: ${outApk.name} (${outApk.length() / 1024} KB)\n")
            outApk
        }
    }
}
