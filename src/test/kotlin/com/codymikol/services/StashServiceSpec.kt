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

    describe("StashService.dropStash") {

        it("removes the given stash from GitDownState") {
            autoClose(
                createTestRepository()
                    .addFile("foo.txt", "one\n")
                    .stageAll()
                    .commitAll("init")
                    .appendToFile("foo.txt", "two\n")
                    .transferIntoGitDownState()
            )

            StashService().saveStash("to drop", includeUntrackedFiles = false)
            val stash = GitDownState.stashes.value.single()

            StashService().dropStash(stash)

            GitDownState.stashes.value shouldBe emptyList()
        }

        it("clears selectedStash when the dropped stash was selected") {
            autoClose(
                createTestRepository()
                    .addFile("foo.txt", "one\n")
                    .stageAll()
                    .commitAll("init")
                    .appendToFile("foo.txt", "two\n")
                    .transferIntoGitDownState()
            )

            StashService().saveStash("to drop", includeUntrackedFiles = false)
            val stash = GitDownState.stashes.value.single()
            GitDownState.selectedStash.value = stash

            StashService().dropStash(stash)

            GitDownState.selectedStash.value shouldBe null
        }

        it("leaves selectedStash untouched when a different stash is dropped") {
            val repo = createTestRepository()
                .addFile("foo.txt", "one\n")
                .stageAll()
                .commitAll("init")

            autoClose(repo)

            repo.appendToFile("foo.txt", "two\n").transferIntoGitDownState()
            StashService().saveStash("first", includeUntrackedFiles = false)

            repo.appendToFile("foo.txt", "three\n")
            StashService().saveStash("second", includeUntrackedFiles = false)

            val keptStash = GitDownState.stashes.value.single { it.description == "second" }
            val droppedStash = GitDownState.stashes.value.single { it.description == "first" }
            GitDownState.selectedStash.value = keptStash

            StashService().dropStash(droppedStash)

            GitDownState.stashes.value.single().description shouldBe "second"
            GitDownState.selectedStash.value shouldBe keptStash
        }

        it("does nothing when the given stash is no longer in the stash list") {
            autoClose(
                createTestRepository()
                    .addFile("foo.txt", "one\n")
                    .stageAll()
                    .commitAll("init")
                    .appendToFile("foo.txt", "two\n")
                    .transferIntoGitDownState()
            )

            StashService().saveStash("only stash", includeUntrackedFiles = false)
            val stash = GitDownState.stashes.value.single()
            StashService().dropStash(stash)

            StashService().dropStash(stash)

            GitDownState.stashes.value shouldBe emptyList()
        }

    }

})
