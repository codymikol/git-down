package data.file

import androidx.compose.ui.graphics.Color
import java.nio.file.Path

object WorkingDirectory {

    class FileModified(override val location: Path) : FileDelta {
        override val letter: String get() = "M"
        override val color: Color get() = Color(78, 140, 228)
        override val type: Status get() = Status.WORKING_DIRECTORY
    }

    class FileAdded(override val location: Path, ) : FileDelta {
        override val letter: String get() = "A"
        override val color: Color get() = Color(121, 177, 90)
        override val type: Status get() = Status.WORKING_DIRECTORY
    }

    class FileDeleted(override val location: Path, ) : FileDelta {
        override val letter: String get() = "D"
        override val color: Color get() = Color(239, 116, 118)
        override val type: Status get() = Status.WORKING_DIRECTORY
    }

}