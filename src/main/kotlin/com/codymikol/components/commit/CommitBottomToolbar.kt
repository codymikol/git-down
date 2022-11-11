package com.codymikol.components.commit

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codymikol.components.SlimButton
import com.codymikol.data.Colors
import com.codymikol.extensions.amendAll
import com.codymikol.extensions.commitAll
import com.codymikol.extensions.unstageAll
import kotlinx.coroutines.launch
import com.codymikol.state.GitDownState

@Composable
@Preview
fun CommitBottomToolbarDemo() {
    CommitBottomToolbar(mutableStateOf("Hello!"))
}

@Composable
fun CommitBottomToolbar(commitMessage: MutableState<String>) {

    val amendEnabled = remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun toggleAmendHead() {
        if(amendEnabled.value) {
           amendEnabled.value = false
        } else {
            val latestCommit = GitDownState.git.value.log().setMaxCount(1).call().iterator().next()
            commitMessage.value = latestCommit.fullMessage
            amendEnabled.value = true
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.background(color = Colors.LightGrayBackground).fillMaxWidth().requiredHeight(48.dp)
            .padding(10.dp)
    ) {
        SlimButton("Unstage All") {
            scope.launch {
                GitDownState.git.value.unstageAll()
                GitDownState.selectedFiles.clear()
            }
        }
        BottomStatusMessage()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(modifier = Modifier, checked = amendEnabled.value, onCheckedChange = { toggleAmendHead() })
            Text(modifier = Modifier.padding(0.dp, 0.dp, 12.dp, 0.dp), text = "Amend Head", fontSize = 12.sp, color = Color.White)
            SlimButton("Commit", disabled = GitDownState.indexIsEmpty.value && !amendEnabled.value) {
                scope.launch {

                    when(amendEnabled.value) {
                        true -> {
                            GitDownState.git.value.amendAll(commitMessage.value)
                            amendEnabled.value = false
                        }
                        false -> {
                            GitDownState.git.value.commitAll(commitMessage.value)
                        }
                    }

                    GitDownState.selectedFiles.clear()
                    commitMessage.value = ""

                }
            }
        }
    }

}