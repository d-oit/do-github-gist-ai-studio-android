package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.ConfigBackupCard
import com.example.ui.components.ConfigConnectionStatusCard
import com.example.ui.components.ConfigThemeSelectorCard
import com.example.ui.viewmodel.GistViewModel
import com.example.ui.viewmodel.TokenVerificationState
import com.example.ui.viewmodel.exportBackup
import com.example.ui.viewmodel.importBackup

@Composable
fun ConfigScreen(viewModel: GistViewModel) {
  val token by viewModel.token.collectAsState()
  val ownerLogin by viewModel.ownerLogin.collectAsState()
  val ownerAvatar by viewModel.ownerAvatar.collectAsState()
  val isRefreshing by viewModel.isRefreshing.collectAsState()
  val isFetchingProfile by viewModel.isFetchingProfile.collectAsState()
  val currentTheme by viewModel.appTheme.collectAsState()

  val context = LocalContext.current
  val tokenVerificationState by viewModel.tokenVerificationState.collectAsState()

  LaunchedEffect(tokenVerificationState) {
    if (tokenVerificationState is TokenVerificationState.Success) {
      Toast.makeText(context, context.getString(R.string.verify_success_toast), Toast.LENGTH_LONG)
        .show()
    }
  }

  val keyboardController = LocalSoftwareKeyboardController.current

  val textFieldColors =
    OutlinedTextFieldDefaults.colors(
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

  val exportLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
      if (uri != null) {
        viewModel.exportBackup(context, uri) { success, msg ->
          if (success) {
            Toast.makeText(context, context.getString(R.string.backup_success), Toast.LENGTH_SHORT)
              .show()
          } else {
            Toast.makeText(
                context,
                context.getString(R.string.backup_failed, msg),
                Toast.LENGTH_LONG
              )
              .show()
          }
        }
      }
    }

  val importLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
      if (uri != null) {
        viewModel.importBackup(context, uri) { success, msg, count ->
          if (success) {
            Toast.makeText(
                context,
                context.getString(R.string.backup_import_success, count),
                Toast.LENGTH_SHORT
              )
              .show()
          } else {
            Toast.makeText(
                context,
                context.getString(R.string.backup_import_failed, msg),
                Toast.LENGTH_LONG
              )
              .show()
          }
        }
      }
    }

  LazyColumn(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Text(
        text = stringResource(R.string.config_credentials_title),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )
      Text(
        text = stringResource(R.string.config_credentials_desc),
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
      )
    }

    item {
      OutlinedTextField(
        value = token,
        onValueChange = { viewModel.updateToken(it) },
        label = { Text(stringResource(R.string.config_token_label)) },
        placeholder = { Text(stringResource(R.string.config_token_placeholder)) },
        modifier = Modifier.fillMaxWidth().testTag("config_token_input"),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        colors = textFieldColors,
        shape = RoundedCornerShape(12.dp)
      )
    }

    item {
      val buttonColors =
        when (tokenVerificationState) {
          is TokenVerificationState.Verifying ->
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
              contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
            )
          is TokenVerificationState.Success ->
            ButtonDefaults.buttonColors(
              containerColor = Color(0xFF2E7D32), // Custom 2026 Material success green
              contentColor = Color.White
            )
          is TokenVerificationState.Error ->
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError
            )
          else ->
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }

      val buttonText =
        when (tokenVerificationState) {
          is TokenVerificationState.Verifying -> stringResource(R.string.verify_loading)
          is TokenVerificationState.Success -> stringResource(R.string.verify_success)
          is TokenVerificationState.Error -> stringResource(R.string.verify_failed)
          else -> stringResource(R.string.verify_idle)
        }

      val buttonIcon =
        when (tokenVerificationState) {
          is TokenVerificationState.Success -> Icons.Default.CheckCircle
          is TokenVerificationState.Error -> Icons.Default.ErrorOutline
          else -> Icons.Default.Check
        }

      Button(
        onClick = {
          keyboardController?.hide()
          viewModel.validateAndFetchProfile()
        },
        enabled =
          tokenVerificationState !is TokenVerificationState.Verifying && token.trim().isNotEmpty(),
        colors = buttonColors,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("config_verify_button")
      ) {
        if (tokenVerificationState is TokenVerificationState.Verifying) {
          CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.5.dp
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(buttonText, fontWeight = FontWeight.Bold)
        } else {
          Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = buttonIcon,
              contentDescription = buttonText,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(buttonText, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    item {
      OutlinedTextField(
        value = ownerLogin,
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.config_username_label)) },
        modifier = Modifier.fillMaxWidth().testTag("config_username_input"),
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
        label = { Text(stringResource(R.string.config_avatar_label)) },
        placeholder = { Text(stringResource(R.string.config_avatar_placeholder)) },
        modifier = Modifier.fillMaxWidth().testTag("config_avatar_input"),
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
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
          ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("config_clear_button")
      ) {
        Row(
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(R.string.config_disconnect_desc),
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.config_disconnect_button), fontWeight = FontWeight.Bold)
        }
      }
    }

    item {
      ConfigThemeSelectorCard(
        currentTheme = currentTheme,
        onThemeChange = { viewModel.updateAppTheme(it) }
      )
    }

    item {
      ConfigBackupCard(
        onExport = { exportLauncher.launch("dogisthub_backup.json") },
        onImport = { importLauncher.launch("application/json") }
      )
    }

    item {
      ConfigConnectionStatusCard(
        token = token,
        isRefreshing = isRefreshing,
        onTestConnection = { viewModel.refreshGists(context) }
      )
    }
  }
}
