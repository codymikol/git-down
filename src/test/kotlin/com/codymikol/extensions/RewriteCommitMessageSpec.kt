package com.codymikol.extensions

import com.codymikol.repository.TestRepository.Companion.createTestRepository
import com.codymikol.state.GitDownState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class RewriteCommitMessageSpec : DescribeSpec({

    describe("Git.rewriteCommitMessage") {

        describe("rewriting an earlier commit's message") {

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

            it("rewrites the message while preserving descendants and content") {
                val git = GitDownState.git.value
                val repo = git.repository
                val target = repo.resolve("HEAD~1")
                val originalAuthor = repo.parseCommit(target).authorIdent

                git.rewriteCommitMessage(target.name, "renamed init")

                // History still has two commits, no orphaned dangling additions.
                repo.parseCommit(repo.resolve("HEAD")).shortMessage shouldBe "add two"
                val rewritten = repo.parseCommit(repo.resolve("HEAD~1"))
                rewritten.fullMessage shouldBe "renamed init"

                // Only the message changes: authorship is carried over verbatim.
                rewritten.authorIdent.name shouldBe originalAuthor.name
                rewritten.authorIdent.emailAddress shouldBe originalAuthor.emailAddress

                // The rewritten commit keeps its original tree, so the tip's tree -
                // and therefore the working tree content - is unchanged.
                java.io.File(repo.workTree, "foo.txt").readText() shouldBe "one\ntwo\n"
            }
        }
    }
})
