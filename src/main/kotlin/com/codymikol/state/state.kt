package com.codymikol.state

import androidx.compose.runtime.*
import com.codymikol.data.diff.DiffTree
import com.codymikol.data.file.FileDelta
import com.codymikol.data.file.Index
import com.codymikol.data.file.Status
import com.codymikol.data.file.WorkingDirectory
import com.codymikol.data.map.CommitGraphNode
import com.codymikol.data.stash.StashListItem
import com.codymikol.extensions.checkoutDetachedHead
import com.codymikol.extensions.getCommitDiff
import com.codymikol.extensions.getCommitDiffAgainstHead
import com.codymikol.extensions.getCurrentRefCommitCount
import com.codymikol.extensions.getStashDiff
import com.codymikol.extensions.getStashes
import com.codymikol.extensions.rewriteCommitMessage
import com.codymikol.tabs.Tab
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.nio.file.Path


object GitDownState {

    val currentTab: MutableState<Tab> = mutableStateOf(Tab.Commit)

    val gitDirectory = mutableStateOf("")

    val selectedFiles = mutableStateListOf<FileDelta>()

    val diffTree = derivedStateOf { DiffTree.make(selectedFiles) }

    val projectName = derivedStateOf { gitDirectory.value.removeSuffix("/.git").split("/").last() }

    val repo = derivedStateOf {
        FileRepositoryBuilder()
            .setGitDir(File(gitDirectory.value))
            .readEnvironment()
            .findGitDir()
            .build()
    }

    val config = derivedStateOf { repo.value.config }

    val git = derivedStateOf {
        println(lastRequestedUpdateTimestamp.value)
        Git(repo.value)
    }

    val isValidGitDirectory = derivedStateOf {
        if (gitDirectory.value.isBlank()) {
            false
        } else {
            try {
                repo.value.branch != null
            } catch (e: Exception) {
                false
            }
        }
    }

    val isInvalidGitDirectorySelected = derivedStateOf {
        gitDirectory.value.isNotBlank() && !isValidGitDirectory.value
    }

