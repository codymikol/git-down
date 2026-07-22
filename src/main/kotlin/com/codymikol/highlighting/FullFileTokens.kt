package com.codymikol.highlighting

import org.treesitter.TSLanguage
import org.treesitter.TSQuery

/**
 * The result of parsing a whole file's text once: the tokens (with byte offsets relative to the
 * full text) alongside the byte range each line occupies within that text, so a token that spans
 * multiple lines (e.g. a block comment) can be sliced onto each line it touches.
 */
data class ParsedFile(
    val lineByteRanges: List<IntRange>,
    val tokens: List<SyntaxToken>,
)

object FullFileTokens {

    fun parse(language: TSLanguage?, text: String, query: TSQuery? = null): ParsedFile {
        val tokens = GrammarParser.parse(language, text, query)
        val lineByteRanges = mutableListOf<IntRange>()
        var byteOffset = 0
        text.split("\n").forEach { line ->
            val lineByteLength = line.toByteArray(Charsets.UTF_8).size
            lineByteRanges.add(byteOffset until byteOffset + lineByteLength)
            byteOffset += lineByteLength + 1 // +1 for the '\n' separator
        }
        return ParsedFile(lineByteRanges, tokens)
    }

    /**
     * Slices [parsedFile]'s tokens down to those overlapping the given line, re-basing each
     * token's byte offsets to be relative to that line's own start (as required by
     * [SyntaxHighlighter.highlight]). A token spanning multiple lines is clipped to the portion
     * that falls on this line.
     */
    fun tokensForLine(parsedFile: ParsedFile, lineIndex: Int): List<SyntaxToken> {
        val lineRange = parsedFile.lineByteRanges.getOrNull(lineIndex) ?: return emptyList()
        val lineStart = lineRange.first
        val lineEnd = lineRange.last + 1

        return parsedFile.tokens.mapNotNull { token ->
            val clippedStart = token.startByte.coerceIn(lineStart, lineEnd)
            val clippedEnd = token.endByte.coerceIn(lineStart, lineEnd)
            if (clippedStart >= clippedEnd) null
            else token.copy(startByte = clippedStart - lineStart, endByte = clippedEnd - lineStart)
        }
    }

}
