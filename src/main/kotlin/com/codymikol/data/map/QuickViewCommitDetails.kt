package com.codymikol.data.map

import com.codymikol.services.RelativeDateTime
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Formats the lines shown in the quick view's left panel (see issue #254):
 *
 *   <full sha>
 *   <commit message>
 *   <committer name> <<email>>
 *   MM/DD/YY, h:mm AM/PM <relative>
 *
 * Unlike CommitCard this exposes the full sha and drops the "Author:" / "Date:"
 * prefixes, since the quick view lays each value out on its own line.
 */
object QuickViewCommitDetails {

    fun hash(node: CommitGraphNode): String = node.sha

    fun message(node: CommitGraphNode): String = node.shortMessage

    fun committer(node: CommitGraphNode): String = "${node.authorName} <${node.authorEmail}>"

    fun date(date: Date, now: Date): String {
        val datePart = SimpleDateFormat("MM/dd/yy").format(date)
        val timePart = SimpleDateFormat("h:mm a").format(date)
        return "$datePart, $timePart ${RelativeDateTime.relative(date, now)}"
    }
}
