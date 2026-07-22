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

    /**
     * Every lane's LazyColumn renders exactly this many rows (padding shorter branches
     * with blank rows past their own commits) so all lanes share the same item count.
     * That's what makes it safe for every lane to render against one shared
     * LazyListState (see MapView) - a shared state clamps to whichever lane's item
     * count last measured, so mismatched counts would fight over the scroll position.
     */
    val maxLoadedRowCount = derivedStateOf {
        commitsByBranch.values.maxOfOrNull { it.size } ?: 0
    }

    fun loadMore(branch: Ref) {
        if (hasMoreByBranch[branch.name] == false) return

        val walker = walkers.getOrPut(branch.name) { CommitHistoryWalker(GitDownState.git.value, branch) }
        val page = walker.nextPage(PAGE_SIZE)

        commitsByBranch.getOrPut(branch.name) { mutableStateListOf() }.addAll(page)
        hasMoreByBranch[branch.name] = walker.hasMore
    }

    /**
     * Branches share a single vertical scroll position (see MapView), so a branch's own
     * loaded-commit count is checked against that shared position rather than a lane-local
     * one. lastVisibleIndex must be the last (bottom-most) visible row, not the first - the
     * first visible index stays well short of loadedCount whenever more than
     * LOAD_MORE_THRESHOLD rows fit in the viewport, so it would never trigger paging.
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
