package windows

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Window
import state.GitDownState
import java.awt.FileDialog
import java.awt.Frame

val isDialogOpen = mutableStateOf(false)

@Composable
private fun FileWindow(
    parent: Frame? = null,
    onCloseRequest: (result: String) -> Unit
) = AwtWindow(
    create = {
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        object : FileDialog(parent, "Choose a file", LOAD) {
            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    onCloseRequest(this.directory + this.file + "/.git")
                }
            }
        }
    },
    dispose = FileDialog::dispose
)

fun handleDirectorySelection(dir: String) {
    GitDownState.gitDirectory.value = dir
    isDialogOpen.value = false
}

@Preview
@Composable
fun DirectorySelector(applicationScope: ApplicationScope) =
    Window(onCloseRequest = applicationScope::exitApplication, title = GitDownState.branchName.value) {
        if (isDialogOpen.value) FileWindow(parent = null, onCloseRequest = ::handleDirectorySelection)
        Button(onClick = { isDialogOpen.value = true }) { Text("Open a Git Repository...") }
        Text("Welcome to GitDown")
    }
