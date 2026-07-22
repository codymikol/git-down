package com.codymikol.highlighting

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

/**
 * Compiles a downloaded grammar's C sources into the shared library tree-sitter dynamically
 * loads at runtime. tree-sitter-grammars only publishes source (and WASM) releases - not
 * prebuilt native binaries per platform - so producing the ".so" the issue calls for means
 * invoking a C compiler already on the host. If none is available, or compilation fails for
 * any reason, that's caught here and treated the same as any other unavailable grammar.
 */
object NativeCompiler {

    private val logger: Logger = LoggerFactory.getLogger(NativeCompiler::class.java)
    private val cCompilerCandidates = listOf("cc", "clang", "gcc")
    private val cppCompilerCandidates = listOf("c++", "clang++", "g++")

    fun sharedLibraryExtension(osName: String = System.getProperty("os.name")): String = when {
        osName.contains("win", ignoreCase = true) -> "dll"
        osName.contains("mac", ignoreCase = true) || osName.contains("darwin", ignoreCase = true) -> "dylib"
        else -> "so"
    }

    fun compile(
        sourceFiles: List<Path>,
        includeDir: Path,
        destination: Path,
        cCandidates: List<String> = cCompilerCandidates,
        cppCandidates: List<String> = cppCompilerCandidates,
    ): Boolean {
        if (sourceFiles.isEmpty()) return false
        val isCpp = sourceFiles.any { it.toString().endsWith(".cc") || it.toString().endsWith(".cpp") }
        val compiler = findCompiler(if (isCpp) cppCandidates else cCandidates) ?: run {
            logger.warn("No C compiler found on PATH; cannot build grammar $destination")
            return false
        }

        val sharedLibFlag = if (sharedLibraryExtension() == "dylib") "-dynamiclib" else "-shared"
        val command = listOf(compiler, sharedLibFlag, "-fPIC", "-O2", "-I", includeDir.toString(), "-o", destination.toString()) +
            sourceFiles.map { it.toString() }

        return try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            if (exitCode != 0) logger.error("Grammar compilation failed for $destination:\n$output")
            exitCode == 0
        } catch (e: Exception) {
            logger.error("Failed to invoke compiler for $destination", e)
            false
        }
    }

    private fun findCompiler(candidates: List<String>): String? = candidates.firstOrNull { candidate ->
        try {
            ProcessBuilder(candidate, "--version")
                .redirectErrorStream(true)
                .start()
                .also { it.inputStream.readAllBytes() }
                .waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

}
