package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.GistWithFiles
import com.example.ui.components.DetailFileItemCard
import com.example.ui.components.DetailRevisionsTabSection
import com.example.ui.components.DetailedCreationInfoCard
import com.example.ui.components.GistCommentsView
import com.example.ui.components.GistSyncStateIndicator
import com.example.ui.components.borderButtonStroke
import com.example.ui.theme.ActivePurple
import com.example.ui.theme.SlateBg
import com.example.ui.viewmodel.GistViewModel
import com.example.ui.viewmodel.clearCommentsState
import com.example.ui.viewmodel.clearPreviewRevisionState
import com.example.ui.viewmodel.deleteComment
import com.example.ui.viewmodel.loadComments
import com.example.ui.viewmodel.loadGistHistory
import com.example.ui.viewmodel.postComment
import com.example.ui.viewmodel.selectRevision

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GistDetailScreen(
  item: GistWithFiles,
  viewModel: GistViewModel? = null,
  onBack: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onTogglePin: () -> Unit,
  onToggleStar: () -> Unit,
  onFork: (() -> Unit)? = null,
  isForking: Boolean = false,
  modifier: Modifier = Modifier
) {
  var activeDetailTab by remember { mutableStateOf("files") }
  var diffViewMode by remember { mutableStateOf("unified") }

  val historyList = viewModel?.historyList?.collectAsStateWithLifecycle()?.value
  val isLoadingHistory = viewModel?.isLoadingHistory?.collectAsStateWithLifecycle()?.value ?: false
  val historyError = viewModel?.historyError?.collectAsStateWithLifecycle()?.value

  val selectedRevisionSha = viewModel?.selectedRevisionSha?.collectAsStateWithLifecycle()?.value
  val currentRevisionGist = viewModel?.currentRevisionGist?.collectAsStateWithLifecycle()?.value
  val parentRevisionGist = viewModel?.parentRevisionGist?.collectAsStateWithLifecycle()?.value
  val isLoadingRevisionContent =
    viewModel?.isLoadingRevisionContent?.collectAsStateWithLifecycle()?.value ?: false
  val revisionContentError = viewModel?.revisionContentError?.collectAsStateWithLifecycle()?.value

  val commentsList = viewModel?.commentsList?.collectAsStateWithLifecycle()?.value ?: emptyList()
  val isLoadingComments =
    viewModel?.isLoadingComments?.collectAsStateWithLifecycle()?.value ?: false
  val commentsError = viewModel?.commentsError?.collectAsStateWithLifecycle()?.value
  val isPostingComment = viewModel?.isPostingComment?.collectAsStateWithLifecycle()?.value ?: false
  val ownerLogin = viewModel?.ownerLogin?.collectAsStateWithLifecycle()?.value ?: ""

  LaunchedEffect(item.gist.id, activeDetailTab) {
    if (activeDetailTab == "revisions" && viewModel != null) {
      if (item.gist.isLocalOnly) {
        viewModel.clearPreviewRevisionState()
      } else {
        viewModel.loadGistHistory(item.gist.id)
      }
    } else if (activeDetailTab == "comments" && viewModel != null) {
      if (item.gist.isLocalOnly) {
        viewModel.clearCommentsState()
      } else {
        viewModel.loadComments(item.gist.id)
      }
    }
  }

  DisposableEffect(item.gist.id) {
    onDispose {
      viewModel?.clearPreviewRevisionState()
      viewModel?.clearCommentsState()
    }
  }

  val filesToCompare =
    remember(currentRevisionGist, parentRevisionGist) {
      val curFiles = currentRevisionGist?.files ?: emptyMap()
      val parFiles = parentRevisionGist?.files ?: emptyMap()
      val allNames = (curFiles.keys + parFiles.keys).toSet().toList().sorted()
      allNames.map { name ->
        val cur = curFiles[name]
        val par = parFiles[name]
        Triple(name, par?.content ?: "", cur?.content ?: "")
      }
    }

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
            if (viewModel != null && !item.gist.isLocalOnly) {
              IconButton(
                onClick = {
                  activeDetailTab = if (activeDetailTab == "files") "revisions" else "files"
                  if (activeDetailTab == "revisions") {
                    viewModel.selectRevision(item.gist.id, null)
                  }
                },
                modifier = Modifier.testTag("detail_revisions_toggle_button")
              ) {
                Icon(
                  imageVector = Icons.Default.History,
                  contentDescription = "Revisions",
                  tint =
                    if (activeDetailTab == "revisions") ActivePurple
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(22.dp)
                )
              }
            }
            if (onFork != null) {
              IconButton(onClick = onFork, modifier = Modifier.testTag("detail_fork_button")) {
                if (isForking) {
                  CircularProgressIndicator(
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
                tint =
                  if (item.gist.isStarred) Color(0xFFFFA000)
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
              )
            }
            IconButton(onClick = onTogglePin, modifier = Modifier.testTag("detail_pin_button")) {
              Icon(
                imageVector =
                  if (item.gist.isPinned) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "Pin",
                tint =
                  if (item.gist.isPinned) ActivePurple
                  else MaterialTheme.colorScheme.onSurfaceVariant,
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
              contentDescription = null,
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
              contentDescription = null,
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
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 16.dp)
          .testTag("detail_screen_lazy_column"),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item { Spacer(modifier = Modifier.height(8.dp)) }

      // Creator and Timestamps info card
      item { DetailedCreationInfoCard(item = item) }

      // Sync state banner indicator
      item {
        GistSyncStateIndicator(
          isLocalOnly = item.gist.isLocalOnly,
          isDirty = item.gist.isDirty,
          isDeleted = item.gist.isDeleted,
          compact = false
        )
      }

      // Auto-Decrypted Security Status Banner
      item {
        Card(
          modifier = Modifier.fillMaxWidth().testTag("detail_decrypted_banner"),
          shape = RoundedCornerShape(12.dp),
          border =
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.5f)),
          colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.LockOpen,
              contentDescription = "Auto-Decrypted",
              tint = Color(0xFF2E7D32),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "AES-256 Encrypted in Room Storage • Automatically Decrypted for Display",
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = Color(0xFF1B5E20)
            )
          }
        }
      }

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
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (item.gist.isPublic) "Public" else "Private",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        .testTag("gist_detail_tag_$tag")
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

      // Tab selector: Files vs Revisions vs Comments (if ViewModel is available)
      if (viewModel != null && !item.gist.isLocalOnly) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            listOf(
                "Files" to Icons.Default.Description,
                "Revisions" to Icons.Default.History,
                "Comments" to Icons.Default.ChatBubbleOutline
              )
              .forEach { (tabName, icon) ->
                val isSelected = activeDetailTab == tabName.lowercase()
                Row(
                  modifier =
                    Modifier.clip(RoundedCornerShape(8.dp))
                      .background(
                        if (isSelected) ActivePurple.copy(alpha = 0.15f) else Color.Transparent
                      )
                      .border(
                        1.dp,
                        if (isSelected) ActivePurple else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(8.dp)
                      )
                      .clickable {
                        activeDetailTab = tabName.lowercase()
                        if (tabName.lowercase() == "revisions") {
                          viewModel.selectRevision(item.gist.id, null)
                        }
                      }
                      .padding(horizontal = 16.dp, vertical = 8.dp)
                      .testTag("detail_tab_${tabName.lowercase()}"),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = icon,
                    contentDescription = tabName,
                    tint =
                      if (isSelected) ActivePurple else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = tabName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) ActivePurple else MaterialTheme.colorScheme.onSurface
                  )
                }
              }
          }
        }
      }

      when (activeDetailTab) {
        "files" -> {
          // Display all files
          items(item.files) { file -> DetailFileItemCard(file = file) }
        }
        "revisions" -> {
          // Revisions tab content
          item {
            DetailRevisionsTabSection(
              selectedRevisionSha = selectedRevisionSha,
              isLoadingHistory = isLoadingHistory,
              historyError = historyError,
              historyList = historyList,
              defaultOwnerLogin = item.gist.ownerLogin,
              onSelectRevision = { sha -> viewModel?.selectRevision(item.gist.id, sha) },
              diffViewMode = diffViewMode,
              onDiffViewModeChange = { diffViewMode = it },
              isLoadingRevisionContent = isLoadingRevisionContent,
              revisionContentError = revisionContentError,
              filesToCompare = filesToCompare
            )
          }
        }
        "comments" -> {
          // Comments tab content
          item {
            GistCommentsView(
              comments = commentsList,
              isLoading = isLoadingComments,
              errorMessage = commentsError,
              isPosting = isPostingComment,
              currentUserLogin = ownerLogin,
              onPostComment = { body -> viewModel?.postComment(item.gist.id, body) },
              onDeleteComment = { commentId -> viewModel?.deleteComment(item.gist.id, commentId) },
              onRetry = { viewModel?.loadComments(item.gist.id) }
            )
          }
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }
}
