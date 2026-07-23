package com.codymikol.state

import com.codymikol.data.map.CommitGraphNode
import com.codymikol.repository.TestRepository.Companion.createTestRepository
import io.kotest.core.spec.style.DescribeSpec
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

            it("should load every commit into commitsByBranch and mark the branch exhausted") {
                val branch = MapState.branches.value.single()

                MapState.loadMore(branch)

                (MapState.commitsByBranch[branch.name] ?: emptyList()) shouldHaveSize 4
                MapState.hasMoreByBranch[branch.name] shouldBe false
            }

            it("should not duplicate rows once a branch is exhausted") {
                val branch = MapState.branches.value.single()

                MapState.loadMore(branch)
                MapState.loadMore(branch)

                (MapState.commitsByBranch[branch.name] ?: emptyList()) shouldHaveSize 4
            }

            it("should clear loaded state on reset") {
                val branch = MapState.branches.value.single()
                MapState.loadMore(branch)

                MapState.reset()

                MapState.commitsByBranch.isEmpty() shouldBe true
                MapState.hasMoreByBranch.isEmpty() shouldBe true
            }
        }

        describe("shouldLoadMore") {

            it("should return false once the branch has no more commits to load") {
                MapState.reset()
                MapState.commitsByBranch["refs/heads/main"] = dummyCommits(4)
                MapState.hasMoreByBranch["refs/heads/main"] = false

                MapState.shouldLoadMore("refs/heads/main", 3) shouldBe false
            }

            it("should return false when the shared last-visible index is far from this branch's loaded end") {
                MapState.reset()
                MapState.commitsByBranch["refs/heads/main"] = dummyCommits(30)
                MapState.hasMoreByBranch["refs/heads/main"] = true

                MapState.shouldLoadMore("refs/heads/main", 0) shouldBe false
            }

            it("should return true when the shared last-visible index nears this branch's loaded end and more remain") {
                MapState.reset()
                MapState.commitsByBranch["refs/heads/main"] = dummyCommits(30)
                MapState.hasMoreByBranch["refs/heads/main"] = true

                MapState.shouldLoadMore("refs/heads/main", 28) shouldBe true
            }

            it("should return true exactly at the load-more threshold boundary") {
                MapState.reset()
                MapState.commitsByBranch["refs/heads/main"] = dummyCommits(30)
                MapState.hasMoreByBranch["refs/heads/main"] = true

                MapState.shouldLoadMore("refs/heads/main", 30 - MapState.LOAD_MORE_THRESHOLD) shouldBe true
            }

            it("should return true when nothing has loaded yet for the branch and more is assumed available") {
                MapState.reset()

                MapState.shouldLoadMore("refs/heads/main", 0) shouldBe true
            }
        }

        describe("a branch with more commits than one page") {

            val repo = createTestRepository().addFile("a.txt", "0").stageAll().commitAll("commit 0")
            repeat(64) { i ->
                repo.appendToFile("a.txt", "$i")
                repo.stageAll()
                repo.commitAll("commit ${i + 1}")
            }
            autoClose(repo.transferIntoGitDownState())

            // Regression guard for #256: a loop driven by shouldLoadMore() (mirroring
            // BranchLane's catch-up effect) must converge in a small, bounded number of
            // iterations rather than spin forever - it must NOT be re-launched by a key
            // that loadMore() itself mutates (commits.size / hasMore), the mechanism that
            // previously froze the UI.
            it("should catch a lane up to a deep scroll position in a bounded number of pages") {
                val branch = MapState.branches.value.single()
                val deepLastVisibleIndex = 60

                var iterations = 0
                while (MapState.shouldLoadMore(branch.name, deepLastVisibleIndex)) {
                    check(iterations < 10) { "loadMore loop did not converge - possible regression of #256" }
                    MapState.loadMore(branch)
                    iterations++
                }

                iterations shouldBe 3
                MapState.hasMoreByBranch[branch.name] shouldBe false
                (MapState.commitsByBranch[branch.name] ?: emptyList()) shouldHaveSize 65
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
        date = Date(),
        parentShas = emptyList(),
    )
}.toMutableList()
