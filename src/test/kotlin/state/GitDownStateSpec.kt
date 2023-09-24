package state

import TestRepository.Companion.createTestRepository
import com.codymikol.state.GitDownState
import io.kotest.core.spec.style.DescribeSpec
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
