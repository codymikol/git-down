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

        describe("maxLoadedRowCount") {

            it("should return zero when nothing has loaded") {
                MapState.reset()

                MapState.maxLoadedRowCount.value shouldBe 0
            }

            it("should return the largest loaded commit count across branches") {
                MapState.reset()
                MapState.commitsByBranch["refs/heads/main"] = dummyCommits(4)
                MapState.commitsByBranch["refs/heads/feature"] = dummyCommits(30)

                MapState.maxLoadedRowCount.value shouldBe 30
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
