package com.donghuaflix.sync

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.donghuaflix.BuildConfig
import com.donghuaflix.data.remote.DonghuaApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val available: Boolean = false,
    val versionName: String = "",
    val changelog: String? = null,
    val downloadUrl: String = "",
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
)

@Singleton
class AppUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: DonghuaApi,
) {
    private val _updateState = MutableStateFlow(UpdateInfo())
    val updateState: StateFlow<UpdateInfo> = _updateState.asStateFlow()

    suspend fun checkForUpdate() {
        try {
            val latest = api.getLatestVersion()
            val currentVersionCode = BuildConfig.VERSION_CODE

            if (latest.versionCode > currentVersionCode) {
                _updateState.value = UpdateInfo(
                    available = true,
                    versionName = latest.versionName,
                    changelog = latest.changelog,
                    downloadUrl = latest.downloadUrl,
                )
            }
        } catch (_: Exception) {
            // Silently fail — update check is non-critical
        }
    }

    fun downloadAndInstall() {
        val url = _updateState.value.downloadUrl
        if (url.isBlank()) return

        _updateState.value = _updateState.value.copy(isDownloading = true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("DonghuaFlix Update")
            .setDescription("Downloading v${_updateState.value.versionName}")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "DonghuaFlix-update.apk")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val downloadId = downloadManager.enqueue(request)

        // Register receiver to install after download
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    _updateState.value = _updateState.value.copy(isDownloading = false)
                    installApk()
                    try { context.unregisterReceiver(this) } catch (_: Exception) {}
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_EXPORTED,
            )
        } else {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            )
        }
    }

    private fun installApk() {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "DonghuaFlix-update.apk",
        )
        if (!file.exists()) return

        val intent = Intent(Intent.ACTION_VIEW).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)

        // Delete APK after a short delay to free TV storage
        Thread {
            Thread.sleep(30_000) // Wait 30s for install to complete
            if (file.exists()) {
                file.delete()
            }
        }.start()
    }

    /** Clean up any leftover APK files from previous updates */
    fun cleanupOldApks() {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir?.exists() == true) {
                downloadsDir.listFiles()?.filter {
                    it.name.startsWith("DonghuaFlix") && it.name.endsWith(".apk")
                }?.forEach { it.delete() }
            }
        } catch (_: Exception) {}
    }

    fun dismissUpdate() {
        _updateState.value = UpdateInfo()
    }
}
