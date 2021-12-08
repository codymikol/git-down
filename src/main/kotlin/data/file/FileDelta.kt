package data.file

import androidx.compose.ui.graphics.Color
import java.nio.file.Path

interface FileDelta {
    val letter: String
    val color: Color
    val location: Path
    val type: Status
}