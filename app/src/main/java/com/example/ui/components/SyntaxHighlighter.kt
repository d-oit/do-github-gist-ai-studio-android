package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

enum class TokenType {
  COMMENT,
  STRING,
  ANNOTATION,
  KEYWORD,
  TYPE,
  NUMBER,
  FUNCTION,
  KEY_JSON,
  TAG_XML,
  ATTR_XML
}

object SyntaxHighlighter {

  private fun getStyleForType(type: TokenType): SpanStyle {
    return when (type) {
      TokenType.COMMENT ->
        SpanStyle(color = Color(0xFF6A9955), fontStyle = FontStyle.Italic) // Muted Green
      TokenType.STRING -> SpanStyle(color = Color(0xFFCE9178)) // Peach/Orange
      TokenType.ANNOTATION -> SpanStyle(color = Color(0xFFDCDCAA)) // Soft Yellow
      TokenType.KEYWORD ->
        SpanStyle(color = Color(0xFF569CD6), fontWeight = FontWeight.Bold) // Light Blue
      TokenType.TYPE -> SpanStyle(color = Color(0xFF4EC9B0)) // Teal
      TokenType.NUMBER -> SpanStyle(color = Color(0xFFB5CEA8)) // Light Green
      TokenType.FUNCTION -> SpanStyle(color = Color(0xFFDCDCAA)) // Soft Yellow
      TokenType.KEY_JSON ->
        SpanStyle(color = Color(0xFF9CDCFE), fontWeight = FontWeight.Bold) // Cyan/Light Blue
      TokenType.TAG_XML ->
        SpanStyle(color = Color(0xFF569CD6), fontWeight = FontWeight.Bold) // XML tag blue
      TokenType.ATTR_XML -> SpanStyle(color = Color(0xFF9CDCFE)) // XML attribute light blue
    }
  }

  private val kotlinJavaPatterns =
    listOf(
      TokenType.COMMENT to "(?://.*|/\\*[\\s\\S]*?\\*/)",
      TokenType.STRING to
        "(?:\"\"\"[\\s\\S]*?\"\"\"|\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*')",
      TokenType.ANNOTATION to "(?:@\\w+)",
      TokenType.KEYWORD to
        "\\b(?:package|import|class|interface|object|fun|val|var|private|protected|public|internal|override|open|abstract|final|null|true|false|if|else|for|while|do|return|when|try|catch|finally|throw|this|super|enum|void|static|new|instanceof|extends|implements|const|inline|reified|suspend|as|is|in|init|constructor|typealias|companion|get|set)\\b",
      TokenType.TYPE to "\\b[A-Z]\\w*\\b",
      TokenType.NUMBER to "(?:\\b0x[0-9a-fA-F]+\\b|\\b\\d+(?:\\.\\d+)?(?:[fFLl])?\\b)",
      TokenType.FUNCTION to "\\b\\w+(?=\\s*\\()"
    )

  private val jsTsPatterns =
    listOf(
      TokenType.COMMENT to "(?://.*|/\\*[\\s\\S]*?\\*/)",
      TokenType.STRING to
        "(?:`[\\s\\S]*?`|\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*')",
      TokenType.KEYWORD to
        "\\b(?:break|case|catch|class|const|continue|debugger|default|delete|do|else|export|extends|finally|for|function|if|import|in|instanceof|new|return|super|switch|this|throw|try|typeof|var|void|while|with|yield|let|static|enum|await|async|implements|interface|package|private|protected|public|as|from|any|number|string|boolean|unknown|never|type|of)\\b",
      TokenType.TYPE to "\\b[A-Z]\\w*\\b",
      TokenType.NUMBER to "(?:\\b0x[0-9a-fA-F]+\\b|\\b\\d+(?:\\.\\d+)?\\b)",
      TokenType.FUNCTION to "\\b\\w+(?=\\s*\\()"
    )

  private val pythonPatterns =
    listOf(
      TokenType.COMMENT to "(?:#.*)",
      TokenType.STRING to
        "(?:\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*')",
      TokenType.ANNOTATION to "(?:@\\w+)",
      TokenType.KEYWORD to
        "\\b(?:False|None|True|and|as|assert|async|await|break|class|continue|def|del|elif|else|except|finally|for|from|global|if|import|in|is|lambda|nonlocal|not|or|pass|raise|return|try|while|with|yield)\\b",
      TokenType.TYPE to "\\b[A-Z]\\w*\\b",
      TokenType.NUMBER to "(?:\\b\\d+(?:\\.\\d+)?\\b)",
      TokenType.FUNCTION to "\\b\\w+(?=\\s*\\()"
    )

