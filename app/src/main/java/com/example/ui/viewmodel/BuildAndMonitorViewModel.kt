package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.build.ApkInstallerHelper
import com.example.data.build.BuildServerClient
import com.example.data.build.BuildServerHealth
import com.example.data.local.BuildRecordDao
import com.example.data.local.BuildRecordEntity
import com.example.data.local.BuildStatus
import com.example.data.local.ProjectDao
import com.example.data.local.ProjectEntity
import com.example.data.local.SecureKeyStore
import com.example.data.local.TechStack
import com.example.data.monitor.DeviceMonitorRepository
import com.example.data.monitor.DeviceTelemetry
import com.example.data.project.CodeAnalyzer
import com.example.data.project.DiagnosticSeverity
import com.example.data.project.ProjectFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class BuildUiState(
    val isBuilding: Boolean = false,
    val liveLogs: String = "",
    val serverHealth: BuildServerHealth? = null,
    val statusMessage: String? = null
)

class BuildAndMonitorViewModel(
    private val projectDao: ProjectDao,
    private val buildRecordDao: BuildRecordDao,
    private val fileManager: ProjectFileManager,
    private val secureKeyStore: SecureKeyStore,
    private val buildServerClient: BuildServerClient,
    private val monitorRepository: DeviceMonitorRepository
) : ViewModel() {

    private val _buildState = MutableStateFlow(BuildUiState())
    val buildState: StateFlow<BuildUiState> = _buildState.asStateFlow()

    val buildHistory: StateFlow<List<BuildRecordEntity>> = buildRecordDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _telemetry = MutableStateFlow(monitorRepository.collectTelemetry())
    val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                _telemetry.value = withContext(Dispatchers.IO) {
                    monitorRepository.collectTelemetry()
                }
                delay(1500)
            }
        }
    }

    fun refreshTelemetryNow() {
        viewModelScope.launch {
            _telemetry.value = withContext(Dispatchers.IO) {
                monitorRepository.collectTelemetry()
            }
        }
    }

    fun checkBuildServerHealth(customUrl: String? = null) {
        viewModelScope.launch {
            val url = customUrl ?: secureKeyStore.editorPreferences.first().buildServerUrl
            val health = buildServerClient.checkHealth(url)
            _buildState.value = _buildState.value.copy(serverHealth = health)
        }
    }

    fun compileProject(
        project: ProjectEntity,
        buildType: String, // DEBUG, RELEASE, WEB_BUNDLE
        keystoreAlias: String = "",
        keystorePassword: String = ""
    ) {
        if (_buildState.value.isBuilding) return

        viewModelScope.launch {
            val start = System.currentTimeMillis()
            val buildId = "bld_${UUID.randomUUID().toString().take(8)}"
            val logBuilder = StringBuilder()

            fun appendLog(chunk: String) {
                logBuilder.append(chunk)
                _buildState.value = _buildState.value.copy(liveLogs = logBuilder.toString())
            }

            _buildState.value = _buildState.value.copy(
                isBuilding = true,
                liveLogs = "",
                statusMessage = null
            )

            projectDao.upsert(
                project.copy(
                    lastBuildStatus = BuildStatus.RUNNING.name,
                    lastBuildMessage = "Compilando ($buildType)...",
                    updatedAt = System.currentTimeMillis()
                )
            )

            val root = File(project.rootPath)
            appendLog("[Code X] Executando análise estática em '${project.name}'...\n")
            val filesMap = withContext(Dispatchers.IO) {
                fileManager.listAllFilesFlat(root).associate {
                    it.relativePath to fileManager.readFile(root, it.relativePath)
                }
            }
            val errors = CodeAnalyzer.analyzeProject(filesMap)
                .filter { it.severity == DiagnosticSeverity.ERROR }

            if (errors.isNotEmpty()) {
                errors.forEach { err ->
                    appendLog("[ERRO] ${err.filePath}:${err.line} -> ${err.message}\n")
                }
                val duration = System.currentTimeMillis() - start
                saveBuildResult(
                    buildId = buildId,
                    project = project,
                    buildType = buildType,
                    status = BuildStatus.FAILED,
                    apkPath = "",
                    apkSize = 0L,
                    logs = logBuilder.toString(),
                    durationMs = duration,
                    summary = "${errors.size} erro(s) de sintaxe impediram a compilação."
                )
                return@launch
            }
            appendLog("[Code X] Verificação estática OK (${filesMap.size} arquivos).\n")

            if (project.techStack == TechStack.WEB_HTML_JS.name && buildType == "WEB_BUNDLE") {
                val outZip = File(fileManager.apksRootDir(), "${project.id}_web_bundle.zip")
                withContext(Dispatchers.IO) { fileManager.zipProject(root, outZip) }
                appendLog("[Code X] Pacote Web gerado: ${outZip.name} (${outZip.length() / 1024} KB)\n")
                saveBuildResult(
                    buildId = buildId,
                    project = project,
                    buildType = buildType,
                    status = BuildStatus.SUCCESS,
                    apkPath = outZip.absolutePath,
                    apkSize = outZip.length(),
                    logs = logBuilder.toString(),
                    durationMs = System.currentTimeMillis() - start,
                    summary = "Pacote Web pronto (${outZip.length() / 1024} KB)"
                )
                return@launch
            }

            val prefs = secureKeyStore.editorPreferences.first()
            val zipFile = File(fileManager.apksRootDir(), "${project.id}_src.zip")
            withContext(Dispatchers.IO) { fileManager.zipProject(root, zipFile) }

            val result = buildServerClient.submitBuildAndStream(
                baseUrl = prefs.buildServerUrl,
                projectZip = zipFile,
                projectName = project.name,
                packageName = project.packageName,
                buildType = buildType,
                keystoreAlias = keystoreAlias,
                keystorePassword = keystorePassword,
                onProgress = { chunk ->
                    appendLog(chunk)
                }
            )
            zipFile.delete()

            val duration = System.currentTimeMillis() - start
            result.onSuccess { apkFile ->
                val finalDest = File(fileManager.apksRootDir(), apkFile.name)
                if (apkFile.absolutePath != finalDest.absolutePath) {
                    apkFile.copyTo(finalDest, overwrite = true)
                    apkFile.delete()
                }
                saveBuildResult(
                    buildId = buildId,
                    project = project,
                    buildType = buildType,
                    status = BuildStatus.SUCCESS,
                    apkPath = finalDest.absolutePath,
                    apkSize = finalDest.length(),
                    logs = logBuilder.toString(),
                    durationMs = duration,
                    summary = "APK $buildType compilado (${finalDest.length() / 1024} KB)"
                )
            }.onFailure { err ->
                appendLog("\n[ERRO] ${err.localizedMessage ?: "Falha na compilação."}\n")
                saveBuildResult(
                    buildId = buildId,
                    project = project,
                    buildType = buildType,
                    status = BuildStatus.FAILED,
                    apkPath = "",
                    apkSize = 0L,
                    logs = logBuilder.toString(),
                    durationMs = duration,
                    summary = err.localizedMessage ?: "Falha ao conectar ao servidor Gradle"
                )
            }
        }
    }

    fun installApk(context: Context, apkPath: String) {
        val file = File(apkPath)
        if (!ApkInstallerHelper.canRequestPackageInstalls(context)) {
            _buildState.value = _buildState.value.copy(
                statusMessage = "Permita 'Instalar apps desconhecidos' nas configurações para instalar o APK."
            )
            ApkInstallerHelper.openInstallPermissionSettings(context)
            return
        }
        ApkInstallerHelper.installApk(context, file).onFailure { e ->
            _buildState.value = _buildState.value.copy(
                statusMessage = e.localizedMessage ?: "Erro ao abrir instalador de APK."
            )
        }
    }

    fun shareApk(context: Context, apkPath: String) {
        ApkInstallerHelper.shareApk(context, File(apkPath)).onFailure { e ->
            _buildState.value = _buildState.value.copy(
                statusMessage = e.localizedMessage ?: "Erro ao compartilhar APK."
            )
        }
    }

    fun clearStatusMessage() {
        _buildState.value = _buildState.value.copy(statusMessage = null)
    }

    private suspend fun saveBuildResult(
        buildId: String,
        project: ProjectEntity,
        buildType: String,
        status: BuildStatus,
        apkPath: String,
        apkSize: Long,
        logs: String,
        durationMs: Long,
        summary: String
    ) {
        val now = System.currentTimeMillis()
        buildRecordDao.upsert(
            BuildRecordEntity(
                id = buildId,
                projectId = project.id,
                projectName = project.name,
                buildType = buildType,
                status = status.name,
                apkPath = apkPath,
                apkSizeBytes = apkSize,
                logs = logs,
                durationMs = durationMs,
                createdAt = now
            )
        )
        projectDao.upsert(
            project.copy(
                lastBuildStatus = status.name,
                lastBuildMessage = summary,
                lastBuiltAt = now,
                updatedAt = now
            )
        )
        _buildState.value = _buildState.value.copy(
            isBuilding = false,
            statusMessage = summary
        )
    }
}
