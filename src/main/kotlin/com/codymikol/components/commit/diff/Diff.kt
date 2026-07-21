package com.codymikol.components.commit.diff

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import com.codymikol.components.commit.diff.file.header.FileHeader
import com.codymikol.data.diff.FileDeltaNode
import com.codymikol.data.diff.Hunk
import com.codymikol.data.diff.LineNode
import com.codymikol.extensions.*
import com.codymikol.highlighting.GrammarCache
import com.codymikol.highlighting.GrammarExtensionRegistry
import com.codymikol.highlighting.GrammarLanguageLoader
import com.codymikol.highlighting.GrammarParser
import com.codymikol.highlighting.SyntaxHighlighter
import com.codymikol.state.Keys
import com.codymikol.typography.GitDownTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import org.slf4j.LoggerFactory

private val grammarCache: GrammarCache by inject(GrammarCache::class.java)
private val logger = LoggerFactory.getLogger("com.codymikol.components.commit.diff.Diff")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Diff(fileDeltaNodes: List<FileDeltaNode>, showActions: Boolean = true) {
    val dragState = remember { DragSelectionState() }
    val listState = rememberLazyListState()
    val diffItems by remember(fileDeltaNodes) {
        derivedStateOf { buildDiffItems(fileDeltaNodes) }
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
                // Line drag-selection must not start tracking (and therefore must not consume
                // any pointer events) unless the initial down is actually over a diff line.
                // detectDragGestures() begins slop tracking - and consumes the slop-crossing
                // move - unconditionally, before its onDragStart callback can veto an invalid
                // target. That consumption cancels any button underneath the pointer (e.g. the
                // Stage/Unstage/Cog buttons in the sticky file header) whenever the click has
                // even a little incidental movement, so the target check has to happen before
                // any tracking/consumption begins rather than inside onDragStart.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val startLineItem = findLineItemAtPosition(down.position.y, diffItems, listState)
                        ?: return@awaitEachGesture

                    val startFileLines = startLineItem.parentFileNode.hunkNodes.map { it.lineNodes }.flatten()
                    val startIndex = startFileLines.indexOf(startLineItem.lineNode)
                    if (startIndex < 0) return@awaitEachGesture

                    var dragStarted = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break

                        if (!dragStarted) {
                            val dx = change.position.x - down.position.x
                            val dy = change.position.y - down.position.y
                            if (hypot(dx, dy) < viewConfiguration.touchSlop) continue
                            dragStarted = true

                            dragState.isDragging.value = true
                            dragState.didDrag.value = false
                            dragState.startIndex.value = startIndex
                            dragState.activeFileNode.value = startLineItem.parentFileNode

                            if (!Keys.isCtrlPressed.value) {
                                if (!Keys.isShiftPressed.value) {
                                    startFileLines.forEach { it.line.selected.value = false }
                                }
                                startLineItem.lineNode.line.selected.value = true
                            }
                        }

                        change.consume()

                        val lineItem = findLineItemAtPosition(change.position.y, diffItems, listState) ?: continue
                        if (dragState.activeFileNode.value != lineItem.parentFileNode) continue

                        val fileLines = lineItem.parentFileNode.hunkNodes.map { it.lineNodes }.flatten()
                        val dragStartIndex = dragState.startIndex.value ?: continue
                        val currentIndex = fileLines.indexOf(lineItem.lineNode)
                        if (currentIndex < 0) continue

                        dragState.didDrag.value = true
                        selectRange(fileLines, dragStartIndex, currentIndex, Keys.isShiftPressed.value)
                    }

                    dragState.reset()
                }
            }
    ) {
        LazyColumn(state = listState) {
            diffItems.forEach { item ->
                when (item) {
                    is DiffItem.FileHeaderItem -> stickyHeader { FileHeader(item.fileDeltaNode, showActions = showActions) }
                    is DiffItem.HunkHeaderItem -> item { HunkHeader(item.hunk) }
                    is DiffItem.LineItem -> item { DiffLine(item.lineNode) }
                }
            }
        }
    }
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
private fun DiffLine(lineNode: LineNode) {

    Box(modifier = Modifier
        .background(lineNode.line.getBackgroundColor())
        .fillMaxWidth()
        .wrapContentHeight()) {
        Row(modifier = Modifier.fillMaxSize()) {
            LineNumberGutter(lineNode.line.originalLineNumber)
            LineNumberGutter(lineNode.line.newLineNumber)
            ModificationTypeGutter(lineNode)

            val displayLine = lineNode.line.value.replace("\t", "  ")
            var highlighted by remember(lineNode) { mutableStateOf<AnnotatedString?>(null) }

            LaunchedEffect(lineNode) {
                try {
                    val extension = lineNode.parent.parent.getPath().substringAfterLast('.', "")
                    if (extension.isEmpty()) return@LaunchedEffect
                    highlighted = withContext(Dispatchers.IO) { highlightLine(extension, displayLine) }
                } catch (e: Exception) {
                    logger.error("Failed to apply syntax highlighting for line", e)
                }
            }

            val annotatedLine = highlighted
            if (annotatedLine != null) {
                GitDownTypography.DiffContent(annotatedLine, lineNode.line.getTextColor())
            } else {
                GitDownTypography.DiffContent(displayLine, lineNode.line.getTextColor())
            }
        }
    }
}

private suspend fun highlightLine(extension: String, text: String): AnnotatedString? {
    val spec = GrammarExtensionRegistry.forExtension(extension) ?: return null
    val grammarPath = grammarCache.ensureGrammar(extension) ?: return null
    val language = GrammarLanguageLoader.load(grammarPath, spec.functionName) ?: return null
    val tokens = GrammarParser.parse(language, text)
    return SyntaxHighlighter.highlight(text, tokens)
}

@Composable
private fun HunkHeader(hunk: Hunk) {
    Row(modifier = Modifier.background(Color(8, 8, 8)).fillMaxWidth()) {
        Spacer(modifier = Modifier.width(86.dp))
        GitDownTypography.DiffHunkHeader(hunk.delimiter)
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
    listState: LazyListState
): DiffItem.LineItem? {
    val visibleItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { itemInfo ->
        val start = itemInfo.offset
        val end = itemInfo.offset + itemInfo.size
        y >= start && y < end
    } ?: return null

    val item = diffItems.getOrNull(visibleItem.index)
    return item as? DiffItem.LineItem
}
