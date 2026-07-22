package com.codymikol.data.file

import androidx.compose.ui.graphics.Color
import com.codymikol.data.diff.Line
import com.codymikol.data.diff.LineType
import com.codymikol.extensions.readHeadBlobText
import com.codymikol.extensions.readIndexBlobText
import com.codymikol.state.GitDownState
import org.eclipse.jgit.treewalk.filter.PathFilter
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path

interface FileDelta {

    companion object {
        private val logger = LoggerFactory.getLogger(FileDelta::class.java)
    }

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

    /**
     * The full available content of the side of the diff [line] belongs to (working tree/index
     * for unstaged changes, index/HEAD for staged changes), for parsing with cross-line grammar
     * context. Returns null when that content can't be read (deleted file, stash, I/O error), so
     * callers can fall back to per-line parsing.
     */
    fun getFullContent(line: Line): String? = try {
        val repo = GitDownState.repo.value
        when (this.type) {
            Status.WORKING_DIRECTORY ->
                if (line.type == LineType.Removed) readIndexBlobText(repo, getPath()) else readWorkingTreeContent()
            Status.INDEX ->
                if (line.type == LineType.Removed) readHeadBlobText(repo, getPath()) else readIndexBlobText(repo, getPath())
            Status.STASH -> null
        }
    } catch (e: Exception) {
        logger.error("Failed to read full content for ${getPath()}", e)
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