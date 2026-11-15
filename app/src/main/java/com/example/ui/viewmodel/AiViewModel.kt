package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiFilePatchParser
import com.example.data.ai.AiGeneratedFile
import com.example.data.ai.MultiAiClient
import com.example.data.local.AiChatDao
import com.example.data.local.AiChatMessageEntity
import com.example.data.local.AiProviderDao
import com.example.data.local.AiProviderEntity
import com.example.data.local.FileHistoryDao
import com.example.data.local.FileHistoryEntity
import com.example.data.local.ProjectDao
import com.example.data.local.ProjectEntity
import com.example.data.local.SecureKeyStore
import com.example.data.project.ProjectFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class AiUiState(
    val selectedProject: ProjectEntity? = null,
    val activeProvider: AiProviderEntity? = null,
    val messages: List<AiChatMessageEntity> = emptyList(),
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val appliedNotice: String? = null
)

class AiViewModel(
    private val projectDao: ProjectDao,
    private val aiProviderDao: AiProviderDao,
    private val aiChatDao: AiChatDao,
    private val fileHistoryDao: FileHistoryDao,
    private val fileManager: ProjectFileManager,
    private val secureKeyStore: SecureKeyStore,
    private val multiAiClient: MultiAiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private var chatJob: Job? = null

    init {
        viewModelScope.launch {
            aiProviderDao.observeAll().collect { providers ->
                val active = providers.firstOrNull { it.isDefault && it.isEnabled }
                    ?: providers.firstOrNull { it.isEnabled }
                _uiState.value = _uiState.value.copy(activeProvider = active)
            }
        }
    }

    fun selectProject(projectId: String?) {
        viewModelScope.launch {
            val proj = if (projectId.isNullOrBlank()) null else projectDao.getById(projectId)
            val channelId = proj?.id ?: "global_workspace"
            _uiState.value = _uiState.value.copy(selectedProject = proj)
            chatJob?.cancel()
            chatJob = viewModelScope.launch {
                aiChatDao.observeForProject(channelId).collect { list ->
                    _uiState.value = _uiState.value.copy(messages = list)
                }
            }
        }
    }

    fun sendPrompt(prompt: String, includeProjectFiles: Boolean) {
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isBlank() || _uiState.value.isGenerating) return

        viewModelScope.launch {
            val provider = aiProviderDao.getDefaultActive()
                ?: aiProviderDao.getAll().firstOrNull { it.isEnabled }
            if (provider == null) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Nenhum provedor de IA ativo. Configure um em AI Providers."
                )
                return@launch
            }
            val apiKey = secureKeyStore.getApiKey(provider.id)
            if (apiKey.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Chave API ausente para ${provider.name}. Acesse AI Providers para adicionar sua chave."
                )
                return@launch
            }

            val project = _uiState.value.selectedProject
            val channelId = project?.id ?: "global_workspace"
            val now = System.currentTimeMillis()

            aiChatDao.insert(
                AiChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = channelId,
                    role = "USER",
                    providerName = provider.name,
                    modelName = provider.selectedModel,
                    content = cleanPrompt,
                    generatedFilesJson = "",
                    createdAt = now
                )
            )

            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                errorMessage = null,
                appliedNotice = null
            )

            val projectContext = if (project != null && includeProjectFiles) {
                buildProjectContextSnippet(project)
            } else {
                ""
            }

            val systemPrompt = """
                Você é o Code X AI, um engenheiro Android Sênior integrado ao IDE Code X.
                Responda sempre em Português do Brasil de forma objetiva e técnica.
                Quando criar ou modificar arquivos de código, envolva CADA arquivo exatamente neste formato para que o Code X possa aplicá-lo automaticamente ao projeto:
                <<<FILE: caminho/relativo/Arquivo.ext>>>
                // código completo aqui
                <<<END_FILE>>>
                Nunca use código incompleto ou '...' quando gerar arquivos.
            """.trimIndent()

            val fullUserPrompt = if (projectContext.isNotBlank()) {
                "$cleanPrompt\n\n--- CONTEXTO DO PROJETO (${project?.name} | ${project?.techStack}) ---\n$projectContext"
            } else {
                cleanPrompt
            }

            val history = _uiState.value.messages
                .filter { it.role == "USER" || it.role == "ASSISTANT" }
                .takeLast(8)
                .map { it.role to it.content }

            try {
                val result = multiAiClient.sendChat(
                    provider = provider,
                    apiKey = apiKey,
                    systemPrompt = systemPrompt,
                    history = history,
                    userPrompt = fullUserPrompt
                )
                val extractedFiles = AiFilePatchParser.extractFiles(result.text)
                val filesJson = if (extractedFiles.isNotEmpty()) AiFilePatchParser.toJson(extractedFiles) else ""

                aiChatDao.insert(
                    AiChatMessageEntity(
                        id = UUID.randomUUID().toString(),
                        projectId = channelId,
                        role = "ASSISTANT",
                        providerName = result.providerName,
                        modelName = result.modelName,
                        content = result.text,
                        generatedFilesJson = filesJson,
                        createdAt = System.currentTimeMillis()
                    )
                )
                _uiState.value = _uiState.value.copy(isGenerating = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    errorMessage = e.localizedMessage ?: "Erro ao consultar ${provider.name}."
                )
            }
        }
    }

    fun applyGeneratedFilesToProject(files: List<AiGeneratedFile>) {
        val project = _uiState.value.selectedProject ?: run {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Selecione um projeto no topo da tela para aplicar os arquivos gerados."
            )
            return
        }
        viewModelScope.launch {
            val root = File(project.rootPath)
            withContext(Dispatchers.IO) {
                for (file in files) {
                    fileManager.writeFile(root, file.relativePath, file.content)
                    fileHistoryDao.insert(
                        FileHistoryEntity(
                            id = UUID.randomUUID().toString(),
                            projectId = project.id,
                            relativePath = file.relativePath,
                            content = file.content,
                            summary = "Gerado por Code X AI",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            projectDao.upsert(project.copy(updatedAt = System.currentTimeMillis()))
            _uiState.value = _uiState.value.copy(
                appliedNotice = "${files.size} arquivo(s) gravado(s) em '${project.name}'!"
            )
        }
    }

    fun clearHistory() {
        val channelId = _uiState.value.selectedProject?.id ?: "global_workspace"
        viewModelScope.launch {
            aiChatDao.clearForProject(channelId)
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, appliedNotice = null)
    }

    private suspend fun buildProjectContextSnippet(project: ProjectEntity): String = withContext(Dispatchers.IO) {
        val root = File(project.rootPath)
        val all = fileManager.listAllFilesFlat(root)
        val sb = StringBuilder()
        sb.append("Arquivos existentes:\n")
        all.take(30).forEach { sb.append("- ${it.relativePath} (${it.sizeBytes} B)\n") }
        sb.append("\nPrincipais trechos:\n")
        all.filter {
            it.extension in setOf("kt", "java", "xml", "html", "css", "js", "kts", "gradle")
        }.take(4).forEach { node ->
            val content = fileManager.readFile(root, node.relativePath).take(2500)
            sb.append("### ${node.relativePath}\n```\n$content\n```\n")
        }
        sb.toString()
    }
}
