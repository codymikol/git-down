package com.codymikol.highlight

object Grammars {

    val PLAIN = Grammar(id = "plain", extensions = emptySet())

    val KOTLIN = Grammar(
        id = "kotlin",
        extensions = setOf("kt", "kts"),
        keywords = setOf(
            "val", "var", "fun", "class", "object", "interface", "if", "else", "when", "for", "while",
            "return", "import", "package", "null", "true", "false", "is", "as", "in", "private", "public",
            "protected", "internal", "override", "companion", "data", "sealed", "enum", "try", "catch",
            "finally", "throw", "this", "super", "typealias", "suspend", "inline", "const", "lateinit"
        ),
        lineComment = "//"
    )

    val JAVA = Grammar(
        id = "java",
        extensions = setOf("java"),
        keywords = setOf(
            "public", "private", "protected", "class", "interface", "extends", "implements", "static",
            "final", "void", "int", "long", "double", "float", "boolean", "char", "byte", "short", "if",
            "else", "for", "while", "return", "new", "import", "package", "null", "true", "false", "this",
            "super", "try", "catch", "finally", "throw", "enum", "abstract", "synchronized"
        ),
        lineComment = "//"
    )

    val JAVASCRIPT = Grammar(
        id = "javascript",
        extensions = setOf("js", "jsx", "ts", "tsx", "mjs", "cjs"),
        keywords = setOf(
            "const", "let", "var", "function", "class", "extends", "if", "else", "for", "while", "return",
            "import", "export", "from", "default", "null", "undefined", "true", "false", "this", "super",
            "try", "catch", "finally", "throw", "new", "typeof", "instanceof", "async", "await", "yield"
        ),
        lineComment = "//"
    )

    val PYTHON = Grammar(
        id = "python",
        extensions = setOf("py", "pyi"),
        keywords = setOf(
            "def", "class", "if", "elif", "else", "for", "while", "return", "import", "from", "as", "None",
            "True", "False", "try", "except", "finally", "raise", "with", "lambda", "yield", "pass",
            "break", "continue", "global", "nonlocal", "self", "not", "and", "or", "is", "in"
        ),
        lineComment = "#"
    )

    val ALL = listOf(KOTLIN, JAVA, JAVASCRIPT, PYTHON)

}
