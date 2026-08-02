package com.codymikol.services

import com.codymikol.data.map.CommitGraphNode

/**
 * Packs each lane's commits tight to the top of the map (see issue #305). Rather
 * than positioning a node at its global row in the merged reverse-topological walk
 * - which leaves gaps wherever a sibling lane's commit sits - every lane's own
 * commits stack down consecutive rows from row 0, independent of the other lanes.
 * Connectors (see [MapConnectors]) then bridge a child's packed row to its parent's,
 * showing which node a commit forks off of.
 *
 * Returns the packed row of each commit keyed by sha, walking [commits] in list
 * (newest-to-oldest) order and giving each the next free row within its assigned
 * lane. A commit whose parent pagination hasn't reached - and so has no lane in
 * [lanesBySha] yet - is skipped; it packs in on its own once loadMore() assigns it
 * a lane, the same way [MapConnectors] defers an unloaded parent.
 */
object LanePacker {

    fun pack(commits: List<CommitGraphNode>, lanesBySha: Map<String, Int>): Map<String, Int> {
        val nextRowByLane = mutableMapOf<Int, Int>()
        val rowBySha = mutableMapOf<String, Int>()

        commits.forEach { commit ->
            val lane = lanesBySha[commit.sha] ?: return@forEach
            val row = nextRowByLane.getOrDefault(lane, 0)
            rowBySha[commit.sha] = row
            nextRowByLane[lane] = row + 1
        }

        return rowBySha
    }
}
