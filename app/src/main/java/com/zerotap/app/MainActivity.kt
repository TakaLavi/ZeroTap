package com.zerotap.app

import android.Manifest
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.zerotap.app.capture.CaptureService
import com.zerotap.app.ui.ZeroTapRoot
import com.zerotap.app.ui.theme.ZeroTapTheme

class MainActivity : ComponentActivity() {

    private lateinit var projectionLauncher: ActivityResultLauncher<Intent>
    private lateinit var notificationLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val projectionManager = getSystemService(MediaProjectionManager::class.java)

        projectionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data
            if (result.resultCode == RESULT_OK && data != null) {
                ContextCompat.startForegroundService(
                    this,
                    CaptureService.intent(this, result.resultCode, data)
                )
            }
        }

        notificationLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* result handled by system UI */ }

        setContent {
            ZeroTapTheme {
                ZeroTapRoot(
                    container = (application as ZeroTapApp).container,
                    onRequestProjection = {
                        runCatching { projectionLauncher.launch(projectionManager.createScreenCaptureIntent()) }
                    },
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onOpenAccessibility = {
                        runCatching { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    },
                    onOpenAppSettings = {
                        runCatching {
                            startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:$packageName")
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}
