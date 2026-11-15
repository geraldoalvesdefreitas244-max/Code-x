package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.TechStack
import com.example.data.project.DiagnosticSeverity
import com.example.ui.components.CodeSyntaxVisualTransformation
import com.example.ui.components.CodeXFeedbackBanner
import com.example.ui.components.CodeXTopBar
import com.example.ui.components.formatDateTime
import com.example.ui.theme.CodeXBlack
import com.example.ui.theme.CodeXCrimson
import com.example.ui.theme.CodeXError
import com.example.ui.theme.CodeXInfo
import com.example.ui.theme.CodeXSuccess
import com.example.ui.theme.CodeXSurface
import com.example.ui.theme.CodeXSurfaceElevated
import com.example.ui.theme.CodeXWarning
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.EditorViewModel
import com.example.ui.viewmodel.WorkspaceViewModel
import java.io.File

@Composable
fun CodeEditorScreen(
    projectId: String,
    editorViewModel: EditorViewModel,
    workspaceViewModel: WorkspaceViewModel,
    onBack: () -> Unit,
    onOpenFileManager: (String) -> Unit,
    onOpenAi: (String) -> Unit,
    onOpenCompiler: (String) -> Unit
) {
    val state by editorViewModel.uiState.collectAsStateWithLifecycle()
    val prefs by workspaceViewModel.editorPreferences.collectAsStateWithLifecycle()

    var showFindReplace by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var showBottomPanel by remember { mutableStateOf(false) }
    var bottomTab by remember { mutableIntStateOf(0) } // 0 = Console de Erros, 1 = Terminal
    var terminalInput by remember { mutableStateOf("") }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showWebPreview by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        editorViewModel.loadProject(projectId)
    }

    val activeTab = state.activeTab
    val activeExt = activeTab?.name?.substringAfterLast('.', "") ?: ""
    val isWebProject = state.project?.techStack == TechStack.WEB_HTML_JS.name

    var textFieldValue by remember(activeTab?.relativePath) {
        mutableStateOf(TextFieldValue(activeTab?.content.orEmpty()))
    }

    // Keep synced when external action (replace / restore history) updates content
    LaunchedEffect(activeTab?.content) {
        val currentContent = activeTab?.content.orEmpty()
        if (currentContent != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(text = currentContent)
        }
    }

    Scaffold(
        topBar = {
            CodeXTopBar(
                title = state.project?.name ?: "Editor",
                subtitle = activeTab?.relativePath ?: "Nenhum arquivo aberto",
                onBack = onBack,
                actions = {
                    if (isWebProject) {
                        IconButton(
                            onClick = {
                                editorViewModel.saveActiveFile("Preview Web")
                                showWebPreview = true
                            },
                            modifier = Modifier.testTag("editor_run_web_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Executar Web Preview", tint = CodeXSuccess)
                        }
                    }
                    IconButton(
                        onClick = { showFindReplace = !showFindReplace },
                        modifier = Modifier.testTag("editor_find_button")
                    ) {
                        Icon(Icons.Default.FindReplace, contentDescription = "Pesquisar e substituir")
                    }
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.testTag("editor_history_button")
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Histórico de alterações")
                    }
                    IconButton(
                        onClick = { editorViewModel.saveActiveFile() },
                        modifier = Modifier.testTag("editor_save_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Salvar arquivo", tint = CodeXCrimson)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            state.feedbackMessage?.let { msg ->
                CodeXFeedbackBanner(msg, isError = false, onDismiss = { editorViewModel.clearFeedback() })
            }

            // Secondary quick toolbar (Files, AI, Compiler, Terminal)
            Surface(color = CodeXSurfaceElevated, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { onOpenFileManager(projectId) },
                        label = { Text("Arquivos") },
                        leadingIcon = { Icon(Icons.Default.Folder, null, modifier = Modifier.size(16.dp)) }
                    )
                    AssistChip(
                        onClick = { onOpenAi(projectId) },
                        label = { Text("Code X AI") },
                        leadingIcon = { Icon(Icons.Default.SmartToy, null, tint = CodeXCrimson, modifier = Modifier.size(16.dp)) }
                    )
                    AssistChip(
                        onClick = { onOpenCompiler(projectId) },
                        label = { Text("Compilar APK") },
                        leadingIcon = { Icon(Icons.Default.Build, null, modifier = Modifier.size(16.dp)) }
                    )
                    AssistChip(
                        onClick = {
                            if (showBottomPanel && bottomTab == 0) showBottomPanel = false
                            else {
                                bottomTab = 0; showBottomPanel = true
                            }
                        },
                        label = { Text("Erros (${state.diagnostics.size})") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.ErrorOutline,
                                null,
                                tint = if (state.diagnostics.any { it.severity == DiagnosticSeverity.ERROR }) CodeXError else CodeXSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    AssistChip(
                        onClick = {
                            if (showBottomPanel && bottomTab == 1) showBottomPanel = false
                            else {
                                bottomTab = 1; showBottomPanel = true
                            }
                        },
                        label = { Text("Terminal") },
                        leadingIcon = { Icon(Icons.Default.Terminal, null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            // Open File Tabs
            if (state.openTabs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CodeXSurface)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.openTabs.forEachIndexed { idx, tab ->
                        val selected = idx == state.activeTabIndex
                        Surface(
                            onClick = { editorViewModel.selectTab(idx) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (selected) CodeXCrimson else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (tab.isDirty) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(CodeXCrimson)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    text = tab.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = { editorViewModel.closeTab(idx) },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Fechar aba", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Find & Replace Bar
            if (showFindReplace && activeTab != null) {
                Surface(color = CodeXSurfaceElevated, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = findQuery,
                                onValueChange = { findQuery = it },
                                label = { Text("Localizar") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = replaceQuery,
                                onValueChange = { replaceQuery = it },
                                label = { Text("Substituir por") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val matchCount = remember(findQuery, textFieldValue.text) {
                                if (findQuery.isBlank()) 0
                                else Regex(Regex.escape(findQuery)).findAll(textFieldValue.text).count()
                            }
                            Text(
                                text = "$matchCount ocorrência(s)",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = {
                                editorViewModel.replaceAllInActiveFile(findQuery, replaceQuery)
                            }) {
                                Text("Substituir tudo")
                            }
                        }
                    }
                }
            }

            // Code Canvas Area
            if (activeTab == null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nenhum arquivo aberto", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { onOpenFileManager(projectId) }) {
                            Text("Abrir Gerenciador de Arquivos", color = CodeXCrimson)
                        }
                    }
                }
            } else {
                val verticalScroll = rememberScrollState()
                val lineCount = remember(textFieldValue.text) {
                    textFieldValue.text.count { it == '\n' } + 1
                }
                val visualTransformation = remember(activeExt) {
                    CodeSyntaxVisualTransformation(activeExt)
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(CodeXBlack)
                        .verticalScroll(verticalScroll)
                ) {
                    if (prefs.showLineNumbers) {
                        Column(
                            modifier = Modifier
                                .background(CodeXSurface)
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            for (line in 1..lineCount) {
                                Text(
                                    text = line.toString(),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = prefs.fontSizeSp.sp,
                                    lineHeight = (prefs.fontSizeSp + 6).sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { updated ->
                            textFieldValue = updated
                            val cursor = updated.selection.start.coerceIn(0, updated.text.length)
                            val beforeCursor = updated.text.substring(0, cursor)
                            val currentWord = beforeCursor.takeLastWhile { it.isLetterOrDigit() || it == '_' || it == '@' }
                            editorViewModel.updateActiveContent(updated.text, currentWord)
                        },
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = prefs.fontSizeSp.sp,
                            lineHeight = (prefs.fontSizeSp + 6).sp,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        cursorBrush = SolidColor(CodeXCrimson),
                        visualTransformation = visualTransformation,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(12.dp)
                            .testTag("code_editor_textfield")
                    )
                }

                // Autocomplete & Symbol Insert Bar
                Surface(color = CodeXSurfaceElevated, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        if (state.suggestions.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                state.suggestions.forEach { token ->
                                    Surface(
                                        onClick = {
                                            val cur = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
                                            val before = textFieldValue.text.substring(0, cur)
                                            val prefixLen = before.takeLastWhile { it.isLetterOrDigit() || it == '_' || it == '@' }.length
                                            val replaced = textFieldValue.text.replaceRange(cur - prefixLen, cur, token)
                                            val newPos = cur - prefixLen + token.length
                                            textFieldValue = TextFieldValue(replaced, TextRange(newPos))
                                            editorViewModel.updateActiveContent(replaced)
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Text(
                                            text = token,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 11.sp,
                                            color = CodeXInfo,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        val symbols = listOf("{", "}", "(", ")", "[", "]", "=", "\"", ";", ".", "<", ">", "/", "val ", "fun ")
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            symbols.forEach { sym ->
                                Surface(
                                    onClick = {
                                        val cur = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
                                        val inserted = StringBuilder(textFieldValue.text).insert(cur, sym).toString()
                                        textFieldValue = TextFieldValue(inserted, TextRange(cur + sym.length))
                                        editorViewModel.updateActiveContent(inserted)
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = CodeXSurface
                                ) {
                                    Text(
                                        text = sym.trim().ifEmpty { sym },
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Drawer: Error Console / Integrated Terminal
            if (showBottomPanel) {
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .background(CodeXSurface)
                ) {
                    TabRow(selectedTabIndex = bottomTab, containerColor = CodeXSurfaceElevated) {
                        Tab(
                            selected = bottomTab == 0,
                            onClick = { bottomTab = 0 },
                            text = { Text("Console de Erros (${state.diagnostics.size})") }
                        )
                        Tab(
                            selected = bottomTab == 1,
                            onClick = { bottomTab = 1 },
                            text = { Text("Terminal Integrado") }
                        )
                    }

                    if (bottomTab == 0) {
                        if (state.diagnostics.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("✓ Nenhum erro detectado no arquivo atual.", color = CodeXSuccess)
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(state.diagnostics) { diag ->
                                    val color = when (diag.severity) {
                                        DiagnosticSeverity.ERROR -> CodeXError
                                        DiagnosticSeverity.WARNING -> CodeXWarning
                                        DiagnosticSeverity.INFO -> CodeXInfo
                                    }
                                    Text(
                                        text = "[${diag.severity}] Linha ${diag.line}: ${diag.message}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        color = color
                                    )
                                }
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(state.terminalLines) { line ->
                                    Text(
                                        text = line,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = terminalInput,
                                    onValueChange = { terminalInput = it },
                                    placeholder = { Text("ls, tree, validate, build debug...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        val cmd = terminalInput
                                        terminalInput = ""
                                        editorViewModel.executeTerminalCommand(cmd) {
                                            onOpenCompiler(projectId)
                                        }
                                    },
                                    modifier = Modifier.testTag("terminal_send_button")
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Executar comando", tint = CodeXCrimson)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Histórico de Alterações") },
            text = {
                if (state.historyEntries.isEmpty()) {
                    Text("Nenhuma versão salva ainda para este arquivo.")
                } else {
                    LazyColumn(
                        modifier = Modifier.height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.historyEntries, key = { it.id }) { entry ->
                            Card(
                                onClick = {
                                    editorViewModel.restoreHistoryEntry(entry)
                                    showHistoryDialog = false
                                },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(entry.summary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${formatDateTime(entry.createdAt)} • ${entry.content.length} caracteres (toque para restaurar)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) { Text("Fechar") }
            }
        )
    }

    if (showWebPreview && state.project != null) {
        WebAppPreviewDialog(
            projectRoot = File(state.project!!.rootPath),
            projectName = state.project!!.name,
            onDismiss = { showWebPreview = false }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebAppPreviewDialog(
    projectRoot: File,
    projectName: String,
    onDismiss: () -> Unit
) {
    val consoleLogs = remember { mutableStateListOf<String>() }
    val indexUrl = remember(projectRoot) {
        "file://${File(projectRoot, "index.html").absolutePath}"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                CodeXTopBar(
                    title = "Preview: $projectName",
                    subtitle = "Execução WebView + Console JS",
                    onBack = onDismiss
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.allowFileAccess = true
                            settings.domStorageEnabled = true
                            webChromeClient = object : WebChromeClient() {
                                override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                                    consoleLogs.add("[${msg.messageLevel()}] ${msg.message()}")
                                    return true
                                }
                            }
                            loadUrl(indexUrl)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
                Surface(
                    color = CodeXSurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    LazyColumn(contentPadding = PaddingValues(8.dp)) {
                        item {
                            Text("Console JavaScript:", style = MaterialTheme.typography.labelSmall, color = CodeXCrimson)
                        }
                        items(consoleLogs) { log ->
                            Text(log, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
