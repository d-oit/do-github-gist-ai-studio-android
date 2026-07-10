package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.example.data.local.entity.GistWithFiles
import com.example.ui.theme.ActivePurple
import com.example.ui.theme.ActivePurpleContainer
import com.example.ui.theme.DarkPurpleText
import com.example.ui.theme.DarkRedText
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.GrayTertiary
import com.example.ui.theme.LightPinkContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SlateBg
import com.example.ui.theme.SlateBorder
import com.example.ui.viewmodel.GistViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = applicationContext as DoGistHubApp
        val viewModelFactory = GistViewModel.Factory(app.repository, app.configPrefs)

        setContent {
            val viewModel: GistViewModel by viewModels { viewModelFactory }
            val appTheme by viewModel.appTheme.collectAsState()
            MyApplicationTheme(themeMode = appTheme) {
                GistHubAppScreen(viewModel)
            }
        }
    }
}

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
        Dialog(
            onDismissRequest = { showEditor = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    val spellSuggestions = remember(editorDescription, editorContent) {
                        getSpellSuggestions(editorDescription, editorContent)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (editingGistId == null) "New Local Draft" else "Edit Local Draft",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        IconButton(onClick = { showEditor = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val textFieldColors = OutlinedTextFieldDefaults.colors(
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

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = editorDescription,
                                onValueChange = { editorDescription = it },
                                label = { Text("Gist Description") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("editor_description"),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    autoCorrect = true,
                                    keyboardType = KeyboardType.Text
                                ),
                                colors = textFieldColors,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = editorFilename,
                                onValueChange = { editorFilename = it },
                                label = { Text("Filename (e.g. main.kt)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("editor_filename"),
                                singleLine = true,
                                colors = textFieldColors,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = editorContent,
                                onValueChange = { editorContent = it },
                                label = { Text("File Content") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 200.dp, max = 400.dp)
                                    .testTag("editor_content"),
                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.None,
                                    autoCorrect = true,
                                    keyboardType = KeyboardType.Text
                                ),
                                colors = textFieldColors,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        if (spellSuggestions.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Spell and Grammar Checker",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Spelling & Grammar Helper",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Suggestions to improve your local draft. Click 'Fix' to apply.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        spellSuggestions.forEach { suggestion ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                                                    .border(1.dp, MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(8.dp))
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = if (suggestion.targetField == "description") "In Description" else "In File Content",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            text = "•",
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = suggestion.type,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (suggestion.type == "Spelling") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = suggestion.explanation,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = suggestion.original,
                                                            fontSize = 11.sp,
                                                            style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "to",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                        Text(
                                                            text = suggestion.replacement,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                                Button(
                                                    onClick = {
                                                        if (suggestion.targetField == "description") {
                                                            editorDescription = editorDescription.replaceFirst(suggestion.original, suggestion.replacement)
                                                        } else {
                                                            editorContent = editorContent.replaceFirst(suggestion.original, suggestion.replacement)
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                    ),
                                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier
                                                        .height(28.dp)
                                                        .testTag("fix_suggestion_btn"),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("Fix", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Public Gist",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Decides visibility status of your snippet.",
                                        fontSize = 11.sp,
                                        color = GraySecondary
                                    )
                                }
                                Switch(
                                    checked = editorIsPublic,
                                    onCheckedChange = { editorIsPublic = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = ActivePurple
                                    )
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Pin to Favorites",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Pin this gist locally on top of your vault list.",
                                        fontSize = 11.sp,
                                        color = GraySecondary
                                    )
                                }
                                Switch(
                                    checked = editorIsPinned,
                                    onCheckedChange = { editorIsPinned = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = ActivePurple
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showEditor = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = GraySecondary)
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val id = editingGistId
                                if (id == null) {
                                    viewModel.createGist(
                                        description = editorDescription,
                                        filename = editorFilename,
                                        content = editorContent,
                                        isPublic = editorIsPublic,
                                        isPinned = editorIsPinned
                                    )
                                } else {
                                    viewModel.updateGist(
                                        id = id,
                                        description = editorDescription,
                                        filename = editorFilename,
                                        content = editorContent,
                                        isPublic = editorIsPublic,
                                        isPinned = editorIsPinned
                                    )
                                }
                                showEditor = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ActivePurple),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Draft", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal Preview Dialog
    if (previewGist != null) {
        val item = previewGist!!
        Dialog(
            onDismissRequest = { previewGist = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SlateBg)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                color = SlateBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.files.firstOrNull()?.filename ?: "Snippet Preview",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { previewGist = null }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = item.gist.description ?: "No description provided",
                        fontSize = 14.sp,
                        color = GraySecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E1E1E) // Dark developer/code terminal style background
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            item {
                                Text(
                                    text = item.files.firstOrNull()?.content ?: "// No content inside",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = Color(0xFFE0E0E0),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (item.gist.isPublic) Icons.Default.Public else Icons.Default.Lock,
                                contentDescription = "Visibility",
                                tint = GrayTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (item.gist.isPublic) "Public Gist" else "Private Gist",
                                fontSize = 12.sp,
                                color = GraySecondary
                            )
                        }

                        Button(
                            onClick = { previewGist = null },
                            colors = ButtonDefaults.buttonColors(containerColor = ActivePurple),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabButton(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    gists: List<GistWithFiles>,
    searchQuery: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onTogglePin: (String) -> Unit,
    onEdit: (GistWithFiles) -> Unit,
    onDelete: (String) -> Unit,
    onPreview: (GistWithFiles) -> Unit
) {
    val filtered = remember(gists, searchQuery) {
        if (searchQuery.isBlank()) {
            gists.sortedWith(compareByDescending<GistWithFiles> { it.gist.isPinned }.thenByDescending { it.gist.createdAt })
        } else {
            gists.filter { item ->
                val filename = item.files.firstOrNull()?.filename ?: ""
                val desc = item.gist.description ?: ""
                val content = item.files.firstOrNull()?.content ?: ""
                filename.contains(searchQuery, ignoreCase = true) ||
                        desc.contains(searchQuery, ignoreCase = true) ||
                        content.contains(searchQuery, ignoreCase = true)
            }.sortedWith(compareByDescending<GistWithFiles> { it.gist.isPinned }.thenByDescending { it.gist.createdAt })
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Local Cache & Sync Status",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ActivePurple,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .background(ActivePurpleContainer, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${filtered.size} Items",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkPurpleText
                    )
                }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Gists Found",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Synchronize online or tap + to create your first offline-first gist draft.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.gist.id }) { item ->
                        GistCard(
                            item = item,
                            onTogglePin = { onTogglePin(item.gist.id) },
                            onEdit = { onEdit(item) },
                            onDelete = { onDelete(item.gist.id) },
                            onPreview = { onPreview(item) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun VaultScreen(
    gists: List<GistWithFiles>,
    searchQuery: String,
    onTogglePin: (String) -> Unit,
    onEdit: (GistWithFiles) -> Unit,
    onDelete: (String) -> Unit,
    onCreateDraftClick: () -> Unit,
    onPreview: (GistWithFiles) -> Unit
) {
    val draftGists = remember(gists) {
        gists.filter { it.gist.isLocalOnly || it.gist.isDirty }
    }

    val filtered = remember(draftGists, searchQuery) {
        if (searchQuery.isBlank()) {
            draftGists.sortedWith(compareByDescending<GistWithFiles> { it.gist.isPinned }.thenByDescending { it.gist.createdAt })
        } else {
            draftGists.filter { item ->
                val filename = item.files.firstOrNull()?.filename ?: ""
                val desc = item.gist.description ?: ""
                filename.contains(searchQuery, ignoreCase = true) ||
                        desc.contains(searchQuery, ignoreCase = true)
            }.sortedWith(compareByDescending<GistWithFiles> { it.gist.isPinned }.thenByDescending { it.gist.createdAt })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Unsaved Drafts & Local Vault",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ActivePurple,
                letterSpacing = 1.sp
            )
            Box(
                modifier = Modifier
                    .background(ActivePurpleContainer, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${filtered.size} Unsynced",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkPurpleText
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Dashed Add Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .clickable(onClick = onCreateDraftClick)
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Draft",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "New Local Draft",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Auto-tracks with isLocalOnly: true",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No offline changes or local drafts.",
                            fontSize = 13.sp,
                            color = GraySecondary
                        )
                    }
                }
            } else {
                items(filtered, key = { it.gist.id }) { item ->
                    GistCard(
                        item = item,
                        onTogglePin = { onTogglePin(item.gist.id) },
                        onEdit = { onEdit(item) },
                        onDelete = { onDelete(item.gist.id) },
                        onPreview = { onPreview(item) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun SyncScreen(
    gists: List<GistWithFiles>,
    isSyncing: Boolean,
    onSyncClick: () -> Unit
) {
    val unsynced = remember(gists) {
        gists.filter { it.gist.isLocalOnly || it.gist.isDirty || it.gist.isDeleted }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (unsynced.isEmpty()) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                contentDescription = "Sync state",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (unsynced.isEmpty()) "All Gists Synchronized" else "Offline Changes Pending",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (unsynced.isEmpty()) {
                "Your local repository matches your cloud profile perfectly."
            } else {
                "You have ${unsynced.size} unsaved changes waiting to be pushed online."
            },
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        if (unsynced.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Pending Queue",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    unsynced.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.files.firstOrNull()?.filename ?: "untitled",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .background(
                                        when {
                                            item.gist.isDeleted -> MaterialTheme.colorScheme.errorContainer
                                            item.gist.isLocalOnly -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.primaryContainer
                                        },
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when {
                                        item.gist.isDeleted -> "Delete"
                                        item.gist.isLocalOnly -> "Draft"
                                        else -> "Dirty"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        item.gist.isDeleted -> MaterialTheme.colorScheme.onErrorContainer
                                        item.gist.isLocalOnly -> MaterialTheme.colorScheme.onError
                                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSyncClick,
            enabled = !isSyncing && unsynced.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("sync_submit_button")
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sync with GitHub", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun ConfigScreen(
    viewModel: GistViewModel
) {
    val token by viewModel.token.collectAsState()
    val ownerLogin by viewModel.ownerLogin.collectAsState()
    val ownerAvatar by viewModel.ownerAvatar.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isFetchingProfile by viewModel.isFetchingProfile.collectAsState()
    val currentTheme by viewModel.appTheme.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current

    val textFieldColors = OutlinedTextFieldDefaults.colors(
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "GitHub Credentials Configuration",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Enter a Personal Access Token (PAT) with 'gist' permissions to sync with GitHub Gists.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            OutlinedTextField(
                value = token,
                onValueChange = { viewModel.updateToken(it) },
                label = { Text("GitHub Token (PAT)") },
                placeholder = { Text("ghp_...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("config_token_input"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.validateAndFetchProfile()
                },
                enabled = !isFetchingProfile && token.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("config_verify_button")
            ) {
                if (isFetchingProfile) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Verify",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify Token & Autofill Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = ownerLogin,
                onValueChange = {},
                readOnly = true,
                label = { Text("GitHub Username (Auto-filled)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("config_username_input"),
                singleLine = true,
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = ownerAvatar,
                onValueChange = {},
                readOnly = true,
                label = { Text("User Avatar URL (Auto-filled)") },
                placeholder = { Text("Not authenticated") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("config_avatar_input"),
                singleLine = true,
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.clearConfig()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("config_clear_button")
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Disconnect",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect & Clear Session", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Visual Accessibility & Theme",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Switch between high-contrast light and dark modes to meet WCAG 2.2 AAA accessibility requirements.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // High Contrast Light
                        val isLightSelected = currentTheme == "light"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .border(
                                    width = if (isLightSelected) 2.dp else 1.dp,
                                    color = if (isLightSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.updateAppTheme("light") }
                                .testTag("theme_toggle_light"),
                            color = if (isLightSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Light Theme",
                                    tint = if (isLightSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column {
                                    Text(
                                        text = "Light Mode",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isLightSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "High Contrast Light",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // High Contrast Dark
                        val isDarkSelected = currentTheme == "dark"
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .border(
                                    width = if (isDarkSelected) 2.dp else 1.dp,
                                    color = if (isDarkSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.updateAppTheme("dark") }
                                .testTag("theme_toggle_dark"),
                            color = if (isDarkSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "Dark Theme",
                                    tint = if (isDarkSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column {
                                    Text(
                                        text = "Dark Mode",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isDarkSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "OLED High Contrast",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Connection Status",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (token.isNotEmpty()) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (token.isNotEmpty()) "Configured" else "No Token Configuration",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (token.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.refreshGists() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                Text("Test Connection & Fetch", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun borderButtonStroke() = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GistCard(
    item: GistWithFiles,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPreview: () -> Unit
) {
    Card(
        onClick = onEdit,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gist_card_${item.gist.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = borderButtonStroke(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.files.firstOrNull()?.filename ?: "untitled.kt",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (item.gist.isPublic) Icons.Default.Public else Icons.Default.Lock,
                            contentDescription = "Visibility",
                            tint = GraySecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.gist.description ?: "No description provided",
                        fontSize = 13.sp,
                        color = GraySecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTogglePin) {
                        Icon(
                            imageVector = if (item.gist.isPinned) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Pin",
                            tint = if (item.gist.isPinned) ActivePurple else GraySecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.gist.isPinned) {
                        Box(
                            modifier = Modifier
                                .background(ActivePurpleContainer, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(ActivePurple)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Pinned",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkPurpleText
                                )
                            }
                        }
                    }

                    if (item.gist.isLocalOnly) {
                        Box(
                            modifier = Modifier
                                .background(LightPinkContainer, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Local Only",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkRedText
                            )
                        }
                    } else if (item.gist.isDirty) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, ErrorRed, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Dirty",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Dirty",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ErrorRed
                                )
                            }
                        }
                    }
                }

                // Action buttons
                Row {
                    IconButton(onClick = onPreview) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Preview Code",
                            tint = GraySecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = GraySecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ErrorRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

data class SpellSuggestion(
    val type: String, // "Spelling" or "Grammar"
    val original: String,
    val replacement: String,
    val explanation: String,
    val targetField: String // "description" or "content"
)

fun getSpellSuggestions(description: String, content: String): List<SpellSuggestion> {
    val list = mutableListOf<SpellSuggestion>()
    
    // Common spelling errors dictionary
    val dictionary = mapOf(
        "teh" to "the",
        "dont" to "don't",
        "cant" to "can't",
        "wont" to "won't",
        "shoudl" to "should",
        "recieve" to "receive",
        "recieved" to "received",
        "seperate" to "separate",
        "definately" to "definitely",
        "goverment" to "government",
        "occured" to "occurred",
        "untill" to "until",
        "truely" to "truly",
        "wierd" to "weird",
        "abbout" to "about",
        "becuase" to "because",
        "enought" to "enough"
    )

    fun checkField(text: String, fieldName: String) {
        if (text.isEmpty()) return

        // 1. Double words
        val doubleWordRegex = Regex("\\b(\\w+)\\s+\\1\\b", RegexOption.IGNORE_CASE)
        doubleWordRegex.findAll(text).forEach { match ->
            val word = match.groupValues[1]
            list.add(
                SpellSuggestion(
                    type = "Grammar",
                    original = match.value,
                    replacement = word,
                    explanation = "Repeated word: '$word'",
                    targetField = fieldName
                )
            )
        }

        // 2. Dictionary spelling checks
        val words = text.split(Regex("[^a-zA-Z']+"))
        for (w in words) {
            val lowercaseW = w.lowercase()
            if (dictionary.containsKey(lowercaseW)) {
                val replacement = dictionary[lowercaseW]!!
                val finalReplacement = if (w.firstOrNull()?.isUpperCase() == true) {
                    replacement.substring(0, 1).uppercase() + replacement.substring(1)
                } else {
                    replacement
                }
                if (list.none { it.original == w && it.targetField == fieldName }) {
                    list.add(
                        SpellSuggestion(
                            type = "Spelling",
                            original = w,
                            replacement = finalReplacement,
                            explanation = "Possible typo: '$w'",
                            targetField = fieldName
                        )
                    )
                }
            }
        }

        // 3. Sentence capitalization checks
        val sentenceRegex = Regex("(?:^|[.!?]\\s+)([a-z])")
        sentenceRegex.findAll(text).forEach { match ->
            val lowercaseStart = match.groupValues[1]
            val capitalized = lowercaseStart.uppercase()
            val fullMatch = match.value
            val replacement = fullMatch.replaceFirst(lowercaseStart, capitalized)
            list.add(
                SpellSuggestion(
                    type = "Grammar",
                    original = fullMatch,
                    replacement = replacement,
                    explanation = "Capitalize sentence starter: '$lowercaseStart'",
                    targetField = fieldName
                )
            )
        }

        // 4. Punctuation spacing
        val spaceBeforePunct = Regex("\\s+([,;.!?])")
        spaceBeforePunct.findAll(text).forEach { match ->
            val punct = match.groupValues[1]
            list.add(
                SpellSuggestion(
                    type = "Grammar",
                    original = match.value,
                    replacement = punct,
                    explanation = "No space before '$punct'",
                    targetField = fieldName
                )
            )
        }
    }

    checkField(description, "description")
    checkField(content, "content")
    return list
}
