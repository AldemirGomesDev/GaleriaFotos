package br.com.aldemir.galeria.core.navigation

/**
 * Destinos de navegação do app.
 *
 * A navegação do MVP é simples (lista -> visualizador), então usamos um
 * sealed interface em vez de uma lib de navegação. Isso mantém o app leve e
 * sem dependências extras; se o grafo crescer, trocamos por navigation-compose.
 */
sealed interface Destination {
    /** Grade com todas as fotos. */
    data object GalleryList : Destination

    /** Visualizador em tela cheia, iniciando no índice [index]. */
    data class PhotoViewer(val index: Int) : Destination
}
