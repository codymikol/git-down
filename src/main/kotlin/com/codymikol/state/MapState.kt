package com.codymikol.state

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
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

    /**
     * The commit node whose detail card is currently shown, or null when no node is
     * selected. Node sha/message text is hidden in the map until a node is clicked
     * (see issue #252); clicking toggles this selection.
     *
     * Identity is (branchName, sha), not sha alone: a commit reachable from several
     * branches is drawn once per lane, so keying selection on sha lit every lane's
     * copy at once. Keying on the lane too means only the clicked node's card shows -
     * never more than one at a time (see issue #263).
     */
    val selectedNode = mutableStateOf<SelectedNode?>(null)

    fun toggleSelectedNode(branchName: String, sha: String) {
        val target = SelectedNode(branchName, sha)
        selectedNode.value = if (selectedNode.value == target) null else target
    }

    /**
     * Selects a node outright, without the toggle-to-clear behaviour of
     * toggleSelectedNode. Right-clicking a node opens its context menu and must
     * always leave that node selected (see issue #253), even when it was already
     * the selected node.
     */
    fun selectNode(branchName: String, sha: String) {
        selectedNode.value = SelectedNode(branchName, sha)
    }

    /**
     * True only for the one rendered node matching the current selection's lane and
     * sha, so lanes sharing a sha never light up together (see issue #263).
     */
    fun isNodeSelected(branchName: String, sha: String): Boolean =
        selectedNode.value == SelectedNode(branchName, sha)

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
        selectedNode.value = null
    }
}

/**
 * Identity of a selected commit node: its lane (branchName) plus its sha. Two
 * branches can hold the same commit, so the sha alone does not identify which
 * rendered node is selected (see issue #263).
 */
data class SelectedNode(val branchName: String, val sha: String)
