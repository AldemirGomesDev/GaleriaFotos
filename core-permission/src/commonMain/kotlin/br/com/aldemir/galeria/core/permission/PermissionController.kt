package br.com.aldemir.galeria.core.permission

enum class PermissionStatus {
    Granted,

    /** Negada, mas ainda é possível solicitar novamente pelo dialog. */
    Denied,

    /** Negada de forma permanente; só é possível liberar em Configurações. */
    PermanentlyDenied,

    /** Ainda não solicitada. */
    NotDetermined,
}

/**
 * Abstração de permissão de acesso às fotos.
 *
 * A implementação é específica de cada plataforma via [createPermissionController]:
 * - Android: READ_MEDIA_IMAGES (API 33+) / READ_EXTERNAL_STORAGE (até API 32)
 * - iOS: PHPhotoLibrary
 */
interface PermissionController {
    fun photoPermissionStatus(): PermissionStatus
    fun requestPhotoPermission(onResult: (PermissionStatus) -> Unit)

    /** Abre a tela de configurações do aplicativo para liberar a permissão. */
    fun openAppSettings()
}

/** Fábrica multiplataforma resolvida por expect/actual. */
expect fun createPermissionController(): PermissionController
