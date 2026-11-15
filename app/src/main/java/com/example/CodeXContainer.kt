package com.example

import android.content.Context
import com.example.data.ai.AiProviderCatalog
import com.example.data.ai.MultiAiClient
import com.example.data.auth.AuthRepository
import com.example.data.build.BuildServerClient
import com.example.data.local.CodeXDatabase
import com.example.data.local.SecureKeyStore
import com.example.data.monitor.DeviceMonitorRepository
import com.example.data.project.ProjectFileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CodeXContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: CodeXDatabase = CodeXDatabase.getInstance(appContext)
    val secureKeyStore: SecureKeyStore = SecureKeyStore(appContext)
    val fileManager: ProjectFileManager = ProjectFileManager(appContext)
    val authRepository: AuthRepository = AuthRepository(
        context = appContext,
        userDao = database.userDao(),
        secureKeyStore = secureKeyStore
    )
    val multiAiClient: MultiAiClient = MultiAiClient()
    val buildServerClient: BuildServerClient = BuildServerClient()
    val deviceMonitorRepository: DeviceMonitorRepository = DeviceMonitorRepository(
        context = appContext,
        fileManager = fileManager
    )

    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        initScope.launch {
            database.aiProviderDao().insertAllIgnore(AiProviderCatalog.defaultProviders())
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: CodeXContainer? = null

        fun getInstance(context: Context): CodeXContainer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CodeXContainer(context).also { INSTANCE = it }
            }
        }
    }
}
