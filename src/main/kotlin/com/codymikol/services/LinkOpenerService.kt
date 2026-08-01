package com.codymikol.services

import org.koin.core.annotation.Single
import java.awt.Desktop
import java.net.URI

@Single
class LinkOpenerService {

    /**
     * Opens [url] with the operating system's default handler. A `mailto:` link is routed to the
     * default mail client via [Desktop.mail], every other scheme to the browser via [Desktop.browse].
     * The previous implementation sent every scheme through [Desktop.browse], which throws
     * `IOException: Failed to show URI` for `mailto:` on platforms (e.g. Linux) that only accept
     * browsable URIs there. [browse] and [mail] are injected so the scheme routing can be unit
     * tested without invoking the real AWT Desktop.
     */
    fun open(
        url: String,
        browse: (URI) -> Unit = ::browseInDesktop,
        mail: (URI) -> Unit = ::mailInDesktop,
    ) {
        val uri = URI.create(url)
        if (uri.scheme?.equals("mailto", ignoreCase = true) == true) mail(uri) else browse(uri)
    }

    companion object {
        fun browseInDesktop(uri: URI) = Desktop.getDesktop().browse(uri)
        fun mailInDesktop(uri: URI) = Desktop.getDesktop().mail(uri)
    }

}
