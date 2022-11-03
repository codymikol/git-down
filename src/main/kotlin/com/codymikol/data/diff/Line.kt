package com.codymikol.data.diff

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import com.codymikol.data.diff.LineType.*

class Line(
    val type: LineType,
    val value: String,
    val symbol: String,
    var selected: MutableState<Boolean> = mutableStateOf(false),
    var originalLineNumber: UInt?,
    var newLineNumber: UInt?,
) {

    fun toggleSelected() = run { this.selected.value = !this.selected.value }
    fun getBackgroundColor(): Color = when (selected.value) {
        true -> Color(71,99,136)
        false -> getUnselectedBackgroundColor()
    }

    private fun getUnselectedBackgroundColor(): Color = when(type) {
        Added -> Color(40, 88, 41)
        Removed -> Color(88, 39, 39)
        Unchanged -> Color.Transparent
        NoNewline -> Color.DarkGray
        else -> Color.Yellow
    }

    fun getTextColor(): Color = when (type) {
        Added, Removed, Unchanged, Unknown -> Color.White
        NoNewline -> Color.Gray
    }

    companion object {
        fun make(line: String): Line {

            val type = when (line[0]) {
                '+' -> Added
                '-' -> Removed
                ' ' -> Unchanged
                '\\' -> NoNewline
                else -> Unknown
            }

            val symbol = when (type) {
                Added -> "+"
                Removed -> "—"
                Unknown -> "?"
                NoNewline, Unchanged -> " "
            }

            return Line(
                type = type,
                value = line.drop(1),
                symbol = symbol,
                selected = mutableStateOf(false),
                originalLineNumber = null,
                newLineNumber = null
            )

        }
    }

}