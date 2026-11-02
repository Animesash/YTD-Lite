package com.ytdlite.app.ui.downloads

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.ytdlite.app.R
import com.ytdlite.app.data.download.DownloadHistory
import com.ytdlite.app.data.download.DownloadedFile
import com.ytdlite.app.data.settings.MediaKind
import com.ytdlite.app.databinding.FragmentDownloadsBinding

/**
 * Вкладка «Загрузки»: история сохранённых файлов.
 *
 * Список берётся из [DownloadHistory] и обновляется при каждом возврате на
 * вкладку, поэтому новая загрузка появляется здесь сразу после окончания.
 * Нажатие открывает файл во внешнем плеере, корзина — удаляет с устройства.
 */
class DownloadsFragment : Fragment(R.layout.fragment_downloads) {

    private var _binding: FragmentDownloadsBinding? = null
    private val binding: FragmentDownloadsBinding
        get() = requireNotNull(_binding) { "binding доступен между onCreateView и onDestroyView" }

    private val adapter = DownloadsAdapter(
        onOpen = { file -> openFile(file) },
        onDelete = { file -> askDelete(file) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.listDownloads.layoutManager = LinearLayoutManager(requireContext())
        binding.listDownloads.adapter = adapter
        reload()
    }

    override fun onResume() {
        super.onResume()
        // Файл мог добавиться, пока экран был скрыт (или удалиться в другом приложении).
        if (_binding != null) reload()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun reload() {
        val binding = _binding ?: return
        val files = DownloadHistory.list(requireContext())
        adapter.submit(files)
        val empty = files.isEmpty()
        binding.emptyDownloads.isVisible = empty
        binding.listDownloads.isVisible = !empty
    }

    // ------------------------------------------------------------------ открыть

    private fun openFile(file: DownloadedFile) {
        val context = requireContext()
        val uri = file.uri?.let { runCatching { Uri.parse(it) }.getOrNull() }
            ?: run {
                val local = DownloadHistory.localFile(file)
                if (local == null) {
                    showMessage(getString(R.string.downloads_open_failed))
                    return
                }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", local)
            }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeOf(file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val opened = runCatching { startActivity(intent) }.isSuccess
        if (!opened) {
            showMessage(getString(R.string.downloads_open_failed))
        }
    }

    private fun mimeOf(file: DownloadedFile): String {
        val extension = file.name.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: if (file.kind == MediaKind.AUDIO) "audio/*" else "video/*"
    }

    // ------------------------------------------------------------------ удалить

    private fun askDelete(file: DownloadedFile) {
        MaterialAlertDialogBuilder(
            requireContext(),
            com.google.android.material.R.style.MaterialAlertDialog_Material3Expressive
        )
            .setTitle(R.string.downloads_delete_title)
            .setMessage(getString(R.string.downloads_delete_message, file.name))
            .setPositiveButton(R.string.downloads_delete) { _, _ -> delete(file) }
            .setNegativeButton(R.string.queue_cancel, null)
            .show()
    }

    private fun delete(file: DownloadedFile) {
        val deleted = DownloadHistory.remove(requireContext(), file.id)
        reload()
        showMessage(
            if (deleted) {
                getString(R.string.downloads_deleted, file.name)
            } else {
                getString(R.string.downloads_delete_failed)
            }
        )
    }

    private fun showMessage(text: String) {
        _binding?.root?.let { Snackbar.make(it, text, Snackbar.LENGTH_SHORT).show() }
    }
}
