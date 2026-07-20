package com.codymikol.services

import org.koin.core.annotation.Single
import java.awt.Dimension
import java.awt.Toolkit

@Single
class WindowSizeService {

    internal var screenSizeProvider: () -> Dimension = ::primaryScreenSize

    /**
     * Sizes the window relative to the current display so it comfortably fits its
     * contents on both small laptop screens and large monitors, never shrinking
     * below the app's minimum usable size.
     */
    fun getDefaultWindowSize(): Dimension {
        val screenSize = screenSizeProvider()
        val width = (screenSize.width * SIZE_RATIO).toInt().coerceAtLeast(MIN_WIDTH)
        val height = (screenSize.height * SIZE_RATIO).toInt().coerceAtLeast(MIN_HEIGHT)
        return Dimension(width, height)
    }

    companion object {
        private const val SIZE_RATIO = 0.8

        const val MIN_WIDTH = 800
        const val MIN_HEIGHT = 500

        private fun primaryScreenSize(): Dimension = Toolkit.getDefaultToolkit().screenSize
    }

}
