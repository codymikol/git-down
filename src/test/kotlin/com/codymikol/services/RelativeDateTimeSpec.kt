package com.codymikol.services

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar
import java.util.Date

class RelativeDateTimeSpec : DescribeSpec({

    fun date(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Date =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }.time

    describe("RelativeDateTime.relative") {

        val base = date(2024, 6, 15, 12, 0)

        it("is Today for the same calendar day") {
            RelativeDateTime.relative(date(2024, 6, 15, 1, 0), base) shouldBe "Today"
        }
        it("is Today for a future date") {
            RelativeDateTime.relative(date(2024, 6, 16, 1, 0), base) shouldBe "Today"
        }
        it("is Yesterday for the previous calendar day") {
            RelativeDateTime.relative(date(2024, 6, 14, 23, 0), base) shouldBe "Yesterday"
        }
        it("counts whole days for less than a month") {
            RelativeDateTime.relative(date(2024, 6, 5, 12, 0), base) shouldBe "10 Days Ago"
        }
        it("counts whole months for less than a year") {
            RelativeDateTime.relative(date(2024, 2, 15, 12, 0), base) shouldBe "4 Months Ago"
        }
        it("counts whole years otherwise") {
            RelativeDateTime.relative(date(2021, 6, 15, 12, 0), base) shouldBe "3 Years Ago"
        }
    }
})
