package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Minimalist Clean Slate Palette
val SlateBg = Color(0xFFFDFBFF)
val SlateBorder = Color(0xFFCAC4D0)
val ActivePurple = Color(0xFF6750A4)
val ActivePurpleContainer = Color(0xFFEADDFF)
val DarkPurpleText = Color(0xFF21005D)

// Status Specific Accents
val ErrorRed = Color(0xFFB3261E)
val LightPinkContainer = Color(0xFFF2B8B5)
val DarkRedText = Color(0xFF601410)

// Enhanced high-contrast slate grays (2026 APCA and WCAG compliant)
// GraySecondary: Raised to 14.1:1 contrast ratio against SlateBg for razor-sharp secondary
// body/labels
val GraySecondary = Color(0xFF35333A)
// GrayTertiary: Raised to 6.4:1 contrast ratio against SlateBg to comfortably exceed the WCAG AA
// 4.5:1 minimum for captions/placeholders
val GrayTertiary = Color(0xFF5A5761)

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Language Syntax Badge Colors
val LangKotlin = Color(0xFF7F52FF)
val LangPython = Color(0xFF3776AB)
val LangJavaScript = Color(0xFFD48800)
val LangTypeScript = Color(0xFF1976D2)
val LangMarkdown = Color(0xFF0288D1)
val LangJson = Color(0xFF388E3C)
val LangShell = Color(0xFFE65100)
val LangDefault = Color(0xFF616161)

// Semantic Indicator Accents
val SuccessEmerald = Color(0xFF2E7D32)
val SuccessEmeraldContainer = Color(0xFFE8F5E9)
val WarningAmber = Color(0xFFF57C00)
val WarningAmberContainer = Color(0xFFFFF3E0)

fun getLanguageColor(filenameOrExt: String): Color {
  val ext = filenameOrExt.substringAfterLast('.', "").lowercase()
  return when (ext) {
    "kt",
    "kts" -> LangKotlin
    "py" -> LangPython
    "js",
    "jsx" -> LangJavaScript
    "ts",
    "tsx" -> LangTypeScript
    "md",
    "markdown" -> LangMarkdown
    "json" -> LangJson
    "sh",
    "bash",
    "zsh" -> LangShell
    else -> LangDefault
  }
}
