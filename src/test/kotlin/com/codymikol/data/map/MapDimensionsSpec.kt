package com.codymikol.data.map

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class MapDimensionsSpec : DescribeSpec({

    describe("branch-title dimensions") {

        it("exposes a titlePadding slider defaulting to 14") {
            val slider = MapDimensions.sliders.firstOrNull { it.label == "titlePadding" }

            slider.shouldNotBeNull()
            slider.get(MapDimensions()) shouldBe 14f
        }

        it("exposes a titleMaxWidth slider") {
            val slider = MapDimensions.sliders.firstOrNull { it.label == "titleMaxWidth" }

            slider.shouldNotBeNull()
            slider.get(MapDimensions()) shouldBe MapDimensions().titleMaxWidth
        }
    }

    describe("MapDimensions.summarize") {

        it("lists every debug slider key with its current value") {
            val summary = MapDimensions.summarize(MapDimensions())

            val lines = summary.lines()
            lines.size shouldBe MapDimensions.sliders.size
            MapDimensions.sliders.forEachIndexed { index, slider ->
                lines[index] shouldBe "${slider.label}: ${MapDimensions.format(slider.get(MapDimensions()))}"
            }
        }

        it("reflects an adjusted value in the summary line") {
            val tuned = MapDimensions().copy(mainlineGap = 40f)

            MapDimensions.summarize(tuned) shouldContain "mainlineGap: 40"
        }
    }
})
