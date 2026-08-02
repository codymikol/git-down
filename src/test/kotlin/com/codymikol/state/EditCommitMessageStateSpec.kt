package com.codymikol.state

import com.codymikol.data.map.CommitGraphNode
import com.codymikol.repository.TestRepository.Companion.createTestRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.util.Date

class EditCommitMessageStateSpec : DescribeSpec({

    fun node(sha: String) = CommitGraphNode(
        sha = sha,
        shortSha = sha.take(7),
        shortMessage = "do a thing",
        authorName = "Ada Lovelace",
        authorEmail = "ada@example.com",
        date = Date(),
        parentShas = emptyList(),
    )

    describe("GitDownState edit message modal") {

        beforeContainer { GitDownState.git.value.close() }

        autoClose(
            createTestRepository()
                .addFile("foo.txt", "one\n")
                .stageAll()
                .commitAll("init")
                .appendToFile("foo.txt", "two\n")
                .stageAll()
                .commitAll("add two")
                .transferIntoGitDownState()
        )

        it("opens and closes the modal for a commit") {
            val commit = node("abc123")

            GitDownState.openEditMessage(commit)
            GitDownState.editMessageCommit.value shouldBe commit

            GitDownState.closeEditMessage()
            GitDownState.editMessageCommit.value shouldBe null
        }

        it("rewrites the commit message and closes the modal") {
            val repo = GitDownState.git.value.repository
            val target = repo.resolve("HEAD~1")
            GitDownState.openEditMessage(node(target.name))

            GitDownState.editCommitMessage(node(target.name), "renamed init")

            repo.parseCommit(repo.resolve("HEAD~1")).fullMessage shouldBe "renamed init"
            repo.parseCommit(repo.resolve("HEAD")).shortMessage shouldBe "add two"
            GitDownState.editMessageCommit.value shouldBe null
        }
    }
})
