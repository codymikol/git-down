package com.codymikol.components.commit.diff.file.header.text

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.codymikol.data.diff.FileDeltaNode

@Composable
fun FileHeaderText(fileDeltaNode: FileDeltaNode) {

    val text = mutableStateOf(fileDeltaNode.fileDelta.getPath())

    Text(
        text.value,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
    )

}