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

        describe("openDiffWithHead") {

            it("stores the commit and switches to the QuickView tab") {
                GitDownState.selectTab(Tab.Map)

                GitDownState.openDiffWithHead(node(message = "hello"))

                GitDownState.quickViewCommit.value?.shortMessage shouldBe "hello"
                GitDownState.currentTab.value shouldBe Tab.QuickView
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

            it("diffs against HEAD when opened via openDiffWithHead") {
                val parentSha = GitDownState.git.value.repository.resolve("HEAD~1").name
                GitDownState.openDiffWithHead(node(sha = parentSha))

                // HEAD~1 against HEAD still differs by a single file...
                GitDownState.quickViewDiffTree.value.fileDeltaNodes shouldHaveSize 1
            }

            it("diffs the parent against HEAD, not the quick view's first-parent diff") {
                val headSha = GitDownState.git.value.repository.resolve("HEAD").name
                // ...but the same commit against its own parent (quick view) versus
                // against HEAD (itself) differ: a quick view of HEAD shows one delta,
                // a HEAD-vs-HEAD diff shows none.
                GitDownState.openDiffWithHead(node(sha = headSha))

                GitDownState.quickViewDiffTree.value.fileDeltaNodes shouldHaveSize 0
            }

            it("returns to the quick view diff after reopening a quick view") {
                val headSha = GitDownState.git.value.repository.resolve("HEAD").name
                GitDownState.openDiffWithHead(node(sha = headSha))
                GitDownState.openQuickView(node(sha = headSha))

                GitDownState.quickViewDiffTree.value.fileDeltaNodes shouldHaveSize 1
            }
        }

        describe("quick view file selection") {

            beforeContainer { GitDownState.git.value.close() }

            autoClose(
                createTestRepository()
                    .addFile("a.txt", "one\n")
                    .addFile("b.txt", "one\n")
                    .stageAll()
                    .commitAll("init")
                    .appendToFile("a.txt", "two\n")
                    .appendToFile("b.txt", "two\n")
                    .stageAll()
                    .commitAll("edit both")
                    .transferIntoGitDownState()
            )

            fun paths() = GitDownState.quickViewDiffTree.value.fileDeltaNodes.map { it.getPath() }

            it("has no file selected when the quick view opens") {
                val headSha = GitDownState.git.value.repository.resolve("HEAD").name
                GitDownState.openQuickView(node(sha = headSha))

                GitDownState.quickViewSelectedFilePath.value shouldBe null
            }

            it("selects a specific file by path") {
                val headSha = GitDownState.git.value.repository.resolve("HEAD").name
                GitDownState.openQuickView(node(sha = headSha))

                val target = paths()[1]
                GitDownState.selectQuickViewFile(target)

                GitDownState.quickViewSelectedFilePath.value shouldBe target
            }

            it("moves to the next file, treating no selection as the first file") {
                val headSha = GitDownState.git.value.repository.resolve("HEAD").name
                GitDownState.openQuickView(node(sha = headSha))

                GitDownState.selectAdjacentQuickViewFile(1)

                GitDownState.quickViewSelectedFilePath.value shouldBe paths()[1]
            }

            it("moves to the previous file") {
                val headSha = GitDownState.git.value.repository.resolve("HEAD").name
                GitDownState.openQuickView(node(sha = headSha))
                GitDownState.selectQuickViewFile(paths()[1])

                GitDownState.selectAdjacentQuickViewFile(-1)

                GitDownState.quickViewSelectedFilePath.value shouldBe paths()[0]
            }

            it("clamps at the last file") {
                val headSha = GitDownState.git.value.repository.resolve("HEAD").name
                GitDownState.openQuickView(node(sha = headSha))
                GitDownState.selectQuickViewFile(paths().last())

                GitDownState.selectAdjacentQuickViewFile(1)

                GitDownState.quickViewSelectedFilePath.value shouldBe paths().last()
            }

            it("clears the selected file when reopening the quick view") {
                val headSha = GitDownState.git.value.repository.resolve("HEAD").name
                GitDownState.openQuickView(node(sha = headSha))
                GitDownState.selectQuickViewFile(paths()[1])

                GitDownState.openQuickView(node(sha = headSha))

                GitDownState.quickViewSelectedFilePath.value shouldBe null
            }
        }
    }
})
