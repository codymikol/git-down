package extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.slf4j.LoggerFactory
import state.GitDownState

class GitExtensions

private val logger = LoggerFactory.getLogger(GitExtensions::class.java)

suspend fun Git.stageAll(): Unit = withContext(Dispatchers.IO) {

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

suspend fun Git.stageFile(location: String): Unit = withContext(Dispatchers.IO) {

    this@stageFile
        .add()
        .addFilepattern(location)
        .call()
        .also { logger.info("Staging file $location") }

}

suspend fun Git.unstageFile(location: String) = withContext(Dispatchers.IO) {

    this@unstageFile
        .reset()
        .addPath(location)
        .call()
        .also { logger.info("unstaging file $location") }

}

suspend fun Git.unstageAll() = withContext(Dispatchers.IO) {
    this@unstageAll
        .reset()
        .call()
        .also { logger.info("Unstaging all files") }
        .unit()
}

suspend fun Git.commitAll(message: String) = withContext(Dispatchers.IO) {
    this@commitAll
        .commit()
        .apply { this.message = message }
        .call()
        .also { logger.info("Committing index")}
        .unit()
}

suspend fun Git.amendAll(message: String) = withContext(Dispatchers.IO) {
    this@amendAll
        .commit()
        .setAmend(true)
        .apply { this.message = message }
        .call()
        .also { logger.info("Amending index")}
        .unit()
}
