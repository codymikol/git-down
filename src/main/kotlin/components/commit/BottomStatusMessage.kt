package components.commit

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import data.Colors
import state.GitDownState

@Composable
private fun RegularText(_text: String) = Text(_text, color = Colors.LightGrayText, fontSize = 12.sp)

@Composable
private fun BoldText(_text: String) =
    Text(_text, fontWeight = FontWeight.Bold, color = Colors.LightGrayText, fontSize = 12.sp)

private fun getObjectName(): String = when (GitDownState.isDetached.value) {
    true -> "HEAD"
    false -> GitDownState.branchName.value
}

private fun getObjectNamePrefix(): String = "on " + when (GitDownState.isDetached.value) {
    true -> "detached"
    false -> "branch"
} + " "

@Composable
fun BottomStatusMessage() = Row {
    RegularText("Commiting as ")
    BoldText("${GitDownState.committingAsName.value} <${GitDownState.comittingAsEmail.value}> ")
    RegularText(getObjectNamePrefix())
    BoldText(getObjectName())
}

@Composable
@Preview
fun TestStatusMessage() {
    return BottomStatusMessage()
}
