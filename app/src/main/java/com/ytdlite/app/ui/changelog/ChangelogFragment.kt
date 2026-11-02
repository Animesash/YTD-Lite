package com.ytdlite.app.ui.changelog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.ytdlite.app.R
import com.ytdlite.app.data.update.Changelog
import com.ytdlite.app.databinding.FragmentChangelogBinding
import com.ytdlite.app.databinding.ViewChangelogBlockBinding
import com.ytdlite.app.ui.SubScreenHost
import com.ytdlite.app.ui.TitledFragment

/**
 * «История изменений»: плитки выпусков (версия + дата). Нажатие на плитку
 * открывает подробности — что нового в этой версии.
 */
class ChangelogFragment : Fragment(R.layout.fragment_changelog), TitledFragment {

    override val screenTitleRes: Int = R.string.changelog_title

    private var _binding: FragmentChangelogBinding? = null
    private val binding: FragmentChangelogBinding
        get() = requireNotNull(_binding) { "binding доступен только между onCreateView и onDestroyView" }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangelogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val container = binding.blocksContainer
        Changelog.entries.forEachIndexed { index, entry ->
            val item = ViewChangelogBlockBinding.inflate(layoutInflater, container, false)
            item.version.text = entry.version
            item.date.text = entry.date
            item.currentBadge.isVisible = index == 0
            item.root.setOnClickListener {
                (activity as? SubScreenHost)?.openSubScreen(
                    ChangelogDetailsFragment().apply {
                        arguments = bundleOf(ChangelogDetailsFragment.ARG_VERSION to entry.version)
                    }
                )
            }
            container.addView(item.root)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
