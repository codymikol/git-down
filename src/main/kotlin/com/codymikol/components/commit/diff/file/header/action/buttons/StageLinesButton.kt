package com.codymikol.components.commit.diff.file.header.action.buttons

import androidx.compose.runtime.Composable
import com.codymikol.data.diff.FileDeltaNode
import com.codymikol.extensions.stageLinesForAddedFile
import com.codymikol.state.GitDownState

@Composable
fun StageLinesButton(fileDeltaNode: FileDeltaNode) = FileHeaderButton("Stage Lines") {
    GitDownState.git.value.stageLinesForAddedFile(fileDeltaNode.getSelectedLines())
}