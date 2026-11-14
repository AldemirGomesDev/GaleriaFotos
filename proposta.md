# Proposta — App de Galeria de Fotos (Kotlin Multiplatform)

## 1. Visão Geral

Aplicativo multiplataforma de galeria de fotos, com a UI compartilhada entre **Android** e **iOS** usando **Compose Multiplatform**. O foco inicial é entregar os fluxos essenciais — **listar imagens** e **abrir em tela cheia** — sobre uma arquitetura modular que permite crescer com as demais features comuns de galeria sem retrabalho.

## 2. Objetivo

Entregar um MVP funcional nas duas plataformas a partir de uma única base de código Kotlin, cobrindo:

- Leitura das fotos do dispositivo (galeria local).
- Exibição em grade (grid) com carregamento eficiente.
- Visualização em tela cheia com navegação entre fotos.

## 3. Plataformas e Stack

| Item | Escolha |
|------|---------|
| Linguagem | Kotlin (Multiplatform) |
| UI | Compose Multiplatform (compartilhada) |
| Android | API 24+ (Android 7.0) |
| iOS | iOS 15+ |
| Build | Gradle (version catalog `libs.versions.toml`) |
| Arquitetura | Multi-módulo, MVVM + navegação |

## 4. Features

### 4.1 MVP (obrigatórias)

- **Listar imagens**: grade responsiva (`LazyVerticalGrid`) com miniaturas.
- **Abrir em tela cheia**: viewer imersivo (fundo escuro, barra escondível).
- **Navegar entre fotos**: swipe horizontal (pager) entre imagens da grade.

### 4.2 Comuns (fase 2, por prioridade)

- **Zoom**: pinch-to-zoom e double-tap no viewer.
- **Compartilhar**: enviar imagem para outros apps (share sheet).
- **Favoritos**: marcar/desmarcar e filtrar por favoritas.
- **Seleção múltipla**: modo de seleção para ações em lote.
- **Excluir**: remover foto (com confirmação).
- **Detalhes/EXIF**: mostrar metadados (data, dimensões, tamanho, localização).
- **Agrupar por álbum/pasta**: organização por pastas do dispositivo.
- **Ordenação**: por data (mais recente/antiga) e nome.
- **Slideshow**: reprodução automática em sequência.

## 5. Arquitetura (módulos)

Segue o padrão modular já usado no `Netflix-CMP` (core + feature):

```
galeria-fotos/
├── composeApp              # entry point (Android app + iOS app)
├── core-navigation         # rotas e destinos tipados
├── core-gallery            # acesso às fotos (expect/actual) + modelos de domínio
├── core-permission         # permissões de mídia (expect/actual)
├── core-shared-ui          # componentes visuais reutilizáveis (grid, viewer)
└── feature-gallery         # telas: listagem e visualização em tela cheia
```

### Responsabilidades

- **core-gallery**: abstração `GalleryRepository` com `expect/actual`.
  - Android: `MediaStore` / Photo Picker (`PickVisualMedia`).
  - iOS: `PHPhotoLibrary` (`PHAsset` + `PHImageManager`).
- **core-permission**: verificação/solicitação de acesso às fotos.
- **core-navigation**: `NavHost` com rotas tipadas (`gallery`, `viewer/{id}`).
- **core-shared-ui**: itens de grade, indicadores de carregamento, tema.
- **feature-gallery**: `GalleryViewModel` (estado, paginação), `GalleryScreen`, `PhotoViewerScreen`.

## 6. Modelo de dados

```kotlin
data class Photo(
    val id: String,          // identificador da mídia no dispositivo
    val uri: String,         // URI de acesso ao conteúdo
    val name: String,
    val dateTaken: Long?,    // timestamp
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val isFavorite: Boolean = false,
)
```

## 7. Bibliotecas-chave

| Biblioteca | Uso |
|------------|-----|
| Compose Multiplatform | UI compartilhada |
| Coil 3 (`coil-compose` / `coil3`) | carregamento de imagens e miniaturas |
| Compose Pager (`foundation.pager`) | swipe entre fotos no viewer |
| `expect`/`actual` nativos | acesso à galeria e permissões (sem lib extra) |
| Kotlinx Serialization | persistência de favoritos/metadados |
| Koin (opcional) ou DI manual | injeção de dependências |

## 8. Pontos de atenção técnica

- **Permissões**: Android 13+ usa `READ_MEDIA_IMAGES`; Android 14+ tem o Photo Picker (sem permissão ampla). iOS exige `NSPhotoLibraryUsageDescription` no `Info.plist`.
- **Thumbnails**: buscar versões de baixa resolução para a grade e só carregar a original no viewer (evita OOM).
- **Paginação**: carregar em lotes (`Paging`/`limit-offset`) em vez de tudo de uma vez.
- **iOS x Android**: `expect/actual` para expor a mesma interface `GalleryRepository` nas duas plataformas.

## 9. Roadmap

1. **Fase 1 (MVP)**: scaffold do projeto, permissões, listagem em grid, viewer em tela cheia.
2. **Fase 2**: zoom, swipe otimizado, compartilhar, ordenação.
3. **Fase 3**: favoritos, seleção múltipla, exclusão, álbuns, EXIF, slideshow.

## 10. Definição de pronto (MVP)

- Listar fotos do dispositivo em grade nas duas plataformas.
- Tocar numa foto abre em tela cheia.
- Swipe navega entre fotos; back/fechar retorna à grade.
- Permissão solicitada corretamente e estados de vazio/erro tratados.
