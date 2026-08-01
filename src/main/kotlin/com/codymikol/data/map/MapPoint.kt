package com.codymikol.data.map

/**
 * A plain x/y point in the map canvas's pixel space. Kept free of Compose's Offset so
 * the connector geometry (see issue #291) stays pure and unit-testable; the map view
 * converts these to Offset at draw time.
 */
data class MapPoint(val x: Float, val y: Float)
