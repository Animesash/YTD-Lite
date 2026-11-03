package com.ytdlite.app.data.queue

import android.content.Context
import com.ytdlite.app.R
import com.ytdlite.app.data.settings.LanguageCatalog
import com.ytdlite.app.data.download.DownloadHistory
import com.ytdlite.app.data.download.DownloadSaver
import com.ytdlite.app.data.download.DownloadedFile
import com.ytdlite.app.data.settings.DownloadTarget
import com.ytdlite.app.data.ytdlp.YtDlpDownloadResult
import com.ytdlite.app.data.ytdlp.YtDlpEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

/**
 * Очередь загрузок, общая на всё приложение.
 *
 * Задачи качаются по одной: движок yt-dlp тяжёлый, а телефон не любит
 * несколько параллельных Python-процессов. Состояние живёт в [StateFlow] —
 * экраны «Очередь» и нижний лист «Главной» просто подписываются на него.
 *
 * После успешной загрузки файл попадает в [DownloadHistory] (экран «Загрузки»),
 * а задача остаётся в очереди со статусом [DownloadState.DONE], пока её
 * не уберут.
 */
object DownloadQueue {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runnerMutex = Mutex()

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())

    /** Текущие и завершённые задачи, новые сверху. */
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    @Volatile
    private var appContext: Context? = null

    /** Вызывается один раз из [com.ytdlite.app.YtdLiteApp]. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** Ставит задачу в очередь и, если очередь простаивает, запускает её. */
    fun enqueue(context: Context, task: DownloadTask) {
        appContext = context.applicationContext
        val prepared = if (task.id.isBlank()) task.copy(id = UUID.randomUUID().toString()) else task
        _tasks.update { current -> listOf(prepared) + current }
        startRunner()
    }

    /** Отменяет активную задачу и убирает ожидающую. */
    fun cancel(taskId: String) {
        val task = _tasks.value.firstOrNull { it.id == taskId } ?: return
        when (task.state) {
            DownloadState.PENDING -> remove(taskId)
            DownloadState.RUNNING -> {
                val context = appContext
                if (context != null) {
                    YtDlpEngine.get(context).cancel(taskId)
                }
                _tasks.update { list ->
                    list.map { if (it.id == taskId) it.copy(state = DownloadState.CANCELED) else it }
                }
            }

            else -> remove(taskId)
        }
    }

    /** Убирает задачу из списка (независимо от состояния). */
    fun remove(taskId: String) {
        _tasks.update { list -> list.filterNot { it.id == taskId } }
    }

    /** Повторяет неудачную задачу. */
    fun retry(taskId: String) {
        _tasks.update { list ->
            list.map {
                if (it.id == taskId) {
                    it.copy(state = DownloadState.PENDING, percent = 0f, message = null)
                } else {
                    it
                }
            }
        }
        startRunner()
    }

    private fun startRunner() {
        scope.launch {
            runnerMutex.withLock {
                while (true) {
                    val next = _tasks.value.firstOrNull { it.state == DownloadState.PENDING }
                        ?: break
                    runTask(next)
                }
            }
        }
    }

    private suspend fun runTask(task: DownloadTask) {
        val context = appContext ?: return
        update(task.id) { it.copy(state = DownloadState.RUNNING, percent = 0f, message = null) }

        val tempDir = File(context.cacheDir, "queue-${task.id}")
        val outputDir = when (val target = task.target) {
            is DownloadTarget.Local -> target.dir
            is DownloadTarget.Tree -> tempDir
        }
        outputDir.mkdirs()

        val request = task.request.copy(outputDirectory = outputDir)

        try {
            val result = YtDlpEngine.get(context).download(request, processId = task.id) { progress ->
                update(task.id) { it.copy(percent = progress.percent) }
            }
            // Пользователь мог отменить задачу, пока шёл процесс.
            if (_tasks.value.firstOrNull { it.id == task.id }?.state == DownloadState.CANCELED) {
                remove(task.id)
                return
            }
            when (result) {
                is YtDlpDownloadResult.Success -> complete(context, task, outputDir)
                YtDlpDownloadResult.Canceled -> remove(task.id)
            }
        } catch (e: Exception) {
            update(task.id) {
                it.copy(
                    state = DownloadState.FAILED,
                    message = e.message ?: e.javaClass.simpleName
                )
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /** Раскладывает готовый файл в папку и записывает его в историю «Загрузок». */
    private fun complete(context: Context, task: DownloadTask, dir: File) {
        val file = newestFile(dir)
        if (file == null) {
            update(task.id) {
                it.copy(state = DownloadState.FAILED, message = LanguageCatalog.localized(context).getString(R.string.queue_no_file))
            }
            return
        }
        val size = file.length()

        try {
            val uri = when (val target = task.target) {
                is DownloadTarget.Tree -> DownloadSaver.copyToTree(context, file, target.uri)
                is DownloadTarget.Local -> null
            }
            if (uri != null) {
                // Файл перенесён в выбранную папку — временную копию убираем.
                file.delete()
            }
            DownloadHistory.add(
                context,
                DownloadedFile(
                    id = task.id,
                    name = file.name,
                    kind = task.kind,
                    uri = uri?.toString(),
                    path = if (uri == null) file.absolutePath else null,
                    sizeBytes = size,
                    savedAt = System.currentTimeMillis(),
                    title = task.title
                )
            )
            update(task.id) {
                it.copy(state = DownloadState.DONE, percent = 100f, savedName = file.name)
            }
        } catch (e: Exception) {
            update(task.id) {
                it.copy(
                    state = DownloadState.FAILED,
                    message = e.message ?: e.javaClass.simpleName
                )
            }
        }
    }

    private fun newestFile(dir: File): File? = dir.listFiles()
        ?.filter { file ->
            file.isFile && file.length() > 0L &&
                !file.name.endsWith(".part") && !file.name.endsWith(".ytdl")
        }
        ?.maxByOrNull { it.lastModified() }

    private inline fun update(taskId: String, crossinline block: (DownloadTask) -> DownloadTask) {
        _tasks.update { list ->
            list.map { if (it.id == taskId) block(it) else it }
        }
    }
}
