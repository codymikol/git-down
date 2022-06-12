package windows

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import data.Colors
import data.recent.RecentProject
import data.recent.RecentProjects
import repositories.RecentProjectRepository
import state.GitDownState
import kotlinx.coroutines.DelicateCoroutinesApi
import org.koin.java.KoinJavaComponent.inject
import java.awt.*
import java.io.File
import java.net.URI
import javax.swing.JFileChooser
import javax.swing.JFrame


val isAppleDialogOpen = mutableStateOf(false)

private val BackgroundColor = Color(44, 44, 44)
private val ExitButtonColor = Color(157, 157, 157)
private val ButtonBackgroundColor = Color(53, 53, 53)
private val ButtonBorderColor = Color(60, 60, 60)

private val recentProjectsRepository: RecentProjectRepository by inject(RecentProjectRepository::class.java)

@Composable
fun RecentProjectSelector(x: Int, y: Int, closeHandler: () -> Unit, recent: RecentProjects) {
    AwtWindow(create = {
        ComposeWindow().apply {
            //todo(mikol): really big hack here for this menu, might just redo into something more manageable...
            setBounds(x + 5, y + 330, 290, 48 * recent.projects.size.coerceAtMost(5) + 16)
            focusableWindowState = false
            defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            type = java.awt.Window.Type.POPUP
            isAlwaysOnTop = true
            isUndecorated = true
            background = java.awt.Color(0, 0,0,0)
            isResizable = false
            isVisible = true
            setContent {
                DropdownMenu(
                    modifier = Modifier.background(Color.Transparent).fillMaxWidth(),
                    expanded = true,
                    onDismissRequest = {}) {
                    recent.projects.forEach {
                        DropdownMenuItem(modifier = Modifier.fillMaxWidth(), onClick = {
                            handleDirectorySelection(it.location)
                            closeHandler()
                        }) {
                            Text(it.name)
                        }
                    }
                }
            }
        }
    },
        dispose = {})

}
@Composable
fun DirectorySelector(applicationScope: ApplicationScope) =
    Window(
        onCloseRequest = applicationScope::exitApplication,
        title = GitDownState.branchName.value,
        icon = painterResource(resourcePath = "icons/icon.png"),
        undecorated = true,
        transparent = true,
        state = rememberWindowState(
            width = windowWidth.dp,
            height = windowHeight.dp,
            placement = WindowPlacement.Floating,
            position = WindowPosition(alignment = Alignment.Center)
        )
    ) {
        this.window.isResizable = false
        this.window.minimumSize = Dimension(windowWidth, windowHeight)
        this.window.size = Dimension(windowWidth, windowHeight)

        if (isAppleDialogOpen.value) AppleFileWindow(
            parent = null,
            onCloseRequest = ::handleDirectorySelection
        )

        val borderSize = 12.dp

        val showRecentProjectDropdown = remember { mutableStateOf(false) }
        val recentProjects = remember { recentProjectsRepository.getRecentProjects() }

        if (showRecentProjectDropdown.value) {
            RecentProjectSelector(window.x, window.y, { showRecentProjectDropdown.value = false }, recentProjects)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(width = 1.dp, color = Color.Black, shape = RoundedCornerShape(borderSize))
                .clip(RoundedCornerShape(borderSize)),
        ) {

            Column(
                modifier = Modifier.fillMaxSize()
                    .background(BackgroundColor),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Toolbar(applicationScope)
                Spacer(modifier = Modifier.height(10.dp))
                GitDownImage()
                Spacer(modifier = Modifier.height(10.dp))
                Text("Welcome to GitDown", color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                InspirationText()
                Spacer(modifier = Modifier.height(14.dp))
                SelectRepositoryButton()
                RecentlyOpenedButton {
                    showRecentProjectDropdown.value = !showRecentProjectDropdown.value
                }
                SocialFooter()
            }

        }
    }

@Composable
private fun SocialFooter() {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(200.dp)
                .fillMaxHeight()
        ) {
            DiscordButton()
            EmailButton()
        }
    }
}

