package state

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import services.GitFactory
import tabs.Tab

object GitDownState {

    val builder = FileRepositoryBuilder()

    val currentTab: MutableState<Tab> = mutableStateOf(Tab.Commit)

    val gitDirectory = mutableStateOf("/Users/codymikol/dev/src/bolts-api/.git")

    val projectName = derivedStateOf { gitDirectory.value.removeSuffix("/.git").split("/").last() }

    val repo = derivedStateOf { GitFactory.makeRepository(gitDirectory.value) }

    val config = derivedStateOf { repo.value.config }

    val refDatabase = derivedStateOf { repo.value.refDatabase }

    val git = derivedStateOf {
        println("Computing git")
        println(test.value)
        GitFactory.makeGit(repo.value)
    }

    val isValidGitDirectory = derivedStateOf { repo.value.branch != null }

    val branchName = derivedStateOf { repo.value.branch ?: "" }

    val commitCount = derivedStateOf { repo.value.refDatabase.refs.size }

    val committingAsName = derivedStateOf { repo.value.config.getString("user", null, "name") ?: "" }

    val comittingAsEmail = derivedStateOf { repo.value.config.getString("user", null, "email") ?: "" }

    val remote = derivedStateOf { repo.value.config.getString("remote", "origin", "url") ?: "" }

    val isRebasing = derivedStateOf { repo.value.repositoryState.isRebasing }

    val isDetached = derivedStateOf {
        (git.value.repository?.refDatabase?.refs?.getOrNull(0)?.isSymbolic?.not()) ?: false
    }



    val status = derivedStateOf { git.value.status().call() }

    val removed = derivedStateOf { status.value.removed }

    val added = derivedStateOf { status.value.added }

    val changed = derivedStateOf { status.value.changed }

//    val ignoredNotInIndex = derivedStateOf { status.value.ignoredNotInIndex }

    val missing = derivedStateOf { status.value.missing }

    val conflicting = derivedStateOf { status.value.conflicting }

    val uncomittedChanges = derivedStateOf { status.value.uncommittedChanges }

    val modified = derivedStateOf { status.value.modified }

    val untracked = derivedStateOf { status.value.untracked }

    // Correct!
    val indexFilesAdded = derivedStateOf {
        status.value.added
    }

    // ???
    val uncommittedChanged = derivedStateOf {
        status.value.uncommittedChanges
    }

    val uncommittedChanges = derivedStateOf { status.value.uncommittedChanges }

    val workingDirectoryFilesModified = derivedStateOf {
        modified.value.filter { uncommittedChanges.value.contains(it) }.toSet()
    }

    val workingDirectoryFilesDeleted = derivedStateOf {

    }

    val indexFilesModified = derivedStateOf {
        uncommittedChanges.value.filter { !modified.value.contains(it) }.toSet()
    }

    val indexHasChanges = derivedStateOf {
        true
    }

    val workingDirectoryHasChanges = derivedStateOf {
        true
    }

    val test = mutableStateOf(1)
}
