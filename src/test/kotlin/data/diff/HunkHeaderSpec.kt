package data.diff

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class HunkHeaderSpec : DescribeSpec({

    describe("HunkHeader") {

        describe("make") {

            val testDelimiter = "@@ -1,2 +3,4 @@"
            
            val hunkHeader = HunkHeader.make(testDelimiter)

            it("should have the correct deletedLineStart") {
                hunkHeader.deletedLineStart shouldBe 1
            }

            it("should have the correct deletedLineEnd") {
                hunkHeader.deletedLineEnd shouldBe 2
            }
            
            it("should have the correct createdLineStart") {
                hunkHeader.createdLineStart shouldBe 3
            }
            
            it("should have the correct createdLineEnd") {
                hunkHeader.createdLineEnd shouldBe 4
            }
            
        }

    }

})
