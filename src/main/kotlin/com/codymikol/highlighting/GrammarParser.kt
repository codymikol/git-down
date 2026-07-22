package com.codymikol.highlighting

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.treesitter.TSLanguage
import org.treesitter.TSNode
import org.treesitter.TSParser

/**
 * Walks the tree-sitter parse tree for a piece of text (a single diff line, or a whole file when
 * called via [FullFileTokens]) into a flat list of leaf tokens for [SyntaxHighlighter]. Parsing
 * untrusted/foreign grammar code is inherently unsafe, so every failure here is caught and turned
 * into an empty (unhighlighted) token list.
 */
object GrammarParser {

    private val logger: Logger = LoggerFactory.getLogger(GrammarParser::class.java)

    fun parse(language: TSLanguage?, text: String): List<SyntaxToken> {
        if (language == null) return emptyList()
        return try {
            TSParser().use { parser ->
                parser.setLanguage(language)
                val tree = parser.parseString(null, text) ?: return emptyList()
                val tokens = mutableListOf<SyntaxToken>()
                collectLeaves(tree.rootNode, tokens)
                tokens
            }
        } catch (e: Throwable) {
            logger.error("Failed to parse text with grammar", e)
            emptyList()
        }
    }

    private fun collectLeaves(node: TSNode, out: MutableList<SyntaxToken>) {
        if (node.childCount == 0) {
            out.add(SyntaxToken(node.type, node.startByte, node.endByte, node.isNamed))
            return
        }
        for (i in 0 until node.childCount) {
            collectLeaves(node.getChild(i), out)
        }
    }

}
