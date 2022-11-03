package com.codymikol.components.commit

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.codymikol.data.Colors

@Composable
fun ColumnScope.CommitEmptyState(text: String) = Row(
    modifier = Modifier.weight(1.0f).fillMaxWidth(),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(text = text, color = Colors.LightGrayText, fontSize = 14.sp)
}
