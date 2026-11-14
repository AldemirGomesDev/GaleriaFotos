package br.com.aldemir.galeria.core.sharing

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri

private var appContext: Context? = null

/** Deve ser chamado uma vez, na inicialização do app Android. */
fun initSharingModule(context: Context) {
    appContext = context.applicationContext
}

actual fun createPhotoSharer(): PhotoSharer = AndroidPhotoSharer()

internal class AndroidPhotoSharer : PhotoSharer {

    override fun sharePhoto(uri: String, name: String) {
        val context = appContext ?: return
        val contentUri = Uri.parse(uri)

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            clipData = ClipData.newRawUri(name, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(sendIntent, name).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    override fun sharePhotos(items: List<ShareItem>) {
        val context = appContext ?: return
        if (items.isEmpty()) return

        val uris = ArrayList<Uri>(items.size)
        items.forEach { uris.add(Uri.parse(it.uri)) }

        val sendIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(sendIntent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
