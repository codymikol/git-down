package com.codymikol.extensions

import org.eclipse.jgit.api.Git
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File
import java.nio.file.Path

private val logger = LoggerFactory.getLogger("EditorExtensions")

/**
 *  Mirrors git's own precedence for choosing an editor: the GIT_EDITOR
 *  environment variable wins over the repository's core.editor config.
 *  Returns null when neither is configured so callers can fall back to
 *  the system's default file handler.
 */
fun Git.getConfiguredEditor(getenv: (String) -> String? = System::getenv): String? =
    getenv("GIT_EDITOR")?.takeIf { it.isNotBlank() }
        ?: this.repository.config.getString("core", null, "editor")?.takeIf { it.isNotBlank() }

/**
 *  Splits a configured editor command (which may itself carry flags, e.g.
 *  `code --wait`, and quoted paths, e.g. `"/opt/My Editor/bin/edit" -w`)
 *  into process arguments and appends the target file, without involving a
 *  shell.
 */
fun buildEditorCommand(editorCommand: String, file: File): List<String> {
    val tokenPattern = Regex("""'([^']*)'|"([^"]*)"|(\S+)""")
    val tokens = tokenPattern.findAll(editorCommand.trim()).map { match ->
        match.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: ""
    }.filter { it.isNotEmpty() }.toList()

    return tokens + file.absolutePath
}

private fun openWithSystemDefaultEditor(file: File) {
    if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
        logger.error("Desktop open is not supported on this platform, unable to open file: ${file.absolutePath}")
        return
    }
    Desktop.getDesktop().open(file)
}

fun Git.openFile(path: Path): Unit = try {
    val file = this.repository.workTree.resolve(path.toFile())

    when (val editor = getConfiguredEditor()) {
        null -> openWithSystemDefaultEditor(file)
        else -> {
            ProcessBuilder(buildEditorCommand(editor, file)).start()
            Unit
        }
    }
} catch (e: Exception) {
    logger.error("An exception was thrown while opening file $path: ${e.message}")
}
