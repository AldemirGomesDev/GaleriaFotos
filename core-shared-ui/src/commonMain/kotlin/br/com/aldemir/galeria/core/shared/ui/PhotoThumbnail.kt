package br.com.aldemir.galeria.core.shared.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.aldemir.galeria.core.gallery.Photo
import coil3.compose.AsyncImage

/**
 * Miniatura de uma foto, carregada pelo Coil 3.
 *
 * - Android: [Photo.uri] é uma URI `content://` do MediaStore, suportada nativamente.
 * - iOS: [Photo.uri] é o `localIdentifier` do PHAsset. Para carregá-lo é preciso um
 *   `Fetcher` customizado (PHImageManager), a ser adicionado quando o alvo iOS for
 *   configurado (ver TODO no core-gallery/iosMain).
 *
 * Suporta seleção: quando [isSelected] é `true`, desenha um overlay e um indicador de
 * checagem. [onLongClick] pode ser usado para entrar no modo de seleção.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoThumbnail(
    photo: Photo,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick ?: {},
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = photo.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        if (photo.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "▶",
                    color = Color.White,
                    fontSize = 20.sp,
                )
            }
        }

        if (isSelected) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✓",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (onToggleFavorite != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable { onToggleFavorite() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (photo.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (photo.isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                    tint = if (photo.isFavorite) Color(0xFFE53935) else Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
