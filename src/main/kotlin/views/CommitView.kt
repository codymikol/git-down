package views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import components.SlimButton
import components.Subheader
import components.commit.CommitBottomToolbar
import components.commit.CommitEmptyState
import components.commit.Dabuggy
import data.Colors
import extensions.stageAll
import kotlinx.coroutines.launch
import state.GitDownState

val commitMessage = mutableStateOf("")

class GitDownTextFieldColors : TextFieldColors {
    @Composable
    override fun backgroundColor(enabled: Boolean): State<Color> = mutableStateOf(Colors.DarkGrayBackground)

    @Composable
    override fun cursorColor(isError: Boolean): State<Color> = mutableStateOf(Color.White)

    @Composable
    override fun indicatorColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> = when (enabled) {
        true -> mutableStateOf(Colors.LightGrayBackground)
        false -> mutableStateOf(Color.Gray)
    }

    @Composable
    override fun labelColor(enabled: Boolean, error: Boolean, interactionSource: InteractionSource): State<Color> =
        mutableStateOf(Color.White)

    @Composable
    override fun leadingIconColor(enabled: Boolean, isError: Boolean): State<Color> = mutableStateOf(Color.White)

    @Composable
    override fun placeholderColor(enabled: Boolean): State<Color> = mutableStateOf(Color.White)

    @Composable
    override fun textColor(enabled: Boolean): State<Color> = mutableStateOf(Color.White)

    @Composable
    override fun trailingIconColor(enabled: Boolean, isError: Boolean): State<Color> = mutableStateOf(Color.White)
}

@Composable
@Preview
fun CommitView() {
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
        Row(modifier = Modifier.weight(40f, true).fillMaxWidth()) {
            Column(
                modifier = Modifier.background(Colors.DarkGrayBackground).weight(40f).fillMaxHeight()
                    .border(width = 1.dp, color = Color.Black)
            ) {
                Column(
                    modifier = Modifier.weight(50f).verticalScroll(state = ScrollState(initial = 0))
                ) { CommitWorkingDirectory() }
                Column(
                    modifier = Modifier.weight(50f).verticalScroll(state = ScrollState(initial = 0))
                ) { CommitIndex() }
            }
            Column(
                modifier = Modifier.weight(60f).fillMaxHeight().background(Colors.DarkGrayBackground)
                    .border(width = 1.dp, color = Color.Black).verticalScroll(ScrollState(0))
            ) {
//                CommitEmptyState("No file selected")
                Dabuggy(GitDownState.removed, "R", Color.Green, "Removed")
                Dabuggy(GitDownState.added, "A", Color.Blue, "Added")
                Dabuggy(GitDownState.changed, "C", Color.Black, "Changed")
//                Dabuggy(GitDownState.ignoredNotInIndex, "I", Color.Yellow, "IgnoredNotInIndex")
                Dabuggy(GitDownState.missing, "M", Color.Transparent, "Missing")
                Dabuggy(GitDownState.conflicting, "X", Color.White, "Conflicting")
                Dabuggy(GitDownState.uncommittedChanged, "U", Color.Cyan, "Uncomitted Changes")
                Dabuggy(GitDownState.modified, "L", Color.Magenta, "Modified")
                Dabuggy(GitDownState.untracked, "U", Color.LightGray, "Untracked")
            }
        }
        Column(Modifier.weight(10f)) {
            Column(modifier = Modifier.fillMaxWidth().background(Colors.LightGrayBackground)) {
                TextField(
                    modifier = Modifier.fillMaxSize().padding(top = 8.dp, start = 8.dp, bottom = 0.dp, end = 8.dp)
                        .background(Colors.DarkGrayBackground),
                    colors = GitDownTextFieldColors(),
                    onValueChange = { commitMessage.value = it },
                    value = commitMessage.value
                )
            }
        }
        CommitBottomToolbar(commitMessage)
    }
}

@Composable
private fun ColumnScope.CommitIndex() {
    Subheader("Index")
    if (GitDownState.indexHasChanges.value) {
        Dabuggy(GitDownState.indexFilesAdded, "A", Color.Green)
        Dabuggy(GitDownState.removed, "D", Color.Red)
        Dabuggy(GitDownState.conflicting, "C", Color.Cyan)
        Dabuggy(GitDownState.indexFilesModified, "M", Color.Blue)
    } else {
        Column { CommitEmptyState("No changes in index") }
    }
}

@Composable
private fun ColumnScope.CommitWorkingDirectory() {

    val scope = rememberCoroutineScope()

    Subheader("Working Directory")
    Box {
        if (GitDownState.workingDirectoryHasChanges.value) {
            Dabuggy(GitDownState.workingDirectoryFilesModified, "M", Color.Blue)
        } else {
            Column { CommitEmptyState("No changes in working directory") }
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().requiredHeight(48.dp).background(Colors.MediumGrayBackground)
            .padding(horizontal = 8.dp)
    ) {
        SlimButton("Discard All...")
        SlimButton("Stage All") {
            scope.launch { GitDownState.git.value.stageAll() }
        }
    }
}
