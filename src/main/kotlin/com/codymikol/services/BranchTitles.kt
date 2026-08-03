package com.codymikol.services

import com.codymikol.data.map.BranchTitle

/**
 * Places branch titles above the lanes of the map (see issue #307). Mirrors
 * [BranchTipNodes]: each ordered tip is pinned to the lane its commit already occupies,
 * so a title's first character sits directly above its node. A tip whose commit has not
 * paged in yet has no lane and is skipped; when two tips resolve to the same lane only
 * the first (mainline-priority) one keeps the lane so it never shows two titles.
 */
object BranchTitles {

    fun place(tips: List<OrderedBranchTip>, lanesBySha: Map<String, Int>): List<BranchTitle> {
        val seenLanes = mutableSetOf<Int>()
        return tips.mapNotNull { tip ->
            val lane = lanesBySha[tip.sha] ?: return@mapNotNull null
            if (!seenLanes.add(lane)) return@mapNotNull null
            BranchTitle(lane, tip.branchNames)
        }
    }
}
