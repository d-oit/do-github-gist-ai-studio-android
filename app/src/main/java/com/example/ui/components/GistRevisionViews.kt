package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ActivePurple

@Composable
fun RevisionHistoryListView(
  historyList: List<com.example.data.remote.model.GistHistoryResponse>,
  defaultOwnerLogin: String,
  onSelectRevision: (String) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    items(historyList.size) { idx ->
      val rev = historyList[idx]
      val formattedRevDate = formatGistDate(rev.committedAt ?: "")
      val change = rev.changeStatus

      Card(
        modifier = Modifier.fillMaxWidth().clickable { rev.version?.let { onSelectRevision(it) } },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatarPlaceholder(login = rev.user?.login ?: defaultOwnerLogin, size = 32.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "@${rev.user?.login ?: defaultOwnerLogin}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "Committed on $formattedRevDate",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Version: ${rev.version?.take(7) ?: "unknown"}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
              )
            }
          }

          if (change != null) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "+${change.additions ?: 0}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3FB950)
              )
              Text(
                text = "-${change.deletions ?: 0}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF85149)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun DetailedRevisionChangesView(
  selectedRevisionSha: String,
  diffViewMode: String,
  onDiffViewModeChange: (String) -> Unit,
  isLoadingRevisionContent: Boolean,
  revisionContentError: String?,
  filesToCompare: List<Triple<String, String, String>>,
  onBack: () -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth().clickable { onBack() }.padding(vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Back",
        tint = ActivePurple,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "Back to Revisions",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = ActivePurple
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Viewing Revision ${selectedRevisionSha.take(7)}",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Comparing files changes",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Unified vs Split Selector Toggle
      Row(
        modifier =
          Modifier.clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
      ) {
        listOf("Unified", "Split").forEach { option ->
          val isSelected = (option.lowercase() == diffViewMode)
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) ActivePurple else Color.Transparent)
                .clickable { onDiffViewModeChange(option.lowercase()) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = option,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (isLoadingRevisionContent) {
      Box(modifier = Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ActivePurple)
      }
    } else if (revisionContentError != null) {
      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
          text = revisionContentError,
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(16.dp)
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.heightIn(max = 450.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        items(filesToCompare.size) { fileIdx ->
          val (filename, oldContent, newContent) = filesToCompare[fileIdx]
          Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = filename,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
              )

              if (diffViewMode == "split") {
                SplitDiffView(oldContent = oldContent, newContent = newContent)
              } else {
                UnifiedDiffView(oldContent = oldContent, newContent = newContent)
              }
            }
          }
        }
      }
    }
  }
}
