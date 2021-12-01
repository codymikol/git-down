@file:OptIn(DelicateCoroutinesApi::class)

package components.commit

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.SlimButton
import data.Colors
import extensions.unstageAll
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import state.GitDownState

@Composable
@Preview
fun CommitBottomToolbar() {

    val scope = rememberCoroutineScope()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.background(color = Colors.LightGrayBackground).fillMaxWidth().requiredHeight(48.dp)
            .padding(10.dp)
    ) {
        SlimButton("Unstage All") {
            scope.launch { GitDownState.git.value.unstageAll() }
        }
        BottomStatusMessage()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(modifier = Modifier, checked = false, onCheckedChange = {})
            Text(modifier = Modifier.padding(12.dp, 0.dp), text = "Amend Head", fontSize = 12.sp, color = Color.White)
            SlimButton("Commit")
        }
    }

}