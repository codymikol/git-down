package com.codymikol.highlighting

data class SyntaxToken(
    val type: String,
    val startByte: Int,
    val endByte: Int,
    val isNamed: Boolean,
)
