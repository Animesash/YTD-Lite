package com.ytdlite.app.ui.downloads

import android.annotation.SuppressLint
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ytdlite.app.R
import com.ytdlite.app.data.settings.LanguageCatalog
import com.ytdlite.app.data.download.DownloadedFile
import com.ytdlite.app.data.settings.MediaKind
import com.ytdlite.app.databinding.ViewDownloadItemBinding
import java.text.DateFormat
import java.util.Date

/**
 * История «Загрузок»: имя файла, тип, размер и дата. Нажатие — открыть,
 * кнопка справа — удалить (с подтверждением).
 */
class DownloadsAdapter(
    private val onOpen: (DownloadedFile) -> Unit,
    private val onDelete: (DownloadedFile) -> Unit
) : RecyclerView.Adapter<DownloadsAdapter.FileHolder>() {

    private var items: List<DownloadedFile> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submit(files: List<DownloadedFile>) {
        items = files
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileHolder =
        FileHolder(
            ViewDownloadItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: FileHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class FileHolder(private val binding: ViewDownloadItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(file: DownloadedFile) {
            val context = binding.root.context

            binding.imageIcon.setImageResource(
                if (file.kind == MediaKind.AUDIO) R.drawable.ic_music_note else R.drawable.ic_movie
            )
            binding.textTitle.text = file.name
            binding.textSubtitle.text = subtitle(file)

            binding.root.setOnClickListener { onOpen(file) }
            binding.buttonDelete.setOnClickListener { onDelete(file) }
            binding.root.contentDescription = context.getString(R.string.downloads_open, file.name)
        }

        private fun subtitle(file: DownloadedFile): String {
            val context = binding.root.context
            val kind = context.getString(
                if (file.kind == MediaKind.AUDIO) R.string.sheet_audio else R.string.sheet_video
            )
            val size = Formatter.formatShortFileSize(context, file.sizeBytes)
            val locale = LanguageCatalog.localeFor(context)
            val date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale)
                .format(Date(file.savedAt))
            return listOf(kind, size, date).joinToString(" · ")
        }
    }
}
