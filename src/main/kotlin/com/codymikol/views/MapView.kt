package com.codymikol.views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codymikol.data.Colors
import com.codymikol.data.map.CommitGraphNode
import com.codymikol.state.GitDownState
import com.codymikol.state.MapState
import com.codymikol.typography.GitDownTypography
import org.eclipse.jgit.lib.Ref

private val LaneWidth = 260.dp
private val NodeRadius = 5.dp
private val GutterX = 20.dp
private val RowHeight = 48.dp

@Composable
@Preview
fun MapView() {

    LaunchedEffect(GitDownState.gitDirectory.value) {
        MapState.reset()
    }

    val branches = MapState.branches.value
    val rowCount = MapState.maxRowCount.value

    when (branches.isEmpty()) {
        true -> MapEmptyState()
        false -> Map(branches)
    }
}

@Composable
private fun Map(branches: List<Ref>) {

    Column(modifier = Modifier.fillMaxSize()) {

        val lazyHorizontalState = rememberLazyListState()

        LazyRow(
            state = lazyHorizontalState,
            modifier = Modifier.fillMaxWidth().fillMaxHeight().background(Colors.DarkGrayBackground)
        ) {
           items(branches.size, key = { branches[it].name }) { BranchLane(branch = branches[it]) }
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
    ) {
        Text(style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp), modifier = Modifier.padding(8.dp).fillMaxWidth(), color = Color.White, text = branchName)
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {

            commits.forEach {
                CommitNode(
                    commit = it,
                    showLeadingGuideline = false,
                    showTrailingGuideline = false,
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
    println("drawing")
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
    println("drawing path $path")
    drawPath(path, color = color)
}
