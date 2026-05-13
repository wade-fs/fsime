/*
  Copyright 2021--2022 Conway
  Licensed under the GNU General Public License v3.0 (GPL-3.0-only).
  This is free software with NO WARRANTY etc. etc.,
  see LICENSE or <https://www.gnu.org/licenses/>.
*/
package com.wade.utilities

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.wade.fsime.R

object Contexty {
    @JvmStatic
    fun loadPreferenceString(
        context: Context,
        preferenceFileName: String?,
        preferenceKey: String?
    ): String? {
        val preferences = context.getSharedPreferences(preferenceFileName, Context.MODE_PRIVATE)
        return preferences.getString(preferenceKey, null)
    }

    @JvmStatic
    fun savePreferenceString(
        context: Context,
        preferenceFileName: String?,
        preferenceKey: String?,
        preferenceValue: String?
    ) {
        val preferences = context.getSharedPreferences(preferenceFileName, Context.MODE_PRIVATE)
        val preferencesEditor = preferences.edit()
        preferencesEditor.putString(preferenceKey, preferenceValue)
        preferencesEditor.apply()
    }

    fun showErrorMessage(context: Context?, message: String?) {
        val alertTextView = TextView(ContextThemeWrapper(context, R.style.InputMessage))
        alertTextView.text = message
        alertTextView.setTextIsSelectable(true)
        val alertDialogBuilder = AlertDialog.Builder(context, R.style.InputAlert)
        alertDialogBuilder
            .setPositiveButton(R.string.label__main_activity__return, null)
            .setView(alertTextView)
        val alertDialog: Dialog = alertDialogBuilder.create()
        val dialog_size = ViewGroup.LayoutParams.WRAP_CONTENT
        alertDialog.show()
        alertDialog.window!!.setLayout(dialog_size, dialog_size)
    }

    @JvmStatic
    fun showSystemInputMethodSettings(context: Context) {
        val inputMethodSettingsIntent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        context.startActivity(inputMethodSettingsIntent)
    }

    @JvmStatic
    fun showSystemKeyboardChanger(context: Context) {
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showInputMethodPicker()
    }
}