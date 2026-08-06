package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GistWithFiles

/**
 * Dedicated Screen for creating a new Gist or editing an existing Gist. Includes fields for
 * Title/Filename, Description, and File Content. Integrates Room AES-256 GistContentEncryptor logic
 * for local encrypted persistence.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditGistScreen(
  existingGist: GistWithFiles? = null,
  onBack: () -> Unit,
  onSave:
    (
      title: String,
      description: String,
      content: String,
      isPublic: Boolean,
      isPinned: Boolean,
      tags: List<String>
    ) -> Unit
) {
  val initialTitle = existingGist?.files?.firstOrNull()?.filename ?: "main.kt"
  val initialDescription = existingGist?.gist?.description ?: ""
  val initialContent = existingGist?.files?.firstOrNull()?.content ?: ""
  val initialIsPublic = existingGist?.gist?.isPublic ?: true
  val initialIsPinned = existingGist?.gist?.isPinned ?: false
  val initialTags = existingGist?.gist?.tags ?: emptyList()

  var title by remember { mutableStateOf(initialTitle) }
  var description by remember { mutableStateOf(initialDescription) }
  var content by remember { mutableStateOf(initialContent) }
  var isPublic by remember { mutableStateOf(initialIsPublic) }
  var isPinned by remember { mutableStateOf(initialIsPinned) }
  var tagsInput by remember { mutableStateOf(initialTags.joinToString(", ")) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  Scaffold(
    modifier = Modifier.fillMaxSize().testTag("create_edit_gist_screen"),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (existingGist == null) "Create Gist" else "Edit Gist",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("create_edit_screen_title")
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("create_edit_back_button")) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          Button(
            onClick = {
              if (title.isBlank()) {
                errorMessage = "Filename/Title cannot be empty"
                return@Button
              }
              if (content.isBlank()) {
                errorMessage = "Content cannot be empty"
                return@Button
              }
              val tagsList = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
              onSave(title.trim(), description.trim(), content, isPublic, isPinned, tagsList)
            },
            modifier = Modifier.testTag("create_edit_save_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            Icon(
              imageVector = Icons.Default.Save,
              contentDescription = "Save Gist",
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save")
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    }
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .padding(16.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Room AES-256 Encryption Security Card
      Card(
        modifier = Modifier.fillMaxWidth().testTag("create_edit_encryption_card"),
        shape = RoundedCornerShape(12.dp),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Room Encryption",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "AES-256 Room Storage Encryption",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.testTag("create_edit_encryption_badge")
            )
            Text(
              text =
                "Content will be automatically encrypted before storing in local Room database.",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
          }
        }
      }

      if (errorMessage != null) {
        Text(
          text = errorMessage!!,
          color = MaterialTheme.colorScheme.error,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.testTag("create_edit_error_message")
        )
      }

      // Title / Filename Field
      OutlinedTextField(
        value = title,
        onValueChange = {
          title = it
          errorMessage = null
        },
        label = { Text("Title / Filename (e.g. snippet.kt)") },
        modifier = Modifier.fillMaxWidth().testTag("create_edit_title_input"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
      )

      // Description Field
      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("Gist Description") },
        modifier = Modifier.fillMaxWidth().testTag("create_edit_description_input"),
        singleLine = false,
        maxLines = 4,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        shape = RoundedCornerShape(12.dp)
      )

      // Tags Field
      OutlinedTextField(
        value = tagsInput,
        onValueChange = { tagsInput = it },
        label = { Text("Tags (comma separated, e.g. kotlin, room, security)") },
        modifier = Modifier.fillMaxWidth().testTag("create_edit_tags_input"),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
      )

      // File Content Code Field
      OutlinedTextField(
        value = content,
        onValueChange = {
          content = it
          errorMessage = null
        },
        label = { Text("Gist Code / Content") },
        modifier = Modifier.fillMaxWidth().height(220.dp).testTag("create_edit_content_input"),
        textStyle =
          MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
          ),
        shape = RoundedCornerShape(12.dp)
      )

      // Public / Private Switch
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = if (isPublic) "Public Gist" else "Secret Gist",
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.sp
            )
            Text(
              text =
                if (isPublic) "Visible publicly on GitHub" else "Only accessible via secret URL",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = isPublic,
            onCheckedChange = { isPublic = it },
            modifier = Modifier.testTag("create_edit_public_switch"),
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
          )
        }
      }
    }
  }
}
