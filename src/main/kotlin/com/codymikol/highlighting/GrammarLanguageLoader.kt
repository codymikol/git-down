package com.codymikol.highlighting

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.treesitter.TSLanguage
import java.nio.file.Files
import java.nio.file.Path

/**
 * Thin wrapper around [TSLanguage.load] - dynamically loading a compiled grammar (a .so/.dylib/
 * .dll) is inherently unsafe (missing file, wrong ABI, corrupt download, wrong function name),
 * so every failure mode here is caught and turned into a null rather than a crash.
 */
object GrammarLanguageLoader {

    private val logger: Logger = LoggerFactory.getLogger(GrammarLanguageLoader::class.java)

    fun load(path: Path, functionName: String): TSLanguage? {
        if (!Files.exists(path)) return null
        return try {
            TSLanguage.load(path.toString(), functionName)
        } catch (e: Throwable) {
            logger.error("Failed to load grammar '$functionName' from $path", e)
            null
        }
    }

}
