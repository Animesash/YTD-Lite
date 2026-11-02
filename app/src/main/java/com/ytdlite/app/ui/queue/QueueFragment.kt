package com.ytdlite.app.ui.queue

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ytdlite.app.R
import com.ytdlite.app.data.queue.DownloadQueue
import com.ytdlite.app.data.queue.DownloadTask
import com.ytdlite.app.databinding.FragmentQueueBinding
import kotlinx.coroutines.launch

/**
 * Вкладка «Очередь»: живые задачи из общей [DownloadQueue].
 *
 * Загрузки идут по одной, поэтому в списке видно, что качается сейчас,
 * что ждёт очереди, что готово и что упало (с кнопкой «Повторить»).
 */
class QueueFragment : Fragment(R.layout.fragment_queue) {

    private var _binding: FragmentQueueBinding? = null
    private val binding: FragmentQueueBinding
        get() = requireNotNull(_binding) { "binding доступен между onCreateView и onDestroyView" }

    private val adapter = QueueAdapter(
        onCancel = { id -> DownloadQueue.cancel(id) },
        onRetry = { id -> DownloadQueue.retry(id) },
        onRemove = { id -> DownloadQueue.remove(id) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQueueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.listQueue.layoutManager = LinearLayoutManager(requireContext())
        binding.listQueue.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            DownloadQueue.tasks.collect { tasks -> render(tasks) }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun render(tasks: List<DownloadTask>) {
        val binding = _binding ?: return
        adapter.submitList(tasks)
        val empty = tasks.isEmpty()
        binding.emptyQueue.isVisible = empty
        binding.listQueue.isVisible = !empty
    }
}
