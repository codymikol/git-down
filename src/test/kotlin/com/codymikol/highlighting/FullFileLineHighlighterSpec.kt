package com.codymikol.highlighting

import androidx.compose.ui.graphics.Color
import com.codymikol.data.diff.FileDeltaNode
import com.codymikol.data.diff.Line
import com.codymikol.data.diff.LineType
import com.codymikol.data.file.FileDelta
import com.codymikol.data.file.Stash
import com.codymikol.data.file.Status
import com.codymikol.extensions.commitAll
import com.codymikol.extensions.stageAll
import com.codymikol.repository.TestRepository.Companion.createTestRepository
import com.codymikol.state.GitDownState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.treesitter.TSQuery
import java.nio.file.Path

private fun content(lines: List<String>) = lines.joinToString("\n", postfix = "\n")

private fun workingDirectoryNodeFor(path: String): FileDeltaNode {
    val fileDelta = GitDownState.workingDirectory.value.first { it.getPath() == path }
    GitDownState.selectedFiles.clear()
    GitDownState.selectedFiles.add(fileDelta)
    return GitDownState.diffTree.value.fileDeltaNodes.single()
}

class FullFileLineHighlighterSpec : DescribeSpec({

    describe("FullFileLineHighlighter.highlight") {

        it("returns null when the file delta's full content can't be read (e.g. a stash entry)") {
            val fileDelta = Stash.FileModified(Path.of("foo.json"), "")
            val line = Line(
                type = LineType.Added,
                value = "1",
                symbol = "+",
                originalLineNumber = null,
                newLineNumber = 1u,
            )

            FullFileLineHighlighter.highlight(fileDelta, line, "1", null).shouldBeNull()
        }

        it("highlights an added diff line using a parse of the file's full contents") {
            val grammarFile = BundledJsonGrammarFixture.extract()
            val language = GrammarLanguageLoader.load(grammarFile, BundledJsonGrammarFixture.FUNCTION_NAME)
            requireNotNull(language) { "Expected the bundled tree-sitter-json fixture to load" }

            autoClose(
                createTestRepository()
                    .addFile("foo.json", content(listOf("[", "  1", "]")))
                    .stageAll()
                    .commitAll("base")
                    .addFile("foo.json", content(listOf("[", "  1,", "  2", "]")))
                    .transferIntoGitDownState()
            )

            val workingNode = workingDirectoryNodeFor("foo.json")
            val addedLine = workingNode.hunkNodes.flatMap { it.lineNodes }
                .first { it.line.type == LineType.Added && it.line.value == "  2" }

            val highlighted = FullFileLineHighlighter.highlight(
                workingNode.fileDelta,
                addedLine.line,
                addedLine.line.value,
                language,
            )

            highlighted.shouldNotBeNull()
            // "2" is a bare number_literal token; tree-sitter-json reports its byte range
            // relative to the diff line's own text once sliced from the full-file parse.
            highlighted.spanStyles.map { it.start to it.end } shouldContain (2 to 3)
        }

        it("forwards a given highlights.scm query so its captures drive the line's coloring") {
            val grammarFile = BundledJsonGrammarFixture.extract()
            val language = GrammarLanguageLoader.load(grammarFile, BundledJsonGrammarFixture.FUNCTION_NAME)
            requireNotNull(language) { "Expected the bundled tree-sitter-json fixture to load" }
            // Deliberately captures the number node as @string: the heuristic would color a
            // "number" node type as a number, so only seeing the string color proves this
            // came from the query's capture, not a heuristic fallback that happens to agree.
            val query = TSQuery(language, "(number) @string")

            autoClose(
                createTestRepository()
                    .addFile("bar.json", content(listOf("[", "  1", "]")))
                    .stageAll()
                    .commitAll("base")
                    .addFile("bar.json", content(listOf("[", "  1,", "  2", "]")))
                    .transferIntoGitDownState()
            )

            val workingNode = workingDirectoryNodeFor("bar.json")
            val addedLine = workingNode.hunkNodes.flatMap { it.lineNodes }
                .first { it.line.type == LineType.Added && it.line.value == "  2" }

            val highlighted = FullFileLineHighlighter.highlight(
                workingNode.fileDelta,
                addedLine.line,
                addedLine.line.value,
                language,
                query,
            )

            highlighted.shouldNotBeNull()
            highlighted.spanStyles.map { it.item.color } shouldContain Color(206, 145, 120)
        }

        it("highlights a line in the second hunk of a multi-hunk diff") {
            // Second hunk's from/to line-number starts diverge from the first hunk's, since the
            // first hunk added one more line than it removed - this is the case Hunk.make's
            // newLineNumberIncrementer must seed from toFileLineNumbersStart, not
            // fromFileLineNumbersStart, or every line here gets mis-numbered and highlight()
            // falls back to null.
            val grammarFile = BundledJsonGrammarFixture.extract()
            val language = GrammarLanguageLoader.load(grammarFile, BundledJsonGrammarFixture.FUNCTION_NAME)
            requireNotNull(language) { "Expected the bundled tree-sitter-json fixture to load" }

            val baseLines = listOf("[") + (1..20).mapIndexed { i, n -> if (i < 19) "  $n," else "  $n" } + listOf("]")
            val modifiedLines = buildList {
                add("[")
                add("  1,")
                add("  100,")
                addAll((2..19).map { "  $it," })
                add("  200,")
                add("  20")
                add("]")
            }

            autoClose(
                createTestRepository()
                    .addFile("multi.json", content(baseLines))
                    .stageAll()
                    .commitAll("base")
                    .addFile("multi.json", content(modifiedLines))
                    .transferIntoGitDownState()
            )

            val workingNode = workingDirectoryNodeFor("multi.json")
            workingNode.hunkNodes.size shouldBe 2

            val secondHunk = workingNode.hunkNodes[1]
            secondHunk.hunk.header.fromFileLineNumbersStart shouldNotBe secondHunk.hunk.header.toFileLineNumbersStart

            val addedLine = secondHunk.lineNodes.first { it.line.type == LineType.Added && it.line.value == "  200," }

            val highlighted = FullFileLineHighlighter.highlight(
                workingNode.fileDelta,
                addedLine.line,
                addedLine.line.value,
                language,
            )

            highlighted.shouldNotBeNull()
            // "200" is a bare number_literal token starting after the two-space indent.
            highlighted.spanStyles.map { it.start to it.end } shouldContain (2 to 5)
        }

        it("returns null instead of parsing a file whose full content is implausibly large") {
            // The line the diff points at genuinely matches the full content's first line, so
            // this exercises the size guard specifically, not the line-mismatch fallback.
            val hugeContent = "a\n" + "b".repeat(10_000_000)
            val fileDelta = object : FileDelta {
                override val letter = "M"
                override val color = Color.White
                override val borderColor = Color.White
                override val location = Path.of("huge.json")
                override val type = Status.WORKING_DIRECTORY
                override fun getFullContent(line: Line): String = hugeContent
            }
            val line = Line(
                type = LineType.Added,
                value = "a",
                symbol = "+",
                originalLineNumber = null,
                newLineNumber = 1u,
            )

            FullFileLineHighlighter.highlight(fileDelta, line, "a", null).shouldBeNull()
        }

    }

})
