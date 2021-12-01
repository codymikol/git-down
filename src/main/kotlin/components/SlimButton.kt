package components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.Colors


private class SlimButtonColors : ButtonColors {
    @Composable
    override fun backgroundColor(enabled: Boolean): State<Color> {
        return mutableStateOf(Colors.DisabledGray)
    }
    @Composable
    override fun contentColor(enabled: Boolean): State<Color> {
        return mutableStateOf(Color.White)
    }

}

@Composable
fun SlimButton(text: String, onClick: () -> Unit = {}) =
    Button(
        onClick = onClick,
        colors = SlimButtonColors(),
        modifier = Modifier.height(28.dp)
    ) { Text(text, fontSize = 12.sp) }

@Composable
@Preview
fun PreviewSlimButton() = SlimButton("Hello") { /** noop **/ }