package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GistWithFiles
import com.example.ui.theme.ActivePurple
import com.example.ui.theme.GraySecondary

@Composable
fun UserAvatarPlaceholder(login: String, size: androidx.compose.ui.unit.Dp = 36.dp) {
  val initial = login.take(1).uppercase()
  Box(
    modifier =
      Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = initial,
      color = MaterialTheme.colorScheme.onPrimaryContainer,
      fontWeight = FontWeight.Bold,
      fontSize = (size.value * 0.45f).sp
    )
  }
}

fun formatGistDate(isoString: String): String {
  return try {
    val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
    parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
    val date = parser.parse(isoString) ?: return isoString
    val formatter =
      java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault())
    formatter.format(date)
  } catch (e: Exception) {
    isoString
  }
}

@Composable
fun DetailedCreationInfoCard(item: GistWithFiles) {
  Card(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    border = borderButtonStroke(),
    shape = RoundedCornerShape(12.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
      )
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
      ) {
        UserAvatarPlaceholder(login = item.gist.ownerLogin, size = 40.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "@${item.gist.ownerLogin}",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = if (item.gist.isPublic) "Public Gist" else "Secret Gist",
            fontSize = 11.sp,
            color = GraySecondary
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Spacer(modifier = Modifier.height(12.dp))

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Created", fontSize = 12.sp, color = GraySecondary)
          Text(
            formatGistDate(item.gist.createdAt),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Updated", fontSize = 12.sp, color = GraySecondary)
          Text(
            formatGistDate(item.gist.updatedAt),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        if (item.gist.htmlUrl.isNotEmpty()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Web URL", fontSize = 12.sp, color = GraySecondary)
            Text(
              text = item.gist.htmlUrl,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = ActivePurple,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false).padding(start = 16.dp)
            )
          }
        }
      }
    }
  }
}
