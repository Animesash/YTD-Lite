package com.ytdlite.app.data.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

/** Что именно скачивается — от этого зависит папка по умолчанию. */
enum class MediaKind { VIDEO, AUDIO }

/**
 * Выбранная пользователем папка (SAF-дерево документов).
 *
 * @param hasAccess false, если приложение потеряло разрешение на запись
 *   (например, папку удалили) — такую папку нужно выбрать заново.
 */
data class SelectedFolder(
    val uri: Uri,
    val name: String,
    val hasAccess: Boolean
)

/** Куда складывать готовый файл: выбранное дерево или папка приложения. */
sealed interface DownloadTarget {
    data class Tree(val uri: Uri) : DownloadTarget
    data class Local(val dir: File) : DownloadTarget
}

/**
 * Папки сохранения аудио и видео.
 *
 * Доступ выдаётся системным выбором папки (`ACTION_OPEN_DOCUMENT_TREE`) и
 * сохраняется через `takePersistableUriPermission`, поэтому не требует
 * разрешений на хранилище и работает на всех версиях от Android 8.
 */
object FolderPreferences {

    private const val PREFS = "ytdlite-settings"
    private const val KEY_VIDEO = "folder_video"
    private const val KEY_VIDEO_NAME = "folder_video_name"
    private const val KEY_AUDIO = "folder_audio"
    private const val KEY_AUDIO_NAME = "folder_audio_name"

    private const val FLAGS =
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun uriKey(kind: MediaKind) = if (kind == MediaKind.VIDEO) KEY_VIDEO else KEY_AUDIO

    private fun nameKey(kind: MediaKind) =
        if (kind == MediaKind.VIDEO) KEY_VIDEO_NAME else KEY_AUDIO_NAME

    /** null — папка не выбрана, используется папка приложения. */
    fun selected(context: Context, kind: MediaKind): SelectedFolder? {
        val prefs = prefs(context)
        val raw = prefs.getString(uriKey(kind), null) ?: return null
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
        val canWrite = context.applicationContext.contentResolver
            .persistedUriPermissions
            .any { it.uri == uri && it.isWritePermission }
        val name = prefs.getString(nameKey(kind), null)
            ?: displayName(context, uri)
            ?: uri.lastPathSegment
            ?: raw
        return SelectedFolder(uri, name, canWrite)
    }

    /** Брать постоянный доступ к дереву и запомнить выбор. */
    fun setSelected(context: Context, kind: MediaKind, uri: Uri) {
        val app = context.applicationContext
        runCatching { app.contentResolver.takePersistableUriPermission(uri, FLAGS) }
        val name = displayName(app, uri) ?: uri.lastPathSegment ?: uri.toString()
        prefs(app).edit()
            .putString(uriKey(kind), uri.toString())
            .putString(nameKey(kind), name)
            .apply()
    }

    /** Вернуть папку приложения и отпустить доступ к выбранной папке. */
    fun clear(context: Context, kind: MediaKind) {
        val app = context.applicationContext
        val raw = prefs(app).getString(uriKey(kind), null)
        prefs(app).edit().remove(uriKey(kind)).remove(nameKey(kind)).apply()
        if (raw != null) {
            val uri = runCatching { Uri.parse(raw) }.getOrNull()
            if (uri != null) {
                runCatching { app.contentResolver.releasePersistableUriPermission(uri, FLAGS) }
            }
        }
    }

    fun isAnySelected(context: Context): Boolean =
        selected(context, MediaKind.VIDEO) != null || selected(context, MediaKind.AUDIO) != null

    /** Папка приложения по умолчанию: Movies — для видео, Music — для аудио. */
    fun defaultDir(context: Context, kind: MediaKind): File {
        val app = context.applicationContext
        val type = if (kind == MediaKind.VIDEO) Environment.DIRECTORY_MOVIES
        else Environment.DIRECTORY_MUSIC
        val fallback = if (kind == MediaKind.VIDEO) "video" else "audio"
        return app.getExternalFilesDir(type) ?: File(app.filesDir, fallback)
    }

    /** Цель сохранения: выбранная папка либо папка приложения. */
    fun target(context: Context, kind: MediaKind): DownloadTarget {
        val folder = selected(context, kind)
        return if (folder != null && folder.hasAccess) {
            DownloadTarget.Tree(folder.uri)
        } else {
            DownloadTarget.Local(defaultDir(context, kind))
        }
    }

    /** Человекочитаемый путь вида /storage/emulated/0/Movies/YTDLite. */
    fun readablePath(uri: Uri): String {
        val documentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
            ?: return uri.toString()
        val volume = documentId.substringBefore(':', "")
        val path = documentId.substringAfter(':', "").trim('/')
        val root = when {
            volume == "primary" -> "/storage/emulated/0"
            volume.isNotEmpty() -> "/storage/$volume"
            else -> ""
        }
        return if (root.isEmpty()) path else "$root/$path".trimEnd('/')
    }

    /** Отображаемое имя папки, как его показывает система. */
    fun displayName(context: Context, uri: Uri): String? {
        val documentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
            ?: return null
        val docUri = runCatching { DocumentsContract.buildDocumentUriUsingTree(uri, documentId) }
            .getOrNull() ?: return null
        return runCatching {
            context.applicationContext.contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }
}
