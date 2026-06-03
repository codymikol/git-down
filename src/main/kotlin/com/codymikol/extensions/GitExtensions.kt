package com.codymikol.extensions

import androidx.compose.runtime.MutableState
import com.codymikol.data.diff.FileDeltaNode
import com.codymikol.data.diff.LineNode
import com.codymikol.data.diff.LineType
import com.codymikol.data.file.WorkingDirectory
import com.codymikol.state.GitDownState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.dircache.DirCache
import org.eclipse.jgit.dircache.DirCacheBuildIterator
import org.eclipse.jgit.dircache.DirCacheEditor
import org.eclipse.jgit.dircache.DirCacheEntry
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.lib.Constants.OBJ_BLOB
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.treewalk.NameConflictTreeWalk
import org.eclipse.jgit.treewalk.TreeWalk.OperationType
import org.eclipse.jgit.treewalk.WorkingTreeIterator
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilter
import org.eclipse.jgit.treewalk.filter.PathFilterGroup
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

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

suspend fun Git.unstageLines(lines: List<LineNode>): Git = command {
    lines.forEach { println(it.line.value) }
}

private data class FileContent(
    val lines: List<String>,
    val endsWithNewline: Boolean,
)

private fun readIndexFileContent(repo: Repository, path: String): FileContent? {
    val entry = repo.readDirCache().getEntry(path) ?: return null
    val loader = repo.open(entry.objectId)
    val text = loader.bytes.toString(Charsets.UTF_8)
    val endsWithNewline = text.endsWith("\n")
    val lines = when {
        text.isEmpty() -> emptyList()
        endsWithNewline -> text.dropLast(1).split("\n")
        else -> text.split("\n")
    }
    return FileContent(lines, endsWithNewline)
}

private fun readHeadFileContent(repo: Repository, path: String): FileContent {
    val headTree = repo.resolve("HEAD^{tree}") ?: return FileContent(emptyList(), false)
    TreeWalk(repo).use { treeWalk ->
        treeWalk.addTree(headTree)
        treeWalk.isRecursive = true
        treeWalk.filter = PathFilter.create(path)
        if (!treeWalk.next()) return FileContent(emptyList(), false)
        val loader = repo.open(treeWalk.getObjectId(0))
        val text = loader.bytes.toString(Charsets.UTF_8)
        val endsWithNewline = text.endsWith("\n")
        val lines = when {
            text.isEmpty() -> emptyList()
            endsWithNewline -> text.dropLast(1).split("\n")
            else -> text.split("\n")
        }
        return FileContent(lines, endsWithNewline)
    }
}

private fun buildStagedLines(
    fileDeltaNode: FileDeltaNode,
    baseLines: List<String>,
    selectedLines: Set<LineNode>,
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

        val stagedSegment = mutableListOf<String>()
        hunkNode.lineNodes.forEach { lineNode ->
            when (lineNode.line.type) {
                LineType.Unchanged -> stagedSegment.add(lineNode.line.value)
                LineType.Removed -> if (!selectedLines.contains(lineNode)) stagedSegment.add(lineNode.line.value)
                LineType.Added -> if (selectedLines.contains(lineNode)) stagedSegment.add(lineNode.line.value)
                LineType.NoNewline, LineType.Unknown -> Unit
            }
        }

        output.addAll(stagedSegment)
        currentIndex = endIndex
    }

    if (currentIndex < baseLines.size) {
        output.addAll(baseLines.subList(currentIndex, baseLines.size))
    }

    return output
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
