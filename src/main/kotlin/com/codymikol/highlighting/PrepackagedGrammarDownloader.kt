package com.codymikol.highlighting

import org.koin.core.annotation.Single
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * io.github.bonede publishes most tree-sitter-grammars repos as Maven artifacts
 * (io.github.bonede:tree-sitter-<language>) with a prebuilt native library embedded as a JAR
 * resource per platform - the same artifact [BundledJsonGrammarFixture] pulls in purely as a
 * test fixture. Extracting that resource straight to disk sidesteps
 * [SourceCompilingGrammarDownloader]'s GitHub-fetch-and-compile path entirely wherever such an
 * artifact is on the classpath; returns false (grammar unavailable here) for the few grammars
 * upstream only ships source for, same as any other unavailable grammar.
 */
@Single(binds = [PrepackagedGrammarDownloader::class])
class PrepackagedGrammarDownloader(
    private val openResource: (String) -> InputStream? = { PrepackagedGrammarDownloader::class.java.getResourceAsStream(it) },
) : GrammarDownloader {

    override suspend fun download(spec: GrammarSpec, destination: Path): Boolean {
        val language = spec.repo.removePrefix("tree-sitter-")
        val extension = NativeCompiler.sharedLibraryExtension()
        val resourcePath = "/lib/${GrammarPlatform.tag}-tree-sitter-$language.$extension"
        val resource = openResource(resourcePath) ?: return false
        resource.use { Files.copy(it, destination, StandardCopyOption.REPLACE_EXISTING) }
        return true
    }

}
