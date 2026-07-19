package com.codymikol.highlight

object SyntaxHighlighter {

    fun highlight(path: String, line: String): List<Token> =
        Tokenizer.tokenize(GrammarRegistry.forPath(path), line)

}
