package com.codymikol.components.commit.diff.file.header.action.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codymikol.components.commit.diff.file.header.colors.HeaderButtonColors
import com.codymikol.data.Colors
import com.codymikol.data.diff.FileDeltaNode
import com.codymikol.data.file.Status
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("FileHeaderCogButton")

@Composable
fun FileHeaderCogButton(fileDeltaNode: FileDeltaNode) {

    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.height(24.dp).width(24.dp).padding(0.dp),
            colors = HeaderButtonColors(),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("⚙", fontSize = 10.sp)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Colors.DarkGrayBackground)
        ) {
            when (fileDeltaNode.fileDelta.type) {
                Status.WORKING_DIRECTORY -> {
                    FileActionMenuItem("View in Diff Tool", "workingDirectory.viewInDiffTool") { expanded = false }
                    Divider(color = Colors.DisabledGray)
                    FileActionMenuItem("Open File", "workingDirectory.openFile") { expanded = false }
                    FileActionMenuItem("Show In Files", "workingDirectory.showInFiles") { expanded = false }
                    Divider(color = Colors.DisabledGray)
                    FileActionMenuItem("Delete File", "workingDirectory.deleteFile") { expanded = false }
                }
                Status.INDEX -> {
                    FileActionMenuItem("View in Diff Tool", "index.viewInDiffTool") { expanded = false }
                    Divider(color = Colors.DisabledGray)
                    FileActionMenuItem("Open File", "index.openFile") { expanded = false }
                    FileActionMenuItem("Show In Files", "index.showInFiles") { expanded = false }
                }
                Status.STASH -> Unit
            }
        }
    }
}

@Composable
private fun FileActionMenuItem(label: String, actionId: String, onSelect: () -> Unit) = DropdownMenuItem(onClick = {
    logger.info("todo: $actionId")
    onSelect()
}) {
    Text(label, color = Color.White, fontSize = 12.sp)
}
