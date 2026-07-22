package com.codymikol.data.diff

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class HunkSpec : DescribeSpec({

    describe("Hunk") {

        describe("make") {

            describe("When the from and to file line number starts diverge") {

                val hunkLines = listOf(
                    "@@ -20,3 +25,3 @@",
                    " unchanged line",
                    "-removed line",
                    "+added line",
                )

                val hunk = Hunk.make(hunkLines)

                it("should number the unchanged line using the from and to starts") {
                    hunk.lines[0].originalLineNumber shouldBe 20U
                    hunk.lines[0].newLineNumber shouldBe 25U
                }

                it("should number the removed line using the from start only") {
                    hunk.lines[1].originalLineNumber shouldBe 21U
                    hunk.lines[1].newLineNumber shouldBe null
                }

                it("should number the added line using the to start only") {
                    hunk.lines[2].originalLineNumber shouldBe null
                    hunk.lines[2].newLineNumber shouldBe 26U
                }

            }

            describe("When the header omits the line counts") {

                val hunkLines = listOf(
                    "@@ -20 +25 @@",
                    " unchanged line",
                )

                val hunk = Hunk.make(hunkLines)

                it("should number the unchanged line using the from and to starts") {
                    hunk.lines[0].originalLineNumber shouldBe 20U
                    hunk.lines[0].newLineNumber shouldBe 25U
                }

            }

        }

    }

})
