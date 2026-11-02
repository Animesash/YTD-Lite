package com.ytdlite.app.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.ytdlite.app.R
import com.ytdlite.app.data.queue.DownloadQueue
import com.ytdlite.app.data.queue.DownloadState
import com.ytdlite.app.data.queue.DownloadTask
import com.ytdlite.app.data.settings.FolderPreferences
import com.ytdlite.app.data.settings.MediaKind
import com.ytdlite.app.data.ytdlp.YtDlpDownloadRequest
import com.ytdlite.app.data.ytdlp.YtDlpEngine
import com.ytdlite.app.data.ytdlp.YtDlpVideoInfo
import com.ytdlite.app.databinding.SheetVideoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID

/**
 * Нижний лист предпросмотра (Material 3 Expressive).
 *
 * Показывает обложку, название и автора, переключатель «Видео / Аудио»,
 * варианты качества и кнопку «Скачать». Если ссылка ведёт только на аудио
 * (например, трек), кнопка «Видео» становится недоступной.
 *
 * Сама загрузка уходит в общую [DownloadQueue]: её видно на вкладке «Очередь»
 * и она продолжается, даже если закрыть этот лист.
 */
class VideoSheetFragment : BottomSheetDialogFragment() {

    private var _binding: SheetVideoBinding? = null
    private val binding: SheetVideoBinding? get() = _binding

    private val engine: YtDlpEngine by lazy { YtDlpEngine.get(requireContext()) }

    private var info: YtDlpVideoInfo? = null

    /** Есть ли в ссылке видео: у аудиотреков кнопка «Видео» серая. */
    private var hasVideo: Boolean = true

    private var selectedHeight: Int? = null
    private var selectedBitrate: Int? = null

    /** Значения выбранных чипов качества: id чипа → высота (p) или кбит/с, -1 — «Лучшее». */
    private val chipValues = mutableMapOf<Int, Int>()

    /** Задача, поставленная в очередь из этого листа. */
    private var taskId: String? = null

    private var observeJob: Job? = null

