package com.codymikol.extensions

import androidx.compose.runtime.MutableState
import com.codymikol.data.diff.FileDeltaNode
import com.codymikol.data.diff.LineNode
import com.codymikol.data.diff.LineType
import com.codymikol.data.file.FileDelta
import com.codymikol.data.file.Stash
import com.codymikol.data.file.WorkingDirectory
import com.codymikol.data.stash.StashListItem
import com.codymikol.state.GitDownState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.errors.JGitInternalException
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.dircache.DirCache
import org.eclipse.jgit.dircache.DirCacheBuildIterator
import org.eclipse.jgit.dircache.DirCacheEditor
import org.eclipse.jgit.dircache.DirCacheEntry
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.errors.LockFailedException
import org.eclipse.jgit.lib.Constants.OBJ_BLOB
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.treewalk.NameConflictTreeWalk
import org.eclipse.jgit.treewalk.TreeWalk.OperationType
import org.eclipse.jgit.treewalk.WorkingTreeIterator
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilter
import org.eclipse.jgit.treewalk.filter.PathFilterGroup
import org.eclipse.jgit.util.io.DisabledOutputStream
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path

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

fun Git.getCurrentRefCommitCount() = try {
    this.log().call().toSet().size
} catch (e: NoHeadException) {
    0
}

fun Git.getStashes(): List<RevCommit> = try {
    this.stashList().call().toList()
} catch (e: Exception) {
    emptyList()
}

fun Git.listLocalBranches(): List<Ref> = try {
    this.branchList().call()
} catch (e: Exception) {
    emptyList()
}

fun Git.getStashDiff(stash: RevCommit): List<FileDelta> = try {
    RevWalk(this.repository).use { walk ->
        val commit = walk.parseCommit(stash)

        when (commit.parentCount > 0) {
            false -> emptyList()
            true -> {
                val parent = walk.parseCommit(commit.getParent(0))

                DiffFormatter(DisabledOutputStream.INSTANCE).use { scanner ->
                    scanner.setRepository(this.repository)
                    scanner.scan(parent.tree, commit.tree).map { entry ->

                        val diffText = ByteArrayOutputStream().also { stream ->
                            DiffFormatter(stream).use { formatter ->
                                formatter.setRepository(this.repository)
                                formatter.format(entry)
                            }
                        }.toString()

                        val path = Path.of(
                            if (entry.changeType == DiffEntry.ChangeType.DELETE) entry.oldPath else entry.newPath
                        )

                        when (entry.changeType) {
                            DiffEntry.ChangeType.ADD -> Stash.FileAdded(path, diffText)
                            DiffEntry.ChangeType.DELETE -> Stash.FileDeleted(path, diffText)
                            else -> Stash.FileModified(path, diffText)
                        }
                    }
                }
            }
        }
    }
} catch (e: Exception) {
    logger.error("An exception was thrown while diffing a stash: ${e.message}")
    emptyList()
}

private const val INDEX_LOCK_RETRY_MAX_ATTEMPTS = 6
private const val INDEX_LOCK_RETRY_INITIAL_DELAY_MS = 100L
private const val INDEX_LOCK_RETRY_BACKOFF_MULTIPLIER = 2

/**
 *  jgit's add/commit commands lock .git/index while they run, which can throw
 *  a LockFailedException (wrapped in JGitInternalException) if another process
 *  is holding that lock. Retry with exponential backoff until it clears.
 */
internal fun <T> retryOnIndexLock(
    maxAttempts: Int = INDEX_LOCK_RETRY_MAX_ATTEMPTS,
    initialDelayMs: Long = INDEX_LOCK_RETRY_INITIAL_DELAY_MS,
    fn: () -> T
): T {
    var attempt = 1
    var delayMs = initialDelayMs
    while (true) {
        try {
            return fn()
        } catch (e: JGitInternalException) {
            if (e.cause !is LockFailedException || attempt >= maxAttempts) throw e
            logger.warn("Git index is locked, retrying in ${delayMs}ms (attempt $attempt/$maxAttempts)")
            Thread.sleep(delayMs)
            attempt++
            delayMs *= INDEX_LOCK_RETRY_BACKOFF_MULTIPLIER
        }
    }
}

