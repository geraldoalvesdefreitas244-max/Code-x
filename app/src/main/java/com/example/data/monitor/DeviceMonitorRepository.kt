package com.example.data.monitor

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.Environment
import android.os.StatFs
import com.example.data.project.ProjectFileManager
import java.io.File

data class DeviceTelemetry(
    val ramTotalBytes: Long,
    val ramAvailBytes: Long,
    val ramUsedBytes: Long,
    val isLowMemory: Boolean,
    val appHeapUsedBytes: Long,
    val appHeapMaxBytes: Long,
    val appPssKb: Int,
    val storageTotalBytes: Long,
    val storageAvailBytes: Long,
    val storageUsedBytes: Long,
    val projectsSizeBytes: Long,
    val batteryLevelPct: Int,
    val batteryTempCelsius: Float?,
    val batteryVoltageMv: Int,
    val batteryStatusLabel: String,
    val cpuCores: Int,
    val cpuCurrentFreqMhz: List<Int>,
    val cpuFreqRestricted: Boolean,
    val deviceModel: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkInt: Int,
    val supportedAbis: String,
    val capturedAtMillis: Long
)

class DeviceMonitorRepository(
    private val context: Context,
    private val fileManager: ProjectFileManager
) {

    fun collectTelemetry(): DeviceTelemetry {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val ramTotal = memInfo.totalMem.coerceAtLeast(1L)
        val ramAvail = memInfo.availMem.coerceAtLeast(0L)
        val ramUsed = (ramTotal - ramAvail).coerceAtLeast(0L)

        val runtime = Runtime.getRuntime()
        val appHeapUsed = (runtime.totalMemory() - runtime.freeMemory()).coerceAtLeast(0L)
        val appHeapMax = runtime.maxMemory().coerceAtLeast(1L)

        val debugMem = Debug.MemoryInfo()
        Debug.getMemoryInfo(debugMem)
        val appPssKb = debugMem.totalPss

        val statFs = StatFs(Environment.getDataDirectory().path)
        val storageTotal = statFs.totalBytes.coerceAtLeast(1L)
        val storageAvail = statFs.availableBytes.coerceAtLeast(0L)
        val storageUsed = (storageTotal - storageAvail).coerceAtLeast(0L)

        val projectsBytes = fileManager.calculateFolderSizeBytes(fileManager.projectsRootDir())

        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else -1
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val tempCelsius = if (tempTenths != Int.MIN_VALUE) tempTenths / 10f else null
        val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val statusInt = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val statusLabel = when (statusInt) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Carregando"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Descarregando"
            BatteryManager.BATTERY_STATUS_FULL -> "Completa"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Conectado (sem carga)"
            else -> "Desconhecido"
        }

        val cores = Runtime.getRuntime().availableProcessors()
        val freqs = readCpuFrequenciesMhz(cores)

        return DeviceTelemetry(
            ramTotalBytes = ramTotal,
            ramAvailBytes = ramAvail,
            ramUsedBytes = ramUsed,
            isLowMemory = memInfo.lowMemory,
            appHeapUsedBytes = appHeapUsed,
            appHeapMaxBytes = appHeapMax,
            appPssKb = appPssKb,
            storageTotalBytes = storageTotal,
            storageAvailBytes = storageAvail,
            storageUsedBytes = storageUsed,
            projectsSizeBytes = projectsBytes,
            batteryLevelPct = batteryPct,
            batteryTempCelsius = tempCelsius,
            batteryVoltageMv = voltageMv,
            batteryStatusLabel = statusLabel,
            cpuCores = cores,
            cpuCurrentFreqMhz = freqs,
            cpuFreqRestricted = freqs.isEmpty(),
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE ?: "?",
            sdkInt = Build.VERSION.SDK_INT,
            supportedAbis = Build.SUPPORTED_ABIS.joinToString(", "),
            capturedAtMillis = System.currentTimeMillis()
        )
    }

    private fun readCpuFrequenciesMhz(cores: Int): List<Int> {
        val result = mutableListOf<Int>()
        for (i in 0 until cores) {
            try {
                val file = File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
                if (file.exists() && file.canRead()) {
                    val khz = file.readText().trim().toIntOrNull()
                    if (khz != null && khz > 0) {
                        result.add(khz / 1000)
                    }
                }
            } catch (_: Exception) {
                // Restricted by SELinux on modern Android versions
            }
        }
        return result
    }
}
