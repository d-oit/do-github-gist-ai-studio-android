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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ActivePurple

@Composable
fun ContentQualityAssistantView(
  qualitySuggestions: List<SpellSuggestion>,
  onFixSuggestion: (SpellSuggestion) -> Unit,
  onFixAllSuggestions: () -> Unit
) {
  if (qualitySuggestions.isEmpty()) return

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .background(MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp))
        .border(1.dp, MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(12.dp))
        .padding(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
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
      if (qualitySuggestions.size > 1) {
        Button(
          onClick = onFixAllSuggestions,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
          modifier = Modifier.height(32.dp).testTag("fix_all_suggestions_btn"),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("Fix All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text =
        "Spell, grammar, and external links are analyzed live. Click 'Fix' to apply corrections instantly.",
      fontSize = 11.sp,
      color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
    )
    Spacer(modifier = Modifier.height(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      qualitySuggestions.forEach { suggestion ->
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
              .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
              )
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
                text =
                  if (suggestion.targetField == "description") "In Description"
                  else "In ${suggestion.targetField}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
              Text(text = "•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(
                text = suggestion.type,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color =
                  when (suggestion.type) {
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
                style =
                  androidx.compose.ui.text.TextStyle(
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                  ),
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
            onClick = { onFixSuggestion(suggestion) },
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
              ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.height(28.dp).testTag("fix_suggestion_btn_${suggestion.original}"),
            shape = RoundedCornerShape(6.dp)
          ) {
            Text("Fix", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
