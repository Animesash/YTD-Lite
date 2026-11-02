package com.ytdlite.app.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ytdlite.app.BuildConfig
import com.ytdlite.app.R
import com.ytdlite.app.databinding.FragmentAboutBinding
import com.ytdlite.app.ui.changelog.ChangelogFragment
import com.ytdlite.app.ui.SubScreenHost
import com.ytdlite.app.ui.TitledFragment

/**
 * Экран «О приложении»: значок, версия и кнопка «История изменений»,
 * которая открывает отдельный экран со списком выпусков.
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
        binding.buttonChangelog.setOnClickListener {
            (activity as? SubScreenHost)?.openSubScreen(ChangelogFragment())
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
