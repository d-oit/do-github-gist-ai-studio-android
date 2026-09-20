package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.TokenVerificationState

/** Clickable status badge/chip displaying PAT authentication state. */
@Composable
fun AuthStatusIndicatorChip(
  token: String,
  ownerLogin: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isAuthenticated = token.trim().isNotEmpty()
  val statusColor = if (isAuthenticated) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
  val labelText =
    if (isAuthenticated && ownerLogin.isNotEmpty() && ownerLogin != "anonymous") {
      "@$ownerLogin"
    } else if (isAuthenticated) {
      "PAT Active"
    } else {
      "No PAT"
    }

  Surface(
    modifier =
      modifier
        .clip(RoundedCornerShape(16.dp))
        .clickable(onClick = onClick)
        .testTag("auth_status_chip"),
    shape = RoundedCornerShape(16.dp),
    color = statusColor.copy(alpha = 0.12f),
    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
      Icon(
        imageVector = if (isAuthenticated) Icons.Default.Key else Icons.Default.KeyOff,
        contentDescription = "Auth Status",
        tint = statusColor,
        modifier = Modifier.size(14.dp)
      )
      Text(text = labelText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
    }
  }
}

/** Dialog displaying GitHub PAT authentication status with option to update or clear the token. */
@Composable
fun AuthStatusDialog(
  show: Boolean,
  token: String,
  ownerLogin: String,
  verificationState: TokenVerificationState,
  isVerifying: Boolean,
  onUpdateToken: (String) -> Unit,
  onVerifyToken: () -> Unit,
  onClearToken: () -> Unit,
  onDismiss: () -> Unit
) {
  if (!show) return

  var inputToken by remember(token) { mutableStateOf(token) }
  var isTokenVisible by remember { mutableStateOf(false) }

  val isAuthenticated = token.trim().isNotEmpty()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = if (isAuthenticated) Icons.Default.Key else Icons.Default.KeyOff,
          contentDescription = null,
          tint = if (isAuthenticated) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
          modifier = Modifier.size(24.dp)
        )
        Text(
          text = "GitHub PAT Authentication",
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Current Status Card
        Surface(
          shape = RoundedCornerShape(12.dp),
          color =
            if (isAuthenticated) Color(0xFF2E7D32).copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
          border =
            androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isAuthenticated) Color(0xFF2E7D32).copy(alpha = 0.3f)
              else MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            ),
          modifier = Modifier.fillMaxWidth().testTag("auth_status_info_card")
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector =
                if (isAuthenticated) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
              contentDescription = null,
              tint = if (isAuthenticated) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
              modifier = Modifier.size(22.dp)
            )
            Column {
              Text(
                text = if (isAuthenticated) "Authenticated" else "Unauthenticated (No Token)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text =
                  if (isAuthenticated) {
                    if (ownerLogin.isNotEmpty() && ownerLogin != "anonymous")
                      "Linked as @$ownerLogin"
                    else "PAT token set"
                  } else {
                    "Provide a GitHub Personal Access Token with 'gist' scope to enable cloud synchronization."
                  },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Text(
          text = "Personal Access Token (PAT)",
          fontWeight = FontWeight.SemiBold,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
          value = inputToken,
          onValueChange = {
            inputToken = it
            onUpdateToken(it)
          },
          placeholder = { Text("ghp_...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("auth_token_input"),
          visualTransformation =
            if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(
              onClick = { isTokenVisible = !isTokenVisible },
              modifier = Modifier.testTag("auth_toggle_visibility")
            ) {
              Icon(
                imageVector =
                  if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (isTokenVisible) "Hide token" else "Show token",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          },
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
          shape = RoundedCornerShape(12.dp)
        )

        // Verification Status Feedback
        AnimatedVisibility(visible = verificationState !is TokenVerificationState.Idle) {
          when (verificationState) {
            is TokenVerificationState.Verifying -> {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.primary
                )
                Text(
                  text = "Verifying token with GitHub...",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
            is TokenVerificationState.Success -> {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = Color(0xFF2E7D32),
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "Token verified & saved successfully!",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium,
                  color = Color(0xFF2E7D32)
                )
              }
            }
            is TokenVerificationState.Error -> {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.ErrorOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = verificationState.message,
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.error
                )
              }
            }
            else -> {}
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = onVerifyToken,
            enabled = !isVerifying && inputToken.trim().isNotEmpty(),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).height(44.dp).testTag("auth_save_button")
          ) {
            if (isVerifying) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
              )
            } else {
              Text("Verify & Save Token", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }

          if (isAuthenticated) {
            OutlinedButton(
              onClick = onClearToken,
              colors =
                ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(44.dp).testTag("auth_clear_button")
            ) {
              Text("Clear", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.testTag("auth_close_button")) {
        Text("Done", fontWeight = FontWeight.Bold)
      }
    },
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(28.dp),
    modifier = Modifier.testTag("auth_status_dialog")
  )
}
