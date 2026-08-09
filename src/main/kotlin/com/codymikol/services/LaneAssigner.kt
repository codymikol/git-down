package com.codymikol.services

import com.codymikol.data.map.CommitGraphNode

/**
 * Assigns each commit in a newest-to-oldest (reverse topological) walk to a
 * lane/column number, GitUp-style: a lane tracks an ancestry lineage. Stateful
 * and streaming so callers can feed it paginated batches - see issue #269 - and
 * get the same lane numbers as a single call over the whole history.
 *
 * [tipLanes] pins a branch tip's commit to a reserved lane so every branch is
 * represented by its own lane even when one tip is an ancestor of another (see
 * issue #315): a tip whose commit would otherwise inherit a descendant's lineage
 * lane is forced onto its own reserved column instead, so no two branch tips
 * ever collapse onto the same lane. [allocateLane] counts above the reserved range,
 * so an unrelated lineage can't squat on a tip's column before the walk reaches that
 * tip - a reserved lane only rejoins the free pool once its own tip has been placed.
 * An empty map restores the plain lineage behaviour.
 */
class LaneAssigner(private val tipLanes: Map<String, Int> = emptyMap()) {

    private val laneAwaitingSha = mutableMapOf<String, MutableList<Int>>()
    private val freeLanes = sortedSetOf<Int>()
    private var laneCount = tipLanes.values.maxOrNull()?.plus(1) ?: 0

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
        // A branch tip always claims its reserved lane, overriding any lineage
        // lane a descendant was awaiting it on, so every branch keeps its own
        // column (#315); those orphaned awaiting lanes fall back into the pool.
        val lane = tipLanes[commit.sha] ?: waitingLanes?.min() ?: allocateLane()
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
