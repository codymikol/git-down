package com.codymikol.components.commit.diff.file.header.action

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codymikol.components.commit.diff.file.header.action.buttons.FileHeaderCogButton
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
private fun FileActions(fileDeltaNode: FileDeltaNode) = Row(verticalAlignment = Alignment.CenterVertically) {
    when (fileDeltaNode.fileDelta.type) {
        Status.WORKING_DIRECTORY -> StageFileButton(fileDeltaNode)
        Status.INDEX -> UnstageFileButton(fileDeltaNode)
    }
    Spacer(modifier = Modifier.width(6.dp))
    FileHeaderCogButton(fileDeltaNode)
}
