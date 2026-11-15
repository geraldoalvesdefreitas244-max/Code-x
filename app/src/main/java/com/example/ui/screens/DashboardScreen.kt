package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.BuildStatus
import com.example.data.local.ProjectEntity
import com.example.data.local.TechStack
import com.example.ui.components.BadgeTone
import com.example.ui.components.CodeXStatusBadge
import com.example.ui.components.ProjectIconBadge
import com.example.ui.components.formatDateTime
import com.example.ui.theme.CodeXCrimson
import com.example.ui.theme.CodeXCrimsonContainer
import com.example.ui.theme.CodeXSurfaceElevated
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.WorkspaceViewModel

private data class DashboardActionItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tag: String,
    val accentCrimson: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    workspaceViewModel: WorkspaceViewModel,
    onNavigateCreateProject: () -> Unit,
    onNavigateProjects: () -> Unit,
    onNavigateAiAssistant: (String?) -> Unit,
    onNavigateAiProviders: () -> Unit,
    onNavigateEditor: (String) -> Unit,
    onNavigateCompiler: (String?) -> Unit,
    onNavigateDeviceMonitor: () -> Unit,
    onNavigateTouchTester: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    val user by authViewModel.currentUser.collectAsStateWithLifecycle()
    val projects by workspaceViewModel.projects.collectAsStateWithLifecycle()
    val latestBuild by workspaceViewModel.latestBuild.collectAsStateWithLifecycle()
    val providers by workspaceViewModel.providerItems.collectAsStateWithLifecycle()

    val configuredKeysCount = providers.count { it.hasApiKey }
    val firstProjectId = projects.firstOrNull()?.id

    val actions = listOf(
        DashboardActionItem("Novo Projeto", "Kotlin, Java ou Web", Icons.Default.Add, "dash_new_project", true, onNavigateCreateProject),
        DashboardActionItem("Meus Projetos", "${projects.size} projetos", Icons.Default.Folder, "dash_my_projects", false, onNavigateProjects),
        DashboardActionItem("Code X AI", "Robô desenvolvedor", Icons.Default.SmartToy, "dash_codex_ai", true) {
            onNavigateAiAssistant(firstProjectId)
        },
        DashboardActionItem("AI Providers", "$configuredKeysCount chave(s) ativas", Icons.Default.Hub, "dash_ai_providers", false, onNavigateAiProviders),
        DashboardActionItem("Editor de Código", "IDE + Terminal", Icons.Default.Code, "dash_code_editor", false) {
            if (firstProjectId != null) onNavigateEditor(firstProjectId) else onNavigateCreateProject()
        },
        DashboardActionItem("Compilar APK", "Gradle Debug/Release", Icons.Default.Build, "dash_compiler", true) {
            onNavigateCompiler(firstProjectId)
        },
        DashboardActionItem("Device Monitor", "RAM, CPU & Bateria", Icons.Default.Memory, "dash_device_monitor", false, onNavigateDeviceMonitor),
        DashboardActionItem("Testar Touch", "Multitouch & Desenho", Icons.Default.TouchApp, "dash_touch_tester", false, onNavigateTouchTester),
        DashboardActionItem("Configurações", "Editor e Build Server", Icons.Default.Settings, "dash_settings", false, onNavigateSettings),
        DashboardActionItem("Perfil", user?.name ?: "Conta", Icons.Default.Person, "dash_profile", false, onNavigateProfile)
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateCreateProject,
                containerColor = CodeXCrimson,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_new_project")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo Projeto")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Header Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(containerColor = CodeXSurfaceElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_dashboard_banner),
                            contentDescription = "Banner Code X",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(164.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(164.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.35f),
                                            Color.Black.copy(alpha = 0.88f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(CodeXCrimson)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "CODE X IDE",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                IconButton(
                                    onClick = onNavigateProfile,
                                    modifier = Modifier.testTag("header_profile_button")
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = "Perfil do usuário",
                                        tint = Color.White
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "Olá, ${user?.name ?: "Desenvolvedor"}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${projects.size} projeto(s) • $configuredKeysCount provedor(es) IA configurado(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Last Build Status Card
            item {
                val buildTone = when (latestBuild?.status) {
                    BuildStatus.SUCCESS.name -> BadgeTone.SUCCESS
                    BuildStatus.FAILED.name -> BadgeTone.ERROR
                    BuildStatus.RUNNING.name -> BadgeTone.WARNING
                    else -> BadgeTone.NEUTRAL
                }
                val buildLabel = when (latestBuild?.status) {
                    BuildStatus.SUCCESS.name -> "SUCESSO"
                    BuildStatus.FAILED.name -> "FALHA"
                    BuildStatus.RUNNING.name -> "COMPILANDO"
                    else -> "SEM COMPILAÇÕES"
                }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    onClick = { onNavigateCompiler(latestBuild?.projectId ?: firstProjectId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("last_build_status_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ÚLTIMA COMPILAÇÃO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = latestBuild?.let { "${it.projectName} (${it.buildType})" }
                                    ?: "Nenhuma compilação executada ainda",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = latestBuild?.let { "Em ${formatDateTime(it.createdAt)} • ${it.durationMs}ms" }
                                    ?: "Toque para abrir o compilador APK",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        CodeXStatusBadge(text = buildLabel, tone = buildTone)
                    }
                }
            }

            // Action Grid (2 columns inside LazyColumn)
            item {
                Text(
                    text = "Módulos do IDE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            val rows = actions.chunked(2)
            items(rows) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (action in pair) {
                        DashboardModuleCard(
                            item = action,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            // Recent Projects Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Projetos Recentes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        onClick = onNavigateProjects,
                        color = Color.Transparent,
                        modifier = Modifier.testTag("see_all_projects_button")
                    ) {
                        Text(
                            text = "Ver todos",
                            color = CodeXCrimson,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (projects.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        onClick = onNavigateCreateProject,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = null,
                                tint = CodeXCrimson,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Nenhum projeto criado",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Toque aqui para criar seu primeiro aplicativo Kotlin, Java ou Web.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(projects.take(5), key = { it.id }) { project ->
                    RecentProjectCard(
                        project = project,
                        onOpenEditor = { onNavigateEditor(project.id) },
                        onOpenAi = { onNavigateAiAssistant(project.id) },
                        onCompile = { onNavigateCompiler(project.id) }
                    )
                }
            }
            item { Spacer(Modifier.height(64.dp)) }
        }
    }
}

@Composable
private fun DashboardModuleCard(
    item: DashboardActionItem,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = item.onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.accentCrimson) CodeXCrimsonContainer.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (item.accentCrimson) CodeXCrimson.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.testTag(item.tag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (item.accentCrimson) CodeXCrimson
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = if (item.accentCrimson) Color.White else CodeXCrimson,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentProjectCard(
    project: ProjectEntity,
    onOpenEditor: () -> Unit,
    onOpenAi: () -> Unit,
    onCompile: () -> Unit
) {
    val stack = runCatching { TechStack.valueOf(project.techStack) }.getOrDefault(TechStack.KOTLIN_COMPOSE)
    Card(
        onClick = onOpenEditor,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_project_${project.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProjectIconBadge(preset = project.iconPreset)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${project.packageName} • v${project.versionName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                CodeXStatusBadge(text = stack.shortLabel, tone = BadgeTone.CRIMSON)
            }
            IconButton(onClick = onOpenAi) {
                Icon(Icons.Default.SmartToy, contentDescription = "Abrir Code X AI", tint = CodeXCrimson)
            }
            IconButton(onClick = onCompile) {
                Icon(Icons.Default.Build, contentDescription = "Compilar", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
