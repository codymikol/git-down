package com.codymikol.extensions

import com.codymikol.data.diff.LineType
import com.codymikol.repository.TestRepository.Companion.createTestRepository
import com.codymikol.state.GitDownState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class StashDiffSpec : DescribeSpec({

    describe("Git.getStashDiff") {

        describe("A stash with a single modified file") {

            autoClose(
                createTestRepository()
                    .addFile("foo.txt", "one\n")
                    .stageAll()
                    .commitAll("init")
                    .appendToFile("foo.txt", "two\n")
                    .stashCreate()
                    .transferIntoGitDownState()
            )

            val stash = GitDownState.stashes.value.single()

            it("should contain a single file delta") {
                GitDownState.git.value.getStashDiff(stash.revCommit) shouldHaveSize 1
            }

            it("should expose the added line through GitDownState.stashDiffTree") {
                GitDownState.selectedStash.value = stash

                val node = GitDownState.stashDiffTree.value.fileDeltaNodes.single()
                val addedValues = node.hunkNodes
                    .flatMap { it.lineNodes }
                    .filter { it.line.type == LineType.Added }
                    .map { it.line.value }

                addedValues shouldBe listOf("two")
            }

        }

        describe("No stash selected") {

            autoClose(
                createTestRepository()
                    .addFile("init.txt", "init")
                    .stageAll()
                    .commitAll("init")
                    .transferIntoGitDownState()
            )

            it("should produce an empty diff tree") {
                GitDownState.selectedStash.value = null
                GitDownState.stashDiffTree.value.fileDeltaNodes shouldHaveSize 0
            }

        }

    }

})
