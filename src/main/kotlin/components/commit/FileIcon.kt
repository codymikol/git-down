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

@Composable
fun FileIcon(letter: String, color: Color) = Column(
    modifier = Modifier.wrapContentSize(Alignment.Center),
) {
    Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(2.dp)).background(color)) {
        Text(letter, textAlign = TextAlign.Center, color = Color.White, fontSize = 10.sp, modifier = Modifier.fillMaxSize().padding(2.dp))
    }
}

@Preview
@Composable
fun TestIcon() = FileIcon("IT", Color.Magenta)
