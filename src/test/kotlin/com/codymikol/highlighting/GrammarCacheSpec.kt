package com.codymikol.highlighting

import com.codymikol.repositories.UserDirectoryRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.io.path.createTempDirectory

private class TestUserDirectoryRepository(private val dir: String) : UserDirectoryRepository() {
    override fun getUserDataDir(): String = dir
}

private class FakeGrammarDownloader(
    private val result: Boolean = true,
    private val onDownload: (GrammarSpec, Path) -> Unit = { _, destination ->
        destination.toFile().apply { parentFile.mkdirs() }.writeText("grammar-bytes")
    },
) : GrammarDownloader {
    var callCount = 0
        private set

    override suspend fun download(spec: GrammarSpec, destination: Path): Boolean {
        callCount++
        if (result) onDownload(spec, destination)
        return result
    }
}

class GrammarCacheSpec : DescribeSpec({

    describe("GrammarCache") {

        fun createCache(downloader: GrammarDownloader) = GrammarCache(
            TestUserDirectoryRepository(createTempDirectory("git-down-grammar-cache-test-").toString()),
            downloader,
        )

        it("returns null without downloading for an extension with no known grammar") {
            val downloader = FakeGrammarDownloader()

            val cache = createCache(downloader)
            val result = runBlocking { cache.ensureGrammar("notarealextension") }

            result shouldBe null
            downloader.callCount shouldBe 0
        }

        it("downloads the grammar on first request when nothing is cached") {
            val downloader = FakeGrammarDownloader()

            val cache = createCache(downloader)
            val result = runBlocking { cache.ensureGrammar("kt") }

            result?.toFile()?.readText() shouldBe "grammar-bytes"
            downloader.callCount shouldBe 1
        }

        it("returns the cached path without downloading again when the file is fresh") {
            val downloader = FakeGrammarDownloader()
            val cache = createCache(downloader)
            val firstResult = runBlocking { cache.ensureGrammar("kt") } // seed the cache
            downloader.callCount shouldBe 1

            val secondResult = runBlocking { cache.ensureGrammar("kt") }

            secondResult shouldBe firstResult
            downloader.callCount shouldBe 1
        }

        it("re-downloads when the cached grammar is older than a week") {
            val dir = createTempDirectory("git-down-grammar-cache-test-")
            var clock = Instant.parse("2026-01-01T00:00:00Z")
            val downloader = FakeGrammarDownloader()
            val cache = GrammarCache(TestUserDirectoryRepository(dir.toString()), downloader) { clock }

            runBlocking { cache.ensureGrammar("kt") }
            downloader.callCount shouldBe 1

            clock = clock.plus(Duration.ofDays(8))
            runBlocking { cache.ensureGrammar("kt") }

            downloader.callCount shouldBe 2
        }

        it("falls back to the stale cached grammar when re-downloading fails") {
            val dir = createTempDirectory("git-down-grammar-cache-test-")
            var clock = Instant.parse("2026-01-01T00:00:00Z")
            var succeed = true
            val downloader = FakeGrammarDownloader()
            val flakyDownloader = object : GrammarDownloader {
                override suspend fun download(spec: GrammarSpec, destination: Path): Boolean =
                    if (succeed) downloader.download(spec, destination) else false
            }
            val cache = GrammarCache(TestUserDirectoryRepository(dir.toString()), flakyDownloader) { clock }

            runBlocking { cache.ensureGrammar("kt") }
            clock = clock.plus(Duration.ofDays(8))
            succeed = false

            val result = runBlocking { cache.ensureGrammar("kt") }

            result?.toFile()?.readText() shouldBe "grammar-bytes"
        }

        it("returns null when downloading fails and nothing was ever cached") {
            val downloader = FakeGrammarDownloader(result = false)

            val cache = createCache(downloader)
            val result = runBlocking { cache.ensureGrammar("kt") }

            result shouldBe null
        }

        it("returns null instead of throwing when the downloader throws") {
            val downloader = object : GrammarDownloader {
                override suspend fun download(spec: GrammarSpec, destination: Path): Boolean {
                    throw RuntimeException("network is on fire")
                }
            }

            val cache = createCache(downloader)
            val result = runBlocking { cache.ensureGrammar("kt") }

            result shouldBe null
        }

    }

})
