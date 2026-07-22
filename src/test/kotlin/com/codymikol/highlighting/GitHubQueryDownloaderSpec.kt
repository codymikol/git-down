package com.codymikol.highlighting

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.io.path.createTempDirectory

private class FakeGitHubRepositoryFetcher(
    private val filesByPath: Map<String, ByteArray>,
    private val listingsByPath: Map<String, List<GitHubEntry>>,
) : GitHubRepositoryFetcher {

    override fun listDirectory(org: String, repo: String, path: String): List<GitHubEntry>? = listingsByPath[path]

    override fun fetchBytes(url: String): ByteArray? = filesByPath[url]
}

class GitHubQueryDownloaderSpec : DescribeSpec({

    describe("GitHubQueryDownloader") {

        val spec = GrammarSpec(repo = "tree-sitter-fake", functionName = "tree_sitter_fake", queriesPath = "queries")

        it("fetches queries/highlights.scm and writes it to destination") {
            val fetcher = FakeGitHubRepositoryFetcher(
                filesByPath = mapOf("https://example.com/highlights.scm" to "(comment) @comment".toByteArray()),
                listingsByPath = mapOf(
                    "queries" to listOf(
                        GitHubEntry("highlights.scm", "file", "https://example.com/highlights.scm"),
                    ),
                ),
            )
            val downloader = GitHubQueryDownloader(fetcher)
            val destination = createTempDirectory("git-down-query-downloader-test-").resolve("out.scm")

            val result = runBlocking { downloader.download(spec, destination) }

            result shouldBe true
            Files.readString(destination) shouldBe "(comment) @comment"
        }

        it("returns false when the spec has no queriesPath") {
            val fetcher = FakeGitHubRepositoryFetcher(emptyMap(), emptyMap())
            val downloader = GitHubQueryDownloader(fetcher)
            val destination = createTempDirectory("git-down-query-downloader-test-").resolve("out.scm")

            val result = runBlocking {
                downloader.download(spec.copy(queriesPath = null), destination)
            }

            result shouldBe false
        }

        it("returns false when the queries directory listing fails") {
            val fetcher = FakeGitHubRepositoryFetcher(emptyMap(), emptyMap())
            val downloader = GitHubQueryDownloader(fetcher)
            val destination = createTempDirectory("git-down-query-downloader-test-").resolve("out.scm")

            val result = runBlocking { downloader.download(spec, destination) }

            result shouldBe false
        }

        it("returns false when the listing has no highlights.scm entry") {
            val fetcher = FakeGitHubRepositoryFetcher(
                filesByPath = emptyMap(),
                listingsByPath = mapOf("queries" to listOf(GitHubEntry("injections.scm", "file", "https://example.com/injections.scm"))),
            )
            val downloader = GitHubQueryDownloader(fetcher)
            val destination = createTempDirectory("git-down-query-downloader-test-").resolve("out.scm")

            val result = runBlocking { downloader.download(spec, destination) }

            result shouldBe false
        }

        it("returns false when fetching the file's bytes fails") {
            val fetcher = FakeGitHubRepositoryFetcher(
                filesByPath = emptyMap(),
                listingsByPath = mapOf(
                    "queries" to listOf(GitHubEntry("highlights.scm", "file", "https://example.com/highlights.scm")),
                ),
            )
            val downloader = GitHubQueryDownloader(fetcher)
            val destination = createTempDirectory("git-down-query-downloader-test-").resolve("out.scm")

            val result = runBlocking { downloader.download(spec, destination) }

            result shouldBe false
        }

    }

})
