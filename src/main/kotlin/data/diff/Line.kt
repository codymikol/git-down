package data.diff

class Line(
    val type: LineType,
    val value: String,
) {

    companion object {
        fun make(line: String): Line {

            val type = when(line[0]) {
                '+' -> LineType.Added
                '-' -> LineType.Removed
                ' ' -> LineType.Unchanged
                else -> LineType.Unknown
            }

            return Line(type, line)

        }
    }

}