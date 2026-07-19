package com.codymikol.components.commit

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CommitBottomToolbarSpec : DescribeSpec({

    describe("truncateBranchName") {

        describe("when the branch name is within the limit") {

            it("should return the branch name unchanged") {
                truncateBranchName("main") shouldBe "main"
            }

            it("should return a 40 character branch name unchanged") {
                val branchName = "a".repeat(40)
                truncateBranchName(branchName) shouldBe branchName
            }

        }

        describe("when the branch name exceeds the limit") {

            it("should truncate to 40 characters and append an ellipsis") {
                val branchName = "a".repeat(50)
                truncateBranchName(branchName) shouldBe ("a".repeat(40) + "...")
            }

        }

    }

    describe("getObjectName") {

        describe("when the head is detached") {

            it("should return HEAD regardless of branch name length") {
                getObjectName(true, "a".repeat(50)) shouldBe "HEAD"
            }

        }

        describe("when the head is not detached") {

            it("should return the branch name unchanged when within the limit") {
                getObjectName(false, "main") shouldBe "main"
            }

            it("should return the truncated branch name when it exceeds the limit") {
                val branchName = "a".repeat(50)
                getObjectName(false, branchName) shouldBe ("a".repeat(40) + "...")
            }

        }

    }

})
