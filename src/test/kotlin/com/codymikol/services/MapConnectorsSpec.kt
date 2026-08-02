package com.codymikol.services

import com.codymikol.data.map.CommitGraphNode
import com.codymikol.data.map.MapConnector
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.util.Date

private fun node(sha: String, vararg parents: String) = CommitGraphNode(
    sha = sha,
    shortSha = sha.take(7),
    shortMessage = "message $sha",
    authorName = "Author",
    authorEmail = "author@example.com",
    date = Date(0),
    parentShas = parents.toList(),
)

class MapConnectorsSpec : DescribeSpec({

    describe("MapConnectors") {

        describe("compute") {

            it("should connect a linear stretch of commits with same-lane connectors") {
                val commits = listOf(node("C", "B"), node("B", "A"), node("A"))
                val lanesBySha = mapOf("C" to 0, "B" to 0, "A" to 0)
                val rowsBySha = mapOf("C" to 0, "B" to 1, "A" to 2)

                val connectors = MapConnectors.compute(commits, lanesBySha, rowsBySha)

                connectors shouldBe listOf(
                    MapConnector(childRow = 0, childLane = 0, parentRow = 1, parentLane = 0),
                    MapConnector(childRow = 1, childLane = 0, parentRow = 2, parentLane = 0),
                )
            }

            it("should connect a merge commit's extra parent to its own split-off lane") {
                val commits = listOf(node("M", "main1", "side1"), node("main1"), node("side1"))
                val lanesBySha = mapOf("M" to 0, "main1" to 0, "side1" to 1)
                // side1 is alone in lane 1, so it packs to row 0 rather than its
                // global list row of 2 - the connector reaches its packed position.
                val rowsBySha = mapOf("M" to 0, "main1" to 1, "side1" to 0)

                val connectors = MapConnectors.compute(commits, lanesBySha, rowsBySha)

                connectors shouldBe listOf(
                    MapConnector(childRow = 0, childLane = 0, parentRow = 1, parentLane = 0),
                    MapConnector(childRow = 0, childLane = 0, parentRow = 0, parentLane = 1),
                )
            }

            it("should converge two lanes onto the same ancestor rather than overlap silently") {
                val commits = listOf(node("main1", "Base"), node("side1", "Base"), node("Base"))
                val lanesBySha = mapOf("main1" to 0, "side1" to 1, "Base" to 0)
                val rowsBySha = mapOf("main1" to 0, "side1" to 0, "Base" to 1)

                val connectors = MapConnectors.compute(commits, lanesBySha, rowsBySha)

                connectors shouldBe listOf(
                    MapConnector(childRow = 0, childLane = 0, parentRow = 1, parentLane = 0),
                    MapConnector(childRow = 0, childLane = 1, parentRow = 1, parentLane = 0),
                )
            }

            it("should draw no connector for a parent pagination hasn't loaded yet") {
                val commits = listOf(node("C", "NotLoaded"))
                val lanesBySha = mapOf("C" to 0)
                val rowsBySha = mapOf("C" to 0)

                val connectors = MapConnectors.compute(commits, lanesBySha, rowsBySha)

                connectors shouldBe emptyList()
            }
        }
    }
})
