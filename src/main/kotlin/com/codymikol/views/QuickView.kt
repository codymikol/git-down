package com.codymikol.views

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codymikol.components.Subheader
import com.codymikol.components.commit.diff.Diff
import com.codymikol.data.Colors
import com.codymikol.data.map.CommitGraphNode
import com.codymikol.data.map.QuickViewCommitDetails
import com.codymikol.state.GitDownState
import java.util.Date

/**
 * The quick view (see issue #254): an ephemeral overlay launched against a single
 * commit. Its commit details sit on the left and the commit's diff (reusing the
 * shared Diff component, read-only) on the right, mirroring the stash view layout.
 */
@Composable
@Preview
fun QuickView() {
    Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
        Column(
            modifier = Modifier
                .background(Colors.DarkGrayBackground)
                .weight(40f)
                .fillMaxHeight()
                .border(width = 1.dp, color = Color.Black)
        ) {
            QuickViewDetailPanel()
        }
        Column(
            modifier = Modifier
                .weight(60f)
                .fillMaxHeight()
                .background(Colors.DarkGrayBackground)
                .border(width = 1.dp, color = Color.Black)
        ) {
            QuickViewDiffPanel()
        }
    }
}

@Composable
private fun ColumnScope.QuickViewDetailPanel() {
    Subheader("Commit")
    when (val commit = GitDownState.quickViewCommit.value) {
        null -> QuickViewEmptyState("No commit selected")
        else -> CommitDetails(commit)
    }
}

@Composable
private fun ColumnScope.CommitDetails(commit: CommitGraphNode) {
    val now = remember(commit.sha) { Date() }

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
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
private fun QuickViewDiffPanel() = when (GitDownState.quickViewCommit.value) {
    null -> Column { QuickViewEmptyState("No commit selected") }
    else -> when (GitDownState.quickViewDiffTree.value.fileDeltaNodes.isNotEmpty()) {
        true -> Diff(GitDownState.quickViewDiffTree.value.fileDeltaNodes, showActions = false)
        false -> Column { QuickViewEmptyState("No changes in commit") }
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
