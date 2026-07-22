package com.example.ui.components

object DraftEditorAiRefactoringHelper {

  fun applyAiAnalysisFixes(
    description: String,
    files: List<Pair<String, String>>,
    tagsInput: String,
    aiAnalysis: GistAiAnalysis?
  ): Triple<String, List<Pair<String, String>>, String> {
    val currentDesc = description
    val hasPrintFix =
      aiAnalysis?.optimizationSuggestions?.any { it.contains("Remove debug print/log") } == true
    val hasStateFix =
      aiAnalysis?.optimizationSuggestions?.any { it.contains("mutableStateOf") } == true
    val hasExtensionFix =
      aiAnalysis?.optimizationSuggestions?.any {
        it.contains("files do not have an extension") || it.contains("do not have an extension")
      } == true

    val updatedFiles =
      files.map { (name, content) ->
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
          val mutableStateRegex = Regex("(?<!remember\\s*\\{\\s*)mutableStateOf\\((.*?)\\)")
          fixedContent =
            mutableStateRegex.replace(fixedContent) { matchResult ->
              "remember { mutableStateOf(${matchResult.groupValues[1]}) }"
            }
        }
        var fixedName = name
        if (hasExtensionFix) {
          val nonExtensionSpecialNames =
            setOf("makefile", "dockerfile", "license", "jenkinsfile", "vagrantfile", "readme")
          if (!name.contains(".") && !nonExtensionSpecialNames.contains(name.lowercase())) {
            fixedName =
              if (fixedContent.contains("@Composable") || fixedContent.contains("fun ")) {
                "$name.kt"
              } else {
                "$name.txt"
              }
          }
        }
        fixedName to fixedContent
      }

    val existingTags =
      tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
    var tagsUpdated = false
    aiAnalysis?.recommendedTags?.forEach { tag ->
      val cleanTag = tag.replace("#", "").trim()
      if (cleanTag.isNotEmpty() && !existingTags.any { it.equals(cleanTag, ignoreCase = true) }) {
        existingTags.add(cleanTag)
        tagsUpdated = true
      }
    }
    val updatedTagsInput = if (tagsUpdated) existingTags.joinToString(", ") else tagsInput

    return Triple(currentDesc, updatedFiles, updatedTagsInput)
  }
}
