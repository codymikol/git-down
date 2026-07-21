package com.codymikol.highlighting

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
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
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
    private val objectMapper: ObjectMapper = ObjectMapper(),
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

            NativeCompiler.compile(sourceFiles, srcDir, destination)
        } finally {
            buildDir.toFile().deleteRecursively()
        }
    }

    private fun fetchDirectory(repo: String, path: String, destination: Path): Boolean {
        val listing = fetchJson("https://api.github.com/repos/$ORG/$repo/contents/$path") ?: return false
        Files.createDirectories(destination)
        for (entry in listing) {
            val name = entry["name"].asText()
            when (entry["type"].asText()) {
                "file" -> {
                    val downloadUrl = entry["download_url"]?.takeIf { !it.isNull }?.asText() ?: continue
                    val bytes = fetchBytes(downloadUrl) ?: return false
                    Files.write(destination.resolve(name), bytes)
                }
                "dir" -> if (!fetchDirectory(repo, "$path/$name", destination.resolve(name))) return false
            }
        }
        return true
    }

    private fun fetchJson(url: String): JsonNode? = try {
        val request = HttpRequest.newBuilder(URI.create(url))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "git-down")
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) null else objectMapper.readTree(response.body())
    } catch (e: Exception) {
        logger.error("Failed to fetch $url", e)
        null
    }

    private fun fetchBytes(url: String): ByteArray? = try {
        val request = HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "git-down").GET().build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() != 200) null else response.body()
    } catch (e: Exception) {
        logger.error("Failed to download $url", e)
        null
    }

}
