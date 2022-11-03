package com.codymikol.data.diff

enum class LineModification {
    Added,
    Removed
}

class HunkHeader(
    val fromFileLineNumbersStart: UInt,
    val fromFileLineNumbersCount: UInt?,
    val fromFileLineModification: LineModification,
    val toFileLineNumbersStart: UInt,
    val toFileLineNumbersCount: UInt?,
    val toFileLineModification: LineModification
) {

    companion object {

        fun make(hunkDelimiter: String): HunkHeader {

            val pieces = hunkDelimiter.split(" ")

            val fromSection = pieces[1]
            val toSection = pieces[2]

            fun getModificationType(part: String): LineModification = when(val sign = part[0]) {
                '+' -> LineModification.Added
                '-' -> LineModification.Removed
                else -> error("Incorrect hunk header sign [$sign]")
            }

            fun getStartCountModification(fromSection: String): Triple<String, String?, LineModification> =
                if (fromSection.contains(",")) {
                fromSection
                    .split(",")
                    .let {
                        val modification = getModificationType(it[0])
                        Triple(it[0].drop(1), it[1], modification)
                    }
            } else {
                Triple(fromSection.drop(1), null, getModificationType(fromSection))
            }

            val (fromStart, fromCount, fromMod) = getStartCountModification(fromSection)
            val (toStart, toCount, toMod) = getStartCountModification(toSection)

            return HunkHeader(
                fromStart.toUInt(),
                fromCount?.toUInt(),
                fromMod,
                toStart.toUInt(),
                toCount?.toUInt(),
                toMod
            )
        }

    }

}

