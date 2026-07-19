package com.codymikol.services

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Path

class FileSystemServiceSpec : DescribeSpec({

    describe("FileSystemService") {

        describe("showInFiles") {

            it("should reveal the file at the given path") {
                val service = FileSystemService()
                val path = Path.of("/tmp/gitdown/example.txt")
                var revealed: File? = null

                service.showInFiles(path) { file -> revealed = file }

                revealed shouldBe path.toFile()
            }

        }

    }

})
