package tabs

sealed class Tab {
    object Commit : Tab()
    object Stash : Tab()
    object Map : Tab()
}