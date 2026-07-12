package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.GistWithFiles
import com.example.ui.theme.ActivePurple
import com.example.ui.theme.GraySecondary
import com.example.ui.theme.GrayTertiary
import com.example.ui.theme.SlateBg

@Composable
fun UserAvatarPlaceholder(
    login: String,
    size: androidx.compose.ui.unit.Dp = 36.dp
) {
    val initial = login.take(1).uppercase()
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
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
        val formatter = java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault())
        formatter.format(date)
    } catch (e: Exception) {
        isoString
    }
}

@Composable
fun DetailedCreationInfoCard(item: GistWithFiles) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        border = borderButtonStroke(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Created", fontSize = 12.sp, color = GraySecondary)
                    Text(formatGistDate(item.gist.createdAt), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Updated", fontSize = 12.sp, color = GraySecondary)
                    Text(formatGistDate(item.gist.updatedAt), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
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
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val lines = text.split("\n")
        var inCodeBlock = false
        var codeBlockContent = ""
        var codeBlockLang = ""
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    val currentLang = codeBlockLang
                    val currentContent = codeBlockContent
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF2D2D2D),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        val highlightedText = remember(currentContent, currentLang) {
                            SyntaxHighlighter.highlight(
                                text = currentContent.trimEnd(),
                                filename = "code.$currentLang"
                            )
                        }
                        Text(
                            text = highlightedText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    codeBlockContent = ""
                    codeBlockLang = ""
                    inCodeBlock = false
                } else {
                    codeBlockLang = trimmed.substring(3).trim()
                    inCodeBlock = true
                }
            } else if (inCodeBlock) {
                codeBlockContent += line + "\n"
            } else if (trimmed.startsWith("#")) {
                val level = trimmed.takeWhile { it == '#' }.length
                val headerText = trimmed.drop(level).trim()
                val size = when (level) {
                    1 -> 20.sp
                    2 -> 18.sp
                    3 -> 16.sp
                    else -> 14.sp
                }
                Text(
                    text = headerText,
                    fontSize = size,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                val bulletText = trimmed.substring(2).trim()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "• ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = parseMarkdownInline(bulletText),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                if (trimmed.isNotEmpty()) {
                    Text(
                        text = parseMarkdownInline(line),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

fun parseMarkdownInline(text: String): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            if (text.startsWith("**", i)) {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(
                        style = SpanStyle(fontWeight = FontWeight.Bold)
                    ) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append("**")
                    i += 2
                }
            } else if (text.startsWith("*", i)) {
                val end = text.indexOf("*", i + 1)
                if (end != -1) {
                    withStyle(
                        style = SpanStyle(fontStyle = FontStyle.Italic)
                    ) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append("*")
                    i += 1
                }
            } else if (text.startsWith("`", i)) {
                val end = text.indexOf("`", i + 1)
                if (end != -1) {
                    withStyle(
                        style = SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color.LightGray.copy(alpha = 0.2f),
                            color = Color(0xFFC7254E)
                        )
                    ) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append("`")
                    i += 1
                }
            } else {
                append(text[i].toString())
                i++
            }
        }
    }
}

enum class DiffLineType {
    ADDED, DELETED, UNCHANGED
}

data class DiffLine(
    val type: DiffLineType,
    val text: String,
    val oldLineNum: Int? = null,
    val newLineNum: Int? = null
)

data class SplitDiffRow(
    val left: DiffLine?,
    val right: DiffLine?
)

fun computeDiff(oldText: String, newText: String): List<DiffLine> {
    val oldLines = oldText.split("\n")
    val newLines = newText.split("\n")
    val n = oldLines.size
    val m = newLines.size

    val dp = Array(n + 1) { IntArray(m + 1) }
    for (i in 1..n) {
        for (j in 1..m) {
            if (oldLines[i - 1] == newLines[j - 1]) {
                dp[i][j] = dp[i - 1][j - 1] + 1
            } else {
                dp[i][j] = maxOf(dp[i - 1][j], dp[i][j - 1])
            }
        }
    }

    val diff = mutableListOf<DiffLine>()
    var i = n
    var j = m
    while (i > 0 || j > 0) {
        if (i > 0 && j > 0 && oldLines[i - 1] == newLines[j - 1]) {
            diff.add(0, DiffLine(DiffLineType.UNCHANGED, oldLines[i - 1], i, j))
            i--
            j--
        } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
            diff.add(0, DiffLine(DiffLineType.ADDED, newLines[j - 1], null, j))
            j--
        } else {
            diff.add(0, DiffLine(DiffLineType.DELETED, oldLines[i - 1], i, null))
            i--
        }
    }
    return diff
}

