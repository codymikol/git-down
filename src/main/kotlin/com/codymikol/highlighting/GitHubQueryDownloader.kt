package com.codymikol.highlighting

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.nio.file.Files
import java.nio.file.Path

/**
 * Fetches a grammar's `queries/highlights.scm` from tree-sitter-grammars, the same source
 * [SourceCompilingGrammarDownloader] pulls each grammar's C sources from, so the captures used
 * for highlighting come from the grammar author's own query definitions rather than a
 * hand-rolled heuristic.
 */
@Single
class GitHubQueryDownloader(
    private val fetcher: GitHubRepositoryFetcher = HttpGitHubRepositoryFetcher(),
) : QueryDownloader {

    companion object {
        private const val ORG = "tree-sitter-grammars"
        private const val QUERY_FILE_NAME = "highlights.scm"
    }

    override suspend fun download(spec: GrammarSpec, destination: Path): Boolean = withContext(Dispatchers.IO) {
        val queriesPath = spec.queriesPath ?: return@withContext false
        val listing = fetcher.listDirectory(ORG, spec.repo, queriesPath) ?: return@withContext false
        val entry = listing.firstOrNull { it.name == QUERY_FILE_NAME && it.type == "file" } ?: return@withContext false
        val bytes = fetcher.fetchBytes(entry.downloadUrl ?: return@withContext false) ?: return@withContext false
        // Destination's parent directory is [QueryCache]'s responsibility, same as
        // SourceCompilingGrammarDownloader leaves it to GrammarCache.
        Files.write(destination, bytes)
        true
    }

}
