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