suspend fun Git.stageAll(): Git = command {

    // setUpdate allows us to add deleted files, but disallows adding new files. So we have to do this twice...
    retryOnIndexLock {
        this@stageAll
            .add()
            .addFilepattern(".")
            .setUpdate(true)
            .call()
            .also { logger.info("Staging all files") }
            .unit()
    }

    retryOnIndexLock {
        this@stageAll
            .add()
            .addFilepattern(".")
            .call()
            .also { logger.info("Staging all files") }
            .unit()
    }

}

suspend fun Git.discardFile(fileDeltaNode: FileDeltaNode): Git = command {
    val path = fileDeltaNode.getPath()

    when (fileDeltaNode.fileDelta) {
        // untracked files aren't in the index, so there is nothing for jgit's
        // checkout command to restore them from. Deleting is the only revert.
        is WorkingDirectory.FileAdded -> File(this@discardFile.repository.workTree, path).delete()
        // path-limited checkout returns a null Ref (only branch checkouts return one),
        // so chaining .unit() here would force a non-null check and NPE on that null.
        else -> this@discardFile.checkout().addPath(path).call()
    }
        .also { logger.info("Discarding changes to $path") }
}

// Deletes a file from the working directory. Intended for new files with only
// additions, where there is no committed version for git to check out back to.
suspend fun Git.deleteFile(location: String): Git = command {
    File(this@deleteFile.repository.workTree, location)
        .delete()
        .also { logger.info("Deleting file $location") }
}

suspend fun Git.unstageLines(lines: List<LineNode>): Git = command {
    if (lines.isEmpty()) return@command

    val fileDeltaNode = lines[0].parent.parent
    require(lines.all { it.parent.parent == fileDeltaNode }) {
        "Unexpected call to unstageLines with multiple files, this currently only supports a single file!"
    }

    val repo = GitDownState.repo.value
    val path = fileDeltaNode.getPath()
    val base = readHeadFileContent(repo, path)
    val selectedSet = lines.toSet()
    val unstagedLines = buildUnstagedLines(fileDeltaNode, base.lines, selectedSet)
    val unstagedText = when {
        unstagedLines.isEmpty() -> ""
        else -> unstagedLines.joinToString("\n", postfix = "\n")
    }

    writeIndexFileContent(repo, path, unstagedText)
}

private data class FileContent(
    val lines: List<String>,
    val endsWithNewline: Boolean,
)

/**
 * Reads a path's raw blob text from the index (staged content). Shared with
 * [com.codymikol.data.file.FileDelta.getFullContent], which needs the same blob but as a whole
 * string rather than split into lines.
 */
internal fun readIndexBlobText(repo: Repository, path: String): String? {
    val entry = repo.readDirCache().getEntry(path) ?: return null
    return repo.open(entry.objectId).bytes.toString(Charsets.UTF_8)
}

/** Reads a path's raw blob text from HEAD. See [readIndexBlobText] for why this is shared. */
internal fun readHeadBlobText(repo: Repository, path: String): String? {
    val headTree = repo.resolve("HEAD^{tree}") ?: return null
    return TreeWalk(repo).use { treeWalk ->
        treeWalk.addTree(headTree)
        treeWalk.isRecursive = true
        treeWalk.filter = PathFilter.create(path)
        if (!treeWalk.next()) null
        else repo.open(treeWalk.getObjectId(0)).bytes.toString(Charsets.UTF_8)
    }
}

private fun String.toFileContent(): FileContent {
    val endsWithNewline = this.endsWith("\n")
    val lines = when {
        this.isEmpty() -> emptyList()
        endsWithNewline -> this.dropLast(1).split("\n")
        else -> this.split("\n")
    }
    return FileContent(lines, endsWithNewline)
}

private fun readIndexFileContent(repo: Repository, path: String): FileContent? =
    readIndexBlobText(repo, path)?.toFileContent()

private fun readHeadFileContent(repo: Repository, path: String): FileContent =
    readHeadBlobText(repo, path)?.toFileContent() ?: FileContent(emptyList(), false)

