package com.codymikol.highlighting

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

/**
 * tree-sitter-grammars (https://github.com/tree-sitter-grammars) publishes each grammar's C
 * source and a WASM build, but not a prebuilt native binary per platform. To end up with the
 * ".so" the issue calls for, this downloads the grammar's `src/` directory from GitHub and
 * compiles it locally with whatever C compiler is on the host's PATH (see [NativeCompiler]).
 * Missing compiler, network failure, or a bad build all just make this return false - the
 * caller ([GrammarCache]) treats that exactly like any other unavailable grammar.
 */
@Single
class SourceCompilingGrammarDownloader(
    private val fetcher: GitHubRepositoryFetcher = HttpGitHubRepositoryFetcher(),
    private val compile: (List<Path>, Path, Path) -> Boolean = NativeCompiler::compile,
) : GrammarDownloader {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SourceCompilingGrammarDownloader::class.java)
        private const val ORG = "tree-sitter-grammars"
    }

    override suspend fun download(spec: GrammarSpec, destination: Path): Boolean = withContext(Dispatchers.IO) {
        val buildDir = Files.createTempDirectory("git-down-grammar-build-")
        try {
            val srcDir = buildDir.resolve("src")
            if (!fetchDirectory(spec.repo, "src", srcDir)) return@withContext false

            val sourceFiles = Files.list(srcDir).use { it.toList() }
                .filter { it.toString().substringAfterLast('.') in setOf("c", "cc", "cpp") }
            if (sourceFiles.isEmpty()) {
                logger.error("No compilable sources found for grammar '${spec.repo}'")
                return@withContext false
            }

            compile(sourceFiles, srcDir, destination)
        } finally {
            buildDir.toFile().deleteRecursively()
        }
    }

    private fun fetchDirectory(repo: String, path: String, destination: Path): Boolean {
        val listing = fetcher.listDirectory(ORG, repo, path) ?: return false
        Files.createDirectories(destination)
        for (entry in listing) {
            // GitHub-supplied names, defensively rejected in case a compromised/malicious
            // response ever tried to escape the build directory.
            if (entry.name.contains('/') || entry.name.contains('\\') || entry.name == "..") {
                logger.warn("Skipping suspicious entry name '${entry.name}' in $repo/$path")
                continue
            }
            when (entry.type) {
                "file" -> {
                    val bytes = fetcher.fetchBytes(entry.downloadUrl ?: continue) ?: return false
                    Files.write(destination.resolve(entry.name), bytes)
                }
                "dir" -> if (!fetchDirectory(repo, "$path/${entry.name}", destination.resolve(entry.name))) return false
            }
        }
        return true
    }

}
