package com.ytdlite.app.data.download

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import com.ytdlite.app.R
import java.io.File
import java.io.IOException

/**
 * Сохранение готового файла в папку, выбранную пользователем через SAF.
 *
 * yt-dlp пишет во временную папку приложения, а потом файл копируется в дерево
 * документов — так работает и папка на карте памяти, и облачный провайдер.
 */
object DownloadSaver {

    /**
     * Копирует [source] в дерево [treeUri] и возвращает документ, под которым
     * файл сохранён в выбранной папке.
     *
     * @throws IOException если провайдер не дал создать документ или открыть поток.
     */
    fun copyToTree(context: Context, source: File, treeUri: Uri): Uri {
        val resolver = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        val mime = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(source.extension.lowercase())
            ?: "application/octet-stream"

        val document = DocumentsContract.createDocument(resolver, parent, mime, source.name)
            ?: throw IOException(context.getString(R.string.err_file_create))

        try {
            val output = resolver.openOutputStream(document)
                ?: throw IOException(context.getString(R.string.err_folder_access))
            output.use { out ->
                source.inputStream().use { input -> input.copyTo(out) }
            }
        } catch (e: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, document) }
            throw e
        }
        return document
    }
}
