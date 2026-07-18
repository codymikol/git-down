package com.codymikol.views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codymikol.components.SlimButton
import com.codymikol.components.Subheader
import com.codymikol.components.commit.ChangedFile
import com.codymikol.components.commit.CommitBottomToolbar
import com.codymikol.components.commit.ConfirmDialog
import com.codymikol.components.commit.diff.Diff
import com.codymikol.data.Colors
import com.codymikol.data.diff.*
import com.codymikol.data.file.FileDelta
import com.codymikol.data.file.Status
import com.codymikol.extensions.*
import com.codymikol.state.GitDownState
import com.codymikol.state.Keys
import com.codymikol.typography.GitDownTypography
import kotlinx.coroutines.launch


val commitMessage = mutableStateOf("")
val isConfirmingDiscardAll = mutableStateOf(false)
val isCommitMessageFocused = mutableStateOf(false)

@Composable
@Preview
fun CommitView() {

    val scope = rememberCoroutineScope()

    if (isConfirmingDiscardAll.value) {
        ConfirmDialog(title = "Discard Changes?",
            content = "This will discard all changes in the working directory, are you sure?",
            { isConfirmingDiscardAll.value = false },
            {
                scope.launch {
                    GitDownState.git.value.discardAllWorkingDirectory()
                    GitDownState.selectedFiles.clear()
                    isConfirmingDiscardAll.value = false
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(40f, true).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .background(Colors.DarkGrayBackground)
                    .weight(40f).fillMaxHeight()
                    .border(width = 1.dp, color = Color.Black)
            ) {
                Column(modifier = Modifier.weight(50f)) { CommitWorkingDirectory() }
                Column(modifier = Modifier.weight(50f)) { CommitIndex() }
            }
            Column(
                modifier = Modifier
                    .weight(60f)
                    .fillMaxHeight()
                    .background(Colors.DarkGrayBackground)
                    .border(width = 1.dp, color = Color.Black)
            ) {
                DiffPanel()
            }
        }
        Column(Modifier.weight(10f)) {
            Column(modifier = Modifier.fillMaxWidth().background(Colors.LightGrayBackground)) {
                CommitMessageInput()
            }
        }
        CommitBottomToolbar(commitMessage)
    }
}

fun DrawScope.drawCommitGuideline(xOffset: Float, lineHeight: Float, spaceHeight: Float) {

    val pathEffect =
        PathEffect.dashPathEffect(floatArrayOf(lineHeight, spaceHeight), 0f)

    drawLine(
        color = Color.White,
        start = Offset(xOffset, 0f),
        end = Offset(xOffset, size.height),
        pathEffect = pathEffect,
        strokeWidth = 0.2f,
    )

}

@Composable
private fun CommitMessageInput() {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(commitMessage.value)) }

    // commitMessage can be mutated externally (amend, post-commit clear); resync when it drifts.
    if (textFieldValue.text != commitMessage.value) {
        textFieldValue = TextFieldValue(commitMessage.value, TextRange(commitMessage.value.length))
    }

    BasicTextField(
        cursorBrush = Brush.verticalGradient(0.00f to Color.White),
        value = textFieldValue,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp, start = 8.dp, bottom = 0.dp, end = 8.dp)
            .background(Colors.DarkGrayBackground)
            .onFocusChanged { isCommitMessageFocused.value = it.isFocused }
            .onPreviewKeyEvent { event ->
                val isShiftEnter = event.type == KeyEventType.KeyDown &&
                    event.key == Key.Enter &&
                    event.isShiftPressed

                if (!isShiftEnter) return@onPreviewKeyEvent false

                val selection = textFieldValue.selection
                val newText = textFieldValue.text.replaceRange(selection.min, selection.max, "\n")
                val newValue = TextFieldValue(newText, TextRange(selection.min + 1))

                textFieldValue = newValue
                commitMessage.value = newValue.text

                true
            },
        textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
        onValueChange = {
            textFieldValue = it
            commitMessage.value = it.text
        },
        decorationBox = { innerTextField ->
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp, 4.dp, 8.dp, 0.dp)
                .drawBehind {
                    drawCommitGuideline(330f, 2f, 4f)
                    drawCommitGuideline(475f, 4f, 2f)
                }) {
                innerTextField()
            }
        }
    )
}

@Composable
private fun DiffPanel() = when(GitDownState.diffTree.value.fileDeltaNodes.size > 0) {
    true -> Diff(GitDownState.diffTree.value.fileDeltaNodes)
    false -> Column { EmptyState("No file selected") }
}

private fun getDeltaEmptyStateMessage(status: Status) = when(status) {
    Status.INDEX -> "No changes in index"
    Status.WORKING_DIRECTORY -> "No changes in working directory"
    Status.STASH -> "No changes in stash"
}

@Composable
private fun FileDeltaPanel(title: String, deltas: State<Set<FileDelta>>, status: Status) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Subheader(title)
        when(deltas.value.isNotEmpty()) {
            true -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(state = ScrollState(initial = 0))
                ) {
                    Spacer(Modifier.height(6.dp))
                    deltas.value.forEach { ChangedFile(it) }
                    Spacer(Modifier.height(6.dp))
                }
            }
            false -> EmptyState(getDeltaEmptyStateMessage(status))
        }

    }
}

@Composable
private fun ColumnScope.EmptyState(message: String) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(message, color = Color.Gray)
        }
    }
}

@Composable
private fun CommitWorkingDirectory() {

    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxHeight()) {

        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
            FileDeltaPanel("Working Directory", GitDownState.workingDirectory, Status.WORKING_DIRECTORY)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(48.dp)
                .background(Colors.MediumGrayBackground)
                .padding(horizontal = 8.dp)
        ) {

            SlimButton("Discard All...", onClick = {
                isConfirmingDiscardAll.value = true
            })

            SlimButton("Stage All", onClick = {
                scope.launch {
                    GitDownState.git.value.stageAll()
                    GitDownState.selectedFiles.clear()
                }
            })
        }
    }

}


@Composable
private fun CommitIndex() {
    FileDeltaPanel("Index", GitDownState.index, Status.INDEX)
}
