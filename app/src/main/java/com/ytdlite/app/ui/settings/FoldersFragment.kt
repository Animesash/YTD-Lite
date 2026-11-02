package com.ytdlite.app.ui.settings

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.ytdlite.app.R
import com.ytdlite.app.data.settings.FolderPreferences
import com.ytdlite.app.data.settings.MediaKind
import com.ytdlite.app.data.settings.SelectedFolder
import com.ytdlite.app.databinding.FragmentFoldersBinding
import com.ytdlite.app.ui.TitledFragment

/**
 * Вкладка «Папки»: куда сохранять видео и аудио.
 *
 * Папка выбирается системным диалогом (`ACTION_OPEN_DOCUMENT_TREE`) —
 * разрешений на хранилище не требуется, доступ сохраняется между запусками.
 * Если папка не выбрана, файлы пойдут в папку приложения.
 */
class FoldersFragment : Fragment(R.layout.fragment_folders), TitledFragment {

    override val screenTitleRes: Int = R.string.folders_title

    private var _binding: FragmentFoldersBinding? = null
    private val binding: FragmentFoldersBinding
        get() = requireNotNull(_binding) { "binding доступен только между onCreateView и onDestroyView" }

    private val pickVideo = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> onFolderPicked(uri, MediaKind.VIDEO) }

    private val pickAudio = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> onFolderPicked(uri, MediaKind.AUDIO) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rowVideo.setIcon(R.drawable.ic_folder_open)
        binding.rowAudio.setIcon(R.drawable.ic_folder_open)
        binding.rowVideo.setTrailingIcon(R.drawable.ic_chevron_right)
        binding.rowAudio.setTrailingIcon(R.drawable.ic_chevron_right)

        binding.rowVideo.setOnRowClickListener { pickVideo.launch(null) }
        binding.rowAudio.setOnRowClickListener { pickAudio.launch(null) }

        binding.resetFolders.setOnClickListener {
            FolderPreferences.clear(requireContext(), MediaKind.VIDEO)
            FolderPreferences.clear(requireContext(), MediaKind.AUDIO)
            render()
            Snackbar.make(binding.root, R.string.folders_reset_done, Snackbar.LENGTH_SHORT).show()
        }

        render()
    }

    override fun onResume() {
        super.onResume()
        // Доступ к папке мог пропасть, пока экран был в фоне.
        if (_binding != null) render()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun onFolderPicked(uri: Uri?, kind: MediaKind) {
        if (uri == null) return
        val context = requireContext()
        FolderPreferences.setSelected(context, kind, uri)
        val stored = FolderPreferences.selected(context, kind)
        render()
        if (stored == null || !stored.hasAccess) {
            Snackbar.make(binding.root, R.string.folders_pick_failed, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun render() {
        val context = requireContext()
        bindRow(binding.rowVideo, R.string.folders_video_title,
            FolderPreferences.selected(context, MediaKind.VIDEO))
        bindRow(binding.rowAudio, R.string.folders_audio_title,
            FolderPreferences.selected(context, MediaKind.AUDIO))
        binding.resetFolders.isEnabled = FolderPreferences.isAnySelected(context)
    }

    private fun bindRow(row: SettingsRowView, @StringRes title: Int, folder: SelectedFolder?) {
        row.setTitle(getString(title))
        row.setSubtitle(
            when {
                folder == null -> getString(R.string.folders_default)
                !folder.hasAccess -> getString(R.string.folders_no_access)
                else -> folder.name + "\n" + FolderPreferences.readablePath(folder.uri)
            }
        )
    }
}
