package views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.SlimButton
import components.Subheader
import components.commit.ChangedFile
import components.commit.CommitBottomToolbar
import data.Colors
import data.diff.Hunk
import data.diff.Line
import data.diff.LineType
import data.file.FileDelta
import extensions.stageAll
import kotlinx.coroutines.launch
import state.GitDownState
import typography.GitDownTypography

val commitMessage = mutableStateOf("")

@Composable
@Preview
fun CommitView() {

    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
        Row(modifier = Modifier.weight(40f, true).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .background(Colors.DarkGrayBackground)
                    .weight(40f).fillMaxHeight()
                    .border(width = 1.dp, color = Color.Black)
            ) {
                Column(modifier = Modifier.weight(50f)) { CommitWorkingDirectory() }
                Column(modifier = Modifier.weight(50f)) { CommitIndex() }
            }
            Column(
                modifier = Modifier
                    .weight(60f)
                    .fillMaxHeight()
                    .background(Colors.DarkGrayBackground)
                    .border(width = 1.dp, color = Color.Black)
                    .verticalScroll(ScrollState(0))
            ) {
                DiffPanel()
            }
        }
        Column(Modifier.weight(10f)) {
            Column(modifier = Modifier.fillMaxWidth().background(Colors.LightGrayBackground)) {
                BasicTextField(
                    cursorBrush = Brush.verticalGradient(0.00f to Color.White),
                    value = commitMessage.value,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp, start = 8.dp, bottom = 0.dp, end = 8.dp)
                        .background(Colors.DarkGrayBackground),
                    textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                    onValueChange = { commitMessage.value = it },
                    decorationBox = { innerTextField ->
                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp, 4.dp, 8.dp, 0.dp)) {
                            // todo(mikol): use this doodad to make the guidelines ;)
                            innerTextField()
                        }
                    }
                )
            }
        }
        CommitBottomToolbar(commitMessage)
    }
}

@Composable
private fun LineNumberGutter(lineNumber: Int?) {
    Row(modifier = Modifier
        .width(36.dp)
        .fillMaxHeight()
    ) {
        GitDownTypography.LineNumber(lineNumber?.toString() ?: "")
    }
}

@Composable
private fun ModificationTypeGutter(line: Line) {
    Box(modifier = Modifier.width(24.dp).fillMaxSize()) { GitDownTypography.DiffType(line.symbol) }
}

@Composable
private fun DiffPanel() {
    GitDownState.selectedFiles.forEach {

        it.getDiff().hunks.forEach { hunk ->

            HunkHeader(hunk)

            hunk.lines.forEach { line ->

                val color = when(line.type) {
                    LineType.Added -> Color(40,88,41)
                    LineType.Removed -> Color(88,39,39)
                    LineType.Unchanged -> Color.Transparent
                    else -> Color.Yellow // todo(mikol): Bake these colors into the Line
                }

                Box(modifier = Modifier.background(color).fillMaxWidth().wrapContentHeight()) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        LineNumberGutter(line.originalLineNumber)
                        LineNumberGutter(line.newLineNumber)
                        ModificationTypeGutter(line)
                        GitDownTypography.DiffContent(line.value)
                    }
                }
            }

        }

    }
}

@Composable
private fun HunkHeader(hunk: Hunk) {
    Row(modifier = Modifier.background(Color(8, 8, 8)).fillMaxWidth()) {
        Spacer(modifier = Modifier.width(86.dp))
        GitDownTypography.DiffHunkHeader(hunk.delimiter)
    }
}


@Composable
private fun FileDeltaPanel(title: String, deltas: State<Set<FileDelta>>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Subheader(title)
        Column(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(6.dp, 0.dp, 0.dp, 0.dp)
            .verticalScroll(state = ScrollState(initial = 0))
        ) {
            Spacer(Modifier.height(6.dp))
            deltas.value.forEach { ChangedFile(it) }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun CommitWorkingDirectory() {

    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxHeight()) {

        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
            FileDeltaPanel("Working Directory", GitDownState.workingDirectory)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(48.dp)
                .background(Colors.MediumGrayBackground)
                .padding(horizontal = 8.dp)
        ) {
            SlimButton("Discard All...")
            SlimButton("Stage All") {
                scope.launch { GitDownState.git.value.stageAll() }
            }
        }
    }

}

@Preview
@Composable
private fun CommitIndex() {
    FileDeltaPanel("Index", GitDownState.index)
}
