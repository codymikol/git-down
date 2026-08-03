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
    // #307: branch titles are pinned above their lane's tip node. titlePadding is the
    // vertical gap between a title and the node it labels; titleMaxWidth caps a title's
    // width before it truncates with an ellipsis. Both are live-tunable like the rest.
    val titlePadding: Float = 14f,
    val titleMaxWidth: Float = 160f,
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
        /**
         * Renders a slider value the way the debug menu shows it: at most two decimals,
         * dropping a trailing ".0" so whole numbers read as plain integers.
         */
        fun format(value: Float): String {
            val rounded = Math.round(value * 100f) / 100f
            return if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
        }

        /**
         * A newline-separated dump of every debug key and its current value, in slider
         * order, for the debug menu's "print to console" button (#309).
         */
        fun summarize(dimensions: MapDimensions): String =
            sliders.joinToString("\n") { "${it.label}: ${format(it.get(dimensions))}" }

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
            Slider("titlePadding", 0f, 48f, { it.titlePadding }, { d, v -> d.copy(titlePadding = v) }),
            Slider("titleMaxWidth", 40f, 400f, { it.titleMaxWidth }, { d, v -> d.copy(titleMaxWidth = v) }),
            Slider("mainlineGap", 0f, 64f, { it.mainlineGap }, { d, v -> d.copy(mainlineGap = v) }),
            Slider("forkCurveTension", 0f, 1f, { it.forkCurveTension }, { d, v -> d.copy(forkCurveTension = v) }),
        )
    }
}
