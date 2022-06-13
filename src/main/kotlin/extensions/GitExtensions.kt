package extensions

import androidx.compose.runtime.MutableState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.slf4j.LoggerFactory
import state.GitDownState

class GitExtensions

private val logger = LoggerFactory.getLogger(GitExtensions::class.java)

/**
 *   This will wrap an issued git command in a suspended IO coroutine
 *   and subsequently scan for changes so the UI can be updated
 */
private suspend fun Git.command(fn: () -> Unit) = withContext(Dispatchers.IO) {
    fn()
    scanForChanges()
}

suspend fun Git.stageAll(): Unit = command {

    // setUpdate allows us to remove deleted files, but disallows adding new files. So we have to do this twice...
    this@stageAll
        .add()
        .addFilepattern(".")
        .setUpdate(true)
        .call()
        .also { logger.info("Staging all files") }
        .unit()

    this@stageAll
        .add()
        .addFilepattern(".")
        .call()
        .also { logger.info("Staging all files") }
        .unit()

}

suspend fun Git.discardFile(location: String): Unit = command {
    TODO()
}

suspend fun Git.discardAllWorkingDirectory(): Unit = command {

    this@discardAllWorkingDirectory
        .checkout()
        .setAllPaths(true)
        .call()

    this@discardAllWorkingDirectory
        .clean()
        .setCleanDirectories(true)
        .call()
        .also { logger.info("Discarding working directory") }

}

suspend fun Git.stageFile(location: String): Unit = command {
    this@stageFile
        .add()
        .addFilepattern(location)
        .call()
        .also { logger.info("Staging file $location") }
}

suspend fun Git.unstageFile(location: String) = command {
    this@unstageFile
        .reset()
        .addPath(location)
        .call()
        .also { logger.info("unstaging file $location") }
}

suspend fun Git.unstageAll() = command {
    this@unstageAll
        .reset()
        .call()
        .also { logger.info("Unstaging all files") }
        .unit()
}

suspend fun Git.commitAll(message: String) = command {
    this@commitAll
        .commit()
        .apply { this.message = message }
        .call()
        .also { logger.info("Committing index") }
        .unit()
}

suspend fun Git.amendAll(message: String) = command {
    this@amendAll
        .commit()
        .setAmend(true)
        .apply { this.message = message }
        .call()
        .also { logger.info("Amending index") }
        .unit()
}

private fun <C> MutableState<Set<C>>.assignWhenDifferent(new: Set<C>) {
    if (!this.value.equals(new)) this.value = new
}

fun Git.scanForChanges() = try {
    val newStatus = this.status().call()

    GitDownState.removed.assignWhenDifferent(newStatus.removed)
    GitDownState.added.assignWhenDifferent(newStatus.added)
    GitDownState.missing.assignWhenDifferent(newStatus.missing)
    GitDownState.conflicting.assignWhenDifferent(newStatus.conflicting)
    GitDownState.modified.assignWhenDifferent(newStatus.modified)
    GitDownState.untracked.assignWhenDifferent(newStatus.untracked)
    GitDownState.ignoredNotInIndex.assignWhenDifferent(newStatus.ignoredNotInIndex)
    GitDownState.uncommittedChanges.assignWhenDifferent(newStatus.uncommittedChanges)

    GitDownState.selectedFiles.forEach { fileDelta ->

//            print(fileDelta.location)
        //todo(mikol): see if selectedFiles DIFFs are different than the current Diffs, if so replace them.

    }
} catch (e: Exception) {
    logger.error("An exception was thrown while updating git state: ${e.message}")
}