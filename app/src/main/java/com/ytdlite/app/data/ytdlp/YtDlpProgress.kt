package com.ytdlite.app.data.ytdlp

/** Прогресс загрузки, который yt-dlp отдаёт построчно. */
data class YtDlpProgress(
    /** Готовность в процентах, 0f..100f. */
    val percent: Float,
    /** Примерное время до конца в секундах, -1 если неизвестно. */
    val etaSeconds: Long,
    /** Сырая строка вывода yt-dlp (пригодится для логов и «Очереди»). */
    val rawLine: String?
)
