package com.codymikol.components.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Must match Material's default MenuDefaults.shape radius, or the
// underlying DropdownMenu Card's corners will still show past this clip.
private val CornerRadius = 4.dp

internal fun themedDropdownMenuModifier(base: Modifier): Modifier = base
    .clip(RoundedCornerShape(CornerRadius))
    .background(MenuColors.Background)
    .border(1.dp, MenuColors.Divider, RoundedCornerShape(CornerRadius))

@Composable
fun ThemedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) = DropdownMenu(
    expanded = expanded,
    onDismissRequest = onDismissRequest,
    modifier = themedDropdownMenuModifier(modifier),
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
