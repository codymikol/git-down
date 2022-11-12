package com.codymikol.data.diff

data class HunkNode(
    val hunk: Hunk,
    val lines: List<LineNode>,
) {

    lateinit var parent: FileDeltaNode
    fun isSelectingLines() = lines.any { it.line.selected.value }

    companion object {
        fun make(hunk: Hunk): HunkNode = HunkNode(hunk, hunk.lines.map(LineNode.Companion::make))

    }

}