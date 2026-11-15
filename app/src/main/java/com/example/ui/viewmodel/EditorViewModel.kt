package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FileHistoryDao
import com.example.data.local.FileHistoryEntity
import com.example.data.local.ProjectDao
import com.example.data.local.ProjectEntity
import com.example.data.local.SecureKeyStore
import com.example.data.project.CodeAnalyzer
import com.example.data.project.CodeDiagnostic
import com.example.data.project.IntegratedTerminalEngine
import com.example.data.project.ProjectFileManager
import com.example.data.project.ProjectFileNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class OpenFileTab(
    val relativePath: String,
    val name: String,
    val content: String,
    val isDirty: Boolean = false
)

data class EditorUiState(
    val project: ProjectEntity? = null,
    val currentBrowserDir: String = "",
    val browserNodes: List<ProjectFileNode> = emptyList(),
    val openTabs: List<OpenFileTab> = emptyList(),
    val activeTabIndex: Int = -1,
    val diagnostics: List<CodeDiagnostic> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val terminalLines: List<String> = listOf("Code X Terminal v1.0 — digite 'help' para comandos."),
    val terminalRelDir: String = "",
    val historyEntries: List<FileHistoryEntity> = emptyList(),
    val feedbackMessage: String? = null
) {
    val activeTab: OpenFileTab?
        get() = openTabs.getOrNull(activeTabIndex)
}

