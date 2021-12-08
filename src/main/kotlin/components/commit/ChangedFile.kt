package components.commit

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.file.FileDelta
import data.file.WorkingDirectory
import state.GitDownState
import java.nio.file.Path

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChangedFile(fileDelta: FileDelta) {
    Row(Modifier.clickable {
        GitDownState.selectedFiles.clear()
        GitDownState.selectedFiles.add(fileDelta)
    }
        .fillMaxWidth()
        .padding(2.dp)) {
        FileIcon(fileDelta.letter, fileDelta.color)
        Text(fileDelta.location.toString().split("/").last(), modifier = Modifier.padding(6.dp, 0.dp, 0.dp, 0.dp), softWrap = false, overflow = TextOverflow.Ellipsis, color = Color.White, fontSize = 12.sp)
    }
}

@Preview
@Composable
fun DemoAddedFile() = ChangedFile(WorkingDirectory.FileDeleted(Path.of("/tmp/lol.txt")))
