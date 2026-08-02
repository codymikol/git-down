package com.codymikol.views

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codymikol.components.ReversedEllipsisText
import com.codymikol.components.Subheader
import com.codymikol.components.commit.FileIcon
import com.codymikol.components.commit.changedFileDisplayText
import com.codymikol.components.commit.diff.Diff
import com.codymikol.components.commit.diff.fileHeaderItemIndices
import com.codymikol.components.commit.diff.fileSelectionForScroll
import com.codymikol.components.commit.diff.stickyFileIndex
import com.codymikol.data.Colors
import com.codymikol.data.diff.FileDeltaNode
import com.codymikol.data.map.CommitGraphNode
import com.codymikol.data.map.QuickViewCommitDetails
import com.codymikol.state.GitDownState
import java.util.Date

// The commit details are capped at this height and scroll past it (see issue #290) so a
// long commit message never crowds out the file list that fills the space below them.
private val CommitDetailsMaxHeight = 240.dp

/**
 * The quick view (see issue #254): an ephemeral overlay launched against a single
 * commit. Its commit details sit on the left and the commit's diff (reusing the
 * shared Diff component, read-only) on the right, mirroring the stash view layout.
 */
@Composable
@Preview
fun QuickView() {
    // The diff's list state is hoisted here so the left panel's file list can scroll the
    // right panel to a clicked file and observe which file header is sticky at its top.
    val diffListState = rememberLazyListState()

    Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
        Column(
            modifier = Modifier
                .background(Colors.DarkGrayBackground)
                .weight(40f)
                .fillMaxHeight()
                .border(width = 1.dp, color = Color.Black)
        ) {
            QuickViewDetailPanel(diffListState)
        }
        Column(
            modifier = Modifier
                .weight(60f)
                .fillMaxHeight()
                .background(Colors.DarkGrayBackground)
                .border(width = 1.dp, color = Color.Black)
        ) {
            QuickViewDiffPanel(diffListState)
        }
    }
}

@Composable
private fun ColumnScope.QuickViewDetailPanel(diffListState: LazyListState) {
    Subheader("Commit")
    when (val commit = GitDownState.quickViewCommit.value) {
        null -> QuickViewEmptyState("No commit selected")
        else -> {
            CommitDetails(commit)
            QuickViewFileList(diffListState)
        }
    }
}

/**
 * The list of files changed in the quick view's commit (see issue #278), drawn directly
 * beneath the commit details behind a thin themed separator and filling the rest of the
 * panel (see issue #290). Selecting a file highlights it and scrolls the diff so its
 * header is at the top; the highlight also tracks whichever header is sticky.
 */
@Composable
private fun ColumnScope.QuickViewFileList(diffListState: LazyListState) {
    val nodes = GitDownState.quickViewDiffTree.value.fileDeltaNodes
    if (nodes.isEmpty()) return

    val selectedPath = GitDownState.quickViewSelectedFilePath.value
    val selectedIndex = nodes.indexOfFirst { it.getPath() == selectedPath }.coerceAtLeast(0)

    // A thin themed separator between the commit details above and the file list, rather
    // than a hard black line (see issue #290).
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Colors.LightGrayBackground))

    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
        itemsIndexed(nodes) { index, node ->
            QuickViewFileRow(
                node = node,
                selected = index == selectedIndex,
                onClick = { GitDownState.selectQuickViewFile(node.getPath()) },
            )
        }
    }
}

