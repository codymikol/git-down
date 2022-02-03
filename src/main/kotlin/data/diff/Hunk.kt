package data.diff

class Hunk(
    val delimiter: String,
    val lines: List<Line>
) {

    companion object {

        fun make(hunkLines: List<String>): Hunk {

            val delimiter = hunkLines[0]

            val lines = hunkLines.drop(1).map { Line.make(it) }

            return Hunk(delimiter, lines)

        }

    }

}