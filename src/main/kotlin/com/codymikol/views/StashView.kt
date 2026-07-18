package com.codymikol.views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.codymikol.components.Subheader
import com.codymikol.components.stash.StashRow
import com.codymikol.data.Colors
import com.codymikol.state.GitDownState

@Composable
@Preview
fun StashView() {
    Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
        Column(
            modifier = Modifier
                .background(Colors.DarkGrayBackground)
                .weight(40f)
                .fillMaxHeight()
                .border(width = 1.dp, color = Color.Black)
        ) {
            StashList()
        }
        Column(
            modifier = Modifier
                .weight(60f)
                .fillMaxHeight()
                .background(Colors.DarkGrayBackground)
                .border(width = 1.dp, color = Color.Black)
        ) {
            StashDiffPanel()
        }
    }
}

@Composable
private fun ColumnScope.StashList() {
    Subheader("Stashes")
    when (GitDownState.stashes.value.isNotEmpty()) {
        true -> Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(state = rememberScrollState())
        ) {
            GitDownState.stashes.value.forEach { stash ->
                StashRow(
                    stash = stash,
                    selected = GitDownState.selectedStash.value == stash,
                    onClick = { GitDownState.selectedStash.value = stash }
                )
            }
        }
        false -> StashEmptyState("No stashes")
    }
}

@Composable
private fun StashDiffPanel() = when (GitDownState.selectedStash.value) {
    null -> Column { StashEmptyState("No stash selected") }
    else -> Column { StashEmptyState("Stash diff view coming soon") }
}

@Composable
private fun ColumnScope.StashEmptyState(message: String) {
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
