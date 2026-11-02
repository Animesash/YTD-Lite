package com.ytdlite.app.ui.update

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.ytdlite.app.R
import com.ytdlite.app.data.ytdlp.YtDlpEngine
import com.ytdlite.app.data.ytdlp.YtDlpPreferences
import com.ytdlite.app.data.ytdlp.YtDlpSource
import com.ytdlite.app.data.ytdlp.YtDlpStatus
import com.ytdlite.app.data.ytdlp.YtDlpUpdateChannel
import com.ytdlite.app.data.ytdlp.YtDlpUpdateChecker
import com.ytdlite.app.databinding.FragmentYtdlpBinding
import com.ytdlite.app.ui.TitledFragment
import kotlinx.coroutines.launch

/**
 * Экран «yt-dlp»: выбор сборки (встроенная или ночная) и кнопка
 * «Проверить сейчас».
 *
 * Проверка только спрашивает GitHub, есть ли версия новее; если да —
 * показывается диалог «Доступно обновление» с выбором Да/Нет, и лишь после
 * подтверждения сборка скачивается и устанавливается.
 */
class YtDlpFragment : Fragment(R.layout.fragment_ytdlp), TitledFragment {

    override val screenTitleRes: Int = R.string.update_section_ytdlp

    private var _binding: FragmentYtdlpBinding? = null
    private val binding: FragmentYtdlpBinding
        get() = requireNotNull(_binding) { "binding доступен только между onCreateView и onDestroyView" }

    private val engine: YtDlpEngine by lazy { YtDlpEngine.get(requireContext().applicationContext) }
    private val preferences: YtDlpPreferences by lazy {
        YtDlpPreferences(requireContext().applicationContext)
    }
    private val checker = YtDlpUpdateChecker()

    private var busy: Boolean = false

    /** true, пока состояние переключателя меняется программно. */
    private var changingEngine: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentYtdlpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonCheck.setOnClickListener { checkNow() }
        binding.buttonRestore.setOnClickListener { restoreBundled() }
        binding.toggleEngine.addOnButtonCheckedListener { _, _, isChecked ->
            if (!isChecked || changingEngine) return@addOnButtonCheckedListener
            updateHint()
        }

        render()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) render()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    // ------------------------------------------------------------ состояние

    private fun render() {
        setEngineChecked(
            if (preferences.source == YtDlpSource.NIGHTLY) R.id.option_nightly
            else R.id.option_bundled
        )
        binding.textYtdlpState.text = YtDlpStatus.describe(requireContext())
        updateHint()
        updateButtons()
    }

    private fun isNightlySelected(): Boolean =
        binding.toggleEngine.checkedButtonId == R.id.option_nightly

    /** Какую сборку проверяем: выбранный сегмент задаёт канал. */
    private fun selectedChannel(): YtDlpUpdateChannel =
        if (isNightlySelected()) YtDlpUpdateChannel.NIGHTLY else YtDlpUpdateChannel.STABLE

    private fun setEngineChecked(buttonId: Int) {
        if (binding.toggleEngine.checkedButtonId == buttonId) return
        changingEngine = true
        binding.toggleEngine.check(buttonId)
        changingEngine = false
    }

    /** Подсказка под переключателем рассказывает про выбранную сборку. */
    private fun updateHint() {
        binding.textYtdlpHint.text = getString(
            if (isNightlySelected()) R.string.update_ytdlp_nightly_hint
            else R.string.update_ytdlp_bundled_hint
        )
    }

    private fun updateButtons() {
        binding.buttonCheck.isEnabled = !busy
        binding.optionBundled.isEnabled = !busy
        binding.optionNightly.isEnabled = !busy
        binding.buttonRestore.isVisible = !busy && preferences.source != YtDlpSource.BUNDLED
    }

    private fun setBusy(value: Boolean) {
        busy = value
        binding.progressYtdlp.isVisible = value
        updateButtons()
    }

    private fun showProgress(message: String) {
        binding.progressYtdlp.isVisible = true
        binding.textYtdlpStatus.text = message
        binding.textYtdlpResult.isVisible = false
    }

    private fun showResult(message: String) {
        binding.progressYtdlp.isVisible = false
        binding.textYtdlpResult.isVisible = true
        binding.textYtdlpResult.text = message
        _binding?.let { Snackbar.make(it.root, message, Snackbar.LENGTH_SHORT).show() }
    }

    // -------------------------------------------------------------- проверка

    private fun checkNow() {
        if (busy) return
        setBusy(true)
        showProgress(getString(R.string.update_ytdlp_checking))

        viewLifecycleOwner.lifecycleScope.launch {
            val channel = selectedChannel()
            val result = checker.check(channel, engine.version())
            if (_binding == null) return@launch
            setBusy(false)
            when (result) {
                is YtDlpUpdateChecker.Result.Available -> {
                    binding.progressYtdlp.isVisible = false
                    askToInstall(channel, result.version)
                }

                YtDlpUpdateChecker.Result.UpToDate ->
                    showResult(getString(R.string.update_ytdlp_no_updates, engine.version().orEmpty()))

                is YtDlpUpdateChecker.Result.Failed ->
                    showResult(getString(R.string.update_ytdlp_failed, result.message))
            }
        }
    }

    /** Окно «Доступно обновление» — Да/Нет (M3 Expressive). */
    private fun askToInstall(channel: YtDlpUpdateChannel, version: String) {
        MaterialAlertDialogBuilder(
            requireContext(),
            com.google.android.material.R.style.MaterialAlertDialog_Material3Expressive
        )
            .setTitle(R.string.update_ytdlp_found_title)
            .setMessage(getString(R.string.update_ytdlp_found_message, version))
            .setPositiveButton(R.string.dialog_yes) { _, _ -> install(channel) }
            .setNegativeButton(R.string.dialog_no, null)
            .show()
    }

    // -------------------------------------------------------------- установка

    private fun install(channel: YtDlpUpdateChannel) {
        if (busy) return
        setBusy(true)
        showProgress(getString(R.string.update_ytdlp_installing))

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                engine.update(channel)
                preferences.source = when (channel) {
                    YtDlpUpdateChannel.NIGHTLY -> YtDlpSource.NIGHTLY
                    YtDlpUpdateChannel.STABLE -> YtDlpSource.STABLE
                }
                showResult(getString(R.string.update_ytdlp_updated, engine.version().orEmpty()))
            } catch (e: Exception) {
                showResult(getString(R.string.update_ytdlp_failed, e.message ?: e.javaClass.simpleName))
            } finally {
                setBusy(false)
                _binding?.let { render() }
            }
        }
    }

    private fun restoreBundled() {
        if (busy) return
        setBusy(true)
        showProgress(getString(R.string.update_ytdlp_restoring))

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                engine.restoreBundled()
                preferences.source = YtDlpSource.BUNDLED
                showResult(getString(R.string.update_ytdlp_restored))
            } catch (e: Exception) {
                showResult(getString(R.string.update_ytdlp_failed, e.message ?: e.javaClass.simpleName))
            } finally {
                setBusy(false)
                _binding?.let { render() }
            }
        }
    }
}
