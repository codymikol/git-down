package com.codymikol.components.stash

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codymikol.components.SlimButton
import com.codymikol.data.Colors

fun stashToolbarButtonClicked(buttonText: String) = println(buttonText)

@Composable
@Preview
fun StashBottomToolbar() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(48.dp)
            .background(Colors.MediumGrayBackground)
            .padding(horizontal = 8.dp)
    ) {
        SlimButton("Apply", onClick = { stashToolbarButtonClicked("Apply") })

        Row(verticalAlignment = Alignment.CenterVertically) {
            SlimButton("+", modifier = Modifier.padding(end = 8.dp), onClick = { stashToolbarButtonClicked("+") })
            SlimButton("-", onClick = { stashToolbarButtonClicked("-") })
        }
    }
}
