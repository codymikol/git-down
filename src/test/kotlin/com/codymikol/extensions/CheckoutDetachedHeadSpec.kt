package com.codymikol.extensions

import com.codymikol.repository.TestRepository.Companion.createTestRepository
import com.codymikol.state.GitDownState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.eclipse.jgit.lib.ObjectId

class CheckoutDetachedHeadSpec : DescribeSpec({

    describe("Git.checkoutDetachedHead") {

        describe("checking out an earlier commit") {

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

            it("detaches HEAD at the requested commit") {
                val git = GitDownState.git.value
                // The production caller passes a raw object-id sha (CommitGraphNode.sha),
                // so exercise that path rather than a symbolic ref.
                val target = git.repository.resolve("HEAD~1")

                git.checkoutDetachedHead(target.name)

                val repo = git.repository
                // A detached HEAD reports its object id as the "branch" name rather
                // than a symbolic refs/heads/ ref.
                ObjectId.isId(repo.branch) shouldBe true
                repo.resolve("HEAD") shouldBe target
            }
        }
    }
})
