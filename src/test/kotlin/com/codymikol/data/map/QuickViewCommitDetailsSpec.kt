package com.codymikol.data.map

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.util.Calendar
import java.util.Date

class QuickViewCommitDetailsSpec : DescribeSpec({

    fun date(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Date =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }.time

    fun node(
        sha: String = "abc1234def5678",
        shortMessage: String = "do a thing",
        authorName: String = "Ada Lovelace",
        authorEmail: String = "ada@example.com",
    ) = CommitGraphNode(
        sha = sha,
        shortSha = sha.take(7),
        shortMessage = shortMessage,
        authorName = authorName,
        authorEmail = authorEmail,
        date = date(2024, 1, 1),
        parentShas = emptyList(),
    )

    describe("QuickViewCommitDetails") {

        describe("hash") {
            it("is the commit's full sha") {
                QuickViewCommitDetails.hash(node(sha = "abc1234def5678")) shouldBe "abc1234def5678"
            }
        }

        describe("message") {
            it("is the commit message") {
                QuickViewCommitDetails.message(node(shortMessage = "fix bug")) shouldBe "fix bug"
            }
        }

        describe("committer") {
            it("renders the committer name and email") {
                QuickViewCommitDetails.committer(node(authorName = "Ada Lovelace", authorEmail = "ada@example.com")) shouldBe
                    "Ada Lovelace <ada@example.com>"
            }
        }

        describe("date") {
            it("renders date, time and relative label without a prefix") {
                val commitDate = date(2024, 3, 5, 14, 30)
                val now = date(2024, 3, 5, 18, 0)
                QuickViewCommitDetails.date(commitDate, now) shouldBe "03/05/24, 2:30 PM Today"
            }
        }
    }
})
