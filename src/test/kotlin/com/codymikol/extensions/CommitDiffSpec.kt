package com.codymikol.extensions

import com.codymikol.data.diff.DiffTree
import com.codymikol.data.diff.LineType
import com.codymikol.repository.TestRepository.Companion.createTestRepository
import com.codymikol.state.GitDownState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class CommitDiffSpec : DescribeSpec({

    describe("Git.getCommitDiff") {

        describe("a commit that modifies a single file") {

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

            val headSha = GitDownState.git.value.repository.resolve("HEAD").name

            it("should contain a single file delta") {
                GitDownState.git.value.getCommitDiff(headSha) shouldHaveSize 1
            }

            it("should expose the added line through the diff tree") {
                val node = DiffTree.make(GitDownState.git.value.getCommitDiff(headSha))
                    .fileDeltaNodes
                    .single()

                val addedValues = node.hunkNodes
                    .flatMap { it.lineNodes }
                    .filter { it.line.type == LineType.Added }
                    .map { it.line.value }

                addedValues shouldBe listOf("two")
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
                GitDownState.git.value.getCommitDiff("does-not-exist") shouldHaveSize 0
            }
        }
    }
})
