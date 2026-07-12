package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GistWithFiles
import com.example.ui.components.GistCard
import com.example.ui.theme.ActivePurple
import com.example.ui.theme.ActivePurpleContainer
import com.example.ui.theme.DarkPurpleText

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    gists: List<GistWithFiles>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedTag: String? = null,
    allTags: List<String> = emptyList(),
    onSelectedTagChange: (String?) -> Unit = {},
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleStar: (String) -> Unit,
    onEdit: (GistWithFiles) -> Unit,
    onDelete: (String) -> Unit,
    onPreview: (GistWithFiles) -> Unit
) {
    val filtered = remember(gists, searchQuery) {
        if (searchQuery.isBlank()) {
            gists.sortedWith(compareByDescending<GistWithFiles> { it.gist.isPinned }.thenByDescending { it.gist.createdAt })
        } else {
            gists.filter { item ->
                val desc = item.gist.description ?: ""
                val matchesDescription = desc.contains(searchQuery, ignoreCase = true)
                val matchesFiles = item.files.any { file ->
                    file.filename.contains(searchQuery, ignoreCase = true) ||
                            file.content.contains(searchQuery, ignoreCase = true)
                }
                matchesDescription || matchesFiles
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
            // Persistent Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search gists by filename or description...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp)
                    .testTag("home_search_bar"),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                )
            )

            // Horizontal Scrollable Tag Filters Row
            if (allTags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = selectedTag == null,
                            onClick = { onSelectedTagChange(null) },
                            label = { Text("All", fontSize = 12.sp) },
                            modifier = Modifier.testTag("tag_filter_all")
                        )
                    }
                    items(allTags) { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = {
                                if (selectedTag == tag) {
                                    onSelectedTagChange(null)
                                } else {
                                    onSelectedTagChange(tag)
                                }
                            },
                            label = { Text("#$tag", fontSize = 12.sp) },
                            modifier = Modifier.testTag("tag_filter_$tag")
                        )
                    }
                }
            }

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
                        text = "${filtered.size} Items",
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
                            onToggleStar = { onToggleStar(item.gist.id) },
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
