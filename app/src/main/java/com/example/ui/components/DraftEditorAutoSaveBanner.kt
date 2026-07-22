package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DraftEditorAutoSaveBanner(
  showRestorePrompt: Boolean,
  hasAutoSavedDraft: Boolean,
  onRestore: () -> Unit,
  onDiscard: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (!showRestorePrompt || !hasAutoSavedDraft) return

  Card(
    modifier = modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("autosave_banner"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Auto-saved draft found",
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Would you like to restore your unsaved edits?",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
        )
      }
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextButton(onClick = onDiscard, modifier = Modifier.testTag("autosave_discard")) {
          Text("Discard", fontSize = 13.sp)
        }
        Button(
          onClick = onRestore,
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier.testTag("autosave_restore")
        ) {
          Text("Restore", fontSize = 13.sp)
        }
      }
    }
  }
}
