package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SearchHistoryEntity

@Composable
fun SearchHistoryView(
  searchHistory: List<SearchHistoryEntity>,
  onSelectQuery: (String) -> Unit,
  onDeleteQuery: (String) -> Unit,
  onClearHistory: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (searchHistory.isEmpty()) return

  Surface(
    modifier = modifier.fillMaxWidth().testTag("search_history_container"),
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    tonalElevation = 1.dp
  ) {
    Column(modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.History,
            contentDescription = "Search history",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Recent Searches",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag("search_history_header")
          )
        }
        TextButton(
          onClick = onClearHistory,
          modifier = Modifier.testTag("clear_all_search_history_btn")
        ) {
          Text(text = "Clear All", fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
      }

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
      ) {
        items(searchHistory, key = { it.query }) { item ->
          AssistChip(
            onClick = { onSelectQuery(item.query) },
            label = {
              Text(
                text = item.query,
                fontSize = 12.sp,
                maxLines = 1,
                modifier = Modifier.testTag("search_history_text_${item.query}")
              )
            },
            trailingIcon = {
              IconButton(
                onClick = { onDeleteQuery(item.query) },
                modifier = Modifier.size(18.dp).testTag("delete_search_history_item_${item.query}")
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Remove ${item.query} from history",
                  modifier = Modifier.size(12.dp)
                )
              }
            },
            colors =
              AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
                labelColor = MaterialTheme.colorScheme.onSurface
              ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("search_history_chip_${item.query}")
          )
        }
      }
    }
  }
}
