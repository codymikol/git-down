package com.codymikol.highlighting

data class GitHubEntry(
    val name: String,
    val type: String,
    val downloadUrl: String?,
)

/**
 * Seam over the GitHub REST API calls [SourceCompilingGrammarDownloader] needs, so its
 * directory-walk/filter logic can be tested against canned responses instead of the network.
 */
interface GitHubRepositoryFetcher {
    fun listDirectory(org: String, repo: String, path: String): List<GitHubEntry>?
    fun fetchBytes(url: String): ByteArray?
}
