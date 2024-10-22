package com.codymikol.components.commit.diff.file.header.icons

import androidx.compose.runtime.Composable
import com.codymikol.components.commit.FileIcon
import com.codymikol.data.diff.FileDeltaNode

@Composable
fun FileHeaderIcon(fileDeltaNode: FileDeltaNode) = FileIcon(
    fileDelta = fileDeltaNode.fileDelta
)
