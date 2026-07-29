package com.codymikol.data.map

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class CommitContextMenuSpec : DescribeSpec({

    describe("CommitContextMenu") {

        it("groups every action as laid out in the issue spec") {
            val labelsByGroup = CommitContextMenu.groups.map { group -> group.map { it.label } }

            labelsByGroup shouldContainExactly listOf(
                listOf("Quick View"),
                listOf("Diff with HEAD..."),
                listOf("Checkout Detached HEAD"),
                listOf(
                    "Edit Message...",
                    "Fixup with Parent",
                    "Squash with Parent",
                    "Swap with Parent (Move Down)",
                    "Swap with Child (Move Up)",
                    "Delete",
                ),
                listOf("Rewrite...", "Split..."),
                listOf(
                    "Revert Against Current Branch",
                    "Cherry-Pick Against Current Branch",
                    "Merge into Current Branch",
                    "Rebase Current Branch onto Here",
                    "Set Tip of Current Branch Here",
                ),
                listOf("Add Tag"),
                listOf("Create Branch"),
            )
        }

        it("pairs each action with its keyboard shortcut") {
            val actions = CommitContextMenu.groups.flatten().associate { it.label to it.shortcut }

            actions["Quick View"] shouldBe "Space"
            actions["Diff with HEAD..."] shouldBe "I"
            actions["Checkout Detached HEAD"] shouldBe "Enter"
            actions["Delete"] shouldBe "Delete"
            actions["Split..."] shouldBe "Ctrl S"
            actions["Merge into Current Branch"] shouldBe "Ctrl M"
            actions["Rebase Current Branch onto Here"] shouldBe "Ctrl R"
            actions["Set Tip of Current Branch Here"] shouldBe "Ctrl T"
            actions["Create Branch"] shouldBe "B"
        }
    }
})
