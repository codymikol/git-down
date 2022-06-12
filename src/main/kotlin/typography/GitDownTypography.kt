package typography

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

const val whiteLevel = 120

object GitDownTypography {


    @Composable
    fun DiffHunkHeader(value: String) {
        Text(
            value,
            modifier = Modifier.fillMaxWidth(),
            color = Color(whiteLevel, whiteLevel, whiteLevel),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }

    @Composable
    fun DiffType(value: String) {
        Text(
            value,
            modifier = Modifier.fillMaxSize(),
            textAlign = TextAlign.Center,
            color = Color(whiteLevel, whiteLevel, whiteLevel),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }

    @Composable
    fun DiffContent(value: String, color: Color) {
        Text(
            value,
            modifier = Modifier.drawBehind {

                // todo(mikol): horrible hack to get gutter lines working on wrapped text...
                // Been working on this all day and just want to get the feature in, please fix this
                // future Cody, I know you can do it!

                // future cody here... What is this? future future cody, please figure it out, thanks!...

                listOf(-20f,-56f).forEach {
                    drawLine(
                        Color(whiteLevel, whiteLevel, whiteLevel),
                        Offset(it, 0f),
                        Offset(it, size.height),
                        1f
                    )
                }
            },
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }

    @Composable
    fun LineNumber(value: String) {
        Text(
            value,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
            color = Color(whiteLevel, whiteLevel, whiteLevel),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }

}