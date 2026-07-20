package com.codymikol.highlight

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class TokenizerSpec : DescribeSpec({

    describe("Tokenizer") {

        describe("tokenize") {

            val grammar = Grammar(
                id = "test",
                extensions = setOf("test"),
                keywords = setOf("val", "fun"),
                lineComment = "//"
            )

            describe("when a word matches a grammar keyword") {

                it("should classify it as a keyword token") {
                    Tokenizer.tokenize(grammar, "val") shouldBe listOf(Token("val", TokenKind.KEYWORD))
                }

            }

            describe("when a word does not match a grammar keyword") {

                it("should classify it as a plain token") {
                    Tokenizer.tokenize(grammar, "value") shouldBe listOf(Token("value", TokenKind.PLAIN))
                }

            }

            describe("when the line contains a quoted string") {

                it("should classify the quoted span as a single string token") {
                    Tokenizer.tokenize(grammar, "val a = \"hi\"") shouldBe listOf(
                        Token("val", TokenKind.KEYWORD),
                        Token(" ", TokenKind.PLAIN),
                        Token("a", TokenKind.PLAIN),
                        Token(" = ", TokenKind.PLAIN),
                        Token("\"hi\"", TokenKind.STRING)
                    )
                }

            }

            describe("when the line contains a line comment") {

                it("should classify everything from the comment marker onward as a comment token") {
                    Tokenizer.tokenize(grammar, "1 // note") shouldBe listOf(
                        Token("1", TokenKind.NUMBER),
                        Token(" ", TokenKind.PLAIN),
                        Token("// note", TokenKind.COMMENT)
                    )
                }

            }

            describe("when the line contains a numeric literal") {

                it("should classify the digits as a number token") {
                    Tokenizer.tokenize(grammar, "x = 42.5") shouldBe listOf(
                        Token("x", TokenKind.PLAIN),
                        Token(" = ", TokenKind.PLAIN),
                        Token("42.5", TokenKind.NUMBER)
                    )
                }

            }

        }

        describe("when the grammar has no syntax rules") {

            it("should classify the whole line as a single plain token without scanning for numbers or strings") {
                Tokenizer.tokenize(Grammars.PLAIN, "don't stop at 42") shouldBe listOf(
                    Token("don't stop at 42", TokenKind.PLAIN)
                )
            }

        }

    }

})
