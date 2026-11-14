package br.com.aldemir.galeria.core.permission

import platform.Foundation.NSURL
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

actual fun createPermissionController(): PermissionController = IosPermissionController()

internal class IosPermissionController : PermissionController {

    override fun photoPermissionStatus(): PermissionStatus =
        PHPhotoLibrary.authorizationStatus().toStatus()

    override fun requestPhotoPermission(onResult: (PermissionStatus) -> Unit) {
        PHPhotoLibrary.requestAuthorization { status ->
            onResult(status.toStatus())
        }
    }

    override fun openAppSettings() {
        // TODO(ios): verificar assinatura de openURL ao compilar com o SDK instalado.
        NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let { url ->
            UIApplication.sharedApplication.openURL(
                url,
                options = mapOf<Any?, Any?>(),
                completionHandler = null,
            )
        }
    }
}

private fun platform.Photos.PHAuthorizationStatus.toStatus(): PermissionStatus = when (this) {
    PHAuthorizationStatusAuthorized -> PermissionStatus.Granted
    PHAuthorizationStatusDenied -> PermissionStatus.Denied
    PHAuthorizationStatusRestricted -> PermissionStatus.PermanentlyDenied
    PHAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
    else -> PermissionStatus.Denied
}
