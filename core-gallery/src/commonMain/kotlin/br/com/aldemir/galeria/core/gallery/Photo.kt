package br.com.aldemir.galeria.core.gallery

/**
 * Representação neutra de uma foto do dispositivo, independente da plataforma.
 */
data class Photo(
    val id: String,
    val uri: String,
    val name: String,
    val dateTaken: Long?,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val isFavorite: Boolean = false,
    val isVideo: Boolean = false,
)
