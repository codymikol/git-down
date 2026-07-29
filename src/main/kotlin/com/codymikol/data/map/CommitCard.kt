package com.codymikol.data.map

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

/**
 * Formats the three lines shown on the floating card that appears when a commit
 * node is clicked in the map view (see issue #252):
 *
 *   <short sha>: <commit message>
 *   Author: <name> <<email>>
 *   Date: MM/DD/YY, h:mm AM/PM <relative>
 *
 * where <relative> is the single most-relevant human label for how long ago the
 * commit was authored (Today / Yesterday / X Days Ago / X Months Ago / X Years Ago).
 */
object CommitCard {

    private const val MILLIS_PER_DAY = 1000L * 60 * 60 * 24
    private const val DAYS_PER_MONTH = 30
    private const val DAYS_PER_YEAR = 365

    fun title(node: CommitGraphNode): String = "${node.shortSha}: ${node.shortMessage}"

    fun author(node: CommitGraphNode): String = "Author: ${node.authorName} <${node.authorEmail}>"

    fun date(date: Date, now: Date): String {
        val datePart = SimpleDateFormat("MM/dd/yy").format(date)
        val timePart = SimpleDateFormat("h:mm a").format(date)
        return "Date: $datePart, $timePart ${relative(date, now)}"
    }

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
