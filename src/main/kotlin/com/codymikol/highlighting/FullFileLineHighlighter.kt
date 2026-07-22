package com.codymikol.highlighting

import androidx.compose.ui.text.AnnotatedString
import com.codymikol.data.diff.Line
import com.codymikol.data.diff.LineType
import com.codymikol.data.file.FileDelta
import org.treesitter.TSLanguage
import java.util.concurrent.ConcurrentHashMap

/**
 * Highlights a single diff line using a parse of the *whole file* it belongs to, so tree-sitter
 * has the cross-line context per-line parsing lacks (multi-line comments, strings, etc). Returns
 * null on anything that prevents this - unreadable content, a line the diff model can't place
 * within the file, or a mismatch between the diff line and the file's own line - so the caller can
 * fall back to per-line highlighting instead.
 */
object FullFileLineHighlighter {

    private val cache = ConcurrentHashMap<Pair<String, String>, ParsedFile>()

    fun highlight(fileDelta: FileDelta, line: Line, displayLine: String, language: TSLanguage?): AnnotatedString? {
        val lineNumber = (if (line.type == LineType.Removed) line.originalLineNumber else line.newLineNumber)
            ?: return null
        val fullContent = fileDelta.getFullContent(line) ?: return null

        val tabReplaced = fullContent.replace("\t", "  ")
        val lines = tabReplaced.split("\n")
        val lineIndex = lineNumber.toInt() - 1
        if (lineIndex !in lines.indices || lines[lineIndex] != displayLine) return null

        val parsedFile = cache.getOrPut(fileDelta.getPath() to tabReplaced) { FullFileTokens.parse(language, tabReplaced) }
        val tokens = FullFileTokens.tokensForLine(parsedFile, lineIndex)
        return SyntaxHighlighter.highlight(displayLine, tokens)
    }

}
