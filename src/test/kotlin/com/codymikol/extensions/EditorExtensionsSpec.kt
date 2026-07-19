package com.codymikol.extensions

import com.codymikol.repository.TestRepository.Companion.createTestRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.io.File

class EditorExtensionsSpec : DescribeSpec({

    describe("Git.getConfiguredEditor") {

        it("prefers the GIT_EDITOR environment variable over core.editor") {
            val repo = createTestRepository()
            repo.git.repository.config.setString("core", null, "editor", "configured-editor")

            repo.git.getConfiguredEditor(getenv = { key -> if (key == "GIT_EDITOR") "env-editor" else null }) shouldBe "env-editor"

            repo.closeGitRepo()
        }

        it("falls back to core.editor when GIT_EDITOR is unset") {
            val repo = createTestRepository()
            repo.git.repository.config.setString("core", null, "editor", "configured-editor")

            repo.git.getConfiguredEditor(getenv = { null }) shouldBe "configured-editor"

            repo.closeGitRepo()
        }

        it("returns null when neither is configured") {
            val repo = createTestRepository()

            repo.git.getConfiguredEditor(getenv = { null }) shouldBe null

            repo.closeGitRepo()
        }

        it("treats a blank GIT_EDITOR as unset") {
            val repo = createTestRepository()
            repo.git.repository.config.setString("core", null, "editor", "configured-editor")

            repo.git.getConfiguredEditor(getenv = { key -> if (key == "GIT_EDITOR") "" else null }) shouldBe "configured-editor"

            repo.closeGitRepo()
        }

    }

    describe("buildEditorCommand") {

        it("appends the file path to a simple command") {
            buildEditorCommand("vim", File("/tmp/foo.txt")) shouldBe listOf("vim", "/tmp/foo.txt")
        }

        it("splits flags from the editor command") {
            buildEditorCommand("code --wait", File("/tmp/foo.txt")) shouldBe listOf("code", "--wait", "/tmp/foo.txt")
        }

        it("preserves quoted segments containing spaces as a single token") {
            buildEditorCommand("\"/opt/My Editor/bin/edit\" -w", File("/tmp/foo.txt")) shouldBe
                listOf("/opt/My Editor/bin/edit", "-w", "/tmp/foo.txt")
        }

    }

})
