package com.codymikol.components.commit.diff.file.header.icons

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.codymikol.components.commit.FileIcon
import com.codymikol.data.file.FileDelta

@Composable
fun FileHeaderIcon(fileDelta: FileDelta) = FileIcon(
    modifier = Modifier.shadow(elevation = 3.dp, shape = RoundedCornerShape(14.dp)),
    fileDelta = fileDelta
)
