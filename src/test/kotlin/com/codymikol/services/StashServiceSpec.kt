package com.codymikol.services

import com.codymikol.repository.TestRepository.Companion.createTestRepository
import com.codymikol.state.GitDownState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

class StashServiceSpec : DescribeSpec({

    describe("StashService.saveStash") {

        it("creates a stash carrying the given message") {
            autoClose(
                createTestRepository()
                    .addFile("foo.txt", "one\n")
                    .stageAll()
                    .commitAll("init")
                    .appendToFile("foo.txt", "two\n")
                    .transferIntoGitDownState()
            )

            StashService().saveStash("my stash message", includeUntrackedFiles = false)

            GitDownState.stashes.value.single().description shouldBe "my stash message"
        }

        it("preserves a message containing MessageFormat-sensitive characters") {
            autoClose(
                createTestRepository()
                    .addFile("foo.txt", "one\n")
                    .stageAll()
                    .commitAll("init")
                    .appendToFile("foo.txt", "two\n")
                    .transferIntoGitDownState()
            )

            StashService().saveStash("don't stop {now}", includeUntrackedFiles = false)

            GitDownState.stashes.value.single().description shouldBe "don't stop {now}"
        }

        it("leaves an untracked file in the working directory when includeUntrackedFiles is false") {
            autoClose(
                createTestRepository()
                    .addFile("foo.txt", "one\n")
                    .stageAll()
                    .commitAll("init")
                    .addFile("untracked.txt", "new\n")
                    .transferIntoGitDownState()
            )

            StashService().saveStash("no untracked", includeUntrackedFiles = false)

            GitDownState.untracked.value shouldContain "untracked.txt"
        }

        it("stashes an untracked file when includeUntrackedFiles is true") {
            autoClose(
                createTestRepository()
                    .addFile("foo.txt", "one\n")
                    .stageAll()
                    .commitAll("init")
                    .addFile("untracked.txt", "new\n")
                    .transferIntoGitDownState()
            )

            StashService().saveStash("with untracked", includeUntrackedFiles = true)

            GitDownState.untracked.value shouldNotContain "untracked.txt"
        }

    }

})
