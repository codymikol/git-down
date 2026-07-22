package com.codymikol.highlighting

import org.koin.core.annotation.Single
import java.nio.file.Path

/**
 * Prefers [PrepackagedGrammarDownloader]'s classpath-resource extraction, falling back to
 * [SourceCompilingGrammarDownloader]'s GitHub-fetch-and-compile path only for the few grammars
 * upstream doesn't publish a prepackaged io.github.bonede artifact for (vim, csv, tcl).
 */
@Single
class CompositeGrammarDownloader(
    private val prepackaged: PrepackagedGrammarDownloader,
    private val sourceCompiling: SourceCompilingGrammarDownloader,
) : GrammarDownloader {

    override suspend fun download(spec: GrammarSpec, destination: Path): Boolean =
        prepackaged.download(spec, destination) || sourceCompiling.download(spec, destination)

}
