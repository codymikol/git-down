package components.commit

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.file.FileDelta
import data.file.WorkingDirectory
import java.nio.file.Path
import javax.swing.DefaultButtonModel

@Composable
fun ChangedFiles(stuff: State<Set<FileDelta>>, letter: String, color: Color, name: String = "") = Column(modifier = Modifier.padding(8.dp)) {
    stuff.value.forEach { ChangedFile(it) }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChangedFile(fileDelta: FileDelta) {
    Row(Modifier.clickable { println(fileDelta.location) }.fillMaxWidth().padding(2.dp)) {
        FileIcon(fileDelta.letter, fileDelta.color)
        Text(fileDelta.location.toString().split("/").last(), modifier = Modifier.padding(6.dp, 0.dp, 0.dp, 0.dp), softWrap = false, overflow = TextOverflow.Ellipsis, color = Color.White)
    }
}

@Preview
@Composable
fun DemoAddedFile() = ChangedFile(WorkingDirectory.FileDeleted(Path.of("/tmp/lol.txt")))
