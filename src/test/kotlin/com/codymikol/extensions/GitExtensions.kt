@file:JvmName("GitExtensionsSpec")

package com.codymikol.extensions

import com.codymikol.data.diff.FileDeltaNode
import com.codymikol.data.diff.LineNode
import com.codymikol.data.diff.LineType
import com.codymikol.data.file.Index
import com.codymikol.data.file.Status
import com.codymikol.repository.TestRepository.Companion.createTestRepository
import com.codymikol.state.GitDownState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Path

private fun content(lines: List<String>) = lines.joinToString("\n", postfix = "\n")

private fun fileDeltaNodeFor(path: String, status: Status): FileDeltaNode {
    val fileDelta = when (status) {
        Status.WORKING_DIRECTORY -> GitDownState.workingDirectory.value.firstOrNull { it.getPath() == path }
        Status.INDEX -> GitDownState.index.value.firstOrNull { it.getPath() == path }
        Status.STASH -> null
    }
    fileDelta.shouldNotBeNull()
    GitDownState.selectedFiles.clear()
    GitDownState.selectedFiles.add(fileDelta)
    return GitDownState.diffTree.value.fileDeltaNodes.single()
}

private fun indexFileDeltaNode(path: String): FileDeltaNode =
    FileDeltaNode.make(Index.FileModified(Path.of(path)))

private fun FileDeltaNode.findLine(type: LineType, value: String): LineNode =
    hunkNodes
        .flatMap { it.lineNodes }
        .first { it.line.type == type && it.line.value == value }

private fun FileDeltaNode.lineTypesByValue() =
    hunkNodes
        .flatMap { it.lineNodes }
        .associate { it.line.value to it.line.type }

private fun FileDeltaNode.hunkCount() = hunkNodes.size

private fun removeLines(lines: List<String>, toRemove: Set<String>) =
    lines.filterNot { toRemove.contains(it) }

