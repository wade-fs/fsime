package com.wade.fsime

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {
    var keyboardPreferences: KeyboardPreferences? = null
    var styledContext: Context? = null
    var rootPreference: String? = null
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        styledContext = preferenceScreen.context
        rootPreference = rootKey
        keyboardPreferences = KeyboardPreferences(requireActivity())
        val bundle = this.arguments
        if (bundle != null && bundle.getInt("notification") == 1) {
            scrollToPreference("notification")
        }
    }

    // 處理 button 事件，例如要叫出客製化輸入表格
    @SuppressLint("RestrictedApi")
    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference == null || preference.key == null) {
            //Run Intent
            return false
        }
        when (preference.key) {
            else -> {}
        }
        return super.onPreferenceTreeClick(preference)
    }
}