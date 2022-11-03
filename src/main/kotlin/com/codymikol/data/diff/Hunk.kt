package com.codymikol.data.diff

import com.codymikol.data.diff.LineType.*

class Hunk(
    val delimiter: String,
    val lines: List<Line>,
    val header: HunkHeader,
) {

    companion object {

        fun make(hunkLines: List<String>): Hunk {

            val delimiter = hunkLines[0]

            val header = HunkHeader.make(delimiter)

            var originalLineNumberIncrementer = header.fromFileLineNumbersStart
            var newLineNumberIncrementer = header.fromFileLineNumbersStart

            val lines = hunkLines.drop(1).map {

                val line = Line.make(it)

                line.originalLineNumber = when(line.type) {
                    Removed, Unchanged -> originalLineNumberIncrementer++
                    Unknown, Added, NoNewline -> null
                }

                line.newLineNumber = when(line.type) {
                    Unchanged, Added -> newLineNumberIncrementer++
                    Removed, Unknown, NoNewline -> null
                }

                line

            }

            return Hunk(delimiter, lines, header)

        }

    }

}