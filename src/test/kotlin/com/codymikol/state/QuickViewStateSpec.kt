package com.codymikol.state

import com.codymikol.data.map.CommitGraphNode
import com.codymikol.repository.TestRepository.Companion.createTestRepository
import com.codymikol.tabs.Tab
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.util.Date

class QuickViewStateSpec : DescribeSpec({

    fun node(sha: String = "abc123def456", message: String = "do a thing") = CommitGraphNode(
        sha = sha,
        shortSha = sha.take(7),
        shortMessage = message,
        authorName = "Ada Lovelace",
        authorEmail = "ada@example.com",
        date = Date(),
        parentShas = emptyList(),
    )

    describe("GitDownState quick view") {

        describe("openQuickView") {

            it("stores the commit and switches to the QuickView tab") {
                GitDownState.selectTab(Tab.Map)

                GitDownState.openQuickView(node(message = "hello"))

                GitDownState.quickViewCommit.value?.shortMessage shouldBe "hello"
                GitDownState.currentTab.value shouldBe Tab.QuickView
            }
        }

        describe("closeQuickView") {

            it("clears the commit and restores the previous tab") {
                GitDownState.selectTab(Tab.Map)
                GitDownState.openQuickView(node())

                GitDownState.closeQuickView()

                GitDownState.quickViewCommit.value shouldBe null
                GitDownState.currentTab.value shouldBe Tab.Map
            }

            it("keeps the originally remembered tab when reopened for another commit") {
                GitDownState.selectTab(Tab.Map)
                GitDownState.openQuickView(node(sha = "one"))
                GitDownState.openQuickView(node(sha = "two"))

                GitDownState.closeQuickView()

                GitDownState.currentTab.value shouldBe Tab.Map
            }
        }

        describe("quickViewDiffTree") {

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

            it("builds the diff tree for the quick view commit") {
                val headSha = GitDownState.git.value.repository.resolve("HEAD").name
                GitDownState.openQuickView(node(sha = headSha))

                GitDownState.quickViewDiffTree.value.fileDeltaNodes shouldHaveSize 1
            }

            it("is empty when no quick view commit is set") {
                GitDownState.closeQuickView()

                GitDownState.quickViewDiffTree.value.fileDeltaNodes shouldHaveSize 0
            }
        }
    }
})
