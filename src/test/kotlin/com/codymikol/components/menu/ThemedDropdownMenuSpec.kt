package com.codymikol.components.menu

import androidx.compose.ui.Modifier
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldNotBe

class ThemedDropdownMenuSpec : DescribeSpec({

    describe("themedDropdownMenuModifier") {

        it("clips before painting the background, so corners stay rounded") {
            val elements = themedDropdownMenuModifier(Modifier)
                .foldIn(mutableListOf<String>()) { acc, element -> acc.apply { add(element.toString()) } }

            val clipIndex = elements.indexOfFirst { it.contains("clip", ignoreCase = true) }
            val backgroundIndex = elements.indexOfFirst { it.contains("background", ignoreCase = true) }

            clipIndex shouldNotBe -1
            backgroundIndex shouldNotBe -1
            clipIndex shouldBeLessThan backgroundIndex
        }

    }

})
