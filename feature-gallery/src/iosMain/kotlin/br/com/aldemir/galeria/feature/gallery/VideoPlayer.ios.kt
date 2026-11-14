package br.com.aldemir.galeria.feature.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
actual fun VideoPlayer(uri: String, modifier: Modifier) {
    // TODO(ios): implementar reprodução com AVPlayer.
    Box(modifier, contentAlignment = Alignment.Center) {
        Text("Reprodução de vídeo indisponível no iOS (em breve).")
    }
}
