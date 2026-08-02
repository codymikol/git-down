package com.codymikol.components.commit.diff

import com.codymikol.data.diff.DiffTree
import com.codymikol.extensions.getCommitDiff
import com.codymikol.repository.TestRepository.Companion.createTestRepository
import com.codymikol.state.GitDownState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class DiffFileIndexSpec : DescribeSpec({

    describe("fileHeaderItemIndices") {

        beforeContainer { GitDownState.git.value.close() }

        autoClose(
            createTestRepository()
                .addFile("a.txt", "l1\nl2\nl3\nl4\nl5\n")
                .addFile("b.txt", "one\n")
                .stageAll()
                .commitAll("init")
                .appendToFile("a.txt", "l6\n")
                .appendToFile("b.txt", "two\n")
                .stageAll()
                .commitAll("edit both")
                .transferIntoGitDownState()
        )

        it("points at every file header in the built diff item list") {
            val headSha = GitDownState.git.value.repository.resolve("HEAD").name
            val nodes = DiffTree.make(GitDownState.git.value.getCommitDiff(headSha)).fileDeltaNodes

            val expected = buildDiffItems(nodes)
                .mapIndexedNotNull { index, item -> if (item is DiffItem.FileHeaderItem) index else null }

            fileHeaderItemIndices(nodes) shouldBe expected
        }
    }

    describe("stickyFileIndex") {

        val headerIndices = listOf(0, 5, 12)

        it("returns -1 when there are no files") {
            stickyFileIndex(0, emptyList()) shouldBe -1
        }

        it("returns the first file at the very top") {
            stickyFileIndex(0, headerIndices) shouldBe 0
        }

        it("stays on a file while its lines are the first visible item") {
            stickyFileIndex(3, headerIndices) shouldBe 0
        }

        it("advances to the next file once its header is the first visible item") {
            stickyFileIndex(5, headerIndices) shouldBe 1
        }

        it("advances to the last file when scrolled past its header") {
            stickyFileIndex(20, headerIndices) shouldBe 2
        }
    }

    describe("fileSelectionForScroll") {

        val headerIndices = listOf(0, 5, 12)

        it("follows the sticky file while the list can still scroll") {
            fileSelectionForScroll(5, headerIndices, selectedIndex = 0, atEnd = false) shouldBe 1
        }

        it("overrides a below-sticky selection while the list can still scroll") {
            fileSelectionForScroll(5, headerIndices, selectedIndex = 2, atEnd = false) shouldBe 1
        }

        it("keeps a selection the user placed on the last file at the end of the list") {
            // The last file's header can't reach the top, so scroll reports the
            // second-to-last file as sticky; the user's last-file selection stands
            // (see issue #308).
            fileSelectionForScroll(6, headerIndices, selectedIndex = 2, atEnd = true) shouldBe 2
        }

        it("keeps a selection on any file below the sticky one at the end") {
            fileSelectionForScroll(0, headerIndices, selectedIndex = 1, atEnd = true) shouldBe 1
        }

        it("snaps to the sticky file when scrolled to the end past the selection") {
            fileSelectionForScroll(12, headerIndices, selectedIndex = 0, atEnd = true) shouldBe 2
        }

        it("follows the sticky file when nothing is selected") {
            fileSelectionForScroll(6, headerIndices, selectedIndex = -1, atEnd = true) shouldBe 1
        }

        it("keeps the current selection when there are no files") {
            fileSelectionForScroll(0, emptyList(), selectedIndex = 3, atEnd = true) shouldBe 3
        }
    }
})
