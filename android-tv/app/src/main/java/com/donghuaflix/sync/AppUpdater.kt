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
    val checked: Boolean = false,
    val message: String? = null,
)

@Singleton
class AppUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: DonghuaApi,
) {
    private val _updateState = MutableStateFlow(UpdateInfo())
    val updateState: StateFlow<UpdateInfo> = _updateState.asStateFlow()

    suspend fun checkForUpdate() {
        // Show checking state immediately
        _updateState.value = UpdateInfo(checked = false, message = "Checking for updates...")
        try {
            val latest = api.getLatestVersion()
            val currentVersionCode = BuildConfig.VERSION_CODE

            if (latest.versionCode > currentVersionCode) {
                _updateState.value = UpdateInfo(
                    available = true,
                    checked = true,
                    versionName = latest.versionName,
                    changelog = latest.changelog,
                    downloadUrl = latest.downloadUrl,
                    message = "Update v${latest.versionName} available!",
                )
            } else {
                _updateState.value = UpdateInfo(
                    available = false,
                    checked = true,
                    versionName = "v${BuildConfig.VERSION_NAME}",
                    message = "You're up to date (v${BuildConfig.VERSION_NAME})",
                )
            }
        } catch (_: Exception) {
            _updateState.value = UpdateInfo(
                checked = true,
                message = "Couldn't check for updates",
            )
        }
    }

    fun downloadAndInstall() {
        val url = _updateState.value.downloadUrl
        if (url.isBlank()) return

        // Open download URL in browser — triggers native download + install
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            _updateState.value = _updateState.value.copy(
                message = "Opening browser to download...",
            )
        } catch (_: Exception) {
            _updateState.value = _updateState.value.copy(
                message = "Couldn't open browser. Go to dl.donghuaflix.cloud manually.",
            )
        }
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
