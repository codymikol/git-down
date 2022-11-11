package com.codymikol.components.commit.diff.file.header.action

import androidx.compose.runtime.Composable
import com.codymikol.components.commit.diff.file.header.action.buttons.StageFileButton
import com.codymikol.components.commit.diff.file.header.action.buttons.StageLinesButton
import com.codymikol.components.commit.diff.file.header.action.buttons.UnstageFileButton
import com.codymikol.components.commit.diff.file.header.action.buttons.UnstageLinesButton
import com.codymikol.data.diff.FileDeltaNode
import com.codymikol.data.file.Status

@Composable
fun FileHeaderActions(fileDeltaNode: FileDeltaNode) = when (fileDeltaNode.isSelectingLines()) {
    true -> LineActions(fileDeltaNode)
    false -> FileActions(fileDeltaNode)
}

@Composable
private fun LineActions(fileDeltaNode: FileDeltaNode) = when (fileDeltaNode.fileDelta.type) {
    Status.WORKING_DIRECTORY -> StageLinesButton(fileDeltaNode)
    Status.INDEX -> UnstageLinesButton(fileDeltaNode)
}

@Composable
private fun FileActions(fileDeltaNode: FileDeltaNode) = when (fileDeltaNode.fileDelta.type) {
    Status.WORKING_DIRECTORY -> StageFileButton(fileDeltaNode)
    Status.INDEX -> UnstageFileButton(fileDeltaNode)
}