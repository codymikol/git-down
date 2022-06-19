package com.codymikol.data.diff

import data.file.FileDelta

data class DiffTree(
    val files: List<FileDeltaNode>,
) {
    companion object {
        fun make(fileDeltas: List<FileDelta>): DiffTree = DiffTree(
            files = fileDeltas.map { fileDelta ->
                FileDeltaNode(
                    fileDelta,
                    fileDelta.getDiff().hunks.map { hunk ->
                        HunkNode(hunk, hunk.lines)
                    }
                )
            }
        )
    }
}