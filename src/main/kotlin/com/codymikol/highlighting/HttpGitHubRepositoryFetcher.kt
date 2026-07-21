package com.codymikol.highlighting

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class HttpGitHubRepositoryFetcher(
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
    private val objectMapper: ObjectMapper = ObjectMapper(),
) : GitHubRepositoryFetcher {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(HttpGitHubRepositoryFetcher::class.java)
    }

    override fun listDirectory(org: String, repo: String, path: String): List<GitHubEntry>? = try {
        val request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/$org/$repo/contents/$path"))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "git-down")
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) null else objectMapper.readTree(response.body()).map { entry ->
            GitHubEntry(
                name = entry["name"].asText(),
                type = entry["type"].asText(),
                downloadUrl = entry["download_url"]?.takeIf { !it.isNull }?.asText(),
            )
        }
    } catch (e: Exception) {
        logger.error("Failed to list $org/$repo/$path", e)
        null
    }

    override fun fetchBytes(url: String): ByteArray? = try {
        val request = HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "git-down").GET().build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() != 200) null else response.body()
    } catch (e: Exception) {
        logger.error("Failed to download $url", e)
        null
    }

}
