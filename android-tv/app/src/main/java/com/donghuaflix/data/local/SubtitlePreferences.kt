package com.donghuaflix.data.local

import android.content.Context
import com.donghuaflix.ui.player.SubtitleSize
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubtitlePreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("subtitle_prefs", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", true)
        set(value) = prefs.edit().putBoolean("enabled", value).apply()

    var size: SubtitleSize
        get() {
            val ordinal = prefs.getInt("size", SubtitleSize.MEDIUM.ordinal)
            return SubtitleSize.entries.getOrElse(ordinal) { SubtitleSize.MEDIUM }
        }
        set(value) = prefs.edit().putInt("size", value.ordinal).apply()

    var bgEnabled: Boolean
        get() = prefs.getBoolean("bg_enabled", true)
        set(value) = prefs.edit().putBoolean("bg_enabled", value).apply()
}
