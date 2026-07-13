package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ActivePurple
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.GrayTertiary

@Composable
fun GitHubMarkdownEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Leave a comment",
    testTagPrefix: String = "markdown"
) {
    var viewMode by remember { mutableStateOf("write") } // "write" or "preview"

    // Convert raw string to TextFieldValue to track selection and cursor state
    var textFieldValue by remember(value) {
        mutableStateOf(
            TextFieldValue(text = value, selection = androidx.compose.ui.text.TextRange(value.length))
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
    ) {
        // Tab Header & Toolbar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Write vs Preview Tabs
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Write Tab
                TextButton(
                    onClick = { viewMode = "write" },
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("${testTagPrefix}_tab_write"),
                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                ) {
                    Text(
                        text = "Write",
                        fontWeight = if (viewMode == "write") FontWeight.Bold else FontWeight.Normal,
                        color = if (viewMode == "write") MaterialTheme.colorScheme.primary else GraySecondary,
                        fontSize = 13.sp
                    )
                }

                // Preview Tab
                TextButton(
                    onClick = { viewMode = "preview" },
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("${testTagPrefix}_tab_preview"),
                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                ) {
                    Text(
                        text = "Preview",
                        fontWeight = if (viewMode == "preview") FontWeight.Bold else FontWeight.Normal,
                        color = if (viewMode == "preview") MaterialTheme.colorScheme.primary else GraySecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // Formatting toolbar (only visible on Write mode)
            if (viewMode == "write") {
                LazyRow(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .testTag("markdown_toolbar"),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        // Title/Header H Button
                        ToolbarButton(
                            label = "H",
                            onClick = {
                                val newVal = applyFormatting(textFieldValue, "header")
                                textFieldValue = newVal
                                onValueChange(newVal.text)
                            },
                            testTag = "markdown_btn_header",
                            contentDescription = "Format Header"
                        )
                    }

                    item {
                        ToolbarButton(
                            icon = Icons.Default.FormatBold,
                            onClick = {
                                val newVal = applyFormatting(textFieldValue, "bold")
                                textFieldValue = newVal
                                onValueChange(newVal.text)
                            },
                            testTag = "markdown_btn_bold",
                            contentDescription = "Format Bold"
                        )
                    }

                    item {
                        ToolbarButton(
                            icon = Icons.Default.FormatItalic,
                            onClick = {
                                val newVal = applyFormatting(textFieldValue, "italic")
                                textFieldValue = newVal
                                onValueChange(newVal.text)
                            },
                            testTag = "markdown_btn_italic",
                            contentDescription = "Format Italic"
                        )
                    }

                    item {
                        ToolbarButton(
                            icon = Icons.Default.FormatQuote,
                            onClick = {
                                val newVal = applyFormatting(textFieldValue, "quote")
                                textFieldValue = newVal
                                onValueChange(newVal.text)
                            },
                            testTag = "markdown_btn_quote",
                            contentDescription = "Format Quote"
                        )
                    }

                    item {
                        ToolbarButton(
                            icon = Icons.Default.Code,
                            onClick = {
                                val newVal = applyFormatting(textFieldValue, "code")
                                textFieldValue = newVal
                                onValueChange(newVal.text)
                            },
                            testTag = "markdown_btn_code",
                            contentDescription = "Format Code Block"
                        )
                    }

                    item {
                        ToolbarButton(
                            icon = Icons.Default.Link,
                            onClick = {
                                val newVal = applyFormatting(textFieldValue, "link")
                                textFieldValue = newVal
                                onValueChange(newVal.text)
                            },
                            testTag = "markdown_btn_link",
                            contentDescription = "Format Link"
                        )
                    }

                    item {
                        ToolbarDivider()
                    }

                    item {
                        ToolbarButton(
                            icon = Icons.Default.FormatListNumbered,
                            onClick = {
                                val newVal = applyFormatting(textFieldValue, "num_list")
                                textFieldValue = newVal
                                onValueChange(newVal.text)
                            },
                            testTag = "markdown_btn_num_list",
                            contentDescription = "Format Numbered List"
                        )
                    }

                    item {
                        ToolbarButton(
                            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                            onClick = {
                                val newVal = applyFormatting(textFieldValue, "bullet_list")
                                textFieldValue = newVal
                                onValueChange(newVal.text)
                            },
                            testTag = "markdown_btn_bullet_list",
                            contentDescription = "Format Bulleted List"
                        )
                    }

                    item {
                        ToolbarButton(
                            icon = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                            onClick = {
                                val newVal = applyFormatting(textFieldValue, "task_list")
                                textFieldValue = newVal
                                onValueChange(newVal.text)
                            },
                            testTag = "markdown_btn_task_list",
                            contentDescription = "Format Task List"
                        )
                    }

                    item {
                        ToolbarDivider()
                    }

                    item {
                        ToolbarButton(
                            icon = Icons.Default.AttachFile,
                            onClick = {
                                val newVal = applyFormatting(textFieldValue, "attachment")
                                textFieldValue = newVal
                                onValueChange(newVal.text)
                            },
                            testTag = "markdown_btn_attachment",
                            contentDescription = "Add Attachment"
                        )
                    }

                    item {
                        ToolbarButton(
                            icon = Icons.Default.AlternateEmail,
                            onClick = {
                                val newVal = applyFormatting(textFieldValue, "mention")
                                textFieldValue = newVal
                                onValueChange(newVal.text)
                            },
                            testTag = "markdown_btn_mention",
                            contentDescription = "Mention User"
                        )
                    }

                    item {
                        ToolbarButton(
                            icon = Icons.AutoMirrored.Filled.Reply,
                            onClick = {
                                val newVal = applyFormatting(textFieldValue, "quote_reply")
                                textFieldValue = newVal
                                onValueChange(newVal.text)
                            },
                            testTag = "markdown_btn_quote_reply",
                            contentDescription = "Quote Reply Reference"
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Body: Editor Panel or Preview Render
        if (viewMode == "preview") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 300.dp)
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    item {
                        if (value.isBlank()) {
                            Text(
                                text = "Nothing to preview yet.",
                                fontSize = 13.sp,
                                color = GrayTertiary,
                                modifier = Modifier.padding(8.dp)
                            )
                        } else {
                            MarkdownText(text = value)
                        }
                    }
                }
            }
        } else {
            // Write view text editor
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    if (newValue.text != value) {
                        onValueChange(newValue.text)
                    }
                },
                placeholder = { Text(text = placeholder) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 300.dp)
                    .testTag("${testTagPrefix}_content"),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                singleLine = false,
                maxLines = 100
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Status indicator footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bottom-Left area: Markdown is supported with small down arrow logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.testTag("markdown_footer_logo")
                ) {
                    Text(
                        text = "M⬇",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = GraySecondary
                    )
                    Text(
                        text = "Markdown is supported",
                        fontSize = 11.sp,
                        color = GraySecondary
                    )
                }

                // Bottom-Right area: Click attachment hint
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .testTag("markdown_footer_attachment_hint")
                        .clickable {
                            // Insert attachment on footer click!
                            val newVal = applyFormatting(textFieldValue, "attachment")
                            textFieldValue = newVal
                            onValueChange(newVal.text)
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach file icon",
                        tint = GraySecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Paste, drop, or click to add files",
                        fontSize = 11.sp,
                        color = GraySecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ToolbarButton(
    icon: ImageVector? = null,
    label: String? = null,
    onClick: () -> Unit,
    testTag: String,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(48.dp) // Touch target compliance (minimum 48dp x 48dp)
            .testTag(testTag)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
        } else if (label != null) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(24.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
