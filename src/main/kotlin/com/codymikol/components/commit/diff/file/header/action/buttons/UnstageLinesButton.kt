package com.codymikol.components.commit.diff.file.header.action.buttons

import androidx.compose.runtime.Composable
import com.codymikol.data.diff.FileDeltaNode
import com.codymikol.extensions.unstageLines
import com.codymikol.state.GitDownState

@Composable
fun UnstageLinesButton(fileDeltaNode: FileDeltaNode) = FileHeaderButton("Unstage Lines") {
    GitDownState.git.value.unstageLines(fileDeltaNode.getSelectedLines())
}