private fun buildPatchedLines(
    fileDeltaNode: FileDeltaNode,
    baseLines: List<String>,
    shouldIncludeChangedLine: (LineNode) -> Boolean,
): List<String> {
    val output = mutableListOf<String>()
    var currentIndex = 0

    fileDeltaNode.hunkNodes.forEach { hunkNode ->
        val header = hunkNode.hunk.header
        val fromCount = header.fromFileLineNumbersCount?.toInt()
            ?: if (header.fromFileLineNumbersStart == 0U) 0 else 1
        val startIndex = (header.fromFileLineNumbersStart.toInt() - 1).coerceAtLeast(0)
        val safeStartIndex = startIndex.coerceAtMost(baseLines.size)
        val endIndex = (safeStartIndex + fromCount).coerceAtMost(baseLines.size)

        if (safeStartIndex > currentIndex) {
            output.addAll(baseLines.subList(currentIndex, safeStartIndex))
        }

        val segment = mutableListOf<String>()
        hunkNode.lineNodes.forEach { lineNode ->
            when (lineNode.line.type) {
                LineType.Unchanged -> segment.add(lineNode.line.value)
                LineType.Removed, LineType.Added -> if (shouldIncludeChangedLine(lineNode)) segment.add(lineNode.line.value)
                LineType.NoNewline, LineType.Unknown -> Unit
            }
        }

        output.addAll(segment)
        currentIndex = endIndex
    }

    if (currentIndex < baseLines.size) {
        output.addAll(baseLines.subList(currentIndex, baseLines.size))
    }

    return output
}

// Base is the current index (pre-change): a selected Removed line is dropped
// (staged as deleted) and a selected Added line is inserted (staged as added).
private fun buildStagedLines(
    fileDeltaNode: FileDeltaNode,
    baseLines: List<String>,
    selectedLines: Set<LineNode>,
): List<String> = buildPatchedLines(fileDeltaNode, baseLines) { lineNode ->
    when (lineNode.line.type) {
        LineType.Removed -> !selectedLines.contains(lineNode)
        LineType.Added -> selectedLines.contains(lineNode)
        else -> false
    }
}

// Base is HEAD (pre-staging): a selected Removed line (a staged deletion) is
// restored, and a selected Added line (a staged addition) is dropped back out.
private fun buildUnstagedLines(
    fileDeltaNode: FileDeltaNode,
    baseLines: List<String>,
    selectedLines: Set<LineNode>,
): List<String> = buildPatchedLines(fileDeltaNode, baseLines) { lineNode ->
    when (lineNode.line.type) {
        LineType.Removed -> selectedLines.contains(lineNode)
        LineType.Added -> !selectedLines.contains(lineNode)
        else -> false
    }
}

private fun writeIndexFileContent(repo: Repository, path: String, content: String) {
    val objectInserter = repo.newObjectInserter()
    val bytes = content.toByteArray()
    val id = objectInserter.insert(OBJ_BLOB, bytes)
    objectInserter.flush()

    val dc = repo.lockDirCache()
    try {
        val existingEntry = dc.getEntry(path)
        val editor = dc.editor()
        editor.add(object : DirCacheEditor.PathEdit(path) {
            override fun apply(ent: DirCacheEntry) {
                ent.fileMode = existingEntry?.fileMode ?: FileMode.REGULAR_FILE
                ent.length = bytes.size
                ent.setObjectId(id)
            }
        })
        editor.finish()
        dc.write()
        dc.commit()
    } finally {
        dc.unlock()
    }
}

suspend fun Git.stageSelectedLines(lines: List<LineNode>): Git = command {
    if (lines.isEmpty()) return@command

    val fileDeltaNode = lines[0].parent.parent
    require(lines.all { it.parent.parent == fileDeltaNode }) {
        "Unexpected call to stageSelectedLines with multiple files, this currently only supports a single file!"
    }

    val repo = GitDownState.repo.value
    val path = fileDeltaNode.getPath()
    val base = readIndexFileContent(repo, path) ?: readHeadFileContent(repo, path)
    val selectedSet = lines.toSet()
    val stagedLines = buildStagedLines(fileDeltaNode, base.lines, selectedSet)
    val stagedText = when {
        stagedLines.isEmpty() -> ""
        base.endsWithNewline || stagedLines.isNotEmpty() -> stagedLines.joinToString("\n", postfix = "\n")
        else -> stagedLines.joinToString("\n")
    }

    writeIndexFileContent(repo, path, stagedText)
}


