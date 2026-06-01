package com.zerotap.app.capture

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.IntentCompat
import com.zerotap.app.ZeroTapApp
import com.zerotap.app.service.AgentNotifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/** Owns the MediaProjection session and turns the latest frame into a Bitmap. */
class ScreenCaptureManager(private val context: Context) {

    private var projection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var width = 0
    private var height = 0
    private var density = 0

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active

    val isReady: Boolean get() = projection != null && imageReader != null

    fun onProjectionReady(mp: MediaProjection) {
        release()
        projection = mp
        val metrics = context.resources.displayMetrics
        width = metrics.widthPixels
        height = metrics.heightPixels
        density = metrics.densityDpi
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader
        mp.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() = release()
        }, null)
        virtualDisplay = mp.createVirtualDisplay(
            "zerotap-capture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface, null, null
        )
        _active.value = true
    }

    suspend fun capture(): Bitmap? = withContext(Dispatchers.Default) {
        val reader = imageReader ?: return@withContext null
        var image = reader.acquireLatestImage()
        var tries = 0
        while (image == null && tries < 5) {
            delay(120)
            image = reader.acquireLatestImage()
            tries++
        }
        if (image == null) return@withContext null
        try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * width
            val stridedWidth = width + rowPadding / pixelStride
            val full = Bitmap.createBitmap(stridedWidth, height, Bitmap.Config.ARGB_8888)
            full.copyPixelsFromBuffer(buffer)
            Bitmap.createBitmap(full, 0, 0, width, height)
        } catch (e: Exception) {
            null
        } finally {
            image.close()
        }
    }

    fun release() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        projection?.stop()
        projection = null
        _active.value = false
    }
}

/** Foreground host required so MediaProjection can run on Android 14+. */
class CaptureService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AgentNotifications.ensureChannels(this)
        ServiceCompat.startForeground(
            this,
            AgentNotifications.NOTIF_CAPTURE,
            AgentNotifications.captureNotification(this),
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val data = intent?.let { IntentCompat.getParcelableExtra(it, EXTRA_RESULT_DATA, Intent::class.java) }

        if (resultCode == Activity.RESULT_OK && data != null) {
            val mpm = getSystemService(MediaProjectionManager::class.java)
            runCatching { mpm.getMediaProjection(resultCode, data) }.getOrNull()?.let { mp ->
                (application as? ZeroTapApp)?.container?.captureManager?.onProjectionReady(mp)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        (application as? ZeroTapApp)?.container?.captureManager?.release()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"

        fun intent(context: Context, resultCode: Int, data: Intent): Intent =
            Intent(context, CaptureService::class.java)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_RESULT_DATA, data)
    }
}
