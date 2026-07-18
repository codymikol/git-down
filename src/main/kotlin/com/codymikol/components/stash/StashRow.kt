package com.codymikol.components.stash

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codymikol.data.Colors
import com.codymikol.data.stash.StashListItem

@Composable
fun StashRow(stash: StashListItem, selected: Boolean, onClick: () -> Unit) {

    val backgroundColor = when (selected) {
        true -> Color(0, 89, 207)
        false -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            stash.title,
            color = Color.White,
            fontSize = 12.sp,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            stash.body,
            color = Colors.LightGrayText,
            fontSize = 11.sp,
        )
    }
}
