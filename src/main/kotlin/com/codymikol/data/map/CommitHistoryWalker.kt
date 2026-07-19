package com.codymikol.data.map

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevWalk

/**
 * Wraps a single JGit RevWalk so a branch's history can be paged in incrementally
 * instead of walking the whole history up front - the walk only advances as far
 * as nextPage() has been asked to go.
 */
class CommitHistoryWalker(git: Git, ref: Ref) : AutoCloseable {

    private val walk = RevWalk(git.repository).also { it.markStart(it.parseCommit(ref.objectId)) }

    var hasMore = true
        private set

    fun nextPage(size: Int): List<CommitGraphNode> {
        if (!hasMore) return emptyList()

        val page = mutableListOf<CommitGraphNode>()

        while (page.size < size) {
            val commit = walk.next()
            if (commit == null) {
                hasMore = false
                break
            }
            page.add(CommitGraphNode.make(commit))
        }

        return page
    }

    override fun close() = walk.close()
}
