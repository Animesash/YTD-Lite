package com.ytdlite.app

import android.app.Application
import android.content.Context
import com.google.android.material.color.DynamicColors
import com.ytdlite.app.data.queue.DownloadQueue
import com.ytdlite.app.data.settings.LanguageCatalog
import java.util.Locale

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

    /**
     * Язык применяем до создания ресурсов приложения: тогда строки на выбранном
     * языке получают и Activity, и фрагменты, и уведомления очереди.
     */
    override fun attachBaseContext(base: Context) {
        runCatching { LanguageCatalog.adoptPlatformChoice(base) }
        // Дата и размер файла форматируются по Locale.getDefault(): без этого
        // они оставались на языке системы, пока интерфейс уже переведён.
        runCatching { Locale.setDefault(LanguageCatalog.localeFor(base)) }
        super.attachBaseContext(LanguageCatalog.wrapSafely(base))
    }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        DownloadQueue.init(this)
    }
}
