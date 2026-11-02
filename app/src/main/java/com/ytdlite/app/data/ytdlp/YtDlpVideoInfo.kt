package com.ytdlite.app.data.ytdlp

import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo

/** Краткая информация о видео, полученная через yt-dlp (`--dump-json`). */
data class YtDlpVideoInfo(
    val id: String?,
    val title: String?,
    val uploader: String?,
    val durationSeconds: Int,
    val thumbnailUrl: String?,
    val webpageUrl: String?,
    val extractor: String?,
    val viewCount: String?,
    val formats: List<YtDlpFormat>
)

/** Один доступный формат (дорожка) видео или аудио. */
data class YtDlpFormat(
    val formatId: String?,
    val ext: String?,
    val note: String?,
    val height: Int,
    val fps: Int,
    val vcodec: String?,
    val acodec: String?,
    val fileSize: Long,
    val url: String?
) {
    /** Разрешение для показа, например «1080p60». */
    val resolutionLabel: String
        get() = when {
            height <= 0 -> note ?: formatId.orEmpty()
            fps > 30 -> "${height}p${fps}"
            else -> "${height}p"
        }

    /** Только аудио (нет видеокодека или он «none»). */
    val isAudioOnly: Boolean
        get() = vcodec == null || vcodec == "none"
}

internal fun VideoInfo.toYtDlpVideoInfo(): YtDlpVideoInfo = YtDlpVideoInfo(
    id = id,
    title = title ?: fulltitle,
    uploader = uploader,
    durationSeconds = duration,
    thumbnailUrl = thumbnail,
    webpageUrl = webpageUrl,
    extractor = extractor,
    viewCount = viewCount,
    formats = formats?.map { it.toYtDlpFormat() }.orEmpty()
)

internal fun VideoFormat.toYtDlpFormat(): YtDlpFormat = YtDlpFormat(
    formatId = formatId,
    ext = ext,
    note = formatNote,
    height = height,
    fps = fps,
    vcodec = vcodec,
    acodec = acodec,
    fileSize = if (fileSize > 0) fileSize else fileSizeApproximate,
    url = url
)
