package com.codymikol.data.map

import org.eclipse.jgit.revwalk.RevCommit
import java.util.Date

data class CommitGraphNode(
    val sha: String,
    val shortSha: String,
    val shortMessage: String,
    val authorName: String,
    val date: Date,
    val parentShas: List<String>,
) {

    val isMergeCommit: Boolean
        get() = parentShas.size > 1

    companion object {
        fun make(revCommit: RevCommit): CommitGraphNode = CommitGraphNode(
            sha = revCommit.name,
            shortSha = revCommit.name.take(7),
            shortMessage = revCommit.shortMessage,
            authorName = revCommit.authorIdent?.name ?: "",
            date = Date(revCommit.commitTime.toLong() * 1000),
            parentShas = revCommit.parents.map { it.name },
        )
    }
}
