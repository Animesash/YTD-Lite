package com.ytdlite.app.ui.home

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.ytdlite.app.R
import com.ytdlite.app.databinding.FragmentHomeBinding

/**
 * Вкладка «Главная».
 *
 * Пользователь вставляет ссылку — справа проявляется круглая кнопка «Поиск».
 * Нажатие открывает нижний лист предпросмотра ([VideoSheetFragment]) с обложкой,
 * названием, выбором «Видео / Аудио», качеством и кнопкой «Скачать».
 */
class HomeFragment : Fragment(R.layout.fragment_home) {

    companion object {
        /** Прозрачность кнопки «Поиск», пока ссылки нет. */
        private const val IDLE_ALPHA = 0.45f
    }

    private var binding: FragmentHomeBinding? = null

    /** Текущее состояние кнопки «Поиск», чтобы не перезапускать анимацию. */
    private var searchShown: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentHomeBinding.bind(view)
        this.binding = binding

        binding.inputUrl.doAfterTextChanged { updateSearchButton(animate = true) }
        binding.inputUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                openPreview()
                true
            } else {
                false
            }
        }
        binding.buttonSearch.setOnClickListener { openPreview() }
        updateSearchButton(animate = false)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    /**
     * Кнопка «Поиск»: в покое она приглушена (но видна — чтобы было понятно,
     * что она есть), а при непустой ссылке становится яркой и активной.
     */
    private fun updateSearchButton(animate: Boolean) {
        val binding = binding ?: return
        val hasLink = binding.inputUrl.text?.toString()?.isNotBlank() == true
        if (hasLink == searchShown) return
        searchShown = hasLink
        if (hasLink) {
            binding.inputLayout.error = null
        }

        val button = binding.buttonSearch
        val targetAlpha = if (hasLink) 1f else IDLE_ALPHA
        val targetScale = if (hasLink) 1f else 0.9f
        button.isClickable = hasLink
        button.animate().cancel()
        if (!animate) {
            button.alpha = targetAlpha
            button.scaleX = targetScale
            button.scaleY = targetScale
            return
        }
        button.animate()
            .alpha(targetAlpha)
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(160L)
            .start()
    }

    private fun openPreview() {
        val binding = binding ?: return
        val url = binding.inputUrl.text?.toString()?.trim().orEmpty()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            binding.inputLayout.error = getString(R.string.home_error_url)
            return
        }
        binding.inputLayout.error = null
        hideKeyboard(binding.inputUrl)
        VideoSheetFragment.newInstance(url)
            .show(parentFragmentManager, VideoSheetFragment.TAG)
    }

    private fun hideKeyboard(view: View) {
        view.clearFocus()
        val manager = requireContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        manager?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
