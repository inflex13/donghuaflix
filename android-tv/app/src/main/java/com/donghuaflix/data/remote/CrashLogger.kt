package com.donghuaflix.data.remote

import android.os.Build
import android.util.Log
import com.donghuaflix.BuildConfig
import com.donghuaflix.data.remote.dto.CrashLogRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashLogger @Inject constructor(
    private val api: DonghuaApi,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun logCrash(throwable: Throwable, screen: String? = null) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        send(
            level = "crash",
            message = throwable.message ?: throwable.javaClass.simpleName,
            stacktrace = sw.toString(),
            screen = screen,
        )
    }

    fun logError(message: String, screen: String? = null, extra: String? = null) {
        send(level = "error", message = message, screen = screen, extra = extra)
    }

    fun logWarning(message: String, screen: String? = null) {
        send(level = "warning", message = message, screen = screen)
    }

    private fun send(
        level: String,
        message: String,
        stacktrace: String? = null,
        screen: String? = null,
        extra: String? = null,
    ) {
        scope.launch {
            try {
                api.sendCrashLog(
                    CrashLogRequest(
                        level = level,
                        message = message,
                        stacktrace = stacktrace,
                        appVersion = BuildConfig.VERSION_NAME,
                        deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})",
                        screen = screen,
                        extra = extra,
                    )
                )
            } catch (e: Exception) {
                Log.w("CrashLogger", "Failed to send crash log: ${e.message}")
            }
        }
    }
}
