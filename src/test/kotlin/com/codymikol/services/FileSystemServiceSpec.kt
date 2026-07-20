package com.codymikol.services

import com.codymikol.repository.TestRepository.Companion.createTestRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Path

class FileSystemServiceSpec : DescribeSpec({

    describe("FileSystemService") {

        describe("showInFiles") {

            it("should resolve a repo-root-level file against the working tree") {
                val repository = createTestRepository().addFile("README.md", "hello").transferIntoGitDownState()
                autoClose(repository)

                val service = FileSystemService()
                var revealed: File? = null

                service.showInFiles(Path.of("README.md")) { file -> revealed = file }

                revealed shouldBe File(repository.dir.toFile(), "README.md")
            }

            it("should resolve a nested file against the working tree") {
                val repository = createTestRepository().addFile("src/foo.txt", "hello").transferIntoGitDownState()
                autoClose(repository)

                val service = FileSystemService()
                var revealed: File? = null

                service.showInFiles(Path.of("src/foo.txt")) { file -> revealed = file }

                revealed shouldBe File(repository.dir.toFile(), "src/foo.txt")
            }

            it("should pass an already-absolute path through unchanged") {
                val repository = createTestRepository().transferIntoGitDownState()
                autoClose(repository)

                val service = FileSystemService()
                val absolute = Path.of("/tmp/gitdown/example.txt")
                var revealed: File? = null

                service.showInFiles(absolute) { file -> revealed = file }

                revealed shouldBe absolute.toFile()
            }

        }

    }

})
