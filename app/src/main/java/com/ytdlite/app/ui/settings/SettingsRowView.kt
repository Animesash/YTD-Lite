package com.ytdlite.app.ui.settings

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.ytdlite.app.databinding.ViewSettingsRowBinding
import kotlin.math.roundToInt

/**
 * Строка списка в стиле Material 3: иконка, заголовок, пояснение и «шеврон».
 * Используется на экранах «Настройки», «О приложении», «Обновление».
 */
class SettingsRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding =
        ViewSettingsRowBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(72)
        setPadding(dp(16), dp(14), dp(16), dp(14))
        setBackgroundResource(selectableItemBackground())
        isFocusable = true
    }

    fun setIcon(@DrawableRes iconRes: Int) {
        binding.icon.setImageResource(iconRes)
    }

    fun setTitle(title: CharSequence) {
        binding.title.text = title
    }

    fun setSubtitle(subtitle: CharSequence?) {
        binding.subtitle.text = subtitle
        binding.subtitle.isVisible = !subtitle.isNullOrBlank()
    }

    /** null — скрыть завершающую иконку. */
    fun setTrailingIcon(@DrawableRes iconRes: Int?) {
        if (iconRes == null) {
            binding.trailing.isVisible = false
        } else {
            binding.trailing.isVisible = true
            binding.trailing.setImageResource(iconRes)
        }
    }

    fun setOnRowClickListener(listener: () -> Unit) {
        setOnClickListener { listener() }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun selectableItemBackground(): Int {
        val out = TypedValue()
        requireNotNull(context.theme).resolveAttribute(
            android.R.attr.selectableItemBackground,
            out,
            true
        )
        return out.resourceId
    }
}
