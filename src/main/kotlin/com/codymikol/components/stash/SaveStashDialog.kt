package com.codymikol.components.stash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.AlertDialog
import androidx.compose.material.Checkbox
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codymikol.components.SlimButton
import com.codymikol.data.Colors
import com.codymikol.typography.jetbrainsMono

fun canSaveStash(message: String): Boolean = message.isNotBlank()

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SaveStashDialog(onDismiss: () -> Unit, onConfirm: (message: String, includeUntrackedFiles: Boolean) -> Unit) {
    var message by remember { mutableStateOf("") }
    var includeUntrackedFiles by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black, RoundedCornerShape(4.dp)).padding(bottom = 8.dp),
        onDismissRequest = { onDismiss() },
        backgroundColor = Colors.LightGrayBackground,
        contentColor = Color.White,
        shape = RoundedCornerShape(4.dp),
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text(
                    "New Stash",
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.White,
                    fontFamily = jetbrainsMono(),
                    style = MaterialTheme.typography.subtitle1
                )

                BasicTextField(
                    value = message,
                    onValueChange = { message = it },
                    cursorBrush = Brush.verticalGradient(0.00f to Color.White),
                    textStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontFamily = jetbrainsMono()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Colors.DarkGrayBackground)
                        .padding(8.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Checkbox(checked = includeUntrackedFiles, onCheckedChange = { includeUntrackedFiles = it })
                    Text(
                        "Include untracked files",
                        color = Colors.LightGrayText,
                        fontFamily = jetbrainsMono(),
                        fontSize = 12.sp
                    )
                }
            }
        },
        dismissButton = {
            SlimButton("Cancel", onClick = { onDismiss() }, modifier = Modifier.padding(bottom = 8.dp).requiredHeight(28.dp))
        },
        confirmButton = {
            SlimButton(
                "Save Stash",
                disabled = !canSaveStash(message),
                onClick = { onConfirm(message, includeUntrackedFiles) },
                modifier = Modifier.padding(bottom = 8.dp).requiredHeight(28.dp)
            )
        }
    )
}
