package com.codymikol.data.map

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar
import java.util.Date

class CommitCardSpec : DescribeSpec({

    fun date(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Date =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }.time

    fun node(
        shortSha: String = "abc1234",
        shortMessage: String = "do a thing",
        authorName: String = "Ada Lovelace",
        authorEmail: String = "ada@example.com",
    ) = CommitGraphNode(
        sha = shortSha + "extended",
        shortSha = shortSha,
        shortMessage = shortMessage,
        authorName = authorName,
        authorEmail = authorEmail,
        date = date(2024, 1, 1),
        parentShas = emptyList(),
    )

    describe("CommitCard") {

        describe("title") {
            it("joins the short sha and the commit message") {
                CommitCard.title(node(shortSha = "abc1234", shortMessage = "fix bug")) shouldBe "abc1234: fix bug"
            }
        }

        describe("author") {
            it("renders the author name and email") {
                CommitCard.author(node(authorName = "Ada Lovelace", authorEmail = "ada@example.com")) shouldBe
                    "Author: Ada Lovelace <ada@example.com>"
            }
        }

        describe("date") {
            it("prefixes with Date: and includes date, time and relative label") {
                val commitDate = date(2024, 3, 5, 14, 30)
                val now = date(2024, 3, 5, 18, 0)
                CommitCard.date(commitDate, now) shouldBe "Date: 03/05/24, 2:30 PM Today"
            }
        }

        describe("relative") {
            val base = date(2024, 6, 15, 12, 0)

            it("is Today for the same calendar day") {
                CommitCard.relative(date(2024, 6, 15, 1, 0), base) shouldBe "Today"
            }
            it("is Today for a future date") {
                CommitCard.relative(date(2024, 6, 16, 1, 0), base) shouldBe "Today"
            }
            it("is Yesterday for the previous calendar day") {
                CommitCard.relative(date(2024, 6, 14, 23, 0), base) shouldBe "Yesterday"
            }
            it("counts whole days for less than a month") {
                CommitCard.relative(date(2024, 6, 5, 12, 0), base) shouldBe "10 Days Ago"
            }
            it("counts whole months for less than a year") {
                CommitCard.relative(date(2024, 2, 15, 12, 0), base) shouldBe "4 Months Ago"
            }
            it("counts whole years otherwise") {
                CommitCard.relative(date(2021, 6, 15, 12, 0), base) shouldBe "3 Years Ago"
            }
        }
    }
})
