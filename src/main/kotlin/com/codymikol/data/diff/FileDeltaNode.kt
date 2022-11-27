package com.codymikol.data.diff

import com.codymikol.data.file.FileDelta

class FileDeltaNode(
    val fileDelta: FileDelta,
    val hunkNodes: List<HunkNode>
) {

    lateinit var parent: DiffTree
    fun getPath() = fileDelta.getPath()

    fun isSelectingLines(): Boolean = hunkNodes.any { hunk -> hunk.isSelectingLines() }

    fun getSelectedLines(): List<LineNode> = hunkNodes.flatMap { hunk
        -> hunk.getSelectedLines() }

    companion object {
        fun make(fileDelta: FileDelta): FileDeltaNode {

            val gitDiff = fileDelta.getDiff()

            val lines = gitDiff.split("\n")

            val lineNumberDelimiterIndices = mutableListOf<Int>()

            lines.forEachIndexed { index, line ->
                if (line.getOrNull(0) == '@') lineNumberDelimiterIndices.add(index)
            }

            val hunks = mutableListOf<Hunk>()

            lineNumberDelimiterIndices.forEachIndexed{ index, thisIndex ->

                val isLast = lineNumberDelimiterIndices.getOrNull(index + 1) == null

                if(isLast) {
                    hunks.add(Hunk.make(lines.subList(thisIndex, lines.size - 1)))
                } else {
                    val nextIndex = lineNumberDelimiterIndices[index + 1]
                    hunks.add(Hunk.make(lines.subList(thisIndex, nextIndex)))
                }

            }

            return FileDeltaNode(fileDelta, hunks.map(HunkNode.Companion::make))

        }
    }

}