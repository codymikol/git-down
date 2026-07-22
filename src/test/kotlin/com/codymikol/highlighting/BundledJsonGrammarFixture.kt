package com.codymikol.highlighting

import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeBytes

/**
 * io.github.bonede:tree-sitter-json bundles a real prebuilt native grammar per platform, purely
 * as a test fixture - it lets specs exercise the tree-sitter binding against an actual compiled
 * shared library instead of only the safe-failure paths, without git-down's own
 * download-and-compile pipeline needing a C compiler in CI.
 */
object BundledJsonGrammarFixture {

    const val FUNCTION_NAME = "tree_sitter_json"

    private val platformTag: String = run {
        val arch = if (System.getProperty("os.arch").contains("aarch64")) "aarch64" else "x86_64"
        val os = System.getProperty("os.name").lowercase()
        val osTag = when {
            "mac" in os -> "macos"
            "win" in os -> "windows"
            else -> "linux-gnu"
        }
        "$arch-$osTag"
    }

    private val extension: String = when {
        "windows" in platformTag -> "dll"
        "macos" in platformTag -> "dylib"
        else -> "so"
    }

    fun extract(): Path {
        val resourcePath = "/lib/$platformTag-tree-sitter-json.$extension"
        val resource = javaClass.getResourceAsStream(resourcePath)
            ?: error("Test fixture not found on classpath: $resourcePath")

        val grammarFile = createTempDirectory("git-down-grammar-fixture-").resolve("json.$extension")
        grammarFile.writeBytes(resource.use { it.readBytes() })
        return grammarFile
    }

}
