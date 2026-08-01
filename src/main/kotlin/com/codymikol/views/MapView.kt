package com.codymikol.views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.codymikol.services.OrderedBranchTip
import com.codymikol.state.GitDownState
import com.codymikol.state.MapState
import kotlinx.coroutines.launch
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
private val CardMaxWidth = 150.dp

// Branch-name pills for each branch tip (#272), pinned to a row at the top of the
// map (#277) and colored by the same rule as the node they label.
private val PillCorner = 8.dp
private val PillFontSize = 10.sp
private val PillSpacing = 4.dp
private val PillMaxWidth = 80.dp

@Composable
@Preview
fun MapView() {

    // Reset only on a genuine project switch, not on every re-entry into the Map tab:
    // returning from the quick view re-runs this effect with the same directory and must
    // preserve the loaded commits and the selected node (see issue #290).
    LaunchedEffect(GitDownState.gitDirectory.value) {
        MapState.resetForDirectory(GitDownState.gitDirectory.value)
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

    Column(modifier = Modifier.fillMaxSize().background(Colors.DarkGrayBackground)) {
        // Branch tips are pinned here at the top of the grid (#277), ordered
        // left-to-right so the mainline everything merges into leads and the side
        // branches follow by how soon they rejoin it - rather than scattered down
        // the history at each tip commit's chronological row.
        BranchPillRow(MapState.orderedBranchTips.value)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
}

// The row of branch-name pills pinned to the top of the map (#277), one pill per
// branch name in merge-proximity order. A tip shared by several branches lays its
// names out together, and a merge-commit tip is blued to match its node.
@Composable
private fun BranchPillRow(tips: List<OrderedBranchTip>) {
    if (tips.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Scroll rather than wrap or clip: the tips stay on one top row even when
            // there are more branches than fit across the map.
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = GutterX, vertical = PillSpacing * 2),
        horizontalArrangement = Arrangement.spacedBy(PillSpacing),
    ) {
        tips.forEach { tip ->
            tip.branchNames.forEach { branchName -> BranchPill(branchName, tipColor(tip)) }
        }
    }
}

// A tip pill takes the same colour rule as its node (nodeColor). The tip may not be
// among the loaded commits yet, in which case it defaults to the non-merge colour.
private fun tipColor(tip: OrderedBranchTip): Color {
    val commit = MapState.commits.firstOrNull { it.sha == tip.sha } ?: return Colors.FileAdded
    return nodeColor(commit)
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight)
            // The selected node draws above its siblings so its card, which is taller
            // than one row, floats over the neighbouring nodes rather than under them.
            .zIndex(if (isSelected) 1f else 0f)
            // A mouse-only primary click (not .clickable) so the node never takes
            // keyboard focus: a focused .clickable swallows the Space key and toggles
            // itself instead of letting the window-level shortcut open the quick view
            // for the selected node (see issue #290 / #254).
            .onClick(matcher = PointerMatcher.mouse(PointerButton.Primary)) { onClick() }
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
        } else if (commit.sha == MapState.headSha.value) {
            // The current HEAD is labelled on its node (#282) so the user can see
            // where it sits. Hidden while this node is selected, when its detail card
            // takes the same spot over the node instead.
            HeadLabel(
                color = nodeColor(commit),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = GutterX + NodeRadius + PillSpacing)
            )
        }

        CommitContextMenu(
            commit = commit,
            expanded = menuExpanded,
            onDismiss = { menuExpanded = false },
        )
    }
}

// A small labeled pill naming a local branch, rendered in the top pill row (#277).
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

// The "HEAD" label pinned to the current HEAD node (#282), sitting just right of
// the node dot. Mirrors the branch pill: a rounded box in the node's own colour
// with bold white text, so HEAD reads consistently with the branch tips it labels.
@Composable
private fun HeadLabel(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PillCorner))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = "HEAD",
            color = Color.White,
            fontSize = PillFontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

// The right-click context menu for a commit node (#253). Its actions are grouped
// by CommitContextMenu, with a divider drawn between each group. Quick View (#254)
// launches the quick view for this commit, Diff with HEAD (#280) diffs it against
// the current HEAD, and Checkout Detached HEAD (#279) checks it out detached; the
// remaining behaviours are wired up in follow-up issues, so those items only
// dismiss for now.
@Composable
private fun CommitContextMenu(
    commit: CommitGraphNode,
    expanded: Boolean,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
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
                        "Checkout Detached HEAD" -> {
                            { scope.launch { GitDownState.checkoutDetachedHead(commit) }; onDismiss() }
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
