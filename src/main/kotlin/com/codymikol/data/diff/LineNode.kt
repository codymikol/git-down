package com.codymikol.data.diff

class LineNode(
    val line: Line
) {

    lateinit var parent: HunkNode

    companion object {

        fun make(line: Line): LineNode {
            return LineNode(line)
        }

    }

}