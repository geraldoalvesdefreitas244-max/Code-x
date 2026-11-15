package com.example.data.project

import com.example.data.local.TechStack
import java.io.File

object ProjectTemplateGenerator {

    fun generateProjectFiles(
        rootDir: File,
        appName: String,
        packageName: String,
        versionName: String,
        versionCode: Int,
        techStack: TechStack
    ) {
        if (!rootDir.exists()) rootDir.mkdirs()
        when (techStack) {
            TechStack.KOTLIN_COMPOSE -> generateKotlinComposeProject(
                rootDir, appName, packageName, versionName, versionCode
            )
            TechStack.JAVA_ANDROID -> generateJavaAndroidProject(
                rootDir, appName, packageName, versionName, versionCode
            )
            TechStack.WEB_HTML_JS -> generateWebProject(
                rootDir, appName, packageName, versionName
            )
        }
    }

    private fun generateKotlinComposeProject(
        rootDir: File,
        appName: String,
        packageName: String,
        versionName: String,
        versionCode: Int
    ) {
        val pkgPath = packageName.replace('.', '/')
        write(rootDir, "settings.gradle.kts", """
            pluginManagement {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories {
                    google()
                    mavenCentral()
                }
            }
            rootProject.name = "$appName"
            include(":app")
        """.trimIndent())

        write(rootDir, "build.gradle.kts", """
            plugins {
                id("com.android.application") version "8.5.2" apply false
                id("org.jetbrains.kotlin.android") version "2.0.20" apply false
                id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
            }
        """.trimIndent())

        write(rootDir, "gradle.properties", """
            org.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8
            android.useAndroidX=true
            kotlin.code.style=official
            android.nonTransitiveRClass=true
        """.trimIndent())

        write(rootDir, "app/build.gradle.kts", """
            plugins {
                id("com.android.application")
                id("org.jetbrains.kotlin.android")
                id("org.jetbrains.kotlin.plugin.compose")
            }

            android {
                namespace = "$packageName"
                compileSdk = 35

                defaultConfig {
                    applicationId = "$packageName"
                    minSdk = 24
                    targetSdk = 35
                    versionCode = $versionCode
                    versionName = "$versionName"
                }

                buildTypes {
                    release {
                        isMinifyEnabled = false
                    }
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                kotlinOptions {
                    jvmTarget = "17"
                }
                buildFeatures {
                    compose = true
                }
            }

            dependencies {
                implementation("androidx.core:core-ktx:1.13.1")
                implementation("androidx.activity:activity-compose:1.9.3")
                implementation(platform("androidx.compose:compose-bom:2024.10.01"))
                implementation("androidx.compose.ui:ui")
                implementation("androidx.compose.material3:material3")
            }
        """.trimIndent())

        write(rootDir, "app/src/main/AndroidManifest.xml", """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application
                    android:allowBackup="true"
                    android:label="@string/app_name"
                    android:supportsRtl="true"
                    android:theme="@android:style/Theme.Material.NoActionBar">
                    <activity
                        android:name=".MainActivity"
                        android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
        """.trimIndent())

        write(rootDir, "app/src/main/res/values/strings.xml", """
            <resources>
                <string name="app_name">$appName</string>
            </resources>
        """.trimIndent())

        write(rootDir, "app/src/main/java/$pkgPath/MainActivity.kt", """
            package $packageName

            import android.os.Bundle
            import androidx.activity.ComponentActivity
            import androidx.activity.compose.setContent
            import androidx.compose.foundation.background
            import androidx.compose.foundation.layout.*
            import androidx.compose.material3.*
            import androidx.compose.runtime.*
            import androidx.compose.ui.Alignment
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.text.font.FontWeight
            import androidx.compose.ui.unit.dp

            class MainActivity : ComponentActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    setContent {
                        MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFE50914))) {
                            AppScreen()
                        }
                    }
                }
            }

            @Composable
            fun AppScreen() {
                var counter by remember { mutableIntStateOf(0) }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF09090B)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$appName",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Criado com Code X Mobile IDE • v$versionName",
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { counter++ }) {
                            Text("Toques: ${'$'}counter")
                        }
                    }
                }
            }
        """.trimIndent())

        File(rootDir, "app/src/main/assets").mkdirs()

        write(rootDir, "README.md", """
            # $appName

            - **Pacote:** `$packageName`
            - **Versão:** `$versionName` (`$versionCode`)
            - **Stack:** Android Nativo (Kotlin + Jetpack Compose)

            Gerado pelo **Code X**.
        """.trimIndent())
    }

