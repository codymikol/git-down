package com.codymikol.extensions

import androidx.compose.ui.awt.ComposeWindow
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener


fun ComposeWindow.onFocusGained(fn: () -> Unit)  {
    addWindowFocusListener(object : WindowFocusListener {
        override fun windowGainedFocus(p0: WindowEvent?) {
            println("gained")
            fn()
        }
        override fun windowLostFocus(p0: WindowEvent?) {}
    })
}

fun ComposeWindow.onFocusLost(fn: () -> Unit)  {
    addWindowFocusListener(object : WindowFocusListener {
        override fun windowGainedFocus(p0: WindowEvent?) {}
        override fun windowLostFocus(p0: WindowEvent?) {
            println("lost")
            fn()
        }
    })
}
