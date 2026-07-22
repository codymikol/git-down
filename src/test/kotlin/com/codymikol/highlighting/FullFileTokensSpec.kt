package com.codymikol.highlighting

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class FullFileTokensSpec : DescribeSpec({

    describe("FullFileTokens.tokensForLine") {

        it("rebases a token fully contained within the target line to be relative to that line's start") {
            // "foo\nbar\n" -> line 0 is "foo" (bytes 0..3), line 1 is "bar" (bytes 4..7)
            val parsedFile = ParsedFile(
                lineByteRanges = listOf(0 until 3, 4 until 7),
                tokens = listOf(SyntaxToken("identifier", 4, 7, true)),
            )

            val tokens = FullFileTokens.tokensForLine(parsedFile, 1)

            tokens shouldBe listOf(SyntaxToken("identifier", 0, 3, true))
        }

        it("splits a token spanning multiple lines into a clipped fragment per line") {
            // "/*\nfoo\n*/" -> a block comment token spanning bytes 0..9 across all 3 lines
            val parsedFile = ParsedFile(
                lineByteRanges = listOf(0 until 2, 3 until 6, 7 until 9),
                tokens = listOf(SyntaxToken("comment", 0, 9, true)),
            )

            FullFileTokens.tokensForLine(parsedFile, 0) shouldBe listOf(SyntaxToken("comment", 0, 2, true))
            FullFileTokens.tokensForLine(parsedFile, 1) shouldBe listOf(SyntaxToken("comment", 0, 3, true))
            FullFileTokens.tokensForLine(parsedFile, 2) shouldBe listOf(SyntaxToken("comment", 0, 2, true))
        }

        it("drops a token that does not overlap the target line at all") {
            val parsedFile = ParsedFile(
                lineByteRanges = listOf(0 until 3, 4 until 7),
                tokens = listOf(SyntaxToken("identifier", 4, 7, true)),
            )

            FullFileTokens.tokensForLine(parsedFile, 0) shouldBe emptyList()
        }

        it("returns an empty list for a line index outside the file") {
            val parsedFile = ParsedFile(lineByteRanges = listOf(0 until 3), tokens = emptyList())

            FullFileTokens.tokensForLine(parsedFile, 5) shouldBe emptyList()
        }

    }

    describe("FullFileTokens.parse") {

        it("computes byte ranges per line accounting for multi-byte UTF-8 characters") {
            // "é=1\nb" -> line 0 is "é=1" (2-byte é + '=' + '1' = 4 bytes), line 1 is "b" (1 byte)
            val parsedFile = FullFileTokens.parse(null, "é=1\nb")

            parsedFile.lineByteRanges shouldBe listOf(0 until 4, 5 until 6)
        }

    }

})
