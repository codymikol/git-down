package com.codymikol.highlighting

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GrammarParserSpec : DescribeSpec({

    describe("GrammarParser") {

        it("returns an empty token list instead of parsing when no language is available") {
            val result = GrammarParser.parse(null, "val x = 1")

            result shouldBe emptyList()
        }

    }

})
