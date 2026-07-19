package com.codymikol.highlight

import androidx.compose.ui.graphics.Color

object TokenColors {

    fun colorFor(kind: TokenKind, baseColor: Color): Color = when (kind) {
        TokenKind.KEYWORD -> Color(198, 120, 221)
        TokenKind.STRING -> Color(152, 195, 121)
        TokenKind.COMMENT -> Color(92, 99, 112)
        TokenKind.NUMBER -> Color(209, 154, 102)
        TokenKind.PLAIN -> baseColor
    }

}
