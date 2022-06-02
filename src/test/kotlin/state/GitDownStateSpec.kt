package state

import extensions.commitAll
import extensions.scanForChanges
import extensions.stageAll
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.eclipse.jgit.api.Git
import java.io.File
import java.nio.file.Path

class GitDownStateSpec : DescribeSpec({

    data class TestRepository(
        val dir: Path,
        val git: Git,
    ) {


        fun addFile(filename: String, content: String) = this.also {
            val path = this.dir.toString() + "/" +  filename
            File(path).also { file -> file.parentFile.mkdirs() }.writeText("Foo")
        }

        fun appendToFile(filename: String, content: String) = this.also {
            val path = this.dir.toString() + "/" + filename
            File(path).appendText(content)
        }

        fun deleteFile(filename: String) = this.also {
            val path = this.dir.toString() + "/" + filename
            File(path).delete()
        }

        fun close() = this.also {
            this.git.close()
        }

        suspend fun stageAll() = this.also {
            this.git.stageAll()
        }

        suspend fun commitAll(message: String) = this.also {
            this.git.commitAll(message)
        }

        fun transferIntoGitDownState() = this.also {
            this.close()
            GitDownState.gitDirectory.value = this.dir.toString() + "/.git"
            GitDownState.git.value.scanForChanges()
        }

        fun closeGitDownState() = this.also {
            GitDownState.git.value.close()
        }
    }

    fun createTestRepository() =
        GitDownState.git.value.close()
            .let{ kotlin.io.path.createTempDirectory("git-down-state-test-") }
            .let { TestRepository(it, Git.init().setDirectory(it.toFile()).call()) }

    describe("GitDownState") {

        beforeContainer { GitDownState.git.value.close() }

        describe("Working Directory") {

            describe("Files Added") {

                lateinit var repo: TestRepository

                beforeTest {
                    repo = createTestRepository()
                      .addFile("foo.txt", "Foo")
                      .transferIntoGitDownState()
                }

                afterTest { repo.closeGitDownState() }

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

                lateinit var repo: TestRepository

                beforeTest {
                    repo = createTestRepository()
                        .addFile("foo.txt", "Foo")
                        .stageAll()
                        .commitAll("test commit")
                        .deleteFile("foo.txt")
                        .transferIntoGitDownState()
                }

                afterTest { repo.closeGitDownState() }

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

                lateinit var repo: TestRepository

                beforeTest {
                    repo = createTestRepository()
                        .addFile("foo.txt", "Foo")
                        .stageAll()
                        .commitAll("test commit")
                        .appendToFile("foo.txt", "Bar")
                        .transferIntoGitDownState()
                }

                afterTest { repo.closeGitDownState() }


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

                lateinit var repo: TestRepository

                beforeTest {
                    repo = createTestRepository()
                        .addFile("foo.txt", "Foo")
                        .stageAll()
                        .transferIntoGitDownState()
                }

                afterTest { repo.closeGitDownState() }

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

                lateinit var repo: TestRepository

                beforeTest {
                    repo = createTestRepository()
                        .addFile("foo.txt", "Foo")
                        .stageAll()
                        .commitAll("test commit")
                        .deleteFile("foo.txt")
                        .stageAll()
                        .transferIntoGitDownState()
                }

                afterTest { repo.closeGitDownState() }

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

                lateinit var repo: TestRepository

                beforeTest {
                    repo = createTestRepository()
                        .addFile("foo.txt", "Foo")
                        .stageAll()
                        .commitAll("test commit")
                        .appendToFile("foo.txt", "Bar")
                        .stageAll()
                        .transferIntoGitDownState()
                }

                afterTest { repo.closeGitDownState() }

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

                lateinit var repo: TestRepository

                beforeTest {
                    repo = createTestRepository()
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
                }

                afterTest { repo.closeGitDownState() }

                it("should properly reflect the commit count of commits [a,b,c]") {
                    GitDownState.commitCount.value shouldBe 3
                }

            }

            describe("indexIsEmpty") {

                describe("Happy path - there is nothing in the index at all") {

                    lateinit var repo: TestRepository

                    beforeTest {
                        repo = createTestRepository()
                            .transferIntoGitDownState()
                    }

                    afterTest { repo.closeGitDownState() }

                    it("should be true") {
                        GitDownState.indexIsEmpty.value shouldBe true
                    }

                }

                describe("When there is a deleted file in the index") {

                    lateinit var repo: TestRepository

                    beforeTest {
                        repo = createTestRepository()
                            .addFile("foo.txt", "Foo")
                            .stageAll()
                            .commitAll("test commit")
                            .deleteFile("foo.txt")
                            .stageAll()
                            .transferIntoGitDownState()
                    }

                    afterTest { repo.closeGitDownState() }

                    it("should be false") {
                        GitDownState.indexIsEmpty.value shouldBe false
                    }

                }

                describe("When there is an added file in the index") {

                    lateinit var repo: TestRepository

                    beforeTest {
                        repo = createTestRepository()
                            .addFile("foo.txt", "Foo")
                            .stageAll()
                            .transferIntoGitDownState()
                    }

                    afterTest { repo.closeGitDownState() }

                    it("should be false") {
                        GitDownState.indexIsEmpty.value shouldBe false
                    }

                }

                describe("When there is a modified file in the index") {

                    lateinit var repo: TestRepository

                    beforeTest {
                        repo = createTestRepository()
                            .addFile("foo.txt", "Foo")
                            .stageAll()
                            .commitAll("test commit")
                            .appendToFile("foo.txt", "Bar")
                            .stageAll()
                            .transferIntoGitDownState()
                    }

                    afterTest { repo.closeGitDownState() }

                    it("should be false") {
                        GitDownState.indexIsEmpty.value shouldBe false
                    }

                }

            }

            describe("workingDirectoryIsEmpty") {

                describe("Happy path - there is nothing in the working directory") {

                    lateinit var repo: TestRepository

                    beforeTest {
                        repo = createTestRepository()
                            .transferIntoGitDownState()
                    }

                    afterTest { repo.closeGitDownState() }

                    it("should be true") {
                        GitDownState.workingDirectoryIsEmpty.value shouldBe true
                    }

                }

                describe("When there is a deleted file in the working directory") {

                    lateinit var repo: TestRepository

                    beforeTest {
                        repo = createTestRepository()
                            .addFile("foo.txt", "Foo")
                            .stageAll()
                            .commitAll("test commit")
                            .deleteFile("foo.txt")
                            .transferIntoGitDownState()
                    }

                    afterTest { repo.closeGitDownState() }

                    it("should be false") {
                        GitDownState.workingDirectoryIsEmpty.value shouldBe false
                    }

                }

                describe("When there is an added file in the working directory") {

                    lateinit var repo: TestRepository

                    beforeTest {
                        repo = createTestRepository()
                            .addFile("foo.txt", "Foo")
                            .transferIntoGitDownState()
                    }

                    afterTest { repo.closeGitDownState() }

                    it("should be false") {
                        GitDownState.workingDirectoryIsEmpty.value shouldBe false
                    }

                }

                describe("When there is a modified file in the working directory") {

                    lateinit var repo: TestRepository

                    beforeTest {
                        repo = createTestRepository()
                            .addFile("foo.txt", "Foo")
                            .stageAll()
                            .commitAll("test commit")
                            .appendToFile("foo.txt", "Bar")
                            .transferIntoGitDownState()
                    }

                    afterTest { repo.closeGitDownState() }

                    it("should be false") {
                        GitDownState.workingDirectoryIsEmpty.value shouldBe false
                    }

                }

            }
        }

    }
})
