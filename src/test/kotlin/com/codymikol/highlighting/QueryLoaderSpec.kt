package com.codymikol.highlighting

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import java.nio.file.Paths
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

class QueryLoaderSpec : DescribeSpec({

    describe("QueryLoader") {

        fun jsonLanguage() = GrammarLanguageLoader.load(
            BundledJsonGrammarFixture.extract(),
            BundledJsonGrammarFixture.FUNCTION_NAME,
        )!!

        it("returns null instead of throwing when the query file does not exist") {
            val result = QueryLoader.load(jsonLanguage(), Paths.get("/does/not/exist.scm"))

            result shouldBe null
        }

        it("compiles a valid highlights.scm against the grammar's language") {
            val queryFile = createTempDirectory("git-down-query-loader-test-").resolve("highlights.scm")
            queryFile.writeText("(string) @string")

            val result = QueryLoader.load(jsonLanguage(), queryFile)

            result.shouldNotBeNull()
        }

        it("returns null instead of throwing when the query text is malformed") {
            val queryFile = createTempDirectory("git-down-query-loader-test-").resolve("highlights.scm")
            queryFile.writeText("(this is not valid tree-sitter query syntax")

            val result = QueryLoader.load(jsonLanguage(), queryFile)

            result.shouldBeNull()
        }

        it("reuses the same compiled query instance for the same file, so it isn't recompiled on every diff line") {
            val queryFile = createTempDirectory("git-down-query-loader-test-").resolve("highlights.scm")
            queryFile.writeText("(string) @string")
            val language = jsonLanguage()

            val first = QueryLoader.load(language, queryFile)
            val second = QueryLoader.load(language, queryFile)

            first shouldBe second
        }

    }

})
