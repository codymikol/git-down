package extensions

fun androidx.compose.ui.graphics.Color.toComposeColor(): java.awt.Color {
    return java.awt.Color(this.red, this.green, this.blue, this.alpha)
}