fun alignSplitDiff(diff: List<DiffLine>): List<SplitDiffRow> {
    val rows = mutableListOf<SplitDiffRow>()
    val tempDeletes = mutableListOf<DiffLine>()
    val tempAdds = mutableListOf<DiffLine>()

    fun flushPending() {
        val maxLen = maxOf(tempDeletes.size, tempAdds.size)
        for (k in 0 until maxLen) {
            val del = tempDeletes.getOrNull(k)
            val add = tempAdds.getOrNull(k)
            rows.add(SplitDiffRow(del, add))
        }
        tempDeletes.clear()
        tempAdds.clear()
    }

    for (line in diff) {
        when (line.type) {
            DiffLineType.UNCHANGED -> {
                flushPending()
                rows.add(SplitDiffRow(line, line))
            }
            DiffLineType.DELETED -> {
                tempDeletes.add(line)
            }
            DiffLineType.ADDED -> {
                tempAdds.add(line)
            }
        }
    }
    flushPending()
    return rows
}

@Composable
fun UnifiedDiffView(oldContent: String, newContent: String) {
    val diffLines = remember(oldContent, newContent) { computeDiff(oldContent, newContent) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .background(Color(0xFF0D1117))
            .padding(vertical = 4.dp)
    ) {
        if (diffLines.isEmpty()) {
            Text(
                text = "No changes in this file.",
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = GraySecondary,
                modifier = Modifier.padding(12.dp)
            )
        } else {
            diffLines.forEach { line ->
                val bgColor = when (line.type) {
                    DiffLineType.ADDED -> Color(0xFF2EA44F).copy(alpha = 0.15f)
                    DiffLineType.DELETED -> Color(0xFFCF222E).copy(alpha = 0.15f)
                    DiffLineType.UNCHANGED -> Color.Transparent
                }
                val textColor = when (line.type) {
                    DiffLineType.ADDED -> Color(0xFF3FB950)
                    DiffLineType.DELETED -> Color(0xFFF85149)
                    DiffLineType.UNCHANGED -> Color(0xFFC9D1D9)
                }
                val prefix = when (line.type) {
                    DiffLineType.ADDED -> "+"
                    DiffLineType.DELETED -> "-"
                    DiffLineType.UNCHANGED -> " "
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = line.oldLineNum?.toString() ?: "",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = GraySecondary.copy(alpha = 0.6f),
                        modifier = Modifier.width(28.dp)
                    )
                    Text(
                        text = line.newLineNum?.toString() ?: "",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = GraySecondary.copy(alpha = 0.6f),
                        modifier = Modifier.width(28.dp)
                    )
                    Text(
                        text = prefix,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.width(16.dp)
                    )
                    Text(
                        text = line.text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = textColor,
                        lineHeight = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun SplitDiffView(oldContent: String, newContent: String) {
    val diffLines = remember(oldContent, newContent) { computeDiff(oldContent, newContent) }
    val splitRows = remember(diffLines) { alignSplitDiff(diffLines) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .background(Color(0xFF0D1117))
            .padding(vertical = 4.dp)
    ) {
        if (splitRows.isEmpty()) {
            Text(
                text = "No changes in this file.",
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = GraySecondary,
                modifier = Modifier.padding(12.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                Column(modifier = Modifier.width(800.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF161B22))
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Original File",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GraySecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Revised File",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GraySecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    splitRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val leftBg = when (row.left?.type) {
                                DiffLineType.DELETED -> Color(0xFFCF222E).copy(alpha = 0.15f)
                                else -> Color.Transparent
                            }
                            val leftTextCol = when (row.left?.type) {
                                DiffLineType.DELETED -> Color(0xFFF85149)
                                else -> Color(0xFFC9D1D9)
                            }
                            val leftPrefix = when (row.left?.type) {
                                DiffLineType.DELETED -> "-"
                                else -> " "
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(leftBg)
                                    .padding(vertical = 2.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = row.left?.oldLineNum?.toString() ?: "",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = GraySecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.width(28.dp)
                                )
                                Text(
                                    text = leftPrefix,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = leftTextCol,
                                    modifier = Modifier.width(12.dp)
                                )
                                Text(
                                    text = row.left?.text ?: "",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = leftTextCol,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(20.dp)
                                    .background(Color(0xFF30363D))
                            )

                            val rightBg = when (row.right?.type) {
                                DiffLineType.ADDED -> Color(0xFF2EA44F).copy(alpha = 0.15f)
                                else -> Color.Transparent
                            }
                            val rightTextCol = when (row.right?.type) {
                                DiffLineType.ADDED -> Color(0xFF3FB950)
                                else -> Color(0xFFC9D1D9)
                            }
                            val rightPrefix = when (row.right?.type) {
                                DiffLineType.ADDED -> "+"
                                else -> " "
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(rightBg)
                                    .padding(vertical = 2.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = row.right?.newLineNum?.toString() ?: "",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = GraySecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.width(28.dp)
                                )
                                Text(
                                    text = rightPrefix,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = rightTextCol,
                                    modifier = Modifier.width(12.dp)
                                )
                                Text(
                                    text = row.right?.text ?: "",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = rightTextCol,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GistPreviewDialog(
    show: Boolean,
    item: GistWithFiles?,
    viewModel: com.example.ui.viewmodel.GistViewModel,
    onDismiss: () -> Unit
) {
    if (!show || item == null) return

    var activeTab by remember { mutableStateOf("files") }
    var historyList by remember { mutableStateOf<List<com.example.data.remote.model.GistHistoryResponse>?>(null) }
    var isLoadingHistory by remember { mutableStateOf(false) }
    var historyError by remember { mutableStateOf<String?>(null) }

    var selectedRevisionSha by remember { mutableStateOf<String?>(null) }
    var diffViewMode by remember { mutableStateOf("unified") } // "unified" or "split"
    
    var isLoadingRevisionContent by remember { mutableStateOf(false) }
    var revisionContentError by remember { mutableStateOf<String?>(null) }
    var currentRevisionGist by remember { mutableStateOf<com.example.data.remote.model.GistResponse?>(null) }
    var parentRevisionGist by remember { mutableStateOf<com.example.data.remote.model.GistResponse?>(null) }

    LaunchedEffect(item.gist.id) {
        if (item.gist.isLocalOnly) {
            historyError = "Revisions are not available for local-only Gists."
            return@LaunchedEffect
        }
        isLoadingHistory = true
        historyError = null
        viewModel.getRemoteGistDetails(item.gist.id)
            .onSuccess { details ->
                historyList = details.history
                if (details.history.isNullOrEmpty()) {
                    historyError = "No revision history found for this Gist."
                }
            }
            .onFailure { err ->
                historyError = "Failed to load revisions: ${err.message}"
            }
        isLoadingHistory = false
    }

    LaunchedEffect(selectedRevisionSha) {
        val sha = selectedRevisionSha
        if (sha == null) {
            currentRevisionGist = null
            parentRevisionGist = null
            return@LaunchedEffect
        }
        
        isLoadingRevisionContent = true
        revisionContentError = null
        currentRevisionGist = null
        parentRevisionGist = null
        
        viewModel.getRemoteGistRevision(item.gist.id, sha)
            .onSuccess { curGist ->
                currentRevisionGist = curGist
                val currentIdx = historyList?.indexOfFirst { it.version == sha } ?: -1
                val parentRev = if (currentIdx != -1 && currentIdx + 1 < (historyList?.size ?: 0)) {
                    historyList?.get(currentIdx + 1)
                } else {
                    null
                }
                
                if (parentRev != null && parentRev.version != null) {
                    viewModel.getRemoteGistRevision(item.gist.id, parentRev.version)
                        .onSuccess { parGist ->
                            parentRevisionGist = parGist
                        }
                        .onFailure {
                            parentRevisionGist = null
                        }
                } else {
                    parentRevisionGist = null
                }
            }
            .onFailure { err ->
                revisionContentError = "Failed to load revision files: ${err.message}"
            }
        isLoadingRevisionContent = false
    }

    val filesToCompare = remember(currentRevisionGist, parentRevisionGist) {
        val curFiles = currentRevisionGist?.files ?: emptyMap()
        val parFiles = parentRevisionGist?.files ?: emptyMap()
        val allNames = (curFiles.keys + parFiles.keys).toSet().toList().sorted()
        allNames.map { name ->
            val cur = curFiles[name]
            val par = parFiles[name]
            Triple(name, par?.content ?: "", cur?.content ?: "")
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateBg)
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = SlateBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gist Snippet Preview",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Navigation Tabs (Files / Revisions)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("Files" to Icons.Default.Description, "Revisions" to Icons.Default.History).forEach { (tabName, icon) ->
                        val isSelected = activeTab == tabName.lowercase()
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ActivePurple.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) ActivePurple else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { 
                                    activeTab = tabName.lowercase()
                                    if (tabName.lowercase() == "revisions") {
                                        selectedRevisionSha = null
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = tabName,
                                tint = if (isSelected) ActivePurple else GraySecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tabName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) ActivePurple else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (activeTab == "files") {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            DetailedCreationInfoCard(item = item)
                        }

                        item {
                            Column {
                                Text(
                                    text = "Description",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.gist.description ?: "No description provided",
                                    fontSize = 14.sp,
                                    color = GraySecondary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }

                        items(item.files.size) { idx ->
                            val file = item.files[idx]
                            val isMarkdown = file.filename.endsWith(".md", ignoreCase = true) || 
                                             file.filename.endsWith(".markdown", ignoreCase = true)
                            
                            var previewMode by remember { mutableStateOf(if (isMarkdown) "markdown" else "raw") }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = borderButtonStroke(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = file.filename,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        
                                        if (isMarkdown) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(
                                                    onClick = { previewMode = "raw" },
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text(
                                                        text = "Raw",
                                                        fontSize = 11.sp,
                                                        fontWeight = if (previewMode == "raw") FontWeight.Bold else FontWeight.Normal,
                                                        color = if (previewMode == "raw") MaterialTheme.colorScheme.primary else GraySecondary
                                                    )
                                                }
                                                TextButton(
                                                    onClick = { previewMode = "markdown" },
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text(
                                                        text = "Markdown",
                                                        fontSize = 11.sp,
                                                        fontWeight = if (previewMode == "markdown") FontWeight.Bold else FontWeight.Normal,
                                                        color = if (previewMode == "markdown") MaterialTheme.colorScheme.primary else GraySecondary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (isMarkdown && previewMode == "markdown") {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                                .padding(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            MarkdownText(text = file.content)
                                        }
                                    } else {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 100.dp, max = 400.dp)
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF1E1E1E)
                                        ) {
                                            LazyColumn(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp)
                                            ) {
                                                item {
                                                    val highlightedText = remember(file.content, file.filename) {
                                                        SyntaxHighlighter.highlight(
                                                            text = file.content.ifEmpty { "// Empty content" },
                                                            filename = file.filename
                                                        )
                                                    }
                                                    Text(
                                                        text = highlightedText,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 12.sp,
                                                        lineHeight = 16.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Revisions Tab
                    if (selectedRevisionSha == null) {
                        if (isLoadingHistory) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = ActivePurple)
                            }
                        } else if (historyError != null) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = historyError ?: "",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val hist = historyList
                                if (hist != null) {
                                    items(hist.size) { idx ->
                                        val rev = hist[idx]
                                        val formattedRevDate = formatGistDate(rev.committedAt ?: "")
                                        val change = rev.changeStatus
                                        
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedRevisionSha = rev.version },
                                            border = borderButtonStroke(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    UserAvatarPlaceholder(login = rev.user?.login ?: item.gist.ownerLogin, size = 32.dp)
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(
                                                            text = "@${rev.user?.login ?: item.gist.ownerLogin}",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            text = "Committed on $formattedRevDate",
                                                            fontSize = 12.sp,
                                                            color = GraySecondary
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "Version: ${rev.version?.take(7) ?: "unknown"}",
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 11.sp,
                                                            color = GrayTertiary
                                                        )
                                                    }
                                                }
                                                
                                                if (change != null) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "+${change.additions ?: 0}",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF3FB950)
                                                        )
                                                        Text(
                                                            text = "-${change.deletions ?: 0}",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFF85149)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Detailed revision view
                        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedRevisionSha = null }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = ActivePurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Back to Revisions",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ActivePurple
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Viewing Revision ${selectedRevisionSha?.take(7)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Comparing files changes",
                                        fontSize = 11.sp,
                                        color = GraySecondary
                                    )
                                }
                                
                                // Unified vs Split Selector Toggle
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("Unified", "Split").forEach { option ->
                                        val isSelected = (option.lowercase() == diffViewMode)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) ActivePurple else Color.Transparent)
                                                .clickable { diffViewMode = option.lowercase() }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = option,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (isLoadingRevisionContent) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = ActivePurple)
                                }
                            } else if (revisionContentError != null) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = revisionContentError ?: "",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    val filesList = filesToCompare
                                    items(filesList.size) { fileIdx ->
                                        val (filename, oldContent, newContent) = filesList[fileIdx]
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            border = borderButtonStroke(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = filename,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                )
                                                
                                                if (diffViewMode == "split") {
                                                    SplitDiffView(oldContent = oldContent, newContent = newContent)
                                                } else {
                                                    UnifiedDiffView(oldContent = oldContent, newContent = newContent)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (item.gist.isPublic) Icons.Default.Public else Icons.Default.Lock,
                            contentDescription = "Visibility",
                            tint = GrayTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (item.gist.isPublic) "Public Gist" else "Private Gist",
                            fontSize = 12.sp,
                            color = GraySecondary
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ActivePurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

