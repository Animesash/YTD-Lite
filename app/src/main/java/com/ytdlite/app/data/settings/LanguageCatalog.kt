package com.ytdlite.app.data.settings

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import com.ytdlite.app.R
import java.util.Locale

/**
 * Языки интерфейса: белорусский, молдавский (румынский), русский, украинский,
 * английский и вариант «как в системе».
 *
 * Выбор хранится в настройках приложения (`ytdlite-settings`, ключ
 * `language_tag`) и применяется через контекст: [wrap] вызывается из
 * `attachBaseContext`, поэтому все ресурсы — Activity, фрагментов, уведомлений —
 * сразу отдают строки на нужном языке.
 *
 * Раньше язык переключался системным per-app language API
 * (`AppCompatDelegate.setApplicationLocales`). Он обязательно перезапускает
 * Activity: при смене языка экран на секунду становился тёмным. Своя локаль
 * позволяет менять язык на месте, без перезапуска (см.
 * `MainActivity.onLanguageChanged`).
 */
object LanguageCatalog {

    const val TAG_BE = "be"
    const val TAG_RO = "ro"
    const val TAG_RU = "ru"
    const val TAG_UK = "uk"
    const val TAG_EN = "en"

    /** Порядок пунктов в списке; null — «как в системе». */
    val entries: List<String?> = listOf(null, TAG_BE, TAG_RO, TAG_RU, TAG_UK, TAG_EN)

    private const val PREFS = "ytdlite-settings"
    private const val KEY_LANGUAGE = "language_tag"

    /** Язык, который последним выставили системные «Языки приложения». */
    private const val KEY_PLATFORM_SEEN = "language_platform_seen"

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

    /** Выбранный в приложении язык; null — берётся системный. */
    fun currentTag(context: Context): String? = prefs(context)
        .getString(KEY_LANGUAGE, null)
        ?.substringBefore(',')
        ?.substringBefore('-')
        ?.takeIf { it.isNotBlank() }

    /** Запоминает выбор. null — «как в системе». */
    fun apply(context: Context, tag: String?) {
        prefs(context).edit().putString(KEY_LANGUAGE, tag.orEmpty()).apply()
    }

    /** Локаль для показа: выбранная в приложении или системная. */
    fun localeFor(context: Context, tag: String? = currentTag(context)): Locale =
        tag?.let { Locale.forLanguageTag(it) } ?: systemLocale()

    /**
     * Базовый контекст с нужной локалью. Вызывается из `attachBaseContext`
     * у [com.ytdlite.app.YtdLiteApp] и `MainActivity`.
     */
    fun wrap(base: Context, tag: String? = currentTag(base)): Context {
        if (tag == null) return base
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(config)
    }

    /**
     * Контекст на выбранном языке — для строк, которые строятся не из Activity:
     * статус yt-dlp, сообщения очереди, тексты ошибок движка.
     */
    fun localized(context: Context): Context =
        runCatching { wrap(context, currentTag(context)) }.getOrDefault(context)

    /**
     * [wrap] для `attachBaseContext`: если подмена локали почему-то не удалась,
     * приложение должно запуститься на языке системы, а не упасть.
     */
    fun wrapSafely(base: Context): Context = runCatching { wrap(base) }.getOrDefault(base)

    /** Название текущего языка для подписи в списке настроек. */
    fun currentLabel(context: Context): String {
        val tag = currentTag(context) ?: return context.getString(R.string.language_system)
        val res = labelRes(tag) ?: return tag
        return context.getString(res)
    }

    /**
     * Если язык поменяли системные «Языки приложения» (Android 13+), забираем
     * выбор оттуда — но только когда он действительно изменился, иначе
     * системное значение перебивало бы выбор внутри приложения при каждом
     * запуске.
     */
    fun adoptPlatformChoice(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val platform = runCatching {
            AppCompatDelegate.getApplicationLocales().toLanguageTags()
        }.getOrNull()?.substringBefore(',') ?: return
        val seen = prefs(context).getString(KEY_PLATFORM_SEEN, "").orEmpty()
        if (platform == seen) return
        prefs(context).edit()
            .putString(KEY_PLATFORM_SEEN, platform)
            .putString(KEY_LANGUAGE, platform)
            .apply()
    }

    /** Локаль системы. `locales` может быть пустым — тогда берём текущую. */
    private fun systemLocale(): Locale {
        val locales = Resources.getSystem().configuration.locales
        return if (locales.isEmpty) Locale.getDefault() else locales[0]
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
