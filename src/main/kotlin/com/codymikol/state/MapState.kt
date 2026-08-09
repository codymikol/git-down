package com.codymikol.state

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.codymikol.data.map.CommitGraphNode
import com.codymikol.data.map.CommitHistoryWalker
import com.codymikol.extensions.listLocalBranches
import com.codymikol.services.BranchTipOrderer
import com.codymikol.services.LanePacker
import com.codymikol.services.LaneAssigner
import kotlin.math.abs

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
    private var laneAssigner: LaneAssigner? = null

    // The git directory whose history is currently loaded, or null before anything has
    // loaded. Lets resetForDirectory() tell a genuine project switch (which must clear
    // the map) apart from merely re-entering the Map tab (which must not), so the
    // selected node survives a round-trip through the quick view (see issue #290).
    private var loadedDirectory: String? = null

    val commits = mutableStateListOf<CommitGraphNode>()

    val lanesBySha = mutableStateMapOf<String, Int>()

    /**
     * Each loaded commit's packed row within its lane (see issue #305): a lane's own
     * commits stack down consecutive rows from the top rather than sitting at their
     * global row in the merged walk, so every lane reads tight to the top of the map.
     * Recomputed from lanesBySha by [LanePacker] each time a page loads.
     */
    val rowBySha = mutableStateMapOf<String, Int>()

    var hasMore = true

    /**
     * How many rows the packed map occupies (see issue #305): the length of the
     * longest lane, since every lane packs tight to the top. This - not the raw
     * loaded-commit count - is the map's vertical extent, so paging and scrolling
     * measure against it. Falls back to the commit count before any lane has been
     * packed (e.g. in tests that seed commits without rows).
     */
    val rowCount: Int
        get() = rowBySha.values.maxOrNull()?.plus(1) ?: commits.size

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
     * Moves the map selection one step across the commit grid (see issue #295), so the
     * whole map is reachable from the keyboard.
     *
     * [rowOffset] walks the ordered commit list up (-1) or down (+1), clamped to the
     * loaded window - a vertical step follows the commit order regardless of lane.
     * [laneOffset] steps to the adjacent lane (-1 left, +1 right) by selecting the
     * commit in that lane nearest the current row, preferring the earlier (upward) row
     * on a tie, and does nothing when that lane holds no loaded commit - lanes are
     * sparse, so not every lane exists at every row.
     *
     * With nothing selected (or the selected node scrolled out of the loaded window) the
     * first loaded commit becomes the selection, giving the arrow keys a starting point.
     */
    fun selectAdjacentNode(rowOffset: Int, laneOffset: Int) {
        if (commits.isEmpty()) return

        val currentIndex = commits.indexOfFirst { it.sha == selectedNodeSha.value }
        if (currentIndex < 0) {
            selectedNodeSha.value = commits.first().sha
            return
        }

        if (rowOffset != 0) {
            val nextIndex = (currentIndex + rowOffset).coerceIn(commits.indices)
            selectedNodeSha.value = commits[nextIndex].sha
            return
        }

        if (laneOffset != 0) {
            val currentLane = lanesBySha[selectedNodeSha.value] ?: return
            val targetLane = currentLane + laneOffset

            val nearest = commits.withIndex()
                .filter { lanesBySha[it.value.sha] == targetLane }
                .minByOrNull { abs(it.index - currentIndex) }
                ?: return

            selectedNodeSha.value = nearest.value.sha
        }
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
        // Reserve one lane per ordered branch tip (default first), so every branch
        // keeps its own column even when one tip is an ancestor of another (#315).
        // Built lazily alongside the walker so orderedBranchTips has resolved.
        val assigner = laneAssigner ?: LaneAssigner(tipLanes()).also { laneAssigner = it }
        val page = walker.nextPage(PAGE_SIZE)

        page.forEach { lanesBySha[it.sha] = assigner.assign(it) }
        commits.addAll(page)

        // Repack from the full loaded list so a page's commits stack onto the rows
        // their lanes already reached, keeping each lane tight to the top (#305).
        rowBySha.clear()
        rowBySha.putAll(LanePacker.pack(commits, lanesBySha))

        hasMore = walker.hasMore
    }

    /**
     * Each ordered branch tip's reserved lane, keyed by its commit sha (#315): the
     * tip's index in [orderedBranchTips] is its column, so lane 0 is the default
     * branch and each side branch follows left-to-right. Feeds [LaneAssigner] so a
     * tip never collapses onto a descendant tip's lane.
     */
    private fun tipLanes(): Map<String, Int> =
        orderedBranchTips.value.withIndex().associate { (index, tip) -> tip.sha to index }

    /**
     * lastVisibleIndex must be the last (bottom-most) visible row, not the first -
     * the first visible index stays well short of rowCount whenever more than
     * LOAD_MORE_THRESHOLD rows fit in the viewport, so it would never trigger paging.
     * Both it and [rowCount] are in packed-row space (see issue #305).
     */
    fun shouldLoadMore(lastVisibleIndex: Int): Boolean {
        if (!hasMore) return false
        return lastVisibleIndex >= rowCount - LOAD_MORE_THRESHOLD
    }

    fun reset() {
        walker?.close()
        walker = null
        laneAssigner = null
        commits.clear()
        lanesBySha.clear()
        rowBySha.clear()
        hasMore = true
        selectedNodeSha.value = null
        loadedDirectory = null
    }

    /**
     * Resets the map only when [directory] differs from the one already loaded (see
     * issue #290). Re-entering the Map tab - for instance on returning from the quick
     * view - re-runs the Map view's launch effect with the same directory, and must
     * leave the loaded commits and the selected node untouched; only a real project
     * switch clears them.
     */
    fun resetForDirectory(directory: String) {
        if (directory == loadedDirectory) return
        reset()
        loadedDirectory = directory
    }
}
