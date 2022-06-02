package views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.SlimButton
import components.Subheader
import components.commit.ChangedFile
import components.commit.CommitBottomToolbar
import components.commit.FileIcon
import data.Colors
import data.diff.Hunk
import data.diff.Line
import data.diff.LineType
import data.file.FileDelta
import data.file.Status
import extensions.stageAll
import extensions.stageFile
import extensions.unstageFile
import kotlinx.coroutines.launch
import state.GitDownState
import typography.GitDownTypography


val commitMessage = mutableStateOf("")

@Composable
@Preview
fun CommitView() {

    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
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
                CommitMessageInput()
            }
        }
        CommitBottomToolbar(commitMessage)
    }
}

fun DrawScope.drawCommitGuideline(xOffset: Float, lineHeight: Float, spaceHeight: Float) {

    val pathEffect =
        PathEffect.dashPathEffect(floatArrayOf(lineHeight, spaceHeight), 0f)

    drawLine(
        color = Color.White,
        start = Offset(xOffset, 0f),
        end = Offset(xOffset, size.height),
        pathEffect = pathEffect,
        strokeWidth = 0.2f,
    )

}

@Composable
private fun CommitMessageInput() {
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
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp, 4.dp, 8.dp, 0.dp)
                .drawBehind {
                    drawCommitGuideline(330f, 2f, 4f)
                    drawCommitGuideline(475f, 4f, 2f)
                }) {
                innerTextField()
            }
        }
    )
}

@Composable
private fun LineNumberGutter(lineNumber: UInt?) {
    Row(
        modifier = Modifier
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

private class HeaderButtonColors : ButtonColors {
    @Composable
    override fun backgroundColor(enabled: Boolean): State<Color> {
        return mutableStateOf(Color.Transparent)
    }

    @Composable
    override fun contentColor(enabled: Boolean): State<Color> {
        return mutableStateOf(Color.White)
    }

}

@Composable
fun ChangedFileHeader(fileDelta: FileDelta) {

    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier.height(32.dp)
            .background(fileDelta.color)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {

            Row(
                modifier = Modifier.fillMaxHeight().wrapContentWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Spacer(modifier = Modifier.width(6.dp))

                FileIcon(
                    modifier = Modifier.shadow(elevation = 3.dp, shape = RoundedCornerShape(14.dp)),
                    fileDelta = fileDelta
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    fileDelta.location.toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

            }


            Row(
                modifier = Modifier.fillMaxHeight().wrapContentWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                OutlinedButton(
                    onClick = {
                        scope.launch {

                            val path = fileDelta.location.toString()

                            when (fileDelta.type) {
                                Status.INDEX -> GitDownState.git.value.unstageFile(path)
                                Status.WORKING_DIRECTORY -> GitDownState.git.value.stageFile(path)
                            }

                            GitDownState.selectedFiles.remove(fileDelta)

                        }
                    },
                    modifier = Modifier.height(24.dp).padding(0.dp),
                    colors = HeaderButtonColors(),
                    contentPadding = PaddingValues(12.dp, 0.dp)
                ) {

                    val actionText = when (fileDelta.type) {
                        Status.INDEX -> "Unstage File"
                        Status.WORKING_DIRECTORY -> "Stage File"
                    }

                    Text(actionText, fontSize = 9.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))
            }
        }

    }
}

@Composable
private fun DiffPanel() {

    GitDownState.selectedFiles.forEach {

        ChangedFileHeader(it)

        it.getDiff().hunks.forEach { hunk ->

            HunkHeader(hunk)

            hunk.lines.forEach { line ->

                val color = when (line.type) {
                    LineType.Added -> Color(40, 88, 41)
                    LineType.Removed -> Color(88, 39, 39)
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
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
                scope.launch {
                    GitDownState.git.value.stageAll()
                    GitDownState.selectedFiles.clear()
                }
            }
        }
    }

}


@Preview
@Composable
private fun CommitIndex() {
    FileDeltaPanel("Index", GitDownState.index)
}
