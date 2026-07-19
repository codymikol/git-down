package com.codymikol.services

import org.koin.core.annotation.Single
import java.awt.Desktop
import java.io.File
import java.nio.file.Path

@Single
class FileSystemService {

    /**
     * Reveals [path] in the operating system's default file viewer. [reveal] is injected so
     * the file/directory resolution can be unit tested without invoking the real AWT Desktop.
     */
    fun showInFiles(path: Path, reveal: (File) -> Unit = ::revealInDesktop) {
        reveal(path.toFile())
    }

    companion object {
        fun revealInDesktop(file: File) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
                desktop.browseFileDirectory(file)
            } else {
                desktop.open(file.parentFile)
            }
        }
    }

}
