package br.com.aldemir.galeria.feature.gallery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.aldemir.galeria.core.gallery.Photo
import br.com.aldemir.galeria.core.gallery.hasSystemDeleteConfirmation
import br.com.aldemir.galeria.core.permission.PermissionStatus
import br.com.aldemir.galeria.core.shared.ui.PhotoThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    uiState: GalleryUiState,
    gridState: LazyGridState,
    onRetry: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectTab: (GalleryTab) -> Unit,
    onPhotoClick: (Photo) -> Unit,
    onToggleSelection: (String) -> Unit,
    onToggleFavorite: (Photo) -> Unit,
    onClearSelection: () -> Unit,
    onShareSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
) {
    val selectionMode = uiState.selectedIds.isNotEmpty()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Reseta o scroll ao trocar de aba, sem afetar a posição preservada ao voltar do viewer.
    var lastTab by remember { mutableStateOf(uiState.activeTab) }
    LaunchedEffect(uiState.activeTab) {
        if (lastTab != uiState.activeTab) {
            gridState.scrollToItem(0)
            lastTab = uiState.activeTab
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            if (selectionMode) "${uiState.selectedIds.size} selecionada(s)"
                            else "Galeria",
                        )
                    },
                    navigationIcon = {
                        if (selectionMode) {
                            TextButton(onClick = onClearSelection) {
                                Text("Cancelar")
                            }
                        }
                    },
                    actions = {
                        if (selectionMode) {
                            TextButton(onClick = onShareSelected) {
                                Text("Compartilhar")
                            }
                            TextButton(
                                onClick = {
                                    if (hasSystemDeleteConfirmation()) {
                                        onDeleteSelected()
                                    } else {
                                        showDeleteDialog = true
                                    }
                                },
                            ) {
                                Text("Excluir")
                            }
                        }
                    },
                )
                if (!selectionMode) {
                    TabRow(selectedTabIndex = uiState.activeTab.ordinal) {
                        Tab(
                            selected = uiState.activeTab == GalleryTab.All,
                            onClick = { onSelectTab(GalleryTab.All) },
                            text = { Text("Todas") },
                        )
                        Tab(
                            selected = uiState.activeTab == GalleryTab.Favorites,
                            onClick = { onSelectTab(GalleryTab.Favorites) },
                            text = { Text("Favoritas") },
                        )
                    }
                }
            }
        },
    ) { padding ->
        val contentModifier = Modifier.fillMaxSize().padding(padding)
        val status = uiState.permissionStatus
        val showPermission = status == PermissionStatus.Denied || status == PermissionStatus.PermanentlyDenied
        val displayedPhotos = if (uiState.activeTab == GalleryTab.Favorites) {
            uiState.photos.filter { it.isFavorite }
        } else {
            uiState.photos
        }

        when {
            showPermission -> PermissionRequiredScreen(
                status = status ?: PermissionStatus.Denied,
                onRequest = onRequestPermission,
                onOpenSettings = onOpenSettings,
                modifier = contentModifier,
            )
            uiState.isLoading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> ErrorState(
                message = uiState.error,
                onRetry = onRetry,
                modifier = contentModifier,
            )
            displayedPhotos.isEmpty() -> Box(contentModifier, contentAlignment = Alignment.Center) {
                Text(
                    if (uiState.activeTab == GalleryTab.Favorites) "Nenhuma foto favorita."
                    else "Nenhuma foto encontrada.",
                )
            }
            else -> PhotoGrid(
                photos = displayedPhotos,
                selectedIds = uiState.selectedIds,
                gridState = gridState,
                onPhotoClick = onPhotoClick,
                onToggleSelection = onToggleSelection,
                onToggleFavorite = onToggleFavorite,
                modifier = contentModifier,
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir fotos?") },
            text = { Text("As fotos selecionadas serão excluídas permanentemente.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteSelected()
                    },
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGrid(
    photos: List<Photo>,
    selectedIds: Set<String>,
    gridState: LazyGridState,
    onPhotoClick: (Photo) -> Unit,
    onToggleSelection: (String) -> Unit,
    onToggleFavorite: (Photo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectionMode = selectedIds.isNotEmpty()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 112.dp),
        state = gridState,
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(photos, key = { it.id }) { photo ->
            PhotoThumbnail(
                photo = photo,
                isSelected = photo.id in selectedIds,
                onClick = {
                    if (selectionMode) onToggleSelection(photo.id)
                    else onPhotoClick(photo)
                },
                onLongClick = {
                    if (!selectionMode) onToggleSelection(photo.id)
                },
                onToggleFavorite = { onToggleFavorite(photo) },
            )
        }
    }
}

@Composable
private fun PermissionRequiredScreen(
    status: PermissionStatus,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Acesso às fotos necessário",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (status == PermissionStatus.PermanentlyDenied) {
                "A permissão foi negada. Ative o acesso às fotos nas configurações do aplicativo."
            } else {
                "Para exibir suas fotos, permita o acesso à galeria."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        if (status == PermissionStatus.Denied) {
            Button(onClick = onRequest) {
                Text("Conceder acesso")
            }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(onClick = onOpenSettings) {
            Text("Abrir configurações")
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text("Tentar novamente")
        }
    }
}
