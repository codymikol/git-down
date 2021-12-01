package components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.Colors

@Composable
fun Subheader(title: String) {
    Row(
        modifier = Modifier
            .background(Colors.LightGrayBackground)
            .requiredHeight(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp), modifier = Modifier.padding(8.dp).fillMaxWidth(), color = Color.White, text = title)
    }
}

@Composable
@Preview
fun DemoSubheader() {
    return Subheader("Working Directory")
}
