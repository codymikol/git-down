package data.diff

class Line(
    val type: LineType,
    val value: String,
    val symbol: String,
    var originalLineNumber: Int?,
    var newLineNumber: Int?,
) {

    companion object {
        fun make(line: String): Line {

            val typeChar = line[0]

            val type = when(typeChar) {
                '+' -> LineType.Added
                '-' -> LineType.Removed
                ' ' -> LineType.Unchanged
                else -> LineType.Unknown
            }

            val symbol = when(type) {
               LineType.Added, LineType.Unchanged -> typeChar
                LineType.Removed -> "—"
               LineType.Unknown -> "?"
            }

            return Line(
                type,
                line.drop(1),
                symbol.toString(),
                null,
                null
            )

        }
    }

}