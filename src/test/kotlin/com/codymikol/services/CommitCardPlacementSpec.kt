package com.codymikol.services

import com.codymikol.data.map.MapDimensions
import com.codymikol.data.map.MapPoint
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CommitCardPlacementSpec : DescribeSpec({

    val dimensions = MapDimensions()

    describe("CommitCardPlacement") {

        describe("place") {

            it("puts the card at the lane's node, offset back by half the tab so the tab sits over the node") {
                // lane 0, first row, no scroll: left edge = gutterX - cardTabSize/2, top = 0
                CommitCardPlacement.place(
                    selectedIndex = 0,
                    lane = 0,
                    firstVisibleIndex = 0,
                    firstVisibleOffset = 0f,
                    dimensions = dimensions,
                ) shouldBe MapPoint(dimensions.gutterX - dimensions.cardTabSize / 2f, 0f)
            }

            it("shifts the card right by one lane width per lane so it tracks its node's lane") {
                CommitCardPlacement.place(
                    selectedIndex = 0,
                    lane = 2,
                    firstVisibleIndex = 0,
                    firstVisibleOffset = 0f,
                    dimensions = dimensions,
                ) shouldBe MapPoint(
                    2 * dimensions.laneWidth + dimensions.gutterX - dimensions.cardTabSize / 2f,
                    0f,
                )
            }

            it("drops the card down one row height per row below the first visible, minus the scroll offset") {
                // selected row 5, first visible row 3 scrolled 10dp up: top = 2*rowHeight - 10
                CommitCardPlacement.place(
                    selectedIndex = 5,
                    lane = 1,
                    firstVisibleIndex = 3,
                    firstVisibleOffset = 10f,
                    dimensions = dimensions,
                ) shouldBe MapPoint(
                    dimensions.laneWidth + dimensions.gutterX - dimensions.cardTabSize / 2f,
                    2 * dimensions.rowHeight - 10f,
                )
            }
        }
    }
})
