package com.codymikol.components.commit.diff.file.header.action.buttons

import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import com.codymikol.components.menu.MenuColors
import com.codymikol.data.Colors
import com.codymikol.data.diff.FileDeltaNode

@Composable
fun FileHeaderCogButtonWorkingDirectory(fileDeltaNode: FileDeltaNode) = FileHeaderCogButton { dismiss ->
    FileActionMenuItem("View in Diff Tool", "workingDirectory.viewInDiffTool", dismiss)
    Divider(color = MenuColors.Divider)
    FileActionMenuItem("Open File", "workingDirectory.openFile", dismiss)
    FileActionMenuItem("Show In Files", "workingDirectory.showInFiles", dismiss)
    Divider(color = MenuColors.Divider)
    FileActionMenuItem("Delete File", "workingDirectory.deleteFile", dismiss)
}
