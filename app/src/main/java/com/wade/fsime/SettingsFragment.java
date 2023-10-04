package com.wade.fsime;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import static android.provider.Settings.Secure.DEFAULT_INPUT_METHOD;
import com.wade.fsime.custom.CustomTable;

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