@Composable
private fun SocialButton(
    iconSrc: String,
    text: String,
    onClick: () -> Any
) = Box(
    modifier = Modifier
        .wrapContentWidth()
        .height(24.dp)
        .clickable { onClick() }, contentAlignment = Alignment.Center
) {
    Row {
        Image(
            painter = painterResource(resourcePath = iconSrc),
            contentDescription = "",
            modifier = Modifier.height(12.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun DiscordButton() =
    SocialButton("icons/discord.svg", "Discord") { openLink("https://discord.gg/aPqQzDFn7N") }

@Composable
private fun EmailButton() =
    SocialButton("icons/email.svg", "Email") { openLink("mailto:hi@codymikol.com") }

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
    val dirFile = File(dir)
    recentProjectsRepository.addRecentProject(
        RecentProject(name = dirFile.parentFile.name, location = dir)
    )
    GitDownState.gitDirectory.value = dir
    isAppleDialogOpen.value = false
}

@Composable
fun ExitButton(applicationScope: ApplicationScope) {

    val exitButtonSize = 64.dp

    Box(
        modifier = Modifier
            .size(32.dp)
            .padding(8.dp)
            .border(
                width = 1.dp,
                color = Colors.DarkGrayBackground,
                shape = RoundedCornerShape(exitButtonSize)
            )
            .clip(RoundedCornerShape(exitButtonSize))
    ) {
        Box(modifier = Modifier.fillMaxSize()
            .clickable {
                applicationScope.exitApplication()
            }
            .align(Alignment.Center)
            .background(ExitButtonColor)
        ) {
            Box(modifier = Modifier.wrapContentSize().align(Alignment.Center)) {
                Text(
                    "⨯",
                    modifier = Modifier.size(14.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp
                )
            }
        }
    }
}

const val windowHeight = 385

const val windowWidth = 310

@Composable
private fun InspirationText() {
    Row {
        Text("Inspired by ", color = Color.White, fontSize = 10.sp)
        Text(
            "Gitup",
            modifier = Modifier
                .clickable { openLink("https://gitup.co/") },
            color = Color.White,
            textDecoration = TextDecoration.Underline,
            fontSize = 10.sp
        )
        Text(" ❤️", color = Color.White, fontSize = 10.sp)
    }
}

fun openLink(url: String) {
    val desktop = Desktop.getDesktop()
    desktop.browse(URI.create(url))
}

@Composable
private fun FrameWindowScope.Toolbar(applicationScope: ApplicationScope) {
    WindowDraggableArea {
        Column(
            modifier = Modifier.fillMaxWidth().height(36.dp),
            horizontalAlignment = Alignment.Start
        ) {
            ExitButton(applicationScope)
        }
    }
}

fun launchSelectRepositoryDialog() {

    val OS = System.getProperty("os.name")

    if (OS == "Apple") {
        isAppleDialogOpen.value = true
    } else {
        val fileChooser = JFileChooser()
        fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        fileChooser.showOpenDialog(null)
        val selectedDirectory = fileChooser.selectedFile?.absolutePath?.plus("/.git")
        if (null != selectedDirectory) {
            handleDirectorySelection(selectedDirectory)
        }
    }

}

@Composable
fun LaunchScreenButton(text: String, onClick: () -> Any) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(ButtonBackgroundColor)
            .border(1.dp, ButtonBorderColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White)
    }
}

@Composable
private fun SelectRepositoryButton() =
    LaunchScreenButton("Open a Git Repository...") { launchSelectRepositoryDialog() }

@Composable
private fun RecentlyOpenedButton(onClick: () -> Unit = {}) =
    LaunchScreenButton("Recently Opened...  ↴") { onClick() }

@Composable
private fun GitDownImage() {
    Image(
        modifier = Modifier.size(148.dp),
        painter = painterResource(resourcePath = "icons/icon_256x256.png"),
        contentDescription = "Git down icon",
    )
}
