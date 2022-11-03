package com.codymikol

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ApplicationScope
import com.codymikol.state.GitDownState
import com.codymikol.windows.DirectorySelector
import com.codymikol.windows.GitDown
@Composable
@Preview
fun App(applicationScope: ApplicationScope) = when (GitDownState.isValidGitDirectory.value) {
    true -> GitDown()
    false -> DirectorySelector(applicationScope)
}
