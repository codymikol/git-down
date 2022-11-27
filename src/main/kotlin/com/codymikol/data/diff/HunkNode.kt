package com.codymikol.data.diff

data class HunkNode(
    val hunk: Hunk,
    val lineNodes: List<LineNode>,
) {

    lateinit var parent: FileDeltaNode
    fun isSelectingLines() = lineNodes.any { it.line.selected.value }

    fun getSelectedLines() = lineNodes.filter { it.line.selected.value }

    companion object {
        fun make(hunk: Hunk): HunkNode = HunkNode(hunk, hunk.lines.map(LineNode.Companion::make))

    }

}