@Composable
private fun QuickViewFileRow(node: FileDeltaNode, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) Color(0, 89, 207) else Color.Transparent

    Row(
        modifier = Modifier
            .clickable { onClick() }
            .fillMaxWidth()
            .height(18.dp)
            .background(background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(12.dp))
        FileIcon(fileDelta = node.fileDelta)
        ReversedEllipsisText(
            text = changedFileDisplayText(node.fileDelta),
            modifier = Modifier.weight(1f).padding(6.dp, 0.dp, 0.dp, 0.dp),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun CommitDetails(commit: CommitGraphNode) {
    val now = remember(commit.sha) { Date() }

    // Wrap-content (no weight) so the details sit directly under the subheader at the
    // top of the panel rather than stretching and pushing the file list to the bottom
    // (see issue #290). Capped and scrollable so a long commit message can never starve
    // the file list that fills the space below it.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = CommitDetailsMaxHeight)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = QuickViewCommitDetails.hash(commit),
            color = Colors.LightGrayText,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = QuickViewCommitDetails.message(commit),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CommitterAvatar(commit)
            Spacer(Modifier.width(8.dp))
            Text(
                text = QuickViewCommitDetails.committer(commit),
                color = Colors.LightGrayText,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = QuickViewCommitDetails.date(commit.date, now),
            color = Colors.LightGrayText,
            fontSize = 12.sp
        )
    }
}

// A small ASCII-initial avatar for the committer - the "<user ascii>" glyph the
// issue calls for, drawn from the first letter of the committer's name.
@Composable
private fun CommitterAvatar(commit: CommitGraphNode) {
    val initial = commit.authorName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Box(
        modifier = Modifier.size(24.dp).clip(CircleShape).background(Colors.LightGrayBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(text = initial, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QuickViewDiffPanel(diffListState: LazyListState) = when (GitDownState.quickViewCommit.value) {
    null -> Column { QuickViewEmptyState("No commit selected") }
    else -> {
        val nodes = GitDownState.quickViewDiffTree.value.fileDeltaNodes
        when (nodes.isNotEmpty()) {
            true -> {
                QuickViewDiffSync(nodes, diffListState)
                Diff(nodes, showActions = false, listState = diffListState)
            }
            false -> Column { QuickViewEmptyState("No changes in commit") }
        }
    }
}

/**
 * Keeps the quick view file selection (see issue #278) and the diff scroll position in
 * sync: scrolling updates the selection to whichever file header is sticky at the top,
 * and selecting a different file scrolls its header to the top.
 */
@Composable
private fun QuickViewDiffSync(nodes: List<FileDeltaNode>, diffListState: LazyListState) {
    // Scroll -> selection: the sticky top header is the highlighted file, except that a
    // selection the user placed on a trailing file too short to reach the top stands once
    // the list is scrolled to its end (see issue #308) rather than snapping back up.
    LaunchedEffect(nodes, diffListState) {
        val headerIndices = fileHeaderItemIndices(nodes)
        snapshotFlow { diffListState.firstVisibleItemIndex to !diffListState.canScrollForward }
            .collect { (firstVisible, atEnd) ->
                val selectedPath = GitDownState.quickViewSelectedFilePath.value
                val selectedIndex = nodes.indexOfFirst { it.getPath() == selectedPath }
                val targetIndex = fileSelectionForScroll(firstVisible, headerIndices, selectedIndex, atEnd)
                if (targetIndex < 0 || targetIndex == selectedIndex) return@collect
                GitDownState.selectQuickViewFile(nodes[targetIndex].getPath())
            }
    }

    // Selection -> scroll: only when the selected file is not already the sticky one, so
    // selection updates that come from a manual scroll do not fight the user's scroll.
    val selectedPath = GitDownState.quickViewSelectedFilePath.value
    LaunchedEffect(selectedPath, nodes) {
        if (selectedPath == null) return@LaunchedEffect
        val selectedIndex = nodes.indexOfFirst { it.getPath() == selectedPath }
        if (selectedIndex < 0) return@LaunchedEffect
        val headerIndices = fileHeaderItemIndices(nodes)
        val stickyIndex = stickyFileIndex(diffListState.firstVisibleItemIndex, headerIndices)
        if (selectedIndex != stickyIndex) diffListState.scrollToItem(headerIndices[selectedIndex])
    }
}

@Composable
private fun ColumnScope.QuickViewEmptyState(message: String) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(message, color = Color.Gray)
        }
    }
}
