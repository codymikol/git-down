package com.codymikol.data.diff

data class HunkNode(
    val hunk: Hunk,
    val lines: List<Line>,
) {
    fun isSelectingLines() = lines.any { it.selected.value }

}