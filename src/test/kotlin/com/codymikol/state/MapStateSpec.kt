package com.codymikol.state

import com.codymikol.data.map.CommitGraphNode
import com.codymikol.repository.TestRepository.Companion.createTestRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.util.Date

class MapStateSpec : DescribeSpec({

    describe("MapState") {

        beforeContainer {
            GitDownState.git.value.close()
            MapState.reset()
        }

        describe("a branch with four commits") {

            autoClose(
                createTestRepository()
                    .addFile("a.txt", "a")
                    .stageAll()
                    .commitAll("commit 1")
                    .appendToFile("a.txt", "b")
                    .stageAll()
                    .commitAll("commit 2")
                    .appendToFile("a.txt", "c")
                    .stageAll()
                    .commitAll("commit 3")
                    .appendToFile("a.txt", "d")
                    .stageAll()
                    .commitAll("commit 4")
                    .transferIntoGitDownState()
            )

            it("should expose exactly one local branch") {
                MapState.branches.value shouldHaveSize 1
            }

            it("should load every commit into commits and mark the walk exhausted") {
                MapState.loadMore()

                MapState.commits shouldHaveSize 4
                MapState.hasMore shouldBe false
            }

            it("should not duplicate rows once the walk is exhausted") {
                MapState.loadMore()
                MapState.loadMore()

                MapState.commits shouldHaveSize 4
            }

            it("should assign a lane to every loaded commit") {
                MapState.loadMore()

                MapState.commits.map { MapState.lanesBySha[it.sha] } shouldBe listOf(0, 0, 0, 0)
            }

            it("should clear loaded state on reset") {
                MapState.loadMore()

                MapState.reset()

                MapState.commits.isEmpty() shouldBe true
                MapState.lanesBySha.isEmpty() shouldBe true
                MapState.hasMore shouldBe true
            }
        }

        describe("two branches sharing history") {

            autoClose(
                createTestRepository()
                    .addFile("a.txt", "a")
                    .stageAll()
                    .commitAll("commit 1")
                    .createBranch("feature")
                    .appendToFile("a.txt", "b")
                    .stageAll()
                    .commitAll("commit 2")
                    .transferIntoGitDownState()
            )

            it("should load a commit reachable from both branch tips exactly once (#263)") {
                MapState.branches.value shouldHaveSize 2

                MapState.loadMore()

                MapState.commits.map { it.shortMessage } shouldBe listOf("commit 2", "commit 1")
            }
        }

        describe("a branch with a merge commit") {

            val initial = createTestRepository()
                .addFile("a.txt", "a")
                .stageAll()
                .commitAll("init")

            val defaultBranchName = initial.git.repository.branch

            autoClose(
                initial
                    .createBranch("feature")
                    .checkout("feature")
                    .appendToFile("a.txt", "b")
                    .stageAll()
                    .commitAll("feature work")
                    .checkout(defaultBranchName)
                    .merge("feature", "merge feature")
                    .transferIntoGitDownState()
            )

            it("should position the merge's side branch on a different lane than the mainline") {
                MapState.loadMore()

                val mainlineLanes = MapState.commits.filter { !it.isMergeCommit && it.shortMessage != "feature work" }
                    .map { MapState.lanesBySha[it.sha] }
                val sideLane = MapState.lanesBySha[MapState.commits.single { it.shortMessage == "feature work" }.sha]

                mainlineLanes.toSet() shouldBe setOf(0)
                sideLane shouldBe 1
            }
        }

        describe("shouldLoadMore") {

            it("should return false once the walk has no more commits to load") {
                MapState.reset()
                MapState.commits.addAll(dummyCommits(4))
                MapState.hasMore = false

                MapState.shouldLoadMore(3) shouldBe false
            }

            it("should return false when the last-visible index is far from the loaded end") {
                MapState.reset()
                MapState.commits.addAll(dummyCommits(30))

                MapState.shouldLoadMore(0) shouldBe false
            }

            it("should return true when the last-visible index nears the loaded end and more remain") {
                MapState.reset()
                MapState.commits.addAll(dummyCommits(30))

                MapState.shouldLoadMore(28) shouldBe true
            }

            it("should return true exactly at the load-more threshold boundary") {
                MapState.reset()
                MapState.commits.addAll(dummyCommits(30))

                MapState.shouldLoadMore(30 - MapState.LOAD_MORE_THRESHOLD) shouldBe true
            }

            it("should return true when nothing has loaded yet and more is assumed available") {
                MapState.reset()

                MapState.shouldLoadMore(0) shouldBe true
            }
        }

        describe("node selection") {

            it("starts with no node selected") {
                MapState.reset()

                MapState.selectedNodeSha.value shouldBe null
            }

            it("selects a node when toggled from empty") {
                MapState.reset()

                MapState.toggleSelectedNode("sha1")

                MapState.selectedNodeSha.value shouldBe "sha1"
            }

            it("clears the selection when the same node is toggled again") {
                MapState.reset()
                MapState.toggleSelectedNode("sha1")

                MapState.toggleSelectedNode("sha1")

                MapState.selectedNodeSha.value shouldBe null
            }

            it("replaces the selection when a different node is toggled") {
                MapState.reset()
                MapState.toggleSelectedNode("sha1")

                MapState.toggleSelectedNode("sha2")

                MapState.selectedNodeSha.value shouldBe "sha2"
            }

            it("clears the selection on reset") {
                MapState.toggleSelectedNode("sha1")

                MapState.reset()

                MapState.selectedNodeSha.value shouldBe null
            }

            it("selects a node when explicitly selected") {
                MapState.reset()

                MapState.selectNode("sha1")

                MapState.selectedNodeSha.value shouldBe "sha1"
            }

            it("keeps a node selected when it is selected again") {
                MapState.reset()
                MapState.selectNode("sha1")

                MapState.selectNode("sha1")

                MapState.selectedNodeSha.value shouldBe "sha1"
            }

            it("replaces the selection when a different node is selected") {
                MapState.reset()
                MapState.selectNode("sha1")

                MapState.selectNode("sha2")

                MapState.selectedNodeSha.value shouldBe "sha2"
            }

            it("resolves the selected node from the loaded commits") {
                MapState.reset()
                MapState.commits.addAll(dummyCommits(3))
                MapState.selectNode("sha2")

                MapState.selectedNode()?.shortMessage shouldBe "commit 2"
            }

            it("resolves to null when nothing is selected") {
                MapState.reset()
                MapState.commits.addAll(dummyCommits(3))

                MapState.selectedNode() shouldBe null
            }

            it("resolves to null when the selected sha is not loaded") {
                MapState.reset()
                MapState.commits.addAll(dummyCommits(3))
                MapState.selectNode("sha-missing")

                MapState.selectedNode() shouldBe null
            }
        }

        describe("branchTipsBySha") {

            autoClose(
                createTestRepository()
                    .addFile("a.txt", "a")
                    .stageAll()
                    .commitAll("commit 1")
                    .transferIntoGitDownState()
            )

            it("maps a branch's tip commit sha to that branch's name") {
                val branch = MapState.branches.value.single()
                val tipSha = branch.objectId.name

                MapState.branchTipsBySha.value[tipSha] shouldBe listOf(branch.name.removePrefix("refs/heads/"))
            }

            it("collects every branch name pointing at a shared tip commit") {
                val original = MapState.branches.value.single()
                val tipSha = original.objectId.name
                GitDownState.git.value.branchCreate().setName("feature").call()
                // branches/branchTipsBySha are derivedStateOf on GitDownState.git,
                // which only recomputes off lastRequestedUpdateTimestamp (see
                // GitDownState.git and Git.command in GitExtensions.kt) - bump it
                // manually since branchCreate() above bypassed that command() wrapper.
                // Incrementing (rather than re-reading the clock) guarantees a value
                // change even if this runs within the same clock tick as the initial
                // read, which mutableStateOf's equality check would otherwise ignore.
                GitDownState.lastRequestedUpdateTimestamp.value += 1

                val names = MapState.branchTipsBySha.value[tipSha]

                names shouldContainExactlyInAnyOrder listOf(
                    original.name.removePrefix("refs/heads/"),
                    "feature",
                )
            }

            it("has no entry for a commit that is nobody's branch tip") {
                MapState.branchTipsBySha.value["not-a-real-sha"] shouldBe null
            }
        }

        describe("more commits than one page") {

            val repo = createTestRepository().addFile("a.txt", "0").stageAll().commitAll("commit 0")
            repeat(64) { i ->
                repo.appendToFile("a.txt", "$i")
                repo.stageAll()
                repo.commitAll("commit ${i + 1}")
            }
            autoClose(repo.transferIntoGitDownState())

            // Regression guard for #256: a loop driven by shouldLoadMore() (mirroring
            // the map screen's catch-up effect) must converge in a small, bounded number
            // of iterations rather than spin forever - it must NOT be re-launched by a key
            // that loadMore() itself mutates (commits.size / hasMore), the mechanism that
            // previously froze the UI.
            it("should catch up to a deep scroll position in a bounded number of pages") {
                val deepLastVisibleIndex = 60

                var iterations = 0
                while (MapState.shouldLoadMore(deepLastVisibleIndex)) {
                    check(iterations < 10) { "loadMore loop did not converge - possible regression of #256" }
                    MapState.loadMore()
                    iterations++
                }

                iterations shouldBe 3
                MapState.hasMore shouldBe false
                MapState.commits shouldHaveSize 65
            }
        }
    }
})

private fun dummyCommits(count: Int) = (1..count).map {
    CommitGraphNode(
        sha = "sha$it",
        shortSha = "sha$it",
        shortMessage = "commit $it",
        authorName = "author",
        authorEmail = "author@example.com",
        date = Date(),
        parentShas = emptyList(),
    )
}
