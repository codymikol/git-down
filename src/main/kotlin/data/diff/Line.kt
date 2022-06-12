package data.diff

import data.diff.LineType.*

class Line(
    val type: LineType,
    val value: String,
    val symbol: String,
    var originalLineNumber: UInt?,
    var newLineNumber: UInt?,
) {

    companion object {
        fun make(line: String): Line {

            val type = when (line[0]) {
                '+' -> Added
                '-' -> Removed
                ' ' -> Unchanged
                '\\' -> NoNewline
                else -> Unknown
            }

            val symbol = when (type) {
                Added -> "+"
                Removed -> "—"
                Unknown -> "?"
                NoNewline, Unchanged -> " "
            }

            return Line(
                type,
                line.drop(1),
                symbol,
                null,
                null
            )

        }
    }

}