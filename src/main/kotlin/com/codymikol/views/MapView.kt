package com.codymikol.views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.codymikol.data.map.MapConnector
import com.codymikol.services.MapConnectors
import com.codymikol.state.GitDownState
import com.codymikol.state.MapState
import java.util.Date

private val LaneWidth = 180.dp
private val NodeRadius = 5.dp
private val GutterX = 20.dp
private val RowHeight = 48.dp
private const val ConnectorStrokeWidth = 2f

// The dog-tag detail card shown when a node is clicked (#252): a rounded-rectangle
// body with a protruding round tab holding a punched hole, sitting over the node.
private val CardTabSize = 22.dp
private val CardHoleSize = 8.dp
private val CardCorner = 10.dp

// Branch-name pills shown on tip commits (#272), colored the same as the node they
// label so they read as an extension of it rather than an unrelated UI element.
private val PillCorner = 8.dp
private val PillFontSize = 10.sp
private val PillSpacing = 4.dp
private val PillEndPadding = 12.dp
private val PillMaxWidth = 80.dp

@Composable
@Preview
fun MapView() {

    LaunchedEffect(GitDownState.gitDirectory.value) {
        MapState.reset()
    }

    when (MapState.branches.value.isEmpty()) {
        true -> MapEmptyState()
        false -> Map()
    }
}

@Composable
private fun Map() {
    val commits = MapState.commits
    val lazyListState = rememberLazyListState()

    val lastVisibleIndex by remember {
        derivedStateOf { lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
    }

    // shouldLoadMore() is true before anything has loaded, so this effect also covers
    // the very first page - no separate initial-load effect needed. It's keyed on the
    // scroll-derived lastVisibleIndex, never on commits.size or hasMore, which
    // loadMore() mutates - keying an effect on a value it mutates is what caused the
    // unbounded reload loop that froze the UI before (see #260 / #256). The while loop
    // just lets one recomposition catch several pages up at once instead of one.
    LaunchedEffect(lastVisibleIndex) {
        while (MapState.shouldLoadMore(lastVisibleIndex)) {
            MapState.loadMore()
        }
    }

    // Recomputed only when commits/lanesBySha themselves change (i.e. on page load),
    // not on every scroll frame - the Canvas below reads lazyListState directly during
    // its own draw phase, so scrolling never triggers recomposition of this list.
    val connectors by remember { derivedStateOf { MapConnectors.compute(commits, MapState.lanesBySha) } }

    Box(modifier = Modifier.fillMaxSize().background(Colors.DarkGrayBackground)) {
        // A single overlay pass over the currently-visible rows (#271): an individual
        // commit row's own draw bounds are clipped to that row, so a diagonal line
        // reaching into a neighbouring row/lane can only be drawn here, not per-node.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawConnectors(connectors, lazyListState)
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(commits.size, key = { commits[it].sha }) { index ->
                val commit = commits[index]
                val lane = MapState.lanesBySha[commit.sha] ?: 0

                CommitNode(
                    commit = commit,
                    isSelected = MapState.selectedNodeSha.value == commit.sha,
                    onClick = { MapState.toggleSelectedNode(commit.sha) },
                    modifier = Modifier.offset(x = LaneWidth * lane).width(LaneWidth),
                )
            }
        }
    }
}

