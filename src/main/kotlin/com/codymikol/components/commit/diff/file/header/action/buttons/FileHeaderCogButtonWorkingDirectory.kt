package com.codymikol.components.commit.diff.file.header.action.buttons

import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.codymikol.components.commit.ConfirmDialog
import com.codymikol.components.menu.MenuColors
import com.codymikol.components.menu.ThemedDropdownMenuItem
import com.codymikol.data.diff.FileDeltaNode
import com.codymikol.extensions.deleteFile
import com.codymikol.extensions.discardFile
import com.codymikol.extensions.openFile
import com.codymikol.state.GitDownState
import kotlinx.coroutines.launch

@Composable
fun FileHeaderCogButtonWorkingDirectory(fileDeltaNode: FileDeltaNode) {

    val scope = rememberCoroutineScope()
    var isConfirmingDiscard by remember { mutableStateOf(false) }
    var isConfirmingDelete by remember { mutableStateOf(false) }

    FileHeaderCogButton { dismiss ->
        FileActionMenuItem("View in Diff Tool", dismiss) { diffToolService.launchDiffTool(fileDeltaNode.fileDelta) }
        Divider(color = MenuColors.Divider)
        FileActionMenuItem("Open File", dismiss) { GitDownState.git.value.openFile(fileDeltaNode.fileDelta.location) }
        FileActionMenuItem("Show In Files", dismiss) { fileSystemService.showInFiles(fileDeltaNode.fileDelta.location) }
        Divider(color = MenuColors.Divider)
        ThemedDropdownMenuItem(label = "Discard Changes", onClick = { isConfirmingDiscard = true; dismiss() })
        ThemedDropdownMenuItem(label = "Delete File", onClick = { isConfirmingDelete = true; dismiss() })
    }

    if (isConfirmingDiscard) {
        ConfirmDialog(
            title = "Discard Changes?",
            content = "This will discard changes to ${fileDeltaNode.getPath()}, are you sure?",
            onDismiss = { isConfirmingDiscard = false },
            onConfirm = {
                scope.launch {
                    GitDownState.git.value.discardFile(fileDeltaNode)
                    isConfirmingDiscard = false
                }
            }
        )
    }

    if (isConfirmingDelete) {
        ConfirmDialog(
            title = "Delete File?",
            content = "This will permanently delete ${fileDeltaNode.getPath()} from disk, are you sure?",
            onDismiss = { isConfirmingDelete = false },
            onConfirm = {
                scope.launch {
                    GitDownState.git.value.deleteFile(fileDeltaNode.getPath())
                    isConfirmingDelete = false
                }
            }
        )
    }
}
