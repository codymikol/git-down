package com.codymikol.data.map

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevWalk

/**
 * Wraps a single JGit RevWalk over a branch's history, draining it in one call.
 */
class CommitHistoryWalker(git: Git, ref: Ref) : AutoCloseable {

    private val walk = RevWalk(git.repository).also { it.markStart(it.parseCommit(ref.objectId)) }

    fun loadAll(): List<CommitGraphNode> = generateSequence { walk.next() }
        .map(CommitGraphNode::make)
        .toList()

    override fun close() = walk.close()
}
