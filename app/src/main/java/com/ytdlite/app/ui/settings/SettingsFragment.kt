package com.ytdlite.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.ytdlite.app.R
import com.ytdlite.app.data.settings.LanguageCatalog
import com.ytdlite.app.databinding.FragmentSettingsBinding
import com.ytdlite.app.ui.SubScreenHost
import com.ytdlite.app.ui.about.AboutFragment
import com.ytdlite.app.ui.update.UpdateFragment

/**
 * «Настройки» — список категорий (Material 3): иконка, название, подпись.
 * Нажатие открывает отдельный экран поверх вкладки, назад — стрелка в тулбаре.
 */
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding: FragmentSettingsBinding
        get() = requireNotNull(_binding) { "binding доступен только между onCreateView и onDestroyView" }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind(binding.rowFolders, R.drawable.ic_folder_open,
            R.string.folders_title, R.string.settings_folders_subtitle) {
            open(FoldersFragment())
        }
        bind(binding.rowUpdate, R.drawable.ic_update,
            R.string.settings_update, R.string.settings_update_subtitle) {
            open(UpdateFragment())
        }
        bind(binding.rowLanguage, R.drawable.ic_language, R.string.language_title, null) {
            open(LanguageFragment())
        }
        bind(binding.rowAbout, R.drawable.ic_info,
            R.string.settings_about, R.string.settings_about_subtitle) {
            open(AboutFragment())
        }
        updateLanguageSubtitle()
    }

    override fun onResume() {
        super.onResume()
        // Язык мог измениться на вложенном экране.
        if (_binding != null) updateLanguageSubtitle()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun bind(
        row: SettingsRowView,
        @DrawableRes icon: Int,
        @StringRes title: Int,
        @StringRes subtitle: Int?,
        onClick: () -> Unit
    ) {
        row.setIcon(icon)
        row.setTitle(getString(title))
        row.setTrailingIcon(R.drawable.ic_chevron_right)
        row.setSubtitle(subtitle?.let { getString(it) })
        row.setOnRowClickListener(onClick)
    }

    private fun updateLanguageSubtitle() {
        binding.rowLanguage.setSubtitle(LanguageCatalog.currentLabel(requireContext()))
    }

    private fun open(fragment: Fragment) {
        (activity as? SubScreenHost)?.openSubScreen(fragment)
    }
}
