package br.com.aldemir.galeria.core.sharing

/** Item a ser compartilhado: o URI de conteúdo da foto e um nome amigável. */
data class ShareItem(
    val uri: String,
    val name: String,
)

/**
 * Abstração para compartilhar fotos com outros aplicativos.
 *
 * A implementação é específica de cada plataforma via [createPhotoSharer]:
 * - Android: Intent ACTION_SEND / ACTION_SEND_MULTIPLE com os URIs de conteúdo.
 * - iOS: UIActivityViewController.
 */
interface PhotoSharer {
    /** Abre a folha de compartilhamento do sistema para a foto identificada por [uri]. */
    fun sharePhoto(uri: String, name: String)

    /** Abre a folha de compartilhamento para múltiplas fotos. */
    fun sharePhotos(items: List<ShareItem>)
}

/** Fábrica multiplataforma resolvida por expect/actual. */
expect fun createPhotoSharer(): PhotoSharer
