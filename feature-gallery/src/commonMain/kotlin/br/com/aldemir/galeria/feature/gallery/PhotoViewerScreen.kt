package br.com.aldemir.galeria.feature.gallery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import br.com.aldemir.galeria.core.gallery.Photo
import br.com.aldemir.galeria.core.gallery.hasSystemDeleteConfirmation
import coil3.compose.AsyncImage

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 3f
// Multiplica o deslocamento de 1 dedo (arrastar com zoom). Ajuste conforme a sensibilidade desejada.
private const val PAN_SPEED = 3f

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    photos: List<Photo>,
    initialIndex: Int,
    onBack: () -> Unit,
    onShare: (Photo) -> Unit,
    onDelete: (Photo) -> Unit,
    onToggleFavorite: (Photo) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { photos.size }
    var isZoomed by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Ao trocar de foto, garante que o swipe volta a ficar habilitado.
    LaunchedEffect(pagerState.settledPage) {
        isZoomed = false
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("${pagerState.currentPage + 1} de ${photos.size}") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { onToggleFavorite(photos[pagerState.currentPage]) }) {
                        Icon(
                            imageVector = if (photos[pagerState.currentPage].isFavorite) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Filled.FavoriteBorder
                            },
                            contentDescription = if (photos[pagerState.currentPage].isFavorite) {
                                "Remover dos favoritos"
                            } else {
                                "Adicionar aos favoritos"
                            },
                            tint = if (photos[pagerState.currentPage].isFavorite) {
                                Color(0xFFE53935)
                            } else {
                                Color.White
                            },
                        )
                    }
                    TextButton(onClick = { onShare(photos[pagerState.currentPage]) }) {
                        Text("Compartilhar")
                    }
                    TextButton(
                        onClick = {
                            if (hasSystemDeleteConfirmation()) {
                                onDelete(photos[pagerState.currentPage])
                            } else {
                                showDeleteDialog = true
                            }
                        },
                    ) {
                        Text("Excluir")
                    }
                },
            )
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding),
            userScrollEnabled = !isZoomed,
        ) { page ->
            val photo = photos[page]
            if (photo.isVideo) {
                VideoPlayer(uri = photo.uri, modifier = Modifier.fillMaxSize())
            } else {
                ZoomablePhoto(
                    photo = photo,
                    onZoomChanged = { zoomed -> isZoomed = zoomed },
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir foto?") },
            text = { Text("Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(photos[pagerState.currentPage])
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

@Composable
private fun ZoomablePhoto(
    photo: Photo,
    onZoomChanged: (Boolean) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(MIN_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    // Tamanho real da imagem decodificada. É mais confiável que os metadados do MediaStore
    // (photo.width/height podem vir zerados ou incorretos), garantindo que o arrasto consiga
    // alcançar as bordas ao ampliar.
    var imageSize by remember { mutableStateOf(IntSize(photo.width, photo.height)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { viewportSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > MIN_SCALE) {
                            scale = MIN_SCALE
                            offset = Offset.Zero
                            onZoomChanged(false)
                        } else {
                            scale = DOUBLE_TAP_SCALE
                            offset = Offset.Zero
                            onZoomChanged(true)
                        }
                    },
                )
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    // Processa no passe Initial para responder ao toque antes dos demais
                    // handlers (pager e detecção de toque), reduzindo a latência do arrasto.
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressedCount = event.changes.count { it.pressed }

                        if (pressedCount >= 2) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                            scale = newScale
                            offset = clampOffset(
                                offset = offset + pan,
                                scale = newScale,
                                viewportSize = viewportSize,
                                imageWidth = imageSize.width,
                                imageHeight = imageSize.height,
                            )
                            onZoomChanged(newScale > MIN_SCALE)
                            event.changes.forEach { it.consume() }
                        } else if (pressedCount == 1 && scale > MIN_SCALE) {
                            val pan = event.calculatePan()
                            offset = clampOffset(
                                offset = offset + pan * PAN_SPEED,
                                scale = scale,
                                viewportSize = viewportSize,
                                imageWidth = imageSize.width,
                                imageHeight = imageSize.height,
                            )
                            event.changes.forEach { it.consume() }
                        }
                        // 1 dedo e sem zoom: não consome o movimento, deixando o swipe
                        // chegar ao HorizontalPager.
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = photo.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            onSuccess = { state ->
                imageSize = IntSize(state.result.image.width, state.result.image.height)
            },
        )
    }
}

/**
 * Limita o deslocamento ([offset]) para que as bordas da imagem ampliada nunca
 * ultrapassem as bordas do viewport (comportamento do Google Photos).
 */
private fun clampOffset(
    offset: Offset,
    scale: Float,
    viewportSize: IntSize,
    imageWidth: Int,
    imageHeight: Int,
): Offset {
    if (scale <= MIN_SCALE || viewportSize.width <= 0 || viewportSize.height <= 0 ||
        imageWidth <= 0 || imageHeight <= 0
    ) {
        return Offset.Zero
    }

    val viewportWidth = viewportSize.width.toFloat()
    val viewportHeight = viewportSize.height.toFloat()
    val aspect = imageWidth.toFloat() / imageHeight.toFloat()

    // Tamanho da imagem desenhada (ContentScale.Fit) dentro do viewport, em 1x.
    val renderedWidth: Float
    val renderedHeight: Float
    if (viewportWidth / viewportHeight > aspect) {
        renderedHeight = viewportHeight
        renderedWidth = viewportHeight * aspect
    } else {
        renderedWidth = viewportWidth
        renderedHeight = viewportWidth / aspect
    }

    val maxX = ((renderedWidth * scale - viewportWidth) / 2f).coerceAtLeast(0f)
    val maxY = ((renderedHeight * scale - viewportHeight) / 2f).coerceAtLeast(0f)

    return Offset(
        x = offset.x.coerceIn(-maxX, maxX),
        y = offset.y.coerceIn(-maxY, maxY),
    )
}
