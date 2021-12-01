package components.commit

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FileIcon(letter: String, color: Color) = Box(
    modifier = Modifier.size(18.dp)
        .background(color = color)
        .clip(RoundedCornerShape(100.dp)),
    contentAlignment = Alignment.Center
) {
    Text(letter, color = Color.White)
}

@Preview
@Composable
fun TestIcon() = FileIcon("IT", Color.Magenta)
