package com.codymikol.highlighting

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.nio.file.Files
import kotlin.io.path.createTempDirectory

class CompositeGrammarDownloaderSpec : DescribeSpec({

    describe("CompositeGrammarDownloader") {

        val spec = GrammarSpec(repo = "tree-sitter-vim", functionName = "tree_sitter_vim")

        fun sourceCompilingDownloader(succeeds: Boolean, onFetch: () -> Unit = {}): SourceCompilingGrammarDownloader {
            val fetcher = object : GitHubRepositoryFetcher {
                override fun listDirectory(org: String, repo: String, path: String): List<GitHubEntry>? {
                    onFetch()
                    return listOf(GitHubEntry("parser.c", "file", "https://example.com/parser.c"))
                }
                override fun fetchBytes(url: String): ByteArray? = "// parser".toByteArray()
            }
            return SourceCompilingGrammarDownloader(fetcher, compile = { _, _, dest ->
                if (succeeds) Files.write(dest, "compiled".toByteArray())
                succeeds
            })
        }

        it("does not fall back to source compiling when the prepackaged download succeeds") {
            var sourceCompilingCalled = false
            val prepackaged = PrepackagedGrammarDownloader { ByteArrayInputStream("native-bytes".toByteArray()) }
            val sourceCompiling = sourceCompilingDownloader(succeeds = true) { sourceCompilingCalled = true }
            val destination = createTempDirectory("git-down-composite-test-").resolve("out.so")

            val result = runBlocking { CompositeGrammarDownloader(prepackaged, sourceCompiling).download(spec, destination) }

            result shouldBe true
            Files.readString(destination) shouldBe "native-bytes"
            sourceCompilingCalled shouldBe false
        }

        it("falls back to source compiling when there is no prepackaged artifact") {
            val prepackaged = PrepackagedGrammarDownloader { null }
            val sourceCompiling = sourceCompilingDownloader(succeeds = true)
            val destination = createTempDirectory("git-down-composite-test-").resolve("out.so")

            val result = runBlocking { CompositeGrammarDownloader(prepackaged, sourceCompiling).download(spec, destination) }

            result shouldBe true
            Files.readString(destination) shouldBe "compiled"
        }

        it("returns false when neither downloader can resolve the grammar") {
            val prepackaged = PrepackagedGrammarDownloader { null }
            val sourceCompiling = sourceCompilingDownloader(succeeds = false)
            val destination = createTempDirectory("git-down-composite-test-").resolve("out.so")

            val result = runBlocking { CompositeGrammarDownloader(prepackaged, sourceCompiling).download(spec, destination) }

            result shouldBe false
        }

    }

})
