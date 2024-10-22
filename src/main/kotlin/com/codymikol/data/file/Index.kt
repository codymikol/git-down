package com.codymikol.data.file

import androidx.compose.ui.graphics.Color
import com.codymikol.data.Colors
import java.nio.file.Path

object Index {

    class FileModified(override val location: Path) : FileDelta {
        override val letter: String get() = "M"
        override val color: Color get() = Colors.FileModified
        override val borderColor: Color = Colors.FileModifiedBorder
        override val type: Status get() = Status.INDEX
    }

    class FileAdded(override val location: Path, ) : FileDelta {
        override val letter: String get() = "A"
        override val color: Color get() = Colors.FileAdded
        override val borderColor: Color = Colors.FileAddedBorder
        override val type: Status get() = Status.INDEX
    }

    class FileDeleted(override val location: Path, ) : FileDelta {
        override val letter: String get() = "D"
        override val color: Color get() = Colors.FileRemoved
        override val borderColor: Color = Colors.FileRemovedBorder
        override val type: Status get() = Status.INDEX
    }

}
