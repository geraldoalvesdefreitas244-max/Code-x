package com.example.data.project

import java.io.File

data class TerminalResult(
    val output: String,
    val newCurrentDir: String,
    val clearScreen: Boolean = false,
    val triggerBuildType: String? = null
)

object IntegratedTerminalEngine {

    fun execute(
        projectRoot: File,
        currentRelDir: String,
        rawCommand: String
    ): TerminalResult {
        val cmd = rawCommand.trim()
        if (cmd.isEmpty()) return TerminalResult("", currentRelDir)

        val parts = cmd.split(Regex("\\s+"))
        val head = parts[0].lowercase()
        val args = parts.drop(1)
        val activeDir = if (currentRelDir.isBlank()) projectRoot else File(projectRoot, currentRelDir)

        return when (head) {
            "help", "?" -> TerminalResult(
                """
                Comandos disponíveis no terminal do projeto:
                  ls [pasta]      Lista arquivos e pastas
                  pwd             Exibe diretório atual
                  cd <pasta>      Navega entre pastas (cd .. para voltar)
                  tree            Exibe árvore completa de arquivos
                  cat <arquivo>   Imprime conteúdo de um arquivo
                  touch <arquivo> Cria um arquivo vazio
                  mkdir <pasta>   Cria uma pasta
                  rm <caminho>    Remove arquivo ou pasta
                  grep <texto>    Busca texto nos arquivos do projeto
                  validate        Executa analisador estático no projeto
                  build debug     Inicia compilação APK Debug
                  build release   Inicia compilação APK Release
                  clear           Limpa o terminal
                """.trimIndent(),
                currentRelDir
            )

            "clear", "cls" -> TerminalResult("", currentRelDir, clearScreen = true)

            "pwd" -> TerminalResult(
                if (currentRelDir.isBlank()) "/" else "/$currentRelDir",
                currentRelDir
            )

            "ls", "ll", "dir" -> {
                val target = if (args.isNotEmpty()) resolveInside(projectRoot, activeDir, args[0]) else activeDir
                if (target == null || !target.exists()) {
                    TerminalResult("ls: diretório não encontrado.", currentRelDir)
                } else {
                    val items = (target.listFiles() ?: emptyArray())
                        .sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
                    val out = if (items.isEmpty()) {
                        "(vazio)"
                    } else {
                        items.joinToString("\n") { f ->
                            if (f.isDirectory) "📁 ${f.name}/" else "📄 ${f.name} (${f.length()} B)"
                        }
                    }
                    TerminalResult(out, currentRelDir)
                }
            }

            "cd" -> {
                val destArg = args.firstOrNull() ?: ""
                if (destArg.isBlank() || destArg == "/" || destArg == "~") {
                    TerminalResult("", "")
                } else {
                    val target = resolveInside(projectRoot, activeDir, destArg)
                    if (target == null || !target.exists() || !target.isDirectory) {
                        TerminalResult("cd: pasta '$destArg' não encontrada.", currentRelDir)
                    } else {
                        val rel = if (target.canonicalFile == projectRoot.canonicalFile) ""
                        else target.canonicalFile.relativeTo(projectRoot.canonicalFile).path.replace(File.separatorChar, '/')
                        TerminalResult("", rel)
                    }
                }
            }

            "tree" -> {
                val sb = StringBuilder()
                buildTree(projectRoot, "", sb)
                TerminalResult(sb.toString().trimEnd(), currentRelDir)
            }

            "cat" -> {
                val arg = args.firstOrNull()
                    ?: return TerminalResult("Uso: cat <arquivo>", currentRelDir)
                val target = resolveInside(projectRoot, activeDir, arg)
                if (target == null || !target.exists() || !target.isFile) {
                    TerminalResult("cat: arquivo '$arg' não encontrado.", currentRelDir)
                } else {
                    TerminalResult(target.readText(), currentRelDir)
                }
            }

            "touch" -> {
                val arg = args.firstOrNull()
                    ?: return TerminalResult("Uso: touch <arquivo>", currentRelDir)
                val target = resolveInside(projectRoot, activeDir, arg)
                    ?: return TerminalResult("Caminho inválido.", currentRelDir)
                target.parentFile?.mkdirs()
                if (!target.exists()) target.createNewFile()
                TerminalResult("Arquivo criado: ${target.name}", currentRelDir)
            }

            "mkdir" -> {
                val arg = args.firstOrNull()
                    ?: return TerminalResult("Uso: mkdir <pasta>", currentRelDir)
                val target = resolveInside(projectRoot, activeDir, arg)
                    ?: return TerminalResult("Caminho inválido.", currentRelDir)
                if (target.mkdirs()) {
                    TerminalResult("Diretório criado: ${target.name}", currentRelDir)
                } else {
                    TerminalResult("Diretório já existe ou não pôde ser criado.", currentRelDir)
                }
            }

            "rm" -> {
                val arg = args.firstOrNull()
                    ?: return TerminalResult("Uso: rm <arquivo_ou_pasta>", currentRelDir)
                val target = resolveInside(projectRoot, activeDir, arg)
                if (target == null || !target.exists() || target.canonicalFile == projectRoot.canonicalFile) {
                    TerminalResult("rm: alvo inválido ou protegido.", currentRelDir)
                } else {
                    target.deleteRecursively()
                    TerminalResult("Removido: $arg", currentRelDir)
                }
            }

            "grep" -> {
                val query = args.joinToString(" ")
                if (query.isBlank()) return TerminalResult("Uso: grep <texto>", currentRelDir)
                val matches = mutableListOf<String>()
                projectRoot.walkTopDown().filter { it.isFile }.forEach { file ->
                    val rel = file.relativeTo(projectRoot).path.replace(File.separatorChar, '/')
                    file.readLines().forEachIndexed { idx, line ->
                        if (line.contains(query, ignoreCase = true)) {
                            matches += "$rel:${idx + 1}: ${line.trim()}"
                        }
                    }
                }
                val out = if (matches.isEmpty()) "Nenhuma ocorrência para '$query'."
                else matches.take(40).joinToString("\n")
                TerminalResult(out, currentRelDir)
            }

            "validate" -> {
                val files = projectRoot.walkTopDown()
                    .filter { it.isFile }
                    .associate { it.relativeTo(projectRoot).path.replace(File.separatorChar, '/') to it.readText() }
                val issues = CodeAnalyzer.analyzeProject(files)
                val out = if (issues.isEmpty()) {
                    "✓ Nenhum erro de sintaxe encontrado (${files.size} arquivos verificados)."
                } else {
                    issues.joinToString("\n") { "[${it.severity}] ${it.filePath}:${it.line} — ${it.message}" }
                }
                TerminalResult(out, currentRelDir)
            }

            "build" -> {
                val mode = args.firstOrNull()?.uppercase() ?: "DEBUG"
                if (mode == "RELEASE" || mode == "DEBUG") {
                    TerminalResult("Disparando compilação $mode...", currentRelDir, triggerBuildType = mode)
                } else {
                    TerminalResult("Uso: build debug | build release", currentRelDir)
                }
            }

            else -> TerminalResult(
                "Comando desconhecido: '$head'. Digite 'help' para ver os comandos suportados.",
                currentRelDir
            )
        }
    }

    private fun resolveInside(root: File, base: File, arg: String): File? {
        return try {
            val candidate = if (arg.startsWith("/")) {
                File(root, arg.trimStart('/'))
            } else {
                File(base, arg)
            }.canonicalFile
            if (candidate.path.startsWith(root.canonicalFile.path)) candidate else null
        } catch (_: Exception) {
            null
        }
    }

    private fun buildTree(dir: File, prefix: String, sb: StringBuilder) {
        val children = (dir.listFiles() ?: emptyArray())
            .sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
        children.forEachIndexed { index, file ->
            val isLast = index == children.lastIndex
            sb.append(prefix).append(if (isLast) "└── " else "├── ").append(file.name)
            if (file.isDirectory) sb.append("/")
            sb.append("\n")
            if (file.isDirectory) {
                buildTree(file, prefix + if (isLast) "    " else "│   ", sb)
            }
        }
    }
}
