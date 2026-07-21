package com.codymikol.highlighting

import androidx.compose.ui.graphics.Color
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class SyntaxHighlighterSpec : DescribeSpec({

    describe("SyntaxHighlighter") {

        it("renders the base color across the whole line when there are no tokens") {
            val result = SyntaxHighlighter.highlight("val x = 1", emptyList())

            result.text shouldBe "val x = 1"
            result.spanStyles shouldBe emptyList()
        }

        it("colors an unnamed alphabetic token as a keyword") {
            val tokens = listOf(SyntaxToken(type = "val", startByte = 0, endByte = 3, isNamed = false))

            val result = SyntaxHighlighter.highlight("val x = 1", tokens)

            result.spanStyles.single().item.color shouldBe Color(86, 156, 214)
            result.spanStyles.single().start shouldBe 0
            result.spanStyles.single().end shouldBe 3
        }

        it("colors a token whose type contains 'comment'") {
            val tokens = listOf(SyntaxToken(type = "line_comment", startByte = 0, endByte = 8, isNamed = true))

            val result = SyntaxHighlighter.highlight("// hello", tokens)

            result.spanStyles.single().item.color shouldBe Color(106, 153, 85)
        }

        it("colors a token whose type contains 'string'") {
            val tokens = listOf(SyntaxToken(type = "string_literal", startByte = 0, endByte = 5, isNamed = true))

            val result = SyntaxHighlighter.highlight("\"abc\"", tokens)

            result.spanStyles.single().item.color shouldBe Color(206, 145, 120)
        }

        it("colors a token whose type contains 'number'") {
            val tokens = listOf(SyntaxToken(type = "number_literal", startByte = 0, endByte = 1, isNamed = true))

            val result = SyntaxHighlighter.highlight("1", tokens)

            result.spanStyles.single().item.color shouldBe Color(181, 206, 168)
        }

        it("leaves a named identifier token unstyled so it falls back to the base color") {
            val tokens = listOf(SyntaxToken(type = "identifier", startByte = 0, endByte = 1, isNamed = true))

            val result = SyntaxHighlighter.highlight("x", tokens)

            result.spanStyles shouldBe emptyList()
        }

        it("clamps a token whose byte range extends past the end of the line") {
            val tokens = listOf(SyntaxToken(type = "val", startByte = 0, endByte = 999, isNamed = false))

            val result = SyntaxHighlighter.highlight("val", tokens)

            result.spanStyles.single().end shouldBe 3
        }

        it("drops a token whose range is entirely out of bounds") {
            val tokens = listOf(SyntaxToken(type = "val", startByte = 10, endByte = 20, isNamed = false))

            val result = SyntaxHighlighter.highlight("val", tokens)

            result.spanStyles shouldBe emptyList()
        }

    }

})
