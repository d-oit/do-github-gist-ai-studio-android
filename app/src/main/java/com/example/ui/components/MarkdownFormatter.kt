package com.example.ui.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** Transforms the selected markdown or text cursor. */
fun applyFormatting(value: TextFieldValue, formatType: String): TextFieldValue {
  val text = value.text
  val selection = value.selection
  val selectedText = text.substring(selection.start, selection.end)

  val (newText, newSelection) =
    when (formatType) {
      "header" -> {
        if (selection.collapsed) {
          val before = text.substring(0, selection.start)
          val after = text.substring(selection.start)
          val insert = "### Header"
          val updated = before + insert + after
          updated to TextRange(selection.start + 4, selection.start + 10)
        } else {
          val before = text.substring(0, selection.start)
          val after = text.substring(selection.end)
          val insert = "### $selectedText"
          val updated = before + insert + after
          updated to TextRange(selection.start + 4, selection.start + 4 + selectedText.length)
        }
      }
      "bold" -> {
        if (selection.collapsed) {
          val before = text.substring(0, selection.start)
          val after = text.substring(selection.start)
          val insert = "**bold_text**"
          val updated = before + insert + after
          updated to TextRange(selection.start + 2, selection.start + 11)
        } else {
          val before = text.substring(0, selection.start)
          val after = text.substring(selection.end)
          val insert = "**$selectedText**"
          val updated = before + insert + after
          updated to TextRange(selection.start + 2, selection.start + 2 + selectedText.length)
        }
      }
      "italic" -> {
        if (selection.collapsed) {
          val before = text.substring(0, selection.start)
          val after = text.substring(selection.start)
          val insert = "*italic_text*"
          val updated = before + insert + after
          updated to TextRange(selection.start + 1, selection.start + 12)
        } else {
          val before = text.substring(0, selection.start)
          val after = text.substring(selection.end)
          val insert = "*$selectedText*"
          val updated = before + insert + after
          updated to TextRange(selection.start + 1, selection.start + 1 + selectedText.length)
        }
      }
      "quote" -> {
        if (selection.collapsed) {
          val before = text.substring(0, selection.start)
          val after = text.substring(selection.start)
          val insert = "> "
          val updated = before + insert + after
          updated to TextRange(selection.start + 2)
        } else {
          val before = text.substring(0, selection.start)
          val after = text.substring(selection.end)
          val lines = selectedText.split("\n").joinToString("\n") { "> $it" }
          val updated = before + lines + after
          updated to TextRange(selection.start, selection.start + lines.length)
        }
      }
      "code" -> {
        if (selection.collapsed) {
          val before = text.substring(0, selection.start)
          val after = text.substring(selection.start)
          val insert = "```\ncode_block\n```"
          val updated = before + insert + after
          updated to TextRange(selection.start + 4, selection.start + 14)
        } else {
          val before = text.substring(0, selection.start)
          val after = text.substring(selection.end)
          val isMultiLine = selectedText.contains("\n")
          val insert = if (isMultiLine) "```\n$selectedText\n```" else "`$selectedText`"
          val updated = before + insert + after
          val offset = if (isMultiLine) 4 else 1
          updated to
            TextRange(selection.start + offset, selection.start + offset + selectedText.length)
        }
      }
      "link" -> {
        if (selection.collapsed) {
          val before = text.substring(0, selection.start)
          val after = text.substring(selection.start)
          val insert = "[Link Text](https://url)"
          val updated = before + insert + after
          updated to TextRange(selection.start + 1, selection.start + 10)
        } else {
          val before = text.substring(0, selection.start)
          val after = text.substring(selection.end)
          val insert = "[$selectedText](https://url)"
          val updated = before + insert + after
          updated to
            TextRange(
              selection.start + selectedText.length + 3,
              selection.start + selectedText.length + 14
            )
        }
      }
      "num_list" -> {
        if (selection.collapsed) {
          val before = text.substring(0, selection.start)
          val after = text.substring(selection.start)
          val insert = "1. "
          val updated = before + insert + after
          updated to TextRange(selection.start + 3)
        } else {
          val before = text.substring(0, selection.start)
          val after = text.substring(selection.end)
          var counter = 1
          val lines = selectedText.split("\n").joinToString("\n") { "${counter++}. $it" }
          val updated = before + lines + after
          updated to TextRange(selection.start, selection.start + lines.length)
        }
      }
      "bullet_list" -> {
        if (selection.collapsed) {
          val before = text.substring(0, selection.start)
          val after = text.substring(selection.start)
          val insert = "- "
          val updated = before + insert + after
          updated to TextRange(selection.start + 2)
        } else {
          val before = text.substring(0, selection.start)
          val after = text.substring(selection.end)
          val lines = selectedText.split("\n").joinToString("\n") { "- $it" }
          val updated = before + lines + after
          updated to TextRange(selection.start, selection.start + lines.length)
        }
      }
      "task_list" -> {
        if (selection.collapsed) {
          val before = text.substring(0, selection.start)
          val after = text.substring(selection.start)
          val insert = "- [ ] Task"
          val updated = before + insert + after
          updated to TextRange(selection.start + 6, selection.start + 10)
        } else {
          val before = text.substring(0, selection.start)
          val after = text.substring(selection.end)
          val lines = selectedText.split("\n").joinToString("\n") { "- [ ] $it" }
          val updated = before + lines + after
          updated to TextRange(selection.start + 6, selection.start + lines.length)
        }
      }
      "attachment" -> {
        val before = text.substring(0, selection.start)
        val after = text.substring(selection.start)
        val insert = "![Alt Text](url)"
        val updated = before + insert + after
        updated to TextRange(selection.start + 2, selection.start + 10)
      }
      "mention" -> {
        val before = text.substring(0, selection.start)
        val after = text.substring(selection.start)
        val insert = "@"
        val updated = before + insert + after
        updated to TextRange(selection.start + 1)
      }
      "quote_reply" -> {
        if (selection.collapsed) {
          val before = text.substring(0, selection.start)
          val after = text.substring(selection.start)
          val insert = "> Reply message"
          val updated = before + insert + after
          updated to TextRange(selection.start + 2, selection.start + 15)
        } else {
          val before = text.substring(0, selection.start)
          val after = text.substring(selection.end)
          val lines = selectedText.split("\n").joinToString("\n") { "> $it" }
          val updated = before + lines + after
          updated to TextRange(selection.start, selection.start + lines.length)
        }
      }
      else -> value.text to value.selection
    }

  return TextFieldValue(text = newText, selection = newSelection)
}
