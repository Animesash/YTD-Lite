package com.ytdlite.app.data.update

import androidx.annotation.StringRes
import com.ytdlite.app.R

/**
 * История изменений приложения.
 *
 * Новый выпуск добавляется первым элементом списка: версия, дата и описание.
 * Тексты лежат в строковых ресурсах всех локалей, поэтому история переводится
 * на все языки приложения.
 */
object Changelog {

    data class Entry(
        /** Версия как в `versionName`, например «v0.1.0-alpha». */
        val version: String,
        /** Дата выпуска, только цифры — одинакова для всех языков. */
        val date: String,
        /** Описание выпуска. */
        @StringRes val summary: Int
    )

    val entries: List<Entry> = listOf(
        Entry(
            version = "v0.1.0-alpha",
            date = "2.11.2026",
            summary = R.string.changelog_0_1_0_summary
        )
    )
}
