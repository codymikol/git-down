package com.codymikol.highlighting

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GrammarExtensionRegistrySpec : DescribeSpec({

    describe("GrammarExtensionRegistry") {

        it("resolves a known extension to its grammar spec") {
            GrammarExtensionRegistry.forExtension("kt") shouldBe GrammarSpec(
                repo = "tree-sitter-kotlin",
                functionName = "tree_sitter_kotlin",
            )
        }

        it("returns null for an unknown extension") {
            GrammarExtensionRegistry.forExtension("notarealextension") shouldBe null
        }

        it("points markdown at its nested source directory, since the repo bundles two grammars") {
            GrammarExtensionRegistry.forExtension("md")?.sourcePath shouldBe "tree-sitter-markdown/src"
        }

        it("points csv at its nested source directory, since the repo bundles csv/tsv/psv") {
            GrammarExtensionRegistry.forExtension("csv")?.sourcePath shouldBe "csv/src"
        }

    }

})
