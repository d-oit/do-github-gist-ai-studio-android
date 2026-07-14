package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxWidth().padding(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    val lines = text.split("\n")
    var inCodeBlock = false
    var codeBlockContent = ""
    var codeBlockLang = ""

    for (line in lines) {
      val trimmed = line.trim()
      if (trimmed.startsWith("```")) {
        if (inCodeBlock) {
          val currentLang = codeBlockLang
          val currentContent = codeBlockContent
          Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF2D2D2D),
            shape = RoundedCornerShape(6.dp)
          ) {
            val highlightedText =
              remember(currentContent, currentLang) {
                SyntaxHighlighter.highlight(
                  text = currentContent.trimEnd(),
                  filename = "code.$currentLang"
                )
              }
            Text(
              text = highlightedText,
              fontFamily = FontFamily.Monospace,
              fontSize = 12.sp,
              modifier = Modifier.padding(8.dp)
            )
          }
          codeBlockContent = ""
          codeBlockLang = ""
          inCodeBlock = false
        } else {
          codeBlockLang = trimmed.substring(3).trim()
          inCodeBlock = true
        }
      } else if (inCodeBlock) {
        codeBlockContent += line + "\n"
      } else if (trimmed.startsWith("#")) {
        val level = trimmed.takeWhile { it == '#' }.length
        val headerText = trimmed.drop(level).trim()
        val size =
          when (level) {
            1 -> 20.sp
            2 -> 18.sp
            3 -> 16.sp
            else -> 14.sp
          }
        Text(
          text = headerText,
          fontSize = size,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
        val bulletText = trimmed.substring(2).trim()
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
          Text(
            text = "• ",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = parseMarkdownInline(bulletText),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      } else {
        if (trimmed.isNotEmpty()) {
          Text(
            text = parseMarkdownInline(line),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}

fun parseMarkdownInline(text: String): androidx.compose.ui.text.AnnotatedString {
  return androidx.compose.ui.text.buildAnnotatedString {
    var i = 0
    while (i < text.length) {
      if (text.startsWith("**", i)) {
        val end = text.indexOf("**", i + 2)
        if (end != -1) {
          withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(text.substring(i + 2, end))
          }
          i = end + 2
        } else {
          append("**")
          i += 2
        }
      } else if (text.startsWith("*", i)) {
        val end = text.indexOf("*", i + 1)
        if (end != -1) {
          withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
            append(text.substring(i + 1, end))
          }
          i = end + 1
        } else {
          append("*")
          i += 1
        }
      } else if (text.startsWith("`", i)) {
        val end = text.indexOf("`", i + 1)
        if (end != -1) {
          withStyle(
            style =
              SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = Color.LightGray.copy(alpha = 0.2f),
                color = Color(0xFFC7254E)
              )
          ) {
            append(text.substring(i + 1, end))
          }
          i = end + 1
        } else {
          append("`")
          i += 1
        }
      } else {
        append(text[i].toString())
        i++
      }
    }
  }
}
