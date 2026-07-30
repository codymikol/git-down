package com.codymikol.services

import com.codymikol.data.map.CommitGraphNode

/**
 * Assigns each commit in a newest-to-oldest (reverse topological) walk to a
 * lane/column number, GitUp-style: a lane tracks an ancestry lineage, not a
 * branch. Stateful and streaming so callers can feed it paginated batches -
 * see issue #269 - and get the same lane numbers as a single call over the
 * whole history.
 */
class LaneAssigner {

    private val laneAwaitingSha = mutableMapOf<String, MutableList<Int>>()
    private val freeLanes = sortedSetOf<Int>()
    private var laneCount = 0

    /**
     * Assigns lanes to [commits], which must already be in reverse
     * topological (newest-to-oldest, child-before-parent) order.
     */
    fun assign(commits: List<CommitGraphNode>): List<Int> = commits.map { assign(it) }

    /**
     * Assigns a lane to a single [commit]. Callers must feed commits in
     * reverse topological order one page at a time; state carries over
     * between calls, so splitting a history across several calls yields the
     * same lanes as one call over the whole thing.
     */
    fun assign(commit: CommitGraphNode): Int {
        val waitingLanes = laneAwaitingSha.remove(commit.sha)
        val lane = waitingLanes?.min() ?: allocateLane()
        waitingLanes?.filter { it != lane }?.forEach { freeLanes.add(it) }

        val parents = commit.parentShas
        if (parents.isEmpty()) {
            freeLanes.add(lane)
        } else {
            awaitOn(parents[0], lane)
            // Extra parents (merge commits) each need their own lane; if a
            // sibling parent just freed one above, it's fair game for reuse
            // here too - a lane is free the moment nothing awaits it.
            for (i in 1 until parents.size) {
                awaitOn(parents[i], allocateLane())
            }
        }

        return lane
    }

    private fun allocateLane(): Int {
        val reused = freeLanes.firstOrNull() ?: return laneCount++
        freeLanes.remove(reused)
        return reused
    }

    private fun awaitOn(sha: String, lane: Int) {
        laneAwaitingSha.getOrPut(sha) { mutableListOf() }.add(lane)
    }
}
