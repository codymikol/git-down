package com.codymikol.tabs

sealed class Tab {
    object Commit : Tab()
    object Stash : Tab()
    object Map : Tab()

    // Launched against a specific commit rather than reached from the tab bar; the
    // quick view (see issue #254) is an ephemeral overlay that restores the prior
    // tab when dismissed.
    object QuickView : Tab()
}