package br.com.aldemir.galeria.core.favorites

/**
 * Persistência do conjunto de fotos favoritas, representado pelos IDs de cada foto.
 *
 * A implementação é específica de cada plataforma via [createFavoritesStore]:
 * - Android: SharedPreferences
 * - iOS: NSUserDefaults
 */
interface FavoritesStore {
    fun loadFavorites(): Set<String>

    fun setFavorite(id: String, isFavorite: Boolean)
}

/** Fábrica multiplataforma resolvida por expect/actual. */
expect fun createFavoritesStore(): FavoritesStore
