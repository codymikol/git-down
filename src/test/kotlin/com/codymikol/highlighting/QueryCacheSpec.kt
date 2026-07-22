package com.codymikol.highlighting

import com.codymikol.repositories.UserDirectoryRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.io.path.createTempDirectory

private class TestUserDirectoryRepository(private val dir: String) : UserDirectoryRepository() {
    override fun getUserDataDir(): String = dir
}

private class FakeQueryDownloader(
    private val result: Boolean = true,
    private val onDownload: (GrammarSpec, Path) -> Unit = { _, destination ->
        destination.toFile().apply { parentFile.mkdirs() }.writeText("(comment) @comment")
    },
) : QueryDownloader {
    var callCount = 0
        private set

    override suspend fun download(spec: GrammarSpec, destination: Path): Boolean {
        callCount++
        if (result) onDownload(spec, destination)
        return result
    }
}

class QueryCacheSpec : DescribeSpec({

    describe("QueryCache") {

        val tomlSpec = GrammarSpec(repo = "tree-sitter-toml", functionName = "tree_sitter_toml")

        fun createCache(downloader: QueryDownloader) = QueryCache(
            TestUserDirectoryRepository(createTempDirectory("git-down-query-cache-test-").toString()),
            downloader,
        )

        it("downloads the queries on first request when nothing is cached") {
            val downloader = FakeQueryDownloader()

            val cache = createCache(downloader)
            val result = runBlocking { cache.ensureQueries(tomlSpec) }

            result?.toFile()?.readText() shouldBe "(comment) @comment"
            downloader.callCount shouldBe 1
        }

        it("returns the cached path without downloading again when the file is fresh") {
            val downloader = FakeQueryDownloader()
            val cache = createCache(downloader)
            val firstResult = runBlocking { cache.ensureQueries(tomlSpec) }
            downloader.callCount shouldBe 1

            val secondResult = runBlocking { cache.ensureQueries(tomlSpec) }

            secondResult shouldBe firstResult
            downloader.callCount shouldBe 1
        }

        it("re-downloads when the cached queries file is older than a week") {
            val dir = createTempDirectory("git-down-query-cache-test-")
            var clock = Instant.parse("2026-01-01T00:00:00Z")
            val downloader = FakeQueryDownloader()
            val cache = QueryCache(TestUserDirectoryRepository(dir.toString()), downloader) { clock }

            runBlocking { cache.ensureQueries(tomlSpec) }
            downloader.callCount shouldBe 1

            clock = clock.plus(Duration.ofDays(8))
            runBlocking { cache.ensureQueries(tomlSpec) }

            downloader.callCount shouldBe 2
        }

        it("returns null without downloading when the spec has no queriesPath") {
            val downloader = FakeQueryDownloader()
            val cache = createCache(downloader)

            val result = runBlocking { cache.ensureQueries(tomlSpec.copy(queriesPath = null)) }

            result.shouldBeNull()
            downloader.callCount shouldBe 0
        }

        it("returns null when downloading fails and nothing was ever cached") {
            val downloader = FakeQueryDownloader(result = false)

            val cache = createCache(downloader)
            val result = runBlocking { cache.ensureQueries(tomlSpec) }

            result.shouldBeNull()
        }

        it("returns null instead of throwing when the downloader throws") {
            val downloader = object : QueryDownloader {
                override suspend fun download(spec: GrammarSpec, destination: Path): Boolean {
                    throw RuntimeException("network is on fire")
                }
            }

            val cache = createCache(downloader)
            val result = runBlocking { cache.ensureQueries(tomlSpec) }

            result.shouldBeNull()
        }

    }

})
