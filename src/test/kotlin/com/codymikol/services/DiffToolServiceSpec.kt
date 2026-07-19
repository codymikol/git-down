package com.codymikol.services

import com.codymikol.data.file.Index
import com.codymikol.data.file.Stash
import com.codymikol.data.file.WorkingDirectory
import com.codymikol.repository.TestRepository.Companion.createTestRepository
import com.codymikol.state.GitDownState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Path

class DiffToolServiceSpec : DescribeSpec({

    describe("DiffToolService.getConfiguredDiffTool") {

        it("returns null when no diff tool is configured") {
            autoClose(createTestRepository().transferIntoGitDownState())

            DiffToolService().getConfiguredDiffTool() shouldBe null
        }

        it("returns the diff.tool value configured in the repository's gitconfig") {
            autoClose(createTestRepository().transferIntoGitDownState())

            GitDownState.repo.value.config.setString("diff", null, "tool", "meld")
            GitDownState.repo.value.config.save()

            DiffToolService().getConfiguredDiffTool() shouldBe "meld"
        }

    }

    describe("DiffToolService.buildDiffToolCommand") {

        it("builds a working directory diff command without --cached") {
            autoClose(createTestRepository().transferIntoGitDownState())

            val command = DiffToolService().buildDiffToolCommand(WorkingDirectory.FileModified(Path.of("foo.txt")))

            command shouldBe listOf("git", "difftool", "-y", "--", "foo.txt")
        }

        it("builds an index diff command with --cached") {
            autoClose(createTestRepository().transferIntoGitDownState())

            val command = DiffToolService().buildDiffToolCommand(Index.FileModified(Path.of("foo.txt")))

            command shouldBe listOf("git", "difftool", "-y", "--cached", "--", "foo.txt")
        }

        it("includes the configured diff tool via -t when one is set") {
            autoClose(createTestRepository().transferIntoGitDownState())

            GitDownState.repo.value.config.setString("diff", null, "tool", "kdiff3")
            GitDownState.repo.value.config.save()

            val command = DiffToolService().buildDiffToolCommand(WorkingDirectory.FileModified(Path.of("foo.txt")))

            command shouldBe listOf("git", "difftool", "-y", "-t", "kdiff3", "--", "foo.txt")
        }

    }

    describe("DiffToolService.launchDiffTool") {

        it("launches the built command in the repository's working tree") {
            autoClose(createTestRepository().transferIntoGitDownState())

            var launchedCommand: List<String>? = null
            var launchedDirectory: File? = null

            val service = DiffToolService()
            service.launchProcess = { command, workingDirectory ->
                launchedCommand = command
                launchedDirectory = workingDirectory
                ProcessBuilder("true").start()
            }

            service.launchDiffTool(WorkingDirectory.FileModified(Path.of("foo.txt")))

            launchedCommand shouldBe listOf("git", "difftool", "-y", "--", "foo.txt")
            launchedDirectory shouldBe GitDownState.repo.value.workTree
        }

        it("does not attempt to launch a diff tool for a stashed file") {
            autoClose(createTestRepository().transferIntoGitDownState())

            var wasLaunched = false

            val service = DiffToolService()
            service.launchProcess = { command, workingDirectory ->
                wasLaunched = true
                ProcessBuilder("true").start()
            }

            service.launchDiffTool(Stash.FileModified(Path.of("foo.txt"), "diff"))

            wasLaunched shouldBe false
        }

    }

})
