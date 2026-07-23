package com.codymikol.state

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.codymikol.data.map.CommitGraphNode
import com.codymikol.data.map.CommitHistoryWalker
import com.codymikol.extensions.listLocalBranches
import org.eclipse.jgit.lib.Ref

/**
 * Backs the Map view. Each branch's full commit history is walked and loaded in one
 * call to load() - there is no lazy/paged loading of rows, since it exists as a simple
 * scroll-able container holding every graph.
 */
object MapState {

    val commitsByBranch = mutableStateMapOf<String, MutableList<CommitGraphNode>>()

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
    val maxRowCount = derivedStateOf {
        commitsByBranch.values.maxOfOrNull { it.size } ?: 0
    }

    fun load(branch: Ref) {
        if (commitsByBranch.containsKey(branch.name)) return

        CommitHistoryWalker(GitDownState.git.value, branch).use { walker ->
            commitsByBranch[branch.name] = mutableStateListOf<CommitGraphNode>().apply { addAll(walker.loadAll()) }
        }
    }

    fun reset() {
        commitsByBranch.clear()
    }
}
