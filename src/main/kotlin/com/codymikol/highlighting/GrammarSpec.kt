package com.codymikol.highlighting

data class GrammarSpec(
    val repo: String,
    val functionName: String,
    // Most tree-sitter-grammars repos put C sources directly under `src/`, but a few bundle
    // multiple grammars in one repo (tree-sitter-markdown, tree-sitter-csv) and nest sources
    // one level deeper.
    val sourcePath: String = "src",
    // Mirrors sourcePath's nesting for each repo's `queries/highlights.scm`. Null means the
    // upstream repo ships no queries directory at all, so query fetching is skipped entirely
    // rather than retried every cache cycle against a path that will never exist.
    val queriesPath: String? = "queries",
)
