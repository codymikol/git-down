package com.codymikol.components.commit.diff.file.header.action.buttons

import androidx.compose.runtime.Composable
import com.codymikol.data.file.FileDelta
import com.codymikol.extensions.stageFile
import com.codymikol.state.GitDownState

@Composable
fun StageFileButton(fileDelta: FileDelta) = FileHeaderButton("Stage File") {
    GitDownState.git.value.stageFile(fileDelta.getPath())
    GitDownState.selectedFiles.remove(fileDelta)
}