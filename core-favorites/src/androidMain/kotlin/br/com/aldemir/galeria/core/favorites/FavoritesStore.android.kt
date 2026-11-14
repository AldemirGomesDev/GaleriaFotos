package br.com.aldemir.galeria.core.favorites

import android.content.Context

private const val PREFS_NAME = "galeria_favorites"
private const val KEY_IDS = "favorite_ids"

private lateinit var appContext: Context

/** Deve ser chamado uma vez, na inicialização do app Android. */
fun initFavoritesModule(context: Context) {
    appContext = context.applicationContext
}

actual fun createFavoritesStore(): FavoritesStore = AndroidFavoritesStore()

internal class AndroidFavoritesStore : FavoritesStore {

    private val prefs get() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun loadFavorites(): Set<String> =
        prefs.getStringSet(KEY_IDS, emptySet())?.toSet() ?: emptySet()

    override fun setFavorite(id: String, isFavorite: Boolean) {
        val current = loadFavorites().toMutableSet()
        if (isFavorite) current.add(id) else current.remove(id)
        prefs.edit().putStringSet(KEY_IDS, current).apply()
    }
}