  private val jsonPatterns =
    listOf(
      TokenType.KEY_JSON to "(?:\"[^\"]+\"\\s*(?=:))",
      TokenType.STRING to "(?:\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\")",
      TokenType.NUMBER to "(?:\\b-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b)",
      TokenType.KEYWORD to "\\b(?:true|false|null)\\b"
    )

  private val xmlHtmlPatterns =
    listOf(
      TokenType.COMMENT to "(?:<!--[\\s\\S]*?-->)",
      TokenType.TAG_XML to "(?:</?[a-zA-Z0-9:-]+>?)",
      TokenType.ATTR_XML to "(?:[a-zA-Z0-9:-]+(?=\\s*=))",
      TokenType.STRING to "(?:\"[^\"]*\"|'[^']*')"
    )

  private val sqlPatterns =
    listOf(
      TokenType.COMMENT to "(?:--.*|/\\*[\\s\\S]*?\\*/)",
      TokenType.STRING to "(?:'[^']*'|\"[^\"]*\")",
      TokenType.KEYWORD to
        "(?i)\\b(?:SELECT|FROM|WHERE|AND|OR|INSERT|UPDATE|DELETE|CREATE|DROP|ALTER|TABLE|INDEX|VIEW|JOIN|INNER|LEFT|RIGHT|FULL|ON|GROUP|BY|HAVING|ORDER|LIMIT|OFFSET|VALUES|INTO|SET|PRIMARY|KEY|FOREIGN|REFERENCES|NOT|NULL|UNIQUE|DEFAULT|CHECK|AS|IN|BETWEEN|LIKE|IS|INT|VARCHAR|TEXT|BOOLEAN|DATE|TIME|TIMESTAMP|TRUE|FALSE)\\b",
      TokenType.NUMBER to "(?:\\b\\d+(?:\\.\\d+)?\\b)"
    )

  private val cssPatterns =
    listOf(
      TokenType.COMMENT to "(?:/\\*[\\s\\S]*?\\*/)",
      TokenType.KEYWORD to "(?:@[a-zA-Z-]+|\\b(?:important)\\b)",
      TokenType.TYPE to "(?:[.#][a-zA-Z0-9_-]+)",
      TokenType.STRING to "(?:\"[^\"]*\"|'[^']*')",
      TokenType.NUMBER to "(?:\\b\\d+(?:px|em|rem|%|vh|vw|ms|s)?\\b)"
    )

  private val genericPatterns =
    listOf(
      TokenType.COMMENT to "(?://.*|/\\*[\\s\\S]*?\\*/|#.*)",
      TokenType.STRING to "(?:\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*')",
      TokenType.NUMBER to "(?:\\b\\d+(?:\\.\\d+)?\\b)"
    )

  fun highlight(text: String, filename: String): AnnotatedString {
    val extension = filename.substringAfterLast('.', "").lowercase()
    val patterns =
      when (extension) {
        "kt",
        "kts",
        "java" -> kotlinJavaPatterns
        "js",
        "jsx",
        "ts",
        "tsx",
        "mjs",
        "cjs" -> jsTsPatterns
        "py" -> pythonPatterns
        "json" -> jsonPatterns
        "xml",
        "html",
        "svg",
        "xhtml" -> xmlHtmlPatterns
        "sql" -> sqlPatterns
        "css" -> cssPatterns
        else -> genericPatterns
      }

    val builder = AnnotatedString.Builder()
    val defaultStyle = SpanStyle(color = Color(0xFFD4D4D4)) // Light grey default

    try {
      val combinedPattern = patterns.joinToString("|") { "(${it.second})" }
      val regex = Regex(combinedPattern)

      var lastIndex = 0
      val matches = regex.findAll(text)

      for (match in matches) {
        // Append plain text leading up to this match
        if (match.range.first > lastIndex) {
          builder.withStyle(defaultStyle) { append(text.substring(lastIndex, match.range.first)) }
        }

        // Find which capturing group matched
        var matchedType: TokenType? = null
        for (i in patterns.indices) {
          val groupVal = match.groups[i + 1]
          if (groupVal != null) {
            matchedType = patterns[i].first
            break
          }
        }

        val style = if (matchedType != null) getStyleForType(matchedType) else defaultStyle
        builder.withStyle(style) { append(match.value) }
        lastIndex = match.range.last + 1
      }

      if (lastIndex < text.length) {
        builder.withStyle(defaultStyle) { append(text.substring(lastIndex)) }
      }
    } catch (e: Exception) {
      // Safe fallback to raw unhighlighted text if compilation/matching fails
      builder.withStyle(defaultStyle) { append(text) }
    }

    return builder.toAnnotatedString()
  }
}
