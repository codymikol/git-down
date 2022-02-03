package data.diff

class Diff(
    val hunks: List<Hunk>
) {

    companion object {
        fun make(gitDiff: String): Diff {

            val lines = gitDiff.split("\n")

            val lineNumberDelimiterIndices = mutableListOf<Int>()

            lines.forEachIndexed { index, line ->
                if (line.getOrNull(0) == '@') lineNumberDelimiterIndices.add(index)
            }

            val chunks = mutableListOf<Hunk>()

            lineNumberDelimiterIndices.forEachIndexed{ index, thisIndex ->

                val isLast = lineNumberDelimiterIndices.getOrNull(index + 1) == null

                if(isLast) {
                    chunks.add(Hunk.make(lines.subList(thisIndex, lines.size - 1)))
                } else {
                    val nextIndex = lineNumberDelimiterIndices[index + 1]
                    chunks.add(Hunk.make(lines.subList(thisIndex, nextIndex)))
                }

            }

            return Diff(chunks)

        }
    }

}