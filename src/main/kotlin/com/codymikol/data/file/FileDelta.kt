package com.codymikol.data.file

import androidx.compose.ui.graphics.Color
import com.codymikol.data.diff.Line
import com.codymikol.data.diff.LineType
import com.codymikol.state.GitDownState
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.filter.PathFilter
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path

interface FileDelta {

    val letter: String
    val color: Color
    val borderColor: Color
    val location: Path
    val type: Status

    fun getPath() = location.toString()

    private fun readWorkingTreeContent(): String? {
        val file = File(GitDownState.repo.value.workTree, getPath())
        return if (file.isFile) file.readText() else null
    }

    private fun readIndexContent(): String? {
        val repo = GitDownState.repo.value
        val entry = repo.readDirCache().getEntry(getPath()) ?: return null
        return repo.open(entry.objectId).bytes.toString(Charsets.UTF_8)
    }

    private fun readHeadContent(): String? {
        val repo = GitDownState.repo.value
        val headTree = repo.resolve("HEAD^{tree}") ?: return null
        return TreeWalk(repo).use { treeWalk ->
            treeWalk.addTree(headTree)
            treeWalk.isRecursive = true
            treeWalk.filter = PathFilter.create(getPath())
            if (!treeWalk.next()) null
            else repo.open(treeWalk.getObjectId(0)).bytes.toString(Charsets.UTF_8)
        }
    }

    /**
     * The full available content of the side of the diff [line] belongs to (working tree/index
     * for unstaged changes, index/HEAD for staged changes), for parsing with cross-line grammar
     * context. Returns null when that content can't be read (deleted file, stash, I/O error), so
     * callers can fall back to per-line parsing.
     */
    fun getFullContent(line: Line): String? = try {
        when (this.type) {
            Status.WORKING_DIRECTORY -> if (line.type == LineType.Removed) readIndexContent() else readWorkingTreeContent()
            Status.INDEX -> if (line.type == LineType.Removed) readHeadContent() else readIndexContent()
            Status.STASH -> null
        }
    } catch (e: Exception) {
        null
    }
    private fun loadWorkingDirectoryDiff(stream: ByteArrayOutputStream) = GitDownState
        .git
        .value
        .diff()
        .setPathFilter(PathFilter.create(this.location.toString()))
        .setOutputStream(stream)
        .call()
        .also { println("Loading diff from the working directory...") }


    private fun loadIndexDiff(stream: ByteArrayOutputStream) = GitDownState
        .git
        .value
        .diff()
        .setCached(true)
        .setPathFilter(PathFilter.create(this.location.toString()))
        .setOutputStream(stream)
        .call()
        .also { println("Loading diff from the current index...") }

    fun getDiff(): String {

        val stream = ByteArrayOutputStream()

        when(this.type) {
            Status.INDEX -> loadIndexDiff(stream)
            Status.WORKING_DIRECTORY -> loadWorkingDirectoryDiff(stream)
            Status.STASH -> Unit
        }

        return stream.toString()

    }

}