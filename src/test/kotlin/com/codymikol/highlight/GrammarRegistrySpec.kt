package com.codymikol.highlight

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GrammarRegistrySpec : DescribeSpec({

    describe("GrammarRegistry") {

        describe("forPath") {

            describe("when the path extension matches a baked-in grammar") {

                it("should resolve the kotlin grammar for a .kt file") {
                    GrammarRegistry.forPath("src/main/kotlin/Foo.kt").id shouldBe "kotlin"
                }

                it("should resolve the python grammar for a .py file") {
                    GrammarRegistry.forPath("scripts/build.py").id shouldBe "python"
                }

            }

            describe("when the path extension is unrecognized") {

                it("should fall back to the plain grammar") {
                    GrammarRegistry.forPath("README.md").id shouldBe "plain"
                }

            }

        }

        describe("register") {

            describe("when a caller registers a custom grammar") {

                val custom = Grammar(id = "custom-lang", extensions = setOf("cstm"), keywords = setOf("FOO"))
                GrammarRegistry.register(custom)

                it("should resolve paths with the registered extension to that grammar") {
                    GrammarRegistry.forPath("file.cstm").id shouldBe "custom-lang"
                }

            }

        }

    }

})
