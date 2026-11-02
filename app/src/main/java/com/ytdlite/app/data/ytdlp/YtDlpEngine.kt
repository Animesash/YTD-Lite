package com.ytdlite.app.data.ytdlp

import android.content.Context
import android.os.Environment
import android.util.Log
import com.ytdlite.app.BuildConfig
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Движок yt-dlp внутри приложения.
 *
 * Внутри лежит `youtubedl-android`, который несёт с собой Python и сам yt-dlp,
 * поэтому отдельный бинарник запускать не нужно:
 * - https://github.com/yt-dlp/yt-dlp — сам загрузчик (стабильные релизы);
 * - https://github.com/yt-dlp/yt-dlp-nightly-builds — ночные сборки, их можно
 *   поставить методом [update] с [YtDlpUpdateChannel.NIGHTLY].
 *
 * Слой ничего не знает про UI: только suspend-функции и колбэк прогресса.
 * Экраны «Главная» / «Загрузки» / «Очередь» подключат его позже.
 *
 * Пример:
 * ```kotlin
 * val engine = YtDlpEngine.get(context)
 * val info = engine.videoInfo("https://youtu.be/…")
 * val file = engine.defaultDownloadDirectory()
 * engine.download(
 *     YtDlpDownloadRequest(url = info.webpageUrl ?: url, outputDirectory = file),
 *     onProgress = { progress -> /* обновить «Очередь» */ }
 * )
 * ```
 */
