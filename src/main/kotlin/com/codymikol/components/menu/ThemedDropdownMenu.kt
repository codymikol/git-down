package com.codymikol.components.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ThemedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) = DropdownMenu(
    expanded = expanded,
    onDismissRequest = onDismissRequest,
    modifier = modifier
        .background(MenuColors.Background)
        .border(1.dp, MenuColors.Divider),
    content = content
)

@Composable
fun ThemedDropdownMenuItem(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    DropdownMenuItem(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.background(if (isHovered) MenuColors.Highlight else MenuColors.Background)
    ) {
        Text(label, color = MenuColors.Text, fontSize = 12.sp)
    }
}

/**
 * A menu item that trails a dimmed keyboard-shortcut hint after its label, used
 * by the map view's commit context menu (see issue #253).
 */
@Composable
fun ThemedDropdownMenuItem(
    label: String,
    shortcut: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    DropdownMenuItem(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.background(if (isHovered) MenuColors.Highlight else MenuColors.Background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = MenuColors.Text, fontSize = 12.sp)
            Text(
                shortcut,
                color = MenuColors.Text.copy(alpha = 0.5f),
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 24.dp),
            )
        }
    }
}
