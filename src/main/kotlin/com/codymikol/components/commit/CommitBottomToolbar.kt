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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codymikol.components.SlimButton
import com.codymikol.data.Colors
import com.codymikol.extensions.amendAll
import com.codymikol.extensions.commitAll
import com.codymikol.extensions.unstageAll
import com.codymikol.state.GitDownState
import kotlinx.coroutines.launch

@Composable
private fun RegularText(_text: String, modifier: Modifier = Modifier) = Text(
    _text,
    color = Colors.LightGrayText,
    fontSize = 12.sp,
    modifier = modifier,
)

@Composable
fun BoldText(_text: String, modifier: Modifier = Modifier) = Text(
    _text,
    fontWeight = FontWeight.Bold,
    color = Colors.LightGrayText,
    fontSize = 12.sp,
    overflow = TextOverflow.Ellipsis,
    modifier = modifier,
)


fun getObjectName(isDetatchedHead: Boolean, branchName: String): String = when (isDetatchedHead) {
    true -> "HEAD"
    false -> branchName
}

fun getObjectNamePrefix(isDetatchedHead: Boolean): String = "on " + when (isDetatchedHead) {
    true -> "detached"
    false -> "branch"
} + " "

@Composable
@Preview
fun CommitBottomToolbar(
    commitMessage: MutableState<String>,
    committingAsName: String = GitDownState.committingAsName.value,
    committingAsEmail: String = GitDownState.comittingAsEmail.value,
    isDetachedHead: Boolean = GitDownState.isDetached.value,
    branchName: String = GitDownState.branchName.value,
    indexIsEmpty: Boolean = GitDownState.indexIsEmpty.value,
) {

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

    BoxWithConstraints() {

    val constraints = this

    val unstageWidth = 100.dp
    val commitWidth = 100.dp
    val amendCheckboxWidth = 50.dp
    val amendTextWidth = 80.dp
    val committingAsWidth = 90.dp

        val totalFixedWidth = unstageWidth + commitWidth + amendTextWidth + amendCheckboxWidth + committingAsWidth

        val showComittingAs = constraints.maxWidth > totalFixedWidth

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.background(color = Colors.LightGrayBackground).fillMaxWidth().requiredHeight(48.dp)
            .padding(10.dp)
    ) {
        SlimButton("Unstage All", onClick = {
            scope.launch {
                GitDownState.git.value.unstageAll()
                GitDownState.selectedFiles.clear()
            }
        })


        if(showComittingAs) RegularText("Commiting as ", modifier = Modifier.requiredWidth(committingAsWidth))
        BoldText("$committingAsName <$committingAsEmail> ")
        RegularText(getObjectNamePrefix(isDetachedHead))
        BoldText(getObjectName(isDetachedHead, branchName), modifier = Modifier.widthIn(max = 100.dp, min = 100.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                modifier = Modifier.requiredWidth(50.dp),
                checked = amendEnabled.value,
                onCheckedChange = { toggleAmendHead() })
            Text(
                modifier = Modifier.padding(0.dp, 0.dp, 12.dp, 0.dp).requiredWidth(80.dp),
                text = "Amend Head",
                fontSize = 12.sp,
                color = Color.White,
                overflow = TextOverflow.Ellipsis,
            )
            SlimButton(
                "Commit",
                modifier = Modifier.requiredWidth(100.dp),
                disabled = indexIsEmpty && !amendEnabled.value,
                onClick = {
                    scope.launch {

                        when (amendEnabled.value) {
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
                })
        }
    }
    }

}

@Composable
@Preview
fun previewCommitBottomToolbar() = CommitBottomToolbar(
    commitMessage = mutableStateOf(""),
    committingAsName = "Cody Mikol",
    committingAsEmail = "codymikol@gmail.com",
    isDetachedHead = false,
    branchName = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    indexIsEmpty = false,
)
