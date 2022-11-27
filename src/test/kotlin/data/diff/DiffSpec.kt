package data.diff

import com.codymikol.data.diff.FileDeltaNode
import com.codymikol.data.diff.LineType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class DiffSpec : DescribeSpec({

    val subject = """diff --git a/src/test/kotlin/state/GitDownStateSpec.kt b/src/test/kotlin/state/GitDownStateSpec.kt
index 39d9a33..960dae4 100644
--- a/src/test/kotlin/state/GitDownStateSpec.kt
+++ b/src/test/kotlin/state/GitDownStateSpec.kt
@@ -16,8 +16,9 @@
         val git: Git,
     ) {
 
+
         fun addFile(filename: String, content: String) = this.also {
-            val path = this.dir.toString() + "/" + filename
+            val path = this.dir.toString() + "/" +  filename
             File(path).also { file -> file.parentFile.mkdirs() }.writeText("Foo")
         }
 
@@ -51,7 +52,6 @@
         fun closeGitDownState() = this.also {
             GitDownState.git.value.close()
         }
-
     }
 
     fun createTestRepository() =
"""

    describe("make") {

        val fileDeltaNode = FileDeltaNode.make(subject)

        it("should have the correct number of chunks based on the number of delimiters in the diff") {
            fileDeltaNode.hunkNodes.size shouldBe 2
        }

        describe("Hunk") {

            val hunk = fileDeltaNode.hunkNodes.getOrNull(0)

            it("should have the proper delimiter for the first hunk") {
                hunk?.delimiter shouldBe "@@ -16,8 +16,9 @@"
            }

            it("should have the proper number of lines in the hunk") {
                hunk?.lineNodes?.size shouldBe 10
            }

        }

        describe("Line") {

            val hunk = fileDeltaNode.hunkNodes.getOrNull(0)

            describe("Added Line") {

                val example = hunk?.lineNodes?.getOrNull(6)

                it("should be the line I expect to be testing") {
                    example?.value shouldBe "            val path = this.dir.toString() + \"/\" +  filename"
                }

                it("should have the correct type") {
                    example?.type shouldBe LineType.Added
                }

            }

            describe("Deleted Line") {

                val example = hunk?.lineNodes?.getOrNull(5)

                it("should be the line I expect to be testing") {
                    example?.value shouldBe "            val path = this.dir.toString() + \"/\" + filename"
                }

                it("should have the correct type") {
                    example?.type shouldBe LineType.Removed
                }

            }

            describe("Unmodified Line") {

                val example = hunk?.lineNodes?.getOrNull(7)

                it("should be the line I expect to be testing") {
                    example?.value shouldBe "            File(path).also { file -> file.parentFile.mkdirs() }.writeText(\"Foo\")"
                }

                it("should have the correct type") {
                    example?.type shouldBe LineType.Unchanged
                }

            }

        }

    }

})
