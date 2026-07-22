package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GistHubTopAppBar(
  isSearchExpanded: Boolean,
  onToggleSearch: () -> Unit,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  isRefreshing: Boolean,
  onRefresh: () -> Unit
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
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
          modifier =
            Modifier.size(40.dp)
              .clip(CircleShape)
              .background(
                brush =
                  Brush.linearGradient(
                    colors =
                      listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                  )
              ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Code,
            contentDescription = "Gist Logo",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp)
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
        IconButton(onClick = onToggleSearch, modifier = Modifier.testTag("search_toggle")) {
          Icon(
            imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
            contentDescription = "Search",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }

        // Refresh/Sync button
        IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh_button")) {
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
          onValueChange = onSearchQueryChange,
          placeholder = { Text("Filter by filename, description, or content...") },
          modifier = Modifier.fillMaxWidth().heightIn(max = 56.dp).testTag("search_field"),
          singleLine = true,
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedTextColor = MaterialTheme.colorScheme.onSurface,
              unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
              focusedPlaceholderColor =
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              unfocusedPlaceholderColor =
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
          shape = RoundedCornerShape(12.dp),
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { onSearchQueryChange("") }) {
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
}

@Composable
fun GistHubBottomBar(activeTab: String, onTabSelected: (String) -> Unit) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
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
      onClick = { onTabSelected("home") },
      modifier = Modifier.testTag("tab_home")
    )
    TabButton(
      label = "Vault",
      icon = Icons.Default.FolderOpen,
      isActive = activeTab == "vault",
      onClick = { onTabSelected("vault") },
      modifier = Modifier.testTag("tab_vault")
    )
    TabButton(
      label = "Sync",
      icon = Icons.Default.Sync,
      isActive = activeTab == "sync",
      onClick = { onTabSelected("sync") },
      modifier = Modifier.testTag("tab_sync")
    )
    TabButton(
      label = "Config",
      icon = Icons.Default.Settings,
      isActive = activeTab == "config",
      onClick = { onTabSelected("config") },
      modifier = Modifier.testTag("tab_config")
    )
  }
}
