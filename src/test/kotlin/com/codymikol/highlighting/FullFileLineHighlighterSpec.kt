package com.codymikol.highlighting

import com.codymikol.data.diff.FileDeltaNode
import com.codymikol.data.diff.Line
import com.codymikol.data.diff.LineType
import com.codymikol.data.file.Stash
import com.codymikol.extensions.commitAll
import com.codymikol.extensions.stageAll
import com.codymikol.repository.TestRepository.Companion.createTestRepository
import com.codymikol.state.GitDownState
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
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

    }

})
