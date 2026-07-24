package com.codymikol.state

import androidx.compose.ui.input.key.Key

object TextSizeShortcut {

    fun isShortcutModifierPressed(
        isCtrlPressed: Boolean,
        isMetaPressed: Boolean,
        osName: String = System.getProperty("os.name") ?: "",
    ): Boolean = if (osName.contains("Mac", ignoreCase = true)) isMetaPressed else isCtrlPressed

    fun isIncrease(key: Key, isShortcutModifierPressed: Boolean): Boolean =
        isShortcutModifierPressed && (key == Key.Equals || key == Key.NumPadAdd)

    fun isDecrease(key: Key, isShortcutModifierPressed: Boolean): Boolean =
        isShortcutModifierPressed && (key == Key.Minus || key == Key.NumPadSubtract)

}
