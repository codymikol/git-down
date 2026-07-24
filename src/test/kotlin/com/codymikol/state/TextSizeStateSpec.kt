package com.codymikol.state

import com.codymikol.data.settings.AppSettings
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class TextSizeStateSpec : DescribeSpec({

    beforeTest {
        GitDownState.headerTextSize.value = AppSettings.DEFAULT_HEADER_TEXT_SIZE
        GitDownState.bodyTextSize.value = AppSettings.DEFAULT_BODY_TEXT_SIZE
    }

    describe("GitDownState text size") {

        it("starts with a header text size of 20") {
            GitDownState.headerTextSize.value shouldBe 20
        }

        it("starts with a body text size of 12") {
            GitDownState.bodyTextSize.value shouldBe 12
        }

        it("increases both header and body text size by 2px") {
            GitDownState.headerTextSize.value = 20
            GitDownState.bodyTextSize.value = 12

            GitDownState.increaseTextSize()

            GitDownState.headerTextSize.value shouldBe 22
            GitDownState.bodyTextSize.value shouldBe 14
        }

        it("decreases both header and body text size by 2px") {
            GitDownState.headerTextSize.value = 20
            GitDownState.bodyTextSize.value = 12

            GitDownState.decreaseTextSize()

            GitDownState.headerTextSize.value shouldBe 18
            GitDownState.bodyTextSize.value shouldBe 10
        }

        it("does not decrease body text size below the minimum of 8px") {
            GitDownState.headerTextSize.value = 20
            GitDownState.bodyTextSize.value = 8

            GitDownState.decreaseTextSize()

            GitDownState.bodyTextSize.value shouldBe 8
        }

        it("does not increase header text size above the maximum of 40px") {
            GitDownState.headerTextSize.value = 40
            GitDownState.bodyTextSize.value = 12

            GitDownState.increaseTextSize()

            GitDownState.headerTextSize.value shouldBe 40
        }

    }

})
