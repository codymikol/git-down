package com.codymikol.data.map

/**
 * Every tunable "magic number" the map view draws with (see issue #291), collected in
 * one place so the debug menu (ctrl+shift+d) can adjust them live. Values are plain
 * Float magnitudes - dp for lengths, sp for font sizes, px for stroke widths - so a
 * slider can drive them directly; the map view applies the right unit at each use.
 */
data class MapDimensions(
    val laneWidth: Float = 180f,
    val nodeRadius: Float = 5f,
    val gutterX: Float = 20f,
    val rowHeight: Float = 48f,
    val connectorStrokeWidth: Float = 2f,
    val cardTabSize: Float = 22f,
    val cardHoleSize: Float = 8f,
    val cardCorner: Float = 10f,
    val pillCorner: Float = 8f,
    val pillFontSize: Float = 10f,
    val pillSpacing: Float = 4f,
    val pillMaxWidth: Float = 80f,
    // #291: how far above a node the incoming vertical connector stops, so the final
    // bezier fork slants down into the node rather than meeting it dead vertical.
    val mainlineGap: Float = 24f,
    // #291: sharpness of the fork's bezier, 0 (a straight diagonal) .. 1 (a crisp
    // right-angle corner). Higher reads as the "somewhat sharp" curve the issue asks for.
    val forkCurveTension: Float = 0.85f,
) {

    /**
     * One adjustable dimension as the debug menu sees it: a label, the inclusive slider
     * range, and pure get/set lenses over [MapDimensions] so the menu never has to know
     * which field it is driving.
     */
    data class Slider(
        val label: String,
        val min: Float,
        val max: Float,
        val get: (MapDimensions) -> Float,
        val set: (MapDimensions, Float) -> MapDimensions,
    )

    companion object {
        val sliders: List<Slider> = listOf(
            Slider("laneWidth", 60f, 320f, { it.laneWidth }, { d, v -> d.copy(laneWidth = v) }),
            Slider("nodeRadius", 2f, 16f, { it.nodeRadius }, { d, v -> d.copy(nodeRadius = v) }),
            Slider("gutterX", 4f, 80f, { it.gutterX }, { d, v -> d.copy(gutterX = v) }),
            Slider("rowHeight", 24f, 96f, { it.rowHeight }, { d, v -> d.copy(rowHeight = v) }),
            Slider("connectorStrokeWidth", 1f, 8f, { it.connectorStrokeWidth }, { d, v -> d.copy(connectorStrokeWidth = v) }),
            Slider("cardTabSize", 10f, 40f, { it.cardTabSize }, { d, v -> d.copy(cardTabSize = v) }),
            Slider("cardHoleSize", 2f, 20f, { it.cardHoleSize }, { d, v -> d.copy(cardHoleSize = v) }),
            Slider("cardCorner", 2f, 24f, { it.cardCorner }, { d, v -> d.copy(cardCorner = v) }),
            Slider("pillCorner", 2f, 20f, { it.pillCorner }, { d, v -> d.copy(pillCorner = v) }),
            Slider("pillFontSize", 6f, 20f, { it.pillFontSize }, { d, v -> d.copy(pillFontSize = v) }),
            Slider("pillSpacing", 1f, 16f, { it.pillSpacing }, { d, v -> d.copy(pillSpacing = v) }),
            Slider("pillMaxWidth", 40f, 200f, { it.pillMaxWidth }, { d, v -> d.copy(pillMaxWidth = v) }),
            Slider("mainlineGap", 0f, 64f, { it.mainlineGap }, { d, v -> d.copy(mainlineGap = v) }),
            Slider("forkCurveTension", 0f, 1f, { it.forkCurveTension }, { d, v -> d.copy(forkCurveTension = v) }),
        )
    }
}
