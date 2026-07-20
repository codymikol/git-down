package com.codymikol.services

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.awt.Dimension

class WindowSizeServiceSpec : DescribeSpec({

    describe("WindowSizeService.getDefaultWindowSize") {

        it("sizes the window to 80% of a large screen") {
            val service = WindowSizeService()
            service.screenSizeProvider = { Dimension(1920, 1080) }

            service.getDefaultWindowSize() shouldBe Dimension(1536, 864)
        }

        it("scales down for a smaller screen while staying above the minimum") {
            val service = WindowSizeService()
            service.screenSizeProvider = { Dimension(1024, 768) }

            service.getDefaultWindowSize() shouldBe Dimension(819, 614)
        }

        it("clamps to the minimum usable size when the screen is smaller than it") {
            val service = WindowSizeService()
            service.screenSizeProvider = { Dimension(640, 480) }

            service.getDefaultWindowSize() shouldBe Dimension(WindowSizeService.MIN_WIDTH, WindowSizeService.MIN_HEIGHT)
        }

    }

})
