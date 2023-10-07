package com.wade.fsime;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {
    KeyboardPreferences keyboardPreferences;
    Context styledContext;
    String rootPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        styledContext = getPreferenceScreen().getContext();
        rootPreference = rootKey;
        keyboardPreferences = new KeyboardPreferences(requireActivity());

        Bundle bundle = this.getArguments();
        if (bundle != null &&
                (bundle.getInt("notification") == 1)) {
            scrollToPreference("notification");
        }
    }

    // 處理 button 事件，例如要叫出客製化輸入表格
    @SuppressLint("RestrictedApi")
    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == null || preference.getKey() == null) {
            //Run Intent
            return false;
        }
        switch (preference.getKey()) {
            default:
                break;
        }
        return super.onPreferenceTreeClick(preference);
    }
}
