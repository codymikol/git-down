package com.codymikol.views

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.codymikol.components.Subheader
import com.codymikol.data.Colors
import com.codymikol.data.map.CommitGraphNode
import com.codymikol.state.GitDownState
import com.codymikol.state.MapState
import com.codymikol.typography.GitDownTypography
import org.eclipse.jgit.lib.Ref

private val LaneWidth = 260.dp
private val NodeRadius = 5.dp
private val GutterX = 20.dp
private const val LoadMoreThreshold = 5

@Composable
@Preview
fun MapView() {

    LaunchedEffect(GitDownState.gitDirectory.value) {
        MapState.reset()
    }

    val branches = MapState.branches.value

    when (branches.isEmpty()) {
        true -> MapEmptyState()
        false -> LazyRow(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxWidth().fillMaxHeight().background(Colors.DarkGrayBackground)
        ) {
            items(branches, key = { it.name }) { branch -> BranchLane(branch) }
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
private fun BranchLane(branch: Ref) {
    val branchName = branch.name.removePrefix("refs/heads/")
    val commits = MapState.commitsByBranch[branch.name] ?: emptyList()
    val hasMore = MapState.hasMoreByBranch[branch.name] ?: true
    val listState = rememberLazyListState()

    // Loading this lane's first page only happens once it's actually composed,
    // which LazyRow only does once the lane scrolls into view - this is what
    // lazily loads branches horizontally.
    LaunchedEffect(branch.name) {
        if (MapState.commitsByBranch[branch.name] == null) {
            MapState.loadMore(branch)
        }
    }

    LaunchedEffect(branch.name, commits.size, hasMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val nearBottom = lastVisibleIndex != null && lastVisibleIndex >= commits.size - LoadMoreThreshold
                if (hasMore && nearBottom) {
                    MapState.loadMore(branch)
                }
            }
    }

    Column(
        modifier = Modifier
            .width(LaneWidth)
            .fillMaxHeight()
            .border(width = 1.dp, color = Color.Black)
    ) {
        Subheader(branchName)
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            itemsIndexed(commits, key = { _, commit -> commit.sha }) { index, commit ->
                CommitNode(
                    commit = commit,
                    showLeadingGuideline = index != 0,
                    showTrailingGuideline = index != commits.lastIndex || hasMore,
                )
            }
        }
    }
}

@Composable
private fun CommitNode(commit: CommitGraphNode, showLeadingGuideline: Boolean, showTrailingGuideline: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .drawBehind { drawCommitNode(commit, showLeadingGuideline, showTrailingGuideline) }
            .padding(start = 40.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)
    ) {
        Column {
            GitDownTypography.CommitSha(commit.shortSha)
            GitDownTypography.CommitSubject(commit.shortMessage)
        }
    }
}

private fun DrawScope.drawCommitNode(commit: CommitGraphNode, showLeadingGuideline: Boolean, showTrailingGuideline: Boolean) {
    val x = GutterX.toPx()
    val centerY = size.height / 2f

    if (showLeadingGuideline) {
        drawLine(color = Colors.LightGrayText, start = Offset(x, 0f), end = Offset(x, centerY), strokeWidth = 2f)
    }

    if (showTrailingGuideline) {
        drawLine(color = Colors.LightGrayText, start = Offset(x, centerY), end = Offset(x, size.height), strokeWidth = 2f)
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
