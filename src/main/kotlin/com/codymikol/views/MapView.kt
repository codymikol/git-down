package com.codymikol.views

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.codymikol.components.menu.MenuColors
import com.codymikol.components.menu.ThemedDropdownMenu
import com.codymikol.components.menu.ThemedDropdownMenuItem
import com.codymikol.data.Colors
import com.codymikol.data.map.CommitCard
import com.codymikol.data.map.CommitContextMenu as CommitContextMenuModel
import com.codymikol.data.map.CommitGraphNode
import com.codymikol.state.GitDownState
import com.codymikol.state.MapScrollState
import com.codymikol.state.MapState
import org.eclipse.jgit.lib.Ref
import java.util.Date
import kotlin.math.roundToInt

// Narrower than a horizontal title would need, since titles are now rotated
// diagonally and take up far less width per lane.
private val LaneWidth = 180.dp
private val NodeRadius = 5.dp
private val GutterX = 20.dp
private val RowHeight = 48.dp

// The dog-tag detail card shown when a node is clicked (#252): a rounded-rectangle
// body with a protruding round tab holding a punched hole, sitting over the node.
private val CardTabSize = 22.dp
private val CardHoleSize = 8.dp
private val CardCorner = 10.dp
private val CardMaxWidth = 150.dp

// The node itself signals interaction (#263): it grows a little while hovered and
// shrinks below its resting size while pressed, instead of a background wash. Both
// ends are driven through animateFloatAsState so the change is animated.
private const val HoveredNodeScale = 1.4f
private const val PressedNodeScale = 0.7f

// Every lane's title sits in a fixed-height box so all lanes' graphs start at the
// same y-offset below the titles, regardless of how long each branch name is.
private val TitleHeight = 110.dp

// Positive (clockwise) so, pivoting on the text's own top-start corner below,
// the label sweeps down-and-right into the box rather than up and out of it.
private val TitleRotation = 45f
private val TitleFontSize = 12.sp
private val TitleTextStartPadding = 12.dp
private val TitleTextTopPadding = 4.dp
private val TitleTextLineHeight = 16.sp

// Text is capped to this width before rotating, so its rotated footprint is
// bounded: rotated 45 degrees around its own top-start corner, its deepest
// point is (TitleTextMaxWidth + ~TitleTextLineHeight) * sin(45deg) below the
// pivot, i.e. below the top of the box, which is ~89dp at default font scale
// for the values here - comfortably under TitleHeight (110dp) with margin
// for TitleTextTopPadding. clip(RectangleShape) below is still a hard
// backstop against any overflow (e.g. from a larger system font scale).
private val TitleTextMaxWidth = 110.dp

@Composable
@Preview
fun MapView() {

    LaunchedEffect(GitDownState.gitDirectory.value) {
        MapState.reset()
        MapScrollState.reset()
    }

    val branches = MapState.branches.value

    when (branches.isEmpty()) {
        true -> MapEmptyState()
        false -> Map(branches)
    }
}

@Composable
private fun Map(branches: List<Ref>) {

    val rowHeightPx = with(LocalDensity.current) { RowHeight.toPx() }
    val headerHeightPx = with(LocalDensity.current) { TitleHeight.toPx() }
    var viewportHeightPx by remember { mutableStateOf(0f) }

    // Every lane's own row viewport sits below its TitleHeight header, so windowing
    // and the scroll bound are both computed against that shrunk height, not the raw
    // container height - otherwise the last loaded row of the tallest lane clips.
    val laneContentHeightPx = (viewportHeightPx - headerHeightPx).coerceAtLeast(0f)

    val maxLoadedRows = branches.maxOfOrNull { MapState.commitsByBranch[it.name]?.size ?: 0 } ?: 0
    val maxOffsetPx = (maxLoadedRows * rowHeightPx - laneContentHeightPx).coerceAtLeast(0f)

    // Every lane windows against MapScrollState's one shared offset (see BranchLane)
    // instead of a LazyListState, so this is the single place that translates drag/wheel
    // input into that offset - no lane ever owns or drives scrolling on its own.
    val verticalScrollableState = rememberScrollableState { delta ->
        val before = MapScrollState.offsetPx
        MapScrollState.scrollBy(delta, maxOffsetPx)
        MapScrollState.offsetPx - before
    }

    Column(modifier = Modifier.fillMaxSize()) {

        val lazyHorizontalState = rememberLazyListState()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .onSizeChanged { viewportHeightPx = it.height.toFloat() }
                .scrollable(orientation = Orientation.Vertical, state = verticalScrollableState)
        ) {
            LazyRow(
                state = lazyHorizontalState,
                modifier = Modifier.fillMaxWidth().fillMaxHeight().background(Colors.DarkGrayBackground)
            ) {
                items(branches.size, key = { branches[it].name }) {
                    BranchLane(branch = branches[it], rowHeightPx = rowHeightPx, viewportHeightPx = laneContentHeightPx)
                }
            }
        }
    }

}

