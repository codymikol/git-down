package data.diff

class HunkHeader(
    val deletedLineStart: Int,
    val deletedLineEnd: Int,
    val createdLineStart: Int,
    val createdLineEnd: Int
) {

    companion object  {

        fun make(hunkDelimiter: String): HunkHeader {

            val (delStart,delEnd,createStart,createEnd) = hunkDelimiter
                .drop(4)
                .dropLast(3)
                .replace(" +", ",")
                .split(",")
                .map { it.toInt() }

            return HunkHeader(delStart,delEnd,createStart,createEnd)
        }

    }

}