package com.codymikol.components.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.AlertDialog
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codymikol.components.SlimButton
import com.codymikol.data.Colors
import com.codymikol.typography.jetbrainsMono

fun canEditMessage(message: String): Boolean = message.isNotBlank()

// The "Edit Message..." modal (see issue #299): a text area pre-populated with the
// commit's current message, a Cancel button that dismisses, and an Accept button that
// rewrites the commit with the edited message. Only the local edit buffer lives here;
// opening/closing and the rewrite itself are driven by the parent through the
// callbacks, matching how SaveStashDialog is wired.
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EditMessageDialog(
    initialMessage: String,
    onDismiss: () -> Unit,
    onConfirm: (message: String) -> Unit,
) {
    var message by remember { mutableStateOf(initialMessage) }

    AlertDialog(
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black, RoundedCornerShape(4.dp)).padding(bottom = 8.dp),
        onDismissRequest = { onDismiss() },
        backgroundColor = Colors.LightGrayBackground,
        contentColor = Color.White,
        shape = RoundedCornerShape(4.dp),
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text(
                    "Edit Message",
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
                        .requiredHeight(120.dp)
                        .background(Colors.DarkGrayBackground)
                        .padding(8.dp)
                )
            }
        },
        dismissButton = {
            SlimButton("Cancel", onClick = { onDismiss() }, modifier = Modifier.padding(bottom = 8.dp).requiredHeight(28.dp))
        },
        confirmButton = {
            SlimButton(
                "Accept",
                disabled = !canEditMessage(message),
                onClick = { onConfirm(message) },
                modifier = Modifier.padding(bottom = 8.dp).requiredHeight(28.dp)
            )
        }
    )
}
