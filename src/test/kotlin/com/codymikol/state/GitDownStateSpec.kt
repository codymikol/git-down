package com.codymikol.state

import com.codymikol.data.diff.LineType
import com.codymikol.extensions.stageAll
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

    }
})
