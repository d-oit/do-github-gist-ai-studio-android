package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GistViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGistScreen(viewModel: GistViewModel, onBack: () -> Unit, onSaveSuccess: () -> Unit) {
  var description by remember { mutableStateOf("") }
  var filename by remember { mutableStateOf("gistfile1.md") }
  var content by remember { mutableStateOf("") }
  var isPublic by remember { mutableStateOf(true) }
  var tagsInput by remember { mutableStateOf("") }

  var filenameError by remember { mutableStateOf<String?>(null) }
  var contentError by remember { mutableStateOf<String?>(null) }

  val ownerLogin by viewModel.ownerLogin.collectAsState()
  val ownerAvatarUrl by viewModel.ownerAvatar.collectAsState()

  val scope = rememberCoroutineScope()
  val scrollState = rememberScrollState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Create New Gist",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("create_gist_back_button")) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Go Back",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        },
        modifier = Modifier.statusBarsPadding()
      )
    }
  ) { paddingValues ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(paddingValues)
          .padding(horizontal = 16.dp)
          .verticalScroll(scrollState),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Spacer(modifier = Modifier.height(4.dp))

      // Filename field
      OutlinedTextField(
        value = filename,
        onValueChange = {
          filename = it
          if (it.isBlank()) {
            filenameError = "Filename cannot be empty"
          } else {
            filenameError = null
          }
        },
        label = { Text("Filename") },
        isError = filenameError != null,
        supportingText = {
          if (filenameError != null) {
            Text(filenameError!!, color = MaterialTheme.colorScheme.error)
          } else {
            Text("Provide a file name with extension (e.g. script.kt)")
          }
        },
        modifier = Modifier.fillMaxWidth().testTag("create_gist_filename_input"),
        singleLine = true
      )

      // Description field
      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("Description (Optional)") },
        placeholder = { Text("Describe what this gist is about...") },
        modifier = Modifier.fillMaxWidth().testTag("create_gist_description_input"),
        maxLines = 3
      )

      // Tags field
      OutlinedTextField(
        value = tagsInput,
        onValueChange = { tagsInput = it },
        label = { Text("Tags (comma-separated, e.g. kotlin, architecture)") },
        placeholder = { Text("Enter tags to categorize...") },
        modifier = Modifier.fillMaxWidth().testTag("create_gist_tags_input"),
        singleLine = true
      )

      // Public status switch
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Public Gist",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "If enabled, this Gist will be visible to everyone on GitHub.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Switch(
          checked = isPublic,
          onCheckedChange = { isPublic = it },
          modifier = Modifier.testTag("create_gist_public_switch")
        )
      }

      // Content field
      OutlinedTextField(
        value = content,
        onValueChange = {
          content = it
          if (it.isBlank()) {
            contentError = "Content cannot be empty"
          } else {
            contentError = null
          }
        },
        label = { Text("File Content") },
        placeholder = { Text("Write your snippet code or notes here...") },
        isError = contentError != null,
        supportingText = {
          if (contentError != null) {
            Text(contentError!!, color = MaterialTheme.colorScheme.error)
          }
        },
        modifier = Modifier.fillMaxWidth().height(240.dp).testTag("create_gist_content_input"),
        maxLines = 150
      )

      // Save Button
      Button(
        onClick = {
          var hasError = false
          if (filename.isBlank()) {
            filenameError = "Filename cannot be empty"
            hasError = true
          }
          if (content.isBlank()) {
            contentError = "Content cannot be empty"
            hasError = true
          }

          if (!hasError) {
            val parsedTags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            scope.launch {
              viewModel.createGist(
                description = description,
                filename = filename,
                content = content,
                isPublic = isPublic,
                isPinned = false,
                tags = parsedTags
              )
              onSaveSuccess()
            }
          }
        },
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("create_gist_save_button")
      ) {
        Text(text = "Save Locally", fontWeight = FontWeight.Bold, fontSize = 16.sp)
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
