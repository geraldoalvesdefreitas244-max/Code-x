package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BadgeTone
import com.example.ui.components.CodeXStatusBadge
import com.example.ui.components.CodeXTopBar
import com.example.ui.components.formatBytes
import com.example.ui.theme.CodeXBlack
import com.example.ui.theme.CodeXCrimson
import com.example.ui.theme.CodeXInfo
import com.example.ui.theme.CodeXSuccess
import com.example.ui.theme.CodeXWarning
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.BuildAndMonitorViewModel
import java.util.Locale

@Composable
fun DeviceMonitorScreen(
    monitorViewModel: BuildAndMonitorViewModel,
    onBack: () -> Unit,
    onOpenTouchTester: () -> Unit
) {
    val telemetry by monitorViewModel.telemetry.collectAsStateWithLifecycle()

    val ramPct = (telemetry.ramUsedBytes.toFloat() / telemetry.ramTotalBytes.toFloat()).coerceIn(0f, 1f)
    val storagePct = (telemetry.storageUsedBytes.toFloat() / telemetry.storageTotalBytes.toFloat()).coerceIn(0f, 1f)

    Scaffold(
        topBar = {
            CodeXTopBar(
                title = "Device Monitor",
                subtitle = "Telemetria real via APIs oficiais do Android",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { monitorViewModel.refreshTelemetryNow() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar telemetria")
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
            // RAM Section
            item {
                TelemetryCard(
                    title = "Memória RAM",
                    icon = Icons.Default.Memory,
                    badge = if (telemetry.isLowMemory) "MEMÓRIA BAIXA" else "${(ramPct * 100).toInt()}% EM USO",
                    badgeTone = if (telemetry.isLowMemory) BadgeTone.ERROR else BadgeTone.CRIMSON
                ) {
                    LinearProgressIndicator(
                        progress = { ramPct },
                        color = CodeXCrimson,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.height(10.dp))
                    MetricRow("RAM Total", formatBytes(telemetry.ramTotalBytes))
                    MetricRow("RAM Utilizada", formatBytes(telemetry.ramUsedBytes))
                    MetricRow("RAM Disponível", formatBytes(telemetry.ramAvailBytes))
                    MetricRow("Heap Code X (JVM)", "${formatBytes(telemetry.appHeapUsedBytes)} / ${formatBytes(telemetry.appHeapMaxBytes)}")
                    MetricRow("Consumo PSS Code X", "${telemetry.appPssKb / 1024} MB (${telemetry.appPssKb} KB)")
                }
            }

            // Storage Section
            item {
                TelemetryCard(
                    title = "Armazenamento Interno",
                    icon = Icons.Default.Storage,
                    badge = "${(storagePct * 100).toInt()}% OCUPADO",
                    badgeTone = BadgeTone.INFO
                ) {
                    LinearProgressIndicator(
                        progress = { storagePct },
                        color = CodeXInfo,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.height(10.dp))
                    MetricRow("Espaço Total", formatBytes(telemetry.storageTotalBytes))
                    MetricRow("Espaço Utilizado", formatBytes(telemetry.storageUsedBytes))
                    MetricRow("Espaço Disponível", formatBytes(telemetry.storageAvailBytes))
                    MetricRow("Tamanho dos Projetos Code X", formatBytes(telemetry.projectsSizeBytes))
                }
            }

            // Battery & Performance Section
            item {
                TelemetryCard(
                    title = "Desempenho & Bateria",
                    icon = Icons.Default.BatteryChargingFull,
                    badge = if (telemetry.batteryLevelPct >= 0) "${telemetry.batteryLevelPct}%" else "N/D",
                    badgeTone = if (telemetry.batteryLevelPct >= 20) BadgeTone.SUCCESS else BadgeTone.WARNING
                ) {
                    MetricRow("Status da Bateria", telemetry.batteryStatusLabel)
                    MetricRow(
                        "Temperatura da Bateria",
                        telemetry.batteryTempCelsius?.let { String.format(Locale.US, "%.1f °C", it) }
                            ?: "Indisponível neste dispositivo"
                    )
                    MetricRow("Voltagem", "${telemetry.batteryVoltageMv} mV")
                    MetricRow("Núcleos de CPU", "${telemetry.cpuCores} cores")
                    MetricRow(
                        "Frequência CPU",
                        if (telemetry.cpuFreqRestricted) "Restrito pelo kernel/SELinux do Android"
                        else telemetry.cpuCurrentFreqMhz.joinToString(" • ") { "${it}MHz" }
                    )
                }
            }

            // Device Info & Touch Launcher
            item {
                TelemetryCard(
                    title = "Informações do Dispositivo",
                    icon = Icons.Default.PhoneAndroid,
                    badge = "API ${telemetry.sdkInt}",
                    badgeTone = BadgeTone.NEUTRAL
                ) {
                    MetricRow("Fabricante / Modelo", "${telemetry.manufacturer} ${telemetry.deviceModel}")
                    MetricRow("Android", "v${telemetry.androidVersion} (SDK ${telemetry.sdkInt})")
                    MetricRow("Arquiteturas (ABIs)", telemetry.supportedAbis)
                }
            }

            item {
                Card(
                    onClick = onOpenTouchTester,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, CodeXCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_touch_tester_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, tint = CodeXCrimson, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Abrir Testador de Touch Screen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Multitouch, taxa de resposta (Hz), precisão e área de desenho com o dedo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryCard(
    title: String,
    icon: ImageVector,
    badge: String,
    badgeTone: BadgeTone,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = CodeXCrimson)
                    Spacer(Modifier.width(8.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                CodeXStatusBadge(badge, badgeTone)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

private data class ActiveTouchPoint(
    val id: Long,
    val position: Offset,
    val pressure: Float
)

private data class StrokeSegment(
    val points: List<Offset>,
    val color: Color
)

@Composable
fun TouchTesterScreen(
    onBack: () -> Unit
) {
    val activePointers = remember { mutableStateMapOf<Long, ActiveTouchPoint>() }
    val strokes = remember { mutableStateListOf<StrokeSegment>() }
    val currentPaths = remember { mutableStateMapOf<Long, MutableList<Offset>>() }

    var drawMode by remember { mutableStateOf(true) }
    var selectedColor by remember { mutableStateOf(CodeXCrimson) }
    var maxSimultaneous by remember { mutableIntStateOf(0) }
    var lastEventTimeNanos by remember { mutableLongStateOf(0L) }
    var touchRateHz by remember { mutableIntStateOf(0) }
    var totalEvents by remember { mutableIntStateOf(0) }

    val pointerPalette = listOf(CodeXCrimson, CodeXInfo, CodeXSuccess, CodeXWarning, Color.White)

    Scaffold(
        topBar = {
            CodeXTopBar(
                title = "Testador de Touch",
                subtitle = "Multitouch • Desenho • Resposta (Hz)",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            strokes.clear()
                            currentPaths.clear()
                            maxSimultaneous = 0
                            totalEvents = 0
                            touchRateHz = 0
                        },
                        modifier = Modifier.testTag("touch_clear_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Limpar tela")
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
            // Telemetry Header
            Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Toques ativos: ${activePointers.size}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = CodeXCrimson
                        )
                        Text(
                            "Máx simultâneo: $maxSimultaneous",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            "Taxa: $touchRateHz Hz",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = CodeXSuccess
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = drawMode,
                            onClick = { drawMode = !drawMode },
                            label = { Text(if (drawMode) "Desenho: ON" else "Desenho: OFF") },
                            leadingIcon = { Icon(Icons.Default.Brush, null, modifier = Modifier.size(16.dp)) }
                        )
                        pointerPalette.forEach { color ->
                            Surface(
                                onClick = { selectedColor = color },
                                shape = CircleShape,
                                color = color,
                                border = if (selectedColor == color) BorderStroke(2.dp, Color.White) else null,
                                modifier = Modifier.size(28.dp)
                            ) {}
                        }
                    }
                }
            }

            // Interactive Multitouch & Drawing Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(CodeXBlack)
                    .testTag("touch_tester_canvas")
                    .pointerInput(drawMode, selectedColor) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val now = System.nanoTime()
                                if (lastEventTimeNanos > 0L && event.type == PointerEventType.Move) {
                                    val dt = now - lastEventTimeNanos
                                    if (dt > 0L) {
                                        touchRateHz = (1_000_000_000L / dt).toInt().coerceIn(1, 1000)
                                    }
                                }
                                lastEventTimeNanos = now
                                totalEvents++

                                for (change in event.changes) {
                                    val id = change.id.value
                                    if (change.pressed) {
                                        activePointers[id] = ActiveTouchPoint(
                                            id = id,
                                            position = change.position,
                                            pressure = change.pressure
                                        )
                                        if (drawMode) {
                                            val list = currentPaths.getOrPut(id) { mutableListOf() }
                                            list.add(change.position)
                                        }
                                    } else {
                                        activePointers.remove(id)
                                        val finished = currentPaths.remove(id)
                                        if (finished != null && finished.size > 1) {
                                            strokes.add(StrokeSegment(finished.toList(), selectedColor))
                                        }
                                    }
                                    change.consume()
                                }
                                if (activePointers.size > maxSimultaneous) {
                                    maxSimultaneous = activePointers.size
                                }
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Precision calibration grid
                    val step = 64.dp.toPx()
                    var x = 0f
                    while (x <= size.width) {
                        drawLine(Color(0xFF1A1A22), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                        x += step
                    }
                    var y = 0f
                    while (y <= size.height) {
                        drawLine(Color(0xFF1A1A22), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                        y += step
                    }

                    // Completed strokes
                    for (stroke in strokes) {
                        if (stroke.points.size >= 2) {
                            val path = Path().apply {
                                moveTo(stroke.points.first().x, stroke.points.first().y)
                                for (i in 1 until stroke.points.size) {
                                    lineTo(stroke.points[i].x, stroke.points[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = stroke.color,
                                style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }

                    // In-progress strokes
                    for ((_, pts) in currentPaths) {
                        if (pts.size >= 2) {
                            val path = Path().apply {
                                moveTo(pts.first().x, pts.first().y)
                                for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                            }
                            drawPath(
                                path = path,
                                color = selectedColor,
                                style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }

                    // Active multitouch indicators + crosshairs
                    activePointers.values.forEachIndexed { idx, pt ->
                        val color = pointerPalette[idx % pointerPalette.size]
                        drawLine(color.copy(alpha = 0.45f), Offset(0f, pt.position.y), Offset(size.width, pt.position.y), strokeWidth = 1.5f)
                        drawLine(color.copy(alpha = 0.45f), Offset(pt.position.x, 0f), Offset(pt.position.x, size.height), strokeWidth = 1.5f)
                        drawCircle(color = color.copy(alpha = 0.25f), radius = 64f, center = pt.position)
                        drawCircle(color = color, radius = 42f, center = pt.position, style = Stroke(width = 4f))
                        drawCircle(color = Color.White, radius = 8f, center = pt.position)
                    }
                }

                // Live coordinate readout overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    if (activePointers.isEmpty()) {
                        Text(
                            text = "Toque com um ou vários dedos na tela para testar precisão e multitouch.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        activePointers.values.forEach { pt ->
                            Text(
                                text = "ID#${pt.id}: (${pt.position.x.toInt()}, ${pt.position.y.toInt()}) • pressão: ${String.format(Locale.US, "%.2f", pt.pressure)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
