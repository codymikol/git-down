package com.codymikol.data.file

import com.codymikol.data.diff.FileDeltaNode
import com.codymikol.data.diff.LineType
import com.codymikol.extensions.commitAll
import com.codymikol.extensions.stageAll
import com.codymikol.repository.TestRepository.Companion.createTestRepository
import com.codymikol.state.GitDownState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

private fun content(lines: List<String>) = lines.joinToString("\n", postfix = "\n")

private fun fileDeltaNodeFor(path: String, status: Status): FileDeltaNode {
    val fileDelta = when (status) {
        Status.WORKING_DIRECTORY -> GitDownState.workingDirectory.value.firstOrNull { it.getPath() == path }
        Status.INDEX -> GitDownState.index.value.firstOrNull { it.getPath() == path }
        Status.STASH -> null
    }
    requireNotNull(fileDelta)
    GitDownState.selectedFiles.clear()
    GitDownState.selectedFiles.add(fileDelta)
    return GitDownState.diffTree.value.fileDeltaNodes.single()
}

class FileDeltaGetFullContentSpec : DescribeSpec({

    describe("FileDelta.getFullContent") {

        describe("an unstaged addition in the working directory") {

            autoClose(
                createTestRepository()
                    .addFile("foo.txt", content(listOf("one", "two", "three")))
                    .stageAll()
                    .commitAll("base")
                    .addFile("foo.txt", content(listOf("one", "two", "three", "four")))
                    .transferIntoGitDownState()
            )

            val workingNode = fileDeltaNodeFor("foo.txt", Status.WORKING_DIRECTORY)
            val addedLine = workingNode.hunkNodes.flatMap { it.lineNodes }.first { it.line.type == LineType.Added }

            it("returns the working tree's full content for an added line") {
                workingNode.fileDelta.getFullContent(addedLine.line) shouldBe
                    content(listOf("one", "two", "three", "four"))
            }

        }

        describe("an unstaged removal, with a separate staged change already in the index") {

            autoClose(
                createTestRepository()
                    .addFile("foo.txt", content(listOf("a", "b", "c")))
                    .stageAll()
                    .commitAll("base")
                    .addFile("foo.txt", content(listOf("a", "b", "c", "d")))
                    .stageAll()
                    .addFile("foo.txt", content(listOf("a", "c", "d")))
                    .transferIntoGitDownState()
            )

            val workingNode = fileDeltaNodeFor("foo.txt", Status.WORKING_DIRECTORY)
            val removedLine = workingNode.hunkNodes.flatMap { it.lineNodes }.first { it.line.type == LineType.Removed }

            it("returns the index's content, not HEAD's, for a removed line") {
                // If this read HEAD instead of the index it would be missing "d".
                workingNode.fileDelta.getFullContent(removedLine.line) shouldBe
                    content(listOf("a", "b", "c", "d"))
            }

        }

        describe("a staged addition in the index") {

            autoClose(
                createTestRepository()
                    .addFile("foo.txt", content(listOf("one", "two", "three")))
                    .stageAll()
                    .commitAll("base")
                    .addFile("foo.txt", content(listOf("one", "two", "three", "four")))
                    .stageAll()
                    .transferIntoGitDownState()
            )

            val indexNode = fileDeltaNodeFor("foo.txt", Status.INDEX)
            val addedLine = indexNode.hunkNodes.flatMap { it.lineNodes }.first { it.line.type == LineType.Added }

            it("returns the index's full content for an added line") {
                indexNode.fileDelta.getFullContent(addedLine.line) shouldBe
                    content(listOf("one", "two", "three", "four"))
            }

        }

        describe("a staged removal") {

            autoClose(
                createTestRepository()
                    .addFile("foo.txt", content(listOf("a", "b", "c")))
                    .stageAll()
                    .commitAll("base")
                    .addFile("foo.txt", content(listOf("a", "c")))
                    .stageAll()
                    .transferIntoGitDownState()
            )

            val indexNode = fileDeltaNodeFor("foo.txt", Status.INDEX)
            val removedLine = indexNode.hunkNodes.flatMap { it.lineNodes }.first { it.line.type == LineType.Removed }

            it("returns HEAD's full content for a removed line") {
                indexNode.fileDelta.getFullContent(removedLine.line) shouldBe
                    content(listOf("a", "b", "c"))
            }

        }

    }

})
