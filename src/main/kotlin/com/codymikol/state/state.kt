package com.codymikol.state

import androidx.compose.runtime.*
import com.codymikol.data.diff.DiffTree
import com.codymikol.data.file.FileDelta
import com.codymikol.data.file.Index
import com.codymikol.data.file.WorkingDirectory
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

    val isValidGitDirectory = derivedStateOf { repo.value.branch != null }

    val branchName = derivedStateOf { repo.value.branch ?: "" }

    val commitCount = derivedStateOf {
        git.value.log().call().toSet().size
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

}
