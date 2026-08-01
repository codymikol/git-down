package com.codymikol.state

import com.codymikol.data.map.MapDimensions
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class MapDebugStateSpec : DescribeSpec({

    describe("MapDebugState") {

        beforeEach {
            MapDebugState.reset()
        }

        it("starts closed with the default dimensions") {
            MapDebugState.isOpen.value shouldBe false
            MapDebugState.dimensions.value shouldBe MapDimensions()
        }

        it("toggles the menu open and closed") {
            MapDebugState.toggle()
            MapDebugState.isOpen.value shouldBe true

            MapDebugState.toggle()
            MapDebugState.isOpen.value shouldBe false
        }

        it("updates a single dimension through a slider without touching the rest") {
            val slider = MapDimensions.sliders.first { it.label == "mainlineGap" }

            MapDebugState.set(slider, 40f)

            MapDebugState.dimensions.value.mainlineGap shouldBe 40f
            // every other value is still its default
            MapDebugState.dimensions.value.copy(mainlineGap = MapDimensions().mainlineGap) shouldBe
                MapDimensions()
        }

        it("resets the dimensions back to their defaults") {
            val slider = MapDimensions.sliders.first { it.label == "laneWidth" }
            MapDebugState.set(slider, 300f)
            MapDebugState.toggle()

            MapDebugState.reset()

            MapDebugState.dimensions.value shouldBe MapDimensions()
            MapDebugState.isOpen.value shouldBe false
        }
    }
})
