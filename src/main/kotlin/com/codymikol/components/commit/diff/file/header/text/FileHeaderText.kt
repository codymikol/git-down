package com.codymikol.components.commit.diff.file.header.text

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.codymikol.data.diff.FileDeltaNode

@Composable
fun FileHeaderText(fileDeltaNode: FileDeltaNode) = Text(
    fileDeltaNode.fileDelta.getPath(),
    color = Color.White,
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium
)