package com.codymikol.windows

import androidx.compose.ui.graphics.Color
import com.codymikol.data.Colors
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class DirectorySelectorSpec : DescribeSpec({

    describe("errorMessageColor") {

        describe("when the selected directory is invalid") {

            it("should return the error color so the message is visible") {
                errorMessageColor(isInvalid = true) shouldBe Colors.FileRemoved
            }

        }

        describe("when the selected directory is not invalid") {

            it("should return a transparent color so the reserved space stays but is invisible") {
                errorMessageColor(isInvalid = false) shouldBe Color.Transparent
            }

        }

    }

})
