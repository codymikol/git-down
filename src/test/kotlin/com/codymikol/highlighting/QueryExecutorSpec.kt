package com.codymikol.highlighting

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.treesitter.TSParser
import org.treesitter.TSQuery

class QueryExecutorSpec : DescribeSpec({

    describe("QueryExecutor.execute") {

        val language = GrammarLanguageLoader.load(
            BundledJsonGrammarFixture.extract(),
            BundledJsonGrammarFixture.FUNCTION_NAME,
        )!!

        fun parse(text: String) = TSParser().use { parser ->
            parser.setLanguage(language)
            parser.parseString(null, text)!!
        }

        it("returns a token per capture, with the capture's name and the node's byte range") {
            val text = "{\"key\": 1}"
            val tree = parse(text)
            val query = TSQuery(language, "(number) @number")

            val tokens = QueryExecutor.execute(query, tree.rootNode, text)

            tokens shouldContain SyntaxToken(type = "number", startByte = 8, endByte = 9, isNamed = true, captureName = "number")
        }

        it("returns a token for every pattern in the query that matches") {
            val text = "{\"key\": 1}"
            val tree = parse(text)
            val query = TSQuery(language, "(string) @string (number) @number")

            val tokens = QueryExecutor.execute(query, tree.rootNode, text)

            tokens.map { it.captureName } shouldContain "string"
            tokens.map { it.captureName } shouldContain "number"
        }

        it("returns an empty list when the query matches nothing") {
            val text = "{\"key\": 1}"
            val tree = parse(text)
            val query = TSQuery(language, "(comment) @comment")

            val tokens = QueryExecutor.execute(query, tree.rootNode, text)

            tokens shouldHaveSize 0
        }

    }

})
