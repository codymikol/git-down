package com.codymikol.components.stash

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class StashBottomToolbarSpec : DescribeSpec({

    describe("dropStashConfirmationMessage") {

        it("names the stash being dropped") {
            dropStashConfirmationMessage("WIP on main: my stash") shouldBe
                "This will permanently delete the stash \"WIP on main: my stash\", are you sure?"
        }

    }

    describe("applyStashConfirmationMessage") {

        it("names the stash being applied") {
            applyStashConfirmationMessage("WIP on main: my stash") shouldBe
                "This will apply the stash \"WIP on main: my stash\" to your working directory, are you sure?"
        }

    }

})
