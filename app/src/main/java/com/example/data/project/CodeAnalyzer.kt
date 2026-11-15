package com.example.data.project

import org.json.JSONArray
import org.json.JSONObject
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

enum class DiagnosticSeverity { ERROR, WARNING, INFO }

data class CodeDiagnostic(
    val filePath: String,
    val line: Int,
    val severity: DiagnosticSeverity,
    val message: String
)

object CodeAnalyzer {

    fun analyzeFile(filePath: String, content: String): List<CodeDiagnostic> {
        val diagnostics = mutableListOf<CodeDiagnostic>()
        val ext = filePath.substringAfterLast('.', "").lowercase()

        when (ext) {
            "kt", "java", "js", "css", "kts", "gradle" -> {
                diagnostics += checkDelimiters(filePath, content)
                if (ext == "java" && !content.contains("class ") && !content.contains("interface ")) {
                    diagnostics += CodeDiagnostic(
                        filePath, 1, DiagnosticSeverity.WARNING,
                        "Nenhuma declaração 'class' ou 'interface' encontrada."
                    )
                }
                if (ext == "kt" && content.contains("findViewById") && !content.contains("import")) {
                    diagnostics += CodeDiagnostic(
                        filePath, 1, DiagnosticSeverity.INFO,
                        "Dica: Prefira Jetpack Compose ou ViewBinding em projetos modernos."
                    )
                }
            }
            "xml" -> {
                diagnostics += validateXml(filePath, content)
            }
            "json" -> {
                diagnostics += validateJson(filePath, content)
            }
            "html", "htm" -> {
                if (!content.contains("<html", ignoreCase = true)) {
                    diagnostics += CodeDiagnostic(
                        filePath, 1, DiagnosticSeverity.WARNING,
                        "Tag raiz <html> não encontrada no documento HTML."
                    )
                }
                diagnostics += checkTagPairs(filePath, content)
            }
        }
        return diagnostics
    }

    fun analyzeProject(projectFiles: Map<String, String>): List<CodeDiagnostic> {
        val all = mutableListOf<CodeDiagnostic>()
        for ((path, content) in projectFiles) {
            all += analyzeFile(path, content)
        }
        return all
    }

    fun autocompleteSuggestions(extension: String, currentWord: String, fullContent: String): List<String> {
        val baseKeywords = when (extension.lowercase()) {
            "kt", "kts" -> listOf(
                "package", "import", "class", "data class", "sealed interface", "object",
                "fun", "val", "var", "private", "override", "suspend", "return",
                "when", "if", "else", "for", "while", "remember", "mutableStateOf",
                "@Composable", "Modifier", "Scaffold", "Column", "Row", "Box",
                "Text", "Button", "Card", "LazyColumn", "Spacer", "LaunchedEffect",
                "StateFlow", "MutableStateFlow", "viewModelScope"
            )
            "java" -> listOf(
                "package", "import", "public", "private", "protected", "class",
                "extends", "implements", "void", "return", "new", "final", "static",
                "if", "else", "switch", "for", "while", "@Override", "AppCompatActivity",
                "onCreate", "setContentView", "findViewById", "Intent", "Bundle"
            )
            "xml" -> listOf(
                "<manifest", "<application", "<activity", "<intent-filter>",
                "android:layout_width=\"match_parent\"", "android:layout_height=\"wrap_content\"",
                "android:orientation=\"vertical\"", "android:text=\"\"",
                "android:name=\"\"", "android:exported=\"true\""
            )
            "html", "htm" -> listOf(
                "<!DOCTYPE html>", "<html lang=\"pt-BR\">", "<head>", "<body>",
                "<main>", "<section>", "<div class=\"\">", "<button>", "<input>",
                "<script src=\"\"></script>", "<link rel=\"stylesheet\" href=\"\" />"
            )
            "css" -> listOf(
                "display: flex;", "display: grid;", "background:", "color:",
                "padding:", "margin:", "border-radius:", "font-size:",
                "justify-content: center;", "align-items: center;"
            )
            "js" -> listOf(
                "const", "let", "function", "async", "await", "return",
                "document.getElementById", "document.querySelector", "addEventListener",
                "fetch", "console.log", "JSON.stringify", "JSON.parse", "localStorage"
            )
            else -> listOf("android", "dependencies", "implementation", "versionName", "versionCode")
        }

        val docIdentifiers = Regex("[a-zA-Z_][a-zA-Z0-9_]{2,}")
            .findAll(fullContent)
            .map { it.value }
            .distinct()
            .take(40)
            .toList()

        val combined = (baseKeywords + docIdentifiers).distinct()
        if (currentWord.isBlank()) return combined.take(12)
        return combined
            .filter { it.startsWith(currentWord, ignoreCase = true) && it != currentWord }
            .take(12)
    }

