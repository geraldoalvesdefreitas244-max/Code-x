package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BadgeTone
import com.example.ui.components.CodeXFeedbackBanner
import com.example.ui.components.CodeXOutlinedButton
import com.example.ui.components.CodeXPrimaryButton
import com.example.ui.components.CodeXStatusBadge
import com.example.ui.components.CodeXTopBar
import com.example.ui.theme.CodeXCrimson
import com.example.ui.theme.CodeXCrimsonContainer
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.BuildAndMonitorViewModel
import com.example.ui.viewmodel.WorkspaceViewModel

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    workspaceViewModel: WorkspaceViewModel,
    onBack: () -> Unit,
    onSignedOut: () -> Unit
) {
    val user by authViewModel.currentUser.collectAsStateWithLifecycle()
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val projects by workspaceViewModel.projects.collectAsStateWithLifecycle()
    val providers by workspaceViewModel.providerItems.collectAsStateWithLifecycle()

    var nameEdit by remember(user?.name) { mutableStateOf(user?.name.orEmpty()) }
    var verificationInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CodeXTopBar(
                title = "Perfil do Usuário",
                subtitle = user?.email ?: "",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        if (user == null) {
            return@Scaffold
        }
        val activeUser = user!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            uiState.errorMessage?.let {
                CodeXFeedbackBanner(it, isError = true, onDismiss = { authViewModel.clearMessages() })
            }
            uiState.successMessage?.let {
                CodeXFeedbackBanner(it, isError = false, onDismiss = { authViewModel.clearMessages() })
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = CodeXCrimson, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(activeUser.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(activeUser.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        CodeXStatusBadge(
                            text = if (activeUser.isEmailVerified) "VERIFICADO" else "PENDENTE",
                            tone = if (activeUser.isEmailVerified) BadgeTone.SUCCESS else BadgeTone.WARNING
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CodeXStatusBadge("${projects.size} Projetos", BadgeTone.CRIMSON)
                        CodeXStatusBadge("${providers.count { it.hasApiKey }} Chaves IA", BadgeTone.INFO)
                        CodeXStatusBadge(activeUser.authProvider, BadgeTone.NEUTRAL)
                    }

                    OutlinedTextField(
                        value = nameEdit,
                        onValueChange = { nameEdit = it },
                        label = { Text("Nome de exibição") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    CodeXOutlinedButton(
                        text = "Atualizar Nome",
                        icon = Icons.Default.CheckCircle,
                        onClick = { authViewModel.updateDisplayName(activeUser.id, nameEdit) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (!activeUser.isEmailVerified) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, CodeXCrimson.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = CodeXCrimson)
                            Spacer(Modifier.width(8.dp))
                            Text("Verificação de E-mail", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        if (!uiState.verificationCodeNotice.isNullOrBlank() || activeUser.verificationCode.isNotBlank()) {
                            Surface(color = CodeXCrimsonContainer, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = "Código de verificação atual: ${uiState.verificationCodeNotice ?: activeUser.verificationCode}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        OutlinedTextField(
                            value = verificationInput,
                            onValueChange = { verificationInput = it },
                            label = { Text("Digite o código de 6 dígitos") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CodeXPrimaryButton(
                                text = "Verificar",
                                onClick = { authViewModel.verifyEmail(activeUser.id, verificationInput) },
                                modifier = Modifier.weight(1f)
                            )
                            CodeXOutlinedButton(
                                text = "Reenviar código",
                                onClick = { authViewModel.resendVerificationCode(activeUser.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = CodeXCrimson)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sincronização na Nuvem", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            if (authViewModel.isFirebaseGoogleReady) "Firebase conectado"
                            else "Requer google-services.json para sincronizar na nuvem",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = activeUser.cloudSyncEnabled,
                        onCheckedChange = { authViewModel.toggleCloudSync(activeUser.id, it) }
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            CodeXPrimaryButton(
                text = "Sair da Conta (Logout)",
                icon = Icons.AutoMirrored.Filled.Logout,
                onClick = { authViewModel.signOut(onSignedOut) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "profile_logout_button"
            )
        }
    }
}

@Composable
fun SettingsScreen(
    workspaceViewModel: WorkspaceViewModel,
    buildViewModel: BuildAndMonitorViewModel,
    onBack: () -> Unit,
    onOpenAiProviders: () -> Unit
) {
    val prefs by workspaceViewModel.editorPreferences.collectAsStateWithLifecycle()
    val buildState by buildViewModel.buildState.collectAsStateWithLifecycle()
    var serverUrlInput by remember(prefs.buildServerUrl) { mutableStateOf(prefs.buildServerUrl) }

    Scaffold(
        topBar = {
            CodeXTopBar(
                title = "Configurações do IDE",
                subtitle = "Editor, Tema e Servidor de Compilação",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Editor de Código", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Text("Tamanho da fonte: ${prefs.fontSizeSp} sp", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = prefs.fontSizeSp.toFloat(),
                        onValueChange = { v ->
                            workspaceViewModel.updateEditorPreferences { it.copy(fontSizeSp = v.toInt()) }
                        },
                        valueRange = 10f..22f,
                        colors = SliderDefaults.colors(thumbColor = CodeXCrimson, activeTrackColor = CodeXCrimson)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Salvamento automático", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = prefs.autoSave,
                            onCheckedChange = { enabled ->
                                workspaceViewModel.updateEditorPreferences { it.copy(autoSave = enabled) }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Numeração de linhas", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = prefs.showLineNumbers,
                            onCheckedChange = { enabled ->
                                workspaceViewModel.updateEditorPreferences { it.copy(showLineNumbers = enabled) }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Modo Escuro Code X", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = prefs.darkTheme,
                            onCheckedChange = { dark ->
                                workspaceViewModel.updateEditorPreferences { it.copy(darkTheme = dark) }
                            }
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Servidor de Compilação Gradle (APK)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = { serverUrlInput = it },
                        label = { Text("URL do Servidor (ex: http://10.0.2.2:8787)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_build_server_input")
                    )
                    buildState.serverHealth?.let { health ->
                        CodeXStatusBadge(
                            text = health.message,
                            tone = if (health.online) BadgeTone.SUCCESS else BadgeTone.WARNING
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CodeXPrimaryButton(
                            text = "Salvar URL",
                            onClick = {
                                workspaceViewModel.updateEditorPreferences { it.copy(buildServerUrl = serverUrlInput) }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CodeXOutlinedButton(
                            text = "Testar",
                            icon = Icons.Default.NetworkCheck,
                            onClick = {
                                workspaceViewModel.updateEditorPreferences { it.copy(buildServerUrl = serverUrlInput) }
                                buildViewModel.checkBuildServerHealth(serverUrlInput)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            CodeXOutlinedButton(
                text = "Gerenciar Provedores & Chaves de IA",
                onClick = onOpenAiProviders,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
