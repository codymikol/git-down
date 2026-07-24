package com.codymikol.state

import androidx.compose.ui.input.key.Key
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class TextSizeShortcutSpec : DescribeSpec({

    describe("TextSizeShortcut.isShortcutModifierPressed") {

        it("requires the Meta key on macOS") {
            TextSizeShortcut.isShortcutModifierPressed(
                isCtrlPressed = false,
                isMetaPressed = true,
                osName = "Mac OS X"
            ) shouldBe true
        }

        it("ignores Ctrl on macOS") {
            TextSizeShortcut.isShortcutModifierPressed(
                isCtrlPressed = true,
                isMetaPressed = false,
                osName = "Mac OS X"
            ) shouldBe false
        }

        it("requires the Ctrl key on Linux") {
            TextSizeShortcut.isShortcutModifierPressed(
                isCtrlPressed = true,
                isMetaPressed = false,
                osName = "Linux"
            ) shouldBe true
        }

        it("ignores Meta on Linux") {
            TextSizeShortcut.isShortcutModifierPressed(
                isCtrlPressed = false,
                isMetaPressed = true,
                osName = "Linux"
            ) shouldBe false
        }

    }

    describe("TextSizeShortcut.isIncrease") {

        it("is true for the '=' key when the shortcut modifier is pressed") {
            TextSizeShortcut.isIncrease(Key.Equals, isShortcutModifierPressed = true) shouldBe true
        }

        it("is false for the '=' key when the shortcut modifier is not pressed") {
            TextSizeShortcut.isIncrease(Key.Equals, isShortcutModifierPressed = false) shouldBe false
        }

        it("is false for an unrelated key") {
            TextSizeShortcut.isIncrease(Key.A, isShortcutModifierPressed = true) shouldBe false
        }

    }

    describe("TextSizeShortcut.isDecrease") {

        it("is true for the '-' key when the shortcut modifier is pressed") {
            TextSizeShortcut.isDecrease(Key.Minus, isShortcutModifierPressed = true) shouldBe true
        }

        it("is false for the '-' key when the shortcut modifier is not pressed") {
            TextSizeShortcut.isDecrease(Key.Minus, isShortcutModifierPressed = false) shouldBe false
        }

        it("is false for an unrelated key") {
            TextSizeShortcut.isDecrease(Key.A, isShortcutModifierPressed = true) shouldBe false
        }

    }

})
