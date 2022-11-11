package com.codymikol.data.diff

import com.codymikol.data.file.FileDelta

data class FileDeltaNode(
    val fileDelta: FileDelta,
    val hunks: List<HunkNode>,
) {
    fun isSelectingLines(): Boolean = hunks.any { hunk -> hunk.isSelectingLines() }

}