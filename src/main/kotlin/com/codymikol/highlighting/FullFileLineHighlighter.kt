package com.codymikol.highlighting

import androidx.compose.ui.text.AnnotatedString
import com.codymikol.data.diff.Line
import com.codymikol.data.diff.LineType
import com.codymikol.data.file.FileDelta
import org.treesitter.TSLanguage
import org.treesitter.TSQuery

/**
 * Highlights a single diff line using a parse of the *whole file* it belongs to, so tree-sitter
 * has the cross-line context per-line parsing lacks (multi-line comments, strings, etc). Returns
 * null on anything that prevents this - unreadable content, a line the diff model can't place
 * within the file, or a mismatch between the diff line and the file's own line - so the caller can
 * fall back to per-line highlighting instead.
 */
object FullFileLineHighlighter {

    // Bounded, not a plain unbounded map: every distinct (path, content) version viewed in a
    // session would otherwise be its own key that's never evicted. Keyed on the query instance
    // too (identity - TSQuery has no equals override) so a cached parse from before a query
    // became available is never served back once one is - only relevant to callers that resolve
    // a query lazily/inconsistently, since a given extension's query is otherwise stable.
    private const val MAX_CACHED_FILES = 32
    private val cache = BoundedCache<Triple<String, String, TSQuery?>, ParsedFile>(MAX_CACHED_FILES)

    // A full-file tree-sitter parse is proportional to the whole file's size, not just the
    // rendered line - an implausibly large file falls back to per-line parsing instead.
    private const val MAX_FULL_FILE_CHARS = 2_000_000

    fun highlight(
        fileDelta: FileDelta,
        line: Line,
        displayLine: String,
        language: TSLanguage?,
        query: TSQuery? = null,
    ): AnnotatedString? {
        val lineNumber = (if (line.type == LineType.Removed) line.originalLineNumber else line.newLineNumber)
            ?: return null
        val fullContent = fileDelta.getFullContent(line) ?: return null
        if (fullContent.length > MAX_FULL_FILE_CHARS) return null

        val tabReplaced = fullContent.replace("\t", "  ")
        val lines = tabReplaced.split("\n")
        val lineIndex = lineNumber.toInt() - 1
        if (lineIndex !in lines.indices || lines[lineIndex] != displayLine) return null

        val parsedFile = cache.getOrPut(Triple(fileDelta.getPath(), tabReplaced, query)) {
            FullFileTokens.parse(language, tabReplaced, query)
        }
        val tokens = FullFileTokens.tokensForLine(parsedFile, lineIndex)
        return SyntaxHighlighter.highlight(displayLine, tokens)
    }

}
