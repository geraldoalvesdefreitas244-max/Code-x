package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.AiFilePatchParser
import com.example.data.auth.AuthRepository
import com.example.data.local.CodeXDatabase
import com.example.data.local.SecureKeyStore
import com.example.data.local.TechStack
import com.example.data.project.CodeAnalyzer
import com.example.data.project.DiagnosticSeverity
import com.example.data.project.IntegratedTerminalEngine
import com.example.data.project.ProjectTemplateGenerator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CodeXCoreTests {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var db: CodeXDatabase
    private lateinit var secureKeyStore: SecureKeyStore
    private lateinit var authRepo: AuthRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, CodeXDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        secureKeyStore = SecureKeyStore(context)
        authRepo = AuthRepository(context, db.userDao(), secureKeyStore)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `template generator creates valid trees for all three stacks`() {
        val kotlinRoot = tempFolder.newFolder("kotlin_app")
        ProjectTemplateGenerator.generateProjectFiles(
            rootDir = kotlinRoot,
            appName = "Alpha App",
            packageName = "com.codex.alpha",
            versionName = "1.0.0",
            versionCode = 1,
            techStack = TechStack.KOTLIN_COMPOSE
        )
        assertTrue(File(kotlinRoot, "app/build.gradle.kts").exists())
        assertTrue(File(kotlinRoot, "app/src/main/java/com/codex/alpha/MainActivity.kt").exists())
        assertTrue(File(kotlinRoot, "app/src/main/AndroidManifest.xml").exists())

        val javaRoot = tempFolder.newFolder("java_app")
        ProjectTemplateGenerator.generateProjectFiles(
            rootDir = javaRoot,
            appName = "Beta App",
            packageName = "com.codex.beta",
            versionName = "2.1.0",
            versionCode = 2,
            techStack = TechStack.JAVA_ANDROID
        )
        assertTrue(File(javaRoot, "app/build.gradle").exists())
        assertTrue(File(javaRoot, "app/src/main/java/com/codex/beta/MainActivity.java").exists())
        assertTrue(File(javaRoot, "app/src/main/res/layout/activity_main.xml").exists())

        val webRoot = tempFolder.newFolder("web_app")
        ProjectTemplateGenerator.generateProjectFiles(
            rootDir = webRoot,
            appName = "Gamma Web",
            packageName = "com.codex.gamma",
            versionName = "1.0",
            versionCode = 1,
            techStack = TechStack.WEB_HTML_JS
        )
        assertTrue(File(webRoot, "index.html").exists())
        assertTrue(File(webRoot, "styles/main.css").exists())
        assertTrue(File(webRoot, "scripts/app.js").exists())
        assertTrue(File(webRoot, "manifest.json").exists())
    }

    @Test
    fun `code analyzer detects syntax errors and validates clean code`() {
        val cleanKt = """
            package com.example
            fun hello() {
                println("Code X {ok}")
            }
        """.trimIndent()
        assertTrue(CodeAnalyzer.analyzeFile("Main.kt", cleanKt).none { it.severity == DiagnosticSeverity.ERROR })

        val brokenKt = """
            package com.example
            fun broken() {
                if (true) {
                    println("missing brace")
            }
        """.trimIndent()
        val ktIssues = CodeAnalyzer.analyzeFile("Broken.kt", brokenKt)
        assertTrue(ktIssues.any { it.severity == DiagnosticSeverity.ERROR })

        val invalidJson = """{"name": "Code X", "broken": }"""
        assertTrue(CodeAnalyzer.analyzeFile("manifest.json", invalidJson).any { it.severity == DiagnosticSeverity.ERROR })

        val validJson = """{"name": "Code X", "version": 1}"""
        assertTrue(CodeAnalyzer.analyzeFile("manifest.json", validJson).isEmpty())

        val invalidXml = """<manifest><application></manifest>"""
        assertTrue(CodeAnalyzer.analyzeFile("AndroidManifest.xml", invalidXml).any { it.severity == DiagnosticSeverity.ERROR })
    }

    @Test
    fun `integrated terminal executes commands inside sandbox and blocks traversal`() {
        val projectDir = tempFolder.newFolder("term_proj")
        ProjectTemplateGenerator.generateProjectFiles(
            rootDir = projectDir,
            appName = "TermApp",
            packageName = "com.codex.term",
            versionName = "1.0",
            versionCode = 1,
            techStack = TechStack.WEB_HTML_JS
        )

        val lsRes = IntegratedTerminalEngine.execute(projectDir, "", "ls")
        assertTrue(lsRes.output.contains("index.html"))

        val mkdirRes = IntegratedTerminalEngine.execute(projectDir, "", "mkdir utils")
        assertTrue(File(projectDir, "utils").isDirectory)
        assertTrue(mkdirRes.output.contains("utils"))

        val cdRes = IntegratedTerminalEngine.execute(projectDir, "", "cd utils")
        assertEquals("utils", cdRes.newCurrentDir)

        val touchRes = IntegratedTerminalEngine.execute(projectDir, cdRes.newCurrentDir, "touch helper.js")
        assertTrue(File(projectDir, "utils/helper.js").exists())
        assertTrue(touchRes.output.contains("helper.js"))

        val grepRes = IntegratedTerminalEngine.execute(projectDir, "", "grep TermApp")
        assertTrue(grepRes.output.contains("index.html"))

        val buildRes = IntegratedTerminalEngine.execute(projectDir, "", "build debug")
        assertEquals("DEBUG", buildRes.triggerBuildType)

        // Sandbox traversal must be blocked
        val escapeRes = IntegratedTerminalEngine.execute(projectDir, "", "cd ../../..")
        assertTrue(escapeRes.output.contains("não encontrada"))
    }

    @Test
    fun `ai file patch parser extracts custom blocks and markdown fallback`() {
        val aiResponse = """
            Implementei a tela de login solicitada:
            <<<FILE: app/src/main/java/com/app/LoginScreen.kt>>>
            package com.app
            fun LoginScreen() {}
            <<<END_FILE>>>
            <<<FILE: /styles/theme.css>>>
            body { background: #09090B; }
            <<<END_FILE>>>
        """.trimIndent()

        val extracted = AiFilePatchParser.extractFiles(aiResponse)
        assertEquals(2, extracted.size)
        assertEquals("app/src/main/java/com/app/LoginScreen.kt", extracted[0].relativePath)
        assertTrue(extracted[0].content.contains("fun LoginScreen()"))
        assertEquals("styles/theme.css", extracted[1].relativePath)

        val serialized = AiFilePatchParser.toJson(extracted)
        val roundTrip = AiFilePatchParser.fromJson(serialized)
        assertEquals(extracted, roundTrip)
    }

    @Test
    fun `auth repository handles local registration, login, verification, and password recovery`() = runBlocking {
        val regResult = authRepo.registerLocalAccount("Lucas Silva", "lucas@codex.dev", "segredo123")
        assertTrue(regResult.isSuccess)
        val user = regResult.getOrNull()
        assertNotNull(user)
        assertFalse(user!!.isEmailVerified)
        assertEquals(6, user.verificationCode.length)

        // Duplicate registration fails
        val dup = authRepo.registerLocalAccount("Outro", "lucas@codex.dev", "segredo123")
        assertTrue(dup.isFailure)

        // Email verification succeeds with generated code
        val verified = authRepo.verifyEmailCode(user.id, user.verificationCode)
        assertTrue(verified.isSuccess)
        assertTrue(verified.getOrThrow().isEmailVerified)

        // Wrong password fails, right password succeeds
        assertTrue(authRepo.loginWithEmailAndPassword("lucas@codex.dev", "errada").isFailure)
        assertTrue(authRepo.loginWithEmailAndPassword("lucas@codex.dev", "segredo123").isSuccess)

        // Password recovery code & reset
        val codeRes = authRepo.requestPasswordRecoveryCode("lucas@codex.dev")
        assertTrue(codeRes.isSuccess)
        val resetRes = authRepo.resetPasswordWithCode("lucas@codex.dev", codeRes.getOrThrow(), "novaSenha99")
        assertTrue(resetRes.isSuccess)
        assertTrue(authRepo.loginWithEmailAndPassword("lucas@codex.dev", "novaSenha99").isSuccess)
    }
}
