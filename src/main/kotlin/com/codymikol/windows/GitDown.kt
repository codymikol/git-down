package com.codymikol.windows

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.codymikol.components.TabButtonLocation
import com.codymikol.components.tabButton
import com.codymikol.data.Colors
import com.codymikol.extensions.scanForChanges
import com.codymikol.state.GitDownState
import com.codymikol.state.Keys
import com.codymikol.tabs.Tab
import com.codymikol.views.CommitView
import com.codymikol.views.MapView
import com.codymikol.views.StashView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.awt.Dimension
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener

@Preview
@Composable
fun GitDown() {

    Window(
        onKeyEvent = {
            Keys.isShiftPressed.value = it.isShiftPressed
            Keys.isCtrlPressed.value = it.isCtrlPressed
            false
        },
        onCloseRequest = {
            GitDownState.gitDirectory.value = ""
        },
        title = GitDownState.projectName.value,
        icon = painterResource(resourcePath = "icons/icon.png"),
        state = rememberWindowState(
            width = windowWidth.dp,
            height = windowHeight.dp,
            placement = WindowPlacement.Floating,
            position = WindowPosition(alignment = Alignment.Center)
        )
    ) {

        DisposableEffect(Unit) {

            window.addWindowFocusListener(object : WindowFocusListener {

                override fun windowGainedFocus(p0: WindowEvent?) {
                    runBlocking {
                        withContext(Dispatchers.IO) {
                            // We do a short block because sometimes editors will save temporary files
                            // for a few ms after losing window focus and these can show up in git-down
                            delay(100L)
                            GitDownState.git.value.scanForChanges()
                        }
                    }
                }

                override fun windowLostFocus(p0: WindowEvent?) { /** noop **/ }

            })
            onDispose {}
        }

        this.window.focusListeners

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
            Column(modifier = Modifier.fillMaxSize().background(color = Colors.DarkGrayBackground)) {
                Row(
                    modifier = Modifier.requiredHeight(48.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WindowDraggableArea(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(modifier = Modifier.padding(10.dp)) {

                                tabButton(
                                    TabButtonLocation.Left,
                                    Tab.Map,
                                    "icons/map.png",
                                    "icons/map_white.png",
                                    "Shows a map of commit history across branches."
                                )

                                tabButton(
                                    TabButtonLocation.Middle,
                                    Tab.Commit,
                                    "icons/commit.png",
                                    "icons/commit_white.png",
                                    "Allows you to view and commit changes to the repository."
                                )

                                tabButton(
                                    TabButtonLocation.Right,
                                    Tab.Stash,
                                    "icons/stash.png",
                                    "icons/stash_white.png",
                                    "Allows you to manage stashes.",
                                )

                            }
                            Column() {
                                Text(
                                    "${GitDownState.projectName.value} — Commit",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${GitDownState.commitCount.value} commits",
                                    color = Colors.LightGrayText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(0.dp, 3.dp, 0.dp, 0.dp)
                                )
                            }
                        }
                    }
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
