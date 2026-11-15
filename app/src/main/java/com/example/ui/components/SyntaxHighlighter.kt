package com.example.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.ui.theme.SyntaxAnnotation
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.SyntaxTag
import com.example.ui.theme.SyntaxType

class CodeSyntaxVisualTransformation(
    private val fileExtension: String
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            highlightCode(text.text, fileExtension),
            OffsetMapping.Identity
        )
    }

    companion object {
        private val kotlinJavaKeywords = Regex(
            """\b(package|import|class|interface|object|data|sealed|enum|fun|val|var|private|public|protected|internal|override|open|abstract|suspend|return|if|else|when|for|while|do|try|catch|finally|throw|new|void|static|final|extends|implements|true|false|null|this|super|by|lazy|in|is|as)\b"""
        )
        private val jsKeywords = Regex(
            """\b(const|let|var|function|async|await|return|if|else|switch|case|for|while|try|catch|import|export|from|default|class|new|true|false|null|undefined|document|window|console)\b"""
        )
        private val stringRegex = Regex("""("([^"\\]|\\.)*"|'([^'\\]|\\.)*')""")
        private val commentRegex = Regex("""(//[^\n]*|/\*[\s\S]*?\*/|<!--[\s\S]*?-->)""")
        private val numberRegex = Regex("""\b\d+(\.\d+)?[fFLl]?\b""")
        private val annotationRegex = Regex("""@[A-Za-z_][A-Za-z0-9_]*""")
        private val typeRegex = Regex("""\b[A-Z][A-Za-z0-9_]*\b""")
        private val xmlTagRegex = Regex("""</?[a-zA-Z0-9:_-]+|/?>""")

        fun highlightCode(raw: String, ext: String): AnnotatedString {
            val builder = AnnotatedString.Builder(raw)
            val lower = ext.lowercase()

            when (lower) {
                "xml", "html", "htm" -> {
                    xmlTagRegex.findAll(raw).forEach {
                        builder.addStyle(SpanStyle(color = SyntaxTag, fontWeight = FontWeight.Medium), it.range.first, it.range.last + 1)
                    }
                    stringRegex.findAll(raw).forEach {
                        builder.addStyle(SpanStyle(color = SyntaxString), it.range.first, it.range.last + 1)
                    }
                    commentRegex.findAll(raw).forEach {
                        builder.addStyle(SpanStyle(color = SyntaxComment), it.range.first, it.range.last + 1)
                    }
                }
                else -> {
                    typeRegex.findAll(raw).forEach {
                        builder.addStyle(SpanStyle(color = SyntaxType), it.range.first, it.range.last + 1)
                    }
                    numberRegex.findAll(raw).forEach {
                        builder.addStyle(SpanStyle(color = SyntaxNumber), it.range.first, it.range.last + 1)
                    }
                    val kwRegex = if (lower == "js" || lower == "css") jsKeywords else kotlinJavaKeywords
                    kwRegex.findAll(raw).forEach {
                        builder.addStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.SemiBold), it.range.first, it.range.last + 1)
                    }
                    annotationRegex.findAll(raw).forEach {
                        builder.addStyle(SpanStyle(color = SyntaxAnnotation), it.range.first, it.range.last + 1)
                    }
                    stringRegex.findAll(raw).forEach {
                        builder.addStyle(SpanStyle(color = SyntaxString), it.range.first, it.range.last + 1)
                    }
                    commentRegex.findAll(raw).forEach {
                        builder.addStyle(SpanStyle(color = SyntaxComment), it.range.first, it.range.last + 1)
                    }
                }
            }
            return builder.toAnnotatedString()
        }
    }
}
