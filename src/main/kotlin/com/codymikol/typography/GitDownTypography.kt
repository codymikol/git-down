package com.codymikol.typography

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.codymikol.gitdown.generated.resources.*
import org.jetbrains.compose.resources.Font


const val whiteLevel = 120

@Composable
fun jetbrainsMono() = FontFamily(
  Font(Res.font.jb_mono_bold, FontWeight.Bold),
  Font(Res.font.jb_mono_bold_italic, FontWeight.Bold, FontStyle.Italic),
  Font(Res.font.jb_mono_extra_bold, FontWeight.ExtraBold),
  Font(Res.font.jb_mono_extra_bold_italic, FontWeight.ExtraBold, FontStyle.Italic),
  Font(Res.font.jb_mono_extra_light, FontWeight.ExtraLight),
  Font(Res.font.jb_mono_extra_light_italic, FontWeight.ExtraLight, FontStyle.Italic),
  Font(Res.font.jb_mono_italic, FontWeight.Normal, FontStyle.Italic),
  Font(Res.font.jb_mono_light, FontWeight.Light),
  Font(Res.font.jb_mono_light_italic, FontWeight.Light, FontStyle.Italic),
  Font(Res.font.jb_mono_medium, FontWeight.Medium),
  Font(Res.font.jb_mono_medium_italic, FontWeight.Medium, FontStyle.Italic),
  Font(Res.font.jb_mono_regular, FontWeight.Normal),
  Font(Res.font.jb_mono_semi_bold, FontWeight.SemiBold),
  Font(Res.font.jb_mono_semi_bold_italic, FontWeight.SemiBold, FontStyle.Italic),
  Font(Res.font.jb_mono_thin, FontWeight.Thin),
  Font(Res.font.jb_mono_thin_italic, FontWeight.Thin, FontStyle.Italic),
)

object GitDownTypography {

    @Composable
    fun DiffHunkHeader(value: String) {
        Text(
            value,
            modifier = Modifier.fillMaxWidth(),
            color = Color(whiteLevel, whiteLevel, whiteLevel),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = jetbrainsMono(),
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
            fontFamily = jetbrainsMono(),
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
                //
                // future cody here... What is this? future future cody, please figure it out, thanks!...
                //
                // Oh no, Cody here from 2024, what is this shit?

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
            fontFamily = jetbrainsMono(),
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
            fontFamily = jetbrainsMono(),
        )
    }

} 
