package com.codymikol.highlighting

/**
 * Maps a file extension to the tree-sitter-grammars (https://github.com/tree-sitter-grammars)
 * repository that provides its grammar, so [GrammarCache] knows what to fetch.
 */
object GrammarExtensionRegistry {

    private val grammarsByExtension: Map<String, GrammarSpec> = mapOf(
        "kt" to GrammarSpec("tree-sitter-kotlin", "tree_sitter_kotlin"),
        "kts" to GrammarSpec("tree-sitter-kotlin", "tree_sitter_kotlin"),
        "toml" to GrammarSpec("tree-sitter-toml", "tree_sitter_toml"),
        "md" to GrammarSpec("tree-sitter-markdown", "tree_sitter_markdown", sourcePath = "tree-sitter-markdown/src"),
        "lua" to GrammarSpec("tree-sitter-lua", "tree_sitter_lua"),
        "hcl" to GrammarSpec("tree-sitter-hcl", "tree_sitter_hcl"),
        "tf" to GrammarSpec("tree-sitter-hcl", "tree_sitter_hcl"),
        "vim" to GrammarSpec("tree-sitter-vim", "tree_sitter_vim"),
        "jl" to GrammarSpec("tree-sitter-julia", "tree_sitter_julia"),
        "hs" to GrammarSpec("tree-sitter-haskell", "tree_sitter_haskell"),
        "scss" to GrammarSpec("tree-sitter-scss", "tree_sitter_scss"),
        "csv" to GrammarSpec("tree-sitter-csv", "tree_sitter_csv", sourcePath = "csv/src"),
        "svelte" to GrammarSpec("tree-sitter-svelte", "tree_sitter_svelte"),
        "vue" to GrammarSpec("tree-sitter-vue", "tree_sitter_vue"),
        "tcl" to GrammarSpec("tree-sitter-tcl", "tree_sitter_tcl"),
        "thrift" to GrammarSpec("tree-sitter-thrift", "tree_sitter_thrift"),
    )

    fun forExtension(extension: String): GrammarSpec? = grammarsByExtension[extension.lowercase()]

}
