package com.codymikol.highlight

data class Grammar(
    val id: String,
    val extensions: Set<String>,
    val keywords: Set<String> = emptySet(),
    val lineComment: String? = null,
    val stringDelimiters: Set<Char> = setOf('"', '\'')
)
