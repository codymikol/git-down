package com.codymikol.data.file

import androidx.compose.ui.graphics.Color
import com.codymikol.data.Colors
import java.nio.file.Path

object Stash {

    class FileModified(override val location: Path, private val diff: String) : FileDelta {
        override val letter: String get() = "M"
        override val color: Color get() = Colors.FileModified
        override val borderColor: Color get() = Colors.FileModifiedBorder
        override val type: Status get() = Status.STASH
        override fun getDiff(): String = diff
    }

    class FileAdded(override val location: Path, private val diff: String) : FileDelta {
        override val letter: String get() = "A"
        override val color: Color get() = Colors.FileAdded
        override val borderColor: Color get() = Colors.FileAddedBorder
        override val type: Status get() = Status.STASH
        override fun getDiff(): String = diff
    }

    class FileDeleted(override val location: Path, private val diff: String) : FileDelta {
        override val letter: String get() = "D"
        override val color: Color get() = Colors.FileRemoved
        override val borderColor: Color get() = Colors.FileRemovedBorder
        override val type: Status get() = Status.STASH
        override fun getDiff(): String = diff
    }

}
