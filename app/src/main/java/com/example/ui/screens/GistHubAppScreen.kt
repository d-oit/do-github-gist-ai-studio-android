package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GistWithFiles
import com.example.ui.components.DraftEditorDialog
import com.example.ui.components.GistPreviewDialog
import com.example.ui.components.TabButton
import com.example.ui.viewmodel.GistViewModel
import kotlinx.coroutines.launch

@Composable
fun GistHubAppScreen(viewModel: GistViewModel) {
    val gists by viewModel.gists.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var activeTab by remember { mutableStateOf("home") }
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearchExpanded by remember { mutableStateOf(false) }

    // Editor form states
    var showEditor by remember { mutableStateOf(false) }
    var editingGistId by remember { mutableStateOf<String?>(null) }
    var editorDescription by remember { mutableStateOf("") }
    var editorFilename by remember { mutableStateOf("") }
    var editorContent by remember { mutableStateOf("") }
    var editorIsPublic by remember { mutableStateOf(true) }
    var editorIsPinned by remember { mutableStateOf(false) }

    // Preview state
    var previewGist by remember { mutableStateOf<GistWithFiles?>(null) }

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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // Logo / Icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "Gist Logo",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "do-gist-hub",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "offline-first sync",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Search button
                        IconButton(
                            onClick = { isSearchExpanded = !isSearchExpanded },
                            modifier = Modifier.testTag("search_toggle")
                        ) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Refresh/Sync button
                        IconButton(
                            onClick = { viewModel.refreshGists() },
                            modifier = Modifier.testTag("refresh_button")
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = isSearchExpanded) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Filter by filename, description, or content...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 56.dp)
                                .testTag("search_field"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .height(80.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabButton(
                    label = "Home",
                    icon = Icons.Default.Home,
                    isActive = activeTab == "home",
                    onClick = { activeTab = "home" },
                    modifier = Modifier.testTag("tab_home")
                )
                TabButton(
                    label = "Vault",
                    icon = Icons.Default.FolderOpen,
                    isActive = activeTab == "vault",
                    onClick = { activeTab = "vault" },
                    modifier = Modifier.testTag("tab_vault")
                )
                TabButton(
                    label = "Sync",
                    icon = Icons.Default.Sync,
                    isActive = activeTab == "sync",
                    onClick = { activeTab = "sync" },
                    modifier = Modifier.testTag("tab_sync")
                )
                TabButton(
                    label = "Config",
                    icon = Icons.Default.Settings,
                    isActive = activeTab == "config",
                    onClick = { activeTab = "config" },
                    modifier = Modifier.testTag("tab_config")
                )
            }
        },
        floatingActionButton = {
            if (activeTab == "home" || activeTab == "vault") {
                androidx.compose.material3.FloatingActionButton(
                    onClick = {
                        editingGistId = null
                        editorDescription = ""
                        editorFilename = ""
                        editorContent = ""
                        editorIsPublic = true
                        editorIsPinned = false
                        showEditor = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .testTag("fab_create_gist")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "New Draft")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab contents
            when (activeTab) {
                "home" -> {
                    HomeScreen(
                        gists = gists,
                        searchQuery = searchQuery,
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refreshGists() },
                        onTogglePin = { viewModel.togglePin(it) },
                        onEdit = { item ->
                            editingGistId = item.gist.id
                            editorDescription = item.gist.description ?: ""
                            editorFilename = item.files.firstOrNull()?.filename ?: ""
                            editorContent = item.files.firstOrNull()?.content ?: ""
                            editorIsPublic = item.gist.isPublic
                            editorIsPinned = item.gist.isPinned
                            showEditor = true
                        },
                        onDelete = { viewModel.deleteGist(it) },
                        onPreview = { previewGist = it }
                    )
                }
                "vault" -> {
                    VaultScreen(
                        gists = gists,
                        searchQuery = searchQuery,
                        onTogglePin = { viewModel.togglePin(it) },
                        onEdit = { item ->
                            editingGistId = item.gist.id
                            editorDescription = item.gist.description ?: ""
                            editorFilename = item.files.firstOrNull()?.filename ?: ""
                            editorContent = item.files.firstOrNull()?.content ?: ""
                            editorIsPublic = item.gist.isPublic
                            editorIsPinned = item.gist.isPinned
                            showEditor = true
                        },
                        onDelete = { viewModel.deleteGist(it) },
                        onCreateDraftClick = {
                            editingGistId = null
                            editorDescription = ""
                            editorFilename = "untitled.kt"
                            editorContent = ""
                            editorIsPublic = true
                            editorIsPinned = false
                            showEditor = true
                        },
                        onPreview = { previewGist = it }
                    )
                }
                "sync" -> {
                    SyncScreen(
                        gists = gists,
                        isSyncing = isSyncing,
                        onSyncClick = { viewModel.syncAll() }
                    )
                }
                "config" -> {
                    ConfigScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Modal Draft Editor
    if (showEditor) {
        // Key the dialog state to avoid stale inputs when opening/closing or changing items
        key(editingGistId) {
            DraftEditorDialog(
                show = showEditor,
                editingGistId = editingGistId,
                onDismiss = { showEditor = false },
                initialDescription = editorDescription,
                initialFilename = editorFilename,
                initialContent = editorContent,
                initialIsPublic = editorIsPublic,
                initialIsPinned = editorIsPinned,
                onSave = { description, filename, content, isPublic, isPinned ->
                    if (editingGistId == null) {
                        viewModel.createGist(
                            description = description,
                            filename = filename,
                            content = content,
                            isPublic = isPublic,
                            isPinned = isPinned
                        )
                    } else {
                        viewModel.updateGist(
                            id = editingGistId!!,
                            description = description,
                            filename = filename,
                            content = content,
                            isPublic = isPublic,
                            isPinned = isPinned
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
            onDismiss = { previewGist = null }
        )
    }
}
