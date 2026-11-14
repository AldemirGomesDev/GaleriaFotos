package br.com.aldemir.galeria.core.gallery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSSortDescriptor
import platform.Photos.PHAsset
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAssetMediaType
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHAssetMediaTypeVideo
import platform.Photos.PHFetchOptions
import platform.Photos.PHPhotoLibrary

actual fun createGalleryRepository(): GalleryRepository = IosGalleryRepository()

actual fun hasSystemDeleteConfirmation(): Boolean = false

internal class IosGalleryRepository : GalleryRepository {

    override suspend fun loadPhotos(): List<Photo> = withContext(Dispatchers.Default) {
        val images = fetchAssets(PHAssetMediaTypeImage, isVideo = false)
        val videos = fetchAssets(PHAssetMediaTypeVideo, isVideo = true)
        (images + videos).sortedWith(compareByDescending { it.dateTaken ?: Long.MIN_VALUE })
    }

    private fun fetchAssets(mediaType: PHAssetMediaType, isVideo: Boolean): List<Photo> {
        val options = PHFetchOptions().apply {
            sortDescriptors = listOf(
                NSSortDescriptor(key = "creationDate", ascending = false),
            )
        }
        val result = PHAsset.fetchAssetsWithMediaType(mediaType, options)
        val count = result.count.toInt()

        return buildList {
            for (i in 0 until count) {
                val asset = result.objectAtIndex(i.toULong()) as? PHAsset ?: continue
                add(
                    Photo(
                        id = asset.localIdentifier,
                        uri = asset.localIdentifier,
                        name = asset.localIdentifier,
                        dateTaken = asset.creationDate?.timeIntervalSince1970?.times(1000)?.toLong(),
                        sizeBytes = 0L,
                        width = asset.pixelWidth.toInt(),
                        height = asset.pixelHeight.toInt(),
                        isVideo = isVideo,
                    ),
                )
            }
        }
    }

    override suspend fun deletePhotos(ids: List<String>, onResult: (Boolean) -> Unit) = withContext(Dispatchers.Default) {
        // TODO(ios): validar interop de PHPhotoLibrary.performChanges ao compilar com o SDK.
        val assets = PHAsset.fetchAssetsWithLocalIdentifiers(ids, null)
        PHPhotoLibrary.sharedPhotoLibrary().performChanges(
            changeBlock = { PHAssetChangeRequest.deleteAssets(assets) },
            completionHandler = { success, _ -> onResult(success) },
        )
    }
}