    private fun checkDelimiters(filePath: String, content: String): List<CodeDiagnostic> {
        val stack = ArrayDeque<Pair<Char, Int>>()
        val diagnostics = mutableListOf<CodeDiagnostic>()
        var inString = false
        var stringChar = '"'
        var inLineComment = false
        var inBlockComment = false
        var line = 1

        var i = 0
        while (i < content.length) {
            val c = content[i]
            val next = content.getOrNull(i + 1)

            if (c == '\n') {
                line++
                inLineComment = false
                i++
                continue
            }
            if (inLineComment) {
                i++
                continue
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false
                    i += 2
                    continue
                }
                i++
                continue
            }
            if (!inString && c == '/' && next == '/') {
                inLineComment = true
                i += 2
                continue
            }
            if (!inString && c == '/' && next == '*') {
                inBlockComment = true
                i += 2
                continue
            }
            if (c == '"' || c == '\'') {
                if (!inString) {
                    inString = true
                    stringChar = c
                } else if (c == stringChar && content.getOrNull(i - 1) != '\\') {
                    inString = false
                }
                i++
                continue
            }
            if (!inString) {
                when (c) {
                    '{', '(', '[' -> stack.addLast(c to line)
                    '}', ')', ']' -> {
                        val expectedOpen = when (c) {
                            '}' -> '{'
                            ')' -> '('
                            else -> '['
                        }
                        if (stack.isEmpty() || stack.last().first != expectedOpen) {
                            diagnostics += CodeDiagnostic(
                                filePath, line, DiagnosticSeverity.ERROR,
                                "Fechamento inesperado '$c' sem abertura correspondente."
                            )
                        } else {
                            stack.removeLast()
                        }
                    }
                }
            }
            i++
        }

        if (inString) {
            diagnostics += CodeDiagnostic(
                filePath, line, DiagnosticSeverity.ERROR,
                "String não fechada antes do fim do arquivo."
            )
        }
        while (stack.isNotEmpty()) {
            val (openChar, openLine) = stack.removeLast()
            diagnostics += CodeDiagnostic(
                filePath, openLine, DiagnosticSeverity.ERROR,
                "Bloco '$openChar' aberto na linha $openLine não foi fechado."
            )
        }
        return diagnostics
    }

    private fun validateXml(filePath: String, content: String): List<CodeDiagnostic> {
        if (content.isBlank()) return emptyList()
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            builder.parse(InputSource(StringReader(content)))
            emptyList()
        } catch (e: Exception) {
            listOf(
                CodeDiagnostic(
                    filePath,
                    1,
                    DiagnosticSeverity.ERROR,
                    "XML inválido: ${e.localizedMessage ?: "erro de sintaxe"}"
                )
            )
        }
    }

    private fun validateJson(filePath: String, content: String): List<CodeDiagnostic> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return emptyList()
        return try {
            if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONObject(trimmed)
            emptyList()
        } catch (e: Exception) {
            listOf(
                CodeDiagnostic(
                    filePath,
                    1,
                    DiagnosticSeverity.ERROR,
                    "JSON inválido: ${e.localizedMessage ?: "erro de sintaxe"}"
                )
            )
        }
    }

    private fun checkTagPairs(filePath: String, content: String): List<CodeDiagnostic> {
        val voidTags = setOf("meta", "link", "br", "hr", "img", "input", "!doctype")
        val regex = Regex("<(/?)([a-zA-Z!][a-zA-Z0-9]*)([^>]*)>")
        val stack = ArrayDeque<String>()
        val issues = mutableListOf<CodeDiagnostic>()
        for (match in regex.findAll(content)) {
            val isClosing = match.groupValues[1] == "/"
            val tag = match.groupValues[2].lowercase()
            val attrs = match.groupValues[3]
            val selfClosing = attrs.trimEnd().endsWith("/")
            if (tag in voidTags || selfClosing || tag.startsWith("!")) continue
            if (!isClosing) {
                stack.addLast(tag)
            } else {
                if (stack.isNotEmpty() && stack.last() == tag) {
                    stack.removeLast()
                } else {
                    issues += CodeDiagnostic(
                        filePath, 1, DiagnosticSeverity.WARNING,
                        "Tag HTML </$tag> fora de ordem ou sem abertura."
                    )
                }
            }
        }
        return issues
    }
}
