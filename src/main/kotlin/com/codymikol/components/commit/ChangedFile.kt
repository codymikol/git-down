package com.codymikol.components.commit

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codymikol.data.file.FileDelta
import com.codymikol.data.file.WorkingDirectory
import com.codymikol.state.GitDownState
import com.codymikol.state.Keys
import java.nio.file.Path


@Composable
fun ChangedFile(fileDelta: FileDelta) {

    val color = when (GitDownState.selectedFiles.contains(fileDelta)) {
        true -> Color(0, 89, 207)
        false -> Color.Transparent
    }

    Row(Modifier
        .clickable {
            if (!Keys.isShiftPressed.value) GitDownState.selectedFiles.clear()
            GitDownState.selectedFiles.add(fileDelta)
        }
        .fillMaxWidth()
        .height(18.dp)
        .background(color), verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(12.dp))
        FileIcon(letter = fileDelta.letter, color = fileDelta.color)
        Text(
            fileDelta.location.toString().split("/").last(),
            modifier = Modifier.padding(6.dp, 0.dp, 0.dp, 0.dp),
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

@Preview
@Composable
fun DemoAddedFile() = ChangedFile(WorkingDirectory.FileDeleted(Path.of("/tmp/lol.txt")))
