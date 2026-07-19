package com.codymikol.highlight

import androidx.compose.ui.graphics.Color

object TokenColors {

    private val keywordColor = Color(198, 120, 221)
    private val stringColor = Color(152, 195, 121)
    private val commentColor = Color(92, 99, 112)
    private val numberColor = Color(209, 154, 102)

    fun colorFor(kind: TokenKind, baseColor: Color): Color = when (kind) {
        TokenKind.KEYWORD -> keywordColor
        TokenKind.STRING -> stringColor
        TokenKind.COMMENT -> commentColor
        TokenKind.NUMBER -> numberColor
        TokenKind.PLAIN -> baseColor
    }

}
