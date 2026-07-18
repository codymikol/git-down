package com.codymikol.state

import com.codymikol.data.diff.LineType
import com.codymikol.extensions.discardAllWorkingDirectory
import com.codymikol.extensions.stageAll
import com.codymikol.extensions.stageSelectedLines
import com.codymikol.extensions.stageLinesForAddedFile
import com.codymikol.repository.TestRepository.Companion.createTestRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe


class GitDownStateSpec : DescribeSpec({

    describe("GitDownState") {

        beforeContainer { GitDownState.git.value.close() }

        describe("Working Directory") {

            describe("Files Added") {

                autoClose(
                    createTestRepository()
                        .addFile("foo.txt", "Foo")
                        .transferIntoGitDownState()
                )

                it("should contain a single file under workingDirectoryFilesAdded") {
                    GitDownState.workingDirectoryFilesAdded.value.size shouldBe 1
                }

                it("should NOT contain any workingDirectoryFilesDeleted") {
                    GitDownState.workingDirectoryFilesDeleted.value.size shouldBe 0
                }

                it("should NOT contain any workingDirectoryFilesModified") {
                    GitDownState.workingDirectoryFilesModified.value.size shouldBe 0
                }

                it("should NOT contain any indexFilesModified") {
                    GitDownState.indexFilesModified.value.size shouldBe 0
                }

                it("should NOT contain any indexFilesDeleted") {
                    GitDownState.indexFilesDeleted.value.size shouldBe 0
                }

                it("should NOT contain any indexFilesAdded") {
                    GitDownState.indexFilesAdded.value.size shouldBe 0
                }

            }

            describe("Staging all files") {

                autoClose(
                    createTestRepository()
                        .addFile("foo.txt", "Foo")
                        .transferIntoGitDownState()
                )

                GitDownState.git.value.stageAll()

                it("should move the workingDirectoryFiles added into the index") {
                    GitDownState.workingDirectoryFilesAdded.value.size shouldBe 0
                    GitDownState.workingDirectoryFilesDeleted.value.size shouldBe 0
                    GitDownState.workingDirectoryFilesModified.value.size shouldBe 0
                    GitDownState.indexFilesModified.value.size shouldBe 0
                    GitDownState.indexFilesDeleted.value.size shouldBe 0
                    GitDownState.indexFilesAdded.value.size shouldBe 1
                }

            }

            describe("Files Deleted") {

                autoClose(
                    createTestRepository()
                        .addFile("foo.txt", "Foo")
                        .stageAll()
                        .commitAll("test commit")
                        .deleteFile("foo.txt")
                        .transferIntoGitDownState()
                )

                it("should contain a single file under workingDirectoryFilesDeleted") {
                    GitDownState.workingDirectoryFilesDeleted.value.size shouldBe 1
                }

                it("should NOT contain any workingDirectoryFilesAdded") {
                    GitDownState.workingDirectoryFilesAdded.value.size shouldBe 0
                }

                it("should NOT contain any workingDirectoryFilesModified") {
                    GitDownState.workingDirectoryFilesModified.value.size shouldBe 0
                }

                it("should NOT contain any indexFilesModified") {
                    GitDownState.indexFilesModified.value.size shouldBe 0
                }

                it("should NOT contain any indexFilesDeleted") {
                    GitDownState.indexFilesDeleted.value.size shouldBe 0
                }

                it("should NOT contain any indexFilesAdded") {
                    GitDownState.indexFilesAdded.value.size shouldBe 0
                }

            }

            describe("Files Modified") {

                autoClose(
                    createTestRepository()
                        .addFile("foo.txt", "Foo")
                        .stageAll()
                        .commitAll("test commit")
                        .appendToFile("foo.txt", "Bar")
                        .transferIntoGitDownState()
                )

                it("should contain a single file under workingDirectoryFilesModified") {
                    GitDownState.workingDirectoryFilesModified.value.size shouldBe 1
                }

                it("should NOT contain any workingDirectoryFilesAdded") {
                    GitDownState.workingDirectoryFilesAdded.value.size shouldBe 0
                }

                it("should NOT contain any workingDirectoryFilesDeleted") {
                    GitDownState.workingDirectoryFilesDeleted.value.size shouldBe 0
                }

                it("should NOT contain any indexFilesModified") {
                    GitDownState.indexFilesModified.value.size shouldBe 0
                }

                it("should NOT contain any indexFilesDeleted") {
                    GitDownState.indexFilesDeleted.value.size shouldBe 0
                }

                it("should NOT contain any indexFilesAdded") {
                    GitDownState.indexFilesAdded.value.size shouldBe 0
                }

            }

            describe("Staging lines of a new file with a trailing newline") {

                autoClose(
                    createTestRepository()
                        .addFile("init.txt", "init")
                        .stageAll()
                        .commitAll("init") // todo(mikol): This is a workaround to an issue where a repository with no commits is basically borked...
                        .addFile("foo.txt", "a\nb\nc\n")
                        .transferIntoGitDownState()
                )

                val fileDeltas = GitDownState.workingDirectory.value

                fileDeltas shouldHaveSize 1

                val fooFileDelta = fileDeltas.toList().first()

                GitDownState.selectedFiles.clear()
                GitDownState.selectedFiles.add(fooFileDelta)

                val diffTree = GitDownState.diffTree.value

                val fileDeltaNodes = diffTree.fileDeltaNodes

                fileDeltaNodes shouldHaveSize 1

                val fooDeltaNode = fileDeltaNodes.first()

                fooDeltaNode.hunkNodes shouldHaveSize 1

                val hunkNode = fooDeltaNode.hunkNodes[0]

                hunkNode.lineNodes shouldHaveSize 3

                val bLine = hunkNode.lineNodes[1]

                GitDownState.git.value.stageLinesForAddedFile(listOf(bLine))

                it("Should now have only 3 lines in the working directory") {

                    val newFileDeltas = GitDownState.workingDirectory.value

                    newFileDeltas shouldHaveSize 1

                    val newFooFileDelta = newFileDeltas.toList().first()

                    GitDownState.selectedFiles.clear()
                    GitDownState.selectedFiles.add(newFooFileDelta)

                    val newDiffTree = GitDownState.diffTree.value

                    val newFileDeltaNodes = newDiffTree.fileDeltaNodes

                    newFileDeltaNodes shouldHaveSize 1

                    val newFooDeltaNode = newFileDeltaNodes.first()

                    newFooDeltaNode.hunkNodes shouldHaveSize 1

                    val newHunkNode = newFooDeltaNode.hunkNodes[0]

                    GitDownState.index.value.first().getDiff()

                    newHunkNode.lineNodes shouldHaveSize 3

                    val (a,b,c) = newHunkNode.lineNodes

                    a.line.value shouldBe "a"
                    a.line.type shouldBe LineType.Added

                    b.line.value shouldBe "b"
                    b.line.type shouldBe LineType.Unchanged

                    c.line.value shouldBe "c"
                    c.line.type shouldBe LineType.Added

                }


            }

        }

        describe("Index") {

            describe("Files Added") {

                autoClose(
                    createTestRepository()
                        .addFile("foo.txt", "Foo")
                        .stageAll()
                        .transferIntoGitDownState()
                )

                it("should contain a single file under indexFilesAdded") {
                    GitDownState.indexFilesAdded.value.size shouldBe 1
                }

                it("should NOT contain any indexFilesModified") {
                    GitDownState.indexFilesModified.value.size shouldBe 0
                }

                it("should NOT contain any indexFilesDeleted") {
                    GitDownState.indexFilesDeleted.value.size shouldBe 0
                }

                it("should NOT contain any workingDirectoryFilesAdded") {
                    GitDownState.workingDirectoryFilesAdded.value.size shouldBe 0
                }

                it("should NOT contain any workingDirectoryFilesDeleted") {
                    GitDownState.workingDirectoryFilesDeleted.value.size shouldBe 0
                }

                it("should NOT contain any workingDirectoryFilesModified") {
                    GitDownState.workingDirectoryFilesModified.value.size shouldBe 0
                }

            }

            describe("Files Deleted") {

                autoClose(
                    createTestRepository()
                        .addFile("foo.txt", "Foo")
                        .stageAll()
                        .commitAll("test commit")
                        .deleteFile("foo.txt")
                        .stageAll()
                        .transferIntoGitDownState()
                )

                it("should contain a single file under indexFilesDeleted") {
                    GitDownState.indexFilesDeleted.value.size shouldBe 1
                }

                it("should NOT contain any indexFilesAdded") {
                    GitDownState.indexFilesAdded.value.size shouldBe 0
                }

                it("should NOT contain any indexFilesModified") {
                    GitDownState.indexFilesModified.value.size shouldBe 0
                }

                it("should NOT contain any workingDirectoryFilesAdded") {
                    GitDownState.workingDirectoryFilesAdded.value.size shouldBe 0
                }

                it("should NOT contain any workingDirectoryFilesDeleted") {
                    GitDownState.workingDirectoryFilesDeleted.value.size shouldBe 0
                }

                it("should NOT contain any workingDirectoryFilesModified") {
                    GitDownState.workingDirectoryFilesModified.value.size shouldBe 0
                }

            }

            describe("Files Modified") {

                autoClose(
                    createTestRepository()
                        .addFile("foo.txt", "Foo")
                        .stageAll()
                        .commitAll("test commit")
                        .appendToFile("foo.txt", "Bar")
                        .stageAll()
                        .transferIntoGitDownState()
                )

                it("should contain a single file under indexFilesModified") {
                    GitDownState.indexFilesModified.value.size shouldBe 1
                }

                it("should NOT contain any indexFilesAdded") {
                    GitDownState.indexFilesAdded.value.size shouldBe 0
                }

                it("should NOT contain any indexFilesDeleted") {
                    GitDownState.indexFilesDeleted.value.size shouldBe 0
                }

                it("should NOT contain any workingDirectoryFilesAdded") {
                    GitDownState.workingDirectoryFilesAdded.value.size shouldBe 0
                }

                it("should NOT contain any workingDirectoryFilesDeleted") {
                    GitDownState.workingDirectoryFilesDeleted.value.size shouldBe 0
                }

                it("should NOT contain any workingDirectoryFilesModified") {
                    GitDownState.workingDirectoryFilesModified.value.size shouldBe 0
                }

            }

            describe("Staging added and deleted lines from a hunk") {

                autoClose(
                    createTestRepository()
                        .addFile("init.txt", "init")
                        .stageAll()
                        .commitAll("init")
                        .addFile("foo.txt", "a\nb\nc\nd\ne\nf\ng\nh\ni\nj\nk\nl\n")
                        .stageAll()
                        .commitAll("base")
                        .addFile("foo.txt", "a\nb\nc\nx\ny\nz\ng\nh\ni\nj\nk\nl\n")
                        .transferIntoGitDownState()
                )

                val workingFileDelta = GitDownState.workingDirectory.value
                    .first { it.getPath() == "foo.txt" }

                GitDownState.selectedFiles.clear()
                GitDownState.selectedFiles.add(workingFileDelta)

                val workingNode = GitDownState.diffTree.value.fileDeltaNodes.single()
                val workingLines = workingNode.hunkNodes.flatMap { it.lineNodes }

                val removedLines = workingLines
                    .filter { it.line.type == LineType.Removed }

                val addedLines = workingLines
                    .filter { it.line.type == LineType.Added }

                check(removedLines.isNotEmpty()) { "Expected removed lines in working hunk" }
                check(addedLines.isNotEmpty()) { "Expected added lines in working hunk" }

                val selected = removedLines + addedLines

                GitDownState.git.value.stageSelectedLines(selected)

                it("should include staged added and removed lines in the index") {
                    val indexFileDelta = GitDownState.index.value
                        .first { it.getPath() == "foo.txt" }

                    GitDownState.selectedFiles.clear()
                    GitDownState.selectedFiles.add(indexFileDelta)

                    val indexNode = GitDownState.diffTree.value.fileDeltaNodes.single()
                    val indexTypes = indexNode.hunkNodes
                        .flatMap { it.lineNodes }
                        .associate { it.line.value to it.line.type }

                    selected.forEach { lineNode ->
                        indexTypes[lineNode.line.value] shouldBe lineNode.line.type
                    }
                }

            }

            describe("Staging two added lines from ten added lines") {

                autoClose(
                    createTestRepository()
                        .addFile("init.txt", "init")
                        .stageAll()
                        .commitAll("init")
                        .addFile("foo.txt", "one\ntwo\nthree\nfour\nfive\n")
                        .stageAll()
                        .commitAll("base")
                        .addFile(
                            "foo.txt",
                            "one\ntwo\nthree\na1\na2\na3\na4\na5\na6\na7\na8\na9\na10\nfour\nfive\n"
                        )
                        .transferIntoGitDownState()
                )

                val workingFileDelta = GitDownState.workingDirectory.value
                    .first { it.getPath() == "foo.txt" }

                GitDownState.selectedFiles.clear()
                GitDownState.selectedFiles.add(workingFileDelta)

                val workingNode = GitDownState.diffTree.value.fileDeltaNodes.single()
                val workingLines = workingNode.hunkNodes.flatMap { it.lineNodes }

                val addedLines = workingLines.filter { it.line.type == LineType.Added }

                check(addedLines.size >= 10) { "Expected at least ten added lines" }

                val selected = addedLines.take(2)

                GitDownState.git.value.stageSelectedLines(selected)
                GitDownState.git.value.discardAllWorkingDirectory()

                it("should include staged added lines in the index") {
                    val indexFileDelta = GitDownState.index.value
                        .first { it.getPath() == "foo.txt" }

                    GitDownState.selectedFiles.clear()
                    GitDownState.selectedFiles.add(indexFileDelta)

                    val indexNode = GitDownState.diffTree.value.fileDeltaNodes.single()
                    val indexTypes = indexNode.hunkNodes
                        .flatMap { it.lineNodes }
                        .associate { it.line.value to it.line.type }

                    selected.forEach { lineNode ->
                        indexTypes[lineNode.line.value] shouldBe LineType.Added
                    }

                    val unstagedLine = addedLines[2].line.value
                    indexTypes[unstagedLine] shouldBe null
                }

            }

            describe("commitCount") {

                autoClose(
                    createTestRepository()
                        .addFile("foo.txt", "Foo")
                        .stageAll()
                        .commitAll("a")
                        .addFile("bar.txt", "Bar")
                        .stageAll()
                        .commitAll("b")
                        .addFile("baz.txt", "Foo")
                        .stageAll()
                        .commitAll("c")
                        .transferIntoGitDownState()
                )

                it("should properly reflect the commit count of commits [a,b,c]") {
                    GitDownState.commitCount.value shouldBe 3
                }

            }

            describe("indexIsEmpty") {

                describe("Happy path - there is nothing in the index at all") {

                    autoClose(
                        createTestRepository()
                            .transferIntoGitDownState()
                    )

                    it("should be true") {
                        GitDownState.indexIsEmpty.value shouldBe true
                    }

                }

                describe("When there is a deleted file in the index") {

                    autoClose(
                        createTestRepository()
                            .addFile("foo.txt", "Foo")
                            .stageAll()
                            .commitAll("test commit")
                            .deleteFile("foo.txt")
                            .stageAll()
                            .transferIntoGitDownState()
                    )

                    it("should be false") {
                        GitDownState.indexIsEmpty.value shouldBe false
                    }

                }

                describe("When there is an added file in the index") {

                    autoClose(
                        createTestRepository()
                            .addFile("foo.txt", "Foo")
                            .stageAll()
                            .transferIntoGitDownState()
                    )

                    it("should be false") {
                        GitDownState.indexIsEmpty.value shouldBe false
                    }

                }

                describe("When there is a modified file in the index") {

                    autoClose(
                        createTestRepository()
                            .addFile("foo.txt", "Foo")
                            .stageAll()
                            .commitAll("test commit")
                            .appendToFile("foo.txt", "Bar")
                            .stageAll()
                            .transferIntoGitDownState()
                    )

                    it("should be false") {
                        GitDownState.indexIsEmpty.value shouldBe false
                    }

                }

            }

            describe("workingDirectoryIsEmpty") {

                describe("Happy path - there is nothing in the working directory") {

                    autoClose(
                        createTestRepository()
                            .transferIntoGitDownState()
                    )

                    it("should be true") {
                        GitDownState.workingDirectoryIsEmpty.value shouldBe true
                    }

                }

                describe("When there is a deleted file in the working directory") {

                    autoClose(
                        createTestRepository()
                            .addFile("foo.txt", "Foo")
                            .stageAll()
                            .commitAll("test commit")
                            .deleteFile("foo.txt")
                            .transferIntoGitDownState()
                    )

                    it("should be false") {
                        GitDownState.workingDirectoryIsEmpty.value shouldBe false
                    }

                }

                describe("When there is an added file in the working directory") {

                    autoClose(
                        createTestRepository()
                            .addFile("foo.txt", "Foo")
                            .transferIntoGitDownState()
                    )

                    it("should be false") {
                        GitDownState.workingDirectoryIsEmpty.value shouldBe false
                    }

                }

                describe("When there is a modified file in the working directory") {

                    autoClose(
                        createTestRepository()
                            .addFile("foo.txt", "Foo")
                            .stageAll()
                            .commitAll("test commit")
                            .appendToFile("foo.txt", "Bar")
                            .transferIntoGitDownState()
                    )

                    it("should be false") {
                        GitDownState.workingDirectoryIsEmpty.value shouldBe false
                    }

                }

            }
        }

        describe("selectAdjacentFile") {

            describe("when there are multiple files in the working directory") {

                autoClose(
                    createTestRepository()
                        .addFile("a.txt", "A")
                        .addFile("b.txt", "B")
                        .addFile("c.txt", "C")
                        .transferIntoGitDownState()
                )

                val files = GitDownState.workingDirectory.value.toList()

                it("should select the next file when moving down") {
                    GitDownState.selectedFiles.clear()
                    GitDownState.selectedFiles.add(files[0])

                    GitDownState.selectAdjacentFile(1)

                    GitDownState.selectedFiles.toList() shouldBe listOf(files[1])
                }

                it("should select the previous file when moving up") {
                    GitDownState.selectedFiles.clear()
                    GitDownState.selectedFiles.add(files[1])

                    GitDownState.selectAdjacentFile(-1)

                    GitDownState.selectedFiles.toList() shouldBe listOf(files[0])
                }

                it("should stay on the first file when moving up from the top") {
                    GitDownState.selectedFiles.clear()
                    GitDownState.selectedFiles.add(files[0])

                    GitDownState.selectAdjacentFile(-1)

                    GitDownState.selectedFiles.toList() shouldBe listOf(files[0])
                }

                it("should stay on the last file when moving down from the bottom") {
                    GitDownState.selectedFiles.clear()
                    GitDownState.selectedFiles.add(files[2])

                    GitDownState.selectAdjacentFile(1)

                    GitDownState.selectedFiles.toList() shouldBe listOf(files[2])
                }

                it("should do nothing when there is no selection") {
                    GitDownState.selectedFiles.clear()

                    GitDownState.selectAdjacentFile(1)

                    GitDownState.selectedFiles.toList() shouldBe emptyList()
                }

                it("should do nothing when multiple files are selected") {
                    GitDownState.selectedFiles.clear()
                    GitDownState.selectedFiles.add(files[0])
                    GitDownState.selectedFiles.add(files[1])

                    GitDownState.selectAdjacentFile(1)

                    GitDownState.selectedFiles.toList() shouldBe listOf(files[0], files[1])
                }

            }

            describe("when a file in the index is selected") {

                autoClose(
                    createTestRepository()
                        .addFile("foo.txt", "Foo")
                        .addFile("bar.txt", "Bar")
                        .stageAll()
                        .transferIntoGitDownState()
                )

                val indexFiles = GitDownState.index.value.toList()

                it("should navigate within the index, not the working directory") {
                    GitDownState.selectedFiles.clear()
                    GitDownState.selectedFiles.add(indexFiles[0])

                    GitDownState.selectAdjacentFile(1)

                    GitDownState.selectedFiles.toList() shouldBe listOf(indexFiles[1])
                }

            }

        }

    }
})
