package com.codymikol.components.commit.diff.file.header.action.buttons

import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import com.codymikol.components.menu.MenuColors
import com.codymikol.data.diff.FileDeltaNode
import com.codymikol.extensions.openFile
import com.codymikol.state.GitDownState

@Composable
fun FileHeaderCogButtonIndex(fileDeltaNode: FileDeltaNode) = FileHeaderCogButton { dismiss ->
    FileActionMenuItem("View in Diff Tool", dismiss) { diffToolService.launchDiffTool(fileDeltaNode.fileDelta) }
    Divider(color = MenuColors.Divider)
    FileActionMenuItem("Open File", dismiss) { GitDownState.git.value.openFile(fileDeltaNode.fileDelta.location) }
    FileActionMenuItem("Show In Files", dismiss) { fileSystemService.showInFiles(fileDeltaNode.fileDelta.location) }
}
