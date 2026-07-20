package com.codymikol.components.commit

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codymikol.components.SlimButton
import com.codymikol.data.Colors
import com.codymikol.typography.jetbrainsMono

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConfirmDialog(title: String? = null, content: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black, RoundedCornerShape(4.dp)).padding(bottom = 8.dp),
        onDismissRequest = { onDismiss() },
        backgroundColor = Colors.LightGrayBackground,
        contentColor = Color.White,
        shape = RoundedCornerShape(4.dp),
        text = {
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            ) {
                if (!title.isNullOrEmpty()) {
                    Text(title,
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color.White,
                        fontFamily = jetbrainsMono(),
                        style = MaterialTheme.typography.subtitle1)
                }

                Text(content, color = Colors.LightGrayText, fontFamily = jetbrainsMono(), fontSize = 12.sp)
            }
        },
        dismissButton = {
//            Surface(shape = MaterialTheme.shapes.medium) {
                SlimButton("No", onClick = { onDismiss() }, modifier = Modifier.padding(bottom = 8.dp).requiredHeight(28.dp))
//            }
        },
        confirmButton = {
            SlimButton("Yes", onClick = { onConfirm() }, modifier = Modifier.padding(bottom = 8.dp).requiredHeight(28.dp))
        }
    )
}
