package com.codymikol.state

import com.codymikol.repository.TestRepository.Companion.createTestRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

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
    }
})
