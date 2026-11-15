package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.build.ApkInstallerHelper
import com.example.data.local.BuildStatus
import com.example.data.local.ProjectEntity
import com.example.data.local.TechStack
import com.example.ui.components.BadgeTone
import com.example.ui.components.CodeXFeedbackBanner
import com.example.ui.components.CodeXOutlinedButton
import com.example.ui.components.CodeXPrimaryButton
import com.example.ui.components.CodeXStatusBadge
import com.example.ui.components.CodeXTopBar
import com.example.ui.components.formatBytes
import com.example.ui.components.formatDateTime
import com.example.ui.theme.CodeXBlack
import com.example.ui.theme.CodeXCrimson
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.BuildAndMonitorViewModel
import com.example.ui.viewmodel.WorkspaceViewModel
import java.io.File

@Composable
fun ApkCompilerScreen(
    initialProjectId: String?,
    workspaceViewModel: WorkspaceViewModel,
    buildViewModel: BuildAndMonitorViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val projects by workspaceViewModel.projects.collectAsStateWithLifecycle()
    val buildState by buildViewModel.buildState.collectAsStateWithLifecycle()
    val history by buildViewModel.buildHistory.collectAsStateWithLifecycle()
    val prefs by workspaceViewModel.editorPreferences.collectAsStateWithLifecycle()

    var selectedProject by remember(projects, initialProjectId) {
        mutableStateOf(
            projects.firstOrNull { it.id == initialProjectId } ?: projects.firstOrNull()
        )
    }
    var buildType by remember { mutableStateOf("DEBUG") }
    var keystoreAlias by remember { mutableStateOf("") }
    var keystorePassword by remember { mutableStateOf("") }

    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val p = selectedProject
        if (uri != null && p != null) {
            workspaceViewModel.exportProjectZip(p, uri)
        }
    }

    LaunchedEffect(prefs.buildServerUrl) {
        buildViewModel.checkBuildServerHealth(prefs.buildServerUrl)
    }

    Scaffold(
        topBar = {
            CodeXTopBar(
                title = "Compilador APK",
                subtitle = "Gradle + Android SDK Build Engine",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { buildViewModel.checkBuildServerHealth(prefs.buildServerUrl) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Verificar servidor Gradle")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            buildState.statusMessage?.let { msg ->
                item {
                    val isErr = msg.contains("Falha", true) || msg.contains("erro", true)
                    CodeXFeedbackBanner(msg, isError = isErr, onDismiss = { buildViewModel.clearStatusMessage() })
                }
            }

            // Build Server Status Card
            item {
                val health = buildState.serverHealth
                Card(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Servidor de Build Gradle", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${prefs.buildServerUrl} • ${health?.message ?: "Verificando..."}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        CodeXStatusBadge(
                            text = if (health?.online == true) "ONLINE" else "OFFLINE",
                            tone = if (health?.online == true) BadgeTone.SUCCESS else BadgeTone.WARNING
                        )
                    }
                }
            }

            // Project Picker
            item {
                Text("Selecionar Projeto", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    projects.forEach { proj ->
                        FilterChip(
                            selected = selectedProject?.id == proj.id,
                            onClick = { selectedProject = proj },
                            label = { Text(proj.name) }
                        )
                    }
                }
            }

            // Build Variant Selector
            item {
                val isWeb = selectedProject?.techStack == TechStack.WEB_HTML_JS.name
                Text("Tipo de Compilação", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = buildType == "DEBUG",
                        onClick = { buildType = "DEBUG" },
                        label = { Text("APK Depuração (Debug)") }
                    )
                    FilterChip(
                        selected = buildType == "RELEASE",
                        onClick = { buildType = "RELEASE" },
                        label = { Text("APK Release Assinado") }
                    )
                    if (isWeb) {
                        FilterChip(
                            selected = buildType == "WEB_BUNDLE",
                            onClick = { buildType = "WEB_BUNDLE" },
                            label = { Text("Pacote Web (.zip)") }
                        )
                    }
                }

                if (buildType == "RELEASE") {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = keystoreAlias,
                            onValueChange = { keystoreAlias = it },
                            label = { Text("Keystore Alias") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = keystorePassword,
                            onValueChange = { keystorePassword = it },
                            label = { Text("Senha Keystore") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Compile & Export Actions
            item {
                val target = selectedProject
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CodeXPrimaryButton(
                        text = if (buildState.isBuilding) "Compilando..." else "Compilar Agora ($buildType)",
                        icon = Icons.Default.Build,
                        enabled = target != null && !buildState.isBuilding,
                        onClick = {
                            if (target != null) {
                                buildViewModel.compileProject(target, buildType, keystoreAlias, keystorePassword)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "compiler_start_build_button"
                    )
                    if (target != null) {
                        CodeXOutlinedButton(
                            text = "Exportar Projeto Completo (.zip)",
                            icon = Icons.Default.FileDownload,
                            onClick = { exportZipLauncher.launch("${target.name.replace(" ", "_")}.zip") },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "compiler_export_zip_button"
                        )
                    }
                    if (buildState.isBuilding) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = CodeXCrimson
                        )
                    }
                }
            }

            // Live Compiler Console
            if (buildState.liveLogs.isNotBlank()) {
                item {
                    Text("Logs de Compilação", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        color = CodeXBlack,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = buildState.liveLogs,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Build History & Generated APKs
            item {
                Text("Histórico de Builds & APKs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(history, key = { it.id }) { rec ->
                val isSuccess = rec.status == BuildStatus.SUCCESS.name
                val hasApk = rec.apkPath.endsWith(".apk") && File(rec.apkPath).exists()
                val apkMeta = remember(rec.apkPath) {
                    if (hasApk) ApkInstallerHelper.inspectApk(context, File(rec.apkPath)) else null
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${rec.projectName} • ${rec.buildType}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "${formatDateTime(rec.createdAt)} • ${rec.durationMs}ms • ${formatBytes(rec.apkSizeBytes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            CodeXStatusBadge(
                                text = rec.status,
                                tone = if (isSuccess) BadgeTone.SUCCESS else BadgeTone.ERROR
                            )
                        }
                        if (apkMeta != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "APK válido: ${apkMeta.packageName} v${apkMeta.versionName} (${apkMeta.versionCode})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        if (hasApk) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CodeXPrimaryButton(
                                    text = "Instalar APK",
                                    icon = Icons.Default.InstallMobile,
                                    onClick = { buildViewModel.installApk(context, rec.apkPath) },
                                    modifier = Modifier.weight(1f)
                                )
                                CodeXOutlinedButton(
                                    text = "Compartilhar",
                                    icon = Icons.Default.Share,
                                    onClick = { buildViewModel.shareApk(context, rec.apkPath) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
