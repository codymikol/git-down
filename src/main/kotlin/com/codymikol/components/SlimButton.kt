package com.codymikol.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codymikol.data.Colors


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
fun SlimButton(
    text: String,
    disabled: Boolean = false,
    onClick: () -> Unit = {},
) =
    Button(
        onClick = onClick,
        enabled = !disabled,
        colors = SlimButtonColors(),
        modifier = Modifier.height(28.dp)
    ) { Text(text, fontSize = 11.sp) }

@Composable
@Preview
fun PreviewSlimButton() = /** noop **/
    /** noop **/
    SlimButton("Hello") { /** noop **/ }