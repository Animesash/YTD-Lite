package com.ytdlite.app.ui.update

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ytdlite.app.R
import com.ytdlite.app.data.ytdlp.YtDlpStatus
import com.ytdlite.app.databinding.FragmentUpdateBinding
import com.ytdlite.app.ui.SubScreenHost
import com.ytdlite.app.ui.TitledFragment

/**
 * Экран «Обновление».
 *
 * Здесь только категория yt-dlp: строка «Обновление и скачать yt-dlp» с
 * текущим движком в подписи. Проверка и установка обновлений — на отдельном
 * экране [YtDlpFragment].
 */
class UpdateFragment : Fragment(R.layout.fragment_update), TitledFragment {

    override val screenTitleRes: Int = R.string.update_title

    private var _binding: FragmentUpdateBinding? = null
    private val binding: FragmentUpdateBinding
        get() = requireNotNull(_binding) { "binding доступен только между onCreateView и onDestroyView" }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rowYtdlp.apply {
            setIcon(R.drawable.ic_update)
            setTitle(getString(R.string.update_ytdlp_open))
            setTrailingIcon(R.drawable.ic_chevron_right)
            setOnRowClickListener {
                (activity as? SubScreenHost)?.openSubScreen(YtDlpFragment())
            }
        }
        updateYtDlpSummary()
    }

    override fun onResume() {
        super.onResume()
        // Движок могли переключить на экране yt-dlp.
        if (_binding != null) updateYtDlpSummary()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    /** Подпись строки: какой движок работает сейчас. */
    private fun updateYtDlpSummary() {
        binding.rowYtdlp.setSubtitle(YtDlpStatus.describe(requireContext()))
    }
}
