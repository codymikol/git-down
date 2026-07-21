package com.codymikol.highlighting

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.treesitter.TSLanguage
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Thin wrapper around [TSLanguage.load] - dynamically loading a compiled grammar (a .so/.dylib/
 * .dll) is inherently unsafe (missing file, wrong ABI, corrupt download, wrong function name),
 * so every failure mode here is caught and turned into a null rather than a crash.
 *
 * Loaded languages are cached by (path, functionName): a diff view re-renders the same grammar
 * for every visible line, and dlopen-ing the native library again on each one would be both slow
 * and wasteful of native handles.
 */
object GrammarLanguageLoader {

    private val logger: Logger = LoggerFactory.getLogger(GrammarLanguageLoader::class.java)
    private val cache = ConcurrentHashMap<String, TSLanguage>()

    fun load(path: Path, functionName: String): TSLanguage? {
        if (!Files.exists(path)) return null
        // Include the file's mtime in the cache key so a grammar recompiled in place by
        // GrammarCache's staleness check (same path, new contents) isn't served stale forever.
        val mtime = try { Files.getLastModifiedTime(path).toMillis() } catch (e: Exception) { 0L }
        val key = "$path|$functionName|$mtime"
        cache[key]?.let { return it }
        return try {
            TSLanguage.load(path.toString(), functionName)?.also { cache[key] = it }
        } catch (e: Throwable) {
            logger.error("Failed to load grammar '$functionName' from $path", e)
            null
        }
    }

}
