package com.codymikol.data.file

import androidx.compose.ui.graphics.Color
import java.nio.file.Path
import com.codymikol.data.Colors

object WorkingDirectory {

    class FileModified(override val location: Path) : FileDelta {
        override val letter: String get() = "M"
        override val color: Color get() = Colors.FileModified
        override val borderColor: Color get() = Colors.FileModifiedBorder
        override val type: Status get() = Status.WORKING_DIRECTORY
    }

    class FileAdded(override val location: Path, ) : FileDelta {
        override val letter: String get() = "A"
        override val color: Color get() = Colors.FileAdded
        override val borderColor: Color get() = Colors.FileAddedBorder
        override val type: Status get() = Status.WORKING_DIRECTORY
    }

    class FileDeleted(override val location: Path, ) : FileDelta {
        override val letter: String get() = "D"
        override val color: Color get() = Colors.FileRemoved
        override val borderColor: Color get() = Colors.FileRemovedBorder
        override val type: Status get() = Status.WORKING_DIRECTORY
    }

}
