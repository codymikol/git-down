package com.codymikol.highlighting

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

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

})
