package com.ytdlite.app.data.ytdlp

/** Результат обновления самого yt-dlp. */
enum class YtDlpUpdateResult {
    /** Скачана и установлена более свежая сборка. */
    UPDATED,

    /** Уже стоит актуальная версия. */
    ALREADY_UP_TO_DATE,

    /** Сервер не вернул статус (не критично). */
    UNKNOWN
}

/** Результат загрузки. */
sealed interface YtDlpDownloadResult {

    /** Файл(ы) успешно скачаны. */
    data class Success(
        val exitCode: Int,
        val output: String
    ) : YtDlpDownloadResult

    /** Загрузка остановлена пользователем. */
    data object Canceled : YtDlpDownloadResult
}
