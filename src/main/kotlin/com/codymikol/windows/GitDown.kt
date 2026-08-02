package com.codymikol.windows

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.codymikol.components.TabButtonLocation
import com.codymikol.components.tabButton
import com.codymikol.data.Colors
import com.codymikol.extensions.onFocusGained
import com.codymikol.extensions.scanForChanges
import com.codymikol.gitdown.generated.resources.Res
import com.codymikol.gitdown.generated.resources.commit
import com.codymikol.gitdown.generated.resources.commit_white
import com.codymikol.gitdown.generated.resources.icon
import com.codymikol.gitdown.generated.resources.map
import com.codymikol.gitdown.generated.resources.map_white
import com.codymikol.gitdown.generated.resources.stash
import com.codymikol.gitdown.generated.resources.stash_white
import com.codymikol.services.StashService
import com.codymikol.services.WindowSizeService
import com.codymikol.state.GitDownState
import com.codymikol.state.Keys
import com.codymikol.state.MapDebugState
import com.codymikol.state.MapState
import com.codymikol.tabs.Tab
import com.codymikol.views.CommitView
import com.codymikol.views.isCommitMessageFocused
import com.codymikol.views.MapView
import com.codymikol.views.QuickView
import com.codymikol.views.StashView
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.java.KoinJavaComponent.inject
import java.awt.Dimension

private val windowSizeService: WindowSizeService by inject(WindowSizeService::class.java)
private val stashService: StashService by inject(StashService::class.java)

