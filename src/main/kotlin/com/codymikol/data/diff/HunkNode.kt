package com.codymikol.data.diff

import com.codymikol.data.diff.Hunk
import com.codymikol.data.diff.Line

data class HunkNode(
    val hunk: Hunk,
    val lines: List<Line>,
)