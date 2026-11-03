package com.ytdlite.app.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ytdlite.app.BuildConfig
import com.ytdlite.app.R
import com.ytdlite.app.databinding.FragmentAboutBinding
import com.ytdlite.app.ui.TitledFragment

/**
 * Экран «О приложении»: значок, версия и строка «История изменений».
 *
 * История изменений живёт не внутри приложения, а на GitHub: строка оформлена
 * как пункт списка «Настроек» (`SettingsRowView`) и открывает страницу релизов
 * [RELEASES_URL] в браузере.
 */
class AboutFragment : Fragment(R.layout.fragment_about), TitledFragment {

    override val screenTitleRes: Int = R.string.about_title

    private var _binding: FragmentAboutBinding? = null
    private val binding: FragmentAboutBinding
        get() = requireNotNull(_binding) { "binding доступен только между onCreateView и onDestroyView" }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textVersion.text = getString(
            R.string.about_version,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        )
        binding.rowChangelog.apply {
            setIcon(R.drawable.ic_history)
            setTitle(getString(R.string.about_changelog))
            setSubtitle(getString(R.string.about_changelog_subtitle))
            setTrailingIcon(R.drawable.ic_chevron_right)
            setOnRowClickListener { openReleases() }
        }
    }

    /** Открывает страницу релизов: там история изменений и готовые APK. */
    private fun openReleases() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL))
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Браузера на устройстве нет — показываем понятную подсказку.
            Toast.makeText(requireContext(), R.string.about_link_failed, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        /** Страница релизов проекта. */
        const val RELEASES_URL = "https://github.com/Animesash/YTD-Lite/releases"
    }
}
