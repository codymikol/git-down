package com.codymikol.highlighting

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private fun writeFakeCompiler(path: Path, exitCode: Int, argsLog: Path): Path {
    path.writeText(
        """
        #!/bin/bash
        if [ "${'$'}1" = "--version" ]; then
          echo "fake-cc"
          exit 0
        fi
        echo "${'$'}@" > "$argsLog"
        prev=""
        for arg in "${'$'}@"; do
          if [ "${'$'}prev" = "-o" ]; then
            touch "${'$'}arg"
          fi
          prev="${'$'}arg"
        done
        exit $exitCode
        """.trimIndent()
    )
    path.toFile().setExecutable(true)
    return path
}

class NativeCompilerSpec : DescribeSpec({

    describe("NativeCompiler.sharedLibraryExtension") {

        it("returns .dll for Windows") {
            NativeCompiler.sharedLibraryExtension("Windows 11") shouldBe "dll"
        }

        it("returns .dylib for Mac") {
            NativeCompiler.sharedLibraryExtension("Mac OS X") shouldBe "dylib"
        }

        it("returns .so for Linux and anything else") {
            NativeCompiler.sharedLibraryExtension("Linux") shouldBe "so"
        }

    }

    describe("NativeCompiler.compile") {

        it("invokes the first available compiler candidate and reports success") {
            val dir = createTempDirectory("git-down-native-compiler-test-")
            val argsLog = dir.resolve("args.log")
            val fakeCompiler = writeFakeCompiler(dir.resolve("fake-cc"), exitCode = 0, argsLog = argsLog)
            val source = dir.resolve("parser.c").apply { writeText("// not really compiled") }
            val destination = dir.resolve("grammar.so")

            val result = NativeCompiler.compile(
                sourceFiles = listOf(source),
                includeDir = dir,
                destination = destination,
                cCandidates = listOf("definitely-not-a-real-compiler", fakeCompiler.toString()),
            )

            result shouldBe true
            destination.exists() shouldBe true
            argsLog.readText() shouldContainArg source.toString()
        }

        it("returns false when no compiler candidate is available") {
            val dir = createTempDirectory("git-down-native-compiler-test-")
            val source = dir.resolve("parser.c").apply { writeText("// not really compiled") }

            val result = NativeCompiler.compile(
                sourceFiles = listOf(source),
                includeDir = dir,
                destination = dir.resolve("grammar.so"),
                cCandidates = listOf("definitely-not-a-real-compiler", "also-not-a-real-compiler"),
            )

            result shouldBe false
        }

        it("returns false when the compiler exits with a non-zero status") {
            val dir = createTempDirectory("git-down-native-compiler-test-")
            val argsLog = dir.resolve("args.log")
            val fakeCompiler = writeFakeCompiler(dir.resolve("fake-cc"), exitCode = 1, argsLog = argsLog)
            val source = dir.resolve("parser.c").apply { writeText("// not really compiled") }
            val destination = dir.resolve("grammar.so")

            val result = NativeCompiler.compile(
                sourceFiles = listOf(source),
                includeDir = dir,
                destination = destination,
                cCandidates = listOf(fakeCompiler.toString()),
            )

            result shouldBe false
        }

        it("returns false and does not attempt to compile when there are no source files") {
            val dir = createTempDirectory("git-down-native-compiler-test-")

            val result = NativeCompiler.compile(
                sourceFiles = emptyList(),
                includeDir = dir,
                destination = dir.resolve("grammar.so"),
                cCandidates = listOf("definitely-not-a-real-compiler"),
            )

            result shouldBe false
        }

    }

})

private infix fun String.shouldContainArg(expected: String) {
    (expected in this) shouldBe true
}
