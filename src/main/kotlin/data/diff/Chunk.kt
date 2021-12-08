package data.diff

class Chunk(
    val delimiter: String,
    val lines: List<Line>
) {

    companion object {

        fun make(chunkLines: List<String>): Chunk {

            val delimiter = chunkLines[0]

            val lines = chunkLines.drop(1).map { Line.make(it) }

            return Chunk(delimiter, lines)

        }

    }

}