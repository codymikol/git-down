package com.codymikol.highlighting

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle

/**
 * Turns the token types produced by [GrammarParser] into an [AnnotatedString] the diff view can
 * render. This has no dependency on the native tree-sitter binding, so it is fully testable
 * without a real compiled grammar. Tokens with no recognized category are left unstyled, so the
 * caller's own base text color (set via `Text(..., color = ...)`) shows through unchanged.
 */
object SyntaxHighlighter {

    private val keywordPattern = Regex("^[a-zA-Z_][a-zA-Z0-9_]*$")

    fun highlight(text: String, tokens: List<SyntaxToken>): AnnotatedString =
        AnnotatedString.Builder(text).apply {
            tokens.forEach { token ->
                val color = colorFor(token) ?: return@forEach
                val start = token.startByte.coerceIn(0, text.length)
                val end = token.endByte.coerceIn(start, text.length)
                if (start < end) addStyle(SpanStyle(color = color), start, end)
            }
        }.toAnnotatedString()

    private fun colorFor(token: SyntaxToken): Color? = when {
        "comment" in token.type -> Color(106, 153, 85)
        "string" in token.type || "char" in token.type -> Color(206, 145, 120)
        "number" in token.type || "integer" in token.type || "float" in token.type -> Color(181, 206, 168)
        !token.isNamed && keywordPattern.matches(token.type) -> Color(86, 156, 214)
        else -> null
    }

}