    private fun generateJavaAndroidProject(
        rootDir: File,
        appName: String,
        packageName: String,
        versionName: String,
        versionCode: Int
    ) {
        val pkgPath = packageName.replace('.', '/')
        write(rootDir, "settings.gradle", """
            pluginManagement {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories {
                    google()
                    mavenCentral()
                }
            }
            rootProject.name = "$appName"
            include ':app'
        """.trimIndent())

        write(rootDir, "build.gradle", """
            plugins {
                id 'com.android.application' version '8.5.2' apply false
            }
        """.trimIndent())

        write(rootDir, "gradle.properties", """
            org.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8
            android.useAndroidX=true
        """.trimIndent())

        write(rootDir, "app/build.gradle", """
            plugins {
                id 'com.android.application'
            }

            android {
                namespace '$packageName'
                compileSdk 35

                defaultConfig {
                    applicationId "$packageName"
                    minSdk 24
                    targetSdk 35
                    versionCode $versionCode
                    versionName "$versionName"
                }

                buildTypes {
                    release {
                        minifyEnabled false
                    }
                }
                compileOptions {
                    sourceCompatibility JavaVersion.VERSION_17
                    targetCompatibility JavaVersion.VERSION_17
                }
            }

            dependencies {
                implementation 'androidx.appcompat:appcompat:1.7.0'
                implementation 'com.google.android.material:material:1.12.0'
            }
        """.trimIndent())

        write(rootDir, "app/src/main/AndroidManifest.xml", """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application
                    android:allowBackup="true"
                    android:label="@string/app_name"
                    android:supportsRtl="true"
                    android:theme="@style/Theme.Material3.Dark.NoActionBar">
                    <activity
                        android:name=".MainActivity"
                        android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
        """.trimIndent())

        write(rootDir, "app/src/main/res/layout/activity_main.xml", """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:background="#09090B"
                android:gravity="center"
                android:orientation="vertical"
                android:padding="24dp">

                <TextView
                    android:id="@+id/titleText"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="$appName"
                    android:textColor="#FFFFFF"
                    android:textSize="26sp"
                    android:textStyle="bold" />

                <Button
                    android:id="@+id/actionButton"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="20dp"
                    android:backgroundTint="#E50914"
                    android:text="Executar Ação"
                    android:textColor="#FFFFFF" />
            </LinearLayout>
        """.trimIndent())

        write(rootDir, "app/src/main/res/values/strings.xml", """
            <resources>
                <string name="app_name">$appName</string>
            </resources>
        """.trimIndent())

        write(rootDir, "app/src/main/java/$pkgPath/MainActivity.java", """
            package $packageName;

            import android.os.Bundle;
            import android.widget.Button;
            import android.widget.TextView;
            import androidx.appcompat.app.AppCompatActivity;

            public class MainActivity extends AppCompatActivity {
                private int count = 0;

                @Override
                protected void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);
                    setContentView(R.layout.activity_main);

                    TextView titleText = findViewById(R.id.titleText);
                    Button actionButton = findViewById(R.id.actionButton);

                    actionButton.setOnClickListener(v -> {
                        count++;
                        titleText.setText("$appName • Cliques: " + count);
                    });
                }
            }
        """.trimIndent())

        File(rootDir, "app/src/main/assets").mkdirs()

        write(rootDir, "README.md", """
            # $appName

            - **Pacote:** `$packageName`
            - **Versão:** `$versionName` (`$versionCode`)
            - **Stack:** Android Nativo (Java)

            Gerado pelo **Code X**.
        """.trimIndent())
    }

    private fun generateWebProject(
        rootDir: File,
        appName: String,
        packageName: String,
        versionName: String
    ) {
        write(rootDir, "index.html", """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>$appName</title>
              <link rel="stylesheet" href="styles/main.css" />
            </head>
            <body>
              <main class="card">
                <span class="badge">CODE X WEB APP • v$versionName</span>
                <h1>$appName</h1>
                <p>Pacote: <code>$packageName</code></p>
                <button id="counterBtn">Interagir (0)</button>
              </main>
              <script src="scripts/app.js"></script>
            </body>
            </html>
        """.trimIndent())

        write(rootDir, "styles/main.css", """
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body {
              font-family: system-ui, -apple-system, sans-serif;
              background: #09090B;
              color: #F8FAFC;
              min-height: 100vh;
              display: grid;
              place-items: center;
              padding: 24px;
            }
            .card {
              width: 100%;
              max-width: 420px;
              background: #111115;
              border: 1px solid #2D2D39;
              border-radius: 18px;
              padding: 28px;
              text-align: center;
            }
            .badge {
              display: inline-block;
              background: #3A0A0F;
              color: #FF4D5A;
              padding: 6px 12px;
              border-radius: 999px;
              font-size: 11px;
              font-weight: 700;
              margin-bottom: 16px;
            }
            h1 { font-size: 26px; margin-bottom: 10px; }
            p { color: #94A3B8; margin-bottom: 24px; font-size: 14px; }
            button {
              width: 100%;
              background: #E50914;
              color: #FFFFFF;
              border: none;
              border-radius: 12px;
              padding: 14px 20px;
              font-size: 15px;
              font-weight: 700;
              cursor: pointer;
            }
        """.trimIndent())

        write(rootDir, "scripts/app.js", """
            let clicks = 0;
            const btn = document.getElementById("counterBtn");
            if (btn) {
              btn.addEventListener("click", () => {
                clicks += 1;
                btn.textContent = `Interagir (${'$'}{clicks})`;
                console.log("Clique registrado:", clicks);
              });
            }
        """.trimIndent())

        write(rootDir, "manifest.json", """
            {
              "name": "$appName",
              "package": "$packageName",
              "version": "$versionName",
              "start_url": "index.html",
              "display": "standalone",
              "theme_color": "#E50914",
              "background_color": "#09090B"
            }
        """.trimIndent())

        write(rootDir, "README.md", """
            # $appName

            - **Identificador:** `$packageName`
            - **Versão:** `$versionName`
            - **Stack:** Web App (HTML5 + CSS3 + JavaScript)
        """.trimIndent())
    }

    private fun write(rootDir: File, relativePath: String, content: String) {
        val target = File(rootDir, relativePath)
        target.parentFile?.mkdirs()
        target.writeText(content + "\n")
    }
}
