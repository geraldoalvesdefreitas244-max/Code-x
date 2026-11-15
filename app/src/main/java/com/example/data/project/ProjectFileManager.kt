package com.example.data.project

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ProjectFileNode(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val extension: String
)

class ProjectFileManager(private val context: Context) {

    fun projectsRootDir(): File {
        return File(context.filesDir, "codex_projects").apply { if (!exists()) mkdirs() }
    }

    fun apksRootDir(): File {
        return File(context.filesDir, "codex_apks").apply { if (!exists()) mkdirs() }
    }

    fun projectDir(projectId: String): File {
        return File(projectsRootDir(), projectId)
    }

    fun listDirectory(projectRoot: File, relativeDir: String = ""): List<ProjectFileNode> {
        val dir = if (relativeDir.isBlank()) projectRoot else File(projectRoot, relativeDir)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        val children = dir.listFiles() ?: return emptyList()
        return children
            .sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
            .map { file ->
                val rel = file.relativeTo(projectRoot).path.replace(File.separatorChar, '/')
                ProjectFileNode(
                    name = file.name,
                    relativePath = rel,
                    isDirectory = file.isDirectory,
                    sizeBytes = if (file.isDirectory) 0L else file.length(),
                    lastModified = file.lastModified(),
                    extension = file.extension.lowercase()
                )
            }
    }

    fun listAllFilesFlat(projectRoot: File): List<ProjectFileNode> {
        if (!projectRoot.exists()) return emptyList()
        return projectRoot.walkTopDown()
            .filter { it.isFile }
            .map { file ->
                val rel = file.relativeTo(projectRoot).path.replace(File.separatorChar, '/')
                ProjectFileNode(
                    name = file.name,
                    relativePath = rel,
                    isDirectory = false,
                    sizeBytes = file.length(),
                    lastModified = file.lastModified(),
                    extension = file.extension.lowercase()
                )
            }
            .sortedBy { it.relativePath }
            .toList()
    }

    fun readFile(projectRoot: File, relativePath: String): String {
        val target = resolveSafe(projectRoot, relativePath)
        if (!target.exists() || !target.isFile) return ""
        return target.readText()
    }

    fun writeFile(projectRoot: File, relativePath: String, content: String) {
        val target = resolveSafe(projectRoot, relativePath)
        target.parentFile?.mkdirs()
        target.writeText(content)
    }

    fun createFile(projectRoot: File, relativePath: String, initialContent: String = ""): Boolean {
        val target = resolveSafe(projectRoot, relativePath)
        if (target.exists()) return false
        target.parentFile?.mkdirs()
        target.writeText(initialContent)
        return true
    }

    fun createFolder(projectRoot: File, relativePath: String): Boolean {
        val target = resolveSafe(projectRoot, relativePath)
        if (target.exists()) return false
        return target.mkdirs()
    }

    fun renameEntry(projectRoot: File, oldRelativePath: String, newName: String): Boolean {
        val cleanName = newName.trim().replace("/", "").replace("\\", "")
        if (cleanName.isBlank()) return false
        val source = resolveSafe(projectRoot, oldRelativePath)
        if (!source.exists()) return false
        val target = File(source.parentFile, cleanName)
        if (target.exists()) return false
        return source.renameTo(target)
    }

    fun deleteEntry(projectRoot: File, relativePath: String): Boolean {
        val target = resolveSafe(projectRoot, relativePath)
        if (!target.exists()) return false
        return target.deleteRecursively()
    }

    fun deleteProjectDirectory(projectRoot: File) {
        if (projectRoot.exists()) {
            projectRoot.deleteRecursively()
        }
    }

    fun duplicateDirectory(sourceDir: File, targetDir: File) {
        if (!sourceDir.exists()) return
        sourceDir.copyRecursively(targetDir, overwrite = true)
    }

    fun calculateFolderSizeBytes(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun zipProject(projectRoot: File, outputZipFile: File): File {
        outputZipFile.parentFile?.mkdirs()
        ZipOutputStream(FileOutputStream(outputZipFile)).use { zos ->
            val files = projectRoot.walkTopDown().filter { it.isFile }.toList()
            val buffer = ByteArray(8192)
            for (file in files) {
                val entryName = file.relativeTo(projectRoot).path.replace(File.separatorChar, '/')
                if (entryName.startsWith("build/") || entryName.startsWith(".gradle/")) continue
                zos.putNextEntry(ZipEntry(entryName))
                FileInputStream(file).use { fis ->
                    var len: Int
                    while (fis.read(buffer).also { len = it } > 0) {
                        zos.write(buffer, 0, len)
                    }
                }
                zos.closeEntry()
            }
        }
        return outputZipFile
    }

    fun exportProjectToUri(projectRoot: File, destinationUri: Uri): Result<Long> {
        return runCatching {
            val tempZip = File(context.cacheDir, "export_${System.currentTimeMillis()}.zip")
            zipProject(projectRoot, tempZip)
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                tempZip.inputStream().use { input ->
                    input.copyTo(out)
                }
            } ?: error("Não foi possível abrir o destino de exportação.")
            val bytes = tempZip.length()
            tempZip.delete()
            bytes
        }
    }

    fun importFileFromUri(projectRoot: File, targetDirRelative: String, uri: Uri, fallbackName: String): Result<String> {
        return runCatching {
            val name = queryDisplayName(uri) ?: fallbackName
            val relPath = if (targetDirRelative.isBlank()) name else "$targetDirRelative/$name"
            val dest = resolveSafe(projectRoot, relPath)
            dest.parentFile?.mkdirs()
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { out ->
                    input.copyTo(out)
                }
            } ?: error("Falha ao ler arquivo selecionado.")
            relPath
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun resolveSafe(root: File, relativePath: String): File {
        val clean = relativePath.trimStart('/', '\\')
        val candidate = File(root, clean).canonicalFile
        val rootCanonical = root.canonicalFile
        require(candidate.path.startsWith(rootCanonical.path)) {
            "Caminho fora do diretório do projeto: $relativePath"
        }
        return candidate
    }
}
