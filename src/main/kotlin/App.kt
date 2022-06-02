package com.codymikol

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ApplicationScope
import state.GitDownState
import windows.DirectorySelector
import windows.GitDown
@Composable
@Preview
fun App(applicationScope: ApplicationScope) = when (GitDownState.isValidGitDirectory.value) {
    true -> GitDown()
    false -> DirectorySelector(applicationScope)
}
