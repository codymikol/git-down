package com.codymikol.components.commit.diff.file.header.action.buttons

import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import com.codymikol.components.menu.MenuColors
import com.codymikol.data.diff.FileDeltaNode

@Composable
fun FileHeaderCogButtonIndex(fileDeltaNode: FileDeltaNode) = FileHeaderCogButton { dismiss ->
    FileActionMenuItem("View in Diff Tool", "index.viewInDiffTool", dismiss)
    Divider(color = MenuColors.Divider)
    FileActionMenuItem("Open File", "index.openFile", dismiss)
    FileActionMenuItem("Show In Files", "index.showInFiles", dismiss)
}
