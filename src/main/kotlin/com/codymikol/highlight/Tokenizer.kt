package com.codymikol.highlight

object Tokenizer {

    fun tokenize(grammar: Grammar, line: String): List<Token> {
        val tokens = mutableListOf<Token>()
        val plain = StringBuilder()
        var i = 0

        fun flushPlain() {
            if (plain.isNotEmpty()) {
                tokens.add(Token(plain.toString(), TokenKind.PLAIN))
                plain.clear()
            }
        }

        while (i < line.length) {
            val c = line[i]

            if (grammar.lineComment != null && line.startsWith(grammar.lineComment, i)) {
                flushPlain()
                tokens.add(Token(line.substring(i), TokenKind.COMMENT))
                i = line.length
                continue
            }

            if (c in grammar.stringDelimiters) {
                flushPlain()
                val start = i
                val quote = c
                i++
                while (i < line.length && line[i] != quote) {
                    if (line[i] == '\\' && i + 1 < line.length) i++
                    i++
                }
                if (i < line.length) i++
                tokens.add(Token(line.substring(start, i), TokenKind.STRING))
                continue
            }

            if (c.isDigit()) {
                flushPlain()
                val start = i
                while (i < line.length && (line[i].isDigit() || line[i] == '.')) i++
                tokens.add(Token(line.substring(start, i), TokenKind.NUMBER))
                continue
            }

            if (c.isLetter() || c == '_') {
                flushPlain()
                val start = i
                while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_')) i++
                val word = line.substring(start, i)
                val kind = if (word in grammar.keywords) TokenKind.KEYWORD else TokenKind.PLAIN
                tokens.add(Token(word, kind))
                continue
            }

            plain.append(c)
            i++
        }

        flushPlain()
        return tokens
    }

}
