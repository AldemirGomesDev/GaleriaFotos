package br.com.aldemir.galeria.core.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private const val REQUEST_CODE = 1001
private const val PREFS_NAME = "galeria_permission"
private const val KEY_REQUESTED = "photo_permission_requested"

private var currentActivity: Activity? = null
private var pendingCallback: ((PermissionStatus) -> Unit)? = null

/** Deve ser chamado uma vez, na Activity principal do app Android. */
fun initPermissionModule(activity: Activity) {
    currentActivity = activity
}

/** Encaminhado pela Activity em [Activity.onRequestPermissionsResult]. */
fun notifyPermissionResult(requestCode: Int, grantResults: IntArray) {
    if (requestCode != REQUEST_CODE) return
    val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
    val status = if (granted) {
        PermissionStatus.Granted
    } else {
        if (shouldShowRationale()) PermissionStatus.Denied else PermissionStatus.PermanentlyDenied
    }
    pendingCallback?.invoke(status)
    pendingCallback = null
}

actual fun createPermissionController(): PermissionController = AndroidPermissionController()

internal class AndroidPermissionController : PermissionController {

    override fun photoPermissionStatus(): PermissionStatus {
        val activity = currentActivity ?: return PermissionStatus.Denied
        val permissions = requiredPermissions()
        return when {
            permissions.all {
                ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
            } -> PermissionStatus.Granted
            !hasRequestedBefore(activity) -> PermissionStatus.NotDetermined
            permissions.any {
                ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
            } -> PermissionStatus.Denied
            else -> PermissionStatus.PermanentlyDenied
        }
    }

    override fun requestPhotoPermission(onResult: (PermissionStatus) -> Unit) {
        val activity = currentActivity ?: run {
            onResult(PermissionStatus.Denied)
            return
        }
        markRequested(activity)
        pendingCallback = onResult
        ActivityCompat.requestPermissions(activity, requiredPermissions(), REQUEST_CODE)
    }

    override fun openAppSettings() {
        val activity = currentActivity ?: return
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", activity.packageName, null),
        )
        activity.startActivity(intent)
    }
}

private fun shouldShowRationale(): Boolean {
    val activity = currentActivity ?: return false
    return requiredPermissions().any {
        ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
    }
}

private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

private fun hasRequestedBefore(activity: Activity): Boolean =
    activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_REQUESTED, false)

private fun markRequested(activity: Activity) {
    activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_REQUESTED, true)
        .apply()
}
