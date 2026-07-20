package com.codymikol.components.commit

import com.codymikol.data.file.WorkingDirectory
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.nio.file.Path

class ChangedFileSpec : DescribeSpec({

    describe("changedFileDisplayText") {

        it("should return the fully qualified file path, not just the file name") {
            val path = Path.of("src", "main", "kotlin", "com", "codymikol", "Foo.kt")
            val fileDelta = WorkingDirectory.FileModified(path)

            changedFileDisplayText(fileDelta) shouldBe path.toString()
            changedFileDisplayText(fileDelta) shouldNotBe "Foo.kt"
        }

    }

})
