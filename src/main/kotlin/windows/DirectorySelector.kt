package windows

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Window
import state.GitDownState
import java.awt.FileDialog
import java.awt.Frame
import javax.swing.JFileChooser

val isAppleDialogOpen = mutableStateOf(false)

@Composable
private fun AppleFileWindow(
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
    isAppleDialogOpen.value = false
}

@Preview
@Composable
fun DirectorySelector(applicationScope: ApplicationScope) =
    Window(
        onCloseRequest = applicationScope::exitApplication,
        title = GitDownState.branchName.value,
        icon = painterResource(resourcePath = "icons/icon.png"),
    ) {
        if (isAppleDialogOpen.value) AppleFileWindow(parent = null, onCloseRequest = ::handleDirectorySelection)
        Button(onClick = {

            val OS = System.getProperty("os.name")

            if(OS == "Apple") {
                isAppleDialogOpen.value = true
            } else {
                val fileChooser = JFileChooser()
                fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                fileChooser.showOpenDialog(null)
                handleDirectorySelection(fileChooser.selectedFile.absolutePath + "/.git")
            }

        }) { Text("Open a Git Repository...") }
        Text("Welcome to GitDown")
    }
