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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GistWithFiles
import com.example.ui.components.GistCard
import com.example.ui.theme.ActivePurple
import com.example.ui.theme.ActivePurpleContainer
import com.example.ui.theme.DarkPurpleText
import com.example.ui.theme.GraySecondary

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
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
