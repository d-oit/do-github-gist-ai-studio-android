package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DraftEditorFileCard(
  index: Int,
  filename: String,
  content: String,
  totalFiles: Int,
  onFilenameChange: (String) -> Unit,
  onContentChange: (String) -> Unit,
  onRemove: () -> Unit
) {
  val textFieldColors =
    OutlinedTextFieldDefaults.colors(
      focusedTextColor = MaterialTheme.colorScheme.onSurface,
      unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
      focusedLabelColor = MaterialTheme.colorScheme.primary,
      unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
      focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
      unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
      focusedBorderColor = MaterialTheme.colorScheme.primary,
      unfocusedBorderColor = MaterialTheme.colorScheme.outline,
      focusedContainerColor = MaterialTheme.colorScheme.surface,
      unfocusedContainerColor = MaterialTheme.colorScheme.surface
    )

  Card(
    modifier = Modifier.fillMaxWidth(),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "File #${index + 1}",
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.primary
        )
        if (totalFiles > 1) {
          IconButton(onClick = onRemove, modifier = Modifier.testTag("remove_file_$index")) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Remove File",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(4.dp))
      OutlinedTextField(
        value = filename,
        onValueChange = onFilenameChange,
        label = { Text("Filename (e.g. script.kt)") },
        modifier = Modifier.fillMaxWidth().testTag("editor_filename_$index"),
        singleLine = true,
        colors = textFieldColors,
        shape = RoundedCornerShape(8.dp)
      )
      Spacer(modifier = Modifier.height(8.dp))

      GitHubMarkdownEditor(
        value = content,
        onValueChange = onContentChange,
        placeholder = "Leave a comment or Gist file content...",
        testTagPrefix = "editor_content_$index"
      )
    }
  }
}
