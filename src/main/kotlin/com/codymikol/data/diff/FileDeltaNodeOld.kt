package com.codymikol.data.diff

import com.codymikol.data.file.FileDelta

data class FileDeltaNodeOld(
    val fileDelta: FileDelta,
    val hunks: List<HunkNode>,
) {

    lateinit var parent: DiffTree


}