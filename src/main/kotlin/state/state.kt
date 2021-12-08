package state

import androidx.compose.runtime.*
import data.file.FileDelta
import data.file.Index
import data.file.WorkingDirectory
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import tabs.Tab
import java.io.File
import java.nio.file.Path


object GitDownState {

    val currentTab: MutableState<Tab> = mutableStateOf(Tab.Commit)

    val gitDirectory = mutableStateOf("/home/cody/dev/git-down/.git")

    val selectedFiles = mutableStateListOf<FileDelta>()

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
        println("Computing git")
        println(test.value)
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

    val status = derivedStateOf { git.value.status().call() }

    val removed = derivedStateOf { status.value.removed }

    val added = derivedStateOf { status.value.added }

    val changed = derivedStateOf { status.value.changed }

    val missing = derivedStateOf { status.value.missing }

    val conflicting = derivedStateOf { status.value.conflicting }

    val modified = derivedStateOf { status.value.modified }

    val untracked = derivedStateOf { status.value.untracked }

    val ignoredNotInIndex = derivedStateOf { status.value.ignoredNotInIndex }

    val uncommittedChanged = derivedStateOf {
        status.value.uncommittedChanges
    }

    private val uncommittedChanges = derivedStateOf { status.value.uncommittedChanges }

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

    val test = mutableStateOf(1)
}
