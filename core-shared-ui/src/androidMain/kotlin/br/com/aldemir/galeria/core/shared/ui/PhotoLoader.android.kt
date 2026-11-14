package br.com.aldemir.galeria.core.shared.ui

import android.content.Context
import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.video.VideoFrameDecoder

/**
 * Configura o [ImageLoader] singleton do Coil com suporte a decodificação de frames de vídeo,
 * para que as miniaturas de vídeo sejam carregadas por AsyncImage. Deve ser chamado uma vez,
 * a partir de um contexto composable (ex.: dentro de `setContent`), na inicialização do app.
 */
@Composable
fun initPhotoLoader(context: Context) {
    setSingletonImageLoaderFactory {
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
    }
}
