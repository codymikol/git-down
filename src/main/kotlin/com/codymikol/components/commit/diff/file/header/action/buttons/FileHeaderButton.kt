package com.codymikol.components.commit.diff.file.header.action.buttons

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codymikol.components.commit.diff.file.header.colors.HeaderButtonColors
import kotlinx.coroutines.launch

@Composable
fun FileHeaderButton(text: String, action: suspend () -> Unit) {

    val scope = rememberCoroutineScope()

    OutlinedButton(
        onClick = {
            scope.launch { action() }
        },
        modifier = Modifier.height(24.dp).padding(0.dp),
        colors = HeaderButtonColors(),
        contentPadding = PaddingValues(12.dp, 0.dp)
    ) {
        Text(text, fontSize = 9.sp)
    }

}