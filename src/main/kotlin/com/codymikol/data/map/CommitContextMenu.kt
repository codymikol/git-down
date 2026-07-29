package com.codymikol.data.map

/**
 * A single actionable entry in a commit node's right-click context menu (see
 * issue #253): a human label plus the keyboard shortcut that will eventually
 * trigger the same action. The concrete behaviours are wired up in follow-up
 * issues; this model only describes the menu's shape.
 */
data class CommitContextMenuAction(
    val label: String,
    val shortcut: String,
)

/**
 * The right-click context menu shown for a commit node in the map view. Actions
 * are split into groups; a divider is drawn between each group when rendered.
 * The grouping mirrors the divider layout described in issue #253.
 */
object CommitContextMenu {

    val groups: List<List<CommitContextMenuAction>> = listOf(
        listOf(
            CommitContextMenuAction("Quick View", "Space"),
        ),
        listOf(
            CommitContextMenuAction("Diff with HEAD...", "I"),
        ),
        listOf(
            CommitContextMenuAction("Checkout Detached HEAD", "Enter"),
        ),
        listOf(
            CommitContextMenuAction("Edit Message...", "E"),
            CommitContextMenuAction("Fixup with Parent", "F"),
            CommitContextMenuAction("Squash with Parent", "S"),
            CommitContextMenuAction("Swap with Parent (Move Down)", "D"),
            CommitContextMenuAction("Swap with Child (Move Up)", "U"),
            CommitContextMenuAction("Delete", "Delete"),
        ),
        listOf(
            CommitContextMenuAction("Rewrite...", "W"),
            CommitContextMenuAction("Split...", "Ctrl S"),
        ),
        listOf(
            CommitContextMenuAction("Revert Against Current Branch", "R"),
            CommitContextMenuAction("Cherry-Pick Against Current Branch", "C"),
            CommitContextMenuAction("Merge into Current Branch", "Ctrl M"),
            CommitContextMenuAction("Rebase Current Branch onto Here", "Ctrl R"),
            CommitContextMenuAction("Set Tip of Current Branch Here", "Ctrl T"),
        ),
        listOf(
            CommitContextMenuAction("Add Tag", "T"),
        ),
        listOf(
            CommitContextMenuAction("Create Branch", "B"),
        ),
    )
}
