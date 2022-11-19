package com.codymikol.components.commit.diff.file.header.action.buttons

import androidx.compose.runtime.Composable
import com.codymikol.data.diff.FileDeltaNode
import com.codymikol.extensions.unstageFile
import com.codymikol.state.GitDownState

@Composable
fun UnstageFileButton(fileDeltaNode: FileDeltaNode) = FileHeaderButton("Unstage File") {
    GitDownState.git.value.unstageFile(fileDeltaNode)
    GitDownState.selectedFiles.remove(fileDeltaNode.fileDelta)
}