package com.codymikol.data.diff

import data.diff.Hunk
import data.diff.Line

data class HunkNode(
    val hunk: Hunk,
    val lines: List<Line>,
)