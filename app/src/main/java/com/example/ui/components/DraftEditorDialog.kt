package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ActivePurple
import com.example.ui.theme.GraySecondary

@Composable
fun DraftEditorDialog(
    show: Boolean,
    editingGistId: String?,
    onDismiss: () -> Unit,
    onSave: (description: String, files: List<Pair<String, String>>, isPublic: Boolean, isPinned: Boolean) -> Unit,
    initialDescription: String = "",
    initialFiles: List<Pair<String, String>> = emptyList(),
    initialIsPublic: Boolean = true,
    initialIsPinned: Boolean = false,
    isAnalyzing: Boolean = false,
    aiAnalysis: GistAiAnalysis? = null,
    onAnalyzeClick: (description: String, files: List<Pair<String, String>>) -> Unit = { _, _ -> },
    onClearAiClick: () -> Unit = {}
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Reactive spelling, grammar, and link checking
                val qualitySuggestions = remember(description, files) {
                    getContentSuggestions(description, files)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (editingGistId == null) "New Local Draft" else "Edit Local Draft",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val textFieldColors = OutlinedTextFieldDefaults.colors(
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

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Description Input Block - Multi-line with raised height
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Gist Description") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp)
                                .testTag("editor_description"),
                            singleLine = false,
                            maxLines = 10,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                autoCorrectEnabled = true,
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

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "File #${index + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (files.size > 1) {
                                        IconButton(
                                            onClick = {
                                                files = files.filterIndexed { idx, _ -> idx != index }
                                            },
                                            modifier = Modifier.testTag("remove_file_$index")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove File",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = file.first,
                                    onValueChange = { newName ->
                                        files = files.mapIndexed { idx, item ->
                                            if (idx == index) newName to item.second else item
                                        }
                                    },
                                    label = { Text("Filename (e.g. script.kt)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("editor_filename_$index"),
                                    singleLine = true,
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                GitHubMarkdownEditor(
                                    value = file.second,
                                    onValueChange = { newContent ->
                                        files = files.mapIndexed { idx, item ->
                                            if (idx == index) item.first to newContent else item
                                        }
                                    },
                                    placeholder = "Leave a comment or Gist file content...",
                                    testTagPrefix = "editor_content_$index"
                                )
                            }
                        }
                    }

                    // Add File Trigger Button
                    item {
                        val isFirstFileDefaultMd = files.isNotEmpty() && files[0].first == "gistfile1.md"
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { files = files + ("" to "") },
                                enabled = isFirstFileDefaultMd,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFirstFileDefaultMd) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isFirstFileDefaultMd) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("add_file_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add File")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add File", fontWeight = FontWeight.Bold)
                            }
                            if (!isFirstFileDefaultMd) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "First file must be named 'gistfile1.md' to add more files.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }

                    // spelling, grammar, and link checkers suggestions
                    if (qualitySuggestions.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Quality Checkers",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Content Quality Assistant",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Spell, grammar, and external links are analyzed live. Click 'Fix' to apply corrections instantly.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    qualitySuggestions.forEach { suggestion ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(8.dp))
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = if (suggestion.targetField == "description") "In Description" else "In ${suggestion.targetField}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = "•",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = suggestion.type,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = when (suggestion.type) {
                                                            "Spelling" -> MaterialTheme.colorScheme.error
                                                            "External Link" -> ActivePurple
                                                            else -> MaterialTheme.colorScheme.primary
                                                        }
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = suggestion.explanation,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = suggestion.original,
                                                        fontSize = 11.sp,
                                                        style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "to",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Text(
                                                        text = suggestion.replacement,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                            Button(
                                                onClick = {
                                                    if (suggestion.targetField == "description") {
                                                        description = description.replaceFirst(suggestion.original, suggestion.replacement)
                                                    } else {
                                                        files = files.map { (name, content) ->
                                                            if (name == suggestion.targetField || content.contains(suggestion.original)) {
                                                                name to content.replaceFirst(suggestion.original, suggestion.replacement)
                                                            } else {
                                                                name to content
                                                            }
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier
                                                    .height(28.dp)
                                                    .testTag("fix_suggestion_btn_${suggestion.original}"),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("Fix", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Hybrid Local LLM Gist AI Assistant Card
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Gist AI",
                                        tint = ActivePurple,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "Gist AI Assistant",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (aiAnalysis != null) {
                                    TextButton(onClick = onClearAiClick) {
                                        Text("Clear", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Uses a lightweight on-device analyzer (Simulated Local LLM) with online Gemini Flash deep support if credentials exist.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (isAnalyzing) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Local LLM model computing metrics...", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            } else if (aiAnalysis != null) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Summary Box
                                    Text(
                                        text = "✨ Summary & Insights:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ActivePurple
                                    )
                                    Text(
                                        text = aiAnalysis.summary,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // Score indicators
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Complexity Level", fontSize = 10.sp, color = GraySecondary)
                                            Text(
                                                text = "${aiAnalysis.complexityLevel} (${aiAnalysis.complexityScore}/10)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (aiAnalysis.complexityScore > 6) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Maintainability Index", fontSize = 10.sp, color = GraySecondary)
                                            Text(
                                                text = aiAnalysis.maintainabilityIndex,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ActivePurple
                                            )
                                        }
                                    }

                                    // Recommended Tags
                                    Text(
                                        text = "🏷️ Recommended Tags (tap to append):",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        aiAnalysis.recommendedTags.forEach { tag ->
                                            Button(
                                                onClick = {
                                                    if (!description.contains(tag)) {
                                                        description = if (description.isBlank()) tag else "$description $tag"
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(tag, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Recommendations
                                    Text(
                                        text = "🚀 Performance & Clean Code Proposals:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    aiAnalysis.optimizationSuggestions.forEach { proposal ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text("•", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ActivePurple)
                                            Text(proposal, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }

                                    Text(
                                        text = if (aiAnalysis.isOnlineGenerated) "Generated using Gemini Pro Online Model" else "Generated fully offline on-device",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { onAnalyzeClick(description, files) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ActivePurple),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .testTag("analyze_gist_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Bolt, contentDescription = "AI")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Analyze with Gist AI", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Metadata visibility configurations (Public & Pinned)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Public Gist",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Decides visibility status of your snippet.",
                                        fontSize = 11.sp,
                                        color = GraySecondary
                                    )
                            }
                            Switch(
                                checked = isPublic,
                                onCheckedChange = { isPublic = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ActivePurple
                                )
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Pin to Favorites",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Pin this gist locally on top of your vault list.",
                                        fontSize = 11.sp,
                                        color = GraySecondary
                                    )
                            }
                            Switch(
                                checked = isPinned,
                                onCheckedChange = { isPinned = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ActivePurple
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = GraySecondary)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            onSave(description, files, isPublic, isPinned)
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
