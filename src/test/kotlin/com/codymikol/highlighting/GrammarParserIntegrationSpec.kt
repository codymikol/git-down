package com.codymikol.highlighting

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.treesitter.TSQuery

class GrammarParserIntegrationSpec : DescribeSpec({

    describe("tree-sitter integration") {

        it("loads a real compiled grammar and highlights a parsed string token") {
            val grammarFile = BundledJsonGrammarFixture.extract()

            val language = GrammarLanguageLoader.load(grammarFile, BundledJsonGrammarFixture.FUNCTION_NAME)
            requireNotNull(language) { "Expected the bundled tree-sitter-json fixture to load" }

            val text = "{\"key\": 1}"
            val tokens = GrammarParser.parse(language, text)
            val stringContentToken = tokens.single { it.type == "string_content" }

            val highlighted = SyntaxHighlighter.highlight(text, tokens)
            highlighted.spanStyles.map { it.start to it.end } shouldContain
                (stringContentToken.startByte to stringContentToken.endByte)
        }

        it("runs a highlights.scm query against the parsed tree when one is given, producing captured tokens") {
            val grammarFile = BundledJsonGrammarFixture.extract()
            val language = GrammarLanguageLoader.load(grammarFile, BundledJsonGrammarFixture.FUNCTION_NAME)
            requireNotNull(language) { "Expected the bundled tree-sitter-json fixture to load" }

            val text = "{\"key\": 1}"
            val query = TSQuery(language, "(number) @number")

            val tokens = GrammarParser.parse(language, text, query)

            tokens.single().captureName shouldBe "number"
        }

    }

})
