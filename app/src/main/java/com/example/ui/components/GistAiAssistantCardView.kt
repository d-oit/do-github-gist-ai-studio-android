package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ActivePurple
import com.example.ui.theme.GraySecondary

@Composable
fun GistAiAssistantCardView(
  isAnalyzing: Boolean,
  aiAnalysis: GistAiAnalysis?,
  onAnalyzeClick: () -> Unit,
  onClearAiClick: () -> Unit,
  onAppendRecommendedTag: (String) -> Unit,
  onAppendAllTags: () -> Unit = {},
  onApplyFixes: () -> Unit
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
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
      text =
        "Uses a lightweight on-device analyzer (Simulated Local LLM) with online Gemini Flash deep support if credentials exist.",
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
        Text(
          "Local LLM model computing metrics...",
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
        )
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
              color =
                if (aiAnalysis.complexityScore > 6) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
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
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "🏷️ Recommended Tags (tap to append):",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          TextButton(
            onClick = onAppendAllTags,
            modifier = Modifier.height(28.dp).testTag("append_all_tags_btn"),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
          ) {
            Text("Append All", fontSize = 11.sp, color = ActivePurple)
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          aiAnalysis.recommendedTags.forEach { tag ->
            Button(
              onClick = { onAppendRecommendedTag(tag) },
              colors =
                ButtonDefaults.buttonColors(
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

        val hasFixable =
          aiAnalysis.optimizationSuggestions.any { suggestion ->
            suggestion.contains("mutableStateOf") ||
              suggestion.contains("Remove debug print/log") ||
              suggestion.contains("files do not have an extension") ||
              suggestion.contains("do not have an extension")
          }
        if (hasFixable) {
          Spacer(modifier = Modifier.height(6.dp))
          Button(
            onClick = onApplyFixes,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.fillMaxWidth().height(32.dp).testTag("apply_ai_fixes_btn"),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("Apply AI Fixes & Recommendations", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }

        Text(
          text =
            if (aiAnalysis.isOnlineGenerated) "Generated using Gemini 3.5-Flash Online Model"
            else "Generated fully offline on-device",
          fontSize = 9.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
          fontWeight = FontWeight.SemiBold
        )
      }
    } else {
      Button(
        onClick = onAnalyzeClick,
        colors = ButtonDefaults.buttonColors(containerColor = ActivePurple),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(40.dp).testTag("analyze_gist_btn")
      ) {
        Icon(imageVector = Icons.Default.Bolt, contentDescription = "AI")
        Spacer(modifier = Modifier.width(6.dp))
        Text("Analyze with Gist AI", fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
    }
  }
}
