package com.codymikol.data.map

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevSort
import org.eclipse.jgit.revwalk.RevWalk

/**
 * Wraps a single JGit RevWalk seeded from every given ref's tip so a commit
 * reachable from more than one of them is still only ever visited once (see
 * #263) - the walk only advances as far as nextPage() has been asked to go.
 * TOPO sorting guarantees a commit is never emitted before its children even
 * when merging several starting points whose commit times don't strictly
 * decrease child-to-parent; COMMIT_TIME_DESC breaks ties between otherwise
 * unordered commits newest-first.
 */
class CommitHistoryWalker(git: Git, refs: List<Ref>) : AutoCloseable {

    private val walk = RevWalk(git.repository).also { walk ->
        walk.sort(RevSort.TOPO)
        walk.sort(RevSort.COMMIT_TIME_DESC, true)
        refs.forEach { walk.markStart(walk.parseCommit(it.objectId)) }
    }

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
