package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable empty state UI component displayed when there are no Gists synchronized or stored
 * locally. Displays a clear title, explanatory message, primary call-to-action button, and optional
 * draft creation button.
 */
@Composable
fun GistListEmptyState(
  onFetchClick: () -> Unit,
  modifier: Modifier = Modifier,
  title: String = "Your Gist Library is Empty",
  message: String =
    "Securely synchronize your GitHub snippets to work offline, or start drafting local code blocks immediately. Your drafts persist locally and can be synced anytime.",
  isRefreshing: Boolean = false,
  onCreateDraftClick: (() -> Unit)? = null
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier.fillMaxWidth().padding(24.dp).testTag("gist_list_empty_state")
  ) {
    // Friendly, high-fidelity decorative illustration container
    Box(
      modifier =
        Modifier.size(100.dp)
          .background(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(32.dp)
          ),
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier =
          Modifier.size(68.dp)
            .background(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = RoundedCornerShape(22.dp)
            ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.CloudSync,
          contentDescription = "Cloud Synchronize",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(36.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = title,
      fontSize = 19.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
      modifier = Modifier.testTag("empty_state_title")
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = message,
      fontSize = 13.sp,
      lineHeight = 18.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 16.dp).testTag("empty_state_message")
    )

    Spacer(modifier = Modifier.height(28.dp))

    // Primary Call-To-Action Button: Fetch from GitHub
    Button(
      onClick = onFetchClick,
      enabled = !isRefreshing,
      colors =
        ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth().height(48.dp).testTag("fetch_from_github_btn")
    ) {
      if (isRefreshing) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp).testTag("empty_state_progress_indicator"),
          strokeWidth = 2.dp,
          color = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text("Fetching Gists...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      } else {
        Icon(
          imageVector = Icons.Default.CloudSync,
          contentDescription = "Download icon",
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Fetch from GitHub", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      }
    }

    if (onCreateDraftClick != null) {
      Spacer(modifier = Modifier.height(12.dp))
      OutlinedButton(
        onClick = onCreateDraftClick,
        shape = RoundedCornerShape(16.dp),
        border =
          androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
          ),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("empty_state_create_draft_btn")
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "New draft icon",
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Create Local Draft", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
      }
    } else {

      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "Or tap the '+' button in the bottom right to create a new draft offline.",
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag("empty_state_helper_text")
      )
    }
  }
}
