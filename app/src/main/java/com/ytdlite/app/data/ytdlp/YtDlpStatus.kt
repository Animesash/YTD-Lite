package com.ytdlite.app.data.ytdlp

import android.content.Context
import com.ytdlite.app.R

/** Текст «Сейчас работает: …» для строки настроек и экрана yt-dlp. */
object YtDlpStatus {

    fun describe(context: Context): String {
        val app = context.applicationContext
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
