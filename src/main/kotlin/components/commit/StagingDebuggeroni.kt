package components.commit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun Dabuggy(stuff: State<Set<String>>, letter: String, color: Color, name: String = "") = Column {
    Text(name, color = Color.White)
    stuff.value.forEach {
        Row {
            FileIcon(letter, color)
            Text(it.split("/").last(), softWrap = false, overflow = TextOverflow.Ellipsis, color = Color.White)
        }
    }
}
