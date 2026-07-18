package com.codymikol.components

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

private val charWidth = 1f

private fun monospaceMeasure(text: String): Float = text.length * charWidth

class ReversedEllipsisTextSpec : DescribeSpec({

    describe("reversedEllipsis") {

        describe("when the text fits within the available width") {

            it("should return the text unchanged") {
                reversedEllipsis(
                    text = "short.kt",
                    maxWidthPx = 20f,
                    measure = ::monospaceMeasure,
                ) shouldBe "short.kt"
            }

        }

        describe("when the text is too long for the available width") {

            val text = "src/main/kotlin/com/codymikol/components/commit/diff/file/header/text/FileHeaderText.kt"

            it("should truncate from the front and prefix with an ellipsis") {
                val result = reversedEllipsis(
                    text = text,
                    maxWidthPx = 20f,
                    measure = ::monospaceMeasure,
                )

                result shouldBe ("..." + text.takeLast(17))
            }

            it("should end with the original text's suffix, preserving the filename") {
                val result = reversedEllipsis(
                    text = text,
                    maxWidthPx = 20f,
                    measure = ::monospaceMeasure,
                )

                text.endsWith(result.removePrefix("...")) shouldBe true
            }

            it("should never exceed the available width") {
                val result = reversedEllipsis(
                    text = text,
                    maxWidthPx = 20f,
                    measure = ::monospaceMeasure,
                )

                (monospaceMeasure(result) <= 20f) shouldBe true
            }

            it("should support a custom ellipsis string") {
                val result = reversedEllipsis(
                    text = text,
                    maxWidthPx = 20f,
                    measure = ::monospaceMeasure,
                    ellipsis = "…",
                )

                result shouldBe ("…" + text.takeLast(19))
            }

        }

        describe("when even the ellipsis does not fit") {

            it("should return just the ellipsis") {
                reversedEllipsis(
                    text = "FileHeaderText.kt",
                    maxWidthPx = 1f,
                    measure = ::monospaceMeasure,
                ) shouldBe "..."
            }

        }

        describe("when the text is empty") {

            it("should return an empty string") {
                reversedEllipsis(
                    text = "",
                    maxWidthPx = 20f,
                    measure = ::monospaceMeasure,
                ) shouldBe ""
            }

        }

        describe("when the available width is zero") {

            it("should return the text unchanged") {
                reversedEllipsis(
                    text = "FileHeaderText.kt",
                    maxWidthPx = 0f,
                    measure = ::monospaceMeasure,
                ) shouldBe "FileHeaderText.kt"
            }

        }

    }

})
