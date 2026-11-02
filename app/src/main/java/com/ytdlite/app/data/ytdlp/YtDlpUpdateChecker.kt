package com.ytdlite.app.data.ytdlp

import com.ytdlite.app.data.update.VersionCompare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Проверка, есть ли новая версия yt-dlp в выбранном канале.
 *
 * Ничего не скачивает: спрашивает у GitHub API последний релиз и сравнивает
 * его с установленной версией. Установка — отдельно, через [YtDlpEngine.update]
 * и только после подтверждения пользователем.
 */
class YtDlpUpdateChecker {

    sealed interface Result {

        /** Есть версия новее установленной. */
        data class Available(val version: String) : Result

        /** Установлена последняя версия канала. */
        data object UpToDate : Result

        /** GitHub недоступен или ответ не разобрался. */
        data class Failed(val message: String) : Result
    }

    suspend fun check(channel: YtDlpUpdateChannel, currentVersion: String?): Result =
        withContext(Dispatchers.IO) {
            try {
                val tag = latestTag(channel)
                if (tag.isBlank()) return@withContext Result.Failed("HTTP error")
                val latest = tag.removePrefix("v").trim()
                if (currentVersion.isNullOrBlank() ||
                    VersionCompare.isNewer(latest, currentVersion)
                ) {
                    Result.Available(latest)
                } else {
                    Result.UpToDate
                }
            } catch (e: Exception) {
                Result.Failed(e.message ?: e.javaClass.simpleName)
            }
        }

    private fun latestTag(channel: YtDlpUpdateChannel): String {
        val connection = (URL(url(channel)).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "YTD-Lite")
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("HTTP ${connection.responseCode}")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(body).optString("tag_name")
        } finally {
            connection.disconnect()
        }
    }

    private fun url(channel: YtDlpUpdateChannel): String = when (channel) {
        YtDlpUpdateChannel.STABLE ->
            "https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest"

        YtDlpUpdateChannel.NIGHTLY ->
            "https://api.github.com/repos/yt-dlp/yt-dlp-nightly-builds/releases/latest"
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
    }
}
