package com.codymikol.data.map

import com.codymikol.services.RelativeDateTime
import java.text.SimpleDateFormat
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
 * commit was authored, supplied by [RelativeDateTime].
 */
object CommitCard {

    fun title(node: CommitGraphNode): String = "${node.shortSha}: ${node.shortMessage}"

    fun author(node: CommitGraphNode): String = "Author: ${node.authorName} <${node.authorEmail}>"

    fun date(date: Date, now: Date): String {
        val datePart = SimpleDateFormat("MM/dd/yy").format(date)
        val timePart = SimpleDateFormat("h:mm a").format(date)
        return "Date: $datePart, $timePart ${RelativeDateTime.relative(date, now)}"
    }
}
