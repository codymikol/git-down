package com.codymikol.views

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codymikol.data.Colors
import com.codymikol.data.map.CommitGraphNode
import com.codymikol.state.GitDownState
import com.codymikol.state.MapState
import com.codymikol.typography.GitDownTypography
import org.eclipse.jgit.lib.Ref

// Narrower than a horizontal title would need, since titles are now rotated
// diagonally and take up far less width per lane.
private val LaneWidth = 180.dp
private val NodeRadius = 5.dp
private val GutterX = 20.dp
private val RowHeight = 48.dp

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
    }

    val branches = MapState.branches.value
    val rowCount = MapState.maxRowCount.value

    // All lanes render against this one vertical LazyListState, rather than each owning
    // its own, so scrolling any lane scrolls every lane's commit nodes in lock-step. Every
    // lane also renders the same rowCount (see BranchLane), which is what makes sharing
    // this state safe across lanes with different numbers of loaded commits.
    val verticalScrollState = rememberLazyListState()
    val horizontalScrollState = rememberLazyListState()

    when (branches.isEmpty()) {
        true -> MapEmptyState()
        false -> LazyRow(
            state = horizontalScrollState,
            modifier = Modifier.fillMaxWidth().fillMaxHeight().background(Colors.DarkGrayBackground)
        ) {
            items(branches, key = { it.name }) { branch -> BranchLane(branch, verticalScrollState, rowCount) }
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
private fun BranchLane(branch: Ref, verticalScrollState: LazyListState, rowCount: Int) {
    val branchName = branch.name.removePrefix("refs/heads/")
    val commits = MapState.commitsByBranch[branch.name] ?: emptyList()

    // A lane's commits load in full as soon as it composes - LazyRow only composes
    // a lane once it scrolls into view, but there is no further lazy loading past
    // that; every graph, once shown, holds its entire history.
    LaunchedEffect(branch.name) {
        MapState.load(branch)
    }

    Column(
        modifier = Modifier
            .width(LaneWidth)
            .fillMaxHeight()
            .border(width = 1.dp, color = Color.Black)
    ) {
        MapLaneTitle(branchName)
        LazyColumn(state = verticalScrollState, modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            items(rowCount, key = { index -> commits.getOrNull(index)?.sha ?: "blank-$index" }) { index ->
                val commit = commits.getOrNull(index)
                when (commit) {
                    null -> Spacer(modifier = Modifier.fillMaxWidth().height(RowHeight))
                    else -> CommitNode(
                        commit = commit,
                        showLeadingGuideline = index != 0,
                        showTrailingGuideline = index != commits.lastIndex,
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

@Composable
private fun CommitNode(commit: CommitGraphNode, showLeadingGuideline: Boolean, showTrailingGuideline: Boolean) {
    Row(
        modifier = Modifier
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
