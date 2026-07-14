package com.example.ui.components

import java.util.UUID

data class SpellSuggestion(
  val id: String = UUID.randomUUID().toString(),
  val type: String, // "Spelling", "Grammar", or "External Link"
  val original: String,
  val replacement: String,
  val explanation: String,
  val targetField: String // "description" or the filename / file UUID
)

/** Legacy compatibility overload for existing single-file unit tests. */
fun getSpellSuggestions(description: String, content: String): List<SpellSuggestion> {
  return getContentSuggestions(description, listOf("content" to content))
}

/** Advanced multi-file spelling, grammar, and external link checker. */
fun getContentSuggestions(
  description: String,
  files: List<Pair<String, String>>
): List<SpellSuggestion> {
  val list = mutableListOf<SpellSuggestion>()

  // Core developer & writing spelling dictionary
  val dictionary =
    mapOf(
      "teh" to "the",
      "dont" to "don't",
      "cant" to "can't",
      "wont" to "won't",
      "shoudl" to "should",
      "recieve" to "receive",
      "recieved" to "received",
      "seperate" to "separate",
      "definately" to "definitely",
      "goverment" to "government",
      "occured" to "occurred",
      "untill" to "until",
      "truely" to "truly",
      "wierd" to "weird",
      "abbout" to "about",
      "becuase" to "because",
      "enought" to "enough",
      "functuon" to "function",
      "valeu" to "value",
      "exmple" to "example",
      "retreive" to "retrieve",
      "asynchronous" to "asynchronous",
      "parmeter" to "parameter"
    )

  fun checkFieldText(text: String, fieldName: String) {
    if (text.isEmpty()) return

    // 1. Double words (Grammar)
    val doubleWordRegex = Regex("\\b(\\w+)\\s+\\1\\b", RegexOption.IGNORE_CASE)
    doubleWordRegex.findAll(text).forEach { match ->
      val word = match.groupValues[1]
      list.add(
        SpellSuggestion(
          type = "Grammar",
          original = match.value,
          replacement = word,
          explanation = "Repeated word: '$word'",
          targetField = fieldName
        )
      )
    }

    // 2. Dictionary spelling checks (Spelling)
    // Split by non-alphabetic characters (excluding single quotes for apostrophes like don't)
    val words = text.split(Regex("[^a-zA-Z']+"))
    for (w in words) {
      val lowercaseW = w.lowercase()
      if (dictionary.containsKey(lowercaseW)) {
        val replacement = dictionary[lowercaseW]!!
        val finalReplacement =
          if (w.firstOrNull()?.isUpperCase() == true) {
            replacement.substring(0, 1).uppercase() + replacement.substring(1)
          } else {
            replacement
          }
        if (list.none { it.original == w && it.targetField == fieldName }) {
          list.add(
            SpellSuggestion(
              type = "Spelling",
              original = w,
              replacement = finalReplacement,
              explanation = "Possible typo: '$w'",
              targetField = fieldName
            )
          )
        }
      }
    }

    // 3. Sentence capitalization checks (Grammar)
    val sentenceRegex = Regex("(?:^|[.!?]\\s+)([a-z])")
    sentenceRegex.findAll(text).forEach { match ->
      val lowercaseStart = match.groupValues[1]
      val capitalized = lowercaseStart.uppercase()
      val fullMatch = match.value
      val replacement = fullMatch.replaceFirst(lowercaseStart, capitalized)
      list.add(
        SpellSuggestion(
          type = "Grammar",
          original = fullMatch,
          replacement = replacement,
          explanation = "Capitalize sentence starter: '$lowercaseStart'",
          targetField = fieldName
        )
      )
    }

    // 4. Punctuation spacing (Grammar)
    val commaSpacingRegex = Regex("([a-zA-Z]+)(,)([a-zA-Z]+)")
    commaSpacingRegex.findAll(text).forEach { match ->
      val word1 = match.groupValues[1]
      val comma = match.groupValues[2]
      val word2 = match.groupValues[3]
      list.add(
        SpellSuggestion(
          type = "Grammar",
          original = match.value,
          replacement = "$word1$comma $word2",
          explanation = "Add a space after comma",
          targetField = fieldName
        )
      )
    }

    val spaceBeforePunct = Regex("\\s+([,;.!?])")
    spaceBeforePunct.findAll(text).forEach { match ->
      val punct = match.groupValues[1]
      list.add(
        SpellSuggestion(
          type = "Grammar",
          original = match.value,
          replacement = punct,
          explanation = "No space before '$punct'",
          targetField = fieldName
        )
      )
    }

    // 5. External Link Checker
    // Regex to identify URL strings starting with http://, https://, or www.
    val urlRegex =
      Regex("(https?://[\\w\\d:#@%/;\\$\\(\\)~_?\\+-=\\\\\\.&]+)", RegexOption.IGNORE_CASE)
    urlRegex.findAll(text).forEach { match ->
      val urlString = match.value
      if (urlString.startsWith("http://", ignoreCase = true)) {
        val upgraded = urlString.replaceFirst("http://", "https://", ignoreCase = true)
        list.add(
          SpellSuggestion(
            type = "External Link",
            original = urlString,
            replacement = upgraded,
            explanation =
              "Insecure external link. Recommended upgrading to HTTPS secure connection.",
            targetField = fieldName
          )
        )
      } else if (urlString.contains("\\") || urlString.contains("[") || urlString.contains("]")) {
        val cleaned = urlString.replace("\\", "").replace("[", "").replace("]", "")
        list.add(
          SpellSuggestion(
            type = "External Link",
            original = urlString,
            replacement = cleaned,
            explanation = "Malformed URL character escape detected.",
            targetField = fieldName
          )
        )
      }
    }
  }

  checkFieldText(description, "description")
  files.forEach { (name, content) ->
    val target = name.ifBlank { "unnamed file" }
    checkFieldText(content, target)
  }

  return list
}
