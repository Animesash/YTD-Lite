package com.ytdlite.app.data.ytdlp

import com.ytdlite.app.BuildConfig
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File

/**
 * Параметры одной загрузки.
 *
 * Каталог по умолчанию — приватная папка приложения
 * (`getExternalFilesDir(DIRECTORY_DOWNLOADS)`), поэтому загрузка работает без
 * разрешений на хранилище на любой версии Android. Перенос в общую папку
 * «Загрузки» появится на экране «Загрузки».
 */
data class YtDlpDownloadRequest(
    val url: String,
    val outputDirectory: File,
    /** Селектор формата yt-dlp, по умолчанию лучшее видео+звук. */
    val formatSelector: String = DEFAULT_FORMAT_SELECTOR,
    val audioOnly: Boolean = false,
    /** Желаемый битрейт аудио в кбит/с; null — лучшее доступное качество. */
    val audioBitrateKbps: Int? = null,
    /** Дополнительные опции yt-dlp, например mapOf("--limit-rate" to "2M"). */
    val extraOptions: Map<String, String> = emptyMap(),
    /** Имя файла без расширения: yt-dlp подставит его сам. */
    val outputTemplate: String = DEFAULT_OUTPUT_TEMPLATE
) {

    internal fun toYoutubeDLRequest(): YoutubeDLRequest {
        val request = YoutubeDLRequest(url)
        request.addOption("-o", File(outputDirectory, outputTemplate).absolutePath)
        request.addOption("--no-playlist")

        if (audioOnly) {
            // Качество аудио задаётся верхней границей битрейта (abr).
            val selector = audioBitrateKbps?.let { "ba[abr<=$it]/ba" } ?: "ba"
            if (BuildConfig.HAS_FFMPEG) {
                request.addOption("-x")
                request.addOption("--audio-format", "m4a")
                request.addOption("-f", selector)
            } else {
                // Без ffmpeg конвертировать нельзя — берём готовую аудиодорожку.
                request.addOption("-f", selector)
            }
        } else {
            request.addOption("-f", formatSelector)
            if (BuildConfig.HAS_FFMPEG) {
                request.addOption("--merge-output-format", "mp4")
            }
        }

        extraOptions.forEach { (option, argument) -> request.addOption(option, argument) }
        return request
    }

    companion object {
        /**
         * Селектор формата по умолчанию.
         *
         * С ffmpeg можно брать лучшие раздельные дорожки и склеивать их
         * (`bv*+ba/b`). Без ffmpeg берём готовый прогрессивный файл со звуком.
         */
        val DEFAULT_FORMAT_SELECTOR: String
            get() = if (BuildConfig.HAS_FFMPEG) "bv*+ba/b" else "b[ext=mp4]/b"

        const val DEFAULT_OUTPUT_TEMPLATE: String = "%(title)s [%(id)s].%(ext)s"
    }
}
