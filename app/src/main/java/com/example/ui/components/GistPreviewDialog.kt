package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.GistWithFiles
import com.example.ui.theme.ActivePurple
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.GrayTertiary
import com.example.ui.theme.SlateBg

@Composable
fun GistPreviewDialog(
    show: Boolean,
    item: GistWithFiles?,
    viewModel: com.example.ui.viewmodel.GistViewModel,
    onDismiss: () -> Unit
) {
    if (!show || item == null) return

    var activeTab by remember { mutableStateOf("files") }
    var historyList by remember { mutableStateOf<List<com.example.data.remote.model.GistHistoryResponse>?>(null) }
    var isLoadingHistory by remember { mutableStateOf(false) }
    var historyError by remember { mutableStateOf<String?>(null) }

    var selectedRevisionSha by remember { mutableStateOf<String?>(null) }
    var diffViewMode by remember { mutableStateOf("unified") } // "unified" or "split"
    
    var isLoadingRevisionContent by remember { mutableStateOf(false) }
    var revisionContentError by remember { mutableStateOf<String?>(null) }
    var currentRevisionGist by remember { mutableStateOf<com.example.data.remote.model.GistResponse?>(null) }
    var parentRevisionGist by remember { mutableStateOf<com.example.data.remote.model.GistResponse?>(null) }

    LaunchedEffect(item.gist.id) {
        if (item.gist.isLocalOnly) {
            historyError = "Revisions are not available for local-only Gists."
            return@LaunchedEffect
        }
        isLoadingHistory = true
        historyError = null
        viewModel.getRemoteGistDetails(item.gist.id)
            .onSuccess { details ->
                historyList = details.history
                if (details.history.isNullOrEmpty()) {
                    historyError = "No revision history found for this Gist."
                }
            }
            .onFailure { err ->
                historyError = "Failed to load revisions: ${err.message}"
            }
        isLoadingHistory = false
    }

    LaunchedEffect(selectedRevisionSha) {
        val sha = selectedRevisionSha
        if (sha == null) {
            currentRevisionGist = null
            parentRevisionGist = null
            return@LaunchedEffect
        }
        
        isLoadingRevisionContent = true
        revisionContentError = null
        currentRevisionGist = null
        parentRevisionGist = null
        
        viewModel.getRemoteGistRevision(item.gist.id, sha)
            .onSuccess { curGist ->
                currentRevisionGist = curGist
                val currentIdx = historyList?.indexOfFirst { it.version == sha } ?: -1
                val parentRev = if (currentIdx != -1 && currentIdx + 1 < (historyList?.size ?: 0)) {
                    historyList?.get(currentIdx + 1)
                } else {
                    null
                }
                
                if (parentRev != null && parentRev.version != null) {
                    viewModel.getRemoteGistRevision(item.gist.id, parentRev.version)
                        .onSuccess { parGist ->
                            parentRevisionGist = parGist
                        }
                        .onFailure {
                            parentRevisionGist = null
                        }
                } else {
                    parentRevisionGist = null
                }
            }
            .onFailure { err ->
                revisionContentError = "Failed to load revision files: ${err.message}"
            }
        isLoadingRevisionContent = false
    }

    val filesToCompare = remember(currentRevisionGist, parentRevisionGist) {
        val curFiles = currentRevisionGist?.files ?: emptyMap()
        val parFiles = parentRevisionGist?.files ?: emptyMap()
        val allNames = (curFiles.keys + parFiles.keys).toSet().toList().sorted()
        allNames.map { name ->
            val cur = curFiles[name]
            val par = parFiles[name]
            Triple(name, par?.content ?: "", cur?.content ?: "")
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
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
                        text = "Gist Snippet Preview",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Navigation Tabs (Files / Revisions)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("Files" to Icons.Default.Description, "Revisions" to Icons.Default.History).forEach { (tabName, icon) ->
                        val isSelected = activeTab == tabName.lowercase()
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ActivePurple.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) ActivePurple else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { 
                                    activeTab = tabName.lowercase()
                                    if (tabName.lowercase() == "revisions") {
                                        selectedRevisionSha = null
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = tabName,
                                tint = if (isSelected) ActivePurple else GraySecondary,
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

                Spacer(modifier = Modifier.height(12.dp))

                if (activeTab == "files") {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            DetailedCreationInfoCard(item = item)
                        }

                        item {
                            Column {
                                Text(
                                    text = "Description",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.gist.description ?: "No description provided",
                                    fontSize = 14.sp,
                                    color = GraySecondary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }

                        items(item.files.size) { idx ->
                            val file = item.files[idx]
                            val isMarkdown = file.filename.endsWith(".md", ignoreCase = true) || 
                                             file.filename.endsWith(".markdown", ignoreCase = true)
                            
                            var previewMode by remember { mutableStateOf(if (isMarkdown) "markdown" else "raw") }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = borderButtonStroke(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = file.filename,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        
                                        if (isMarkdown) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(
                                                    onClick = { previewMode = "raw" },
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text(
                                                        text = "Raw",
                                                        fontSize = 11.sp,
                                                        fontWeight = if (previewMode == "raw") FontWeight.Bold else FontWeight.Normal,
                                                        color = if (previewMode == "raw") MaterialTheme.colorScheme.primary else GraySecondary
                                                    )
                                                }
                                                TextButton(
                                                    onClick = { previewMode = "markdown" },
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text(
                                                        text = "Markdown",
                                                        fontSize = 11.sp,
                                                        fontWeight = if (previewMode == "markdown") FontWeight.Bold else FontWeight.Normal,
                                                        color = if (previewMode == "markdown") MaterialTheme.colorScheme.primary else GraySecondary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (isMarkdown && previewMode == "markdown") {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                                .padding(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            MarkdownText(text = file.content)
                                        }
                                    } else {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 100.dp, max = 400.dp)
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E1E1E)
                                        ) {
                                            LazyColumn(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp)
                                            ) {
                                                item {
                                                    val highlightedText = remember(file.content, file.filename) {
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
                    }
                } else {
                    // Revisions Tab
                    val currentRevisionSha = selectedRevisionSha
                    if (currentRevisionSha == null) {
                        if (isLoadingHistory) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = ActivePurple)
                            }
                        } else if (historyError != null) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = historyError ?: "",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            val hist = historyList
                            if (hist != null) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    RevisionHistoryListView(
                                        historyList = hist,
                                        defaultOwnerLogin = item.gist.ownerLogin,
                                        onSelectRevision = { selectedRevisionSha = it }
                                    )
                                }
                            }
                        }
                    } else {
                        // Detailed revision view
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            DetailedRevisionChangesView(
                                selectedRevisionSha = currentRevisionSha,
                                diffViewMode = diffViewMode,
                                onDiffViewModeChange = { diffViewMode = it },
                                isLoadingRevisionContent = isLoadingRevisionContent,
                                revisionContentError = revisionContentError,
                                filesToCompare = filesToCompare,
                                onBack = { selectedRevisionSha = null }
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
                        onClick = onDismiss,
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

