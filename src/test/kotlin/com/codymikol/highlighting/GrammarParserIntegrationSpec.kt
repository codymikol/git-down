package com.codymikol.highlighting

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeBytes

/**
 * io.github.bonede:tree-sitter-json bundles a real prebuilt native grammar per platform, purely
 * as a test fixture - it lets us exercise GrammarLanguageLoader/GrammarParser/SyntaxHighlighter
 * against an actual compiled `.so` (rather than only the safe-failure paths the other specs
 * cover), without git-down's own download-and-compile pipeline needing a C compiler in CI.
 */
class GrammarParserIntegrationSpec : DescribeSpec({

    describe("tree-sitter integration") {

        it("loads a real compiled grammar and highlights a parsed string token") {
            val resourcePath = "/lib/$bundledGrammarPlatformTag-tree-sitter-json.$bundledGrammarExtension"
            val resource = javaClass.getResourceAsStream(resourcePath)
                ?: error("Test fixture not found on classpath: $resourcePath")

            val grammarFile = createTempDirectory("git-down-grammar-integration-test-").resolve("json.$bundledGrammarExtension")
            grammarFile.writeBytes(resource.use { it.readBytes() })

            val language = GrammarLanguageLoader.load(grammarFile, "tree_sitter_json")
            requireNotNull(language) { "Expected the bundled tree-sitter-json fixture to load" }

            val text = "{\"key\": 1}"
            val tokens = GrammarParser.parse(language, text)
            val stringContentToken = tokens.single { it.type == "string_content" }

            val highlighted = SyntaxHighlighter.highlight(text, tokens)
            highlighted.spanStyles.map { it.start to it.end } shouldContain
                (stringContentToken.startByte to stringContentToken.endByte)
        }

    }

})

private val bundledGrammarPlatformTag: String = run {
    val arch = if (System.getProperty("os.arch").contains("aarch64")) "aarch64" else "x86_64"
    val os = System.getProperty("os.name").lowercase()
    val osTag = when {
        "mac" in os -> "macos"
        "win" in os -> "windows"
        else -> "linux-gnu"
    }
    "$arch-$osTag"
}

private val bundledGrammarExtension: String = when {
    "windows" in bundledGrammarPlatformTag -> "dll"
    "macos" in bundledGrammarPlatformTag -> "dylib"
    else -> "so"
}
