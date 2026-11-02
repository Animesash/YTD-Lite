package com.ytdlite.app.ui.changelog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.ytdlite.app.R
import com.ytdlite.app.data.update.Changelog
import com.ytdlite.app.databinding.FragmentChangelogDetailsBinding
import com.ytdlite.app.ui.TitledFragment

/**
 * Подробности выпуска: версия, дата и описание.
 * Версия приходит аргументом из [ChangelogFragment].
 */
class ChangelogDetailsFragment : Fragment(R.layout.fragment_changelog_details), TitledFragment {

    override val screenTitleRes: Int = R.string.changelog_title

    /** В тулбаре показываем саму версию, а не название раздела. */
    override val screenTitle: CharSequence?
        get() = arguments?.getString(ARG_VERSION)

    private var _binding: FragmentChangelogDetailsBinding? = null
    private val binding: FragmentChangelogDetailsBinding
        get() = requireNotNull(_binding) { "binding доступен только между onCreateView и onDestroyView" }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangelogDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val version = arguments?.getString(ARG_VERSION)
        val entry = Changelog.entries.firstOrNull { it.version == version }
        if (entry == null) {
            // Аргумент потерялся — показываем последний выпуск.
            bind(Changelog.entries.first(), isLatest = true)
        } else {
            bind(entry, isLatest = Changelog.entries.firstOrNull() === entry)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun bind(entry: Changelog.Entry, isLatest: Boolean) {
        binding.version.text = entry.version
        binding.date.text = entry.date
        binding.currentBadge.isVisible = isLatest

        binding.summary.text = getString(entry.summary)
    }

    companion object {
        const val ARG_VERSION = "version"
    }
}
