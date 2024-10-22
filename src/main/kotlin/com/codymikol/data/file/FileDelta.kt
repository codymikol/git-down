package com.codymikol.data.file

import androidx.compose.ui.graphics.Color
import com.codymikol.state.GitDownState
import org.eclipse.jgit.treewalk.filter.PathFilter
import java.io.ByteArrayOutputStream
import java.nio.file.Path

interface FileDelta {

    val letter: String
    val color: Color
    val borderColor: Color
    val location: Path
    val type: Status

    fun getPath() = location.toString()
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
        }

        return stream.toString()

    }

}