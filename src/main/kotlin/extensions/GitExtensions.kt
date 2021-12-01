package extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.slf4j.LoggerFactory
import state.GitDownState

class GitExtensions

private val logger = LoggerFactory.getLogger(GitExtensions::class.java)

suspend fun Git.stageAll(): Unit = withContext(Dispatchers.IO) {
    this@stageAll
        .add()
        .addFilepattern(".")
        .call()
        .also { logger.info("Staging all files") }
        .unit()
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
