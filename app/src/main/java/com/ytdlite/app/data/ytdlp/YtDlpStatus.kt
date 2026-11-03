package com.ytdlite.app.data.ytdlp

import android.content.Context
import com.ytdlite.app.R
import com.ytdlite.app.data.settings.LanguageCatalog

/** Текст «Сейчас работает: …» для строки настроек и экрана yt-dlp. */
object YtDlpStatus {

    fun describe(context: Context): String {
        // Контекст приложения живёт дольше Activity, поэтому локаль берём явно.
        val app = LanguageCatalog.localized(context.applicationContext)
        val version = YtDlpEngine.get(app).version()
        return when (YtDlpPreferences(app).source) {
            YtDlpSource.NIGHTLY -> if (version != null) {
                app.getString(R.string.update_ytdlp_current_nightly, version)
            } else {
                app.getString(R.string.update_ytdlp_current_bundled)
            }

            YtDlpSource.STABLE -> if (version != null) {
                app.getString(R.string.update_ytdlp_current_stable, version)
            } else {
                app.getString(R.string.update_ytdlp_current_bundled)
            }

            YtDlpSource.BUNDLED -> app.getString(R.string.update_ytdlp_current_bundled)
        }
    }
}
