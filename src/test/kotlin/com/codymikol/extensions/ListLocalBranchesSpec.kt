package com.codymikol.extensions

import com.codymikol.repository.TestRepository.Companion.createTestRepository
import com.codymikol.state.GitDownState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class ListLocalBranchesSpec : DescribeSpec({

    describe("Git.listLocalBranches") {

        beforeContainer { GitDownState.git.value.close() }

        describe("a repository with two local branches") {

            autoClose(
                createTestRepository()
                    .addFile("a.txt", "a")
                    .stageAll()
                    .commitAll("init")
                    .createBranch("feature")
                    .transferIntoGitDownState()
            )

            it("should list both branches") {
                val names = GitDownState.git.value.listLocalBranches().map { it.name }
                names shouldHaveSize 2
                names.any { it.endsWith("feature") } shouldBe true
            }
        }

        describe("a repository with no commits") {

            autoClose(createTestRepository().transferIntoGitDownState())

            it("should return an empty list") {
                GitDownState.git.value.listLocalBranches() shouldHaveSize 0
            }
        }
    }
})
