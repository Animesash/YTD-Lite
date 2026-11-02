package com.ytdlite.app.data.settings

import android.content.Context

/**
 * Флаг «Больше не показывать» для приветственного окна первого запуска.
 *
 * Пока флаг не выставлен, окно показывается при каждом старте приложения —
 * так его можно перечитать, если закрыл, не отметив галочку.
 */
class FirstRunPreferences(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** true — окно больше не показывать. */
    var dismissed: Boolean
        get() = prefs.getBoolean(KEY_DISMISSED, false)
        set(value) = prefs.edit().putBoolean(KEY_DISMISSED, value).apply()

    private companion object {
        const val PREFS_NAME = "ytdlite-settings"
        const val KEY_DISMISSED = "first_run_dialog_dismissed"
    }
}
