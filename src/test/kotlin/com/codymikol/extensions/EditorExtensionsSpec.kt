package com.codymikol.extensions

import com.codymikol.repository.TestRepository.Companion.createTestRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Path

class EditorExtensionsSpec : DescribeSpec({

    describe("Git.openFile") {

        it("opens the file with the system default handler, ignoring a configured terminal editor") {
            val repo = createTestRepository()
            repo.addFile("foo.txt", "hello")
            // A terminal editor (vim, nano, or CI's noop "true") launched without a
            // controlling terminal silently does nothing, which is exactly the bug
            // this test guards against: "Open File" must always use the OS's default
            // file handler rather than git's commit-message editor.
            repo.git.repository.config.setString("core", null, "editor", "true")

            var opened: File? = null
            repo.git.openFile(Path.of("foo.txt"), open = { opened = it })

            opened shouldBe File(repo.git.repository.workTree, "foo.txt")

            repo.closeGitRepo()
        }

        it("resolves nested paths relative to the repository's work tree") {
            val repo = createTestRepository()
            repo.addFile("nested/dir/foo.txt", "hello")

            var opened: File? = null
            repo.git.openFile(Path.of("nested/dir/foo.txt"), open = { opened = it })

            opened shouldBe File(repo.git.repository.workTree, "nested/dir/foo.txt")

            repo.closeGitRepo()
        }

    }

})
