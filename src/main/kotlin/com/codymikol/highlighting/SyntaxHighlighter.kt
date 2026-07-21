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

    fun highlight(text: String, tokens: List<SyntaxToken>): AnnotatedString {
        // tree-sitter reports UTF-8 byte offsets; Kotlin Strings are indexed in UTF-16 chars, so
        // any non-ASCII text needs the two translated before they can be used as span bounds.
        val byteToCharIndexMap = buildByteToCharIndexMap(text)
        return AnnotatedString.Builder(text).apply {
            tokens.forEach { token ->
                val color = colorFor(token) ?: return@forEach
                val start = charIndexFor(byteToCharIndexMap, token.startByte)
                val end = charIndexFor(byteToCharIndexMap, token.endByte).coerceAtLeast(start)
                if (start < end) addStyle(SpanStyle(color = color), start, end)
            }
        }.toAnnotatedString()
    }

    private fun buildByteToCharIndexMap(text: String): IntArray {
        val byteLength = text.toByteArray(Charsets.UTF_8).size
        val map = IntArray(byteLength + 1) { text.length }
        var byteIndex = 0
        var charIndex = 0
        while (charIndex < text.length) {
            val codePoint = text.codePointAt(charIndex)
            val charCount = Character.charCount(codePoint)
            val byteCount = String(Character.toChars(codePoint)).toByteArray(Charsets.UTF_8).size
            for (offset in 0 until byteCount) map[byteIndex + offset] = charIndex
            byteIndex += byteCount
            charIndex += charCount
        }
        return map
    }

    private fun charIndexFor(byteToCharIndexMap: IntArray, byteOffset: Int): Int =
        byteToCharIndexMap[byteOffset.coerceIn(0, byteToCharIndexMap.size - 1)]

    private fun colorFor(token: SyntaxToken): Color? = when {
        "comment" in token.type -> Color(106, 153, 85)
        "string" in token.type || "char" in token.type -> Color(206, 145, 120)
        "number" in token.type || "integer" in token.type || "float" in token.type -> Color(181, 206, 168)
        !token.isNamed && keywordPattern.matches(token.type) -> Color(86, 156, 214)
        else -> null
    }

}
