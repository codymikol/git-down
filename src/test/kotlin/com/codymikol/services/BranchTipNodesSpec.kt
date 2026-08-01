package com.codymikol.services

import com.codymikol.data.map.BranchTipNode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class BranchTipNodesSpec : DescribeSpec({

    describe("BranchTipNodes") {

        describe("place") {

            it("places each ordered tip in the lane its loaded commit occupies") {
                val tips = listOf(
                    OrderedBranchTip("A", listOf("main")),
                    OrderedBranchTip("B", listOf("foo")),
                )
                val lanesBySha = mapOf("A" to 0, "B" to 1)

                BranchTipNodes.place(tips, lanesBySha) shouldBe listOf(
                    BranchTipNode("A", 0),
                    BranchTipNode("B", 1),
                )
            }

            it("skips a tip whose commit pagination has not loaded yet") {
                val tips = listOf(
                    OrderedBranchTip("A", listOf("main")),
                    OrderedBranchTip("C", listOf("bar")),
                )
                val lanesBySha = mapOf("A" to 0)

                BranchTipNodes.place(tips, lanesBySha) shouldBe listOf(
                    BranchTipNode("A", 0),
                )
            }

            it("keeps only the first tip when two share a lane, preserving mainline priority") {
                val tips = listOf(
                    OrderedBranchTip("A", listOf("main")),
                    OrderedBranchTip("B", listOf("foo")),
                )
                val lanesBySha = mapOf("A" to 0, "B" to 0)

                BranchTipNodes.place(tips, lanesBySha) shouldBe listOf(
                    BranchTipNode("A", 0),
                )
            }
        }
    }
})
