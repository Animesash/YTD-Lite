package com.ytdlite.app.data.ytdlp

import com.yausername.youtubedl_android.YoutubeDL

/**
 * Канал обновления самого yt-dlp.
 *
 * - [STABLE] — релизы https://github.com/yt-dlp/yt-dlp
 * - [NIGHTLY] — ночные сборки https://github.com/yt-dlp/yt-dlp-nightly-builds
 *
 * Библиотека `youtubedl-android` скачивает соответствующую сборку с GitHub
 * и подменяет ею исполняемый файл внутри приложения.
 */
enum class YtDlpUpdateChannel {

    /** Стабильные релизы yt-dlp. */
    STABLE,

    /** Ночные сборки yt-dlp (свежие фиксы для сайтов). */
    NIGHTLY;

    internal fun toLibraryChannel(): YoutubeDL.UpdateChannel = when (this) {
        STABLE -> YoutubeDL.UpdateChannel.STABLE
        NIGHTLY -> YoutubeDL.UpdateChannel.NIGHTLY
    }
}
