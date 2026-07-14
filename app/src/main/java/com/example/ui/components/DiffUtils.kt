package com.example.ui.components

enum class DiffLineType {
  ADDED,
  DELETED,
  UNCHANGED
}

data class DiffLine(
  val type: DiffLineType,
  val text: String,
  val oldLineNum: Int? = null,
  val newLineNum: Int? = null
)

data class SplitDiffRow(val left: DiffLine?, val right: DiffLine?)

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
