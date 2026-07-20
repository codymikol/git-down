package com.codymikol.components.stash

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class StashBottomToolbarSpec : DescribeSpec({

    describe("stashToolbarButtonClicked") {

        it("prints the button text to the console") {
            val originalOut = System.out
            val captured = ByteArrayOutputStream()
            try {
                System.setOut(PrintStream(captured))
                stashToolbarButtonClicked("Apply")
            } finally {
                System.setOut(originalOut)
            }
            captured.toString().trim() shouldBe "Apply"
        }

    }

})
