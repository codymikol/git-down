package com.codymikol.services

import com.codymikol.data.map.MapPoint

/**
 * The four points a single map connector is drawn from (see issue #291): rather than a
 * straight diagonal between two nodes, a connector runs straight down the child's lane
 * to a synthetic [bend] node, then a quadratic bezier ([control] is its control point)
 * forks across into the parent's lane. A same-lane connector's points all share the
 * child's x, so the bezier collapses to the plain vertical it used to be.
 */
data class ConnectorPath(
    val start: MapPoint,
    val bend: MapPoint,
    val control: MapPoint,
    val end: MapPoint,
    val isSameLane: Boolean,
)

object ConnectorGeometry {

    /**
     * Builds the [ConnectorPath] for a connector whose child node is at ([childX],
     * [childY]) and parent node at ([parentX], [parentY]). The vertical stops
     * [mainlineGap] px above the parent (never rising above the child), and the fork's
     * bezier control slides from the segment midpoint ([forkCurveTension] 0, a straight
     * diagonal) toward the crisp corner ([forkCurveTension] 1, a right angle).
     */
    fun path(
        childX: Float,
        childY: Float,
        parentX: Float,
        parentY: Float,
        mainlineGap: Float,
        forkCurveTension: Float,
    ): ConnectorPath {
        val bendY = maxOf(childY, parentY - mainlineGap)
        val bend = MapPoint(childX, bendY)
        val end = MapPoint(parentX, parentY)

        // Sharp corner (straight down, then a right angle into the parent's row) vs. the
        // plain midpoint of the fork segment; tension blends between the two.
        val corner = MapPoint(childX, parentY)
        val mid = MapPoint((bend.x + end.x) / 2f, (bend.y + end.y) / 2f)
        val control = MapPoint(
            lerp(mid.x, corner.x, forkCurveTension),
            lerp(mid.y, corner.y, forkCurveTension),
        )

        return ConnectorPath(
            start = MapPoint(childX, childY),
            bend = bend,
            control = control,
            end = end,
            isSameLane = childX == parentX,
        )
    }

    private fun lerp(from: Float, to: Float, t: Float) = from + (to - from) * t
}
