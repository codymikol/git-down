package com.codymikol.services

import java.util.Calendar
import java.util.Date

/**
 * The single most-relevant human label for how long ago [date] was, measured
 * against [now]: Today / Yesterday / X Days Ago / X Months Ago / X Years Ago.
 *
 * Extracted out of CommitCard (see issue #254) so any view - the commit detail
 * card, the quick view, and future surfaces - can render the same relative
 * timestamp from one place.
 */
object RelativeDateTime {

    private const val MILLIS_PER_DAY = 1000L * 60 * 60 * 24
    private const val DAYS_PER_MONTH = 30
    private const val DAYS_PER_YEAR = 365

    fun relative(date: Date, now: Date): String {
        val days = calendarDaysBetween(date, now)
        return when {
            days <= 0L -> "Today"
            days == 1L -> "Yesterday"
            days < DAYS_PER_MONTH -> "$days Days Ago"
            days < DAYS_PER_YEAR -> "${days / DAYS_PER_MONTH} Months Ago"
            else -> "${days / DAYS_PER_YEAR} Years Ago"
        }
    }

    /**
     * Whole calendar days from [from] to [to], measured from local midnight so that
     * a commit made late yesterday and a check made early today read as "Yesterday"
     * rather than collapsing to a sub-24h "Today".
     */
    private fun calendarDaysBetween(from: Date, to: Date): Long {
        val fromMidnight = atMidnight(from)
        val toMidnight = atMidnight(to)
        return (toMidnight - fromMidnight) / MILLIS_PER_DAY
    }

    private fun atMidnight(date: Date): Long = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
