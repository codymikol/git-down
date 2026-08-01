package com.codymikol.state

import androidx.compose.runtime.mutableStateOf
import com.codymikol.data.map.MapDimensions

/**
 * Transient, map-only state for the #291 debug menu: the live [MapDimensions] the map
 * draws with and whether the menu overlay is open. Kept out of GitDownState - these are
 * throwaway tuning knobs, not project state - and toggled with ctrl+shift+d.
 */
object MapDebugState {

    val dimensions = mutableStateOf(MapDimensions())

    val isOpen = mutableStateOf(false)

    fun toggle() {
        isOpen.value = !isOpen.value
    }

    /** Drives a single dimension from its slider, leaving every other value untouched. */
    fun set(slider: MapDimensions.Slider, value: Float) {
        dimensions.value = slider.set(dimensions.value, value)
    }

    fun reset() {
        dimensions.value = MapDimensions()
        isOpen.value = false
    }
}
