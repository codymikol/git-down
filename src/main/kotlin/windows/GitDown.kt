package windows

import androidx.compose.desktop.DesktopTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import components.tabButton
import data.Colors
import kotlinx.coroutines.*
import org.eclipse.jgit.api.Git
import state.GitDownState
import state.GitDownState.test
import tabs.Tab
import views.CommitView
import views.MapView
import views.StashView
import java.awt.Dimension

@DelicateCoroutinesApi
@Preview
@Composable
fun GitDown() {

    org.eclipse.jgit.lib.Repository.getGlobalListenerList().addIndexChangedListener {
        GitDownState.test.value += 1
        println("Changed @ addIndexChangedListener")
    }
    org.eclipse.jgit.lib.Repository.getGlobalListenerList().addConfigChangedListener {
        GitDownState.test.value += 1
        println("Changed @ addConfigChangedListener")
    }
    org.eclipse.jgit.lib.Repository.getGlobalListenerList().addRefsChangedListener {
        GitDownState.test.value += 1
        println("Changed @ addRefsChangedListener")
    }

    GitDownState.config.value.addChangeListener {
        GitDownState.test.value += 1
        println("Changed @ addRefsChangedListener")
    }

    Window(
        onCloseRequest = { GitDownState.gitDirectory.value = "" },
        title = GitDownState.projectName.value,
        icon = painterResource(resourcePath = "icons/icon.png"),
    ) {

    this.window.minimumSize = Dimension(800, 500)

        CompositionLocalProvider(
            LocalScrollbarStyle provides ScrollbarStyle(
                minimalHeight = 16.dp,
                thickness = 8.dp,
                shape = MaterialTheme.shapes.small,
                hoverDurationMillis = 300,
                unhoverColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                hoverColor = MaterialTheme.colors.onSurface.copy(alpha = 0.50f)
            )
        ) {
            Column(modifier = Modifier.fillMaxSize().background(color = data.Colors.DarkGrayBackground)) {
                Row(
                    modifier = Modifier.requiredHeight(48.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(modifier = Modifier.padding(10.dp)) {
                            tabButton(
                                currentTab = GitDownState.currentTab,
                                thisTab = Tab.Map,
                                resourceLocation = "icons/map.png",
                                description = "Shows a map of commit history across branches."
                            )
                            tabButton(
                                currentTab = GitDownState.currentTab,
                                thisTab = Tab.Commit,
                                resourceLocation = "icons/commit.png",
                                description = "Allows you to view and commit changes to the repository."
                            )
                            tabButton(
                                currentTab = GitDownState.currentTab,
                                thisTab = Tab.Stash,
                                resourceLocation = "icons/stash.png",
                                description = "Allows you to manage stashes."
                            )
                        }
                        Column() {
                            Text(
                                "${GitDownState.projectName.value} — Commit " + GitDownState.test.value,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${GitDownState.commitCount.value} commits",
                                color = Colors.LightGrayText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Light
                            )
                        }
                    }
                    Row {
//                    Button(onClick = {}) {
//                        Image(
//                            painter = painterResource(resourcePath = "icons/stash.png"),
//                            contentDescription = "Update the local stuff"
//                        )
//                    }
                    }
//                TextField(value = "", onValueChange = {}, placeholder = { "Search Repository..." })
                }
                Column {
                    when (GitDownState.currentTab.value) {
                        Tab.Commit -> CommitView()
                        Tab.Map -> MapView()
                        Tab.Stash -> StashView()
                    }
                }
            }
        }
    }
}
