package com.codymikol.components.commit.diff.file.header.action.buttons

import androidx.compose.foundation.layout.*
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codymikol.components.commit.diff.file.header.colors.HeaderButtonColors
import com.codymikol.components.menu.ThemedDropdownMenu
import com.codymikol.components.menu.ThemedDropdownMenuItem
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("FileHeaderCogButton")

@Composable
internal fun FileHeaderCogButton(menuItems: @Composable (dismiss: () -> Unit) -> Unit) {

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

        ThemedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            menuItems { expanded = false }
        }
    }
}

@Composable
internal fun FileActionMenuItem(label: String, actionId: String, onSelect: () -> Unit) = ThemedDropdownMenuItem(
    label = label,
    onClick = {
        logger.info("todo: $actionId")
        onSelect()
    }
)
