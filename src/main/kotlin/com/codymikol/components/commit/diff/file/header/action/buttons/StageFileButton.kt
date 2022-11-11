package com.codymikol.components.commit.diff.file.header.action.buttons

import androidx.compose.runtime.Composable
import com.codymikol.data.diff.FileDeltaNode
import com.codymikol.extensions.stageFile
import com.codymikol.state.GitDownState

@Composable
fun StageFileButton(fileDeltaNode: FileDeltaNode) = FileHeaderButton("Stage File") {
    GitDownState.git.value.stageFile(fileDeltaNode.fileDelta.getPath())
    GitDownState.selectedFiles.remove(fileDeltaNode.fileDelta)
}