    private var downloading: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = SheetVideoBinding.inflate(inflater, container, false)
        _binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        // Программная смена выбора тоже вызывает слушатель — реагируем только
        // на включение кнопки, чтобы не строить чипы дважды.
        binding.toggleKind.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked) renderQuality()
        }
        binding.buttonDownload.setOnClickListener { startDownload() }

        loadInfo()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    // ---------------------------------------------------------------- информация

    private fun loadInfo() {
        val url = arguments?.getString(ARG_URL)?.takeIf { it.isNotBlank() } ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val loaded = engine.videoInfo(url)
                info = loaded
                showContent(loaded)
            } catch (e: Exception) {
                showError(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    private fun showContent(info: YtDlpVideoInfo) {
        val binding = binding ?: return
        hasVideo = info.formats.isEmpty() || info.formats.any { !it.isAudioOnly && it.height > 0 }

        binding.progressInfo.isVisible = false
        binding.textError.isVisible = false
        binding.groupContent.isVisible = true

        binding.textTitle.text = info.title?.takeIf { it.isNotBlank() }
            ?: arguments?.getString(ARG_URL).orEmpty()
        binding.textSubtitle.text = subtitle(info)

        binding.buttonVideo.isEnabled = hasVideo
        binding.textKindHint.isVisible = !hasVideo

        binding.toggleKind.check(if (hasVideo) R.id.button_video else R.id.button_audio)
        renderQuality()
        loadCover(info.thumbnailUrl)
    }

    private fun showError(message: String) {
        val binding = binding ?: return
        binding.progressInfo.isVisible = false
        binding.groupContent.isVisible = false
        binding.textError.isVisible = true
        binding.textError.text = getString(R.string.sheet_error, message)
    }

    private fun subtitle(info: YtDlpVideoInfo): String {
        val parts = mutableListOf<String>()
        info.uploader?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        formatDuration(info.durationSeconds)?.let { parts.add(it) }
        return parts.joinToString(" · ")
    }

    private fun formatDuration(seconds: Int): String? {
        if (seconds <= 0) return null
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val rest = seconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, rest)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, rest)
        }
    }

    /** Обложка грузится напрямую, без лишних библиотек — APK остаётся маленьким. */
    private fun loadCover(url: String?) {
        if (url.isNullOrBlank()) return
        viewLifecycleOwner.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) { downloadBitmap(url) } ?: return@launch
            binding?.imageCover?.setImageBitmap(bitmap)
        }
    }

    private fun downloadBitmap(url: String): Bitmap? = runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "YTD-Lite")
        }
        try {
            connection.inputStream.use { BitmapFactory.decodeStream(it) }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    // ---------------------------------------------------------------- качество

    /** Чипы качества: разрешения для видео, битрейт для аудио. */
    private fun renderQuality() {
        val binding = binding ?: return
        val info = info ?: return
        val audio = binding.buttonAudio.isChecked

        selectedHeight = null
        selectedBitrate = null
        chipValues.clear()
        binding.chipsQuality.removeAllViews()

        val options: List<Pair<String, Int?>> = if (audio) {
            listOf(
                getString(R.string.sheet_quality_best) to null,
                "192 kbps" to 192,
                "128 kbps" to 128,
                "64 kbps" to 64
            )
        } else {
            val heights = info.formats
                .filter { !it.isAudioOnly && it.height > 0 }
                .map { it.height }
                .distinct()
                .sortedDescending()
                .take(6)
            buildList {
                add(getString(R.string.sheet_quality_best) to null)
                heights.forEach { height -> add("${height}p" to height) }
            }
        }

        options.forEachIndexed { index, (label, value) ->
            val chip = layoutInflater
                .inflate(R.layout.view_quality_chip, binding.chipsQuality, false) as Chip
            chip.id = View.generateViewId()
            chip.text = label
            chip.isChecked = index == 0
            chipValues[chip.id] = value ?: BEST
            binding.chipsQuality.addView(chip)
        }

        binding.chipsQuality.setOnCheckedStateChangeListener { _, checkedIds ->
            val id = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val value = chipValues[id] ?: return@setOnCheckedStateChangeListener
            val chosen = value.takeIf { it != BEST }
            if (binding.buttonAudio.isChecked) {
                selectedBitrate = chosen
            } else {
                selectedHeight = chosen
            }
        }
    }

    /** Селектор видео: с ffmpeg склеиваем дорожки, без него берём готовый файл. */
    private fun videoSelector(height: Int?): String = when {
        engine.hasFfmpeg -> if (height != null) {
            "bv*[height<=$height]+ba/b[height<=$height]"
        } else {
            "bv*+ba/b"
        }

        else -> if (height != null) {
            "b[height<=$height]/b"
        } else {
            "b[ext=mp4]/b"
        }
    }

    // ---------------------------------------------------------------- в очередь

    private fun startDownload() {
        val binding = binding ?: return
        val info = info ?: return
        val context = context ?: return
        if (downloading) return

        val audio = binding.buttonAudio.isChecked
        val url = info.webpageUrl?.takeIf { it.isNotBlank() }
            ?: arguments?.getString(ARG_URL)
            ?: return
        val kind = if (audio) MediaKind.AUDIO else MediaKind.VIDEO
        val quality = if (audio) {
            selectedBitrate?.let { "$it kbps" } ?: getString(R.string.sheet_quality_best)
        } else {
            selectedHeight?.let { "${it}p" } ?: getString(R.string.sheet_quality_best)
        }

        val id = UUID.randomUUID().toString()
        val task = DownloadTask(
            id = id,
            url = url,
            title = info.title?.takeIf { it.isNotBlank() } ?: url,
            author = info.uploader,
            kind = kind,
            quality = quality,
            thumbnailUrl = info.thumbnailUrl,
            request = YtDlpDownloadRequest(
                url = url,
                // Настоящую папку подставит очередь — здесь значение не важно.
                outputDirectory = context.cacheDir,
                formatSelector = videoSelector(selectedHeight),
                audioOnly = audio,
                audioBitrateKbps = if (audio) selectedBitrate else null
            ),
            target = FolderPreferences.target(context, kind)
        )

        taskId = id
        DownloadQueue.enqueue(context, task)
        observeTask(id)
        postStatus(getString(R.string.queue_added))
        setDownloading(true)
    }

    /** Следит за своей задачей в общей очереди и показывает прогресс. */
    private fun observeTask(id: String) {
        observeJob?.cancel()
        observeJob = viewLifecycleOwner.lifecycleScope.launch {
            DownloadQueue.tasks.collect { tasks ->
                val task = tasks.firstOrNull { it.id == id }
                if (task == null) {
                    setDownloading(false)
                    return@collect
                }
                setDownloading(task.isActive)
                val binding = binding ?: return@collect
                when (task.state) {
                    DownloadState.RUNNING -> {
                        binding.progressDownload.isVisible = true
                        if (task.percent >= 0f) {
                            binding.progressBar.isIndeterminate = false
                            binding.progressBar.setProgressCompat(task.percent.toInt(), true)
                            binding.textPercent.text = "${task.percent.toInt()}%"
                        } else {
                            binding.progressBar.isIndeterminate = true
                            binding.textPercent.text = ""
                        }
                    }

                    DownloadState.PENDING -> {
                        binding.progressDownload.isVisible = true
                        binding.progressBar.isIndeterminate = true
                        binding.textPercent.text = ""
                    }

                    DownloadState.DONE -> postStatus(
                        getString(R.string.sheet_done, task.savedName ?: task.title)
                    )

                    DownloadState.FAILED -> postStatus(
                        getString(R.string.sheet_failed, task.message.orEmpty())
                    )

                    DownloadState.CANCELED -> postStatus(getString(R.string.sheet_canceled))
                }
            }
        }
    }

    private fun setDownloading(active: Boolean) {
        if (downloading == active) return
        downloading = active
        post {
            val binding = binding ?: return@post
            binding.buttonDownload.isEnabled = !active
            binding.buttonVideo.isEnabled = hasVideo && !active
            binding.buttonAudio.isEnabled = !active
            for (index in 0 until binding.chipsQuality.childCount) {
                binding.chipsQuality.getChildAt(index).isEnabled = !active
            }
            if (active) {
                binding.textStatus.isVisible = false
                binding.progressDownload.isVisible = true
                binding.progressBar.isIndeterminate = true
                binding.textPercent.text = ""
            } else {
                binding.progressDownload.isVisible = false
            }
        }
    }

    private fun postStatus(text: String) {
        post {
            val binding = binding ?: return@post
            binding.textStatus.isVisible = true
            binding.textStatus.text = text
        }
    }

    /** Колбэки очереди могут прийти из фонового потока — показываем через view.post. */
    private fun post(block: () -> Unit) {
        _binding?.root?.post(block)
    }

    companion object {
        const val TAG: String = "VideoSheet"

        private const val ARG_URL = "extra_url"

        /** Значение «Лучшее качество» в чипах. */
        private const val BEST = -1

        fun newInstance(url: String): VideoSheetFragment = VideoSheetFragment().apply {
            arguments = Bundle().apply { putString(ARG_URL, url) }
        }
    }
}
