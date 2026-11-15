package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ProjectEntity
import com.example.data.local.TechStack
import com.example.data.project.ProjectFileNode
import com.example.ui.components.BadgeTone
import com.example.ui.components.CodeXFeedbackBanner
import com.example.ui.components.CodeXPrimaryButton
import com.example.ui.components.CodeXStatusBadge
import com.example.ui.components.CodeXTopBar
import com.example.ui.components.ProjectIconBadge
import com.example.ui.components.formatBytes
import com.example.ui.components.formatDateTime
import com.example.ui.theme.CodeXCrimson
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.EditorViewModel
import com.example.ui.viewmodel.WorkspaceViewModel

@Composable
fun CreateProjectScreen(
    authViewModel: AuthViewModel,
    workspaceViewModel: WorkspaceViewModel,
    onBack: () -> Unit,
    onProjectCreated: (String) -> Unit
) {
    val user by authViewModel.currentUser.collectAsStateWithLifecycle()
    val banner by workspaceViewModel.statusBanner.collectAsStateWithLifecycle()

    var appName by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("com.codex.meuapp") }
    var versionName by remember { mutableStateOf("1.0.0") }
    var versionCode by remember { mutableStateOf("1") }
    var selectedStack by remember { mutableStateOf(TechStack.KOTLIN_COMPOSE) }
    var selectedIcon by remember { mutableStateOf("CODE") }

    val iconPresets = listOf("CODE", "ANDROID", "ROCKET", "BOLT", "TERMINAL", "WEB")

    Scaffold(
        topBar = {
            CodeXTopBar(
                title = "Criar Novo Projeto",
                subtitle = "Configuração da estrutura e tecnologia",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 560.dp)) {
                banner?.let { (ok, msg) ->
                    CodeXFeedbackBanner(msg, isError = !ok, onDismiss = { workspaceViewModel.clearBanner() })
                    Spacer(Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = appName,
                    onValueChange = {
                        appName = it
                        val slug = it.lowercase().replace(Regex("[^a-z0-9]"), "")
                        if (slug.isNotBlank()) packageName = "com.codex.$slug"
                    },
                    label = { Text("Nome do Aplicativo") },
                    placeholder = { Text("Ex: LojaX") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_project_name_input")
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Pacote Android (applicationId)") },
                    placeholder = { Text("com.empresa.meuapp") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_project_package_input")
                )
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = versionName,
                        onValueChange = { versionName = it },
                        label = { Text("Versão (versionName)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_project_version_name_input")
                    )
                    OutlinedTextField(
                        value = versionCode,
                        onValueChange = { versionCode = it.filter { c -> c.isDigit() } },
                        label = { Text("Build (Code)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_project_version_code_input")
                    )
                }

                Spacer(Modifier.height(18.dp))
                Text("Ícone do Projeto", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    iconPresets.forEach { preset ->
                        val isSelected = selectedIcon == preset
                        Surface(
                            onClick = { selectedIcon = preset },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) CodeXCrimson else MaterialTheme.colorScheme.outline
                            ),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Box(modifier = Modifier.padding(6.dp)) {
                                ProjectIconBadge(preset = preset)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("Tecnologia / Linguagem", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TechStack.entries.forEach { stack ->
                        val selected = selectedStack == stack
                        Card(
                            onClick = { selectedStack = stack },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) CodeXCrimson else MaterialTheme.colorScheme.outline
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("stack_option_${stack.name}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(stack.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = when (stack) {
                                        TechStack.KOTLIN_COMPOSE -> "Gera estrutura completa Gradle + Jetpack Compose + Material 3 + AndroidManifest"
                                        TechStack.JAVA_ANDROID -> "Gera estrutura Gradle + AppCompat Java + Layout XML + AndroidManifest"
                                        TechStack.WEB_HTML_JS -> "Gera index.html, CSS3, JavaScript, manifest.json com preview ao vivo"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                CodeXPrimaryButton(
                    text = "Criar Estrutura do Projeto",
                    icon = Icons.Default.Add,
                    onClick = {
                        val ownerId = user?.id ?: return@CodeXPrimaryButton
                        workspaceViewModel.createProject(
                            ownerUserId = ownerId,
                            name = appName,
                            packageName = packageName,
                            versionName = versionName,
                            versionCode = versionCode.toIntOrNull() ?: 1,
                            techStack = selectedStack,
                            iconPreset = selectedIcon,
                            onCreated = { created -> onProjectCreated(created.id) }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "create_project_submit_button"
                )
            }
        }
    }
}

@Composable
fun ProjectsScreen(
    workspaceViewModel: WorkspaceViewModel,
    onBack: () -> Unit,
    onCreateNew: () -> Unit,
    onOpenEditor: (String) -> Unit,
    onOpenFileManager: (String) -> Unit,
    onOpenCompiler: (String) -> Unit,
    onOpenAi: (String) -> Unit
) {
    val projects by workspaceViewModel.projects.collectAsStateWithLifecycle()
    val banner by workspaceViewModel.statusBanner.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var stackFilter by remember { mutableStateOf<TechStack?>(null) }
    var renamingProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deletingProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var exportingProject by remember { mutableStateOf<ProjectEntity?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val target = exportingProject
        if (uri != null && target != null) {
            workspaceViewModel.exportProjectZip(target, uri)
        }
        exportingProject = null
    }

    val filtered = projects.filter { proj ->
        val matchesText = searchQuery.isBlank() ||
            proj.name.contains(searchQuery, ignoreCase = true) ||
            proj.packageName.contains(searchQuery, ignoreCase = true)
        val matchesStack = stackFilter == null || proj.techStack == stackFilter?.name
        matchesText && matchesStack
    }

    Scaffold(
        topBar = {
            CodeXTopBar(
                title = "Meus Projetos",
                subtitle = "${projects.size} projeto(s) salvos no dispositivo",
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNew,
                containerColor = CodeXCrimson,
                contentColor = Color.White,
                modifier = Modifier.testTag("projects_fab_new")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo Projeto")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            banner?.let { (ok, msg) ->
                CodeXFeedbackBanner(msg, isError = !ok, onDismiss = { workspaceViewModel.clearBanner() })
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nome ou pacote...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("projects_search_input")
            )

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = stackFilter == null,
                    onClick = { stackFilter = null },
                    label = { Text("Todos") }
                )
                TechStack.entries.forEach { st ->
                    FilterChip(
                        selected = stackFilter == st,
                        onClick = { stackFilter = if (stackFilter == st) null else st },
                        label = { Text(st.shortLabel) }
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { project ->
                    var menuExpanded by remember { mutableStateOf(false) }
                    val stack = runCatching { TechStack.valueOf(project.techStack) }.getOrDefault(TechStack.KOTLIN_COMPOSE)
                    Card(
                        onClick = { onOpenEditor(project.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ProjectIconBadge(preset = project.iconPreset)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        project.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${project.packageName} • v${project.versionName} (${project.versionCode})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Ações do projeto")
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Abrir no Editor") },
                                            leadingIcon = { Icon(Icons.Default.Code, null) },
                                            onClick = { menuExpanded = false; onOpenEditor(project.id) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Gerenciador de Arquivos") },
                                            leadingIcon = { Icon(Icons.Default.Folder, null) },
                                            onClick = { menuExpanded = false; onOpenFileManager(project.id) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Renomear") },
                                            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) },
                                            onClick = {
                                                menuExpanded = false
                                                renameText = project.name
                                                renamingProject = project
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Duplicar") },
                                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                                            onClick = {
                                                menuExpanded = false
                                                workspaceViewModel.duplicateProject(project)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Exportar ZIP") },
                                            leadingIcon = { Icon(Icons.Default.FileDownload, null) },
                                            onClick = {
                                                menuExpanded = false
                                                exportingProject = project
                                                exportLauncher.launch("${project.name.replace(" ", "_")}.zip")
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = {
                                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                deletingProject = project
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CodeXStatusBadge(stack.shortLabel, BadgeTone.CRIMSON)
                                    CodeXStatusBadge(
                                        text = "Build: ${project.lastBuildStatus}",
                                        tone = when (project.lastBuildStatus) {
                                            "SUCCESS" -> BadgeTone.SUCCESS
                                            "FAILED" -> BadgeTone.ERROR
                                            else -> BadgeTone.NEUTRAL
                                        }
                                    )
                                }
                                Row {
                                    IconButton(onClick = { onOpenAi(project.id) }) {
                                        Icon(Icons.Default.SmartToy, contentDescription = "Code X AI", tint = CodeXCrimson)
                                    }
                                    IconButton(onClick = { onOpenCompiler(project.id) }) {
                                        Icon(Icons.Default.Build, contentDescription = "Compilar APK")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    renamingProject?.let { target ->
        AlertDialog(
            onDismissRequest = { renamingProject = null },
            title = { Text("Renomear Projeto") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Novo nome") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    workspaceViewModel.renameProject(target, renameText)
                    renamingProject = null
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { renamingProject = null }) { Text("Cancelar") }
            }
        )
    }

    deletingProject?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingProject = null },
            title = { Text("Excluir Projeto") },
            text = { Text("Deseja excluir '${target.name}' e todos os seus arquivos? Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    workspaceViewModel.deleteProject(target)
                    deletingProject = null
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingProject = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun FileManagerScreen(
    projectId: String,
    editorViewModel: EditorViewModel,
    onBack: () -> Unit,
    onOpenInEditor: (String) -> Unit
) {
    val state by editorViewModel.uiState.collectAsStateWithLifecycle()

    var showCreateFile by remember { mutableStateOf(false) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var newEntryName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<ProjectFileNode?>(null) }

    LaunchedEffect(projectId) {
        editorViewModel.loadProject(projectId)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            editorViewModel.importFile(uri)
        }
    }

    Scaffold(
        topBar = {
            CodeXTopBar(
                title = "Gerenciador de Arquivos",
                subtitle = "${state.project?.name ?: ""} /${state.currentBrowserDir}",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Importar arquivo")
                    }
                    IconButton(onClick = { newEntryName = ""; showCreateFolder = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Nova pasta")
                    }
                    IconButton(onClick = { newEntryName = ""; showCreateFile = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Novo arquivo")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            state.feedbackMessage?.let { msg ->
                CodeXFeedbackBanner(msg, isError = false, onDismiss = { editorViewModel.clearFeedback() })
            }

            if (state.currentBrowserDir.isNotBlank()) {
                Surface(
                    onClick = {
                        val parent = state.currentBrowserDir.substringBeforeLast('/', "")
                        editorViewModel.navigateDirectory(parent)
                    },
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = CodeXCrimson)
                        Spacer(Modifier.width(12.dp))
                        Text(".. (voltar pasta)", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.browserNodes, key = { it.relativePath }) { node ->
                    Card(
                        onClick = {
                            if (node.isDirectory) {
                                editorViewModel.navigateDirectory(node.relativePath)
                            } else {
                                editorViewModel.openFile(node.relativePath)
                                onOpenInEditor(projectId)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (node.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                                contentDescription = null,
                                tint = if (node.isDirectory) CodeXCrimson else MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(node.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (node.isDirectory) "Pasta" else "${formatBytes(node.sizeBytes)} • ${formatDateTime(node.lastModified)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                newEntryName = node.name
                                renameTarget = node
                            }) {
                                Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Renomear")
                            }
                            IconButton(onClick = { editorViewModel.deleteNode(node) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateFile) {
        AlertDialog(
            onDismissRequest = { showCreateFile = false },
            title = { Text("Novo Arquivo") },
            text = {
                OutlinedTextField(
                    value = newEntryName,
                    onValueChange = { newEntryName = it },
                    label = { Text("Nome do arquivo (ex: TelaLogin.kt)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editorViewModel.createNewFile(newEntryName)
                    showCreateFile = false
                }) { Text("Criar") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFile = false }) { Text("Cancelar") }
            }
        )
    }

    if (showCreateFolder) {
        AlertDialog(
            onDismissRequest = { showCreateFolder = false },
            title = { Text("Nova Pasta") },
            text = {
                OutlinedTextField(
                    value = newEntryName,
                    onValueChange = { newEntryName = it },
                    label = { Text("Nome da pasta") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editorViewModel.createNewFolder(newEntryName)
                    showCreateFolder = false
                }) { Text("Criar") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolder = false }) { Text("Cancelar") }
            }
        )
    }

    renameTarget?.let { node ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Renomear") },
            text = {
                OutlinedTextField(
                    value = newEntryName,
                    onValueChange = { newEntryName = it },
                    label = { Text("Novo nome") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editorViewModel.renameNode(node, newEntryName)
                    renameTarget = null
                }) { Text("Renomear") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancelar") }
            }
        )
    }
}
