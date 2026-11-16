package br.com.aldemir.galeria.core.gallery

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DELETE_REQUEST_CODE = 2001

private lateinit var appContext: Context
private var galleryActivity: Activity? = null
private var pendingDeleteCallback: ((Boolean) -> Unit)? = null

/** Deve ser chamado uma vez, na inicialização do app Android. */
fun initGalleryModule(context: Context) {
    appContext = context.applicationContext
}

/** Deve ser chamado uma vez, na Activity principal (necessário para exclusão com confirmação). */
fun initGalleryActivity(activity: Activity) {
    galleryActivity = activity
}

/** Encaminhado pela Activity em [Activity.onActivityResult] ao concluir a exclusão. */
fun notifyDeleteResult(requestCode: Int, resultCode: Int) {
    if (requestCode != DELETE_REQUEST_CODE) return
    val deleted = resultCode == Activity.RESULT_OK
    pendingDeleteCallback?.invoke(deleted)
    pendingDeleteCallback = null
}

actual fun createGalleryRepository(): GalleryRepository = AndroidGalleryRepository(appContext)

actual fun hasSystemDeleteConfirmation(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

internal class AndroidGalleryRepository(
    private val context: Context,
) : GalleryRepository {

    override suspend fun loadPhotos(): List<Photo> = withContext(Dispatchers.IO) {
        val media = mutableListOf<Photo>()
        media += queryCollection(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, isVideo = false)
        media += queryCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, isVideo = true)
        // Do mais recente para o mais antigo, pela data efetiva (captura ou modificação).
        media.sortedWith(compareByDescending { it.dateTaken ?: Long.MIN_VALUE })
    }

    private fun queryCollection(collection: Uri, isVideo: Boolean): List<Photo> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
        )
        // Do mais recente para o mais antigo, pela data de captura (DATE_TAKEN, em ms) ou,
        // quando ela não existe, pela data de modificação (DATE_MODIFIED, em s -> *1000).
        val sortOrder =
            "MAX(${MediaStore.MediaColumns.DATE_TAKEN}, ${MediaStore.MediaColumns.DATE_MODIFIED} * 1000) DESC"

        val result = mutableListOf<Photo>()
        context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)

            while (cursor.moveToNext()) {
                val rawId = cursor.getLong(idCol)
                result += Photo(
                    id = if (isVideo) "video:$rawId" else "image:$rawId",
                    uri = ContentUris.withAppendedId(collection, rawId).toString(),
                    name = cursor.getString(nameCol) ?: rawId.toString(),
                    dateTaken = maxOf(
                        cursor.getLong(dateCol),
                        cursor.getLong(dateModifiedCol) * 1000L,
                    ),
                    sizeBytes = cursor.getLong(sizeCol),
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    isVideo = isVideo,
                )
            }
        }
        return result
    }

    /** Resolve a coleção do MediaStore e o id numérico a partir do [Photo.id] prefixado. */
    private fun parseId(id: String): Pair<Uri, Long> =
        if (id.startsWith("video:")) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI to id.removePrefix("video:").toLong()
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI to id.removePrefix("image:").toLong()
        }

    override suspend fun deletePhotos(ids: List<String>, onResult: (Boolean) -> Unit) {
        if (ids.isEmpty()) {
            onResult(true)
            return
        }
        val uris = ids.map { id ->
            val (collection, rawId) = parseId(id)
            ContentUris.withAppendedId(collection, rawId)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: a exclusão de mídia não própria exige confirmação via dialog do sistema.
            val activity = galleryActivity
            if (activity == null) {
                onResult(false)
                return
            }
            pendingDeleteCallback = onResult
            val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
            activity.startIntentSenderForResult(
                pendingIntent.intentSender,
                DELETE_REQUEST_CODE,
                null,
                0,
                0,
                0,
            )
        } else {
            withContext(Dispatchers.IO) {
                uris.forEach { context.contentResolver.delete(it, null, null) }
            }
            onResult(true)
        }
    }
}
