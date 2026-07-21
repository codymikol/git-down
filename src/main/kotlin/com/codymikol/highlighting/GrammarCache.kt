package com.codymikol.highlighting

import com.codymikol.repositories.UserDirectoryRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.FileTime
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Lazily fetches and caches tree-sitter grammars (as compiled shared libraries) under
 * git-down's managed data directory, keyed by file extension. Any failure to resolve, stat,
 * or download a grammar is caught and logged here so a broken grammar can never take down the
 * diff view - callers just get null back and fall back to unhighlighted text.
 */
@Single
class GrammarCache(
    private val userDirectoryRepository: UserDirectoryRepository,
    private val downloader: GrammarDownloader,
    private val now: () -> Instant = Instant::now,
) {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(GrammarCache::class.java)
        private val staleAfter: Duration = Duration.ofDays(7)
    }

    // A diff view opens with every visible line hitting this at once for the same extension;
    // without serializing per extension they'd race to (re)compile the same destination file.
    private val locksByExtension = ConcurrentHashMap<String, Mutex>()

    suspend fun ensureGrammar(extension: String): Path? {
        val spec = GrammarExtensionRegistry.forExtension(extension) ?: return null
        val lock = locksByExtension.getOrPut(extension) { Mutex() }
        return lock.withLock {
            try {
                val path = grammarPath(spec)
                if (Files.exists(path) && !isStale(path)) return@withLock path
                if (attemptDownload(spec, path)) path
                else if (Files.exists(path)) path
                else null
            } catch (e: Exception) {
                logger.error("Failed to ensure grammar for extension '$extension'", e)
                null
            }
        }
    }

    private fun isStale(path: Path): Boolean {
        val modifiedAt = Files.getLastModifiedTime(path).toInstant()
        return Duration.between(modifiedAt, now()) > staleAfter
    }

    private suspend fun attemptDownload(spec: GrammarSpec, destination: Path): Boolean = try {
        Files.createDirectories(destination.parent)
        val succeeded = downloader.download(spec, destination)
        if (succeeded) Files.setLastModifiedTime(destination, FileTime.from(now()))
        succeeded
    } catch (e: Exception) {
        logger.error("Failed to download grammar '${spec.repo}'", e)
        false
    }

    private fun grammarPath(spec: GrammarSpec): Path = Paths.get(
        requireNotNull(userDirectoryRepository.getUserDataDir()),
        "grammars",
        "${spec.repo}.${NativeCompiler.sharedLibraryExtension()}",
    )

}