@Preview
@Composable
fun GitDown(applicationScope: ApplicationScope) {

    val defaultWindowSize = windowSizeService.getDefaultWindowSize()

    val scope = rememberCoroutineScope()

    Window(
        // Tab / Shift+Tab cycles focus through the commit view's three areas
        // (#298). Handled in the preview phase so it fires even while the commit
        // message text field holds focus, where it would otherwise be consumed
        // for default focus traversal.
        onPreviewKeyEvent = {
            val isCommitTabCycle = it.type == KeyEventType.KeyDown &&
                it.key == Key.Tab &&
                !it.isCtrlPressed && !it.isAltPressed &&
                GitDownState.currentTab.value == Tab.Commit

            if (isCommitTabCycle) {
                GitDownState.cycleCommitFocus(forward = !it.isShiftPressed)
                true
            } else {
                false
            }
        },
        onKeyEvent = {
            Keys.isShiftPressed.value = it.isShiftPressed
            Keys.isCtrlPressed.value = it.isCtrlPressed

            val isDown = it.type == KeyEventType.KeyDown
            val tab = GitDownState.currentTab.value

            val isFileSelectionArrow = isDown &&
                (it.key == Key.DirectionUp || it.key == Key.DirectionDown) &&
                tab == Tab.Commit &&
                !isCommitMessageFocused.value &&
                GitDownState.selectedFiles.size == 1

            // Space on the map with a node selected launches the quick view (#254)
            // against that node.
            val quickViewTarget = if (isDown && it.key == Key.Spacebar && tab == Tab.Map)
                MapState.selectedNode() else null

            // "I" on the map with a node selected diffs that node against HEAD (#280).
            val diffWithHeadTarget = if (isDown && it.key == Key.I && tab == Tab.Map)
                MapState.selectedNode() else null

            // Enter on the map with a node selected checks that node out as a
            // detached HEAD (#279).
            val checkoutDetachedTarget = if (isDown && it.key == Key.Enter && tab == Tab.Map)
                MapState.selectedNode() else null

            // Ctrl+Shift+D on the map toggles the debug menu that tunes the map's
            // "magic number" dimensions live (#291).
            val isMapDebugToggle = isDown && tab == Tab.Map &&
                it.isCtrlPressed && it.isShiftPressed && it.key == Key.D

            // Space or Escape closes the quick view while it is open.
            val shouldCloseQuickView = isDown && tab == Tab.QuickView &&
                (it.key == Key.Spacebar || it.key == Key.Escape)

            // Up/Down selects the previous/next file in the quick view (#278).
            val isQuickViewFileArrow = isDown && tab == Tab.QuickView &&
                (it.key == Key.DirectionUp || it.key == Key.DirectionDown)

            // Arrow keys walk the map grid (#295): up/down step through the commit rows,
            // left/right step to the adjacent lane.
            val isMapNavArrow = isDown && tab == Tab.Map &&
                (it.key == Key.DirectionUp || it.key == Key.DirectionDown ||
                    it.key == Key.DirectionLeft || it.key == Key.DirectionRight)

            // Stash shortcuts (#296) stand down while any stash dialog is open, so a
            // keystroke can't mutate the selection or silently drop a stash from under
            // an open save/apply/drop dialog.
            val isStashTab = tab == Tab.Stash && !GitDownState.isStashDialogOpen.value

            // Up/Down selects the previous/next stash on the stash view (#296).
            val isStashArrow = isDown && isStashTab &&
                (it.key == Key.DirectionUp || it.key == Key.DirectionDown)

            // Delete drops the selected stash on the stash view (#296).
            val stashDeleteTarget = if (isDown && it.key == Key.Delete && isStashTab)
                GitDownState.selectedStash.value else null

            // Enter prompts to confirm applying the selected stash (#296).
            val shouldPromptApplyStash = isDown && isStashTab &&
                it.key == Key.Enter && GitDownState.selectedStash.value != null

            when {
                isMapDebugToggle -> {
                    MapDebugState.toggle()
                    true
                }
                isFileSelectionArrow -> {
                    GitDownState.selectAdjacentFile(if (it.key == Key.DirectionUp) -1 else 1)
                    true
                }
                isQuickViewFileArrow -> {
                    GitDownState.selectAdjacentQuickViewFile(if (it.key == Key.DirectionUp) -1 else 1)
                    true
                }
                isMapNavArrow -> {
                    when (it.key) {
                        Key.DirectionUp -> MapState.selectAdjacentNode(-1, 0)
                        Key.DirectionDown -> MapState.selectAdjacentNode(1, 0)
                        Key.DirectionLeft -> MapState.selectAdjacentNode(0, -1)
                        Key.DirectionRight -> MapState.selectAdjacentNode(0, 1)
                    }
                    true
                }
                isStashArrow -> {
                    GitDownState.selectAdjacentStash(if (it.key == Key.DirectionUp) -1 else 1)
                    true
                }
                stashDeleteTarget != null -> {
                    scope.launch { stashService.dropStash(stashDeleteTarget) }
                    true
                }
                shouldPromptApplyStash -> {
                    GitDownState.promptApplyStash()
                    true
                }
                shouldCloseQuickView -> {
                    GitDownState.closeQuickView()
                    true
                }
                quickViewTarget != null -> {
                    GitDownState.openQuickView(quickViewTarget)
                    true
                }
                diffWithHeadTarget != null -> {
                    GitDownState.openDiffWithHead(diffWithHeadTarget)
                    true
                }
                checkoutDetachedTarget != null -> {
                    scope.launch { GitDownState.checkoutDetachedHead(checkoutDetachedTarget) }
                    true
                }
                else -> false
            }
        },
        onCloseRequest = {
            GitDownState.returnToProjectSelection()
        },
        title = GitDownState.projectName.value,
        icon = painterResource(Res.drawable.icon),
        undecorated = true,
        state = rememberWindowState(
            width = defaultWindowSize.width.dp,
            height = defaultWindowSize.height.dp,
            placement = WindowPlacement.Floating,
            position = WindowPosition(alignment = Alignment.Center)
        )
    ) {

        window.onFocusGained {
            GitDownState.lastRequestedUpdateTimestamp.value = System.currentTimeMillis()
            GitDownState.git.value.scanForChanges()
        }

        this.window.minimumSize = Dimension(WindowSizeService.MIN_WIDTH, WindowSizeService.MIN_HEIGHT)

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
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(10.dp)) {

                                tabButton(
                                    TabButtonLocation.Left,
                                    Tab.Map,
                                    Res.drawable.map,
                                    Res.drawable.map_white,
                                    "Shows a map of commit history across branches."
                                )

                                tabButton(
                                    TabButtonLocation.Middle,
                                    Tab.Commit,
                                    Res.drawable.commit,
                                    Res.drawable.commit_white,
                                    "Allows you to view and commit changes to the repository."
                                )

                                tabButton(
                                    TabButtonLocation.Right,
                                    Tab.Stash,
                                    Res.drawable.stash,
                                    Res.drawable.stash_white,
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
                            Spacer(modifier = Modifier.weight(1f))
                            // On the commit / map / stash screens exit returns to the
                            // splash / project selection screen rather than quitting the
                            // whole app (see issue #265).
                            ExitButton(applicationScope) { GitDownState.returnToProjectSelection() }
                        }
                    }
                }
                Column {
                    when (GitDownState.currentTab.value) {
                        Tab.Commit -> CommitView()
                        Tab.Map -> MapView()
                        Tab.Stash -> StashView()
                        Tab.QuickView -> QuickView()
                    }
                }
            }
        }
    }
}
