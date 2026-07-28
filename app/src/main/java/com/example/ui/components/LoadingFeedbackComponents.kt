package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ActivePurple
import com.example.ui.theme.ActivePurpleContainer
import com.example.ui.theme.DarkPurpleText

/** Unified sealed interface representing data fetching state. */
sealed interface DataLoadingState<out T> {
  data object Idle : DataLoadingState<Nothing>

  data class Loading(
    val message: String = "Fetching data...",
    val progress: Float? = null,
    val isCancelable: Boolean = false,
    val onCancel: (() -> Unit)? = null
  ) : DataLoadingState<Nothing>

  data class Refreshing(val message: String = "Syncing updates...") : DataLoadingState<Nothing>

  data class Success<T>(val data: T) : DataLoadingState<T>

  data class Error(val message: String, val onRetry: (() -> Unit)? = null) :
    DataLoadingState<Nothing>

  data class Empty(
    val title: String = "No Data Found",
    val description: String = "There are no items to display at this time.",
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
  ) : DataLoadingState<Nothing>
}

/** Floating feedback bar displayed at top of screens during background data fetching or syncing. */
@Composable
fun LoadingFeedbackBar(
  isLoading: Boolean,
  message: String,
  modifier: Modifier = Modifier,
  onDismiss: (() -> Unit)? = null
) {
  AnimatedVisibility(
    visible = isLoading,
    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
    modifier = modifier
  ) {
    Surface(
      color = ActivePurpleContainer,
      contentColor = DarkPurpleText,
      tonalElevation = 6.dp,
      shadowElevation = 4.dp,
      modifier = Modifier.fillMaxWidth().testTag("loading_feedback_bar")
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp).testTag("loading_feedback_bar_spinner"),
              strokeWidth = 2.dp,
              color = ActivePurple
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = message,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = DarkPurpleText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.testTag("loading_feedback_bar_message")
            )
          }

          if (onDismiss != null) {
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(28.dp).testTag("loading_feedback_bar_dismiss_btn")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss loading feedback",
                tint = DarkPurpleText,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
        LinearProgressIndicator(
          modifier = Modifier.fillMaxWidth().height(2.dp),
          color = ActivePurple,
          trackColor = ActivePurpleContainer
        )
      }
    }
  }
}

/**
 * Modal elevated card overlay for modal or blocking data fetching operations (e.g. AI analysis,
 * profile fetch, fork).
 */
