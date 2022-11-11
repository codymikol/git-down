package com.codymikol.components.commit.diff.file.header.action

import androidx.compose.runtime.Composable
import com.codymikol.components.commit.diff.file.header.action.buttons.StageFileButton
import com.codymikol.components.commit.diff.file.header.action.buttons.StageLinesButton
import com.codymikol.components.commit.diff.file.header.action.buttons.UnstageFileButton
import com.codymikol.components.commit.diff.file.header.action.buttons.UnstageLinesButton
import com.codymikol.data.file.FileDelta
import com.codymikol.data.file.Status

@Composable
fun FileHeaderActions(fileDelta: FileDelta) = when (isCurrentlySelectingLines()) {
    true -> LineActions(fileDelta)
    false -> FileActions(fileDelta)
}

private fun isCurrentlySelectingLines(): Boolean {
    // todo(mikol): Figure out how we are tracking selected lines :/
    return false
}


@Composable
private fun LineActions(fileDelta: FileDelta) = when (fileDelta.type) {
    Status.WORKING_DIRECTORY -> StageLinesButton()
    Status.INDEX -> UnstageLinesButton()
}

@Composable
private fun FileActions(fileDelta: FileDelta) = when (fileDelta.type) {
    Status.WORKING_DIRECTORY -> StageFileButton(fileDelta)
    Status.INDEX -> UnstageFileButton(fileDelta)
}