package com.codymikol.extensions

import com.codymikol.repository.TestRepository.Companion.createTestRepository
import com.codymikol.state.GitDownState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.io.File

class DeleteFileSpec : DescribeSpec({

    describe("Git.deleteFile") {

        describe("Deleting a new, untracked file") {

            val repository = createTestRepository()
                .addFile("init.txt", "init")
                .stageAll()
                .commitAll("init")
                .addFile("new-file.txt", "one\n")
                .transferIntoGitDownState()

            autoClose(repository)

            GitDownState.git.value.deleteFile("new-file.txt")

            it("should remove the file from disk") {
                File(repository.dir.toString(), "new-file.txt").exists() shouldBe false
            }

            it("should no longer report the file as untracked") {
                GitDownState.untracked.value.contains("new-file.txt") shouldBe false
            }

        }

        describe("Deleting a file that does not exist") {

            val repository = createTestRepository()
                .addFile("init.txt", "init")
                .stageAll()
                .commitAll("init")
                .transferIntoGitDownState()

            autoClose(repository)

            it("should not throw") {
                GitDownState.git.value.deleteFile("missing.txt")
            }

        }

    }

})
