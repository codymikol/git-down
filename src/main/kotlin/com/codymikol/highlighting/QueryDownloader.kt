package com.codymikol.highlighting

import java.nio.file.Path

interface QueryDownloader {
    suspend fun download(spec: GrammarSpec, destination: Path): Boolean
}
