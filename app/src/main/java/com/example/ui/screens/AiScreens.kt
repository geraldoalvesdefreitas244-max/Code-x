package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ai.AiFilePatchParser
import com.example.data.local.AiChatMessageEntity
import com.example.ui.components.BadgeTone
import com.example.ui.components.CodeXFeedbackBanner
import com.example.ui.components.CodeXOutlinedButton
import com.example.ui.components.CodeXPrimaryButton
import com.example.ui.components.CodeXStatusBadge
import com.example.ui.components.CodeXTopBar
import com.example.ui.theme.CodeXCrimson
import com.example.ui.theme.CodeXCrimsonContainer
import com.example.ui.theme.CodeXSurfaceElevated
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.WorkspaceViewModel

@Composable
fun CodeXAiScreen(
    initialProjectId: String?,
    aiViewModel: AiViewModel,
    workspaceViewModel: WorkspaceViewModel,
    onBack: () -> Unit,
    onOpenProviders: () -> Unit
) {
    val uiState by aiViewModel.uiState.collectAsStateWithLifecycle()
    val projects by workspaceViewModel.projects.collectAsStateWithLifecycle()

    var prompt by remember { mutableStateOf("") }
    var includeFiles by remember { mutableStateOf(true) }

    LaunchedEffect(initialProjectId) {
        aiViewModel.selectProject(initialProjectId)
    }

    val quickPrompts = listOf(
        "Crie um aplicativo de rede social com login, cadastro, perfil, publicação de fotos e sistema de curtidas.",
        "Analise os arquivos do projeto e corrija possíveis erros.",
        "Adicione uma tela de configurações com Material Design 3.",
        "Crie uma camada de API REST e repositório assíncrono."
    )

    Scaffold(
        topBar = {
            CodeXTopBar(
                title = "Code X AI",
                subtitle = uiState.activeProvider?.let { "${it.name} • ${it.selectedModel}" }
                    ?: "Nenhum provedor ativo",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onOpenProviders, modifier = Modifier.testTag("ai_open_providers")) {
                        Icon(Icons.Default.Hub, contentDescription = "AI Providers")
                    }
                    IconButton(onClick = { aiViewModel.clearHistory() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Limpar conversa")
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
            // Project Selector & Context Header
            Surface(color = CodeXSurfaceElevated, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_codex_ai_avatar),
                            contentDescription = "Avatar Code X AI",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .border(1.dp, CodeXCrimson, CircleShape)
                        )
                        FilterChip(
                            selected = uiState.selectedProject == null,
                            onClick = { aiViewModel.selectProject(null) },
                            label = { Text("Geral") }
                        )
                        projects.forEach { proj ->
                            FilterChip(
                                selected = uiState.selectedProject?.id == proj.id,
                                onClick = { aiViewModel.selectProject(proj.id) },
                                label = { Text(proj.name) }
                            )
                        }
                    }
                    if (uiState.selectedProject != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = includeFiles,
                                onCheckedChange = { includeFiles = it }
                            )
                            Text(
                                text = "Analisar arquivos de '${uiState.selectedProject!!.name}' automaticamente",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            uiState.errorMessage?.let { err ->
                CodeXFeedbackBanner(err, isError = true, onDismiss = { aiViewModel.clearMessages() })
            }
            uiState.appliedNotice?.let { ok ->
                CodeXFeedbackBanner(ok, isError = false, onDismiss = { aiViewModel.clearMessages() })
            }

            // Messages List
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (uiState.messages.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_codex_ai_avatar),
                                        contentDescription = "Code X AI",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, CodeXCrimson, CircleShape)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("Assistente Code X AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Gera telas, corrige erros, cria APIs e grava arquivos direto no projeto.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Text("Sugestões rápidas:", style = MaterialTheme.typography.labelMedium, color = CodeXCrimson)
                                Spacer(Modifier.height(6.dp))
                                quickPrompts.forEach { sample ->
                                    AssistChip(
                                        onClick = { prompt = sample },
                                        label = { Text(sample, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(uiState.messages, key = { it.id }) { msg ->
                        AiChatBubble(
                            message = msg,
                            onApplyFiles = { files -> aiViewModel.applyGeneratedFilesToProject(files) }
                        )
                    }
                }

                if (uiState.isGenerating) {
                    item {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CodeXCrimson, strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Code X AI está gerando resposta...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Input Box
            Surface(color = CodeXSurfaceElevated, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        placeholder = { Text("Descreva o app, tela ou erro para corrigir...") },
                        maxLines = 4,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_prompt_input")
                    )
                    IconButton(
                        onClick = {
                            val toSend = prompt
                            prompt = ""
                            aiViewModel.sendPrompt(toSend, includeFiles)
                        },
                        enabled = !uiState.isGenerating && prompt.isNotBlank(),
                        modifier = Modifier.testTag("ai_send_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Enviar comando", tint = CodeXCrimson)
                    }
                }
            }
        }
    }
}

@Composable
private fun AiChatBubble(
    message: AiChatMessageEntity,
    onApplyFiles: (List<com.example.data.ai.AiGeneratedFile>) -> Unit
) {
    val isUser = message.role == "USER"
    val extracted = remember(message.generatedFilesJson) {
        AiFilePatchParser.fromJson(message.generatedFilesJson)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUser) CodeXCrimsonContainer.copy(alpha = 0.65f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isUser) CodeXCrimson.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isUser) "Você" else "Code X AI (${message.providerName} • ${message.modelName})",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isUser) Color.White else CodeXCrimson,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium
            )
            if (extracted.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "${extracted.size} arquivo(s) detectado(s):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        extracted.forEach { file ->
                            Text(
                                text = "• ${file.relativePath} (${file.content.lines().size} linhas)",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        CodeXPrimaryButton(
                            text = "Aplicar Arquivos no Projeto",
                            icon = Icons.Default.AutoAwesome,
                            onClick = { onApplyFiles(extracted) },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "ai_apply_files_button"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiProvidersScreen(
    workspaceViewModel: WorkspaceViewModel,
    onBack: () -> Unit,
    onConfigureProvider: (String) -> Unit
) {
    val providers by workspaceViewModel.providerItems.collectAsStateWithLifecycle()
    val banner by workspaceViewModel.statusBanner.collectAsStateWithLifecycle()
    val testingId by workspaceViewModel.testingProviderId.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf("https://") }
    var customModel by remember { mutableStateOf("") }
    var customKey by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CodeXTopBar(
                title = "Central AI Providers",
                subtitle = "Chaves protegidas via Android Keystore AES-256",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_custom_provider_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar provedor")
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
            banner?.let { (ok, msg) ->
                CodeXFeedbackBanner(msg, isError = !ok, onDismiss = { workspaceViewModel.clearBanner() })
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(providers, key = { it.entity.id }) { item ->
                    val p = item.entity
                    val isTesting = testingId == p.id
                    Card(
                        onClick = { onConfigureProvider(p.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(
                            1.dp,
                            if (p.isDefault) CodeXCrimson else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("provider_card_${p.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        if (p.isDefault) {
                                            Spacer(Modifier.width(8.dp))
                                            CodeXStatusBadge("PADRÃO", BadgeTone.CRIMSON)
                                        }
                                    }
                                    Text(
                                        text = "Modelo: ${p.selectedModel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = p.isEnabled,
                                    onCheckedChange = { workspaceViewModel.toggleProviderEnabled(p, it) }
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CodeXStatusBadge(
                                    text = if (item.hasApiKey) "Chave: ${item.maskedKey}" else "Sem chave API",
                                    tone = if (item.hasApiKey) BadgeTone.SUCCESS else BadgeTone.WARNING
                                )
                                CodeXStatusBadge(
                                    text = p.lastTestStatus,
                                    tone = if (p.lastTestStatus.startsWith("Online")) BadgeTone.INFO else BadgeTone.NEUTRAL
                                )
                            }

                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CodeXOutlinedButton(
                                    text = if (isTesting) "Testando..." else "Testar",
                                    icon = Icons.Default.NetworkCheck,
                                    enabled = !isTesting && item.hasApiKey,
                                    onClick = { workspaceViewModel.testProviderConnection(p) },
                                    modifier = Modifier.weight(1f)
                                )
                                CodeXOutlinedButton(
                                    text = if (p.isDefault) "Padrão" else "Definir Padrão",
                                    icon = Icons.Default.Star,
                                    enabled = !p.isDefault && p.isEnabled,
                                    onClick = { workspaceViewModel.setDefaultProvider(p.id) },
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { onConfigureProvider(p.id) }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Configurar API")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Adicionar Provedor Compatível") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Nome (ex: Ollama / Together)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        label = { Text("Base URL (OpenAI v1)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customModel,
                        onValueChange = { customModel = it },
                        label = { Text("Modelo padrão") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customKey,
                        onValueChange = { customKey = it },
                        label = { Text("Chave API") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    workspaceViewModel.addCustomProvider(customName, customUrl, customModel, customKey)
                    showAddDialog = false
                }) { Text("Adicionar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiSettingsScreen(
    providerId: String,
    workspaceViewModel: WorkspaceViewModel,
    onBack: () -> Unit
) {
    val providers by workspaceViewModel.providerItems.collectAsStateWithLifecycle()
    val banner by workspaceViewModel.statusBanner.collectAsStateWithLifecycle()
    val testingId by workspaceViewModel.testingProviderId.collectAsStateWithLifecycle()

    val item = providers.firstOrNull { it.entity.id == providerId }
    val provider = item?.entity

    var apiKey by remember(providerId) { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var selectedModel by remember(provider?.selectedModel) { mutableStateOf(provider?.selectedModel.orEmpty()) }
    var baseUrl by remember(provider?.baseUrl) { mutableStateOf(provider?.baseUrl.orEmpty()) }
    var isEnabled by remember(provider?.isEnabled) { mutableStateOf(provider?.isEnabled ?: true) }
    var modelsMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(providerId) {
        apiKey = workspaceViewModel.loadApiKey(providerId)
    }

    Scaffold(
        topBar = {
            CodeXTopBar(
                title = provider?.name ?: "Configurações de API",
                subtitle = "Chave, modelo e endpoint oficial",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        if (provider == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Provedor não encontrado.")
            }
            return@Scaffold
        }

        val modelsList = remember(provider.availableModelsCsv) {
            provider.availableModelsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            banner?.let { (ok, msg) ->
                CodeXFeedbackBanner(msg, isError = !ok, onDismiss = { workspaceViewModel.clearBanner() })
            }

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Chave API (criptografada no dispositivo)") },
                leadingIcon = { Icon(Icons.Default.Key, null) },
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Alternar visibilidade")
                    }
                },
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_key_input")
            )

            ExposedDropdownMenuBox(
                expanded = modelsMenuOpen,
                onExpandedChange = { modelsMenuOpen = it }
            ) {
                OutlinedTextField(
                    value = selectedModel,
                    onValueChange = { selectedModel = it },
                    label = { Text("Modelo (selecione ou digite)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelsMenuOpen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable)
                        .testTag("api_model_input")
                )
                ExposedDropdownMenu(
                    expanded = modelsMenuOpen,
                    onDismissRequest = { modelsMenuOpen = false }
                ) {
                    modelsList.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m) },
                            onClick = {
                                selectedModel = m
                                modelsMenuOpen = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Endpoint Base URL") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_baseurl_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Provedor ativo", style = MaterialTheme.typography.titleSmall)
                Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
            }

            CodeXPrimaryButton(
                text = "Salvar Configurações",
                icon = Icons.Default.CheckCircle,
                onClick = {
                    workspaceViewModel.saveProviderConfig(provider, apiKey, selectedModel, baseUrl, isEnabled)
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "api_save_button"
            )

            CodeXOutlinedButton(
                text = if (testingId == provider.id) "Testando conexão..." else "Testar Conexão Real",
                icon = Icons.Default.NetworkCheck,
                enabled = testingId != provider.id,
                onClick = {
                    workspaceViewModel.saveProviderConfig(provider, apiKey, selectedModel, baseUrl, isEnabled)
                    workspaceViewModel.testProviderConnection(
                        provider.copy(selectedModel = selectedModel, baseUrl = baseUrl),
                        overrideKey = apiKey
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "api_test_button"
            )

            if (item.hasApiKey) {
                TextButton(
                    onClick = {
                        apiKey = ""
                        workspaceViewModel.removeApiKey(provider.id)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text("Remover chave API", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
