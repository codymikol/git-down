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

class LaneAssignerSpec : DescribeSpec({

    describe("LaneAssigner") {

        describe("assign") {

            it("should keep every commit in a linear history on the same lane") {
                val assigner = LaneAssigner()
                val commits = listOf(
                    node("C", "B"),
                    node("B", "A"),
                    node("A"),
                )

                val lanes = assigner.assign(commits)

                lanes shouldBe listOf(0, 0, 0)
            }

            it("should give the merge side a new lane and reclaim it after rejoin") {
                val assigner = LaneAssigner()
                // M merges "side" onto "main"; both rejoin at Base, freeing
                // side's lane while main's lane (0) stays active awaiting
                // Root. Unrelated is a fresh, non-converging root commit
                // that should reuse the freed lane rather than allocate a
                // new one.
                val commits = listOf(
                    node("M", "main1", "side1"),
                    node("side1", "Base"),
                    node("main1", "Base"),
                    node("Base", "Root"),
                    node("Unrelated"),
                    node("Root"),
                )

                val lanes = assigner.assign(commits)

                lanes shouldBe listOf(0, 1, 0, 0, 1, 0)
            }

            it("should free a dead-end fork's lane once it runs out of parents, and reuse it") {
                val assigner = LaneAssigner()
                // F forks off "main" into "dead", which has no further
                // parents. Main keeps going on lane 0; dead's lane (1)
                // should free immediately and be reused by Other.
                val commits = listOf(
                    node("F", "main1", "dead"),
                    node("dead"),
                    node("main1", "Root"),
                    node("Other"),
                    node("Root"),
                )

                val lanes = assigner.assign(commits)

                lanes shouldBe listOf(0, 1, 0, 1, 0)
            }

            it("should keep the leftmost lane and free the rest when 3+ lanes converge") {
                val assigner = LaneAssigner()
                // M is a three-way octopus merge of a, b, c, each of which
                // is a direct child of the same Ancestor. All three lanes
                // should collapse onto the leftmost (0) at Ancestor.
                val commits = listOf(
                    node("M", "a", "b", "c"),
                    node("a", "Ancestor"),
                    node("b", "Ancestor"),
                    node("c", "Ancestor"),
                    node("Ancestor"),
                )

                val lanes = assigner.assign(commits)

                lanes shouldBe listOf(0, 0, 1, 2, 0)
            }

            it("should give a new commit the leftmost free lane, not lane 0 unconditionally") {
                val assigner = LaneAssigner()
                // F forks lane 0 into main0 (stays lane 0, stays active) and
                // dead1 (lane 1, dies immediately). With lane 0 still busy,
                // a fresh root commit must land on the only free lane (1),
                // not lane 0.
                val commits = listOf(
                    node("F", "main0", "dead1"),
                    node("dead1"),
                    node("main0", "mainParent"),
                    node("NewRoot"),
                    node("mainParent"),
                )

                val lanes = assigner.assign(commits)

                lanes shouldBe listOf(0, 1, 0, 1, 0)
            }

            it("should assign identical lanes whether a page is walked in one call or split across two") {
                val commits = listOf(
                    node("M", "main1", "side1"),
                    node("side1", "Base"),
                    node("main1", "Base"),
                    node("Base", "Root"),
                    node("Unrelated"),
                    node("Root"),
                )

                val oneCall = LaneAssigner().assign(commits)

                val paged = LaneAssigner().let { assigner ->
                    assigner.assign(commits.subList(0, 3)) + assigner.assign(commits.subList(3, commits.size))
                }

                paged shouldBe oneCall
            }
        }
    }
})
