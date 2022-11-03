package data.diff

import com.codymikol.data.diff.HunkHeader
import com.codymikol.data.diff.LineModification
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class HunkHeaderSpec : DescribeSpec({

    describe("HunkHeader") {

        describe("make") {

            describe("When the removed and added both have two values") {

                val testDelimiter = "@@ -1,2 +3,4 @@"

                val hunkHeader = HunkHeader.make(testDelimiter)

                it("should have the correct deletedLineStart") {
                    hunkHeader.fromFileLineNumbersStart shouldBe 1U
                }

                it("should have the correct fromFileLineModification") {
                    hunkHeader.fromFileLineModification shouldBe LineModification.Removed
                }

                it("should have the correct deletedLineEnd") {
                    hunkHeader.fromFileLineNumbersCount shouldBe 2U
                }

                it("should have the correct createdLineStart") {
                    hunkHeader.toFileLineNumbersStart shouldBe 3U
                }

                it("should have the correct createdLineEnd") {
                    hunkHeader.toFileLineNumbersCount shouldBe 4U
                }

            }

            describe("When the removed lines have a single value") {

                val testDelimiter = "@@ -1 +3,4 @@"

                val hunkHeader = HunkHeader.make(testDelimiter)

                it("should have the correct deletedLineStart") {
                    hunkHeader.fromFileLineNumbersStart shouldBe 1U
                }

                it("should have the correct deletedLineEnd") {
                    hunkHeader.fromFileLineNumbersCount shouldBe null
                }

                it("should have the correct createdLineStart") {
                    hunkHeader.toFileLineNumbersStart shouldBe 3U
                }

                it("should have the correct createdLineEnd") {
                    hunkHeader.toFileLineNumbersCount shouldBe 4U
                }

            }

            describe("When the added lines have a single value") {

                val testDelimiter = "@@ -0,0 +1 @@"

                val hunkHeader = HunkHeader.make(testDelimiter)

                it("should have the correct deletedLineStart") {
                    hunkHeader.fromFileLineNumbersStart shouldBe 0U
                }

                it("should have the correct deletedLineEnd") {
                    hunkHeader.fromFileLineNumbersCount shouldBe 0U
                }

                it("should have the correct createdLineStart") {
                    hunkHeader.toFileLineNumbersStart shouldBe 1U
                }

                it("should have the correct createdLineEnd") {
                    hunkHeader.toFileLineNumbersCount shouldBe null
                }

            }

            describe("When both added and removed have a single value") {

                val testDelimiter = "@@ -1 +1 @@"

                val hunkHeader = HunkHeader.make(testDelimiter)

                it("should have the correct deletedLineStart") {
                    hunkHeader.fromFileLineNumbersStart shouldBe 1U
                }

                it("should have the correct deletedLineEnd") {
                    hunkHeader.fromFileLineNumbersCount shouldBe null
                }

                it("should have the correct createdLineStart") {
                    hunkHeader.toFileLineNumbersStart shouldBe 1U
                }

                it("should have the correct toFileLineNumbersCount") {
                    hunkHeader.toFileLineNumbersCount shouldBe null
                }

            }
            
        }

    }

})
