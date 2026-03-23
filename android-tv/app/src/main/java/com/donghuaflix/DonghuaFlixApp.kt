package com.donghuaflix

import android.app.Application
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import dagger.hilt.android.HiltAndroidApp
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.Executors

@HiltAndroidApp
class DonghuaFlixApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupUncaughtExceptionHandler()
    }

    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // Use a direct OkHttp call since Hilt might not be fully initialized
                val payload = mapOf(
                    "level" to "crash",
                    "message" to (throwable.message ?: throwable.javaClass.simpleName),
                    "stacktrace" to Log.getStackTraceString(throwable),
                    "app_version" to BuildConfig.VERSION_NAME,
                    "device_info" to "${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                    "screen" to "UncaughtException:${thread.name}",
                )
                val json = Gson().toJson(payload)
                val body = json.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(BuildConfig.API_BASE_URL + "/api/crash-logs")
                    .post(body)
                    .build()

                // Fire synchronously on a background thread with a short timeout
                // so we have a chance to send before the process dies
                val client = OkHttpClient.Builder()
                    .callTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val executor = Executors.newSingleThreadExecutor()
                val future = executor.submit {
                    try {
                        client.newCall(request).execute().close()
                    } catch (e: Exception) {
                        Log.w("CrashLogger", "Failed to send crash log: ${e.message}")
                    }
                }
                // Wait up to 3 seconds for the log to send
                try {
                    future.get(3, java.util.concurrent.TimeUnit.SECONDS)
                } catch (_: Exception) {}
            } catch (_: Exception) {
                // Don't let crash logging prevent the default handler from running
            }

            // Delegate to default handler so the app crashes normally
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
