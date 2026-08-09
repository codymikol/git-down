package com.codymikol.services

import com.codymikol.data.map.BranchTipNode

/**
 * Places branch-tip nodes across the top row of the map (see issue #291). Each ordered
 * tip is pinned to the lane its commit already occupies in the graph, so the top nodes
 * line up with the lanes cascading down into the mainline. Every branch reserves its own
 * lane (see issue #315), so distinct tips never collide - one node is placed per branch.
 * Tips whose commit pagination hasn't loaded yet have no lane and are skipped.
 */
object BranchTipNodes {

    fun place(tips: List<OrderedBranchTip>, lanesBySha: Map<String, Int>): List<BranchTipNode> =
        tips.mapNotNull { tip ->
            val lane = lanesBySha[tip.sha] ?: return@mapNotNull null
            BranchTipNode(tip.sha, lane)
        }
}
