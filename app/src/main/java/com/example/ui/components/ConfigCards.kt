package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun ConfigThemeSelectorCard(
  currentTheme: String,
  onThemeChange: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = stringResource(R.string.config_theme_title),
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = stringResource(R.string.config_theme_desc),
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(16.dp))

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // High Contrast Light
        val isLightSelected = currentTheme == "light"
        Surface(
          modifier =
            Modifier.weight(1f)
              .height(60.dp)
              .border(
                width = if (isLightSelected) 2.dp else 1.dp,
                color =
                  if (isLightSelected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
              )
              .clip(RoundedCornerShape(8.dp))
              .clickable { onThemeChange("light") }
              .testTag("theme_toggle_light"),
          color =
            if (isLightSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        ) {
          Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Visibility,
              contentDescription = stringResource(R.string.config_theme_light_desc),
              tint =
                if (isLightSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column {
              Text(
                text = stringResource(R.string.config_theme_light_title),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color =
                  if (isLightSelected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = stringResource(R.string.config_theme_light_subtitle),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // High Contrast Dark
        val isDarkSelected = currentTheme == "dark"
        Surface(
          modifier =
            Modifier.weight(1f)
              .height(60.dp)
              .border(
                width = if (isDarkSelected) 2.dp else 1.dp,
                color =
                  if (isDarkSelected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
              )
              .clip(RoundedCornerShape(8.dp))
              .clickable { onThemeChange("dark") }
              .testTag("theme_toggle_dark"),
          color =
            if (isDarkSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        ) {
          Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.VisibilityOff,
              contentDescription = stringResource(R.string.config_theme_dark_desc),
              tint =
                if (isDarkSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column {
              Text(
                text = stringResource(R.string.config_theme_dark_title),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color =
                  if (isDarkSelected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = stringResource(R.string.config_theme_dark_subtitle),
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

@Composable
fun ConfigBackupCard(onExport: () -> Unit, onImport: () -> Unit, modifier: Modifier = Modifier) {
  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = stringResource(R.string.backup_title),
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = stringResource(R.string.backup_description),
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(16.dp))

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = onExport,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.weight(1f).height(48.dp).testTag("config_backup_button")
        ) {
          Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = stringResource(R.string.backup_button),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.backup_button),
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
          }
        }

        Button(
          onClick = onImport,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.weight(1f).height(48.dp).testTag("config_import_backup_button")
        ) {
          Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Download,
              contentDescription = stringResource(R.string.backup_import_button),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.backup_import_button),
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
          }
        }
      }
    }
  }
}

@Composable
fun ConfigConnectionStatusCard(
  token: String,
  isRefreshing: Boolean,
  onTestConnection: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surface
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = stringResource(R.string.config_connection_status),
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier =
            Modifier.size(8.dp)
              .clip(CircleShape)
              .background(
                if (token.isNotEmpty()) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
              )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text =
            stringResource(
              if (token.isNotEmpty()) R.string.config_status_configured
              else R.string.config_status_no_token
            ),
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      if (token.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = onTestConnection,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
          if (isRefreshing) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          } else {
            Text(
              text = stringResource(R.string.config_test_connection),
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}
