package data.diff

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
                    LineType.Removed -> originalLineNumberIncrementer++
                    LineType.Unknown -> null
                    LineType.Unchanged -> originalLineNumberIncrementer++
                    LineType.Added -> null
                }

                line.newLineNumber = when(line.type) {
                    LineType.Removed -> null
                    LineType.Unknown -> null
                    LineType.Unchanged -> newLineNumberIncrementer++
                    LineType.Added -> newLineNumberIncrementer++
                }

                line

            }

            return Hunk(delimiter, lines, header)

        }

    }

}