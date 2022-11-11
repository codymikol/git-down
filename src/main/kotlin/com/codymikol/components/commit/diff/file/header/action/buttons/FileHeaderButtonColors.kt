package com.codymikol.components.commit.diff.file.header.colors

import androidx.compose.material.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

class HeaderButtonColors : ButtonColors {
    @Composable
    override fun backgroundColor(enabled: Boolean): State<Color> {
        return mutableStateOf(Color.Transparent)
    }

    @Composable
    override fun contentColor(enabled: Boolean): State<Color> {
        return mutableStateOf(Color.White)
    }

}
