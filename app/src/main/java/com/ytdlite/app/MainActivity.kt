package com.ytdlite.app

import android.content.Context
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.google.android.material.transition.MaterialSharedAxis
import com.ytdlite.app.data.settings.LanguageCatalog
import com.ytdlite.app.databinding.ActivityMainBinding
import com.ytdlite.app.ui.SubScreenHost
import com.ytdlite.app.ui.TitledFragment
import com.ytdlite.app.ui.downloads.DownloadsFragment
import com.ytdlite.app.ui.home.HomeFragment
import com.ytdlite.app.ui.onboarding.FirstRunDialog
import com.ytdlite.app.ui.queue.QueueFragment
import com.ytdlite.app.ui.settings.SettingsFragment
import java.util.Locale

/**
 * Единственная Activity приложения.
 *
 * Снизу — панель из четырёх вкладок: Главная, Загрузки, Очередь, Настройки.
 * Вкладки переключаются скрытием/показом фрагментов, а вложенные экраны
 * (например «О приложении», «Обновление») кладутся в back stack поверх вкладки.
 */
class MainActivity : AppCompatActivity(), SubScreenHost {

    private lateinit var binding: ActivityMainBinding

    private class Screen(
        val tag: String,
        val factory: () -> Fragment
    )

    private val screens: Map<Int, Screen> = mapOf(
        R.id.nav_home to Screen("home") { HomeFragment() },
        R.id.nav_downloads to Screen("downloads") { DownloadsFragment() },
        R.id.nav_queue to Screen("queue") { QueueFragment() },
        R.id.nav_settings to Screen("settings") { SettingsFragment() }
    )

    /** Пока true, слушатель back stack не трогает вкладки (идёт восстановление состояния). */
    private var restoringState: Boolean = false

    /** Подавляет реакцию слушателя, когда подэкраны закрываются программно. */
    private var suppressBackStackListener: Boolean = false

    /**
     * Язык применяем к контексту Activity до создания её ресурсов: тогда и
     * разметка, и диалоги, и `getString()` сразу берут нужный язык.
     *
     * Никаких переопределений `getResources()`, `getLayoutInflater()` и
     * `getSystemService()` здесь быть не должно: `LayoutInflater.from(activity)`
     * сам приходит в `getSystemService`, и переопределение уходило в рекурсию —
     * на Android 16 приложение падало на старте.
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageCatalog.wrapSafely(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoringState = savedInstanceState != null
        // Material 3: контент под системными панелями, фон — цвет поверхности.
        enableEdgeToEdge()
        // Пока новое окно не отрисовано при пересоздании, видна подложка окна.
        window.setBackgroundDrawableResource(R.color.md_surface)
        // Даты и String.format берут Locale.getDefault() — держим его на языке
        // приложения, иначе в «Загрузках» месяц остаётся на языке системы.
        runCatching { Locale.setDefault(LanguageCatalog.localeFor(this)) }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { item ->
            if (restoringState) {
                true
            } else {
                showScreen(item.itemId)
                true
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (suppressBackStackListener) return@addOnBackStackChangedListener
            if (supportFragmentManager.backStackEntryCount == 0) {
                // Вернулись из вложенного экрана — снова показываем текущую вкладку.
                val current = currentTabFragment()
                if (current != null && current.isHidden) {
                    supportFragmentManager.commit {
                        setReorderingAllowed(true)
                        show(current)
                    }
                }
            }
            updateToolbar()
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_home
            // Приветственное окно первого запуска (галочка «Больше не показывать»).
            FirstRunDialog.showIfNeeded(this)
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val fragmentManager = supportFragmentManager
                    if (fragmentManager.backStackEntryCount > 0) {
                        fragmentManager.popBackStack()
                    } else if (binding.bottomNav.selectedItemId != R.id.nav_home) {
                        binding.bottomNav.selectedItemId = R.id.nav_home
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        restoringState = false
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        updateToolbar()
    }

    /**
     * Применяет выбранный язык.
     *
     * Единственный надёжный способ — пересоздать Activity: язык задаётся в
     * [attachBaseContext], то есть работает только для нового экземпляра.
     * Пересоздание идёт с сохранением состояния, поэтому пользователь остаётся
     * на том же экране («Настройки» → «Язык»), но уже на новом языке.
     */
    fun onLanguageChanged() {
        runCatching { Locale.setDefault(LanguageCatalog.localeFor(this)) }
        recreate()
    }

    /**
     * Кладёт экран поверх текущего: вкладки или уже открытого подэкрана
     * (например «Настройки» → «Обновление» → «yt-dlp»).
     */
    override fun openSubScreen(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val current = fragmentManager.fragments
            .lastOrNull { it.isAdded && !it.isHidden } ?: return

        // Material 3 Expressive motion: вложенный экран «выдвигается» по оси Z.
        fragment.enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        fragment.returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)

        fragmentManager.commit {
            setReorderingAllowed(true)
            hide(current)
            add(R.id.fragment_container, fragment)
            addToBackStack(fragment.javaClass.simpleName)
        }
    }

    /** Показывает экран выбранной вкладки, остальные скрывает. */
    private fun showScreen(itemId: Int) {
        val screen = screens[itemId] ?: return

        // При переходе на другую вкладку закрываем открытые подэкраны.
        closeSubScreens()

        val fragmentManager = supportFragmentManager
        val target = fragmentManager.findFragmentByTag(screen.tag)
        val others = fragmentManager.fragments.filter { it.tag != screen.tag }

        fragmentManager.commit {
            setReorderingAllowed(true)
            others.forEach { hide(it) }
            if (target == null) {
                add(R.id.fragment_container, screen.factory(), screen.tag)
            } else {
                show(target)
            }
        }
        updateToolbar()
    }

    private fun closeSubScreens() {
        val fragmentManager = supportFragmentManager
        if (fragmentManager.backStackEntryCount == 0) return
        suppressBackStackListener = true
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        suppressBackStackListener = false
    }

    private fun currentTabFragment(): Fragment? {
        val screen = screens[binding.bottomNav.selectedItemId] ?: return null
        return supportFragmentManager.findFragmentByTag(screen.tag)
    }

    private fun updateToolbar() {
        val fragmentManager = supportFragmentManager
        if (fragmentManager.backStackEntryCount > 0) {
            val top = fragmentManager.fragments.lastOrNull { !it.isHidden && it.isAdded }
            val titled = top as? TitledFragment
            val titleRes = titled?.screenTitleRes
            if (titled != null && titleRes != null) {
                binding.toolbar.setTitle(titled.screenTitle ?: getString(titleRes))
                binding.toolbar.setSubtitle(null)
                binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
                binding.toolbar.setNavigationOnClickListener {
                    supportFragmentManager.popBackStack()
                }
                return
            }
        }

        // На вкладках сверху только название приложения: подпись вкладки
        // дублировала бы нижнюю навигацию.
        binding.toolbar.setNavigationIcon(null)
        binding.toolbar.setSubtitle(null)
        binding.toolbar.setTitle(brandedTitle())
    }

    /** «YTD» — фирменным голубым, «-Lite» — обычным цветом темы. */
    private fun brandedTitle(): CharSequence {
        val full = getString(R.string.app_name)
        val split = full.indexOf('-')
        if (split <= 0) return full
        val color = ContextCompat.getColor(this, R.color.md_brand_blue)
        return SpannableString(full).apply {
            setSpan(
                ForegroundColorSpan(color),
                0,
                split,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
}
