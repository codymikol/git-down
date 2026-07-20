package com.codymikol.components.commit

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CommitBottomToolbarSpec : DescribeSpec({

    describe("getObjectName") {

        describe("when the head is detached") {

            it("should return HEAD regardless of branch name length") {
                getObjectName(true, "a".repeat(50)) shouldBe "HEAD"
            }

        }

        describe("when the head is not detached") {

            it("should return the branch name unchanged when short") {
                getObjectName(false, "main") shouldBe "main"
            }

            it("should return the branch name unchanged even when long, leaving truncation to the UI") {
                val branchName = "a".repeat(50)
                getObjectName(false, branchName) shouldBe branchName
            }

        }

    }

})
