package com.codymikol.state

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.codymikol.data.map.CommitGraphNode
import com.codymikol.data.map.CommitHistoryWalker
import com.codymikol.extensions.listLocalBranches
import com.codymikol.services.BranchTipOrderer
import com.codymikol.services.LaneAssigner

/**
 * Backs the Map view. Every local branch tip is walked in one merged RevWalk (see
 * #270), so a commit reachable from more than one branch is loaded and rendered
 * exactly once (#263) - loadMore() only advances that single walker as far as the
 * UI has actually scrolled, kept open across calls so re-walking from the start is
 * never needed.
 */
object MapState {

    const val PAGE_SIZE = 30
    const val LOAD_MORE_THRESHOLD = 5

    private var walker: CommitHistoryWalker? = null
    private var laneAssigner = LaneAssigner()

    val commits = mutableStateListOf<CommitGraphNode>()

    val lanesBySha = mutableStateMapOf<String, Int>()

    var hasMore = true

    /**
     * Sha of the commit node whose detail card is currently shown, or null when no
     * node is selected. Node sha/message text is hidden in the map until a node is
     * clicked (see issue #252); clicking toggles this selection.
     */
    val selectedNodeSha = mutableStateOf<String?>(null)

    fun toggleSelectedNode(sha: String) {
        selectedNodeSha.value = if (selectedNodeSha.value == sha) null else sha
    }

    /**
     * Selects a node outright, without the toggle-to-clear behaviour of
     * toggleSelectedNode. Right-clicking a node opens its context menu and must
     * always leave that node selected (see issue #253), even when it was already
     * the selected node.
     */
    fun selectNode(sha: String) {
        selectedNodeSha.value = sha
    }

    /**
     * The full node for the currently selected sha, looked up across every loaded
     * commit, or null when nothing is selected (or the node has scrolled out of the
     * loaded window). Used to launch the quick view (see issue #254) for the node
     * the user selected in the map.
     */
    fun selectedNode(): CommitGraphNode? {
        val sha = selectedNodeSha.value ?: return null
        return commits.firstOrNull { it.sha == sha }
    }

    val branches = derivedStateOf {
        GitDownState.git.value.listLocalBranches().sortedBy { it.name }
    }

    /**
     * Every local branch's tip commit sha, mapped to the name(s) of the branch(es)
     * pointing at it - a commit shared by multiple branch tips collects all of their
     * names. Backs the branch-name pills shown on tip commits (see issue #272).
     */
    val branchTipsBySha = derivedStateOf {
        branches.value.groupBy(
            keySelector = { it.objectId.name },
            valueTransform = { it.name.removePrefix("refs/heads/") }
        )
    }

    /**
     * Sha of the commit the current HEAD points at, or null when the repository has
     * no commits yet (an unresolvable HEAD). Backs the "HEAD" label drawn on that
     * node in the map (see issue #282) so the user can see where HEAD sits. Keyed on
     * GitDownState.git like the other tip state, so it recomputes after a checkout
     * moves HEAD.
     */
    val headSha = derivedStateOf {
        GitDownState.git.value.repository.resolve("HEAD")?.name
    }

    /**
     * Every branch tip ordered for the pill row pinned to the top of the map (see
     * issue #277): the default branch first, then each side branch by how soon it
     * merges back into the default tip. Left-to-right this reads as the lanes
     * cleanly converging on the mainline.
     */
    val orderedBranchTips = derivedStateOf {
        BranchTipOrderer.order(
            GitDownState.git.value,
            GitDownState.git.value.repository.branch,
            branches.value,
        )
    }

    fun loadMore() {
        if (!hasMore) return

        val walker = walker ?: CommitHistoryWalker(GitDownState.git.value, branches.value).also { walker = it }
        val page = walker.nextPage(PAGE_SIZE)

        page.forEach { lanesBySha[it.sha] = laneAssigner.assign(it) }
        commits.addAll(page)
        hasMore = walker.hasMore
    }

    /**
     * lastVisibleIndex must be the last (bottom-most) visible row, not the first -
     * the first visible index stays well short of commits.size whenever more than
     * LOAD_MORE_THRESHOLD rows fit in the viewport, so it would never trigger paging.
     */
    fun shouldLoadMore(lastVisibleIndex: Int): Boolean {
        if (!hasMore) return false
        return lastVisibleIndex >= commits.size - LOAD_MORE_THRESHOLD
    }

    fun reset() {
        walker?.close()
        walker = null
        laneAssigner = LaneAssigner()
        commits.clear()
        lanesBySha.clear()
        hasMore = true
        selectedNodeSha.value = null
    }
}
