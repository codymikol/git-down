package state

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import tabs.Tab
import java.io.File

object GitDownState {

    val currentTab: MutableState<Tab> = mutableStateOf(Tab.Commit)

    val gitDirectory = mutableStateOf("/home/cody/dev/git-down/.git")

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

    val commitCount = derivedStateOf { repo.value.refDatabase.refs.size }

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
        modified.value.filter { uncommittedChanges.value.contains(it) }.toSet()
    }

    val workingDirectoryFilesAdded = derivedStateOf {
        untracked.value.toSet()
    }

    val workingDirectoryFilesDeleted = derivedStateOf {
        missing.value.filter { uncommittedChanges.value.contains(it) }.toSet()
    }

    val indexFilesModified = derivedStateOf {
        uncommittedChanges.value.filter {
            !modified.value.contains(it) && !added.value.contains(it) && !missing.value.contains(it) && !removed.value.contains(it)
        }.toSet()
    }

    val indexFilesAdded = derivedStateOf {
        status.value.added
    }

    val indexFilesDeleted = derivedStateOf {
        status.value.removed
    }

    val indexHasChanges = derivedStateOf {
        true
    }

    val test = mutableStateOf(1)
}
