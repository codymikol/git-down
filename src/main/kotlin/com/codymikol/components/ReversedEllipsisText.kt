package com.codymikol.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Truncates [text] from the front, keeping its trailing characters intact, so that
 * [ellipsis] + the remaining suffix fits within [maxWidthPx] as reported by [measure].
 * Pulled out as a pure function so the truncation logic can be unit tested without a
 * Compose measuring environment.
 */
fun reversedEllipsis(
    text: String,
    maxWidthPx: Float,
    measure: (String) -> Float,
    ellipsis: String = "...",
): String {
    if (maxWidthPx <= 0f || text.isEmpty()) return text
    if (measure(text) <= maxWidthPx) return text

    if (measure(ellipsis) > maxWidthPx) return ellipsis

    var low = 0
    var high = text.length
    var best = 0

    while (low <= high) {
        val mid = (low + high) / 2
        val suffix = text.takeLast(mid)
        if (measure(ellipsis + suffix) <= maxWidthPx) {
            best = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }

    return ellipsis + text.takeLast(best)
}

@Composable
fun ReversedEllipsisText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    fontSize: TextUnit = 12.sp,
    fontWeight: FontWeight = FontWeight.Medium,
    ellipsis: String = "...",
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = remember(color, fontSize, fontWeight) {
        TextStyle(color = color, fontSize = fontSize, fontWeight = fontWeight)
    }

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = with(density) { maxWidth.toPx() }

        val displayText = remember(text, maxWidthPx, style, ellipsis) {
            reversedEllipsis(
                text = text,
                maxWidthPx = maxWidthPx,
                measure = { candidate -> textMeasurer.measure(candidate, style).size.width.toFloat() },
                ellipsis = ellipsis,
            )
        }

        Text(
            text = displayText,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            softWrap = false,
            maxLines = 1,
        )
    }
}

@Composable
@Preview
fun PreviewReversedEllipsisText() = ReversedEllipsisText(
    "src/main/kotlin/com/codymikol/components/commit/diff/file/header/text/FileHeaderText.kt",
    modifier = Modifier,
)
