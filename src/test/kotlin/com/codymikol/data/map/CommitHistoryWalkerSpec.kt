package com.codymikol.data.map

import com.codymikol.extensions.listLocalBranches
import com.codymikol.repository.TestRepository.Companion.createTestRepository
import com.codymikol.state.GitDownState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class CommitHistoryWalkerSpec : DescribeSpec({

    describe("CommitHistoryWalker") {

        beforeContainer { GitDownState.git.value.close() }

        describe("a branch with five commits") {

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
                    .appendToFile("a.txt", "e")
                    .stageAll()
                    .commitAll("commit 5")
                    .transferIntoGitDownState()
            )

            val branch = GitDownState.git.value.listLocalBranches().single()

            it("should page through history newest first") {
                val walker = CommitHistoryWalker(GitDownState.git.value, branch)

                val firstPage = walker.nextPage(3)
                firstPage.map { it.shortMessage } shouldBe listOf("commit 5", "commit 4", "commit 3")
                walker.hasMore shouldBe true

                val secondPage = walker.nextPage(3)
                secondPage.map { it.shortMessage } shouldBe listOf("commit 2", "commit 1")
                walker.hasMore shouldBe false

                val thirdPage = walker.nextPage(3)
                thirdPage shouldHaveSize 0

                walker.close()
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

            it("should mark the merge commit as a merge commit with two parents") {
                val branch = GitDownState.git.value.listLocalBranches()
                    .first { it.name.endsWith(defaultBranchName) }

                val walker = CommitHistoryWalker(GitDownState.git.value, branch)
                val page = walker.nextPage(10)

                val mergeCommits = page.filter { it.isMergeCommit }
                mergeCommits shouldHaveSize 1
                mergeCommits.single().parentShas shouldHaveSize 2

                walker.close()
            }
        }
    }
})
