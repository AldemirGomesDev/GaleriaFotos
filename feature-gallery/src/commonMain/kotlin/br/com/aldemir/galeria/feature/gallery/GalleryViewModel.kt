package br.com.aldemir.galeria.feature.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.aldemir.galeria.core.gallery.GalleryRepository
import br.com.aldemir.galeria.core.gallery.Photo
import br.com.aldemir.galeria.core.gallery.createGalleryRepository
import br.com.aldemir.galeria.core.favorites.createFavoritesStore
import br.com.aldemir.galeria.core.permission.PermissionStatus
import br.com.aldemir.galeria.core.permission.createPermissionController
import br.com.aldemir.galeria.core.sharing.ShareItem
import br.com.aldemir.galeria.core.sharing.createPhotoSharer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GalleryTab { All, Favorites }

data class GalleryUiState(
    val isLoading: Boolean = true,
    val photos: List<Photo> = emptyList(),
    val error: String? = null,
    val permissionStatus: PermissionStatus? = null,
    val selectedIds: Set<String> = emptySet(),
    val activeTab: GalleryTab = GalleryTab.All,
)

class GalleryViewModel(
    private val repository: GalleryRepository = createGalleryRepository(),
) : ViewModel() {

    private val permission = createPermissionController()
    private val sharer = createPhotoSharer()
    private val favorites = createFavoritesStore()

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            when (val status = permission.photoPermissionStatus()) {
                PermissionStatus.Granted -> fetchPhotos()
                PermissionStatus.NotDetermined -> requestPermission()
                PermissionStatus.Denied,
                PermissionStatus.PermanentlyDenied -> showPermissionRequired(status)
            }
        }
    }

    fun requestPermission() {
        _uiState.value = GalleryUiState(isLoading = true)
        permission.requestPhotoPermission { status ->
            if (status == PermissionStatus.Granted) {
                load()
            } else {
                showPermissionRequired(status)
            }
        }
    }

    fun openSettings() {
        permission.openAppSettings()
    }

    fun selectTab(tab: GalleryTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun toggleFavorite(photo: Photo) {
        val nowFavorite = !photo.isFavorite
        favorites.setFavorite(photo.id, nowFavorite)
        _uiState.update { state ->
            state.copy(
                photos = state.photos.map {
                    if (it.id == photo.id) it.copy(isFavorite = nowFavorite) else it
                },
            )
        }
    }

    fun share(photo: Photo) {
        sharer.sharePhoto(photo.uri, photo.name)
    }

    fun deletePhoto(photo: Photo, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deletePhotos(listOf(photo.id)) { deleted ->
                refresh()
                if (deleted) onDeleted()
            }
        }
    }

    fun toggleSelection(id: String) {
        _uiState.update { state ->
            val selected = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
            state.copy(selectedIds = selected)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun shareSelected() {
        val items = _uiState.value.photos
            .filter { it.id in _uiState.value.selectedIds }
            .map { ShareItem(it.uri, it.name) }
        if (items.isEmpty()) return
        sharer.sharePhotos(items)
    }

    fun deleteSelected() {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        clearSelection()
        viewModelScope.launch {
            repository.deletePhotos(ids) {
                refresh()
            }
        }
    }

    /** Recarrega as fotos sem exibir o indicador de carregamento. */
    fun refresh() {
        viewModelScope.launch {
            if (permission.photoPermissionStatus() != PermissionStatus.Granted) return@launch
            try {
                val photos = withFavorites(repository.loadPhotos())
                _uiState.update { it.copy(photos = photos, error = null) }
            } catch (_: Exception) {
                // Mantém a lista atual em caso de falha transitória.
            }
        }
    }

    private suspend fun fetchPhotos() {
        _uiState.value = GalleryUiState(isLoading = true)
        try {
            val photos = withFavorites(repository.loadPhotos())
            _uiState.value = GalleryUiState(
                isLoading = false,
                photos = photos,
                permissionStatus = PermissionStatus.Granted,
            )
        } catch (e: Exception) {
            showError(e.message ?: "Não foi possível carregar as fotos.")
        }
    }

    private fun withFavorites(photos: List<Photo>): List<Photo> {
        val favoriteIds = favorites.loadFavorites()
        return photos.map { it.copy(isFavorite = it.id in favoriteIds) }
    }

    private fun showPermissionRequired(status: PermissionStatus) {
        _uiState.value = GalleryUiState(isLoading = false, permissionStatus = status)
    }

    private fun showError(message: String) {
        _uiState.value = GalleryUiState(isLoading = false, error = message)
    }
}
