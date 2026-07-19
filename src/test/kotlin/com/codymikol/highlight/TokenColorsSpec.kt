package com.codymikol.highlight

import androidx.compose.ui.graphics.Color
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class TokenColorsSpec : DescribeSpec({

    describe("TokenColors") {

        describe("colorFor") {

            describe("when the token is plain") {

                it("should return the supplied base color unchanged") {
                    TokenColors.colorFor(TokenKind.PLAIN, Color.White) shouldBe Color.White
                }

            }

            describe("when the token is a keyword, string, comment, or number") {

                it("should return a color distinct from the base color for each") {
                    TokenColors.colorFor(TokenKind.KEYWORD, Color.White) shouldNotBe Color.White
                    TokenColors.colorFor(TokenKind.STRING, Color.White) shouldNotBe Color.White
                    TokenColors.colorFor(TokenKind.COMMENT, Color.White) shouldNotBe Color.White
                    TokenColors.colorFor(TokenKind.NUMBER, Color.White) shouldNotBe Color.White
                }

            }

        }

    }

})
