package com.codymikol.state

import com.codymikol.data.map.CommitGraphNode
import com.codymikol.repository.TestRepository.Companion.createTestRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.eclipse.jgit.lib.ObjectId
import java.util.Date

class CheckoutDetachedHeadStateSpec : DescribeSpec({

    fun node(sha: String) = CommitGraphNode(
        sha = sha,
        shortSha = sha.take(7),
        shortMessage = "do a thing",
        authorName = "Ada Lovelace",
        authorEmail = "ada@example.com",
        date = Date(),
        parentShas = emptyList(),
    )

    describe("GitDownState.checkoutDetachedHead") {

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

        it("detaches HEAD at the given commit") {
            val parent = GitDownState.git.value.repository.resolve("HEAD~1")

            GitDownState.checkoutDetachedHead(node(parent.name))

            val repo = GitDownState.git.value.repository
            ObjectId.isId(repo.branch) shouldBe true
            repo.resolve("HEAD") shouldBe parent
        }
    }
})
