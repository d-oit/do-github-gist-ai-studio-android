package com.example.data.repository

object LanguageDetector {
  fun detectLanguage(filename: String): String {
    val lower = filename.lowercase()
    return when {
      lower.endsWith(".kt") || lower.endsWith(".kts") -> "Kotlin"
      lower.endsWith(".java") -> "Java"
      lower.endsWith(".py") -> "Python"
      lower.endsWith(".js") || lower.endsWith(".jsx") || lower.endsWith(".mjs") -> "JavaScript"
      lower.endsWith(".ts") || lower.endsWith(".tsx") -> "TypeScript"
      lower.endsWith(".json") -> "JSON"
      lower.endsWith(".xml") -> "XML"
      lower.endsWith(".html") -> "HTML"
      lower.endsWith(".css") || lower.endsWith(".scss") -> "CSS"
      lower.endsWith(".md") || lower.endsWith(".markdown") -> "Markdown"
      lower.endsWith(".yml") || lower.endsWith(".yaml") -> "YAML"
      lower.endsWith(".toml") -> "TOML"
      lower.endsWith(".rs") -> "Rust"
      lower.endsWith(".go") -> "Go"
      lower.endsWith(".swift") -> "Swift"
      lower.endsWith(".rb") -> "Ruby"
      lower.endsWith(".sql") -> "SQL"
      lower.endsWith(".sh") || lower.endsWith(".bash") || lower.endsWith(".zsh") -> "Shell"
      lower == "makefile" -> "Makefile"
      lower == "dockerfile" -> "Dockerfile"
      else -> "Text"
    }
  }
}
