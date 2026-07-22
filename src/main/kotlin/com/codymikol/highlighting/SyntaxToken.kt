package com.codymikol.highlighting

data class SyntaxToken(
    val type: String,
    val startByte: Int,
    val endByte: Int,
    val isNamed: Boolean,
    // Set when this token came from executing a highlights.scm query capture (e.g. "string",
    // "function.builtin") rather than the leaf-node heuristic; null keeps the old fallback path.
    val captureName: String? = null,
)
