package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.remote.model.GistCommentResponse

@Composable
fun GistCommentsView(
  comments: List<GistCommentResponse>,
  isLoading: Boolean,
  errorMessage: String?,
  isPosting: Boolean,
  currentUserLogin: String,
  onPostComment: (String) -> Unit,
  onDeleteComment: (Long) -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier
) {
  var commentInputText by remember { mutableStateOf("") }

  Column(
    modifier = modifier.fillMaxWidth().testTag("gist_comments_view"),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Comment Composer Card
    Card(
      modifier = Modifier.fillMaxWidth().testTag("comment_composer_card"),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = borderButtonStroke()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Add a Comment",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = commentInputText,
          onValueChange = { commentInputText = it },
          placeholder = { Text(stringResource(R.string.comments_input_placeholder)) },
          modifier = Modifier.fillMaxWidth().height(110.dp).testTag("comment_input"),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
          shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = {
              if (commentInputText.isNotBlank()) {
                val textToSubmit = commentInputText
                onPostComment(textToSubmit)
                commentInputText = ""
              }
            },
            enabled = commentInputText.isNotBlank() && !isPosting,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(48.dp).testTag("comment_submit_button")
          ) {
            if (isPosting) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(stringResource(R.string.comments_posting))
            } else {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.comments_submit_button),
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(stringResource(R.string.comments_submit_button), fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Loading State
    if (isLoading) {
      Box(
        modifier = Modifier.fillMaxWidth().padding(24.dp).testTag("comments_loading_indicator"),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
      }
    }

    // Error State
    if (errorMessage != null) {
      Card(
        modifier = Modifier.fillMaxWidth().testTag("comments_error_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(12.dp)
      ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = errorMessage,
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onErrorContainer
            )
          }
          Button(
            onClick = onRetry,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onErrorContainer,
                contentColor = MaterialTheme.colorScheme.errorContainer
              ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(36.dp).testTag("comments_retry_button")
          ) {
            Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Empty State
    if (comments.isEmpty() && !isLoading && errorMessage == null) {
      Card(
        modifier = Modifier.fillMaxWidth().testTag("comments_empty_state"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = borderButtonStroke()
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(32.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = stringResource(R.string.comments_empty_title),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = stringResource(R.string.comments_empty_desc),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
          )
        }
      }
    }

    // Comments List
    comments.forEach { comment ->
      Card(
        modifier = Modifier.fillMaxWidth().testTag("gist_comment_item_${comment.id ?: 0}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = borderButtonStroke()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          // Author Header
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier =
                Modifier.size(32.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "@${comment.user?.login ?: "anonymous"}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              if (!comment.createdAt.isNullOrBlank()) {
                Text(
                  text = comment.createdAt.substringBefore("T"),
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            if (
              comment.user?.login != null &&
                comment.user.login.equals(currentUserLogin, ignoreCase = true) &&
                comment.id != null
            ) {
              IconButton(
                onClick = { onDeleteComment(comment.id) },
                modifier = Modifier.size(48.dp).testTag("delete_comment_${comment.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = stringResource(R.string.comments_delete_desc),
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Body with markdown
          MarkdownText(text = comment.body.orEmpty(), modifier = Modifier.fillMaxWidth())
        }
      }
    }
  }
}
