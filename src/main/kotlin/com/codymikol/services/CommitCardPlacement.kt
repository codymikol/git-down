package com.codymikol.services

import com.codymikol.data.map.MapDimensions
import com.codymikol.data.map.MapPoint

/**
 * Where the selected node's floating detail card sits when it is drawn as an overlay
 * across the whole map rather than inside its own lane (see issue #297). Rendering the
 * card inside its [CommitNode] clipped it to a single lane's width; an overlay positioned
 * by this pure formula escapes that bound so a long commit line can spill over the lanes
 * to its right instead of being cut off.
 *
 * The returned [MapPoint]'s x/y are dp magnitudes (not the pixels [MapPoint] is otherwise
 * used for) locating the card's top-left corner in the map body's coordinate space, where
 * lane 0 starts at x = 0 and the first visible row starts at y = -firstVisibleOffset. The
 * caller wraps the card in a [dimensions.rowHeight]-tall box so it centres over its row,
 * matching how the in-lane card used to align.
 */
object CommitCardPlacement {

    /**
     * Places the card for the node at [selectedIndex] in lane [lane], given the list's
     * current scroll ([firstVisibleIndex] and [firstVisibleOffset], both in dp). The x
     * matches the old in-lane alignment: the lane's left edge plus the gutter, pulled
     * back half a tab so the round tab still sits over the node dot.
     */
    fun place(
        selectedIndex: Int,
        lane: Int,
        firstVisibleIndex: Int,
        firstVisibleOffset: Float,
        dimensions: MapDimensions,
    ): MapPoint {
        val x = lane * dimensions.laneWidth + dimensions.gutterX - dimensions.cardTabSize / 2f
        val y = (selectedIndex - firstVisibleIndex) * dimensions.rowHeight - firstVisibleOffset
        return MapPoint(x, y)
    }
}
