package com.codymikol.services

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.net.URI

class LinkOpenerServiceSpec : DescribeSpec({

    describe("LinkOpenerService") {

        describe("open") {

            it("should route a mailto: link to the mail client, not the browser") {
                val service = LinkOpenerService()
                var browsed: URI? = null
                var mailed: URI? = null

                service.open(
                    "mailto:hi@codymikol.com",
                    browse = { browsed = it },
                    mail = { mailed = it },
                )

                mailed shouldBe URI.create("mailto:hi@codymikol.com")
                browsed shouldBe null
            }

            it("should match the mailto scheme case-insensitively") {
                val service = LinkOpenerService()
                var browsed: URI? = null
                var mailed: URI? = null

                service.open(
                    "MAILTO:hi@codymikol.com",
                    browse = { browsed = it },
                    mail = { mailed = it },
                )

                mailed shouldBe URI.create("MAILTO:hi@codymikol.com")
                browsed shouldBe null
            }

            it("should route an https: link to the browser, not the mail client") {
                val service = LinkOpenerService()
                var browsed: URI? = null
                var mailed: URI? = null

                service.open(
                    "https://gitup.co/",
                    browse = { browsed = it },
                    mail = { mailed = it },
                )

                browsed shouldBe URI.create("https://gitup.co/")
                mailed shouldBe null
            }

        }

    }

})
