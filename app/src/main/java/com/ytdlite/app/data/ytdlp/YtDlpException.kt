package com.ytdlite.app.data.ytdlp

/** Любая ошибка движка yt-dlp: инициализация, разбор ссылки, загрузка, обновление. */
class YtDlpException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
