package com.codymikol.components.commit.diff.file.header.text

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.codymikol.data.file.FileDelta

@Composable
fun FileHeaderText(fileDelta: FileDelta) = Text(
    fileDelta.location.toString(),
    color = Color.White,
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium
)