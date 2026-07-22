package com.codymikol.highlighting

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.string.shouldMatch

class GrammarPlatformSpec : DescribeSpec({

    describe("GrammarPlatform") {

        it("resolves an arch-os tag matching the format io.github.bonede jars embed resources under") {
            GrammarPlatform.tag shouldMatch "(aarch64|x86_64)-(macos|windows|linux-gnu)"
        }

    }

})
