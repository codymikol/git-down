package com.codymikol.highlighting

import com.codymikol.repositories.UserDirectoryRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory

private class GrammarCacheUserDirectoryRepository(private val dir: String) : UserDirectoryRepository() {
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

        val kotlinSpec = GrammarSpec(repo = "tree-sitter-kotlin", functionName = "tree_sitter_kotlin")

        fun createCache(downloader: GrammarDownloader) = GrammarCache(
            GrammarCacheUserDirectoryRepository(createTempDirectory("git-down-grammar-cache-test-").toString()),
            downloader,
        )

        it("downloads the grammar on first request when nothing is cached") {
            val downloader = FakeGrammarDownloader()

            val cache = createCache(downloader)
            val result = runBlocking { cache.ensureGrammar(kotlinSpec) }

            result?.toFile()?.readText() shouldBe "grammar-bytes"
            downloader.callCount shouldBe 1
        }

        it("returns the cached path without downloading again when the file is fresh") {
            val downloader = FakeGrammarDownloader()
            val cache = createCache(downloader)
            val firstResult = runBlocking { cache.ensureGrammar(kotlinSpec) } // seed the cache
            downloader.callCount shouldBe 1

            val secondResult = runBlocking { cache.ensureGrammar(kotlinSpec) }

            secondResult shouldBe firstResult
            downloader.callCount shouldBe 1
        }

        it("re-downloads when the cached grammar is older than a week") {
            val dir = createTempDirectory("git-down-grammar-cache-test-")
            var clock = Instant.parse("2026-01-01T00:00:00Z")
            val downloader = FakeGrammarDownloader()
            val cache = GrammarCache(GrammarCacheUserDirectoryRepository(dir.toString()), downloader) { clock }

            runBlocking { cache.ensureGrammar(kotlinSpec) }
            downloader.callCount shouldBe 1

            clock = clock.plus(Duration.ofDays(8))
            runBlocking { cache.ensureGrammar(kotlinSpec) }

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
            val cache = GrammarCache(GrammarCacheUserDirectoryRepository(dir.toString()), flakyDownloader) { clock }

            runBlocking { cache.ensureGrammar(kotlinSpec) }
            clock = clock.plus(Duration.ofDays(8))
            succeed = false

            val result = runBlocking { cache.ensureGrammar(kotlinSpec) }

            result?.toFile()?.readText() shouldBe "grammar-bytes"
        }

        it("returns null when downloading fails and nothing was ever cached") {
            val downloader = FakeGrammarDownloader(result = false)

            val cache = createCache(downloader)
            val result = runBlocking { cache.ensureGrammar(kotlinSpec) }

            result shouldBe null
        }

        it("returns null instead of throwing when the downloader throws") {
            val downloader = object : GrammarDownloader {
                override suspend fun download(spec: GrammarSpec, destination: Path): Boolean {
                    throw RuntimeException("network is on fire")
                }
            }

            val cache = createCache(downloader)
            val result = runBlocking { cache.ensureGrammar(kotlinSpec) }

            result shouldBe null
        }

        it("serializes concurrent requests for the same grammar so only one download happens") {
            val concurrentCalls = AtomicInteger(0)
            val maxConcurrentCalls = AtomicInteger(0)
            val totalCalls = AtomicInteger(0)
            val downloader = object : GrammarDownloader {
                override suspend fun download(spec: GrammarSpec, destination: Path): Boolean {
                    totalCalls.incrementAndGet()
                    maxConcurrentCalls.updateAndGet { maxOf(it, concurrentCalls.incrementAndGet()) }
                    delay(10)
                    destination.toFile().apply { parentFile.mkdirs() }.writeText("grammar-bytes")
                    concurrentCalls.decrementAndGet()
                    return true
                }
            }
            val cache = createCache(downloader)

            // Real OS-thread parallelism (not just single-threaded coroutine interleaving) is
            // required to exercise the ConcurrentHashMap.computeIfAbsent race this guards.
            runBlocking(Dispatchers.Default) {
                coroutineScope {
                    repeat(50) { launch { cache.ensureGrammar(kotlinSpec) } }
                }
            }

            maxConcurrentCalls.get() shouldBe 1
            totalCalls.get() shouldBe 1
        }

        it("serializes concurrent requests for specs that share the same grammar repo") {
            val concurrentCalls = AtomicInteger(0)
            val maxConcurrentCalls = AtomicInteger(0)
            val totalCalls = AtomicInteger(0)
            val downloader = object : GrammarDownloader {
                override suspend fun download(spec: GrammarSpec, destination: Path): Boolean {
                    totalCalls.incrementAndGet()
                    maxConcurrentCalls.updateAndGet { maxOf(it, concurrentCalls.incrementAndGet()) }
                    delay(10)
                    destination.toFile().apply { parentFile.mkdirs() }.writeText("grammar-bytes")
                    concurrentCalls.decrementAndGet()
                    return true
                }
            }
            val cache = createCache(downloader)

            // "kt" and "kts" both resolve to a GrammarSpec with the same repo/destination file.
            val ktSpec = GrammarSpec(repo = "tree-sitter-kotlin", functionName = "tree_sitter_kotlin")
            val ktsSpec = GrammarSpec(repo = "tree-sitter-kotlin", functionName = "tree_sitter_kotlin")
            runBlocking(Dispatchers.Default) {
                coroutineScope {
                    repeat(25) { launch { cache.ensureGrammar(ktSpec) } }
                    repeat(25) { launch { cache.ensureGrammar(ktsSpec) } }
                }
            }

            maxConcurrentCalls.get() shouldBe 1
            totalCalls.get() shouldBe 1
        }

    }

})
