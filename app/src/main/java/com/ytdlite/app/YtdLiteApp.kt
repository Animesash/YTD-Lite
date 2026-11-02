package com.ytdlite.app

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.ytdlite.app.data.queue.DownloadQueue

/**
 * Точка входа приложения.
 *
 * Включает Material You: на Android 12+ цветовая схема подстраивается
 * под системные динамические цвета. На более старых версиях остаётся
 * фирменная красно-нейтральная палитра Material 3 из values/colors.xml.
 *
 * Ещё здесь инициализируется общая очередь загрузок: она живёт всё время
 * работы приложения, поэтому загрузка продолжается, даже если закрыть лист
 * предпросмотра или уйти на другую вкладку.
 */
class YtdLiteApp : Application() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        DownloadQueue.init(this)
    }
}
