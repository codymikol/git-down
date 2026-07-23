package com.codymikol.components.stash

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class SaveStashDialogSpec : DescribeSpec({

    describe("canSaveStash") {

        it("is false for a blank message") {
            canSaveStash("   ") shouldBe false
        }

        it("is false for an empty message") {
            canSaveStash("") shouldBe false
        }

        it("is true for a non-blank message") {
            canSaveStash("my stash message") shouldBe true
        }

    }

})
