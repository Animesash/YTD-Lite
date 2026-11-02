package com.ytdlite.app.data.ytdlp

import android.content.Context

/** Какая сборка yt-dlp сейчас используется. */
enum class YtDlpSource {

    /** Встроенная в приложение (по умолчанию). */
    BUNDLED,

    /** Скачанный стабильный релиз yt-dlp (обновление для «Встроенной сборки»). */
    STABLE,

    /** Скачанная с GitHub ночная сборка. */
    NIGHTLY
}

/** Настройки движка, которые выбирает пользователь. */
class YtDlpPreferences(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var source: YtDlpSource
        get() = when (prefs.getString(KEY_SOURCE, null)) {
            VALUE_NIGHTLY -> YtDlpSource.NIGHTLY
            VALUE_STABLE -> YtDlpSource.STABLE
            else -> YtDlpSource.BUNDLED
        }
        set(value) {
            val stored = when (value) {
                YtDlpSource.NIGHTLY -> VALUE_NIGHTLY
                YtDlpSource.STABLE -> VALUE_STABLE
                YtDlpSource.BUNDLED -> VALUE_BUNDLED
            }
            prefs.edit().putString(KEY_SOURCE, stored).apply()
        }

    private companion object {
        const val PREFS_NAME = "ytdlite-settings"
        const val KEY_SOURCE = "ytdlp_source"
        const val VALUE_BUNDLED = "bundled"
        const val VALUE_STABLE = "stable"
        const val VALUE_NIGHTLY = "nightly"
    }
}
