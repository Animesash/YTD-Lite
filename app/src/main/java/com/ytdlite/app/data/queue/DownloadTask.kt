package com.ytdlite.app.data.queue

import com.ytdlite.app.data.settings.DownloadTarget
import com.ytdlite.app.data.settings.MediaKind
import com.ytdlite.app.data.ytdlp.YtDlpDownloadRequest

/** Состояние задачи в очереди. */
enum class DownloadState {
    /** Ждёт своей очереди. */
    PENDING,

    /** Качается прямо сейчас. */
    RUNNING,

    /** Успешно сохранено (файл уже в истории «Загрузок»). */
    DONE,

    /** Ошибка: текст в [DownloadTask.message]. */
    FAILED,

    /** Отменено пользователем. */
    CANCELED
}

/**
 * Одна задача загрузки.
 *
 * Задачу создаёт «Главная» из нижнего листа предпросмотра, а выполняет
 * [DownloadQueue] — по одной за раз, чтобы не перегружать телефон.
 */
data class DownloadTask(
    val id: String,
    val url: String,
    val title: String,
    val author: String?,
    val kind: MediaKind,
    /** Подпись качества для списка: «1080p», «Лучшее», «192 kbps». */
    val quality: String?,
    val thumbnailUrl: String?,
    val request: YtDlpDownloadRequest,
    val target: DownloadTarget,
    val state: DownloadState = DownloadState.PENDING,
    /** Готовность 0f..100f, -1f если неизвестна. */
    val percent: Float = 0f,
    val message: String? = null,
    val savedName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** Задача ещё не закончилась (её можно отменить). */
    val isActive: Boolean
        get() = state == DownloadState.PENDING || state == DownloadState.RUNNING
}
