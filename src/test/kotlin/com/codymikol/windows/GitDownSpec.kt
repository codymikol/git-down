package com.codymikol.windows

import androidx.compose.ui.unit.dp
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GitDownSpec : DescribeSpec({

    describe("WindowCornerRadius") {

        it("is a subtle radius applied to the outermost application window") {
            WindowCornerRadius shouldBe 8.dp
        }

    }

})
