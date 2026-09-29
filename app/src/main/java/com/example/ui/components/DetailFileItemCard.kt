package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GistFileEntity

@Composable
fun DetailFileItemCard(file: GistFileEntity, modifier: Modifier = Modifier) {
  val isMarkdown =
    file.filename.endsWith(".md", ignoreCase = true) ||
      file.filename.endsWith(".markdown", ignoreCase = true)

  var previewMode by remember { mutableStateOf(if (isMarkdown) "markdown" else "raw") }
  val clipboardManager = LocalClipboardManager.current
  var isCopied by remember { mutableStateOf(false) }

  if (isCopied) {
    LaunchedEffect(Unit) {
      kotlinx.coroutines.delay(2000)
      isCopied = false
    }
  }

  Card(
    modifier = modifier.fillMaxWidth().testTag("detail_file_card_${file.filename}"),
    border = borderButtonStroke(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = file.filename,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${file.language ?: "Plain Text"} • ${file.size} bytes",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (isMarkdown) {
            TextButton(
              onClick = { previewMode = "raw" },
              modifier = Modifier.height(36.dp).testTag("file_mode_raw_${file.filename}")
            ) {
              Text(
                text = "Raw",
                fontSize = 12.sp,
                fontWeight = if (previewMode == "raw") FontWeight.Bold else FontWeight.Normal,
                color =
                  if (previewMode == "raw") MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            TextButton(
              onClick = { previewMode = "markdown" },
              modifier = Modifier.height(36.dp).testTag("file_mode_markdown_${file.filename}")
            ) {
              Text(
                text = "Markdown",
                fontSize = 12.sp,
                fontWeight = if (previewMode == "markdown") FontWeight.Bold else FontWeight.Normal,
                color =
                  if (previewMode == "markdown") MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          IconButton(
            onClick = {
              clipboardManager.setText(AnnotatedString(file.content))
              isCopied = true
            },
            modifier = Modifier.size(36.dp).testTag("copy_file_button_${file.filename}")
          ) {
            Icon(
              imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
              contentDescription = "Copy to Clipboard",
              tint =
                if (isCopied) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (isMarkdown && previewMode == "markdown") {
        Surface(
          modifier =
            Modifier.fillMaxWidth()
              .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
              .padding(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
          shape = RoundedCornerShape(8.dp)
        ) {
          MarkdownText(text = file.content)
        }
      } else {
        Surface(
          modifier =
            Modifier.fillMaxWidth()
              .heightIn(min = 100.dp, max = 500.dp)
              .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFF1E1E1E)
        ) {
          LazyColumn(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            item {
              val highlightedText =
                remember(file.content, file.filename) {
                  SyntaxHighlighter.highlight(
                    text = file.content.ifEmpty { "// Empty content" },
                    filename = file.filename
                  )
                }
              Text(
                text = highlightedText,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp
              )
            }
          }
        }
      }
    }
  }
}
