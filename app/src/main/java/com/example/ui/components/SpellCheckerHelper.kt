package com.example.ui.components

data class SpellSuggestion(
    val type: String, // "Spelling" or "Grammar"
    val original: String,
    val replacement: String,
    val explanation: String,
    val targetField: String // "description" or "content"
)

fun getSpellSuggestions(description: String, content: String): List<SpellSuggestion> {
    val list = mutableListOf<SpellSuggestion>()
    
    // Common spelling errors dictionary
    val dictionary = mapOf(
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
        "enought" to "enough"
    )

    fun checkField(text: String, fieldName: String) {
        if (text.isEmpty()) return

        // 1. Double words
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

        // 2. Dictionary spelling checks
        val words = text.split(Regex("[^a-zA-Z']+"))
        for (w in words) {
            val lowercaseW = w.lowercase()
            if (dictionary.containsKey(lowercaseW)) {
                val replacement = dictionary[lowercaseW]!!
                val finalReplacement = if (w.firstOrNull()?.isUpperCase() == true) {
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

        // 3. Sentence capitalization checks
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

        // 4. Punctuation spacing
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
    }

    checkField(description, "description")
    checkField(content, "content")
    return list
}