    val branchName = derivedStateOf {
        try {
            repo.value.branch ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    val commitCount = derivedStateOf {
        git.value.getCurrentRefCommitCount()
    }

    val stashes = derivedStateOf {
        git.value.getStashes().map { StashListItem.make(repo.value, it) }
    }

    val selectedStash = mutableStateOf<StashListItem?>(null)

    // The commit the quick view (see issue #254) is launched against, or null when
    // the quick view is closed. Held here rather than read from MapState so the view
    // survives the map scrolling or resetting out from under it.
    val quickViewCommit = mutableStateOf<CommitGraphNode?>(null)

    // The commit whose message the "Edit Message..." modal (see issue #299) is open
    // for, or null when the modal is closed. The map view renders the modal while this
    // is non-null and pre-populates it from the commit's full message.
    val editMessageCommit = mutableStateOf<CommitGraphNode?>(null)

    // True when the quick view diffs its commit against HEAD (Diff with HEAD, see
    // issue #280) rather than against the commit's own first parent (the plain quick
    // view, #254). Both reuse the same ephemeral diff view; only the base tree differs.
    val quickViewAgainstHead = mutableStateOf(false)

    val quickViewDiffTree = derivedStateOf {
        val deltas = quickViewCommit.value?.let { commit ->
            if (quickViewAgainstHead.value) git.value.getCommitDiffAgainstHead(commit.sha)
            else git.value.getCommitDiff(commit.sha)
        } ?: emptyList()
        DiffTree.make(deltas)
    }

    // The path of the file highlighted in the quick view file list (see issue #278),
    // or null when nothing is explicitly selected - in which case the first file (the
    // one whose header is sticky at the top of the diff) is treated as the selection.
    val quickViewSelectedFilePath = mutableStateOf<String?>(null)

    // The tab to return to when the quick view is dismissed. Only captured on the way
    // in so reopening the quick view for a different commit keeps the original.
    private var tabBeforeQuickView: Tab = Tab.Map

    val stashDiffTree = derivedStateOf {
        DiffTree.make(selectedStash.value?.let { git.value.getStashDiff(it.revCommit) } ?: emptyList())
    }

    val committingAsName = derivedStateOf { repo.value.config.getString("user", null, "name") ?: "" }

    val comittingAsEmail = derivedStateOf { repo.value.config.getString("user", null, "email") ?: "" }

    val isDetached = derivedStateOf {
        (git.value.repository?.refDatabase?.refs?.getOrNull(0)?.isSymbolic?.not()) ?: false
    }

    val status = derivedStateOf {
        git.value.status().call()
    }

    val removed = mutableStateOf(emptySet<String>())

    val added = mutableStateOf(emptySet<String>())

    val changed = mutableStateOf(emptySet<String>())

    val missing = mutableStateOf(emptySet<String>())

    val conflicting = mutableStateOf(emptySet<String>())

    val modified = mutableStateOf(emptySet<String>())

    val untracked = mutableStateOf(emptySet<String>())

    val ignoredNotInIndex = mutableStateOf(emptySet<String>())

    val uncommittedChanges = mutableStateOf(emptySet<String>())

    val workingDirectoryFilesModified = derivedStateOf {
        modified.value.filter { uncommittedChanges.value.contains(it) }
            .map { WorkingDirectory.FileModified(Path.of(it)) }
            .toSet()
    }

    val workingDirectoryFilesAdded = derivedStateOf {
        untracked.value
            .map { WorkingDirectory.FileAdded(Path.of(it)) }
            .toSet()
    }

    val workingDirectoryFilesDeleted = derivedStateOf {
        missing.value.filter { uncommittedChanges.value.contains(it) }
            .map { WorkingDirectory.FileDeleted(Path.of(it)) }
            .toSet()
    }

    val indexFilesModified = derivedStateOf {
        uncommittedChanges.value.filter {
            !modified.value.contains(it) && !added.value.contains(it) && !missing.value.contains(it) && !removed.value.contains(it)
        }
            .map { Index.FileModified(Path.of(it)) }
            .toSet()
    }

    val indexFilesAdded = derivedStateOf {
        status.value.added
            .map { Index.FileAdded(Path.of(it)) }
            .toSet()
    }

    val indexFilesDeleted = derivedStateOf {
        status.value.removed
            .map { Index.FileDeleted(Path.of(it)) }
            .toSet()
    }

    val index: State<Set<FileDelta>> = derivedStateOf {
        indexFilesAdded.value + indexFilesModified.value + indexFilesDeleted.value
    }

    val workingDirectory: State<Set<FileDelta>> = derivedStateOf {
        workingDirectoryFilesAdded.value + workingDirectoryFilesModified.value + workingDirectoryFilesDeleted.value
    }

    val indexIsEmpty = derivedStateOf {
        indexFilesAdded.value.isEmpty()
                && indexFilesDeleted.value.isEmpty()
                && indexFilesModified.value.isEmpty()
    }

    val workingDirectoryIsEmpty = derivedStateOf {
        workingDirectoryFilesAdded.value.isEmpty()
                && workingDirectoryFilesDeleted.value.isEmpty()
                && workingDirectoryFilesModified.value.isEmpty()
    }

    //todo(mikol): this is not ideal, work out a better way to manage this...
    val lastRequestedUpdateTimestamp = mutableStateOf(System.currentTimeMillis())

    /**
     * Closes the current project and returns to the splash / project selection
     * screen (see issue #265). Clearing [gitDirectory] flips [isValidGitDirectory]
     * back to false, which App observes to swap GitDown out for DirectorySelector.
     *
     * Also resets the map: the Map view's launch effect only refreshes when the git
     * directory changes (see issue #290), so without clearing MapState here, reopening
     * the same repository would keep the stale commit graph and leak the open walker.
     */
    fun returnToProjectSelection() {
        MapState.reset()
        gitDirectory.value = ""
    }

    fun selectTab(tab: Tab) {
        currentTab.value = tab

        if (tab == Tab.Stash) {
            selectedStash.value = stashes.value.firstOrNull()
        }

        if (tab == Tab.Commit && selectedFiles.isEmpty()) {
            val firstFile = workingDirectory.value.firstOrNull() ?: index.value.firstOrNull()
            firstFile?.let { selectedFiles.add(it) }
        }
    }

    /**
     * Launches the quick view (see issue #254) against [commit], remembering the tab
     * to return to on close.
     */
    fun openQuickView(commit: CommitGraphNode) {
        if (currentTab.value != Tab.QuickView) tabBeforeQuickView = currentTab.value
        quickViewCommit.value = commit
        quickViewAgainstHead.value = false
        quickViewSelectedFilePath.value = null
        selectTab(Tab.QuickView)
    }

    /**
     * Launches the "Diff with HEAD" view (see issue #280) against [commit]: the same
     * ephemeral diff view as the quick view, but diffing [commit] against the current
     * HEAD instead of its own first parent.
     */
    fun openDiffWithHead(commit: CommitGraphNode) {
        if (currentTab.value != Tab.QuickView) tabBeforeQuickView = currentTab.value
        quickViewCommit.value = commit
        quickViewAgainstHead.value = true
        quickViewSelectedFilePath.value = null
        selectTab(Tab.QuickView)
    }

    /**
     * Checks [commit] out as a detached HEAD (see issue #279): the working tree is
     * moved to that commit without creating or moving any branch. Backs the map
     * view's "Checkout Detached HEAD" action.
     */
    suspend fun checkoutDetachedHead(commit: CommitGraphNode) {
        git.value.checkoutDetachedHead(commit.sha)
    }

    /** Opens the "Edit Message..." modal (see issue #299) for [commit]. */
    fun openEditMessage(commit: CommitGraphNode) {
        editMessageCommit.value = commit
    }

    /** Dismisses the "Edit Message..." modal (see issue #299) without any change. */
    fun closeEditMessage() {
        editMessageCommit.value = null
    }

    /**
     * Rewrites [commit]'s message to [newMessage] (see issue #299), replaying it and
     * its descendants so the change lands in history, then closes the modal. Backs the
     * modal's Accept button.
     */
    suspend fun editCommitMessage(commit: CommitGraphNode, newMessage: String) {
        git.value.rewriteCommitMessage(commit.sha, newMessage)
        editMessageCommit.value = null
    }

    /** Closes the quick view, clearing its commit and restoring the prior tab. */
    fun closeQuickView() {
        quickViewCommit.value = null
        quickViewAgainstHead.value = false
        quickViewSelectedFilePath.value = null
        selectTab(tabBeforeQuickView)
    }

    /** Highlights [path] in the quick view file list (see issue #278). */
    fun selectQuickViewFile(path: String) {
        quickViewSelectedFilePath.value = path
    }

    /**
     * Moves the quick view file selection by [offset] (see issue #278), clamped to the
     * commit's file list. With nothing selected the first file - the one sticky at the
     * top of the diff - is treated as the current selection.
     */
    fun selectAdjacentQuickViewFile(offset: Int) {
        val paths = quickViewDiffTree.value.fileDeltaNodes.map { it.getPath() }
        if (paths.isEmpty()) return

        val currentIndex = paths.indexOf(quickViewSelectedFilePath.value).coerceAtLeast(0)
        val nextIndex = (currentIndex + offset).coerceIn(paths.indices)

        quickViewSelectedFilePath.value = paths[nextIndex]
    }

    fun selectAdjacentFile(offset: Int) {
        val current = selectedFiles.singleOrNull() ?: return

        val siblings = when (current.type) {
            Status.WORKING_DIRECTORY -> workingDirectory.value
            Status.INDEX -> index.value
            Status.STASH -> emptySet<FileDelta>()
        }.toList()

        val currentIndex = siblings.indexOf(current)
        if (currentIndex < 0) return

        val nextIndex = (currentIndex + offset).coerceIn(siblings.indices)

        selectedFiles.clear()
        selectedFiles.add(siblings[nextIndex])
    }

}
