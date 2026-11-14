package br.com.aldemir.galeria

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.aldemir.galeria.core.gallery.initGalleryActivity
import br.com.aldemir.galeria.core.gallery.initGalleryModule
import br.com.aldemir.galeria.core.gallery.notifyDeleteResult
import br.com.aldemir.galeria.core.favorites.initFavoritesModule
import br.com.aldemir.galeria.core.permission.initPermissionModule
import br.com.aldemir.galeria.core.permission.notifyPermissionResult
import br.com.aldemir.galeria.core.sharing.initSharingModule
import br.com.aldemir.galeria.core.shared.ui.initPhotoLoader

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initGalleryModule(applicationContext)
        initGalleryActivity(this)
        initPermissionModule(this)
        initFavoritesModule(applicationContext)
        initSharingModule(applicationContext)
        enableEdgeToEdge()
        setContent {
            initPhotoLoader(applicationContext)
            App()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        notifyPermissionResult(requestCode, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        notifyDeleteResult(requestCode, resultCode)
    }
}
