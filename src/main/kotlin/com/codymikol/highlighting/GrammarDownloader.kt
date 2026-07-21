package com.codymikol.highlighting

import java.nio.file.Path

interface GrammarDownloader {
    suspend fun download(spec: GrammarSpec, destination: Path): Boolean
}
