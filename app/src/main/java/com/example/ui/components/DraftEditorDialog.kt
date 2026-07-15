package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ActivePurple
import com.example.ui.theme.GraySecondary
import kotlinx.coroutines.delay

@Composable
fun DraftEditorDialog(
  show: Boolean,
  editingGistId: String?,
  onDismiss: () -> Unit,
  onSave:
    (
      description: String,
      files: List<Pair<String, String>>,
      isPublic: Boolean,
      isPinned: Boolean,
      tags: List<String>
    ) -> Unit,
  initialDescription: String = "",
  initialFiles: List<Pair<String, String>> = emptyList(),
  initialIsPublic: Boolean = true,
  initialIsPinned: Boolean = false,
  initialTags: List<String> = emptyList(),
  isAnalyzing: Boolean = false,
  aiAnalysis: GistAiAnalysis? = null,
  onAnalyzeClick: (description: String, files: List<Pair<String, String>>) -> Unit = { _, _ -> },
  onClearAiClick: () -> Unit = {},
  onAutoSave:
    (
      description: String,
      files: List<Pair<String, String>>,
      isPublic: Boolean,
      isPinned: Boolean,
      tags: List<String>
    ) -> Unit =
    { _, _, _, _, _ ->
    },
  onLoadAutoSave: () -> com.example.data.local.pref.AutoSavedDraft? = { null },
  onClearAutoSave: () -> Unit = {}
) {
  if (!show) return

  var description by remember { mutableStateOf(initialDescription) }
  // Represent dynamic files list, ensuring at least one empty file template exists on new creations
  var files by remember {
    mutableStateOf(
      if (initialFiles.isEmpty() || (initialFiles.size == 1 && initialFiles[0].first.isEmpty())) {
        listOf("gistfile1.md" to (initialFiles.firstOrNull()?.second ?: ""))
      } else {
        initialFiles
      }
    )
  }
  var isPublic by remember { mutableStateOf(initialIsPublic) }
  var isPinned by remember { mutableStateOf(initialIsPinned) }
  var tagsInput by remember { mutableStateOf(initialTags.joinToString(", ")) }

  // Detect if there's an auto-saved draft on open
  var autoSavedDraft by remember {
    mutableStateOf<com.example.data.local.pref.AutoSavedDraft?>(null)
  }
  var showRestorePrompt by remember { mutableStateOf(false) }

  LaunchedEffect(editingGistId) {
    val saved = onLoadAutoSave()
    if (saved != null) {
      val initialFilesStandardized =
        if (initialFiles.isEmpty()) listOf("gistfile1.md" to "") else initialFiles
      val savedFilesMapped = saved.files.map { it.filename to it.content }
      val hasDiff =
        saved.description != initialDescription ||
          savedFilesMapped != initialFilesStandardized ||
          saved.isPublic != initialIsPublic ||
          saved.isPinned != initialIsPinned ||
          saved.tags != initialTags
      if (hasDiff) {
        autoSavedDraft = saved
        showRestorePrompt = true
      }
    }
  }

  val onRestore = {
    autoSavedDraft?.let { saved ->
      description = saved.description
      files = saved.files.map { it.filename to it.content }
      isPublic = saved.isPublic
      isPinned = saved.isPinned
      tagsInput = saved.tags.joinToString(", ")
    }
    showRestorePrompt = false
  }

  val onDiscard = {
    onClearAutoSave()
    showRestorePrompt = false
  }

  // Keep track of the last saved state to avoid redundant saving
  val initialKey = remember {
    val initialFilesStandardized =
      if (initialFiles.isEmpty()) listOf("gistfile1.md" to "") else initialFiles
    "$initialDescription|$initialFilesStandardized|$initialIsPublic|$initialIsPinned|${initialTags.joinToString(", ")}"
  }
  var lastSavedState by remember { mutableStateOf<String?>(initialKey) }
  var isSavedIndicatorVisible by remember { mutableStateOf(false) }

  LaunchedEffect(description, files, isPublic, isPinned, tagsInput) {
    // Debounce of 3 seconds: wait for the user to pause typing before auto-saving
    delay(3000)

    val currentStateKey = "$description|$files|$isPublic|$isPinned|$tagsInput"
    if (currentStateKey != lastSavedState) {
      val parsedTags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
      onAutoSave(description, files, isPublic, isPinned, parsedTags)
      lastSavedState = currentStateKey
      isSavedIndicatorVisible = true
      delay(2000)
      isSavedIndicatorVisible = false
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.background)
          .statusBarsPadding()
          .navigationBarsPadding(),
      color = MaterialTheme.colorScheme.background
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Reactive spelling, grammar, and link checking
        val qualitySuggestions =
          remember(description, files) { getContentSuggestions(description, files) }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
          ) {
            Text(
              text = if (editingGistId == null) "New Local Draft" else "Edit Local Draft",
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground
            )
            if (isSavedIndicatorVisible) {
              Spacer(modifier = Modifier.width(8.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Auto-saved",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = "Auto-saved",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }
          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
          }
        }

        if (showRestorePrompt && autoSavedDraft != null) {
          Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("autosave_banner"),
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
              ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Auto-saved draft found",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Would you like to restore your unsaved edits?",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
              }
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                TextButton(onClick = onDiscard, modifier = Modifier.testTag("autosave_discard")) {
                  Text("Discard", fontSize = 13.sp)
                }
                Button(
                  onClick = onRestore,
                  shape = RoundedCornerShape(8.dp),
                  colors =
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  modifier = Modifier.testTag("autosave_restore")
                ) {
                  Text("Restore", fontSize = 13.sp)
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val textFieldColors =
          OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            unfocusedPlaceholderColor =
              MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
          )

        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Description Input Block - Multi-line with raised height
          item {
            OutlinedTextField(
              value = description,
              onValueChange = { description = it },
              label = { Text("Gist Description") },
              modifier =
                Modifier.fillMaxWidth().heightIn(min = 100.dp).testTag("editor_description"),
              singleLine = false,
              maxLines = 10,
              keyboardOptions =
                KeyboardOptions(
                  capitalization = KeyboardCapitalization.Sentences,
                  autoCorrectEnabled = true,
                  keyboardType = KeyboardType.Text
                ),
              colors = textFieldColors,
              shape = RoundedCornerShape(12.dp)
            )
          }

          // Tags Input Block - Comma separated tags
          item {
            OutlinedTextField(
              value = tagsInput,
              onValueChange = { tagsInput = it },
              label = { Text("Tags (comma-separated, e.g. kotlin, architecture)") },
              modifier = Modifier.fillMaxWidth().testTag("editor_tags"),
              singleLine = true,
              keyboardOptions =
                KeyboardOptions(
                  capitalization = KeyboardCapitalization.None,
                  autoCorrectEnabled = false,
                  keyboardType = KeyboardType.Text
                ),
              colors = textFieldColors,
              shape = RoundedCornerShape(12.dp)
            )
          }

          // Stacked Multi-File Cards
          item {
            Text(
              text = "Gist Files (${files.size})",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }

          items(files.size) { index ->
            val file = files[index]
            DraftEditorFileCard(
              index = index,
              filename = file.first,
              content = file.second,
              totalFiles = files.size,
              onFilenameChange = { newName ->
                files =
                  files.mapIndexed { idx, item ->
                    if (idx == index) newName to item.second else item
                  }
              },
              onContentChange = { newContent ->
                files =
                  files.mapIndexed { idx, item ->
                    if (idx == index) item.first to newContent else item
                  }
              },
              onRemove = { files = files.filterIndexed { idx, _ -> idx != index } }
            )
          }

          // Add File Trigger Button
          item {
            Column(modifier = Modifier.fillMaxWidth()) {
              Button(
                onClick = { files = files + ("" to "") },
                enabled = true,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                  ),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_file_btn")
              ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add File")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add File", fontWeight = FontWeight.Bold)
              }
            }
          }

          // spelling, grammar, and link checkers suggestions
          if (qualitySuggestions.isNotEmpty()) {
            item {
              ContentQualityAssistantView(
                qualitySuggestions = qualitySuggestions,
                onFixSuggestion = { suggestion ->
                  if (suggestion.targetField == "description") {
                    description =
                      description.replaceFirst(suggestion.original, suggestion.replacement)
                  } else {
                    files =
                      files.map { (name, content) ->
                        if (
                          name == suggestion.targetField || content.contains(suggestion.original)
                        ) {
                          name to content.replaceFirst(suggestion.original, suggestion.replacement)
                        } else {
                          name to content
                        }
                      }
                  }
                },
                onFixAllSuggestions = {
                  var currentDesc = description
                  var currentFiles = files
                  qualitySuggestions.forEach { suggestion ->
                    if (suggestion.targetField == "description") {
                      currentDesc =
                        currentDesc.replaceFirst(suggestion.original, suggestion.replacement)
                    } else {
                      currentFiles =
                        currentFiles.map { (name, content) ->
                          if (
                            name == suggestion.targetField || content.contains(suggestion.original)
                          ) {
                            name to
                              content.replaceFirst(suggestion.original, suggestion.replacement)
                          } else {
                            name to content
                          }
                        }
                    }
                  }
                  description = currentDesc
                  files = currentFiles
                }
              )
            }
          }

          // Hybrid Local LLM Gist AI Assistant Card
          item {
            GistAiAssistantCardView(
              isAnalyzing = isAnalyzing,
              aiAnalysis = aiAnalysis,
              onAnalyzeClick = { onAnalyzeClick(description, files) },
              onClearAiClick = onClearAiClick,
              onAppendRecommendedTag = { tag ->
                if (!description.contains(tag)) {
                  description = if (description.isBlank()) tag else "$description $tag"
                }
              },
              onApplyFixes = {
                var currentDesc = description
                var currentFiles = files

                val hasPrintFix =
                  aiAnalysis?.optimizationSuggestions?.any {
                    it.contains("Remove debug print/log")
                  } == true
                val hasStateFix =
                  aiAnalysis?.optimizationSuggestions?.any { it.contains("mutableStateOf") } == true
                val hasExtensionFix =
                  aiAnalysis?.optimizationSuggestions?.any {
                    it.contains("files do not have an extension") ||
                      it.contains("do not have an extension")
                  } == true

                currentFiles =
                  currentFiles.map { (name, content) ->
                    var fixedContent = content
                    if (hasPrintFix) {
                      fixedContent =
                        fixedContent
                          .lines()
                          .filter { line ->
                            !line.contains("println(") &&
                              !line.contains("System.out.print") &&
                              !line.contains("Log.d(") &&
                              !line.contains("Log.e(")
                          }
                          .joinToString("\n")
                    }
                    if (hasStateFix) {
                      val mutableStateRegex =
                        Regex("(?<!remember\\s*\\{\\s*)mutableStateOf\\((.*?)\\)")
                      fixedContent =
                        mutableStateRegex.replace(fixedContent) { matchResult ->
                          "remember { mutableStateOf(${matchResult.groupValues[1]}) }"
                        }
                    }
                    var fixedName = name
                    if (hasExtensionFix) {
                      val nonExtensionSpecialNames =
                        setOf(
                          "makefile",
                          "dockerfile",
                          "license",
                          "jenkinsfile",
                          "vagrantfile",
                          "readme"
                        )
                      if (
                        !name.contains(".") && !nonExtensionSpecialNames.contains(name.lowercase())
                      ) {
                        fixedName =
                          if (fixedContent.contains("@Composable") || fixedContent.contains("fun "))
                            "$name.kt"
                          else "$name.txt"
                      }
                    }
                    fixedName to fixedContent
                  }

                aiAnalysis?.recommendedTags?.forEach { tag ->
                  if (!currentDesc.contains(tag)) {
                    currentDesc = if (currentDesc.isBlank()) tag else "$currentDesc $tag"
                  }
                }

                description = currentDesc
                files = currentFiles
              }
            )
          }

          // Metadata visibility configurations (Public & Pinned)
          item {
            DraftEditorSwitchRow(
              title = "Public Gist",
              description = "Decides visibility status of your snippet.",
              checked = isPublic,
              onCheckedChange = { isPublic = it }
            )
          }

          item {
            DraftEditorSwitchRow(
              title = "Pin to Favorites",
              description = "Pin this gist locally on top of your vault list.",
              checked = isPinned,
              onCheckedChange = { isPinned = it }
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(
            onClick = onDismiss,
            colors = ButtonDefaults.textButtonColors(contentColor = GraySecondary)
          ) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(12.dp))
          Button(
            onClick = {
              val parsedTags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
              onSave(description, files, isPublic, isPinned, parsedTags)
            },
            colors = ButtonDefaults.buttonColors(containerColor = ActivePurple),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Save Draft", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
