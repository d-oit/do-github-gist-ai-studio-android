package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GistWithFiles
import com.example.ui.components.DetailedCreationInfoCard
import com.example.ui.components.MarkdownText
import com.example.ui.components.SyntaxHighlighter
import com.example.ui.components.borderButtonStroke
import com.example.ui.theme.ActivePurple
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.SlateBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GistDetailScreen(
  item: GistWithFiles,
  onBack: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onTogglePin: () -> Unit,
  onToggleStar: () -> Unit,
  onFork: (() -> Unit)? = null,
  isForking: Boolean = false,
  modifier: Modifier = Modifier
) {
  Scaffold(
    topBar = {
      Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().statusBarsPadding()
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Gist Details",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            if (onFork != null) {
              IconButton(onClick = onFork, modifier = Modifier.testTag("detail_fork_button")) {
                if (isForking) {
                  androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 1.5.dp,
                    color = ActivePurple
                  )
                } else {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.CallSplit,
                    contentDescription = "Fork Gist",
                    tint = ActivePurple,
                    modifier = Modifier.size(22.dp)
                  )
                }
              }
            }
            IconButton(onClick = onToggleStar, modifier = Modifier.testTag("detail_star_button")) {
              Icon(
                imageVector =
                  if (item.gist.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Star",
                tint = if (item.gist.isStarred) Color(0xFFFFA000) else GraySecondary,
                modifier = Modifier.size(22.dp)
              )
            }
            IconButton(onClick = onTogglePin, modifier = Modifier.testTag("detail_pin_button")) {
              Icon(
                imageVector =
                  if (item.gist.isPinned) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "Pin",
                tint = if (item.gist.isPinned) ActivePurple else GraySecondary,
                modifier = Modifier.size(22.dp)
              )
            }
          }
        }
      }
    },
    bottomBar = {
      Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().navigationBarsPadding()
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = onDelete,
            colors =
              ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border =
              androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(48.dp).testTag("detail_delete_button")
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete",
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Delete", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          }

          Button(
            onClick = onEdit,
            colors = ButtonDefaults.buttonColors(containerColor = ActivePurple),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(48.dp).testTag("detail_edit_button")
          ) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Edit",
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Draft", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          }
        }
      }
    },
    containerColor = SlateBg,
    modifier = modifier.fillMaxSize().testTag("gist_detail_screen")
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item { Spacer(modifier = Modifier.height(8.dp)) }

      // Creator and Timestamps info card
      item { DetailedCreationInfoCard(item = item) }

      // Description & Tags card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
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
              Text(
                text = "Description",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector =
                    if (item.gist.isPublic) Icons.Default.Public else Icons.Default.Lock,
                  contentDescription = "Visibility",
                  tint = GraySecondary,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (item.gist.isPublic) "Public" else "Private",
                  fontSize = 11.sp,
                  color = GraySecondary
                )
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text =
                item.gist.description?.ifEmpty { "No description provided" }
                  ?: "No description provided",
              fontSize = 14.sp,
              color = MaterialTheme.colorScheme.onSurface,
              lineHeight = 20.sp
            )

            if (item.gist.tags.isNotEmpty()) {
              Spacer(modifier = Modifier.height(12.dp))
              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                item.gist.tags.forEach { tag ->
                  Box(
                    modifier =
                      Modifier.background(
                          MaterialTheme.colorScheme.secondaryContainer,
                          RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                  ) {
                    Text(
                      text = "#$tag",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Display all files
      items(item.files) { file ->
        val isMarkdown =
          file.filename.endsWith(".md", ignoreCase = true) ||
            file.filename.endsWith(".markdown", ignoreCase = true)

        var previewMode by remember { mutableStateOf(if (isMarkdown) "markdown" else "raw") }

        Card(
          modifier = Modifier.fillMaxWidth().testTag("detail_file_card_${file.filename}"),
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
                  color = GraySecondary
                )
              }

              if (isMarkdown) {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
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
                        else GraySecondary
                    )
                  }
                  TextButton(
                    onClick = { previewMode = "markdown" },
                    modifier = Modifier.height(36.dp).testTag("file_mode_markdown_${file.filename}")
                  ) {
                    Text(
                      text = "Markdown",
                      fontSize = 12.sp,
                      fontWeight =
                        if (previewMode == "markdown") FontWeight.Bold else FontWeight.Normal,
                      color =
                        if (previewMode == "markdown") MaterialTheme.colorScheme.primary
                        else GraySecondary
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isMarkdown && previewMode == "markdown") {
              Surface(
                modifier =
                  Modifier.fillMaxWidth()
                    .border(
                      1.dp,
                      MaterialTheme.colorScheme.outlineVariant,
                      RoundedCornerShape(8.dp)
                    )
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
                    .border(
                      1.dp,
                      MaterialTheme.colorScheme.outlineVariant,
                      RoundedCornerShape(8.dp)
                    ),
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

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }
}