class YtDlpEngine private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val initMutex = Mutex()

    @Volatile
    private var initialized: Boolean = false

    /** Инициализированы ли Python-рантайм и yt-dlp. */
    val isInitialized: Boolean
        get() = initialized

    /** Доступен ли ffmpeg в этой сборке (склейка дорожек, извлечение аудио). */
    val hasFfmpeg: Boolean
        get() = BuildConfig.HAS_FFMPEG

    /**
     * Готовит движок к работе: распаковывает Python, yt-dlp и ffmpeg.
     * Безопасно вызывать многократно и параллельно — тяжёлая работа
     * выполняется один раз.
     */
    suspend fun prepare() {
        if (initialized) return
        withContext(Dispatchers.IO) {
            initMutex.withLock {
                if (initialized) return@withLock
                try {
                    YoutubeDL.init(appContext)
                    // ffmpeg нужен, чтобы склеивать видео+звук и извлекать аудио.
                    // В облегчённой сборке его нет (см. withFfmpeg в build.gradle.kts).
                    if (BuildConfig.HAS_FFMPEG) {
                        FFmpeg.init(appContext)
                    }
                    initialized = true
                    Log.i(TAG, "yt-dlp готов, версия: ${version()}")
                } catch (e: Exception) {
                    throw YtDlpException("Не удалось инициализировать yt-dlp", e)
                }
            }
        }
    }

    /** Версия установленного yt-dlp (например «2026.10.31»), либо null. */
    fun version(): String? = runCatching { YoutubeDL.versionName(appContext) }.getOrNull()

    /** Название сборки yt-dlp (stable / nightly), если известно. */
    fun versionTag(): String? = runCatching { YoutubeDL.version(appContext) }.getOrNull()

    /**
     * Обновляет сам yt-dlp с GitHub: стабильный релиз или ночную сборку.
     * Нужно, когда сайты начинают ломать извлечение.
     */
    suspend fun update(channel: YtDlpUpdateChannel = YtDlpUpdateChannel.NIGHTLY): YtDlpUpdateResult {
        prepare()
        return withContext(Dispatchers.IO) {
            try {
                when (YoutubeDL.updateYoutubeDL(appContext, channel.toLibraryChannel())) {
                    YoutubeDL.UpdateStatus.DONE -> YtDlpUpdateResult.UPDATED
                    YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> YtDlpUpdateResult.ALREADY_UP_TO_DATE
                    else -> YtDlpUpdateResult.UNKNOWN
                }
            } catch (e: Exception) {
                throw YtDlpException("Не удалось обновить yt-dlp", e)
            }
        }
    }

    /** Метаданные видео: название, автор, длительность, доступные форматы. */
    suspend fun videoInfo(url: String): YtDlpVideoInfo {
        validateUrl(url)
        prepare()
        return withContext(Dispatchers.IO) {
            try {
                val request = YoutubeDLRequest(url).addOption("--no-playlist")
                YoutubeDL.getInfo(request).toYtDlpVideoInfo()
            } catch (e: Exception) {
                throw YtDlpException("Не удалось получить информацию о видео", e)
            }
        }
    }

    /**
     * Скачивает видео (или аудио) и отдаёт прогресс.
     *
     * [processId] позволяет потом остановить загрузку через [cancel] —
     * это понадобится кнопке «Отменить» на экране «Очередь».
     */
    suspend fun download(
        request: YtDlpDownloadRequest,
        processId: String? = null,
        onProgress: (YtDlpProgress) -> Unit = {}
    ): YtDlpDownloadResult {
        validateUrl(request.url)
        prepare()
        request.outputDirectory.mkdirs()

        return withContext(Dispatchers.IO) {
            try {
                val response = YoutubeDL.execute(
                    request.toYoutubeDLRequest(),
                    processId,
                    redirectErrorStream = false,
                    callback = { percent, etaSeconds, line ->
                        onProgress(YtDlpProgress(percent, etaSeconds, line))
                    }
                )
                YtDlpDownloadResult.Success(exitCode = response.exitCode, output = response.out)
            } catch (e: YoutubeDL.CanceledException) {
                YtDlpDownloadResult.Canceled
            } catch (e: Exception) {
                throw YtDlpException("Ошибка загрузки", e)
            }
        }
    }

    /**
     * Возвращает встроенную в приложение сборку yt-dlp:
     * удаляет скачанную ночную сборку и заново распаковывает ту, что лежит в APK.
     */
    suspend fun restoreBundled() {
        prepare()
        withContext(Dispatchers.IO) {
            try {
                // Сбрасываем метку установленной версии — так библиотека
                // снова считает движок встроенным.
                appContext.getSharedPreferences(LIBRARY_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .remove(LIBRARY_KEY_VERSION)
                    .remove(LIBRARY_KEY_VERSION_NAME)
                    .apply()

                val ytdlpDir = File(
                    File(appContext.noBackupFilesDir, YoutubeDL.baseName),
                    YoutubeDL.ytdlpDirName
                )
                if (ytdlpDir.exists() && !ytdlpDir.deleteRecursively()) {
                    throw YtDlpException("Не удалось удалить скачанную сборку yt-dlp")
                }
                // Распаковываем встроенный бинарник заново.
                YoutubeDL.init_ytdlp(appContext, ytdlpDir)
                Log.i(TAG, "Возвращена встроенная сборка yt-dlp")
            } catch (e: YtDlpException) {
                throw e
            } catch (e: Exception) {
                throw YtDlpException("Не удалось вернуть встроенную версию yt-dlp", e)
            }
        }
    }

    /** true, если используется встроенная сборка (обновлений не было). */
    fun isBundledVersion(): Boolean = versionTag() == null

    /** Останавливает загрузку, запущенную с указанным [processId]. */
    fun cancel(processId: String): Boolean =
        runCatching { YoutubeDL.destroyProcessById(processId) }.getOrDefault(false)

    /**
     * Папка по умолчанию для скачанных файлов:
     * `Android/data/com.ytdlite.app/files/Download` — доступна без разрешений.
     */
    fun defaultDownloadDirectory(): File =
        appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(appContext.filesDir, "downloads")

    private fun validateUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            throw YtDlpException("Пустая ссылка")
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            throw YtDlpException("Ссылка должна начинаться с http:// или https://")
        }
    }

    companion object {
        private const val TAG = "YtDlpEngine"

        // Внутренние ключи библиотеки youtubedl-android: по ним она понимает,
        // что движок был обновлён вручную.
        private const val LIBRARY_PREFS = "youtubedl-android"
        private const val LIBRARY_KEY_VERSION = "dlpVersion"
        private const val LIBRARY_KEY_VERSION_NAME = "dlpVersionName"

        // Канал обновления по умолчанию: ночные сборки свежее и лучше обходят
        // изменения на сайтах. Переключение — на экране «Настройки» позже.
        const val DEFAULT_UPDATE_CHANNEL_NAME: String = "NIGHTLY"

        @Volatile
        private var instance: YtDlpEngine? = null

        /** Единственный экземпляр движка. */
        fun get(context: Context): YtDlpEngine =
            instance ?: synchronized(this) {
                instance ?: YtDlpEngine(context).also { instance = it }
            }
    }
}
