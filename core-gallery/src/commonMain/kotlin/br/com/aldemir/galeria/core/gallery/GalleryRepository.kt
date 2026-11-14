package br.com.aldemir.galeria.core.gallery

/**
 * Contrato de acesso às fotos do dispositivo.
 *
 * A implementação é específica de cada plataforma via [createGalleryRepository]:
 * - Android: MediaStore
 * - iOS: PHPhotoLibrary
 */
interface GalleryRepository {
    suspend fun loadPhotos(): List<Photo>

    /**
     * Remove permanentemente as fotos identificadas por [ids].
     *
     * No Android 11+ (API 30+), a exclusão exige confirmação do usuário por meio de um
     * dialog do sistema (MediaStore.createDeleteRequest), portanto é assíncrona. [onResult]
     * é invocado com `true` se a exclusão foi confirmada e `false` se foi cancelada.
     */
    suspend fun deletePhotos(ids: List<String>, onResult: (Boolean) -> Unit = {})
}

/** Fábrica multiplataforma resolvida por expect/actual. */
expect fun createGalleryRepository(): GalleryRepository

/**
 * Indica se a plataforma já apresenta um dialog nativo de confirmação ao excluir mídia
 * (ex.: Android 11+ via MediaStore.createDeleteRequest). Quando `false`, a UI deve exibir
 * sua própria confirmação antes de excluir.
 */
expect fun hasSystemDeleteConfirmation(): Boolean