fun getInputStreamLength(inputStream: InputStream): Long {
    var length = 0L
    val buffer = ByteArray(1024) // You can adjust the buffer size as needed

    try {
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            length += bytesRead
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    return length
}

fun getPatchedFileContent(selectedLines: List<LineNode>): String {

    val allLines = selectedLines[0].parent.lineNodes




    val patch = selectedLines
        .filter { it.line.type == LineType.Added  }
        .joinToString(separator = "\n") { it.line.value }

    // Sad baby CLRF filter, need to fix this
    return when(patch.endsWith("\n")) {
        true -> patch
        false -> patch + "\n"
    }

}


// todo(mikol): This is a modified version the Jgit AddCommand, some things here I understand and some things
//  do not, I should make sure to really have a good understanding of everything noted before doing a GA release...
suspend fun Git.stageLinesForAddedFile(lineNodes: List<LineNode>): Git = command {

    val fileDeltaNode = lineNodes[0].parent.parent

    assert(!lineNodes.any { it.parent.parent != fileDeltaNode }) {
        "Unexpected call to stageLinesForAddedFile with multiple files, this currently only supports a single file!"
    }

    val toAddFilePatterns = fileDeltaNode.fileDelta.getPath()
    val repo = GitDownState.repo.value

    var dc: DirCache? = null

    try {

        val objectInserter = repo.newObjectInserter()
        val treeWalk = NameConflictTreeWalk(repo)
        treeWalk.isRecursive = true

        treeWalk.operationType = OperationType.CHECKIN_OP

        dc = repo.lockDirCache()

        val builder = dc.builder()

        treeWalk.addTree(DirCacheBuildIterator(builder))

        val workingTreeIterator = FileTreeIterator(repo)

        workingTreeIterator.setDirCacheIterator(treeWalk, 0)
        treeWalk.addTree(workingTreeIterator)

        val pathFilter = PathFilterGroup.createFromStrings(toAddFilePatterns)

        treeWalk.filter = pathFilter

        while(treeWalk.next()) {
            val cache = treeWalk.getTree(0, DirCacheIterator::class.java)
            val file = treeWalk.getTree(1, WorkingTreeIterator::class.java)
            val path = treeWalk.rawPath
            val entry = DirCacheEntry(path)
            val fileMode = file.getIndexFileMode(cache)
            if (fileMode == FileMode.TREE) continue
            entry.fileMode = fileMode

            entry.setLastModified(file.entryLastModifiedInstant)

            val patchedFile = getPatchedFileContent(lineNodes)

            //todo(mikol): FIXME
//            val patchedFile = "b\n"
            val byteArray = patchedFile.toByteArray()
            val inputStream = ByteArrayInputStream(byteArray)

            val length = byteArray.size

            entry.length = length

            try {
//                val inputStream = file.openEntryStream()
                val id = objectInserter.insert(OBJ_BLOB, length.toLong(), inputStream)
                entry.setObjectId(id)
            } catch (e: Exception) {
                //todo(mikol): do something less useless
                logger.error("AHHHHHHHHHHHHHHHHHHHHHH")
            }
            builder.add(entry)

        }

        objectInserter.flush()
        builder.commit()

    } catch (e: IOException) {
        println("SAD")
    } finally {
        dc?.unlock()
    }

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

suspend fun Git.saveStash(message: String, includeUntrackedFiles: Boolean) = command {
    // stashCreate().call() returns null when there is nothing to stash (e.g. only
    // untracked files exist and includeUntrackedFiles is false), mirroring plain
    // `git stash`'s no-op behavior; .unit() would NPE on that null receiver.
    //
    // setWorkingDirectoryMessage() treats its whole argument as a MessageFormat
    // pattern, so message is quoted to keep apostrophes and literal { } braces
    // from being interpreted as format syntax.
    this@saveStash
        .stashCreate()
        .setWorkingDirectoryMessage("On {0}: '${message.replace("'", "''")}'")
        .setIncludeUntracked(includeUntrackedFiles)
        .call()
        ?.also { logger.info("Saving stash") }
}

suspend fun Git.dropStash(stash: StashListItem) = command {
    // StashDropCommand.setStashRef() takes the 0-based position of the stash within
    // the reflog (stash@{0} = most recent), not a sha or ref string, so the stash's
    // position must be resolved from the current stash list first.
    this@dropStash
        .getStashes()
        .indexOfFirst { it.name == stash.sha }
        .takeIf { it >= 0 }
        ?.let { index ->
            logger.info("Dropping stash ${stash.sha}")
            // stashDrop().call() returns null when the dropped entry was the last
            // stash, so logging must happen before the call rather than chained
            // off its result.
            this@dropStash.stashDrop().setStashRef(index).call()
        }
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
