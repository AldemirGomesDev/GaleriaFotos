package br.com.aldemir.galeria

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.aldemir.galeria.core.navigation.Destination
import br.com.aldemir.galeria.core.permission.PermissionStatus
import br.com.aldemir.galeria.feature.gallery.GalleryScreen
import br.com.aldemir.galeria.feature.gallery.GalleryViewModel
import br.com.aldemir.galeria.feature.gallery.PhotoViewerScreen

@Composable
fun App() {
    val darkTheme = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
    ) {
        val viewModel = viewModel { GalleryViewModel() }
        val uiState by viewModel.uiState.collectAsState()
        var destination by remember { mutableStateOf<Destination>(Destination.GalleryList) }
        // Mantém a posição do scroll da galeria ao alternar entre lista e visualização.
        val gridState = rememberLazyGridState()

        LaunchedEffect(Unit) {
            viewModel.load()
        }

        // Ao voltar para o app (ex.: saindo de Configurações), re-checa a permissão
        // apenas se ainda estivermos bloqueados por permissão negada.
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            when (viewModel.uiState.value.permissionStatus) {
                PermissionStatus.Denied, PermissionStatus.PermanentlyDenied -> viewModel.load()
                // Recarrega em silêncio para refletir mudanças externas (ex.: exclusão confirmada).
                PermissionStatus.Granted -> viewModel.refresh()
                else -> Unit
            }
        }

        when (val current = destination) {
            is Destination.GalleryList -> GalleryScreen(
                uiState = uiState,
                gridState = gridState,
                onRetry = viewModel::load,
                onRequestPermission = viewModel::requestPermission,
                onOpenSettings = viewModel::openSettings,
                onSelectTab = viewModel::selectTab,
                onPhotoClick = { photo ->
                    val index = uiState.photos.indexOfFirst { it.id == photo.id }
                    if (index >= 0) destination = Destination.PhotoViewer(index)
                },
                onToggleSelection = viewModel::toggleSelection,
                onToggleFavorite = viewModel::toggleFavorite,
                onClearSelection = viewModel::clearSelection,
                onShareSelected = viewModel::shareSelected,
                onDeleteSelected = viewModel::deleteSelected,
            )
            is Destination.PhotoViewer -> PhotoViewerScreen(
                photos = uiState.photos,
                initialIndex = current.index,
                onBack = { destination = Destination.GalleryList },
                onShare = viewModel::share,
                onDelete = { photo ->
                    viewModel.deletePhoto(photo) {
                        destination = Destination.GalleryList
                    }
                },
                onToggleFavorite = viewModel::toggleFavorite,
            )
        }
    }
}
