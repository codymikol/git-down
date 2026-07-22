package com.codymikol.highlighting

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.treesitter.TSLanguage
import org.treesitter.TSQuery
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Compiles a cached `highlights.scm` file into a [TSQuery] for a grammar's [TSLanguage].
 * Compiling untrusted/foreign query text is inherently unsafe (syntax errors, capture names the
 * binding rejects), so every failure mode here is caught and turned into a null rather than a
 * crash - mirrors [GrammarLanguageLoader]'s handling of the compiled grammar itself.
 *
 * Compiled queries are cached by (path, mtime): a diff view re-renders the same grammar for every
 * visible line, and re-parsing the same query text on each one would be wasteful.
 */
object QueryLoader {

    private val logger: Logger = LoggerFactory.getLogger(QueryLoader::class.java)
    private val cache = ConcurrentHashMap<String, TSQuery>()

    fun load(language: TSLanguage, path: Path): TSQuery? {
        if (!Files.exists(path)) return null
        val mtime = try { Files.getLastModifiedTime(path).toMillis() } catch (e: Exception) { 0L }
        val key = "$path|$mtime"
        cache[key]?.let { return it }
        return try {
            val text = Files.readString(path)
            TSQuery(language, text).also { cache[key] = it }
        } catch (e: Throwable) {
            logger.error("Failed to compile highlight query from $path", e)
            null
        }
    }

}
