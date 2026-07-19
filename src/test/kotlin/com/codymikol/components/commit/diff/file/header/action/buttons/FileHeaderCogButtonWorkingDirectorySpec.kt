package com.codymikol.components.commit.diff.file.header.action.buttons

import com.codymikol.data.file.WorkingDirectory
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Path

class FileHeaderCogButtonWorkingDirectorySpec : DescribeSpec({

    describe("isNewUntrackedFile") {

        describe("when the file is newly added and untracked") {

            it("should return true") {
                isNewUntrackedFile(WorkingDirectory.FileAdded(Path.of("new-file.txt"))) shouldBe true
            }

        }

        describe("when the file is a tracked modification") {

            it("should return false") {
                isNewUntrackedFile(WorkingDirectory.FileModified(Path.of("existing.txt"))) shouldBe false
            }

        }

        describe("when the file is a tracked deletion") {

            it("should return false") {
                isNewUntrackedFile(WorkingDirectory.FileDeleted(Path.of("existing.txt"))) shouldBe false
            }

        }

    }

})
