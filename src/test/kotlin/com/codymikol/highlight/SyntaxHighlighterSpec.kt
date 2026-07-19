package com.codymikol.highlight

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class SyntaxHighlighterSpec : DescribeSpec({

    describe("SyntaxHighlighter") {

        describe("highlight") {

            describe("when the path resolves to a known grammar") {

                it("should tokenize the line using that grammar's keywords") {
                    SyntaxHighlighter.highlight("Main.kt", "val") shouldBe listOf(Token("val", TokenKind.KEYWORD))
                }

            }

            describe("when the path does not resolve to a known grammar") {

                it("should tokenize the line as plain text") {
                    SyntaxHighlighter.highlight("README.md", "hello") shouldBe listOf(Token("hello", TokenKind.PLAIN))
                }

            }

        }

    }

})
