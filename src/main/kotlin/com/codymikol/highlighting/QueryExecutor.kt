package com.codymikol.highlighting

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.treesitter.TSNode
import org.treesitter.TSQuery
import org.treesitter.TSQueryCursor

/**
 * Runs a compiled highlights.scm [TSQuery] over a parsed tree, turning each capture into a
 * [SyntaxToken] carrying its capture name (e.g. "string", "function.builtin") for
 * [SyntaxHighlighter] to theme. Executing a query against untrusted/foreign grammar output is
 * inherently unsafe, so every failure here is caught and turned into an empty (unhighlighted)
 * capture list, same as [GrammarParser]'s leaf-collection fallback.
 */
object QueryExecutor {

    private val logger: Logger = LoggerFactory.getLogger(QueryExecutor::class.java)

    fun execute(query: TSQuery, root: TSNode, text: String): List<SyntaxToken> = try {
        TSQueryCursor().use { cursor ->
            cursor.exec(query, root, text)
            val tokens = mutableListOf<SyntaxToken>()
            for (match in cursor.captures) {
                val capture = match.captures[match.captureIndex]
                tokens.add(
                    SyntaxToken(
                        type = capture.node.type,
                        startByte = capture.node.startByte,
                        endByte = capture.node.endByte,
                        isNamed = capture.node.isNamed,
                        captureName = query.getCaptureNameForId(capture.index),
                    ),
                )
            }
            tokens
        }
    } catch (e: Throwable) {
        logger.error("Failed to execute highlight query", e)
        emptyList()
    }

}
