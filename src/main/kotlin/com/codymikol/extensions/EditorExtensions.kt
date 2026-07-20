package com.codymikol.extensions

import org.eclipse.jgit.api.Git
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File
import java.nio.file.Path

private val logger = LoggerFactory.getLogger("EditorExtensions")

private fun openWithSystemDefaultEditor(file: File) {
    if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
        logger.error("Desktop open is not supported on this platform, unable to open file: ${file.absolutePath}")
        return
    }
    Desktop.getDesktop().open(file)
}

/**
 *  Always opens via the OS's default file handler. A configured git editor
 *  (GIT_EDITOR/core.editor) is meant for git's own short-lived, terminal-attached
 *  edits (commit messages, rebase todo) — spawning it here detached from any
 *  terminal silently does nothing for common terminal editors (vim, nano, ...),
 *  which is why "Open File" must not route through it.
 */
fun Git.openFile(path: Path, open: (File) -> Unit = ::openWithSystemDefaultEditor): Unit = try {
    open(File(this.repository.workTree, path.toString()))
} catch (e: Exception) {
    logger.error("An exception was thrown while opening file $path: ${e.message}")
}
