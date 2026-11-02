package com.ytdlite.app.data.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.ytdlite.app.R

/**
 * Языки интерфейса: белорусский, молдавский (румынский), русский, украинский,
 * английский и вариант «как в системе».
 *
 * Переключение — per-app language API AppCompat: выбор сохраняется сам и
 * применяется сразу, Activity пересоздаётся на новом языке.
 */
object LanguageCatalog {

    const val TAG_BE = "be"
    const val TAG_RO = "ro"
    const val TAG_RU = "ru"
    const val TAG_UK = "uk"
    const val TAG_EN = "en"

    /** Порядок пунктов в списке; null — «как в системе». */
    val entries: List<String?> = listOf(null, TAG_BE, TAG_RO, TAG_RU, TAG_UK, TAG_EN)

    /** null — язык не из списка (показываем сам тег). */
    @StringRes
    fun labelRes(tag: String?): Int? = when (tag) {
        null -> R.string.language_system
        TAG_BE -> R.string.language_be
        TAG_RO -> R.string.language_ro
        TAG_RU -> R.string.language_ru
        TAG_UK -> R.string.language_uk
        TAG_EN -> R.string.language_en
        else -> null
    }

    /** Выбранный язык приложения; null — берётся системный. */
    fun currentTag(): String? = AppCompatDelegate.getApplicationLocales()
        .toLanguageTags()
        .substringBefore(',')
        .substringBefore('-')
        .takeIf { it.isNotBlank() }

    fun apply(tag: String?) {
        val locales = if (tag == null) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    /** Название текущего языка для подписи в списке настроек. */
    fun currentLabel(context: Context): String {
        val tag = currentTag() ?: return context.getString(R.string.language_system)
        val res = labelRes(tag) ?: return tag
        return context.getString(res)
    }
}