class EditorViewModel(
    private val projectDao: ProjectDao,
    private val fileHistoryDao: FileHistoryDao,
    private val fileManager: ProjectFileManager,
    private val secureKeyStore: SecureKeyStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private var historyCollectorJob: Job? = null

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            val project = projectDao.getById(projectId) ?: return@launch
            val root = File(project.rootPath)
            val nodes = withContext(Dispatchers.IO) { fileManager.listDirectory(root, "") }
            val allFiles = withContext(Dispatchers.IO) { fileManager.listAllFilesFlat(root) }

            val defaultFile = allFiles.firstOrNull {
                it.name == "MainActivity.kt" || it.name == "MainActivity.java" || it.name == "index.html"
            } ?: allFiles.firstOrNull()

            val initialTabs = if (defaultFile != null && _uiState.value.project?.id != projectId) {
                val content = withContext(Dispatchers.IO) { fileManager.readFile(root, defaultFile.relativePath) }
                listOf(OpenFileTab(defaultFile.relativePath, defaultFile.name, content, false))
            } else if (_uiState.value.project?.id == projectId) {
                _uiState.value.openTabs
            } else {
                emptyList()
            }

            val activeIdx = if (initialTabs.isNotEmpty()) {
                if (_uiState.value.activeTabIndex in initialTabs.indices) _uiState.value.activeTabIndex else 0
            } else -1

            _uiState.value = _uiState.value.copy(
                project = project,
                currentBrowserDir = "",
                browserNodes = nodes,
                openTabs = initialTabs,
                activeTabIndex = activeIdx
            )
            refreshDiagnosticsAndSuggestions()
            observeActiveFileHistory()
        }
    }

    fun navigateDirectory(relDir: String) {
        val root = _uiState.value.project?.let { File(it.rootPath) } ?: return
        viewModelScope.launch {
            val nodes = withContext(Dispatchers.IO) { fileManager.listDirectory(root, relDir) }
            _uiState.value = _uiState.value.copy(currentBrowserDir = relDir, browserNodes = nodes)
        }
    }

    fun openFile(relativePath: String) {
        val project = _uiState.value.project ?: return
        val existingIdx = _uiState.value.openTabs.indexOfFirst { it.relativePath == relativePath }
        if (existingIdx >= 0) {
            _uiState.value = _uiState.value.copy(activeTabIndex = existingIdx)
            refreshDiagnosticsAndSuggestions()
            observeActiveFileHistory()
            return
        }
        viewModelScope.launch {
            val root = File(project.rootPath)
            val content = withContext(Dispatchers.IO) { fileManager.readFile(root, relativePath) }
            val name = relativePath.substringAfterLast('/')
            val updatedTabs = _uiState.value.openTabs + OpenFileTab(relativePath, name, content, false)
            _uiState.value = _uiState.value.copy(
                openTabs = updatedTabs,
                activeTabIndex = updatedTabs.lastIndex
            )
            refreshDiagnosticsAndSuggestions()
            observeActiveFileHistory()
        }
    }

    fun selectTab(index: Int) {
        if (index in _uiState.value.openTabs.indices) {
            _uiState.value = _uiState.value.copy(activeTabIndex = index)
            refreshDiagnosticsAndSuggestions()
            observeActiveFileHistory()
        }
    }

    fun closeTab(index: Int) {
        val tabs = _uiState.value.openTabs.toMutableList()
        if (index !in tabs.indices) return
        tabs.removeAt(index)
        val nextIndex = when {
            tabs.isEmpty() -> -1
            index <= _uiState.value.activeTabIndex -> (_uiState.value.activeTabIndex - 1).coerceAtLeast(0)
            else -> _uiState.value.activeTabIndex
        }
        _uiState.value = _uiState.value.copy(openTabs = tabs, activeTabIndex = nextIndex)
        refreshDiagnosticsAndSuggestions()
        observeActiveFileHistory()
    }

    fun updateActiveContent(newContent: String, cursorWord: String = "") {
        val idx = _uiState.value.activeTabIndex
        val tabs = _uiState.value.openTabs.toMutableList()
        val tab = tabs.getOrNull(idx) ?: return
        if (tab.content == newContent) return

        tabs[idx] = tab.copy(content = newContent, isDirty = true)
        val ext = tab.name.substringAfterLast('.', "")
        val suggestions = CodeAnalyzer.autocompleteSuggestions(ext, cursorWord, newContent)
        val diagnostics = CodeAnalyzer.analyzeFile(tab.relativePath, newContent)

        _uiState.value = _uiState.value.copy(
            openTabs = tabs,
            suggestions = suggestions,
            diagnostics = diagnostics
        )

        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            val prefs = secureKeyStore.editorPreferences.first()
            if (prefs.autoSave) {
                delay(1200)
                saveActiveFile(summary = "Salvamento automático", recordHistory = false)
            }
        }
    }

    fun saveActiveFile(summary: String = "Edição manual", recordHistory: Boolean = true) {
        val project = _uiState.value.project ?: return
        val idx = _uiState.value.activeTabIndex
        val tab = _uiState.value.openTabs.getOrNull(idx) ?: return

        viewModelScope.launch {
            val root = File(project.rootPath)
            withContext(Dispatchers.IO) {
                fileManager.writeFile(root, tab.relativePath, tab.content)
            }
            if (recordHistory) {
                fileHistoryDao.insert(
                    FileHistoryEntity(
                        id = UUID.randomUUID().toString(),
                        projectId = project.id,
                        relativePath = tab.relativePath,
                        content = tab.content,
                        summary = summary,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
            projectDao.upsert(project.copy(updatedAt = System.currentTimeMillis()))
            val tabs = _uiState.value.openTabs.toMutableList()
            if (idx in tabs.indices) {
                tabs[idx] = tabs[idx].copy(isDirty = false)
            }
            _uiState.value = _uiState.value.copy(
                openTabs = tabs,
                feedbackMessage = if (recordHistory) "Arquivo ${tab.name} salvo." else null
            )
        }
    }

    fun replaceAllInActiveFile(query: String, replacement: String): Int {
        val tab = _uiState.value.activeTab ?: return 0
        if (query.isBlank()) return 0
        var count = 0
        val updated = Regex(Regex.escape(query)).replace(tab.content) {
            count++
            replacement
        }
        if (count > 0) {
            updateActiveContent(updated)
            saveActiveFile(summary = "Substituição ($count ocorrências)")
        }
        return count
    }

    fun restoreHistoryEntry(entry: FileHistoryEntity) {
        updateActiveContent(entry.content)
        saveActiveFile(summary = "Restaurado do histórico", recordHistory = true)
    }

    fun createNewFile(name: String, initialContent: String = "") {
        val project = _uiState.value.project ?: return
        val clean = name.trim().trimStart('/')
        if (clean.isBlank()) return
        val rel = if (_uiState.value.currentBrowserDir.isBlank()) clean else "${_uiState.value.currentBrowserDir}/$clean"
        viewModelScope.launch {
            val root = File(project.rootPath)
            val ok = withContext(Dispatchers.IO) { fileManager.createFile(root, rel, initialContent) }
            if (ok) {
                navigateDirectory(_uiState.value.currentBrowserDir)
                openFile(rel)
                _uiState.value = _uiState.value.copy(feedbackMessage = "Arquivo '$clean' criado.")
            } else {
                _uiState.value = _uiState.value.copy(feedbackMessage = "Arquivo já existe.")
            }
        }
    }

    fun createNewFolder(name: String) {
        val project = _uiState.value.project ?: return
        val clean = name.trim().trim('/')
        if (clean.isBlank()) return
        val rel = if (_uiState.value.currentBrowserDir.isBlank()) clean else "${_uiState.value.currentBrowserDir}/$clean"
        viewModelScope.launch {
            val root = File(project.rootPath)
            val ok = withContext(Dispatchers.IO) { fileManager.createFolder(root, rel) }
            if (ok) {
                navigateDirectory(_uiState.value.currentBrowserDir)
                _uiState.value = _uiState.value.copy(feedbackMessage = "Pasta '$clean' criada.")
            }
        }
    }

    fun renameNode(node: ProjectFileNode, newName: String) {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            val root = File(project.rootPath)
            val ok = withContext(Dispatchers.IO) { fileManager.renameEntry(root, node.relativePath, newName) }
            if (ok) {
                navigateDirectory(_uiState.value.currentBrowserDir)
                _uiState.value = _uiState.value.copy(feedbackMessage = "Renomeado para '$newName'.")
            }
        }
    }

    fun deleteNode(node: ProjectFileNode) {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            val root = File(project.rootPath)
            val ok = withContext(Dispatchers.IO) { fileManager.deleteEntry(root, node.relativePath) }
            if (ok) {
                val tabIdx = _uiState.value.openTabs.indexOfFirst { it.relativePath == node.relativePath }
                if (tabIdx >= 0) closeTab(tabIdx)
                navigateDirectory(_uiState.value.currentBrowserDir)
                _uiState.value = _uiState.value.copy(feedbackMessage = "'${node.name}' removido.")
            }
        }
    }

    fun importFile(uri: Uri) {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            val root = File(project.rootPath)
            val res = withContext(Dispatchers.IO) {
                fileManager.importFileFromUri(
                    root,
                    _uiState.value.currentBrowserDir,
                    uri,
                    "import_${System.currentTimeMillis()}.txt"
                )
            }
            res.onSuccess { rel ->
                navigateDirectory(_uiState.value.currentBrowserDir)
                openFile(rel)
                _uiState.value = _uiState.value.copy(feedbackMessage = "Arquivo importado: $rel")
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(feedbackMessage = e.localizedMessage)
            }
        }
    }

    fun executeTerminalCommand(command: String, onTriggerBuild: (String) -> Unit) {
        val project = _uiState.value.project ?: return
        val root = File(project.rootPath)
        val promptPrefix = "${project.name}:/${_uiState.value.terminalRelDir}$ $command"

        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                IntegratedTerminalEngine.execute(root, _uiState.value.terminalRelDir, command)
            }
            val nextLines = if (res.clearScreen) {
                listOf("Terminal limpo.")
            } else {
                val list = _uiState.value.terminalLines.toMutableList()
                list.add(promptPrefix)
                if (res.output.isNotBlank()) list.add(res.output)
                list.takeLast(150)
            }
            _uiState.value = _uiState.value.copy(
                terminalLines = nextLines,
                terminalRelDir = res.newCurrentDir
            )
            navigateDirectory(_uiState.value.currentBrowserDir)
            res.triggerBuildType?.let { onTriggerBuild(it) }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }

    private fun refreshDiagnosticsAndSuggestions() {
        val tab = _uiState.value.activeTab
        if (tab == null) {
            _uiState.value = _uiState.value.copy(diagnostics = emptyList(), suggestions = emptyList())
            return
        }
        val ext = tab.name.substringAfterLast('.', "")
        _uiState.value = _uiState.value.copy(
            diagnostics = CodeAnalyzer.analyzeFile(tab.relativePath, tab.content),
            suggestions = CodeAnalyzer.autocompleteSuggestions(ext, "", tab.content)
        )
    }

    private fun observeActiveFileHistory() {
        historyCollectorJob?.cancel()
        val project = _uiState.value.project ?: return
        val tab = _uiState.value.activeTab ?: run {
            _uiState.value = _uiState.value.copy(historyEntries = emptyList())
            return
        }
        historyCollectorJob = viewModelScope.launch {
            fileHistoryDao.observeForFile(project.id, tab.relativePath).collect { entries ->
                _uiState.value = _uiState.value.copy(historyEntries = entries)
            }
        }
    }
}
