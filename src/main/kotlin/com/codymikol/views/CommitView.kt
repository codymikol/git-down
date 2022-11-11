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
import com.codymikol.data.diff.FileDeltaNode
import com.codymikol.data.diff.Hunk
import com.codymikol.data.diff.Line
import com.codymikol.data.file.FileDelta
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
private fun ModificationTypeGutter(line: Line) {
    Box(modifier = Modifier.width(24.dp).fillMaxSize()) { GitDownTypography.DiffType(line.symbol) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiffPanel() = LazyColumn {

    GitDownState.diffTree.value.files.forEach { fileDeltaNode ->

        stickyHeader { FileHeader(fileDeltaNode) }

        fileDeltaNode.hunks.forEach { hunkNode ->

            item { HunkHeader(hunkNode.hunk) }

            hunkNode.lines.forEach { line -> item { DiffLine(line, fileDeltaNode) } }

        }
    }
}

@Composable
private fun DiffLine(line: Line, parentFileNode: FileDeltaNode) {

    val fileLines = parentFileNode.hunks.map { it.lines }.flatten()

    //todo(mikol): figure out drag selection :')
    //todo(mikol): limit click hitbox to around the gutter area

    Box(modifier = Modifier
        .background(line.getBackgroundColor())
        .fillMaxWidth()
        .wrapContentHeight()) {
        Row(modifier = Modifier
            .clickable {
                when {
                    Keys.isShiftPressed.value -> shiftSelectLine(fileLines, line)
                    Keys.isCtrlPressed.value -> ctrlSelectLine(line)
                    else -> unmodifiedLineSelect(fileLines, line)
                }
            }
            .fillMaxSize()) {
            LineNumberGutter(line.originalLineNumber)
            LineNumberGutter(line.newLineNumber)
            ModificationTypeGutter(line)
            GitDownTypography.DiffContent(line.value, line.getTextColor())
        }
    }
}

private fun shiftSelectLine(fileLines: List<Line>, line: Line) {
    val current = fileLines.indexOf(line)
    val low = fileLines.indexOfFirstOrNull { it.selected.value }
    val high = fileLines.indexOfLastOrNull { it.selected.value }
    if (null == high || null == low) {
        unmodifiedLineSelect(fileLines, line)
    } else {
        if (current > high) (high until current + 1).forEach { fileLines[it].selected.value = true }
        if (current < low) (current until low).forEach { fileLines[it].selected.value = true }
    }
}

private fun unmodifiedLineSelect(fileLines: List<Line>, line: Line) {
    fileLines.forEach { if (line != it) it.selected.value = false }
    line.toggleSelected()
}

private fun ctrlSelectLine(line: Line) {
    line.toggleSelected()
}

@Composable
private fun HunkHeader(hunk: Hunk) {
    Row(modifier = Modifier.background(Color(8, 8, 8)).fillMaxWidth()) {
        Spacer(modifier = Modifier.width(86.dp))
        GitDownTypography.DiffHunkHeader(hunk.delimiter)
    }
}


@Composable
private fun FileDeltaPanel(title: String, deltas: State<Set<FileDelta>>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Subheader(title)
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
}

@Composable
private fun CommitWorkingDirectory() {

    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxHeight()) {

        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
            FileDeltaPanel("Working Directory", GitDownState.workingDirectory)
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


@Preview
@Composable
private fun CommitIndex() {
    FileDeltaPanel("Index", GitDownState.index)
}