@Composable
private fun MapEmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight().background(Colors.DarkGrayBackground),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("No branches found", color = Color.Gray)
        }
    }
}

@Composable
private fun BranchLane(branch: Ref, rowHeightPx: Float, viewportHeightPx: Float) {
    val branchName = branch.name.removePrefix("refs/heads/")
    val commits = MapState.commitsByBranch[branch.name] ?: emptyList()

    val firstVisibleIndex = MapScrollState.firstVisibleIndex(rowHeightPx)
    val visibleRowCount = MapScrollState.visibleRowCount(viewportHeightPx, rowHeightPx)
    val lastVisibleIndex = firstVisibleIndex + visibleRowCount - 1

    // shouldLoadMore() is true before a branch has loaded anything, so this effect also
    // covers the lane's first page - no separate initial-load effect needed. It's keyed
    // on the scroll-derived lastVisibleIndex, never on commits.size or hasMore, which
    // loadMore() mutates - keying an effect on a value it mutates is what caused the
    // unbounded reload loop that froze the UI before (see #260 / #256). The while loop
    // just lets one recomposition catch a lane up several pages instead of one.
    LaunchedEffect(branch.name, lastVisibleIndex) {
        while (MapState.shouldLoadMore(branch.name, lastVisibleIndex)) {
            MapState.loadMore(branch)
        }
    }

    Column(
        modifier = Modifier
            .width(LaneWidth)
            .fillMaxHeight()
    ) {
        MapLaneTitle(branchName)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clipToBounds()
        ) {
            val visibleEnd = (firstVisibleIndex + visibleRowCount).coerceIn(0, commits.size)
            val visibleStart = firstVisibleIndex.coerceIn(0, visibleEnd)

            for (index in visibleStart until visibleEnd) {
                val commit = commits[index]
                val yOffsetPx = (index * rowHeightPx - MapScrollState.offsetPx).roundToInt()

                key(commit.sha) {
                    CommitNode(
                        commit = commit,
                        branchName = branch.name,
                        isSelected = MapState.isNodeSelected(branch.name, commit.sha),
                        onClick = { MapState.toggleSelectedNode(branch.name, commit.sha) },
                        showLeadingGuideline = false,
                        showTrailingGuideline = false,
                        modifier = Modifier.offset { IntOffset(0, yOffsetPx) },
                    )
                }
            }
        }

    }
}


@Composable
private fun MapLaneTitle(branchName: String) {
    // The map's own background already shows through here (BranchLane's Column
    // paints no background of its own), so the title needs no fill of its own
    // to satisfy "same background color as the map".
    //
    // The text's own top-start corner is pinned as the rotation pivot (rather
    // than the default center), so it stays fixed at the top of the box and
    // the rest of the label sweeps down/across into the title box's own
    // reserved height rather than toward the graph below; clipping to the box
    // is a hard backstop against any residual overflow so a rotated title can
    // never bleed into the graph.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(TitleHeight)
            .clip(RectangleShape),
        contentAlignment = Alignment.TopStart
    ) {
        Text(
            text = branchName,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = TitleFontSize,
                lineHeight = TitleTextLineHeight
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = TitleTextStartPadding, top = TitleTextTopPadding)
                .width(TitleTextMaxWidth)
                .graphicsLayer(rotationZ = TitleRotation, transformOrigin = TransformOrigin(0f, 0f))
        )
    }
}

