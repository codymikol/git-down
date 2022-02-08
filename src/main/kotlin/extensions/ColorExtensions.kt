package extensions

import java.awt.Color

fun androidx.compose.ui.graphics.Color.toComposeColor(): java.awt.Color {
    return java.awt.Color(this.red, this.green, this.blue, this.alpha)
}