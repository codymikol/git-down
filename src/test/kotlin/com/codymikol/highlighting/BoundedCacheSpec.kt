package com.codymikol.highlighting

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class BoundedCacheSpec : DescribeSpec({

    describe("BoundedCache.getOrPut") {

        it("returns the cached value without recomputing it on a repeat key") {
            val cache = BoundedCache<String, Int>(maxSize = 2)
            var calls = 0

            cache.getOrPut("a") { calls++; 1 }
            cache.getOrPut("a") { calls++; 1 }

            calls shouldBe 1
        }

        it("evicts the least recently used entry once capacity is exceeded") {
            val cache = BoundedCache<String, Int>(maxSize = 2)
            cache.getOrPut("a") { 1 }
            cache.getOrPut("b") { 2 }
            cache.getOrPut("a") { 1 } // touch "a" so "b" is now the least-recently-used entry
            cache.getOrPut("c") { 3 } // exceeds capacity, evicts "b"

            var recomputed = false
            cache.getOrPut("b") { recomputed = true; 2 }

            recomputed shouldBe true
        }

    }

})
