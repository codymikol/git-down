package com.codymikol.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Every BranchLane windows its rows against this one shared vertical offset instead
 * of each lane owning a LazyListState - sharing a LazyListState across lanes crashes
 * the app (see #260 / #255), so lanes stay in lock-step by reading the same plain
 * offset instead of a shared framework scroll state.
 */
object MapScrollState {

    var offsetPx by mutableStateOf(0f)
        private set

    fun scrollBy(deltaPx: Float, maxOffsetPx: Float) {
        offsetPx = (offsetPx + deltaPx).coerceIn(0f, maxOffsetPx.coerceAtLeast(0f))
    }

    fun firstVisibleIndex(rowHeightPx: Float): Int =
        if (rowHeightPx <= 0f) 0 else floor(offsetPx / rowHeightPx).toInt().coerceAtLeast(0)

    fun visibleRowCount(viewportHeightPx: Float, rowHeightPx: Float): Int =
        if (rowHeightPx <= 0f) 0 else ceil(viewportHeightPx / rowHeightPx).toInt() + 1

    fun reset() {
        offsetPx = 0f
    }
}
