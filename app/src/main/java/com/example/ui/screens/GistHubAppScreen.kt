package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GistWithFiles
import com.example.ui.components.DraftEditorDialog
import com.example.ui.components.GistHubBottomBar
import com.example.ui.components.GistHubTopAppBar
import com.example.ui.components.GistPreviewDialog
import com.example.ui.components.LoadingFeedbackBar
import com.example.ui.components.LoadingFeedbackOverlay
import com.example.ui.viewmodel.GistViewModel
import com.example.ui.viewmodel.clearAutoSavedDraft
import com.example.ui.viewmodel.clearSearchHistory
import com.example.ui.viewmodel.deleteSearchQuery
import com.example.ui.viewmodel.getAutoSavedDraft
import com.example.ui.viewmodel.saveAutoSavedDraft
import com.example.ui.viewmodel.saveSearchQuery
import kotlinx.coroutines.launch

@Composable
fun GistHubAppScreen(viewModel: GistViewModel) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val gists by viewModel.gists.collectAsState()
  val isRefreshing by viewModel.isRefreshing.collectAsState()
  val isSyncing by viewModel.isSyncing.collectAsState()
  val isOnline by viewModel.isOnline.collectAsState()
  val statusMessage by viewModel.statusMessage.collectAsState()
  val errorMessage by viewModel.errorMessage.collectAsState()
  val syncStatus by viewModel.syncStatus.collectAsState()
  val lastSyncTime by viewModel.lastSyncTime.collectAsState()
  val isOfflineOnly by viewModel.isOfflineOnly.collectAsState()
  val pendingDeleteEvent by viewModel.pendingDeleteEvent.collectAsState()

  val selectedTag by viewModel.selectedTag.collectAsState()
  val allTags by viewModel.allTags.collectAsState()
  val sortOption by viewModel.sortOption.collectAsState()
  val showStarredOnly by viewModel.showStarredOnly.collectAsState()

  val remoteGists by viewModel.remoteGists.collectAsState()
  val isFetchingRemote by viewModel.isFetchingRemote.collectAsState()
  val remoteError by viewModel.remoteError.collectAsState()
  val isForking by viewModel.isForking.collectAsState()
  val isFetchingProfile by viewModel.isFetchingProfile.collectAsState()

  var activeTab by remember { mutableStateOf("home") }
  val searchQuery by viewModel.searchQuery.collectAsState()
  val searchHistory by viewModel.searchHistory.collectAsState()
  var isSearchExpanded by remember { mutableStateOf(false) }

  // Editor form states
  var showEditor by remember { mutableStateOf(false) }
  var editingGistId by remember { mutableStateOf<String?>(null) }
  var editorDescription by remember { mutableStateOf("") }
  var editorFiles by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
  var editorIsPublic by remember { mutableStateOf(true) }
  var editorIsPinned by remember { mutableStateOf(false) }
  var editorTags by remember { mutableStateOf<List<String>>(emptyList()) }

  // Reactive Detail View and Preview state
  var selectedDetailGistId by remember { mutableStateOf<String?>(null) }
  val selectedDetailGist =
    remember(gists, selectedDetailGistId) { gists.find { it.gist.id == selectedDetailGistId } }
  var previewGist by remember { mutableStateOf<GistWithFiles?>(null) }

  // Deletion confirmation state
  var gistIdToDelete by remember { mutableStateOf<String?>(null) }

  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  LaunchedEffect(statusMessage) {
    statusMessage?.let {
      scope.launch {
        snackbarHostState.showSnackbar(it)
        viewModel.clearMessages()
      }
    }
  }

  LaunchedEffect(errorMessage) {
    errorMessage?.let {
      scope.launch {
        snackbarHostState.showSnackbar(it)
        viewModel.clearMessages()
      }
    }
  }

  LaunchedEffect(syncStatus) {
    if (
      syncStatus is com.example.data.repository.SyncStatus.Error ||
        syncStatus is com.example.data.repository.SyncStatus.Success
    ) {
      scope.launch {
        com.example.ui.util.SyncNotificationUtil.showSyncStatusSnackbar(
          snackbarHostState = snackbarHostState,
          syncStatus = syncStatus,
          onDismiss = { viewModel.dismissSyncError() }
        )
      }
    }
  }

  LaunchedEffect(pendingDeleteEvent) {
    val deletedItem = pendingDeleteEvent
    if (deletedItem != null) {
      scope.launch {
        val filename = deletedItem.files.firstOrNull()?.filename ?: "Gist"
        val result =
          snackbarHostState.showSnackbar(
            message = "\"$filename\" deleted",
            actionLabel = "Undo",
            duration = androidx.compose.material3.SnackbarDuration.Short
          )
        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
          viewModel.restoreGist(deletedItem)
        }
        viewModel.clearPendingDeleteEvent()
      }
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      if (selectedDetailGist == null) {
        GistHubTopAppBar(
          isSearchExpanded = isSearchExpanded,
          onToggleSearch = { isSearchExpanded = !isSearchExpanded },
          searchQuery = searchQuery,
          onSearchQueryChange = { viewModel.updateSearchQuery(it) },
          isRefreshing = isRefreshing,
          onRefresh = { viewModel.refreshGists(context) },
          lastSyncTime = lastSyncTime,
          syncStatus = syncStatus,
          isOfflineOnly = isOfflineOnly,
          onToggleOfflineOnly = { viewModel.toggleOfflineOnly() }
        )
      }
    },
    bottomBar = {
      if (selectedDetailGist == null) {
        GistHubBottomBar(activeTab = activeTab, onTabSelected = { activeTab = it })
      }
    },
    floatingActionButton = {
      if (selectedDetailGist == null && (activeTab == "home" || activeTab == "vault")) {
        androidx.compose.material3.FloatingActionButton(
          onClick = {
            editingGistId = null
            editorDescription = ""
            editorFiles = emptyList()
            editorIsPublic = true
            editorIsPinned = false
            editorTags = emptyList()
            showEditor = true
          },
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.padding(bottom = 16.dp).testTag("fab_create_gist")
        ) {
          Icon(imageVector = Icons.Default.Add, contentDescription = "New Draft")
        }
      }
    },
    containerColor = MaterialTheme.colorScheme.background
  ) { innerPadding ->
    if (selectedDetailGist != null) {
      GistDetailScreen(
        item = selectedDetailGist,
        onBack = { selectedDetailGistId = null },
        onEdit = {
          val currentItem = selectedDetailGist
          selectedDetailGistId = null
          editingGistId = currentItem.gist.id
          editorDescription = currentItem.gist.description ?: ""
          editorFiles = currentItem.files.map { it.filename to it.content }
          editorIsPublic = currentItem.gist.isPublic
          editorIsPinned = currentItem.gist.isPinned
          editorTags = currentItem.gist.tags
          showEditor = true
        },
        onDelete = {
          println("DEBUG: GistDetailScreen onDelete called for id: ${selectedDetailGist.gist.id}")
          gistIdToDelete = selectedDetailGist.gist.id
        },
        onTogglePin = { viewModel.togglePin(selectedDetailGist.gist.id) },
        onToggleStar = { viewModel.toggleStar(selectedDetailGist.gist.id) },
        onFork =
          if (selectedDetailGist.gist.isPublic) {
            { viewModel.forkGist(selectedDetailGist.gist.id) }
          } else null,
        isForking = isForking == selectedDetailGist.gist.id
      )
    } else {
      Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        LoadingFeedbackBar(isLoading = isSyncing, message = "Synchronizing Gists with GitHub...")
        // Tab contents
        when (activeTab) {
          "home" -> {
            HomeScreen(
              gists = gists,
              searchQuery = searchQuery,
              onSearchQueryChange = { viewModel.updateSearchQuery(it) },
              selectedTag = selectedTag,
              allTags = allTags,
              onSelectedTagChange = { viewModel.updateSelectedTag(it) },
              sortOption = sortOption,
              onSortOptionChange = { viewModel.updateSortOption(it) },
              showStarredOnly = showStarredOnly,
              onToggleShowStarredOnly = { viewModel.toggleShowStarredOnly() },
              isRefreshing = isRefreshing,
              onRefresh = { viewModel.refreshGists(context) },
              onTogglePin = { viewModel.togglePin(it) },
              onToggleStar = { viewModel.toggleStar(it) },
              onEdit = { item ->
                editingGistId = item.gist.id
                editorDescription = item.gist.description ?: ""
                editorFiles = item.files.map { it.filename to it.content }
                editorIsPublic = item.gist.isPublic
                editorIsPinned = item.gist.isPinned
                editorTags = item.gist.tags
                showEditor = true
              },
              onDelete = { gistIdToDelete = it },
              onPreview = { previewGist = it },
              lastSyncTime = lastSyncTime,
              searchHistory = searchHistory,
              onSaveSearchQuery = { viewModel.saveSearchQuery(it) },
              onDeleteSearchQuery = { viewModel.deleteSearchQuery(it) },
              onClearSearchHistory = { viewModel.clearSearchHistory() },
              onCreateDraftClick = {
                editingGistId = null
                editorDescription = ""
                editorFiles = emptyList()
                editorIsPublic = true
                editorIsPinned = false
                editorTags = emptyList()
                showEditor = true
              }
            )
          }
          "vault" -> {
            VaultScreen(
              gists = gists,
              searchQuery = searchQuery,
              onSearchQueryChange = { viewModel.updateSearchQuery(it) },
              onTogglePin = { viewModel.togglePin(it) },
              onToggleStar = { viewModel.toggleStar(it) },
              onEdit = { item ->
                editingGistId = item.gist.id
                editorDescription = item.gist.description ?: ""
                editorFiles = item.files.map { it.filename to it.content }
                editorIsPublic = item.gist.isPublic
                editorIsPinned = item.gist.isPinned
                editorTags = item.gist.tags
                showEditor = true
              },
              onDelete = { gistIdToDelete = it },
              onCreateDraftClick = {
                editingGistId = null
                editorDescription = ""
                editorFiles = emptyList()
                editorIsPublic = true
                editorIsPinned = false
                editorTags = emptyList()
                showEditor = true
              },
              onPreview = { previewGist = it },
              isRefreshing = isRefreshing,
              onRefresh = { viewModel.refreshGists(context) }
            )
          }
          "sync" -> {
            SyncScreen(
              gists = gists,
              isSyncing = isSyncing,
              onSyncClick = { viewModel.syncAll() },
              remoteGists = remoteGists,
              isFetchingRemote = isFetchingRemote,
              remoteError = remoteError,
              onRefreshRemote = { viewModel.fetchRemoteGistsDirectly() },
              isForking = isForking,
              onForkClick = { viewModel.forkGist(it) },
              lastSyncTime = lastSyncTime,
              syncStatus = syncStatus,
              isOnline = isOnline,
              isOfflineOnly = isOfflineOnly,
              onToggleOfflineOnly = { viewModel.toggleOfflineOnly() }
            )
          }
          "config" -> {
            ConfigScreen(viewModel = viewModel)
          }
        }
      }
    }
  }

  val aiAnalysis by viewModel.aiAnalysis.collectAsState()
  val isAnalyzingGist by viewModel.isAnalyzingGist.collectAsState()

  // Modal Draft Editor
  if (showEditor) {
    // Key the dialog state to avoid stale inputs when opening/closing or changing items
    key(editingGistId) {
      DraftEditorDialog(
        show = showEditor,
        editingGistId = editingGistId,
        onDismiss = {
          showEditor = false
          viewModel.clearAiAnalysis()
        },
        initialDescription = editorDescription,
        initialFiles = editorFiles,
        initialIsPublic = editorIsPublic,
        initialIsPinned = editorIsPinned,
        initialTags = editorTags,
        isAnalyzing = isAnalyzingGist,
        aiAnalysis = aiAnalysis,
        onAnalyzeClick = { desc, filesList -> viewModel.analyzeGistContent(desc, filesList) },
        onClearAiClick = { viewModel.clearAiAnalysis() },
        onAutoSave = { desc, filesList, isPub, isPin, tagList ->
          viewModel.saveAutoSavedDraft(editingGistId, desc, filesList, isPub, isPin, tagList)
        },
        onLoadAutoSave = { viewModel.getAutoSavedDraft(editingGistId) },
        onClearAutoSave = { viewModel.clearAutoSavedDraft(editingGistId) },
        onSave = { description, filesList, isPublic, isPinned, tags ->
          if (editingGistId == null) {
            viewModel.createGist(
              description = description,
              files = filesList,
              isPublic = isPublic,
              isPinned = isPinned,
              tags = tags
            )
          } else {
            viewModel.updateGist(
              id = editingGistId!!,
              description = description,
              files = filesList,
              isPublic = isPublic,
              isPinned = isPinned,
              tags = tags
            )
          }
          showEditor = false
        }
      )
    }
  }

  // Modal Preview Dialog
  if (previewGist != null) {
    GistPreviewDialog(
      show = previewGist != null,
      item = previewGist,
      viewModel = viewModel,
      onDismiss = { previewGist = null }
    )
  }

  // Deletion Confirmation Dialog
  if (gistIdToDelete != null) {
    AlertDialog(
      onDismissRequest = { gistIdToDelete = null },
      title = {
        Text(
          text = "Delete Gist",
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
      },
      text = {
        Text(
          text =
            "Are you sure you want to delete this Gist? This action cannot be undone and will permanently remove it.",
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            gistIdToDelete?.let { id ->
              viewModel.deleteGist(id)
              if (selectedDetailGistId == id) {
                selectedDetailGistId = null
              }
            }
            gistIdToDelete = null
          },
          modifier = Modifier.testTag("delete_confirm_confirm")
        ) {
          Text(
            text = "Delete",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
          )
        }
      },
      dismissButton = {
        TextButton(
          onClick = { gistIdToDelete = null },
          modifier = Modifier.testTag("delete_confirm_cancel")
        ) {
          Text(
            text = "Cancel",
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(28.dp),
      modifier = Modifier.testTag("delete_confirm_dialog")
    )
  }

  LoadingFeedbackOverlay(
    isLoading = isFetchingProfile,
    title = "Verifying Token",
    message = "Retrieving user profile and authorization scopes from GitHub..."
  )

  LoadingFeedbackOverlay(
    isLoading = isForking != null,
    title = "Forking Gist",
    message = "Duplicating Gist to your account and saving local copy..."
  )
}
