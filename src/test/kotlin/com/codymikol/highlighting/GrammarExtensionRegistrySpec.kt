package com.codymikol.highlighting

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GrammarExtensionRegistrySpec : DescribeSpec({

    describe("GrammarExtensionRegistry") {

        it("resolves a known extension to its grammar spec") {
            GrammarExtensionRegistry.forExtension("kt") shouldBe GrammarSpec(
                repo = "tree-sitter-kotlin",
                functionName = "tree_sitter_kotlin",
                queriesPath = null,
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

        it("defaults queriesPath to the repo's top-level queries directory") {
            GrammarExtensionRegistry.forExtension("toml")?.queriesPath shouldBe "queries"
        }

        it("points markdown's queries at its nested directory, matching its nested source") {
            GrammarExtensionRegistry.forExtension("md")?.queriesPath shouldBe "tree-sitter-markdown/queries"
        }

        it("points csv's queries at its nested directory, matching its nested source") {
            GrammarExtensionRegistry.forExtension("csv")?.queriesPath shouldBe "csv/queries"
        }

        it("points vim/vue/tcl's queries at their per-language nested directory") {
            GrammarExtensionRegistry.forExtension("vim")?.queriesPath shouldBe "queries/vim"
            GrammarExtensionRegistry.forExtension("vue")?.queriesPath shouldBe "queries/vue"
            GrammarExtensionRegistry.forExtension("tcl")?.queriesPath shouldBe "queries/tcl"
        }

        it("has no queriesPath for grammars whose upstream repo ships no queries directory") {
            GrammarExtensionRegistry.forExtension("kt")?.queriesPath shouldBe null
            GrammarExtensionRegistry.forExtension("hcl")?.queriesPath shouldBe null
        }

    }

})
