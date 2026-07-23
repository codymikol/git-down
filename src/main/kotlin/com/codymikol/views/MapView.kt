package com.codymikol.views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codymikol.data.Colors
import com.codymikol.data.map.CommitGraphNode
import com.codymikol.state.GitDownState
import com.codymikol.state.MapScrollState
import com.codymikol.state.MapState
import com.codymikol.typography.GitDownTypography
import org.eclipse.jgit.lib.Ref
import kotlin.math.roundToInt

private val LaneWidth = 260.dp
private val NodeRadius = 5.dp
private val GutterX = 20.dp
private val RowHeight = 48.dp
private val LaneHeaderHeight = 32.dp

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
    val headerHeightPx = with(LocalDensity.current) { LaneHeaderHeight.toPx() }
    var viewportHeightPx by remember { mutableStateOf(0f) }

    // Every lane's own row viewport sits below its LaneHeaderHeight header, so windowing
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
        Text(
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp),
            modifier = Modifier.height(LaneHeaderHeight).padding(8.dp).fillMaxWidth(),
            color = Color.White,
            text = branchName,
        )
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
private fun CommitNode(
    commit: CommitGraphNode,
    showLeadingGuideline: Boolean,
    showTrailingGuideline: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight)
            .drawBehind { drawCommitNode(commit, showLeadingGuideline, showTrailingGuideline) }
            .padding(start = 40.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)
    ) {
        Column {
            GitDownTypography.CommitSha(commit.shortSha)
            GitDownTypography.CommitSubject(commit.shortMessage)
        }
    }
}

private fun DrawScope.drawCommitNode(
    commit: CommitGraphNode,
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

    when (commit.isMergeCommit) {
        true -> drawDiamond(center = Offset(x, centerY), radius = NodeRadius.toPx(), color = Colors.FileModified)
        false -> drawCircle(color = Colors.FileAdded, radius = NodeRadius.toPx(), center = Offset(x, centerY))
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
