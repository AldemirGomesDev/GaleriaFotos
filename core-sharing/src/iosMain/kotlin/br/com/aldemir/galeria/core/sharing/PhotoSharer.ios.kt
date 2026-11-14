package br.com.aldemir.galeria.core.sharing

import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual fun createPhotoSharer(): PhotoSharer = IosPhotoSharer()

internal class IosPhotoSharer : PhotoSharer {

    override fun sharePhoto(uri: String, name: String) {
        val url = NSURL.URLWithString(uri) ?: return
        presentShareSheet(listOf(url))
    }

    override fun sharePhotos(items: List<ShareItem>) {
        val urls = items.mapNotNull { NSURL.URLWithString(it.uri) }
        if (urls.isEmpty()) return
        presentShareSheet(urls)
    }

    private fun presentShareSheet(items: List<NSURL>) {
        // TODO(ios): resolver asset local (PHAsset) e compartilhar via UIActivityViewController.
        val controller = UIActivityViewController(
            activityItems = items,
            applicationActivities = null,
        )
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(controller, animated = true, completion = null)
    }
}
