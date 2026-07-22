package com.codymikol.highlighting

/**
 * The architecture-os identifier io.github.bonede's prepackaged tree-sitter-<language> jars
 * embed their per-platform native library resource under (e.g. "aarch64-macos",
 * "x86_64-linux-gnu"), distinct from [NativeCompiler.sharedLibraryExtension]'s simpler
 * dll/dylib/so distinction for that same resource's file extension.
 */
object GrammarPlatform {

    val tag: String = run {
        val arch = if (System.getProperty("os.arch").contains("aarch64")) "aarch64" else "x86_64"
        val osName = System.getProperty("os.name").lowercase()
        val osTag = when {
            "mac" in osName -> "macos"
            "win" in osName -> "windows"
            else -> "linux-gnu"
        }
        "$arch-$osTag"
    }

}
