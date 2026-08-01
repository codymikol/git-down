package com.codymikol.extensions

import com.codymikol.data.diff.DiffTree
import com.codymikol.data.diff.LineType
import com.codymikol.repository.TestRepository.Companion.createTestRepository
import com.codymikol.state.GitDownState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class CommitDiffAgainstHeadSpec : DescribeSpec({

    describe("Git.getCommitDiffAgainstHead") {

        describe("a commit that differs from HEAD") {

            autoClose(
                createTestRepository()
                    .addFile("foo.txt", "one\n")
                    .stageAll()
                    .commitAll("init")
                    .appendToFile("foo.txt", "two\n")
                    .stageAll()
                    .commitAll("add two")
                    .transferIntoGitDownState()
            )

            it("should contain a single file delta") {
                GitDownState.git.value.getCommitDiffAgainstHead("HEAD~1") shouldHaveSize 1
            }

            it("should diff the selected commit against HEAD, not its own parent") {
                val node = DiffTree.make(GitDownState.git.value.getCommitDiffAgainstHead("HEAD~1"))
                    .fileDeltaNodes
                    .single()

                // HEAD has "one\ntwo"; the selected commit (HEAD~1) has only "one",
                // so turning HEAD into it removes "two".
                val removedValues = node.hunkNodes
                    .flatMap { it.lineNodes }
                    .filter { it.line.type == LineType.Removed }
                    .map { it.line.value }

                removedValues shouldBe listOf("two")
            }

            it("should produce an empty diff for HEAD against itself") {
                GitDownState.git.value.getCommitDiffAgainstHead("HEAD") shouldHaveSize 0
            }
        }

        describe("an unknown commit reference") {

            autoClose(
                createTestRepository()
                    .addFile("init.txt", "init")
                    .stageAll()
                    .commitAll("init")
                    .transferIntoGitDownState()
            )

            it("should produce an empty diff") {
                GitDownState.git.value.getCommitDiffAgainstHead("does-not-exist") shouldHaveSize 0
            }
        }
    }
})
