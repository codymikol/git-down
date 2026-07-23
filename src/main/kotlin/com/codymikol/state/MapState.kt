package com.codymikol.state

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.codymikol.data.map.CommitGraphNode
import com.codymikol.data.map.CommitHistoryWalker
import com.codymikol.extensions.listLocalBranches
import org.eclipse.jgit.lib.Ref

/**
 * Backs the Map view. Branch history is walked lazily - loadMore() only advances
 * a branch's RevWalk as far as the UI has actually scrolled, one CommitHistoryWalker
 * per branch, kept open across calls so re-walking from the start is never needed.
 */
object MapState {

    const val PAGE_SIZE = 30
    const val LOAD_MORE_THRESHOLD = 5

    private val walkers = mutableMapOf<String, CommitHistoryWalker>()

    val commitsByBranch = mutableStateMapOf<String, MutableList<CommitGraphNode>>()

    val hasMoreByBranch = mutableStateMapOf<String, Boolean>()

    val branches = derivedStateOf {
        GitDownState.git.value.listLocalBranches().sortedBy { it.name }
    }

    fun loadMore(branch: Ref) {
        if (hasMoreByBranch[branch.name] == false) return

        val walker = walkers.getOrPut(branch.name) { CommitHistoryWalker(GitDownState.git.value, branch) }
        val page = walker.nextPage(PAGE_SIZE)

        commitsByBranch.getOrPut(branch.name) { mutableStateListOf() }.addAll(page)
        hasMoreByBranch[branch.name] = walker.hasMore
    }

    /**
     * Branches share a single vertical scroll position (see MapScrollState), so a
     * branch's own loaded-commit count is checked against that shared position rather
     * than a lane-local one. lastVisibleIndex must be the last (bottom-most) visible
     * row, not the first - the first visible index stays well short of loadedCount
     * whenever more than LOAD_MORE_THRESHOLD rows fit in the viewport, so it would
     * never trigger paging.
     */
    fun shouldLoadMore(branchName: String, lastVisibleIndex: Int): Boolean {
        if (hasMoreByBranch[branchName] == false) return false
        val loadedCount = commitsByBranch[branchName]?.size ?: 0
        return lastVisibleIndex >= loadedCount - LOAD_MORE_THRESHOLD
    }

    fun reset() {
        walkers.values.forEach { it.close() }
        walkers.clear()
        commitsByBranch.clear()
        hasMoreByBranch.clear()
    }
}
