package com.codymikol.services

import com.codymikol.data.map.BranchTipNode

/**
 * Places branch-tip nodes across the top row of the map (see issue #291). Each ordered
 * tip is pinned to the lane its commit already occupies in the graph, so the top nodes
 * line up with the lanes cascading down into the mainline. Tips whose commit pagination
 * hasn't loaded yet have no lane and are skipped; if two tips resolve to the same lane
 * only the first (mainline-priority) one is kept so a lane never gets two top nodes.
 */
object BranchTipNodes {

    fun place(tips: List<OrderedBranchTip>, lanesBySha: Map<String, Int>): List<BranchTipNode> {
        val seenLanes = mutableSetOf<Int>()
        return tips.mapNotNull { tip ->
            val lane = lanesBySha[tip.sha] ?: return@mapNotNull null
            if (!seenLanes.add(lane)) return@mapNotNull null
            BranchTipNode(tip.sha, lane)
        }
    }
}
