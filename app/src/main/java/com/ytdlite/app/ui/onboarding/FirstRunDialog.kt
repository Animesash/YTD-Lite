package com.ytdlite.app.ui.onboarding

import android.app.Activity
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ytdlite.app.R
import com.ytdlite.app.data.settings.FirstRunPreferences
import com.ytdlite.app.databinding.DialogFirstRunBinding

/**
 * Приветственное окно первого запуска: о чём приложение, что это alpha-версия.
 *
 * Галочка «Больше не показывать» сохраняется в [FirstRunPreferences]; если её
 * не отметили, окно появится снова при следующем запуске.
 */
object FirstRunDialog {

    fun showIfNeeded(activity: Activity) {
        val preferences = FirstRunPreferences(activity.applicationContext)
        if (preferences.dismissed) return

        val binding = DialogFirstRunBinding.inflate(LayoutInflater.from(activity))

        MaterialAlertDialogBuilder(
            activity,
            com.google.android.material.R.style.MaterialAlertDialog_Material3Expressive
        )
            .setTitle(R.string.first_run_title)
            .setMessage(R.string.first_run_message)
            .setView(binding.root)
            .setPositiveButton(R.string.first_run_ok) { _, _ ->
                if (binding.dontShow.isChecked) {
                    preferences.dismissed = true
                }
            }
            .show()
    }
}
