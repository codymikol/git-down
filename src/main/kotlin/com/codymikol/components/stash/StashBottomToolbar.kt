package com.codymikol.components.stash

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codymikol.components.SlimButton
import com.codymikol.components.commit.ConfirmDialog
import com.codymikol.data.Colors
import com.codymikol.services.StashService
import com.codymikol.state.GitDownState
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

internal val stashService: StashService by inject(StashService::class.java)

fun dropStashConfirmationMessage(description: String): String =
    "This will permanently delete the stash \"$description\", are you sure?"

fun applyStashConfirmationMessage(description: String): String =
    "This will apply the stash \"$description\" to your working directory, are you sure?"

@Composable
@Preview
fun StashBottomToolbar() {
    val scope = rememberCoroutineScope()
    var isSavingStash by remember { mutableStateOf(false) }
    var isConfirmingDrop by remember { mutableStateOf(false) }
    var isConfirmingApply by remember { mutableStateOf(false) }

    val selectedStash = GitDownState.selectedStash.value

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(48.dp)
            .background(Colors.MediumGrayBackground)
            .padding(horizontal = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SlimButton("+", modifier = Modifier.padding(end = 8.dp), onClick = { isSavingStash = true })
            SlimButton("-", disabled = selectedStash == null, onClick = { isConfirmingDrop = true })
        }

        SlimButton("Apply", disabled = selectedStash == null, onClick = { isConfirmingApply = true })
    }

    if (isSavingStash) {
        SaveStashDialog(
            onDismiss = { isSavingStash = false },
            onConfirm = { message, includeUntrackedFiles ->
                scope.launch {
                    try {
                        stashService.saveStash(message, includeUntrackedFiles)
                    } finally {
                        isSavingStash = false
                    }
                }
            }
        )
    }

    if (isConfirmingDrop && selectedStash != null) {
        ConfirmDialog(
            title = "Drop Stash?",
            content = dropStashConfirmationMessage(selectedStash.description),
            onDismiss = { isConfirmingDrop = false },
            onConfirm = {
                scope.launch {
                    try {
                        stashService.dropStash(selectedStash)
                    } finally {
                        isConfirmingDrop = false
                    }
                }
            }
        )
    }

    if (isConfirmingApply && selectedStash != null) {
        ConfirmDialog(
            title = "Apply Stash?",
            content = applyStashConfirmationMessage(selectedStash.description),
            onDismiss = { isConfirmingApply = false },
            onConfirm = {
                scope.launch {
                    try {
                        stashService.applyStash(selectedStash)
                    } finally {
                        isConfirmingApply = false
                    }
                }
            }
        )
    }
}
