package com.codymikol.services

import com.codymikol.data.map.BranchTitle

/**
 * Places branch titles above the lanes of the map (see issue #307). Mirrors
 * [BranchTipNodes]: each ordered tip is pinned to the lane its commit already occupies,
 * so a title's first character sits directly above its node. Every branch reserves its
 * own lane (see issue #315), so distinct tips never collide - one title is placed per
 * branch. A tip whose commit has not paged in yet has no lane and is skipped.
 */
object BranchTitles {

    fun place(tips: List<OrderedBranchTip>, lanesBySha: Map<String, Int>): List<BranchTitle> =
        tips.mapNotNull { tip ->
            val lane = lanesBySha[tip.sha] ?: return@mapNotNull null
            BranchTitle(lane, tip.branchNames)
        }
}
