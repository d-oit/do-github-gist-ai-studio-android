package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GraySecondary

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
