package com.ytdlite.app.data.download

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.ytdlite.app.data.settings.MediaKind
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Один сохранённый файл в истории «Загрузок».
 *
 * @param uri документ в папке, выбранной через SAF (тогда [path] = null).
 * @param path файл в папке приложения (тогда [uri] = null).
 */
data class DownloadedFile(
    val id: String,
    val name: String,
    val kind: MediaKind,
    val uri: String?,
    val path: String?,
    val sizeBytes: Long,
    val savedAt: Long,
    val title: String
)

/**
 * История скачанных файлов.
 *
 * Хранится в SharedPreferences обычным JSON — без Room и лишних зависимостей,
 * список небольшой (последние [MAX_ITEMS] файлов).
 */
object DownloadHistory {

    private const val PREFS = "ytdlite-downloads"
    private const val KEY_FILES = "files"
    private const val MAX_ITEMS = 500

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Файлы, новые сверху. */
    fun list(context: Context): List<DownloadedFile> {
        val raw = prefs(context).getString(KEY_FILES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.toFile()
            }
        }.getOrDefault(emptyList()).sortedByDescending { it.savedAt }
    }

    fun add(context: Context, file: DownloadedFile) {
        val updated = (listOf(file) + list(context).filterNot { it.id == file.id })
            .take(MAX_ITEMS)
        save(context, updated)
    }

    /**
     * Удаляет запись из истории и сам файл: документ SAF или файл в папке
     * приложения. Возвращает true, если файл действительно удалён.
     */
    fun remove(context: Context, id: String): Boolean {
        val file = list(context).firstOrNull { it.id == id } ?: return false
        val deleted = deleteContent(context, file)
        save(context, list(context).filterNot { it.id == id })
        return deleted
    }

    /** Файл на диске, если он лежит в папке приложения (нужно для «Открыть»). */
    fun localFile(file: DownloadedFile): File? =
        file.path?.let { File(it) }?.takeIf { it.exists() }

    private fun deleteContent(context: Context, file: DownloadedFile): Boolean {
        val app = context.applicationContext
        val uri = file.uri?.let { runCatching { Uri.parse(it) }.getOrNull() }
        if (uri != null) {
            return runCatching { DocumentsContract.deleteDocument(app.contentResolver, uri) }
                .getOrDefault(false)
        }
        val path = file.path ?: return false
        return runCatching { File(path).delete() }.getOrDefault(false)
    }

    private fun save(context: Context, files: List<DownloadedFile>) {
        val array = JSONArray()
        files.forEach { array.put(it.toJson()) }
        prefs(context).edit().putString(KEY_FILES, array.toString()).apply()
    }

    private fun DownloadedFile.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("kind", kind.name)
        put("uri", uri)
        put("path", path)
        put("size", sizeBytes)
        put("savedAt", savedAt)
        put("title", title)
    }

    private fun JSONObject.toFile(): DownloadedFile? {
        val id = optString("id").takeIf { it.isNotEmpty() } ?: return null
        val name = optString("name").takeIf { it.isNotEmpty() } ?: return null
        val kind = runCatching { MediaKind.valueOf(optString("kind", MediaKind.VIDEO.name)) }
            .getOrDefault(MediaKind.VIDEO)
        return DownloadedFile(
            id = id,
            name = name,
            kind = kind,
            uri = optString("uri").takeIf { it.isNotEmpty() && it != "null" },
            path = optString("path").takeIf { it.isNotEmpty() && it != "null" },
            sizeBytes = optLong("size", 0L),
            savedAt = optLong("savedAt", 0L),
            title = optString("title").takeIf { it.isNotEmpty() } ?: name
        )
    }
}
