package com.codymikol.data.stash

import com.codymikol.repository.TestRepository.Companion.createTestRepository
import com.codymikol.state.GitDownState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class StashListItemSpec : DescribeSpec({

    describe("GitDownState.stashes") {

        describe("A default stash with no custom message") {

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

            it("should be described as WIP") {
                stash.description shouldBe "WIP"
            }

            it("should reference the branch the stash was made on") {
                stash.branchOrSha shouldBe GitDownState.branchName.value
            }

            it("should reference the commit the stash was made on top of") {
                stash.commitMessage shouldContain "init"
            }

            it("should render a title containing the abbreviated sha") {
                stash.title shouldContain stash.sha.take(7)
            }

        }

        describe("A stash with a custom message") {

            autoClose(
                createTestRepository()
                    .addFile("foo.txt", "one\n")
                    .stageAll()
                    .commitAll("init")
                    .appendToFile("foo.txt", "two\n")
                    .stashCreate("my custom message")
                    .transferIntoGitDownState()
            )

            val stash = GitDownState.stashes.value.single()

            it("should use the custom message as the description") {
                stash.description shouldBe "my custom message"
            }

            it("should reference the branch the stash was made on") {
                stash.branchOrSha shouldBe GitDownState.branchName.value
            }

        }

        describe("Multiple stashes") {

            autoClose(
                createTestRepository()
                    .addFile("foo.txt", "one\n")
                    .stageAll()
                    .commitAll("init")
                    .appendToFile("foo.txt", "two\n")
                    .stashCreate()
                    .appendToFile("foo.txt", "three\n")
                    .stashCreate()
                    .transferIntoGitDownState()
            )

            it("should list both stashes") {
                GitDownState.stashes.value shouldHaveSize 2
            }

        }

    }

})
