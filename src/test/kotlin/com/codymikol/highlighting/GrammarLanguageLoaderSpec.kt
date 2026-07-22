package com.codymikol.highlighting

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Paths
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeBytes

class GrammarLanguageLoaderSpec : DescribeSpec({

    describe("GrammarLanguageLoader") {

        it("returns null instead of throwing when the grammar file does not exist") {
            val result = GrammarLanguageLoader.load(Paths.get("/does/not/exist.so"), "tree_sitter_kotlin")

            result shouldBe null
        }

        it("returns null instead of throwing when the file is not a valid shared library") {
            val garbage = createTempDirectory("git-down-grammar-loader-test-").resolve("garbage.so")
            garbage.writeBytes(ByteArray(16) { it.toByte() })

            val result = GrammarLanguageLoader.load(garbage, "tree_sitter_kotlin")

            result shouldBe null
        }

        it("reuses the same loaded language instance for the same grammar file, so it isn't reloaded on every diff line") {
            val grammarFile = BundledJsonGrammarFixture.extract()

            val first = GrammarLanguageLoader.load(grammarFile, BundledJsonGrammarFixture.FUNCTION_NAME)
            val second = GrammarLanguageLoader.load(grammarFile, BundledJsonGrammarFixture.FUNCTION_NAME)

            first shouldBe second
        }

    }

})
