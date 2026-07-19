package com.codymikol.state

import androidx.compose.runtime.*
import com.codymikol.data.diff.DiffTree
import com.codymikol.data.file.FileDelta
import com.codymikol.data.file.Index
import com.codymikol.data.file.Status
import com.codymikol.data.file.WorkingDirectory
import com.codymikol.data.stash.StashListItem
import com.codymikol.extensions.getCurrentRefCommitCount
import com.codymikol.extensions.getStashDiff
import com.codymikol.extensions.getStashes
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