// The sha and commit message are hidden by default to preserve space (#252); the
// node itself is the whole clickable target and its detail card only appears while
// this node is selected.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommitNode(
    commit: CommitGraphNode,
    branchName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    showLeadingGuideline: Boolean,
    showTrailingGuideline: Boolean,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val nodeScale by animateFloatAsState(
        targetValue = when {
            isPressed -> PressedNodeScale
            isHovered -> HoveredNodeScale
            else -> 1f
        },
        label = "commitNodeScale",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight)
            // The selected node draws above its siblings so its card, which is taller
            // than one row, floats over the neighbouring nodes rather than under them.
            .zIndex(if (isSelected) 1f else 0f)
            .hoverable(interactionSource)
            // indication = null drops the default ripple/hover background wash so only
            // the node graphic reacts to hover and press (see issue #263); the scale is
            // applied when the node is drawn, below.
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            // Right-clicking opens the context menu and always leaves this node
            // selected (see issue #253) - selectNode, not the click toggle.
            .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary)) {
                MapState.selectNode(branchName, commit.sha)
                menuExpanded = true
            }
            .drawBehind { drawCommitNode(commit, nodeScale, showLeadingGuideline, showTrailingGuideline) }
    ) {
        if (isSelected) {
            CommitDetailCard(
                commit = commit,
                color = nodeColor(commit),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = GutterX - CardTabSize / 2)
            )
        }

        CommitContextMenu(
            expanded = menuExpanded,
            onDismiss = { menuExpanded = false },
        )
    }
}

// The right-click context menu for a commit node (#253). Its actions are grouped
// by CommitContextMenu, with a divider drawn between each group; concrete
// behaviours are wired up in follow-up issues, so items only dismiss for now.
@Composable
private fun CommitContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
) {
    ThemedDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        CommitContextMenuModel.groups.forEachIndexed { index, group ->
            if (index > 0) Divider(color = MenuColors.Divider)
            group.forEach { action ->
                ThemedDropdownMenuItem(
                    label = action.label,
                    shortcut = action.shortcut,
                    onClick = onDismiss,
                )
            }
        }
    }
}

// The floating dog-tag card: a round tab holding a punched hole sits over the node
// and sticks out a little farther than the rounded-rectangle body, which extends to
// the right with the commit's details. Coloured the same as the node it describes.
@Composable
private fun CommitDetailCard(
    commit: CommitGraphNode,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val now = remember(commit.sha) { Date() }

    Row(
        modifier = modifier.widthIn(max = CardMaxWidth),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(CardTabSize)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(CardHoleSize)
                    .clip(CircleShape)
                    .background(Colors.DarkGrayBackground)
            )
        }

        // Tucked left under the tab so the tab's hole end protrudes past the body.
        Column(
            modifier = Modifier
                .offset(x = -CardCorner)
                .clip(RoundedCornerShape(CardCorner))
                .background(color)
                .padding(start = CardCorner + 6.dp, top = 6.dp, end = 10.dp, bottom = 6.dp),
        ) {
            CardLine(CommitCard.title(commit), FontWeight.Bold)
            CardLine(CommitCard.author(commit), FontWeight.Normal)
            CardLine(CommitCard.date(commit.date, now), FontWeight.Normal)
        }
    }
}

@Composable
private fun CardLine(text: String, fontWeight: FontWeight) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = fontWeight,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun nodeColor(commit: CommitGraphNode): Color =
    if (commit.isMergeCommit) Colors.FileModified else Colors.FileAdded

private fun DrawScope.drawCommitNode(
    commit: CommitGraphNode,
    scale: Float,
    showLeadingGuideline: Boolean,
    showTrailingGuideline: Boolean,
) {
    val x = GutterX.toPx()
    val centerY = size.height / 2f

    if (showLeadingGuideline) {
        drawLine(color = Colors.LightGrayText, start = Offset(x, 0f), end = Offset(x, centerY), strokeWidth = 2f)
    }

    if (showTrailingGuideline) {
        drawLine(
            color = Colors.LightGrayText,
            start = Offset(x, centerY),
            end = Offset(x, size.height),
            strokeWidth = 2f
        )
    }

    // Only the node graphic scales with hover/press, so the row's click target and
    // any detail card stay put while the node itself grows or shrinks (see issue #263).
    val radius = NodeRadius.toPx() * scale

    when (commit.isMergeCommit) {
        true -> drawDiamond(center = Offset(x, centerY), radius = radius, color = nodeColor(commit))
        false -> drawCircle(color = nodeColor(commit), radius = radius, center = Offset(x, centerY))
    }
}

private fun DrawScope.drawDiamond(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius, center.y)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius, center.y)
        close()
    }
    drawPath(path, color = color)
}
