package com.codymikol.extensions

import androidx.compose.runtime.MutableState
import com.codymikol.data.diff.FileDeltaNode
import com.codymikol.data.file.WorkingDirectory
import com.codymikol.state.GitDownState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.slf4j.LoggerFactory

class GitExtensions

private val logger = LoggerFactory.getLogger(GitExtensions::class.java)

/**
 *   This will wrap an issued git command in a suspended IO coroutine
 *   and subsequently scan for changes so the UI can be updated
 */
private suspend fun Git.command(fn: () -> Unit) = withContext(Dispatchers.IO) {
    fn()
    GitDownState.lastRequestedUpdateTimestamp.value = System.currentTimeMillis()
    scanForChanges()
}.let { this }

suspend fun Git.stageAll(): Git = command {

    // setUpdate allows us to add deleted files, but disallows adding new files. So we have to do this twice...
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

suspend fun Git.discardFile(location: String): Git = command {
    TODO()
}

suspend fun Git.unstageLines(): Git = command {

//    val repoState = repository.repositoryState
//
//    val dc = repository.lockDirCache()
//
//    try {
//        val walk = TreeWalk(repository)
//
//        if(commitTree)
//
//    } finally {
//        dc.unlock()
//    }

}

suspend fun Git.stageLines(): Git = command {
    TODO()
}

suspend fun Git.discardAllWorkingDirectory(): Git = command {

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

suspend fun Git.stageFile(fileDeltaNode: FileDeltaNode): Git = command {

    val shouldSetUpdate = fileDeltaNode.fileDelta is WorkingDirectory.FileDeleted

    this@stageFile
        .add()
        .addFilepattern(fileDeltaNode.getPath())
        .setUpdate(shouldSetUpdate)
        .call()
        .also { logger.info("Staging file ${fileDeltaNode.getPath()}") }
}

suspend fun Git.unstageFile(fileDeltaNode: FileDeltaNode) = command {
    this@unstageFile
        .reset()
        .addPath(fileDeltaNode.getPath())
        .call()
        .also { logger.info("unstaging file ${fileDeltaNode.getPath()}") }
}

suspend fun Git.unstageAll() = command {
    this@unstageAll
        .reset()
        .setMode(ResetCommand.ResetType.MIXED)
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