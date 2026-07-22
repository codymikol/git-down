package com.codymikol.highlighting

import androidx.compose.ui.graphics.Color

/**
 * Maps tree-sitter highlights.scm capture names (https://tree-sitter.github.io/tree-sitter/using-parsers/queries/3-predicates-and-directives.html#standard-capture-names)
 * to colors, baked to match the app's existing diff-view aesthetic - comment/string/number/
 * keyword reuse the exact colors [SyntaxHighlighter] already used before queries existed.
 * Capture names not listed here (e.g. "operator", "punctuation.*") are intentionally left out so
 * they fall back to the base text color instead of being invented.
 */
object HighlightTheme {

    // Named so SyntaxHighlighter's node-type heuristic can reuse the exact same colors instead
    // of duplicating the literals.
    val comment = Color(106, 153, 85)
    val string = Color(206, 145, 120)
    val number = Color(181, 206, 168)
    val keyword = Color(86, 156, 214)

    val colors: Map<String, Color> = mapOf(
        "comment" to comment,
        "string" to string,
        "string.escape" to Color(215, 186, 125),
        "number" to number,
        "keyword" to keyword,
        "tag" to keyword,
        "function" to Color(220, 220, 170),
        "type" to Color(78, 201, 176),
        "constant" to Color(79, 193, 255),
        "property" to Color(156, 220, 254),
        "variable.parameter" to Color(156, 220, 254),
        "attribute" to Color(156, 220, 254),
    )

    /**
     * Resolves a capture name to a color, trying progressively shorter dotted prefixes (e.g.
     * "function.builtin" falls back to "function") since highlights.scm files capture with
     * fine-grained names this baked theme doesn't need to enumerate individually.
     */
    fun colorFor(captureName: String): Color? {
        var name = captureName
        while (true) {
            colors[name]?.let { return it }
            val dotIndex = name.lastIndexOf('.')
            if (dotIndex < 0) return null
            name = name.substring(0, dotIndex)
        }
    }

}
