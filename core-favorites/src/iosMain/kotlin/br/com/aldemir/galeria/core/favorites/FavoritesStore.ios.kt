package br.com.aldemir.galeria.core.favorites

import platform.Foundation.NSUserDefaults

private const val KEY_IDS = "favorite_ids"

actual fun createFavoritesStore(): FavoritesStore = IosFavoritesStore()

internal class IosFavoritesStore : FavoritesStore {

    private val defaults get() = NSUserDefaults.standardUserDefaults

    override fun loadFavorites(): Set<String> =
        defaults.stringArrayForKey(KEY_IDS)?.filterIsInstance<String>()?.toSet() ?: emptySet()

    override fun setFavorite(id: String, isFavorite: Boolean) {
        val current = loadFavorites().toMutableSet()
        if (isFavorite) current.add(id) else current.remove(id)
        defaults.setObject(current.toList(), forKey = KEY_IDS)
    }
}
