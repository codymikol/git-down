package com.codymikol.highlighting

data class GrammarSpec(
    val repo: String,
    val functionName: String,
    // Most tree-sitter-grammars repos put C sources directly under `src/`, but a few bundle
    // multiple grammars in one repo (tree-sitter-markdown, tree-sitter-csv) and nest sources
    // one level deeper.
    val sourcePath: String = "src",
)
