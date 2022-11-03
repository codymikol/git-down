package com.codymikol.components.commit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ConfirmDialog(title: String? = null, content: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = { onDismiss() },
        text = {
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            ) {
                if (!title.isNullOrEmpty()) {
                    Text(title,
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.subtitle1)
                }

                Text(content)
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("No")
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm() }) {
                Text("Yes")
            }
        }
    )
}