class GitExtensions : DescribeSpec({

    describe("stageSelectedLines") {

        describe("Staging a single line in a hunk") {

          autoClose(
              createTestRepository()
                  .addFile("init.txt", "init")
                  .stageAll()
                  .commitAll("init")
                  .addFile("foo.txt", content(listOf("one", "two", "three", "four", "five", "six")))
                  .stageAll()
                  .commitAll("base")
                  .addFile("foo.txt", content(listOf("one", "two", "three", "alpha", "beta", "gamma", "four", "five", "six")))
                  .transferIntoGitDownState()
          )

          val workingNode = fileDeltaNodeFor("foo.txt", Status.WORKING_DIRECTORY)
          val betaLine = workingNode.findLine(LineType.Added, "beta")

          GitDownState.git.value.stageSelectedLines(listOf(betaLine))

          it("should properly add that line to staging") {
              val indexNode = indexFileDeltaNode("foo.txt")
              indexNode.findLine(LineType.Added, "beta").line.value shouldBe "beta"
              indexNode.lineTypesByValue()["alpha"] shouldBe null
              indexNode.lineTypesByValue()["gamma"] shouldBe null
          }

          it("should leave the other lines in the unstaged hunk") {
              val workingNode = fileDeltaNodeFor("foo.txt", Status.WORKING_DIRECTORY)
              val lineTypes = workingNode.lineTypesByValue()
              lineTypes["alpha"] shouldBe LineType.Added
              lineTypes["beta"] shouldBe LineType.Unchanged
              lineTypes["gamma"] shouldBe LineType.Added
          }

        }

        describe("Staging three deleted lines in a six line hunk") {

          autoClose(
              createTestRepository()
                  .addFile("init.txt", "init")
                  .stageAll()
                  .commitAll("init")
                  .addFile("foo.txt", content(listOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")))
                  .stageAll()
                  .commitAll("base")
                  .addFile("foo.txt", content(listOf("a", "b", "i", "j")))
                  .transferIntoGitDownState()
          )

          val workingNode = fileDeltaNodeFor("foo.txt", Status.WORKING_DIRECTORY)
          val selected = listOf(
              workingNode.findLine(LineType.Removed, "c"),
              workingNode.findLine(LineType.Removed, "d"),
              workingNode.findLine(LineType.Removed, "e"),
          )

          GitDownState.git.value.stageSelectedLines(selected)
        
          it("should properly add those lines to staging") {
              val indexNode = indexFileDeltaNode("foo.txt")
              val indexTypes = indexNode.lineTypesByValue()
              indexTypes["c"] shouldBe LineType.Removed
              indexTypes["d"] shouldBe LineType.Removed
              indexTypes["e"] shouldBe LineType.Removed
          }

          it("should leave the other lines in the unstaged hunk") {
              val workingNode = fileDeltaNodeFor("foo.txt", Status.WORKING_DIRECTORY)
              val workingTypes = workingNode.lineTypesByValue()
              workingTypes["f"] shouldBe LineType.Removed
              workingTypes["g"] shouldBe LineType.Removed
              workingTypes["h"] shouldBe LineType.Removed
          }

        }


        describe("Staging three added lines in a six line hunk") {

          autoClose(
              createTestRepository()
                  .addFile("init.txt", "init")
                  .stageAll()
                  .commitAll("init")
                  .addFile("foo.txt", content(listOf("one", "two", "three", "four", "five", "six")))
                  .stageAll()
                  .commitAll("base")
                  .addFile("foo.txt", content(listOf("one", "two", "three", "a1", "a2", "a3", "a4", "a5", "a6", "four", "five", "six")))
                  .transferIntoGitDownState()
          )

          val workingNode = fileDeltaNodeFor("foo.txt", Status.WORKING_DIRECTORY)
          val selected = listOf(
              workingNode.findLine(LineType.Added, "a2"),
              workingNode.findLine(LineType.Added, "a3"),
              workingNode.findLine(LineType.Added, "a4"),
          )

          GitDownState.git.value.stageSelectedLines(selected)
        
          it("should properly add those lines to staging") {
              val indexNode = indexFileDeltaNode("foo.txt")
              val indexTypes = indexNode.lineTypesByValue()
              indexTypes["a2"] shouldBe LineType.Added
              indexTypes["a3"] shouldBe LineType.Added
              indexTypes["a4"] shouldBe LineType.Added
          }

          it("should leave the other lines in the unstaged hunk") {
              val workingNode = fileDeltaNodeFor("foo.txt", Status.WORKING_DIRECTORY)
              val workingTypes = workingNode.lineTypesByValue()
              workingTypes["a1"] shouldBe LineType.Added
              workingTypes["a2"] shouldBe LineType.Unchanged
              workingTypes["a3"] shouldBe LineType.Unchanged
              workingTypes["a4"] shouldBe LineType.Unchanged
              workingTypes["a5"] shouldBe LineType.Added
              workingTypes["a6"] shouldBe LineType.Added
          }
        
        }

        describe("Staging three added lines and three deleted lines in a nine line hunk") {

          autoClose(
              createTestRepository()
                  .addFile("init.txt", "init")
                  .stageAll()
                  .commitAll("init")
                  .addFile("foo.txt", content(listOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l")))
                  .stageAll()
                  .commitAll("base")
                  .addFile("foo.txt", content(listOf("a", "b", "c", "x", "y", "z", "g", "h", "i", "p", "q", "r", "j", "k", "l")))
                  .transferIntoGitDownState()
          )

          val workingNode = fileDeltaNodeFor("foo.txt", Status.WORKING_DIRECTORY)
          val selected = listOf(
              workingNode.findLine(LineType.Removed, "d"),
              workingNode.findLine(LineType.Removed, "e"),
              workingNode.findLine(LineType.Removed, "f"),
              workingNode.findLine(LineType.Added, "x"),
              workingNode.findLine(LineType.Added, "y"),
              workingNode.findLine(LineType.Added, "z"),
          )

          GitDownState.git.value.stageSelectedLines(selected)
        
          it("should properly add those lines to staging") {
              val indexNode = indexFileDeltaNode("foo.txt")
              val indexTypes = indexNode.lineTypesByValue()
              indexTypes["d"] shouldBe LineType.Removed
              indexTypes["e"] shouldBe LineType.Removed
              indexTypes["f"] shouldBe LineType.Removed
              indexTypes["x"] shouldBe LineType.Added
              indexTypes["y"] shouldBe LineType.Added
              indexTypes["z"] shouldBe LineType.Added
          }

          it("should leave the other lines in the unstaged hunk") {
              val workingNode = fileDeltaNodeFor("foo.txt", Status.WORKING_DIRECTORY)
              val workingTypes = workingNode.lineTypesByValue()
              workingTypes["p"] shouldBe LineType.Added
              workingTypes["q"] shouldBe LineType.Added
              workingTypes["r"] shouldBe LineType.Added
          }

        }

        describe("Staging two added lines btween two hunks") {

          val baseLines = (1..40).map { "line-$it" }
          val modifiedLines = listOf(
              "line-1",
              "line-2",
              "a1",
              "a2",
              "line-3",
              "line-4",
              "line-5",
              "line-6",
              "line-7",
              "line-8",
              "line-9",
              "line-10",
              "line-11",
              "line-12",
              "line-13",
              "line-14",
              "line-15",
              "line-16",
              "line-17",
              "line-18",
              "b1",
              "b2",
              "line-19",
              "line-20",
              "line-21",
              "line-22",
              "line-23",
              "line-24",
              "line-25",
              "line-26",
              "line-27",
              "line-28",
              "line-29",
              "line-30",
              "line-31",
              "line-32",
              "line-33",
              "line-34",
              "c1",
              "c2",
              "line-35",
              "line-36",
              "line-37",
              "line-38",
              "line-39",
              "line-40",
          )

          autoClose(
              createTestRepository()
                  .addFile("init.txt", "init")
                  .stageAll()
                  .commitAll("init")
                  .addFile("foo.txt", content(baseLines))
                  .stageAll()
                  .commitAll("base")
                  .addFile("foo.txt", content(modifiedLines))
                  .transferIntoGitDownState()
          )

          val workingNode = fileDeltaNodeFor("foo.txt", Status.WORKING_DIRECTORY)
          val selected = listOf(
              workingNode.findLine(LineType.Added, "b1"),
              workingNode.findLine(LineType.Added, "b2"),
          )

          GitDownState.git.value.stageSelectedLines(selected)
        
          it("should properly add those lines to staging") {
              val indexNode = indexFileDeltaNode("foo.txt")
              val indexTypes = indexNode.lineTypesByValue()
              indexTypes["b1"] shouldBe LineType.Added
              indexTypes["b2"] shouldBe LineType.Added
          }

          it("should leave the other lines in the unstaged hunk") {
              val workingNode = fileDeltaNodeFor("foo.txt", Status.WORKING_DIRECTORY)
              workingNode.hunkCount() shouldBe 2
              val workingTypes = workingNode.lineTypesByValue()
              workingTypes["a1"] shouldBe LineType.Added
              workingTypes["a2"] shouldBe LineType.Added
              workingTypes["c1"] shouldBe LineType.Added
              workingTypes["c2"] shouldBe LineType.Added
          }

        }

        describe("Staging two deleted lines between two hunks") {

          val baseLines = (1..30).map { "line-$it" }
          val modifiedLines = removeLines(
              baseLines,
              setOf("line-3", "line-4", "line-10", "line-11", "line-20", "line-21")
          )

          autoClose(
              createTestRepository()
                  .addFile("init.txt", "init")
                  .stageAll()
                  .commitAll("init")
                  .addFile("foo.txt", content(baseLines))
                  .stageAll()
                  .commitAll("base")
                  .addFile("foo.txt", content(modifiedLines))
                  .transferIntoGitDownState()
          )

          val workingNode = fileDeltaNodeFor("foo.txt", Status.WORKING_DIRECTORY)
          val selected = listOf(
              workingNode.findLine(LineType.Removed, "line-10"),
              workingNode.findLine(LineType.Removed, "line-11"),
          )

          GitDownState.git.value.stageSelectedLines(selected)
        
          it("should properly add those lines to staging") {
              val indexNode = indexFileDeltaNode("foo.txt")
              val indexTypes = indexNode.lineTypesByValue()
              indexTypes["line-10"] shouldBe LineType.Removed
              indexTypes["line-11"] shouldBe LineType.Removed
          }

          it("should leave the other lines in the unstaged hunk") {
              val workingNode = fileDeltaNodeFor("foo.txt", Status.WORKING_DIRECTORY)
              workingNode.hunkCount() shouldBe 2
              val workingTypes = workingNode.lineTypesByValue()
              workingTypes["line-3"] shouldBe LineType.Removed
              workingTypes["line-4"] shouldBe LineType.Removed
              workingTypes["line-20"] shouldBe LineType.Removed
              workingTypes["line-21"] shouldBe LineType.Removed
          }

        }

        describe("Staging two added lines and two deleted lines between two hunks") {
        
          val baseLines = (1..30).map { "line-$it" }
          val modifiedLines = listOf(
              "line-1",
              "line-2",
              "x1",
              "x2",
              "line-3",
              "line-4",
              "line-5",
              "line-6",
              "line-7",
              "line-8",
              "line-9",
              "y1",
              "y2",
              "line-12",
              "line-13",
              "line-14",
              "line-15",
              "line-16",
              "line-17",
              "line-18",
              "line-19",
              "line-20",
              "z1",
              "z2",
              "line-21",
              "line-22",
              "line-23",
              "line-24",
              "line-25",
              "line-26",
              "line-27",
              "line-28",
              "line-29",
              "line-30",
          )

          autoClose(
              createTestRepository()
                  .addFile("init.txt", "init")
                  .stageAll()
                  .commitAll("init")
                  .addFile("foo.txt", content(baseLines))
                  .stageAll()
                  .commitAll("base")
                  .addFile("foo.txt", content(modifiedLines))
                  .transferIntoGitDownState()
          )

          val workingNode = fileDeltaNodeFor("foo.txt", Status.WORKING_DIRECTORY)
          val selected = listOf(
              workingNode.findLine(LineType.Removed, "line-10"),
              workingNode.findLine(LineType.Removed, "line-11"),
              workingNode.findLine(LineType.Added, "y1"),
              workingNode.findLine(LineType.Added, "y2"),
          )

          GitDownState.git.value.stageSelectedLines(selected)

          it("should properly add those lines to staging") {
              val indexNode = indexFileDeltaNode("foo.txt")
              val indexTypes = indexNode.lineTypesByValue()
              indexTypes["line-10"] shouldBe LineType.Removed
              indexTypes["line-11"] shouldBe LineType.Removed
              indexTypes["y1"] shouldBe LineType.Added
              indexTypes["y2"] shouldBe LineType.Added
          }

          it("should leave the other lines in the unstaged hunk") {
              val workingNode = fileDeltaNodeFor("foo.txt", Status.WORKING_DIRECTORY)
              workingNode.hunkCount() shouldBe 2
              val workingTypes = workingNode.lineTypesByValue()
              workingTypes["x1"] shouldBe LineType.Added
              workingTypes["x2"] shouldBe LineType.Added
              workingTypes["z1"] shouldBe LineType.Added
              workingTypes["z2"] shouldBe LineType.Added
          }

        }

    }

    describe("unstageSelectedLines") {

        describe("Unstaging a few lines between two hunks") {



        }

    }

    describe("discardFile") {

        describe("Discarding changes to a modified tracked file") {

            val repo = autoClose(
                createTestRepository()
                    .addFile("foo.txt", "original\n")
                    .stageAll()
                    .commitAll("init")
                    .addFile("foo.txt", "changed\n")
                    .transferIntoGitDownState()
            )

            val workingNode = fileDeltaNodeFor("foo.txt", Status.WORKING_DIRECTORY)

            GitDownState.git.value.discardFile(workingNode)

            it("should restore the file's committed content on disk") {
                File(repo.dir.toString(), "foo.txt").readText() shouldBe "original\n"
            }

            it("should no longer report the file as changed") {
                GitDownState.workingDirectory.value.any { it.getPath() == "foo.txt" } shouldBe false
            }

        }

        describe("Discarding a newly added untracked file") {

            val repo = autoClose(
                createTestRepository()
                    .addFile("init.txt", "init")
                    .stageAll()
                    .commitAll("init")
                    .addFile("new.txt", "new content\n")
                    .transferIntoGitDownState()
            )

            val workingNode = fileDeltaNodeFor("new.txt", Status.WORKING_DIRECTORY)

            GitDownState.git.value.discardFile(workingNode)

            it("should delete the file from disk") {
                File(repo.dir.toString(), "new.txt").exists() shouldBe false
            }

            it("should no longer report the file in the working directory") {
                GitDownState.workingDirectory.value.any { it.getPath() == "new.txt" } shouldBe false
            }

        }

        describe("Discarding a deleted tracked file") {

            val repo = autoClose(
                createTestRepository()
                    .addFile("foo.txt", "original\n")
                    .stageAll()
                    .commitAll("init")
                    .deleteFile("foo.txt")
                    .transferIntoGitDownState()
            )

            val workingNode = fileDeltaNodeFor("foo.txt", Status.WORKING_DIRECTORY)

            GitDownState.git.value.discardFile(workingNode)

            it("should restore the file's committed content on disk") {
                File(repo.dir.toString(), "foo.txt").readText() shouldBe "original\n"
            }

            it("should no longer report the file as missing") {
                GitDownState.workingDirectory.value.any { it.getPath() == "foo.txt" } shouldBe false
            }

        }

    }

})
