package com.codymikol.views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codymikol.components.SlimButton
import com.codymikol.components.Subheader
import com.codymikol.components.commit.ChangedFile
import com.codymikol.components.commit.CommitBottomToolbar
import com.codymikol.components.commit.ConfirmDialog
import com.codymikol.components.commit.diff.file.header.FileHeader
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
    BasicTextField(
        cursorBrush = Brush.verticalGradient(0.00f to Color.White),
        value = commitMessage.value,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp, start = 8.dp, bottom = 0.dp, end = 8.dp)
            .background(Colors.DarkGrayBackground),
        textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
        onValueChange = { commitMessage.value = it },
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
private fun LineNumberGutter(lineNumber: UInt?) {
    Row(
        modifier = Modifier
            .width(36.dp)
            .fillMaxHeight()
    ) {
        GitDownTypography.LineNumber(lineNumber?.toString() ?: "")
    }
}

@Composable
private fun ModificationTypeGutter(lineNode: LineNode) {
    Box(modifier = Modifier.width(24.dp).fillMaxSize()) { GitDownTypography.DiffType(lineNode.line.symbol) }
}

@Composable
private fun DiffPanel() = when(GitDownState.diffTree.value.fileDeltaNodes.size > 0) {
    true -> Diff()
    false -> Column { EmptyState("No file selected") }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Diff() {
    LazyColumn {
        GitDownState.diffTree.value.fileDeltaNodes.forEach { fileDeltaNode ->
            stickyHeader { FileHeader(fileDeltaNode) }
            fileDeltaNode.hunkNodes.forEach { hunkNode ->
                item { HunkHeader(hunkNode.hunk) }
                hunkNode.lineNodes.forEach { lineNode -> item { DiffLine(lineNode, fileDeltaNode) } }
            }
        }
    }
}

@Composable
private fun DiffLine(lineNode: LineNode, parentFileNode: FileDeltaNode) {

    val fileLines = parentFileNode.hunkNodes.map { it.lineNodes }.flatten()

    //todo(mikol): figure out drag selection :')
    //todo(mikol): limit click hitbox to around the gutter area

    Box(modifier = Modifier
        .background(lineNode.line.getBackgroundColor())
        .fillMaxWidth()
        .wrapContentHeight()) {
        Row(modifier = Modifier
            .clickable {
                when {
                    Keys.isShiftPressed.value -> shiftSelectLine(fileLines, lineNode)
                    Keys.isCtrlPressed.value -> ctrlSelectLine(lineNode)
                    else -> unmodifiedLineSelect(fileLines, lineNode)
                }
            }
            .fillMaxSize()) {
            LineNumberGutter(lineNode.line.originalLineNumber)
            LineNumberGutter(lineNode.line.newLineNumber)
            ModificationTypeGutter(lineNode)
            GitDownTypography.DiffContent(lineNode.line.value, lineNode.line.getTextColor())
        }
    }
}

private fun shiftSelectLine(fileLines: List<LineNode>, lineNode: LineNode) {
    val current = fileLines.indexOf(lineNode)
    val low = fileLines.indexOfFirstOrNull { it.line.selected.value }
    val high = fileLines.indexOfLastOrNull { it.line.selected.value }
    if (null == high || null == low) {
        unmodifiedLineSelect(fileLines, lineNode)
    } else {
        if (current > high) (high until current + 1).forEach { fileLines[it].line.selected.value = true }
        if (current < low) (current until low).forEach { fileLines[it].line.selected.value = true }
    }
}

private fun unmodifiedLineSelect(fileLines: List<LineNode>, lineNode: LineNode) {
    fileLines.forEach { if (lineNode != it) it.line.selected.value = false }
    lineNode.line.toggleSelected()
}

private fun ctrlSelectLine(lineNode: LineNode) {
    lineNode.line.toggleSelected()
}

@Composable
private fun HunkHeader(hunk: Hunk) {
    Row(modifier = Modifier.background(Color(8, 8, 8)).fillMaxWidth()) {
        Spacer(modifier = Modifier.width(86.dp))
        GitDownTypography.DiffHunkHeader(hunk.delimiter)
    }
}

private fun getDeltaEmptyStateMessage(status: Status) = when(status) {
    Status.INDEX -> "No changes in index"
    Status.WORKING_DIRECTORY -> "No changes in working directory"
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
            SlimButton("Discard All...") {
                isConfirmingDiscardAll.value = true
            }
            SlimButton("Stage All") {
                scope.launch {
                    GitDownState.git.value.stageAll()
                    GitDownState.selectedFiles.clear()
                }
            }
        }
    }

}


@Composable
private fun CommitIndex() {
    FileDeltaPanel("Index", GitDownState.index, Status.INDEX)
}
