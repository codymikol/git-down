package com.codymikol.services

import com.codymikol.extensions.listLocalBranches
import com.codymikol.repository.TestRepository.Companion.createTestRepository
import com.codymikol.state.GitDownState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class BranchTipOrdererSpec : DescribeSpec({

    describe("BranchTipOrderer") {

        beforeContainer {
            GitDownState.git.value.close()
        }

        describe("a default branch and two side branches") {

            // early/ diverges at the root, so mainline has gained two commits since
            // their shared history; late/ diverges at the mainline tip, gaining none.
            // Ordered by that gap, late/ sits nearer the mainline than early/.
            val initial = createTestRepository()
                .addFile("a.txt", "a")
                .stageAll()
                .commitAll("root")

            val defaultBranchName = initial.git.repository.branch

            autoClose(
                initial
                    .createBranch("early")
                    .checkout("early")
                    .appendToFile("a.txt", "early")
                    .stageAll()
                    .commitAll("early-1")
                    .checkout(defaultBranchName)
                    .appendToFile("a.txt", "b")
                    .stageAll()
                    .commitAll("main-2")
                    .appendToFile("a.txt", "c")
                    .stageAll()
                    .commitAll("main-3")
                    .createBranch("late")
                    .checkout("late")
                    .appendToFile("a.txt", "late")
                    .stageAll()
                    .commitAll("late-1")
                    .checkout(defaultBranchName)
                    .transferIntoGitDownState()
            )

            it("puts the default tip first, then side tips by merge proximity") {
                val git = GitDownState.git.value

                val order = BranchTipOrderer.order(
                    git,
                    defaultBranchName,
                    git.listLocalBranches(),
                )

                order.map { it.branchNames } shouldBe listOf(
                    listOf(defaultBranchName),
                    listOf("late"),
                    listOf("early"),
                )
            }

            it("falls back to branch-name order when no branch matches the default") {
                val git = GitDownState.git.value

                val order = BranchTipOrderer.order(git, "does-not-exist", git.listLocalBranches())

                order.map { it.branchNames.first() } shouldBe listOf(defaultBranchName, "early", "late").sorted()
            }
        }

        describe("two branches sharing a tip") {

            val initial = createTestRepository()
                .addFile("a.txt", "a")
                .stageAll()
                .commitAll("root")

            val defaultBranchName = initial.git.repository.branch

            autoClose(
                initial
                    .createBranch("alias")
                    .transferIntoGitDownState()
            )

            it("collects every branch name on the one shared tip") {
                val git = GitDownState.git.value

                val order = BranchTipOrderer.order(
                    git,
                    defaultBranchName,
                    git.listLocalBranches(),
                )

                order.map { it.branchNames } shouldBe listOf(
                    listOf("alias", defaultBranchName).sorted(),
                )
            }
        }

        describe("no branches") {

            it("returns an empty ordering") {
                BranchTipOrderer.order(GitDownState.git.value, "main", emptyList()) shouldBe emptyList()
            }
        }
    }
})
