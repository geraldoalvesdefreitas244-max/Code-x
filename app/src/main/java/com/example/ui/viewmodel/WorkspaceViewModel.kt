package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.MultiAiClient
import com.example.data.local.AiProviderDao
import com.example.data.local.AiProviderEntity
import com.example.data.local.BuildRecordDao
import com.example.data.local.BuildRecordEntity
import com.example.data.local.EditorPreferences
import com.example.data.local.ProjectDao
import com.example.data.local.ProjectEntity
import com.example.data.local.ProviderType
import com.example.data.local.SecureKeyStore
import com.example.data.local.TechStack
import com.example.data.project.ProjectFileManager
import com.example.data.project.ProjectTemplateGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class ProviderUiItem(
    val entity: AiProviderEntity,
    val hasApiKey: Boolean,
    val maskedKey: String
)

class WorkspaceViewModel(
    private val projectDao: ProjectDao,
    private val aiProviderDao: AiProviderDao,
    private val buildRecordDao: BuildRecordDao,
    private val fileManager: ProjectFileManager,
    private val secureKeyStore: SecureKeyStore,
    private val multiAiClient: MultiAiClient
) : ViewModel() {

    private val _statusBanner = MutableStateFlow<Pair<Boolean, String>?>(null)
    val statusBanner: StateFlow<Pair<Boolean, String>?> = _statusBanner.asStateFlow()

    private val _providerItems = MutableStateFlow<List<ProviderUiItem>>(emptyList())
    val providerItems: StateFlow<List<ProviderUiItem>> = _providerItems.asStateFlow()

    private val _testingProviderId = MutableStateFlow<String?>(null)
    val testingProviderId: StateFlow<String?> = _testingProviderId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val projects: StateFlow<List<ProjectEntity>> = secureKeyStore.activeUserId
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(emptyList()) else projectDao.observeByOwner(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val latestBuild: StateFlow<BuildRecordEntity?> = buildRecordDao.observeLatest()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val editorPreferences: StateFlow<EditorPreferences> = secureKeyStore.editorPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), EditorPreferences())

    init {
        viewModelScope.launch {
            aiProviderDao.observeAll().collect { providers ->
                val items = providers.map { p ->
                    ProviderUiItem(
                        entity = p,
                        hasApiKey = secureKeyStore.hasApiKey(p.id),
                        maskedKey = secureKeyStore.getMaskedKey(p.id)
                    )
                }
                _providerItems.value = items
            }
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun createProject(
        ownerUserId: String,
        name: String,
        packageName: String,
        versionName: String,
        versionCode: Int,
        techStack: TechStack,
        iconPreset: String,
        onCreated: (ProjectEntity) -> Unit
    ) {
        viewModelScope.launch {
            val cleanName = name.trim()
            val cleanPkg = packageName.trim()
            if (cleanName.length < 2) {
                _statusBanner.value = false to "Informe um nome válido para o aplicativo."
                return@launch
            }
            if (!Regex("^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+$").matches(cleanPkg)) {
                _statusBanner.value = false to "Pacote inválido. Ex: com.empresa.meuapp"
                return@launch
            }
            val projectId = "proj_${UUID.randomUUID().toString().take(10)}"
            val rootDir = fileManager.projectDir(projectId)

            withContext(Dispatchers.IO) {
                ProjectTemplateGenerator.generateProjectFiles(
                    rootDir = rootDir,
                    appName = cleanName,
                    packageName = cleanPkg,
                    versionName = versionName.ifBlank { "1.0.0" },
                    versionCode = versionCode.coerceAtLeast(1),
                    techStack = techStack
                )
            }

            val now = System.currentTimeMillis()
            val entity = ProjectEntity(
                id = projectId,
                ownerUserId = ownerUserId,
                name = cleanName,
                packageName = cleanPkg,
                versionName = versionName.ifBlank { "1.0.0" },
                versionCode = versionCode.coerceAtLeast(1),
                techStack = techStack.name,
                iconPreset = iconPreset,
                rootPath = rootDir.absolutePath,
                createdAt = now,
                updatedAt = now
            )
            projectDao.upsert(entity)
            _statusBanner.value = true to "Projeto '$cleanName' criado com sucesso!"
            onCreated(entity)
        }
    }

    fun renameProject(project: ProjectEntity, newName: String) {
        val clean = newName.trim()
        if (clean.length < 2) {
            _statusBanner.value = false to "Nome muito curto."
            return
        }
        viewModelScope.launch {
            projectDao.upsert(project.copy(name = clean, updatedAt = System.currentTimeMillis()))
            _statusBanner.value = true to "Projeto renomeado para '$clean'."
        }
    }

    fun duplicateProject(project: ProjectEntity) {
        viewModelScope.launch {
            val newId = "proj_${UUID.randomUUID().toString().take(10)}"
            val newDir = fileManager.projectDir(newId)
            withContext(Dispatchers.IO) {
                fileManager.duplicateDirectory(File(project.rootPath), newDir)
            }
            val now = System.currentTimeMillis()
            val copy = project.copy(
                id = newId,
                name = "${project.name} (Cópia)",
                rootPath = newDir.absolutePath,
                createdAt = now,
                updatedAt = now
            )
            projectDao.upsert(copy)
            _statusBanner.value = true to "Projeto duplicado como '${copy.name}'."
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                fileManager.deleteProjectDirectory(File(project.rootPath))
            }
            projectDao.deleteById(project.id)
            _statusBanner.value = true to "Projeto '${project.name}' excluído."
        }
    }

    fun exportProjectZip(project: ProjectEntity, destinationUri: Uri) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                fileManager.exportProjectToUri(File(project.rootPath), destinationUri)
            }
            res.onSuccess { bytes ->
                _statusBanner.value = true to "Projeto exportado (${bytes / 1024} KB)."
            }.onFailure { e ->
                _statusBanner.value = false to (e.localizedMessage ?: "Erro ao exportar projeto.")
            }
        }
    }

    suspend fun loadApiKey(providerId: String): String = secureKeyStore.getApiKey(providerId)

    fun saveProviderConfig(
        provider: AiProviderEntity,
        apiKey: String,
        selectedModel: String,
        baseUrl: String,
        isEnabled: Boolean
    ) {
        viewModelScope.launch {
            if (apiKey.isNotBlank()) {
                secureKeyStore.saveApiKey(provider.id, apiKey)
            }
            val models = provider.availableModelsCsv.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toMutableList()
            val cleanModel = selectedModel.trim()
            if (cleanModel.isNotBlank() && cleanModel !in models) {
                models.add(0, cleanModel)
            }
            aiProviderDao.upsert(
                provider.copy(
                    selectedModel = cleanModel.ifBlank { provider.selectedModel },
                    availableModelsCsv = models.joinToString(","),
                    baseUrl = baseUrl.trim().ifBlank { provider.baseUrl },
                    isEnabled = isEnabled,
                    updatedAt = System.currentTimeMillis()
                )
            )
            refreshProviderCache()
            _statusBanner.value = true to "Configuração de ${provider.name} salva com criptografia."
        }
    }

    fun addCustomProvider(name: String, baseUrl: String, model: String, apiKey: String) {
        viewModelScope.launch {
            val cleanName = name.trim()
            val cleanUrl = baseUrl.trim()
            val cleanModel = model.trim()
            if (cleanName.isBlank() || !cleanUrl.startsWith("http") || cleanModel.isBlank()) {
                _statusBanner.value = false to "Preencha nome, URL (http/https) e modelo válido."
                return@launch
            }
            val id = "custom_${UUID.randomUUID().toString().take(8)}"
            if (apiKey.isNotBlank()) {
                secureKeyStore.saveApiKey(id, apiKey)
            }
            aiProviderDao.upsert(
                AiProviderEntity(
                    id = id,
                    name = cleanName,
                    providerType = ProviderType.CUSTOM.name,
                    baseUrl = cleanUrl,
                    selectedModel = cleanModel,
                    availableModelsCsv = cleanModel,
                    isEnabled = true,
                    isDefault = false,
                    updatedAt = System.currentTimeMillis()
                )
            )
            refreshProviderCache()
            _statusBanner.value = true to "Provedor '$cleanName' adicionado."
        }
    }

    fun removeApiKey(providerId: String) {
        viewModelScope.launch {
            secureKeyStore.removeApiKey(providerId)
            val current = aiProviderDao.getById(providerId)
            if (current != null) {
                aiProviderDao.upsert(current.copy(lastTestStatus = "Chave removida", lastLatencyMs = 0L))
            }
            refreshProviderCache()
            _statusBanner.value = true to "Chave API removida com segurança."
        }
    }

    fun toggleProviderEnabled(provider: AiProviderEntity, enabled: Boolean) {
        viewModelScope.launch {
            aiProviderDao.upsert(provider.copy(isEnabled = enabled, updatedAt = System.currentTimeMillis()))
        }
    }

    fun setDefaultProvider(providerId: String) {
        viewModelScope.launch {
            aiProviderDao.setDefault(providerId)
            _statusBanner.value = true to "IA padrão atualizada."
        }
    }

    fun testProviderConnection(provider: AiProviderEntity, overrideKey: String? = null) {
        viewModelScope.launch {
            _testingProviderId.value = provider.id
            val keyToUse = overrideKey?.takeIf { it.isNotBlank() } ?: secureKeyStore.getApiKey(provider.id)
            val result = multiAiClient.testConnection(provider, keyToUse)
            aiProviderDao.upsert(
                provider.copy(
                    lastTestStatus = if (result.success) "Online (${result.latencyMs}ms)" else "Erro: ${result.message}",
                    lastLatencyMs = result.latencyMs,
                    updatedAt = System.currentTimeMillis()
                )
            )
            refreshProviderCache()
            _testingProviderId.value = null
            _statusBanner.value = result.success to result.message
        }
    }

    fun updateEditorPreferences(transform: (EditorPreferences) -> EditorPreferences) {
        viewModelScope.launch {
            secureKeyStore.updateEditorPreferences(transform)
        }
    }

    private suspend fun refreshProviderCache() {
        val providers = aiProviderDao.getAll()
        _providerItems.value = providers.map { p ->
            ProviderUiItem(
                entity = p,
                hasApiKey = secureKeyStore.hasApiKey(p.id),
                maskedKey = secureKeyStore.getMaskedKey(p.id)
            )
        }
    }
}
