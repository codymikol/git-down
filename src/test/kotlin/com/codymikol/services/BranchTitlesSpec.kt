package com.codymikol.services

import com.codymikol.data.map.BranchTitle
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class BranchTitlesSpec : DescribeSpec({

    describe("BranchTitles") {

        describe("place") {

            it("places each ordered tip's names in the lane its loaded commit occupies") {
                val tips = listOf(
                    OrderedBranchTip("A", listOf("main")),
                    OrderedBranchTip("B", listOf("foo")),
                )
                val lanesBySha = mapOf("A" to 0, "B" to 1)

                BranchTitles.place(tips, lanesBySha) shouldBe listOf(
                    BranchTitle(0, listOf("main")),
                    BranchTitle(1, listOf("foo")),
                )
            }

            it("carries every name of a tip shared by several branches") {
                val tips = listOf(OrderedBranchTip("A", listOf("main", "release")))
                val lanesBySha = mapOf("A" to 0)

                BranchTitles.place(tips, lanesBySha) shouldBe listOf(
                    BranchTitle(0, listOf("main", "release")),
                )
            }

            it("skips a tip whose commit pagination has not loaded yet") {
                val tips = listOf(
                    OrderedBranchTip("A", listOf("main")),
                    OrderedBranchTip("C", listOf("bar")),
                )
                val lanesBySha = mapOf("A" to 0)

                BranchTitles.place(tips, lanesBySha) shouldBe listOf(
                    BranchTitle(0, listOf("main")),
                )
            }

            it("keeps only the first tip when two share a lane, preserving mainline priority") {
                val tips = listOf(
                    OrderedBranchTip("A", listOf("main")),
                    OrderedBranchTip("B", listOf("foo")),
                )
                val lanesBySha = mapOf("A" to 0, "B" to 0)

                BranchTitles.place(tips, lanesBySha) shouldBe listOf(
                    BranchTitle(0, listOf("main")),
                )
            }
        }
    }
})
