package com.ytdlite.app.ui

import androidx.fragment.app.Fragment

/**
 * Реализуется [com.ytdlite.app.MainActivity].
 *
 * Позволяет экранам (например, «Настройкам») открывать вложенные экраны
 * поверх вкладки: «О приложении», «Обновление».
 */
interface SubScreenHost {

    fun openSubScreen(fragment: Fragment)
}

/** Экран, у которого есть своё название для тулбара. */
interface TitledFragment {

    @get:androidx.annotation.StringRes
    val screenTitleRes: Int

    /**
     * Необязательный заголовок вместо строкового ресурса — например версия
     * («v0.1.0-alpha») на экране подробностей истории изменений.
     */
    val screenTitle: CharSequence? get() = null
}
