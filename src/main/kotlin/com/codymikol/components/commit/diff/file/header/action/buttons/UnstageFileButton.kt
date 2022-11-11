package com.codymikol.components.commit.diff.file.header.action.buttons

import androidx.compose.runtime.Composable
import com.codymikol.data.file.FileDelta
import com.codymikol.extensions.unstageFile
import com.codymikol.state.GitDownState

@Composable
fun UnstageFileButton(fileDelta: FileDelta) = FileHeaderButton("Unstage File") {
    GitDownState.git.value.unstageFile(fileDelta.getPath())
    GitDownState.selectedFiles.remove(fileDelta)
}