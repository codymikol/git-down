package com.codymikol.highlighting

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.name

private class FakeGitHubRepositoryFetcher(
    private val filesByPath: Map<String, ByteArray>,
    private val listingsByPath: Map<String, List<GitHubEntry>>,
) : GitHubRepositoryFetcher {

    var fetchBytesCallCount = 0
        private set

    override fun listDirectory(org: String, repo: String, path: String): List<GitHubEntry>? = listingsByPath[path]

    override fun fetchBytes(url: String): ByteArray? {
        fetchBytesCallCount++
        return filesByPath[url]
    }
}

class SourceCompilingGrammarDownloaderSpec : DescribeSpec({

    describe("SourceCompilingGrammarDownloader") {

        val spec = GrammarSpec(repo = "tree-sitter-fake", functionName = "tree_sitter_fake")

        it("fetches the src/ tree, compiles it, and writes the result to destination") {
            val fetcher = FakeGitHubRepositoryFetcher(
                filesByPath = mapOf(
                    "https://example.com/parser.c" to "// parser".toByteArray(),
                    "https://example.com/parser.h" to "// header".toByteArray(),
                ),
                listingsByPath = mapOf(
                    "src" to listOf(
                        GitHubEntry("parser.c", "file", "https://example.com/parser.c"),
                        GitHubEntry("tree_sitter", "dir", null),
                    ),
                    "src/tree_sitter" to listOf(
                        GitHubEntry("parser.h", "file", "https://example.com/parser.h"),
                    ),
                ),
            )
            var compiledSources: List<Path>? = null
            val destination = createTempDirectory("git-down-source-downloader-test-").resolve("out.so")
            val downloader = SourceCompilingGrammarDownloader(fetcher, compile = { sources, _, dest ->
                compiledSources = sources
                Files.write(dest, "compiled".toByteArray())
                true
            })

            val result = runBlocking { downloader.download(spec, destination) }

            result shouldBe true
            Files.readString(destination) shouldBe "compiled"
            compiledSources?.map { it.name } shouldBe listOf("parser.c")
        }

        it("returns false when the top-level directory listing fails") {
            val fetcher = FakeGitHubRepositoryFetcher(emptyMap(), emptyMap())
            val downloader = SourceCompilingGrammarDownloader(fetcher, compile = { _, _, _ -> true })

            val result = runBlocking {
                downloader.download(spec, createTempDirectory("git-down-source-downloader-test-").resolve("out.so"))
            }

            result shouldBe false
        }

        it("returns false without compiling when no .c/.cc/.cpp sources are found") {
            val fetcher = FakeGitHubRepositoryFetcher(
                filesByPath = emptyMap(),
                listingsByPath = mapOf("src" to listOf(GitHubEntry("README.md", "file", null))),
            )
            var compileInvoked = false
            val downloader = SourceCompilingGrammarDownloader(fetcher, compile = { _, _, _ -> compileInvoked = true; true })

            val result = runBlocking {
                downloader.download(spec, createTempDirectory("git-down-source-downloader-test-").resolve("out.so"))
            }

            result shouldBe false
            compileInvoked shouldBe false
        }

        it("returns false when fetching a file's bytes fails partway through") {
            val fetcher = FakeGitHubRepositoryFetcher(
                filesByPath = emptyMap(), // parser.c has a download URL but no bytes registered -> fetchBytes returns null
                listingsByPath = mapOf(
                    "src" to listOf(GitHubEntry("parser.c", "file", "https://example.com/parser.c")),
                ),
            )
            val downloader = SourceCompilingGrammarDownloader(fetcher, compile = { _, _, _ -> true })

            val result = runBlocking {
                downloader.download(spec, createTempDirectory("git-down-source-downloader-test-").resolve("out.so"))
            }

            result shouldBe false
        }

        it("skips a file entry with a suspicious path-escaping name") {
            val fetcher = FakeGitHubRepositoryFetcher(
                filesByPath = mapOf("https://example.com/parser.c" to "// parser".toByteArray()),
                listingsByPath = mapOf(
                    "src" to listOf(
                        GitHubEntry("../evil.c", "file", "https://example.com/evil.c"),
                        GitHubEntry("parser.c", "file", "https://example.com/parser.c"),
                    ),
                ),
            )
            var compiledSources: List<Path>? = null
            val downloader = SourceCompilingGrammarDownloader(fetcher, compile = { sources, _, dest ->
                compiledSources = sources
                Files.write(dest, "compiled".toByteArray())
                true
            })

            val result = runBlocking {
                downloader.download(spec, createTempDirectory("git-down-source-downloader-test-").resolve("out.so"))
            }

            result shouldBe true
            compiledSources?.map { it.name } shouldBe listOf("parser.c")
        }

        it("fetches from the spec's sourcePath rather than assuming a top-level src/") {
            val nestedSpec = GrammarSpec(
                repo = "tree-sitter-markdown",
                functionName = "tree_sitter_markdown",
                sourcePath = "tree-sitter-markdown/src",
            )
            val fetcher = FakeGitHubRepositoryFetcher(
                filesByPath = mapOf("https://example.com/parser.c" to "// parser".toByteArray()),
                listingsByPath = mapOf(
                    "tree-sitter-markdown/src" to listOf(
                        GitHubEntry("parser.c", "file", "https://example.com/parser.c"),
                    ),
                ),
            )
            var compiledSources: List<Path>? = null
            val downloader = SourceCompilingGrammarDownloader(fetcher, compile = { sources, _, dest ->
                compiledSources = sources
                Files.write(dest, "compiled".toByteArray())
                true
            })

            val result = runBlocking {
                downloader.download(nestedSpec, createTempDirectory("git-down-source-downloader-test-").resolve("out.so"))
            }

            result shouldBe true
            compiledSources?.map { it.name } shouldBe listOf("parser.c")
        }

    }

})
