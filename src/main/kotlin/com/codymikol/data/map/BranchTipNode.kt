package com.codymikol.data.map

/**
 * A branch tip drawn as a node pinned to the top row of the map grid (see issue #291),
 * sitting in [lane] - the same lane its commit [sha] occupies in the graph below - so
 * the lanes visibly start at the top and cascade down into the mainline.
 */
data class BranchTipNode(val sha: String, val lane: Int)