@Composable
fun LoadingFeedbackOverlay(
  isLoading: Boolean,
  title: String = "Processing Request",
  message: String = "Please wait while data is retrieved...",
  progress: Float? = null,
  isCancelable: Boolean = false,
  onCancel: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  if (isLoading) {
    Box(
      modifier =
        modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.45f))
          .padding(24.dp)
          .testTag("loading_feedback_overlay"),
      contentAlignment = Alignment.Center
    ) {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(0.9f).testTag("loading_feedback_overlay_card")
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier =
              Modifier.size(64.dp)
                .background(
                  color = ActivePurpleContainer.copy(alpha = 0.5f),
                  shape = RoundedCornerShape(32.dp)
                ),
            contentAlignment = Alignment.Center
          ) {
            if (progress != null) {
              CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(40.dp).testTag("loading_feedback_overlay_progress"),
                color = ActivePurple,
                trackColor = ActivePurpleContainer
              )
            } else {
              CircularProgressIndicator(
                modifier = Modifier.size(40.dp).testTag("loading_feedback_overlay_spinner"),
                strokeWidth = 3.dp,
                color = ActivePurple
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("loading_feedback_overlay_title")
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = message,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("loading_feedback_overlay_message")
          )

          if (isCancelable && onCancel != null) {
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(
              onClick = onCancel,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(36.dp).testTag("loading_feedback_overlay_cancel_btn")
            ) {
              Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

/** Skeleton placeholder item simulating loading Gist card layout. */
@Composable
fun SkeletonLoadingItem(modifier: Modifier = Modifier) {
  Card(
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
      ),
    shape = RoundedCornerShape(16.dp),
    modifier = modifier.fillMaxWidth().testTag("skeleton_loading_item")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier =
            Modifier.width(140.dp)
              .height(18.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
        )
        Box(
          modifier =
            Modifier.size(24.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Box(
        modifier =
          Modifier.fillMaxWidth(0.85f)
            .height(14.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
      )
      Spacer(modifier = Modifier.height(6.dp))
      Box(
        modifier =
          Modifier.fillMaxWidth(0.5f)
            .height(14.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
      )
      Spacer(modifier = Modifier.height(14.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
          modifier =
            Modifier.width(60.dp)
              .height(20.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(ActivePurpleContainer.copy(alpha = 0.4f))
        )
        Box(
          modifier =
            Modifier.width(80.dp)
              .height(20.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
        )
      }
    }
  }
}

/** Skeleton placeholder list to display while initial data is fetching. */
@Composable
fun SkeletonLoadingList(count: Int = 3, modifier: Modifier = Modifier) {
  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = modifier.testTag("skeleton_loading_list")
  ) {
    repeat(count) { SkeletonLoadingItem() }
  }
}

/**
 * Universal state box wrapper that handles Loading, Error, Empty, and Content states seamlessly.
 */
@Composable
fun <T> LoadingFeedbackBox(
  state: DataLoadingState<T>,
  onRetry: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
  loadingContent: @Composable () -> Unit = { SkeletonLoadingList() },
  emptyContent: @Composable (DataLoadingState.Empty) -> Unit = { emptyState ->
    DefaultEmptyStateView(emptyState)
  },
  errorContent: @Composable (DataLoadingState.Error) -> Unit = { errorState ->
    DefaultErrorStateView(errorState, onRetry)
  },
  content: @Composable (T) -> Unit
) {
  Box(modifier = modifier.fillMaxSize()) {
    when (state) {
      is DataLoadingState.Idle -> {
        // Render empty or idle placeholder
      }
      is DataLoadingState.Loading -> {
        loadingContent()
      }
      is DataLoadingState.Refreshing -> {
        // Showing content with top refresh indicator or passing through content
      }
      is DataLoadingState.Success -> {
        content(state.data)
      }
      is DataLoadingState.Error -> {
        errorContent(state)
      }
      is DataLoadingState.Empty -> {
        emptyContent(state)
      }
    }
  }
}

@Composable
private fun DefaultEmptyStateView(
  emptyState: DataLoadingState.Empty,
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier.fillMaxWidth().padding(24.dp).testTag("loading_feedback_empty_view")
  ) {
    Box(
      modifier =
        Modifier.size(72.dp)
          .background(
            color = ActivePurpleContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(36.dp)
          ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.CloudSync,
        contentDescription = "Empty Data",
        tint = ActivePurple,
        modifier = Modifier.size(36.dp)
      )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = emptyState.title,
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = emptyState.description,
      fontSize = 13.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 16.dp)
    )
    if (emptyState.actionLabel != null && emptyState.onAction != null) {
      Spacer(modifier = Modifier.height(20.dp))
      Button(
        onClick = emptyState.onAction,
        colors = ButtonDefaults.buttonColors(containerColor = ActivePurple),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.testTag("loading_feedback_empty_action_btn")
      ) {
        Text(emptyState.actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun DefaultErrorStateView(
  errorState: DataLoadingState.Error,
  onRetry: (() -> Unit)?,
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier.fillMaxWidth().padding(24.dp).testTag("loading_feedback_error_view")
  ) {
    Box(
      modifier =
        Modifier.size(64.dp)
          .background(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(32.dp)
          ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.ErrorOutline,
        contentDescription = "Error",
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(32.dp)
      )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = "Data Fetching Error",
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.error,
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = errorState.message,
      fontSize = 13.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 16.dp)
    )
    val retryAction = errorState.onRetry ?: onRetry
    if (retryAction != null) {
      Spacer(modifier = Modifier.height(20.dp))
      Button(
        onClick = retryAction,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.testTag("loading_feedback_retry_btn")
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = "Retry",
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Retry Fetch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
