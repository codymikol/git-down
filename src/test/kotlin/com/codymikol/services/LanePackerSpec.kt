package com.codymikol.services

import com.codymikol.data.map.CommitGraphNode
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

class LanePackerSpec : DescribeSpec({

    describe("LanePacker") {

        describe("pack") {

            it("should pack a linear single-lane history down consecutive rows") {
                val commits = listOf(node("C", "B"), node("B", "A"), node("A"))
                val lanesBySha = mapOf("C" to 0, "B" to 0, "A" to 0)

                val rows = LanePacker.pack(commits, lanesBySha)

                rows shouldBe mapOf("C" to 0, "B" to 1, "A" to 2)
            }

            it("should pack each lane tight to the top independently of list order") {
                // side1 sits between two lane-0 commits in the list, yet it is the
                // only commit in lane 1 so it packs to row 0 - its lane's top - not
                // to its global position in the list.
                val commits = listOf(
                    node("M", "main1", "side1"),
                    node("side1", "Base"),
                    node("main1", "Base"),
                    node("Base"),
                )
                val lanesBySha = mapOf("M" to 0, "side1" to 1, "main1" to 0, "Base" to 0)

                val rows = LanePacker.pack(commits, lanesBySha)

                rows shouldBe mapOf("M" to 0, "side1" to 0, "main1" to 1, "Base" to 2)
            }

            it("should skip a commit that has no lane assigned yet") {
                val commits = listOf(node("C", "B"), node("B", "A"))
                val lanesBySha = mapOf("C" to 0)

                val rows = LanePacker.pack(commits, lanesBySha)

                rows shouldBe mapOf("C" to 0)
            }
        }
    }
})
