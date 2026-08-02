package com.codymikol.data.map

import org.eclipse.jgit.revwalk.RevCommit
import java.util.Date

data class CommitGraphNode(
    val sha: String,
    val shortSha: String,
    val shortMessage: String,
    val authorName: String,
    val authorEmail: String,
    val date: Date,
    val parentShas: List<String>,
    // The full, multi-line commit message, used to pre-populate the "Edit Message..."
    // modal (#299). Defaults to shortMessage so the many single-line test fixtures that
    // build nodes directly need not supply it.
    val fullMessage: String = shortMessage,
) {

    val isMergeCommit: Boolean
        get() = parentShas.size > 1

    companion object {
        fun make(revCommit: RevCommit): CommitGraphNode = CommitGraphNode(
            sha = revCommit.name,
            shortSha = revCommit.name.take(7),
            shortMessage = revCommit.shortMessage,
            authorName = revCommit.authorIdent?.name ?: "",
            authorEmail = revCommit.authorIdent?.emailAddress ?: "",
            date = Date(revCommit.commitTime.toLong() * 1000),
            parentShas = revCommit.parents.map { it.name },
            fullMessage = revCommit.fullMessage,
        )
    }
}
