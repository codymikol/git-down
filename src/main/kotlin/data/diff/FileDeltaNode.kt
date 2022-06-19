package com.codymikol.data.diff

import data.file.FileDelta

data class FileDeltaNode(
    val fileDelta: FileDelta,
    val hunks: List<HunkNode>,
)