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
    val dragState = remember { DragSelectionState() }
    val listState = rememberLazyListState()
    val diffItems by remember(GitDownState.diffTree.value.fileDeltaNodes) {
        derivedStateOf { buildDiffItems(GitDownState.diffTree.value.fileDeltaNodes) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(diffItems, listState) {
                detectTapGestures { position ->
                    val lineItem = findLineItemAtPosition(position.y, diffItems, listState) ?: return@detectTapGestures
                    val fileLines = lineItem.parentFileNode.hunkNodes.map { it.lineNodes }.flatten()
                    val lineNode = lineItem.lineNode
                    when {
                        Keys.isShiftPressed.value -> shiftSelectLine(fileLines, lineNode)
                        Keys.isCtrlPressed.value -> ctrlSelectLine(lineNode)
                        else -> unmodifiedLineSelect(fileLines, lineNode)
                    }
                }
            }
            .pointerInput(diffItems, listState) {
                detectDragGestures(
                    onDragStart = { position ->
                        val lineItem = findLineItemAtPosition(position.y, diffItems, listState) ?: return@detectDragGestures
                        val fileLines = lineItem.parentFileNode.hunkNodes.map { it.lineNodes }.flatten()
                        val startIndex = fileLines.indexOf(lineItem.lineNode)
                        if (startIndex < 0) return@detectDragGestures

                        dragState.isDragging.value = true
                        dragState.didDrag.value = false
                        dragState.startIndex.value = startIndex
                        dragState.activeFileNode.value = lineItem.parentFileNode

                        if (!Keys.isCtrlPressed.value) {
                            if (!Keys.isShiftPressed.value) {
                                fileLines.forEach { it.line.selected.value = false }
                            }
                            lineItem.lineNode.line.selected.value = true
                        }
                    },
                    onDrag = { change, _ ->
                        if (!dragState.isDragging.value) return@detectDragGestures
                        val lineItem = findLineItemAtPosition(change.position.y, diffItems, listState) ?: return@detectDragGestures
                        if (dragState.activeFileNode.value != lineItem.parentFileNode) return@detectDragGestures

                        val fileLines = lineItem.parentFileNode.hunkNodes.map { it.lineNodes }.flatten()
                        val startIndex = dragState.startIndex.value ?: return@detectDragGestures
                        val currentIndex = fileLines.indexOf(lineItem.lineNode)
                        if (currentIndex < 0) return@detectDragGestures

                        dragState.didDrag.value = true
                        selectRange(fileLines, startIndex, currentIndex, Keys.isShiftPressed.value)
                    },
                    onDragEnd = { dragState.reset() },
                    onDragCancel = { dragState.reset() }
                )
            }
    ) {
        LazyColumn(state = listState) {
            diffItems.forEach { item ->
                when (item) {
                    is DiffItem.FileHeaderItem -> stickyHeader { FileHeader(item.fileDeltaNode) }
                    is DiffItem.HunkHeaderItem -> item { HunkHeader(item.hunk) }
                    is DiffItem.LineItem -> item { DiffLine(item.lineNode) }
                }
            }
        }
    }
}

@Composable
private fun DiffLine(lineNode: LineNode) {

    //todo(mikol): limit click hitbox to around the gutter area

    Box(modifier = Modifier
        .background(lineNode.line.getBackgroundColor())
        .fillMaxWidth()
        .wrapContentHeight()) {
        Row(modifier = Modifier.fillMaxSize()) {
            LineNumberGutter(lineNode.line.originalLineNumber)
            LineNumberGutter(lineNode.line.newLineNumber)
            ModificationTypeGutter(lineNode)

            val displayLine = lineNode.line.value.replace("\t", "  ")

            GitDownTypography.DiffContent(displayLine, lineNode.line.getTextColor())
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

private fun selectRange(fileLines: List<LineNode>, startIndex: Int, endIndex: Int, additive: Boolean) {
    val low = minOf(startIndex, endIndex)
    val high = maxOf(startIndex, endIndex)

    if (!additive) {
        fileLines.forEach { it.line.selected.value = false }
    }

    (low..high).forEach { fileLines[it].line.selected.value = true }
}

private class DragSelectionState {
    val isDragging = mutableStateOf(false)
    val didDrag = mutableStateOf(false)
    val startIndex = mutableStateOf<Int?>(null)
    val activeFileNode = mutableStateOf<FileDeltaNode?>(null)

    fun reset() {
        isDragging.value = false
        didDrag.value = false
        startIndex.value = null
        activeFileNode.value = null
    }
}

private sealed class DiffItem {
    data class FileHeaderItem(val fileDeltaNode: FileDeltaNode) : DiffItem()
    data class HunkHeaderItem(val hunk: Hunk) : DiffItem()
    data class LineItem(val lineNode: LineNode, val parentFileNode: FileDeltaNode) : DiffItem()
}

private fun buildDiffItems(fileDeltaNodes: List<FileDeltaNode>): List<DiffItem> {
    val items = ArrayList<DiffItem>()
    fileDeltaNodes.forEach { fileDeltaNode ->
        items.add(DiffItem.FileHeaderItem(fileDeltaNode))
        fileDeltaNode.hunkNodes.forEach { hunkNode ->
            items.add(DiffItem.HunkHeaderItem(hunkNode.hunk))
            hunkNode.lineNodes.forEach { lineNode ->
                items.add(DiffItem.LineItem(lineNode, fileDeltaNode))
            }
        }
    }
    return items
}

private fun findLineItemAtPosition(
    y: Float,
    diffItems: List<DiffItem>,
    listState: androidx.compose.foundation.lazy.LazyListState
): DiffItem.LineItem? {
    val visibleItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { itemInfo ->
        val start = itemInfo.offset
        val end = itemInfo.offset + itemInfo.size
        y >= start && y < end
    } ?: return null

    val item = diffItems.getOrNull(visibleItem.index)
    return item as? DiffItem.LineItem
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
