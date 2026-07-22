package com.codymikol.highlighting

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.nio.file.Files
import kotlin.io.path.createTempDirectory

class PrepackagedGrammarDownloaderSpec : DescribeSpec({

    describe("PrepackagedGrammarDownloader") {

        val spec = GrammarSpec(repo = "tree-sitter-kotlin", functionName = "tree_sitter_kotlin")
        val extension = NativeCompiler.sharedLibraryExtension()

        it("writes the classpath resource for the grammar's platform to the destination") {
            var requestedPath: String? = null
            val downloader = PrepackagedGrammarDownloader { path ->
                requestedPath = path
                ByteArrayInputStream("native-bytes".toByteArray())
            }
            val destination = createTempDirectory("git-down-prepackaged-test-").resolve("out.$extension")

            val result = runBlocking { downloader.download(spec, destination) }

            result shouldBe true
            Files.readString(destination) shouldBe "native-bytes"
            requestedPath shouldBe "/lib/${GrammarPlatform.tag}-tree-sitter-kotlin.$extension"
        }

        it("returns false without writing a file when no prepackaged resource exists") {
            val downloader = PrepackagedGrammarDownloader { null }
            val destination = createTempDirectory("git-down-prepackaged-test-").resolve("out.$extension")

            val result = runBlocking { downloader.download(spec, destination) }

            result shouldBe false
            Files.exists(destination) shouldBe false
        }

    }

})
