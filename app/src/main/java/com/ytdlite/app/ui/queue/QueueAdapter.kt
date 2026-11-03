package com.ytdlite.app.ui.queue

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ytdlite.app.R
import com.ytdlite.app.data.queue.DownloadState
import com.ytdlite.app.data.queue.DownloadTask
import com.ytdlite.app.data.settings.MediaKind
import com.ytdlite.app.databinding.ViewQueueItemBinding

/**
 * Список задач «Очереди»: название, тип и качество, живой статус с прогрессом.
 * Кнопка справа: отменить активную, повторить упавшую, убрать завершённую.
 */
class QueueAdapter(
    private val onCancel: (String) -> Unit,
    private val onRetry: (String) -> Unit,
    private val onRemove: (String) -> Unit
) : ListAdapter<DownloadTask, QueueAdapter.TaskHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskHolder =
        TaskHolder(
            ViewQueueItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: TaskHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskHolder(private val binding: ViewQueueItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: DownloadTask) {
            val context = binding.root.context

            binding.imageIcon.setImageResource(
                if (task.kind == MediaKind.AUDIO) R.drawable.ic_music_note else R.drawable.ic_movie
            )
            binding.textTitle.text = task.title

            val kind = context.getString(
                if (task.kind == MediaKind.AUDIO) R.string.sheet_audio else R.string.sheet_video
            )
            val quality = task.quality ?: context.getString(R.string.sheet_quality_best)
            val details = listOf(kind, quality).joinToString(" · ")
            val status = when (task.state) {
                DownloadState.PENDING -> context.getString(R.string.queue_waiting)
                DownloadState.RUNNING -> context.getString(
                    R.string.queue_downloading,
                    task.percent.coerceAtLeast(0f).toInt()
                )

                DownloadState.DONE -> context.getString(
                    R.string.queue_done,
                    task.savedName ?: task.title
                )

                DownloadState.FAILED -> context.getString(
                    R.string.queue_failed,
                    task.message.orEmpty()
                )

                DownloadState.CANCELED -> context.getString(R.string.queue_canceled)
            }
            binding.textSubtitle.text = "$details · $status"

            val running = task.state == DownloadState.RUNNING
            binding.progress.isVisible = running || task.state == DownloadState.PENDING
            if (running) {
                if (task.percent >= 0f) {
                    binding.progress.isIndeterminate = false
                    binding.progress.setProgressCompat(task.percent.toInt(), true)
                } else {
                    binding.progress.isIndeterminate = true
                }
            } else {
                binding.progress.isIndeterminate = true
            }

            binding.buttonAction.setIconResource(
                if (task.state == DownloadState.FAILED) R.drawable.ic_refresh else R.drawable.ic_close
            )
            binding.buttonAction.contentDescription = context.getString(
                when (task.state) {
                    DownloadState.FAILED -> R.string.queue_retry
                    DownloadState.PENDING, DownloadState.RUNNING -> R.string.queue_cancel
                    else -> R.string.queue_remove
                }
            )
            binding.buttonAction.setOnClickListener {
                when (task.state) {
                    DownloadState.FAILED -> onRetry(task.id)
                    DownloadState.PENDING, DownloadState.RUNNING -> onCancel(task.id)
                    else -> onRemove(task.id)
                }
            }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<DownloadTask>() {
            override fun areItemsTheSame(oldItem: DownloadTask, newItem: DownloadTask): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: DownloadTask, newItem: DownloadTask): Boolean =
                oldItem == newItem
        }
    }
}
