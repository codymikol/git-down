package com.codymikol.state

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class MapScrollStateSpec : DescribeSpec({

    describe("MapScrollState") {

        beforeEach { MapScrollState.reset() }

        describe("scrollBy") {

            it("should accumulate the offset across calls") {
                MapScrollState.scrollBy(48f, maxOffsetPx = 1000f)
                MapScrollState.scrollBy(32f, maxOffsetPx = 1000f)

                MapScrollState.offsetPx shouldBe 80f
            }

            it("should not scroll past zero") {
                MapScrollState.scrollBy(-20f, maxOffsetPx = 1000f)

                MapScrollState.offsetPx shouldBe 0f
            }

            it("should not scroll past the given max offset") {
                MapScrollState.scrollBy(5000f, maxOffsetPx = 200f)

                MapScrollState.offsetPx shouldBe 200f
            }
        }

        describe("firstVisibleIndex") {

            it("should be zero when nothing has scrolled") {
                MapScrollState.firstVisibleIndex(rowHeightPx = 48f) shouldBe 0
            }

            it("should floor the offset divided by row height") {
                MapScrollState.scrollBy(100f, maxOffsetPx = 1000f)

                MapScrollState.firstVisibleIndex(rowHeightPx = 48f) shouldBe 2
            }
        }

        describe("visibleRowCount") {

            it("should cover the viewport with one extra row for partial scroll") {
                MapScrollState.visibleRowCount(viewportHeightPx = 480f, rowHeightPx = 48f) shouldBe 11
            }

            it("should round up a viewport that doesn't divide evenly by row height") {
                MapScrollState.visibleRowCount(viewportHeightPx = 100f, rowHeightPx = 48f) shouldBe 4
            }
        }

        describe("reset") {

            it("should return the offset to zero") {
                MapScrollState.scrollBy(300f, maxOffsetPx = 1000f)

                MapScrollState.reset()

                MapScrollState.offsetPx shouldBe 0f
            }
        }
    }
})
