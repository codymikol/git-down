package com.codymikol.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codymikol.data.Colors
import com.codymikol.data.map.MapDimensions
import com.codymikol.state.MapDebugState

/**
 * The map's live-tuning overlay (see issue #291), toggled with ctrl+shift+d. One slider
 * per parametrized "magic number", each driving [MapDebugState] so the map redraws as
 * the value moves - a scratch pad for dialling the graph in, not persisted anywhere.
 */
@Composable
fun MapDebugMenu(modifier: Modifier = Modifier) {
    val dimensions = MapDebugState.dimensions.value

    Column(
        modifier = modifier
            .width(280.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Colors.MediumGrayBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Map debug (ctrl+shift+d)",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        LazyColumn(
            modifier = Modifier.heightIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
        ) {
            items(MapDimensions.sliders) { slider ->
                val value = slider.get(dimensions)

                Text(
                    text = "${slider.label}: ${formatValue(value)}",
                    color = Colors.LightGrayText,
                    fontSize = 10.sp,
                )
                Slider(
                    value = value,
                    onValueChange = { MapDebugState.set(slider, it) },
                    valueRange = slider.min..slider.max,
                    colors = SliderDefaults.colors(
                        thumbColor = Colors.FileAdded,
                        activeTrackColor = Colors.FileAdded,
                        inactiveTrackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.24f),
                    ),
                )
            }
        }
    }
}

private fun formatValue(value: Float): String {
    val rounded = Math.round(value * 100f) / 100f
    return if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
}
