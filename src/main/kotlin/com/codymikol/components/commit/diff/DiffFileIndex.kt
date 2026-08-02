package com.codymikol.components.commit.diff

import com.codymikol.data.diff.FileDeltaNode

/**
 * The flat LazyColumn item index of each file's sticky header, mirroring the layout
 * built by the Diff component: one item per file header, then per hunk one hunk header
 * followed by one item per line. Used by the quick view (see issue #278) to scroll the
 * diff so a selected file's header lands at the top.
 */
fun fileHeaderItemIndices(fileDeltaNodes: List<FileDeltaNode>): List<Int> {
    val indices = ArrayList<Int>(fileDeltaNodes.size)
    var running = 0
    fileDeltaNodes.forEach { node ->
        indices.add(running)
        running += 1
        node.hunkNodes.forEach { hunk -> running += 1 + hunk.lineNodes.size }
    }
    return indices
}

/**
 * The index of the file whose header is currently sticky at the top of the diff - the
 * last file header at or above [firstVisibleItemIndex] - or -1 when there are no files.
 * [fileHeaderIndices] is the output of [fileHeaderItemIndices].
 */
fun stickyFileIndex(firstVisibleItemIndex: Int, fileHeaderIndices: List<Int>): Int =
    fileHeaderIndices.indexOfLast { it <= firstVisibleItemIndex }

/**
 * The file selection a scroll position should produce in the quick view (see issue
 * #278), given the file the user currently has selected ([selectedIndex], or a value
 * outside the file range when nothing is selected) and whether the diff is scrolled to
 * its very end ([atEnd]).
 *
 * Normally this follows the sticky top header. But the last files in a diff can be too
 * short for their headers to ever reach the top, so once the list is at its end a scroll
 * reports an earlier file as sticky. Snapping the selection back to that sticky file would
 * fight a selection the user just placed on one of those trailing files (see issue #308),
 * so at the end we keep any selection sitting on or below the sticky file instead.
 * [fileHeaderIndices] is the output of [fileHeaderItemIndices].
 */
fun fileSelectionForScroll(
    firstVisibleItemIndex: Int,
    fileHeaderIndices: List<Int>,
    selectedIndex: Int,
    atEnd: Boolean,
): Int {
    val stickyIndex = stickyFileIndex(firstVisibleItemIndex, fileHeaderIndices)
    if (stickyIndex < 0) return selectedIndex
    if (atEnd && selectedIndex in (stickyIndex + 1)..fileHeaderIndices.lastIndex) return selectedIndex
    return stickyIndex
}