// Row height is fixed, so a row's on-screen y-offset is a straight formula from the
// first visible row - true for every loaded row, not just the ones currently in
// layoutInfo.visibleItemsInfo, which is what lets a connector reach into a
// neighbouring row without that row needing to be measured this frame.
private fun DrawScope.drawConnectors(connectors: List<MapConnector>, lazyListState: LazyListState) {
    val rowHeightPx = RowHeight.toPx()
    val gutterXPx = GutterX.toPx()
    val laneWidthPx = LaneWidth.toPx()
    val firstIndex = lazyListState.firstVisibleItemIndex
    val firstOffset = lazyListState.firstVisibleItemScrollOffset

    fun rowCenterY(index: Int) = (index - firstIndex) * rowHeightPx - firstOffset + rowHeightPx / 2f
    fun laneCenterX(lane: Int) = gutterXPx + lane * laneWidthPx

    val visibleTop = -rowHeightPx
    val visibleBottom = size.height + rowHeightPx

    connectors.forEach { connector ->
        val childY = rowCenterY(connector.childIndex)
        val parentY = rowCenterY(connector.parentIndex)
        if (childY < visibleTop && parentY < visibleTop) return@forEach
        if (childY > visibleBottom && parentY > visibleBottom) return@forEach

        // Cross-lane connectors (merge splits, reconvergences) get the same blue as a
        // merge node so the line reads as distinct from a same-lane continuation,
        // rather than relying on the diagonal angle alone.
        val sameLane = connector.childLane == connector.parentLane
        drawLine(
            color = if (sameLane) Colors.LightGrayText else Colors.FileModified,
            start = Offset(laneCenterX(connector.childLane), childY),
            end = Offset(laneCenterX(connector.parentLane), parentY),
            strokeWidth = ConnectorStrokeWidth,
        )
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

// The sha and commit message are hidden by default to preserve space (#252); the
// node itself is the whole clickable target and its detail card only appears while
// this node is selected.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommitNode(
    commit: CommitGraphNode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val tipBranches = MapState.branchTipsBySha.value[commit.sha] ?: emptyList()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight)
            // The selected node draws above its siblings so its card, which is taller
            // than one row, floats over the neighbouring nodes rather than under them.
            .zIndex(if (isSelected) 1f else 0f)
            .clickable { onClick() }
            // Right-clicking opens the context menu and always leaves this node
            // selected (see issue #253) - selectNode, not the click toggle.
            .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary)) {
                MapState.selectNode(commit.sha)
                menuExpanded = true
            }
            .drawBehind { drawCommitNode(commit) }
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

        // Hidden while the detail card is showing: the card sizes to its content
        // (leading) and a full row of pills (up to PillMaxWidth each, trailing) can
        // together exceed LaneWidth, so they'd otherwise risk overlapping.
        if (tipBranches.isNotEmpty() && !isSelected) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = PillEndPadding),
                horizontalArrangement = Arrangement.spacedBy(PillSpacing),
            ) {
                tipBranches.forEach { branchName -> BranchPill(branchName, nodeColor(commit)) }
            }
        }

        CommitContextMenu(
            commit = commit,
            expanded = menuExpanded,
            onDismiss = { menuExpanded = false },
        )
    }
}

// A small labeled pill naming a local branch whose tip is this commit (#272); shown
// instead of the old per-column branch header now that it labels a node directly.
@Composable
private fun BranchPill(branchName: String, color: Color) {
    Box(
        modifier = Modifier
            .widthIn(max = PillMaxWidth)
            .clip(RoundedCornerShape(PillCorner))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = branchName,
            color = Color.White,
            fontSize = PillFontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// The right-click context menu for a commit node (#253). Its actions are grouped
// by CommitContextMenu, with a divider drawn between each group. Quick View (#254)
// launches the quick view for this commit and Diff with HEAD (#280) diffs it
// against the current HEAD; the remaining behaviours are wired up in follow-up
// issues, so those items only dismiss for now.
@Composable
private fun CommitContextMenu(
    commit: CommitGraphNode,
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
                    onClick = when (action.label) {
                        "Quick View" -> {
                            { GitDownState.openQuickView(commit); onDismiss() }
                        }
                        "Diff with HEAD..." -> {
                            { GitDownState.openDiffWithHead(commit); onDismiss() }
                        }
                        else -> onDismiss
                    },
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
        modifier = modifier,
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

private fun DrawScope.drawCommitNode(commit: CommitGraphNode) {
    val x = GutterX.toPx()
    val centerY = size.height / 2f

    when (commit.isMergeCommit) {
        true -> drawDiamond(center = Offset(x, centerY), radius = NodeRadius.toPx(), color = nodeColor(commit))
        false -> drawCircle(color = nodeColor(commit), radius = NodeRadius.toPx(), center = Offset(x, centerY))
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
