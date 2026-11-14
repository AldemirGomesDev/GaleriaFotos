package br.com.aldemir.galeria.feature.gallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Reproduz um vídeo dentro do app.
 *
 * - Android: Media3/ExoPlayer.
 * - iOS: placeholder (TODO: AVPlayer).
 */
@Composable
expect fun VideoPlayer(uri: String, modifier: Modifier = Modifier)
