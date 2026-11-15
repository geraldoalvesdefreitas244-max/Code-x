package com.example.data.ai

import org.json.JSONArray
import org.json.JSONObject

data class AiGeneratedFile(
    val relativePath: String,
    val content: String
)

object AiFilePatchParser {

    private val customTagRegex = Regex(
        pattern = """<<<FILE:\s*([^\r\n>]+?)\s*>>>\s*\r?\n([\s\S]*?)\r?\n<<<END_FILE>>>""",
        options = setOf(RegexOption.MULTILINE)
    )

    private val markdownHeadingFileRegex = Regex(
        pattern = """(?:\*\*|###?\s+|`)([a-zA-Z0-9_./\-]+\.(?:kt|java|xml|html|css|js|json|gradle|kts|md|txt))`?\*?\*?\s*\r?\n```[a-zA-Z0-9_-]*\r?\n([\s\S]*?)\r?\n```""",
        options = setOf(RegexOption.MULTILINE)
    )

    fun extractFiles(responseText: String): List<AiGeneratedFile> {
        val result = linkedMapOf<String, AiGeneratedFile>()
        for (match in customTagRegex.findAll(responseText)) {
            val path = match.groupValues[1].trim().trimStart('/')
            val code = match.groupValues[2]
            if (path.isNotBlank()) {
                result[path] = AiGeneratedFile(path, code)
            }
        }
        if (result.isEmpty()) {
            for (match in markdownHeadingFileRegex.findAll(responseText)) {
                val path = match.groupValues[1].trim().trimStart('/')
                val code = match.groupValues[2]
                if (path.isNotBlank()) {
                    result[path] = AiGeneratedFile(path, code)
                }
            }
        }
        return result.values.toList()
    }

    fun toJson(files: List<AiGeneratedFile>): String {
        val arr = JSONArray()
        for (file in files) {
            arr.put(JSONObject().apply {
                put("path", file.relativePath)
                put("content", file.content)
            })
        }
        return arr.toString()
    }

    fun fromJson(json: String): List<AiGeneratedFile> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { idx ->
                val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
                val path = obj.optString("path")
                val content = obj.optString("content")
                if (path.isNotBlank()) AiGeneratedFile(path, content) else null
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
