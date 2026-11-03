package com.ytdlite.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ytdlite.app.MainActivity
import com.ytdlite.app.R
import com.ytdlite.app.data.settings.LanguageCatalog
import com.ytdlite.app.databinding.FragmentLanguageBinding
import com.ytdlite.app.databinding.ViewLanguageOptionBinding
import com.ytdlite.app.ui.TitledFragment

/**
 * Экран «Язык»: белорусский, молдавский (румынский), русский, украинский,
 * английский и вариант «как в системе».
 */
class LanguageFragment : Fragment(R.layout.fragment_language), TitledFragment {

    override val screenTitleRes: Int = R.string.language_title

    private var _binding: FragmentLanguageBinding? = null
    private val binding: FragmentLanguageBinding
        get() = requireNotNull(_binding) { "binding доступен только между onCreateView и onDestroyView" }

    private val options = mutableListOf<Pair<ViewLanguageOptionBinding, String?>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLanguageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val current = LanguageCatalog.currentTag(requireContext())
        options.clear()
        LanguageCatalog.entries.forEach { tag ->
            val item = ViewLanguageOptionBinding.inflate(
                layoutInflater,
                binding.optionsContainer,
                true
            )
            // У всех пунктов один и тот же id радиокнопки (R.id.radio). Из-за
            // этого FragmentManager при пересоздании Activity (а смена языка
            // как раз пересоздаёт её) сохранял состояние по id и раздавал одну
            // и ту же галочку всем пунктам: после выбора UK галочка пропадала,
            // после выбора EN появлялась на всех строках сразу. Даём каждой
            // радиокнопке свой id и вообще не сохраняем её состояние — выбор
            // всегда берём из LanguageCatalog.currentTag().
            item.radio.id = View.generateViewId()
            item.radio.isSaveEnabled = false

            val labelRes = LanguageCatalog.labelRes(tag) ?: R.string.language_system
            item.label.text = getString(labelRes)
            item.radio.isChecked = tag == current
            item.root.setOnClickListener { select(tag) }
            options += item to tag
        }
    }

    override fun onDestroyView() {
        options.clear()
        _binding = null
        super.onDestroyView()
    }

    private fun select(tag: String?) {
        val context = requireContext()
        syncChecks(tag)
        if (tag == LanguageCatalog.currentTag(context)) return

        // Запоминаем выбор и сразу перерисовываем интерфейс на новом языке:
        // Activity не перезапускается, поэтому нет тёмного кадра.
        LanguageCatalog.apply(context, tag)
        (activity as? MainActivity)?.onLanguageChanged()
    }

    private fun syncChecks(tag: String?) {
        options.forEach { (item, itemTag) -> item.radio.isChecked = itemTag == tag }
    }
}
