package components.commit

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.file.FileDelta

@Composable
fun FileIcon(modifier: Modifier = Modifier, letter: String, color: Color) = Column(
    modifier = Modifier.wrapContentSize(Alignment.Center),
) {
    Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(2.dp)).background(color).then(modifier)) {
        Text(letter, textAlign = TextAlign.Center, color = Color.White, fontSize = 10.sp, modifier = Modifier.fillMaxSize().padding(2.dp))
    }
}

@Composable
fun FileIcon(modifier: Modifier = Modifier, fileDelta: FileDelta) = FileIcon(modifier, fileDelta.letter, fileDelta.color)

@Preview
@Composable
fun TestIcon() = FileIcon(letter = "IT", color = Color.Magenta)
