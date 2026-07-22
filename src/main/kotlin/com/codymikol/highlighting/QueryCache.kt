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
 * Lazily fetches and caches each grammar's `highlights.scm` under git-down's managed data
 * directory, keyed by grammar repo - mirrors [GrammarCache]'s staleness/locking behavior for the
 * compiled grammar itself. Any failure here is caught and logged so a broken/missing query file
 * can never take down the diff view - callers just get null back and fall back to the
 * heuristic-only highlighting [GrammarParser] already provides.
 */
@Single
class QueryCache(
    private val userDirectoryRepository: UserDirectoryRepository,
    private val downloader: QueryDownloader,
    private val now: () -> Instant = Instant::now,
) {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(QueryCache::class.java)
        private val staleAfter: Duration = Duration.ofDays(7)
    }

    private val locksByRepo = ConcurrentHashMap<String, Mutex>()

    suspend fun ensureQueries(spec: GrammarSpec): Path? {
        if (spec.queriesPath == null) return null
        val lock = locksByRepo.computeIfAbsent(spec.repo) { Mutex() }
        return lock.withLock {
            try {
                val path = queriesPath(spec)
                if (Files.exists(path) && !isStale(path)) return@withLock path
                if (attemptDownload(spec, path)) path
                else if (Files.exists(path)) path
                else null
            } catch (e: Exception) {
                logger.error("Failed to ensure highlight queries for '${spec.repo}'", e)
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
        logger.error("Failed to download highlight queries for '${spec.repo}'", e)
        false
    }

    private fun queriesPath(spec: GrammarSpec): Path = Paths.get(
        requireNotNull(userDirectoryRepository.getUserDataDir()),
        "queries",
        "${spec.repo}.scm",
    )

}
