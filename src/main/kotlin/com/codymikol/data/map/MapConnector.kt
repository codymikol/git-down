package com.codymikol.data.map

/**
 * A parent/child edge to draw over the map's lanes. childRow/parentRow are the
 * commits' packed rows within their lanes (see issue #305 / [com.codymikol.services.LanePacker]),
 * not their global rows in the merged walk, so a connector reaches the node where
 * it actually sits after each lane is packed tight to the top.
 */
data class MapConnector(
    val childRow: Int,
    val childLane: Int,
    val parentRow: Int,
    val parentLane: Int,
)
