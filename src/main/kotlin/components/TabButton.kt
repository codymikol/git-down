package components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import data.Colors
import tabs.Tab

private class TabButtonColors(val selected: Boolean) : ButtonColors {

    private fun getButtonBackground(): Color = when (selected) {
        true -> Colors.LightGrayBackground
        false -> Colors.DarkGrayBackground
    }

    @Composable
    override fun backgroundColor(enabled: Boolean): State<Color> = mutableStateOf(getButtonBackground())

    @Composable
    override fun contentColor(enabled: Boolean): State<Color> = mutableStateOf(Colors.LightGrayText)
}

@Composable
@Preview
fun tabButton(currentTab: MutableState<Tab>, thisTab: Tab, resourceLocation: String, description: String) =
    Button(
        onClick = { currentTab.value = thisTab },
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(Dp(0F), color = Color.Black),
        colors = TabButtonColors(currentTab.value == thisTab),
        modifier = Modifier.width(38.dp).height(30.dp).padding(1.dp).border(width = 0.dp, color = Color.Transparent)
    ) {
        Image(
            modifier = Modifier.scale(3.0f),
            painter = painterResource(resourcePath = resourceLocation),
            contentDescription = description,
        )
    }
